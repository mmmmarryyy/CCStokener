package rath.jmsn.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.ImageIcon;
import rath.msnm.util.StringUtil;

/**
 *
 * @author Kim, Min Jong, pistos@skypond.snu.ac.kr
 * @version $Id: Emoticon.java,v 1.2 2007/06/04 09:27:20 nevard Exp $, since 2002/03/24
 */
public class Emoticon {

    private Hashtable emoticons = new Hashtable();

    private static Emoticon INSTANCE = null;

    private Emoticon() {
    }

    public static Emoticon getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Emoticon();
            INSTANCE.loadEmoticons(Emoticon.class.getResource("/resources/text/emoticon.properties"));
        }
        return INSTANCE;
    }

    private void loadEmoticons(URL url) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0 || line.charAt(0) == '#') continue;
                int i0 = line.indexOf('=');
                if (i0 != -1) {
                    String key = line.substring(0, i0).trim();
                    String value = line.substring(i0 + 1).trim();
                    value = StringUtil.replaceString(value, "\\n", "\n");
                    URL eUrl = Emoticon.class.getResource("/resources/emoticon/" + value);
                    if (eUrl != null) emoticons.put(key, new ImageIcon(eUrl));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * 지원되는 Emoticon을 반환한다. 
	 * @return
	 */
    public Enumeration getEmoticons() {
        return emoticons.keys();
    }

    public ImageIcon get(String key) {
        return (ImageIcon) emoticons.get(key);
    }
}
