package com.google.code.sagetvaddons.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.code.webhooks.GoogleCodeSecretKey;

/**
 * Servlet implementation class for Servlet: BuildLauncher
 *
 */
public final class BuildLauncher extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    static final long serialVersionUID = 1L;

    private static final Map<String, Integer> LATEST_BUILD = Collections.synchronizedMap(new HashMap<String, Integer>());

    private static String getPayload(BufferedReader r) throws IOException {
        String line;
        String result = "";
        while ((line = r.readLine()) != null) result = result.concat(line + "\n");
        return result.trim();
    }

    private static String getProject(JSONObject o) throws JSONException {
        JSONObject rev = o.getJSONArray("revisions").getJSONObject(0);
        String path;
        if (rev.getJSONArray("modified").length() > 0) path = rev.getJSONArray("modified").getString(0); else if (rev.getJSONArray("added").length() > 0) path = rev.getJSONArray("added").getString(0); else path = rev.getJSONArray("removed").getString(0);
        Matcher m = Pattern.compile("^\\/trunk\\/([a-z1-9]+)\\/.*").matcher(path);
        if (m.matches()) return m.group(1) + "-trunk";
        return "default";
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String expectedHash = request.getHeader("Google-Code-Project-Hosting-Hook-Hmac");
        if (expectedHash == null) expectedHash = "";
        response.setDateHeader("Expires", new Date(0).getTime());
        response.setHeader("Cache-Control", "no-cache, must-revalidate");
        response.setHeader("Content-Type", "text/plain");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        PrintWriter writer = response.getWriter();
        System.out.println(fmt.format(new Date()) + ": Web Hook processing started!");
        Properties props = new Properties();
        props.load(new FileReader(new File("sagetv-addons.properties").getAbsolutePath()));
        String payload = getPayload(request.getReader());
        String project = null;
        JSONObject o = null;
        try {
            o = new JSONObject(payload);
            project = getProject(o);
        } catch (JSONException e) {
            response.setStatus(400);
            writer.write("InvalidPayload\n");
            e.printStackTrace(writer);
            return;
        }
        String secretProp = project + ".googlecode.secret";
        String key = props.getProperty(secretProp);
        if ((key == null || key.length() == 0) && ((key = props.getProperty("googlecode.secret")) == null || key.length() == 0)) throw new IOException("Project secret key [" + secretProp + "] must be defined in properties file and cannot be zero-length or googlecode.secret must be defined");
        if (!doAuth(expectedHash, key, payload)) {
            response.setStatus(403);
            writer.write("AuthenticationFailed");
            return;
        }
        int rev = getRevision(o);
        if (shouldBuild(project, rev)) triggerBuild(props, project, rev); else System.out.println("GC Webhooks: Ignoring web hook for project '" + project + "': A revision higher than or equal to " + rev + " has already been built");
        writer.write("OK");
        System.out.println(fmt.format(new Date()) + ": Web Hook processing completed!");
    }

    private boolean shouldBuild(String project, int rev) {
        Integer latest = LATEST_BUILD.get(project);
        return latest == null || rev > latest;
    }

    private int getRevision(JSONObject o) {
        try {
            return o.getJSONArray("revisions").getJSONObject(0).getInt("revision");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void triggerBuild(Properties props, String project, int rev) throws IOException {
        boolean doBld = Boolean.parseBoolean(props.getProperty(project + ".bld"));
        String url = props.getProperty(project + ".url");
        if (!doBld || project == null || project.length() == 0) {
            System.out.println("BuildLauncher: Not configured to build '" + project + "'");
            return;
        } else if (url == null) {
            throw new IOException("Tried to launch build for project '" + project + "' but " + project + ".url property is not defined!");
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        System.out.println(fmt.format(new Date()) + ": Triggering a build via: " + url);
        BufferedReader r = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        while (r.readLine() != null) ;
        System.out.println(fmt.format(new Date()) + ": Build triggered!");
        LATEST_BUILD.put(project, rev);
        r.close();
        System.out.println(fmt.format(new Date()) + ": triggerBuild() done!");
    }

    private boolean doAuth(String hash, String key, String payload) throws IOException {
        if (hash == null) return false;
        SecretKey k = new GoogleCodeSecretKey(key);
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(k);
            String computedHash = new String(Hex.encodeHex(mac.doFinal(payload.getBytes(Charset.forName("UTF-8")))));
            return hash.equals(computedHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
