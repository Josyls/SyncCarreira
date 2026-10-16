package com.synccarreira.synccarreira_api.repositories;

import com.synccarreira.synccarreira_api.entities.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    boolean existsByStudentIdAndJourneyNumber(Long studentId, Integer journeyNumber);

    boolean existsByStudentIdAndAppointmentId(Long studentId, Long appointmentId);

    List<Feedback> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<Feedback> findByPsychologistIdOrderByCreatedAtDesc(Long psychologistId);

    // Busca sempre filtrando pelo aluno: um aluno nunca acessa o feedback de outro
    Optional<Feedback> findByIdAndStudentId(Long id, Long studentId);

    List<Feedback> findByStudentIdIn(Collection<Long> studentIds);
}
