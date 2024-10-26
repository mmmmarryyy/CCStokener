import java.util.*;
import java.io.*;
import java.net.*;
import iwork.eheap2.*;
import java.applet.*;

public class SoundClip implements Runnable {

    Event templateEvent;

    EventHeap m_EventHeap;

    String currentDir;

    HashMap AudioFiles = new HashMap();

    SoundClip(String server) {
        super();
        System.out.println("Trying to connect\n");
        m_EventHeap = new EventHeap(server);
        System.out.println("Did we connect?");
        try {
            templateEvent = new Event("AudioEvent");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        currentDir = System.getProperty("user.dir") + System.getProperty("file.separator");
        File f = new File(".");
        String[] fileList = f.list(new FilenameFilter() {

            public boolean accept(File f, String s) {
                return s.endsWith(".wav");
            }
        });
        try {
            for (int i = 0; i < fileList.length; i++) {
                System.out.println("loading audio file://" + currentDir + fileList[i]);
                AudioFiles.put(fileList[i], Applet.newAudioClip(new URL("file://" + currentDir + fileList[i])));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                Event e = m_EventHeap.waitForEvent(templateEvent);
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
                AudioFiles.put(name, Applet.newAudioClip(new URL("file://" + currentDir + name)));
            } else if (command.equals("Play")) {
                String name = e.getPostValueString("Name");
                AudioClip ac = (AudioClip) AudioFiles.get(name);
                ac.play();
                System.out.println("playing audio file: " + name);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] argv) {
        SoundClip sc = new SoundClip(argv[0]);
        Thread t = new Thread(sc);
        t.start();
    }

    public static boolean getFile(String s, String name) {
        try {
            File f = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + name);
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
