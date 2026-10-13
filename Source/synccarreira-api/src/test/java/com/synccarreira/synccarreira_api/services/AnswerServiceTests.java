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
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.QuestionFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(answerRepository.save(any())).thenAnswer(invocation -> {
            Answer saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
    }

    @Test
    void insertShouldDeletePreviousAnswersOfSameQuestionBeforeSavingNewAnswer() {
        mockValidInsert();
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());

        AnswerDTO result = service.insert(dto);

        Assertions.assertEquals(10L, result.getId());
        // O conteúdo da resposta vem do texto da opção escolhida
        Assertions.assertEquals(option.getOptionText(), result.getContent());
        InOrder inOrder = Mockito.inOrder(answerRepository);
        inOrder.verify(answerRepository).deleteByStudentAndQuestion(student.getId(), question.getId());
        inOrder.verify(answerRepository).save(any());
    }

    @Test
    void insertShouldRecalculateScoreUsingOnlyCurrentAnswers() {
        mockValidInsert();
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
    void insertShouldThrowResourceNotFoundExceptionWhenQuestionOptionDoesNotExist() {
        Mockito.when(questionOptionRepository.findById(nonExistingId)).thenReturn(Optional.empty());
        AnswerInsertDTO invalidDto = new AnswerInsertDTO(nonExistingId);

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.insert(invalidDto));
        Mockito.verify(answerRepository, Mockito.never()).deleteByStudentAndQuestion(anyLong(), anyLong());
        Mockito.verify(answerRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldSaveAnswerForLoggedStudent() {
        mockValidInsert();
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());

        AnswerDTO result = service.insert(dto);

        Assertions.assertEquals(student.getId(), result.getStudent().getId());
        Mockito.verify(answerRepository).deleteByStudentAndQuestion(student.getId(), question.getId());
    }

    @Test
    void insertShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(questionOptionRepository.findById(option.getId())).thenReturn(Optional.of(option));
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

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
