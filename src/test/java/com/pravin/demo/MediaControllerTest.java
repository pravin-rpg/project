package com.pravin.demo;

import com.pravin.demo.controller.MediaController;
import com.pravin.demo.model.AudioVideoUploadResponse;
import com.pravin.demo.service.AudioVideoService;
import com.pravin.demo.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AudioVideoService audioVideoService;

    @MockBean
    private ChatService chatService;

    @Test
    void shouldUploadMediaSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "dummy content".getBytes());
        AudioVideoUploadResponse mockResponse = new AudioVideoUploadResponse("Success", "test.mp4", "transcript");
        when(audioVideoService.processAudioVideo(any())).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/upload/media").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.filename").value("test.mp4"));
    }

    @Test
    void shouldReturnSummary() throws Exception {
        when(chatService.chat(anyString())).thenReturn("This is a summary.");

        mockMvc.perform(post("/api/summary").param("filename", "test.mp4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("This is a summary."));
    }
}
