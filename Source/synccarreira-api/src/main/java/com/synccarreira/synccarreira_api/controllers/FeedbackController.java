package com.synccarreira.synccarreira_api.controllers;

import com.synccarreira.synccarreira_api.dto.CustomError;
import com.synccarreira.synccarreira_api.dto.ValidationError;
import com.synccarreira.synccarreira_api.dto.feedback.FeedbackDTO;
import com.synccarreira.synccarreira_api.dto.feedback.JourneyFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.dto.feedback.SessionFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.dto.feedback.StudentFeedbackStatusDTO;
import com.synccarreira.synccarreira_api.services.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/feedbacks")
@Tag(name = "Feedbacks", description = "Feedbacks individuais e opcionais da psicóloga para o aluno, ao final da jornada ou depois de uma sessão.")
public class FeedbackController {

    private static final String JSON = "application/json";

    private final FeedbackService feedbackService;

    public FeedbackController(final FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/journey")
    @Operation(summary = "Psicóloga envia um feedback ao aluno sobre uma jornada concluída. Sem journeyNumber, vale para a última jornada concluída. O aluno é avisado por e-mail.")
    @ApiResponse(responseCode = "201", description = "Feedback enviado com sucesso.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = FeedbackDTO.class)) })
    @ApiResponse(responseCode = "403", description = "O aluno não é da instituição da psicóloga.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    @ApiResponse(responseCode = "409", description = "A jornada já recebeu feedback ou o contrato da psicóloga está vencido.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    @ApiResponse(responseCode = "422", description = "Dados inválidos ou o aluno não concluiu a jornada.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = ValidationError.class)) })
    public ResponseEntity<FeedbackDTO> sendJourneyFeedback(@Valid @RequestBody JourneyFeedbackInsertDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.sendJourneyFeedback(dto));
    }

    @PostMapping("/session")
    @Operation(summary = "Psicóloga envia um feedback individual a um aluno que participou de uma sessão realizada. O aluno é avisado por e-mail.")
    @ApiResponse(responseCode = "201", description = "Feedback enviado com sucesso.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = FeedbackDTO.class)) })
    @ApiResponse(responseCode = "403", description = "A sessão é de outra psicóloga ou o aluno não é da instituição dela.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    @ApiResponse(responseCode = "404", description = "Sessão ou aluno não encontrado.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    @ApiResponse(responseCode = "409", description = "O aluno já recebeu feedback desta sessão ou o contrato da psicóloga está vencido.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    @ApiResponse(responseCode = "422", description = "Dados inválidos, sessão cancelada ou ainda não realizada, ou aluno que não participou.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = ValidationError.class)) })
    public ResponseEntity<FeedbackDTO> sendSessionFeedback(@Valid @RequestBody SessionFeedbackInsertDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.sendSessionFeedback(dto));
    }

    @GetMapping("/sent")
    @Operation(summary = "Lista os feedbacks enviados pela psicóloga logada, do mais recente para o mais antigo.")
    @ApiResponse(responseCode = "200", description = "Feedbacks encontrados com sucesso.",
            content = { @Content(mediaType = JSON, array = @ArraySchema(schema = @Schema(implementation = FeedbackDTO.class))) })
    public ResponseEntity<List<FeedbackDTO>> findSentFeedbacks() {
        return ResponseEntity.ok(feedbackService.findSentFeedbacks());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Apaga um feedback. Feedbacks não podem ser editados, e só a psicóloga que escreveu pode apagar.")
    @ApiResponse(responseCode = "204", description = "Feedback apagado com sucesso.")
    @ApiResponse(responseCode = "403", description = "O feedback foi escrito por outra psicóloga.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    @ApiResponse(responseCode = "404", description = "Feedback não encontrado.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    public ResponseEntity<Void> delete(@Parameter(description = "Id do feedback.", required = true) @PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/panel/{psychologistId}")
    @Operation(summary = "Painel da psicóloga: para cada aluno da instituição, informa as jornadas concluídas, se a última já recebeu feedback e quantos feedbacks de sessão ele recebeu.")
    @ApiResponse(responseCode = "200", description = "Situação dos feedbacks encontrada com sucesso.",
            content = { @Content(mediaType = JSON, array = @ArraySchema(schema = @Schema(implementation = StudentFeedbackStatusDTO.class))) })
    @ApiResponse(responseCode = "403", description = "Acesso ao painel de outra psicóloga.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    public ResponseEntity<List<StudentFeedbackStatusDTO>> findFeedbackStatus(
            @Parameter(description = "Id da psicóloga.", required = true) @PathVariable Long psychologistId) {
        return ResponseEntity.ok(feedbackService.findFeedbackStatus(psychologistId));
    }

    @GetMapping("/me")
    @Operation(summary = "Lista os feedbacks recebidos pelo aluno logado (de jornada e de sessão), do mais recente para o mais antigo.")
    @ApiResponse(responseCode = "200", description = "Feedbacks encontrados com sucesso.",
            content = { @Content(mediaType = JSON, array = @ArraySchema(schema = @Schema(implementation = FeedbackDTO.class))) })
    public ResponseEntity<List<FeedbackDTO>> findMyFeedbacks() {
        return ResponseEntity.ok(feedbackService.findMyFeedbacks());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marca como lido um feedback do aluno logado (chamado quando o aluno abre o feedback).")
    @ApiResponse(responseCode = "200", description = "Feedback marcado como lido.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = FeedbackDTO.class)) })
    @ApiResponse(responseCode = "404", description = "Feedback não encontrado para o aluno logado.",
            content = { @Content(mediaType = JSON, schema = @Schema(implementation = CustomError.class)) })
    public ResponseEntity<FeedbackDTO> markAsRead(@Parameter(description = "Id do feedback.", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.markAsRead(id));
    }
}
