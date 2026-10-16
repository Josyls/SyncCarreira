package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.SynthesisDTO;
import com.synccarreira.synccarreira_api.dto.SynthesisInsertDTO;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Synthesis;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ConflictException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SynthesisService {

    private static final String ALREADY_SUBMITTED = "A síntese desta trilha já foi enviada.";

    private final SynthesisRepository synthesisRepository;

    private final StudentRepository studentRepository;

    private final TrailRepository trailRepository;

    private final TrailService trailService;

    private final AuthService authService;

    private final JourneyHistoryService journeyHistoryService;

    public SynthesisService(
            final SynthesisRepository synthesisRepository,
            final StudentRepository studentRepository,
            final TrailRepository trailRepository,
            final TrailService trailService,
            final AuthService authService,
            final JourneyHistoryService journeyHistoryService) {
        this.synthesisRepository = synthesisRepository;
        this.studentRepository = studentRepository;
        this.trailRepository = trailRepository;
        this.trailService = trailService;
        this.authService = authService;
        this.journeyHistoryService = journeyHistoryService;
    }

    @Transactional(readOnly = true)
    public List<SynthesisDTO> findForLoggedStudent() {
        Student student = loggedStudent();
        return synthesisRepository.findByStudentId(student.getId())
                .stream()
                .map(SynthesisDTO::new)
                .toList();
    }

    @Transactional
    public SynthesisDTO insert(SynthesisInsertDTO dto) {
        Student student = loggedStudent();
        Trail trail = trailRepository.findById(dto.trailId())
                .orElseThrow(() -> new ResourceNotFoundException("Trilha não encontrada. ID: " + dto.trailId()));

        if (!trailService.canAccess(trail.getId())) {
            throw new ForbiddenException("Trilha não liberada. Conclua a trilha anterior para enviar esta síntese.");
        }
        // Na trilha de informação não há perguntas, então a síntese final é liberada assim que a trilha é acessada
        if (!trailService.areAllQuestionsAnswered(trail, student.getId())) {
            throw new BusinessException("Responda todas as perguntas da trilha antes de enviar a síntese.");
        }
        if (synthesisRepository.existsByStudentIdAndTrailId(student.getId(), trail.getId())) {
            throw new ConflictException(ALREADY_SUBMITTED);
        }

        Synthesis entity = new Synthesis();
        entity.setContent(dto.content().strip());
        entity.setStudent(student);
        entity.setTrail(trail);
        entity.setCreatedAt(Instant.now());
        try {
            entity = synthesisRepository.saveAndFlush(entity);
        }
        catch (DataIntegrityViolationException e) {
            // Duas requisições simultâneas para a mesma trilha: a restrição única do banco barra a segunda
            throw new ConflictException(ALREADY_SUBMITTED);
        }
        // RF-15: ao enviar a última síntese, a jornada é concluída e guardada no histórico do aluno
        journeyHistoryService.registerJourneyIfConcluded(student);
        return new SynthesisDTO(entity);
    }

    private Student loggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }
}
