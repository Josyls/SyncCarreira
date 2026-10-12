package com.synccarreira.synccarreira_api.dto.result;

import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;

import java.time.Instant;
import java.util.List;

// Resultado de uma jornada concluída (RF-12): scores em percentual, áreas em destaque,
// subáreas sugeridas dessas áreas e o aviso de que o resultado não é um diagnóstico (RN-07)
public record JourneyResultDTO(
        Long journeyId,
        Integer journeyNumber,
        Instant concludedAt,
        List<AreaScoreDTO> scores,
        List<KnowledgeArea> highlightedAreas,
        List<SubareaDTO> suggestions,
        String disclaimer
) {
}
