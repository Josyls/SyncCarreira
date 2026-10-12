-- RF-12 - Leque de carreiras sugeridas: subáreas das áreas do conhecimento com carreiras e cursos possíveis
-- Conteúdo baseado nas áreas e rótulos da Cine Brasil (Inep), classificação oficial dos cursos de graduação
-- (Portaria MEC nº 1.715/2019). As descrições devem ser revisadas pela psicóloga (RN-06).
-- Necessário nos bancos MySQL (dev/prod), que usam spring.jpa.hibernate.ddl-auto=none.
-- No perfil "test" (H2) o Hibernate cria as tabelas e o import.sql carrega o mesmo conteúdo.
-- O comentário antes de cada subárea indica a área da Cine Brasil usada como base (apenas documentação, não vai para o banco).
-- Pré-requisito: script rf-15-historico-jornadas.sql já aplicado (tabela tb_jornada_concluida).

-- Máximo possível de cada área guardado na conclusão da jornada, para o percentual do resultado não mudar
-- se as perguntas ou os pesos forem alterados depois. Fica nulo nas jornadas concluídas antes deste script:
-- nelas o backend usa o máximo das perguntas atuais.
ALTER TABLE tb_jornada_concluida
    ADD COLUMN score_maximo_humanas    DOUBLE NULL,
    ADD COLUMN score_maximo_exatas     DOUBLE NULL,
    ADD COLUMN score_maximo_biologicas DOUBLE NULL,
    ADD COLUMN score_maximo_artes      DOUBLE NULL;

CREATE TABLE tb_subarea_conhecimento (
    id_subarea         BIGINT        NOT NULL AUTO_INCREMENT,
    area_conhecimento  VARCHAR(20)   NOT NULL,
    nome_subarea       VARCHAR(255)  NOT NULL,
    descricao_subarea  VARCHAR(500)  NOT NULL,
    ordem_exibicao     INT           NOT NULL,
    PRIMARY KEY (id_subarea)
);

CREATE TABLE tb_subarea_carreira (
    fk_subarea      BIGINT        NOT NULL,
    ordem_carreira  INT           NOT NULL,
    nome_carreira   VARCHAR(255)  NOT NULL,
    PRIMARY KEY (fk_subarea, ordem_carreira),
    CONSTRAINT fk_carreira_subarea FOREIGN KEY (fk_subarea) REFERENCES tb_subarea_conhecimento (id_subarea) ON DELETE CASCADE
);

-- Cine Brasil 071 e 072 - Engenharia e profissões correlatas; produção e processamento
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Engenharias e Produção', 'Projeta, constrói e melhora máquinas, sistemas, processos industriais e produtos, aplicando matemática e física para resolver problemas práticos.', 1);
-- Cine Brasil 073 - Arquitetura e construção
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Arquitetura e Construção', 'Planeja e constrói edificações, cidades e infraestrutura, como casas, prédios, estradas e redes de transporte, unindo técnica, segurança e qualidade de vida.', 2);
-- Cine Brasil 06 - Computação e Tecnologias da Informação e Comunicação (TIC)
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Computação e Tecnologia da Informação', 'Cria e mantém softwares, sistemas, jogos, redes e soluções com dados e inteligência artificial que fazem a tecnologia funcionar no dia a dia.', 3);
-- Cine Brasil 053 e 054 - Ciências físicas, matemática e estatística
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Ciências Exatas e da Terra', 'Estuda números, padrões, a matéria, a energia e o planeta, por meio de cálculos, experimentos e análise de dados.', 4);
-- Cine Brasil 0114 - Formação de professores em áreas específicas
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Educação em Exatas (licenciaturas)', 'Forma professores para ensinar matemática, física, química, estatística e computação na educação básica.', 5);
-- Cine Brasil 0911, 0912, 0913 e 0916 - Odontologia, medicina, enfermagem e farmácia
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Medicina e Cuidado em Saúde', 'Cuida da saúde das pessoas na prevenção, no diagnóstico e no tratamento de doenças, em hospitais, clínicas, postos de saúde e farmácias.', 1);
-- Cine Brasil 0914 e 0915 - Tecnologia de diagnóstico e tratamento; prevenção, terapia e reabilitação
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Reabilitação, Diagnóstico e Bem-estar', 'Atua na recuperação de movimentos e funções, na alimentação saudável, na atividade física e nos exames que ajudam a identificar doenças.', 2);
-- Cine Brasil 051 e 052 - Ciências biológicas e meio ambiente
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Ciências Biológicas e Meio Ambiente', 'Estuda os seres vivos, da molécula ao ecossistema, e busca formas de conservar a natureza e de usar a biotecnologia a favor das pessoas.', 3);
-- Cine Brasil 08 - Agricultura, silvicultura, pesca e veterinária
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Ciências Agrárias e Veterinária', 'Trabalha com a produção de alimentos, o cuidado com os animais, as florestas e o uso sustentável do solo e da água.', 4);
-- Cine Brasil 0114 - Formação de professores em áreas específicas
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Educação em Biológicas (licenciaturas)', 'Forma professores de biologia, ciências, educação física e áreas ligadas à saúde e ao campo para a educação básica e profissional.', 5);
-- Cine Brasil 031 - Ciências sociais e comportamentais
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Ciências Sociais e Comportamentais', 'Busca compreender como as pessoas pensam, se relacionam e se organizam em grupos, na economia, na política e no espaço geográfico.', 1);
-- Cine Brasil 04 - Negócios, administração e direito
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Direito, Gestão e Negócios', 'Atua na organização de empresas, instituições públicas e da sociedade, nas leis e na garantia de direitos, nas finanças e no comércio.', 2);
-- Cine Brasil 032 - Jornalismo e informação
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Comunicação e Informação', 'Produz, organiza e divulga informações e notícias em jornais, rádio, TV e internet, e cuida de acervos em bibliotecas, arquivos e museus.', 3);
-- Cine Brasil 022 e 023 - Humanidades e línguas
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Humanidades e Letras', 'Estuda a história, o pensamento, as religiões, as línguas e a literatura para entender as culturas e a experiência humana.', 4);
-- Cine Brasil 0113, 0114 e 0115 - Formação de professores
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Educação em Humanas (licenciaturas)', 'Forma professores para a educação infantil, os anos iniciais e as disciplinas de humanas e de línguas na educação básica.', 5);
-- Cine Brasil 0213 - Belas artes
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Artes Visuais', 'Cria e analisa obras como pinturas, esculturas, gravuras e instalações, e estuda a história e a conservação da arte.', 1);
-- Cine Brasil 0215 - Música e artes cênicas
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Música e Artes Cênicas', 'Envolve compor, tocar, cantar, atuar e dançar, levando a arte ao palco, ao estúdio e a diferentes públicos.', 2);
-- Cine Brasil 0212 - Moda, design de interiores e desenho industrial
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Design e Moda', 'Une criatividade e técnica para projetar produtos, peças gráficas, ambientes e roupas que sejam bonitos, úteis e funcionais.', 3);
-- Cine Brasil 0211 - Produção audiovisual, de mídia e cultural
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Audiovisual, Mídia e Produção Cultural', 'Produz filmes, animações, fotografias, músicas gravadas, conteúdos multimídia e eventos culturais.', 4);
-- Cine Brasil 0114 - Formação de professores em áreas específicas
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Educação em Artes (licenciaturas)', 'Forma professores de artes visuais, música, teatro, dança e cinema para a educação básica.', 5);
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (1, 0, 'Engenharia mecânica');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (1, 1, 'Engenharia elétrica');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (1, 2, 'Engenharia química');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (1, 3, 'Engenharia de produção');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (1, 4, 'Engenharia de controle e automação');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (1, 5, 'Engenharia aeroespacial');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (1, 6, 'Engenharia de materiais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (2, 0, 'Arquitetura e urbanismo');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (2, 1, 'Engenharia civil');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (2, 2, 'Engenharia de transportes');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (2, 3, 'Engenharia cartográfica');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (2, 4, 'Construção de edifícios');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (2, 5, 'Agrimensura');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (3, 0, 'Ciência da computação');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (3, 1, 'Engenharia de software');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (3, 2, 'Sistemas de informação');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (3, 3, 'Ciência de dados');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (3, 4, 'Inteligência artificial');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (3, 5, 'Segurança da informação');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (3, 6, 'Jogos digitais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (3, 7, 'Redes de computadores');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (4, 0, 'Matemática');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (4, 1, 'Estatística');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (4, 2, 'Física');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (4, 3, 'Química');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (4, 4, 'Astronomia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (4, 5, 'Ciências atuariais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (4, 6, 'Geologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (4, 7, 'Meteorologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (5, 0, 'Licenciatura em Matemática');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (5, 1, 'Licenciatura em Física');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (5, 2, 'Licenciatura em Química');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (5, 3, 'Licenciatura em Estatística');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (5, 4, 'Licenciatura em Computação');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (6, 0, 'Medicina');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (6, 1, 'Enfermagem');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (6, 2, 'Odontologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (6, 3, 'Farmácia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (7, 0, 'Fisioterapia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (7, 1, 'Nutrição');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (7, 2, 'Fonoaudiologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (7, 3, 'Educação física');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (7, 4, 'Biomedicina');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (7, 5, 'Análises clínicas e toxicológicas');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (7, 6, 'Optometria');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (8, 0, 'Biologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (8, 1, 'Biotecnologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (8, 2, 'Bioquímica');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (8, 3, 'Ciências ambientais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (8, 4, 'Ecologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (8, 5, 'Toxicologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (9, 0, 'Medicina veterinária');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (9, 1, 'Agronomia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (9, 2, 'Engenharia florestal');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (9, 3, 'Engenharia agrícola');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (9, 4, 'Agroecologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (9, 5, 'Aquicultura');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (9, 6, 'Engenharia de pesca');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (10, 0, 'Licenciatura em Biologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (10, 1, 'Licenciatura em Ciências Naturais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (10, 2, 'Licenciatura em Educação Física');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (10, 3, 'Licenciatura em Ciências Agrárias');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (10, 4, 'Licenciatura em Enfermagem');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (11, 0, 'Psicologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (11, 1, 'Economia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (11, 2, 'Ciências sociais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (11, 3, 'Sociologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (11, 4, 'Antropologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (11, 5, 'Ciência política');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (11, 6, 'Relações internacionais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (11, 7, 'Geografia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (12, 0, 'Direito');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (12, 1, 'Administração');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (12, 2, 'Contabilidade');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (12, 3, 'Administração pública');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (12, 4, 'Comércio exterior');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (12, 5, 'Logística');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (12, 6, 'Marketing');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (12, 7, 'Publicidade e propaganda');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (13, 0, 'Jornalismo');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (13, 1, 'Comunicação social');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (13, 2, 'Rádio, TV e internet');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (13, 3, 'Produção editorial');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (13, 4, 'Biblioteconomia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (13, 5, 'Arquivologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (13, 6, 'Museologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (14, 0, 'História');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (14, 1, 'Filosofia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (14, 2, 'Arqueologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (14, 3, 'Teologia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (14, 4, 'Letras português');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (14, 5, 'Letras inglês');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (14, 6, 'Letras espanhol');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (14, 7, 'Letras tradutor e intérprete');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (15, 0, 'Pedagogia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (15, 1, 'Licenciatura em História');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (15, 2, 'Licenciatura em Geografia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (15, 3, 'Licenciatura em Filosofia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (15, 4, 'Licenciatura em Ciências Sociais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (15, 5, 'Licenciatura em Letras');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (15, 6, 'Educação especial');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (16, 0, 'Artes visuais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (16, 1, 'Artes plásticas');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (16, 2, 'Artes');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (16, 3, 'História da arte');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (17, 0, 'Música');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (17, 1, 'Teatro');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (17, 2, 'Dança');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (17, 3, 'Artes cênicas');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (17, 4, 'Produção cênica');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (18, 0, 'Design');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (18, 1, 'Design gráfico');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (18, 2, 'Design de produto');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (18, 3, 'Design de interiores');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (18, 4, 'Desenho industrial');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (18, 5, 'Moda');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (19, 0, 'Cinema e audiovisual');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (19, 1, 'Animação');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (19, 2, 'Fotografia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (19, 3, 'Produção audiovisual');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (19, 4, 'Produção multimídia');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (19, 5, 'Produção fonográfica');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (19, 6, 'Produção cultural');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (20, 0, 'Licenciatura em Artes Visuais');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (20, 1, 'Licenciatura em Música');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (20, 2, 'Licenciatura em Teatro');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (20, 3, 'Licenciatura em Dança');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (20, 4, 'Licenciatura em Cinema e Audiovisual');
INSERT INTO tb_subarea_carreira (fk_subarea, ordem_carreira, nome_carreira) VALUES (20, 5, 'Licenciatura em Artes');
