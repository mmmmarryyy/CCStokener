package org.mcisb.util;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;
import org.mcisb.util.io.*;

/**
 * 
 * @author Neil Swainston
 */
public class RegularExpressionUtils {

    /**
	 * 
	 */
    public static final String EC_REGEX = "(?=.*)[\\d]+\\.[\\d]+\\.[\\d]+\\.[\\d]+(?=.*)";

    /**
	 * 
	 */
    public static final String INCHI_REGEX = "(?=.*)InChI=[\\d]+S?/[A-Z[\\d]+]+/[\\w/-[,][+][\\(][\\)]]+(?=.*)";

    /**
	 * 
	 */
    public static final String YEAST_ORF_REGEX = "(?=.*)Y[A-Z]{2}[\\d]{3}[A-Z](?=.*)";

    /**
	 * 
	 * @param url
	 * @param regex
	 * @return Collection<String>
	 * @throws IOException
	 */
    public static Collection<String> getMatches(final URL url, final String regex) throws IOException {
        return getMatches(url, regex, 0);
    }

    /**
	 *
	 * @param is
	 * @param regex
	 * @return Collection
	 * @throws IOException
	 */
    public static Collection<String> getMatches(final InputStream is, final String regex) throws IOException {
        return getMatches(is, regex, 0);
    }

    /**
	 * 
	 * @param input
	 * @param regex
	 * @return Collection
	 */
    public static Collection<String> getMatches(final String input, final String regex) {
        return getMatches(input, regex, 0);
    }

    /**
	 * 
	 * @param url
	 * @param regex
	 * @return Collection<String>
	 * @throws IOException
	 */
    public static Collection<String> getAllMatches(final URL url, final String regex) throws IOException {
        return getAllMatches(url, regex, 0);
    }

    /**
	 *
	 * @param is
	 * @param regex
	 * @return Collection
	 * @throws IOException
	 */
    public static Collection<String> getAllMatches(final InputStream is, final String regex) throws IOException {
        return getAllMatches(is, regex, 0);
    }

    /**
	 * 
	 * @param input
	 * @param regex
	 * @return Collection
	 */
    public static Collection<String> getAllMatches(final String input, final String regex) {
        return getAllMatches(input, regex, 0);
    }

    /**
	 *
	 * @param url
	 * @param regex
	 * @param flags 
	 * @return Collection
	 * @throws IOException
	 */
    public static Collection<String> getMatches(final URL url, final String regex, final int flags) throws IOException {
        return new LinkedHashSet<String>(getAllMatches(url, regex, flags));
    }

    /**
	 *
	 * @param url
	 * @param regex
	 * @param flags 
	 * @return Collection
	 * @throws IOException
	 */
    public static Collection<String> getAllMatches(final URL url, final String regex, final int flags) throws IOException {
        InputStream is = null;
        try {
            is = url.openStream();
            return getAllMatches(is, regex, flags);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
	 *
	 * @param is
	 * @param regex
	 * @param flags 
	 * @return Collection
	 * @throws IOException
	 */
    public static Collection<String> getMatches(final InputStream is, final String regex, final int flags) throws IOException {
        return getMatches(new String(StreamReader.read(is)), regex, flags);
    }

    /**
	 *
	 * @param is
	 * @param regex
	 * @param flags 
	 * @return Collection
	 * @throws IOException
	 */
    public static Collection<String> getAllMatches(final InputStream is, final String regex, final int flags) throws IOException {
        return getAllMatches(new String(StreamReader.read(is)), regex, flags);
    }

    /**
	 * 
	 * @param input
	 * @param regex
	 * @param flags 
	 * @return Collection
	 */
    public static Collection<String> getMatches(final String input, final String regex, final int flags) {
        return new LinkedHashSet<String>(getAllMatches(input, regex, flags));
    }

    /**
	 * 
	 * @param input
	 * @param regex
	 * @param flags 
	 * @return Collection
	 */
    public static Collection<String> getAllMatches(final String input, final String regex, final int flags) {
        final Collection<String> matches = new ArrayList<String>();
        final Pattern pattern = Pattern.compile(regex, flags);
        final Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            matches.add(input.substring(matcher.start(), matcher.end()));
        }
        return matches;
    }
}
