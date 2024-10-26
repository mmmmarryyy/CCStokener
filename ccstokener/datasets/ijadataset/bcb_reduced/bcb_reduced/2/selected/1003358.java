package org.gdbi.db.pgvcgi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Vector;
import org.gdbi.api.GdbiConstants;
import org.gdbi.api.GdbiDebug;
import org.gdbi.api.GdbiIOException;
import org.gdbi.api.GdbiRecType;
import org.gdbi.api.GdbiUtilDebug;
import org.gdbi.api.GdbiXref;
import org.gdbi.util.debug.UDebugError;

/**
 * CGI access.
 */
public class CgiAccess implements GdbiConstants {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final String NAME_UNKNOWN = "<unknown>";

    private static final String STR_ERROR = "ERROR";

    private static final String STR_SUCCESS = "SUCCESS";

    private static final String STR_ERROR_CONNECT = STR_ERROR + " 12:";

    private static final String VAR_CHAR_SET = "CHARACTER_SET";

    private static final String VAR_GEDCOM = "GEDCOM";

    private static final String VAR_READ_ONLY = "READ_ONLY";

    private static final String VAR_ROOT_ID = "PEDIGREE_ROOT_ID";

    private static final String ERR_VERSION = "Version";

    private static final String ERR_CONNECT = "Connection";

    private static final String ERR_SET_GEDCOM = "Set GEDCOM";

    private static final GdbiUtilDebug udebug = new GdbiUtilDebug(new Debug());

    private final String baseURL;

    private final CgiFeatures features;

    private String sessionid = null;

    private boolean triedConnection = false;

    private String encoding = DEFAULT_ENCODING;

    private boolean readonly = false;

    private boolean badStatus;

    private String statusText;

    private String[] listgedcomVal;

    private String sessionName = "PHPSESSID";

    /**
     * This constructor is used by PgvcgiPreLogin to get the public info before
     * logging on.
     */
    public CgiAccess(String url) throws GdbiIOException {
        baseURL = url.trim();
        final String version = actionVersion();
        if (version == null) throw createIOException(ERR_VERSION);
        features = new CgiFeatures(version);
    }

    /**
     * This is the full access, using the info we got earlier from
     * PgvcgiPreLogin.
     */
    public CgiAccess(PgvcgiPreLogin prelogin, String user, String password, String gedname, boolean readonly) throws GdbiIOException {
        baseURL = prelogin.url.trim();
        features = prelogin.features;
        listgedcomVal = prelogin.dblist;
        final String errConnect = actionConnect(user, password, readonly);
        if (errConnect != null) throw createIOException(errConnect);
        if ((gedname != null) && (gedname.length() > 0)) {
            if (!userSetGedcom(gedname)) throw createIOException(ERR_SET_GEDCOM);
        }
        String enc = actionGetvar(VAR_CHAR_SET);
        if (enc != null) encoding = enc;
    }

    public String getBaseUrl() {
        return baseURL;
    }

    public CgiFeatures getCgiFeatures() {
        return features;
    }

    public boolean actionAppend(String xref, String gedrec) {
        return false;
    }

    public String actionConnect(String user, String passwd, boolean readonlyIn) {
        String param = urlParam("username", user) + urlParam("password", passwd);
        if (readonlyIn && features.has_read_only) param += urlParam("readonly", "1");
        String[] lines = readURL("connect", param);
        triedConnection = true;
        if ((lines == null) || (lines.length < 1)) return "Connect: No session ID";
        if (lines.length > 1) return "Connect: Extra lines after session ID";
        String line = lines[0];
        if (line.length() < 3) return "Connect: Bad session ID";
        sessionName = line.substring(0, line.indexOf("\t"));
        sessionid = line.substring(line.indexOf("\t") + 1);
        readonly = readonlyIn;
        if ((!readonly) && features.has_read_only) {
            final String roVal = actionGetvar(VAR_READ_ONLY);
            if ((roVal != null) && (roVal.equals("1"))) readonly = true;
        }
        return null;
    }

    public boolean actionDelete(String xref) {
        return readUrlSuccess("delete", "&xref=" + xref);
    }

    public String[] actionGet(String xref) {
        assert (xref.trim().length() > 0);
        return readURL("get", "&xref=" + xref);
    }

    public String[] actionGet(String xref, boolean keepfile) {
        assert (xref.trim().length() > 0);
        String params = "&xref=" + xref;
        if (keepfile) params = params + "&keepfile=1";
        return readURL("get", params);
    }

    public String actionGetvar(String name) {
        final String value = readUrlLine("getvar", "&var=" + name);
        udebug.dprintln("getvar: " + name + " = " + value);
        return value;
    }

    public String[] actionGetxref(String type, String position, String xref) {
        String param = "&type=" + type + "&position=" + position;
        if (xref != null) param += "&xref=" + xref;
        return readURL("getxref", param);
    }

    public String[] actionListgedcoms() {
        listgedcomVal = readURL("listgedcoms", null);
        return listgedcomVal;
    }

    public String[] actionSearch(String query) {
        return readURL("search", "&query=" + query);
    }

    public String[] actionSoundex(String firstname, String lastname) {
        UDebugError.not_implemented();
        return null;
    }

    public boolean actionUpdate(String xref, String gedrec) {
        udebug.dprintln("updating: " + xref);
        String param = urlParam("xref", xref) + urlParam("gedrec", gedrec);
        return readUrlSuccess("update", param);
    }

    public String actionVersion() {
        return readUrlLine("version", null);
    }

    public boolean actionUploadMedia(String mediafile, String thumbnail) {
        if (mediafile != null) udebug.dprintln("uploading mediafile: " + mediafile);
        if (thumbnail != null) udebug.dprintln("uploading thumbnail: " + thumbnail);
        return readURLUpload("uploadmedia", mediafile, thumbnail);
    }

    public String userNewXrefFam(GdbiXref xref) {
        return oneGetxref(TAG_FAM, "new", null);
    }

    public String userNewXrefIndi(GdbiXref xref) {
        return oneGetxref(TAG_INDI, "new", null);
    }

    public String userNewXrefObje(GdbiXref xref) {
        return oneGetxref(TAG_OBJE, "new", null);
    }

    public String userNewXrefNote(GdbiXref xref) {
        return oneGetxref(TAG_NOTE, "new", null);
    }

    public String userNewXrefRepo(GdbiXref xref) {
        return oneGetxref(TAG_REPO, "new", null);
    }

    public String userNewXrefSour(GdbiXref xref) {
        return oneGetxref(TAG_SOUR, "new", null);
    }

    public String userGetGedcom() {
        String name = actionGetvar(VAR_GEDCOM);
        if ((name == null) && (!features.has_anon_getvar)) {
            udebug.dprintln("cannot get name, using " + NAME_UNKNOWN);
            name = NAME_UNKNOWN;
        }
        return name;
    }

    public String userGetDefaultIndi() {
        return actionGetvar(VAR_ROOT_ID);
    }

    public String userGetFirstIndi() {
        return oneGetxref(TAG_INDI, "first", null);
    }

    public String userGetLastIndi() {
        return oneGetxref(TAG_INDI, "last", null);
    }

    public String userGetNextIndi(String xref) {
        return oneGetxref(TAG_INDI, "next", xref);
    }

    public String userGetPrevIndi(String xref) {
        return oneGetxref(TAG_INDI, "prev", xref);
    }

    public String[] userSearchName(String name) {
        if (name.indexOf("/") == -1) name += ".*/";
        String[] lines = actionSearch(" NAME .*" + name);
        return lines;
    }

    public boolean userSetGedcom(String gedname) {
        final String param = "&" + features.set_gedcom_variable + "=" + gedname;
        return (readURL(features.set_gedcom_command, param) != null);
    }

    /**
     * Returns true if getxref will work for this GdbiRecType.
     *
     * Error message for other types is:
     *     ERROR 18: Invalid $type specification.
     *     Valid types are INDI, FAM, SOUR, REPO, NOTE, OBJE, or OTHER
     */
    public static boolean canGetxref(GdbiRecType type) {
        boolean can = false;
        switch(type.id) {
            case GdbiRecType.ID_FAM:
            case GdbiRecType.ID_INDI:
            case GdbiRecType.ID_NOTE:
            case GdbiRecType.ID_OBJE:
            case GdbiRecType.ID_REPO:
            case GdbiRecType.ID_SOUR:
                can = true;
        }
        return can;
    }

    public boolean isReadOnly() {
        return readonly;
    }

    /**
     * Create a new GdbiIOException() with the latest PGV error message.
     * bug 1051609
     */
    public GdbiIOException createIOException(String errType) {
        String msg = "ERROR: " + errType + "\n\n";
        msg += "Status: " + statusText;
        if (listgedcomVal != null) {
            msg += "\n\nGEDCOMs:\n";
            for (int i = 0; i < listgedcomVal.length; i++) {
                msg += listgedcomVal[i].replace('\t', '-') + "\n";
            }
        }
        return new GdbiIOException(msg);
    }

    /**
     * Check for connection errors.
     */
    public boolean isValid() {
        if (statusText.indexOf(STR_ERROR_CONNECT) >= 0) return false;
        return true;
    }

    /**
     * Returns first line, usually from the output of readURL(),
     * when there is exactly one line.
     */
    private String getOnlyLine(String[] lines) {
        final String only = ((lines == null) || (lines.length != 1)) ? null : lines[0];
        udebug.dprintln("getOnlyLine: " + only);
        return only;
    }

    /**
     * Returns output for actions that return a single string.
     * @param action
     * @param param
     * @return output string or null
     */
    private String readUrlLine(final String action, final String param) {
        String[] lines = readURL(action, param);
        return getOnlyLine(lines);
    }

    private boolean readUrlSuccess(final String action, final String param) {
        String[] lines = readURL(action, param);
        return ((lines != null) && (lines.length == 0));
    }

    /**
     * Read output of URL, parse status line, and return rest of output.
     * @param action PGV command
     * @param param  key=val list separated with &
     * @return PUT output or null
     */
    private synchronized String[] readURL(final String action, String param) {
        assert ((param == null) || (param.trim().length() > 0));
        if (param == null) param = "";
        final String fullParam = "action=" + action + param + ((sessionid == null) ? "" : "&" + sessionName + "=" + sessionid);
        udebug.dprintln("PUT value: " + fullParam);
        if (udebug.getVerbose()) {
            udebug.vprintln("Who is making us slow down?\n\n" + "Stack trace -- NOT an error:\n");
            (new Exception()).printStackTrace();
            udebug.vprintln("\n");
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
        if ((sessionid == null) && (triedConnection)) return null;
        Vector<String> lines = new Vector<String>();
        boolean gotSuccess = false;
        badStatus = false;
        try {
            URL url = new URL(baseURL);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.print(fullParam);
            out.close();
            InputStreamReader in = new InputStreamReader(conn.getInputStream(), encoding);
            gotSuccess = readLines(in, lines);
            in.close();
            if (gotSuccess && (lines.size() > 0)) {
                String first = lines.get(0);
                if (first.indexOf(STR_ERROR) >= 0) gotSuccess = false;
            }
            if (!gotSuccess) {
                if (triedConnection || udebug.getDebug()) errorMessage(action, "status=" + statusText, fullParam);
                if (statusText.indexOf(STR_ERROR) < 0) {
                    if (udebug.getDebug()) {
                        udebug.dprintln("Bad error message: " + statusText);
                        udebug.dprintln("CGI output:");
                        final int length = Math.min(lines.size(), 50);
                        for (int i = 0; i < length; i++) udebug.dprintln(lines.get(i));
                        udebug.dprintln("\n");
                    }
                    badStatus = true;
                }
                return null;
            }
        } catch (IOException e) {
            statusText = "IO Error: " + e.getMessage();
            System.err.println(statusText);
            badStatus = true;
            return null;
        }
        return lines.toArray(new String[0]);
    }

    /**
     * Read loop helper for readURL().
     */
    private boolean readLines(InputStreamReader in, Vector<String> lines) throws IOException {
        boolean gotSuccess = false;
        final BufferedReader data = new BufferedReader(in);
        statusText = data.readLine();
        udebug.dprintln("status = " + statusText);
        if (statusText == null) statusText = STR_ERROR + ": missing CGI output";
        gotSuccess = STR_SUCCESS.equals(statusText.trim());
        String line;
        int length = 0;
        while ((line = data.readLine()) != null) {
            udebug.vprintln("line = " + line);
            lines.add(line);
            length += line.length();
        }
        String lstr = "" + length;
        if (length >= 10000) lstr = "" + (length + 500) / 1000 + "K";
        udebug.dprintln("readLines: success=" + gotSuccess + ", length=" + lstr);
        udebug.dprintln("\n");
        return gotSuccess;
    }

    private synchronized boolean readURLUpload(final String action, String mediafile, String thumbnail) {
        udebug.dprintln("url action=" + action);
        if (udebug.getVerbose()) {
            udebug.vprintln("Who is making us slow down?");
            (new Exception()).printStackTrace();
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
        if ((sessionid == null) && (triedConnection)) return false;
        Vector<String> lines = new Vector<String>();
        boolean gotSuccess = false;
        badStatus = false;
        try {
            URL url = new URL(baseURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            String boundary = "--------------" + getRandomString();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2, boundary.length()));
            conn.connect();
            OutputStream out = conn.getOutputStream();
            String content = "";
            content = content + boundary + "\r\n";
            content = content + "Content-Disposition: form-data; name=\"action\"\r\n\r\n";
            content = content + action + "\r\n";
            if (sessionid != null) {
                content = content + boundary + "\r\n";
                content = content + "Content-Disposition: form-data; name=\"" + sessionName + "\"\r\n\r\n";
                content = content + sessionid + "\r\n";
            }
            out.write(content.getBytes());
            content = "";
            if (mediafile != null) {
                File testfile = new File(mediafile);
                content = content + boundary + "\r\n";
                content = content + "Content-Disposition: form-data; name=\"mediafile\"; filename=\"" + testfile.getName() + "\"\r\n";
                content = content + "Content-Type: image/jpeg\r\n\r\n";
                out.write(content.getBytes());
                FileInputStream is = new FileInputStream(testfile);
                byte[] buf = new byte[512];
                int res = -1;
                while ((res = is.read(buf)) != -1) {
                    out.write(buf);
                }
                is.close();
                out.write("\r\n".getBytes());
                content = "";
            }
            if (thumbnail != null) {
                File testfile = new File(thumbnail);
                content = content + boundary + "\r\n";
                content = content + "Content-Disposition: form-data; name=\"thumbnail\"; filename=\"" + testfile.getName() + "\"\r\n";
                content = content + "Content-Type: image/jpeg\r\n\r\n";
                out.write(content.getBytes());
                FileInputStream is = new FileInputStream(thumbnail);
                byte[] buf = new byte[512];
                int res = -1;
                while ((res = is.read(buf)) != -1) {
                    out.write(buf);
                }
                is.close();
                out.write("\r\n".getBytes());
            }
            content = boundary + "--\r\n";
            out.write(content.getBytes());
            out.close();
            InputStreamReader in = new InputStreamReader(conn.getInputStream(), encoding);
            BufferedReader data = new BufferedReader(in);
            statusText = data.readLine();
            udebug.dprintln("status = " + statusText);
            if (statusText == null) statusText = STR_ERROR + ": missing CGI output";
            gotSuccess = STR_SUCCESS.equals(statusText);
            String line;
            while ((line = data.readLine()) != null) {
                udebug.vprintln("line = " + line);
                lines.add(line);
            }
            in.close();
            udebug.dprintln("\n");
            if (gotSuccess && (lines.size() > 0)) {
                String first = lines.get(0);
                if (first.indexOf(STR_ERROR) >= 0) gotSuccess = false;
            }
            if (!gotSuccess) {
                errorMessage(action, statusText, "");
                if (statusText.indexOf(STR_ERROR) < 0) {
                    System.err.println("Bad error message: " + statusText);
                    badStatus = true;
                }
                return false;
            }
        } catch (IOException e) {
            statusText = "IO Error: " + e.getMessage();
            System.err.println(statusText);
            badStatus = true;
            return false;
        }
        return true;
    }

    private String getRandomString() {
        String alphaNum = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuffer sbRan = new StringBuffer(11);
        int num;
        for (int i = 0; i < 11; i++) {
            num = (int) (Math.random() * (alphaNum.length() - 1));
            sbRan.append(alphaNum.substring(num, num + 1));
        }
        return sbRan.toString();
    }

    private String urlParam(String action, String value) {
        return "&" + action + "=" + encode(value);
    }

    private String encode(String in) {
        String out = in;
        try {
            out = URLEncoder.encode(in, encoding);
        } catch (IOException e) {
        }
        if (!in.equals(out)) udebug.dprintln("encoded: " + out);
        return out;
    }

    private String oneGetxref(String type, String position, String xref) {
        String[] xrefs = actionGetxref(type, position, xref);
        return getOnlyLine(xrefs);
    }

    private void errorMessage(String action, String message, String param) {
        System.err.println("CGI error: action=" + action + ": " + message);
        udebug.dprintln("full URL parameter: " + param);
    }

    static class Debug extends GdbiDebug.Default implements GdbiDebug {

        public String getDebugDescription() {
            return "CGI gedcom access protocol for phpGedView";
        }
    }
}
