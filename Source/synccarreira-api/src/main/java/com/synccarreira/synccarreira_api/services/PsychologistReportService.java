package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.SynthesisDTO;
import com.synccarreira.synccarreira_api.dto.psychologist.PanelDTOs;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportDTOs;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportFile;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.projections.PanelStudentProjection;
import com.synccarreira.synccarreira_api.repositories.SchoolClassRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.services.reports.JourneyReportWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PsychologistReportService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final PsychologistPanelService panelService;

    private final SchoolClassRepository schoolClassRepository;

    private final StudentRepository studentRepository;

    private final SynthesisRepository synthesisRepository;

    private final JourneyReportWriter reportWriter;

    public PsychologistReportService(
            final PsychologistPanelService panelService,
            final SchoolClassRepository schoolClassRepository,
            final StudentRepository studentRepository,
            final SynthesisRepository synthesisRepository,
            final JourneyReportWriter reportWriter) {
        this.panelService = panelService;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.synthesisRepository = synthesisRepository;
        this.reportWriter = reportWriter;
    }

    @Transactional(readOnly = true)
    public ReportFile generateClassReport(Long psychologistId, Long schoolClassId) {
        Long institutionId = panelService.findAccessibleInstitutionId(psychologistId);
        SchoolClass schoolClass = schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada. ID: " + schoolClassId));
        validateInstitution(schoolClass, institutionId, "Acesso negado: a turma não pertence à instituição da psicóloga.");

        List<Student> students = studentRepository.searchBySchoolClassId(schoolClassId);
        Map<Long, List<SynthesisDTO>> synthesesByStudent = synthesisRepository.findBySchoolClassId(schoolClassId)
                .stream()
                .collect(Collectors.groupingBy(
                        synthesis -> synthesis.getStudent().getId(),
                        Collectors.mapping(SynthesisDTO::new, Collectors.toList())));

        List<ReportDTOs.StudentReport> studentReports = buildStudentReports(institutionId, students, synthesesByStudent);
        PanelDTOs.PanelSummary summary = panelService.summarize(studentReports.stream()
                .map(ReportDTOs.StudentReport::status)
                .toList());

        ReportDTOs.ClassReport report = new ReportDTOs.ClassReport(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getSchoolYear(),
                institutionName(schoolClass.getInstitution()),
                summary.totalStudents(),
                summary.notStartedStudents(),
                summary.inProgressStudents(),
                summary.concludedStudents(),
                summary.inDoubtStudents(),
                studentReports);
        return new ReportFile(
                fileName("turma", schoolClass.getName(), String.valueOf(schoolClass.getSchoolYear())),
                reportWriter.writeClassReport(report));
    }

    @Transactional(readOnly = true)
    public ReportFile generateStudentReport(Long psychologistId, Long studentId) {
        Long institutionId = panelService.findAccessibleInstitutionId(psychologistId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + studentId));
        validateInstitution(student.getDeterminedSchoolClass(), institutionId, "Acesso negado: o aluno não pertence à instituição da psicóloga.");

        List<SynthesisDTO> syntheses = synthesisRepository.findByStudentId(studentId)
                .stream()
                .map(SynthesisDTO::new)
                .toList();

        ReportDTOs.StudentReport report = buildStudentReports(institutionId, List.of(student), Map.of(studentId, syntheses)).getFirst();
        return new ReportFile(
                fileName("aluno", student.getName()),
                reportWriter.writeStudentReport(report));
    }

    private List<ReportDTOs.StudentReport> buildStudentReports(
            Long institutionId,
            List<Student> students,
            Map<Long, List<SynthesisDTO>> synthesesByStudent) {
        List<PanelStudentProjection> panelStudents = students.stream()
                .map(student -> new PanelStudentProjection(
                        student.getId(),
                        student.getName(),
                        student.getDeterminedSchoolClass().getId(),
                        student.getDeterminedSchoolClass().getName(),
                        student.getInDoubt(),
                        student.getDoubtFlaggedAt()))
                .toList();
        Map<Long, PanelDTOs.StudentStatus> statusByStudent = panelService.buildStudentStatuses(institutionId, panelStudents)
                .stream()
                .collect(Collectors.toMap(PanelDTOs.StudentStatus::studentId, Function.identity()));

        return students.stream()
                .map(student -> toStudentReport(
                        student,
                        statusByStudent.get(student.getId()),
                        synthesesByStudent.getOrDefault(student.getId(), List.of())))
                .toList();
    }

    private static ReportDTOs.StudentReport toStudentReport(Student student, PanelDTOs.StudentStatus status, List<SynthesisDTO> syntheses) {
        SchoolClass schoolClass = student.getDeterminedSchoolClass();
        return new ReportDTOs.StudentReport(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getScholarYear(),
                student.getSchoolType(),
                student.getRace(),
                schoolClass.getName(),
                schoolClass.getSchoolYear(),
                institutionName(schoolClass.getInstitution()),
                student.getHumanitiesScore(),
                student.getExactSciencesScore(),
                student.getBiologicalSciencesScore(),
                student.getArtsScore(),
                highestScoreAreas(student),
                status,
                syntheses);
    }

    // Sem nenhuma pontuação, todas as áreas empatariam em zero: nesse caso o aluno ainda não tem área de afinidade
    private static List<KnowledgeArea> highestScoreAreas(Student student) {
        boolean hasScore = Stream.of(
                        student.getHumanitiesScore(),
                        student.getExactSciencesScore(),
                        student.getBiologicalSciencesScore(),
                        student.getArtsScore())
                .anyMatch(score -> score != null && score > 0);
        return hasScore ? student.highestScoreAreas() : List.of();
    }

    private static void validateInstitution(SchoolClass schoolClass, Long institutionId, String message) {
        Institution institution = schoolClass != null ? schoolClass.getInstitution() : null;
        if (institution == null || !institution.getId().equals(institutionId)) {
            throw new ForbiddenException(message);
        }
    }

    private static String institutionName(Institution institution) {
        if (institution == null) {
            return null;
        }
        String tradeName = institution.getTradeName();
        return tradeName != null && !tradeName.isBlank() ? tradeName : institution.getLegalName();
    }

    // Ex.: relatorio-turma-3o-ano-a-2026-2026-10-08.xlsx
    private static String fileName(String type, String... parts) {
        StringBuilder name = new StringBuilder("relatorio-").append(type);
        for (String part : parts) {
            String slug = slug(part);
            if (!slug.isEmpty()) {
                name.append('-').append(slug);
            }
        }
        return name.append('-').append(LocalDate.now(ZONE)).append(".xlsx").toString();
    }

    private static String slug(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(?:^-)|(?:-$)", "");
    }
}
