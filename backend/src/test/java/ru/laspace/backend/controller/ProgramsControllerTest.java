package ru.laspace.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.laspace.backend.dto.programs.ProgramCreateRequest;
import ru.laspace.backend.security.JwtAuthenticationFilter;
import ru.laspace.backend.security.JwtService;
import ru.laspace.backend.service.pr.Pr01Service;
import ru.laspace.backend.service.pr.Pr03Service;
import ru.laspace.backend.service.pr.Pr04Service;
import ru.laspace.backend.service.programs.ProgramsService;

@WebMvcTest(controllers = ProgramsController.class, excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import({ GlobalExceptionHandler.class, JwtAuthenticationFilter.class })
class ProgramsControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ProgramsService programsService;

        @MockitoBean
        private Pr01Service pr01Service;

        @MockitoBean
        private Pr03Service pr03Service;

        @MockitoBean
        private Pr04Service pr04Service;

        @MockitoBean
        private JwtService jwtService;

        // --- POST /api/programs/create ---

        @Test
        void createProgram_validRequest_returns201() throws Exception {
                when(programsService.saveProgram(any())).thenReturn(5);

                mockMvc.perform(post("/api/programs/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildRequest())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.numRp").value(5));

                verify(programsService).saveProgram(any());
        }

        @Test
        void createProgram_missingMainData_returns400() throws Exception {
                String json = "{\"modes\":[]}";

                mockMvc.perform(post("/api/programs/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createProgram_missingModes_returns400() throws Exception {
                String json = "{\"mainData\":{\"numKa\":1525,\"dateOn\":\"2026-04-30T00:00:00\"," +
                                "\"dateOff\":\"2026-04-30T23:59:00\",\"typeRp\":3}}";

                mockMvc.perform(post("/api/programs/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createProgram_serviceThrows_returns500() throws Exception {
                when(programsService.saveProgram(any()))
                                .thenThrow(new RuntimeException("DB error"));

                mockMvc.perform(post("/api/programs/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildRequest())))
                                .andExpect(status().isInternalServerError());
        }

        // --- POST /api/programs/{numRp}/{numKa}/pr01/generate ---

        @Test
        void generatePr01_returns200WithContent() throws Exception {
                when(pr01Service.generateAndSave(1, 1525)).thenReturn("ПР01:1525,...");

                mockMvc.perform(post("/api/programs/1/1525/pr01/generate"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("ПР01:1525,..."));
        }

        @Test
        void generatePr01_serviceThrows_returns500() throws Exception {
                when(pr01Service.generateAndSave(99, 1525))
                                .thenThrow(new RuntimeException("ПРЦА не найдена"));

                mockMvc.perform(post("/api/programs/99/1525/pr01/generate"))
                                .andExpect(status().isInternalServerError());
        }

        // --- POST /api/programs/{numRp}/{numKa}/pr03/generate ---

        @Test
        void generatePr03_returns200WithContent() throws Exception {
                when(pr03Service.generateAndSave(1, 1525)).thenReturn("ПР03:1525,...");

                mockMvc.perform(post("/api/programs/1/1525/pr03/generate"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("ПР03:1525,..."));
        }

        // --- POST /api/programs/{numRp}/{numKa}/pr04/generate ---

        @Test
        void generatePr04_returns200WithContent() throws Exception {
                when(pr04Service.generateAndSave(1, 1525)).thenReturn("ПР04:1525,...");

                mockMvc.perform(post("/api/programs/1/1525/pr04/generate"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("ПР04:1525,..."));
        }

        // --- вспомогательные методы ---

        private ProgramCreateRequest buildRequest() {
                ProgramCreateRequest.MainData mainData = new ProgramCreateRequest.MainData();
                mainData.setNumKa(1525);
                mainData.setDateOn(LocalDateTime.of(2026, 4, 30, 0, 0));
                mainData.setDateOff(LocalDateTime.of(2026, 4, 30, 23, 59));
                mainData.setTypeRp(3);
                mainData.setPrOtpr(0);

                ProgramCreateRequest request = new ProgramCreateRequest();
                request.setMainData(mainData);
                request.setModes(List.of());
                return request;
        }
}