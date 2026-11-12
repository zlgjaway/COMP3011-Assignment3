package comp3011.assignment1.service;

public class TranscriptionResponse {

    private String text;
    private Usage usage;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public static class Usage {

        private String type;
        private long input_tokens;
        private long output_tokens;
        private long total_tokens;
        private InputTokenDetails input_token_details;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getInput_tokens() {
            return input_tokens;
        }

        public void setInput_tokens(long input_tokens) {
            this.input_tokens = input_tokens;
        }

        public long getOutput_tokens() {
            return output_tokens;
        }

        public void setOutput_tokens(long output_tokens) {
            this.output_tokens = output_tokens;
        }

        public long getTotal_tokens() {
            return total_tokens;
        }

        public void setTotal_tokens(long total_tokens) {
            this.total_tokens = total_tokens;
        }

        public InputTokenDetails getInput_token_details() {
            return input_token_details;
        }

        public void setInput_token_details(
                InputTokenDetails input_token_details) {
            this.input_token_details = input_token_details;
        }
    }

    public static class InputTokenDetails {

        private long audio_tokens;
        private long text_tokens;

        public long getAudio_tokens() {
            return audio_tokens;
        }

        public void setAudio_tokens(long audio_tokens) {
            this.audio_tokens = audio_tokens;
        }

        public long getText_tokens() {
            return text_tokens;
        }

        public void setText_tokens(long text_tokens) {
            this.text_tokens = text_tokens;
        }
    }
}