package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.QuestionOptionDTO;
import com.synccarreira.synccarreira_api.dto.QuestionDTO;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.repositories.QuestionRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionService {

    private static final int LIMIT_QUESTIONS_PER_TRAIL = 10;

    private final QuestionRepository questionRepository;

    private final TrailRepository trailRepository;

    private final TrailService trailService;

    public QuestionService(
            final QuestionRepository questionRepository,
            final TrailRepository trailRepository,
            final TrailService trailService) {
        this.questionRepository = questionRepository;
        this.trailRepository = trailRepository;
        this.trailService = trailService;
    }

    @Transactional(readOnly = true)
    public List<QuestionDTO> findAll() {
        return questionRepository.findAll()
                .stream()
                .map(QuestionDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionDTO findById(Long id) {
        return new QuestionDTO(questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pergunta não encontrada. ID: " + id)));
    }

    @Transactional(readOnly = true)
    public List<QuestionDTO> findByTrail(Long trailId) {
        if (!trailService.canAccess(trailId)) {
            throw new BusinessException("Trilha não liberada. Termine a trilha anterior para avançar.");
        }
        return questionRepository.findByTrailId(trailId)
                .stream()
                .map(QuestionDTO::new)
                .toList();
    }

    @Transactional
    public QuestionDTO createQuestion(QuestionDTO dto) {
        Trail trail = trailRepository.findById(dto.trailId())
                .orElseThrow(() -> new EntityNotFoundException("Trilha não encontrada. ID: " + dto.trailId()));

        long total = questionRepository.countByTrailId(dto.trailId());
        if (total >= LIMIT_QUESTIONS_PER_TRAIL) {
            throw new IllegalStateException(
                    "A trilha '" + trail.getName() + "' já atingiu o limite de " +
                            LIMIT_QUESTIONS_PER_TRAIL + " perguntas.");
        }

        Question question = new Question();
        question.setContent(dto.content());
        question.setQuestionType(dto.questionType());
        question.setTrail(trail);

        if (question.acceptsOptions()) {
            validateOptions(dto.options());
            addOptions(question, dto.options());
        }

        question = questionRepository.save(question);
        return new QuestionDTO(question);
    }

    @Transactional
    public QuestionDTO updateQuestion(Long id, QuestionDTO dto) {
        Question question = questionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Pergunta não encontrada. ID: " + id));

        Trail trail = trailRepository.findById(dto.trailId())
                .orElseThrow(() -> new EntityNotFoundException("Trilha não encontrada. ID: " + dto.trailId()));

        question.setContent(dto.content());
        question.setQuestionType(dto.questionType());
        question.setTrail(trail);

        question.getOptions().clear();
        if (question.acceptsOptions()) {
            validateOptions(dto.options());
            addOptions(question, dto.options());
        }

        question = questionRepository.save(question);
        return new QuestionDTO(question);
    }

    @Transactional
    public void deleteQuestionById(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pergunta não encontrada. ID: " + id);
        }
        questionRepository.deleteById(id);
    }

    private void validateOptions(List<QuestionOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException(
                    "Perguntas de múltipla escolha, checkbox ou Likert devem ter ao menos uma opção.");
        }
    }

    private void addOptions(Question question, List<QuestionOptionDTO> optionDTOs) {
        for (QuestionOptionDTO optionDTO : optionDTOs) {
            QuestionOption opcao = new QuestionOption();
            opcao.setOptionText(optionDTO.optionText());
            opcao.setHumanitiesWeight(optionDTO.humanitiesWeight());
            opcao.setBiologicalSciencesWeight(optionDTO.biologicalSciencesWeight());
            opcao.setExactSciencesWeight(optionDTO.exactSciencesWeight());
            opcao.setArtsWeight(optionDTO.artsWeight());
            opcao.setQuestion(question);
            question.getOptions().add(opcao);
        }
    }
}
