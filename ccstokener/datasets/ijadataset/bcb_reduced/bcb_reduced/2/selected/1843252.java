package axb.sys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

public class FileOper {

    public static String[] getFiles() {
        File dir = new File(".");
        String[] children = dir.list();
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.startsWith("setting.") | name.startsWith("artifacts.") | name.startsWith("DL_")) return false;
                return name.endsWith(".xml");
            }
        };
        children = dir.list(filter);
        for (int i = 0; i < children.length; i++) {
            children[i] = children[i].substring(0, children[i].indexOf("."));
        }
        return children;
    }

    public static boolean findFile(String filename) {
        File dir = new File(".");
        String[] children = dir.list();
        for (int i = 0; i < children.length; i++) {
            if (children[i].equals(filename)) return true;
        }
        return false;
    }

    /**
	 * ����ļ�·��
	 * 
	 * @param bundleID
	 * @param entry
	 * @return
	 */
    public static String getRealPath(String bundleID, String path) {
        if (path == null || path.equals("")) return "";
        char p = path.charAt(0);
        if (p == 'C' || p == 'D' || p == 'E' || p == 'F' || p == 'G' || p == 'H' || p == 'A') {
            return path;
        }
        URL urlentry = null;
        String strEntry = "";
        try {
            Bundle bundle = Platform.getBundle(bundleID);
            urlentry = bundle.getEntry("");
            strEntry = FileLocator.toFileURL(urlentry).getPath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return strEntry + path;
    }

    /**
	 * ��ð�װ·��
	 * 
	 * @param bundleID
	 * @param path
	 * @return
	 */
    public static String getInstallPath() {
        String strEntry = null;
        String installPath = null;
        try {
            Location installLoc = LocationManager.getInstallLocation();
            if (installLoc != null) {
                URL installURL = installLoc.getURL();
                strEntry = installURL.getPath();
            }
            installPath = strEntry.substring(1, strEntry.length());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return installPath;
    }

    public static URL getInstallURL() {
        String strEntry = null;
        Location installLoc = LocationManager.getInstallLocation();
        URL installURL = installLoc.getURL();
        return installURL;
    }

    public static void writeMethod1(String fileName) {
        fileName = getInstallURL().getPath() + fileName;
        System.out.println(fileName);
        try {
            FileWriter writer = new FileWriter(fileName);
            writer.write("Hello Kuka:\n");
            writer.write("  My name is coolszy!\n");
            writer.write("  I like you and miss you��");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * ʹ��FileWriter�����ı��ļ���׷����Ϣ
	 */
    public static void writeMethod2(String content) {
        String fileName = getInstallURL().getPath() + "log_" + ".txt";
        try {
            FileWriter writer = new FileWriter(fileName, true);
            SimpleDateFormat format = new SimpleDateFormat();
            String time = format.format(new Date());
            writer.write("\n\t" + time + "         " + content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * ʹ��BufferedWriter��д�ı��ļ�
	 */
    public static void writeMethod3(String fileName) {
        fileName = getInstallURL().getPath() + fileName;
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName), 1);
            out.write("Hello Kuka:");
            out.newLine();
            out.write("  My name is coolszy!\n");
            out.write("  I like you and miss you��");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * ʹ��FileReader����ı��ļ�
	 */
    public static void readMethod1() {
        String fileName = "C:/kuka.txt";
        int c = 0;
        try {
            FileReader reader = new FileReader(fileName);
            c = reader.read();
            while (c != -1) {
                System.out.print((char) c);
                c = reader.read();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * ʹ��BufferedReader����ı��ļ�
	 */
    public static void readMethod2() {
        String fileName = "c:/kuka.txt";
        String line = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            line = in.readLine();
            while (line != null) {
                System.out.println(line);
                line = in.readLine();
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Image getImage(String filename) {
        Image image = null;
        return image;
    }

    /**
	 * �����ļ�������
	 * 
	 * @param urlString
	 *            �����ص��ļ���ַ
	 * @param filename
	 *            �����ļ���
	 * @throws Exception
	 *             �����쳣
	 */
    public static void download(String urlString, String filename) throws Exception {
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        InputStream is = con.getInputStream();
        byte[] bs = new byte[1024];
        int len;
        OutputStream os = new FileOutputStream(filename);
        while ((len = is.read(bs)) != -1) {
            os.write(bs, 0, len);
        }
        os.close();
        is.close();
    }
}
