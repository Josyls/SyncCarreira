package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.JourneyDoubtDTO;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.repositories.PsychologistRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.events.EmailEvent;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ConflictException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.InstitutionFactory;
import com.synccarreira.synccarreira_api.tests.PsychologistFactory;
import com.synccarreira.synccarreira_api.tests.SchoolClassFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import com.synccarreira.synccarreira_api.tests.TrailFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class JourneyServiceTests {

    @InjectMocks
    private JourneyService service;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TrailRepository trailRepository;

    @Mock
    private SynthesisRepository synthesisRepository;

    @Mock
    private PsychologistRepository psychologistRepository;

    @Mock
    private TrailService trailService;

    @Mock
    private AuthService authService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Student student, studentInDoubt;
    private SchoolClass schoolClass;
    private Institution institution;
    private Psychologist psychologist, expiredContractPsychologist;
    private Trail trail;
    private Long existingStudentId, existingInstitutionId;
    private Instant doubtFlaggedAt;
    private List<Trail> trailList, emptyTrailList;
    private List<Psychologist> psychologistList, expiredContractPsychologistList;

    @BeforeEach
    void setUp() {
        existingStudentId = 1L;
        existingInstitutionId = 1L;
        doubtFlaggedAt = Instant.parse("2026-10-01T13:30:00Z");

        institution = InstitutionFactory.createInstitution();
        schoolClass = SchoolClassFactory.createSchoolClass();
        schoolClass.setInstitution(institution);

        student = StudentFactory.createStudent();
        student.setDeterminedSchoolClass(schoolClass);

        studentInDoubt = StudentFactory.createStudent();
        studentInDoubt.setDeterminedSchoolClass(schoolClass);
        studentInDoubt.setInDoubt(true);
        studentInDoubt.setDoubtFlaggedAt(doubtFlaggedAt);

        psychologist = PsychologistFactory.createPsychologist();
        expiredContractPsychologist = PsychologistFactory.createExpiredContractPsychologist();
        expiredContractPsychologist.setEmail("psicologa.vencida@gmail.com");

        trail = TrailFactory.createTrail();

        emptyTrailList = new ArrayList<>();
        trailList = new ArrayList<>();
        trailList.add(trail);

        psychologistList = new ArrayList<>();
        psychologistList.add(psychologist);
        psychologistList.add(expiredContractPsychologist);

        expiredContractPsychologistList = new ArrayList<>();
        expiredContractPsychologistList.add(expiredContractPsychologist);
    }

    @Test
    void findDoubtShouldReturnJourneyDoubtDTONotInDoubtWhenStudentHasNotFlaggedDoubt() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));

        JourneyDoubtDTO result = service.findDoubt();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(student.getId(), result.studentId());
        Assertions.assertFalse(result.inDoubt());
        Assertions.assertNull(result.doubtFlaggedAt());
    }

    @Test
    void findDoubtShouldReturnJourneyDoubtDTOInDoubtWhenStudentHasFlaggedDoubt() {
        Mockito.when(authService.authenticated()).thenReturn(studentInDoubt);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(studentInDoubt));

        JourneyDoubtDTO result = service.findDoubt();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(studentInDoubt.getId(), result.studentId());
        Assertions.assertTrue(result.inDoubt());
        Assertions.assertEquals(doubtFlaggedAt, result.doubtFlaggedAt());
    }

    @Test
    void findDoubtShouldReturnJourneyDoubtDTONotInDoubtWhenInDoubtIsNull() {
        student.setInDoubt(null);
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));

        JourneyDoubtDTO result = service.findDoubt();

        Assertions.assertFalse(result.inDoubt());
    }

    @Test
    void findDoubtShouldReturnResourceNotFoundExceptionWhenStudentDoesNotExist() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.findDoubt();
        });
    }

    @Test
    void flagDoubtShouldReturnJourneyDoubtDTOAndNotifyPsychologistWhenJourneyIsConcluded() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, existingStudentId)).thenReturn(true);
        Mockito.when(studentRepository.flagDoubt(Mockito.eq(existingStudentId), any())).thenReturn(1);
        Mockito.when(psychologistRepository.findByInstitutionId(existingInstitutionId)).thenReturn(List.of(psychologist));

        JourneyDoubtDTO result = service.flagDoubt();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(student.getId(), result.studentId());
        Assertions.assertTrue(result.inDoubt());
        Assertions.assertNotNull(result.doubtFlaggedAt());

        ArgumentCaptor<EmailEvent> captor = ArgumentCaptor.forClass(EmailEvent.class);
        Mockito.verify(eventPublisher).publishEvent(captor.capture());
        EmailEvent event = captor.getValue();
        Assertions.assertEquals(psychologist.getEmail(), event.to());
        Assertions.assertEquals(EmailService.JOURNEY_DOUBT_TEMPLATE, event.templateName());
        Assertions.assertEquals(psychologist.getName(), event.templateModel().get("recipientName"));
        Assertions.assertEquals(student.getName(), event.templateModel().get("studentName"));
        Assertions.assertEquals(student.getEmail(), event.templateModel().get("studentEmail"));
        Assertions.assertEquals(schoolClass.getName(), event.templateModel().get("schoolClassName"));
        Assertions.assertEquals(institution.getTradeName(), event.templateModel().get("institutionName"));
        Assertions.assertNotNull(event.templateModel().get("flaggedAt"));
    }

    @Test
    void flagDoubtShouldNotifyOnlyPsychologistsWithValidContract() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, existingStudentId)).thenReturn(true);
        Mockito.when(studentRepository.flagDoubt(Mockito.eq(existingStudentId), any())).thenReturn(1);
        Mockito.when(psychologistRepository.findByInstitutionId(existingInstitutionId)).thenReturn(psychologistList);

        service.flagDoubt();

        ArgumentCaptor<EmailEvent> captor = ArgumentCaptor.forClass(EmailEvent.class);
        Mockito.verify(eventPublisher, Mockito.times(1)).publishEvent(captor.capture());
        Assertions.assertEquals(psychologist.getEmail(), captor.getValue().to());
    }

    @Test
    void flagDoubtShouldUseLegalNameWhenInstitutionTradeNameIsBlank() {
        institution.setTradeName("  ");
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, existingStudentId)).thenReturn(true);
        Mockito.when(studentRepository.flagDoubt(Mockito.eq(existingStudentId), any())).thenReturn(1);
        Mockito.when(psychologistRepository.findByInstitutionId(existingInstitutionId)).thenReturn(List.of(psychologist));

        service.flagDoubt();

        ArgumentCaptor<EmailEvent> captor = ArgumentCaptor.forClass(EmailEvent.class);
        Mockito.verify(eventPublisher).publishEvent(captor.capture());
        Assertions.assertEquals(institution.getLegalName(), captor.getValue().templateModel().get("institutionName"));
    }

    @Test
    void flagDoubtShouldNotNotifyWhenSchoolClassHasNoInstitution() {
        schoolClass.setInstitution(null);
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, existingStudentId)).thenReturn(true);
        Mockito.when(studentRepository.flagDoubt(Mockito.eq(existingStudentId), any())).thenReturn(1);

        JourneyDoubtDTO result = service.flagDoubt();

        Assertions.assertTrue(result.inDoubt());
        Mockito.verify(psychologistRepository, Mockito.never()).findByInstitutionId(any());
        Mockito.verify(eventPublisher, Mockito.never()).publishEvent(any(Object.class));
    }

    @Test
    void flagDoubtShouldNotNotifyWhenStudentHasNoSchoolClass() {
        student.setDeterminedSchoolClass(null);
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, existingStudentId)).thenReturn(true);
        Mockito.when(studentRepository.flagDoubt(Mockito.eq(existingStudentId), any())).thenReturn(1);

        JourneyDoubtDTO result = service.flagDoubt();

        Assertions.assertTrue(result.inDoubt());
        Mockito.verify(psychologistRepository, Mockito.never()).findByInstitutionId(any());
        Mockito.verify(eventPublisher, Mockito.never()).publishEvent(any(Object.class));
    }

    @Test
    void flagDoubtShouldNotNotifyWhenInstitutionHasNoPsychologistWithValidContract() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, existingStudentId)).thenReturn(true);
        Mockito.when(studentRepository.flagDoubt(Mockito.eq(existingStudentId), any())).thenReturn(1);
        Mockito.when(psychologistRepository.findByInstitutionId(existingInstitutionId)).thenReturn(expiredContractPsychologistList);

        JourneyDoubtDTO result = service.flagDoubt();

        Assertions.assertTrue(result.inDoubt());
        Mockito.verify(eventPublisher, Mockito.never()).publishEvent(any(Object.class));
    }

    @Test
    void flagDoubtShouldReturnConflictExceptionWhenStudentIsAlreadyInDoubt() {
        Mockito.when(authService.authenticated()).thenReturn(studentInDoubt);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(studentInDoubt));

        Assertions.assertThrows(ConflictException.class, () -> {
            service.flagDoubt();
        });
        Mockito.verify(studentRepository, Mockito.never()).flagDoubt(any(), any());
        Mockito.verify(eventPublisher, Mockito.never()).publishEvent(any(Object.class));
    }

    @Test
    void flagDoubtShouldReturnBusinessExceptionWhenThereAreNoTrails() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(emptyTrailList);

        Assertions.assertThrows(BusinessException.class, () -> {
            service.flagDoubt();
        });
        Mockito.verify(studentRepository, Mockito.never()).flagDoubt(any(), any());
    }

    @Test
    void flagDoubtShouldReturnBusinessExceptionWhenSynthesisWasNotSubmitted() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(false);

        Assertions.assertThrows(BusinessException.class, () -> {
            service.flagDoubt();
        });
        Mockito.verify(studentRepository, Mockito.never()).flagDoubt(any(), any());
    }

    @Test
    void flagDoubtShouldReturnBusinessExceptionWhenNotAllQuestionsWereAnswered() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, existingStudentId)).thenReturn(false);

        Assertions.assertThrows(BusinessException.class, () -> {
            service.flagDoubt();
        });
        Mockito.verify(studentRepository, Mockito.never()).flagDoubt(any(), any());
    }

    @Test
    void flagDoubtShouldReturnConflictExceptionWhenConcurrentRequestFlaggedDoubtFirst() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findAll()).thenReturn(trailList);
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(existingStudentId, trail.getId())).thenReturn(true);
        Mockito.when(trailService.areAllQuestionsAnswered(trail, existingStudentId)).thenReturn(true);
        Mockito.when(studentRepository.flagDoubt(Mockito.eq(existingStudentId), any())).thenReturn(0);

        Assertions.assertThrows(ConflictException.class, () -> {
            service.flagDoubt();
        });
        Mockito.verify(eventPublisher, Mockito.never()).publishEvent(any(Object.class));
    }

    @Test
    void flagDoubtShouldReturnResourceNotFoundExceptionWhenStudentDoesNotExist() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(existingStudentId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.flagDoubt();
        });
    }
}
