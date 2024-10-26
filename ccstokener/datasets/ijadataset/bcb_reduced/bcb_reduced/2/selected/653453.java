package de.objectcode.portlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

public class TagSplitter {

    private String name;

    private String password;

    private String loginURL;

    private String logoutURL;

    private String loginData;

    public TagSplitter(LoginData loginData) {
        name = loginData.getLogin();
        password = loginData.getPassword();
        loginURL = loginData.getLoginURL();
        logoutURL = loginData.getLogoutURL();
    }

    public TagSplitter(PortletPreferences prefs) {
        name = prefs.getValue("user", null);
        password = prefs.getValue("password", null);
        loginURL = prefs.getValue("loginURL", null);
        logoutURL = prefs.getValue("logoutURL", null);
    }

    /**
	 * 
	 * Splits the Stringbuilder with regex and add it into Stringlist
	 * @param strB
	 * @param regex
	 * @return
	 */
    public List<String> getInputTags(StringBuilder strB, String regex) {
        List<String> found = new ArrayList<String>();
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(strB);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

    public List<String> getNames(List<String> found, String tag) {
        List<String> names = new ArrayList<String>();
        int namePos = 0;
        int firstPos = 0;
        int endPos = 0;
        String temp2 = null;
        for (String temp : found) {
            namePos = temp.indexOf(tag);
            firstPos = temp.indexOf("\"", namePos);
            endPos = temp.indexOf("\"", firstPos + 1);
            temp2 = temp.substring(firstPos + 1, endPos);
            if (temp.contains("\"password\"") && tag.equals("value=")) {
                names.add(password);
            } else if (temp.contains("\"text\"") && tag.equals("value=")) {
                names.add(name);
            } else {
                names.add(temp2);
            }
        }
        return names;
    }

    /**
	 * 
	 * splits a webpage 
	 * @param firstTag (start tag)
	 * @param endTag   (end tag)
	 * @return
	 * @throws IOException
	 */
    public StringBuilder getTag(String firstTag, String endTag) throws IOException {
        URL url = new URL(loginURL);
        URLConnection urlConnect = url.openConnection();
        BufferedReader buf = new BufferedReader(new InputStreamReader(urlConnect.getInputStream()));
        StringBuilder strB = new StringBuilder();
        String input = null;
        while ((input = buf.readLine()) != null) strB.append(input);
        int firstTagPos = strB.indexOf(firstTag);
        int endTagPos = strB.indexOf(endTag);
        if (endTagPos > 0) {
            strB = new StringBuilder(strB.substring(firstTagPos, endTagPos));
        }
        buf.close();
        return strB;
    }

    /**
	 * 
	 * @param prefs
	 * @param name (name to save) 
	 * @param value (value to save)
	 * @throws ReadOnlyException
	 * @throws ValidatorException
	 * @throws IOException
	 */
    public static void saveParameter(PortletPreferences prefs, String name, String value) throws ReadOnlyException, ValidatorException, IOException {
        prefs.setValue(name, value);
        prefs.store();
    }

    public String getLoginURL() {
        return loginURL;
    }

    public void setLoginURL(String loginURL) {
        this.loginURL = loginURL;
    }

    public String getLogoutURL() {
        return logoutURL;
    }

    public void setLogoutURL(String logoutURL) {
        this.logoutURL = logoutURL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
