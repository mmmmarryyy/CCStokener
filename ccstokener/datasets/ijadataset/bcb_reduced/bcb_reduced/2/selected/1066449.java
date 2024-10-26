package com.itextpdf.tool.xml.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.itextpdf.text.log.Level;
import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import com.itextpdf.tool.xml.exceptions.LocaleMessages;
import com.itextpdf.tool.xml.exceptions.RuntimeWorkerException;

/**
 * @author redlab_b
 *
 */
public class FileRetrieveImpl implements FileRetrieve {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRetrieveImpl.class);

    private final List<File> rootdirs;

    private final List<String> urls;

    /**
	 *
	 */
    public FileRetrieveImpl() {
        rootdirs = new CopyOnWriteArrayList<File>();
        urls = new CopyOnWriteArrayList<String>();
    }

    /**
	 * Constructs a new FileRetrieveImpl with the given root url's and
	 * directories
	 *
	 * @param strings an array of strings, if the String starts with http or
	 *            https it's taken as URL otherwise we check if it's a directory
	 *            with
	 *
	 *            <pre>
	 * File f = new File(str);
	 * f.isDirectory()
	 * </pre>
	 */
    public FileRetrieveImpl(final String... strings) {
        this();
        for (String s : strings) {
            if (s.startsWith("http") || s.startsWith("https")) {
                urls.add(s);
            } else {
                File f = new File(s);
                if (f.isDirectory()) {
                    rootdirs.add(f);
                }
            }
        }
    }

    /**
	 * Constructs a new FileRetrieveImpl with the given root url's and
	 * directories
	 *
	 * @param strings an array of strings, if the String starts with http or
	 *            https it's taken as URL otherwise we check if it's a directory
	 *            with
	 *
	 *            <pre>
	 * File f = new File(str);
	 * f.isDirectory()
	 * </pre>
	 */
    public FileRetrieveImpl(File rootdir) {
        this();
        if (rootdir.isDirectory()) rootdirs.add(rootdir);
    }

    /**
	 * ProcessFromHref first tries to create an {@link URL} from the given <code>href</code>,
	 * if that throws a {@link MalformedURLException}, it will prepend the given
	 * root URLs to <code>href</code> until a valid URL is found.<br />If by then there is
	 * no valid url found, this method will see if the given <code>href</code> is a valid file
	 * and can read it.<br />If it's not a valid file or a file that can't be read,
	 * the given root directories will be set as root path with the given <code>href</code> as
	 * file path until a valid file has been found.
	 */
    public void processFromHref(final String href, final ReadingProcessor processor) throws IOException {
        if (LOGGER.isLogging(Level.DEBUG)) {
            LOGGER.debug(String.format(LocaleMessages.getInstance().getMessage("retrieve.file.from"), href));
        }
        URL url = null;
        File f = null;
        boolean isfile = false;
        try {
            url = new URL(href);
        } catch (MalformedURLException e) {
            try {
                url = detectWithRootUrls(href);
            } catch (MalformedURLException e1) {
                f = new File(href);
                isfile = true;
                if (!(f.isFile() && f.canRead())) {
                    isfile = false;
                    for (File root : rootdirs) {
                        f = new File(root, href);
                        if (f.isFile() && f.canRead()) {
                            isfile = true;
                            break;
                        }
                    }
                }
            }
        }
        InputStream in = null;
        if (null != url) {
            in = url.openStream();
        } else if (isfile) {
            in = new FileInputStream(f);
        } else {
            throw new IOException(LocaleMessages.getInstance().getMessage("retrieve.file.from.nothing"));
        }
        read(processor, in);
    }

    /**
	 * @param href the reference
	 * @throws MalformedURLException if no valid URL could be found.
	 */
    private URL detectWithRootUrls(final String href) throws MalformedURLException {
        for (String root : urls) {
            try {
                return new URL(root + href);
            } catch (MalformedURLException e) {
            }
        }
        throw new MalformedURLException();
    }

    public void processFromStream(final InputStream in, final ReadingProcessor processor) throws IOException {
        read(processor, in);
    }

    /**
     * @param processor
     * @param in
     * @throws IOException
     */
    private void read(final ReadingProcessor processor, final InputStream in) throws IOException {
        try {
            int inbit = -1;
            while ((inbit = in.read()) != -1) {
                processor.process(inbit);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException e) {
                throw new RuntimeWorkerException(e);
            }
        }
    }

    /**
	 * Add a root directory.
	 * @param dir the root directory
	 */
    public void addRootDir(final File dir) {
        rootdirs.add(dir);
    }

    /**
	 * Add a root URL.
	 * @param url the URL
	 */
    public void addURL(final String url) {
        urls.add(url);
    }
}
