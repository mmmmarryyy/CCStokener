package net.sf.json.groovy;

import groovy.lang.GroovyObjectSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

/**
 * A Helper class modeled after XmlSlurper
 * 
 * @author Andres Almiray <aalmiray@users.sourceforge.net>
 */
public class JsonSlurper extends GroovyObjectSupport {

    private JsonConfig jsonConfig;

    public JsonSlurper() {
        this(new JsonConfig());
    }

    public JsonSlurper(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig != null ? jsonConfig : new JsonConfig();
    }

    public JSON parse(File file) throws IOException {
        return parse(new FileReader(file));
    }

    public JSON parse(URL url) throws IOException {
        return parse(url.openConnection().getInputStream());
    }

    public JSON parse(InputStream input) throws IOException {
        return parse(new InputStreamReader(input));
    }

    public JSON parse(String uri) throws IOException {
        return parse(new URL(uri));
    }

    public JSON parse(Reader reader) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader in = new BufferedReader(reader);
        String line = null;
        while ((line = in.readLine()) != null) {
            buffer.append(line).append("\n");
        }
        return parseText(buffer.toString());
    }

    public JSON parseText(String text) {
        return JSONSerializer.toJSON(text, jsonConfig);
    }
}
