package com.synccarreira.synccarreira_api.repositories;

import com.synccarreira.synccarreira_api.entities.*;
import com.synccarreira.synccarreira_api.entities.enums.InstitutionType;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
public class AnswerRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private StudentRepository studentRepository;

    private Student ana, bruno;
    private Question checkboxQuestion, likertQuestion;
    private QuestionOption technology, arts, nature, agree;

    @BeforeEach
    void setUp() {
        Institution institution = entityManager.persist(
                new Institution(null, "Escola Estadual Pedro II", "Escola Estadual Pedro II", "33158816000105", InstitutionType.PUBLICA, Boolean.TRUE, Instant.now()));
        SchoolClass schoolClass = new SchoolClass(null, "3º ano A", 2026, Instant.now());
        schoolClass.setInstitution(institution);
        entityManager.persist(schoolClass);

        ana = persistStudent("Ana", "ana@gmail.com", schoolClass);
        bruno = persistStudent("Bruno", "bruno@gmail.com", schoolClass);

        Trail trail = new Trail();
        trail.setName(TrailName.AUTOCONHECIMENTO);
        trail.setSequentialOrder(99);
        entityManager.persist(trail);

        checkboxQuestion = persistQuestion("Quais assuntos te interessam?", QuestionType.CHECKBOX, trail);
        likertQuestion = persistQuestion("Tenho clareza sobre meu futuro.", QuestionType.LIKERT, trail);
        technology = persistOption("Tecnologia", checkboxQuestion);
        arts = persistOption("Artes", checkboxQuestion);
        nature = persistOption("Natureza", checkboxQuestion);
        agree = persistOption("Concordo", likertQuestion);

        // Ana respondeu a mesma pergunta várias vezes (situação gerada antes da substituição de respostas)
        persistAnswer(ana, technology);
        persistAnswer(ana, arts);
        persistAnswer(ana, nature);
        persistAnswer(ana, agree);
        persistAnswer(bruno, technology);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByStudentAndQuestionShouldReturnAnswersOfStudentToThatQuestionOrderedById() {
        List<Answer> result = answerRepository.findByStudentAndQuestion(ana.getId(), checkboxQuestion.getId());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(List.of(technology.getId(), arts.getId(), nature.getId()),
                result.stream().map(answer -> answer.getQuestionOption().getId()).toList());
        Assertions.assertTrue(result.getFirst().getId() < result.getLast().getId());
    }

    @Test
    void findByStudentAndQuestionShouldNotReturnAnswersOfOtherStudentsOrQuestions() {
        List<Answer> brunoAnswers = answerRepository.findByStudentAndQuestion(bruno.getId(), checkboxQuestion.getId());
        List<Answer> anaLikert = answerRepository.findByStudentAndQuestion(ana.getId(), likertQuestion.getId());

        Assertions.assertEquals(1, brunoAnswers.size());
        Assertions.assertEquals(technology.getId(), brunoAnswers.getFirst().getQuestionOption().getId());
        Assertions.assertEquals(1, anaLikert.size());
        Assertions.assertEquals(agree.getId(), anaLikert.getFirst().getQuestionOption().getId());
    }

    @Test
    void findByStudentAndQuestionShouldReturnEmptyWhenStudentHasNotAnsweredQuestion() {
        Assertions.assertTrue(answerRepository.findByStudentAndQuestion(bruno.getId(), likertQuestion.getId()).isEmpty());
    }

    @Test
    void findByIdForUpdateShouldReturnStudentLockingItsRow() {
        // Confere que a consulta com SELECT ... FOR UPDATE funciona no banco
        Student result = studentRepository.findByIdForUpdate(ana.getId()).orElseThrow();

        Assertions.assertEquals("Ana", result.getName());
        Assertions.assertTrue(studentRepository.findByIdForUpdate(9999L).isEmpty());
    }

    private Student persistStudent(String name, String email, SchoolClass schoolClass) {
        Student student = new Student();
        student.setName(name);
        student.setEmail(email);
        student.setDeterminedSchoolClass(schoolClass);
        return entityManager.persist(student);
    }

    private Question persistQuestion(String content, QuestionType type, Trail trail) {
        Question question = new Question();
        question.setContent(content);
        question.setQuestionType(type);
        question.setTrail(trail);
        return entityManager.persist(question);
    }

    private QuestionOption persistOption(String text, Question question) {
        QuestionOption option = new QuestionOption();
        option.setOptionText(text);
        option.setHumanitiesWeight(0.0);
        option.setBiologicalSciencesWeight(0.0);
        option.setExactSciencesWeight(0.0);
        option.setArtsWeight(0.0);
        option.setQuestion(question);
        return entityManager.persist(option);
    }

    private void persistAnswer(Student student, QuestionOption option) {
        Answer answer = new Answer();
        answer.setStudent(student);
        answer.setQuestionOption(option);
        entityManager.persist(answer);
    }
}
