package net.sf.jannot.source;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import java.util.zip.ZipException;
import net.sf.jannot.picard.LineBlockCompressedInputStream;
import net.sf.jannot.tabix.TabixWriter;
import net.sf.jannot.tabix.TabixWriter.Conf;
import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableHTTPStream;
import net.sf.samtools.util.SeekableStream;
import be.abeel.io.LineIterator;
import be.abeel.net.URIFactory;

/**
 * @author Thomas Abeel
 * 
 */
public class Locator {

    private static Logger log = Logger.getLogger(Locator.class.getCanonicalName());

    private String locator;

    private long length = -1;

    private boolean exists = false;

    private boolean streamCompressed = false;

    private boolean blockCompressed = false;

    private String ext;

    @Override
    public String toString() {
        return locator;
    }

    /**
	 * Removes the index extension from the file name
	 */
    public void stripIndex() {
        if (locator.endsWith(".mfi") || locator.endsWith(".tbi") || locator.endsWith(".bai") || locator.endsWith(".fai")) {
            locator = locator.substring(0, locator.length() - 4);
            init();
        }
    }

    public Locator(String l) {
        this.locator = l.trim();
        init();
    }

    /**
	 * 
	 */
    private void init() {
        String[] arr = locator.toString().toLowerCase().split("\\.");
        initExt(arr);
        if (isURL()) {
            initURL();
        } else initFile();
    }

    public boolean isStreamCompressed() {
        return streamCompressed;
    }

    public boolean isBlockCompressed() {
        return blockCompressed;
    }

    /**
	 * @param arr
	 */
    private void initExt(String[] arr) {
        streamCompressed = false;
        blockCompressed = false;
        ext = arr[arr.length - 1];
        if (arr[arr.length - 1].equals("bgz")) {
            ext = arr[arr.length - 2];
            blockCompressed = true;
        }
        if (arr[arr.length - 1].equals("gz")) {
            ext = arr[arr.length - 2];
            streamCompressed = true;
        }
    }

    /**
	 * 
	 */
    private void initURL() {
        try {
            log.fine("Checking: " + locator);
            URLConnection conn = URIFactory.url(locator).openConnection();
            conn.setUseCaches(false);
            log.info(conn.getHeaderFields().toString());
            String header = conn.getHeaderField(null);
            if (header.contains("404")) {
                log.info("404 file not found: " + locator);
                return;
            }
            if (header.contains("500")) {
                log.info("500 server error: " + locator);
                return;
            }
            if (conn.getContentLength() > 0) {
                byte[] buffer = new byte[50];
                conn.getInputStream().read(buffer);
                if (new String(buffer).trim().startsWith("<!DOCTYPE")) return;
            } else if (conn.getContentLength() == 0) {
                exists = true;
                return;
            }
            exists = true;
            length = conn.getContentLength();
        } catch (Exception ioe) {
            System.err.println(ioe);
        }
    }

    /**
	 * 
	 */
    private void initFile() {
        exists = new File(locator).exists();
        if (exists) length = new File(locator).length();
    }

    /**
	 * @param locator
	 * @return
	 */
    public String getPostfix() {
        if (isTabix()) return "tbi";
        if (isFasta()) return "fai";
        if (isBAM()) return "bai";
        if (isMaf()) return "mfi";
        return null;
    }

    public boolean isURL() {
        return locator.startsWith("http://") || locator.startsWith("https://") || locator.startsWith("file://");
    }

    public long length() {
        return length;
    }

    public boolean exists() {
        return exists;
    }

    /**
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
    public URL url() throws MalformedURLException, URISyntaxException {
        return URIFactory.url(locator);
    }

    public File file() {
        return new File(locator);
    }

    /**
	 * @return
	 */
    public boolean isWebservice() {
        return locator.indexOf('&') >= 0 || locator.indexOf('?') >= 0;
    }

    /**
	 * @return
	 */
    public boolean isTDF() {
        return ext.equals("tdf");
    }

    public boolean isWig() {
        return ext.equals("wig");
    }

    public boolean isBigWig() {
        return ext.equals("bw") || ext.equals("bigwig");
    }

    public boolean isTabix() {
        return ext.equals("gff") || ext.equals("gff3") || ext.equals("bed") || ext.equals("tsv") || ext.equals("pileup") || ext.equals("swig") || ext.equals("tab");
    }

    public boolean requiresIndex() {
        return (isMaf() && isBlockCompressed()) || isBAM() || ext.equals("tsv") || ext.equals("pileup") || ext.equals("swig") || ext.equals("tab");
    }

    public boolean recommendedIndex() {
        return requiresIndex() || isFasta() || isMaf();
    }

    public boolean supportsIndex() {
        return recommendedIndex() || requiresIndex() || ext.equals("gff") || ext.equals("gff3") || ext.equals("bed");
    }

    public boolean isBAM() {
        return ext.equals("bam");
    }

    public boolean isFasta() {
        return ext.equals("fasta") || ext.equals("fa") || ext.equals("fas") || ext.equals("con") || ext.equals("fna");
    }

    public boolean isMaf() {
        return ext.equals("maf");
    }

    /**
	 * @return
	 */
    public Conf getTabixConfiguration() {
        if (!isTabix()) return null;
        if (ext.equals("gff") || ext.equals("gff3")) {
            return TabixWriter.GFF_CONF;
        }
        if (ext.equals(ext.equals("bed"))) {
            return TabixWriter.BED_CONF;
        }
        Conf out = new Conf(0, 0, 0, 0, '#', 0);
        if (ext.equals("pileup") || ext.equals("swig") || ext.equals("tab") || ext.equals("tsv")) {
            out.chrColumn = 1;
            out.startColumn = 2;
            out.endColumn = 2;
        }
        return out;
    }

    /**
	 * @return
	 * @throws FileNotFoundException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
    public SeekableStream stream() throws FileNotFoundException, MalformedURLException, URISyntaxException {
        if (!isURL()) return new SeekableFileStream(this.file()); else return new SeekableHTTPStream(this.url());
    }

    public boolean isAnyCompressed() {
        return streamCompressed || blockCompressed;
    }

    public String getName() throws MalformedURLException, URISyntaxException {
        if (isURL()) {
            int slashIndex = url().getPath().lastIndexOf('/');
            return url().getPath().substring(slashIndex + 1);
        } else return file().getName();
    }
}
