package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.AnswerDTO;
import com.synccarreira.synccarreira_api.dto.AnswerInsertDTO;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.QuestionOptionRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnswerService {

    private final AnswerRepository answerRepository;

    private final QuestionOptionRepository questionOptionRepository;

    private final StudentRepository studentRepository;

    private final AuthService authService;

    public AnswerService(final AnswerRepository answerRepository, final QuestionOptionRepository questionOptionRepository, final StudentRepository studentRepository, final AuthService authService) {
        this.answerRepository = answerRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.studentRepository = studentRepository;
        this.authService = authService;
    }

    // O aluno é sempre o usuário do token: ninguém lê ou grava respostas de outro aluno
    @Transactional(readOnly = true)
    public List<AnswerDTO> findForLoggedStudent(Long trailId) {
        Student student = loggedStudent();
        List<Answer> result = answerRepository.findByStudentAndTrail(student.getId(), trailId);
        return result.stream().map(AnswerDTO::new).toList();
    }

    @Transactional
    public AnswerDTO insert(AnswerInsertDTO dto) {
        Answer entity = new Answer();
        copyDtoToEntity(dto, entity);
        answerRepository.deleteByStudentAndQuestion(entity.getStudent().getId(), entity.getQuestionOption().getQuestion().getId());
        entity = answerRepository.save(entity);
        recalculateStudentScore(entity.getStudent());
        return new AnswerDTO(entity);
    }

    private void copyDtoToEntity(AnswerInsertDTO dto, Answer entity) {
        QuestionOption questionOption = questionOptionRepository.findById(dto.getQuestionOptionId()).orElseThrow(() -> new ResourceNotFoundException("Question option not found"));
        Student student = loggedStudent();
        entity.setQuestionOption(questionOption);
        entity.setStudent(student);
    }

    private void recalculateStudentScore(Student student) {
        List<Answer> allAnswers = answerRepository.findByStudentId(student.getId());

        double humanities = 0.0, exactSciences = 0.0, biological = 0.0, arts = 0.0;

        for (Answer answer : allAnswers) {
            QuestionOption option = answer.getQuestionOption();
            if (option != null) {
                humanities    += option.getHumanitiesWeight();
                exactSciences += option.getExactSciencesWeight();
                biological    += option.getBiologicalSciencesWeight();
                arts          += option.getArtsWeight();
            }
        }

        student.setHumanitiesScore(humanities);
        student.setExactSciencesScore(exactSciences);
        student.setBiologicalSciencesScore(biological);
        student.setArtsScore(arts);

        studentRepository.save(student);
    }

    private Student loggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }
}
