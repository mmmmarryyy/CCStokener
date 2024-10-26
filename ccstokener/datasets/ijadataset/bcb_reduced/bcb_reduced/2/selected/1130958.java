package encodingconverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * 本类使用ICU4J包进行文档编码获取
 *
 * @author qxo(qxodream@gmail.com)
 */
public class EncodeDetector {

    /**
	 * 获取编码
	 *
	 * @throws IOException
	 * @throws Exception
	 */
    public static String getEncoding(byte[] data) {
        final CharsetDetector detector = new CharsetDetector();
        detector.setText(data);
        return getEncoding(detector);
    }

    public static String getEncoding(InputStream data) throws IOException {
        try {
            final CharsetDetector detector = new CharsetDetector();
            byte[] arrays = IOUtils.toByteArray(data);
            detector.setText(arrays);
            return getEncoding(detector);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            if ("mark/reset not supported".equals(ex.getMessage())) {
                byte[] arrays = IOUtils.toByteArray(data);
                return getEncoding(arrays);
            }
            throw ex;
        }
    }

    public static String getEncoding(File file) throws IOException {
        final String encoding = getEncoding(new FileInputStream(file));
        System.out.println("file:" + file + " encoding:" + encoding);
        return encoding;
    }

    public static String getEncoding(URL url) throws IOException {
        final String encoding = getEncoding(url.openStream());
        System.out.println("url:" + url + " encoding:" + encoding);
        return encoding;
    }

    public static String getEncoding(CharsetDetector detector) {
        CharsetMatch[] matches = detector.detectAll();
        if (matches == null || matches.length < 1) {
            return null;
        }
        System.out.println("All possibilities");
        CharsetMatch match = matches[0];
        String encoding = match.getName();
        int confidence = match.getConfidence();
        final Set firstMatchEncodings = new HashSet();
        final Set allEncodings = new HashSet();
        for (CharsetMatch m : matches) {
            final String charsetName = m.getName();
            if (m.getConfidence() >= confidence) {
                firstMatchEncodings.add(charsetName);
            }
            allEncodings.add(charsetName);
            System.out.println("CharsetName:" + charsetName + " Confidence:" + m.getConfidence());
        }
        if (firstMatchEncodings.contains("GB18030")) {
            encoding = "GB18030";
        } else if (firstMatchEncodings.contains("GBK")) {
            encoding = "GBK";
        } else if (confidence < 100 && !firstMatchEncodings.contains("UTF-8") && !firstMatchEncodings.contains("ISO-8859-1")) {
            if (allEncodings.contains("Big5")) {
                encoding = "Big5";
            }
        }
        return encoding;
    }
}
