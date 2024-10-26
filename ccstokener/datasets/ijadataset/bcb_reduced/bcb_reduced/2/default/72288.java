import java.io.*;
import java.net.*;

class FTPClientConnection {

    public final String host;

    public final String user;

    protected final String password;

    protected URLConnection urlc;

    public FTPClientConnection(String _host, String _user, String _password) {
        host = _host;
        user = _user;
        password = _password;
        urlc = null;
    }

    protected URL makeURL(String targetfile) throws MalformedURLException {
        if (user == null) return new URL("ftp://" + host + "/" + targetfile + ";type=i"); else return new URL("ftp://" + user + ":" + password + "@" + host + "/" + targetfile + ";type=i");
    }

    protected InputStream openDownloadStream(String targetfile) throws Exception {
        URL url = makeURL(targetfile);
        urlc = url.openConnection();
        InputStream is = urlc.getInputStream();
        return is;
    }

    protected OutputStream openUploadStream(String targetfile) throws Exception {
        URL url = makeURL(targetfile);
        urlc = url.openConnection();
        OutputStream os = urlc.getOutputStream();
        return os;
    }

    protected void close() {
        urlc = null;
    }
}
