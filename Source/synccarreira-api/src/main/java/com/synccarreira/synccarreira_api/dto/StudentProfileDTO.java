package com.synccarreira.synccarreira_api.dto;

import com.synccarreira.synccarreira_api.entities.Student;

import java.time.Instant;

// Perfil do aluno logado (RF-14). O front usa profileCompleted logo após o login para decidir se exibe as perguntas de afinidade.
public record StudentProfileDTO(
        Long id,
        String name,
        String email,
        boolean profileCompleted,
        Instant profileCompletedAt,
        StudentAffinityDTO affinities,
        StudentScoreDTO score
) {

    public StudentProfileDTO(Student student) {
        this(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.hasCompletedProfile(),
                student.getProfileCompletedAt(),
                new StudentAffinityDTO(student),
                new StudentScoreDTO(student)
        );
    }
}
