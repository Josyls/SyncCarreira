package com.synccarreira.synccarreira_api.entities;

import com.synccarreira.synccarreira_api.entities.enums.FeedbackType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

// Feedback individual e opcional que a psicóloga envia ao aluno (RF-09), exibido na área do aluno (RF-16).
// JORNADA: ao final de uma jornada concluída (no máximo 1 por jornada do aluno).
// SESSAO: depois de uma sessão realizada da qual o aluno participou (no máximo 1 por aluno em cada sessão).
@Entity
@Table(name = "tb_feedback",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_feedback_aluno_jornada", columnNames = {"fk_aluno", "numero_jornada"}),
                @UniqueConstraint(name = "uk_feedback_aluno_agendamento", columnNames = {"fk_aluno", "fk_agendamento"})
        })
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_feedback")
    @EqualsAndHashCode.Include
    @Getter @Setter
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_feedback", nullable = false, length = 20)
    @Getter @Setter
    private FeedbackType type;

    @ManyToOne
    @JoinColumn(name = "fk_aluno", nullable = false, foreignKey = @ForeignKey(name = "fk_feedback_aluno"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Getter @Setter
    private Student student;

    @ManyToOne
    @JoinColumn(name = "fk_psicologa", nullable = false, foreignKey = @ForeignKey(name = "fk_feedback_psicologa"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Getter @Setter
    private Psychologist psychologist;

    // Preenchido só nos feedbacks de jornada
    @Column(name = "numero_jornada")
    @Getter @Setter
    private Integer journeyNumber;

    // Preenchido só nos feedbacks de sessão
    @ManyToOne
    @JoinColumn(name = "fk_agendamento", foreignKey = @ForeignKey(name = "fk_feedback_agendamento"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Getter @Setter
    private Appointment appointment;

    @Column(name = "conteudo", nullable = false, columnDefinition = "TEXT")
    @Getter @Setter
    private String content;

    @Column(name = "data_envio", nullable = false)
    @Getter @Setter
    private Instant createdAt;

    // Nulo enquanto o aluno não abrir o feedback
    @Column(name = "data_leitura")
    @Getter @Setter
    private Instant readAt;
}
