package net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

public class IlkDeneme extends Thread {

    public void okIlkDenemeBu() {
        try {
            URL con = new URL("http://studivz.net");
            URLConnection urlCon = con.openConnection();
            urlCon.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
            String decodedString;
            while ((decodedString = in.readLine()) != null) {
                String qs = "<title>";
                System.out.println("xx:" + decodedString);
            }
            in.close();
        } catch (IOException e) {
        }
    }

    public void run() {
        int j = 10;
        for (int i = 0; i < j; i++) {
            try {
                sleep(1000);
                System.out.println("hehe :)" + i);
                okIlkDenemeBu();
                j++;
            } catch (InterruptedException e) {
                System.err.println("Error:" + e);
            }
        }
    }

    public void ringSound() {
        try {
            int sampleRate = 22050;
            AudioFormat audioformat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 2, 4, sampleRate, false);
            DataLine.Info datalineinfo = new DataLine.Info(SourceDataLine.class, audioformat);
            if (!AudioSystem.isLineSupported(datalineinfo)) {
                System.out.println("Line matching " + datalineinfo + " is not supported.");
            } else {
                SourceDataLine sourcedataline = (SourceDataLine) AudioSystem.getLine(datalineinfo);
                sourcedataline.open(audioformat);
                sourcedataline.start();
                byte[] samples = new byte[1000];
                for (int s = 1; s < 10; s++) {
                    for (int freq = 1000; freq < 2000; freq += s) {
                        float size = ((float) sampleRate) / ((float) freq);
                        float amplitude = 32000;
                        int adr = 0;
                        for (int i = 0; i < size; i++, adr += 4) {
                            double sin = Math.sin((double) i / (double) size * 2.0 * Math.PI);
                            int sample = (int) (sin * amplitude);
                            samples[adr + 0] = (byte) (sample);
                            samples[adr + 1] = (byte) (sample >>> 8);
                            samples[adr + 2] = (byte) (sample);
                            samples[adr + 3] = (byte) (sample >>> 8);
                        }
                        sourcedataline.write(samples, 0, adr);
                    }
                }
                sourcedataline.drain();
                sourcedataline.stop();
                sourcedataline.close();
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        IlkDeneme ilk = new IlkDeneme();
        System.out.println("start");
        ilk.run();
    }
}
