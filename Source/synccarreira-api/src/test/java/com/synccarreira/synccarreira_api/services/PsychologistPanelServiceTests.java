package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.psychologist.PanelDTOs;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.Role;
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
import com.synccarreira.synccarreira_api.tests.PsychologistFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class PsychologistPanelServiceTests {

    @InjectMocks
    private PsychologistPanelService service;

    @Mock
    private PsychologistRepository psychologistRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TrailRepository trailRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private SynthesisRepository synthesisRepository;

    @Mock
    private AuthService authService;

    private Psychologist psychologist, otherPsychologist;
    private User admin;
    private Long psychologistId, nonExistingPsychologistId, institutionId;
    private List<Trail> trails;
    private List<TrailQuestionCountProjection> questionCounts;
    private List<PanelStudentProjection> students;
    private List<AnsweredQuestionsProjection> answeredQuestions;
    private List<SubmittedSynthesisProjection> submittedSyntheses;

    @BeforeEach
    void setUp() {
        psychologist = PsychologistFactory.createPsychologist();
        psychologistId = psychologist.getId();
        institutionId = psychologist.getInstitution().getId();
        nonExistingPsychologistId = 100L;
        otherPsychologist = PsychologistFactory.createPsychologist();
        otherPsychologist.setId(2L);
        admin = new User();
        admin.setId(99L);
        admin.addRole(new Role(2L, "ROLE_ADMIN"));

        trails = List.of(
                createTrail(1L, TrailName.AUTOCONHECIMENTO, 1),
                createTrail(2L, TrailName.INFLUENCIAS, 2),
                createTrail(3L, TrailName.PLANO_DE_FUTURO, 3),
                createTrail(4L, TrailName.INFORMACAO, 4));
        questionCounts = List.of(
                new TrailQuestionCountProjection(1L, 4L),
                new TrailQuestionCountProjection(2L, 2L),
                new TrailQuestionCountProjection(3L, 2L));

        // Ana concluiu tudo, Bruno está no meio da trilha 2 e Carla ainda não começou
        students = List.of(
                new PanelStudentProjection(10L, "Ana", 1L, "3º ano A", false, null),
                new PanelStudentProjection(11L, "Bruno", 1L, "3º ano A", false, null),
                new PanelStudentProjection(12L, "Carla", 2L, "3º ano B", false, null));
        answeredQuestions = List.of(
                new AnsweredQuestionsProjection(10L, 1L, 4L),
                new AnsweredQuestionsProjection(10L, 2L, 2L),
                new AnsweredQuestionsProjection(10L, 3L, 2L),
                new AnsweredQuestionsProjection(11L, 1L, 4L),
                new AnsweredQuestionsProjection(11L, 2L, 1L));
        // Ana enviou as sínteses das três trilhas e a síntese final (trilha de informação); Bruno, só a da trilha 1
        submittedSyntheses = List.of(
                new SubmittedSynthesisProjection(10L, 1L),
                new SubmittedSynthesisProjection(10L, 2L),
                new SubmittedSynthesisProjection(10L, 3L),
                new SubmittedSynthesisProjection(10L, 4L),
                new SubmittedSynthesisProjection(11L, 1L));
    }

    private static Trail createTrail(Long id, TrailName name, Integer order) {
        Trail trail = new Trail();
        trail.setId(id);
        trail.setName(name);
        trail.setSequentialOrder(order);
        return trail;
    }

    private void mockPanelData() {
        Mockito.when(psychologistRepository.findById(psychologistId)).thenReturn(Optional.of(psychologist));
        mockProgressData();
        Mockito.when(studentRepository.searchPanelStudentsByInstitution(institutionId)).thenReturn(students);
    }

    private void mockProgressData() {
        Mockito.when(trailRepository.findAllByOrderBySequentialOrderAsc()).thenReturn(trails);
        Mockito.when(questionRepository.countQuestionsByTrail()).thenReturn(questionCounts);
        Mockito.when(answerRepository.countAnsweredQuestionsByInstitution(institutionId)).thenReturn(answeredQuestions);
        Mockito.when(synthesisRepository.findSubmittedByInstitution(institutionId)).thenReturn(submittedSyntheses);
    }

    private static PanelDTOs.StudentStatus createStudentStatus(Long studentId, ProgressStatus journeyStatus, boolean inDoubt) {
        return new PanelDTOs.StudentStatus(
                studentId, "Aluno " + studentId, 1L, "3º ano A", 0, 8, 0, 0, 3,
                TrailName.AUTOCONHECIMENTO, false, journeyStatus, inDoubt, null, List.of());
    }

    @Test
    void findPanelShouldReturnProgressOfEveryStudentInEveryTrailWhenLoggedUserIsTheSamePsychologist() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        mockPanelData();

        PanelDTOs.PanelSummary result = service.findPanel(psychologistId);

        Assertions.assertEquals(3, result.totalStudents());
        Assertions.assertEquals(1, result.notStartedStudents());
        Assertions.assertEquals(1, result.inProgressStudents());
        Assertions.assertEquals(1, result.concludedStudents());
        Assertions.assertEquals(3, result.students().size());
        result.students().forEach(student -> Assertions.assertEquals(3, student.trails().size()));
    }

    @Test
    void findPanelShouldMarkJourneyAsConcludedWhenStudentConcludedAllTrailsAndSubmittedFinalSynthesis() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        mockPanelData();

        PanelDTOs.StudentStatus ana = service.findPanel(psychologistId).students().get(0);

        Assertions.assertEquals(ProgressStatus.CONCLUIDA, ana.journeyStatus());
        Assertions.assertEquals(3, ana.concludedTrails());
        Assertions.assertEquals(8, ana.answeredQuestions());
        Assertions.assertEquals(100, ana.progressPercentage());
        Assertions.assertTrue(ana.finalSynthesisSubmitted());
        Assertions.assertNull(ana.currentTrail());
        ana.trails().forEach(trail -> Assertions.assertTrue(trail.synthesisSubmitted()));
    }

    @Test
    void findPanelShouldKeepTrailInProgressWhenStudentAnsweredAllQuestionsButDidNotSubmitSynthesis() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        mockPanelData();
        // Ana respondeu todas as perguntas da trilha 3, mas não enviou a síntese dela
        Mockito.when(synthesisRepository.findSubmittedByInstitution(institutionId)).thenReturn(List.of(
                new SubmittedSynthesisProjection(10L, 1L),
                new SubmittedSynthesisProjection(10L, 2L)));

        PanelDTOs.StudentStatus ana = service.findPanel(psychologistId).students().get(0);

        PanelDTOs.TrailProgress futurePlanTrail = ana.trails().get(2);
        Assertions.assertEquals(100, futurePlanTrail.progressPercentage());
        Assertions.assertFalse(futurePlanTrail.synthesisSubmitted());
        Assertions.assertEquals(ProgressStatus.EM_ANDAMENTO, futurePlanTrail.status());
        Assertions.assertEquals(2, ana.concludedTrails());
        Assertions.assertEquals(TrailName.PLANO_DE_FUTURO, ana.currentTrail());
        Assertions.assertEquals(ProgressStatus.EM_ANDAMENTO, ana.journeyStatus());
    }

    @Test
    void findPanelShouldPointToInformationTrailWhenOnlyFinalSynthesisIsMissing() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        mockPanelData();
        Mockito.when(synthesisRepository.findSubmittedByInstitution(institutionId)).thenReturn(List.of(
                new SubmittedSynthesisProjection(10L, 1L),
                new SubmittedSynthesisProjection(10L, 2L),
                new SubmittedSynthesisProjection(10L, 3L)));

        PanelDTOs.StudentStatus ana = service.findPanel(psychologistId).students().get(0);

        Assertions.assertEquals(3, ana.concludedTrails());
        Assertions.assertFalse(ana.finalSynthesisSubmitted());
        Assertions.assertEquals(TrailName.INFORMACAO, ana.currentTrail());
        Assertions.assertEquals(ProgressStatus.EM_ANDAMENTO, ana.journeyStatus());
    }

    @Test
    void findPanelShouldReturnProgressPerTrailAndCurrentTrailWhenStudentIsInProgress() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        mockPanelData();

        PanelDTOs.StudentStatus bruno = service.findPanel(psychologistId).students().get(1);

        Assertions.assertEquals(ProgressStatus.EM_ANDAMENTO, bruno.journeyStatus());
        Assertions.assertEquals(1, bruno.concludedTrails());
        Assertions.assertEquals(5, bruno.answeredQuestions());
        Assertions.assertEquals(8, bruno.totalQuestions());
        Assertions.assertEquals(62, bruno.progressPercentage());
        Assertions.assertEquals(TrailName.INFLUENCIAS, bruno.currentTrail());

        List<PanelDTOs.TrailProgress> trailProgress = bruno.trails();
        Assertions.assertEquals(ProgressStatus.CONCLUIDA, trailProgress.get(0).status());
        Assertions.assertEquals(ProgressStatus.EM_ANDAMENTO, trailProgress.get(1).status());
        Assertions.assertEquals(50, trailProgress.get(1).progressPercentage());
        Assertions.assertEquals(ProgressStatus.NAO_INICIADA, trailProgress.get(2).status());
        Assertions.assertEquals(0, trailProgress.get(2).answeredQuestions());
    }

    @Test
    void findPanelShouldReturnNotStartedStatusWhenStudentHasNoAnswers() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        mockPanelData();

        PanelDTOs.StudentStatus carla = service.findPanel(psychologistId).students().get(2);

        Assertions.assertEquals(ProgressStatus.NAO_INICIADA, carla.journeyStatus());
        Assertions.assertEquals(0, carla.answeredQuestions());
        Assertions.assertEquals(0, carla.progressPercentage());
        Assertions.assertEquals(TrailName.AUTOCONHECIMENTO, carla.currentTrail());
        carla.trails().forEach(trail -> Assertions.assertEquals(ProgressStatus.NAO_INICIADA, trail.status()));
    }

    @Test
    void findPanelShouldIgnoreInformationTrailWhenCalculatingProgress() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        mockPanelData();

        PanelDTOs.StudentStatus ana = service.findPanel(psychologistId).students().get(0);

        Assertions.assertEquals(3, ana.trails().size());
        Assertions.assertTrue(ana.trails().stream().noneMatch(trail -> trail.trailName() == TrailName.INFORMACAO));
        Assertions.assertEquals(3, ana.totalTrails());
        Assertions.assertEquals(ProgressStatus.CONCLUIDA, ana.journeyStatus());
    }

    @Test
    void findPanelShouldReturnPanelWhenLoggedUserIsAdmin() {
        Mockito.when(authService.authenticated()).thenReturn(admin);
        mockPanelData();

        PanelDTOs.PanelSummary result = service.findPanel(psychologistId);

        Assertions.assertEquals(3, result.totalStudents());
    }

    @Test
    void findPanelShouldReturnEmptyPanelWhenInstitutionHasNoStudents() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(psychologistRepository.findById(psychologistId)).thenReturn(Optional.of(psychologist));
        Mockito.when(trailRepository.findAllByOrderBySequentialOrderAsc()).thenReturn(trails);
        Mockito.when(questionRepository.countQuestionsByTrail()).thenReturn(questionCounts);
        Mockito.when(answerRepository.countAnsweredQuestionsByInstitution(institutionId)).thenReturn(List.of());
        Mockito.when(studentRepository.searchPanelStudentsByInstitution(institutionId)).thenReturn(List.of());

        PanelDTOs.PanelSummary result = service.findPanel(psychologistId);

        Assertions.assertEquals(0, result.totalStudents());
        Assertions.assertTrue(result.students().isEmpty());
    }

    @Test
    void findPanelShouldThrowForbiddenExceptionWhenLoggedUserIsAnotherPsychologist() {
        Mockito.when(authService.authenticated()).thenReturn(otherPsychologist);

        Assertions.assertThrows(ForbiddenException.class, () -> service.findPanel(psychologistId));
        Mockito.verify(studentRepository, Mockito.never()).searchPanelStudentsByInstitution(any());
    }

    @Test
    void findPanelShouldThrowResourceNotFoundExceptionWhenPsychologistDoesNotExist() {
        Mockito.when(authService.authenticated()).thenReturn(admin);
        Mockito.when(psychologistRepository.findById(nonExistingPsychologistId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findPanel(nonExistingPsychologistId));
    }

    @Test
    void findPanelShouldThrowBusinessExceptionWhenPsychologistHasNoInstitution() {
        psychologist.setInstitution(null);
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(psychologistRepository.findById(psychologistId)).thenReturn(Optional.of(psychologist));

        Assertions.assertThrows(BusinessException.class, () -> service.findPanel(psychologistId));
        Mockito.verify(studentRepository, Mockito.never()).searchPanelStudentsByInstitution(any());
    }

    @Test
    void findAccessibleInstitutionIdShouldReturnInstitutionIdWhenLoggedUserIsTheSamePsychologist() {
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(psychologistRepository.findById(psychologistId)).thenReturn(Optional.of(psychologist));

        Long result = service.findAccessibleInstitutionId(psychologistId);

        Assertions.assertEquals(institutionId, result);
    }

    @Test
    void findAccessibleInstitutionIdShouldReturnInstitutionIdWhenLoggedUserIsAdmin() {
        Mockito.when(authService.authenticated()).thenReturn(admin);
        Mockito.when(psychologistRepository.findById(psychologistId)).thenReturn(Optional.of(psychologist));

        Long result = service.findAccessibleInstitutionId(psychologistId);

        Assertions.assertEquals(institutionId, result);
    }

    @Test
    void findAccessibleInstitutionIdShouldThrowForbiddenExceptionWhenLoggedUserIsAnotherPsychologist() {
        Mockito.when(authService.authenticated()).thenReturn(otherPsychologist);

        Assertions.assertThrows(ForbiddenException.class, () -> service.findAccessibleInstitutionId(psychologistId));
        Mockito.verify(psychologistRepository, Mockito.never()).findById(any());
    }

    @Test
    void findAccessibleInstitutionIdShouldThrowResourceNotFoundExceptionWhenPsychologistDoesNotExist() {
        Mockito.when(authService.authenticated()).thenReturn(admin);
        Mockito.when(psychologistRepository.findById(nonExistingPsychologistId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findAccessibleInstitutionId(nonExistingPsychologistId));
    }

    @Test
    void findAccessibleInstitutionIdShouldThrowBusinessExceptionWhenPsychologistHasNoInstitution() {
        psychologist.setInstitution(null);
        Mockito.when(authService.authenticated()).thenReturn(psychologist);
        Mockito.when(psychologistRepository.findById(psychologistId)).thenReturn(Optional.of(psychologist));

        Assertions.assertThrows(BusinessException.class, () -> service.findAccessibleInstitutionId(psychologistId));
    }

    @Test
    void buildStudentStatusesShouldReturnProgressOnlyOfGivenStudents() {
        mockProgressData();
        // Só o Bruno: os dados de progresso dos outros alunos da instituição são ignorados
        List<PanelStudentProjection> onlyBruno = List.of(students.get(1));

        List<PanelDTOs.StudentStatus> result = service.buildStudentStatuses(institutionId, onlyBruno);

        Assertions.assertEquals(1, result.size());
        PanelDTOs.StudentStatus bruno = result.get(0);
        Assertions.assertEquals(11L, bruno.studentId());
        Assertions.assertEquals("Bruno", bruno.studentName());
        Assertions.assertEquals(ProgressStatus.EM_ANDAMENTO, bruno.journeyStatus());
        Assertions.assertEquals(5, bruno.answeredQuestions());
        Assertions.assertEquals(TrailName.INFLUENCIAS, bruno.currentTrail());
        Assertions.assertEquals(3, bruno.trails().size());
        Mockito.verify(studentRepository, Mockito.never()).searchPanelStudentsByInstitution(any());
        Mockito.verifyNoInteractions(authService, psychologistRepository);
    }

    @Test
    void buildStudentStatusesShouldKeepOrderOfGivenStudents() {
        mockProgressData();
        List<PanelStudentProjection> reversed = List.of(students.get(2), students.get(1), students.get(0));

        List<PanelDTOs.StudentStatus> result = service.buildStudentStatuses(institutionId, reversed);

        Assertions.assertEquals(List.of(12L, 11L, 10L), result.stream().map(PanelDTOs.StudentStatus::studentId).toList());
    }

    @Test
    void buildStudentStatusesShouldKeepDoubtDataOfStudent() {
        mockProgressData();
        Instant flaggedAt = Instant.parse("2026-10-05T12:00:00Z");
        List<PanelStudentProjection> anaInDoubt = List.of(new PanelStudentProjection(10L, "Ana", 1L, "3º ano A", true, flaggedAt));

        PanelDTOs.StudentStatus ana = service.buildStudentStatuses(institutionId, anaInDoubt).get(0);

        Assertions.assertTrue(ana.inDoubt());
        Assertions.assertEquals(flaggedAt, ana.doubtFlaggedAt());
    }

    @Test
    void buildStudentStatusesShouldReturnEmptyListWhenNoStudentsAreGiven() {
        mockProgressData();

        List<PanelDTOs.StudentStatus> result = service.buildStudentStatuses(institutionId, List.of());

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void summarizeShouldCountStudentsByJourneyStatusAndDoubt() {
        List<PanelDTOs.StudentStatus> statuses = List.of(
                createStudentStatus(1L, ProgressStatus.NAO_INICIADA, false),
                createStudentStatus(2L, ProgressStatus.EM_ANDAMENTO, false),
                createStudentStatus(3L, ProgressStatus.EM_ANDAMENTO, false),
                createStudentStatus(4L, ProgressStatus.CONCLUIDA, true),
                createStudentStatus(5L, ProgressStatus.CONCLUIDA, false));

        PanelDTOs.PanelSummary result = service.summarize(statuses);

        Assertions.assertEquals(5, result.totalStudents());
        Assertions.assertEquals(1, result.notStartedStudents());
        Assertions.assertEquals(2, result.inProgressStudents());
        Assertions.assertEquals(2, result.concludedStudents());
        Assertions.assertEquals(1, result.inDoubtStudents());
        Assertions.assertSame(statuses, result.students());
    }

    @Test
    void summarizeShouldReturnZeroedSummaryWhenThereAreNoStudents() {
        PanelDTOs.PanelSummary result = service.summarize(List.of());

        Assertions.assertEquals(0, result.totalStudents());
        Assertions.assertEquals(0, result.notStartedStudents());
        Assertions.assertEquals(0, result.inProgressStudents());
        Assertions.assertEquals(0, result.concludedStudents());
        Assertions.assertEquals(0, result.inDoubtStudents());
        Assertions.assertTrue(result.students().isEmpty());
    }
}
