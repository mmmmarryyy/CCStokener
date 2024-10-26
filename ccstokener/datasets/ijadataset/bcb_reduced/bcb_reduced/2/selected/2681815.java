package net.sourceforge.pinyinlookup.engine.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Mapping between Pinyin and Zhuyin.
 * Holds the list of valid Pinyin and Zhuyin strings.
 * 
 * @author Vincent Petry <PVince81@users.sourceforge.net>
 */
public class PinyinMap {

    private LinkedHashMap<String, String> pinyinZhuyinMap;

    private LinkedHashMap<String, String> zhuyinPinyinMap;

    public PinyinMap(String mapFile) throws IOException {
        pinyinZhuyinMap = new LinkedHashMap<String, String>();
        zhuyinPinyinMap = new LinkedHashMap<String, String>();
        loadMap(mapFile);
    }

    private void loadMap(String mapFile) throws IOException {
        if (!mapFile.startsWith("file:/")) {
            loadMap((new File(mapFile)).toURI());
        } else {
            loadMap(URI.create(mapFile));
        }
    }

    private void loadMap(URI uri) throws IOException {
        BufferedReader reader = null;
        InputStream stream = null;
        try {
            URL url = uri.toURL();
            stream = url.openStream();
            if (url.getFile().endsWith(".gz")) {
                stream = new GZIPInputStream(stream);
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    String[] parts = line.split(" ");
                    if (parts.length == 2) {
                        pinyinZhuyinMap.put(parts[0], parts[1]);
                        zhuyinPinyinMap.put(parts[1], parts[0]);
                    }
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public Set<String> getAvailablePinyin() {
        return Collections.unmodifiableSet(pinyinZhuyinMap.keySet());
    }

    public Set<String> getAvailableZhuyin() {
        return Collections.unmodifiableSet(zhuyinPinyinMap.keySet());
    }

    public String toZhuyin(String pinyin) {
        return pinyinZhuyinMap.get(pinyin);
    }

    public String toPinyin(String zhuyin) {
        return zhuyinPinyinMap.get(zhuyin);
    }
}
