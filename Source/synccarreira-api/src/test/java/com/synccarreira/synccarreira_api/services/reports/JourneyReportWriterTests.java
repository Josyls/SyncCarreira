package com.synccarreira.synccarreira_api.services.reports;

import com.synccarreira.synccarreira_api.dto.SynthesisDTO;
import com.synccarreira.synccarreira_api.dto.psychologist.PanelDTOs;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportDTOs;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.ProgressStatus;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class JourneyReportWriterTests {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    // 12:00 em UTC corresponde a 09:00 no horário de Brasília
    private static final Instant DOUBT_FLAGGED_AT = Instant.parse("2026-10-05T12:00:00Z");
    private static final LocalDateTime DOUBT_FLAGGED_AT_LOCAL = LocalDateTime.of(2026, 10, 5, 9, 0);
    private static final Instant SYNTHESIS_CREATED_AT = Instant.parse("2026-09-20T18:30:00Z");
    private static final LocalDateTime SYNTHESIS_CREATED_AT_LOCAL = LocalDateTime.of(2026, 9, 20, 15, 30);

    private JourneyReportWriter writer;

    private ReportDTOs.StudentReport ana, bruno, carla;
    private ReportDTOs.ClassReport classReport;

    private Workbook workbook;

    @BeforeEach
    void setUp() {
        writer = new JourneyReportWriter();

        // Ana concluiu a jornada e está em dúvida, com empate nas quatro áreas
        ana = createStudentReport(
                10L, "Ana Júlia", "Parda", "Pública",
                new PanelDTOs.StudentStatus(10L, "Ana Júlia", 1L, "3º ano A", 12, 12, 100, 3, 3,
                        null, true, ProgressStatus.CONCLUIDA, true, DOUBT_FLAGGED_AT, List.of(
                                trailProgress(1L, TrailName.AUTOCONHECIMENTO, 1, 5, 5, true),
                                trailProgress(2L, TrailName.INFLUENCIAS, 2, 4, 4, true),
                                trailProgress(3L, TrailName.PLANO_DE_FUTURO, 3, 3, 3, true))),
                9.5, 9.5, 9.5, 9.5,
                List.of(KnowledgeArea.HUMANAS, KnowledgeArea.BIOLOGICAS, KnowledgeArea.EXATAS, KnowledgeArea.ARTES),
                List.of(
                        synthesis(1L, TrailName.AUTOCONHECIMENTO, 1, "Síntese de autoconhecimento"),
                        synthesis(2L, TrailName.INFLUENCIAS, 2, "Síntese de influências"),
                        synthesis(3L, TrailName.PLANO_DE_FUTURO, 3, "Síntese do plano de futuro"),
                        synthesis(4L, TrailName.INFORMACAO, 4, "Síntese final da jornada")));

        // Bruno está no meio da segunda trilha
        bruno = createStudentReport(
                11L, "Bruno Lima", "Branca", "Particular",
                new PanelDTOs.StudentStatus(11L, "Bruno Lima", 1L, "3º ano A", 7, 12, 58, 1, 3,
                        TrailName.INFLUENCIAS, false, ProgressStatus.EM_ANDAMENTO, false, null, List.of(
                                trailProgress(1L, TrailName.AUTOCONHECIMENTO, 1, 5, 5, true),
                                trailProgress(2L, TrailName.INFLUENCIAS, 2, 2, 4, false),
                                trailProgress(3L, TrailName.PLANO_DE_FUTURO, 3, 0, 3, false))),
                3.0, 12.25, 1.5, 0.0,
                List.of(KnowledgeArea.EXATAS),
                List.of(synthesis(5L, TrailName.AUTOCONHECIMENTO, 1, "Síntese do Bruno")));

        // Carla não começou: sem pontuação, sem raça informada e com tipo de escola em branco
        carla = createStudentReport(
                12L, "Carla Dias", null, " ",
                new PanelDTOs.StudentStatus(12L, "Carla Dias", 1L, "3º ano A", 0, 12, 0, 0, 3,
                        TrailName.AUTOCONHECIMENTO, false, ProgressStatus.NAO_INICIADA, false, null, List.of(
                                trailProgress(1L, TrailName.AUTOCONHECIMENTO, 1, 0, 5, false),
                                trailProgress(2L, TrailName.INFLUENCIAS, 2, 0, 4, false),
                                trailProgress(3L, TrailName.PLANO_DE_FUTURO, 3, 0, 3, false))),
                0.0, 0.0, 0.0, 0.0,
                List.of(),
                List.of());

        classReport = new ReportDTOs.ClassReport(1L, "3º ano A", 2026, "E.E. Pedro II", 3, 1, 1, 1, 1, List.of(ana, bruno, carla));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (workbook != null) {
            workbook.close();
        }
    }

    private static ReportDTOs.StudentReport createStudentReport(
            Long id, String name, String race, String schoolType, PanelDTOs.StudentStatus status,
            Double humanities, Double exactSciences, Double biologicalSciences, Double arts,
            List<KnowledgeArea> highestScoreAreas, List<SynthesisDTO> syntheses) {
        return new ReportDTOs.StudentReport(
                id, name, name.toLowerCase().replace(' ', '.') + "@gmail.com", "3º ano do Ensino Médio", schoolType, race,
                "3º ano A", 2026, "E.E. Pedro II",
                humanities, exactSciences, biologicalSciences, arts, highestScoreAreas, status, syntheses);
    }

    private static PanelDTOs.TrailProgress trailProgress(Long id, TrailName name, int order, long answered, long total, boolean synthesisSubmitted) {
        ProgressStatus status = answered == total && synthesisSubmitted
                ? ProgressStatus.CONCLUIDA
                : answered > 0 ? ProgressStatus.EM_ANDAMENTO : ProgressStatus.NAO_INICIADA;
        return new PanelDTOs.TrailProgress(id, name, order, answered, total, (int) (answered * 100 / total), synthesisSubmitted, status);
    }

    private static SynthesisDTO synthesis(Long trailId, TrailName trailName, int order, String content) {
        return new SynthesisDTO(trailId, content, trailId, trailName, order, trailName == TrailName.INFORMACAO, SYNTHESIS_CREATED_AT);
    }

    private Workbook read(byte[] content) throws IOException {
        workbook = new XSSFWorkbook(new ByteArrayInputStream(content));
        return workbook;
    }

    private static List<String> sheetNames(Workbook workbook) {
        return IntStream.range(0, workbook.getNumberOfSheets())
                .mapToObj(workbook::getSheetName)
                .toList();
    }

    private static List<String> headers(Sheet sheet) {
        Row header = sheet.getRow(0);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            result.add(header.getCell(i).getStringCellValue());
        }
        return result;
    }

    // Nas abas de campo/valor, retorna a célula de valor do campo informado
    private static Cell valueOf(Sheet sheet, String field) {
        for (Row row : sheet) {
            if (field.equals(row.getCell(0).getStringCellValue())) {
                return row.getCell(1);
            }
        }
        throw new AssertionError("Campo não encontrado na aba " + sheet.getSheetName() + ": " + field);
    }

    private static String text(Cell cell) {
        Assertions.assertEquals(CellType.STRING, cell.getCellType());
        return cell.getStringCellValue();
    }

    private static double number(Cell cell) {
        Assertions.assertEquals(CellType.NUMERIC, cell.getCellType());
        return cell.getNumericCellValue();
    }

    // Retorna o valor da célula: texto como String e número como Double
    private static Object cellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> cell.getNumericCellValue();
            default -> throw new AssertionError("Tipo de célula inesperado em " + cell.getAddress() + ": " + cell.getCellType());
        };
    }

    // Retorna os valores das células de from (inclusivo) a to (exclusivo)
    private static List<Object> values(Row row, int from, int to) {
        List<Object> result = new ArrayList<>();
        for (int i = from; i < to; i++) {
            result.add(cellValue(row.getCell(i)));
        }
        return result;
    }

    // Nas abas de campo/valor, retorna os valores dos campos informados
    private static Map<String, Object> values(Sheet sheet, Set<String> fields) {
        Map<String, Object> result = new HashMap<>();
        for (String field : fields) {
            result.put(field, cellValue(valueOf(sheet, field)));
        }
        return result;
    }

    private static String format(Cell cell) {
        return cell.getCellStyle().getDataFormatString();
    }

    private static LocalDateTime dateTime(Cell cell) {
        Assertions.assertTrue(DateUtil.isCellDateFormatted(cell));
        return cell.getLocalDateTimeCellValue();
    }

    private static void assertGeneratedNow(Cell cell) {
        ZonedDateTime generatedAt = dateTime(cell).atZone(ZONE);
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        Assertions.assertTrue(Duration.between(generatedAt, now).abs().toMinutes() < 1,
                "Data de geração inesperada: " + generatedAt);
    }

    private static String autoFilter(Sheet sheet) {
        XSSFSheet xssfSheet = (XSSFSheet) sheet;
        return xssfSheet.getCTWorksheet().isSetAutoFilter() ? xssfSheet.getCTWorksheet().getAutoFilter().getRef() : null;
    }

    @Test
    void writeClassReportShouldCreateSummaryStudentsTrailsAndSynthesesSheets() throws IOException {
        Workbook result = read(writer.writeClassReport(classReport));

        Assertions.assertEquals(List.of("Resumo", "Alunos", "Progresso por trilha", "Sínteses"), sheetNames(result));
    }

    @Test
    void writeClassReportShouldWriteClassDataAndStatusCountsInSummarySheet() throws IOException {
        Sheet summary = read(writer.writeClassReport(classReport)).getSheet("Resumo");

        Assertions.assertEquals(List.of("Campo", "Valor"), headers(summary));
        Assertions.assertEquals("E.E. Pedro II", text(valueOf(summary, "Instituição")));
        Assertions.assertEquals("3º ano A", text(valueOf(summary, "Turma")));
        Assertions.assertEquals(2026, number(valueOf(summary, "Ano letivo")));
        Assertions.assertEquals(3, number(valueOf(summary, "Total de alunos")));
        Assertions.assertEquals(1, number(valueOf(summary, "Jornada não iniciada")));
        Assertions.assertEquals(1, number(valueOf(summary, "Jornada em andamento")));
        Assertions.assertEquals(1, number(valueOf(summary, "Jornada concluída")));
        Assertions.assertEquals(1, number(valueOf(summary, "Alunos em dúvida sobre a escolha profissional")));
        assertGeneratedNow(valueOf(summary, "Relatório gerado em"));
    }

    @Test
    void writeClassReportShouldWriteOneRowPerStudentInStudentsSheet() throws IOException {
        Sheet students = read(writer.writeClassReport(classReport)).getSheet("Alunos");

        Assertions.assertEquals(List.of(
                "Aluno", "E-mail", "Ano de escolaridade", "Tipo de escola", "Raça/cor",
                "Status da jornada", "Progresso", "Perguntas respondidas", "Total de perguntas",
                "Trilhas concluídas", "Total de trilhas", "Trilha atual", "Síntese final enviada",
                "Em dúvida", "Dúvida sinalizada em",
                "Pontuação em Humanas", "Pontuação em Exatas", "Pontuação em Biológicas", "Pontuação em Artes",
                "Áreas de maior afinidade"), headers(students));
        Assertions.assertEquals(3, students.getLastRowNum());
        Assertions.assertEquals("Ana Júlia", text(students.getRow(1).getCell(0)));
        Assertions.assertEquals("Bruno Lima", text(students.getRow(2).getCell(0)));
        Assertions.assertEquals("Carla Dias", text(students.getRow(3).getCell(0)));
    }

    @Test
    void writeClassReportShouldWriteJourneyStatusOfStudentInStudentsSheet() throws IOException {
        Row brunoRow = read(writer.writeClassReport(classReport)).getSheet("Alunos").getRow(2);

        Assertions.assertEquals(List.of(
                "bruno.lima@gmail.com", "3º ano do Ensino Médio", "Particular", "Branca", "Em andamento",
                0.58, 7.0, 12.0, 1.0, 3.0, "Influências", "Não", "Não", "-"), values(brunoRow, 1, 15));
        Assertions.assertEquals("0%", format(brunoRow.getCell(6)));
    }

    @Test
    void writeClassReportShouldWriteScoresAndHighestScoreAreasInStudentsSheet() throws IOException {
        Sheet students = read(writer.writeClassReport(classReport)).getSheet("Alunos");
        Row anaRow = students.getRow(1);
        Row brunoRow = students.getRow(2);

        Assertions.assertEquals(3.0, number(brunoRow.getCell(15)));
        Assertions.assertEquals(12.25, number(brunoRow.getCell(16)));
        Assertions.assertEquals(1.5, number(brunoRow.getCell(17)));
        Assertions.assertEquals(0.0, number(brunoRow.getCell(18)));
        Assertions.assertEquals("0.00", format(brunoRow.getCell(16)));
        Assertions.assertEquals("Exatas", text(brunoRow.getCell(19)));
        Assertions.assertEquals("Humanas, Biológicas, Exatas, Artes", text(anaRow.getCell(19)));
    }

    @Test
    void writeClassReportShouldWriteConcludedJourneyAndDoubtInStudentsSheet() throws IOException {
        Row anaRow = read(writer.writeClassReport(classReport)).getSheet("Alunos").getRow(1);

        Assertions.assertEquals("Concluída", text(anaRow.getCell(5)));
        Assertions.assertEquals(1.0, number(anaRow.getCell(6)));
        // Quem concluiu a jornada não tem trilha atual
        Assertions.assertEquals("-", text(anaRow.getCell(11)));
        Assertions.assertEquals("Sim", text(anaRow.getCell(12)));
        Assertions.assertEquals("Sim", text(anaRow.getCell(13)));
        Assertions.assertEquals(DOUBT_FLAGGED_AT_LOCAL, dateTime(anaRow.getCell(14)));
        Assertions.assertEquals("dd/mm/yyyy hh:mm", format(anaRow.getCell(14)));
    }

    @Test
    void writeClassReportShouldWriteDashWhenValueIsMissingOrBlank() throws IOException {
        Row carlaRow = read(writer.writeClassReport(classReport)).getSheet("Alunos").getRow(3);

        Assertions.assertEquals("-", text(carlaRow.getCell(3)));
        Assertions.assertEquals("-", text(carlaRow.getCell(4)));
        Assertions.assertEquals("Não iniciada", text(carlaRow.getCell(5)));
        Assertions.assertEquals("Autoconhecimento", text(carlaRow.getCell(11)));
        Assertions.assertEquals("-", text(carlaRow.getCell(19)));
    }

    @Test
    void writeClassReportShouldWriteProgressOfEveryStudentInEveryTrail() throws IOException {
        Sheet trails = read(writer.writeClassReport(classReport)).getSheet("Progresso por trilha");

        Assertions.assertEquals(List.of(
                "Aluno", "Trilha", "Ordem", "Perguntas respondidas", "Total de perguntas", "Progresso", "Síntese enviada", "Status"),
                headers(trails));
        Assertions.assertEquals(9, trails.getLastRowNum());

        Row brunoInfluences = trails.getRow(5);
        Assertions.assertEquals("Bruno Lima", text(brunoInfluences.getCell(0)));
        Assertions.assertEquals("Influências", text(brunoInfluences.getCell(1)));
        Assertions.assertEquals(2, number(brunoInfluences.getCell(2)));
        Assertions.assertEquals(2, number(brunoInfluences.getCell(3)));
        Assertions.assertEquals(4, number(brunoInfluences.getCell(4)));
        Assertions.assertEquals(0.5, number(brunoInfluences.getCell(5)));
        Assertions.assertEquals("0%", format(brunoInfluences.getCell(5)));
        Assertions.assertEquals("Não", text(brunoInfluences.getCell(6)));
        Assertions.assertEquals("Em andamento", text(brunoInfluences.getCell(7)));

        Assertions.assertEquals("Concluída", text(trails.getRow(4).getCell(7)));
        Assertions.assertEquals("Plano de futuro", text(trails.getRow(6).getCell(1)));
        Assertions.assertEquals("Não iniciada", text(trails.getRow(6).getCell(7)));
    }

    @Test
    void writeClassReportShouldWriteEverySynthesisWithStudentName() throws IOException {
        Sheet syntheses = read(writer.writeClassReport(classReport)).getSheet("Sínteses");

        Assertions.assertEquals(List.of("Aluno", "Trilha", "Tipo", "Enviada em", "Conteúdo"), headers(syntheses));
        Assertions.assertEquals(5, syntheses.getLastRowNum());

        Row anaFirst = syntheses.getRow(1);
        Assertions.assertEquals("Ana Júlia", text(anaFirst.getCell(0)));
        Assertions.assertEquals("Autoconhecimento", text(anaFirst.getCell(1)));
        Assertions.assertEquals("Síntese da trilha", text(anaFirst.getCell(2)));
        Assertions.assertEquals(SYNTHESIS_CREATED_AT_LOCAL, dateTime(anaFirst.getCell(3)));
        Assertions.assertEquals("Síntese de autoconhecimento", text(anaFirst.getCell(4)));

        Row anaFinal = syntheses.getRow(4);
        Assertions.assertEquals("Informação", text(anaFinal.getCell(1)));
        Assertions.assertEquals("Síntese final da jornada", text(anaFinal.getCell(2)));

        Assertions.assertEquals("Bruno Lima", text(syntheses.getRow(5).getCell(0)));
    }

    @Test
    void writeClassReportShouldEnableFilterAndFreezeHeaderInTableSheets() throws IOException {
        Workbook result = read(writer.writeClassReport(classReport));

        Assertions.assertEquals("A1:T4", autoFilter(result.getSheet("Alunos")));
        Assertions.assertEquals("A1:H10", autoFilter(result.getSheet("Progresso por trilha")));
        Assertions.assertEquals("A1:E6", autoFilter(result.getSheet("Sínteses")));
        for (Sheet sheet : result) {
            Assertions.assertNotNull(sheet.getPaneInformation(), sheet.getSheetName());
            Assertions.assertTrue(sheet.getPaneInformation().isFreezePane(), sheet.getSheetName());
            Assertions.assertEquals(1, sheet.getPaneInformation().getHorizontalSplitPosition(), sheet.getSheetName());
        }
        Assertions.assertNull(autoFilter(result.getSheet("Resumo")));
    }

    @Test
    void writeClassReportShouldWriteHeaderInBoldAndSynthesisContentWithWrappedText() throws IOException {
        Workbook result = read(writer.writeClassReport(classReport));
        Sheet syntheses = result.getSheet("Sínteses");

        Cell header = syntheses.getRow(0).getCell(0);
        Assertions.assertTrue(result.getFontAt(header.getCellStyle().getFontIndex()).getBold());
        Assertions.assertTrue(syntheses.getRow(1).getCell(4).getCellStyle().getWrapText());
        Assertions.assertEquals(100 * 256, syntheses.getColumnWidth(4));
    }

    @Test
    void writeClassReportShouldWriteOnlyHeadersWhenClassHasNoStudents() throws IOException {
        ReportDTOs.ClassReport emptyClass = new ReportDTOs.ClassReport(1L, "3º ano A", 2026, "E.E. Pedro II", 0, 0, 0, 0, 0, List.of());

        Workbook result = read(writer.writeClassReport(emptyClass));

        Assertions.assertEquals(0, number(valueOf(result.getSheet("Resumo"), "Total de alunos")));
        for (String sheetName : List.of("Alunos", "Progresso por trilha", "Sínteses")) {
            Sheet sheet = result.getSheet(sheetName);
            Assertions.assertEquals(0, sheet.getLastRowNum(), sheetName);
            Assertions.assertNull(autoFilter(sheet), sheetName);
        }
    }

    @Test
    void writeStudentReportShouldCreateStudentTrailsAndSynthesesSheets() throws IOException {
        Workbook result = read(writer.writeStudentReport(bruno));

        Assertions.assertEquals(List.of("Aluno", "Progresso por trilha", "Sínteses"), sheetNames(result));
    }

    @Test
    void writeStudentReportShouldWriteStudentDataInStudentSheet() throws IOException {
        Sheet student = read(writer.writeStudentReport(bruno)).getSheet("Aluno");

        Map<String, Object> expected = Map.ofEntries(
                Map.entry("Nome", "Bruno Lima"),
                Map.entry("E-mail", "bruno.lima@gmail.com"),
                Map.entry("Instituição", "E.E. Pedro II"),
                Map.entry("Turma", "3º ano A"),
                Map.entry("Ano letivo", 2026.0),
                Map.entry("Ano de escolaridade", "3º ano do Ensino Médio"),
                Map.entry("Tipo de escola", "Particular"),
                Map.entry("Raça/cor", "Branca"),
                Map.entry("Pontuação em Humanas", 3.0),
                Map.entry("Pontuação em Exatas", 12.25),
                Map.entry("Pontuação em Biológicas", 1.5),
                Map.entry("Pontuação em Artes", 0.0),
                Map.entry("Áreas de maior afinidade", "Exatas"));

        Assertions.assertEquals(List.of("Campo", "Valor"), headers(student));
        Assertions.assertEquals(expected, values(student, expected.keySet()));
        assertGeneratedNow(valueOf(student, "Relatório gerado em"));
    }

    @Test
    void writeStudentReportShouldWriteJourneyStatusInStudentSheet() throws IOException {
        Sheet student = read(writer.writeStudentReport(bruno)).getSheet("Aluno");

        Assertions.assertEquals("Em andamento", text(valueOf(student, "Status da jornada")));
        Assertions.assertEquals(0.58, number(valueOf(student, "Progresso")), 0.0001);
        Assertions.assertEquals("0%", format(valueOf(student, "Progresso")));
        Assertions.assertEquals(7, number(valueOf(student, "Perguntas respondidas")));
        Assertions.assertEquals(12, number(valueOf(student, "Total de perguntas")));
        Assertions.assertEquals(1, number(valueOf(student, "Trilhas concluídas")));
        Assertions.assertEquals(3, number(valueOf(student, "Total de trilhas")));
        Assertions.assertEquals("Influências", text(valueOf(student, "Trilha atual")));
        Assertions.assertEquals("Não", text(valueOf(student, "Síntese final enviada")));
        Assertions.assertEquals("Não", text(valueOf(student, "Em dúvida sobre a escolha profissional")));
        Assertions.assertEquals("-", text(valueOf(student, "Dúvida sinalizada em")));
    }

    @Test
    void writeStudentReportShouldWriteDoubtDateInStudentSheetWhenStudentIsInDoubt() throws IOException {
        Sheet student = read(writer.writeStudentReport(ana)).getSheet("Aluno");

        Assertions.assertEquals("Sim", text(valueOf(student, "Em dúvida sobre a escolha profissional")));
        Assertions.assertEquals(DOUBT_FLAGGED_AT_LOCAL, dateTime(valueOf(student, "Dúvida sinalizada em")));
    }

    @Test
    void writeStudentReportShouldWriteTrailsAndSynthesesWithoutStudentColumn() throws IOException {
        Workbook result = read(writer.writeStudentReport(ana));
        Sheet trails = result.getSheet("Progresso por trilha");
        Sheet syntheses = result.getSheet("Sínteses");

        Assertions.assertEquals(List.of(
                "Trilha", "Ordem", "Perguntas respondidas", "Total de perguntas", "Progresso", "Síntese enviada", "Status"),
                headers(trails));
        Assertions.assertEquals(3, trails.getLastRowNum());
        Assertions.assertEquals("Autoconhecimento", text(trails.getRow(1).getCell(0)));
        Assertions.assertEquals("Sim", text(trails.getRow(1).getCell(5)));
        Assertions.assertEquals("A1:G4", autoFilter(trails));

        Assertions.assertEquals(List.of("Trilha", "Tipo", "Enviada em", "Conteúdo"), headers(syntheses));
        Assertions.assertEquals(4, syntheses.getLastRowNum());
        Assertions.assertEquals("Informação", text(syntheses.getRow(4).getCell(0)));
        Assertions.assertEquals("Síntese final da jornada", text(syntheses.getRow(4).getCell(1)));
        Assertions.assertEquals(SYNTHESIS_CREATED_AT_LOCAL, dateTime(syntheses.getRow(4).getCell(2)));
        Assertions.assertEquals("Síntese final da jornada", text(syntheses.getRow(4).getCell(3)));
        Assertions.assertEquals(100 * 256, syntheses.getColumnWidth(3));
    }

    @Test
    void writeStudentReportShouldKeepFullSynthesisContent() throws IOException {
        String longContent = "Gosto de entender como as pessoas pensam.\n".repeat(119);
        ReportDTOs.StudentReport report = createStudentReport(
                11L, "Bruno Lima", "Branca", "Particular", bruno.status(), 3.0, 12.25, 1.5, 0.0,
                List.of(KnowledgeArea.EXATAS), List.of(synthesis(1L, TrailName.AUTOCONHECIMENTO, 1, longContent)));

        Cell content = read(writer.writeStudentReport(report)).getSheet("Sínteses").getRow(1).getCell(3);

        Assertions.assertEquals(longContent, text(content));
        Assertions.assertTrue(content.getCellStyle().getWrapText());
    }

    @Test
    void writeStudentReportShouldWriteOnlyHeaderInSynthesesSheetWhenStudentHasNoSyntheses() throws IOException {
        Sheet syntheses = read(writer.writeStudentReport(carla)).getSheet("Sínteses");

        Assertions.assertEquals(0, syntheses.getLastRowNum());
        Assertions.assertNull(autoFilter(syntheses));
    }
}
