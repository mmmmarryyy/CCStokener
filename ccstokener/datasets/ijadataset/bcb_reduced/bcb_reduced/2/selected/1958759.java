package services.core.commands.state;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class PropertyFileSupport {

    private static Map<String, Long> csvPropertyFileLinePosition = Collections.synchronizedMap(new HashMap<String, Long>());

    public static final String PROPERTY_FILE_HOME = "PROPERTY_FILE_HOME";

    public static final String PROPERTY_LINE_SEPARATOR = "\r\n";

    private String lineSeparator = PROPERTY_LINE_SEPARATOR;

    public PropertyFileSupport() {
    }

    public Properties getProperties(String relativePropertyFilePath, String propertyFilter) throws IOException {
        String propertiesAsString = getPropertiesAsString(relativePropertyFilePath, propertyFilter);
        ByteArrayInputStream stringInputStream = new ByteArrayInputStream(propertiesAsString.getBytes());
        Properties properties = new Properties();
        properties.load(stringInputStream);
        stringInputStream.close();
        return properties;
    }

    public String getPropertiesAsString(String relativePropertyFilePath, String propertyFilter) throws IOException {
        return getPropertiesAsString(getPropertyFileUrl(relativePropertyFilePath), propertyFilter);
    }

    protected String getPropertiesAsString(URL propertyFile, String propertyFilter) throws IOException {
        if (propertyFile.getFile().endsWith(".csv")) {
            return getPropertiesFromFileWithDotCsvExtension(propertyFile, propertyFilter);
        } else {
            return getPropertiesFromFileWithDotPropertiesExtension(propertyFile, propertyFilter);
        }
    }

    protected String getPropertiesFromFileWithDotPropertiesExtension(URL propertyFile, String propertyFilter) throws IOException {
        Properties properties = new Properties();
        URLConnection connection = propertyFile.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        properties.load(connection.getInputStream());
        StringBuffer result = new StringBuffer();
        Set<Object> propertyKeySet = new TreeSet<Object>(properties.keySet());
        for (Object propertyKeyEntry : propertyKeySet) {
            String propertyKey = String.valueOf(propertyKeyEntry);
            String propertyValue = String.valueOf(properties.getProperty(propertyKey));
            if (propertyKey.matches(propertyFilter)) {
                result.append(propertyKey + "=" + propertyValue + getLineSeparator());
            }
        }
        return String.valueOf(result);
    }

    protected String getPropertiesFromFileWithDotCsvExtension(URL propertyFile, String propertyFilter) throws IOException {
        String fileContents = getUrlContentsAsString(propertyFile);
        BufferedReader br;
        String line;
        long currentLinePosition;
        br = new BufferedReader(new StringReader(fileContents));
        currentLinePosition = -1;
        while ((line = br.readLine()) != null) {
            currentLinePosition++;
        }
        long numberOfPropertyEntries = currentLinePosition;
        Long lastReadPropertyEntryPosition = csvPropertyFileLinePosition.get(propertyFile.toExternalForm());
        if (null == lastReadPropertyEntryPosition) {
            lastReadPropertyEntryPosition = 0L;
        } else if (lastReadPropertyEntryPosition + 1 > numberOfPropertyEntries) {
            lastReadPropertyEntryPosition = 0L;
        }
        br = new BufferedReader(new StringReader(fileContents));
        StringBuffer result = new StringBuffer();
        line = br.readLine();
        currentLinePosition = 0;
        String[] propertyNames = line.split(",");
        Set<String> orderedPropertyNames = new TreeSet<String>(Arrays.asList(propertyNames));
        propertyNames = orderedPropertyNames.toArray(new String[0]);
        long currentReadPropertyEntryPosition = currentLinePosition;
        while ((line = br.readLine()) != null) {
            currentLinePosition++;
            currentReadPropertyEntryPosition = currentLinePosition;
            if (currentReadPropertyEntryPosition == lastReadPropertyEntryPosition + 1) {
                String[] propertyValues = line.split(",");
                for (int i = 0; i < propertyNames.length; i++) {
                    String propertyName = propertyNames[i];
                    String propertyValue = propertyValues[i];
                    if (propertyName.matches(propertyFilter)) {
                        result.append(propertyName + "=" + propertyValue + getLineSeparator());
                    }
                }
                break;
            }
        }
        csvPropertyFileLinePosition.put(propertyFile.toExternalForm(), currentReadPropertyEntryPosition);
        return String.valueOf(result);
    }

    public URL getPropertyFileUrl(String propertyFilePath) {
        URL propertyFile = null;
        try {
            if (propertyFilePath.startsWith("http")) {
                propertyFile = new URL(propertyFilePath);
            } else {
                propertyFile = (new File(getPropertyFileHomePath() + "/" + propertyFilePath)).toURL();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return propertyFile;
    }

    protected static String getUrlContentsAsString(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        StringBuffer result = new StringBuffer();
        while ((line = br.readLine()) != null) {
            result.append(line + "\r\n");
        }
        br.close();
        return result.toString();
    }

    public String getPropertyFileHomePath() {
        if (null != System.getProperty(PROPERTY_FILE_HOME)) {
            return System.getProperty(PROPERTY_FILE_HOME);
        } else if (null != System.getenv(PROPERTY_FILE_HOME)) {
            return System.getenv(PROPERTY_FILE_HOME);
        } else {
            return new File(".").getAbsolutePath();
        }
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }
}
