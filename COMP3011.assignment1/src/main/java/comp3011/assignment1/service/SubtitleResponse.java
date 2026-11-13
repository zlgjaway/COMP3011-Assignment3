package comp3011.assignment1.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubtitleResponse {

    private String text;
    private double duration;
    private List<Segment> segments;
    private List<Word> words;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments;
    }

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    public static class Segment {

        private double start;
        private double end;
        private String text;

        @JsonProperty("no_speech_prob")
        private double noSpeechProb;

        @JsonProperty("avg_logprob")
        private double avgLogprob;

        public double getStart() {
            return start;
        }

        public void setStart(double start) {
            this.start = start;
        }

        public double getEnd() {
            return end;
        }

        public void setEnd(double end) {
            this.end = end;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public double getNoSpeechProb() {
            return noSpeechProb;
        }

        public void setNoSpeechProb(double noSpeechProb) {
            this.noSpeechProb = noSpeechProb;
        }

        public double getAvgLogprob() {
            return avgLogprob;
        }

        public void setAvgLogprob(double avgLogprob) {
            this.avgLogprob = avgLogprob;
        }
    }

    public static class Word {

        private double start;
        private double end;
        private String word;

        public double getStart() {
            return start;
        }

        public void setStart(double start) {
            this.start = start;
        }

        public double getEnd() {
            return end;
        }

        public void setEnd(double end) {
            this.end = end;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }
    }
}
