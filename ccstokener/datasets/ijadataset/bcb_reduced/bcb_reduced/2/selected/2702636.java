package com.google.code.sagetvaddons.dev.ant.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * <p>An ant task that will submit a SageTV plugin manifest to the SageTV Plugin Repository web site.</p>
 * 
 * <p>
 * Use this task in your ant build.xml file to submit a plugin manifest to the SageTV plugin repository.
 * Simply include this class in your ant classpath and use it as follows:
 * </p>
 * 
 * <pre>
 * &lt;task name="submit" depends="compile"&gt;
 * 	&lt;taskdef classname="com.google.code.sagetvaddons.dev.ant.task.SageTvPluginSubmitter" name="sagesubmit" /&gt;
 * 	&lt;sagesubmit name="Your Name" user="Your Forum ID" email="Your email" pluginid="plugin_id" reqtype="NEW" descriptor="XML_descriptor_path" /&gt;
 * &lt;/task&gt;
 * </pre>
 * 
 * <p>Where the following attributes are <b>required</b>:</p>
 * <ul>
 *    <li><b>name</b>: Your (author's) name</li>
 *    <li><b>user</b>: The author's SageTV Forum user id</li>
 *    <li><b>email</b>: The author's email address; the confirmation link from Sage will be sent to this address so make sure it's a valid email address</li>
 *    <li><b>pluginid</b>: Your plugin's unique identifier string</li>
 *    <li><b>reqtype</b>: The type of submission request; one of NEW (a new plugin addition), UPGRADE (update existing plugin), DELETE (remove plugin), TEST (test XML manifest)</li>
 *    <li><b>descriptor</b>: Path to XML descriptor (manifest) file; if relative then it's relative to the build.xml location</li>
 * </ul>
 * 
 * <p>The following attributes are <b>optional</b>:</p>
 * <ul>
 *    <li><b>encoding</b>: The encoding used in the XML file; if not specified then "UTF-8" is assumed</li>
 * </ul>
 * @author Derek Battams &lt;derek AT battams DOT ca&gt;
 * @version $Id: SageTvPluginSubmitter.java 803 2010-05-14 16:56:30Z derek@battams.ca $
 */
public class SageTvPluginSubmitter extends Task {

    /**
	 * The valid values for the reqtype attribute
	 * @author Derek Battams &lt;Derek AT battams DOT ca&gt;	 *
	 */
    public static enum PluginRequestType {

        NEW("New Plugin"), UPGRADE("Upgraded Plugin"), DELETE("Remove Plugin"), TEST("Test Plugin");

        private String val;

        private PluginRequestType(String val) {
            this.val = val;
        }

        /**
		 * Return the value expected by the SageTV server for the given request type
		 * @return The enum coverted to a string recognized by the SageTV server
		 */
        public String getVal() {
            return val;
        }
    }

    private String name;

    private String userId;

    private String email;

    private PluginRequestType reqType;

    private String pluginId;

    private File descriptor;

    private String xmlEnc;

    @Override
    public void execute() throws BuildException {
        if (name == null || name.length() == 0) throw new BuildException("Name attribute cannot be empty!");
        if (userId == null || userId.length() == 0) throw new BuildException("User attribute cannot be empty!");
        if (email == null || email.length() == 0) throw new BuildException("Email attribute cannot be empty!");
        if (descriptor == null || !descriptor.isFile()) throw new BuildException("Invalid SageTV plugin descriptor!");
        if (reqType == null) throw new BuildException("Request type cannot be null!");
        if (pluginId == null || pluginId.length() == 0) throw new BuildException("Plugin ID attribute cannot be empty!");
        if (descriptor == null || !descriptor.isFile()) throw new BuildException("Plugin descriptor does not exist!");
        if (xmlEnc == null || xmlEnc.length() == 0) xmlEnc = "UTF-8";
        try {
            StringBuilder payload = new StringBuilder();
            payload.append("Name=" + URLEncoder.encode(name, "UTF-8"));
            payload.append("&Email=" + URLEncoder.encode(email, "UTF-8"));
            payload.append("&Username=" + URLEncoder.encode(userId, "UTF-8"));
            payload.append("&PluginID=" + URLEncoder.encode(pluginId, "UTF-8"));
            payload.append("&RequestType=" + URLEncoder.encode(reqType.getVal(), "UTF-8"));
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(descriptor), xmlEnc));
            String line;
            StringBuilder doc = new StringBuilder();
            while ((line = r.readLine()) != null) doc.append(line + "\n");
            r.close();
            payload.append("&Manifest=" + URLEncoder.encode(doc.toString(), "UTF-8"));
            URL url = new URL("http://download.sage.tv/pluginSubmit.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(payload.toString());
            out.flush();
            out.close();
            r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while (r.readLine() != null) ;
            r.close();
            if (conn.getResponseCode() != 200) throw new BuildException("Invalid response received from SageTV [" + conn.getResponseMessage() + "]");
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public void setEncoding(String enc) {
        xmlEnc = enc;
    }

    public void setDescriptor(File f) {
        descriptor = f;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUser(String user) {
        this.userId = user;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPluginId(String id) {
        this.pluginId = id;
    }

    public void setReqType(PluginRequestType reqType) {
        this.reqType = reqType;
    }
}
