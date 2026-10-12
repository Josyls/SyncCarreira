package com.synccarreira.synccarreira_api.entities;

import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Registro congelado de uma jornada concluída pelo aluno (RF-15).
// Guarda uma cópia dos dados da jornada, porque respostas e sínteses são apagadas quando o aluno inicia uma nova jornada.
@Entity
@Table(name = "tb_jornada_concluida",
        uniqueConstraints = @UniqueConstraint(name = "uk_jornada_aluno_numero", columnNames = {"fk_aluno", "numero_jornada"}))
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JourneyRecord {

    private static final String AREA_SEPARATOR = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_jornada")
    @EqualsAndHashCode.Include
    @Getter @Setter
    private Long id;

    // Ao excluir o aluno, o banco exclui o histórico dele junto (LGPD)
    @ManyToOne
    @JoinColumn(name = "fk_aluno", nullable = false, foreignKey = @ForeignKey(name = "fk_jornada_aluno"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Getter @Setter
    private Student student;

    @Column(name = "numero_jornada", nullable = false)
    @Getter @Setter
    private Integer journeyNumber;

    @Column(name = "data_inicio")
    @Getter @Setter
    private Instant startedAt;

    @Column(name = "data_conclusao", nullable = false)
    @Getter @Setter
    private Instant concludedAt;

    @Column(name = "nome_turma")
    @Getter @Setter
    private String schoolClassName;

    @Column(name = "ano_letivo")
    @Getter @Setter
    private Integer schoolYear;

    @Column(name = "nome_instituicao")
    @Getter @Setter
    private String institutionName;

    @Column(name = "score_humanas", nullable = false)
    @Getter @Setter
    private Double humanitiesScore;

    @Column(name = "score_exatas", nullable = false)
    @Getter @Setter
    private Double exactSciencesScore;

    @Column(name = "score_biologicas", nullable = false)
    @Getter @Setter
    private Double biologicalSciencesScore;

    @Column(name = "score_artes", nullable = false)
    @Getter @Setter
    private Double artsScore;

    // Máximo possível de cada área no momento da conclusão (RF-12): afinidade máxima + maior peso de cada pergunta.
    // Guardado para que o percentual da jornada concluída não mude se as perguntas ou os pesos forem alterados depois.
    // Nulo apenas em jornadas concluídas antes do RF-12.
    @Column(name = "score_maximo_humanas")
    @Getter @Setter
    private Double humanitiesMaxScore;

    @Column(name = "score_maximo_exatas")
    @Getter @Setter
    private Double exactSciencesMaxScore;

    @Column(name = "score_maximo_biologicas")
    @Getter @Setter
    private Double biologicalSciencesMaxScore;

    @Column(name = "score_maximo_artes")
    @Getter @Setter
    private Double artsMaxScore;

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

    // Áreas de maior afinidade no momento da conclusão, separadas por vírgula (ex.: "EXATAS,ARTES")
    @Column(name = "areas_destaque")
    @Getter @Setter
    private String highestScoreAreas;

    @Column(name = "em_duvida", nullable = false)
    @Getter @Setter
    private Boolean inDoubt = false;

    @Column(name = "data_sinalizacao_duvida")
    @Getter @Setter
    private Instant doubtFlaggedAt;

    // Perguntas, respostas, sínteses e links da jornada, em JSON (ver JourneyContent)
    @Column(name = "conteudo", nullable = false, columnDefinition = "TEXT")
    @Getter @Setter
    private String content;

    public boolean hasMaxScores() {
        return humanitiesMaxScore != null && exactSciencesMaxScore != null
                && biologicalSciencesMaxScore != null && artsMaxScore != null;
    }

    public void setMaxScores(Map<KnowledgeArea, Double> maxScores) {
        this.humanitiesMaxScore = maxScores.get(KnowledgeArea.HUMANAS);
        this.biologicalSciencesMaxScore = maxScores.get(KnowledgeArea.BIOLOGICAS);
        this.exactSciencesMaxScore = maxScores.get(KnowledgeArea.EXATAS);
        this.artsMaxScore = maxScores.get(KnowledgeArea.ARTES);
    }

    public List<KnowledgeArea> highestScoreAreaList() {
        if (highestScoreAreas == null || highestScoreAreas.isBlank()) {
            return List.of();
        }
        return Arrays.stream(highestScoreAreas.split(AREA_SEPARATOR))
                .map(String::trim)
                .map(KnowledgeArea::valueOf)
                .toList();
    }

    public void setHighestScoreAreaList(List<KnowledgeArea> areas) {
        this.highestScoreAreas = areas.stream()
                .map(KnowledgeArea::name)
                .collect(Collectors.joining(AREA_SEPARATOR));
    }
}
