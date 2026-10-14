package com.synccarreira.synccarreira_api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synccarreira.synccarreira_api.dto.InformationLinkDTO;
import com.synccarreira.synccarreira_api.dto.InformationTrailDTO;
import com.synccarreira.synccarreira_api.dto.result.AreaScoreDTO;
import com.synccarreira.synccarreira_api.entities.enums.KnowledgeArea;
import com.synccarreira.synccarreira_api.services.InformationLinkService;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.StudentFactory;
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

@WebMvcTest(value = InformationLinkController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class InformationLinkControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InformationLinkService service;

    @Test
    void findForLoggedStudentShouldReturnInformationTrail() throws Exception {
        InformationTrailDTO dto = new InformationTrailDTO(
                List.of(KnowledgeArea.BIOLOGICAS),
                StudentFactory.createStudentScoreDTO(),
                List.of(new AreaScoreDTO(KnowledgeArea.BIOLOGICAS, "Biológicas", 8.25, 15.0, 55.0, true)),
                List.of(new InformationLinkDTO(1L, "Educação Física", "https://www.confef.org.br/", KnowledgeArea.BIOLOGICAS)),
                List.of(new InformationLinkDTO(2L, "ProUni", "https://acessounico.mec.gov.br/prouni", null)));
        when(service.findForLoggedStudent()).thenReturn(dto);

        ResultActions result = mockMvc.perform(get("/information-links/me").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.recommendedAreas[0]").value("BIOLOGICAS"));
        result.andExpect(jsonPath("$.score.biologicalSciencesScore").value(8.25));
        result.andExpect(jsonPath("$.areaScores[0].percentage").value(55.0));
        result.andExpect(jsonPath("$.areaScores[0].highlighted").value(true));
        result.andExpect(jsonPath("$.careerLinks[0].topic").value("Educação Física"));
        result.andExpect(jsonPath("$.careerLinks[0].url").value("https://www.confef.org.br/"));
        result.andExpect(jsonPath("$.universityAccessLinks[0].topic").value("ProUni"));
    }

    @Test
    void findForLoggedStudentShouldReturnForbiddenWhenPreviousTrailsAreNotConcluded() throws Exception {
        when(service.findForLoggedStudent()).thenThrow(ForbiddenException.class);

        ResultActions result = mockMvc.perform(get("/information-links/me").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
    }

    @Test
    void findAllShouldReturnAllLinks() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new InformationLinkDTO(1L, "Educação Física", "https://www.confef.org.br/", KnowledgeArea.BIOLOGICAS),
                new InformationLinkDTO(2L, "ProUni", "https://acessounico.mec.gov.br/prouni", null)));

        ResultActions result = mockMvc.perform(get("/information-links").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$[0].topic").value("Educação Física"));
        result.andExpect(jsonPath("$[0].knowledgeArea").value("BIOLOGICAS"));
        result.andExpect(jsonPath("$[1].topic").value("ProUni"));
    }

    @Test
    void createShouldReturnCreatedWhenDataIsValid() throws Exception {
        when(service.create(any())).thenReturn(
                new InformationLinkDTO(10L, "Medicina", "https://www.cfm.org.br/", KnowledgeArea.BIOLOGICAS));

        ResultActions result = mockMvc.perform(post("/information-links")
                .content("{\"topic\": \"Medicina\", \"url\": \"https://www.cfm.org.br/\", \"knowledgeArea\": \"BIOLOGICAS\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(header().string("Location", "http://localhost/information-links/10"));
        result.andExpect(jsonPath("$.id").value(10));
        result.andExpect(jsonPath("$.topic").value("Medicina"));
    }

    @Test
    void createShouldReturnCreatedWhenKnowledgeAreaIsMissing() throws Exception {
        when(service.create(any())).thenReturn(
                new InformationLinkDTO(11L, "Vestibular", "https://www.vestibular.com.br/", null));

        ResultActions result = mockMvc.perform(post("/information-links")
                .content("{\"topic\": \"Vestibular\", \"url\": \"https://www.vestibular.com.br/\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void createShouldReturnUnprocessableContentWhenTopicIsBlank() throws Exception {
        ResultActions result = mockMvc.perform(post("/information-links")
                .content("{\"topic\": \" \", \"url\": \"https://www.cfm.org.br/\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.errors[0].fieldName").value("topic"));
        verify(service, never()).create(any());
    }

    @Test
    void createShouldReturnUnprocessableContentWhenUrlIsInvalid() throws Exception {
        ResultActions result = mockMvc.perform(post("/information-links")
                .content("{\"topic\": \"Medicina\", \"url\": \"www.cfm.org.br\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.errors[0].fieldName").value("url"));
        verify(service, never()).create(any());
    }

    @Test
    void createShouldReturnUnprocessableContentWhenKnowledgeAreaDoesNotExist() throws Exception {
        ResultActions result = mockMvc.perform(post("/information-links")
                .content("{\"topic\": \"Medicina\", \"url\": \"https://www.cfm.org.br/\", \"knowledgeArea\": \"SAUDE\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
        verify(service, never()).create(any());
    }

    @Test
    void deleteShouldReturnNoContentWhenIdExists() throws Exception {
        ResultActions result = mockMvc.perform(delete("/information-links/{id}", 1L));

        result.andExpect(status().isNoContent());
        verify(service).delete(1L);
    }

    @Test
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Link não encontrado. ID: 99")).when(service).delete(99L);

        ResultActions result = mockMvc.perform(delete("/information-links/{id}", 99L));

        result.andExpect(status().isNotFound());
    }
}
