# AI Chat Assistant Web App

A production-ready, full-stack **AI Chat Assistant** web application built with **Java 17 / Spring Boot 3** and **React (Vite)**, powered by **Google Gemini API** (`gemini-2.5-flash`) via reactive REST streaming (SSE) and persistent storage in **MongoDB**.

---

## 🚀 Key Features

1. **Real-time SSE Streaming**: Full server-to-browser streaming via Server-Sent Events (`Flux<ServerSentEvent<String>>`) with real-time UI text rendering.
2. **Stop Generating**: Immediate stream abort via browser `AbortController` with automatic partial-text persistence on the server.
3. **Tone Toggle ("Vibe Check")**: Dynamically switch between **Professional**, **Casual**, and **Concise** tones. The backend maps the selected tone to Gemini's `systemInstruction`, instantly shifting response styles even mid-conversation.
4. **Auto-Generated Titles**: After the initial exchange, an asynchronous non-streaming Gemini call generates a concise 3-5 word title (with robust fallback to first prompt truncation).
5. **Thread History & Search**: Persistent chat history sidebar listing threads sorted newest first, with server-side title search (`?search=query`).
6. **Markdown & Syntax Highlighting**: Rich markdown rendering for AI responses with code language badges, Prism syntax highlighting, and one-click "Copy code" buttons.
7. **Strict Server-Side Architecture**: All business logic, history assembly, role mapping (`assistant` -> `model`), tone instruction injection, and safety filter block handling reside strictly on the backend.

---

## 🛠️ Tech Stack

- **Backend**:
  - Java 17
  - Spring Boot 3.3 (Spring MVC, Spring WebFlux for reactive `WebClient`, Spring Data MongoDB, Jakarta Validation)
  - Maven
- **Frontend**:
  - React 18 + Vite
  - Plain modern CSS (custom design system, responsive dark theme, zero heavy component libraries)
  - `react-markdown` + `remark-gfm` + `prismjs` + `lucide-react`
- **Database**:
  - MongoDB (Document-based chat threads with embedded message history)
- **AI / LLM**:
  - Google Gemini API (`POST /v1beta/models/{model}:streamGenerateContent?alt=sse` and `POST /v1beta/models/{model}:generateContent`)
  - Authenticated via server-side header `x-goog-api-key` (keys are never exposed to the client)

---

## 🏛️ Architecture Diagram

```
+-----------------------------------------------------------------------------------+
|                                  BROWSER (React + Vite)                           |
|  +------------------+   +--------------------+   +-----------------------------+  |
|  |     Sidebar      |   |    ChatWindow      |   |         ChatInput           |  |
|  | (Search/Threads) |   | (Bubbles/Markdown) |   | (Send / Stop / Auto-resize) |  |
|  +--------+---------+   +---------+----------+   +--------------+--------------+  |
|           |                       |                             |                 |
|           +-----------------------+-----------------------------+                 |
|                                   |                                               |
|                    fetch / ReadableStream (SSE reader)                            |
+-----------------------------------|-----------------------------------------------+
                                    | HTTP / JSON / SSE
                                    v
+-----------------------------------------------------------------------------------+
|                        SPRING BOOT 3 BACKEND (Port 8080)                          |
|                                                                                   |
|  [ConversationController]                                                         |
|     |-- GET    /api/conversations?search=...                                      |
|     |-- POST   /api/conversations                                                 |
|     |-- GET    /api/conversations/{id}                                            |
|     |-- DELETE /api/conversations/{id}                                            |
|     \-- POST   /api/conversations/{id}/messages/stream (Produces text/event-stream)|
|                                                                                   |
|  [ChatStreamService] <-------------------------> [ToneInstructionProvider]         |
|     |  1. Persist user message to Mongo              (Professional/Casual/Concise)|
|     |  2. Assemble chat history (user/model)                                      |
|     |  3. Inject systemInstruction                                                |
|     |  4. Stream tokens via Flux<ServerSentEvent>                                 |
|     |  5. On complete -> save full response & invoke TitleGenerationService       |
|     \  6. On abort -> save partial response to Mongo                              |
|                                                                                   |
|  [GeminiClient] (WebClient)                                                       |
|     |-- POST .../models/{model}:streamGenerateContent?alt=sse                     |
|     \-- POST .../models/{model}:generateContent (Non-streaming for titles)        |
+------------------+----------------------------------------+-----------------------+
                   |                                        |
                   v                                        v
     +--------------------------+             +--------------------------+
     |      MongoDB Database    |             |    Google Gemini API     |
     |   (Collection: chats)    |             |  (v1beta / REST + SSE)   |
     +--------------------------+             +--------------------------+
```

---

## 🔄 Request Flow: From Click to MongoDB

```
1. USER CLICKS 'SEND' (or presses Enter)
   │
   ├─► Frontend appends user message & an empty AI message bubble into local React state.
   ├─► useChatStream hook opens POST /api/conversations/{id}/messages/stream with fetch().
   │
2. BACKEND CONTROLLER & SERVICE
   │
   ├─► ConversationService loads the conversation from MongoDB.
   ├─► Appends user message { role: "user", content: "...", timestamp: Instant.now() }.
   ├─► Saves immediately to MongoDB so user prompt is never lost.
   │
3. PAYLOAD PREPARATION & TONE MAPPING
   │
   ├─► ToneInstructionProvider maps selected Tone enum -> system instruction string.
   ├─► Chat history messages mapped: role "assistant" -> Gemini role "model".
   ├─► GeminiRequest built with systemInstruction and full conversation contents.
   │
4. REACTIVE STREAMING WITH GEMINI
   │
   ├─► GeminiClient sends WebClient POST to Gemini SSE endpoint with header x-goog-api-key.
   ├─► WebClient parses Gemini SSE stream and extracts text from candidates[0].content.parts[0].text.
   ├─► Tokens wrapped in ServerSentEvent.builder(token).event("token").build() and relayed to client.
   │
5. FRONTEND CONSUMPTION
   │
   ├─► ReadableStream reader in useChatStream decodes UTF-8 SSE chunks.
   ├─► Matches "event: token" -> appends token to the live assistant bubble in real time.
   │
6. COMPLETION OR CANCELLATION
   │
   ├─► Normal Completion:
   │     - Backend emits event "done".
   │     - Saves complete accumulated AI response to MongoDB.
   │     - If first exchange, TitleGenerationService asynchronously generates a concise title.
   │     - Frontend updates sidebar thread list.
   │
   └─► User Clicks "Stop Generating":
         - Frontend AbortController aborts the HTTP request.
         - Backend reactive Flux triggers doOnCancel().
         - Backend saves partial accumulated text + " [stopped]" to MongoDB.
```

---

## ⚙️ Environment Variables

Copy `.env.example` to `.env` or configure system environment variables:

| Variable | Description | Default | Required? |
|---|---|---|---|
| `GEMINI_API_KEY` | Google Gemini API Key from AI Studio | _None_ | **Yes** |
| `GEMINI_MODEL` | Gemini Model ID | `gemini-2.5-flash` | No |
| `MONGODB_URI` | MongoDB Connection URI | `mongodb://localhost:27017/chatbox` | Yes (or local default) |
| `PORT` | Backend server port | `8080` | No |
| `CORS_ALLOWED_ORIGINS` | Permitted browser origins | `http://localhost:5173,http://localhost:3000` | No |
| `VITE_API_BASE_URL` | Base API URL for frontend | `""` (Uses Vite proxy) | No |

---

## 🚀 Run Commands

### 1. Prerequisites
- **Java 17+** (`java -version`)
- **Maven 3.8+** (`mvn -version`)
- **Node.js 18+ & npm** (`node -v`, `npm -v`)
- **MongoDB** running locally on port 27017 or a MongoDB Atlas connection URI.

### 2. Backend Setup & Run
```bash
# Navigate to backend folder
cd backend

# Set your Gemini API key (Linux/macOS)
export GEMINI_API_KEY="your-api-key-here"
# OR in Windows PowerShell:
$env:GEMINI_API_KEY="your-api-key-here"

# Build and run Spring Boot
mvn spring-boot:run
```
The backend starts at `http://localhost:8080`.

### 3. Frontend Setup & Run
```bash
# Navigate to frontend folder (in a new terminal)
cd frontend

# Install dependencies (if not already installed)
npm install

# Start Vite development server
npm run dev
```
The frontend is accessible at `http://localhost:5173`.

---

## 📡 API Endpoint Reference

| Method | Endpoint | Request Body | Response / Events | Description |
|---|---|---|---|---|
| `GET` | `/api/conversations?search=` | None | `List<ConversationSummaryResponse>` | List all threads ordered newest first, with optional title search |
| `POST` | `/api/conversations` | `{ "title": "...", "tone": "Professional" }` | `Conversation` (201 Created) | Create a new conversation thread |
| `GET` | `/api/conversations/{id}` | None | `Conversation` (200 OK) | Retrieve complete conversation history |
| `DELETE` | `/api/conversations/{id}` | None | `204 No Content` | Delete a conversation thread |
| `POST` | `/api/conversations/{id}/messages/stream` | `{ "content": "string", "tone": "Professional" }` | `text/event-stream` | Stream AI response tokens via SSE |

### SSE Stream Events (`/messages/stream`)
- `event: token` -> Chunk of text generated by Gemini (`data: <text_chunk>`)
- `event: done` -> Full text generated on completion (`data: <full_text>`)
- `event: error` -> Error description if Gemini or network fails (`data: <error_message>`)

---

## 🧠 Design Decisions & Trade-Offs

1. **`Flux<ServerSentEvent<String>>` vs `SseEmitter`**:
   - `WebClient` natively outputs a reactive `Flux<T>`. Using `Flux<ServerSentEvent<String>>` provides backpressure handling and integrates directly with `doOnCancel()` and `doOnComplete()`, making abort handling robust without thread-blocking.
2. **Jackson JSON Tree Streaming Parser**:
   - Rather than mapping Gemini's polymorphic response schema into rigid POJOs, `GeminiClient` uses `ObjectMapper.readTree()`. This safely parses nested structures (`candidates[0].content.parts[0].text`) and handles safety blocks (`promptFeedback.blockReason`) without throwing `NullPointerException`.
3. **Dual Persistence Model**:
   - The user message is persisted to MongoDB *before* initiating the external Gemini call. If the LLM call fails or times out, the user's prompt is never lost. If the client stops generation mid-stream, `doOnCancel()` immediately saves the partial text with `[stopped]`.
4. **Tone Mapping via Dedicated Provider**:
   - `ToneInstructionProvider` encapsulates tone definitions. Gemini's `systemInstruction` field is dynamic per request, ensuring tone switches take effect on the very next message without modifying past conversation records.
5. **Vite Proxy in Development**:
   - The frontend Vite server proxies `/api` requests to `http://localhost:8080`, preventing CORS friction during local development while allowing custom backend URLs in production.

---

## 🎓 Viva Cheat Sheet (Quick Reference)

- **How does SSE Streaming work here?**
  1. The browser initiates a `POST` request with `Accept: text/event-stream`.
  2. The Spring Boot backend connects reactively to Gemini's SSE endpoint via `WebClient`.
  3. Tokens are parsed and wrapped into Spring `ServerSentEvent` objects with `event: token`.
  4. The frontend reads the stream byte-by-byte via `ReadableStream.getReader()`, parsing double-newline delimited SSE frames and updating state.
- **How does Tone reach Gemini's `systemInstruction`?**
  - The client transmits the active tone (`Professional`, `Casual`, or `Concise`) with every message.
  - The backend `ToneInstructionProvider` converts this enum into explicit system instructions.
  - The instruction is injected into the root `systemInstruction` field of the Gemini JSON request payload.
- **What is the MongoDB Data Model?**
  - Document `conversations`: `{ id, title, tone, createdAt, updatedAt, messages: [ { role, content, timestamp } ] }`.
  - Roles stored: `"user"` and `"assistant"`.
  - When communicating with Gemini, backend maps stored `"assistant"` role to Gemini's expected `"model"` role.
- **How is stream cancellation handled?**
  - Frontend triggers `abortController.abort()`.
  - Backend `Flux` catches cancellation in `doOnCancel()`, logs the event, and persists whatever partial text was generated to MongoDB.
