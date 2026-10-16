package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.dto.StudentProfileDTO;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class StudentProfileServiceTests {

    @InjectMocks
    private StudentProfileService service;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AuthService authService;

    private Student student;
    private StudentAffinityDTO affinityDTO;

    @BeforeEach
    void setUp() {
        student = StudentFactory.createStudent();
        affinityDTO = StudentFactory.createStudentAffinityDTO();
    }

    private void mockLoggedStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
    }

    private void mockSave() {
        Mockito.when(studentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
    void findMyProfileShouldReturnIncompleteProfileWhenAffinitiesWereNotAnswered() {
        mockLoggedStudent();

        StudentProfileDTO result = service.findMyProfile();

        Assertions.assertEquals(student.getId(), result.id());
        Assertions.assertFalse(result.profileCompleted());
        Assertions.assertNull(result.profileCompletedAt());
        Assertions.assertNull(result.affinities().humanities());
    }

    @Test
    void findMyProfileShouldReturnAffinitiesWhenProfileIsCompleted() {
        student = StudentFactory.createStudentWithCompletedProfile();
        mockLoggedStudent();

        StudentProfileDTO result = service.findMyProfile();

        Assertions.assertTrue(result.profileCompleted());
        Assertions.assertEquals(affinityDTO, result.affinities());
    }

    @Test
    void findMyProfileShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.findMyProfile());
    }

    @Test
    void updateMyAffinitiesShouldSaveAffinitiesAndCompleteProfile() {
        mockLoggedStudent();
        mockSave();
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());

        StudentProfileDTO result = service.updateMyAffinities(affinityDTO);

        Assertions.assertTrue(result.profileCompleted());
        Assertions.assertNotNull(result.profileCompletedAt());
        Assertions.assertEquals(affinityDTO, result.affinities());
        Mockito.verify(studentRepository).save(student);
    }

    @Test
    void updateMyAffinitiesShouldAddAffinitiesToScoresOfCurrentAnswers() {
        mockLoggedStudent();
        mockSave();
        Mockito.when(answerRepository.findByStudentId(student.getId()))
                .thenReturn(List.of(answerWithWeights(10.0, 1.0, 0.0, 2.5)));

        StudentProfileDTO result = service.updateMyAffinities(new StudentAffinityDTO(3, 2, 4, 5));

        Assertions.assertEquals(13.0, result.score().humanitiesScore());
        Assertions.assertEquals(3.0, result.score().exactSciencesScore());
        Assertions.assertEquals(4.0, result.score().biologicalSciencesScore());
        Assertions.assertEquals(7.5, result.score().artsScore());
    }

    @Test
    void updateMyAffinitiesShouldReplacePreviousAffinitiesWithoutAccumulating() {
        student = StudentFactory.createStudentWithCompletedProfile();
        Instant firstCompletion = student.getProfileCompletedAt();
        mockLoggedStudent();
        mockSave();
        Mockito.when(answerRepository.findByStudentId(student.getId())).thenReturn(List.of());

        StudentProfileDTO result = service.updateMyAffinities(new StudentAffinityDTO(0, 5, 1, 0));

        Assertions.assertEquals(0.0, result.score().humanitiesScore());
        Assertions.assertEquals(5.0, result.score().exactSciencesScore());
        Assertions.assertEquals(1.0, result.score().biologicalSciencesScore());
        Assertions.assertEquals(0.0, result.score().artsScore());
        // A data em que o perfil foi concluído pela primeira vez é mantida
        Assertions.assertEquals(firstCompletion, result.profileCompletedAt());
    }

    @Test
    void updateMyAffinitiesShouldThrowResourceNotFoundExceptionWhenLoggedUserIsNotAStudent() {
        Mockito.when(authService.authenticated()).thenReturn(student);
        Mockito.when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.updateMyAffinities(affinityDTO));
        Mockito.verify(studentRepository, Mockito.never()).save(any());
    }
}
