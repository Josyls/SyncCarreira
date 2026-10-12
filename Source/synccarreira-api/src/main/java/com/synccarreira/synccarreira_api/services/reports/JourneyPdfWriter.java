package com.synccarreira.synccarreira_api.services.reports;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.dto.journey.JourneyContent;
import com.synccarreira.synccarreira_api.dto.journey.JourneyPdfData;
import com.synccarreira.synccarreira_api.dto.journey.JourneyRecordDTO;
import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.dto.result.JourneyResultDTO;
import com.synccarreira.synccarreira_api.dto.result.SubareaDTO;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// Gera o PDF de uma jornada concluída: perfil, perguntas e respostas de cada trilha, sínteses e resultado (RF-15)
@Component
public class JourneyPdfWriter {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZONE);
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONE);

    private static final Font TITLE = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITLE = new Font(Font.HELVETICA, 11, Font.ITALIC);
    private static final Font SECTION = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font BOLD = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font NOTE = new Font(Font.HELVETICA, 9, Font.ITALIC);

    private static final float MARGIN = 50f;
    private static final float CELL_PADDING = 6f;
    private static final float HEADER_GRAY = 0.9f;
    private static final String NOT_INFORMED = "Não informado";

    public byte[] write(JourneyPdfData data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        try {
            PdfWriter.getInstance(document, out);
            document.addTitle("Minha jornada nº " + data.journey().journeyNumber());
            document.addAuthor("SyncCarreira");
            HeaderFooter footer = new HeaderFooter(new Phrase("SyncCarreira · página ", NOTE), true);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setBorder(Rectangle.NO_BORDER);
            document.setFooter(footer);
            document.open();

            writeHeader(document, data);
            writeProfile(document, 1, data.journey().affinities());
            int section = 2;
            for (JourneyContent.TrailContent trail : data.content().trails()) {
                writeTrail(document, section, trail);
                section++;
            }
            writeLinks(document, data.content().links());
            writeResult(document, section, data.journey(), data.result());

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Não foi possível gerar o PDF da jornada.", e);
        }
        return out.toByteArray();
    }

    private static void writeHeader(Document document, JourneyPdfData data) {
        JourneyRecordDTO journey = data.journey();
        Paragraph title = new Paragraph("SyncCarreira — Minha jornada nº " + journey.journeyNumber(), TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        String period = journey.startedAt() != null
                ? "Iniciada em " + DATE.format(journey.startedAt()) + " · concluída em " + DATE.format(journey.concludedAt())
                : "Concluída em " + DATE.format(journey.concludedAt());
        Paragraph subtitle = new Paragraph(period, SUBTITLE);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(12f);
        document.add(subtitle);

        PdfPTable table = keyValueTable();
        addRow(table, "Aluno", data.studentName());
        addRow(table, "E-mail", data.studentEmail());
        addRow(table, "Ano escolar", orNotInformed(data.scholarYear()));
        addRow(table, "Turma", journey.schoolClassName() != null
                ? journey.schoolClassName() + (journey.schoolYear() != null ? " (" + journey.schoolYear() + ")" : "")
                : NOT_INFORMED);
        addRow(table, "Instituição", orNotInformed(journey.institutionName()));
        document.add(table);
    }

    private static void writeProfile(Document document, int section, StudentAffinityDTO affinities) {
        document.add(sectionTitle(section + ". Meu perfil"));
        document.add(new Paragraph("Afinidades que você declarou com cada área (de 0 a 5):", NORMAL));
        PdfPTable table = keyValueTable();
        addHeader(table, "Área", "Afinidade");
        addRow(table, label(KnowledgeArea.HUMANAS), valueOf(affinities.humanities()));
        addRow(table, label(KnowledgeArea.EXATAS), valueOf(affinities.exactSciences()));
        addRow(table, label(KnowledgeArea.BIOLOGICAS), valueOf(affinities.biologicalSciences()));
        addRow(table, label(KnowledgeArea.ARTES), valueOf(affinities.arts()));
        document.add(table);
    }

    private static void writeTrail(Document document, int section, JourneyContent.TrailContent trail) {
        document.add(sectionTitle(section + ". " + label(trail.trailName())));

        List<JourneyContent.AnswerContent> answers = trail.answers();
        for (int i = 0; i < answers.size(); i++) {
            JourneyContent.AnswerContent answer = answers.get(i);
            Paragraph question = new Paragraph((i + 1) + ". " + answer.question(), BOLD);
            question.setSpacingBefore(6f);
            document.add(question);
            document.add(new Paragraph("Resposta: " + (answer.answer() != null ? answer.answer() : "sem resposta registrada"), NORMAL));
        }

        String synthesisTitle = trail.trailName() == TrailName.INFORMACAO ? "Síntese final" : "Síntese";
        if (trail.synthesisCreatedAt() != null) {
            synthesisTitle += " (enviada em " + DATE.format(trail.synthesisCreatedAt()) + ")";
        }
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100f);
        box.setSpacingBefore(8f);
        PdfPCell header = cell(synthesisTitle, BOLD);
        header.setGrayFill(HEADER_GRAY);
        box.addCell(header);
        box.addCell(cell(orNotInformed(trail.synthesis()), NORMAL));
        document.add(box);
    }

    private static void writeLinks(Document document, List<JourneyContent.LinkContent> links) {
        if (links == null || links.isEmpty()) {
            return;
        }
        Paragraph title = new Paragraph("Links para continuar pesquisando", BOLD);
        title.setSpacingBefore(10f);
        document.add(title);
        for (JourneyContent.LinkContent link : links) {
            String area = link.knowledgeArea() != null ? " [" + label(link.knowledgeArea()) + "]" : "";
            document.add(new Paragraph("• " + link.topic() + area + ": " + link.url(), NORMAL));
        }
    }

    // RF-12: scores em percentual do máximo possível de cada área, área(s) em destaque e leque de subáreas sugeridas
    private static void writeResult(Document document, int section, JourneyRecordDTO journey, JourneyResultDTO result) {
        document.add(sectionTitle(section + ". Resultado"));
        document.add(new Paragraph("Percentual alcançado em cada área, considerando o máximo de pontos possível em cada uma:", NORMAL));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(4f);
        table.setWidths(new float[] { 2f, 1f, 1f });
        for (String title : List.of("Área", "Pontuação", "Percentual")) {
            PdfPCell header = cell(title, BOLD);
            header.setGrayFill(HEADER_GRAY);
            table.addCell(header);
        }
        for (AreaScoreDTO score : result.scores()) {
            Font font = score.highlighted() ? BOLD : NORMAL;
            String area = score.highlighted() ? score.areaName() + " (destaque)" : score.areaName();
            table.addCell(cell(area, font));
            table.addCell(cell(score(score.score()) + " de " + score(score.maxScore()), font));
            table.addCell(cell(score(score.percentage()) + "%", font));
        }
        document.add(table);

        List<KnowledgeArea> highlighted = result.highlightedAreas();
        String highest = highlighted == null || highlighted.isEmpty()
                ? "Ainda não há área em destaque."
                : (highlighted.size() == 1 ? "Área em destaque: " : "Áreas em destaque (empate): ")
                        + highlighted.stream().map(JourneyPdfWriter::label).collect(Collectors.joining(", ")) + ".";
        Paragraph highestParagraph = new Paragraph(highest, BOLD);
        highestParagraph.setSpacingBefore(8f);
        document.add(highestParagraph);

        if (journey.inDoubt()) {
            String when = journey.doubtFlaggedAt() != null ? " em " + DATE.format(journey.doubtFlaggedAt()) : "";
            document.add(new Paragraph("Ao final desta jornada, você sinalizou que ainda estava em dúvida sobre a escolha profissional" + when + ".", NORMAL));
        }

        writeSuggestions(document, result.suggestions());

        // RN-07: o resultado é um leque de possibilidades, nunca um diagnóstico fechado
        Paragraph disclaimer = new Paragraph(result.disclaimer(), NOTE);
        disclaimer.setSpacingBefore(12f);
        document.add(disclaimer);
    }

    private static void writeSuggestions(Document document, List<SubareaDTO> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }
        Paragraph title = new Paragraph("Leque de possibilidades", SECTION);
        title.setSpacingBefore(14f);
        title.setSpacingAfter(4f);
        document.add(title);
        document.add(new Paragraph("Subáreas da sua área em destaque, com carreiras e cursos que você pode pesquisar "
                + "(classificação baseada na Cine Brasil/Inep):", NORMAL));
        for (SubareaDTO subarea : suggestions) {
            Paragraph name = new Paragraph(subarea.name() + " (" + label(subarea.area()) + ")", BOLD);
            name.setSpacingBefore(8f);
            document.add(name);
            document.add(new Paragraph(subarea.description(), NORMAL));
            document.add(new Paragraph("Carreiras e cursos possíveis: " + String.join(", ", subarea.careers()) + ".", NORMAL));
        }
    }

    private static Paragraph sectionTitle(String text) {
        Paragraph paragraph = new Paragraph(text, SECTION);
        paragraph.setSpacingBefore(16f);
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    private static PdfPTable keyValueTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(4f);
        table.setWidths(new float[] { 1f, 2f });
        return table;
    }

    private static void addHeader(PdfPTable table, String first, String second) {
        PdfPCell firstCell = cell(first, BOLD);
        PdfPCell secondCell = cell(second, BOLD);
        firstCell.setGrayFill(HEADER_GRAY);
        secondCell.setGrayFill(HEADER_GRAY);
        table.addCell(firstCell);
        table.addCell(secondCell);
    }

    private static void addRow(PdfPTable table, String key, String value) {
        table.addCell(cell(key, BOLD));
        table.addCell(cell(value, NORMAL));
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(CELL_PADDING);
        return cell;
    }

    private static String orNotInformed(String value) {
        return value != null && !value.isBlank() ? value : NOT_INFORMED;
    }

    private static String valueOf(Integer value) {
        return value != null ? String.valueOf(value) : NOT_INFORMED;
    }

    private static String score(Double value) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", value != null ? value : 0.0);
    }

    private static String label(TrailName trailName) {
        return switch (trailName) {
            case AUTOCONHECIMENTO -> "Autoconhecimento";
            case INFLUENCIAS -> "Influências";
            case PLANO_DE_FUTURO -> "Plano de futuro";
            case INFORMACAO -> "Informação";
        };
    }

    private static String label(KnowledgeArea area) {
        return switch (area) {
            case HUMANAS -> "Humanas";
            case EXATAS -> "Exatas";
            case BIOLOGICAS -> "Biológicas";
            case ARTES -> "Artes";
        };
    }

    // Data no formato aaaa-mm-dd, usada no nome do arquivo
    public static String formatFileDate(Instant instant) {
        return FILE_DATE.format(instant);
    }
}
