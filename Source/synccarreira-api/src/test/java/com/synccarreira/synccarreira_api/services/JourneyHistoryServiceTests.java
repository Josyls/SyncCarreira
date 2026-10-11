package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.journey.JourneyContent;
import com.synccarreira.synccarreira_api.dto.journey.JourneyRecordDTO;
import com.synccarreira.synccarreira_api.dto.journey.JourneyStatusDTO;
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
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.JourneyFactory;
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

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private JourneyHistoryService service;

    private Student student;
    private Trail firstTrail, informationTrail;
    private Question question;
    private QuestionOption option;

    @BeforeEach
    void setUp() {
        service = new JourneyHistoryService(studentRepository, journeyRecordRepository, trailRepository, answerRepository,
                synthesisRepository, informationLinkRepository, authService, objectMapper);

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
    void findMyJourneysShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findMyJourneys());
    }
}
