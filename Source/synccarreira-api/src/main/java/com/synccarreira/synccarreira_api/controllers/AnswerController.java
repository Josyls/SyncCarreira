package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.*;
import com.synccarreira.synccarreira_api.services.AnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/answers")
@Tag(name = "Respostas", description = "Endpoints para gerenciar respostas dadas pelos alunos às perguntas das trilhas.")
public class AnswerController {

    @Autowired
    private AnswerService answerService;

    @Operation(summary = "Busca todas as respostas que o aluno logado deu às perguntas de uma trilha, pelo id da trilha.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Respostas encontradas com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<List<AnswerDTO>> findForLoggedStudent(
            @Parameter(description = "Id da trilha à qual as respostas pertencem.", required = true)
            @RequestParam(value="trailId", required = true)
            Long trailId
    ) {
        List<AnswerDTO> dto = answerService.findForLoggedStudent(trailId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Insere uma nova resposta do aluno logado.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Resposta inserida com sucesso.",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = StudentDTO.class)) }
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Dados inválidos.",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class)) }
            )
    })
    @PostMapping
    public ResponseEntity<AnswerDTO> insert(@Valid @RequestBody AnswerInsertDTO dto) {
        AnswerDTO result = answerService.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(result.getId()).toUri();
        return ResponseEntity.created(uri).body(result);
    }
}
