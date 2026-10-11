package com.synccarreira.synccarreira_api.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synccarreira.synccarreira_api.services.JourneyHistoryService;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.tests.JourneyFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

@WebMvcTest(value = JourneyHistoryController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class JourneyHistoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JourneyHistoryService service;

    @Test
    void findMyJourneysShouldReturnConcludedJourneys() throws Exception {
        when(service.findMyJourneys()).thenReturn(List.of(JourneyFactory.createJourneyRecordDTO()));

        ResultActions result = mockMvc.perform(get("/journeys/me").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$[0].journeyNumber").value(1));
        result.andExpect(jsonPath("$[0].artsScore").value(20.5));
        result.andExpect(jsonPath("$[0].highestScoreAreas[0]").value("ARTES"));
        result.andExpect(jsonPath("$[0].affinities.arts").value(5));
    }

    @Test
    void findMyCurrentJourneyShouldReturnStatus() throws Exception {
        when(service.findMyCurrentJourney()).thenReturn(JourneyFactory.createJourneyStatusDTO());

        ResultActions result = mockMvc.perform(get("/journeys/me/current").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.currentJourneyNumber").value(2));
        result.andExpect(jsonPath("$.currentJourneyConcluded").value(false));
        result.andExpect(jsonPath("$.concludedJourneys").value(1));
    }

    @Test
    void startNewJourneyShouldReturnNewStatus() throws Exception {
        when(service.startNewJourney()).thenReturn(JourneyFactory.createJourneyStatusDTO());

        ResultActions result = mockMvc.perform(post("/journeys/me/new").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.currentJourneyNumber").value(2));
    }

    @Test
    void startNewJourneyShouldReturnUnprocessableContentWhenCurrentJourneyIsNotConcluded() throws Exception {
        when(service.startNewJourney()).thenThrow(new BusinessException("Conclua a jornada atual antes de iniciar uma nova."));

        ResultActions result = mockMvc.perform(post("/journeys/me/new").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
    }
}
