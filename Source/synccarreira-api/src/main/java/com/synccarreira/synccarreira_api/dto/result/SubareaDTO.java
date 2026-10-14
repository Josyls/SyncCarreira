package com.synccarreira.synccarreira_api.dto.result;

import com.synccarreira.synccarreira_api.entities.KnowledgeSubarea;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;

import java.util.List;

public record SubareaDTO(
        Long id,
        KnowledgeArea area,
        String name,
        String description,
        List<String> careers
) {

    public SubareaDTO(KnowledgeSubarea subarea) {
        this(
                subarea.getId(),
                subarea.getArea(),
                subarea.getName(),
                subarea.getDescription(),
                List.copyOf(subarea.getCareers())
        );
    }
}
