package bg.invider.script;

import bg.invider.util.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import java.net.URLConnection;
import java.lang.reflect.Method;
import org.jdesktop.jdic.browser.WebBrowser;

/**
 * Stores the generated regular expressions for the script.
 * 
 * @author meddle
 * @version 1.2
 */
public class Script {

    public static final String[] SCRIPT_COMPONENTS = { "content", "title", "summary", "author", "date" };

    public static final Map<String, Method> GETTERS;

    static {
        GETTERS = new HashMap<String, Method>(SCRIPT_COMPONENTS.length);
        try {
            for (String component : SCRIPT_COMPONENTS) {
                GETTERS.put(component, Script.class.getMethod(generateGetterName(component), (Class[]) null));
            }
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private String[] choices;

    private String html;

    private ScriptComponent currentComponent;

    private ScriptComponent content = new ScriptComponent(SCRIPT_COMPONENTS[0]);

    private ScriptComponent title = new ScriptComponent(SCRIPT_COMPONENTS[1]);

    private ScriptComponent summary = new ScriptComponent(SCRIPT_COMPONENTS[2]);

    private ScriptComponent author = new ScriptComponent(SCRIPT_COMPONENTS[3]);

    private ScriptComponent date = new ScriptComponent(SCRIPT_COMPONENTS[4]);

    public Script() {
        currentComponent = content;
    }

    private static String generateGetterName(String component) {
        String getter = StringUtils.capitalize(component);
        int spaceIndex = getter.indexOf(' ');
        while (spaceIndex != -1) {
            getter = getter.substring(0, spaceIndex) + StringUtils.capitalize(getter.substring(spaceIndex + 1));
            spaceIndex = getter.indexOf(' ');
        }
        return "get" + getter;
    }

    public ScriptComponent getComponentByName(String name) {
        try {
            return ((ScriptComponent) Script.GETTERS.get(name).invoke(this, (Object[]) null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getChoices() {
        return choices;
    }

    public void setChoices(String[] choices) {
        this.choices = choices;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(WebBrowser browser) {
        String htmlCode = "";
        try {
            URL url = browser.getURL();
            URLConnection conn = url.openConnection();
            String contentType = conn.getContentType();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), getContentEncoding(contentType)));
            String buffer = "";
            while (buffer != null) {
                try {
                    htmlCode += buffer + "\n";
                    buffer = br.readLine();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    break;
                }
            }
            this.html = htmlCode;
        } catch (Exception e) {
        }
    }

    private String getContentEncoding(String contentType) {
        if (contentType.indexOf("charset=") == -1) {
            return "utf-8";
        }
        int start = contentType.indexOf("charset=") + 8;
        return contentType.substring(start);
    }

    public ScriptComponent getContent() {
        return content;
    }

    public void setContent(ScriptComponent content) {
        this.content = content;
    }

    public ScriptComponent getTitle() {
        return title;
    }

    public void setTitle(ScriptComponent title) {
        this.title = title;
    }

    public ScriptComponent getSummary() {
        return summary;
    }

    public void setSummary(ScriptComponent summary) {
        this.summary = summary;
    }

    public ScriptComponent getAuthor() {
        return author;
    }

    public void setAuthor(ScriptComponent author) {
        this.author = author;
    }

    public ScriptComponent getDate() {
        return date;
    }

    public void setDate(ScriptComponent date) {
        this.date = date;
    }

    public ScriptComponent getCurrentComponent() {
        return currentComponent;
    }

    public void setCurrentComponent(ScriptComponent currentComponent) {
        this.currentComponent = currentComponent;
    }

    public void clear() {
        content = new ScriptComponent(SCRIPT_COMPONENTS[0]);
        title = new ScriptComponent(SCRIPT_COMPONENTS[1]);
        summary = new ScriptComponent(SCRIPT_COMPONENTS[2]);
        author = new ScriptComponent(SCRIPT_COMPONENTS[3]);
        date = new ScriptComponent(SCRIPT_COMPONENTS[4]);
        choices = null;
        currentComponent = content;
    }
}
