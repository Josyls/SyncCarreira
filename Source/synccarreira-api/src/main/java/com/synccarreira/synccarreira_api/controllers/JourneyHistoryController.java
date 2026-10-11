package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.CustomError;
import com.synccarreira.synccarreira_api.dto.journey.JourneyRecordDTO;
import com.synccarreira.synccarreira_api.dto.journey.JourneyStatusDTO;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportFile;
import com.synccarreira.synccarreira_api.services.JourneyHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/journeys/me")
@Tag(name = "Histórico de jornadas", description = "Endpoints para o aluno logado consultar as jornadas concluídas e iniciar uma nova jornada.")
public class JourneyHistoryController {

    private final JourneyHistoryService journeyHistoryService;

    public JourneyHistoryController(final JourneyHistoryService journeyHistoryService) {
        this.journeyHistoryService = journeyHistoryService;
    }

    @GetMapping
    @Operation(summary = "Lista as jornadas concluídas pelo aluno logado, da mais antiga para a mais recente, com scores e áreas de maior afinidade de cada uma.")
    @ApiResponse(
            responseCode = "200",
            description = "Histórico encontrado com sucesso.",
            content = { @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = JourneyRecordDTO.class))) }
    )
    @ApiResponse(
            responseCode = "404",
            description = "Aluno não encontrado.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    public ResponseEntity<List<JourneyRecordDTO>> findMyJourneys() {
        return ResponseEntity.ok(journeyHistoryService.findMyJourneys());
    }

    @GetMapping("/current")
    @Operation(summary = "Informa o número da jornada atual do aluno logado e se ela já foi concluída.")
    @ApiResponse(
            responseCode = "200",
            description = "Jornada atual encontrada com sucesso.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = JourneyStatusDTO.class)) }
    )
    @ApiResponse(
            responseCode = "404",
            description = "Aluno não encontrado.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    public ResponseEntity<JourneyStatusDTO> findMyCurrentJourney() {
        return ResponseEntity.ok(journeyHistoryService.findMyCurrentJourney());
    }

    @PostMapping("/new")
    @Operation(summary = "Inicia uma nova jornada para o aluno logado. A jornada concluída continua no histórico; as respostas e sínteses atuais são apagadas e as trilhas recomeçam.")
    @ApiResponse(
            responseCode = "200",
            description = "Nova jornada iniciada com sucesso.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = JourneyStatusDTO.class)) }
    )
    @ApiResponse(
            responseCode = "404",
            description = "Aluno não encontrado.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    @ApiResponse(
            responseCode = "422",
            description = "A jornada atual ainda não foi concluída.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    public ResponseEntity<JourneyStatusDTO> startNewJourney() {
        return ResponseEntity.ok(journeyHistoryService.startNewJourney());
    }

    @GetMapping("/{journeyId}/pdf")
    @Operation(summary = "Baixa o PDF de uma jornada concluída do aluno logado, com perfil, perguntas e respostas, sínteses e resultado.")
    @ApiResponse(
            responseCode = "200",
            description = "PDF gerado com sucesso.",
            content = { @Content(mediaType = "application/pdf") }
    )
    @ApiResponse(
            responseCode = "404",
            description = "Jornada concluída não encontrada para o aluno logado.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    public ResponseEntity<byte[]> downloadMyJourneyPdf(
            @Parameter(description = "Id da jornada concluída.", required = true)
            @PathVariable Long journeyId
    ) {
        ReportFile file = journeyHistoryService.generateMyJourneyPdf(journeyId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getFileName())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.getContent().length)
                .body(file.getContent());
    }
}
