package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.dto.StudentProfileDTO;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// Personalização de perfil do aluno (RF-14). O aluno é sempre o usuário do token.
@Service
public class StudentProfileService {

    private final StudentRepository studentRepository;
    private final AnswerRepository answerRepository;
    private final AuthService authService;

    public StudentProfileService(
            final StudentRepository studentRepository,
            final AnswerRepository answerRepository,
            final AuthService authService) {
        this.studentRepository = studentRepository;
        this.answerRepository = answerRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public StudentProfileDTO findMyProfile() {
        return new StudentProfileDTO(loggedStudent());
    }

    // As afinidades podem ser alteradas a qualquer momento: o score é recalculado do zero, sem somar valores repetidos
    @Transactional
    public StudentProfileDTO updateMyAffinities(StudentAffinityDTO dto) {
        Student student = loggedStudent();

        student.setHumanitiesAffinity(dto.humanities());
        student.setExactSciencesAffinity(dto.exactSciences());
        student.setBiologicalSciencesAffinity(dto.biologicalSciences());
        student.setArtsAffinity(dto.arts());
        if (student.getProfileCompletedAt() == null) {
            student.setProfileCompletedAt(Instant.now());
        }

        student.recalculateScores(answerRepository.findByStudentId(student.getId()));
        student = studentRepository.save(student);
        return new StudentProfileDTO(student);
    }

    private Student loggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }
}
