package com.synccarreira.synccarreira_api.dto;

import com.synccarreira.synccarreira_api.entities.Student;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Respostas do aluno às 4 perguntas de afinidade do perfil (RF-14), em escala Likert de 0 a 5
public record StudentAffinityDTO(
        @NotNull(message = "Campo obrigatório")
        @Min(value = StudentAffinityDTO.MIN, message = "A afinidade deve ser no mínimo " + StudentAffinityDTO.MIN)
        @Max(value = StudentAffinityDTO.MAX, message = "A afinidade deve ser no máximo " + StudentAffinityDTO.MAX)
        Integer humanities,

        @NotNull(message = "Campo obrigatório")
        @Min(value = StudentAffinityDTO.MIN, message = "A afinidade deve ser no mínimo " + StudentAffinityDTO.MIN)
        @Max(value = StudentAffinityDTO.MAX, message = "A afinidade deve ser no máximo " + StudentAffinityDTO.MAX)
        Integer exactSciences,

        @NotNull(message = "Campo obrigatório")
        @Min(value = StudentAffinityDTO.MIN, message = "A afinidade deve ser no mínimo " + StudentAffinityDTO.MIN)
        @Max(value = StudentAffinityDTO.MAX, message = "A afinidade deve ser no máximo " + StudentAffinityDTO.MAX)
        Integer biologicalSciences,

        @NotNull(message = "Campo obrigatório")
        @Min(value = StudentAffinityDTO.MIN, message = "A afinidade deve ser no mínimo " + StudentAffinityDTO.MIN)
        @Max(value = StudentAffinityDTO.MAX, message = "A afinidade deve ser no máximo " + StudentAffinityDTO.MAX)
        Integer arts
) {

    public static final int MIN = 0;
    public static final int MAX = 5;

    public StudentAffinityDTO(Student student) {
        this(
                student.getHumanitiesAffinity(),
                student.getExactSciencesAffinity(),
                student.getBiologicalSciencesAffinity(),
                student.getArtsAffinity()
        );
    }
}
