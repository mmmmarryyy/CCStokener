package com.amazonaws.mturk.util;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class CreateScriptUtil {

    public enum ScriptType {

        Dos(".cmd", "\r\n"), Unix(".sh", "\n");

        private String extension;

        private String eol;

        ScriptType(String extension, String eol) {
            this.extension = extension;
            this.eol = eol;
        }

        public String getExtension() {
            return extension;
        }

        public String getEol() {
            return eol;
        }
    }

    public static String readResource(String resource) throws Exception {
        URL url = CreateScriptUtil.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalArgumentException("Could not find resource " + resource + ".");
        }
        StringWriter out = new StringWriter();
        Reader in = new InputStreamReader(url.openStream(), "UTF-8");
        try {
            char[] buffer = new char[1024];
            int nread = 0;
            while ((nread = in.read(buffer)) != -1) {
                out.write(buffer, 0, nread);
            }
            return out.toString();
        } finally {
            in.close();
        }
    }

    public static String generateScriptSource(CreateScriptUtil.ScriptType type, Map<String, String> input, String resourcePath) throws Exception {
        String template = CreateScriptUtil.readResource(resourcePath);
        if (input != null && input.values() != null) {
            Iterator iter = input.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                template = template.replaceAll(Pattern.quote(key), input.get(key));
            }
        }
        return template;
    }
}
