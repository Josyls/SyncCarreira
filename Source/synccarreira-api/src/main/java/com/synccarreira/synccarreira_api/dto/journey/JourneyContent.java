package com.synccarreira.synccarreira_api.dto.journey;

import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;

import java.time.Instant;
import java.util.List;

// Conteúdo de uma jornada concluída, guardado em JSON no JourneyRecord (RF-15)
public record JourneyContent(
        List<TrailContent> trails,
        List<LinkContent> links
) {

    public record TrailContent(
            TrailName trailName,
            Integer sequentialOrder,
            List<AnswerContent> answers,
            String synthesis,
            Instant synthesisCreatedAt
    ) {
    }

    // Pergunta e a resposta escolhida pelo aluno (null quando a pergunta ficou sem resposta)
    public record AnswerContent(
            String question,
            QuestionType questionType,
            String answer
    ) {
    }

    // Link da trilha de Informação. knowledgeArea nulo = link geral (Enem, ProUni, SiSU...)
    public record LinkContent(
            String topic,
            String url,
            KnowledgeArea knowledgeArea
    ) {
    }
}
