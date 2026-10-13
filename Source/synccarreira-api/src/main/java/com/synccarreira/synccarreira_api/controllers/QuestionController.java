package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.QuestionDTO;
import com.synccarreira.synccarreira_api.services.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/questions")
@Tag(name = "Perguntas", description = "Endpoints para interagir com as perguntas da aplicação.")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @GetMapping
    @Operation(summary = "Busca todas as perguntas.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perguntas encontradas com sucesso."
            )
    })
    public ResponseEntity<List<QuestionDTO>> findAll() {
        return ResponseEntity.ok(questionService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma pergunta pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pergunta encontrada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<QuestionDTO> findById(
            @Parameter(description = "Id da pergunta.", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(questionService.findById(id));
    }

    @GetMapping("/trail/{trailId}")
    @Operation(summary = "Busca uma lista de perguntas pelo id da trilha correspondente.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perguntas encontradas com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<List<QuestionDTO>> findByTrilha(
            @Parameter(description = "Id da trilha.", required = true)
            @PathVariable Long trailId
    ) {
        return ResponseEntity.ok(questionService.findByTrail(trailId));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova pergunta.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Pergunta criada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<QuestionDTO> createQuestion(@Valid @RequestBody QuestionDTO dto) {
        QuestionDTO question = questionService.createQuestion(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(question.id())
                .toUri();
        return ResponseEntity.created(uri).body(question);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma pergunta pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Pergunta atualizada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<QuestionDTO> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionDTO dto) {
        return ResponseEntity.ok(questionService.updateQuestion(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta uma pergunta pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Pergunta deletada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<Void> deleteQuestionById(@PathVariable Long id) {
        questionService.deleteQuestionById(id);
        return ResponseEntity.noContent().build();
    }
}
