package com.custombond.controller;

import com.custombond.dto.request.VendorIssuanceRequest;
import com.custombond.dto.response.VendorIssuanceAcknowledgement;
import com.custombond.pipeline.PipelineStepType;
import com.custombond.service.VendorPipelineAsyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link VendorController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer (controller + MVC configuration).
 * All service dependencies are replaced with Mockito mocks so no external calls are made.
 *
 * <p><strong>How to run:</strong>
 * <pre>
 *   mvn test -Dtest=VendorControllerTest
 * </pre>
 */
@WebMvcTest(VendorController.class)
@DisplayName("VendorController – /vendor/issue endpoint tests")
class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VendorPipelineAsyncService vendorPipelineAsyncService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Default stub – async pipeline always returns immediately
        when(vendorPipelineAsyncService.runPipeline(any(), anyString(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    // -----------------------------------------------------------------------
    // Happy-path tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /vendor/issue – full pipeline without file returns 202 with requestId")
    void issue_fullPipelineNoFile_returns202WithRequestId() throws Exception {
        VendorIssuanceRequest request = buildMinimalRequest(
                List.of(PipelineStepType.ISSUE_QUOTE, PipelineStepType.ISSUE_POLICY));
        request.setPolicyNo("POL-001");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        MvcResult result = mockMvc.perform(multipart("/vendor/issue").file(dataPart))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Request accepted. Processing pipeline asynchronously."))
                .andExpect(jsonPath("$.steps").isArray())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        VendorIssuanceAcknowledgement ack = objectMapper.readValue(
                responseJson, VendorIssuanceAcknowledgement.class);

        assertThat(ack.getRequestId()).isNotBlank();
        assertThat(ack.getVendorRequestId()).isEqualTo("REQ-TEST-001");
        assertThat(ack.getSteps()).containsExactly(
                PipelineStepType.ISSUE_QUOTE, PipelineStepType.ISSUE_POLICY);
    }

    @Test
    @DisplayName("POST /vendor/issue – pipeline with file part delegates file bytes to async service")
    void issue_withFile_fileBytesDelegatedToAsyncService() throws Exception {
        VendorIssuanceRequest request = buildMinimalRequest(
                List.of(PipelineStepType.UPLOAD_DOCUMENT));

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        byte[] fileContent = "PDF_CONTENT".getBytes();
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf", fileContent);

        mockMvc.perform(multipart("/vendor/issue").file(dataPart).file(filePart))
                .andExpect(status().isAccepted());

        // Verify the async service was called with the correct file bytes
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(vendorPipelineAsyncService).runPipeline(
                any(), anyString(), bytesCaptor.capture(), nameCaptor.capture(), any());

        assertThat(bytesCaptor.getValue()).isEqualTo(fileContent);
        assertThat(nameCaptor.getValue()).isEqualTo("contract.pdf");
    }

    @Test
    @DisplayName("POST /vendor/issue – each call generates a unique requestId")
    void issue_multipleRequests_eachHasUniqueRequestId() throws Exception {
        VendorIssuanceRequest request = buildMinimalRequest(
                List.of(PipelineStepType.ISSUE_POLICY));
        request.setPolicyNo("POL-002");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        MvcResult first = mockMvc.perform(multipart("/vendor/issue")
                        .file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(request))))
                .andExpect(status().isAccepted())
                .andReturn();

        MvcResult second = mockMvc.perform(multipart("/vendor/issue")
                        .file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(request))))
                .andExpect(status().isAccepted())
                .andReturn();

        String id1 = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("requestId").asText();
        String id2 = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("requestId").asText();

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("POST /vendor/issue – async service is called exactly once per request")
    void issue_asyncServiceCalledOnce() throws Exception {
        VendorIssuanceRequest request = buildMinimalRequest(
                List.of(PipelineStepType.ISSUE_QUOTE));
        request.setPolicyNo("POL-003");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/vendor/issue").file(dataPart))
                .andExpect(status().isAccepted());

        verify(vendorPipelineAsyncService, times(1))
                .runPipeline(any(), anyString(), isNull(), isNull(), isNull());
    }

    // -----------------------------------------------------------------------
    // Validation / error tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /vendor/issue – missing vendorId returns 400")
    void issue_missingVendorId_returns400() throws Exception {
        VendorIssuanceRequest request = VendorIssuanceRequest.builder()
                // vendorId intentionally omitted
                .steps(List.of(PipelineStepType.ISSUE_POLICY))
                .policyNo("POL-004")
                .build();

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/vendor/issue").file(dataPart))
                .andExpect(status().isBadRequest());

        verify(vendorPipelineAsyncService, never())
                .runPipeline(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /vendor/issue – empty steps list returns 400")
    void issue_emptyStepsList_returns400() throws Exception {
        VendorIssuanceRequest request = VendorIssuanceRequest.builder()
                .vendorId("V001")
                .steps(List.of())     // violates @NotEmpty
                .build();

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/vendor/issue").file(dataPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /vendor/issue – missing data part returns 400")
    void issue_missingDataPart_returns400() throws Exception {
        mockMvc.perform(multipart("/vendor/issue"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a minimal valid {@link VendorIssuanceRequest} for the given steps.
     */
    private VendorIssuanceRequest buildMinimalRequest(List<PipelineStepType> steps) {
        return VendorIssuanceRequest.builder()
                .vendorId("V001")
                .vendorRequestId("REQ-TEST-001")
                .steps(steps)
                .build();
    }
}
