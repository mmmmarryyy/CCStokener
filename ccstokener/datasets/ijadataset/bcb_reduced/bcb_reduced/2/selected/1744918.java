package rath.msnm.swing.util;

import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import javax.sound.sampled.*;
import rath.msnm.LocalCopy;
import rath.msnm.swing.MainFrame;

/**
 * �濡�� �ɾ�ٴϸ鼭 ������ ���� �� �ִٴ�
 * �� ������ ��ũ��.
 * Wav ���� �ܿ��� �׽�Ʈ�غ��� ������ ����.
 *
 * @author Jang-Ho Hwang, rath@linuxkorea.co.kr
 * @version $Id: MusicBox.java,v 1.3 2002/03/05 13:41:43 xrath Exp $
 */
public class MusicBox {

    public static final String SOUND_LOGIN = "Login";

    public static final String SOUND_MESSAGE_1 = "Message.1";

    public static final String SOUND_MESSAGE_2 = "Message.2";

    private static LineListener clipListener = new ClipListener();

    private static Hashtable waveMap = new Hashtable();

    private static Hashtable acceptMap = new Hashtable();

    /**
	 * ������ �о� �� ���� �����͸� �޸𸮷� �ø���.
	 */
    public static void init() {
        Class cl = MusicBox.class;
        LocalCopy local = MainFrame.LOCALCOPY;
        try {
            URL urlLogin = new URL(local.getProperty(MainFrame.SOUND_LOGIN_PROP, cl.getResource("/resources/sounds/login.wav").toString()));
            URL urlMessage1 = new URL(local.getProperty(MainFrame.SOUND_MESSAGE_1_PROP, cl.getResource("/resources/sounds/message1.wav").toString()));
            URL urlMessage2 = new URL(local.getProperty(MainFrame.SOUND_MESSAGE_2_PROP, cl.getResource("/resources/sounds/message2.wav").toString()));
            waveMap.put(SOUND_LOGIN, readBytes(urlLogin));
            waveMap.put(SOUND_MESSAGE_1, readBytes(urlMessage1));
            waveMap.put(SOUND_MESSAGE_2, readBytes(urlMessage2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] readBytes(URL url) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        InputStream in = url.openStream();
        try {
            int readlen;
            while ((readlen = in.read(buf)) > 0) bos.write(buf, 0, readlen);
        } finally {
            in.close();
        }
        return bos.toByteArray();
    }

    public static void setEnabled(String name, boolean enable) {
        if (enable) acceptMap.put(name, ""); else acceptMap.remove(name);
    }

    public static boolean isEnabled(String name) {
        return acceptMap.containsKey(name);
    }

    /**
	 * �ش� �̸��� �ش��ϴ� Wave ������ �����Ѵ�. 
	 * �� method�� �񵿱�ν�, ��ٷ� return�ȴ�.
	 */
    public static void play(String name) {
        byte[] dat = (byte[]) waveMap.get(name);
        if (dat == null) return;
        if (!acceptMap.containsKey(name)) return;
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(dat));
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat(), ((int) ais.getFrameLength() * format.getFrameSize()));
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.addLineListener(clipListener);
            clip.open(ais);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * ���ְ� ������ �ش� Clip�� close�ϱ� ���� �������̴�.
	 * ���� Thread�� ���ϴ� ����, Redhat 7.1, JDK 1.4���� 
	 * ��ٷ� close() �Ұ�� VM Panic�� �����ױ� �����̴�.
	 */
    private static class ClipListener implements LineListener {

        public void update(final LineEvent e) {
            if (e.getType().equals(LineEvent.Type.STOP)) {
                new Thread(new Runnable() {

                    public void run() {
                        e.getLine().close();
                    }
                }).start();
            }
        }
    }
}
