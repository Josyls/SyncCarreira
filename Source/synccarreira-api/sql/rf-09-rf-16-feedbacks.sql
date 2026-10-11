-- RF-09 / RF-16 - Feedbacks individuais da psicóloga para o aluno
-- Necessário nos bancos MySQL (dev/prod), que usam spring.jpa.hibernate.ddl-auto=none.
-- No perfil "test" (H2) o Hibernate cria a tabela automaticamente.
-- Pré-requisito: script rf-15-historico-jornadas.sql já aplicado.

CREATE TABLE tb_feedback (
    id_feedback     BIGINT       NOT NULL AUTO_INCREMENT,
    tipo_feedback   VARCHAR(20)  NOT NULL,
    fk_aluno        BIGINT       NOT NULL,
    fk_psicologa    BIGINT       NOT NULL,
    numero_jornada  INT          NULL,
    fk_agendamento  BIGINT       NULL,
    conteudo        TEXT         NOT NULL,
    data_envio      DATETIME(6)  NOT NULL,
    data_leitura    DATETIME(6)  NULL,
    PRIMARY KEY (id_feedback),
    -- No máximo 1 feedback por jornada do aluno e 1 por aluno em cada sessão (NULLs não conflitam)
    CONSTRAINT uk_feedback_aluno_jornada UNIQUE (fk_aluno, numero_jornada),
    CONSTRAINT uk_feedback_aluno_agendamento UNIQUE (fk_aluno, fk_agendamento),
    CONSTRAINT fk_feedback_aluno FOREIGN KEY (fk_aluno) REFERENCES tb_aluno (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_psicologa FOREIGN KEY (fk_psicologa) REFERENCES tb_psicologa (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_agendamento FOREIGN KEY (fk_agendamento) REFERENCES tb_agendamento (id_agendamento) ON DELETE CASCADE
);
