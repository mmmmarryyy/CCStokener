package com.ssg.tools.jsonxml.common.tools;

import com.ssg.tools.jsonxml.BigFactory;
import com.ssg.tools.jsonxml.Tester;
import com.ssg.tools.jsonxml.common.Formats;
import com.ssg.tools.jsonxml.common.ValidationError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import javax.xml.transform.stream.StreamSource;

/**
 * Provides skeleton for creation of simple command line tools.
 * Implements several utility methods.
 * 
 * Extend this class and create "public static void main(String[])" to instantiate it and execute "run(String[])" method.
 * 
 * @author ssg
 */
public abstract class CommandLineToolPrototype {

    private BigFactory factory = BigFactory.getInstance();

    private Formats formats = new Formats();

    /**
     * Implement this method to print help info on command syntax and parameters.
     * Extend this class and implement abstract methods.
     * Create 
     */
    public abstract void help();

    /**
     * Implement arguments poarsing and execution in this method.
     * @param args
     * @throws Exception 
     */
    public abstract void run(String[] args) throws Exception;

    /**
     * Parses content of URL with optional validation.
     * Returns parsed object.
     * 
     * @param url
     * @param validateURL
     * @return
     * @throws IOException 
     */
    public Object parsePath(URL url, URL validateURL) throws IOException {
        if (url == null) {
            help();
            System.err.println("  ERROR: no source is given: " + url);
            return null;
        }
        String input = null;
        for (String inputF : getFactory().getSupportedFormats()) {
            Tester t = getFactory().getTester(inputF);
            if (t != null) {
                if (t.test(url)) {
                    input = inputF;
                    List<ValidationError> errors = null;
                    errors = t.validate(new StreamSource(getReader(url)), (validateURL != null) ? new StreamSource(getReader(validateURL)) : null);
                    if (!errors.isEmpty()) {
                        help();
                        System.err.println("  ERROR: input file syntax/data error(s) [" + input + "]:");
                        for (ValidationError err : errors) {
                            System.err.println("    " + err.getMessage());
                        }
                        return null;
                    }
                    break;
                }
            }
        }
        if (input == null || getFactory().getParser(input) == null) {
            help();
            System.err.println("  ERROR: unknown or unsupported input file format [" + input + "]");
            return null;
        }
        try {
            Reader r = getReader(url);
            return getFactory().getParser(input).parse(r, getFormats());
        } catch (Throwable th) {
            help();
            System.err.println("  ERROR: failed to parse as " + input + " : " + th);
        }
        return null;
    }

    /**
     * Creates reader for file or URL presented by path.
     * 
     * @param path
     * @return
     * @throws MalformedURLException
     * @throws IOException 
     */
    public Reader getReader(URL url) throws IOException {
        if (url != null) {
            return new InputStreamReader(url.openStream());
        } else {
            return null;
        }
    }

    /**
     * @return the factory
     */
    public BigFactory getFactory() {
        return factory;
    }

    /**
     * @param factory the factory to set
     */
    public void setFactory(BigFactory factory) {
        this.factory = factory;
    }

    /**
     * @return the formats
     */
    public Formats getFormats() {
        return formats;
    }

    /**
     * @param formats the formats to set
     */
    public void setFormats(Formats formats) {
        this.formats = formats;
    }

    /**
     * Tries to parse value as boolean and throws exception if failed.
     * Valid values are "false" and "true" only.
     * 
     * @param value
     * @return
     * @throws ParseException 
     */
    public boolean parseBoolean(String value) throws ParseException {
        if (value == null) {
            throw new ParseException("Null can't be converted to boolean", 0);
        } else if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else {
            throw new ParseException("Invalid boolean value: " + value, 0);
        }
    }

    /**
     * Returns locale corresponding to provided name (value).
     * Checks if locale is supported and throws exception if not.
     * 
     * @param value
     * @return
     * @throws ParseException 
     */
    public Locale parseLocale(String value) throws ParseException {
        Locale locale = new Locale(value);
        boolean locValid = false;
        for (Locale loc : Locale.getAvailableLocales()) {
            if (loc.equals(locale)) {
                locValid = true;
                break;
            }
        }
        if (!locValid) {
            throw new ParseException("Unrecognized locale: " + value, 0);
        }
        return locale;
    }
}
