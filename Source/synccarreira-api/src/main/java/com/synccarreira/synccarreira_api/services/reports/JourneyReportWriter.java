package com.synccarreira.synccarreira_api.services.reports;

import com.synccarreira.synccarreira_api.dto.SynthesisDTO;
import com.synccarreira.synccarreira_api.dto.psychologist.PanelDTOs;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportDTOs;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.ProgressStatus;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

// Monta as planilhas (xlsx) dos relatórios da jornada que a psicóloga baixa pelo painel
@Component
public class JourneyReportWriter {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static final String STUDENT = "Aluno";
    private static final String TRAIL = "Trilha";
    private static final String ANSWERED_QUESTIONS = "Perguntas respondidas";
    private static final String TOTAL_QUESTIONS = "Total de perguntas";
    private static final String PROGRESS = "Progresso";

    private static final int NAME_WIDTH = 32;
    private static final int CONTENT_WIDTH = 100;

    public byte[] writeClassReport(ReportDTOs.ClassReport report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            writeClassSummarySheet(workbook, styles, report);
            writeStudentsSheet(workbook, styles, report.students());
            writeTrailsSheet(workbook, styles, report.students(), true);
            writeSynthesesSheet(workbook, styles, report.students(), true);
            return toBytes(workbook);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o relatório da turma.", e);
        }
    }

    public byte[] writeStudentReport(ReportDTOs.StudentReport report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            writeStudentSheet(workbook, styles, report);
            writeTrailsSheet(workbook, styles, List.of(report), false);
            writeSynthesesSheet(workbook, styles, List.of(report), false);
            return toBytes(workbook);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o relatório do aluno.", e);
        }
    }

    private static void writeClassSummarySheet(Workbook workbook, Styles styles, ReportDTOs.ClassReport report) {
        SheetWriter writer = new SheetWriter(workbook.createSheet("Resumo"), styles);
        writer.keyValueHeader();
        writer.row("Instituição", report.institutionName());
        writer.row("Turma", report.schoolClassName());
        writer.row("Ano letivo", report.schoolYear());
        writer.row("Relatório gerado em", Instant.now());
        writer.row("Total de alunos", report.totalStudents());
        writer.row("Jornada não iniciada", report.notStartedStudents());
        writer.row("Jornada em andamento", report.inProgressStudents());
        writer.row("Jornada concluída", report.concludedStudents());
        writer.row("Alunos em dúvida sobre a escolha profissional", report.inDoubtStudents());
        writer.width(0, 46);
        writer.width(1, NAME_WIDTH);
    }

    private static void writeStudentSheet(Workbook workbook, Styles styles, ReportDTOs.StudentReport report) {
        PanelDTOs.StudentStatus status = report.status();
        SheetWriter writer = new SheetWriter(workbook.createSheet(STUDENT), styles);
        writer.keyValueHeader();
        writer.row("Nome", report.name());
        writer.row("E-mail", report.email());
        writer.row("Instituição", report.institutionName());
        writer.row("Turma", report.schoolClassName());
        writer.row("Ano letivo", report.schoolYear());
        writer.row("Ano de escolaridade", report.scholarYear());
        writer.row("Tipo de escola", report.schoolType());
        writer.row("Raça/cor", report.race());
        writer.row("Status da jornada", label(status.journeyStatus()));
        writer.row(PROGRESS, new Percentage(status.progressPercentage()));
        writer.row(ANSWERED_QUESTIONS, status.answeredQuestions());
        writer.row(TOTAL_QUESTIONS, status.totalQuestions());
        writer.row("Trilhas concluídas", status.concludedTrails());
        writer.row("Total de trilhas", status.totalTrails());
        writer.row("Trilha atual", label(status.currentTrail()));
        writer.row("Síntese final enviada", status.finalSynthesisSubmitted());
        writer.row("Em dúvida sobre a escolha profissional", status.inDoubt());
        writer.row("Dúvida sinalizada em", status.doubtFlaggedAt());
        writer.row("Pontuação em Humanas", report.humanitiesScore());
        writer.row("Pontuação em Exatas", report.exactSciencesScore());
        writer.row("Pontuação em Biológicas", report.biologicalSciencesScore());
        writer.row("Pontuação em Artes", report.artsScore());
        writer.row("Áreas de maior afinidade", labels(report.highestScoreAreas()));
        writer.row("Relatório gerado em", Instant.now());
        writer.width(0, 40);
        writer.width(1, NAME_WIDTH);
    }

    private static void writeStudentsSheet(Workbook workbook, Styles styles, List<ReportDTOs.StudentReport> students) {
        SheetWriter writer = new SheetWriter(workbook.createSheet("Alunos"), styles);
        writer.header(
                STUDENT, "E-mail", "Ano de escolaridade", "Tipo de escola", "Raça/cor",
                "Status da jornada", PROGRESS, ANSWERED_QUESTIONS, TOTAL_QUESTIONS,
                "Trilhas concluídas", "Total de trilhas", "Trilha atual", "Síntese final enviada",
                "Em dúvida", "Dúvida sinalizada em",
                "Pontuação em Humanas", "Pontuação em Exatas", "Pontuação em Biológicas", "Pontuação em Artes",
                "Áreas de maior afinidade");
        for (ReportDTOs.StudentReport student : students) {
            PanelDTOs.StudentStatus status = student.status();
            writer.row(
                    student.name(), student.email(), student.scholarYear(), student.schoolType(), student.race(),
                    label(status.journeyStatus()), new Percentage(status.progressPercentage()),
                    status.answeredQuestions(), status.totalQuestions(),
                    status.concludedTrails(), status.totalTrails(), label(status.currentTrail()), status.finalSynthesisSubmitted(),
                    status.inDoubt(), status.doubtFlaggedAt(),
                    student.humanitiesScore(), student.exactSciencesScore(), student.biologicalSciencesScore(), student.artsScore(),
                    labels(student.highestScoreAreas()));
        }
        writer.width(0, NAME_WIDTH);
        writer.width(1, NAME_WIDTH);
        writer.finishTable();
    }

    // Progresso de cada aluno em cada trilha de perguntas
    private static void writeTrailsSheet(Workbook workbook, Styles styles, List<ReportDTOs.StudentReport> students, boolean withStudentColumn) {
        SheetWriter writer = new SheetWriter(workbook.createSheet("Progresso por trilha"), styles);
        writer.header(titles(withStudentColumn,
                TRAIL, "Ordem", ANSWERED_QUESTIONS, TOTAL_QUESTIONS, PROGRESS, "Síntese enviada", "Status"));
        for (ReportDTOs.StudentReport student : students) {
            for (PanelDTOs.TrailProgress trail : student.status().trails()) {
                writer.row(values(withStudentColumn, student.name(),
                        label(trail.trailName()), trail.sequentialOrder(), trail.answeredQuestions(), trail.totalQuestions(),
                        new Percentage(trail.progressPercentage()), trail.synthesisSubmitted(), label(trail.status())));
            }
        }
        if (withStudentColumn) {
            writer.width(0, NAME_WIDTH);
        }
        writer.finishTable();
    }

    private static void writeSynthesesSheet(Workbook workbook, Styles styles, List<ReportDTOs.StudentReport> students, boolean withStudentColumn) {
        SheetWriter writer = new SheetWriter(workbook.createSheet("Sínteses"), styles);
        writer.header(titles(withStudentColumn, TRAIL, "Tipo", "Enviada em", "Conteúdo"));
        for (ReportDTOs.StudentReport student : students) {
            for (SynthesisDTO synthesis : student.syntheses()) {
                writer.row(values(withStudentColumn, student.name(),
                        label(synthesis.trailName()),
                        synthesis.journeySynthesis() ? "Síntese final da jornada" : "Síntese da trilha",
                        synthesis.createdAt(),
                        new WrappedText(synthesis.content())));
            }
        }
        int offset = withStudentColumn ? 1 : 0;
        if (withStudentColumn) {
            writer.width(0, NAME_WIDTH);
        }
        writer.width(offset + 1, 26);
        writer.width(offset + 3, CONTENT_WIDTH);
        writer.finishTable();
    }

    // No relatório da turma, as tabelas por trilha e de sínteses ganham a coluna do aluno
    private static String[] titles(boolean withStudentColumn, String... titles) {
        if (!withStudentColumn) {
            return titles;
        }
        String[] result = new String[titles.length + 1];
        result[0] = STUDENT;
        System.arraycopy(titles, 0, result, 1, titles.length);
        return result;
    }

    private static Object[] values(boolean withStudentColumn, String studentName, Object... values) {
        if (!withStudentColumn) {
            return values;
        }
        Object[] result = new Object[values.length + 1];
        result[0] = studentName;
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    private static byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    private static String label(ProgressStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case NAO_INICIADA -> "Não iniciada";
            case EM_ANDAMENTO -> "Em andamento";
            case CONCLUIDA -> "Concluída";
        };
    }

    private static String label(TrailName trailName) {
        if (trailName == null) {
            return null;
        }
        return switch (trailName) {
            case AUTOCONHECIMENTO -> "Autoconhecimento";
            case INFLUENCIAS -> "Influências";
            case PLANO_DE_FUTURO -> "Plano de futuro";
            case INFORMACAO -> "Informação";
        };
    }

    private static String labels(List<KnowledgeArea> areas) {
        if (areas == null || areas.isEmpty()) {
            return null;
        }
        return areas.stream()
                .map(area -> switch (area) {
                    case HUMANAS -> "Humanas";
                    case BIOLOGICAS -> "Biológicas";
                    case EXATAS -> "Exatas";
                    case ARTES -> "Artes";
                })
                .collect(Collectors.joining(", "));
    }

    // Percentual de 0 a 100, gravado na célula como fração com formato de porcentagem
    private record Percentage(int value) {
    }

    // Texto longo, gravado com quebra de linha automática
    private record WrappedText(String value) {
    }

    private static final class Styles {

        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle wrappedText;
        private final CellStyle integer;
        private final CellStyle decimal;
        private final CellStyle percentage;
        private final CellStyle dateTime;

        private Styles(Workbook workbook) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            header = workbook.createCellStyle();
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);
            header.setVerticalAlignment(VerticalAlignment.CENTER);

            text = bodyStyle(workbook);

            wrappedText = bodyStyle(workbook);
            wrappedText.setWrapText(true);

            integer = bodyStyle(workbook);
            integer.setDataFormat(workbook.createDataFormat().getFormat("0"));

            decimal = bodyStyle(workbook);
            decimal.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            percentage = bodyStyle(workbook);
            percentage.setDataFormat(workbook.createDataFormat().getFormat("0%"));

            dateTime = bodyStyle(workbook);
            dateTime.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        }

        private static CellStyle bodyStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setVerticalAlignment(VerticalAlignment.TOP);
            return style;
        }
    }

    private static final class SheetWriter {

        private static final int MIN_WIDTH = 14;

        private final Sheet sheet;
        private final Styles styles;
        private int rowIndex;
        private int columns;

        private SheetWriter(Sheet sheet, Styles styles) {
            this.sheet = sheet;
            this.styles = styles;
        }

        private void keyValueHeader() {
            header("Campo", "Valor");
        }

        private void header(String... titles) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < titles.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(titles[i]);
                cell.setCellStyle(styles.header);
                // A largura não é calculada automaticamente porque isso depende das fontes instaladas no servidor
                width(i, Math.max(titles[i].length() + 4, MIN_WIDTH));
            }
            columns = titles.length;
            sheet.createFreezePane(0, 1);
        }

        private void row(Object... values) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < values.length; i++) {
                write(row.createCell(i), values[i]);
            }
        }

        private void width(int column, int characters) {
            sheet.setColumnWidth(column, characters * 256);
        }

        // Habilita o filtro nas colunas da tabela
        private void finishTable() {
            if (rowIndex > 1) {
                sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, columns - 1));
            }
        }

        private void write(Cell cell, Object value) {
            switch (value) {
                case null -> {
                    cell.setCellValue("-");
                    cell.setCellStyle(styles.text);
                }
                case String text -> {
                    cell.setCellValue(text.isBlank() ? "-" : text);
                    cell.setCellStyle(styles.text);
                }
                case WrappedText(String text) -> {
                    cell.setCellValue(text);
                    cell.setCellStyle(styles.wrappedText);
                }
                case Boolean flag -> {
                    cell.setCellValue(Boolean.TRUE.equals(flag) ? "Sim" : "Não");
                    cell.setCellStyle(styles.text);
                }
                case Instant instant -> {
                    cell.setCellValue(LocalDateTime.ofInstant(instant, ZONE));
                    cell.setCellStyle(styles.dateTime);
                }
                case Percentage(int percentage) -> {
                    cell.setCellValue(percentage / 100.0);
                    cell.setCellStyle(styles.percentage);
                }
                case Double number -> {
                    cell.setCellValue(number);
                    cell.setCellStyle(styles.decimal);
                }
                case Number number -> {
                    cell.setCellValue(number.doubleValue());
                    cell.setCellStyle(styles.integer);
                }
                default -> {
                    cell.setCellValue(value.toString());
                    cell.setCellStyle(styles.text);
                }
            }
        }
    }
}
