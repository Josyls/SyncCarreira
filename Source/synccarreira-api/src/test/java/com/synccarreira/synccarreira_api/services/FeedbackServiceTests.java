package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.feedback.FeedbackDTO;
import com.synccarreira.synccarreira_api.dto.feedback.JourneyFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.dto.feedback.StudentFeedbackStatusDTO;
import com.synccarreira.synccarreira_api.entities.Appointment;
import com.synccarreira.synccarreira_api.entities.Feedback;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.enums.FeedbackType;
import com.synccarreira.synccarreira_api.entities.enums.ScheduleStatus;
import com.synccarreira.synccarreira_api.projections.PanelStudentProjection;
import com.synccarreira.synccarreira_api.repositories.AppointmentRepository;
import com.synccarreira.synccarreira_api.repositories.FeedbackRepository;
import com.synccarreira.synccarreira_api.repositories.JourneyRecordRepository;
import com.synccarreira.synccarreira_api.repositories.PsychologistRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.events.EmailEvent;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ConflictException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.AppointmentFactory;
import com.synccarreira.synccarreira_api.tests.FeedbackFactory;
import com.synccarreira.synccarreira_api.tests.InstitutionFactory;
import com.synccarreira.synccarreira_api.tests.JourneyFactory;
import com.synccarreira.synccarreira_api.tests.PsychologistFactory;
import com.synccarreira.synccarreira_api.tests.SchoolClassFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class FeedbackServiceTests {

    @InjectMocks
    private FeedbackService service;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PsychologistRepository psychologistRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private JourneyRecordRepository journeyRecordRepository;

    @Mock
    private PsychologistPanelService panelService;

    @Mock
    private AuthService authService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Psychologist psychologist;
    private Student student;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        psychologist = PsychologistFactory.createPsychologist();
        student = StudentFactory.createStudent();
        student.setId(10L);
        SchoolClass schoolClass = SchoolClassFactory.createSchoolClass();
        schoolClass.setInstitution(psychologist.getInstitution());
        student.setDeterminedSchoolClass(schoolClass);

        // Sessão individual que já terminou
        appointment = AppointmentFactory.createAppointment();
        appointment.setPsychologist(psychologist);
        appointment.setDateTime(LocalDateTime.now().minusDays(1));
        appointment.getStudents().clear();
        appointment.getStudents().add(student);
    }

    private void mockLoggedPsychologist() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(psychologistRepository.findById(psychologist.getId())).thenReturn(Optional.of(psychologist));
    }

    private void mockLoggedStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
    }

    private void mockSave() {
        Mockito.when(feedbackRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Feedback saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
    }

    // ---------- Feedback de jornada ----------

    @Test
    void sendJourneyFeedbackShouldSaveFeedbackForLastConcludedJourneyAndNotifyStudent() {
        mockLoggedPsychologist();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId())).thenReturn(List.of(
                JourneyFactory.createJourneyRecord(student, 1, "{}"),
                JourneyFactory.createJourneyRecord(student, 2, "{}")));
        Mockito.when(feedbackRepository.existsByStudentIdAndJourneyNumber(student.getId(), 2)).thenReturn(false);
        mockSave();

        FeedbackDTO result = service.sendJourneyFeedback(FeedbackFactory.createJourneyFeedbackInsertDTO(student.getId()));

        Assertions.assertEquals(FeedbackType.JORNADA, result.type());
        Assertions.assertEquals(2, result.journeyNumber());
        Assertions.assertEquals(FeedbackFactory.CONTENT, result.content());
        Assertions.assertEquals(psychologist.getName(), result.psychologistName());
        Assertions.assertFalse(result.read());

        ArgumentCaptor<EmailEvent> captor = ArgumentCaptor.forClass(EmailEvent.class);
        Mockito.verify(eventPublisher).publishEvent(captor.capture());
        Assertions.assertEquals(student.getEmail(), captor.getValue().to());
        Assertions.assertEquals(EmailService.FEEDBACK_TEMPLATE, captor.getValue().templateName());
        // O e-mail é só um aviso: o texto do feedback não vai para o e-mail
        Assertions.assertFalse(captor.getValue().templateModel().containsValue(FeedbackFactory.CONTENT));
    }

    @Test
    void sendJourneyFeedbackShouldUseRequestedJourneyWhenItWasConcluded() {
        mockLoggedPsychologist();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.existsByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(true);
        Mockito.when(feedbackRepository.existsByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(false);
        mockSave();

        FeedbackDTO result = service.sendJourneyFeedback(new JourneyFeedbackInsertDTO(student.getId(), 1, FeedbackFactory.CONTENT));

        Assertions.assertEquals(1, result.journeyNumber());
    }

    @Test
    void sendJourneyFeedbackShouldThrowBusinessExceptionWhenStudentHasNoConcludedJourney() {
        mockLoggedPsychologist();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId())).thenReturn(List.of());

        Assertions.assertThrows(BusinessException.class,
                () -> service.sendJourneyFeedback(FeedbackFactory.createJourneyFeedbackInsertDTO(student.getId())));
        Mockito.verify(feedbackRepository, Mockito.never()).saveAndFlush(any());
        Mockito.verify(eventPublisher, Mockito.never()).publishEvent(any());
    }

    @Test
    void sendJourneyFeedbackShouldThrowBusinessExceptionWhenRequestedJourneyWasNotConcluded() {
        mockLoggedPsychologist();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.existsByStudentIdAndJourneyNumber(student.getId(), 3)).thenReturn(false);

        Assertions.assertThrows(BusinessException.class,
                () -> service.sendJourneyFeedback(new JourneyFeedbackInsertDTO(student.getId(), 3, FeedbackFactory.CONTENT)));
    }

    @Test
    void sendJourneyFeedbackShouldThrowConflictExceptionWhenJourneyAlreadyHasFeedback() {
        mockLoggedPsychologist();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId()))
                .thenReturn(List.of(JourneyFactory.createJourneyRecord(student, 1, "{}")));
        Mockito.when(feedbackRepository.existsByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(true);

        Assertions.assertThrows(ConflictException.class,
                () -> service.sendJourneyFeedback(FeedbackFactory.createJourneyFeedbackInsertDTO(student.getId())));
        Mockito.verify(feedbackRepository, Mockito.never()).saveAndFlush(any());
    }

    @Test
    void sendJourneyFeedbackShouldThrowConflictExceptionWhenUniqueConstraintIsViolated() {
        mockLoggedPsychologist();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId()))
                .thenReturn(List.of(JourneyFactory.createJourneyRecord(student, 1, "{}")));
        Mockito.when(feedbackRepository.existsByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(false);
        Mockito.when(feedbackRepository.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);

        Assertions.assertThrows(ConflictException.class,
                () -> service.sendJourneyFeedback(FeedbackFactory.createJourneyFeedbackInsertDTO(student.getId())));
        Mockito.verify(eventPublisher, Mockito.never()).publishEvent(any());
    }

    @Test
    void sendJourneyFeedbackShouldThrowForbiddenExceptionWhenStudentIsFromAnotherInstitution() {
        mockLoggedPsychologist();
        Institution otherInstitution = InstitutionFactory.createInstitution();
        otherInstitution.setId(2L);
        student.getDeterminedSchoolClass().setInstitution(otherInstitution);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));

        Assertions.assertThrows(ForbiddenException.class,
                () -> service.sendJourneyFeedback(FeedbackFactory.createJourneyFeedbackInsertDTO(student.getId())));
    }

    @Test
    void sendJourneyFeedbackShouldThrowIllegalStateExceptionWhenContractIsExpired() {
        psychologist.setContractExpirationDate(LocalDate.now().minusDays(1));
        mockLoggedPsychologist();

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.sendJourneyFeedback(FeedbackFactory.createJourneyFeedbackInsertDTO(student.getId())));
        Mockito.verify(studentRepository, Mockito.never()).findById(any());
    }

    @Test
    void sendJourneyFeedbackShouldThrowForbiddenExceptionWhenLoggedUserIsNotAPsychologist() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(psychologistRepository.findById(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ForbiddenException.class,
                () -> service.sendJourneyFeedback(FeedbackFactory.createJourneyFeedbackInsertDTO(student.getId())));
    }

    // ---------- Feedback de sessão ----------

    private void mockValidSession() {
        mockLoggedPsychologist();
        Mockito.when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
    }

    @Test
    void sendSessionFeedbackShouldSaveFeedbackWhenStudentParticipatedInRealizedSession() {
        mockValidSession();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(feedbackRepository.existsByStudentIdAndAppointmentId(student.getId(), appointment.getId())).thenReturn(false);
        mockSave();

        FeedbackDTO result = service.sendSessionFeedback(FeedbackFactory.createSessionFeedbackInsertDTO(appointment.getId(), student.getId()));

        Assertions.assertEquals(FeedbackType.SESSAO, result.type());
        Assertions.assertEquals(appointment.getId(), result.appointmentId());
        Assertions.assertEquals(appointment.getTitle(), result.appointmentTitle());
        Assertions.assertNull(result.journeyNumber());
        Mockito.verify(eventPublisher).publishEvent(any(EmailEvent.class));
    }

    @Test
    void sendSessionFeedbackShouldAcceptSessionMarkedAsRealizada() {
        appointment.setDateTime(LocalDateTime.now().plusDays(1));
        appointment.setScheduleStatus(ScheduleStatus.REALIZADA);
        mockValidSession();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(feedbackRepository.existsByStudentIdAndAppointmentId(student.getId(), appointment.getId())).thenReturn(false);
        mockSave();

        FeedbackDTO result = service.sendSessionFeedback(FeedbackFactory.createSessionFeedbackInsertDTO(appointment.getId(), student.getId()));

        Assertions.assertEquals(FeedbackType.SESSAO, result.type());
    }

    @Test
    void sendSessionFeedbackShouldThrowBusinessExceptionWhenSessionHasNotEndedYet() {
        appointment.setDateTime(LocalDateTime.now().minusMinutes(10));
        mockValidSession();

        Assertions.assertThrows(BusinessException.class,
                () -> service.sendSessionFeedback(FeedbackFactory.createSessionFeedbackInsertDTO(appointment.getId(), student.getId())));
        Mockito.verify(feedbackRepository, Mockito.never()).saveAndFlush(any());
    }

    @Test
    void sendSessionFeedbackShouldThrowBusinessExceptionWhenSessionWasCancelled() {
        appointment.setScheduleStatus(ScheduleStatus.CANCELADA);
        mockValidSession();

        Assertions.assertThrows(BusinessException.class,
                () -> service.sendSessionFeedback(FeedbackFactory.createSessionFeedbackInsertDTO(appointment.getId(), student.getId())));
    }

    @Test
    void sendSessionFeedbackShouldThrowBusinessExceptionWhenStudentDidNotParticipate() {
        Student otherStudent = AppointmentFactory.createSecondStudent();
        otherStudent.setDeterminedSchoolClass(student.getDeterminedSchoolClass());
        mockValidSession();
        Mockito.when(studentRepository.findById(otherStudent.getId())).thenReturn(Optional.of(otherStudent));

        Assertions.assertThrows(BusinessException.class,
                () -> service.sendSessionFeedback(FeedbackFactory.createSessionFeedbackInsertDTO(appointment.getId(), otherStudent.getId())));
    }

    @Test
    void sendSessionFeedbackShouldThrowForbiddenExceptionWhenSessionIsFromAnotherPsychologist() {
        Psychologist otherPsychologist = PsychologistFactory.createPsychologist();
        otherPsychologist.setId(2L);
        appointment.setPsychologist(otherPsychologist);
        mockValidSession();

        Assertions.assertThrows(ForbiddenException.class,
                () -> service.sendSessionFeedback(FeedbackFactory.createSessionFeedbackInsertDTO(appointment.getId(), student.getId())));
    }

    @Test
    void sendSessionFeedbackShouldThrowConflictExceptionWhenStudentAlreadyHasFeedbackForSession() {
        mockValidSession();
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(feedbackRepository.existsByStudentIdAndAppointmentId(student.getId(), appointment.getId())).thenReturn(true);

        Assertions.assertThrows(ConflictException.class,
                () -> service.sendSessionFeedback(FeedbackFactory.createSessionFeedbackInsertDTO(appointment.getId(), student.getId())));
    }

    @Test
    void sendSessionFeedbackShouldThrowResourceNotFoundExceptionWhenSessionDoesNotExist() {
        mockLoggedPsychologist();
        Mockito.when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.sendSessionFeedback(FeedbackFactory.createSessionFeedbackInsertDTO(99L, student.getId())));
    }

    // ---------- Apagar e listar (psicóloga) ----------

    @Test
    void deleteShouldRemoveFeedbackWrittenByLoggedPsychologist() {
        mockLoggedPsychologist();
        Feedback feedback = FeedbackFactory.createJourneyFeedback(student, psychologist, 1);
        Mockito.when(feedbackRepository.findById(feedback.getId())).thenReturn(Optional.of(feedback));

        service.delete(feedback.getId());

        Mockito.verify(feedbackRepository).delete(feedback);
    }

    @Test
    void deleteShouldThrowForbiddenExceptionWhenFeedbackWasWrittenByAnotherPsychologist() {
        mockLoggedPsychologist();
        Psychologist otherPsychologist = PsychologistFactory.createPsychologist();
        otherPsychologist.setId(2L);
        Feedback feedback = FeedbackFactory.createJourneyFeedback(student, otherPsychologist, 1);
        Mockito.when(feedbackRepository.findById(feedback.getId())).thenReturn(Optional.of(feedback));

        Assertions.assertThrows(ForbiddenException.class, () -> service.delete(feedback.getId()));
        Mockito.verify(feedbackRepository, Mockito.never()).delete(any());
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenFeedbackDoesNotExist() {
        mockLoggedPsychologist();
        Mockito.when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }

    @Test
    void findSentFeedbacksShouldReturnFeedbacksOfLoggedPsychologist() {
        mockLoggedPsychologist();
        Mockito.when(feedbackRepository.findByPsychologistIdOrderByCreatedAtDesc(psychologist.getId()))
                .thenReturn(List.of(FeedbackFactory.createJourneyFeedback(student, psychologist, 1)));

        List<FeedbackDTO> result = service.findSentFeedbacks();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(student.getId(), result.getFirst().studentId());
    }

    @Test
    void findFeedbackStatusShouldInformWhichStudentsReceivedFeedback() {
        Long institutionId = psychologist.getInstitution().getId();
        Student otherStudent = AppointmentFactory.createSecondStudent();
        Mockito.when(panelService.findAccessibleInstitutionId(psychologist.getId())).thenReturn(institutionId);
        Mockito.when(studentRepository.searchPanelStudentsByInstitution(institutionId)).thenReturn(List.of(
                new PanelStudentProjection(student.getId(), student.getName(), 1L, "3º A", false, null),
                new PanelStudentProjection(otherStudent.getId(), otherStudent.getName(), 1L, "3º A", false, null)));
        Mockito.when(journeyRecordRepository.findByStudentIdIn(List.of(student.getId(), otherStudent.getId()))).thenReturn(List.of(
                JourneyFactory.createJourneyRecord(student, 1, "{}"),
                JourneyFactory.createJourneyRecord(student, 2, "{}")));
        Mockito.when(feedbackRepository.findByStudentIdIn(List.of(student.getId(), otherStudent.getId()))).thenReturn(List.of(
                FeedbackFactory.createJourneyFeedback(student, psychologist, 2),
                FeedbackFactory.createSessionFeedback(student, psychologist, appointment)));

        List<StudentFeedbackStatusDTO> result = service.findFeedbackStatus(psychologist.getId());

        StudentFeedbackStatusDTO withFeedback = result.get(0);
        Assertions.assertEquals(2, withFeedback.concludedJourneys());
        Assertions.assertEquals(2, withFeedback.lastConcludedJourneyNumber());
        Assertions.assertTrue(withFeedback.journeyFeedbackSent());
        Assertions.assertEquals(1, withFeedback.sessionFeedbacksSent());

        StudentFeedbackStatusDTO withoutJourney = result.get(1);
        Assertions.assertEquals(0, withoutJourney.concludedJourneys());
        Assertions.assertNull(withoutJourney.lastConcludedJourneyNumber());
        Assertions.assertFalse(withoutJourney.journeyFeedbackSent());
    }

    @Test
    void findFeedbackStatusShouldReturnEmptyListWhenInstitutionHasNoStudents() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologist.getId())).thenReturn(1L);
        Mockito.when(studentRepository.searchPanelStudentsByInstitution(1L)).thenReturn(List.of());

        Assertions.assertTrue(service.findFeedbackStatus(psychologist.getId()).isEmpty());
        Mockito.verify(feedbackRepository, Mockito.never()).findByStudentIdIn(any());
    }

    // ---------- Aluno ----------

    @Test
    void findMyFeedbacksShouldReturnFeedbacksOfLoggedStudent() {
        mockLoggedStudent();
        Mockito.when(feedbackRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).thenReturn(List.of(
                FeedbackFactory.createJourneyFeedback(student, psychologist, 1),
                FeedbackFactory.createSessionFeedback(student, psychologist, appointment)));

        List<FeedbackDTO> result = service.findMyFeedbacks();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(FeedbackType.JORNADA, result.get(0).type());
        Assertions.assertEquals(FeedbackType.SESSAO, result.get(1).type());
    }

    @Test
    void markAsReadShouldSetReadDateOnFirstOpening() {
        mockLoggedStudent();
        Feedback feedback = FeedbackFactory.createJourneyFeedback(student, psychologist, 1);
        Mockito.when(feedbackRepository.findByIdAndStudentId(feedback.getId(), student.getId())).thenReturn(Optional.of(feedback));
        Mockito.when(feedbackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackDTO result = service.markAsRead(feedback.getId());

        Assertions.assertTrue(result.read());
        Assertions.assertNotNull(result.readAt());
    }

    @Test
    void markAsReadShouldKeepFirstReadDateWhenOpenedAgain() {
        mockLoggedStudent();
        Instant firstRead = Instant.parse("2026-10-06T10:00:00Z");
        Feedback feedback = FeedbackFactory.createJourneyFeedback(student, psychologist, 1);
        feedback.setReadAt(firstRead);
        Mockito.when(feedbackRepository.findByIdAndStudentId(feedback.getId(), student.getId())).thenReturn(Optional.of(feedback));

        FeedbackDTO result = service.markAsRead(feedback.getId());

        Assertions.assertEquals(firstRead, result.readAt());
        Mockito.verify(feedbackRepository, Mockito.never()).save(any());
    }

    @Test
    void markAsReadShouldThrowResourceNotFoundExceptionWhenFeedbackIsFromAnotherStudent() {
        mockLoggedStudent();
        Mockito.when(feedbackRepository.findByIdAndStudentId(5L, student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(5L));
    }
}
