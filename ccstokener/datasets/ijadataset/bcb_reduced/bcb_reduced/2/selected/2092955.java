package de.fmf.pva;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import org.htmlparser.util.Translate;

public class ButtonMachine implements Runnable {

    public static String FONT_DEFAULT = "default";

    public static String FONT_DEFAULT_WeissAufSchwarz = "default-WeissAufSchwarz";

    public static String FONT_DEFAULT_SchwarzAufRot = "default-SchwarzAufRot";

    public static String FONT_DEFAULT_WeissAufRot = "default-WeissAufRot";

    public static String FONT_DEFAULT_SchwarzAufGrau = "default-SchwarzAufGrau";

    public static String FONT_DEFAULT_WeissAufGrau = "default-WeissAufGrau";

    public static String FONT_FL = "fl";

    public static String FONT_FL_WeissAufSchwarz = "fl-WeissAufSchwarz";

    public static String FONT_FL_SchwarzAufRot = "fl-SchwarzAufRot";

    public static String FONT_FL_WeissAufRot = "fl-WeissAufRot";

    public static String FONT_FL_SchwarzAufGrau = "fl-SchwarzAufGrau";

    public static String FONT_FL_WeissAufGrau = "fl-WeissAufGrau";

    private String color;

    private String text;

    private String fileName;

    private File destPath;

    public ButtonMachine(boolean proxy, String proxyIp, String proxyPort) {
        if (proxy) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", proxyIp);
            System.getProperties().put("proxyPort", proxyPort);
        } else {
            System.out.println(this.getClass().getName() + "\tINIT BUTTONMACHINE USING NO PROXY");
        }
    }

    public ButtonMachine(String proxyIp, String proxyPort, File destPath, String fileName, String text, String color) {
        System.getProperties().put("proxySet", "true");
        System.getProperties().put("proxyHost", proxyIp);
        System.getProperties().put("proxyPort", proxyPort);
        this.destPath = destPath;
        this.fileName = fileName;
        this.text = text;
        this.color = color;
    }

    @Override
    public void run() {
        createPicture(destPath, fileName, text, color);
    }

    public void createPicture(File destPath, String fileName, String text, String color) {
        try {
            text = Translate.decode(text);
            text = java.net.URLEncoder.encode(text, "UTF-8");
            if (destPath == null) destPath = new File(".");
            if (destPath != null) destPath.mkdirs();
            if (new File(destPath, fileName).exists()) System.out.println("WARNING FILE EXISTS: " + destPath + File.separator + fileName);
            String userProfileDir = (System.getenv().get("APPDATA") != null) ? System.getenv().get("APPDATA") : ".";
            File userSettingsDir = new File(userProfileDir, "fma" + File.separator + "imageGenerator");
            userSettingsDir.mkdirs();
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(destPath, fileName)), 1024 * 250);
            BufferedOutputStream out_control = new BufferedOutputStream(new FileOutputStream(new File(userSettingsDir, destPath.getPath().replace(File.separatorChar, '_') + "_" + fileName)), 1024 * 250);
            URL url = new URL("http://www.porsche.com/ImageMachines/CCTitles.aspx/" + fileName + "?text=" + text + "&mode=" + color);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("ISO-8859-1")));
            int inputLine;
            while ((inputLine = in.read()) != -1) {
                out.write(inputLine);
                out_control.write(inputLine);
            }
            out.flush();
            out_control.flush();
            out.close();
            out_control.close();
            out = null;
            out_control = null;
            in = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
