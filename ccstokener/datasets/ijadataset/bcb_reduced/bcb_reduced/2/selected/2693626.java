package cn.fwso.extract;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author James Tang
 *
 */
public class DownloadFile {

    private String saveDir = null;

    private File inputFile = null;

    private ArrayList<String> downlist = null;

    public DownloadFile(File inputFile, String saveDir) {
        this.inputFile = inputFile;
        this.saveDir = saveDir;
    }

    public void process() {
        this.downlist = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.inputFile));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    this.downlist.add(line);
                }
            }
            br.close();
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            int counts = 0;
            for (String f : this.downlist) {
                String filename = this.saveDir + "\\" + f.substring(f.lastIndexOf("/") + 1);
                URL url = new URL(f);
                bis = new BufferedInputStream(url.openStream());
                bos = new BufferedOutputStream(new FileOutputStream(filename));
                byte[] bytes = new byte[128];
                int len = 0;
                while ((len = bis.read(bytes)) > 0) {
                    bos.write(bytes, 0, len);
                }
                bis.close();
                bos.flush();
                bos.close();
                counts++;
                System.out.println("下载完成[" + counts + "]" + f);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DownloadFile df = new DownloadFile(new File("D:\\tmp\\photos.txt"), "D:\\tmp");
        df.process();
    }
}
