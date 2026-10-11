package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.CustomError;
import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.dto.StudentProfileDTO;
import com.synccarreira.synccarreira_api.dto.ValidationError;
import com.synccarreira.synccarreira_api.services.StudentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students/me")
@Tag(name = "Perfil do aluno", description = "Endpoints para o aluno logado consultar e personalizar o próprio perfil.")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(final StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping
    @Operation(summary = "Busca o perfil do aluno logado, com as afinidades declaradas, os scores atuais e se o perfil já foi preenchido.")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil encontrado com sucesso.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = StudentProfileDTO.class)) }
    )
    @ApiResponse(
            responseCode = "404",
            description = "Aluno não encontrado.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    public ResponseEntity<StudentProfileDTO> findMyProfile() {
        return ResponseEntity.ok(studentProfileService.findMyProfile());
    }

    @PutMapping("/affinities")
    @Operation(summary = "Registra ou altera as afinidades do aluno logado com Humanas, Exatas, Biológicas e Artes (0 a 5). Os valores são somados aos scores de cada área.")
    @ApiResponse(
            responseCode = "200",
            description = "Afinidades registradas e scores recalculados com sucesso.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = StudentProfileDTO.class)) }
    )
    @ApiResponse(
            responseCode = "404",
            description = "Aluno não encontrado.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    @ApiResponse(
            responseCode = "422",
            description = "Afinidade ausente ou fora da escala de 0 a 5.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class)) }
    )
    public ResponseEntity<StudentProfileDTO> updateMyAffinities(@Valid @RequestBody StudentAffinityDTO dto) {
        return ResponseEntity.ok(studentProfileService.updateMyAffinities(dto));
    }
}
