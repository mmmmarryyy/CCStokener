package net.sf.brightside.stockswatcher.ticker.service.dynamic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import net.sf.brightside.stockswatcher.ticker.service.api.dynamic.ICreateFileWriter;
import net.sf.brightside.stockswatcher.ticker.service.api.dynamic.ITickerLoader;

public class TickerLoaderImpl implements ITickerLoader {

    private BufferedReader inTicker;

    private BufferedWriter outTicker;

    private ICreateFileWriter createFileWriter;

    private String inputLine;

    private URL url;

    public BufferedReader setUpReader() {
        BufferedReader reader = null;
        try {
            URLConnection conn = url.openConnection();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return reader;
    }

    public BufferedWriter setUpWriter() {
        BufferedWriter writer = null;
        try {
            FileWriter fw = createFileWriter.createFileWriter();
            writer = new BufferedWriter(fw);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return writer;
    }

    public void dragTickerOnLocal() {
        inTicker = this.setUpReader();
        outTicker = this.setUpWriter();
        try {
            while ((inputLine = inTicker.readLine()) != null) {
                outTicker.write(inputLine + "\n");
            }
            outTicker.flush();
            outTicker.close();
            inTicker.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ICreateFileWriter getCreateFileWriter() {
        return this.createFileWriter;
    }

    public void setCreateFileWriter(ICreateFileWriter fwCmd) {
        this.createFileWriter = fwCmd;
    }

    public URL getURL() {
        return this.url;
    }

    public void setURL(URL url) {
        this.url = url;
    }
}
