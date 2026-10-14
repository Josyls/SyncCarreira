package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.journey.JourneyContent;
import com.synccarreira.synccarreira_api.dto.journey.JourneyPdfData;
import com.synccarreira.synccarreira_api.dto.result.JourneyResultDTO;
import com.synccarreira.synccarreira_api.dto.journey.JourneyRecordDTO;
import com.synccarreira.synccarreira_api.dto.journey.JourneyStatusDTO;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportFile;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.InformationLink;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.JourneyRecord;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Synthesis;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.enums.InstitutionType;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.InformationLinkRepository;
import com.synccarreira.synccarreira_api.repositories.JourneyRecordRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.services.reports.JourneyPdfWriter;
import com.synccarreira.synccarreira_api.tests.JourneyFactory;
import com.synccarreira.synccarreira_api.tests.ResultFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
public class JourneyHistoryServiceTests {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private JourneyRecordRepository journeyRecordRepository;

    @Mock
    private TrailRepository trailRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private SynthesisRepository synthesisRepository;

    @Mock
    private InformationLinkRepository informationLinkRepository;

    @Mock
    private AuthService authService;

    @Mock
    private JourneyPdfWriter journeyPdfWriter;

    @Mock
    private JourneyResultService journeyResultService;

    @Mock
    private QuestionRepository questionRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private JourneyHistoryService service;

    private Student student;
    private Trail firstTrail, informationTrail;
    private Question question;
    private QuestionOption option;

    @BeforeEach
    void setUp() {
        service = new JourneyHistoryService(studentRepository, journeyRecordRepository, trailRepository, answerRepository,
                synthesisRepository, informationLinkRepository, authService, objectMapper, journeyPdfWriter, journeyResultService,
                new AreaScoreCalculator(questionRepository));

        // Scores: humanas 7.5, exatas 6.0, biológicas 8.25, artes 5.0 (maior: biológicas)
        student = StudentFactory.createStudentWithCompletedProfile();
        Institution institution = new Institution(1L, "Escola Estadual Central LTDA", "Escola Estadual Central", "11222333000181",
                InstitutionType.PUBLICA, true, Instant.now());
        SchoolClass schoolClass = new SchoolClass(1L, "3º A", 2026, Instant.now());
        schoolClass.setInstitution(institution);
        student.setDeterminedSchoolClass(schoolClass);

        firstTrail = new Trail(1L, TrailName.AUTOCONHECIMENTO, 1, new ArrayList<>());
        informationTrail = new Trail(4L, TrailName.INFORMACAO, 2, new ArrayList<>());
        question = new Question(10L, JourneyFactory.QUESTION, QuestionType.MULTIPLA_ESCOLHA, firstTrail, new ArrayList<>());
        option = new QuestionOption(100L, JourneyFactory.ANSWER, 0.0, 0.0, 0.0, 10.0, question);
        question.getOptions().add(option);
        firstTrail.getQuestions().add(question);
    }

    private void mockLoggedStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
    }

    private Synthesis synthesisOf(Trail trail, String content) {
        return new Synthesis(trail.getId(), content, student, trail, JourneyFactory.CONCLUDED_AT);
    }

    // Banco de perguntas com o peso máximo informado para cada área (o máximo possível soma a afinidade máxima, 5)
    private void mockQuestionBank(double humanities, double biologicalSciences, double exactSciences, double arts) {
        Question bankQuestion = new Question(20L, "Pergunta", QuestionType.MULTIPLA_ESCOLHA, firstTrail, new ArrayList<>());
        bankQuestion.getOptions().add(new QuestionOption(200L, "A", humanities, biologicalSciences, exactSciences, arts, bankQuestion));
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(bankQuestion));
    }

    private void mockConcludedJourney() {
        Mockito.when(journeyRecordRepository.existsByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(false);
        Mockito.when(trailRepository.findAllByOrderBySequentialOrderAsc()).thenReturn(List.of(firstTrail, informationTrail));
        Mockito.when(synthesisRepository.findByStudentId(student.getId())).thenReturn(List.of(
                synthesisOf(firstTrail, JourneyFactory.SYNTHESIS),
                synthesisOf(informationTrail, "Síntese final")));
    }

    @Test
    void registerJourneyIfConcludedShouldSaveRecordWithSnapshotOfJourney() {
        mockConcludedJourney();
        // Mesmo máximo (15) em todas as áreas: o maior percentual coincide com o maior score bruto
        mockQuestionBank(10.0, 10.0, 10.0, 10.0);
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of(new Answer(1L, student, option)));
        Mockito.when(informationLinkRepository.findByKnowledgeAreaInOrderByTopicAscIdAsc(List.of(KnowledgeArea.BIOLOGICAS)))
                .thenReturn(List.of(new InformationLink(1L, "Cursos de Biologia", "https://exemplo.com/bio", KnowledgeArea.BIOLOGICAS)));
        Mockito.when(informationLinkRepository.findByKnowledgeAreaIsNullOrderByTopicAscIdAsc())
                .thenReturn(List.of(new InformationLink(2L, "SiSU", "https://acessounico.mec.gov.br/sisu", null)));

        service.registerJourneyIfConcluded(student);

        ArgumentCaptor<JourneyRecord> captor = ArgumentCaptor.forClass(JourneyRecord.class);
        Mockito.verify(journeyRecordRepository).save(captor.capture());
        JourneyRecord journey = captor.getValue();
        Assertions.assertEquals(1, journey.getJourneyNumber());
        Assertions.assertEquals(student.getProfileCompletedAt(), journey.getStartedAt());
        Assertions.assertNotNull(journey.getConcludedAt());
        Assertions.assertEquals("3º A", journey.getSchoolClassName());
        Assertions.assertEquals(2026, journey.getSchoolYear());
        Assertions.assertEquals("Escola Estadual Central", journey.getInstitutionName());
        Assertions.assertEquals(8.25, journey.getBiologicalSciencesScore());
        Assertions.assertEquals(5, journey.getArtsAffinity());
        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), journey.highestScoreAreaList());
        Assertions.assertEquals(15.0, journey.getBiologicalSciencesMaxScore());
        Assertions.assertTrue(journey.hasMaxScores());
        Assertions.assertFalse(journey.getInDoubt());

        JourneyContent content = objectMapper.readValue(journey.getContent(), JourneyContent.class);
        Assertions.assertEquals(2, content.trails().size());
        JourneyContent.TrailContent trail = content.trails().getFirst();
        Assertions.assertEquals(TrailName.AUTOCONHECIMENTO, trail.trailName());
        Assertions.assertEquals(JourneyFactory.QUESTION, trail.answers().getFirst().question());
        Assertions.assertEquals(JourneyFactory.ANSWER, trail.answers().getFirst().answer());
        Assertions.assertEquals(JourneyFactory.SYNTHESIS, trail.synthesis());
        Assertions.assertEquals(2, content.links().size());
    }

    @Test
    void registerJourneyIfConcludedShouldStoreMaxScoresAndHighlightAreaWithHighestPercentage() {
        mockConcludedJourney();
        // Biológicas tem o maior score bruto (8.25), mas o máximo dela é 45: 18,3%. Humanas: 7.5 de 15 = 50%
        mockQuestionBank(10.0, 40.0, 10.0, 10.0);
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());
        Mockito.when(informationLinkRepository.findByKnowledgeAreaInOrderByTopicAscIdAsc(List.of(KnowledgeArea.HUMANAS)))
                .thenReturn(List.of(new InformationLink(3L, "Direito", "https://exemplo.com/direito", KnowledgeArea.HUMANAS)));

        service.registerJourneyIfConcluded(student);

        ArgumentCaptor<JourneyRecord> captor = ArgumentCaptor.forClass(JourneyRecord.class);
        Mockito.verify(journeyRecordRepository).save(captor.capture());
        JourneyRecord journey = captor.getValue();
        Assertions.assertEquals(List.of(KnowledgeArea.HUMANAS), journey.highestScoreAreaList());
        Assertions.assertEquals(15.0, journey.getHumanitiesMaxScore());
        Assertions.assertEquals(45.0, journey.getBiologicalSciencesMaxScore());
        Assertions.assertEquals(15.0, journey.getExactSciencesMaxScore());
        Assertions.assertEquals(15.0, journey.getArtsMaxScore());
        JourneyContent content = objectMapper.readValue(journey.getContent(), JourneyContent.class);
        Assertions.assertEquals("Direito", content.links().getFirst().topic());
    }

    @Test
    void registerJourneyIfConcludedShouldUseStartOfCurrentJourneyWhenItIsNotTheFirstOne() {
        Instant startedAt = Instant.parse("2027-02-10T10:00:00Z");
        student.setCurrentJourneyNumber(2);
        student.setCurrentJourneyStartedAt(startedAt);
        Mockito.when(journeyRecordRepository.existsByStudentIdAndJourneyNumber(student.getId(), 2)).thenReturn(false);
        Mockito.when(trailRepository.findAllByOrderBySequentialOrderAsc()).thenReturn(List.of(informationTrail));
        Mockito.when(synthesisRepository.findByStudentId(student.getId())).thenReturn(List.of(synthesisOf(informationTrail, "Síntese final")));
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());

        service.registerJourneyIfConcluded(student);

        ArgumentCaptor<JourneyRecord> captor = ArgumentCaptor.forClass(JourneyRecord.class);
        Mockito.verify(journeyRecordRepository).save(captor.capture());
        Assertions.assertEquals(2, captor.getValue().getJourneyNumber());
        Assertions.assertEquals(startedAt, captor.getValue().getStartedAt());
    }

    @Test
    void registerJourneyIfConcludedShouldDoNothingWhenSomeTrailHasNoSynthesis() {
        Mockito.when(journeyRecordRepository.existsByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(false);
        Mockito.when(trailRepository.findAllByOrderBySequentialOrderAsc()).thenReturn(List.of(firstTrail, informationTrail));
        Mockito.when(synthesisRepository.findByStudentId(student.getId())).thenReturn(List.of(synthesisOf(firstTrail, JourneyFactory.SYNTHESIS)));

        service.registerJourneyIfConcluded(student);

        Mockito.verify(journeyRecordRepository, Mockito.never()).save(any());
    }

    @Test
    void registerJourneyIfConcludedShouldDoNothingWhenJourneyWasAlreadyRegistered() {
        Mockito.when(journeyRecordRepository.existsByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(true);

        service.registerJourneyIfConcluded(student);

        Mockito.verify(trailRepository, Mockito.never()).findAllByOrderBySequentialOrderAsc();
        Mockito.verify(journeyRecordRepository, Mockito.never()).save(any());
    }

    @Test
    void findMyJourneysShouldUseCurrentDoubtOnlyForCurrentJourney() {
        mockLoggedStudent();
        student.setCurrentJourneyNumber(2);
        student.setInDoubt(true);
        student.setDoubtFlaggedAt(JourneyFactory.CONCLUDED_AT);
        JourneyRecord previous = JourneyFactory.createJourneyRecord(student, 1, "{}");
        JourneyRecord current = JourneyFactory.createJourneyRecord(student, 2, "{}");
        Mockito.when(journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId())).thenReturn(List.of(previous, current));

        List<JourneyRecordDTO> result = service.findMyJourneys();

        Assertions.assertEquals(2, result.size());
        Assertions.assertFalse(result.get(0).inDoubt());
        Assertions.assertTrue(result.get(1).inDoubt());
        Assertions.assertEquals(List.of(KnowledgeArea.ARTES), result.get(1).highestScoreAreas());
        Assertions.assertEquals(5, result.get(1).affinities().arts());
    }

    @Test
    void findMyCurrentJourneyShouldInformWhenCurrentJourneyIsConcluded() {
        mockLoggedStudent();
        Mockito.when(journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId()))
                .thenReturn(List.of(JourneyFactory.createJourneyRecord(student, 1, "{}")));

        JourneyStatusDTO result = service.findMyCurrentJourney();

        Assertions.assertEquals(1, result.currentJourneyNumber());
        Assertions.assertTrue(result.currentJourneyConcluded());
        Assertions.assertEquals(1, result.concludedJourneys());
        Assertions.assertEquals(student.getProfileCompletedAt(), result.currentJourneyStartedAt());
    }

    @Test
    void findMyCurrentJourneyShouldInformWhenCurrentJourneyIsInProgress() {
        mockLoggedStudent();
        Mockito.when(journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId())).thenReturn(List.of());

        JourneyStatusDTO result = service.findMyCurrentJourney();

        Assertions.assertFalse(result.currentJourneyConcluded());
        Assertions.assertEquals(0, result.concludedJourneys());
    }

    @Test
    void startNewJourneyShouldKeepHistoryAndResetCurrentJourney() {
        mockLoggedStudent();
        student.setInDoubt(true);
        student.setDoubtFlaggedAt(JourneyFactory.CONCLUDED_AT);
        JourneyRecord concluded = JourneyFactory.createJourneyRecord(student, 1, "{}");
        Mockito.when(journeyRecordRepository.findByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(Optional.of(concluded));
        Mockito.when(studentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId())).thenReturn(List.of(concluded));

        JourneyStatusDTO result = service.startNewJourney();

        // A dúvida sinalizada depois da conclusão fica registrada no histórico
        Assertions.assertTrue(concluded.getInDoubt());
        Assertions.assertEquals(JourneyFactory.CONCLUDED_AT, concluded.getDoubtFlaggedAt());
        Mockito.verify(journeyRecordRepository).save(concluded);
        Mockito.verify(answerRepository).deleteAllByStudentId(student.getId());
        Mockito.verify(synthesisRepository).deleteAllByStudentId(student.getId());

        Assertions.assertEquals(2, student.getCurrentJourneyNumber());
        Assertions.assertNotNull(student.getCurrentJourneyStartedAt());
        Assertions.assertFalse(student.getInDoubt());
        Assertions.assertNull(student.getDoubtFlaggedAt());
        // Sem respostas, o score volta a ser apenas as afinidades do perfil (3, 2, 4, 5)
        Assertions.assertEquals(3.0, student.getHumanitiesScore());
        Assertions.assertEquals(2.0, student.getExactSciencesScore());
        Assertions.assertEquals(4.0, student.getBiologicalSciencesScore());
        Assertions.assertEquals(5.0, student.getArtsScore());

        Assertions.assertEquals(2, result.currentJourneyNumber());
        Assertions.assertFalse(result.currentJourneyConcluded());
        Assertions.assertEquals(1, result.concludedJourneys());
    }

    @Test
    void startNewJourneyShouldThrowBusinessExceptionWhenCurrentJourneyIsNotConcluded() {
        mockLoggedStudent();
        Mockito.when(journeyRecordRepository.findByStudentIdAndJourneyNumber(student.getId(), 1)).thenReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class, () -> service.startNewJourney());
        Mockito.verify(answerRepository, Mockito.never()).deleteAllByStudentId(anyLong());
        Mockito.verify(synthesisRepository, Mockito.never()).deleteAllByStudentId(anyLong());
        Mockito.verify(studentRepository, Mockito.never()).save(any());
    }

    @Test
    void generateMyJourneyPdfShouldReturnPdfOfConcludedJourney() {
        mockLoggedStudent();
        String content = objectMapper.writeValueAsString(JourneyFactory.createJourneyContent());
        JourneyRecord journey = JourneyFactory.createJourneyRecord(student, 1, content);
        Mockito.when(journeyRecordRepository.findByIdAndStudentId(1L, student.getId())).thenReturn(Optional.of(journey));
        byte[] pdf = new byte[] { 1, 2, 3 };
        Mockito.when(journeyPdfWriter.write(any())).thenReturn(pdf);
        JourneyResultDTO journeyResult = ResultFactory.createJourneyResult();
        Mockito.when(journeyResultService.resultFor(journey)).thenReturn(journeyResult);

        ReportFile result = service.generateMyJourneyPdf(1L);

        Assertions.assertEquals("minha-jornada-1-2026-10-01.pdf", result.getFileName());
        Assertions.assertArrayEquals(pdf, result.getContent());
        ArgumentCaptor<JourneyPdfData> captor = ArgumentCaptor.forClass(JourneyPdfData.class);
        Mockito.verify(journeyPdfWriter).write(captor.capture());
        Assertions.assertEquals(student.getName(), captor.getValue().studentName());
        Assertions.assertEquals(1, captor.getValue().journey().journeyNumber());
        Assertions.assertEquals(JourneyFactory.SYNTHESIS, captor.getValue().content().trails().getFirst().synthesis());
        Assertions.assertSame(journeyResult, captor.getValue().result());
    }

    @Test
    void generateMyJourneyPdfShouldThrowResourceNotFoundExceptionWhenJourneyIsNotFromLoggedStudent() {
        mockLoggedStudent();
        Mockito.when(journeyRecordRepository.findByIdAndStudentId(99L, student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.generateMyJourneyPdf(99L));
        Mockito.verify(journeyPdfWriter, Mockito.never()).write(any());
        Mockito.verify(journeyResultService, Mockito.never()).resultFor(any());
    }

    @Test
    void findMyJourneysShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findMyJourneys());
    }
}
