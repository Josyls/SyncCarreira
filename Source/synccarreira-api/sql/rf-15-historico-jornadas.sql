-- RF-15 - Histórico de jornadas anteriores
-- Necessário nos bancos MySQL (dev/prod), que usam spring.jpa.hibernate.ddl-auto=none.
-- No perfil "test" (H2) o Hibernate cria as tabelas e colunas automaticamente.
-- Pré-requisito: script rf-14-afinidades-perfil.sql já aplicado.

-- Jornada em andamento de cada aluno (alunos existentes começam na jornada 1)
ALTER TABLE tb_aluno
    ADD COLUMN numero_jornada_atual      INT         NOT NULL DEFAULT 1,
    ADD COLUMN data_inicio_jornada_atual DATETIME(6) NULL;

-- Cópia congelada de cada jornada concluída
CREATE TABLE tb_jornada_concluida (
    id_jornada               BIGINT       NOT NULL AUTO_INCREMENT,
    fk_aluno                 BIGINT       NOT NULL,
    numero_jornada           INT          NOT NULL,
    data_inicio              DATETIME(6)  NULL,
    data_conclusao           DATETIME(6)  NOT NULL,
    nome_turma               VARCHAR(255) NULL,
    ano_letivo               INT          NULL,
    nome_instituicao         VARCHAR(255) NULL,
    score_humanas            DOUBLE       NOT NULL,
    score_exatas             DOUBLE       NOT NULL,
    score_biologicas         DOUBLE       NOT NULL,
    score_artes              DOUBLE       NOT NULL,
    afinidade_humanas        INT          NULL,
    afinidade_exatas         INT          NULL,
    afinidade_biologicas     INT          NULL,
    afinidade_artes          INT          NULL,
    areas_destaque           VARCHAR(255) NULL,
    em_duvida                BIT          NOT NULL DEFAULT 0,
    data_sinalizacao_duvida  DATETIME(6)  NULL,
    -- MEDIUMTEXT: 30 perguntas + 4 sínteses de até 5000 caracteres podem passar do limite de 64 KB do TEXT
    conteudo                 MEDIUMTEXT   NOT NULL,
    PRIMARY KEY (id_jornada),
    CONSTRAINT uk_jornada_aluno_numero UNIQUE (fk_aluno, numero_jornada),
    CONSTRAINT fk_jornada_aluno FOREIGN KEY (fk_aluno) REFERENCES tb_aluno (id_usuario) ON DELETE CASCADE
);
