package com.synccarreira.synccarreira_api.controllers;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synccarreira.synccarreira_api.dto.psychologist.ReportFile;
import com.synccarreira.synccarreira_api.services.PsychologistReportService;
import com.synccarreira.synccarreira_api.services.exceptions.BusinessException;
import com.synccarreira.synccarreira_api.services.exceptions.ForbiddenException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(value = PsychologistReportController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class PsychologistReportControllerTests {

    private static final String CLASS_REPORT_URL = "/psychologist-panel/{psychologistId}/reports/classes/{schoolClassId}";
    private static final String STUDENT_REPORT_URL = "/psychologist-panel/{psychologistId}/reports/students/{studentId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PsychologistReportService service;

    private Long psychologistId, otherPsychologistId, psychologistWithoutInstitutionId;
    private Long existingId, nonExistingId, otherInstitutionId;
    private byte[] content;
    private ReportFile classReport, studentReport;

    @BeforeEach
    void setUp() {
        psychologistId = 1L;
        otherPsychologistId = 2L;
        psychologistWithoutInstitutionId = 3L;
        existingId = 1L;
        nonExistingId = 100L;
        otherInstitutionId = 2L;
        content = new byte[] {80, 75, 3, 4};

        classReport = new ReportFile("relatorio-turma-3-ano-a-2026-2026-10-08.xlsx", content);
        studentReport = new ReportFile("relatorio-aluno-ana-souza-2026-10-08.xlsx", content);
    }

    @Test
    void downloadClassReportShouldReturnXlsxFileWhenClassExists() throws Exception {
        when(service.generateClassReport(psychologistId, existingId)).thenReturn(classReport);

        ResultActions result = mockMvc.perform(get(CLASS_REPORT_URL, psychologistId, existingId));

        result.andExpect(status().isOk());
        result.andExpect(content().contentType(ReportFile.XLSX_MEDIA_TYPE));
        result.andExpect(content().bytes(content));
        result.andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, content.length));
        result.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"relatorio-turma-3-ano-a-2026-2026-10-08.xlsx\""));
    }

    @Test
    void downloadClassReportShouldNotEncodeFileNameWithCharset() throws Exception {
        when(service.generateClassReport(psychologistId, existingId)).thenReturn(classReport);

        ResultActions result = mockMvc.perform(get(CLASS_REPORT_URL, psychologistId, existingId));

        result.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, not(containsString("UTF-8"))));
    }

    @Test
    void downloadClassReportShouldReturnNotFoundWhenClassOrPsychologistDoesNotExist() throws Exception {
        when(service.generateClassReport(psychologistId, nonExistingId)).thenThrow(ResourceNotFoundException.class);

        ResultActions result = mockMvc.perform(get(CLASS_REPORT_URL, psychologistId, nonExistingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
        result.andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void downloadClassReportShouldReturnForbiddenWhenClassBelongsToAnotherInstitution() throws Exception {
        when(service.generateClassReport(psychologistId, otherInstitutionId))
                .thenThrow(new ForbiddenException("Acesso negado: a turma não pertence à instituição da psicóloga."));

        ResultActions result = mockMvc.perform(get(CLASS_REPORT_URL, psychologistId, otherInstitutionId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
        result.andExpect(jsonPath("$.error").value("Acesso negado: a turma não pertence à instituição da psicóloga."));
    }

    @Test
    void downloadClassReportShouldReturnForbiddenWhenLoggedUserIsAnotherPsychologist() throws Exception {
        when(service.generateClassReport(otherPsychologistId, existingId)).thenThrow(ForbiddenException.class);

        ResultActions result = mockMvc.perform(get(CLASS_REPORT_URL, otherPsychologistId, existingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
    }

    @Test
    void downloadClassReportShouldReturnUnprocessableContentWhenPsychologistHasNoInstitution() throws Exception {
        when(service.generateClassReport(psychologistWithoutInstitutionId, existingId)).thenThrow(BusinessException.class);

        ResultActions result = mockMvc.perform(get(CLASS_REPORT_URL, psychologistWithoutInstitutionId, existingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
    }

    @Test
    void downloadStudentReportShouldReturnXlsxFileWhenStudentExists() throws Exception {
        when(service.generateStudentReport(psychologistId, existingId)).thenReturn(studentReport);

        ResultActions result = mockMvc.perform(get(STUDENT_REPORT_URL, psychologistId, existingId));

        result.andExpect(status().isOk());
        result.andExpect(content().contentType(ReportFile.XLSX_MEDIA_TYPE));
        result.andExpect(content().bytes(content));
        result.andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, content.length));
        result.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"relatorio-aluno-ana-souza-2026-10-08.xlsx\""));
    }

    @Test
    void downloadStudentReportShouldReturnNotFoundWhenStudentOrPsychologistDoesNotExist() throws Exception {
        when(service.generateStudentReport(psychologistId, nonExistingId)).thenThrow(ResourceNotFoundException.class);

        ResultActions result = mockMvc.perform(get(STUDENT_REPORT_URL, psychologistId, nonExistingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
        result.andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void downloadStudentReportShouldReturnForbiddenWhenStudentBelongsToAnotherInstitution() throws Exception {
        when(service.generateStudentReport(psychologistId, otherInstitutionId))
                .thenThrow(new ForbiddenException("Acesso negado: o aluno não pertence à instituição da psicóloga."));

        ResultActions result = mockMvc.perform(get(STUDENT_REPORT_URL, psychologistId, otherInstitutionId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
        result.andExpect(jsonPath("$.error").value("Acesso negado: o aluno não pertence à instituição da psicóloga."));
    }

    @Test
    void downloadStudentReportShouldReturnForbiddenWhenLoggedUserIsAnotherPsychologist() throws Exception {
        when(service.generateStudentReport(otherPsychologistId, existingId)).thenThrow(ForbiddenException.class);

        ResultActions result = mockMvc.perform(get(STUDENT_REPORT_URL, otherPsychologistId, existingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
    }

    @Test
    void downloadStudentReportShouldReturnUnprocessableContentWhenPsychologistHasNoInstitution() throws Exception {
        when(service.generateStudentReport(psychologistWithoutInstitutionId, existingId)).thenThrow(BusinessException.class);

        ResultActions result = mockMvc.perform(get(STUDENT_REPORT_URL, psychologistWithoutInstitutionId, existingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableContent());
    }
}
