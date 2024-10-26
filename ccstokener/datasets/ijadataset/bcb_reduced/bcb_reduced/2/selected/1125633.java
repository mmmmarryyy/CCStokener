package org.apache.ws.jaxme.js.pattern;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import org.apache.ws.jaxme.js.JavaSource;
import org.apache.ws.jaxme.js.JavaSourceFactory;
import org.apache.ws.jaxme.js.util.JavaParser;
import antlr.RecognitionException;
import antlr.TokenStreamException;

/** Reflector for gathering information about a Java
 * source file.
 */
public class SourceReflector implements Reflector {

    private final File file;

    private final URL url;

    /** Creates a new <code>SourceReflector</code>, which
     * is going to read the Java source file <code>pFile</code>.
	 */
    public SourceReflector(File pFile) {
        file = pFile;
        url = null;
    }

    /** Creates a new <code>SourceReflector</code>, which
     * is going to read the Java source file from <code>pURL</code>.
     */
    public SourceReflector(URL pURL) {
        file = null;
        url = pURL;
    }

    public JavaSource getJavaSource(final JavaSourceFactory pFactory) throws RecognitionException, TokenStreamException, IOException {
        List result;
        if (file == null) {
            InputStream istream = null;
            try {
                istream = url.openStream();
                Reader r = new InputStreamReader(istream);
                result = new JavaParser(pFactory).parse(r);
                istream.close();
                istream = null;
            } finally {
                if (istream != null) {
                    try {
                        istream.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        } else {
            Reader r = null;
            try {
                r = new FileReader(file);
                result = new JavaParser(pFactory).parse(r);
                r.close();
                r = null;
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
        if (result.size() > 1) {
            throw new RecognitionException("The Java source file contained multiple classes.");
        }
        if (result.size() > 1) {
            throw new RecognitionException("The Java source file contained multiple classes.");
        }
        return (JavaSource) result.get(0);
    }
}
