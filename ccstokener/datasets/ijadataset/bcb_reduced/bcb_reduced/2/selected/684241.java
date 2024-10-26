package com.hp.hpl.jena.shared.wg;

import java.io.*;
import java.net.*;

/**
 *
 * In test cases we cannot open all the input files
 * while creating the test suite, but must defer the
 * opening until the test is actually run.
 * @author  jjc
 */
class LazyURLInputStream extends LazyInputStream {

    private URL url;

    /** Creates new LazyZipEntryInputStream */
    LazyURLInputStream(URL url) {
        this.url = url;
    }

    InputStream open() throws IOException {
        URLConnection conn = url.openConnection();
        return conn.getInputStream();
    }
}
