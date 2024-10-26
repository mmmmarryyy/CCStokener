package com.x5.template;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.Format;
import com.csvreader.CsvReader;

public class ChunkLocale {

    private String localeCode;

    private HashMap<String, String> translations;

    private static HashMap<String, ChunkLocale> locales = new HashMap<String, ChunkLocale>();

    public static ChunkLocale getInstance(String localeCode, Chunk context) {
        ChunkLocale instance = locales.get(localeCode);
        if (instance != null) {
            return instance;
        } else {
            instance = new ChunkLocale(localeCode, context);
            locales.put(localeCode, instance);
            return instance;
        }
    }

    public static void registerLocale(String localeCode, String[] translations) {
        ChunkLocale instance = new ChunkLocale(localeCode, translations);
        locales.put(localeCode, instance);
    }

    private ChunkLocale(String localeCode, Chunk context) {
        this.localeCode = localeCode;
        loadTranslations(context);
    }

    private ChunkLocale(String localeCode, String[] strings) {
        this.localeCode = localeCode;
        if (strings != null && strings.length > 1) {
            this.translations = new HashMap<String, String>();
            for (int i = 0; i + 1 < strings.length; i++) {
                String a = strings[i];
                String b = strings[i + 1];
                translations.put(a, b);
            }
        }
    }

    private void loadTranslations(Chunk context) {
        try {
            InputStream in = locateLocaleDB(context);
            if (in == null) return;
            Charset charset = grokLocaleDBCharset();
            CsvReader reader = new CsvReader(in, charset);
            reader.setUseComments(true);
            String[] entry;
            entry = reader.getValues();
            translations = new HashMap<String, String>();
            while (entry != null) {
                if (entry.length > 1 && entry[0] != null && entry[1] != null) {
                    String key = entry[0];
                    String localString = entry[1];
                    translations.put(key, localString);
                }
                entry = reader.readRecord() ? reader.getValues() : null;
            }
        } catch (IOException e) {
            System.err.println("ERROR loading locale DB: " + localeCode);
            e.printStackTrace(System.err);
        }
    }

    private Charset grokLocaleDBCharset() {
        String override = System.getProperty("chunk.localedb.charset");
        if (override != null) {
            Charset charset = null;
            try {
                charset = Charset.forName(override);
            } catch (IllegalCharsetNameException e) {
            } catch (UnsupportedCharsetException e) {
            }
            if (charset != null) return charset;
        }
        try {
            return Charset.forName("UTF-8");
        } catch (Exception e) {
        }
        return Charset.defaultCharset();
    }

    private InputStream locateLocaleDB(Chunk context) throws java.io.IOException {
        String localePath = System.getProperty("chunk.localedb.path");
        if (localePath != null) {
            File folder = new File(localePath);
            if (folder.exists()) {
                File file = new File(folder, localeCode + "/translate.csv");
                if (file.exists()) {
                    return new FileInputStream(file);
                }
            }
        }
        String path = "/locale/" + localeCode + "/translate.csv";
        InputStream in = this.getClass().getResourceAsStream(path);
        if (in != null) return in;
        String cp = System.getProperty("java.class.path");
        if (cp == null) return null;
        String[] jars = cp.split(":");
        if (jars == null) return null;
        for (String jar : jars) {
            if (jar.endsWith(".jar")) {
                in = peekInsideJar("jar:file:" + jar, path);
                if (in != null) return in;
            }
        }
        return null;
    }

    private InputStream peekInsideJar(String jar, String resourcePath) {
        String resourceURL = jar + "!" + resourcePath;
        try {
            URL url = new URL(resourceURL);
            InputStream in = url.openStream();
            if (in != null) return in;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        try {
            String zipPath = jar.replaceFirst("^jar:file:", "");
            String zipResourcePath = resourcePath.replaceFirst("^/", "");
            ZipFile zipFile = new ZipFile(zipPath);
            ZipEntry entry = zipFile.getEntry(zipResourcePath);
            if (entry != null) {
                return zipFile.getInputStream(entry);
            }
        } catch (java.io.IOException e) {
        }
        return null;
    }

    public String translate(String string, String[] args, Chunk context) {
        return processFormatString(string, args, context, translations);
    }

    public static String processFormatString(String string, String[] args, Chunk context) {
        return processFormatString(string, args, context, null);
    }

    public static String processFormatString(String string, String[] args, Chunk context, HashMap<String, String> translations) {
        if (string == null) return null;
        String xlated = string;
        if (translations != null && translations.containsKey(string)) {
            xlated = translations.get(string);
        }
        if (!xlated.contains("%s") || args == null || context == null) {
            return xlated;
        }
        Object[] values = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            String tagName = args[i];
            if (tagName.startsWith("~")) {
                Object val = context.get(tagName.substring(1));
                String valString = (val == null ? "" : val.toString());
                values[i] = valString;
            } else {
                values[i] = tagName;
            }
        }
        try {
            return String.format(xlated, values);
        } catch (IllegalFormatException e) {
            return xlated;
        }
    }
}
