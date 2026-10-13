package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.psychologist.PanelDTOs;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportDTOs;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportFile;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Synthesis;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.ProgressStatus;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import com.synccarreira.synccarreira_api.projections.PanelStudentProjection;
import com.synccarreira.synccarreira_api.repositories.SchoolClassRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.services.reports.JourneyReportWriter;
import com.synccarreira.synccarreira_api.tests.InstitutionFactory;
import com.synccarreira.synccarreira_api.tests.SchoolClassFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import com.synccarreira.synccarreira_api.tests.SynthesisFactory;
import com.synccarreira.synccarreira_api.tests.TrailFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class PsychologistReportServiceTests {

    @InjectMocks
    private PsychologistReportService service;

    @Mock
    private PsychologistPanelService panelService;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SynthesisRepository synthesisRepository;

    @Mock
    private JourneyReportWriter reportWriter;

    @Captor
    private ArgumentCaptor<ReportDTOs.ClassReport> classReportCaptor;

    @Captor
    private ArgumentCaptor<ReportDTOs.StudentReport> studentReportCaptor;

    @Captor
    private ArgumentCaptor<List<PanelStudentProjection>> panelStudentsCaptor;

    private Long psychologistId, institutionId, schoolClassId, nonExistingSchoolClassId, nonExistingStudentId, studentId;
    private Institution institution;
    private SchoolClass schoolClass, otherInstitutionSchoolClass;
    private Student ana, bruno;
    private PanelDTOs.StudentStatus anaStatus, brunoStatus;
    private List<Synthesis> anaSyntheses;
    private byte[] content;

    @BeforeEach
    void setUp() {
        psychologistId = 1L;
        nonExistingSchoolClassId = 100L;
        nonExistingStudentId = 100L;
        content = new byte[] {1, 2, 3};

        institution = InstitutionFactory.createInstitution();
        institutionId = institution.getId();
        schoolClass = SchoolClassFactory.createSchoolClass();
        schoolClass.setInstitution(institution);
        schoolClassId = schoolClass.getId();

        Institution otherInstitution = InstitutionFactory.createInstitution();
        otherInstitution.setId(2L);
        otherInstitutionSchoolClass = new SchoolClass(2L, "1º ano B", 2026, Instant.now());
        otherInstitutionSchoolClass.setInstitution(otherInstitution);

        // Ana concluiu a jornada e está em dúvida; Bruno ainda não começou e não tem pontuação
        ana = StudentFactory.createStudent();
        ana.setDeterminedSchoolClass(schoolClass);
        ana.setInDoubt(true);
        ana.setDoubtFlaggedAt(Instant.parse("2026-10-05T12:00:00Z"));
        bruno = StudentFactory.createStudent();
        bruno.setId(2L);
        bruno.setName("Bruno Lima");
        bruno.setEmail("bruno.lima@gmail.com");
        bruno.setHumanitiesScore(0.0);
        bruno.setExactSciencesScore(0.0);
        bruno.setBiologicalSciencesScore(0.0);
        bruno.setArtsScore(0.0);
        bruno.setDeterminedSchoolClass(schoolClass);

        studentId = ana.getId();

        anaStatus = new PanelDTOs.StudentStatus(
                ana.getId(), ana.getName(), schoolClassId, schoolClass.getName(), 8, 8, 100, 3, 3,
                null, true, ProgressStatus.CONCLUIDA, true, ana.getDoubtFlaggedAt(), List.of());
        brunoStatus = new PanelDTOs.StudentStatus(
                bruno.getId(), bruno.getName(), schoolClassId, schoolClass.getName(), 0, 8, 0, 0, 3,
                TrailName.AUTOCONHECIMENTO, false, ProgressStatus.NAO_INICIADA, false, null, List.of());

        // As sínteses da fábrica pertencem ao aluno de id 1 (Ana)
        anaSyntheses = List.of(
                SynthesisFactory.createSynthesis(TrailFactory.createTrail()),
                SynthesisFactory.createSynthesis(SynthesisFactory.createInformationTrail()));
    }

    private static String today() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString();
    }

    private void mockClassReportData() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(schoolClassRepository.findById(schoolClassId)).thenReturn(Optional.of(schoolClass));
        Mockito.when(studentRepository.searchBySchoolClassId(schoolClassId)).thenReturn(List.of(ana, bruno));
        Mockito.when(synthesisRepository.findBySchoolClassId(schoolClassId)).thenReturn(anaSyntheses);
        // A ordem retornada é diferente da ordem dos alunos: o relatório deve associar o status pelo id
        Mockito.when(panelService.buildStudentStatuses(eq(institutionId), anyList())).thenReturn(List.of(brunoStatus, anaStatus));
        Mockito.when(panelService.summarize(anyList())).thenReturn(new PanelDTOs.PanelSummary(2, 1, 0, 1, 1, List.of(anaStatus, brunoStatus)));
        Mockito.when(reportWriter.writeClassReport(any())).thenReturn(content);
    }

    private void mockStudentReportData() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(studentRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
        Mockito.when(synthesisRepository.findByStudentId(ana.getId())).thenReturn(anaSyntheses);
        Mockito.when(panelService.buildStudentStatuses(eq(institutionId), anyList())).thenReturn(List.of(anaStatus));
        Mockito.when(reportWriter.writeStudentReport(any())).thenReturn(content);
    }

    @Test
    void generateClassReportShouldReturnXlsxFileWithClassNameAndSchoolYearInFileName() {
        mockClassReportData();

        ReportFile result = service.generateClassReport(psychologistId, schoolClassId);

        Assertions.assertEquals("relatorio-turma-3-ano-a-2026-" + today() + ".xlsx", result.getFileName());
        Assertions.assertArrayEquals(content, result.getContent());
    }

    @Test
    void generateClassReportShouldWriteClassDataAndSummary() {
        mockClassReportData();

        service.generateClassReport(psychologistId, schoolClassId);

        Mockito.verify(reportWriter).writeClassReport(classReportCaptor.capture());
        ReportDTOs.ClassReport report = classReportCaptor.getValue();
        Assertions.assertEquals(schoolClassId, report.schoolClassId());
        Assertions.assertEquals("3º ano A", report.schoolClassName());
        Assertions.assertEquals(2026, report.schoolYear());
        Assertions.assertEquals("E.E. Pedro II", report.institutionName());
        Assertions.assertEquals(2, report.totalStudents());
        Assertions.assertEquals(1, report.notStartedStudents());
        Assertions.assertEquals(0, report.inProgressStudents());
        Assertions.assertEquals(1, report.concludedStudents());
        Assertions.assertEquals(1, report.inDoubtStudents());
    }

    @Test
    void generateClassReportShouldSummarizeStatusesOfClassStudents() {
        mockClassReportData();

        service.generateClassReport(psychologistId, schoolClassId);

        Mockito.verify(panelService).summarize(List.of(anaStatus, brunoStatus));
    }

    @Test
    void generateClassReportShouldWriteEveryStudentWithStatusAndSyntheses() {
        mockClassReportData();

        service.generateClassReport(psychologistId, schoolClassId);

        Mockito.verify(reportWriter).writeClassReport(classReportCaptor.capture());
        List<ReportDTOs.StudentReport> students = classReportCaptor.getValue().students();
        Assertions.assertEquals(2, students.size());

        ReportDTOs.StudentReport anaReport = students.get(0);
        Assertions.assertEquals(ana.getId(), anaReport.studentId());
        Assertions.assertSame(anaStatus, anaReport.status());
        Assertions.assertEquals(2, anaReport.syntheses().size());
        Assertions.assertEquals(TrailName.AUTOCONHECIMENTO, anaReport.syntheses().get(0).trailName());
        Assertions.assertFalse(anaReport.syntheses().get(0).journeySynthesis());
        Assertions.assertTrue(anaReport.syntheses().get(1).journeySynthesis());

        ReportDTOs.StudentReport brunoReport = students.get(1);
        Assertions.assertEquals(bruno.getId(), brunoReport.studentId());
        Assertions.assertSame(brunoStatus, brunoReport.status());
        Assertions.assertTrue(brunoReport.syntheses().isEmpty());
    }

    @Test
    void generateClassReportShouldCalculateStatusesFromClassStudents() {
        mockClassReportData();

        service.generateClassReport(psychologistId, schoolClassId);

        Mockito.verify(panelService).buildStudentStatuses(eq(institutionId), panelStudentsCaptor.capture());
        List<PanelStudentProjection> panelStudents = panelStudentsCaptor.getValue();
        Assertions.assertEquals(2, panelStudents.size());
        PanelStudentProjection anaProjection = panelStudents.get(0);
        Assertions.assertEquals(ana.getId(), anaProjection.studentId());
        Assertions.assertEquals(ana.getName(), anaProjection.studentName());
        Assertions.assertEquals(schoolClassId, anaProjection.schoolClassId());
        Assertions.assertEquals(schoolClass.getName(), anaProjection.schoolClassName());
        Assertions.assertTrue(anaProjection.inDoubt());
        Assertions.assertEquals(ana.getDoubtFlaggedAt(), anaProjection.doubtFlaggedAt());
        Assertions.assertEquals(bruno.getId(), panelStudents.get(1).studentId());
        Assertions.assertFalse(panelStudents.get(1).inDoubt());
    }

    @Test
    void generateClassReportShouldReturnHighestScoreAreasOnlyForStudentsWithScore() {
        mockClassReportData();

        service.generateClassReport(psychologistId, schoolClassId);

        Mockito.verify(reportWriter).writeClassReport(classReportCaptor.capture());
        List<ReportDTOs.StudentReport> students = classReportCaptor.getValue().students();
        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), students.get(0).highestScoreAreas());
        // Sem pontuação, as quatro áreas empatariam em zero
        Assertions.assertTrue(students.get(1).highestScoreAreas().isEmpty());
    }

    @Test
    void generateClassReportShouldUseLegalNameWhenInstitutionHasNoTradeName() {
        institution.setTradeName(" ");
        mockClassReportData();

        service.generateClassReport(psychologistId, schoolClassId);

        Mockito.verify(reportWriter).writeClassReport(classReportCaptor.capture());
        Assertions.assertEquals("Escola Estadual Pedro II", classReportCaptor.getValue().institutionName());
        Assertions.assertEquals("Escola Estadual Pedro II", classReportCaptor.getValue().students().get(0).institutionName());
    }

    @Test
    void generateClassReportShouldReturnEmptyReportWhenClassHasNoStudents() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(schoolClassRepository.findById(schoolClassId)).thenReturn(Optional.of(schoolClass));
        Mockito.when(studentRepository.searchBySchoolClassId(schoolClassId)).thenReturn(List.of());
        Mockito.when(synthesisRepository.findBySchoolClassId(schoolClassId)).thenReturn(List.of());
        Mockito.when(panelService.buildStudentStatuses(institutionId, List.of())).thenReturn(List.of());
        Mockito.when(panelService.summarize(List.of())).thenReturn(new PanelDTOs.PanelSummary(0, 0, 0, 0, 0, List.of()));
        Mockito.when(reportWriter.writeClassReport(any())).thenReturn(content);

        service.generateClassReport(psychologistId, schoolClassId);

        Mockito.verify(reportWriter).writeClassReport(classReportCaptor.capture());
        Assertions.assertEquals(0, classReportCaptor.getValue().totalStudents());
        Assertions.assertTrue(classReportCaptor.getValue().students().isEmpty());
    }

    @Test
    void generateClassReportShouldThrowResourceNotFoundExceptionWhenClassDoesNotExist() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(schoolClassRepository.findById(nonExistingSchoolClassId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.generateClassReport(psychologistId, nonExistingSchoolClassId));
        Mockito.verifyNoInteractions(studentRepository, reportWriter);
    }

    @Test
    void generateClassReportShouldThrowForbiddenExceptionWhenClassBelongsToAnotherInstitution() {
        Long otherClassId = otherInstitutionSchoolClass.getId();
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(schoolClassRepository.findById(otherClassId)).thenReturn(Optional.of(otherInstitutionSchoolClass));

        Assertions.assertThrows(ForbiddenException.class, () -> service.generateClassReport(psychologistId, otherClassId));
        Mockito.verifyNoInteractions(studentRepository, synthesisRepository, reportWriter);
    }

    @Test
    void generateClassReportShouldThrowForbiddenExceptionWhenClassHasNoInstitution() {
        schoolClass.setInstitution(null);
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(schoolClassRepository.findById(schoolClassId)).thenReturn(Optional.of(schoolClass));

        Assertions.assertThrows(ForbiddenException.class, () -> service.generateClassReport(psychologistId, schoolClassId));
        Mockito.verifyNoInteractions(studentRepository, synthesisRepository, reportWriter);
    }

    @Test
    void generateClassReportShouldThrowForbiddenExceptionWhenLoggedUserCannotAccessPanel() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenThrow(ForbiddenException.class);

        Assertions.assertThrows(ForbiddenException.class, () -> service.generateClassReport(psychologistId, schoolClassId));
        Mockito.verifyNoInteractions(schoolClassRepository, studentRepository, reportWriter);
    }

    @Test
    void generateClassReportShouldThrowBusinessExceptionWhenPsychologistHasNoInstitution() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenThrow(BusinessException.class);

        Assertions.assertThrows(BusinessException.class, () -> service.generateClassReport(psychologistId, schoolClassId));
        Mockito.verifyNoInteractions(schoolClassRepository, studentRepository, reportWriter);
    }

    @Test
    void generateStudentReportShouldReturnXlsxFileWithStudentNameInFileName() {
        mockStudentReportData();

        ReportFile result = service.generateStudentReport(psychologistId, ana.getId());

        Assertions.assertEquals("relatorio-aluno-ana-souza-" + today() + ".xlsx", result.getFileName());
        Assertions.assertArrayEquals(content, result.getContent());
    }

    @Test
    void generateStudentReportShouldRemoveAccentsAndSpecialCharactersFromFileName() {
        ana.setName("  João D'Ávila  ");
        mockStudentReportData();

        ReportFile result = service.generateStudentReport(psychologistId, ana.getId());

        Assertions.assertEquals("relatorio-aluno-joao-d-avila-" + today() + ".xlsx", result.getFileName());
    }

    @Test
    void generateStudentReportShouldWriteStudentDataStatusAndSyntheses() {
        mockStudentReportData();

        service.generateStudentReport(psychologistId, ana.getId());

        Mockito.verify(reportWriter).writeStudentReport(studentReportCaptor.capture());
        ReportDTOs.StudentReport report = studentReportCaptor.getValue();
        Assertions.assertEquals(ana.getId(), report.studentId());
        Assertions.assertEquals("Ana Souza", report.name());
        Assertions.assertEquals("ana.souza@gmail.com", report.email());
        Assertions.assertEquals("3º ano do Ensino Médio", report.scholarYear());
        Assertions.assertEquals("Pública", report.schoolType());
        Assertions.assertEquals("Parda", report.race());
        Assertions.assertEquals("3º ano A", report.schoolClassName());
        Assertions.assertEquals(2026, report.schoolYear());
        Assertions.assertEquals("E.E. Pedro II", report.institutionName());
        Assertions.assertEquals(7.5, report.humanitiesScore());
        Assertions.assertEquals(6.0, report.exactSciencesScore());
        Assertions.assertEquals(8.25, report.biologicalSciencesScore());
        Assertions.assertEquals(5.0, report.artsScore());
        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), report.highestScoreAreas());
        Assertions.assertSame(anaStatus, report.status());
        Assertions.assertEquals(2, report.syntheses().size());
        Assertions.assertEquals(SynthesisFactory.CONTENT, report.syntheses().get(0).content());
        Assertions.assertTrue(report.syntheses().get(1).journeySynthesis());
    }

    @Test
    void generateStudentReportShouldCalculateStatusOnlyOfTheStudent() {
        mockStudentReportData();

        service.generateStudentReport(psychologistId, ana.getId());

        Mockito.verify(panelService).buildStudentStatuses(eq(institutionId), panelStudentsCaptor.capture());
        List<PanelStudentProjection> panelStudents = panelStudentsCaptor.getValue();
        Assertions.assertEquals(1, panelStudents.size());
        Assertions.assertEquals(ana.getId(), panelStudents.get(0).studentId());
    }

    @Test
    void generateStudentReportShouldReturnEmptySynthesesWhenStudentHasNotSubmittedAny() {
        mockStudentReportData();
        Mockito.when(synthesisRepository.findByStudentId(ana.getId())).thenReturn(List.of());

        service.generateStudentReport(psychologistId, ana.getId());

        Mockito.verify(reportWriter).writeStudentReport(studentReportCaptor.capture());
        Assertions.assertTrue(studentReportCaptor.getValue().syntheses().isEmpty());
    }

    @Test
    void generateStudentReportShouldThrowResourceNotFoundExceptionWhenStudentDoesNotExist() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(studentRepository.findById(nonExistingStudentId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.generateStudentReport(psychologistId, nonExistingStudentId));
        Mockito.verifyNoInteractions(synthesisRepository, reportWriter);
    }

    @Test
    void generateStudentReportShouldThrowForbiddenExceptionWhenStudentBelongsToAnotherInstitution() {
        ana.setDeterminedSchoolClass(otherInstitutionSchoolClass);
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(studentRepository.findById(ana.getId())).thenReturn(Optional.of(ana));

        Assertions.assertThrows(ForbiddenException.class, () -> service.generateStudentReport(psychologistId, studentId));
        Mockito.verifyNoInteractions(synthesisRepository, reportWriter);
    }

    @Test
    void generateStudentReportShouldThrowForbiddenExceptionWhenStudentHasNoSchoolClass() {
        ana.setDeterminedSchoolClass(null);
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenReturn(institutionId);
        Mockito.when(studentRepository.findById(ana.getId())).thenReturn(Optional.of(ana));

        Assertions.assertThrows(ForbiddenException.class, () -> service.generateStudentReport(psychologistId, studentId));
        Mockito.verifyNoInteractions(synthesisRepository, reportWriter);
    }

    @Test
    void generateStudentReportShouldThrowForbiddenExceptionWhenLoggedUserCannotAccessPanel() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenThrow(ForbiddenException.class);

        Assertions.assertThrows(ForbiddenException.class, () -> service.generateStudentReport(psychologistId, studentId));
        Mockito.verifyNoInteractions(studentRepository, synthesisRepository, reportWriter);
    }

    @Test
    void generateStudentReportShouldThrowResourceNotFoundExceptionWhenPsychologistDoesNotExist() {
        Mockito.when(panelService.findAccessibleInstitutionId(psychologistId)).thenThrow(ResourceNotFoundException.class);

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.generateStudentReport(psychologistId, studentId));
        Mockito.verifyNoInteractions(studentRepository, synthesisRepository, reportWriter);
    }
}
