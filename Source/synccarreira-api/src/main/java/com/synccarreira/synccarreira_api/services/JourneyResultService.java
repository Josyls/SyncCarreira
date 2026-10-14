package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.dto.result.JourneyResultDTO;
import com.synccarreira.synccarreira_api.dto.result.SubareaDTO;
import com.synccarreira.synccarreira_api.entities.JourneyRecord;
import com.synccarreira.synccarreira_api.entities.KnowledgeSubarea;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.repositories.JourneyRecordRepository;
import com.synccarreira.synccarreira_api.repositories.KnowledgeSubareaRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Resultado da jornada com o leque de carreiras sugeridas (RF-12 / RN-07).
// As áreas são comparadas em percentual do máximo possível de cada uma (ver AreaScoreCalculator).
@Service
public class JourneyResultService {

    public static final String DISCLAIMER = "Este resultado é um ponto de partida para a sua reflexão, e não um diagnóstico de carreira. "
            + "As áreas e carreiras sugeridas mostram possibilidades a partir das suas respostas neste momento: seus interesses podem mudar "
            + "e muitas outras profissões podem combinar com você. Converse com a psicóloga da sua escola e com pessoas que atuam nessas "
            + "áreas antes de tomar qualquer decisão.";

    private final JourneyRecordRepository journeyRecordRepository;
    private final StudentRepository studentRepository;
    private final KnowledgeSubareaRepository subareaRepository;
    private final AuthService authService;
    private final AreaScoreCalculator areaScoreCalculator;

    public JourneyResultService(
            final JourneyRecordRepository journeyRecordRepository,
            final StudentRepository studentRepository,
            final KnowledgeSubareaRepository subareaRepository,
            final AuthService authService,
            final AreaScoreCalculator areaScoreCalculator) {
        this.journeyRecordRepository = journeyRecordRepository;
        this.studentRepository = studentRepository;
        this.subareaRepository = subareaRepository;
        this.authService = authService;
        this.areaScoreCalculator = areaScoreCalculator;
    }

    // Resultado da jornada atual, disponível depois que ela é concluída
    @Transactional(readOnly = true)
    public JourneyResultDTO findMyCurrentResult() {
        Student student = loggedStudent();
        JourneyRecord journey = journeyRecordRepository
                .findByStudentIdAndJourneyNumber(student.getId(), student.getCurrentJourneyNumber())
                .orElseThrow(() -> new BusinessException("O resultado fica disponível quando você conclui a jornada, enviando a síntese de todas as trilhas."));
        return resultFor(journey);
    }

    // Resultado de uma jornada concluída do histórico (RF-15)
    @Transactional(readOnly = true)
    public JourneyResultDTO findMyJourneyResult(Long journeyId) {
        Student student = loggedStudent();
        JourneyRecord journey = journeyRecordRepository.findByIdAndStudentId(journeyId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Jornada não encontrada. ID: " + journeyId));
        return resultFor(journey);
    }

    // Usa os scores e os máximos guardados na conclusão da jornada: o resultado não muda depois,
    // nem se o aluno alterar o perfil, nem se as perguntas ou os pesos forem alterados
    @Transactional(readOnly = true)
    public JourneyResultDTO resultFor(JourneyRecord journey) {
        Map<KnowledgeArea, Double> scores = AreaScoreCalculator.areaMap(
                journey.getHumanitiesScore(),
                journey.getBiologicalSciencesScore(),
                journey.getExactSciencesScore(),
                journey.getArtsScore());
        // Jornadas concluídas antes do RF-12 não têm o máximo guardado: usa o das perguntas atuais
        Map<KnowledgeArea, Double> maxScores = journey.hasMaxScores()
                ? AreaScoreCalculator.areaMap(
                        journey.getHumanitiesMaxScore(),
                        journey.getBiologicalSciencesMaxScore(),
                        journey.getExactSciencesMaxScore(),
                        journey.getArtsMaxScore())
                : areaScoreCalculator.currentMaxScores();
        List<AreaScoreDTO> areaScores = AreaScoreCalculator.calculate(scores, maxScores);
        List<KnowledgeArea> highlighted = AreaScoreCalculator.highlightedAreas(areaScores);

        List<SubareaDTO> suggestions = highlighted.isEmpty()
                ? List.of()
                : subareaRepository.findByAreaInOrderByAreaAscDisplayOrderAsc(highlighted)
                        .stream()
                        .sorted(Comparator.comparingInt((KnowledgeSubarea subarea) -> highlighted.indexOf(subarea.getArea()))
                                .thenComparing(KnowledgeSubarea::getDisplayOrder))
                        .map(SubareaDTO::new)
                        .toList();

        return new JourneyResultDTO(
                journey.getId(),
                journey.getJourneyNumber(),
                journey.getConcludedAt(),
                areaScores,
                highlighted,
                suggestions,
                DISCLAIMER);
    }

    private Student loggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }
}
