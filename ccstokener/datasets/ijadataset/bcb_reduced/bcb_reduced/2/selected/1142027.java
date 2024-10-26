package jp.co.withone.osgi.gadget.picasaconnector;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import jp.co.withone.osgi.gadget.pictureviewer.PictureController;
import com.google.gdata.util.AuthenticationException;

public class PictureControllerImpl implements PictureController {

    PicasaConnector connector = null;

    int index = 0;

    int max = 0;

    public PictureControllerImpl(PicasaConnector connector) {
        this.connector = connector;
    }

    @Override
    public byte[] getPicture() {
        if (PicasaConnectorSettingView.userName == null || PicasaConnectorSettingView.password == null) {
            return null;
        }
        if (!this.connector.isAuthenticated) {
            try {
                this.connector.authenticate(PicasaConnectorSettingView.userName, PicasaConnectorSettingView.password);
            } catch (AuthenticationException e) {
                return null;
            }
        }
        if (this.connector.urlList == null) {
            List<URL> list = this.connector.getPhotosURLList();
            if (list == null) {
                return null;
            }
            this.connector.urlList = list;
            max = this.connector.urlList.size();
        }
        if (index >= max) {
            index = 0;
        }
        byte[] reBytes = getBytes(this.connector.urlList.get(index++));
        return reBytes;
    }

    private byte[] getBytes(URL url) {
        InputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = new BufferedOutputStream(baos);
        int c;
        try {
            is = url.openStream();
            while ((c = is.read()) != -1) {
                os.write(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return baos.toByteArray();
    }
}
