package com.synccarreira.synccarreira_api.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Feedback individual sobre uma sessão realizada (RF-09)
public record SessionFeedbackInsertDTO(
        @NotNull(message = "A sessão é obrigatória")
        Long appointmentId,

        @NotNull(message = "O aluno é obrigatório")
        Long studentId,

        @NotBlank(message = "Campo obrigatório")
        @Size(max = FeedbackDTO.MAX_CONTENT_LENGTH, message = "O feedback deve ter no máximo " + FeedbackDTO.MAX_CONTENT_LENGTH + " caracteres")
        String content
) {
}
