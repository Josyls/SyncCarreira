package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.JourneyDoubtDTO;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.repositories.PsychologistRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.events.EmailEvent;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ConflictException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JourneyService {

    private static final Logger log = LoggerFactory.getLogger(JourneyService.class);

    private static final String DOUBT_ALREADY_FLAGGED = "A dúvida sobre a escolha profissional já foi sinalizada.";

    private static final String JOURNEY_DOUBT_SUBJECT = "SyncCarreira - Aluno concluiu a jornada e está em dúvida";

    private static final DateTimeFormatter FLAGGED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    private final StudentRepository studentRepository;

    private final TrailRepository trailRepository;

    private final SynthesisRepository synthesisRepository;

    private final PsychologistRepository psychologistRepository;

    private final TrailService trailService;

    private final AuthService authService;

    private final ApplicationEventPublisher eventPublisher;

    public JourneyService(
            final StudentRepository studentRepository,
            final TrailRepository trailRepository,
            final SynthesisRepository synthesisRepository,
            final PsychologistRepository psychologistRepository,
            final TrailService trailService,
            final AuthService authService,
            final ApplicationEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.trailRepository = trailRepository;
        this.synthesisRepository = synthesisRepository;
        this.psychologistRepository = psychologistRepository;
        this.trailService = trailService;
        this.authService = authService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public JourneyDoubtDTO findDoubt() {
        Student student = loggedStudent();
        return new JourneyDoubtDTO(student.getId(), Boolean.TRUE.equals(student.getInDoubt()), student.getDoubtFlaggedAt());
    }

    @Transactional
    public JourneyDoubtDTO flagDoubt() {
        Student student = loggedStudent();
        if (Boolean.TRUE.equals(student.getInDoubt())) {
            throw new ConflictException(DOUBT_ALREADY_FLAGGED);
        }
        if (!isJourneyConcluded(student.getId())) {
            throw new BusinessException("Conclua a jornada, respondendo todas as perguntas e enviando todas as sínteses, antes de sinalizar dúvida.");
        }

        Instant flaggedAt = Instant.now();
        // Duas requisições simultâneas: só a primeira marca o aluno, e a psicóloga é avisada uma única vez
        if (studentRepository.flagDoubt(student.getId(), flaggedAt) == 0) {
            throw new ConflictException(DOUBT_ALREADY_FLAGGED);
        }
        notifyPsychologists(student, flaggedAt);
        return new JourneyDoubtDTO(student.getId(), true, flaggedAt);
    }

    private boolean isJourneyConcluded(Long studentId) {
        List<Trail> trails = trailRepository.findAll();
        return !trails.isEmpty() && trails.stream()
                .allMatch(trail -> synthesisRepository.existsByStudentIdAndTrailId(studentId, trail.getId())
                        && trailService.areAllQuestionsAnswered(trail, studentId));
    }

    // O aviso é enviado por e-mail às psicólogas com contrato vigente da instituição do aluno, após o commit
    private void notifyPsychologists(Student student, Instant flaggedAt) {
        SchoolClass schoolClass = student.getDeterminedSchoolClass();
        if (schoolClass == null) {
            log.warn("Aluno {} sinalizou dúvida, mas não está vinculado a nenhuma turma.", student.getId());
            return;
        }

        Institution institution = schoolClass.getInstitution();
        if (institution == null) {
            log.warn("Aluno {} sinalizou dúvida, mas a turma dele não está vinculada a nenhuma instituição.", student.getId());
            return;
        }

        List<Psychologist> psychologists = psychologistRepository.findByInstitutionId(institution.getId())
                .stream()
                .filter(Psychologist::isContractValid)
                .toList();
        if (psychologists.isEmpty()) {
            log.warn("Aluno {} sinalizou dúvida, mas a instituição {} não tem psicóloga com contrato vigente para ser avisada.",
                    student.getId(), institution.getId());
            return;
        }

        for (Psychologist psychologist : psychologists) {
            Map<String, Object> map = new HashMap<>();
            map.put("recipientName", psychologist.getName());
            map.put("studentName", student.getName());
            map.put("studentEmail", student.getEmail());
            map.put("schoolClassName", schoolClass.getName());
            map.put("institutionName", institutionName(institution));
            map.put("flaggedAt", FLAGGED_AT_FORMATTER.format(flaggedAt));

            eventPublisher.publishEvent(new EmailEvent(psychologist.getEmail(), JOURNEY_DOUBT_SUBJECT, EmailService.JOURNEY_DOUBT_TEMPLATE, map));
        }
    }

    private static String institutionName(Institution institution) {
        String tradeName = institution.getTradeName();
        return tradeName != null && !tradeName.isBlank() ? tradeName : institution.getLegalName();
    }

    private Student loggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }
}
