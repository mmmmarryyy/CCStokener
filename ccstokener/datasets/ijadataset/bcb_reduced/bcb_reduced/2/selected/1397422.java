package br.gov.ba.mam.gerente;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * Classe respons�vel por realizar o update da aplica��o. Faz download do
 * arquivo necess�rio e atualiza as bibliotecas.
 * 
 * @author cassio
 */
public class Update {

    /** Arquivo para update */
    private String urlUpdateFile = "http://mam-obras.googlecode.com/svn/trunk/deploy/build/lib/mam.jar?=last";

    /** Realiza a atualiza��o do software */
    public void atualizar(String proxy, int porta) {
        download(urlUpdateFile, "lib" + File.separator + "mam.jar", proxy, porta);
    }

    /** Faz download de arquivo */
    private void download(String address, String localFileName, String host, int porta) {
        InputStream in = null;
        URLConnection conn = null;
        OutputStream out = null;
        System.out.println("Update.download() BAIXANDO " + address);
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            if (host != "" && host != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, porta));
                conn = url.openConnection(proxy);
            } else {
                conn = url.openConnection();
            }
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            System.out.println(localFileName + "\t" + numWritten);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }
}
