package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.CustomError;
import com.synccarreira.synccarreira_api.dto.psychologist.ReportFile;
import com.synccarreira.synccarreira_api.services.PsychologistReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/psychologist-panel/{psychologistId}/reports")
@Tag(name = "Relatórios da psicóloga", description = "Endpoints para baixar, em planilha xlsx, o panorama da jornada dos alunos: dados do aluno, progresso nas trilhas, status e sínteses enviadas.")
public class PsychologistReportController {

    private final PsychologistReportService reportService;

    public PsychologistReportController(final PsychologistReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/classes/{schoolClassId}")
    @Operation(summary = "Baixa o relatório da turma, com o panorama da jornada de todos os alunos dela.")
    @ApiResponse(
            responseCode = "200",
            description = "Relatório gerado com sucesso.",
            content = { @Content(mediaType = ReportFile.XLSX_MEDIA_TYPE, schema = @Schema(type = "string", format = "binary")) }
    )
    @ApiResponse(
            responseCode = "403",
            description = "Acesso ao relatório de outra psicóloga ou de turma de outra instituição.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    @ApiResponse(
            responseCode = "404",
            description = "Psicóloga ou turma não encontrada.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    @ApiResponse(
            responseCode = "422",
            description = "Psicóloga não vinculada a uma instituição.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    public ResponseEntity<byte[]> downloadClassReport(
            @Parameter(description = "Id da psicóloga dona do painel.", required = true)
            @PathVariable Long psychologistId,
            @Parameter(description = "Id da turma.", required = true)
            @PathVariable Long schoolClassId
    ) {
        return download(reportService.generateClassReport(psychologistId, schoolClassId));
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Baixa o relatório individual do aluno, com o panorama da jornada dele.")
    @ApiResponse(
            responseCode = "200",
            description = "Relatório gerado com sucesso.",
            content = { @Content(mediaType = ReportFile.XLSX_MEDIA_TYPE, schema = @Schema(type = "string", format = "binary")) }
    )
    @ApiResponse(
            responseCode = "403",
            description = "Acesso ao relatório de outra psicóloga ou de aluno de outra instituição.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    @ApiResponse(
            responseCode = "404",
            description = "Psicóloga ou aluno não encontrado.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    @ApiResponse(
            responseCode = "422",
            description = "Psicóloga não vinculada a uma instituição.",
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CustomError.class)) }
    )
    public ResponseEntity<byte[]> downloadStudentReport(
            @Parameter(description = "Id da psicóloga dona do painel.", required = true)
            @PathVariable Long psychologistId,
            @Parameter(description = "Id do aluno.", required = true)
            @PathVariable Long studentId
    ) {
        return download(reportService.generateStudentReport(psychologistId, studentId));
    }

    private static ResponseEntity<byte[]> download(ReportFile file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getFileName())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(ReportFile.XLSX_MEDIA_TYPE))
                .contentLength(file.getContent().length)
                .body(file.getContent());
    }
}
