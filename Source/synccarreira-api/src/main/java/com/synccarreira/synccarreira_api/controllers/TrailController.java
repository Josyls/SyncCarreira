package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.CustomError;
import com.synccarreira.synccarreira_api.dto.TrailDTO;
import com.synccarreira.synccarreira_api.dto.TrailUpdateDTO;
import com.synccarreira.synccarreira_api.services.TrailService;
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
@RequestMapping("/trails")
@Tag(name = "Trilhas", description = "Endpoints para interagir com as trilhas da aplicação.")
public class TrailController {

    @Autowired
    private TrailService trailService;

    @GetMapping
    @Operation(summary = "Busca todas as trilhas.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Trilhas encontradas com sucesso."
            )
    })
    public ResponseEntity<List<TrailDTO>> findAll() {
        return ResponseEntity.ok(trailService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma trilha pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Trilha encontrada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<TrailDTO> findById(
            @Parameter(description = "Id da trilha.", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(trailService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova trilha.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Trilha criada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<TrailDTO> create(@Valid @RequestBody TrailDTO dto) {
        TrailDTO trailDto = trailService.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(trailDto.id())
                .toUri();
        return ResponseEntity.created(uri).body(trailDto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma trilha pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Trilha atualizada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable content.",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
            )
    })
    public ResponseEntity<TrailDTO> update(
            @Parameter(description = "Id da trilha que será atualizada.", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TrailUpdateDTO dto
    ) {
        return ResponseEntity.ok(trailService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta uma trilha pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Trilha deletada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id da trilha que será deletada.", required = true)
            @PathVariable Long id
    ) {
        trailService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/can-access")
    @Operation(summary = "Verifica se o aluno pode acessar a trilha informada, através do id da trilha.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Verificação feita com sucesso."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<Boolean> canAccess(
            @Parameter(description = "Id da trilha que será verificada se o aluno pode acessar.", required = true)
            @PathVariable Long id) {
        boolean access = trailService.canAccess(id);
        return ResponseEntity.ok(access);
    }
}
