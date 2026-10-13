package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.QuestionDTO;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.repositories.QuestionRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.QuestionFactory;
import com.synccarreira.synccarreira_api.tests.TrailFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class QuestionServiceTests {

    @InjectMocks
    private QuestionService service;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private TrailRepository trailRepository;

    @Mock
    private TrailService trailService;

    private Long existingQuestionId, nonExistingQuestionId, existingTrailId, nonExistingTrailId;
    private Trail trail;
    private Question question, question1, openQuestion;
    private QuestionDTO questionDTO, openQuestionDTO, emptyOptionsQuestionDTO, nonExistingTrailQuestionDTO;
    private List<Question> questionList;

    @BeforeEach
    void setUp() {
        existingQuestionId = 1L;
        nonExistingQuestionId = 100L;
        existingTrailId = 1L;
        nonExistingTrailId = 100L;

        trail = TrailFactory.createTrail();
        question = QuestionFactory.createQuestion();
        question1 = QuestionFactory.createCheckboxQuestion();
        openQuestion = QuestionFactory.createOpenQuestion();
        questionDTO = QuestionFactory.createQuestionDTO();
        openQuestionDTO = QuestionFactory.createOpenQuestionDTO();
        emptyOptionsQuestionDTO = QuestionFactory.createEmptyOptionsQuestionDTO();
        nonExistingTrailQuestionDTO = QuestionFactory.createNonExistingTrailQuestionDTO();

        questionList = new ArrayList<>();
        questionList.add(question);
        questionList.add(question1);
    }

    @Test
    void findAllShouldReturnQuestionDTOList() {
        Mockito.when(questionRepository.findAll()).thenReturn(questionList);

        List<QuestionDTO> result = service.findAll();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(question.getId(), result.getFirst().id());
        Assertions.assertEquals(question.getContent(), result.getFirst().content());
        Assertions.assertEquals(question.getQuestionType(), result.getFirst().questionType());
        Assertions.assertEquals(question1.getId(), result.getLast().id());
        Assertions.assertEquals(question1.getContent(), result.getLast().content());
        Assertions.assertEquals(question1.getQuestionType(), result.getLast().questionType());
    }

    @Test
    void findByIdShouldReturnQuestionDTOWhenIdExists() {
        Mockito.when(questionRepository.findById(existingQuestionId)).thenReturn(Optional.of(question));

        QuestionDTO result = service.findById(existingQuestionId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(existingQuestionId, result.id());
        Assertions.assertEquals(question.getContent(), result.content());
        Assertions.assertEquals(question.getQuestionType(), result.questionType());
        Assertions.assertEquals(trail.getId(), result.trailId());
        Assertions.assertEquals(question.getOptions().size(), result.options().size());
    }

    @Test
    void findByIdShouldReturnEntityNotFoundExceptionWhenIdDoesNotExist() {
        Mockito.when(questionRepository.findById(nonExistingQuestionId)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> {
            service.findById(nonExistingQuestionId);
        });
    }

    @Test
    void findByTrailShouldReturnQuestionDTOListWhenTrailCanBeAccessed() {
        Mockito.when(trailService.canAccess(existingTrailId)).thenReturn(true);
        Mockito.when(questionRepository.findByTrailId(existingTrailId)).thenReturn(questionList);

        List<QuestionDTO> result = service.findByTrail(existingTrailId);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(question.getId(), result.getFirst().id());
        Assertions.assertEquals(trail.getId(), result.getFirst().trailId());
        Assertions.assertEquals(question1.getId(), result.getLast().id());
        Assertions.assertEquals(trail.getId(), result.getLast().trailId());
    }

    @Test
    void findByTrailShouldReturnBusinessExceptionWhenTrailCannotBeAccessed() {
        Mockito.when(trailService.canAccess(existingTrailId)).thenReturn(false);

        Assertions.assertThrows(BusinessException.class, () -> {
            service.findByTrail(existingTrailId);
        });
    }

    @Test
    void createQuestionShouldReturnQuestionDTOWhenQuestionDTOIsValid() {
        Mockito.when(trailRepository.findById(existingTrailId)).thenReturn(Optional.of(trail));
        Mockito.when(questionRepository.countByTrailId(existingTrailId)).thenReturn(0L);
        Mockito.when(questionRepository.save(any())).thenReturn(question);

        QuestionDTO result = service.createQuestion(questionDTO);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(question.getId(), result.id());
        Assertions.assertEquals(question.getContent(), result.content());
        Assertions.assertEquals(question.getQuestionType(), result.questionType());
        Assertions.assertEquals(trail.getId(), result.trailId());
        Assertions.assertEquals(question.getOptions().size(), result.options().size());
    }

    @Test
    void createQuestionShouldReturnQuestionDTOWhenQuestionTypeIsAbertaAndHasNoOptions() {
        Mockito.when(trailRepository.findById(existingTrailId)).thenReturn(Optional.of(trail));
        Mockito.when(questionRepository.countByTrailId(existingTrailId)).thenReturn(0L);
        Mockito.when(questionRepository.save(any())).thenReturn(openQuestion);

        QuestionDTO result = service.createQuestion(openQuestionDTO);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(openQuestion.getId(), result.id());
        Assertions.assertEquals(QuestionType.ABERTA, result.questionType());
        Assertions.assertTrue(result.options().isEmpty());
    }

    @Test
    void createQuestionShouldReturnEntityNotFoundExceptionWhenTrailIdDoesNotExist() {
        Mockito.when(trailRepository.findById(nonExistingTrailId)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> {
            service.createQuestion(nonExistingTrailQuestionDTO);
        });
    }

    @Test
    void createQuestionShouldReturnIllegalStateExceptionWhenTrailReachedQuestionLimit() {
        Mockito.when(trailRepository.findById(existingTrailId)).thenReturn(Optional.of(trail));
        Mockito.when(questionRepository.countByTrailId(existingTrailId)).thenReturn(10L);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            service.createQuestion(questionDTO);
        });
    }

    @Test
    void createQuestionShouldReturnIllegalArgumentExceptionWhenQuestionAcceptsOptionsAndOptionsAreEmpty() {
        Mockito.when(trailRepository.findById(existingTrailId)).thenReturn(Optional.of(trail));
        Mockito.when(questionRepository.countByTrailId(existingTrailId)).thenReturn(0L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.createQuestion(emptyOptionsQuestionDTO);
        });
    }

    @Test
    void updateQuestionShouldReturnQuestionDTOWhenIdExists() {
        Mockito.when(questionRepository.findById(existingQuestionId)).thenReturn(Optional.of(question));
        Mockito.when(trailRepository.findById(existingTrailId)).thenReturn(Optional.of(trail));
        Mockito.when(questionRepository.save(any())).thenReturn(question);

        QuestionDTO result = service.updateQuestion(existingQuestionId, questionDTO);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(question.getId(), result.id());
        Assertions.assertEquals(questionDTO.content(), result.content());
        Assertions.assertEquals(questionDTO.questionType(), result.questionType());
        Assertions.assertEquals(questionDTO.trailId(), result.trailId());
        Assertions.assertEquals(questionDTO.options().size(), result.options().size());
    }

    @Test
    void updateQuestionShouldReturnEntityNotFoundExceptionWhenIdDoesNotExist() {
        Mockito.when(questionRepository.findById(nonExistingQuestionId)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> {
            service.updateQuestion(nonExistingQuestionId, questionDTO);
        });
    }

    @Test
    void updateQuestionShouldReturnEntityNotFoundExceptionWhenTrailIdDoesNotExist() {
        Mockito.when(questionRepository.findById(existingQuestionId)).thenReturn(Optional.of(question));
        Mockito.when(trailRepository.findById(nonExistingTrailId)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> {
            service.updateQuestion(existingQuestionId, nonExistingTrailQuestionDTO);
        });
    }

    @Test
    void updateQuestionShouldReturnIllegalArgumentExceptionWhenQuestionAcceptsOptionsAndOptionsAreEmpty() {
        Mockito.when(questionRepository.findById(existingQuestionId)).thenReturn(Optional.of(question));
        Mockito.when(trailRepository.findById(existingTrailId)).thenReturn(Optional.of(trail));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.updateQuestion(existingQuestionId, emptyOptionsQuestionDTO);
        });
    }

    @Test
    void deleteQuestionByIdShouldDoNothingWhenIdExists() {
        Mockito.when(questionRepository.existsById(existingQuestionId)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> {
            service.deleteQuestionById(existingQuestionId);
        });

        Mockito.verify(questionRepository).deleteById(existingQuestionId);
    }

    @Test
    void deleteQuestionByIdShouldReturnResourceNotFoundExceptionWhenIdDoesNotExist() {
        Mockito.when(questionRepository.existsById(nonExistingQuestionId)).thenReturn(false);

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteQuestionById(nonExistingQuestionId);
        });
    }
}
