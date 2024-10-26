package net.sf.tomcatdeployer.service.client;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import net.sf.tomcatdeployer.service.HelperService;

public class WrappedHelperService implements HelperService {

    public WrappedHelperService(HelperService svc, String url) {
        this.svc = svc;
        this.url = url;
    }

    public String checkSum(String context, String file) {
        return svc.checkSum(context, file);
    }

    public List listFiles(String context) {
        return svc.listFiles(context);
    }

    public boolean reloadContext(String context) {
        return svc.reloadContext(context);
    }

    public boolean removeFile(String context, String path) {
        return svc.removeFile(context, path);
    }

    public boolean saveAs(String context, String path, InputStream source) {
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) (new URL(url)).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream os = conn.getOutputStream();
            HessianOutput hso = new HessianOutput(os);
            hso.startCall("saveAs");
            hso.writeString(context);
            hso.writeString(path);
            hso.writeByteBufferStart();
            byte buf[] = new byte[1024];
            do {
                int read = source.read(buf);
                if (read == -1) break;
                if (read != 0) hso.writeByteBufferPart(buf, 0, read);
            } while (true);
            hso.writeByteBufferEnd(buf, 0, 0);
            hso.completeCall();
            hso.flush();
            hso.close();
            os.close();
            boolean result;
            InputStream is = conn.getInputStream();
            HessianInput hsi = new HessianInput(is);
            hsi.startReply();
            result = hsi.readBoolean();
            hsi.completeReply();
            return result;
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean touchFile(String context, String path) {
        return svc.touchFile(context, path);
    }

    public Date getServerTime() {
        return svc.getServerTime();
    }

    public boolean mkdir(String context, String path) {
        return svc.mkdir(context, path);
    }

    private String url;

    private HelperService svc;
}
