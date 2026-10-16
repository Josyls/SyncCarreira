package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.SynthesisDTO;
import com.synccarreira.synccarreira_api.dto.SynthesisInsertDTO;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Synthesis;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ConflictException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import com.synccarreira.synccarreira_api.tests.SynthesisFactory;
import com.synccarreira.synccarreira_api.tests.TrailFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class SynthesisServiceTests {

    @InjectMocks
    private SynthesisService service;

    @Mock
    private SynthesisRepository synthesisRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TrailRepository trailRepository;

    @Mock
    private TrailService trailService;

    @Mock
    private AuthService authService;

    @Mock
    private JourneyHistoryService journeyHistoryService;

    private Student student;
    private Trail trail, informationTrail;
    private Long nonExistingTrailId;
    private SynthesisInsertDTO dto, invalidDto;

    @BeforeEach
    void setUp() {
        student = StudentFactory.createStudent();
        trail = TrailFactory.createTrail();
        informationTrail = SynthesisFactory.createInformationTrail();
        nonExistingTrailId = 100L;
        dto = SynthesisFactory.createSynthesisInsertDTO(trail.getId());
        invalidDto = SynthesisFactory.createSynthesisInsertDTO(nonExistingTrailId);
    }

    private void mockLoggedStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
    }

    private void mockTrailReadyForSynthesis(Trail trail) {
        mockLoggedStudent();
        Mockito.when(trailRepository.findById(trail.getId())).thenReturn(Optional.of(trail));
        Mockito.when(trailService.canAccess(trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, student.getId())).thenReturn(true);
    }

    @Test
    void findForLoggedStudentShouldReturnSynthesesOfLoggedStudent() {
        mockLoggedStudent();
        Mockito.when(synthesisRepository.findByStudentId(student.getId()))
                .thenReturn(List.of(SynthesisFactory.createSynthesis(trail), SynthesisFactory.createSynthesis(informationTrail)));

        List<SynthesisDTO> result = service.findForLoggedStudent();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(TrailName.AUTOCONHECIMENTO, result.get(0).trailName());
        Assertions.assertFalse(result.get(0).journeySynthesis());
        Assertions.assertTrue(result.get(1).journeySynthesis());
    }

    @Test
    void insertShouldSaveSynthesisWhenTrailQuestionsAreAllAnswered() {
        mockTrailReadyForSynthesis(trail);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(student.getId(), trail.getId())).thenReturn(false);
        Mockito.when(synthesisRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Synthesis saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        SynthesisDTO result = service.insert(new SynthesisInsertDTO(trail.getId(), "  " + SynthesisFactory.CONTENT + "  "));

        Assertions.assertEquals(1L, result.id());
        Assertions.assertEquals(SynthesisFactory.CONTENT, result.content());
        Assertions.assertEquals(trail.getId(), result.trailId());
        Assertions.assertFalse(result.journeySynthesis());
        Assertions.assertNotNull(result.createdAt());
    }

    @Test
    void insertShouldSaveJourneySynthesisWhenTrailIsInformationTrail() {
        mockTrailReadyForSynthesis(informationTrail);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(student.getId(), informationTrail.getId())).thenReturn(false);
        Mockito.when(synthesisRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SynthesisDTO result = service.insert(SynthesisFactory.createSynthesisInsertDTO(informationTrail.getId()));

        Assertions.assertEquals(TrailName.INFORMACAO, result.trailName());
        Assertions.assertTrue(result.journeySynthesis());
    }

    @Test
    void insertShouldAskJourneyHistoryToRegisterJourneyWhenSynthesisIsSaved() {
        mockTrailReadyForSynthesis(informationTrail);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(student.getId(), informationTrail.getId())).thenReturn(false);
        Mockito.when(synthesisRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.insert(SynthesisFactory.createSynthesisInsertDTO(informationTrail.getId()));

        // RF-15: o histórico verifica se esta síntese concluiu a jornada
        Mockito.verify(journeyHistoryService).registerJourneyIfConcluded(student);
    }

    @Test
    void insertShouldThrowForbiddenExceptionWhenTrailIsNotUnlocked() {
        mockLoggedStudent();
        Mockito.when(trailRepository.findById(trail.getId())).thenReturn(Optional.of(trail));
        Mockito.when(trailService.canAccess(trail.getId())).thenReturn(false);

        Assertions.assertThrows(ForbiddenException.class,
                () -> service.insert(dto));
        Mockito.verify(synthesisRepository, Mockito.never()).saveAndFlush(any());
    }

    @Test
    void insertShouldThrowBusinessExceptionWhenTrailHasUnansweredQuestions() {
        mockLoggedStudent();
        Mockito.when(trailRepository.findById(trail.getId())).thenReturn(Optional.of(trail));
        Mockito.when(trailService.canAccess(trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, student.getId())).thenReturn(false);

        Assertions.assertThrows(BusinessException.class,
                () -> service.insert(dto));
        Mockito.verify(synthesisRepository, Mockito.never()).saveAndFlush(any());
    }

    @Test
    void insertShouldThrowConflictExceptionWhenSynthesisWasAlreadySubmitted() {
        mockTrailReadyForSynthesis(trail);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(student.getId(), trail.getId())).thenReturn(true);

        Assertions.assertThrows(ConflictException.class,
                () -> service.insert(dto));
        Mockito.verify(synthesisRepository, Mockito.never()).saveAndFlush(any());
        Mockito.verify(journeyHistoryService, Mockito.never()).registerJourneyIfConcluded(any());
    }

    @Test
    void insertShouldThrowConflictExceptionWhenUniqueConstraintIsViolated() {
        mockTrailReadyForSynthesis(trail);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(student.getId(), trail.getId())).thenReturn(false);
        Mockito.when(synthesisRepository.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);

        Assertions.assertThrows(ConflictException.class,
                () -> service.insert(dto));
    }

    @Test
    void insertShouldThrowResourceNotFoundExceptionWhenTrailDoesNotExist() {
        mockLoggedStudent();
        Mockito.when(trailRepository.findById(nonExistingTrailId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.insert(invalidDto));
    }

    @Test
    void insertShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.insert(dto));
    }
}
