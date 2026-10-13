package com.synccarreira.synccarreira_api.entities;

import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
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
}
