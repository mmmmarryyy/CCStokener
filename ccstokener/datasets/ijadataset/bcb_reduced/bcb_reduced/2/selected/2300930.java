package portablepgp.core.hkp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import portablepgp.core.KeyRing;

/**
 *
 * @author Primiano Tucci - http://www.primianotucci.com/
 */
public class HKPKeySearch {

    public static Collection<SearchKeyResult> searchKey(String iText, String iKeyServer) throws Exception {
        Vector<SearchKeyResult> outVec = new Vector<SearchKeyResult>();
        String uri = iKeyServer + "/pks/lookup?search=" + URLEncoder.encode(iText, "UTF-8");
        URL url = new URL(uri);
        BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
        Pattern regex = Pattern.compile("pub.*?<a\\s+href\\s*=\"(.*?)\".*?>\\s*(\\w+)\\s*</a>.*?(\\d+-\\d+-\\d+).*?<a\\s+href\\s*=\".*?\".*?>\\s*(.+?)\\s*</a>", Pattern.CANON_EQ);
        String line;
        while ((line = input.readLine()) != null) {
            Matcher regexMatcher = regex.matcher(line);
            while (regexMatcher.find()) {
                String id = regexMatcher.group(2);
                String downUrl = iKeyServer + regexMatcher.group(1);
                String downDate = regexMatcher.group(3);
                String name = HTMLDecoder.decodeHTML(regexMatcher.group(4));
                outVec.add(new SearchKeyResult(id, name, downDate, downUrl));
            }
        }
        input.close();
        return outVec;
    }

    public static PGPPublicKeyRing importRemoteKey(String iUrl) throws Exception {
        URL url = new URL(iUrl);
        InputStream is = url.openStream();
        PGPPublicKeyRing outKey = KeyRing.importPublicKey(is);
        is.close();
        return outKey;
    }
}
