package comp3011.assignment1.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.service.TranscriptionResponse;
import comp3011.assignment1.service.TranscriptionService;

@RestController
@RequestMapping("/api/v1")
public class AudioController {

    private final TranscriptionService transcriptionService;

    public AudioController(
            TranscriptionService transcriptionService) {

        this.transcriptionService = transcriptionService;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<TranscriptionResponse> transcribe(
            @RequestParam("audio") MultipartFile audio) {

        try {

            TranscriptionResponse response =
                    transcriptionService.transcribe(audio);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}