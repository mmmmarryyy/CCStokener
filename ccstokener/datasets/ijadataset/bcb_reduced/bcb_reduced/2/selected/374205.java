package ch.oblivion.comixviewer.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class SimplePageLoader implements IPageLoader {

    public SimplePageLoader() {
    }

    @Override
    public void loadData(URL url, OutputStream output) {
        try {
            System.out.print("Loading " + url + ": ");
            InputStream is = url.openStream();
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    int count = is.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    output.write(buffer, 0, count);
                    System.out.print("#");
                }
            } finally {
                System.out.println(" - finished");
                is.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
