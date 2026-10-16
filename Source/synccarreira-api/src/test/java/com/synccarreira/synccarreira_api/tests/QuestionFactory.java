package com.synccarreira.synccarreira_api.tests;

import com.synccarreira.synccarreira_api.dto.QuestionDTO;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;

public class QuestionFactory {

    public static Question createQuestion() {
        Question question = new Question(1L, "Qual área você mais gosta?", QuestionType.MULTIPLA_ESCOLHA, TrailFactory.createTrail(), new ArrayList<>());
        question.getOptions().add(QuestionOptionFactory.createQuestionOption(question));
        return question;
    }

    // Pergunta de resposta única: responder de novo substitui a opção escolhida
    public static Question createLikertQuestion() {
        Question question = new Question(4L, "Tenho clareza sobre meu futuro.", QuestionType.LIKERT, TrailFactory.createTrail(), new ArrayList<>());
        question.getOptions().add(new QuestionOption(5L, "Concordo", 1.0, 0.0, 0.0, 0.0, question));
        question.getOptions().add(new QuestionOption(6L, "Discordo", 0.0, 0.0, 0.0, 1.0, question));
        return question;
    }

    public static Question createCheckboxQuestion() {
        return new Question(2L, "Com quais atividades você se identifica?", QuestionType.CHECKBOX, TrailFactory.createTrail(), new ArrayList<>());
    }

    public static Question createOpenQuestion() {
        return new Question(3L, "Descreva a profissão dos seus sonhos.", QuestionType.ABERTA, TrailFactory.createTrail(), new ArrayList<>());
    }

    public static QuestionDTO createQuestionDTO() {
        return new QuestionDTO(null, "Qual área você mais gosta?", QuestionType.MULTIPLA_ESCOLHA, 1L, List.of(QuestionOptionFactory.createQuestionOptionDTO()));
    }

    public static QuestionDTO createOpenQuestionDTO() {
        return new QuestionDTO(null, "Descreva a profissão dos seus sonhos.", QuestionType.ABERTA, 1L, null);
    }

    public static QuestionDTO createEmptyOptionsQuestionDTO() {
        return new QuestionDTO(null, "Qual área você mais gosta?", QuestionType.LIKERT, 1L, new ArrayList<>());
    }

    public static QuestionDTO createNonExistingTrailQuestionDTO() {
        return new QuestionDTO(null, "Qual área você mais gosta?", QuestionType.MULTIPLA_ESCOLHA, 100L, List.of(QuestionOptionFactory.createQuestionOptionDTO()));
    }
}
