package org.wikiup.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;
import org.wikiup.util.StringUtil;

public class WikiupProperties extends WikiupDynamicSingleton<WikiupProperties> {

    public static char OPEN_BRACKET;

    public static char CLOSE_BRACKET;

    public static char VARIABLE_TOKEN;

    public static Pattern VARIABLE_NAME_PATTERN;

    public static String NAMESPACE_SPLITTER;

    public static String DEFAULT_FACTORY_ATTRIBUTE;

    public static String CHAR_SET;

    public static String TRIM_CHAR_SET = " \t\r\n";

    public static String DATE_TIME_PATTERN;

    public static String DATE_PATTERN;

    private Properties properties;

    public static synchronized WikiupProperties getInstance() {
        return getInstance(WikiupProperties.class);
    }

    public WikiupProperties() {
        URL url = Thread.currentThread().getContextClassLoader().getResource("wikiup/wikiup.properties");
        properties = new Properties();
        if (url != null) {
            try {
                InputStream is = url.openStream();
                properties.load(is);
                is.close();
            } catch (IOException ex) {
            }
        }
        init(properties);
    }

    public String getProperty(String name, String def) {
        return properties.getProperty(name, def);
    }

    private void init(Properties p) {
        VARIABLE_TOKEN = getCharacter(getDefaultProperty(p, "wikiup.variable.head-token", null), '$');
        OPEN_BRACKET = getCharacter(getDefaultProperty(p, "wikiup.variable.open-bracket", null), '{');
        CLOSE_BRACKET = getCharacter(getDefaultProperty(p, "wikiup.variable.close-bracket", null), '}');
        VARIABLE_NAME_PATTERN = getVariableNamePattern(getDefaultProperty(p, "wikiup.variable.character-pattern", "\\w\\d\\.-:_"));
        NAMESPACE_SPLITTER = getDefaultProperty(p, "wikiup.variable.namespace-splitter", ":./\\");
        DEFAULT_FACTORY_ATTRIBUTE = getDefaultProperty(p, "wikiup.factory.attribute-name", "class");
        CHAR_SET = getDefaultProperty(p, "wikiup.charset", "utf-8");
        DATE_TIME_PATTERN = getDefaultProperty(p, "wikiup.pattern.date-time", "yyyy-MM-dd HH:mm:ss.S");
        DATE_PATTERN = getDefaultProperty(p, "wikiup.pattern.date", "yyyy-MM-dd");
    }

    public String getConfigure(String value, String defValue, String globalValue) {
        return value != null ? value : defValue != null ? defValue : globalValue;
    }

    private Pattern getVariableNamePattern(String charPattern) {
        StringBuffer pattern = new StringBuffer();
        pattern.append("([").append(charPattern).append('\\').append(CLOSE_BRACKET);
        pattern.append("]|(\\").append(VARIABLE_TOKEN).append('\\').append(OPEN_BRACKET).append("))+");
        return Pattern.compile(pattern.toString());
    }

    @Override
    protected void copyInstance(WikiupProperties instance) {
        properties = instance.properties;
    }

    private char getCharacter(String value, char def) {
        return StringUtil.isEmpty(value) ? def : value.charAt(0);
    }

    private String getDefaultProperty(Properties p, String key, String defValue) {
        String value = p.getProperty(key, null);
        if (value == null && defValue != null) p.setProperty(key, (value = defValue));
        return value;
    }
}
