package iwork.icrafter.uigen;

import java.io.*;
import java.net.*;
import java.security.cert.Certificate;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.jar.*;
import org.w3c.dom.*;
import iwork.state.*;
import iwork.icrafter.uigen.*;
import iwork.icrafter.util.*;

public class TemplateCache extends GeneratorCache {

    public TemplateCache() throws CacheException {
        super();
    }

    protected Element downloadAndVerify(Element gElem) throws CacheException {
        try {
            String url = XMLHelper.GetChildText(gElem, "originalLocation");
            String id = XMLHelper.GetChildText(gElem, "id");
            URLConnection urlC = new URL(url).openConnection();
            String gElemStr = XMLHelper.ToString(gElem);
            int index = gElemStr.indexOf("</generator>");
            String cachedFileName = downloadInternal(urlC);
            gElemStr = gElemStr.substring(0, index) + "<location>" + cachedFileName + "</location>" + gElemStr.substring(index);
            index = gElemStr.indexOf("</generator>");
            gElemStr = gElemStr.substring(0, index) + "<downloadTime>" + System.currentTimeMillis() + "</downloadTime>" + gElemStr.substring(index);
            genHash.put(id, gElemStr);
            writeFile(genHash, genFileName);
            return XMLHelper.GetRootElement(gElemStr);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    protected String downloadInternal(URLConnection connection) throws IOException, CertificateException {
        boolean success = false;
        InputStream in = connection.getInputStream();
        JarInputStream jarIn = new JarInputStream(in);
        if (jarIn == null) {
            Utils.debug("GeneratorCache", "***jarInput stream null");
            return null;
        }
        Utils.debug("GeneratorCache", "CREATED JAR INPUT STREAM");
        JarEntry gen = jarIn.getNextJarEntry();
        while (gen != null) {
            String genName = gen.getName();
            Utils.debug("GeneratorCache", "Jar entry name: " + genName);
            if (genName.startsWith("META-INF")) {
                continue;
            }
            File localGenFile = new File(remoteDownloadDir, genName);
            if (localGenFile.exists()) {
                return "";
            }
            byte[] genBytes = new byte[1000];
            FileOutputStream outF = new FileOutputStream(localGenFile);
            int numRead = jarIn.read(genBytes);
            while (numRead != -1) {
                outF.write(genBytes, 0, numRead);
                numRead = jarIn.read(genBytes);
            }
            outF.close();
            Utils.debug("GeneratorCache", "Extracted file: " + localGenFile.getName());
            java.security.cert.Certificate[] certs = gen.getCertificates();
            if (certs == null) {
                localGenFile.delete();
                Utils.debug("GeneratorCache", "Certificates null!");
                continue;
            }
            Utils.debug("GeneratorCache", "Non-null certs");
            for (int j = 0; j < certs.length; j++) {
                PublicKey pubKey = certs[j].getPublicKey();
                if (pubKey == null) {
                    Utils.debug("GeneratorCache", "Pubkey from cert null!");
                    continue;
                }
                try {
                    cert.verify(pubKey);
                    success = true;
                    return localGenFile.getAbsolutePath();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            localGenFile.delete();
            gen = jarIn.getNextJarEntry();
        }
        return null;
    }
}
