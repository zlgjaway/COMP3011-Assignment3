
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
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

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
    
    
    private static final Path CACHE_DIR = Path.of(
            System.getProperty("user.home"),
            ".comp3011",
            "subtitle-cache"
    );

    private Path cachePath(File mediaFile) throws Exception {
        // Hash the media content, not its filename.
        // This lets the same video reuse subtitles if moved or renamed.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (var input = Files.newInputStream(mediaFile.toPath())) {
            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }

        String hash = HexFormat.of().formatHex(digest.digest());
        return CACHE_DIR.resolve(hash + ".json");
    }

    private List<Subtitle> readCache(Path path) throws IOException {
        JsonNode json = mapper.readTree(path.toFile());

        if (!json.isArray()) {
            throw new IOException("Invalid subtitle cache format");
        }

        List<Subtitle> result = new ArrayList<>();

        for (JsonNode item : json) {
            double start = item.path("start").asDouble();
            double end = item.path("end").asDouble();
            String text = item.path("text").asText();

            if (!Double.isFinite(start)
                    || !Double.isFinite(end)
                    || start < 0
                    || end <= start
                    || text.isBlank()) {
                throw new IOException("Invalid cached subtitle");
            }

            result.add(new Subtitle(start, end, text));
        }

        if (result.isEmpty()) {
            throw new IOException("Subtitle cache is empty");
        }

        return List.copyOf(result);
    }

    private void writeCache(Path path, List<Subtitle> subtitles)
            throws IOException {

        Files.createDirectories(CACHE_DIR);

        Path temp = Files.createTempFile(
                CACHE_DIR, "subtitles-", ".tmp");

        try {
            mapper.writeValue(temp.toFile(), subtitles);

            try {
                Files.move(
                        temp,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(
                        temp,
                        path,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }


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

    

	private final HttpClient client = HttpClient.newBuilder()
	        .connectTimeout(java.time.Duration.ofSeconds(10))
	        .build();
    private final ObjectMapper mapper = new ObjectMapper();
    
    private Path extractAudio(File mediaFile) throws Exception {
        Path audioFile = Files.createTempFile("subtitle-audio-", ".mp3");

        try {
            try (FFmpegFrameGrabber grabber =
                         new FFmpegFrameGrabber(mediaFile);
                 FFmpegFrameRecorder recorder =
                         new FFmpegFrameRecorder(audioFile.toString(), 0, 1)) {

                grabber.start();

                recorder.setFormat("mp3");
                recorder.setAudioCodec(
                        org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MP3);
                recorder.setSampleRate(16000);
                recorder.setAudioChannels(1);
                recorder.setAudioBitrate(64000);

                recorder.start();

                Frame frame;

                while ((frame = grabber.grabSamples()) != null) {
                    if (frame.samples != null) {
                        recorder.recordSamples(
                                grabber.getSampleRate(),
                                grabber.getAudioChannels(),
                                frame.samples);
                    }
                }
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
    	    Path cacheFile = null;

    	    try {
    	        cacheFile = cachePath(mediaFile);

    	        if (Files.exists(cacheFile)) {
    	            try {
    	                List<Subtitle> cached = readCache(cacheFile);

    	                System.out.println(
    	                        "Loaded " + cached.size()
    	                        + " subtitles from disk cache.");

    	                return cached;

    	            } catch (IOException e) {
    	                System.err.println(
    	                        "Subtitle cache invalid, requesting again: "
    	                        + e.getMessage());
    	            }
    	        }

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

                List<Subtitle> result = List.copyOf(subtitles);

                if (!result.isEmpty()) {
                    try {
                        writeCache(cacheFile, result);

                        System.out.println(
                                "Saved " + result.size()
                                + " subtitles to disk cache.");

                    } catch (IOException e) {
                        System.err.println(
                                "Unable to save subtitle cache: "
                                + e.getMessage());
                    }
                }

                return result;

    	    } catch (Exception e) {
    	        System.err.println("Subtitle request failed:");
    	        e.printStackTrace();

    	        throw new RuntimeException(
    	                "Unable to retrieve subtitles", e);
    	    }finally {
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
