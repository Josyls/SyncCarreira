package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.repositories.QuestionRepository;
import com.synccarreira.synccarreira_api.tests.TrailFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class AreaScoreCalculatorTests {

    @InjectMocks
    private AreaScoreCalculator calculator;

    @Mock
    private QuestionRepository questionRepository;

    // Perguntas de resposta única. Máximo possível (afinidade 5 + maior peso de cada pergunta): humanas 16, biológicas 11, exatas 17, artes 8
    private void mockQuestions() {
        Question first = new Question(1L, "Pergunta 1", QuestionType.LIKERT, TrailFactory.createTrail(), new ArrayList<>());
        first.getOptions().add(new QuestionOption(1L, "A", 10.0, 0.0, 0.0, 0.0, first));
        first.getOptions().add(new QuestionOption(2L, "B", 0.0, 5.0, 2.0, 0.0, first));
        Question second = new Question(2L, "Pergunta 2", QuestionType.CHECKBOX, TrailFactory.createTrail(), new ArrayList<>());
        second.getOptions().add(new QuestionOption(3L, "C", 0.0, 0.0, 10.0, 3.0, second));
        second.getOptions().add(new QuestionOption(4L, "D", 1.0, 1.0, 1.0, 1.0, second));
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(first, second));
    }

    private static AreaScoreDTO scoreOf(List<AreaScoreDTO> scores, KnowledgeArea area) {
        return scores.stream().filter(score -> score.area() == area).findFirst().orElseThrow();
    }

    @Test
    void calculateShouldHighlightAreaWithHighestPercentageInsteadOfHighestRawScore() {
        mockQuestions();

        // Exatas tem o maior score bruto (11), mas Biológicas tem o maior percentual (11 de 11)
        List<AreaScoreDTO> result = calculator.calculate(8.0, 11.0, 11.0, 4.0);

        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), AreaScoreCalculator.highlightedAreas(result));
        Assertions.assertEquals(100.0, scoreOf(result, KnowledgeArea.BIOLOGICAS).percentage());
        Assertions.assertEquals(64.7, scoreOf(result, KnowledgeArea.EXATAS).percentage());
        Assertions.assertEquals(17.0, scoreOf(result, KnowledgeArea.EXATAS).maxScore());
        Assertions.assertEquals(11.0, scoreOf(result, KnowledgeArea.EXATAS).score());
        Assertions.assertEquals(50.0, scoreOf(result, KnowledgeArea.HUMANAS).percentage());
        Assertions.assertEquals(50.0, scoreOf(result, KnowledgeArea.ARTES).percentage());
        Assertions.assertEquals("Biológicas", scoreOf(result, KnowledgeArea.BIOLOGICAS).areaName());
        // Do maior para o menor percentual; empates na ordem do enum
        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS, KnowledgeArea.EXATAS, KnowledgeArea.HUMANAS, KnowledgeArea.ARTES),
                result.stream().map(AreaScoreDTO::area).toList());
    }

    @Test
    void calculateShouldHighlightAllTiedAreas() {
        mockQuestions();

        // Humanas 8 de 16 e Artes 4 de 8: as duas com 50%
        List<AreaScoreDTO> result = calculator.calculate(8.0, 1.0, 1.0, 4.0);

        Assertions.assertEquals(List.of(KnowledgeArea.HUMANAS, KnowledgeArea.ARTES), AreaScoreCalculator.highlightedAreas(result));
    }

    @Test
    void calculateShouldNotHighlightAnyAreaWhenAllScoresAreZeroOrNull() {
        mockQuestions();

        List<AreaScoreDTO> result = calculator.calculate(0.0, null, 0.0, null);

        Assertions.assertTrue(AreaScoreCalculator.highlightedAreas(result).isEmpty());
        Assertions.assertTrue(result.stream().allMatch(score -> score.percentage() == 0.0));
    }

    @Test
    void calculateShouldLimitPercentageTo100() {
        mockQuestions();

        List<AreaScoreDTO> result = calculator.calculate(1.0, 1.0, 1.0, 20.0);

        Assertions.assertEquals(100.0, scoreOf(result, KnowledgeArea.ARTES).percentage());
    }

    @Test
    void calculateShouldConsiderOnlyAffinityWhenThereAreNoQuestions() {
        Mockito.when(questionRepository.findAll()).thenReturn(List.of());

        List<AreaScoreDTO> result = calculator.calculate(5.0, 2.5, 0.0, 0.0);

        Assertions.assertEquals(100.0, scoreOf(result, KnowledgeArea.HUMANAS).percentage());
        Assertions.assertEquals(50.0, scoreOf(result, KnowledgeArea.BIOLOGICAS).percentage());
        Assertions.assertEquals(5.0, scoreOf(result, KnowledgeArea.EXATAS).maxScore());
    }

    @Test
    void calculateShouldIgnoreNegativeWeightsAndOpenQuestionsInMaxScore() {
        Question negative = new Question(1L, "Pergunta", QuestionType.MULTIPLA_ESCOLHA, TrailFactory.createTrail(), new ArrayList<>());
        negative.getOptions().add(new QuestionOption(1L, "A", -2.0, 4.0, null, 0.0, negative));
        Question open = new Question(2L, "Aberta", QuestionType.ABERTA, TrailFactory.createTrail(), new ArrayList<>());
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(negative, open));

        List<AreaScoreDTO> result = calculator.calculate(0.0, 0.0, 0.0, 0.0);

        Assertions.assertEquals(5.0, scoreOf(result, KnowledgeArea.HUMANAS).maxScore());
        Assertions.assertEquals(9.0, scoreOf(result, KnowledgeArea.BIOLOGICAS).maxScore());
        Assertions.assertEquals(5.0, scoreOf(result, KnowledgeArea.EXATAS).maxScore());
    }

    @Test
    void calculateWithGivenMaxScoresShouldNotQueryQuestions() {
        // Usado nas jornadas concluídas, com o máximo guardado na conclusão
        List<AreaScoreDTO> result = AreaScoreCalculator.calculate(
                AreaScoreCalculator.areaMap(8.0, 11.0, 11.0, 4.0),
                AreaScoreCalculator.areaMap(16.0, 11.0, 17.0, 8.0));

        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), AreaScoreCalculator.highlightedAreas(result));
        Assertions.assertEquals(64.7, scoreOf(result, KnowledgeArea.EXATAS).percentage());
        Mockito.verifyNoInteractions(questionRepository);
    }

    @Test
    void currentMaxScoresShouldSumMaxAffinityAndHighestWeightOfEachQuestion() {
        mockQuestions();

        Assertions.assertEquals(AreaScoreCalculator.areaMap(16.0, 11.0, 17.0, 8.0), calculator.currentMaxScores());
    }

    @Test
    void calculateShouldFollowQuestionsCurrentlyRegistered() {
        // O máximo acompanha as perguntas cadastradas: a mesma pontuação vale outro percentual se os pesos mudarem
        mockQuestions();
        double before = scoreOf(calculator.calculate(8.0, 0.0, 0.0, 0.0), KnowledgeArea.HUMANAS).percentage();

        Question changed = new Question(1L, "Pergunta 1", QuestionType.MULTIPLA_ESCOLHA, TrailFactory.createTrail(), new ArrayList<>());
        changed.getOptions().add(new QuestionOption(1L, "A", 27.0, 0.0, 0.0, 0.0, changed));
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(changed));
        double after = scoreOf(calculator.calculate(8.0, 0.0, 0.0, 0.0), KnowledgeArea.HUMANAS).percentage();

        Assertions.assertEquals(50.0, before);
        Assertions.assertEquals(25.0, after);
    }

    @Test
    void currentMaxScoresShouldSumPositiveWeightsOfAllOptionsWhenQuestionIsMultipleChoice() {
        // Na múltipla escolha o aluno pode marcar todas as opções; peso negativo não aumenta o máximo
        Question multiple = new Question(1L, "Pergunta", QuestionType.MULTIPLA_ESCOLHA, TrailFactory.createTrail(), new ArrayList<>());
        multiple.getOptions().add(new QuestionOption(1L, "A", 10.0, 0.0, -3.0, 0.0, multiple));
        multiple.getOptions().add(new QuestionOption(2L, "B", 4.0, 5.0, 2.0, null, multiple));
        multiple.getOptions().add(new QuestionOption(3L, "C", 1.0, 0.0, 0.0, 0.0, multiple));
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(multiple));

        Assertions.assertEquals(AreaScoreCalculator.areaMap(20.0, 10.0, 7.0, 5.0), calculator.currentMaxScores());
    }

    @Test
    void currentMaxScoresShouldUseHighestWeightForSingleAnswerQuestionsAndSumForMultipleChoice() {
        Question single = new Question(1L, "Única", QuestionType.CHECKBOX, TrailFactory.createTrail(), new ArrayList<>());
        single.getOptions().add(new QuestionOption(1L, "A", 10.0, 0.0, 0.0, 0.0, single));
        single.getOptions().add(new QuestionOption(2L, "B", 4.0, 0.0, 0.0, 0.0, single));
        Question multiple = new Question(2L, "Múltipla", QuestionType.MULTIPLA_ESCOLHA, TrailFactory.createTrail(), new ArrayList<>());
        multiple.getOptions().add(new QuestionOption(3L, "C", 10.0, 0.0, 0.0, 0.0, multiple));
        multiple.getOptions().add(new QuestionOption(4L, "D", 4.0, 0.0, 0.0, 0.0, multiple));
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(single, multiple));

        // Humanas: afinidade 5 + 10 (maior peso da única) + 14 (soma da múltipla)
        Assertions.assertEquals(29.0, calculator.currentMaxScores().get(KnowledgeArea.HUMANAS));
    }

    @Test
    void calculateShouldReachFullPercentageOnlyWhenAllMultipleChoiceOptionsAreChecked() {
        Question multiple = new Question(1L, "Múltipla", QuestionType.MULTIPLA_ESCOLHA, TrailFactory.createTrail(), new ArrayList<>());
        multiple.getOptions().add(new QuestionOption(1L, "A", 0.0, 0.0, 0.0, 5.0, multiple));
        multiple.getOptions().add(new QuestionOption(2L, "B", 0.0, 0.0, 0.0, 10.0, multiple));
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(multiple));

        // Máximo de artes: afinidade 5 + 5 + 10 = 20
        double oneOption = scoreOf(calculator.calculate(0.0, 0.0, 0.0, 15.0), KnowledgeArea.ARTES).percentage();
        double allOptions = scoreOf(calculator.calculate(0.0, 0.0, 0.0, 20.0), KnowledgeArea.ARTES).percentage();

        Assertions.assertEquals(75.0, oneOption);
        Assertions.assertEquals(100.0, allOptions);
    }
}
