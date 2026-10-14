package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.dto.result.JourneyResultDTO;
import com.synccarreira.synccarreira_api.entities.JourneyRecord;
import com.synccarreira.synccarreira_api.entities.KnowledgeSubarea;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.repositories.JourneyRecordRepository;
import com.synccarreira.synccarreira_api.repositories.KnowledgeSubareaRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.JourneyFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import com.synccarreira.synccarreira_api.tests.TrailFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class JourneyResultServiceTests {

    private JourneyResultService service;

    @Mock
    private JourneyRecordRepository journeyRecordRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private KnowledgeSubareaRepository subareaRepository;

    @Mock
    private AuthService authService;

    private Student student;
    private JourneyRecord journey;

    @BeforeEach
    void setUp() {
        // Usa o cálculo real de percentual, com as perguntas mockadas
        service = new JourneyResultService(journeyRecordRepository, studentRepository, subareaRepository, authService,
                new AreaScoreCalculator(questionRepository));
        student = StudentFactory.createStudentWithCompletedProfile();
        journey = JourneyFactory.createJourneyRecord(student, 1, "{}");
    }

    // Máximo guardado na conclusão da jornada: humanas 16, biológicas 11, exatas 17, artes 8
    private void setStoredMaxScores() {
        journey.setHumanitiesMaxScore(16.0);
        journey.setBiologicalSciencesMaxScore(11.0);
        journey.setExactSciencesMaxScore(17.0);
        journey.setArtsMaxScore(8.0);
    }
    // Perguntas atuais de resposta única, com máximo possível (afinidade 5 + maior peso de cada pergunta): humanas 16, biológicas 11, exatas 17, artes 8
    // Perguntas atuais com máximo possível (afinidade 5 + maior peso de cada pergunta): humanas 16, biológicas 11, exatas 17, artes 8
    private void mockQuestions() {
        Question first = new Question(1L, "Pergunta 1", QuestionType.LIKERT, TrailFactory.createTrail(), new ArrayList<>());
        first.getOptions().add(new QuestionOption(1L, "A", 10.0, 0.0, 0.0, 0.0, first));
        first.getOptions().add(new QuestionOption(2L, "B", 0.0, 5.0, 2.0, 0.0, first));
        Question second = new Question(2L, "Pergunta 2", QuestionType.CHECKBOX, TrailFactory.createTrail(), new ArrayList<>());
        second.getOptions().add(new QuestionOption(3L, "C", 0.0, 0.0, 10.0, 3.0, second));
        second.getOptions().add(new QuestionOption(4L, "D", 1.0, 1.0, 1.0, 1.0, second));
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(first, second));
    }

    private void setScores(double humanities, double biologicalSciences, double exactSciences, double arts) {
        journey.setHumanitiesScore(humanities);
        journey.setBiologicalSciencesScore(biologicalSciences);
        journey.setExactSciencesScore(exactSciences);
        journey.setArtsScore(arts);
    }

    private static KnowledgeSubarea subarea(Long id, KnowledgeArea area, String name, int order) {
        KnowledgeSubarea subarea = new KnowledgeSubarea();
        subarea.setId(id);
        subarea.setArea(area);
        subarea.setName(name);
        subarea.setDescription("Descrição de " + name);
        subarea.setDisplayOrder(order);
        subarea.getCareers().addAll(List.of("Carreira 1", "Carreira 2"));
        return subarea;
    }

    private static AreaScoreDTO scoreOf(JourneyResultDTO result, KnowledgeArea area) {
        return result.scores().stream().filter(score -> score.area() == area).findFirst().orElseThrow();
    }

    @Test
    void resultForShouldHighlightAreaWithHighestPercentageInsteadOfHighestRawScore() {
        setStoredMaxScores();
        // Exatas tem o maior score bruto (11), mas Biológicas tem o maior percentual (11 de 11)
        setScores(8.0, 11.0, 11.0, 4.0);
        Mockito.when(subareaRepository.findByAreaInOrderByAreaAscDisplayOrderAsc(List.of(KnowledgeArea.BIOLOGICAS)))
                .thenReturn(List.of(subarea(1L, KnowledgeArea.BIOLOGICAS, "Saúde", 1), subarea(2L, KnowledgeArea.BIOLOGICAS, "Meio Ambiente", 2)));

        JourneyResultDTO result = service.resultFor(journey);

        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), result.highlightedAreas());
        Assertions.assertEquals(100.0, scoreOf(result, KnowledgeArea.BIOLOGICAS).percentage());
        Assertions.assertEquals(64.7, scoreOf(result, KnowledgeArea.EXATAS).percentage());
        Assertions.assertEquals(17.0, scoreOf(result, KnowledgeArea.EXATAS).maxScore());
        Assertions.assertEquals(50.0, scoreOf(result, KnowledgeArea.HUMANAS).percentage());
        Assertions.assertEquals(50.0, scoreOf(result, KnowledgeArea.ARTES).percentage());
        Assertions.assertTrue(scoreOf(result, KnowledgeArea.BIOLOGICAS).highlighted());
        Assertions.assertFalse(scoreOf(result, KnowledgeArea.EXATAS).highlighted());
        // Scores ordenados do maior para o menor percentual
        Assertions.assertEquals(KnowledgeArea.BIOLOGICAS, result.scores().getFirst().area());
        Assertions.assertEquals(List.of("Saúde", "Meio Ambiente"), result.suggestions().stream().map(s -> s.name()).toList());
        Assertions.assertEquals(JourneyResultService.DISCLAIMER, result.disclaimer());
        Assertions.assertEquals(journey.getJourneyNumber(), result.journeyNumber());
        // Jornada concluída usa o máximo guardado, sem consultar as perguntas atuais
        Mockito.verify(questionRepository, Mockito.never()).findAll();
    }

    @Test
    void resultForShouldNotChangeWhenQuestionsChangeAfterJourneyIsConcluded() {
        setStoredMaxScores();
        setScores(8.0, 11.0, 11.0, 4.0);
        // Depois da conclusão, as perguntas passaram a valer muito mais em Biológicas
        Question changed = new Question(9L, "Nova pergunta", QuestionType.MULTIPLA_ESCOLHA, TrailFactory.createTrail(), new ArrayList<>());
        changed.getOptions().add(new QuestionOption(9L, "A", 0.0, 100.0, 0.0, 0.0, changed));
        Mockito.lenient().when(questionRepository.findAll()).thenReturn(List.of(changed));
        Mockito.when(subareaRepository.findByAreaInOrderByAreaAscDisplayOrderAsc(List.of(KnowledgeArea.BIOLOGICAS))).thenReturn(List.of());

        JourneyResultDTO result = service.resultFor(journey);

        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), result.highlightedAreas());
        Assertions.assertEquals(100.0, scoreOf(result, KnowledgeArea.BIOLOGICAS).percentage());
        Assertions.assertEquals(11.0, scoreOf(result, KnowledgeArea.BIOLOGICAS).maxScore());
        Mockito.verify(questionRepository, Mockito.never()).findAll();
    }

    @Test
    void resultForShouldUseCurrentQuestionsWhenJourneyHasNoStoredMaxScores() {
        // Jornada concluída antes do RF-12: sem máximo guardado
        mockQuestions();
        setScores(8.0, 11.0, 11.0, 4.0);
        Mockito.when(subareaRepository.findByAreaInOrderByAreaAscDisplayOrderAsc(List.of(KnowledgeArea.BIOLOGICAS))).thenReturn(List.of());

        JourneyResultDTO result = service.resultFor(journey);

        Assertions.assertFalse(journey.hasMaxScores());
        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), result.highlightedAreas());
        Assertions.assertEquals(17.0, scoreOf(result, KnowledgeArea.EXATAS).maxScore());
    }

    @Test
    void resultForShouldHighlightAllTiedAreasAndSuggestSubareasOfEachOne() {
        setStoredMaxScores();
        // Humanas 8 de 16 e Artes 4 de 8: as duas com 50%
        setScores(8.0, 1.0, 1.0, 4.0);
        Mockito.when(subareaRepository.findByAreaInOrderByAreaAscDisplayOrderAsc(List.of(KnowledgeArea.HUMANAS, KnowledgeArea.ARTES)))
                .thenReturn(List.of(subarea(1L, KnowledgeArea.ARTES, "Design", 1), subarea(2L, KnowledgeArea.HUMANAS, "Direito", 1)));

        JourneyResultDTO result = service.resultFor(journey);

        Assertions.assertEquals(List.of(KnowledgeArea.HUMANAS, KnowledgeArea.ARTES), result.highlightedAreas());
        Assertions.assertTrue(scoreOf(result, KnowledgeArea.HUMANAS).highlighted());
        Assertions.assertTrue(scoreOf(result, KnowledgeArea.ARTES).highlighted());
        // Sugestões seguem a ordem das áreas em destaque
        Assertions.assertEquals(List.of("Direito", "Design"), result.suggestions().stream().map(s -> s.name()).toList());
    }

    @Test
    void resultForShouldNotHighlightAnyAreaWhenAllScoresAreZero() {
        setStoredMaxScores();
        setScores(0.0, 0.0, 0.0, 0.0);

        JourneyResultDTO result = service.resultFor(journey);

        Assertions.assertTrue(result.highlightedAreas().isEmpty());
        Assertions.assertTrue(result.suggestions().isEmpty());
        Assertions.assertTrue(result.scores().stream().noneMatch(AreaScoreDTO::highlighted));
        Mockito.verify(subareaRepository, Mockito.never()).findByAreaInOrderByAreaAscDisplayOrderAsc(any());
    }

    @Test
    void findMyCurrentResultShouldReturnResultOfConcludedCurrentJourney() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.findByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(Optional.of(journey));
        setStoredMaxScores();
        Mockito.when(subareaRepository.findByAreaInOrderByAreaAscDisplayOrderAsc(any())).thenReturn(List.of());

        JourneyResultDTO result = service.findMyCurrentResult();

        Assertions.assertEquals(journey.getId(), result.journeyId());
        Assertions.assertEquals(4, result.scores().size());
    }

    @Test
    void findMyCurrentResultShouldThrowBusinessExceptionWhenCurrentJourneyIsNotConcluded() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.findByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class, () -> service.findMyCurrentResult());
        Mockito.verify(questionRepository, Mockito.never()).findAll();
    }

    @Test
    void findMyJourneyResultShouldThrowResourceNotFoundExceptionWhenJourneyIsNotFromLoggedStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(journeyRecordRepository.findByIdAndStudentId(99L, student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findMyJourneyResult(99L));
    }
}
