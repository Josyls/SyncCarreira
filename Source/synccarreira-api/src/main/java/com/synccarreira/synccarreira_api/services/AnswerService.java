package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.AnswerDTO;
import com.synccarreira.synccarreira_api.dto.AnswerInsertDTO;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionOptionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnswerService {

    private final AnswerRepository answerRepository;

    private final QuestionOptionRepository questionOptionRepository;

    private final StudentRepository studentRepository;

    private final AuthService authService;

    private final TrailService trailService;

    public AnswerService(
            final AnswerRepository answerRepository,
            final QuestionOptionRepository questionOptionRepository,
            final StudentRepository studentRepository,
            final AuthService authService,
            final TrailService trailService) {
        this.answerRepository = answerRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.studentRepository = studentRepository;
        this.authService = authService;
        this.trailService = trailService;
    }

    // O aluno é sempre o usuário do token: ninguém lê ou grava respostas de outro aluno
    @Transactional(readOnly = true)
    public List<AnswerDTO> findForLoggedStudent(Long trailId) {
        Student student = loggedStudent();
        List<Answer> result = answerRepository.findByStudentAndTrail(student.getId(), trailId);
        return result.stream().map(AnswerDTO::new).toList();
    }

    // Nas perguntas de MULTIPLA_ESCOLHA o aluno pode marcar várias opções: cada opção marcada é uma resposta.
    // Nos demais tipos, cada aluno tem uma resposta por pergunta: responder de novo substitui a opção escolhida.
    // A linha do aluno fica travada até o fim da transação, então respostas simultâneas do mesmo aluno
    // (cliques rápidos, várias abas) são gravadas uma de cada vez, sem deadlock no MySQL.
    // Se mesmo assim o banco derrubar a transação por bloqueio, ela é repetida automaticamente.
    @Retryable(
            retryFor = PessimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Transactional
    public AnswerDTO insert(AnswerInsertDTO dto) {
        QuestionOption questionOption = findQuestionOption(dto.getQuestionOptionId());
        Student student = lockLoggedStudent();
        Question question = questionOption.getQuestion();
        checkTrailAccess(question);

        List<Answer> previousAnswers = answerRepository.findByStudentAndQuestion(student.getId(), question.getId());
        Answer entity = isMultipleChoice(question)
                ? checkOption(previousAnswers, student, questionOption)
                : replaceAnswer(previousAnswers, student, questionOption);
        recalculateStudentScore(student);
        return new AnswerDTO(entity);
    }

    // Desmarca uma opção de uma pergunta de MULTIPLA_ESCOLHA. Se a opção já não estava marcada, nada muda.
    // Nos demais tipos não há como desmarcar: a resposta só é substituída por outra opção.
    @Retryable(
            retryFor = PessimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Transactional
    public void uncheck(Long questionOptionId) {
        QuestionOption questionOption = findQuestionOption(questionOptionId);
        Student student = lockLoggedStudent();
        Question question = questionOption.getQuestion();
        checkTrailAccess(question);
        if (!isMultipleChoice(question)) {
            throw new BusinessException("Só é possível desmarcar opções de perguntas de múltipla escolha.");
        }

        // Apaga pelo id das respostas encontradas (e não por faixa), pelo mesmo motivo do replaceAnswer
        List<Answer> checkedAnswers = answerRepository.findByStudentAndQuestion(student.getId(), question.getId())
                .stream()
                .filter(answer -> isSameOption(answer, questionOption))
                .toList();
        if (checkedAnswers.isEmpty()) {
            return;
        }
        answerRepository.deleteAll(checkedAnswers);
        recalculateStudentScore(student);
    }

    // Marcar uma opção que já estava marcada não cria outra resposta
    private Answer checkOption(List<Answer> previousAnswers, Student student, QuestionOption questionOption) {
        return previousAnswers.stream()
                .filter(answer -> isSameOption(answer, questionOption))
                .findFirst()
                .orElseGet(() -> answerRepository.save(new Answer(null, student, questionOption)));
    }

    // Atualiza a resposta existente em vez de apagar e inserir: o DELETE por faixa travava intervalos
    // do índice e causava deadlock entre transações simultâneas
    private Answer replaceAnswer(List<Answer> previousAnswers, Student student, QuestionOption questionOption) {
        Answer entity = previousAnswers.isEmpty() ? new Answer() : previousAnswers.getFirst();
        if (previousAnswers.size() > 1) {
            answerRepository.deleteAll(previousAnswers.subList(1, previousAnswers.size()));
        }
        entity.setStudent(student);
        entity.setQuestionOption(questionOption);
        return answerRepository.save(entity);
    }

    private static boolean isMultipleChoice(Question question) {
        return question.getQuestionType() == QuestionType.MULTIPLA_ESCOLHA;
    }

    private static boolean isSameOption(Answer answer, QuestionOption questionOption) {
        return answer.getQuestionOption() != null && questionOption.getId().equals(answer.getQuestionOption().getId());
    }

    private QuestionOption findQuestionOption(Long questionOptionId) {
        return questionOptionRepository.findById(questionOptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question option not found"));
    }

    // Mesma regra da listagem de perguntas: não é possível responder uma trilha ainda bloqueada
    private void checkTrailAccess(Question question) {
        if (!trailService.canAccess(question.getTrail().getId())) {
            throw new ForbiddenException("Trilha não liberada. Preencha seu perfil e conclua a trilha anterior para responder estas perguntas.");
        }
    }

    // Recalcula a partir de todas as respostas atuais somadas às afinidades do perfil (RF-14)
    private void recalculateStudentScore(Student student) {
        student.recalculateScores(answerRepository.findByStudentId(student.getId()));
        studentRepository.save(student);
    }

    private Student lockLoggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }

    private Student loggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }
}
