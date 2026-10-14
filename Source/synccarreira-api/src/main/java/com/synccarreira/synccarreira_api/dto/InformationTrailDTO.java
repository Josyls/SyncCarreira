package com.synccarreira.synccarreira_api.dto;

import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;

import java.util.List;

// recommendedAreas: área(s) com o maior percentual do máximo possível (RN-05), as mesmas do resultado da jornada (RF-12)
public record InformationTrailDTO(
        List<KnowledgeArea> recommendedAreas,
        StudentScoreDTO score,
        List<AreaScoreDTO> areaScores,
        List<InformationLinkDTO> careerLinks,
        List<InformationLinkDTO> universityAccessLinks
) {
}
