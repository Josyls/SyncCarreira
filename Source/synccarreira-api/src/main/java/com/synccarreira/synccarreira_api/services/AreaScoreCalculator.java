package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.QuestionOption;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.QuestionType;
import com.synccarreira.synccarreira_api.repositories.QuestionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Compara as áreas do conhecimento pelo percentual do máximo possível de cada uma (RF-12 / RN-05).
// As perguntas não distribuem a mesma quantidade de pontos para todas as áreas, então o score bruto
// favoreceria a área com mais pontos em jogo. Usado na conclusão da jornada, no resultado, no PDF e na trilha de Informação.
@Component
public class AreaScoreCalculator {

    private final QuestionRepository questionRepository;

    public AreaScoreCalculator(final QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    // Percentuais com o máximo possível das perguntas cadastradas agora (jornada em andamento, ex.: trilha de Informação)
    @Transactional(readOnly = true)
    public List<AreaScoreDTO> calculate(Double humanities, Double biologicalSciences, Double exactSciences, Double arts) {
        return calculate(areaMap(humanities, biologicalSciences, exactSciences, arts), currentMaxScores());
    }

    // Scores das 4 áreas em percentual, do maior para o menor, marcando a(s) área(s) em destaque.
    // Recebe o máximo de cada área, para que uma jornada concluída use o máximo guardado na conclusão.
    // Em caso de empate, todas as áreas empatadas ficam em destaque. Sem nenhuma pontuação, não há destaque.
    public static List<AreaScoreDTO> calculate(Map<KnowledgeArea, Double> rawScores, Map<KnowledgeArea, Double> maxScores) {
        Map<KnowledgeArea, Double> scores = new EnumMap<>(KnowledgeArea.class);
        Map<KnowledgeArea, Double> percentages = new EnumMap<>(KnowledgeArea.class);
        for (KnowledgeArea area : KnowledgeArea.values()) {
            double score = valueOrZero(rawScores.get(area));
            scores.put(area, score);
            percentages.put(area, percentage(score, maxScores.get(area)));
        }
        double best = percentages.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        return percentages.entrySet().stream()
                .sorted(Map.Entry.<KnowledgeArea, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new AreaScoreDTO(
                        entry.getKey(),
                        label(entry.getKey()),
                        round(scores.get(entry.getKey())),
                        round(valueOrZero(maxScores.get(entry.getKey()))),
                        entry.getValue(),
                        best > 0 && entry.getValue() == best))
                .toList();
    }

    // Áreas em destaque na ordem do enum (Humanas, Biológicas, Exatas, Artes)
    public static List<KnowledgeArea> highlightedAreas(List<AreaScoreDTO> scores) {
        return scores.stream()
                .filter(AreaScoreDTO::highlighted)
                .map(AreaScoreDTO::area)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    // Máximo possível por área: a afinidade máxima do perfil (RF-14) mais o máximo de cada pergunta.
    // Nas perguntas de MULTIPLA_ESCOLHA o aluno pode marcar todas as opções, então o máximo é a soma dos pesos positivos.
    // Nos demais tipos o aluno fica com uma única opção (o AnswerService substitui a resposta anterior), então é o maior peso.
    // É calculado a partir das perguntas cadastradas agora. Na conclusão da jornada, esse máximo é guardado no
    // JourneyRecord, para que o resultado de uma jornada concluída não mude se as perguntas ou os pesos mudarem depois.
    @Transactional(readOnly = true)
    public Map<KnowledgeArea, Double> currentMaxScores() {
        Map<KnowledgeArea, Double> maxScores = new EnumMap<>(KnowledgeArea.class);
        Arrays.stream(KnowledgeArea.values()).forEach(area -> maxScores.put(area, (double) StudentAffinityDTO.MAX));
        for (Question question : questionRepository.findAll()) {
            for (KnowledgeArea area : KnowledgeArea.values()) {
                maxScores.merge(area, questionMaxScore(question, area), Double::sum);
            }
        }
        return maxScores;
    }

    // Monta o mapa área -> valor a partir dos 4 campos usados nas entidades
    public static Map<KnowledgeArea, Double> areaMap(Double humanities, Double biologicalSciences, Double exactSciences, Double arts) {
        Map<KnowledgeArea, Double> values = new EnumMap<>(KnowledgeArea.class);
        values.put(KnowledgeArea.HUMANAS, humanities);
        values.put(KnowledgeArea.BIOLOGICAS, biologicalSciences);
        values.put(KnowledgeArea.EXATAS, exactSciences);
        values.put(KnowledgeArea.ARTES, arts);
        return values;
    }

    // Um peso negativo não aumenta o máximo possível
    private static double questionMaxScore(Question question, KnowledgeArea area) {
        if (question.getQuestionType() == QuestionType.MULTIPLA_ESCOLHA) {
            return question.getOptions().stream()
                    .mapToDouble(option -> Math.max(0.0, weight(option, area)))
                    .sum();
        }
        double bestOption = question.getOptions().stream()
                .mapToDouble(option -> weight(option, area))
                .max()
                .orElse(0.0);
        return Math.max(0.0, bestOption);
    }

    private static double weight(QuestionOption option, KnowledgeArea area) {
        Double weight = switch (area) {
            case HUMANAS -> option.getHumanitiesWeight();
            case BIOLOGICAS -> option.getBiologicalSciencesWeight();
            case EXATAS -> option.getExactSciencesWeight();
            case ARTES -> option.getArtsWeight();
        };
        return valueOrZero(weight);
    }

    private static double percentage(double score, Double maxScore) {
        if (maxScore == null || maxScore <= 0) {
            return 0.0;
        }
        return round(Math.max(0.0, Math.min(100.0, score * 100.0 / maxScore)));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }

    public static String label(KnowledgeArea area) {
        return switch (area) {
            case HUMANAS -> "Humanas";
            case BIOLOGICAS -> "Biológicas";
            case EXATAS -> "Exatas";
            case ARTES -> "Artes";
        };
    }
}
