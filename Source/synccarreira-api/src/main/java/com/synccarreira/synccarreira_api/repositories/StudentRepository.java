package com.synccarreira.synccarreira_api.repositories;

import com.synccarreira.synccarreira_api.dto.StudentDetailsDTO;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.projections.PanelStudentProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query(value = "SELECT new com.synccarreira.synccarreira_api.dto.StudentDetailsDTO(obj.id, obj.name, obj.email, obj.scholarYear, obj.schoolType, obj.race, c.name, i.legalName) " +
            "FROM Student obj " +
            "LEFT JOIN obj.determinedSchoolClass c " +
            "LEFT JOIN c.institution i",
            countQuery = "SELECT count(obj) FROM Student obj")
    Page<StudentDetailsDTO> searchAllPaged(Pageable pageable);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE tb_aluno
            SET fk_id_turma = :classId
            WHERE id_usuario = :studentId""")
    void setSchoolClass(@Param("studentId") Long studentId, @Param("classId") Long classId);

    // Só marca o aluno que ainda não está em dúvida: retorna 0 quando a dúvida já tinha sido sinalizada
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE tb_aluno
            SET em_duvida = TRUE, data_sinalizacao_duvida = :flaggedAt
            WHERE id_usuario = :studentId AND em_duvida = FALSE""")
    int flagDoubt(@Param("studentId") Long studentId, @Param("flaggedAt") Instant flaggedAt);

    @Query("SELECT new com.synccarreira.synccarreira_api.projections.PanelStudentProjection(obj.id, obj.name, c.id, c.name, obj.inDoubt, obj.doubtFlaggedAt) " +
            "FROM Student obj " +
            "JOIN obj.determinedSchoolClass c " +
            "WHERE c.institution.id = :institutionId " +
            "ORDER BY c.name, obj.name")
    List<PanelStudentProjection> searchPanelStudentsByInstitution(@Param("institutionId") Long institutionId);

    @Query("SELECT obj FROM Student obj " +
            "JOIN FETCH obj.determinedSchoolClass c " +
            "LEFT JOIN FETCH c.institution " +
            "WHERE c.id = :schoolClassId " +
            "ORDER BY obj.name")
    List<Student> searchBySchoolClassId(@Param("schoolClassId") Long schoolClassId);
}
