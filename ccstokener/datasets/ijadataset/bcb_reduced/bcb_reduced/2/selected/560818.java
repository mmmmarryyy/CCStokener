package ffmpeg;

import gui.FFMpegGui;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import language.Language;
import util.Constants;
import util.ConvertUtils;
import util.Downloader;

/**
 * Objects of FFMpeg represant an interface the ffmpeg program
 * @author SebastianWe
 *
 */
public class FFMpeg implements Runnable {

    private InputMedium inputMedium;

    private OutputMedium outputMedium;

    private CodecVector supportedCodecs;

    private String path;

    private FFMpegProgressReceiver recv;

    public static int AUDIO = 1;

    public static int VIDEO = 2;

    private Language lang;

    /**
	 * Constructs a new Object of FFMpeg
	 * @param path
	 * 				The path to ffmpeg
	 * @param lang
	 * 				Language
	 * @param recv
	 * 				Receiver of conversion state
	 * @throws IOException
	 * 				when ffmpeg binary was not found
	 */
    public FFMpeg(String path, Language lang, FFMpegProgressReceiver recv) throws IOException {
        this.path = path;
        this.recv = recv;
        this.lang = lang;
        supportedCodecs = new CodecVector(lang);
        scan();
    }

    /**
	 * Returns a CodecVector of supported Codecs
	 * @return CodecVector
	 */
    public CodecVector getSupportedCodecs() {
        return supportedCodecs;
    }

    /**
	 * Return a String array that represents a Codec List 
	 * @param type
	 * 			specify the kind of codec
	 * 			1 = audio
	 * 			2 = video
	 * @return
	 * 			String array of codecs from <type>
	 */
    public String[] getSupportedCodecsList(int type) {
        Vector<Codec> tmp = new Vector<Codec>();
        Iterator<Codec> i = supportedCodecs.iterator();
        while (i.hasNext()) {
            Codec t = i.next();
            if (type == AUDIO) {
                if (t.isAudioCodec() && t.isEncodeable()) tmp.add(t);
            } else if (type == VIDEO) {
                if (t.isVideoCodec() && t.isEncodeable()) tmp.add(t);
            }
        }
        String[] codecs = new String[tmp.size()];
        for (int j = 0; j < tmp.size(); j++) codecs[j] = tmp.get(j).getShortDescription();
        return codecs;
    }

    /**
	 * 
	 * @param path
	 * 				Path to ffmpeg
	 * @throws IOException
	 * 				when ffmpeg binary was not found
	 */
    public void setPath(String path) throws IOException {
        this.path = path;
        scan();
    }

    /**
	 * Scans for codecs
	 * @throws IOException
	 * 						on errors with ffmpeg process
	 */
    private void scan() throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec(new String[] { path, "-formats" });
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        boolean scan = false;
        while ((line = br.readLine()) != null) {
            if (Constants.debug) System.out.println(line);
            if (scan) {
                if (line.length() == 0) {
                    scan = false;
                    break;
                }
                String flags = line.substring(1, 7);
                supportedCodecs.add(new Codec(flags.charAt(0) == 'D', flags.charAt(1) == 'E', flags.charAt(2) == 'V', flags.charAt(2) == 'A', flags.charAt(2) == 'S', flags.charAt(3) == 'S', flags.charAt(4) == 'D', flags.charAt(5) == 'T', line.substring(8, 24).trim(), line.substring(24).trim()));
            }
            if (line.contains("Codecs:")) scan = true;
        }
    }

    /**
	 * Sets a new inputMedium
	 * @param input
	 * 				Path to input file
	 * @param caller
	 * 				Caller of the method
	 * @param recv
	 * 				Download-State Receiver
	 * @throws IOException
	 * 				when video url is incorrect or there are problems with the input medium format
	 */
    public void setInput(String input, Component caller, FFMpegProgressReceiver recv) throws IOException {
        inputMedium = null;
        if (input.contains("youtube")) {
            URL url = new URL(input);
            InputStreamReader read = new InputStreamReader(url.openStream());
            BufferedReader in = new BufferedReader(read);
            String inputLine;
            String line = null;
            String vid = input.substring(input.indexOf("?v=") + 3);
            if (vid.indexOf("&") != -1) vid = vid.substring(0, vid.indexOf("&"));
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("\"t\": \"")) {
                    line = inputLine.substring(inputLine.indexOf("\"t\": \"") + 6);
                    line = line.substring(0, line.indexOf("\""));
                    break;
                }
            }
            in.close();
            if (line == null) throw new IOException("Could not find flv-Video");
            Downloader dl = new Downloader("http://www.youtube.com/get_video?video_id=" + vid + "&t=" + line, recv, lang);
            dl.start();
            return;
        }
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec(new String[] { path, "-i", input });
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line;
        Codec videoCodec = null;
        Codec audioCodec = null;
        double duration = -1;
        String aspectRatio = null;
        String scala = null;
        String colorSpace = null;
        String rate = null;
        String mrate = null;
        String aRate = null;
        String aFreq = null;
        String aChannel = null;
        try {
            while ((line = br.readLine()) != null) {
                if (Constants.debug) System.out.println(line);
                if (line.contains("Duration:")) {
                    int hours = Integer.parseInt(line.substring(12, 14));
                    int mins = Integer.parseInt(line.substring(15, 17));
                    double secs = Double.parseDouble(line.substring(18, line.indexOf(',')));
                    duration = secs + 60 * mins + hours * 60 * 60;
                    Pattern pat = Pattern.compile("[0-9]+ kb/s");
                    Matcher m = pat.matcher(line);
                    if (m.find()) mrate = line.substring(m.start(), m.end());
                }
                if (line.contains("Video:")) {
                    String info = line.substring(24);
                    String parts[] = info.split(", ");
                    Pattern pat = Pattern.compile("Video: [a-zA-Z0-9]+,");
                    Matcher m = pat.matcher(line);
                    String codec = "";
                    if (m.find()) codec = line.substring(m.start(), m.end());
                    videoCodec = supportedCodecs.getCodecByName(codec.replace("Video: ", "").replace(",", ""));
                    colorSpace = parts[1];
                    pat = Pattern.compile("[0-9]+x[0-9]+");
                    m = pat.matcher(info);
                    if (m.find()) scala = info.substring(m.start(), m.end());
                    pat = Pattern.compile("DAR [0-9]+:[0-9]+");
                    m = pat.matcher(info);
                    if (m.find()) aspectRatio = info.substring(m.start(), m.end()).replace("DAR ", ""); else if (scala != null) aspectRatio = String.valueOf((double) (Math.round(((double) ConvertUtils.getWidthFromScala(scala) / (double) ConvertUtils.getHeightFromScala(scala)) * 100)) / 100);
                    pat = Pattern.compile("[0-9]+ kb/s");
                    m = pat.matcher(info);
                    if (m.find()) rate = info.substring(m.start(), m.end());
                } else if (line.contains("Audio:")) {
                    String info = line.substring(24);
                    Pattern pat = Pattern.compile("Audio: [a-zA-Z0-9]+,");
                    Matcher m = pat.matcher(line);
                    String codec = "";
                    if (m.find()) codec = line.substring(m.start(), m.end()).replace("Audio: ", "").replace(",", "");
                    if (codec.equals("mp3")) codec = "libmp3lame";
                    audioCodec = supportedCodecs.getCodecByName(codec);
                    pat = Pattern.compile("[0-9]+ kb/s");
                    m = pat.matcher(info);
                    if (m.find()) aRate = info.substring(m.start(), m.end());
                    pat = Pattern.compile("[0-9]+ Hz");
                    m = pat.matcher(info);
                    if (m.find()) aFreq = info.substring(m.start(), m.end());
                    if (line.contains("5.1")) aChannel = "5.1"; else if (line.contains("2.1")) aChannel = "2.1"; else if (line.contains("stereo")) aChannel = "Stereo"; else if (line.contains("mono")) aChannel = "Mono";
                }
                if (videoCodec != null && audioCodec != null && duration != -1) {
                    if (rate == null && mrate != null && aRate != null) rate = String.valueOf(ConvertUtils.getRateFromRateString(mrate) - ConvertUtils.getRateFromRateString(aRate)) + " kb/s";
                    inputMedium = new InputMedium(audioCodec, videoCodec, input, duration, colorSpace, aspectRatio, scala, rate, mrate, aRate, aFreq, aChannel);
                    break;
                }
            }
            if ((videoCodec != null || audioCodec != null) && duration != -1) inputMedium = new InputMedium(audioCodec, videoCodec, input, duration, colorSpace, aspectRatio, scala, rate, mrate, aRate, aFreq, aChannel);
        } catch (Exception exc) {
            if (caller != null) JOptionPane.showMessageDialog(caller, lang.inputerror + " Audiocodec? " + (audioCodec != null) + " Videocodec? " + (videoCodec != null), lang.error, JOptionPane.ERROR_MESSAGE);
            if (Constants.debug) System.out.println("Audiocodec: " + audioCodec + "\nVideocodec: " + videoCodec);
            if (Constants.debug) exc.printStackTrace();
            throw new IOException("Input file error");
        }
        if (inputMedium == null) {
            if (caller != null) JOptionPane.showMessageDialog(caller, lang.inputerror + " Audiocodec? " + (audioCodec != null) + " Videocodec? " + (videoCodec != null), lang.error, JOptionPane.ERROR_MESSAGE);
            if (Constants.debug) System.out.println("Audiocodec: " + audioCodec + "\nVideocodec: " + videoCodec);
            throw new IOException("Input file error");
        }
    }

    /**
	 * Sets a new output medium
	 * @param output
	 * 				Path to output file
	 * @param audio
	 * 				audio codec short description
	 * @param video
	 * 				video codec short description
	 * @param aspect
	 * 				aspect ratio
	 * @param scala
	 * 				scala (e.g. 123x234) 
	 * @param rate
	 * 				video bitrate
	 * @param arate
	 * 				audio bitrate
	 * @param type
	 * 				encoding type (vcd, svcd, dvd, user defined[, ...])
	 * @param bps
	 * 				frames per second
	 */
    public void setOutput(String output, String audio, String video, String aspect, String scala, String rate, String arate, int type, String bps, String channels) {
        outputMedium = new OutputMedium(supportedCodecs.getCodecByName(audio), supportedCodecs.getCodecByName(video), output, null, aspect, scala, rate, null, arate, null, channels, type, bps);
    }

    /**
	 * Starts conversion
	 * @pre Should be invoked by an extra Thread
	 * @throws IOException
	 */
    private void convert() throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] cmd;
        int channels = Integer.parseInt(outputMedium.getAChannel());
        switch(channels) {
            case 1:
                channels = 1;
                break;
            case 2:
                channels = 2;
                break;
            case 3:
                channels = 3;
                break;
            case 4:
                channels = 6;
                break;
            default:
                channels = 0;
                break;
        }
        if (outputMedium.getType() == 0) cmd = new String[] { path, "-i", inputMedium.getPath(), "-target", "pal-vcd", outputMedium.getPath() }; else if (outputMedium.getType() == 1) cmd = new String[] { path, "-i", inputMedium.getPath(), "-target", "pal-svcd", outputMedium.getPath() }; else if (outputMedium.getType() == 2) cmd = new String[] { path, "-i", inputMedium.getPath(), "-target", "pal-dvd", outputMedium.getPath() }; else {
            if (!outputMedium.getAspectRatio().equals(lang.automatic)) {
                if (outputMedium.getARate().isEmpty()) {
                    if (outputMedium.getBPS().equals("0")) cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-aspect", outputMedium.getAspectRatio(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), outputMedium.getPath() }; else cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-r", outputMedium.getBPS(), "-aspect", outputMedium.getAspectRatio(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), outputMedium.getPath() };
                } else if (outputMedium.getAChannel().equals("0")) {
                    if (outputMedium.getBPS().equals("0")) cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-aspect", outputMedium.getAspectRatio(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), "-ab", Integer.parseInt(outputMedium.getARate().trim()) * 1000 + "", outputMedium.getPath() }; else cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-r", outputMedium.getBPS(), "-aspect", outputMedium.getAspectRatio(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), "-ab", Integer.parseInt(outputMedium.getARate().trim()) * 1000 + "", outputMedium.getPath() };
                } else {
                    if (outputMedium.getBPS().equals("0")) cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-aspect", outputMedium.getAspectRatio(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), "-ac", channels + "", "-ab", Integer.parseInt(outputMedium.getARate().trim()) * 1000 + "", outputMedium.getPath() }; else cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-r", outputMedium.getBPS(), "-aspect", outputMedium.getAspectRatio(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), "-ac", channels + "", "-ab", Integer.parseInt(outputMedium.getARate().trim()) * 1000 + "", outputMedium.getPath() };
                }
            } else {
                if (outputMedium.getARate().isEmpty()) {
                    if (outputMedium.getBPS().equals("0")) cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), outputMedium.getPath() }; else cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-r", outputMedium.getBPS(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), outputMedium.getPath() };
                } else if (outputMedium.getAChannel().equals("0")) {
                    if (outputMedium.getBPS().equals("0")) cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), "-ab", Integer.parseInt(outputMedium.getARate().trim()) * 1000 + "", outputMedium.getPath() }; else cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-r", outputMedium.getBPS(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), "-ab", Integer.parseInt(outputMedium.getARate().trim()) * 1000 + "", outputMedium.getPath() };
                } else {
                    if (outputMedium.getBPS().equals("0")) cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), "-ac", channels + "", "-ab", Integer.parseInt(outputMedium.getARate().trim()) * 1000 + "", outputMedium.getPath() }; else cmd = new String[] { path, "-i", inputMedium.getPath(), "-vcodec", outputMedium.getVideoCodec().getShortDescription(), "-s", outputMedium.getScala(), "-r", outputMedium.getBPS(), "-b", Integer.parseInt(outputMedium.getRate().trim()) * 1000 + "", "-acodec", outputMedium.getAudioCodec().getShortDescription(), "-ac", channels + "", "-ab", Integer.parseInt(outputMedium.getARate().trim()) * 1000 + "", outputMedium.getPath() };
                }
            }
        }
        if (Constants.debug) {
            for (int i = 0; i < cmd.length; i++) System.out.print(cmd[i] + " ");
            System.out.println();
        }
        Process p = rt.exec(cmd);
        int exit = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        try {
            OutputStreamWriter osw = new OutputStreamWriter(p.getOutputStream());
            osw.write("y");
            osw.flush();
        } catch (Exception exc) {
            exit = 1;
            if (Constants.debug) System.out.println(exc.getMessage());
        }
        String line, lastline = "";
        if (recv != null) {
            int i = 0;
            while ((line = br.readLine()) != null) {
                lastline = line;
                if (Constants.debug) System.out.println(line);
                if (Thread.currentThread().isInterrupted()) {
                    recv.setProgress(102);
                    p.destroy();
                    return;
                }
                if (line.contains("frame=")) {
                    i++;
                    double time = Double.parseDouble(line.substring(line.indexOf("time=") + 5, line.indexOf("bitrate=") - 1));
                    int progress = (int) (time / inputMedium.getDuration() * 100);
                    if (progress >= 0 && progress < 100) recv.setProgress(progress); else recv.setProgress(100);
                }
            }
            try {
                Thread.sleep(500);
                exit = p.exitValue();
            } catch (Exception exc) {
                if (Constants.debug) System.out.println(exc.getMessage());
            }
            if (i == 0) {
                if (recv instanceof FFMpegGui) JOptionPane.showMessageDialog((FFMpegGui) recv, lastline, "FFMpeg Error", JOptionPane.ERROR_MESSAGE);
                recv.setProgress(103);
            } else if (exit != 0) recv.setProgress(105); else recv.setProgress(101);
        }
    }

    @Override
    public void run() {
        try {
            convert();
        } catch (Exception exc) {
            if (Constants.debug) {
                System.out.println("Conversion failed! " + exc.getMessage());
                exc.printStackTrace();
            }
        }
    }

    /**
	 * Return the inputMedium
	 * @return inputMedium or null if no inputMedium is set
	 */
    public InputMedium getInputMedium() {
        return inputMedium;
    }

    /**
	 * Returns the path to ffmpeg
	 * @return path
	 */
    public String getPath() {
        return path;
    }
}
