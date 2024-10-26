package org.jomper.pluto;

import java.lang.Thread;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.File;
import java.io.IOException;
import org.jomper.pluto.SingleFileThreadWriter;
import org.jomper.pluto.ConnectionDigestHandler;
import org.jomper.pluto.ConnectionDigestHandlerDefaultImpl;
import org.jomper.pluto.model.ConnectionDigest;
import org.jomper.pluto.util.JomperXMLOperator;

public class SingleFileMultiThreadDownload {

    private URL url = null;

    private URLConnection ConnServer(String arg) {
        URLConnection uc = null;
        try {
            url = new URL(arg);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            uc = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uc;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        SingleFileMultiThreadDownload mtd = new SingleFileMultiThreadDownload();
        ConnectionDigestHandler cdh = new ConnectionDigestHandlerDefaultImpl();
        cdh.setConnectionDigest(mtd.ConnServer(args[0]), 5);
        ConnectionDigest connectionDigest = cdh.getConnectionDigest();
        JomperXMLOperator jxo = new JomperXMLOperator();
        jxo.setRootName("Contents");
        jxo.open(connectionDigest.getFileName() + ".xml");
        File file = new File(connectionDigest.getFileName());
        connectionDigest.setLocalPath(file.getAbsolutePath());
        if (!file.exists()) {
            jxo.add(connectionDigest);
            jxo.save();
        } else {
            cdh.getConnectionDigest(jxo, connectionDigest);
        }
        mtd.process(connectionDigest, jxo);
    }

    private void process(ConnectionDigest connectionDigest, JomperXMLOperator jxo) {
        int c = 1;
        int len = connectionDigest.getThreadsDigest().size();
        for (int i = 0; i < len; i++) {
            Thread thread = new Thread(new SingleFileThreadWriter(connectionDigest, c++, jxo));
            thread.start();
        }
    }
}
