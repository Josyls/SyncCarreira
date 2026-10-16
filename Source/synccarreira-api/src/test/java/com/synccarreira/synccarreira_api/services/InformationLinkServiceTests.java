package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.InformationLinkDTO;
import com.synccarreira.synccarreira_api.dto.InformationLinkInsertDTO;
import com.synccarreira.synccarreira_api.dto.InformationTrailDTO;
import com.synccarreira.synccarreira_api.entities.InformationLink;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import com.synccarreira.synccarreira_api.repositories.InformationLinkRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
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
public class InformationLinkServiceTests {

    private InformationLinkService service;

    @Mock
    private InformationLinkRepository informationLinkRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TrailRepository trailRepository;

    @Mock
    private TrailService trailService;

    @Mock
    private AuthService authService;

    @Mock
    private QuestionRepository questionRepository;

    private Student student;
    private Trail informationTrail;
    private InformationLink biologicalLink, humanitiesLink, prouniLink;

    @BeforeEach
    void setUp() {
        // Usa o cálculo real de percentual (RF-12), com as perguntas mockadas
        service = new InformationLinkService(informationLinkRepository, studentRepository, trailRepository, trailService, authService,
                new AreaScoreCalculator(questionRepository));

        // Na factory: humanas 7.5, exatas 6.0, biológicas 8.25 e artes 5.0 (maior score bruto: biológicas)
        student = StudentFactory.createStudent();

        informationTrail = new Trail();
        informationTrail.setId(4L);
        informationTrail.setName(TrailName.INFORMACAO);
        informationTrail.setSequentialOrder(4);

        biologicalLink = new InformationLink(1L, "Educação Física", "https://www.confef.org.br/", KnowledgeArea.BIOLOGICAS);
        humanitiesLink = new InformationLink(2L, "Direito", "https://www.aurum.com.br/blog/direito-digital/", KnowledgeArea.HUMANAS);
        prouniLink = new InformationLink(3L, "ProUni", "https://acessounico.mec.gov.br/prouni", null);
    }

    // Uma pergunta com peso máximo informado para cada área (o máximo possível soma a afinidade máxima, 5)
    private void mockQuestionWithMaxWeights(double humanities, double biologicalSciences, double exactSciences, double arts) {
        Question question = new Question(1L, "Pergunta", QuestionType.MULTIPLA_ESCOLHA, TrailFactory.createTrail(), new ArrayList<>());
        question.getOptions().add(new QuestionOption(1L, "A", humanities, biologicalSciences, exactSciences, arts, question));
        Mockito.when(questionRepository.findAll()).thenReturn(List.of(question));
    }

    private void mockAccessToInformationTrail(boolean canAccess) {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findByName(TrailName.INFORMACAO)).thenReturn(Optional.of(informationTrail));
        Mockito.when(trailService.canAccess(informationTrail.getId())).thenReturn(canAccess);
    }

    @Test
    void findForLoggedStudentShouldReturnLinksOfHighestPercentageAreaAndUniversityAccessLinks() {
        mockAccessToInformationTrail(true);
        // Mesmo máximo (15) em todas as áreas: o maior percentual coincide com o maior score bruto
        mockQuestionWithMaxWeights(10.0, 10.0, 10.0, 10.0);
        Mockito.when(informationLinkRepository.findByKnowledgeAreaInOrderByTopicAscIdAsc(List.of(KnowledgeArea.BIOLOGICAS)))
                .thenReturn(List.of(biologicalLink));
        Mockito.when(informationLinkRepository.findByKnowledgeAreaIsNullOrderByTopicAscIdAsc()).thenReturn(List.of(prouniLink));

        InformationTrailDTO result = service.findForLoggedStudent();

        Assertions.assertEquals(List.of(KnowledgeArea.BIOLOGICAS), result.recommendedAreas());
        Assertions.assertEquals(student.getBiologicalSciencesScore(), result.score().biologicalSciencesScore());
        Assertions.assertEquals(4, result.areaScores().size());
        Assertions.assertEquals(KnowledgeArea.BIOLOGICAS, result.areaScores().getFirst().area());
        Assertions.assertEquals(55.0, result.areaScores().getFirst().percentage());
        Assertions.assertEquals(1, result.careerLinks().size());
        Assertions.assertEquals("Educação Física", result.careerLinks().getFirst().topic());
        Assertions.assertEquals(1, result.universityAccessLinks().size());
        Assertions.assertEquals("ProUni", result.universityAccessLinks().getFirst().topic());
        Assertions.assertNull(result.universityAccessLinks().getFirst().knowledgeArea());
    }

    @Test
    void findForLoggedStudentShouldReturnLinksOfAllTiedAreasWhenScoresAreTied() {
        student.setHumanitiesScore(student.getBiologicalSciencesScore());
        mockAccessToInformationTrail(true);
        mockQuestionWithMaxWeights(10.0, 10.0, 10.0, 10.0);
        List<KnowledgeArea> tiedAreas = List.of(KnowledgeArea.HUMANAS, KnowledgeArea.BIOLOGICAS);
        Mockito.when(informationLinkRepository.findByKnowledgeAreaInOrderByTopicAscIdAsc(tiedAreas))
                .thenReturn(List.of(humanitiesLink, biologicalLink));
        Mockito.when(informationLinkRepository.findByKnowledgeAreaIsNullOrderByTopicAscIdAsc()).thenReturn(List.of(prouniLink));

        InformationTrailDTO result = service.findForLoggedStudent();

        Assertions.assertEquals(tiedAreas, result.recommendedAreas());
        Assertions.assertEquals(2, result.careerLinks().size());
    }

    @Test
    void findForLoggedStudentShouldUsePercentageInsteadOfRawScore() {
        mockAccessToInformationTrail(true);
        // Biológicas tem o maior score bruto (8.25), mas o máximo dela é 45: 18,3%. Humanas: 7.5 de 15 = 50%
        mockQuestionWithMaxWeights(10.0, 40.0, 10.0, 10.0);
        Mockito.when(informationLinkRepository.findByKnowledgeAreaInOrderByTopicAscIdAsc(List.of(KnowledgeArea.HUMANAS)))
                .thenReturn(List.of(humanitiesLink));
        Mockito.when(informationLinkRepository.findByKnowledgeAreaIsNullOrderByTopicAscIdAsc()).thenReturn(List.of(prouniLink));

        InformationTrailDTO result = service.findForLoggedStudent();

        Assertions.assertEquals(List.of(KnowledgeArea.HUMANAS), result.recommendedAreas());
        Assertions.assertEquals("Direito", result.careerLinks().getFirst().topic());
        Assertions.assertTrue(result.areaScores().getFirst().highlighted());
    }

    @Test
    void findForLoggedStudentShouldReturnLinksOfAllAreasWhenStudentHasNoScore() {
        student.setHumanitiesScore(0.0);
        student.setExactSciencesScore(0.0);
        student.setBiologicalSciencesScore(0.0);
        student.setArtsScore(0.0);
        mockAccessToInformationTrail(true);
        mockQuestionWithMaxWeights(10.0, 10.0, 10.0, 10.0);
        List<KnowledgeArea> allAreas = List.of(KnowledgeArea.values());
        Mockito.when(informationLinkRepository.findByKnowledgeAreaInOrderByTopicAscIdAsc(allAreas))
                .thenReturn(List.of(humanitiesLink, biologicalLink));
        Mockito.when(informationLinkRepository.findByKnowledgeAreaIsNullOrderByTopicAscIdAsc()).thenReturn(List.of(prouniLink));

        InformationTrailDTO result = service.findForLoggedStudent();

        Assertions.assertEquals(allAreas, result.recommendedAreas());
        Assertions.assertTrue(result.areaScores().stream().noneMatch(score -> score.highlighted()));
    }

    @Test
    void findForLoggedStudentShouldThrowForbiddenExceptionWhenPreviousTrailsAreNotConcluded() {
        mockAccessToInformationTrail(false);

        Assertions.assertThrows(ForbiddenException.class, () -> service.findForLoggedStudent());
        Mockito.verify(informationLinkRepository, Mockito.never()).findByKnowledgeAreaInOrderByTopicAscIdAsc(any());
    }

    @Test
    void findForLoggedStudentShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findForLoggedStudent());
    }

    @Test
    void findForLoggedStudentShouldThrowResourceNotFoundExceptionWhenInformationTrailDoesNotExist() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Mockito.when(trailRepository.findByName(TrailName.INFORMACAO)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findForLoggedStudent());
    }

    @Test
    void findAllShouldReturnAllLinks() {
        Mockito.when(informationLinkRepository.findAllByOrderByTopicAscIdAsc())
                .thenReturn(List.of(humanitiesLink, biologicalLink, prouniLink));

        List<InformationLinkDTO> result = service.findAll();

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("Direito", result.getFirst().topic());
        Assertions.assertNull(result.get(2).knowledgeArea());
    }

    @Test
    void createShouldSaveTrimmedLinkAndReturnDTO() {
        InformationLinkInsertDTO dto = new InformationLinkInsertDTO("  Medicina  ", " https://www.cfm.org.br/ ", KnowledgeArea.BIOLOGICAS);
        Mockito.when(informationLinkRepository.save(any())).thenAnswer(invocation -> {
            InformationLink saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        InformationLinkDTO result = service.create(dto);

        Assertions.assertEquals(10L, result.id());
        Assertions.assertEquals("Medicina", result.topic());
        Assertions.assertEquals("https://www.cfm.org.br/", result.url());
        Assertions.assertEquals(KnowledgeArea.BIOLOGICAS, result.knowledgeArea());
    }

    @Test
    void createShouldSaveUniversityAccessLinkWhenKnowledgeAreaIsNull() {
        InformationLinkInsertDTO dto = new InformationLinkInsertDTO("Vestibular", "https://www.vestibular.com.br/", null);
        Mockito.when(informationLinkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InformationLinkDTO result = service.create(dto);

        Assertions.assertNull(result.knowledgeArea());
    }

    @Test
    void deleteShouldDeleteLinkWhenIdExists() {
        Mockito.when(informationLinkRepository.findById(1L)).thenReturn(Optional.of(biologicalLink));

        Assertions.assertDoesNotThrow(() -> service.delete(1L));
        Mockito.verify(informationLinkRepository).delete(biologicalLink);
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
        Mockito.when(informationLinkRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
        Mockito.verify(informationLinkRepository, Mockito.never()).delete(any());
    }
}
