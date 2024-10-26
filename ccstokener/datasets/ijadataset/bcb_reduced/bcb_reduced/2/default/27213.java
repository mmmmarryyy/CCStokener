import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.*;

/**
* Downloads updated datafiles from the web.
* @author Jim
* @version 2
*/
public class UpdateGrabber {

    /**
					* Starts the whole thing.
					*/
    public UpdateGrabber(int i) {
        try {
            grab(i);
            unzip();
            cleanup();
        } catch (Exception e) {
        }
    }

    /**
					* Removes downloaded ZIP file, and then exits and tells to restart the client.
					*/
    public static void cleanup() throws IOException {
        System.out.println("Cleaning up...");
        String j = (new StringBuilder()).append(dir).append("data.zip").toString();
        boolean success = (new File(j)).delete();
        System.out.println("Update completed successfully, please restart the game.");
        System.exit(0);
    }

    /**
					* Unzips files in the recently downloaded archive.
					*/
    public static void unzip() throws IOException {
        System.out.println("Extracting cache...");
        ZipFile zipfile = new ZipFile(new File((new StringBuilder()).append(dir).append("data.zip").toString()));
        Enumeration enumeration = zipfile.entries();
        do {
            if (!enumeration.hasMoreElements()) break;
            ZipEntry zipentry = (ZipEntry) enumeration.nextElement();
            DataInputStream datainputstream = new DataInputStream(zipfile.getInputStream(zipentry));
            byte abyte0[] = new byte[(int) zipentry.getSize()];
            datainputstream.readFully(abyte0);
            String s = (new StringBuilder()).append(dir).append(zipentry.getName()).toString();
            if (zipentry.isDirectory()) {
                File file = new File(s);
                file.mkdir();
            } else {
                File file1 = new File(s);
                file1.createNewFile();
                DataOutputStream dataoutputstream = new DataOutputStream(new FileOutputStream(file1));
                dataoutputstream.write(abyte0);
                CRC32 crc32 = new CRC32();
                crc32.update(abyte0);
                long l = crc32.getValue();
                long l1 = zipentry.getCrc();
                if (l != l1) {
                    System.out.println((new StringBuilder()).append("CRCs differing for ").append(zipentry.getName()).toString());
                    System.out.println("May have been tampered with!");
                }
            }
        } while (true);
    }

    /**
					* Finds folder to put the junk in.
					*/
    public static final String findcachedir() {
        String as[] = { "c:/windows/", "c:/winnt/", "d:/windows/", "d:/winnt/", "e:/windows/", "e:/winnt/", "f:/windows/", "f:/winnt/", "c:/", "~/", "/tmp/", "", "c:/rscache", "/rscache" };
        String s = "sourmud";
        for (int i = 0; i < as.length; i++) try {
            String s1 = as[i];
            if (s1.length() > 0) {
                File file = new File(s1);
                if (!file.exists()) continue;
            }
            File file1 = new File(s1 + s);
            if (file1.exists() || file1.mkdir()) return s1 + s + "/";
        } catch (Exception _ex) {
        }
        return null;
    }

    /**
					* Downloads the zip file from updates.sourmud.jwarez.net.
					*/
    public static void grab(int kz) {
        File file = new File((new StringBuilder()).append(dir).append("data.zip").toString());
        try {
            (new File(dir)).mkdir();
            file.createNewFile();
            System.out.println("Downloading updates...");
            URL url = new URL("http://updates.sourmud.jwarez.net/update" + kz + ".zip");
            DataInputStream datainputstream = new DataInputStream(url.openStream());
            FileOutputStream fileoutputstream = new FileOutputStream(file);
            byte abyte0[] = new byte[0x100000];
            boolean flag = false;
            int i;
            int j;
            for (j = 0; (i = datainputstream.read(abyte0)) > -1; j += i) {
                fileoutputstream.write(abyte0, 0, i);
                fileoutputstream.flush();
            }
            fileoutputstream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
					* Directory to use.
					*/
    static String dir;

    static {
        dir = (new StringBuilder()).append(findcachedir()).toString();
    }
}
