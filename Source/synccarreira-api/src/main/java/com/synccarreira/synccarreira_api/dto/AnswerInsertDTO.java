package com.synccarreira.synccarreira_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AnswerInsertDTO {

    @NotNull(message = "A opção escolhida é obrigatória")
    @Getter
    private Long questionOptionId;
}
