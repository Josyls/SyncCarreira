package com.synccarreira.synccarreira_api.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Feedback de fim de jornada (RF-09). Sem journeyNumber, vale para a última jornada concluída pelo aluno.
public record JourneyFeedbackInsertDTO(
        @NotNull(message = "O aluno é obrigatório")
        Long studentId,

        Integer journeyNumber,

        @NotBlank(message = "Campo obrigatório")
        @Size(max = FeedbackDTO.MAX_CONTENT_LENGTH, message = "O feedback deve ter no máximo " + FeedbackDTO.MAX_CONTENT_LENGTH + " caracteres")
        String content
) {
}
