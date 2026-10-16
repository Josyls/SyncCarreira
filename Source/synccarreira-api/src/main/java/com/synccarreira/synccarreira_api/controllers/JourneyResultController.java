package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.CustomError;
import com.synccarreira.synccarreira_api.dto.result.JourneyResultDTO;
import com.synccarreira.synccarreira_api.services.JourneyResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Resultado da jornada", description = "Scores em percentual, áreas em destaque e leque de subáreas e carreiras sugeridas (RF-12).")
public class JourneyResultController {

    private static final String JSON = "application/json";

    private final JourneyResultService journeyResultService;

    public JourneyResultController(final JourneyResultService journeyResultService) {
        this.journeyResultService = journeyResultService;
    }

    @GetMapping("/journey/result")
    @Operation(summary = "Resultado da jornada atual do aluno logado: scores das 4 áreas em percentual, área(s) em destaque, subáreas sugeridas e o aviso de que não é um diagnóstico.")
    @ApiResponse(responseCode = "200", description = "Resultado gerado com sucesso.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = JourneyResultDTO.class)) })
    @ApiResponse(responseCode = "422", description = "A jornada atual ainda não foi concluída.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    public ResponseEntity<JourneyResultDTO> findMyCurrentResult() {
        return ResponseEntity.ok(journeyResultService.findMyCurrentResult());
    }

    @GetMapping("/journeys/me/{journeyId}/result")
    @Operation(summary = "Resultado de uma jornada concluída do histórico do aluno logado.")
    @ApiResponse(responseCode = "200", description = "Resultado gerado com sucesso.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = JourneyResultDTO.class)) })
    @ApiResponse(responseCode = "404", description = "Jornada concluída não encontrada para o aluno logado.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    public ResponseEntity<JourneyResultDTO> findMyJourneyResult(
            @Parameter(description = "Id da jornada concluída.", required = true) @PathVariable Long journeyId) {
        return ResponseEntity.ok(journeyResultService.findMyJourneyResult(journeyId));
    }
}
