package com.synccarreira.synccarreira_api.entities;

import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Subárea de uma área do conhecimento, com carreiras e cursos possíveis (RF-12).
// Conteúdo baseado nas áreas e rótulos da Cine Brasil (Inep), carregado pelo import.sql.
@Entity
@Table(name = "tb_subarea_conhecimento")
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class KnowledgeSubarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_subarea")
    @EqualsAndHashCode.Include
    @Getter @Setter
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_conhecimento", nullable = false, length = 20)
    @Getter @Setter
    private KnowledgeArea area;

    @Column(name = "nome_subarea", nullable = false)
    @Getter @Setter
    private String name;

    @Column(name = "descricao_subarea", nullable = false, length = 500)
    @Getter @Setter
    private String description;

    @Column(name = "ordem_exibicao", nullable = false)
    @Getter @Setter
    private Integer displayOrder;

    @ElementCollection
    @CollectionTable(name = "tb_subarea_carreira", joinColumns = @JoinColumn(name = "fk_subarea"))
    @OrderColumn(name = "ordem_carreira")
    @Column(name = "nome_carreira", nullable = false)
    @Getter
    private List<String> careers = new ArrayList<>();
}
