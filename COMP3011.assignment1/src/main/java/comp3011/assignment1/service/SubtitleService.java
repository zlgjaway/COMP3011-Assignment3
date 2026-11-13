
package comp3011.assignment1.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.controller.GlobalStats;

@Service
public class SubtitleService {

    private final RestClient restClient;
    private final GlobalStats globalStats;
    private final String apiUrl;
    private final String apiKey;

    public SubtitleService(
            GlobalStats globalStats,
            @Value("${subtitle.api.url}") String apiUrl,
            @Value("${subtitle.api.key:}") String apiKey) {

        this.restClient = RestClient.create();
        this.globalStats = globalStats;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public SubtitleResponse transcribe(MultipartFile mediaFile)
            throws IOException {

        if (mediaFile == null || mediaFile.isEmpty()) {
            throw new IllegalArgumentException("Media file is empty.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured.");
        }

        String filename = mediaFile.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Uploaded media must have a filename and extension.");
        }

        // Prepare the uploaded media as a multipart file part.
        ByteArrayResource fileResource =
                new ByteArrayResource(mediaFile.getBytes()) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                };

        HttpHeaders fileHeaders = new HttpHeaders();

        if (mediaFile.getContentType() != null) {
            fileHeaders.setContentType(
                    MediaType.parseMediaType(mediaFile.getContentType()));
        } else {
            fileHeaders.setContentType(
                    MediaType.APPLICATION_OCTET_STREAM);
        }

        HttpEntity<ByteArrayResource> filePart =
                new HttpEntity<>(fileResource, fileHeaders);

        // Build the multipart request required by Whisper.
        MultiValueMap<String, Object> requestBody =
                new LinkedMultiValueMap<>();

        requestBody.add("file", filePart);
        requestBody.add("model", "whisper-1");
        requestBody.add("response_format", "verbose_json");
        requestBody.add("timestamp_granularities[]", "word");
        requestBody.add("timestamp_granularities[]", "segment");

        // Send the request to OpenAI.
        SubtitleResponse response = restClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(requestBody)
                .retrieve()
                .body(SubtitleResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Whisper returned an empty response.");
        }

        // Discard a segment only when BOTH conditions are true.
        if (response.getSegments() != null) {
            List<SubtitleResponse.Segment> filteredSegments =
                    response.getSegments().stream()
                            .filter(segment ->
                                    !(segment.getNoSpeechProb() > 0.6
                                    && segment.getAvgLogprob() < -1.0))
                            .toList();

            response.setSegments(filteredSegments);
        }

        // Record the duration only after a successful transcription.
        globalStats.addAudioSeconds(response.getDuration());

        return response;
    }
}
