package com.synccarreira.synccarreira_api.dto.journey;

import java.time.Instant;

// Situação da jornada atual do aluno (RF-15).
// Quando currentJourneyConcluded é true, o front exibe o botão "iniciar nova jornada" e o download do PDF.
public record JourneyStatusDTO(
        Integer currentJourneyNumber,
        Instant currentJourneyStartedAt,
        boolean currentJourneyConcluded,
        int concludedJourneys
) {
}
