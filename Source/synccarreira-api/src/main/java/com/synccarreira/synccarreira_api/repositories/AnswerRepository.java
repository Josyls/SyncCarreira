package com.synccarreira.synccarreira_api.repositories;

import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.projections.AnsweredQuestionsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT a FROM Answer a " +
            "JOIN FETCH a.questionOption qo " +
            "JOIN FETCH qo.question q " +
            "WHERE a.student.id = :studentId " +
            "AND q.trail.id = :trailId")
    List<Answer> findByStudentAndTrail(Long studentId, Long trailId);

    @Query("SELECT a FROM Answer a JOIN FETCH a.questionOption WHERE a.student.id = :studentId")
    List<Answer> findByStudentId(Long studentId);

    // Respostas do aluno para uma pergunta, da mais antiga para a mais nova.
    // Em perguntas de MULTIPLA_ESCOLHA há uma por opção marcada. Nos demais tipos normalmente há no máximo uma;
    // mais de uma só em dados gravados antes da substituição de respostas.
    @Query("SELECT a FROM Answer a " +
            "JOIN FETCH a.questionOption qo " +
            "WHERE a.student.id = :studentId " +
            "AND qo.question.id = :questionId " +
            "ORDER BY a.id")
    List<Answer> findByStudentAndQuestion(@Param("studentId") Long studentId, @Param("questionId") Long questionId);

    @Query("SELECT new com.synccarreira.synccarreira_api.projections.AnsweredQuestionsProjection(s.id, q.trail.id, COUNT(DISTINCT q.id)) " +
            "FROM Answer a " +
            "JOIN a.student s " +
            "JOIN s.determinedSchoolClass c " +
            "JOIN a.questionOption qo " +
            "JOIN qo.question q " +
            "WHERE c.institution.id = :institutionId " +
            "GROUP BY s.id, q.trail.id")
    List<AnsweredQuestionsProjection> countAnsweredQuestionsByInstitution(@Param("institutionId") Long institutionId);

    // Usado ao iniciar uma nova jornada (RF-15): as respostas da jornada anterior já estão guardadas em JourneyRecord
    @Modifying
    @Query("DELETE FROM Answer a WHERE a.student.id = :studentId")
    int deleteAllByStudentId(@Param("studentId") Long studentId);
}
