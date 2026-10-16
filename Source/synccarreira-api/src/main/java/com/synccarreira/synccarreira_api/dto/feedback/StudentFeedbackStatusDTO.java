package com.synccarreira.synccarreira_api.dto.feedback;

// Situação dos feedbacks de cada aluno, para o painel da psicóloga (RF-09)
public record StudentFeedbackStatusDTO(
        Long studentId,
        String studentName,
        String schoolClassName,
        int concludedJourneys,
        Integer lastConcludedJourneyNumber,
        boolean journeyFeedbackSent,
        long sessionFeedbacksSent
) {
}
