INSERT INTO tb_usuario (nome_usuario, email_usuario, senha_usuario) VALUES ('João Silva', 'joao@gmail.com', '$2a$10$HibjoKCNHoOlUpl1UFYkuee58qncnGO00giJ1zhh3advi3Sct/FD2');
INSERT INTO tb_usuario (nome_usuario, email_usuario, senha_usuario) VALUES ('Fernanda Castro', 'fernanda@gmail.com', '$2a$10$HibjoKCNHoOlUpl1UFYkuee58qncnGO00giJ1zhh3advi3Sct/FD2');

INSERT INTO tb_role (autoridade_role) VALUES ('ROLE_USER');
INSERT INTO tb_role (autoridade_role) VALUES ('ROLE_ADMIN');
INSERT INTO tb_role (autoridade_role) VALUES ('ROLE_PSICOLOGA');

INSERT INTO tb_usuario_role (id_usuario , id_role) VALUES (1, 1);
INSERT INTO tb_usuario_role (id_usuario , id_role) VALUES (2, 2);

INSERT INTO tb_trilha (nome_trilha, ordem_sequencial_trilha) VALUES ('AUTOCONHECIMENTO', 1);
INSERT INTO tb_trilha (nome_trilha, ordem_sequencial_trilha) VALUES ('INFLUENCIAS', 2);
INSERT INTO tb_trilha (nome_trilha, ordem_sequencial_trilha) VALUES ('PLANO_DE_FUTURO', 3);
INSERT INTO tb_trilha (nome_trilha, ordem_sequencial_trilha) VALUES ('INFORMACAO', 4);

INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Quais destes assuntos costumam prender sua atenção naturalmente em conversas, vídeos ou leituras?', 'CHECKBOX', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Como você avalia sua facilidade atual em lidar com os seguintes desafios?', 'LIKERT', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Em qual destes cenários você sente que seu desempenho é melhor?', 'MULTIPLA_ESCOLHA', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Das atividades abaixo, qual você curte e Faz com frequência (gera prazer imediato)?', 'MULTIPLA_ESCOLHA', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Qual atividade você faz, mas não curte (faz por obrigação ou necessidade)? ', 'MULTIPLA_ESCOLHA', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Quais destas atividades você curte, mas Não Faz (por falta de tempo, dinheiro ou ferramenta)?', 'CHECKBOX', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Para você, o que define uma atividade bem-sucedida?', 'MULTIPLA_ESCOLHA', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('O que mais te incomoda em uma atividade ou ambiente?', 'MULTIPLA_ESCOLHA', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Quanto você está disposto a se dedicar agora a aprender algo totalmente novo, mesmo que seja difícil no início?', 'LIKERT', 1);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Se você tivesse que descrever seu "jeitão" profissional hoje, qual frase melhor se encaixa?', 'MULTIPLA_ESCOLHA', 1);

INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Minha família tem uma expectativa clara sobre qual carreira eu devo seguir?', 'LIKERT', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Quando penso em uma carreira, a reação da minha família é um fator que me preocupa?', 'LIKERT', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Como a maioria dos seus amigos próximos está em relação à escolha profissional?', 'MULTIPLA_ESCOLHA', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Quais áreas abaixo são as mais valorizadas pelas pessoas do seu convívio (família, escola, bairro).', 'CHECKBOX', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Já deixei de considerar uma carreira porque achei que ela não teria prestígio suficiente aos olhos de outras pessoas.', 'LIKERT', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Já deixei de considerar alguma carreira por conta de uma reação negativa de alguém próximo.', 'MULTIPLA_ESCOLHA', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Quais das situações abaixo você reconhece na sua vida?', 'CHECKBOX', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Quem mais influencia o que você pensa sobre sua carreira?', 'CHECKBOX', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('O quanto a carreira que você está considerando é uma escolha sua, e não uma resposta ao que os outros esperam de você?', 'LIKERT', 2);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Sobre a influência que mais pesou na sua vida até aqui, como você se sente em relação a ela?', 'MULTIPLA_ESCOLHA', 2);

INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Tenho clareza sobre o tipo de vida que quero ter no futuro (rotina, conforto, liberdade, etc.).', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Consigo imaginar como será meu dia a dia na vida adulta.', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Minha escolha profissional está alinhada com o estilo de vida que desejo.', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Considero fatores como renda, qualidade de vida e tempo livre ao pensar no meu futuro.', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Estou disposto(a) a fazer sacrifícios no presente para alcançar meus objetivos futuros.', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Levo em conta minhas condições atuais (financeiras, familiares, tempo) ao planejar meu futuro.', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Tenho objetivos claros para os próximos anos (ex: estudar, trabalhar, mudar de cidade, viajar).', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Consigo adaptar meus planos quando as circunstâncias mudam.', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Minhas decisões atuais estão contribuindo para o futuro que eu quero construir.', 'LIKERT', 3);
INSERT INTO tb_pergunta (enunciado_pergunta, tipo_pergunta, fk_trilha) VALUES ('Eu reflito sobre o que realmente é importante para mim antes de tomar decisões sobre o futuro.', 'LIKERT', 3);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Tecnologia e funcionamento de sistemas.', 0.0, 0.0, 10.0, 0.0, 1);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Comportamento humano e relações sociais.', 10.0, 0.0, 0.0, 0.0, 1);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Expressão artística e criatividade.', 0.0, 0.0, 0.0, 10.0, 1);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Organização, números e lógica.', 0.0, 0.0, 10.0, 0.0, 1);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Natureza, saúde e bem-estar.', 0.0, 10.0, 0.0, 0.0, 1);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Política, história e atualidades.', 10.0, 0.0, 0.0, 0.0, 1);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Resolver problemas lógicos ou matemáticos.', 0.0, 0.0, 10.0, 0.0, 2);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Escrever textos ou expressar ideias com clareza.', 5.0, 0.0, 0.0, 5.0, 2);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Trabalhar em equipe e mediar conflitos.', 10.0, 0.0, 0.0, 0.0, 2);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Aprender a usar novas ferramentas digitais.', 0.0, 0.0, 10.0, 0.0, 2);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Organizar tarefas e cumprir prazos.', 4.0, 0.0, 6.0, 0.0, 2);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Ambientes dinâmicos, com muita interação social e trocas constantes.', 7.0, 3.0, 0.0, 0.0, 3);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Ambientes tranquilos, onde posso ter foco individual e autonomia.', 0.0, 0.0, 7.0, 3.0, 3);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Criar coisas novas (desenhos, códigos, textos, receitas).', 0.0, 0.0, 4.0, 6.0, 4);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Ajudar pessoas a resolverem problemas pessoais.', 8.0, 2.0, 0.0, 0.0, 4);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Analisar dados, fatos ou notícias.', 4.0, 0.0, 6.0, 0.0, 4);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Praticar esportes ou atividades físicas.', 0.0, 8.0, 0.0, 2.0, 4);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Seguir rotinas muito rígidas e repetitivas.', 5.0, 0.0, 0.0, 5.0, 5);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Falar em público ou liderar reuniões.', 0.0, 3.0, 7.0, 0.0, 5);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Estudar temas teóricos muito densos.', 0.0, 4.0, 6.0, 0.0, 5);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Lidar com tarefas burocráticas e manuais.', 3.0, 0.0, 2.0, 5.0, 5);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Viajar e conhecer novas culturas.', 6.0, 0.0, 0.0, 4.0, 6);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Aprender um novo idioma ou habilidade técnica.', 3.0, 0.0, 7.0, 0.0, 6);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Praticar um hobby artístico ou manual.', 0.0, 2.0, 0.0, 8.0, 6);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Participar de projetos voluntários ou sociais.', 8.0, 2.0, 0.0, 0.0, 6);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('O reconhecimento financeiro e status social.', 3.0, 0.0, 7.0, 0.0, 7);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('O impacto positivo que causei na vida de alguém.', 6.0, 4.0, 0.0, 0.0, 7);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('A superação de um desafio técnico difícil.', 0.0, 0.0, 10.0, 0.0, 7);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('O equilíbrio entre o trabalho e meu tempo livre.', 2.0, 2.0, 2.0, 4.0, 7);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Sentir que estou fazendo algo que vai contra meus princípios éticos.', 6.0, 4.0, 0.0, 0.0, 8);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Sentir que minhas habilidades não estão sendo aproveitadas.', 0.0, 0.0, 5.0, 5.0, 8);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('(1) Prefiro focar no que já sei fazer.', 2.0, 3.0, 5.0, 0.0, 9);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('(5) Estou totalmente aberto a desafios fora da minha zona de conforto.', 3.0, 2.0, 3.0, 2.0, 9);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('"O realizador": Gosto de ver resultados concretos e mãos na massa.', 0.0, 5.0, 5.0, 0.0, 10);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('"O pensador": Gosto de entender os porquês e criar estratégias.', 4.0, 0.0, 6.0, 0.0, 10);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('"O conector": Gosto de mediar pessoas e facilitar comunicações.', 8.0, 0.0, 0.0, 2.0, 10);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 11);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 11);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 11);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.0, 4.0, 4.0, 0.0, 11);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 1.0, 4.5, 4.5, 0.0, 11);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.0, 1.0, 2.0, 5.0, 12);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.0, 2.0, 2.0, 4.0, 12);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 12);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.0, 4.0, 4.0, 0.0, 12);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 1.0, 4.5, 4.5, 0.0, 12);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('A maioria já decidiu', 2.0, 3.0, 4.0, 1.0, 13);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Estamos todos em dúvida', 3.0, 2.0, 2.0, 3.0, 13);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('A maioria não pensa muito nisso', 3.0, 2.0, 2.0, 3.0, 13);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Não sei dizer', 2.5, 2.5, 2.5, 2.5, 13);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Saúde', 0.0, 10.0, 0.0, 0.0, 14);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Tecnologia', 0.0, 0.0, 10.0, 0.0, 14);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Direito', 7.0, 0.0, 3.0, 0.0, 14);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Educação', 10.0, 0.0, 0.0, 0.0, 14);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Arte e Design', 0.0, 0.0, 0.0, 10.0, 14);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Negócios', 3.0, 0.0, 7.0, 0.0, 14);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Ofícios técnicos', 2.0, 0.0, 8.0, 0.0, 14);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 3.0, 1.0, 1.0, 5.0, 15);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 3.0, 2.0, 2.0, 3.0, 15);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 15);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.0, 4.0, 4.0, 0.0, 15);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 1.0, 4.5, 4.5, 0.0, 15);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Sim, e ainda me afeta', 2.0, 4.0, 4.0, 0.0, 16);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Sim, mas já superei', 3.0, 2.0, 2.0, 3.0, 16);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Não me lembro de algo assim', 2.5, 2.5, 2.5, 2.5, 16);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Nunca aconteceu', 2.0, 1.0, 2.0, 5.0, 16);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Preciso trabalhar enquanto estudo', 3.0, 1.0, 5.0, 1.0, 17);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Tenho dificuldade de acesso financeiro a cursos', 4.0, 0.0, 4.0, 2.0, 17);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Moro longe das principais instituições', 3.0, 2.0, 3.0, 2.0, 17);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Preciso ajudar minha família financeiramente', 2.0, 0.0, 7.0, 1.0, 17);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Não me vejo representado nas carreiras que conheço', 3.0, 2.0, 2.0, 3.0, 17);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Nenhuma das anteriores', 2.5, 2.5, 2.5, 2.5, 17);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Família', 1.0, 4.5, 4.5, 0.0, 18);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Amigos', 3.0, 2.0, 2.0, 3.0, 18);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Escola e professores', 4.0, 3.0, 3.0, 0.0, 18);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Mídia e redes sociais', 1.0, 2.0, 5.0, 2.0, 18);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Situação financeira', 1.0, 1.0, 7.0, 1.0, 18);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Religião e cultura', 5.0, 3.0, 1.0, 1.0, 18);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Eu mesmo(a)', 2.0, 2.0, 2.0, 4.0, 18);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('É totalmente o que os outros esperam de mim', 1.0, 4.5, 4.5, 0.0, 19);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('É um pouco o que esperam de mim.', 2.0, 3.5, 3.5, 1.0, 19);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro.', 2.5, 2.5, 2.5, 2.5, 19);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('É um pouco minha escolha.', 3.0, 2.0, 2.0, 3.0, 19);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('É completamente minha escolha.', 2.0, 1.0, 1.0, 6.0, 19);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Fico bem com ela, quero que continue assim', 2.0, 4.0, 4.0, 0.0, 20);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Quero repensar o quanto ela me afeta', 3.0, 1.5, 1.5, 4.0, 20);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Não tinha me dado conta dessa influência antes', 2.5, 2.5, 2.5, 2.5, 20);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Prefiro não responder agora', 2.5, 2.5, 2.5, 2.5, 20);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 21);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 21);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 21);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.5, 2.0, 2.5, 3.0, 21);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 2.5, 1.5, 2.5, 3.5, 21);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 22);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 22);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 22);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.5, 2.5, 2.5, 2.5, 22);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 2.5, 2.5, 2.5, 2.5, 22);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 23);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 23);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 23);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.0, 2.0, 3.0, 3.0, 23);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 1.5, 1.5, 3.5, 3.5, 23);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 24);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 24);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 24);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.0, 2.0, 4.0, 2.0, 24);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 1.5, 1.5, 5.0, 2.0, 24);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 25);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 25);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 25);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.0, 4.0, 4.0, 0.0, 25);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 1.0, 4.5, 4.5, 0.0, 25);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 26);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 26);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 26);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 3.0, 1.0, 4.0, 2.0, 26);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 3.0, 0.0, 5.0, 2.0, 26);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 27);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 27);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 27);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.5, 2.5, 3.0, 2.0, 27);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 2.5, 2.5, 4.0, 1.0, 27);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 28);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 28);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 28);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 3.0, 2.0, 1.0, 4.0, 28);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 3.5, 2.0, 0.0, 4.5, 28);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 29);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 29);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 29);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 2.5, 2.5, 2.5, 2.5, 29);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 2.5, 2.5, 2.5, 2.5, 29);

INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo totalmente', 2.5, 2.5, 2.5, 2.5, 30);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Discordo', 2.5, 2.5, 2.5, 2.5, 30);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Neutro', 2.5, 2.5, 2.5, 2.5, 30);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo', 4.0, 1.0, 1.0, 4.0, 30);
INSERT INTO tb_opcao_pergunta (texto_opcao, peso_humanas, peso_biologicas, peso_exatas, peso_arte, fk_pergunta) VALUES ('Concordo totalmente', 4.5, 0.5, 0.5, 4.5, 30);

INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Direito', 'https://www.aurum.com.br/blog/direito-digital/', 'HUMANAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Direito', 'https://www.guiadacarreira.com.br/carreira/direito-penal/', 'HUMANAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Psicologia', 'https://www.guiadacarreira.com.br/carreira/psicologia-clinica/', 'HUMANAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Psicologia', 'https://www.gupy.io/blog/psicologia-organizacional', 'HUMANAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Jornalismo', 'https://www.guiadacarreira.com.br/carreira/jornalismo/', 'HUMANAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Jornalismo', 'https://abraji.org.br/', 'HUMANAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Jornalismo', 'https://portal.comunique-se.com.br/', 'HUMANAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Jornalismo', 'https://ajor.org.br/', 'HUMANAS');

INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Administração', 'https://www.gupy.io/blog/recursos-humanos', 'EXATAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Logística', 'https://brasilescola.uol.com.br/geografia/transportes.htm', 'EXATAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Logística', 'https://www.fazcomex.com.br/blog/logistica-internacional-o-que-e/', 'EXATAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Logística', 'https://blog.unopar.com.br/areas-da-logistica/', 'EXATAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Economia', 'https://www.ipea.gov.br/', 'EXATAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Arquitetura e Urbanismo', 'https://www.archdaily.com.br/br/tag/restauracao', 'EXATAS');

INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Educação Física', 'https://www.confef.org.br/', 'BIOLOGICAS');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Agronomia', 'https://blog.unopar.com.br/areas-da-agronomia/', 'BIOLOGICAS');

INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Publicidade e Propaganda', 'https://rockcontent.com/br/blog/copywriting/', 'ARTES');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Marketing', 'https://rockcontent.com/br/blog/branding/', 'ARTES');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Marketing', 'https://blog.unopar.com.br/areas-do-marketing/', 'ARTES');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Moda', 'https://www.abit.org.br/', 'ARTES');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Gastronomia', 'https://www.abrasel.com.br/', 'ARTES');
INSERT INTO tb_link_informacao (topico_link, url_link, area_conhecimento) VALUES ('Gastronomia', 'https://blog.unopar.com.br/areas-da-gastronomia/', 'ARTES');

INSERT INTO tb_link_informacao (topico_link, url_link) VALUES ('ENEM', 'https://www.gov.br/inep/pt-br/areas-de-atuacao/avaliacao-e-exames-educacionais/enem');
INSERT INTO tb_link_informacao (topico_link, url_link) VALUES ('SISU', 'https://acessounico.mec.gov.br/sisu');
INSERT INTO tb_link_informacao (topico_link, url_link) VALUES ('ProUni', 'https://acessounico.mec.gov.br/prouni');
INSERT INTO tb_link_informacao (topico_link, url_link) VALUES ('FIES', 'https://acessounico.mec.gov.br/fies');

INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Engenharias e Produção', 'Projeta, constrói e melhora máquinas, sistemas, processos industriais e produtos, aplicando matemática e física para resolver problemas práticos.', 1);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Arquitetura e Construção', 'Planeja e constrói edificações, cidades e infraestrutura, como casas, prédios, estradas e redes de transporte, unindo técnica, segurança e qualidade de vida.', 2);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Computação e Tecnologia da Informação', 'Cria e mantém softwares, sistemas, jogos, redes e soluções com dados e inteligência artificial que fazem a tecnologia funcionar no dia a dia.', 3);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Ciências Exatas e da Terra', 'Estuda números, padrões, a matéria, a energia e o planeta, por meio de cálculos, experimentos e análise de dados.', 4);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('EXATAS', 'Educação em Exatas (licenciaturas)', 'Forma professores para ensinar matemática, física, química, estatística e computação na educação básica.', 5);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Medicina e Cuidado em Saúde', 'Cuida da saúde das pessoas na prevenção, no diagnóstico e no tratamento de doenças, em hospitais, clínicas, postos de saúde e farmácias.', 1);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Reabilitação, Diagnóstico e Bem-estar', 'Atua na recuperação de movimentos e funções, na alimentação saudável, na atividade física e nos exames que ajudam a identificar doenças.', 2);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Ciências Biológicas e Meio Ambiente', 'Estuda os seres vivos, da molécula ao ecossistema, e busca formas de conservar a natureza e de usar a biotecnologia a favor das pessoas.', 3);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Ciências Agrárias e Veterinária', 'Trabalha com a produção de alimentos, o cuidado com os animais, as florestas e o uso sustentável do solo e da água.', 4);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('BIOLOGICAS', 'Educação em Biológicas (licenciaturas)', 'Forma professores de biologia, ciências, educação física e áreas ligadas à saúde e ao campo para a educação básica e profissional.', 5);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Ciências Sociais e Comportamentais', 'Busca compreender como as pessoas pensam, se relacionam e se organizam em grupos, na economia, na política e no espaço geográfico.', 1);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Direito, Gestão e Negócios', 'Atua na organização de empresas, instituições públicas e da sociedade, nas leis e na garantia de direitos, nas finanças e no comércio.', 2);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Comunicação e Informação', 'Produz, organiza e divulga informações e notícias em jornais, rádio, TV e internet, e cuida de acervos em bibliotecas, arquivos e museus.', 3);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Humanidades e Letras', 'Estuda a história, o pensamento, as religiões, as línguas e a literatura para entender as culturas e a experiência humana.', 4);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('HUMANAS', 'Educação em Humanas (licenciaturas)', 'Forma professores para a educação infantil, os anos iniciais e as disciplinas de humanas e de línguas na educação básica.', 5);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Artes Visuais', 'Cria e analisa obras como pinturas, esculturas, gravuras e instalações, e estuda a história e a conservação da arte.', 1);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Música e Artes Cênicas', 'Envolve compor, tocar, cantar, atuar e dançar, levando a arte ao palco, ao estúdio e a diferentes públicos.', 2);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Design e Moda', 'Une criatividade e técnica para projetar produtos, peças gráficas, ambientes e roupas que sejam bonitos, úteis e funcionais.', 3);
INSERT INTO tb_subarea_conhecimento (area_conhecimento, nome_subarea, descricao_subarea, ordem_exibicao) VALUES ('ARTES', 'Audiovisual, Mídia e Produção Cultural', 'Produz filmes, animações, fotografias, músicas gravadas, conteúdos multimídia e eventos culturais.', 4);
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
