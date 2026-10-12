package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.AnswerDTO;
import com.synccarreira.synccarreira_api.dto.AnswerInsertDTO;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionOptionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.QuestionFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private Question question;
    private QuestionOption option;
    private AnswerInsertDTO dto;
    private Long nonExistingId;
    private Long trailId;

    @BeforeEach
    void setUp() {
        student = StudentFactory.createStudent();
        question = QuestionFactory.createQuestion();
        option = question.getOptions().getFirst();
        dto = new AnswerInsertDTO(option.getId());
        nonExistingId = 100L;
        trailId = 1L;
    }

    private void mockValidInsert() {
        Mockito.when(questionOptionRepository.findById(option.getId())).thenReturn(Optional.of(option));
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findByIdForUpdate(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(trailService.canAccess(question.getTrail().getId())).thenReturn(true);
        Mockito.when(answerRepository.save(any())).thenAnswer(invocation -> {
            // Resposta nova recebe o id 10; resposta existente mantém o id
            Answer saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(10L);
            }
            return saved;
        });
    }

    @Test
    void insertShouldCreateNewAnswerWhenQuestionWasNotAnsweredYet() {
        mockValidInsert();
        Mockito.when(answerRepository.findByStudentAndQuestion(student.getId(), question.getId())).thenReturn(List.of());
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());

        AnswerDTO result = service.insert(dto);

        Assertions.assertEquals(10L, result.getId());
        // O conteúdo da resposta vem do texto da opção escolhida
        Assertions.assertEquals(option.getOptionText(), result.getContent());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @Test
    void insertShouldUpdateExistingAnswerInsteadOfDeletingAndInserting() {
        mockValidInsert();
        QuestionOption previousOption = new QuestionOption(2L, "Outra opção", 0.0, 0.0, 0.0, 1.0, question);
        Answer previousAnswer = new Answer(7L, student, previousOption);
        Mockito.when(answerRepository.findByStudentAndQuestion(student.getId(), question.getId())).thenReturn(List.of(previousAnswer));
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of(previousAnswer));

        AnswerDTO result = service.insert(dto);

        // A mesma resposta (id 7) passa a apontar para a nova opção
        ArgumentCaptor<Answer> captor = ArgumentCaptor.forClass(Answer.class);
        Mockito.verify(answerRepository).save(captor.capture());
        Assertions.assertEquals(7L, captor.getValue().getId());
        Assertions.assertEquals(option.getId(), captor.getValue().getQuestionOption().getId());
        Assertions.assertEquals(7L, result.getId());
        Mockito.verify(answerRepository, Mockito.never()).deleteAll(any());
    }

    @Test
    void insertShouldKeepOnlyOneAnswerWhenStudentHasDuplicatedAnswersForQuestion() {
        mockValidInsert();
        QuestionOption otherOption = new QuestionOption(2L, "Outra opção", 0.0, 0.0, 0.0, 1.0, question);
        Answer oldest = new Answer(7L, student, otherOption);
        Answer duplicated = new Answer(8L, student, otherOption);
        Answer anotherDuplicated = new Answer(9L, student, option);
        Mockito.when(answerRepository.findByStudentAndQuestion(student.getId(), question.getId()))
                .thenReturn(List.of(oldest, duplicated, anotherDuplicated));
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of(oldest));

        service.insert(dto);

        // Mantém a mais antiga (atualizada com a nova opção) e apaga as duplicadas
        Mockito.verify(answerRepository).deleteAll(List.of(duplicated, anotherDuplicated));
        Assertions.assertEquals(option.getId(), oldest.getQuestionOption().getId());
    }

    @Test
    void insertShouldLockStudentRowBeforeSavingAnswer() {
        mockValidInsert();
        Mockito.when(answerRepository.findByStudentAndQuestion(student.getId(), question.getId())).thenReturn(List.of());
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());

        service.insert(dto);

        InOrder inOrder = Mockito.inOrder(studentRepository, answerRepository);
        inOrder.verify(studentRepository).findByIdForUpdate(student.getId());
        inOrder.verify(answerRepository).save(any());
        Mockito.verify(studentRepository, Mockito.never()).findById(anyLong());
    }

    @Test
    void insertShouldRecalculateScoreUsingOnlyCurrentAnswers() {
        mockValidInsert();
        Mockito.when(answerRepository.findByStudentAndQuestion(student.getId(), question.getId())).thenReturn(List.of());
        // Depois da substituição, só a nova resposta (peso 1.0 em exatas) existe para o aluno
        Answer currentAnswer = new Answer(10L, student, option);
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of(currentAnswer));

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
        mockValidInsert();
        Mockito.when(answerRepository.findByStudentAndQuestion(student.getId(), question.getId())).thenReturn(List.of());
        Answer currentAnswer = new Answer(10L, student, option);
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of(currentAnswer));

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
        mockValidInsert();
        Mockito.when(answerRepository.findByStudentAndQuestion(student.getId(), question.getId())).thenReturn(List.of());
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());

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
