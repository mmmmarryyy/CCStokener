package com.intel.gpe.services.jms.common.ds;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import com.intel.gpe.services.jms.common.StageInClient;
import com.intel.gpe.services.jms.common.StageOutClient;
import com.intel.gpe.services.tss.common.CommonTargetSystemResource;
import com.intel.gpe.tsi.common.File;
import com.intel.gpe.tsi.common.User;

/**
 * @author Alexander Lukichev
 * @version $Id: HTTPTransferClient.java,v 1.4 2006/12/13 11:43:09 dnpetrov Exp $
 */
public class HTTPTransferClient implements StageInClient, StageOutClient {

    public void fetchFile(User user, Object securitySetup, URI uri, File file, CommonTargetSystemResource tsr) throws Exception {
        URL url = new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath());
        URLConnection connection = url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        InputStream is = connection.getInputStream();
        long read = 0;
        int s = 0;
        int chunk = 16384;
        byte[] buf = new byte[chunk];
        while (s != -1) {
            s = is.read(buf, 0, chunk);
            tsr.putChunk(user, file, read == 0, buf, read, s);
            read += s;
        }
        is.close();
        tsr.changeOwner(user, file, user);
    }

    public boolean supports(String scheme) {
        return scheme.equals("http");
    }

    public void putFile(User user, Object securitySetup, File file, URI uri, CommonTargetSystemResource tsr) throws Exception {
        URL url = new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath());
        URLConnection connection = url.openConnection();
        connection.setDoInput(false);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        int chunk = 16384;
        long ofs = 0;
        while (true) {
            byte[] buf = tsr.getChunk(user, file, ofs, chunk);
            if (buf != null && buf.length > 0) {
                os.write(buf, 0, buf.length);
                ofs += buf.length;
            } else {
                break;
            }
        }
        os.close();
    }
}
