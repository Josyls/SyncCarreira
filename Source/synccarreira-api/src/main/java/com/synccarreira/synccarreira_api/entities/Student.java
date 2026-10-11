package com.synccarreira.synccarreira_api.entities;

import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "tb_aluno")
@NoArgsConstructor
@AllArgsConstructor
public class Student extends User {

    @Column(name = "ano_escolaridade")
    @Getter
    @Setter
    private String scholarYear;

    @Column(name = "tipo_escola")
    @Getter
    @Setter
    private String schoolType;

    @Column(name = "raca")
    @Getter
    @Setter
    private String race;

    @ManyToMany(mappedBy = "students")
    @Getter
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "student")
    @Getter
    private List<Answer> answerList = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "fk_id_turma", nullable = true)
    @Getter
    @Setter
    private SchoolClass determinedSchoolClass;

    @Column(name = "score_humanas")
    @Getter @Setter
    private Double humanitiesScore = 0.0;

    @Column(name = "score_exatas")
    @Getter @Setter
    private Double exactSciencesScore = 0.0;

    @Column(name = "score_biologicas")
    @Getter @Setter
    private Double biologicalSciencesScore = 0.0;

    @Column(name = "score_artes")
    @Getter @Setter
    private Double artsScore = 0.0;

    @Column(name = "em_duvida", nullable = false)
    @Getter @Setter
    private Boolean inDoubt = false;

    @Column(name = "data_sinalizacao_duvida")
    @Getter @Setter
    private Instant doubtFlaggedAt;

    // Afinidades declaradas pelo aluno no perfil (RF-14), de 0 a 5. Nulas enquanto o perfil não foi preenchido.
    @Column(name = "afinidade_humanas")
    @Getter @Setter
    private Integer humanitiesAffinity;

    @Column(name = "afinidade_exatas")
    @Getter @Setter
    private Integer exactSciencesAffinity;

    @Column(name = "afinidade_biologicas")
    @Getter @Setter
    private Integer biologicalSciencesAffinity;

    @Column(name = "afinidade_artes")
    @Getter @Setter
    private Integer artsAffinity;

    @Column(name = "data_perfil_concluido")
    @Getter @Setter
    private Instant profileCompletedAt;

    // Jornada em andamento (RF-15). As concluídas ficam guardadas em JourneyRecord.
    @Column(name = "numero_jornada_atual", nullable = false)
    @Getter @Setter
    private Integer currentJourneyNumber = 1;

    @Column(name = "data_inicio_jornada_atual")
    @Getter @Setter
    private Instant currentJourneyStartedAt;

    public boolean hasCompletedProfile() {
        return humanitiesAffinity != null
                && exactSciencesAffinity != null
                && biologicalSciencesAffinity != null
                && artsAffinity != null;
    }

    // O score de cada área é sempre recalculado do zero: afinidade declarada no perfil + pesos das opções respondidas.
    // Assim, alterar o perfil ou trocar uma resposta nunca soma o mesmo valor duas vezes.
    public void recalculateScores(Collection<Answer> answers) {
        double humanities = affinityOrZero(humanitiesAffinity);
        double exactSciences = affinityOrZero(exactSciencesAffinity);
        double biologicalSciences = affinityOrZero(biologicalSciencesAffinity);
        double arts = affinityOrZero(artsAffinity);

        for (Answer answer : answers) {
            QuestionOption option = answer.getQuestionOption();
            if (option != null) {
                humanities += valueOrZero(option.getHumanitiesWeight());
                exactSciences += valueOrZero(option.getExactSciencesWeight());
                biologicalSciences += valueOrZero(option.getBiologicalSciencesWeight());
                arts += valueOrZero(option.getArtsWeight());
            }
        }

        this.humanitiesScore = humanities;
        this.exactSciencesScore = exactSciences;
        this.biologicalSciencesScore = biologicalSciences;
        this.artsScore = arts;
    }

    public List<KnowledgeArea> highestScoreAreas() {
        Map<KnowledgeArea, Double> scores = new EnumMap<>(KnowledgeArea.class);
        scores.put(KnowledgeArea.HUMANAS, valueOrZero(humanitiesScore));
        scores.put(KnowledgeArea.BIOLOGICAS, valueOrZero(biologicalSciencesScore));
        scores.put(KnowledgeArea.EXATAS, valueOrZero(exactSciencesScore));
        scores.put(KnowledgeArea.ARTES, valueOrZero(artsScore));

        double highestScore = Collections.max(scores.values());
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() == highestScore)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static double valueOrZero(Double score) {
        return score != null ? score : 0.0;
    }

    private static double affinityOrZero(Integer affinity) {
        return affinity != null ? affinity : 0.0;
    }
}
