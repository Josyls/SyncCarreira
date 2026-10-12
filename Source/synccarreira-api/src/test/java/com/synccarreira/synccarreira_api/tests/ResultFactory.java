package com.synccarreira.synccarreira_api.tests;

import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.dto.result.JourneyResultDTO;
import com.synccarreira.synccarreira_api.dto.result.SubareaDTO;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.services.JourneyResultService;

import java.time.Instant;
import java.util.List;

public class ResultFactory {

    public static JourneyResultDTO createJourneyResult() {
        return new JourneyResultDTO(
                1L,
                1,
                Instant.parse("2026-10-01T15:30:00Z"),
                List.of(
                        new AreaScoreDTO(KnowledgeArea.BIOLOGICAS, "Biológicas", 58.0, 121.0, 47.9, true),
                        new AreaScoreDTO(KnowledgeArea.HUMANAS, "Humanas", 60.0, 143.5, 41.8, false),
                        new AreaScoreDTO(KnowledgeArea.EXATAS, "Exatas", 50.0, 166.5, 30.0, false),
                        new AreaScoreDTO(KnowledgeArea.ARTES, "Artes", 30.0, 133.5, 22.5, false)),
                List.of(KnowledgeArea.BIOLOGICAS),
                List.of(new SubareaDTO(1L, KnowledgeArea.BIOLOGICAS, "Medicina e Cuidado em Saúde",
                        "Cuidado direto com a saúde das pessoas.", List.of("Medicina", "Enfermagem"))),
                JourneyResultService.DISCLAIMER);
    }

    public static JourneyResultDTO createEmptyJourneyResult() {
        return new JourneyResultDTO(1L, 1, Instant.parse("2026-10-01T15:30:00Z"), List.of(), List.of(), List.of(),
                JourneyResultService.DISCLAIMER);
    }
}
