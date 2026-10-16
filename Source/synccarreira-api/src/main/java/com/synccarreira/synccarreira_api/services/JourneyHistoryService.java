package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.journey.JourneyContent;
import com.synccarreira.synccarreira_api.dto.journey.JourneyPdfData;
import com.synccarreira.synccarreira_api.dto.journey.JourneyRecordDTO;
import com.synccarreira.synccarreira_api.dto.journey.JourneyStatusDTO;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportFile;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.JourneyRecord;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.entities.SchoolClass;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.Synthesis;
import com.synccarreira.synccarreira_api.entities.Trail;
import com.synccarreira.synccarreira_api.entities.User;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.repositories.AnswerRepository;
import com.synccarreira.synccarreira_api.repositories.InformationLinkRepository;
import com.synccarreira.synccarreira_api.repositories.JourneyRecordRepository;
import com.synccarreira.synccarreira_api.repositories.StudentRepository;
import com.synccarreira.synccarreira_api.repositories.SynthesisRepository;
import com.synccarreira.synccarreira_api.repositories.TrailRepository;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.services.reports.JourneyPdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Histórico de jornadas do aluno (RF-15). Uma jornada é o percurso completo pelas trilhas, com todas as sínteses enviadas.
// Ao concluir, uma cópia da jornada é guardada em JourneyRecord. Ao iniciar uma nova, respostas e sínteses atuais são apagadas.
@Service
public class JourneyHistoryService {

    private static final String ANSWER_SEPARATOR = "; ";

    private final StudentRepository studentRepository;
    private final JourneyRecordRepository journeyRecordRepository;
    private final TrailRepository trailRepository;
    private final AnswerRepository answerRepository;
    private final SynthesisRepository synthesisRepository;
    private final InformationLinkRepository informationLinkRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final JourneyPdfWriter journeyPdfWriter;
    private final JourneyResultService journeyResultService;
    private final AreaScoreCalculator areaScoreCalculator;

    public JourneyHistoryService(
            final StudentRepository studentRepository,
            final JourneyRecordRepository journeyRecordRepository,
            final TrailRepository trailRepository,
            final AnswerRepository answerRepository,
            final SynthesisRepository synthesisRepository,
            final InformationLinkRepository informationLinkRepository,
            final AuthService authService,
            final ObjectMapper objectMapper,
            final JourneyPdfWriter journeyPdfWriter,
            final JourneyResultService journeyResultService,
            final AreaScoreCalculator areaScoreCalculator) {
        this.studentRepository = studentRepository;
        this.journeyRecordRepository = journeyRecordRepository;
        this.trailRepository = trailRepository;
        this.answerRepository = answerRepository;
        this.synthesisRepository = synthesisRepository;
        this.informationLinkRepository = informationLinkRepository;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.journeyPdfWriter = journeyPdfWriter;
        this.journeyResultService = journeyResultService;
        this.areaScoreCalculator = areaScoreCalculator;
    }

    @Transactional(readOnly = true)
    public List<JourneyRecordDTO> findMyJourneys() {
        Student student = loggedStudent();
        return journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId())
                .stream()
                .map(journey -> toDTO(journey, student))
                .toList();
    }

    @Transactional(readOnly = true)
    public JourneyStatusDTO findMyCurrentJourney() {
        return status(loggedStudent());
    }

    // Só existem registros de jornadas concluídas, e a busca filtra pelo aluno do token
    @Transactional(readOnly = true)
    public ReportFile generateMyJourneyPdf(Long journeyId) {
        Student student = loggedStudent();
        JourneyRecord journey = journeyRecordRepository.findByIdAndStudentId(journeyId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Jornada não encontrada. ID: " + journeyId));

        JourneyContent content = objectMapper.readValue(journey.getContent(), JourneyContent.class);
        JourneyPdfData data = new JourneyPdfData(
                student.getName(),
                student.getEmail(),
                student.getScholarYear(),
                toDTO(journey, student),
                content,
                journeyResultService.resultFor(journey));

        // Ex.: minha-jornada-2-2026-10-11.pdf
        String fileName = "minha-jornada-" + journey.getJourneyNumber() + "-"
                + JourneyPdfWriter.formatFileDate(journey.getConcludedAt()) + ".pdf";
        return new ReportFile(fileName, journeyPdfWriter.write(data));
    }

    // Chamado depois que o aluno envia uma síntese: se todas as trilhas têm síntese, a jornada atual foi concluída
    @Transactional
    public void registerJourneyIfConcluded(Student student) {
        Integer journeyNumber = student.getCurrentJourneyNumber();
        if (journeyRecordRepository.existsByStudentIdAndJourneyNumber(student.getId(), journeyNumber)) {
            return;
        }

        List<Trail> trails = trailRepository.findAllByOrderBySequentialOrderAsc();
        Map<Long, Synthesis> synthesesByTrail = synthesisRepository.findByStudentId(student.getId())
                .stream()
                .collect(Collectors.toMap(synthesis -> synthesis.getTrail().getId(), Function.identity(), (first, second) -> first));
        // Cada síntese só é aceita depois que todas as perguntas da trilha foram respondidas
        boolean concluded = !trails.isEmpty() && trails.stream().allMatch(trail -> synthesesByTrail.containsKey(trail.getId()));
        if (!concluded) {
            return;
        }

        journeyRecordRepository.save(buildRecord(student, trails, synthesesByTrail));
    }

    // O aluno só inicia uma nova jornada depois de concluir a atual, que já está guardada no histórico
    @Transactional
    public JourneyStatusDTO startNewJourney() {
        Student student = loggedStudent();
        JourneyRecord currentJourney = journeyRecordRepository
                .findByStudentIdAndJourneyNumber(student.getId(), student.getCurrentJourneyNumber())
                .orElseThrow(() -> new BusinessException("Conclua a jornada atual, enviando a síntese de todas as trilhas, antes de iniciar uma nova."));

        // A sinalização de dúvida acontece depois da conclusão, então é registrada no histórico só agora
        currentJourney.setInDoubt(Boolean.TRUE.equals(student.getInDoubt()));
        currentJourney.setDoubtFlaggedAt(student.getDoubtFlaggedAt());
        journeyRecordRepository.save(currentJourney);

        answerRepository.deleteAllByStudentId(student.getId());
        synthesisRepository.deleteAllByStudentId(student.getId());

        student.setInDoubt(false);
        student.setDoubtFlaggedAt(null);
        student.setCurrentJourneyNumber(student.getCurrentJourneyNumber() + 1);
        student.setCurrentJourneyStartedAt(Instant.now());
        // Sem respostas, o score volta a ser apenas as afinidades do perfil (RF-14)
        student.recalculateScores(List.of());
        student = studentRepository.save(student);

        return status(student);
    }

    private JourneyRecord buildRecord(Student student, List<Trail> trails, Map<Long, Synthesis> synthesesByTrail) {
        JourneyRecord journey = new JourneyRecord();
        journey.setStudent(student);
        journey.setJourneyNumber(student.getCurrentJourneyNumber());
        journey.setStartedAt(student.getCurrentJourneyStartedAt() != null
                ? student.getCurrentJourneyStartedAt()
                : student.getProfileCompletedAt());
        journey.setConcludedAt(Instant.now());

        SchoolClass schoolClass = student.getDeterminedSchoolClass();
        if (schoolClass != null) {
            journey.setSchoolClassName(schoolClass.getName());
            journey.setSchoolYear(schoolClass.getSchoolYear());
            journey.setInstitutionName(institutionName(schoolClass.getInstitution()));
        }

        journey.setHumanitiesScore(valueOrZero(student.getHumanitiesScore()));
        journey.setExactSciencesScore(valueOrZero(student.getExactSciencesScore()));
        journey.setBiologicalSciencesScore(valueOrZero(student.getBiologicalSciencesScore()));
        journey.setArtsScore(valueOrZero(student.getArtsScore()));
        journey.setHumanitiesAffinity(student.getHumanitiesAffinity());
        journey.setExactSciencesAffinity(student.getExactSciencesAffinity());
        journey.setBiologicalSciencesAffinity(student.getBiologicalSciencesAffinity());
        journey.setArtsAffinity(student.getArtsAffinity());
        // RF-12: guarda o máximo possível de cada área neste momento e as áreas em destaque pelo percentual,
        // para que o resultado da jornada concluída não mude se as perguntas ou os pesos forem alterados depois
        Map<KnowledgeArea, Double> maxScores = areaScoreCalculator.currentMaxScores();
        journey.setMaxScores(maxScores);
        List<KnowledgeArea> highestScoreAreas = AreaScoreCalculator.highlightedAreas(AreaScoreCalculator.calculate(
                AreaScoreCalculator.areaMap(
                        journey.getHumanitiesScore(),
                        journey.getBiologicalSciencesScore(),
                        journey.getExactSciencesScore(),
                        journey.getArtsScore()),
                maxScores));
        journey.setHighestScoreAreaList(highestScoreAreas);
        journey.setInDoubt(false);

        JourneyContent content = new JourneyContent(
                buildTrails(student, trails, synthesesByTrail),
                buildLinks(highestScoreAreas));
        journey.setContent(objectMapper.writeValueAsString(content));
        return journey;
    }

    private List<JourneyContent.TrailContent> buildTrails(Student student, List<Trail> trails, Map<Long, Synthesis> synthesesByTrail) {
        Map<Long, String> answersByQuestion = answerRepository.findByStudentId(student.getId())
                .stream()
                .filter(answer -> answer.getQuestionOption() != null && answer.getQuestionOption().getQuestion() != null)
                .collect(Collectors.groupingBy(
                        answer -> answer.getQuestionOption().getQuestion().getId(),
                        Collectors.mapping(answer -> answer.getQuestionOption().getOptionText(),
                                Collectors.joining(ANSWER_SEPARATOR))));

        List<JourneyContent.TrailContent> result = new ArrayList<>();
        for (Trail trail : trails) {
            List<JourneyContent.AnswerContent> answers = trail.getQuestions()
                    .stream()
                    .sorted(Comparator.comparing(Question::getId))
                    .map(question -> new JourneyContent.AnswerContent(
                            question.getContent(),
                            question.getQuestionType(),
                            answersByQuestion.get(question.getId())))
                    .toList();
            Synthesis synthesis = synthesesByTrail.get(trail.getId());
            result.add(new JourneyContent.TrailContent(
                    trail.getName(),
                    trail.getSequentialOrder(),
                    answers,
                    synthesis.getContent(),
                    synthesis.getCreatedAt()));
        }
        return result;
    }

    private List<JourneyContent.LinkContent> buildLinks(List<KnowledgeArea> highestScoreAreas) {
        return Stream.concat(
                        informationLinkRepository.findByKnowledgeAreaInOrderByTopicAscIdAsc(highestScoreAreas).stream(),
                        informationLinkRepository.findByKnowledgeAreaIsNullOrderByTopicAscIdAsc().stream())
                .map(link -> new JourneyContent.LinkContent(link.getTopic(), link.getUrl(), link.getKnowledgeArea()))
                .toList();
    }

    // Na jornada atual (concluída, ainda sem nova jornada iniciada) a dúvida pode ter sido sinalizada depois do registro
    private static JourneyRecordDTO toDTO(JourneyRecord journey, Student student) {
        if (journey.getJourneyNumber().equals(student.getCurrentJourneyNumber())) {
            return new JourneyRecordDTO(journey, Boolean.TRUE.equals(student.getInDoubt()), student.getDoubtFlaggedAt());
        }
        return new JourneyRecordDTO(journey, Boolean.TRUE.equals(journey.getInDoubt()), journey.getDoubtFlaggedAt());
    }

    private JourneyStatusDTO status(Student student) {
        List<JourneyRecord> journeys = journeyRecordRepository.findByStudentIdOrderByJourneyNumberAsc(student.getId());
        boolean currentConcluded = journeys.stream()
                .anyMatch(journey -> journey.getJourneyNumber().equals(student.getCurrentJourneyNumber()));
        return new JourneyStatusDTO(
                student.getCurrentJourneyNumber(),
                student.getCurrentJourneyStartedAt() != null ? student.getCurrentJourneyStartedAt() : student.getProfileCompletedAt(),
                currentConcluded,
                journeys.size());
    }

    private static String institutionName(Institution institution) {
        if (institution == null) {
            return null;
        }
        String tradeName = institution.getTradeName();
        return tradeName != null && !tradeName.isBlank() ? tradeName : institution.getLegalName();
    }

    private static double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }

    private Student loggedStudent() {
        User user = authService.authenticated();
        return studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado. ID: " + user.getId()));
    }
}
