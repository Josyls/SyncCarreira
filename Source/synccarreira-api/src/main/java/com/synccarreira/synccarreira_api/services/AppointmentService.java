package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.AppointmentCancelDTO;
import com.synccarreira.synccarreira_api.dto.AppointmentDTO;
import com.synccarreira.synccarreira_api.dto.AppointmentFeedbackDTO;
import com.synccarreira.synccarreira_api.dto.AppointmentInsertDTO;
import com.synccarreira.synccarreira_api.entities.Appointment;
import com.synccarreira.synccarreira_api.entities.Psychologist;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final AppointmentRepository appointmentRepository;

    private final PsychologistRepository psychologistRepository;

    private final StudentRepository studentRepository;

    private final PsychologistService psychologistService;

    private final AuthService authService;

    public AppointmentService(
            final AppointmentRepository appointmentRepository,
            final PsychologistRepository psychologistRepository,
            final StudentRepository studentRepository,
            final PsychologistService psychologistService,
            final AuthService authService) {
        this.appointmentRepository = appointmentRepository;
        this.psychologistRepository = psychologistRepository;
        this.studentRepository = studentRepository;
        this.psychologistService = psychologistService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByPsychologist(Long psychologistId) {
        validateSelfAccess(psychologistId);
        return appointmentRepository.findByPsychologistIdOrderByDateTimeAsc(psychologistId)
                .stream()
                .map(AppointmentDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByStudent(Long studentId) {
        validateSelfAccess(studentId);
        return appointmentRepository.findByStudentId(studentId)
                .stream()
                .map(AppointmentDTO::new)
                .toList();
    }

    @Transactional
    public AppointmentDTO insert(AppointmentInsertDTO dto) {
        Appointment appointment = new Appointment();
        copyDtoToEntity(dto, appointment);
        appointment.setScheduleStatus(ScheduleStatus.AGENDADA);
        appointment = appointmentRepository.save(appointment);
        return new AppointmentDTO(appointment);
    }

    @Transactional
    public AppointmentDTO update(Long id, AppointmentInsertDTO dto) {
        Appointment appointment = findEntityById(id);
        Long currentPsychologistId = appointment.getPsychologist().getId();
        validateSelfAccess(currentPsychologistId);
        if (!currentPsychologistId.equals(dto.psychologistId())) {
            validateSelfAccess(dto.psychologistId());
        }
        if (appointment.getScheduleStatus() != ScheduleStatus.AGENDADA) {
            throw new IllegalStateException(
                    "Apenas sessões agendadas podem ser editadas. Status atual: " + appointment.getScheduleStatus() + ".");
        }
        copyDtoToEntity(dto, appointment);
        appointment = appointmentRepository.save(appointment);
        return new AppointmentDTO(appointment);
    }

    @Transactional
    public AppointmentDTO cancel(Long id, AppointmentCancelDTO dto) {
        Appointment appointment = findEntityById(id);
        validateSelfAccess(appointment.getPsychologist().getId());
        if (appointment.getScheduleStatus() != ScheduleStatus.AGENDADA) {
            throw new IllegalStateException(
                    "Apenas sessões agendadas podem ser canceladas. Status atual: " + appointment.getScheduleStatus() + ".");
        }
        appointment.setScheduleStatus(ScheduleStatus.CANCELADA);
        appointment.setCancelReason(dto.cancelReason());
        appointment = appointmentRepository.save(appointment);
        return new AppointmentDTO(appointment);
    }

    @Transactional
    public AppointmentDTO registerFeedback(Long id, AppointmentFeedbackDTO dto) {
        Appointment appointment = findEntityById(id);
        validateSelfAccess(appointment.getPsychologist().getId());
        if (appointment.getScheduleStatus() == ScheduleStatus.CANCELADA) {
            throw new IllegalStateException("Não é possível registrar feedback de uma sessão cancelada.");
        }
        appointment.setScheduleStatus(ScheduleStatus.REALIZADA);
        appointment.setFeedback(dto.feedback());
        appointment.setFeedbackDate(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        appointment = appointmentRepository.save(appointment);
        return new AppointmentDTO(appointment);
    }

    private void validateSelfAccess(Long requestedUserId) {
        User user = authService.authenticated();
        if (user.hasRole(ROLE_ADMIN)) {
            return;
        }
        if (!user.getId().equals(requestedUserId)) {
            throw new ForbiddenException("Acesso negado: só é permitido acessar a própria agenda.");
        }
    }

    private Appointment findEntityById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado. ID: " + id));
    }

    private void copyDtoToEntity(AppointmentInsertDTO dto, Appointment entity) {
        Set<Long> studentIds = new LinkedHashSet<>(dto.studentIds());
        validateStudentCount(dto.scheduleType(), studentIds.size());

        psychologistService.validateIfContractIsActive(dto.psychologistId());
        Psychologist psychologist = psychologistRepository.findById(dto.psychologistId())
                .orElseThrow(() -> new ResourceNotFoundException("Psicóloga não encontrada. ID: " + dto.psychologistId()));

        List<Student> students = studentRepository.findAllById(studentIds);
        if (students.size() != studentIds.size()) {
            List<Long> foundIds = students.stream().map(Student::getId).toList();
            List<Long> missingIds = studentIds.stream().filter(studentId -> !foundIds.contains(studentId)).toList();
            throw new ResourceNotFoundException("Aluno(s) não encontrado(s). ID(s): " + missingIds);
        }
        validateSameInstitution(psychologist, students);

        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setDateTime(dto.dateTime());
        entity.setDurationMinutes(dto.durationMinutes());
        entity.setScheduleType(dto.scheduleType());
        entity.setPsychologist(psychologist);
        entity.getStudents().clear();
        entity.getStudents().addAll(students);
        entity.setGoogleEventId(dto.googleEventId());
        entity.setMeetLink(dto.meetLink());
        entity.setCalendarLink(dto.calendarLink());
    }

    // RNF-08: a psicóloga só agenda sessões com alunos da instituição dela
    private static void validateSameInstitution(Psychologist psychologist, List<Student> students) {
        if (psychologist.getInstitution() == null) {
            throw new BusinessException("A psicóloga não está vinculada a nenhuma instituição.");
        }
        Long institutionId = psychologist.getInstitution().getId();
        List<Long> outsideIds = students.stream()
                .filter(student -> !belongsToInstitution(student, institutionId))
                .map(Student::getId)
                .toList();
        if (!outsideIds.isEmpty()) {
            throw new ForbiddenException("Acesso negado: aluno(s) de outra instituição ou sem turma. ID(s): " + outsideIds);
        }
    }

    private static boolean belongsToInstitution(Student student, Long institutionId) {
        SchoolClass schoolClass = student.getDeterminedSchoolClass();
        return schoolClass != null
                && schoolClass.getInstitution() != null
                && institutionId.equals(schoolClass.getInstitution().getId());
    }

    private void validateStudentCount(ScheduleType scheduleType, int count) {
        if (scheduleType == ScheduleType.INDIVIDUAL && count != 1) {
            throw new BusinessException("Sessões individuais devem ter exatamente 1 aluno.");
        }
        if (scheduleType == ScheduleType.GRUPO && count < 2) {
            throw new BusinessException("Sessões em grupo devem ter 2 ou mais alunos.");
        }
    }
}
