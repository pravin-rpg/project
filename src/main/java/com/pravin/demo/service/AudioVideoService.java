package com.pravin.demo.service;

import com.pravin.demo.model.AudioVideoUploadResponse;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AudioVideoService {

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final VectorStore vectorStore;
    private final Path uploadDir;

    public AudioVideoService(OpenAiAudioTranscriptionModel transcriptionModel,
                             VectorStore vectorStore,
                             @Value("${app.upload.dir:uploads}") String uploadPath) {
        this.transcriptionModel = transcriptionModel;
        this.vectorStore = vectorStore;
        this.uploadDir = Paths.get(System.getProperty("user.dir"), uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            System.out.println("✅ Media upload directory: " + uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public AudioVideoUploadResponse processAudioVideo(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("audio/") && !contentType.startsWith("video/"))) {
            throw new IllegalArgumentException("Only audio and video files are allowed");
        }

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(filename);
        file.transferTo(filePath.toFile());

        // Whisper transcription
        FileSystemResource resource = new FileSystemResource(filePath);
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);

        String fullTranscription = response.getResult().getOutput();

        // Parse SRT blocks and store in vector DB
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();
        String[] blocks = fullTranscription.split("\\R\\s*\\R");
        
        for (String block : blocks) {
            String[] lines = block.split("\\R");
            if (lines.length >= 3) {
                String times = lines[1];
                String[] timeParts = times.split(" --> ");
                if (timeParts.length == 2) {
                    String startTime = timeParts[0];
                    String endTime = timeParts[1];

                    StringBuilder textBuilder = new StringBuilder();
                    for (int i = 2; i < lines.length; i++) {
                        textBuilder.append(lines[i]).append(" ");
                    }
                    String textContent = textBuilder.toString().trim();
                    
                    // Prepend timestamp to content so the LLM explicitly sees it!
                    String contentWithTime = "[" + startTime + " to " + endTime + "] " + textContent;

                    org.springframework.ai.document.Document doc = new org.springframework.ai.document.Document(
                            contentWithTime,
                            Map.of(
                                    "filename", filename,
                                    "originalName", file.getOriginalFilename(),
                                    "type", "media",
                                    "startTime", startTime,
                                    "endTime", endTime
                            )
                    );
                    documents.add(doc);
                }
            }
        }
        
        if (documents.isEmpty()) {
            // Fallback if SRT parsing fails for any reason
            org.springframework.ai.document.Document doc = new org.springframework.ai.document.Document(
                    fullTranscription,
                    Map.of("filename", filename, "originalName", file.getOriginalFilename(), "type", "media")
            );
            documents.add(doc);
        }

        vectorStore.add(documents);

        return new AudioVideoUploadResponse(
                "Audio/Video uploaded and transcribed successfully!",
                file.getOriginalFilename(),
                fullTranscription
        );
    }
}