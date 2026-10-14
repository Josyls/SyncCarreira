package com.synccarreira.synccarreira_api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synccarreira.synccarreira_api.dto.feedback.JourneyFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.dto.feedback.SessionFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.services.FeedbackService;
import com.synccarreira.synccarreira_api.services.exceptions.ConflictException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.tests.FeedbackFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@WebMvcTest(value = FeedbackController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class FeedbackControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService service;

    private ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));
    }

    @Test
    void sendJourneyFeedbackShouldReturnCreated() throws Exception {
        when(service.sendJourneyFeedback(any())).thenReturn(FeedbackFactory.createJourneyFeedbackDTO());

        ResultActions result = postJson("/feedbacks/journey", new JourneyFeedbackInsertDTO(1L, null, FeedbackFactory.CONTENT));

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.type").value("JORNADA"));
        result.andExpect(jsonPath("$.journeyNumber").value(1));
        result.andExpect(jsonPath("$.read").value(false));
    }

    @Test
    void sendJourneyFeedbackShouldReturnUnprocessableContentWhenContentIsBlank() throws Exception {
        ResultActions result = postJson("/feedbacks/journey", new JourneyFeedbackInsertDTO(1L, null, "  "));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.errors[0].fieldName").value("content"));
        verify(service, never()).sendJourneyFeedback(any());
    }

    @Test
    void sendJourneyFeedbackShouldReturnConflictWhenJourneyAlreadyHasFeedback() throws Exception {
        when(service.sendJourneyFeedback(any())).thenThrow(new ConflictException("Já foi enviado um feedback para esta jornada do aluno."));

        ResultActions result = postJson("/feedbacks/journey", new JourneyFeedbackInsertDTO(1L, null, FeedbackFactory.CONTENT));

        result.andExpect(status().isConflict());
    }

    @Test
    void sendSessionFeedbackShouldReturnCreated() throws Exception {
        when(service.sendSessionFeedback(any())).thenReturn(FeedbackFactory.createJourneyFeedbackDTO());

        ResultActions result = postJson("/feedbacks/session", FeedbackFactory.createSessionFeedbackInsertDTO(1L, 1L));

        result.andExpect(status().isCreated());
    }

    @Test
    void sendSessionFeedbackShouldReturnUnprocessableContentWhenAppointmentIsMissing() throws Exception {
        ResultActions result = postJson("/feedbacks/session", new SessionFeedbackInsertDTO(null, 1L, "Texto"));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.errors[0].fieldName").value("appointmentId"));
    }

    @Test
    void findSentFeedbacksShouldReturnList() throws Exception {
        when(service.findSentFeedbacks()).thenReturn(List.of(FeedbackFactory.createJourneyFeedbackDTO()));

        ResultActions result = mockMvc.perform(get("/feedbacks/sent").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$[0].psychologistName").value("Lorena Souza"));
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        ResultActions result = mockMvc.perform(delete("/feedbacks/1"));

        result.andExpect(status().isNoContent());
        verify(service).delete(1L);
    }

    @Test
    void deleteShouldReturnForbiddenWhenFeedbackIsFromAnotherPsychologist() throws Exception {
        doThrow(new ForbiddenException("Acesso negado")).when(service).delete(2L);

        ResultActions result = mockMvc.perform(delete("/feedbacks/2"));

        result.andExpect(status().isForbidden());
    }

    @Test
    void findFeedbackStatusShouldReturnStatusOfStudents() throws Exception {
        when(service.findFeedbackStatus(1L)).thenReturn(List.of(FeedbackFactory.createStudentFeedbackStatusDTO()));

        ResultActions result = mockMvc.perform(get("/feedbacks/panel/1").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$[0].journeyFeedbackSent").value(true));
        result.andExpect(jsonPath("$[0].sessionFeedbacksSent").value(2));
    }

    @Test
    void findMyFeedbacksShouldReturnList() throws Exception {
        when(service.findMyFeedbacks()).thenReturn(List.of(FeedbackFactory.createJourneyFeedbackDTO()));

        ResultActions result = mockMvc.perform(get("/feedbacks/me").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$[0].content").value(FeedbackFactory.CONTENT));
    }

    @Test
    void markAsReadShouldReturnFeedback() throws Exception {
        when(service.markAsRead(1L)).thenReturn(FeedbackFactory.createJourneyFeedbackDTO());

        ResultActions result = mockMvc.perform(patch("/feedbacks/1/read").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(1));
    }
}
