package com.pravin.demo.controller;

import com.pravin.demo.model.AudioVideoUploadResponse;
import com.pravin.demo.model.SummaryResponse;
import com.pravin.demo.service.AudioVideoService;
import com.pravin.demo.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MediaController {

    private final AudioVideoService audioVideoService;
    private final ChatService chatService;

    public MediaController(AudioVideoService audioVideoService, ChatService chatService) {
        this.audioVideoService = audioVideoService;
        this.chatService = chatService;
    }

    @PostMapping("/upload/media")
    public ResponseEntity<AudioVideoUploadResponse> uploadMedia(@RequestParam("file") MultipartFile file) {
        try {
            AudioVideoUploadResponse response = audioVideoService.processAudioVideo(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new AudioVideoUploadResponse("Upload failed: " + e.getMessage(), null, null)
            );
        }
    }

    @PostMapping("/summary")
    public SummaryResponse getSummary(@RequestParam String filename) {
        String prompt = "Provide a concise summary of the uploaded media file: " + filename;
        String summary = chatService.chat(prompt);
        return new SummaryResponse(summary);
    }
}