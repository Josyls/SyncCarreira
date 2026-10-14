package com.synccarreira.synccarreira_api.tests;

import com.synccarreira.synccarreira_api.dto.journey.JourneyContent;
import com.synccarreira.synccarreira_api.dto.journey.JourneyRecordDTO;
import com.synccarreira.synccarreira_api.dto.journey.JourneyStatusDTO;
import com.synccarreira.synccarreira_api.entities.JourneyRecord;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;

import java.time.Instant;
import java.util.List;

public class JourneyFactory {

    public static final Instant STARTED_AT = Instant.parse("2026-03-01T12:00:00Z");
    public static final Instant CONCLUDED_AT = Instant.parse("2026-10-01T15:30:00Z");
    public static final String QUESTION = "O que você mais gosta de fazer no tempo livre?";
    public static final String ANSWER = "Desenhar e pintar";
    public static final String SYNTHESIS = "Percebi que gosto de criar coisas com as mãos.";

    public static JourneyContent createJourneyContent() {
        return new JourneyContent(
                List.of(
                        new JourneyContent.TrailContent(
                                TrailName.AUTOCONHECIMENTO, 1,
                                List.of(new JourneyContent.AnswerContent(QUESTION, QuestionType.MULTIPLA_ESCOLHA, ANSWER)),
                                SYNTHESIS, CONCLUDED_AT),
                        new JourneyContent.TrailContent(
                                TrailName.INFORMACAO, 4, List.of(),
                                "Pesquisei cursos de Design e Arquitetura.", CONCLUDED_AT)),
                List.of(
                        new JourneyContent.LinkContent("Cursos de Artes", "https://exemplo.com/artes", KnowledgeArea.ARTES),
                        new JourneyContent.LinkContent("SiSU", "https://acessounico.mec.gov.br/sisu", null)));
    }

    public static JourneyRecord createJourneyRecord(Student student, Integer journeyNumber, String content) {
        JourneyRecord journey = new JourneyRecord();
        journey.setId(journeyNumber.longValue());
        journey.setStudent(student);
        journey.setJourneyNumber(journeyNumber);
        journey.setStartedAt(STARTED_AT);
        journey.setConcludedAt(CONCLUDED_AT);
        journey.setSchoolClassName("3º A");
        journey.setSchoolYear(2026);
        journey.setInstitutionName("Escola Estadual Central");
        journey.setHumanitiesScore(10.0);
        journey.setExactSciencesScore(8.0);
        journey.setBiologicalSciencesScore(6.0);
        journey.setArtsScore(20.5);
        journey.setHumanitiesAffinity(3);
        journey.setExactSciencesAffinity(2);
        journey.setBiologicalSciencesAffinity(4);
        journey.setArtsAffinity(5);
        journey.setHighestScoreAreaList(List.of(KnowledgeArea.ARTES));
        journey.setInDoubt(false);
        journey.setContent(content);
        return journey;
    }

    public static JourneyRecordDTO createJourneyRecordDTO() {
        return new JourneyRecordDTO(createJourneyRecord(StudentFactory.createStudentWithCompletedProfile(), 1, "{}"), false, null);
    }

    public static JourneyStatusDTO createJourneyStatusDTO() {
        return new JourneyStatusDTO(2, Instant.parse("2026-10-02T10:00:00Z"), false, 1);
    }
}
