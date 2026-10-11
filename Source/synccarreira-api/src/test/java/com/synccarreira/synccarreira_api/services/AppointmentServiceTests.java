package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.AppointmentCancelDTO;
import com.synccarreira.synccarreira_api.dto.AppointmentDTO;
import com.synccarreira.synccarreira_api.dto.AppointmentFeedbackDTO;
import com.synccarreira.synccarreira_api.dto.AppointmentInsertDTO;
import com.synccarreira.synccarreira_api.entities.Appointment;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.Role;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.entities.enums.ScheduleStatus;
import com.synccarreira.synccarreira_api.entities.enums.ScheduleType;
import com.synccarreira.synccarreira_api.repositories.AppointmentRepository;
import com.synccarreira.synccarreira_api.repositories.PsychologistRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.AppointmentFactory;
import com.synccarreira.synccarreira_api.tests.InstitutionFactory;
import com.synccarreira.synccarreira_api.tests.PsychologistFactory;
import com.synccarreira.synccarreira_api.tests.SchoolClassFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTests {

    @InjectMocks
    private AppointmentService service;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PsychologistRepository psychologistRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PsychologistService psychologistService;

    @Mock
    private AuthService authService;

    private Appointment appointment;
    private Psychologist psychologist, otherPsychologist;
    private User admin;
    private Student student, secondStudent;
    private Long existingAppointmentId, nonExistingAppointmentId, psychologistId, studentId;

    @BeforeEach
    void setUp() {
        appointment = AppointmentFactory.createAppointment();
        psychologist = PsychologistFactory.createPsychologist();
        psychologistId = psychologist.getId();
        otherPsychologist = PsychologistFactory.createPsychologist();
        otherPsychologist.setId(2L);
        admin = new User();
        admin.setId(99L);
        admin.addRole(new Role(2L, "ROLE_ADMIN"));
        student = StudentFactory.createStudent();
        studentId = student.getId();
        secondStudent = AppointmentFactory.createSecondStudent();
        // Os alunos estudam na mesma instituição da psicóloga (RNF-08)
        SchoolClass schoolClass = SchoolClassFactory.createSchoolClass();
        schoolClass.setInstitution(psychologist.getInstitution());
        student.setDeterminedSchoolClass(schoolClass);
        secondStudent.setDeterminedSchoolClass(schoolClass);
        existingAppointmentId = 1L;
        nonExistingAppointmentId = 100L;
    }

    @Test
    void findByPsychologistShouldReturnAppointmentDTOListWhenLoggedUserIsTheSamePsychologist() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(appointmentRepository.findByPsychologistIdOrderByDateTimeAsc(psychologistId))
                .thenReturn(List.of(appointment));

        List<AppointmentDTO> result = service.findByPsychologist(psychologistId);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(appointment.getId(), result.getFirst().id());
        Assertions.assertEquals(psychologist.getId(), result.getFirst().psychologist().id());
        Assertions.assertEquals(student.getEmail(), result.getFirst().students().getFirst().email());
    }

    @Test
    void findByStudentShouldReturnAppointmentDTOListWhenLoggedUserIsTheSameStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(appointmentRepository.findByStudentId(studentId)).thenReturn(List.of(appointment));

        List<AppointmentDTO> result = service.findByStudent(studentId);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(appointment.getTitle(), result.getFirst().title());
    }

    @Test
    void findByPsychologistShouldThrowForbiddenExceptionWhenLoggedUserIsAnotherPsychologist() {
        Mockito.when(authService.authenticated()).thenReturn(otherPsychologist);

        Assertions.assertThrows(ForbiddenException.class, () -> service.findByPsychologist(psychologistId));
        Mockito.verify(appointmentRepository, Mockito.never()).findByPsychologistIdOrderByDateTimeAsc(any());
    }

    @Test
    void findByStudentShouldThrowForbiddenExceptionWhenLoggedUserIsAnotherStudent() {
        Mockito.when(authService.authenticated()).thenReturn(secondStudent);

        Assertions.assertThrows(ForbiddenException.class, () -> service.findByStudent(studentId));
        Mockito.verify(appointmentRepository, Mockito.never()).findByStudentId(any());
    }

    @Test
    void findByPsychologistShouldReturnAppointmentDTOListWhenLoggedUserIsAdmin() {
        Mockito.when(authService.authenticated()).thenReturn(admin);
        Mockito.when(appointmentRepository.findByPsychologistIdOrderByDateTimeAsc(psychologistId))
                .thenReturn(List.of(appointment));

        List<AppointmentDTO> result = service.findByPsychologist(psychologist.getId());

        Assertions.assertEquals(1, result.size());
    }

    @Test
    void findByStudentShouldReturnAppointmentDTOListWhenLoggedUserIsAdmin() {
        Mockito.when(authService.authenticated()).thenReturn(admin);
        Mockito.when(appointmentRepository.findByStudentId(student.getId())).thenReturn(List.of(appointment));

        List<AppointmentDTO> result = service.findByStudent(student.getId());

        Assertions.assertEquals(1, result.size());
    }

    @Test
    void insertShouldReturnAppointmentDTOWithStatusAgendadaWhenIndividualDataIsValid() {
        AppointmentInsertDTO dto = AppointmentFactory.createIndividualAppointmentInsertDTO();
        Mockito.when(psychologistRepository.findById(dto.psychologistId())).thenReturn(Optional.of(psychologist));
        Mockito.when(studentRepository.findAllById(Set.of(1L))).thenReturn(List.of(student));
        Mockito.when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDTO result = service.insert(dto);

        Assertions.assertEquals(ScheduleStatus.AGENDADA, result.scheduleStatus());
        Assertions.assertEquals(ScheduleType.INDIVIDUAL, result.scheduleType());
        Assertions.assertEquals(dto.title(), result.title());
        Assertions.assertEquals(dto.meetLink(), result.meetLink());
        Assertions.assertEquals(1, result.students().size());
        Mockito.verify(psychologistService).validateIfContractIsActive(dto.psychologistId());
    }

    @Test
    void insertShouldReturnAppointmentDTOWhenGroupDataIsValid() {
        AppointmentInsertDTO dto = AppointmentFactory.createGroupAppointmentInsertDTO();
        Mockito.when(psychologistRepository.findById(dto.psychologistId())).thenReturn(Optional.of(psychologist));
        Mockito.when(studentRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(student, secondStudent));
        Mockito.when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDTO result = service.insert(dto);

        Assertions.assertEquals(ScheduleType.GRUPO, result.scheduleType());
        Assertions.assertEquals(2, result.students().size());
    }

    @Test
    void insertShouldThrowForbiddenExceptionWhenStudentIsFromAnotherInstitution() {
        AppointmentInsertDTO dto = AppointmentFactory.createGroupAppointmentInsertDTO();
        SchoolClass otherSchoolClass = SchoolClassFactory.createSchoolClass();
        otherSchoolClass.setId(2L);
        Institution otherInstitution = InstitutionFactory.createInstitution();
        otherInstitution.setId(2L);
        otherSchoolClass.setInstitution(otherInstitution);
        secondStudent.setDeterminedSchoolClass(otherSchoolClass);
        Mockito.when(psychologistRepository.findById(dto.psychologistId())).thenReturn(Optional.of(psychologist));
        Mockito.when(studentRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(student, secondStudent));

        Assertions.assertThrows(ForbiddenException.class, () -> service.insert(dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldThrowForbiddenExceptionWhenStudentHasNoSchoolClass() {
        AppointmentInsertDTO dto = AppointmentFactory.createIndividualAppointmentInsertDTO();
        student.setDeterminedSchoolClass(null);
        Mockito.when(psychologistRepository.findById(dto.psychologistId())).thenReturn(Optional.of(psychologist));
        Mockito.when(studentRepository.findAllById(Set.of(1L))).thenReturn(List.of(student));

        Assertions.assertThrows(ForbiddenException.class, () -> service.insert(dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldThrowBusinessExceptionWhenPsychologistHasNoInstitution() {
        AppointmentInsertDTO dto = AppointmentFactory.createIndividualAppointmentInsertDTO();
        psychologist.setInstitution(null);
        Mockito.when(psychologistRepository.findById(dto.psychologistId())).thenReturn(Optional.of(psychologist));
        Mockito.when(studentRepository.findAllById(Set.of(1L))).thenReturn(List.of(student));

        Assertions.assertThrows(BusinessException.class, () -> service.insert(dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldThrowBusinessExceptionWhenIndividualHasMoreThanOneStudent() {
        AppointmentInsertDTO dto = AppointmentFactory.createAppointmentInsertDTO(ScheduleType.INDIVIDUAL, List.of(1L, 2L));

        Assertions.assertThrows(BusinessException.class, () -> service.insert(dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldThrowBusinessExceptionWhenGroupHasOnlyOneStudent() {
        AppointmentInsertDTO dto = AppointmentFactory.createAppointmentInsertDTO(ScheduleType.GRUPO, List.of(1L, 1L));

        Assertions.assertThrows(BusinessException.class, () -> service.insert(dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldThrowIllegalStateExceptionWhenContractIsExpired() {
        AppointmentInsertDTO dto = AppointmentFactory.createIndividualAppointmentInsertDTO();
        Mockito.doThrow(IllegalStateException.class).when(psychologistService).validateIfContractIsActive(dto.psychologistId());

        Assertions.assertThrows(IllegalStateException.class, () -> service.insert(dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void insertShouldThrowResourceNotFoundExceptionWhenStudentDoesNotExist() {
        AppointmentInsertDTO dto = AppointmentFactory.createGroupAppointmentInsertDTO();
        Mockito.when(psychologistRepository.findById(dto.psychologistId())).thenReturn(Optional.of(psychologist));
        Mockito.when(studentRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(student));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.insert(dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void updateShouldReturnAppointmentDTOWhenStatusIsAgendada() {
        AppointmentInsertDTO dto = AppointmentFactory.createGroupAppointmentInsertDTO();
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(appointment));
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(psychologistRepository.findById(dto.psychologistId())).thenReturn(Optional.of(psychologist));
        Mockito.when(studentRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(student, secondStudent));
        Mockito.when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDTO result = service.update(existingAppointmentId, dto);

        Assertions.assertEquals(ScheduleType.GRUPO, result.scheduleType());
        Assertions.assertEquals(2, result.students().size());
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
        AppointmentInsertDTO dto = AppointmentFactory.createIndividualAppointmentInsertDTO();
        Mockito.when(appointmentRepository.findById(nonExistingAppointmentId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.update(nonExistingAppointmentId, dto));
    }

    @Test
    void updateShouldThrowIllegalStateExceptionWhenAppointmentIsCancelled() {
        Appointment cancelled = AppointmentFactory.createAppointmentWithStatus(ScheduleStatus.CANCELADA);
        AppointmentInsertDTO dto = AppointmentFactory.createIndividualAppointmentInsertDTO();
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(cancelled));
        Mockito.when(authService.authenticated()).thenReturn(psychologist);

        Assertions.assertThrows(IllegalStateException.class, () -> service.update(existingAppointmentId, dto));
    }

    @Test
    void updateShouldThrowForbiddenExceptionWhenLoggedUserIsAnotherPsychologist() {
        AppointmentInsertDTO dto = AppointmentFactory.createIndividualAppointmentInsertDTO();
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(appointment));
        Mockito.when(authService.authenticated()).thenReturn(otherPsychologist);

        Assertions.assertThrows(ForbiddenException.class, () -> service.update(existingAppointmentId, dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void updateShouldThrowForbiddenExceptionWhenPsychologistTriesToTransferAppointmentToAnotherPsychologist() {
        AppointmentInsertDTO dto = new AppointmentInsertDTO(
                "Título", null, LocalDateTime.now().plusDays(2), 50, ScheduleType.INDIVIDUAL,
                otherPsychologist.getId(), List.of(1L), null, null, null);
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(appointment));
        Mockito.when(authService.authenticated()).thenReturn(psychologist);

        Assertions.assertThrows(ForbiddenException.class, () -> service.update(existingAppointmentId, dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void cancelShouldSetStatusCanceladaAndReason() {
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(appointment));
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDTO result = service.cancel(existingAppointmentId, new AppointmentCancelDTO("Imprevisto"));

        Assertions.assertEquals(ScheduleStatus.CANCELADA, result.scheduleStatus());
        Assertions.assertEquals("Imprevisto", result.cancelReason());
    }

    @Test
    void cancelShouldSetStatusCanceladaWhenLoggedUserIsAdmin() {
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(appointment));
        Mockito.when(authService.authenticated()).thenReturn(admin);
        Mockito.when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDTO result = service.cancel(existingAppointmentId, new AppointmentCancelDTO("Imprevisto"));

        Assertions.assertEquals(ScheduleStatus.CANCELADA, result.scheduleStatus());
    }

    @Test
    void cancelShouldThrowIllegalStateExceptionWhenAppointmentIsAlreadyDone() {
        Appointment done = AppointmentFactory.createAppointmentWithStatus(ScheduleStatus.REALIZADA);
        AppointmentCancelDTO dto = new AppointmentCancelDTO("Imprevisto");
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(done));
        Mockito.when(authService.authenticated()).thenReturn(psychologist);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.cancel(existingAppointmentId, dto));
    }

    @Test
    void cancelShouldThrowForbiddenExceptionWhenLoggedUserIsAnotherPsychologist() {
        AppointmentCancelDTO dto = new AppointmentCancelDTO("Imprevisto");
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(appointment));
        Mockito.when(authService.authenticated()).thenReturn(otherPsychologist);

        Assertions.assertThrows(ForbiddenException.class,
                () -> service.cancel(existingAppointmentId, dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }

    @Test
    void registerFeedbackShouldSetStatusRealizadaAndFeedbackDate() {
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(appointment));
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDTO result = service.registerFeedback(existingAppointmentId, new AppointmentFeedbackDTO("Sessão produtiva."));

        Assertions.assertEquals(ScheduleStatus.REALIZADA, result.scheduleStatus());
        Assertions.assertEquals("Sessão produtiva.", result.feedback());
        Assertions.assertNotNull(result.feedbackDate());
    }

    @Test
    void registerFeedbackShouldThrowIllegalStateExceptionWhenAppointmentIsCancelled() {
        AppointmentFeedbackDTO dto = new AppointmentFeedbackDTO("Sessão produtiva.");
        Appointment cancelled = AppointmentFactory.createAppointmentWithStatus(ScheduleStatus.CANCELADA);
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(cancelled));
        Mockito.when(authService.authenticated()).thenReturn(psychologist);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.registerFeedback(existingAppointmentId, dto));
    }

    @Test
    void registerFeedbackShouldThrowForbiddenExceptionWhenLoggedUserIsAnotherPsychologist() {
        AppointmentFeedbackDTO dto = new AppointmentFeedbackDTO("Sessão produtiva.");
        Mockito.when(appointmentRepository.findById(existingAppointmentId)).thenReturn(Optional.of(appointment));
        Mockito.when(authService.authenticated()).thenReturn(otherPsychologist);

        Assertions.assertThrows(ForbiddenException.class,
                () -> service.registerFeedback(existingAppointmentId, dto));
        Mockito.verify(appointmentRepository, Mockito.never()).save(any());
    }
}
