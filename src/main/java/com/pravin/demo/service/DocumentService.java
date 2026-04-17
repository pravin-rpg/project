package com.pravin.demo.service;

import com.pravin.demo.model.UploadResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class DocumentService {

    private final VectorStore vectorStore;
    private final Path uploadDir;

    public DocumentService(VectorStore vectorStore, @Value("${app.upload.dir:uploads}") String uploadPath) {
        this.vectorStore = vectorStore;
        this.uploadDir = Paths.get(System.getProperty("user.dir"), uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            System.out.println("✅ Upload directory: " + uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    public UploadResponse ingestPdf(MultipartFile file) {
        try {
            if (file.isEmpty() || !"application/pdf".equals(file.getContentType())) {
                return new UploadResponse("Only PDF files are allowed", null);
            }

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(filename);

            // Save file
            file.transferTo(filePath.toFile());
            System.out.println("✅ File saved: " + filePath);

            // Read PDF
            FileSystemResource resource = new FileSystemResource(filePath);
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            List<Document> documents = pdfReader.get();

            // Split into chunks
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocuments = splitter.split(documents);

            // Add metadata
            splitDocuments.forEach(doc -> {
                doc.getMetadata().put("filename", filename);
                doc.getMetadata().put("originalName", file.getOriginalFilename());
            });

            // Store in vector DB
            vectorStore.add(splitDocuments);
            System.out.println("✅ PDF successfully indexed with " + splitDocuments.size() + " chunks");

            return new UploadResponse("PDF uploaded and indexed successfully!", file.getOriginalFilename());

        } catch (Exception e) {
            e.printStackTrace();  // This will show full error in console
            return new UploadResponse("Upload failed: " + e.getClass().getSimpleName() + " - " + e.getMessage(), null);
        }
    }
}