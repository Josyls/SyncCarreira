package com.synccarreira.synccarreira_api.entities;

import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StudentTests {

    @Test
    void highestScoreAreasShouldReturnAreaWithHighestScore() {
        Student student = StudentFactory.createStudent();

        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), student.highestScoreAreas());
    }

    @Test
    void highestScoreAreasShouldReturnAllTiedAreasWhenThereIsATie() {
        Student student = StudentFactory.createStudent();
        student.setHumanitiesScore(40.0);
        student.setArtsScore(40.0);

        Assertions.assertEquals(List.of(KnowledgeArea.HUMANAS, KnowledgeArea.ARTES), student.highestScoreAreas());
    }

    @Test
    void highestScoreAreasShouldTreatNullScoreAsZero() {
        Student student = StudentFactory.createStudent();
        student.setHumanitiesScore(null);
        student.setBiologicalSciencesScore(null);
        student.setExactSciencesScore(null);

        Assertions.assertEquals(List.of(KnowledgeArea.ARTES), student.highestScoreAreas());
    }

    private static Answer answerWithWeights(double humanities, double exactSciences, double biologicalSciences, double arts) {
        QuestionOption option = new QuestionOption();
        option.setHumanitiesWeight(humanities);
        option.setExactSciencesWeight(exactSciences);
        option.setBiologicalSciencesWeight(biologicalSciences);
        option.setArtsWeight(arts);
        Answer answer = new Answer();
        answer.setQuestionOption(option);
        return answer;
    }

    @Test
    void hasCompletedProfileShouldReturnFalseWhenAnyAffinityIsMissing() {
        Student student = StudentFactory.createStudentWithCompletedProfile();
        student.setArtsAffinity(null);

        Assertions.assertFalse(student.hasCompletedProfile());
        Assertions.assertFalse(StudentFactory.createStudent().hasCompletedProfile());
    }

    @Test
    void hasCompletedProfileShouldReturnTrueWhenAllAffinitiesAreFilled() {
        Assertions.assertTrue(StudentFactory.createStudentWithCompletedProfile().hasCompletedProfile());
    }

    @Test
    void recalculateScoresShouldSumAffinitiesAndAnswerWeights() {
        // Afinidades: humanas 3, exatas 2, biológicas 4, artes 5
        Student student = StudentFactory.createStudentWithCompletedProfile();

        student.recalculateScores(List.of(answerWithWeights(10.0, 0.0, 1.5, 0.0), answerWithWeights(0.0, 4.0, 0.0, 2.0)));

        Assertions.assertEquals(13.0, student.getHumanitiesScore());
        Assertions.assertEquals(6.0, student.getExactSciencesScore());
        Assertions.assertEquals(5.5, student.getBiologicalSciencesScore());
        Assertions.assertEquals(7.0, student.getArtsScore());
    }

    @Test
    void recalculateScoresShouldTreatMissingAffinitiesAsZero() {
        Student student = StudentFactory.createStudent();

        student.recalculateScores(List.of(answerWithWeights(1.0, 2.0, 3.0, 4.0)));

        Assertions.assertEquals(1.0, student.getHumanitiesScore());
        Assertions.assertEquals(2.0, student.getExactSciencesScore());
        Assertions.assertEquals(3.0, student.getBiologicalSciencesScore());
        Assertions.assertEquals(4.0, student.getArtsScore());
    }

    @Test
    void recalculateScoresShouldNotAccumulateWhenCalledMoreThanOnce() {
        Student student = StudentFactory.createStudentWithCompletedProfile();
        List<Answer> answers = List.of(answerWithWeights(1.0, 1.0, 1.0, 1.0));

        student.recalculateScores(answers);
        student.recalculateScores(answers);

        Assertions.assertEquals(4.0, student.getHumanitiesScore());
        Assertions.assertEquals(3.0, student.getExactSciencesScore());
        Assertions.assertEquals(5.0, student.getBiologicalSciencesScore());
        Assertions.assertEquals(6.0, student.getArtsScore());
    }

    @Test
    void recalculateScoresShouldUseOnlyAffinitiesWhenThereAreNoAnswers() {
        Student student = StudentFactory.createStudentWithCompletedProfile();

        student.recalculateScores(List.of());

        Assertions.assertEquals(List.of(KnowledgeArea.ARTES), student.highestScoreAreas());
        Assertions.assertEquals(5.0, student.getArtsScore());
    }
}
