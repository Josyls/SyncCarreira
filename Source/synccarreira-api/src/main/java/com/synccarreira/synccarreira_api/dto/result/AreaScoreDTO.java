package com.synccarreira.synccarreira_api.dto.result;

import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;

// Score de uma área no resultado da jornada (RF-12). O percentual é o score sobre o máximo possível da área,
// para que áreas com mais pontos disponíveis nas perguntas não fiquem em vantagem.
public record AreaScoreDTO(
        KnowledgeArea area,
        String areaName,
        double score,
        double maxScore,
        double percentage,
        boolean highlighted
) {
}
