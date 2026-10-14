package com.synccarreira.synccarreira_api.dto.journey;

import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.entities.JourneyRecord;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;

import java.time.Instant;
import java.util.List;

// Resumo de uma jornada concluída, usado no histórico do aluno para acompanhar a evolução (RF-15)
public record JourneyRecordDTO(
        Long id,
        Integer journeyNumber,
        Instant startedAt,
        Instant concludedAt,
        String schoolClassName,
        Integer schoolYear,
        String institutionName,
        Double humanitiesScore,
        Double exactSciencesScore,
        Double biologicalSciencesScore,
        Double artsScore,
        StudentAffinityDTO affinities,
        List<KnowledgeArea> highestScoreAreas,
        boolean inDoubt,
        Instant doubtFlaggedAt
) {

    public JourneyRecordDTO(JourneyRecord journey, boolean inDoubt, Instant doubtFlaggedAt) {
        this(
                journey.getId(),
                journey.getJourneyNumber(),
                journey.getStartedAt(),
                journey.getConcludedAt(),
                journey.getSchoolClassName(),
                journey.getSchoolYear(),
                journey.getInstitutionName(),
                journey.getHumanitiesScore(),
                journey.getExactSciencesScore(),
                journey.getBiologicalSciencesScore(),
                journey.getArtsScore(),
                new StudentAffinityDTO(
                        journey.getHumanitiesAffinity(),
                        journey.getExactSciencesAffinity(),
                        journey.getBiologicalSciencesAffinity(),
                        journey.getArtsAffinity()),
                journey.highestScoreAreaList(),
                inDoubt,
                doubtFlaggedAt
        );
    }
}
