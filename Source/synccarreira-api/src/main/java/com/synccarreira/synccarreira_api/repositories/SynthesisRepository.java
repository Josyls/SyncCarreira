package com.synccarreira.synccarreira_api.repositories;

import com.synccarreira.synccarreira_api.entities.Synthesis;
import com.synccarreira.synccarreira_api.projections.SubmittedSynthesisProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SynthesisRepository extends JpaRepository<Synthesis, Long> {

    boolean existsByStudentIdAndTrailId(Long studentId, Long trailId);

    @Query("SELECT s FROM Synthesis s " +
            "JOIN FETCH s.trail t " +
            "WHERE s.student.id = :studentId " +
            "ORDER BY t.sequentialOrder")
    List<Synthesis> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT new com.synccarreira.synccarreira_api.projections.SubmittedSynthesisProjection(st.id, s.trail.id) " +
            "FROM Synthesis s " +
            "JOIN s.student st " +
            "JOIN st.determinedSchoolClass c " +
            "WHERE c.institution.id = :institutionId")
    List<SubmittedSynthesisProjection> findSubmittedByInstitution(@Param("institutionId") Long institutionId);

    @Query("SELECT s FROM Synthesis s " +
            "JOIN FETCH s.trail t " +
            "JOIN FETCH s.student st " +
            "WHERE st.determinedSchoolClass.id = :schoolClassId " +
            "ORDER BY st.name, t.sequentialOrder")
    List<Synthesis> findBySchoolClassId(@Param("schoolClassId") Long schoolClassId);
}
