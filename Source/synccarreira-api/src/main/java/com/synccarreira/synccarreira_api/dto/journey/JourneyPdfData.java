package com.synccarreira.synccarreira_api.dto.journey;

import com.synccarreira.synccarreira_api.dto.result.JourneyResultDTO;

// Dados usados para montar o PDF de uma jornada concluída (RF-15), incluindo o resultado com o leque de carreiras (RF-12)
public record JourneyPdfData(
        String studentName,
        String studentEmail,
        String scholarYear,
        JourneyRecordDTO journey,
        JourneyContent content,
        JourneyResultDTO result
) {
}
