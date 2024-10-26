package org.microemu.iphone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import org.microemu.MIDletEntry;
import org.microemu.app.CommonInterface;
import org.microemu.app.launcher.Launcher;

public class IPhoneLauncher extends Launcher {

    private static final File MIDLET_FOLDER = new File("/var/mobile/Media/MIDlets");

    private static final Command CMD_ADD = new Command("Add", Command.ITEM, 0);

    private TextBox textBox;

    static {
        MIDLET_FOLDER.mkdirs();
        for (File file : MIDLET_FOLDER.listFiles()) {
            System.out.println("Loading: " + file);
            try {
                addJar(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void addJar(File file) throws MalformedURLException, IOException, ClassNotFoundException {
        URLClassLoader classLoader = new IPhoneClassLoader(file);
        InputStream is = classLoader.getResourceAsStream("/META-INF/MANIFEST.MF");
        Manifest manifest = new Manifest(is);
        for (Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
            if (entry.getKey().toString().startsWith("MIDlet-")) {
                try {
                    Integer.parseInt(entry.getKey().toString().substring(7));
                    System.out.println("Adding: " + entry.getValue());
                    String[] value = entry.getValue().toString().split(",");
                    addMIDletEntry(new MIDletEntry(value[0].trim(), classLoader.loadClass(value[2].trim())));
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    public IPhoneLauncher(CommonInterface common) {
        super(common);
    }

    @Override
    public void startApp() {
        menuList = new List("Launcher", List.EXCLUSIVE);
        menuList.addCommand(CMD_LAUNCH);
        menuList.addCommand(CMD_ADD);
        menuList.setCommandListener(this);
        fillList();
        textBox = new TextBox("Add MIDlet", "http://www.pyx4me.com/maven2-snapshot/org/microemu/microemu-demo/2.0.3-SNAPSHOT/microemu-demo-2.0.3-20081126.080125-55-me.jar", 255, TextField.URL);
        textBox.addCommand(CMD_ADD);
        textBox.setCommandListener(this);
        Display.getDisplay(this).setCurrent(menuList);
    }

    private void fillList() {
        menuList.deleteAll();
        if (midletEntries.size() == 0) {
            menuList.append(NOMIDLETS, null);
        } else {
            for (int i = 0; i < midletEntries.size(); i++) {
                menuList.append(((MIDletEntry) midletEntries.elementAt(i)).getName(), null);
            }
        }
    }

    @Override
    public void commandAction(Command c, Displayable d) {
        if (c == CMD_ADD) {
            if (d == menuList) {
                Display.getDisplay(this).setCurrent(textBox);
            } else if (d == textBox) {
                System.out.println(textBox.getString());
                try {
                    URL url = new URL(textBox.getString());
                    File file = new File(MIDLET_FOLDER, URLEncoder.encode(url.toString(), "UTF-8").replace('%', '_'));
                    copyToFile(url.openStream(), file);
                    addJar(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                Display.getDisplay(this).setCurrent(menuList);
            }
        } else super.commandAction(c, d);
    }

    private static void copyToFile(InputStream is, File dst) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = is.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } finally {
            closeQuietly(fos);
        }
    }

    /**
 * Unconditionally close an <code>OutputStream</code>.
 * <p>
 * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored.
 * This is typically used in finally blocks.
 *
 * @param output  the OutputStream to close, may be null or already closed
 */
    public static void closeQuietly(OutputStream output) {
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException ignore) {
        }
    }
}

final class IPhoneClassLoader extends URLClassLoader {

    private File file;

    IPhoneClassLoader(File file) throws MalformedURLException {
        super(new URL[] { file.toURL() });
        this.file = file;
    }

    @Override
    public URL getResource(String name) {
        System.out.println("IPhoneClassLoader.getResource(" + name + ")");
        try {
            Enumeration<URL> enumeration = getResources(name);
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                if (url.toExternalForm().contains(file.toURL().toExternalForm())) return url;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        if (url == null) return null; else try {
            return url.openStream();
        } catch (IOException e) {
            return null;
        }
    }
}
