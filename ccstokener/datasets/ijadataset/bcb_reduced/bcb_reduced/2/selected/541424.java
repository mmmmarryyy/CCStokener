package apollo.datamodel.seq;

import java.util.*;
import java.io.IOException;
import java.io.*;
import java.lang.String;
import java.net.*;
import apollo.config.Config;
import apollo.util.HTMLUtil;

public class DataUtil {

    private DataUtil() {
    }

    public static int getLengthFromPFetch(String id) {
        try {
            Socket t = new Socket(Config.getPFetchServer(), 22100);
            DataOutputStream sos = new DataOutputStream(t.getOutputStream());
            String allArgs = new String();
            allArgs = "-l " + id + "\n";
            sos.write(allArgs.getBytes());
            DataInputStream is = new DataInputStream(t.getInputStream());
            boolean more = true;
            String finalStr = "";
            while (more) {
                String str = is.readLine();
                if (str == null) {
                    more = false;
                } else {
                    finalStr += str;
                }
            }
            return Integer.parseInt(finalStr);
        } catch (Exception e) {
            System.out.println("getLengthFromPFetch: error: " + e);
        }
        return 0;
    }

    public static int getLengthFromSRS(String id) {
        try {
            String host = Config.getSRSServer();
            int port = 80;
            String baseId = id;
            int dotInd;
            if ((dotInd = baseId.lastIndexOf('.')) > 0) {
                baseId = baseId.substring(0, dotInd);
            }
            String urlstr = "http://" + host + ":" + port + "/srs6bin/cgi-bin/wgetz?-f+SeqLength+-f+AccNumber+[embl-sv:\"" + id + "\"]|[embl-AccNumber:\"" + id + "\"]|[swall-AccNumber:\"" + baseId + "\"]";
            URL url = new URL(urlstr);
            URLConnection urlconn = url.openConnection();
            DataInputStream in = new DataInputStream(urlconn.getInputStream());
            String data;
            try {
                boolean hadId = false;
                while ((data = in.readLine()) != null) {
                    data = HTMLUtil.removeHtml(data);
                    if (data.indexOf("AC ") == 0) {
                        if (data.indexOf(baseId) > -1) {
                            hadId = true;
                        }
                    } else if (data.indexOf("SQ ") == 0 && hadId) {
                        hadId = false;
                        int startPos = data.indexOf("Sequence");
                        if (startPos == -1) startPos = data.indexOf("SEQUENCE");
                        startPos += "Sequence".length() + 1;
                        StringTokenizer tokenizer = new StringTokenizer(data.substring(startPos));
                        int length = Integer.parseInt(tokenizer.nextToken());
                        return length;
                    }
                }
            } catch (IOException ioex) {
                System.out.println("Exception " + ioex);
            }
            in.close();
        } catch (MalformedURLException ex) {
            System.out.println("Exception " + ex);
        } catch (IOException ioex) {
            System.out.println("Exception " + ioex);
        } catch (Exception ex) {
            System.out.println("Exception " + ex);
            ex.printStackTrace();
        }
        return 0;
    }
}
