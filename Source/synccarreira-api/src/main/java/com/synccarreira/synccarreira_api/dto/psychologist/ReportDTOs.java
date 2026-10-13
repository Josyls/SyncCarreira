package com.synccarreira.synccarreira_api.dto.psychologist;

import com.synccarreira.synccarreira_api.dto.SynthesisDTO;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;

import java.util.List;

public final class ReportDTOs {

    private ReportDTOs() {
    }

    // Panorama da jornada de um aluno: dados cadastrais, pontuação por área, progresso nas trilhas e sínteses enviadas
    public record StudentReport(
            Long studentId,
            String name,
            String email,
            String scholarYear,
            String schoolType,
            String race,
            String schoolClassName,
            Integer schoolYear,
            String institutionName,
            Double humanitiesScore,
            Double exactSciencesScore,
            Double biologicalSciencesScore,
            Double artsScore,
            List<KnowledgeArea> highestScoreAreas,
            PanelDTOs.StudentStatus status,
            List<SynthesisDTO> syntheses
    ) {
    }

    public record ClassReport(
            Long schoolClassId,
            String schoolClassName,
            Integer schoolYear,
            String institutionName,
            int totalStudents,
            int notStartedStudents,
            int inProgressStudents,
            int concludedStudents,
            int inDoubtStudents,
            List<StudentReport> students
    ) {
    }
}
