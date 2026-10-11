package com.synccarreira.synccarreira_api.tests;

import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.dto.StudentDTO;
import com.synccarreira.synccarreira_api.dto.StudentDetailsDTO;
import com.synccarreira.synccarreira_api.dto.StudentInsertDTO;
import com.synccarreira.synccarreira_api.dto.StudentProfileDTO;
import com.synccarreira.synccarreira_api.dto.StudentScoreDTO;
import com.synccarreira.synccarreira_api.entities.Role;
import com.synccarreira.synccarreira_api.entities.Student;

import java.time.Instant;

public class StudentFactory {

    public static final String STUDENT_ROLE = "ROLE_USER";

    public static Role createStudentRole() {
        return new Role(1L, STUDENT_ROLE);
    }

    public static Student createStudent() {
        Student student = new Student();
        student.setId(1L);
        student.setName("Ana Souza");
        student.setEmail("ana.souza@gmail.com");
        student.setScholarYear("3º ano do Ensino Médio");
        student.setSchoolType("Pública");
        student.setRace("Parda");
        student.setHumanitiesScore(7.5);
        student.setExactSciencesScore(6.0);
        student.setBiologicalSciencesScore(8.25);
        student.setArtsScore(5.0);
        student.addRole(createStudentRole());
        return student;
    }

    public static StudentInsertDTO createStudentInsertDTO() {
        return new StudentInsertDTO(
                "Ana Souza",
                "ana.souza@gmail.com",
                "3º ano do Ensino Médio",
                "Pública",
                "Parda"
        );
    }

    public static StudentInsertDTO createStudentUpdateDTO() {
        return new StudentInsertDTO(
                "Ana Souza Lima",
                "ana.lima@gmail.com",
                "2º ano do Ensino Médio",
                "Particular",
                "Preta"
        );
    }

    public static StudentInsertDTO createInvalidStudentInsertDTO() {
        return new StudentInsertDTO(
                "",
                "email-invalido",
                "3º ano do Ensino Médio",
                "Pública",
                "Parda"
        );
    }

    public static StudentDTO createStudentDTO() {
        return new StudentDTO(createStudent());
    }

    public static StudentScoreDTO createStudentScoreDTO() {
        return new StudentScoreDTO(createStudent());
    }

    public static StudentDetailsDTO createStudentDetailsDTO() {
        return new StudentDetailsDTO(
                1L,
                "Ana Souza",
                "ana.souza@gmail.com",
                "3º ano do Ensino Médio",
                "Pública",
                "Parda",
                "3º A",
                "Escola Estadual Central"
        );
    }

    // Aluno que já respondeu as 4 perguntas de afinidade do perfil (RF-14)
    public static Student createStudentWithCompletedProfile() {
        Student student = createStudent();
        student.setHumanitiesAffinity(3);
        student.setExactSciencesAffinity(2);
        student.setBiologicalSciencesAffinity(4);
        student.setArtsAffinity(5);
        student.setProfileCompletedAt(Instant.parse("2026-10-01T12:00:00Z"));
        return student;
    }

    public static StudentAffinityDTO createStudentAffinityDTO() {
        return new StudentAffinityDTO(3, 2, 4, 5);
    }

    public static StudentProfileDTO createStudentProfileDTO() {
        return new StudentProfileDTO(createStudentWithCompletedProfile());
    }
}
