package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.UserDTO;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class TrailServiceTests {

    @InjectMocks
    private TrailService service;

    @Mock
    private TrailRepository trailRepository;

    @Mock
    private UserService userService;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private SynthesisRepository synthesisRepository;

    @Mock
    private StudentRepository studentRepository;

    private Long studentId, nonExistingTrailId;
    private Trail firstTrail, secondTrail;
    private Question firstQuestion, secondQuestion;

    @BeforeEach
    void setUp() {
        studentId = 1L;
        nonExistingTrailId = 100L;

        firstTrail = new Trail(1L, TrailName.AUTOCONHECIMENTO, 1, new ArrayList<>());
        secondTrail = new Trail(2L, TrailName.INFLUENCIAS, 2, new ArrayList<>());

        firstQuestion = new Question(1L, "Qual área você mais gosta?", QuestionType.MULTIPLA_ESCOLHA, firstTrail, new ArrayList<>());
        secondQuestion = new Question(2L, "Com quais atividades você se identifica?", QuestionType.CHECKBOX, firstTrail, new ArrayList<>());
        firstTrail.getQuestions().add(firstQuestion);
        firstTrail.getQuestions().add(secondQuestion);
    }

    private static Answer answerTo(Question question) {
        QuestionOption option = new QuestionOption();
        option.setQuestion(question);
        Answer answer = new Answer();
        answer.setQuestionOption(option);
        return answer;
    }

    private void mockAccessToSecondTrail() {
        Mockito.when(trailRepository.findById(secondTrail.getId())).thenReturn(Optional.of(secondTrail));
        Mockito.when(trailRepository.findBySequentialOrder(1)).thenReturn(Optional.of(firstTrail));
        Mockito.when(userService.findMe()).thenReturn(new UserDTO(studentId, "Ana Souza", "ana.souza@gmail.com", Set.of()));
    }

    private void mockAccessToFirstTrail(Optional<Student> loggedStudent) {
        Mockito.when(trailRepository.findById(firstTrail.getId())).thenReturn(Optional.of(firstTrail));
        Mockito.when(userService.findMe()).thenReturn(new UserDTO(studentId, "Ana Souza", "ana.souza@gmail.com", Set.of()));
        Mockito.when(studentRepository.findById(studentId)).thenReturn(loggedStudent);
    }

    @Test
    void canAccessShouldReturnTrueWhenTrailIsTheFirstOneAndStudentCompletedProfile() {
        mockAccessToFirstTrail(Optional.of(StudentFactory.createStudentWithCompletedProfile()));

        Assertions.assertTrue(service.canAccess(firstTrail.getId()));
        Mockito.verify(answerRepository, Mockito.never()).findByStudentAndTrail(any(), any());
    }

    @Test
    void canAccessShouldReturnFalseWhenTrailIsTheFirstOneAndStudentDidNotCompleteProfile() {
        mockAccessToFirstTrail(Optional.of(StudentFactory.createStudent()));

        Assertions.assertFalse(service.canAccess(firstTrail.getId()));
    }

    @Test
    void canAccessShouldReturnTrueWhenTrailIsTheFirstOneAndLoggedUserIsNotAStudent() {
        // Admin e psicóloga não preenchem perfil de aluno e continuam consultando as perguntas da primeira trilha
        mockAccessToFirstTrail(Optional.empty());

        Assertions.assertTrue(service.canAccess(firstTrail.getId()));
    }

    @Test
    void canAccessShouldReturnTrueWhenPreviousTrailHasAllQuestionsAnsweredAndSynthesisSubmitted() {
        mockAccessToSecondTrail();
        Mockito.when(answerRepository.findByStudentAndTrail(studentId, firstTrail.getId()))
                .thenReturn(List.of(answerTo(firstQuestion), answerTo(secondQuestion)));
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(studentId, firstTrail.getId())).thenReturn(true);

        Assertions.assertTrue(service.canAccess(secondTrail.getId()));
    }

    @Test
    void canAccessShouldReturnFalseWhenPreviousTrailHasAllQuestionsAnsweredButSynthesisWasNotSubmitted() {
        mockAccessToSecondTrail();
        Mockito.when(answerRepository.findByStudentAndTrail(studentId, firstTrail.getId()))
                .thenReturn(List.of(answerTo(firstQuestion), answerTo(secondQuestion)));
        Mockito.when(synthesisRepository.existsByStudentIdAndTrailId(studentId, firstTrail.getId())).thenReturn(false);

        Assertions.assertFalse(service.canAccess(secondTrail.getId()));
    }

    @Test
    void canAccessShouldReturnFalseWhenPreviousTrailHasUnansweredQuestions() {
        mockAccessToSecondTrail();
        Mockito.when(answerRepository.findByStudentAndTrail(studentId, firstTrail.getId()))
                .thenReturn(List.of(answerTo(firstQuestion)));

        Assertions.assertFalse(service.canAccess(secondTrail.getId()));
        Mockito.verify(synthesisRepository, Mockito.never()).existsByStudentIdAndTrailId(any(), any());
    }

    @Test
    void canAccessShouldThrowResourceNotFoundExceptionWhenTrailDoesNotExist() {
        Mockito.when(trailRepository.findById(nonExistingTrailId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.canAccess(nonExistingTrailId));
    }

    @Test
    void areAllQuestionsAnsweredShouldReturnTrueWhenTrailHasNoQuestions() {
        Trail informationTrail = new Trail(4L, TrailName.INFORMACAO, 4, new ArrayList<>());
        Mockito.when(answerRepository.findByStudentAndTrail(studentId, informationTrail.getId())).thenReturn(List.of());

        Assertions.assertTrue(service.areAllQuestionsAnswered(informationTrail, studentId));
    }
}
