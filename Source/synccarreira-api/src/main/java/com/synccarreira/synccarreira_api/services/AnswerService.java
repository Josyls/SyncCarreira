package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.AnswerDTO;
import com.synccarreira.synccarreira_api.dto.AnswerInsertDTO;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionOptionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
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

    // Cada aluno tem uma resposta por pergunta: responder de novo substitui a opção escolhida.
    // A linha do aluno fica travada até o fim da transação, então respostas simultâneas do mesmo aluno
    // (cliques rápidos, várias abas) são gravadas uma de cada vez, sem deadlock no MySQL.
    // Se mesmo assim o banco derrubar a transação por bloqueio, ela é repetida automaticamente.
    @Retryable(
            retryFor = PessimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Transactional
    public AnswerDTO insert(AnswerInsertDTO dto) {
        QuestionOption questionOption = questionOptionRepository.findById(dto.getQuestionOptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question option not found"));
        Student student = lockLoggedStudent();
        Question question = questionOption.getQuestion();
        // Mesma regra da listagem de perguntas: não é possível responder uma trilha ainda bloqueada
        if (!trailService.canAccess(question.getTrail().getId())) {
            throw new ForbiddenException("Trilha não liberada. Preencha seu perfil e conclua a trilha anterior para responder estas perguntas.");
        }

        // Atualiza a resposta existente em vez de apagar e inserir: o DELETE por faixa travava intervalos
        // do índice e causava deadlock entre transações simultâneas
        List<Answer> previousAnswers = answerRepository.findByStudentAndQuestion(student.getId(), question.getId());
        Answer entity = previousAnswers.isEmpty() ? new Answer() : previousAnswers.getFirst();
        if (previousAnswers.size() > 1) {
            answerRepository.deleteAll(previousAnswers.subList(1, previousAnswers.size()));
        }
        entity.setStudent(student);
        entity.setQuestionOption(questionOption);
        entity = answerRepository.save(entity);
        recalculateStudentScore(student);
        return new AnswerDTO(entity);
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
