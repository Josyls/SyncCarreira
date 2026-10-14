package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.AnswerDTO;
import com.synccarreira.synccarreira_api.dto.AnswerInsertDTO;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionOptionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.QuestionFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
public class AnswerServiceTests {

    @InjectMocks
    private AnswerService service;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AuthService authService;

    @Mock
    private TrailService trailService;

    private Student student;
    // Pergunta de MULTIPLA_ESCOLHA: Matemática (1.0 em exatas) e Biologia (1.0 em biológicas)
    private Question question;
    private QuestionOption option;
    private QuestionOption otherOption;
    // Pergunta LIKERT, de resposta única: Concordo (1.0 em humanas) e Discordo (1.0 em artes)
    private Question singleAnswerQuestion;
    private QuestionOption agree;
    private QuestionOption disagree;
    private AnswerInsertDTO dto;
    private Long nonExistingId;
    private Long trailId;

    @BeforeEach
    void setUp() {
        student = StudentFactory.createStudent();
        question = QuestionFactory.createQuestion();
        option = question.getOptions().getFirst();
        otherOption = new QuestionOption(2L, "Biologia", 0.0, 1.0, 0.0, 0.0, question);
        question.getOptions().add(otherOption);
        singleAnswerQuestion = QuestionFactory.createLikertQuestion();
        agree = singleAnswerQuestion.getOptions().get(0);
        disagree = singleAnswerQuestion.getOptions().get(1);
        dto = new AnswerInsertDTO(option.getId());
        nonExistingId = 100L;
        trailId = 1L;
    }

    private void mockValidRequest(QuestionOption chosen) {
        Mockito.when(questionOptionRepository.findById(chosen.getId())).thenReturn(Optional.of(chosen));
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findByIdForUpdate(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(trailService.canAccess(chosen.getQuestion().getTrail().getId())).thenReturn(true);
    }

    private void mockSave() {
        Mockito.when(answerRepository.save(any())).thenAnswer(invocation -> {
            // Resposta nova recebe o id 10; resposta existente mantém o id
            Answer saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(10L);
            }
            return saved;
        });
    }

    private void mockPreviousAnswers(Question answered, List<Answer> answers) {
        Mockito.when(answerRepository.findByStudentAndQuestion(student.getId(), answered.getId())).thenReturn(answers);
    }

    private void mockCurrentAnswers(List<Answer> answers) {
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(answers);
    }

    // Perguntas de resposta única (todos os tipos, exceto MULTIPLA_ESCOLHA)

    @Test
    void insertShouldCreateNewAnswerWhenQuestionWasNotAnsweredYet() {
        mockValidRequest(agree);
        mockSave();
        mockPreviousAnswers(singleAnswerQuestion, List.of());
        mockCurrentAnswers(List.of());

        AnswerDTO result = service.insert(new AnswerInsertDTO(agree.getId()));

        Assertions.assertEquals(10L, result.getId());
        // O conteúdo da resposta vem do texto da opção escolhida
        Assertions.assertEquals(agree.getOptionText(), result.getContent());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @Test
    void insertShouldUpdateExistingAnswerInsteadOfDeletingAndInserting() {
        mockValidRequest(agree);
        mockSave();
        Answer previousAnswer = new Answer(7L, student, disagree);
        mockPreviousAnswers(singleAnswerQuestion, List.of(previousAnswer));
        mockCurrentAnswers(List.of(previousAnswer));

        AnswerDTO result = service.insert(new AnswerInsertDTO(agree.getId()));

        // A mesma resposta (id 7) passa a apontar para a nova opção
        ArgumentCaptor<Answer> captor = ArgumentCaptor.forClass(Answer.class);
        Mockito.verify(answerRepository).save(captor.capture());
        Assertions.assertEquals(7L, captor.getValue().getId());
        Assertions.assertEquals(agree.getId(), captor.getValue().getQuestionOption().getId());
        Assertions.assertEquals(7L, result.getId());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @ParameterizedTest
    @EnumSource(value = QuestionType.class, names = "MULTIPLA_ESCOLHA", mode = EnumSource.Mode.EXCLUDE)
    void insertShouldReplacePreviousAnswerWhenQuestionIsNotMultipleChoice(QuestionType type) {
        singleAnswerQuestion.setQuestionType(type);
        mockValidRequest(agree);
        mockSave();
        Answer previousAnswer = new Answer(7L, student, disagree);
        mockPreviousAnswers(singleAnswerQuestion, List.of(previousAnswer));
        mockCurrentAnswers(List.of(previousAnswer));

        AnswerDTO result = service.insert(new AnswerInsertDTO(agree.getId()));

        // Continua uma única resposta por pergunta, agora com a nova opção
        Assertions.assertEquals(7L, result.getId());
        Assertions.assertEquals(agree.getId(), previousAnswer.getQuestionOption().getId());
        Mockito.verify(answerRepository, Mockito.times(1)).save(any());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @Test
    void insertShouldKeepOnlyOneAnswerWhenStudentHasDuplicatedAnswersForQuestion() {
        mockValidRequest(agree);
        mockSave();
        Answer oldest = new Answer(7L, student, disagree);
        Answer duplicated = new Answer(8L, student, disagree);
        Answer anotherDuplicated = new Answer(9L, student, agree);
        mockPreviousAnswers(singleAnswerQuestion, List.of(oldest, duplicated, anotherDuplicated));
        mockCurrentAnswers(List.of(oldest));

        service.insert(new AnswerInsertDTO(agree.getId()));

        // Mantém a mais antiga (atualizada com a nova opção) e apaga as duplicadas
        Mockito.verify(answerRepository).deleteAll(List.of(duplicated, anotherDuplicated));
        Assertions.assertEquals(agree.getId(), oldest.getQuestionOption().getId());
    }

    // Perguntas de MULTIPLA_ESCOLHA: várias opções marcadas ao mesmo tempo

    @Test
    void insertShouldCreateAnswerWhenMultipleChoiceQuestionWasNotAnsweredYet() {
        mockValidRequest(option);
        mockSave();
        mockPreviousAnswers(question, List.of());
        mockCurrentAnswers(List.of());

        AnswerDTO result = service.insert(dto);

        Assertions.assertEquals(10L, result.getId());
        Assertions.assertEquals(option.getOptionText(), result.getContent());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @Test
    void insertShouldAddAnswerKeepingOtherCheckedOptionsWhenQuestionIsMultipleChoice() {
        mockValidRequest(option);
        mockSave();
        Answer previousAnswer = new Answer(7L, student, otherOption);
        mockPreviousAnswers(question, List.of(previousAnswer));
        mockCurrentAnswers(List.of(previousAnswer, new Answer(10L, student, option)));

        AnswerDTO result = service.insert(dto);

        // Uma resposta nova para a opção marcada; a resposta anterior continua com a outra opção
        ArgumentCaptor<Answer> captor = ArgumentCaptor.forClass(Answer.class);
        Mockito.verify(answerRepository).save(captor.capture());
        Assertions.assertEquals(10L, captor.getValue().getId());
        Assertions.assertEquals(option.getId(), captor.getValue().getQuestionOption().getId());
        Assertions.assertEquals(student, captor.getValue().getStudent());
        Assertions.assertEquals(10L, result.getId());
        Assertions.assertEquals(otherOption.getId(), previousAnswer.getQuestionOption().getId());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @Test
    void insertShouldSumWeightsOfAllCheckedOptionsWhenQuestionIsMultipleChoice() {
        mockValidRequest(option);
        mockSave();
        Answer previousAnswer = new Answer(7L, student, otherOption);
        mockPreviousAnswers(question, List.of(previousAnswer));
        mockCurrentAnswers(List.of(previousAnswer, new Answer(10L, student, option)));

        service.insert(dto);

        // Matemática soma 1.0 em exatas e Biologia soma 1.0 em biológicas
        Assertions.assertEquals(1.0, student.getExactSciencesScore());
        Assertions.assertEquals(1.0, student.getBiologicalSciencesScore());
        Assertions.assertEquals(0.0, student.getHumanitiesScore());
        Assertions.assertEquals(0.0, student.getArtsScore());
        Mockito.verify(studentRepository).save(student);
    }

    @Test
    void insertShouldNotDuplicateAnswerWhenOptionIsAlreadyChecked() {
        mockValidRequest(option);
        Answer checked = new Answer(7L, student, option);
        Answer other = new Answer(8L, student, otherOption);
        mockPreviousAnswers(question, List.of(checked, other));
        mockCurrentAnswers(List.of(checked, other));

        AnswerDTO result = service.insert(dto);

        // Marcar de novo devolve a resposta que já existia, sem gravar nem apagar nada
        Assertions.assertEquals(7L, result.getId());
        Mockito.verify(answerRepository, Mockito.never()).save(any());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    // Regras comuns a todos os tipos de pergunta

    @Test
    void insertShouldLockStudentRowBeforeSavingAnswer() {
        mockValidRequest(option);
        mockSave();
        mockPreviousAnswers(question, List.of());
        mockCurrentAnswers(List.of());

        service.insert(dto);

        InOrder inOrder = Mockito.inOrder(studentRepository, answerRepository);
        inOrder.verify(studentRepository).findByIdForUpdate(student.getId());
        inOrder.verify(answerRepository).save(any());
        Mockito.verify(studentRepository, Mockito.never()).findById(anyLong());
    }

    @Test
    void insertShouldRecalculateScoreUsingOnlyCurrentAnswers() {
        mockValidRequest(option);
        mockSave();
        mockPreviousAnswers(question, List.of());
        // Só a nova resposta (peso 1.0 em exatas) existe para o aluno
        Answer currentAnswer = new Answer(10L, student, option);
        mockCurrentAnswers(List.of(currentAnswer));

        service.insert(dto);

        Assertions.assertEquals(1.0, student.getExactSciencesScore());
        Assertions.assertEquals(0.0, student.getHumanitiesScore());
        Assertions.assertEquals(0.0, student.getBiologicalSciencesScore());
        Assertions.assertEquals(0.0, student.getArtsScore());
        Mockito.verify(studentRepository).save(student);
    }

    @Test
    void insertShouldAddProfileAffinitiesToRecalculatedScore() {
        // Afinidades do perfil (RF-14): humanas 3, exatas 2, biológicas 4, artes 5
        student = StudentFactory.createStudentWithCompletedProfile();
        mockValidRequest(option);
        mockSave();
        mockPreviousAnswers(question, List.of());
        Answer currentAnswer = new Answer(10L, student, option);
        mockCurrentAnswers(List.of(currentAnswer));

        service.insert(dto);

        // A opção escolhida soma 1.0 em exatas
        Assertions.assertEquals(3.0, student.getHumanitiesScore());
        Assertions.assertEquals(3.0, student.getExactSciencesScore());
        Assertions.assertEquals(4.0, student.getBiologicalSciencesScore());
        Assertions.assertEquals(5.0, student.getArtsScore());
    }

    @Test
    void insertShouldThrowForbiddenExceptionWhenTrailIsNotAccessible() {
        Mockito.when(questionOptionRepository.findById(option.getId())).thenReturn(Optional.of(option));
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findByIdForUpdate(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(trailService.canAccess(question.getTrail().getId())).thenReturn(false);

        Assertions.assertThrows(ForbiddenException.class, () -> service.insert(dto));
        Mockito.verify(answerRepository, Mockito.never()).findByStudentAndQuestion(anyLong(), anyLong());
        Mockito.verify(answerRepository, Mockito.never()).save(any());
        Mockito.verify(studentRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldThrowResourceNotFoundExceptionWhenQuestionOptionDoesNotExist() {
        Mockito.when(questionOptionRepository.findById(nonExistingId)).thenReturn(Optional.empty());
        AnswerInsertDTO invalidDto = new AnswerInsertDTO(nonExistingId);

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.insert(invalidDto));
        Mockito.verify(studentRepository, Mockito.never()).findByIdForUpdate(anyLong());
        Mockito.verify(answerRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldSaveAnswerForLoggedStudent() {
        mockValidRequest(option);
        mockSave();
        mockPreviousAnswers(question, List.of());
        mockCurrentAnswers(List.of());

        AnswerDTO result = service.insert(dto);

        Assertions.assertEquals(student.getId(), result.getStudent().getId());
    }

    @Test
    void insertShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(questionOptionRepository.findById(option.getId())).thenReturn(Optional.of(option));
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findByIdForUpdate(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.insert(dto));
        Mockito.verify(answerRepository, Mockito.never()).save(any());
    }

    // Desmarcar uma opção (somente MULTIPLA_ESCOLHA)

    @Test
    void uncheckShouldDeleteOnlyAnswerOfThatOptionAndRecalculateScore() {
        mockValidRequest(option);
        Answer checked = new Answer(7L, student, option);
        Answer other = new Answer(8L, student, otherOption);
        mockPreviousAnswers(question, List.of(checked, other));
        // Depois de desmarcar, só a outra opção (1.0 em biológicas) continua marcada
        mockCurrentAnswers(List.of(other));

        service.uncheck(option.getId());

        Mockito.verify(answerRepository).deleteAll(List.of(checked));
        Assertions.assertEquals(0.0, student.getExactSciencesScore());
        Assertions.assertEquals(1.0, student.getBiologicalSciencesScore());
        Mockito.verify(studentRepository).save(student);
    }

    @Test
    void uncheckShouldDeleteAllAnswersOfThatOptionWhenTheyAreDuplicated() {
        mockValidRequest(option);
        Answer checked = new Answer(7L, student, option);
        Answer other = new Answer(8L, student, otherOption);
        Answer duplicated = new Answer(9L, student, option);
        mockPreviousAnswers(question, List.of(checked, other, duplicated));
        mockCurrentAnswers(List.of(other));

        service.uncheck(option.getId());

        Mockito.verify(answerRepository).deleteAll(List.of(checked, duplicated));
    }

    @Test
    void uncheckShouldAllowUncheckingLastCheckedOption() {
        mockValidRequest(option);
        Answer checked = new Answer(7L, student, option);
        mockPreviousAnswers(question, List.of(checked));
        mockCurrentAnswers(List.of());

        service.uncheck(option.getId());

        // A pergunta volta a ficar sem resposta
        Mockito.verify(answerRepository).deleteAll(List.of(checked));
        Assertions.assertEquals(0.0, student.getExactSciencesScore());
        Mockito.verify(studentRepository).save(student);
    }

    @Test
    void uncheckShouldDoNothingWhenOptionIsNotChecked() {
        mockValidRequest(option);
        mockPreviousAnswers(question, List.of(new Answer(8L, student, otherOption)));

        service.uncheck(option.getId());

        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
        Mockito.verify(answerRepository, Mockito.never()).findByStudentId(anyLong());
        Mockito.verify(studentRepository, Mockito.never()).save(any());
    }

    @Test
    void uncheckShouldLockStudentRowBeforeDeletingAnswer() {
        mockValidRequest(option);
        Answer checked = new Answer(7L, student, option);
        mockPreviousAnswers(question, List.of(checked));
        mockCurrentAnswers(List.of());

        service.uncheck(option.getId());

        InOrder inOrder = Mockito.inOrder(studentRepository, answerRepository);
        inOrder.verify(studentRepository).findByIdForUpdate(student.getId());
        inOrder.verify(answerRepository).deleteAll(List.of(checked));
        Mockito.verify(studentRepository, Mockito.never()).findById(anyLong());
    }

    @ParameterizedTest
    @EnumSource(value = QuestionType.class, names = "MULTIPLA_ESCOLHA", mode = EnumSource.Mode.EXCLUDE)
    void uncheckShouldThrowBusinessExceptionWhenQuestionIsNotMultipleChoice(QuestionType type) {
        singleAnswerQuestion.setQuestionType(type);
        mockValidRequest(agree);

        Assertions.assertThrows(BusinessException.class, () -> service.uncheck(agree.getId()));
        Mockito.verify(answerRepository, Mockito.never()).findByStudentAndQuestion(anyLong(), anyLong());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
        Mockito.verify(studentRepository, Mockito.never()).save(any());
    }

    @Test
    void uncheckShouldThrowForbiddenExceptionWhenTrailIsNotAccessible() {
        Mockito.when(questionOptionRepository.findById(option.getId())).thenReturn(Optional.of(option));
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findByIdForUpdate(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(trailService.canAccess(question.getTrail().getId())).thenReturn(false);

        Assertions.assertThrows(ForbiddenException.class, () -> service.uncheck(option.getId()));
        Mockito.verify(answerRepository, Mockito.never()).findByStudentAndQuestion(anyLong(), anyLong());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @Test
    void uncheckShouldThrowResourceNotFoundExceptionWhenQuestionOptionDoesNotExist() {
        Mockito.when(questionOptionRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.uncheck(nonExistingId));
        Mockito.verify(studentRepository, Mockito.never()).findByIdForUpdate(anyLong());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @Test
    void uncheckShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(questionOptionRepository.findById(option.getId())).thenReturn(Optional.of(option));
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findByIdForUpdate(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.uncheck(option.getId()));
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    // Listagem

    @Test
    void findForLoggedStudentShouldReturnOnlyAnswersOfLoggedStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(answerRepository.findByStudentAndTrail(student.getId(), trailId)).thenReturn(List.of(new Answer(10L, student, option)));

        List<AnswerDTO> result = service.findForLoggedStudent(trailId);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(student.getId(), result.getFirst().getStudent().getId());
        Mockito.verify(answerRepository).findByStudentAndTrail(student.getId(), trailId);
    }

    @Test
    void findForLoggedStudentShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findForLoggedStudent(trailId));
        Mockito.verify(answerRepository, Mockito.never()).findByStudentAndTrail(anyLong(), anyLong());
    }
}
