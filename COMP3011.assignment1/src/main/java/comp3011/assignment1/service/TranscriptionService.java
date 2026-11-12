package comp3011.assignment1.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.controller.GlobalStats;

@Service
public class TranscriptionService {

    private final RestClient restClient;
    private final GlobalStats globalStats;

    @Value("${transcription.url}")
    private String transcriptionUrl;

    @Value("${transcription.api-key:}")
    private String apiKey;

    public TranscriptionService(
            RestClient.Builder restClientBuilder,
            GlobalStats globalStats) {

        this.restClient = restClientBuilder.build();
        this.globalStats = globalStats;
    }

    public TranscriptionResponse transcribe(
            MultipartFile audio) throws Exception {

        ByteArrayResource audioResource =
                new ByteArrayResource(audio.getBytes()) {

                    @Override
                    public String getFilename() {
                        return audio.getOriginalFilename();
                    }
                };

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add("file", audioResource);
        body.add("model", "gpt-4o-mini-transcribe");

        RestClient.RequestHeadersSpec<?> request =
                restClient.post()
                        .uri(transcriptionUrl)
                        .contentType(
                                MediaType.MULTIPART_FORM_DATA
                        )
                        .body(body);

        if (apiKey != null && !apiKey.isBlank()) {
            request = request.header(
                    "Authorization",
                    "Bearer " + apiKey
            );
        }

        TranscriptionResponse response =
                request
                        .retrieve()
                        .body(TranscriptionResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Empty transcription response."
            );
        }

        // Local Whisper does not provide usage.
        // OpenAI does provide usage.
        if (response.getUsage() != null) {

            globalStats.addInputTokens(
                    response.getUsage()
                            .getInput_tokens()
            );

            globalStats.addOutputTokens(
                    response.getUsage()
                            .getOutput_tokens()
            );
        }

        return response;
    }
}