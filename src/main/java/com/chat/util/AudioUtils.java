package com.chat.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream; // <--- DÒNG NÀY ĐANG BỊ THIẾU
import java.util.Base64;

public class AudioUtils {
    private static final AudioFormat FORMAT = new AudioFormat(8000.0f, 16, 1, true, true); // 8kHz, 16bit, Mono
    private TargetDataLine microphone;
    private boolean recording = false;
    private ByteArrayOutputStream out;

    public void startRecording() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("Microphone not supported");
            return;
        }
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
        if (out == null) return null;
        byte[] audioData = out.toByteArray();
        if (audioData.length == 0) return null;
        return Base64.getEncoder().encodeToString(audioData);
    }

    public static void playBase64Audio(String base64) {
        new Thread(() -> {
            try {
                byte[] audioData = Base64.getDecoder().decode(base64);
                InputStream stream = new ByteArrayInputStream(audioData);
                AudioInputStream audioStream = new AudioInputStream(stream, FORMAT, audioData.length / FORMAT.getFrameSize());
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(FORMAT);
                speakers.start();

                byte[] buffer = new byte[4096];
                int count;
                while ((count = audioStream.read(buffer, 0, buffer.length)) != -1) {
                    speakers.write(buffer, 0, count);
                }
                speakers.drain();
                speakers.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}