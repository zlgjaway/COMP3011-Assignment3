
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SubtitleClient {

    public record Subtitle(
            double start,
            double end,
            String text) {
    }

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public CompletableFuture<List<Subtitle>> fetch(File mediaFile) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String boundary = "Boundary" + UUID.randomUUID();

                byte[] fileBytes = Files.readAllBytes(mediaFile.toPath());

                String header =
                        "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; "
                        + "name=\"media\"; filename=\""
                        + mediaFile.getName() + "\"\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n";

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

                for (JsonNode segment : json.path("segments")) {

                    double start = segment.path("start").asDouble();
                    double end = segment.path("end").asDouble();
                    String text = segment.path("text").asText();

                    if (end > start && !text.isBlank()) {
                        subtitles.add(
                                new Subtitle(start, end, text.trim()));
                    }
                }

                return List.copyOf(subtitles);

            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to retrieve subtitles", e);
            }
        });
    }
}
