
package comp3011.assignment1.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.service.SubtitleResponse;
import comp3011.assignment1.service.SubtitleService;

@RestController
@RequestMapping("/api/v1")
public class SubtitleController {

    private final SubtitleService subtitleService;

    public SubtitleController(SubtitleService subtitleService) {
        this.subtitleService = subtitleService;
    }

    @PostMapping("/subtitles")
    public ResponseEntity<SubtitleResponse> generateSubtitles(
            @RequestParam("media") MultipartFile media) {

        try {
            SubtitleResponse response =
                    subtitleService.transcribe(media);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();

        } catch (IOException e) {
            System.err.println("Subtitle file processing error:");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();

        } catch (Exception e) {
            System.err.println("Subtitle generation error:");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
