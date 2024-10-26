package com.google.code.javastorage.dropio.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.code.javastorage.dropio.rest.HttpUtil;

/**
 * 
 * @author thomas.scheuchzer@gmail.com
 * 
 */
public class GetContent {

    private Logger log = LoggerFactory.getLogger(GetContent.class);

    private static final Pattern PATTERN = Pattern.compile(".*(http://drop.io/download/.*/original_content).*");

    @SuppressWarnings("unchecked")
    public InputStream openStream(URL url) throws IOException {
        List<String> lines = IOUtils.readLines(url.openStream());
        for (String line : lines) {
            Matcher m = PATTERN.matcher(line);
            if (m.matches()) {
                String origUrl = m.group(1);
                log.info("Loading content from: " + origUrl);
                return new HttpUtil<InputStream>() {

                    @Override
                    public InputStream handleResponse(InputStream response) throws IOException {
                        return new ByteArrayInputStream(IOUtils.toByteArray(response));
                    }
                }.get(new URL(origUrl));
            }
        }
        log.error("No origUrl found for url: " + url);
        return null;
    }
}
