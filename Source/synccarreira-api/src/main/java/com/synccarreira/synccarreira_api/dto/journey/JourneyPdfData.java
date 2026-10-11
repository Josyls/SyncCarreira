package com.synccarreira.synccarreira_api.dto.journey;

// Dados usados para montar o PDF de uma jornada concluída (RF-15)
public record JourneyPdfData(
        String studentName,
        String studentEmail,
        String scholarYear,
        JourneyRecordDTO journey,
        JourneyContent content
) {
}
