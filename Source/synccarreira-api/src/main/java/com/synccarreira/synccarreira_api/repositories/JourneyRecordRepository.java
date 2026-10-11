package com.synccarreira.synccarreira_api.repositories;

import com.synccarreira.synccarreira_api.entities.JourneyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JourneyRecordRepository extends JpaRepository<JourneyRecord, Long> {

    boolean existsByStudentIdAndJourneyNumber(Long studentId, Integer journeyNumber);

    List<JourneyRecord> findByStudentIdOrderByJourneyNumberAsc(Long studentId);

    Optional<JourneyRecord> findByStudentIdAndJourneyNumber(Long studentId, Integer journeyNumber);

    // Busca sempre filtrando pelo aluno: um aluno nunca acessa a jornada de outro
    Optional<JourneyRecord> findByIdAndStudentId(Long id, Long studentId);

    List<JourneyRecord> findByStudentIdIn(Collection<Long> studentIds);
}
