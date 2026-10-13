package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.psychologist.PanelDTOs;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.entities.enums.ProgressStatus;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import com.synccarreira.synccarreira_api.projections.AnsweredQuestionsProjection;
import com.synccarreira.synccarreira_api.projections.PanelStudentProjection;
import com.synccarreira.synccarreira_api.projections.SubmittedSynthesisProjection;
import com.synccarreira.synccarreira_api.projections.TrailQuestionCountProjection;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.PsychologistRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PsychologistPanelService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final PsychologistRepository psychologistRepository;

    private final StudentRepository studentRepository;

    private final TrailRepository trailRepository;

    private final QuestionRepository questionRepository;

    private final AnswerRepository answerRepository;

    private final SynthesisRepository synthesisRepository;

    private final AuthService authService;

    public PsychologistPanelService(
            final PsychologistRepository psychologistRepository,
            final StudentRepository studentRepository,
            final TrailRepository trailRepository,
            final QuestionRepository questionRepository,
            final AnswerRepository answerRepository,
            final SynthesisRepository synthesisRepository,
            final AuthService authService) {
        this.psychologistRepository = psychologistRepository;
        this.studentRepository = studentRepository;
        this.trailRepository = trailRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.synthesisRepository = synthesisRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PanelDTOs.PanelSummary findPanel(Long psychologistId) {
        Long institutionId = findAccessibleInstitutionId(psychologistId);
        List<PanelDTOs.StudentStatus> students = buildStudentStatuses(
                institutionId,
                studentRepository.searchPanelStudentsByInstitution(institutionId));
        return summarize(students);
    }

    // Valida que o usuário logado pode acessar o painel e retorna a instituição da psicóloga
    public Long findAccessibleInstitutionId(Long psychologistId) {
        validateSelfAccess(psychologistId);
        return findInstitutionId(psychologistId);
    }

    // Calcula o progresso na jornada dos alunos informados, que devem ser da instituição
    public List<PanelDTOs.StudentStatus> buildStudentStatuses(Long institutionId, List<PanelStudentProjection> students) {
        List<Trail> allTrails = trailRepository.findAllByOrderBySequentialOrderAsc();
        List<Trail> trails = allTrails.stream()
                .filter(trail -> trail.getName() != TrailName.INFORMACAO)
                .toList();
        // A síntese final da jornada é a síntese enviada na trilha de informação
        Long informationTrailId = allTrails.stream()
                .filter(trail -> trail.getName() == TrailName.INFORMACAO)
                .map(Trail::getId)
                .findFirst()
                .orElse(null);
        Map<Long, Long> totalQuestionsByTrail = questionRepository.countQuestionsByTrail()
                .stream()
                .collect(Collectors.toMap(TrailQuestionCountProjection::trailId, TrailQuestionCountProjection::totalQuestions));
        Map<Long, Map<Long, Long>> answeredQuestionsByStudent = answerRepository.countAnsweredQuestionsByInstitution(institutionId)
                .stream()
                .collect(Collectors.groupingBy(
                        AnsweredQuestionsProjection::studentId,
                        Collectors.toMap(AnsweredQuestionsProjection::trailId, AnsweredQuestionsProjection::answeredQuestions)));
        Map<Long, Set<Long>> submittedSynthesesByStudent = synthesisRepository.findSubmittedByInstitution(institutionId)
                .stream()
                .collect(Collectors.groupingBy(
                        SubmittedSynthesisProjection::studentId,
                        Collectors.mapping(SubmittedSynthesisProjection::trailId, Collectors.toSet())));

        return students.stream()
                .map(student -> buildStudentStatus(
                        student,
                        trails,
                        informationTrailId,
                        totalQuestionsByTrail,
                        answeredQuestionsByStudent.getOrDefault(student.studentId(), Map.of()),
                        submittedSynthesesByStudent.getOrDefault(student.studentId(), Set.of())))
                .toList();
    }

    public PanelDTOs.PanelSummary summarize(List<PanelDTOs.StudentStatus> students) {
        return new PanelDTOs.PanelSummary(
                students.size(),
                countByJourneyStatus(students, ProgressStatus.NAO_INICIADA),
                countByJourneyStatus(students, ProgressStatus.EM_ANDAMENTO),
                countByJourneyStatus(students, ProgressStatus.CONCLUIDA),
                (int) students.stream().filter(PanelDTOs.StudentStatus::inDoubt).count(),
                students);
    }

    private PanelDTOs.StudentStatus buildStudentStatus(
            PanelStudentProjection student,
            List<Trail> trails,
            Long informationTrailId,
            Map<Long, Long> totalQuestionsByTrail,
            Map<Long, Long> answeredQuestionsByTrail,
            Set<Long> submittedSynthesisTrailIds) {
        List<PanelDTOs.TrailProgress> trailProgressList = new ArrayList<>();
        long answeredQuestions = 0;
        long totalQuestions = 0;
        int concludedTrails = 0;
        Trail currentTrail = null;

        for (Trail trail : trails) {
            long trailTotal = totalQuestionsByTrail.getOrDefault(trail.getId(), 0L);
            long trailAnswered = answeredQuestionsByTrail.getOrDefault(trail.getId(), 0L);
            boolean synthesisSubmitted = submittedSynthesisTrailIds.contains(trail.getId());
            ProgressStatus status = trailStatus(trailAnswered, trailTotal, synthesisSubmitted);

            trailProgressList.add(new PanelDTOs.TrailProgress(
                    trail.getId(),
                    trail.getName(),
                    trail.getSequentialOrder(),
                    trailAnswered,
                    trailTotal,
                    percentage(trailAnswered, trailTotal),
                    synthesisSubmitted,
                    status));

            answeredQuestions += trailAnswered;
            totalQuestions += trailTotal;
            if (status == ProgressStatus.CONCLUIDA) {
                concludedTrails++;
            } else if (currentTrail == null) {
                currentTrail = trail;
            }
        }

        boolean finalSynthesisSubmitted = informationTrailId != null && submittedSynthesisTrailIds.contains(informationTrailId);
        TrailName currentTrailName = currentTrail != null ? currentTrail.getName() : null;
        if (currentTrailName == null && !finalSynthesisSubmitted) {
            // Concluiu as trilhas de perguntas, mas ainda falta a síntese final na trilha de informação
            currentTrailName = TrailName.INFORMACAO;
        }

        return new PanelDTOs.StudentStatus(
                student.studentId(),
                student.studentName(),
                student.schoolClassId(),
                student.schoolClassName(),
                answeredQuestions,
                totalQuestions,
                percentage(answeredQuestions, totalQuestions),
                concludedTrails,
                trails.size(),
                currentTrailName,
                finalSynthesisSubmitted,
                journeyStatus(answeredQuestions, concludedTrails, trails.size(), finalSynthesisSubmitted),
                Boolean.TRUE.equals(student.inDoubt()),
                student.doubtFlaggedAt(),
                trailProgressList);
    }

    private static ProgressStatus trailStatus(long answered, long total, boolean synthesisSubmitted) {
        if (total > 0 && answered >= total && synthesisSubmitted) {
            return ProgressStatus.CONCLUIDA;
        }
        return answered > 0 ? ProgressStatus.EM_ANDAMENTO : ProgressStatus.NAO_INICIADA;
    }

    private static ProgressStatus journeyStatus(long answeredQuestions, int concludedTrails, int totalTrails, boolean finalSynthesisSubmitted) {
        if (totalTrails > 0 && concludedTrails == totalTrails && finalSynthesisSubmitted) {
            return ProgressStatus.CONCLUIDA;
        }
        return answeredQuestions > 0 ? ProgressStatus.EM_ANDAMENTO : ProgressStatus.NAO_INICIADA;
    }

    private static int percentage(long answered, long total) {
        if (total == 0) {
            return 0;
        }
        return (int) (Math.min(answered, total) * 100 / total);
    }

    private static int countByJourneyStatus(List<PanelDTOs.StudentStatus> students, ProgressStatus status) {
        return (int) students.stream().filter(student -> student.journeyStatus() == status).count();
    }

    private Long findInstitutionId(Long psychologistId) {
        Psychologist psychologist = psychologistRepository.findById(psychologistId)
                .orElseThrow(() -> new ResourceNotFoundException("Psicóloga não encontrada. ID: " + psychologistId));
        if (psychologist.getInstitution() == null) {
            throw new BusinessException("A psicóloga não está vinculada a nenhuma instituição.");
        }
        return psychologist.getInstitution().getId();
    }

    private void validateSelfAccess(Long psychologistId) {
        User user = authService.authenticated();
        if (user.hasRole(ROLE_ADMIN)) {
            return;
        }
        if (!user.getId().equals(psychologistId)) {
            throw new ForbiddenException("Acesso negado: só é permitido acessar o próprio painel.");
        }
    }
}
