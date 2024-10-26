package xmlsync2;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.w3c.dom.Node;

public class XmlPost {

    Node source = null;

    String url = "";

    String uid = "";

    String pwd = "";

    public XmlPost(Node node) {
        this.source = node;
    }

    public String run(final String url, final String uid, final String pwd) throws XmlsyncException {
        this.uid = uid;
        this.pwd = pwd;
        this.url = url;
        return post();
    }

    private String post() throws XmlsyncException {
        String result = "";
        try {
            URL url = new URL(this.url);
            String qry = URLEncoder.encode("sync") + "=" + URLEncoder.encode(XMLAPI.xml(source));
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            uc.setRequestMethod("POST");
            uc.setDoOutput(true);
            uc.setDoInput(true);
            uc.setUseCaches(false);
            DataOutputStream dos = new DataOutputStream(uc.getOutputStream());
            dos.writeBytes(qry);
            dos.flush();
            dos.close();
            InputStreamReader in = new InputStreamReader(uc.getInputStream());
            int chr = in.read();
            while (chr != -1) {
                result = result + (char) chr;
                chr = in.read();
            }
            in.close();
        } catch (Exception e) {
            throw new XmlsyncException("<br/>[[XmlPost.post] Exception] source : " + e.getMessage());
        }
        return result;
    }
}
