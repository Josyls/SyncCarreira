package com.synccarreira.synccarreira_api.services.reports;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.dto.journey.JourneyContent;
import com.synccarreira.synccarreira_api.dto.journey.JourneyPdfData;
import com.synccarreira.synccarreira_api.dto.journey.JourneyRecordDTO;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import com.synccarreira.synccarreira_api.tests.JourneyFactory;
import com.synccarreira.synccarreira_api.tests.ResultFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public class JourneyPdfWriterTests {

    private final JourneyPdfWriter writer = new JourneyPdfWriter();

    private static String extractText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    @Test
    void writeShouldGenerateValidPdfWithJourneyContent() throws Exception {
        JourneyPdfData data = new JourneyPdfData("Ana Souza", "ana.souza@gmail.com", "3º ano",
                JourneyFactory.createJourneyRecordDTO(), JourneyFactory.createJourneyContent(),
                ResultFactory.createJourneyResult());

        byte[] pdf = writer.write(data);

        Assertions.assertTrue(pdf.length > 0);
        Assertions.assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        String text = extractText(pdf);
        Assertions.assertTrue(text.contains("Souza"));
        Assertions.assertTrue(text.contains("Autoconhecimento"));
        // Resultado em percentual e leque de carreiras (RF-12)
        Assertions.assertTrue(text.contains("47,9%") || text.contains("47.9%"));
        Assertions.assertTrue(text.contains("Medicina e Cuidado em Sa"));
        Assertions.assertTrue(text.contains("Enfermagem"));
        Assertions.assertTrue(text.contains("diagn"));
    }

    @Test
    void writeShouldHandleMissingOptionalData() throws Exception {
        // Jornada sem turma, sem afinidades, sem área de destaque, com pergunta sem resposta e dúvida sinalizada
        JourneyRecordDTO journey = new JourneyRecordDTO(1L, 1, null, Instant.parse("2026-10-01T15:30:00Z"),
                null, null, null, 0.0, 0.0, 0.0, 0.0,
                new StudentAffinityDTO(null, null, null, null), List.of(), true, null);
        JourneyContent content = new JourneyContent(
                List.of(new JourneyContent.TrailContent(TrailName.AUTOCONHECIMENTO, 1,
                        List.of(new JourneyContent.AnswerContent("Pergunta sem resposta?", QuestionType.LIKERT, null)),
                        null, null)),
                List.of());
        JourneyPdfData data = new JourneyPdfData("Bruno Lima", "bruno@gmail.com", null, journey, content,
                ResultFactory.createEmptyJourneyResult());

        byte[] pdf = writer.write(data);

        PdfReader reader = new PdfReader(pdf);
        try {
            Assertions.assertTrue(reader.getNumberOfPages() >= 1);
        } finally {
            reader.close();
        }
    }

    @Test
    void formatFileDateShouldUseIsoDateInSaoPauloZone() {
        // 02:00 UTC ainda é dia 30/09 em São Paulo
        Assertions.assertEquals("2026-09-30", JourneyPdfWriter.formatFileDate(Instant.parse("2026-10-01T02:00:00Z")));
    }
}
