package comp3011.assignment1.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AdminController {

    private final ApplicationContext applicationContext;
    private final GlobalStats globalStats;

    // Time when this Spring Boot application started
    private final Instant serverStart = Instant.now();

    // Prevent shutdown from being requested more than once
    private final AtomicBoolean shutdownInProgress =
            new AtomicBoolean(false);

    public AdminController(
            ApplicationContext applicationContext,
            GlobalStats globalStats) {

        this.applicationContext = applicationContext;
        this.globalStats = globalStats;
    }


    @GetMapping("/admin/uptime")
    public ResponseEntity<UptimeResponse> getServerUptime() {

        Instant now = Instant.now();

        double uptimeSeconds =
                Duration.between(serverStart, now)
                        .toNanos() / 1_000_000_000.0;

        UptimeResponse response = new UptimeResponse(
                serverStart.toString(),
                now.toString(),
                uptimeSeconds
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/admin/shutdown")
    public ResponseEntity<?> shutdownServer() {

        if (!shutdownInProgress.compareAndSet(false, true)) {

            ErrorResponse error = new ErrorResponse(
                    Instant.now().toString(),
                    409,
                    "Conflict",
                    "Graceful shutdown is already in progress.",
                    "/api/v1/admin/shutdown"
            );

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(error);
        }

        ShutdownResponse response =
                new ShutdownResponse(
                        "Graceful shutdown requested."
                );

        Thread shutdownThread = new Thread(() -> {

            try {

                Thread.sleep(100);

                int exitCode =
                        SpringApplication.exit(
                                applicationContext,
                                () -> 0
                        );

                System.exit(exitCode);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }

        });

        shutdownThread.start();

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }


    @GetMapping("/global/stats")
    public ResponseEntity<GlobalStatsResponse> getGlobalStats() {

        GlobalStatsResponse response =
                new GlobalStatsResponse(
                        globalStats.getInputTokens(),
                        globalStats.getOutputTokens()
                );

        return ResponseEntity.ok(response);
    }


    public static class UptimeResponse {

        private final String utcServerStart;
        private final String utcNow;
        private final double serverUptimeSeconds;

        public UptimeResponse(
                String utcServerStart,
                String utcNow,
                double serverUptimeSeconds) {

            this.utcServerStart = utcServerStart;
            this.utcNow = utcNow;
            this.serverUptimeSeconds = serverUptimeSeconds;
        }

        public String getUtcServerStart() {
            return utcServerStart;
        }

        public String getUtcNow() {
            return utcNow;
        }

        public double getServerUptimeSeconds() {
            return serverUptimeSeconds;
        }
    }


    public static class ShutdownResponse {

        private final String message;

        public ShutdownResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }


    public static class GlobalStatsResponse {

        private final long inputTokens;
        private final long outputTokens;

        public GlobalStatsResponse(
                long inputTokens,
                long outputTokens) {

            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }

        public long getInputTokens() {
            return inputTokens;
        }

        public long getOutputTokens() {
            return outputTokens;
        }
    }


    public static class ErrorResponse {

        private final String timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;

        public ErrorResponse(
                String timestamp,
                int status,
                String error,
                String message,
                String path) {

            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }
    }
}