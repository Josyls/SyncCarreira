package com.synccarreira.synccarreira_api.repositories;

import com.synccarreira.synccarreira_api.entities.KnowledgeSubarea;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface KnowledgeSubareaRepository extends JpaRepository<KnowledgeSubarea, Long> {

    @EntityGraph(attributePaths = "careers")
    List<KnowledgeSubarea> findByAreaInOrderByAreaAscDisplayOrderAsc(Collection<KnowledgeArea> areas);
}
