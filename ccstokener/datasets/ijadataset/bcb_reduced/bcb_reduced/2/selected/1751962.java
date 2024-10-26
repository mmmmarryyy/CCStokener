package org.hybridlabs.source.beautifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.hunsicker.jalopy.Jalopy;
import de.hunsicker.jalopy.language.antlr.JavaNode;
import de.hunsicker.jalopy.storage.Loggers;

/**
 * Abstract implementation of the Beautifier interface focussing on Java
 * beautification.
 *
 * @author Karsten Klein, hybrid labs
 * @author Karsten Thoms, itemis AG
 *
 */
public abstract class JavaBeautifier implements Beautifier, ImportBeautifierJalopyConstants {

    private static final Pattern PATTERN_NEWLINE = Pattern.compile("\\n");

    private static final Logger LOG = LoggerFactory.getLogger(JavaBeautifier.class);

    private String conventionFilePath;

    private boolean conventionFileInitialized = false;

    public String getConventionFilePath() {
        return conventionFilePath;
    }

    public void setConventionFilePath(String conventionFilePath) {
        this.conventionFilePath = conventionFilePath;
        conventionFileInitialized = false;
    }

    protected JavaNode createJavaNode(org.hybridlabs.source.beautifier.CharacterSequence sequence, File file) {
        initialize();
        JavaNode node = null;
        Jalopy jalopy = initializeJalopy();
        final PrintStream out = System.out;
        try {
            System.setOut(outToLog4J(out));
            jalopy.setInput(sequence.getString(), file.getAbsolutePath());
            node = jalopy.parse();
        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
        } finally {
            cleanupJalopy();
            System.setOut(out);
        }
        return node;
    }

    private static PrintStream outToLog4J(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {

            @Override
            public void print(final String string) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(string);
                }
            }

            @Override
            public void write(int b) {
            }

            @Override
            public void write(byte[] buf, int off, int len) {
            }
        };
    }

    private URL testUrl(URL url) {
        if (url != null) {
            InputStream inputStream = null;
            try {
                inputStream = url.openStream();
            } catch (IOException e) {
                return null;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
            return url;
        }
        return null;
    }

    private void initialize() {
        initializeConventionFileUrl();
    }

    private void initializeConventionFileUrl() {
        if (conventionFileInitialized) {
            return;
        }
        conventionFileInitialized = true;
        URL url = null;
        if (conventionFilePath != null) {
            url = testUrl(getClass().getResource(conventionFilePath));
            if (url == null) {
                try {
                    url = testUrl(new URL("file:" + conventionFilePath));
                } catch (MalformedURLException e) {
                    LOG.error("Cannot read convention file from 'file:" + conventionFilePath + "'.", e);
                }
            }
        }
        if (url == null) {
            url = testUrl(getClass().getResource("/default-convention.xml"));
        }
        if (url != null) {
            try {
                Jalopy.setConvention(url);
            } catch (IOException e) {
                LOG.error("Cannot read convention file from '" + url + "'.", e);
            }
        }
    }

    protected int findPositionInCharacterSequence(CharacterSequence sequence, int line, int column) {
        Pattern newlinePattern = PATTERN_NEWLINE;
        Matcher newLineMatcher = newlinePattern.matcher(sequence);
        int pos = 0;
        line--;
        while (line > 0) {
            newLineMatcher.find();
            pos = newLineMatcher.end();
            line--;
        }
        pos += column;
        if (pos >= 0) {
            while (pos < sequence.length() && (sequence.charAt(pos) == '\r' || sequence.charAt(pos) == '\n')) {
                pos++;
            }
        }
        return pos;
    }

    protected void format(CharacterSequence sequence, File file) {
        Jalopy jalopy = initializeJalopy();
        try {
            jalopy.setInput(sequence.getString(), file.getAbsolutePath());
            StringBuffer sb = new StringBuffer();
            jalopy.setOutput(sb);
            jalopy.format();
            sequence.set(sb);
        } finally {
            cleanupJalopy();
        }
    }

    private Jalopy initializeJalopy() {
        Jalopy jalopy = new Jalopy();
        jalopy.setInspect(false);
        jalopy.setBackup(false);
        jalopy.setForce(false);
        return jalopy;
    }

    private Class Log4j_Logger;

    private Method Logger_getAllAppenders;

    private Method Logger_removeAppender;

    private boolean useLog4j = true;

    /**
	 * @see http://code.google.com/p/hybridlabs-beautifier/issues/detail?id=14
	 */
    @SuppressWarnings("unchecked")
    private void cleanupJalopy() {
        if (!useLog4j) return;
        try {
            List<Object> toBeDeleted = new ArrayList<Object>();
            Log4j_Logger = Class.forName("org.apache.log4j.Logger");
            Object logger = Log4j_Logger.getField("ALL");
            if (Logger_getAllAppenders == null) {
                Logger_getAllAppenders = logger.getClass().getMethod("getAllAppenders", (Class[]) null);
                Logger_removeAppender = logger.getClass().getMethod("removeAppender", Class.forName("org.apache.log4j.Appender"));
            }
            for (Enumeration<Object> it = (Enumeration<Object>) Logger_getAllAppenders.invoke(logger, (Object[]) null); it.hasMoreElements(); ) {
                Object obj = it.nextElement();
                String name = obj.getClass().getName();
                if (name.equals("de.hunsicker.jalopy.Jalopy$SpyAppender")) {
                    toBeDeleted.add((Object) obj);
                }
            }
            for (Object appender : toBeDeleted) {
                Logger_removeAppender.invoke(logger, appender);
            }
        } catch (Exception e) {
            useLog4j = false;
            LOG.debug("Log4j not found.");
        }
    }
}
