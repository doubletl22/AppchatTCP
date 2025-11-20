package com.chat.util;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class AudioUtils {
    private static final AudioFormat FORMAT = new AudioFormat(8000.0f, 16, 1, true, true); // Chất lượng thấp cho nhẹ
    private TargetDataLine microphone;
    private boolean recording = false;
    private ByteArrayOutputStream out;

    public void startRecording() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(FORMAT);
        microphone.start();
        recording = true;
        out = new ByteArrayOutputStream();

        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (recording) {
                int count = microphone.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
        }).start();
    }

    public String stopRecording() {
        recording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        // Trả về chuỗi Base64 để gửi qua mạng JSON
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public static void playBase64Audio(String base64) {
        new Thread(() -> {
            try {
                byte[] audioData = Base64.getDecoder().decode(base64);
                SourceDataLine speakers;
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
                speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(FORMAT);
                speakers.start();
                speakers.write(audioData, 0, audioData.length);
                speakers.drain();
                speakers.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}