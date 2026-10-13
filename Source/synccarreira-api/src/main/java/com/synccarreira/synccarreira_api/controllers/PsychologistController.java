package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.PsychologistDTO;
import com.synccarreira.synccarreira_api.dto.PsychologistInsertDTO;
import com.synccarreira.synccarreira_api.services.PsychologistService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/psychologists")
@Tag(name = "Psicólogos", description = "Endpoints para interagir com a entidade psicólogo dentro da aplicação.")
public class PsychologistController {

    @Autowired
    private PsychologistService psychologistService;

    @GetMapping
    @Operation(summary = "Busca todos os psicólogos.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Psicólogos encontrados com sucesso."
            )
    })
    public ResponseEntity<List<PsychologistDTO>> findAll() {
        return ResponseEntity.ok(psychologistService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um(a) psicólogo(a) pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Psicólogo(a) encontrado(a) com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<PsychologistDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(psychologistService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cria um(a) novo(a) psicólogo(a).")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Psicólogo(a) criado(a) com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<PsychologistDTO> createPsychologist(@Valid @RequestBody PsychologistInsertDTO dto) {
        PsychologistDTO psicologa = psychologistService.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(psicologa.id())
                .toUri();
        return ResponseEntity.created(uri).body(psicologa);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um(a) psicólogo(a) pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Psicólogo(a) atualizado(a) com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    public ResponseEntity<PsychologistDTO> update(@PathVariable Long id, @Valid @RequestBody PsychologistInsertDTO dto) {
        return ResponseEntity.ok(psychologistService.update(id, dto));
    }

    @Operation(summary = "Deleta um(a) psicólogo(a) pelo id.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Psicólogo(a) deletada com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        psychologistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verifica se o contrato de um(a) psicólogo(a) ainda está válido, através do id do(a) psicólogo(a).")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Psicólogo(a) deletado(a) com sucesso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found.",
                    content = @Content
            )
    })
    @GetMapping("/{id}/valid-contract")
    public ResponseEntity<Boolean> isContractValid(@PathVariable Long id) {
        PsychologistDTO psychologistDTO = psychologistService.findById(id);
        return ResponseEntity.ok(psychologistDTO.isContractValid());
    }
}
