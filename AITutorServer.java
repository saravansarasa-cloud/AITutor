import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AITutorServer {

    // API key loaded from .env file or environment variable
    private static final String API_KEY = loadApiKey();

    // Allowed subjects with keywords for better matching
    private static final Map<String, String[]> ALLOWED_SUBJECTS = new HashMap<>();

    static {
        ALLOWED_SUBJECTS.put("Java", new String[] { "java", "jvm", "spring", "servlet" });
        ALLOWED_SUBJECTS.put("C++", new String[] { "c++", "cpp", "c plus" });
        ALLOWED_SUBJECTS.put("Data Structures",
                new String[] { "data structure", "array", "linked list", "tree", "graph", "stack", "queue", "heap" });
        ALLOWED_SUBJECTS.put("Operating Systems",
                new String[] { "operating system", "os", "process", "thread", "memory management", "scheduling" });
        ALLOWED_SUBJECTS.put("DBMS",
                new String[] { "dbms", "database", "sql", "query", "normalization", "transaction" });
        ALLOWED_SUBJECTS.put("Networks", new String[] { "network", "tcp", "ip", "http", "osi", "protocol" });
    }

    /**
     * Loads API key from .env file or environment variable
     */
    private static String loadApiKey() {
        // First try to read from .env file
        try {
            File envFile = new File(".env");
            if (envFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(envFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("GEMINI_API_KEY=")) {
                        String key = line.substring(15).trim();
                        // Remove quotes if present
                        if (key.startsWith("\"") && key.endsWith("\"")) {
                            key = key.substring(1, key.length() - 1);
                        }
                        reader.close();
                        System.out.println("✅ API key loaded from .env file");
                        return key;
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            System.err.println("⚠️ Could not read .env file: " + e.getMessage());
        }

        // Fallback to environment variable
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.isEmpty()) {
            System.out.println("✅ API key loaded from environment variable");
            return key;
        }

        return null;
    }

    public static void main(String[] args) throws IOException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("❌ ERROR: GEMINI_API_KEY not found!");
            System.err.println("Please create a .env file with:");
            System.err.println("GEMINI_API_KEY=your-api-key-here");
            System.err.println("\nGet your free API key at: https://aistudio.google.com/app/apikey");
            return;
        }

        // Try multiple ports until we find one that's available
        int[] portsToTry = { 8080, 8081, 8082, 9090, 9091, 3000, 5000 };
        HttpServer server = null;
        int usedPort = -1;

        for (int port : portsToTry) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                usedPort = port;
                break; // Success! Exit the loop
            } catch (java.net.BindException e) {
                System.out.println("⚠️ Port " + port + " is busy, trying next...");
            }
        }

        if (server == null) {
            System.err.println("❌ ERROR: All ports are in use! Please close some applications.");
            return;
        }

        System.out.println("🚀 AI Tutor backend running at http://localhost:" + usedPort + "/");
        System.out.println("📝 Update your index.html to use port " + usedPort);
        System.out.println("🤖 Using Google Gemini API (Free Tier: 60 requests/min)");

        server.createContext("/ask", AITutorServer::handleRequest);
        server.setExecutor(null);
        server.start();
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        System.out.println("\n📨 Request received: " + exchange.getRequestMethod());

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            System.out.println("✅ Handling OPTIONS (CORS preflight)");
            sendCORS(exchange);
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            System.out.println("❌ Wrong method: " + exchange.getRequestMethod());
            sendResponse(exchange, 405, "ERROR: Only POST method supported");
            return;
        }

        // Read the raw message from request body
        InputStream input = exchange.getRequestBody();
        String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("📥 Received body: " + body);

        String userMessage = extractMessageSimple(body);
        System.out.println("📝 Extracted message: '" + userMessage + "'");

        if (userMessage.isEmpty()) {
            System.out.println("❌ Empty message received");
            sendResponse(exchange, 400, "ERROR: Empty message");
            return;
        }

        // Check if question is about allowed subjects
        String detectedSubject = detectSubject(userMessage);

        String reply;
        if (detectedSubject != null) {
            System.out.println("✅ Allowed subject detected: " + detectedSubject);
            System.out.println("🤖 Calling Gemini API...");
            reply = fetchAIResponse(userMessage, detectedSubject);
            System.out.println("✅ AI Response received: " + reply.substring(0, Math.min(50, reply.length())) + "...");
        } else {
            System.out.println("❌ Question outside allowed subjects");
            reply = "❌ Sorry, I can only answer questions about: Java, C++, Data Structures, Operating Systems, DBMS, and Networks. Please ask about one of these topics.";
        }

        sendCORS(exchange);
        sendResponse(exchange, 200, reply);
        System.out.println("✅ Response sent successfully\n");
    }

    /**
     * Simple extraction without JSON library - finds text between quotes
     */
    private static String extractMessageSimple(String body) {
        try {
            // Look for "message":"..." pattern
            int messageStart = body.indexOf("\"message\"");
            if (messageStart == -1)
                return "";

            int firstQuote = body.indexOf("\"", messageStart + 9);
            if (firstQuote == -1)
                return "";

            int secondQuote = body.indexOf("\"", firstQuote + 1);
            if (secondQuote == -1)
                return "";

            return body.substring(firstQuote + 1, secondQuote);
        } catch (Exception e) {
            System.err.println("Error extracting message: " + e.getMessage());
            return "";
        }
    }

    /**
     * Detects if the user message is about an allowed subject
     * Returns the subject name if found, null otherwise
     */
    private static String detectSubject(String message) {
        String lowerMessage = message.toLowerCase();

        for (Map.Entry<String, String[]> entry : ALLOWED_SUBJECTS.entrySet()) {
            String subject = entry.getKey();
            String[] keywords = entry.getValue();

            for (String keyword : keywords) {
                if (lowerMessage.contains(keyword)) {
                    return subject;
                }
            }
        }
        return null;
    }

    /**
     * Fetches response from Gemini API with subject context
     */
    private static String fetchAIResponse(String userQuestion, String subject) {
        try {
            // CORRECT Gemini API endpoint - v1beta with gemini-pro model
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                    + API_KEY;

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Build the prompt with subject context
            String systemPrompt = "You are an AI tutor specializing in " + subject +
                    ". Provide clear, educational explanations. Keep responses concise and helpful.";

            String fullPrompt = systemPrompt + "\n\nQuestion: " + userQuestion;

            // Build Gemini-specific JSON payload
            String payload = buildGeminiPayload(fullPrompt);

            System.out.println("📤 Sending to Gemini: " + apiUrl);
            System.out.println("📦 Payload: " + payload.substring(0, Math.min(100, payload.length())) + "...");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            System.out.println("📊 Response Code: " + responseCode);

            if (responseCode == 429) {
                return "⚠️ Rate limit exceeded (60 requests/min). Please wait a moment and try again.";
            } else if (responseCode == 400) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                System.err.println("❌ 400 Error details: " + errorResponse.toString());
                return "⚠️ Invalid request format. Error: " + errorResponse.toString();
            } else if (responseCode == 403) {
                return "⚠️ API key invalid. Get a new key at https://aistudio.google.com/app/apikey";
            } else if (responseCode == 404) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                System.err.println("❌ 404 Error details: " + errorResponse.toString());
                return "⚠️ Model not found. Error: " + errorResponse.toString();
            } else if (responseCode != 200) {
                return "⚠️ API Error: " + responseCode;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            System.out.println(
                    "📨 Raw response: " + response.toString().substring(0, Math.min(200, response.length())) + "...");

            return extractGeminiMessage(response.toString());

        } catch (Exception e) {
            System.err.println("❌ Error fetching AI response: " + e.getMessage());
            e.printStackTrace();
            return "⚠️ Error: " + e.getMessage();
        }
    }

    /**
     * Builds JSON payload for Gemini API
     * FIXED: Increased maxOutputTokens from 500 to 2048
     */
    private static String buildGeminiPayload(String prompt) {
        String escapedPrompt = escapeJson(prompt);

        return "{" +
                "\"contents\":[{" +
                "\"parts\":[{" +
                "\"text\":\"" + escapedPrompt + "\"" +
                "}]" +
                "}]," +
                "\"generationConfig\":{" +
                "\"temperature\":0.7," +
                "\"maxOutputTokens\":2048," +
                "\"topP\":0.95," +
                "\"topK\":40" +
                "}" +
                "}";
    }

    /**
     * Escapes special characters for JSON
     */
    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Extracts AI message from Gemini response
     * COMPLETELY REWRITTEN with better error handling
     */
    private static String extractGeminiMessage(String response) {
        try {
            System.out.println("📦 FULL RESPONSE:");
            System.out.println(response);
            System.out.println("📦 END RESPONSE\n");

            // Check for empty response
            if (response == null || response.trim().isEmpty()) {
                return "⚠️ Received empty response from API";
            }

            // Check for MAX_TOKENS error
            if (response.contains("\"finishReason\":\"MAX_TOKENS\"") ||
                    response.contains("\"finishReason\": \"MAX_TOKENS\"")) {
                System.err.println("❌ Response truncated due to MAX_TOKENS");
                return "⚠️ Response was too long and got cut off. Please ask a more specific question.";
            }

            // Check for other finish reasons that indicate problems
            if (response.contains("\"finishReason\":\"SAFETY\"")) {
                return "⚠️ Response blocked due to safety filters. Please rephrase your question.";
            }

            if (response.contains("\"finishReason\":\"RECITATION\"")) {
                return "⚠️ Response blocked due to recitation concerns. Please rephrase your question.";
            }

            // Check for API errors
            if (response.contains("\"error\"")) {
                System.err.println("❌ API returned an error");

                // Try to extract error message
                int errorMsgIndex = response.indexOf("\"message\"");
                if (errorMsgIndex != -1) {
                    int colonPos = response.indexOf(":", errorMsgIndex);
                    int startQuote = response.indexOf("\"", colonPos);
                    int endQuote = response.indexOf("\"", startQuote + 1);
                    if (startQuote != -1 && endQuote != -1) {
                        String errorMsg = response.substring(startQuote + 1, endQuote);
                        return "⚠️ API Error: " + errorMsg;
                    }
                }
                return "⚠️ API returned an error. Check console for details.";
            }

            // Look for "text" field in the response
            int textIndex = response.indexOf("\"text\"");

            if (textIndex == -1) {
                // No text field found - check if parts array is empty
                if (response.contains("\"parts\":[]") || response.contains("\"parts\": []")) {
                    return "⚠️ API returned empty content. The response may have been filtered or truncated.";
                }

                System.err.println("❌ Could not find 'text' field in response");
                String snippet = response.length() > 300 ? response.substring(0, 300) + "..." : response;
                return "⚠️ Unexpected response format: " + snippet;
            }

            System.out.println("✅ Found 'text' field at position: " + textIndex);

            // Find the colon after "text"
            int colonIndex = response.indexOf(":", textIndex);
            if (colonIndex == -1) {
                return "⚠️ Malformed JSON response";
            }

            // Skip whitespace and find opening quote
            int quoteIndex = -1;
            for (int i = colonIndex + 1; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '"') {
                    quoteIndex = i;
                    break;
                } else if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    System.err.println("❌ Expected quote but found: " + c);
                    return "⚠️ Malformed JSON response";
                }
            }

            if (quoteIndex == -1) {
                return "⚠️ Malformed JSON response - no opening quote";
            }

            // Extract content between quotes, handling escapes
            StringBuilder content = new StringBuilder();
            boolean escaped = false;

            for (int i = quoteIndex + 1; i < response.length(); i++) {
                char c = response.charAt(i);

                if (escaped) {
                    switch (c) {
                        case 'n':
                            content.append('\n');
                            break;
                        case 'r':
                            content.append('\r');
                            break;
                        case 't':
                            content.append('\t');
                            break;
                        case '\\':
                            content.append('\\');
                            break;
                        case '"':
                            content.append('"');
                            break;
                        case '/':
                            content.append('/');
                            break;
                        case 'b':
                            content.append('\b');
                            break;
                        case 'f':
                            content.append('\f');
                            break;
                        default:
                            content.append(c);
                            break;
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    // Found closing quote
                    String result = content.toString();

                    if (result.trim().isEmpty()) {
                        return "⚠️ API returned empty text content";
                    }

                    System.out.println("✅ Successfully extracted " + result.length() + " characters");
                    System.out.println("✅ Preview: " + result.substring(0, Math.min(100, result.length())));
                    return result;
                } else {
                    content.append(c);
                }
            }

            return "⚠️ Incomplete JSON response - no closing quote found";

        } catch (Exception e) {
            System.err.println("❌ Exception parsing response: " + e.getMessage());
            e.printStackTrace();
            return "⚠️ Error parsing response: " + e.getMessage();
        }
    }

    /**
     * Sends response back to client
     */
    private static void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        // Wrap response in simple format for frontend
        String wrappedResponse = "{\"reply\":\"" + escapeJson(response) + "\"}";
        byte[] bytes = wrappedResponse.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /**
     * Adds CORS headers for browser compatibility
     */
    private static void sendCORS(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
    }
}