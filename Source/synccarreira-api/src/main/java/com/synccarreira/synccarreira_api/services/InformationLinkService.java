package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.InformationLinkDTO;
import com.synccarreira.synccarreira_api.dto.InformationLinkInsertDTO;
import com.synccarreira.synccarreira_api.dto.InformationTrailDTO;
import com.synccarreira.synccarreira_api.dto.StudentScoreDTO;
import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.entities.InformationLink;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.entities.enums.TrailName;
import com.synccarreira.synccarreira_api.repositories.InformationLinkRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InformationLinkService {

    private final InformationLinkRepository informationLinkRepository;

    private final StudentRepository studentRepository;

    private final TrailRepository trailRepository;

    private final TrailService trailService;

    private final AuthService authService;

    private final AreaScoreCalculator areaScoreCalculator;

    public InformationLinkService(
            final InformationLinkRepository informationLinkRepository,
            final StudentRepository studentRepository,
            final TrailRepository trailRepository,
            final TrailService trailService,
            final AuthService authService,
            final AreaScoreCalculator areaScoreCalculator) {
        this.informationLinkRepository = informationLinkRepository;
        this.studentRepository = studentRepository;
        this.trailRepository = trailRepository;
        this.trailService = trailService;
        this.authService = authService;
        this.areaScoreCalculator = areaScoreCalculator;
    }

    @Transactional(readOnly = true)
    public InformationTrailDTO findForLoggedStudent() {
        User user = authService.authenticated();
        Student student = studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
        validateInformationTrailAccess();

        // Mesma lógica do resultado da jornada (RF-12): maior percentual do máximo possível, e não o maior score bruto
        List<AreaScoreDTO> areaScores = areaScoreCalculator.calculate(
                student.getHumanitiesScore(),
                student.getBiologicalSciencesScore(),
                student.getExactSciencesScore(),
                student.getArtsScore());
        List<KnowledgeArea> highlightedAreas = AreaScoreCalculator.highlightedAreas(areaScores);
        // Sem nenhuma pontuação não há área em destaque: mostra os links de todas as áreas, como antes
        List<KnowledgeArea> recommendedAreas = highlightedAreas.isEmpty() ? List.of(KnowledgeArea.values()) : highlightedAreas;
        List<InformationLinkDTO> careerLinks = informationLinkRepository.findByKnowledgeAreaInOrderByTopicAscIdAsc(recommendedAreas)
                .stream()
                .map(InformationLinkDTO::new)
                .toList();
        List<InformationLinkDTO> universityAccessLinks = informationLinkRepository.findByKnowledgeAreaIsNullOrderByTopicAscIdAsc()
                .stream()
                .map(InformationLinkDTO::new)
                .toList();

        return new InformationTrailDTO(recommendedAreas, new StudentScoreDTO(student), areaScores, careerLinks, universityAccessLinks);
    }

    @Transactional(readOnly = true)
    public List<InformationLinkDTO> findAll() {
        return informationLinkRepository.findAllByOrderByTopicAscIdAsc()
                .stream()
                .map(InformationLinkDTO::new)
                .toList();
    }

    @Transactional
    public InformationLinkDTO create(InformationLinkInsertDTO dto) {
        InformationLink entity = new InformationLink();
        entity.setTopic(dto.topic().trim());
        entity.setUrl(dto.url().trim());
        entity.setKnowledgeArea(dto.knowledgeArea());
        return new InformationLinkDTO(informationLinkRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        InformationLink entity = informationLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Link não encontrado. ID: " + id));
        informationLinkRepository.delete(entity);
    }

    private void validateInformationTrailAccess() {
        Trail informationTrail = trailRepository.findByName(TrailName.INFORMACAO)
                .orElseThrow(() -> new ResourceNotFoundException("Trilha de informação não encontrada."));
        if (!trailService.canAccess(informationTrail.getId())) {
            throw new ForbiddenException("Acesso negado: conclua as trilhas anteriores para acessar a trilha de informação.");
        }
    }
}
