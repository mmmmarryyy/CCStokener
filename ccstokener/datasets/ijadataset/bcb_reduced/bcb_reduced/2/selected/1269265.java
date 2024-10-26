package com.elibera.ccs.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.swing.JOptionPane;
import com.elibera.ccs.app.ApplicationParams;
import com.elibera.ccs.app.ContentPackageEditor;
import com.elibera.ccs.app.MLEeditorApplet;
import com.elibera.ccs.app.Post;
import com.elibera.ccs.res.Msg;
import com.elibera.ccs.util.Base64;
import com.elibera.ccs.util.HelperStd;
import com.elibera.util.scorm.ContentPackage;

/**
 * @author meisi
 *
 */
public class HelperContentPackage extends com.elibera.util.scorm.HelperContentPackage {

    /**
	 * std request values for the platform server request
	 * @param hc
	 * @param applet
	 */
    public static void setStdRequestHeadersForPlatformServer(HttpURLConnection hc, ApplicationParams applet) {
        String cookie = applet.getParameter("cookie");
        if (cookie != null) hc.setRequestProperty("Cookie", cookie);
    }

    /**
	 * Lädt das Zip-File vom Server (anhand der Server Daten im Lernobjekt) und
	 * befüllt das ContentPackage mit dem Inhalt aus dem Zip-File
	 * @param conf
	 * @param lernobjekt
	 */
    public static void retrieveLernobjektFormServer(InterfaceDocContainer conf, ContentPackage lernobjekt) {
        try {
            byte[] zipData = getPackageFromServer(conf, lernobjekt.getId(), lernobjekt.getUsername(), lernobjekt.getPass(), lernobjekt);
            System.out.println(zipData + ":" + (zipData != null ? new String(zipData) : ""));
            if (zipData != null && zipData.length > 10) retrieveLernobjekt(lernobjekt, zipData);
        } catch (Exception e) {
            System.out.println("retrieveLernobjektFormServer");
            JOptionPane.showMessageDialog(conf.getEditorPanel(), Msg.getString("HelperContentPackage.RETRIEVE_LO_FROM_SERVER_ERROR_UNKOWN") + e.getMessage(), Msg.getMsg("WORD_ERROR"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * lädt das Lernobjekt vom Server herunter
	 * @param conf
	 * @param id
	 * @param username
	 * @param pass
	 * @return
	 */
    public static byte[] getPackageFromServer(InterfaceDocContainer conf, long id, String username, String pass, ContentPackage lernobjekt) {
        HttpURLConnection hc = null;
        try {
            if (HelperStd.isEmpty(conf.getMLEConfig().serverUrlOpenLernobjekt)) return new byte[0];
            URL server = new URL(conf.getMLEConfig().serverUrlOpenLernobjekt + '?' + MLEeditorApplet.PARAM_EDITOR_CONTENT_ID + '=' + id + '&' + MLEeditorApplet.PARAM_EDITOR_USERNAME + '=' + URLEncoder.encode(username, "UTF-8") + '&' + MLEeditorApplet.PARAM_EDITOR_PASS + '=' + URLEncoder.encode(pass, "UTF-8"));
            hc = (HttpURLConnection) server.openConnection();
            setStdRequestHeadersForPlatformServer(hc, conf.getMLEConfig().appParams);
            hc.connect();
            conf.getMLEConfig().setSecRules(hc.getHeaderField(MLEeditorApplet.PARAM_EDITOR_SEC_RULES));
            InputStream in = hc.getInputStream();
            byte[] b = Post.getResultFromHTTPInputStream(in);
            in.close();
            if (b != null) return b;
            return new byte[0];
        } catch (Exception e) {
            System.out.println("getPackageFromServer");
            lernobjekt.getError().append("error-getPackageFromServer:" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (hc != null) hc.disconnect();
        }
        return new byte[0];
    }

    /**
	 * speichert das Lernobjekt auf den Server ab
	 * @param conf
	 * @param lernobjekt
	 */
    public static void saveLernobjektToServer(InterfaceDocContainer conf, ContentPackageEditor lernobjekt, int pages) {
        try {
            System.out.println("create package");
            byte[] data = createContentPackageFile(lernobjekt);
            System.out.println("POSTbINARY:" + data.length);
            System.out.println(Base64.encode(data).length);
            String lang = lernobjekt.getLang();
            if (lang == null) lang = Msg.stdLoc.getLanguage();
            String[] params = { MLEeditorApplet.PARAM_EDITOR_CONTENT_ID, MLEeditorApplet.PARAM_EDITOR_USERNAME, MLEeditorApplet.PARAM_EDITOR_PASS, MLEeditorApplet.PARAM_EDITOR_TITLE, MLEeditorApplet.PARAM_EDITOR_DESC, MLEeditorApplet.PARAM_EDITOR_LANG, MLEeditorApplet.PARAM_EDITOR_LO_FEATURES, MLEeditorApplet.PARAM_EDITOR_LO_VERSION, MLEeditorApplet.PARAM_EDITOR_LO_PAGES, MLEeditorApplet.PARAM_EDITOR_EBOOK, MLEeditorApplet.PARAM_EDITOR_CONTENT };
            byte[][] values = { new Long(lernobjekt.getId()).toString().getBytes(), lernobjekt.getUsername().getBytes(), lernobjekt.getPass().getBytes(), lernobjekt.getTitelServer().getBytes(), lernobjekt.getDesc().getBytes(), lang.getBytes(), lernobjekt.getFeatures().getBytes(), (lernobjekt.getVersion() + "").getBytes(), (pages + "").getBytes(), (lernobjekt.ebook ? "true" : "false").getBytes(), data };
            String[] contentTypes = { null, null, null, null, null, null, null, null, null, null, "application/zip" };
            StringBuffer ret = new StringBuffer();
            if (!Post.post(params, values, contentTypes, conf.getMLEConfig().serverUrlSaveLernobjekt, ret, conf.getMLEConfig().appParams)) {
                JOptionPane.showMessageDialog(conf.getEditorPanel(), Msg.getString("HelperContentPackage.RETRIEVE_SEND_LO_TO_SERVER_ERROR_UNKOWN") + ret.toString(), Msg.getMsg("WORD_ERROR"), JOptionPane.ERROR_MESSAGE);
            }
            String error = ret.toString();
            System.out.println("Server return:" + error);
            if (error.toLowerCase().compareTo("ok") == 0 || error.compareTo("1") == 0 || error.toLowerCase().compareTo("true") == 0) {
                JOptionPane.showMessageDialog(conf.getEditorPanel(), Msg.getString("HelperContentPackage.RETRIEVE_SEND_LO_TO_SERVER_SUCCESS"), Msg.getString("HelperContentPackage.RETRIEVE_SEND_LO_TO_SERVER_SUCCESS_TITEL"), JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(conf.getEditorPanel(), Msg.getString("HelperContentPackage.RETRIEVE_SEND_LO_TO_SERVER_ERROR_UNKOWN") + ret.toString(), Msg.getMsg("WORD_ERROR"), JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            System.out.println("saveLernobjektToServer");
            lernobjekt.getError().append("error-saveLernobjektToServer:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void setDefaultValuesForLearningObject(ContentPackageEditor lernobjekt, InterfaceDocContainer conf) {
        ApplicationParams applet = conf.getMLEConfig().appParams;
        lernobjekt.ebook = HelperXMLParserSimple.isXMLAttributValueTrue(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_EBOOK));
        lernobjekt.setId(HelperStd.parseLong(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_CONTENT_ID), 0));
        lernobjekt.setTitel(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_TITLE, null));
        lernobjekt.setTitelServer(lernobjekt.getTitel());
        lernobjekt.setDesc(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_DESC));
        if (!HelperStd.isEmpty(lernobjekt.getDesc())) lernobjekt.setDesc(lernobjekt.getDesc().replace('\n', '\r'));
        lernobjekt.setUserid(HelperStd.parseLong(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_USERID), -1));
        lernobjekt.setLang(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_LANG));
        lernobjekt.setUsername(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_USERNAME));
        lernobjekt.setPass(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_PASS));
        lernobjekt.setKName(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_KNAME));
        lernobjekt.setLang(Msg.stdLoc.getLanguage());
        System.out.println("loc:" + Msg.stdLoc.getLanguage() + ":" + applet.getParameter(MLEeditorApplet.PARAM_EDITOR_LANG));
        lernobjekt.setVersion(HelperStd.parseInt(applet.getParameter(MLEeditorApplet.PARAM_EDITOR_LO_VERSION), lernobjekt.getVersion()));
        lernobjekt.urlMetaMLEBewerten = applet.getParameter(MLEeditorApplet.PARAM_EDITOR_META_MLE_BEWERTEN);
        lernobjekt.urlMetaMLEDetails = applet.getParameter(MLEeditorApplet.PARAM_EDITOR_META_MLE_DETAIL);
        lernobjekt.urlMetaMLEUser = applet.getParameter(MLEeditorApplet.PARAM_EDITOR_META_MLE_USER);
        lernobjekt.urlMetaMLERefresh = applet.getParameter(MLEeditorApplet.PARAM_EDITOR_META_MLE_REFRESH);
    }

    /**
	 * setzt die Applet Params und holt das LO vom Server und gibt es zurück
	 * @param conf
	 * @param lernobjekt
	 * @param appParams
	 * @return
	 */
    public static ContentPackageEditor getLernobjektApplet(InterfaceDocContainer conf) {
        ContentPackageEditor lernobjekt = new ContentPackageEditor();
        try {
            setDefaultValuesForLearningObject(lernobjekt, conf);
            HelperContentPackage.retrieveLernobjektFormServer(conf, lernobjekt);
        } catch (Exception e) {
            System.out.println("Mled init-getLernobjektApplet:" + e.getMessage());
            e.printStackTrace();
        }
        return lernobjekt;
    }

    /**
	 * öffnet das Lernobjekt aus dem JAR File
	 * @param conf
	 * @param localFile
	 * @return
	 */
    public static ContentPackageEditor openJARLernobjekt(String localJarFile, InterfaceDocContainer conf) {
        ContentPackageEditor lernobjekt = new ContentPackageEditor();
        try {
            setDefaultValuesForLearningObject(lernobjekt, conf);
            byte[] b = readJARFile(localJarFile);
            HelperContentPackage.retrieveLernobjekt(lernobjekt, b);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(Msg.getString("HelperContentPackage.31") + e.getMessage());
        }
        return lernobjekt;
    }

    /**
	 * liest ein File aus dem JAR File aus und gibt es zurück
	 * @param localJarFile
	 * @return
	 */
    public static byte[] readJARFile(String localJarFile) {
        try {
            InputStream is = System.class.getResourceAsStream(localJarFile);
            byte[] b = new byte[is.available()];
            is.read(b);
            return b;
        } catch (Exception e) {
            System.out.println("readJARFile:" + e.getMessage() + "," + localJarFile);
        }
        return new byte[0];
    }

    public static String readHTMLFile(String url) {
        StringBuffer ct = new StringBuffer();
        StringBuffer enc = new StringBuffer();
        byte[] data = readHTTPFile(url, ct, enc);
        if (ct.length() > 0) {
            try {
                return new String(data, ct.toString());
            } catch (Exception e) {
            }
        }
        return new String(data);
    }

    public static byte[] readHTTPFile(String url, StringBuffer contentType, StringBuffer encoding) {
        try {
            URL u = new URL(url);
            URLConnection urlConn = u.openConnection();
            urlConn.setReadTimeout(10 * 1000);
            urlConn.setConnectTimeout(10 * 1000);
            urlConn.setDoInput(true);
            urlConn.setDoOutput(false);
            String status = urlConn.getHeaderField(null).toLowerCase();
            String location = urlConn.getHeaderField("Location");
            String cookie = urlConn.getHeaderField("Set-Cookie");
            int times = 0;
            while ((status.indexOf("http/1.1 3") >= 0 || status.indexOf("http/1.0 3") >= 0) && !HelperStd.isEmpty(location)) {
                if (!HelperStd.isEmpty(urlConn.getHeaderField("Set-Cookie"))) cookie = urlConn.getHeaderField("Set-Cookie");
                u = new URL(location);
                urlConn = u.openConnection();
                urlConn.setReadTimeout(10 * 1000);
                urlConn.setConnectTimeout(10 * 1000);
                urlConn.setDoInput(true);
                urlConn.setDoOutput(false);
                urlConn.setRequestProperty("Cookie", cookie);
                status = urlConn.getHeaderField(null).toLowerCase();
                location = urlConn.getHeaderField("Location");
                times++;
                if (times > 10) break;
            }
            System.out.println(urlConn.getHeaderField(null) + ":" + urlConn.getContentLength() + ":" + u);
            if (contentType != null) contentType.append(urlConn.getContentType());
            if (encoding != null) {
                String enc = null, ct = urlConn.getContentType();
                if (ct != null && ct.indexOf("charset=") > 0) {
                    int a = ct.indexOf("charset=") + "charset=".length();
                    enc = ct.substring(a);
                }
                if (enc == null) enc = urlConn.getContentEncoding();
                if (enc == null) enc = "ISO-8859-1";
                encoding.append(enc);
            }
            BufferedInputStream in = new BufferedInputStream(urlConn.getInputStream());
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int read = 0;
            while (read != -1) {
                read = in.read(b);
                if (read > 0) bout.write(b, 0, read);
            }
            in.close();
            System.out.println(bout.size());
            return bout.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("readHTTPFile:" + e.getMessage() + "," + url);
        }
        return new byte[0];
    }
}
