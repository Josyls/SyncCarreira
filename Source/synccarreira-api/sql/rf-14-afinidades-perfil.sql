-- RF-14 - Personalização de perfil: afinidades declaradas pelo aluno (escala de 0 a 5)
-- Necessário nos bancos MySQL (dev/prod), que usam spring.jpa.hibernate.ddl-auto=none.
-- No perfil "test" (H2) o Hibernate cria as colunas automaticamente.

ALTER TABLE tb_aluno
    ADD COLUMN afinidade_humanas     INT          NULL,
    ADD COLUMN afinidade_exatas      INT          NULL,
    ADD COLUMN afinidade_biologicas  INT          NULL,
    ADD COLUMN afinidade_artes       INT          NULL,
    ADD COLUMN data_perfil_concluido DATETIME(6)  NULL;
