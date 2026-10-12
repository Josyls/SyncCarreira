package com.synccarreira.synccarreira_api.repositories;

import com.synccarreira.synccarreira_api.entities.KnowledgeSubarea;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

// Usa as subáreas e carreiras cadastradas no import.sql (baseadas na Cine Brasil)
@DataJpaTest
@ActiveProfiles("test")
public class KnowledgeSubareaRepositoryTests {

    @Autowired
    private KnowledgeSubareaRepository repository;

    @Test
    void importShouldLoadFiveSubareasWithCareersForEachArea() {
        for (KnowledgeArea area : KnowledgeArea.values()) {
            List<KnowledgeSubarea> result = repository.findByAreaInOrderByAreaAscDisplayOrderAsc(List.of(area));

            Assertions.assertEquals(5, result.size(), "Subáreas de " + area);
            Assertions.assertTrue(result.stream().allMatch(subarea -> subarea.getCareers().size() >= 4), "Carreiras de " + area);
            Assertions.assertTrue(result.stream().allMatch(subarea -> subarea.getDescription() != null && !subarea.getDescription().isBlank()));
            // Cada área tem uma subárea de Educação com as licenciaturas correspondentes
            Assertions.assertTrue(result.stream().anyMatch(subarea -> subarea.getName().startsWith("Educação")), "Educação em " + area);
        }
    }

    @Test
    void findByAreaInShouldReturnSubareasOrderedByDisplayOrderWithCareersInOrder() {
        List<KnowledgeSubarea> result = repository.findByAreaInOrderByAreaAscDisplayOrderAsc(List.of(KnowledgeArea.EXATAS));

        Assertions.assertEquals("Engenharias e Produção", result.getFirst().getName());
        Assertions.assertEquals("Engenharia mecânica", result.getFirst().getCareers().getFirst());
        // Arquitetura e urbanismo segue a Cine Brasil: área geral 07 (Engenharia, produção e construção)
        Assertions.assertTrue(result.stream().anyMatch(subarea -> subarea.getCareers().contains("Arquitetura e urbanismo")));
    }

    @Test
    void findByAreaInShouldReturnSubareasOfAllGivenAreas() {
        List<KnowledgeSubarea> result = repository.findByAreaInOrderByAreaAscDisplayOrderAsc(
                List.of(KnowledgeArea.HUMANAS, KnowledgeArea.ARTES));

        Assertions.assertEquals(10, result.size());
        Assertions.assertTrue(result.stream().allMatch(subarea ->
                subarea.getArea() == KnowledgeArea.HUMANAS || subarea.getArea() == KnowledgeArea.ARTES));
    }
}
