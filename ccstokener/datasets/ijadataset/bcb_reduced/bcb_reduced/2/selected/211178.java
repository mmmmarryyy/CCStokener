package org.apache.batik.apps.rasterizer;

import java.io.IOException;
import java.io.InputStream;
import org.apache.batik.util.ParsedURL;

public class SVGConverterURLSource implements SVGConverterSource {

    /** 
     * SVG file extension 
     */
    protected static final String SVG_EXTENSION = ".svg";

    protected static final String SVGZ_EXTENSION = ".svgz";

    public static final String ERROR_INVALID_URL = "SVGConverterURLSource.error.invalid.url";

    ParsedURL purl;

    String name;

    public SVGConverterURLSource(String url) throws SVGConverterException {
        this.purl = new ParsedURL(url);
        String path = this.purl.getPath();
        int n = path.lastIndexOf('/');
        String file = path;
        if (n != -1) {
            file = path.substring(n + 1);
        }
        if (file.length() == 0) {
            int idx = path.lastIndexOf('/', n - 1);
            file = path.substring(idx + 1, n);
        }
        if (file.length() == 0) {
            throw new SVGConverterException(ERROR_INVALID_URL, new Object[] { url });
        }
        n = file.indexOf('?');
        String args = "";
        if (n != -1) {
            args = file.substring(n + 1);
            file = file.substring(0, n);
        }
        name = file;
        String ref = this.purl.getRef();
        if ((ref != null) && (ref.length() != 0)) {
            name += "_" + ref.hashCode();
        }
        if ((args != null) && (args.length() != 0)) {
            name += "_" + args.hashCode();
        }
    }

    public String toString() {
        return purl.toString();
    }

    public String getURI() {
        return toString();
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof SVGConverterURLSource)) {
            return false;
        }
        return purl.equals(((SVGConverterURLSource) o).purl);
    }

    public int hashCode() {
        return purl.hashCode();
    }

    public InputStream openStream() throws IOException {
        return purl.openStream();
    }

    public boolean isSameAs(String srcStr) {
        return toString().equals(srcStr);
    }

    public boolean isReadable() {
        return true;
    }

    public String getName() {
        return name;
    }
}
