package com.synccarreira.synccarreira_api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synccarreira.synccarreira_api.dto.AnswerDTO;
import com.synccarreira.synccarreira_api.entities.Answer;
import com.synccarreira.synccarreira_api.entities.Question;
import com.synccarreira.synccarreira_api.services.AnswerService;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.QuestionFactory;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

@WebMvcTest(value = AnswerController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class AnswerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnswerService service;

    @Test
    void insertShouldReturnCreatedWithOptionTextAsContent() throws Exception {
        Question question = QuestionFactory.createQuestion();
        AnswerDTO dto = new AnswerDTO(new Answer(10L, StudentFactory.createStudent(), question.getOptions().getFirst()));
        when(service.insert(any())).thenReturn(dto);

        ResultActions result = mockMvc.perform(post("/answers")
                .content("{\"questionOptionId\": 1}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").value(10L));
        result.andExpect(jsonPath("$.content").value("Matemática"));
    }

    @Test
    void insertShouldIgnoreContentFieldStillSentByOldClients() throws Exception {
        Question question = QuestionFactory.createQuestion();
        when(service.insert(any())).thenReturn(new AnswerDTO(new Answer(10L, StudentFactory.createStudent(), question.getOptions().getFirst())));

        ResultActions result = mockMvc.perform(post("/answers")
                .content("{\"content\": \"texto enviado pelo cliente\", \"studentId\": 1, \"questionOptionId\": 1}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.content").value("Matemática"));
    }

    @Test
    void insertShouldReturnUnprocessableContentWhenQuestionOptionIsMissing() throws Exception {
        ResultActions result = mockMvc.perform(post("/answers")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.errors[0].fieldName").value("questionOptionId"));
        verify(service, never()).insert(any());
    }

    @Test
    void insertShouldIgnoreStudentIdSentByClient() throws Exception {
        Question question = QuestionFactory.createQuestion();
        when(service.insert(any())).thenReturn(new AnswerDTO(new Answer(10L, StudentFactory.createStudent(), question.getOptions().getFirst())));

        ResultActions result = mockMvc.perform(post("/answers")
                .content("{\"studentId\": 999, \"questionOptionId\": 1}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
    }

    @Test
    void findForLoggedStudentShouldReturnOkWithAnswersOfTrail() throws Exception {
        Question question = QuestionFactory.createQuestion();
        when(service.findForLoggedStudent(1L)).thenReturn(List.of(new AnswerDTO(new Answer(10L, StudentFactory.createStudent(), question.getOptions().getFirst()))));

        // Um studentId enviado por clientes antigos não é mais usado
        ResultActions result = mockMvc.perform(get("/answers")
                .param("trailId", "1")
                .param("studentId", "999")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$[0].id").value(10L));
        verify(service).findForLoggedStudent(1L);
    }

    // Tratamento de exceções: corpo ilegível (400), valor incompatível (422) e bloqueio no banco (409)

    @Test
    void insertShouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
        ResultActions result = mockMvc.perform(post("/answers")
                .content("{\"questionOptionId\": 1")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.error").value("Corpo da requisição ausente ou com JSON malformado."));
        verify(service, never()).insert(any());
    }

    @Test
    void insertShouldReturnBadRequestWhenBodyIsMissing() throws Exception {
        ResultActions result = mockMvc.perform(post("/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        verify(service, never()).insert(any());
    }

    @Test
    void insertShouldReturnUnprocessableContentWhenValueHasWrongType() throws Exception {
        ResultActions result = mockMvc.perform(post("/answers")
                .content("{\"questionOptionId\": \"abc\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.error").value("Valor inválido em um ou mais campos da requisição."));
        verify(service, never()).insert(any());
    }

    @Test
    void insertShouldReturnConflictWhenDatabaseLockFails() throws Exception {
        when(service.insert(any())).thenThrow(new CannotAcquireLockException("Deadlock found when trying to get lock"));

        ResultActions result = mockMvc.perform(post("/answers")
                .content("{\"questionOptionId\": 1}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isConflict());
        result.andExpect(jsonPath("$.error").value("Os dados estavam sendo alterados por outra operação ao mesmo tempo. Tente novamente."));
    }

    // Desmarcar uma opção de pergunta de múltipla escolha

    @Test
    void uncheckShouldReturnNoContent() throws Exception {
        ResultActions result = mockMvc.perform(delete("/answers")
                .param("questionOptionId", "1")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNoContent());
        verify(service).uncheck(1L);
    }

    @Test
    void uncheckShouldReturnBadRequestWhenQuestionOptionIsMissing() throws Exception {
        ResultActions result = mockMvc.perform(delete("/answers")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        verify(service, never()).uncheck(anyLong());
    }

    @Test
    void uncheckShouldReturnUnprocessableContentWhenQuestionIsNotMultipleChoice() throws Exception {
        doThrow(new BusinessException("Só é possível desmarcar opções de perguntas de múltipla escolha."))
                .when(service).uncheck(1L);

        ResultActions result = mockMvc.perform(delete("/answers")
                .param("questionOptionId", "1")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.error").value("Só é possível desmarcar opções de perguntas de múltipla escolha."));
    }

    @Test
    void uncheckShouldReturnNotFoundWhenQuestionOptionDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Question option not found")).when(service).uncheck(100L);

        ResultActions result = mockMvc.perform(delete("/answers")
                .param("questionOptionId", "100")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    @Test
    void uncheckShouldReturnForbiddenWhenTrailIsNotAccessible() throws Exception {
        doThrow(new ForbiddenException("Trilha não liberada.")).when(service).uncheck(1L);

        ResultActions result = mockMvc.perform(delete("/answers")
                .param("questionOptionId", "1")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
    }

    @Test
    void uncheckShouldReturnConflictWhenDatabaseLockFails() throws Exception {
        doThrow(new CannotAcquireLockException("Deadlock found when trying to get lock")).when(service).uncheck(1L);

        ResultActions result = mockMvc.perform(delete("/answers")
                .param("questionOptionId", "1")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isConflict());
    }
}
