package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.feedback.FeedbackDTO;
import com.synccarreira.synccarreira_api.dto.feedback.JourneyFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.dto.feedback.SessionFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.dto.feedback.StudentFeedbackStatusDTO;
import com.synccarreira.synccarreira_api.entities.Appointment;
import com.synccarreira.synccarreira_api.entities.Feedback;
import com.synccarreira.synccarreira_api.entities.JourneyRecord;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.User;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Feedbacks individuais e opcionais da psicóloga para o aluno (RF-09) e a área de feedbacks do aluno (RF-16)
@Service
public class FeedbackService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FEEDBACK_SUBJECT = "SyncCarreira - Você recebeu um novo feedback";
    private static final String ALREADY_SENT_JOURNEY = "Já foi enviado um feedback para esta jornada do aluno.";
    private static final String ALREADY_SENT_SESSION = "Já foi enviado um feedback para este aluno nesta sessão.";

    private final FeedbackRepository feedbackRepository;
    private final StudentRepository studentRepository;
    private final PsychologistRepository psychologistRepository;
    private final AppointmentRepository appointmentRepository;
    private final JourneyRecordRepository journeyRecordRepository;
    private final PsychologistPanelService panelService;
    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    public FeedbackService(
            final FeedbackRepository feedbackRepository,
            final StudentRepository studentRepository,
            final PsychologistRepository psychologistRepository,
            final AppointmentRepository appointmentRepository,
            final JourneyRecordRepository journeyRecordRepository,
            final PsychologistPanelService panelService,
            final AuthService authService,
            final ApplicationEventPublisher eventPublisher) {
        this.feedbackRepository = feedbackRepository;
        this.studentRepository = studentRepository;
        this.psychologistRepository = psychologistRepository;
        this.appointmentRepository = appointmentRepository;
        this.journeyRecordRepository = journeyRecordRepository;
        this.panelService = panelService;
        this.authService = authService;
        this.eventPublisher = eventPublisher;
    }

    // ---------- Psicóloga ----------

    @Transactional
    public FeedbackDTO sendJourneyFeedback(JourneyFeedbackInsertDTO dto) {
        Psychologist psychologist = loggedPsychologistWithValidContract();
        Student student = findStudentOfInstitution(dto.studentId(), psychologist);

        Integer journeyNumber = resolveConcludedJourney(student, dto.journeyNumber());
        if (feedbackRepository.existsByStudentIdAndJourneyNumber(student.getId(), journeyNumber)) {
            throw new ConflictException(ALREADY_SENT_JOURNEY);
        }

        Feedback feedback = newFeedback(FeedbackType.JORNADA, student, psychologist, dto.content());
        feedback.setJourneyNumber(journeyNumber);
        feedback = save(feedback, ALREADY_SENT_JOURNEY);

        notifyStudent(feedback, "a sua jornada nº " + journeyNumber);
        return new FeedbackDTO(feedback);
    }

    @Transactional
    public FeedbackDTO sendSessionFeedback(SessionFeedbackInsertDTO dto) {
        Psychologist psychologist = loggedPsychologistWithValidContract();
        Appointment appointment = appointmentRepository.findById(dto.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada. ID: " + dto.appointmentId()));
        if (!appointment.getPsychologist().getId().equals(psychologist.getId())) {
            throw new ForbiddenException("Acesso negado: só a psicóloga responsável pela sessão pode enviar feedback sobre ela.");
        }
        validateRealized(appointment);

        Student student = findStudentOfInstitution(dto.studentId(), psychologist);
        boolean participated = appointment.getStudents().stream()
                .anyMatch(participant -> participant.getId().equals(student.getId()));
        if (!participated) {
            throw new BusinessException("O aluno não participou desta sessão.");
        }
        if (feedbackRepository.existsByStudentIdAndAppointmentId(student.getId(), appointment.getId())) {
            throw new ConflictException(ALREADY_SENT_SESSION);
        }

        Feedback feedback = newFeedback(FeedbackType.SESSAO, student, psychologist, dto.content());
        feedback.setAppointment(appointment);
        feedback = save(feedback, ALREADY_SENT_SESSION);

        notifyStudent(feedback, "a sessão \"" + appointment.getTitle() + "\" de " + DATE.format(appointment.getDateTime()));
        return new FeedbackDTO(feedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackDTO> findSentFeedbacks() {
        Psychologist psychologist = loggedPsychologist();
        return feedbackRepository.findByPsychologistIdOrderByCreatedAtDesc(psychologist.getId())
                .stream()
                .map(FeedbackDTO::new)
                .toList();
    }

    // O feedback não pode ser editado, só apagado, e apenas pela psicóloga que o escreveu
    @Transactional
    public void delete(Long feedbackId) {
        Psychologist psychologist = loggedPsychologist();
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback não encontrado. ID: " + feedbackId));
        if (!feedback.getPsychologist().getId().equals(psychologist.getId())) {
            throw new ForbiddenException("Acesso negado: só a psicóloga que escreveu o feedback pode apagá-lo.");
        }
        feedbackRepository.delete(feedback);
    }

    // Painel: quais alunos da instituição já receberam feedback
    @Transactional(readOnly = true)
    public List<StudentFeedbackStatusDTO> findFeedbackStatus(Long psychologistId) {
        Long institutionId = panelService.findAccessibleInstitutionId(psychologistId);
        List<PanelStudentProjection> students = studentRepository.searchPanelStudentsByInstitution(institutionId);
        if (students.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = students.stream().map(PanelStudentProjection::studentId).toList();
        Map<Long, List<JourneyRecord>> journeysByStudent = journeyRecordRepository.findByStudentIdIn(studentIds)
                .stream()
                .collect(Collectors.groupingBy(journey -> journey.getStudent().getId()));
        Map<Long, List<Feedback>> feedbacksByStudent = feedbackRepository.findByStudentIdIn(studentIds)
                .stream()
                .collect(Collectors.groupingBy(feedback -> feedback.getStudent().getId()));

        return students.stream()
                .map(student -> toStatus(
                        student,
                        journeysByStudent.getOrDefault(student.studentId(), List.of()),
                        feedbacksByStudent.getOrDefault(student.studentId(), List.of())))
                .toList();
    }

    // ---------- Aluno ----------

    @Transactional(readOnly = true)
    public List<FeedbackDTO> findMyFeedbacks() {
        Student student = loggedStudent();
        return feedbackRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(FeedbackDTO::new)
                .toList();
    }

    // Marca como lido quando o aluno abre o feedback. Abrir de novo mantém a data da primeira leitura.
    @Transactional
    public FeedbackDTO markAsRead(Long feedbackId) {
        Student student = loggedStudent();
        Feedback feedback = feedbackRepository.findByIdAndStudentId(feedbackId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Feedback não encontrado. ID: " + feedbackId));
        if (feedback.getReadAt() == null) {
            feedback.setReadAt(Instant.now());
            feedback = feedbackRepository.save(feedback);
        }
        return new FeedbackDTO(feedback);
    }

    // ---------- Regras auxiliares ----------

    private Integer resolveConcludedJourney(Student student, Integer requestedJourneyNumber) {
        if (requestedJourneyNumber != null) {
            if (!journeyRecordRepository.existsByStudentIdAndJourneyNumber(student.getId(), requestedJourneyNumber)) {
                throw new BusinessException("O aluno não concluiu a jornada nº " + requestedJourneyNumber + ".");
            }
            return requestedJourneyNumber;
        }
        return journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId())
                .stream()
                .map(JourneyRecord::getJourneyNumber)
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new BusinessException("O aluno ainda não concluiu nenhuma jornada."));
    }

    // Considera realizada a sessão não cancelada cujo horário de término já passou (mesma regra exibida pelo front)
    private static void validateRealized(Appointment appointment) {
        if (appointment.getScheduleStatus() == ScheduleStatus.CANCELADA) {
            throw new BusinessException("Não é possível enviar feedback de uma sessão cancelada.");
        }
        if (appointment.getScheduleStatus() == ScheduleStatus.REALIZADA) {
            return;
        }
        int duration = appointment.getDurationMinutes() != null ? appointment.getDurationMinutes() : 0;
        LocalDateTime end = appointment.getDateTime().plusMinutes(duration);
        if (!end.isBefore(LocalDateTime.now(ZONE))) {
            throw new BusinessException("A sessão ainda não foi realizada. O feedback fica disponível depois do horário de término.");
        }
    }

    private Student findStudentOfInstitution(Long studentId, Psychologist psychologist) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + studentId));
        Long institutionId = psychologist.getInstitution() != null ? psychologist.getInstitution().getId() : null;
        SchoolClass schoolClass = student.getDeterminedSchoolClass();
        Long studentInstitutionId = schoolClass != null && schoolClass.getInstitution() != null
                ? schoolClass.getInstitution().getId()
                : null;
        if (institutionId == null || !institutionId.equals(studentInstitutionId)) {
            throw new ForbiddenException("Acesso negado: o aluno não pertence à instituição da psicóloga.");
        }
        return student;
    }

    private static Feedback newFeedback(FeedbackType type, Student student, Psychologist psychologist, String content) {
        Feedback feedback = new Feedback();
        feedback.setType(type);
        feedback.setStudent(student);
        feedback.setPsychologist(psychologist);
        feedback.setContent(content.strip());
        feedback.setCreatedAt(Instant.now());
        return feedback;
    }

    private Feedback save(Feedback feedback, String conflictMessage) {
        try {
            return feedbackRepository.saveAndFlush(feedback);
        }
        catch (DataIntegrityViolationException e) {
            // Duas requisições simultâneas: a restrição única do banco barra a segunda
            throw new ConflictException(conflictMessage);
        }
    }

    // O e-mail só avisa que há um feedback novo, sem o texto (privacidade dos alunos)
    private void notifyStudent(Feedback feedback, String feedbackSubject) {
        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", feedback.getStudent().getName());
        model.put("psychologistName", feedback.getPsychologist().getName());
        model.put("feedbackSubject", feedbackSubject);
        eventPublisher.publishEvent(new EmailEvent(feedback.getStudent().getEmail(), FEEDBACK_SUBJECT, EmailService.FEEDBACK_TEMPLATE, model));
    }

    private static StudentFeedbackStatusDTO toStatus(PanelStudentProjection student, List<JourneyRecord> journeys, List<Feedback> feedbacks) {
        Optional<Integer> lastJourney = journeys.stream()
                .map(JourneyRecord::getJourneyNumber)
                .max(Comparator.naturalOrder());
        boolean journeyFeedbackSent = lastJourney.isPresent() && feedbacks.stream()
                .anyMatch(feedback -> feedback.getType() == FeedbackType.JORNADA
                        && lastJourney.get().equals(feedback.getJourneyNumber()));
        long sessionFeedbacks = feedbacks.stream()
                .filter(feedback -> feedback.getType() == FeedbackType.SESSAO)
                .count();
        return new StudentFeedbackStatusDTO(
                student.studentId(),
                student.studentName(),
                student.schoolClassName(),
                journeys.size(),
                lastJourney.orElse(null),
                journeyFeedbackSent,
                sessionFeedbacks);
    }

    private Psychologist loggedPsychologist() {
        User user = authService.authenticated();
        return psychologistRepository.findById(user.getId())
                .orElseThrow(() -> new ForbiddenException("Acesso negado: apenas psicólogas podem enviar ou apagar feedbacks."));
    }

    // Enviar feedback exige contrato vigente, como agendar sessões
    private Psychologist loggedPsychologistWithValidContract() {
        Psychologist psychologist = loggedPsychologist();
        if (!psychologist.isContractValid()) {
            throw new IllegalStateException("Operação bloqueada: o contrato da psicóloga '" + psychologist.getName()
                    + "' está vencido desde " + psychologist.getContractExpirationDate() + ".");
        }
        return psychologist;
    }

    private Student loggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }
}
