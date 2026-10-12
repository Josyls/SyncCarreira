package com.synccarreira.synccarreira_api.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synccarreira.synccarreira_api.services.JourneyResultService;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.ResultFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(value = JourneyResultController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class JourneyResultControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JourneyResultService service;

    @Test
    void findMyCurrentResultShouldReturnScoresHighlightAndSuggestions() throws Exception {
        when(service.findMyCurrentResult()).thenReturn(ResultFactory.createJourneyResult());

        ResultActions result = mockMvc.perform(get("/journey/result").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.journeyNumber").value(1));
        result.andExpect(jsonPath("$.scores.length()").value(4));
        result.andExpect(jsonPath("$.scores[0].area").value("BIOLOGICAS"));
        result.andExpect(jsonPath("$.scores[0].percentage").value(47.9));
        result.andExpect(jsonPath("$.scores[0].highlighted").value(true));
        result.andExpect(jsonPath("$.highlightedAreas[0]").value("BIOLOGICAS"));
        result.andExpect(jsonPath("$.suggestions[0].name").value("Medicina e Cuidado em Saúde"));
        result.andExpect(jsonPath("$.suggestions[0].careers[1]").value("Enfermagem"));
        result.andExpect(jsonPath("$.disclaimer").value(JourneyResultService.DISCLAIMER));
    }

    @Test
    void findMyCurrentResultShouldReturnUnprocessableEntityWhenJourneyIsNotConcluded() throws Exception {
        when(service.findMyCurrentResult()).thenThrow(new BusinessException("Jornada não concluída"));

        ResultActions result = mockMvc.perform(get("/journey/result").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
    }

    @Test
    void findMyJourneyResultShouldReturnResultOfHistoryJourney() throws Exception {
        when(service.findMyJourneyResult(1L)).thenReturn(ResultFactory.createJourneyResult());

        ResultActions result = mockMvc.perform(get("/journeys/me/1/result").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.journeyId").value(1));
    }

    @Test
    void findMyJourneyResultShouldReturnNotFoundWhenJourneyDoesNotExist() throws Exception {
        when(service.findMyJourneyResult(99L)).thenThrow(new ResourceNotFoundException("Jornada não encontrada"));

        ResultActions result = mockMvc.perform(get("/journeys/me/99/result").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }
}
