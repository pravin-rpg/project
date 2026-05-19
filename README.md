# OmniStream AI

An AI-powered RAG (Retrieval-Augmented Generation) system that lets you upload **PDFs, audio, and video files** and then ask natural language questions about their content. For media files, answers include clickable timestamps that jump directly to the relevant moment in the player.

---

## What It Does

1. **Upload a PDF** → the document is parsed, chunked, and stored as vector embeddings in PostgreSQL (pgvector).
2. **Upload an audio or video file** → the file is transcribed via OpenAI Whisper, split into timestamped SRT blocks, and each block is stored as a vector embedding.
3. **Ask questions in the chat** → Spring AI retrieves the most relevant chunks from the vector store and passes them to GPT-4o-mini to generate a grounded answer.
4. **Timestamp links** → when the AI references a moment in a media file, the timestamp appears as a clickable link that seeks the built-in media player to that exact time.
5. **Auto-summary** → after every upload, the app automatically generates an actionable summary of the content.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3.4, Spring AI 1.0.0 |
| LLM | OpenAI GPT-4o-mini |
| Embeddings | OpenAI `text-embedding-3-small` |
| Transcription | OpenAI Whisper (`whisper-1`) |
| Vector Store | PostgreSQL + pgvector (HNSW index, cosine distance, 1536 dims) |
| PDF Parsing | Spring AI PDF Document Reader + Apache Tika |
| Frontend | React 19, Vite 8 |
| Containerization | Docker / Docker Compose |

---

## Project Structure

```
demo/
├── src/main/java/com/pravin/demo/
│   ├── controller/
│   │   ├── ChatController.java       # POST /api/chat
│   │   ├── UploadController.java     # POST /api/upload  (PDF)
│   │   └── MediaController.java     # POST /api/upload/media, POST /api/summary
│   ├── service/
│   │   ├── ChatService.java          # RAG Q&A via QuestionAnswerAdvisor
│   │   ├── DocumentService.java      # PDF ingestion → vector store
│   │   └── AudioVideoService.java   # Whisper transcription → vector store
│   └── model/                        # Request/response records
├── frontend/                         # React + Vite UI
│   └── src/App.jsx                   # Single-page chat + media player
├── compose.yaml                      # Docker Compose (app + pgvector)
├── Dockerfile                        # Backend container
└── application.properties            # Config (port, OpenAI, DB)
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/upload` | Upload and index a PDF file |
| `POST` | `/api/upload/media` | Upload and transcribe an audio/video file |
| `POST` | `/api/summary?filename=` | Generate a summary for an uploaded file |
| `POST` | `/api/chat` | Ask a question; returns a RAG-grounded answer |

---

## Prerequisites

- Java 21+
- Node.js 18+ (for the frontend)
- Docker (for PostgreSQL + pgvector)
- An OpenAI API key

---

## Getting Started

### 1. Start the database

```bash
docker compose up -d
```

This spins up a PostgreSQL instance with the pgvector extension on port `15432`. The schema (`vector_store` table) is created automatically on first boot.

### 2. Configure your OpenAI key

Set the environment variable before starting the backend:

```bash
# Windows CMD
set OPENAI_API_KEY=sk-...

# PowerShell
$env:OPENAI_API_KEY="sk-..."
```

Or update `src/main/resources/application.properties` directly:

```properties
spring.ai.openai.api-key=sk-your-key-here
```

### 3. Run the backend

```bash
./mvnw spring-boot:run
```

The API starts on **http://localhost:8081**.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The UI starts on **http://localhost:5173** and proxies API calls to the backend.

---

## How the RAG Pipeline Works

```
User uploads file
       │
       ▼
  PDF? ──► PagePdfDocumentReader ──► TokenTextSplitter ──► VectorStore (pgvector)
       │
  Audio/Video? ──► Whisper API (SRT output) ──► SRT block parser ──► VectorStore
                                                  (each block tagged with start/end time)
       │
User asks question
       │
       ▼
QuestionAnswerAdvisor retrieves top-k similar chunks from VectorStore
       │
       ▼
GPT-4o-mini generates answer grounded in retrieved context
       │
       ▼
Frontend renders answer; timestamps become clickable seek links
```

---

## Configuration Reference

Key settings in `application.properties`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8081` | Backend port |
| `spring.ai.openai.chat.options.model` | `gpt-4o-mini` | Chat model |
| `spring.ai.openai.embedding.options.model` | `text-embedding-3-small` | Embedding model |
| `spring.ai.openai.audio.transcription.options.model` | `whisper-1` | Transcription model |
| `spring.datasource.url` | `jdbc:postgresql://localhost:15432/documentqa` | DB connection |
| `app.upload.dir` | `uploads` | Local file storage path |
| `spring.ai.vectorstore.pgvector.dimensions` | `1536` | Must match embedding model output |

---

## Supported File Types

- **Documents:** PDF
- **Audio:** MP3, WAV, MP4 audio, and other formats supported by Whisper
- **Video:** MP4, MOV, and other common video formats
