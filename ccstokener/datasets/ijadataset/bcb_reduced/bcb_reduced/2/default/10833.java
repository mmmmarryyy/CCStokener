import iwork.eheap2.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class AudioEventListener implements Runnable {

    iwork.eheap2.Event templateEvent;

    EventHeap m_EventHeap;

    public static String MEDIA_PLAYER = "D:\\Program Files\\Winamp\\winamp.exe";

    public static String MEDIA_DIRECTORY = "D:\\buttons\\data\\sounds\\";

    public static String TTS_PROGRAM = "C:\\Program Files\\ReadPlease 2002\\ReadPlease.exe /state=1 /text=";

    AudioEventListener(String server) {
        super();
        System.out.println("Trying to connect\n");
        m_EventHeap = new EventHeap(server);
        System.out.println("Did we connect?");
        try {
            templateEvent = new iwork.eheap2.Event("AudioEvent");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                iwork.eheap2.Event e = m_EventHeap.waitForEvent(templateEvent);
                handleEventHeapEvent(e);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void handleEventHeapEvent(iwork.eheap2.Event e) {
        try {
            String command = e.getPostValueString("AudioCommand");
            System.out.println("command: " + command);
            if (command.equals("LoadFromURL")) {
                String url = e.getPostValueString("URL");
                String name = e.getPostValueString("Name");
                System.out.println("fetched file from url: " + getFile(url, name));
            } else if (command.equals("Play")) {
                String name = e.getPostValueString("Name");
                Runtime.getRuntime().exec(MEDIA_PLAYER + " " + MEDIA_DIRECTORY + name);
                System.out.println("playing audio file: " + name);
            } else if (command.equals("Read")) {
                String text = e.getPostValueString("Text");
                String cmd = TTS_PROGRAM + "\"" + text + "\"";
                Runtime.getRuntime().exec(cmd);
                System.out.println("reading text: " + text);
                System.out.println("command line: " + cmd);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] argv) {
        AudioEventListener ael = new AudioEventListener(argv[0]);
        Thread t = new Thread(ael);
        t.start();
    }

    public static boolean getFile(String s, String name) {
        try {
            File f = new File("D:\\buttons\\data\\sounds\\" + name);
            URL url = new URL(s);
            URLConnection conn = url.openConnection();
            BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
            int ch;
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
            while ((ch = bis.read()) != -1) {
                bos.write(ch);
            }
            System.out.println("wrote audio url: " + s + " \nto file " + f);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
