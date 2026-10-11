package com.synccarreira.synccarreira_api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synccarreira.synccarreira_api.dto.StudentAffinityDTO;
import com.synccarreira.synccarreira_api.dto.StudentProfileDTO;
import com.synccarreira.synccarreira_api.services.StudentProfileService;
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
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(value = StudentProfileController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class StudentProfileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentProfileService service;

    private ResultActions performUpdate(Object body) throws Exception {
        return mockMvc.perform(put("/students/me/affinities")
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));
    }

    @Test
    void findMyProfileShouldReturnProfileOfLoggedStudent() throws Exception {
        when(service.findMyProfile()).thenReturn(StudentFactory.createStudentProfileDTO());

        ResultActions result = mockMvc.perform(get("/students/me").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.profileCompleted").value(true));
        result.andExpect(jsonPath("$.affinities.humanities").value(3));
        result.andExpect(jsonPath("$.affinities.arts").value(5));
        result.andExpect(jsonPath("$.score.studentId").value(1L));
    }

    @Test
    void findMyProfileShouldReturnNotFoundWhenLoggedUserIsNotAStudent() throws Exception {
        when(service.findMyProfile()).thenThrow(new ResourceNotFoundException("Aluno não encontrado. ID: 2"));

        ResultActions result = mockMvc.perform(get("/students/me").accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    @Test
    void updateMyAffinitiesShouldReturnOkWhenDataIsValid() throws Exception {
        StudentProfileDTO dto = StudentFactory.createStudentProfileDTO();
        when(service.updateMyAffinities(any())).thenReturn(dto);

        ResultActions result = performUpdate(StudentFactory.createStudentAffinityDTO());

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.profileCompleted").value(true));
        result.andExpect(jsonPath("$.affinities.biologicalSciences").value(4));
    }

    @Test
    void updateMyAffinitiesShouldReturnUnprocessableContentWhenAffinityIsAboveFive() throws Exception {
        ResultActions result = performUpdate(new StudentAffinityDTO(6, 2, 4, 5));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.errors[0].fieldName").value("humanities"));
        verify(service, never()).updateMyAffinities(any());
    }

    @Test
    void updateMyAffinitiesShouldReturnUnprocessableContentWhenAffinityIsNegative() throws Exception {
        ResultActions result = performUpdate(new StudentAffinityDTO(3, -1, 4, 5));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.errors[0].fieldName").value("exactSciences"));
    }

    @Test
    void updateMyAffinitiesShouldReturnUnprocessableContentWhenAffinityIsMissing() throws Exception {
        ResultActions result = performUpdate(new StudentAffinityDTO(3, 2, null, 5));

        result.andExpect(status().isUnprocessableContent());
        result.andExpect(jsonPath("$.errors[0].fieldName").value("biologicalSciences"));
    }
}
