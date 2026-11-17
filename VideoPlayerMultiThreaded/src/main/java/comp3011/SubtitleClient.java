
package comp3011;

import java.io.File;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SubtitleClient {

    public record Subtitle(
            double start,
            double end,
            String text) {
    }
    
    private static final int MAX_CHARS = 42;
    private static final int MAX_LINES = 2;
    private static final double MAX_CUE_DURATION = 3.0;
    private static final double MAX_WORD_GAP = 0.35;


	private List<Subtitle> splitWords(JsonNode words) {
	    List<Subtitle> result = new ArrayList<>();
	
	    StringBuilder text = new StringBuilder();
	
	    double cueStart = 0;
	    double cueEnd = 0;
	
	    int lineLength = 0;
	    int lineCount = 1;
	
	    for (JsonNode wordNode : words) {
	        String word = wordNode.path("word").asText().trim();
	        double start = wordNode.path("start").asDouble();
	        double end = wordNode.path("end").asDouble();
	
	        if (word.isEmpty() || end <= start) {
	            continue;
	        }
	
	        if (text.length() > 0) {
	            double gap = start - cueEnd;
	            double duration = end - cueStart;
	
	            boolean longPause = gap > MAX_WORD_GAP;
	            boolean tooLong = duration > MAX_CUE_DURATION;
	
	            boolean sentenceEnd =
	                    text.toString().matches("(?s).*[.!?]$");
	
	            boolean needsNewLine =
	                    lineLength + 1 + word.length() > MAX_CHARS;
	
	            boolean tooManyLines =
	                    needsNewLine && lineCount >= MAX_LINES;
	
	            if (longPause || tooLong || tooManyLines
	                    || (sentenceEnd && gap > 0.10)) {
	
	                result.add(new Subtitle(
	                        cueStart, cueEnd, text.toString()));
	
	                text.setLength(0);
	                lineLength = 0;
	                lineCount = 1;
	
	            } else if (needsNewLine) {
	                text.append("\n");
	                lineLength = 0;
	                lineCount++;
	            }
	        }
	
	        if (text.length() == 0) {
	            cueStart = start;
	        } else if (lineLength > 0) {
	            text.append(" ");
	            lineLength++;
	        }
	
	        text.append(word);
	        lineLength += word.length();
	        cueEnd = end;
	    }
	
	    if (text.length() > 0) {
	        result.add(new Subtitle(
	                cueStart, cueEnd, text.toString()));
	    }
	
	    return result;
	}

    

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    private Path extractAudio(File mediaFile) throws Exception {
        Path audioFile = Files.createTempFile("subtitle-audio-", ".mp3");

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "ffmpeg",
                    "-nostdin",
                    "-y",
                    "-i", mediaFile.getAbsolutePath(),
                    "-vn",
                    "-ac", "1",
                    "-ar", "16000",
                    "-b:a", "64k",
                    audioFile.toString()
            );

            builder.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = builder.start();

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("FFmpeg audio extraction timed out");
            }

            if (process.exitValue() != 0) {
                throw new IOException(
                        "FFmpeg failed with exit code " + process.exitValue());
            }

            return audioFile;

        } catch (Exception e) {
            Files.deleteIfExists(audioFile);
            throw e;
        }
    }

    public CompletableFuture<List<Subtitle>> fetch(File mediaFile) {

        return CompletableFuture.supplyAsync(() -> {
            Path audioFile = null;

            try {
                audioFile = extractAudio(mediaFile);

                String boundary = "Boundary" + UUID.randomUUID();

                byte[] fileBytes = Files.readAllBytes(audioFile);

                String header =
                        "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; "
                        + "name=\"media\"; filename=\"audio.mp3\"\r\n"
                        + "Content-Type: audio/mpeg\r\n\r\n";

                String footer = "\r\n--" + boundary + "--\r\n";

                byte[] headerBytes =
                        header.getBytes(StandardCharsets.UTF_8);

                byte[] footerBytes =
                        footer.getBytes(StandardCharsets.UTF_8);

                byte[] body = new byte[
                        headerBytes.length
                        + fileBytes.length
                        + footerBytes.length];

                System.arraycopy(
                        headerBytes, 0, body, 0, headerBytes.length);

                System.arraycopy(
                        fileBytes, 0, body,
                        headerBytes.length, fileBytes.length);

                System.arraycopy(
                        footerBytes, 0, body,
                        headerBytes.length + fileBytes.length,
                        footerBytes.length);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/v1/subtitles"))
                        .header(
                                "Content-Type",
                                "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException(
                            "Subtitle API returned HTTP "
                            + response.statusCode()
                            + ": " + response.body());
                }

                JsonNode json = mapper.readTree(response.body());
                List<Subtitle> subtitles = new ArrayList<>();

                JsonNode words = json.path("words");

                if (words.isArray() && !words.isEmpty()) {
                    subtitles.addAll(splitWords(words));
                } else {
                    for (JsonNode segment : json.path("segments")) {
                        double start = segment.path("start").asDouble();
                        double end = segment.path("end").asDouble();
                        String text = segment.path("text").asText();

                        if (end > start && !text.isBlank()) {
                            subtitles.add(
                                    new Subtitle(start, end, text.trim()));
                        }
                    }
                }

                return List.copyOf(subtitles);

            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to retrieve subtitles", e);

            } finally {
                if (audioFile != null) {
                    try {
                        Files.deleteIfExists(audioFile);
                    } catch (IOException e) {
                        System.err.println(
                                "Unable to delete temporary audio: "
                                + e.getMessage());
                    }
                }
            }
        });
    }
}
