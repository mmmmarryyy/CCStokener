package fado;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Very simple properties file. Preserves order of entries in file, including comments and blank lines.
 * 
 * Format of each line:
 * 
 * key = value # comment
 * 
 * If duplicate key is in file, a later key entry hides the prior entry. For example
 * 
 * fruit = apple fruit = banana
 * 
 * The "banana" is the value for key "fruit".
 * 
 * Writing out the properties file "pretty prints" the data.
 * 
 * @author osgood_j
 * 
 */
public class Properties {

    ArrayList<Line> lines = null;

    HashMap<String, Line> map = null;

    public Properties() {
        clear();
    }

    private URL _url = null;

    public URL getURL() {
        return _url;
    }

    public void clear() {
        lines = new ArrayList<Line>();
        map = new HashMap<String, Line>();
    }

    public void set(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        Line line = null;
        if (map.containsKey(key)) {
            line = map.get(key);
            line.value = value;
        } else {
            line = new Line();
            line.key = key;
            line.value = value;
            lines.add(line);
            map.put(key, line);
        }
    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        String value = defaultValue;
        if (map.containsKey(key)) {
            Line line = map.get(key);
            Object temp = line.value;
            if (temp != null) {
                value = temp.toString();
            }
        }
        return value;
    }

    /**
	 * Feed in list of parameters which make up the compound key. Null parameters and empty strings are dropped.
	 * 
	 * @param args
	 */
    public String getCompound(String... args) {
        String key = merge(args);
        return get(key);
    }

    public String merge(String... args) {
        StringBuilder sb = new StringBuilder();
        boolean more = false;
        for (String arg : args) {
            if (more) {
                sb.append(".");
            }
            sb.append(arg);
            more = true;
        }
        return sb.toString();
    }

    public void remove(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (map.containsKey(key)) {
            Line line = map.get(key);
            map.remove(key);
            lines.remove(line);
        }
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    static class Line {

        String key;

        String value;

        String comment;

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (key != null && value != null) {
                sb.append(key);
                sb.append(" = ");
                sb.append(value.toString());
                sb.append(" ");
            }
            if (comment != null) {
                sb.append("# ");
                sb.append(comment);
            }
            return sb.toString();
        }
    }

    public void load(String name) throws IOException {
        ClassLoader loader = getClass().getClassLoader();
        _url = loader.getResource(name + ".properties");
        if (_url != null) {
            InputStream in = _url.openStream();
            InputStreamReader reader = new InputStreamReader(in);
            load(reader);
        }
    }

    public void load(Reader reader) throws IOException {
        BufferedReader buffer = new BufferedReader(reader);
        while (true) {
            String data = buffer.readLine();
            if (data == null) break;
            Line line = new Line();
            int comment = data.indexOf('#');
            if (comment > -1) {
                String temp = data.substring(comment + 1);
                line.comment = temp;
                data = data.substring(0, comment);
            }
            int equal = data.indexOf('=');
            if (equal > -1) {
                String key = data.substring(0, equal);
                line.key = key.trim();
                String value = data.substring(equal + 1);
                line.value = value.trim();
            }
            lines.add(line);
            if (line.key != null) {
                map.put(line.key, line);
            }
        }
    }

    public void store(Writer writer) throws IOException {
        PrintWriter pw = new PrintWriter(writer);
        for (Line line : lines) {
            if (line != null) {
                pw.println(line.toString());
            }
        }
        writer.close();
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            store(sw);
        } catch (IOException e) {
        }
        return sw.toString();
    }

    public java.util.Properties toProperties() {
        java.util.Properties props = new java.util.Properties();
        for (Map.Entry<String, Line> entry : map.entrySet()) {
            String key = entry.getKey();
            Line line = entry.getValue();
            if (line != null) {
                String value = line.value;
                if (value != null) {
                    props.put(key, value);
                }
            }
        }
        return props;
    }

    public void fromProperties(java.util.Properties props) {
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            set(key, value);
        }
    }

    public int hashCode() {
        return map.hashCode();
    }
}
