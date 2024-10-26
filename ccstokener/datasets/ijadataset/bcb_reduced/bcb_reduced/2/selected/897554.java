package ti.mcore.symtable.ti;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import ti.elfutil.ELFFile;
import ti.mcore.u.FileUtil;

/**
 * A handler for "x-elf" URLs, for example:
 * <pre>
 *   x-elf:file:/Volumes/MacintoshHD2/robclark/mybuild.axf!/mybuild.tdt
 * </pre>
 * This type of URL can be used to refer to files embedded within the ELF
 * file.
 */
class XElfProtocol extends AbstractURLStreamHandlerService {

    public URLConnection openConnection(URL url) {
        return new XElfConnection(url);
    }
}

class XElfConnection extends URLConnection {

    XElfConnection(URL url) {
        super(url);
    }

    public void connect() throws IOException {
    }

    public InputStream getInputStream() throws IOException {
        String url = getURL().toExternalForm();
        int idx = url.indexOf('!');
        if (idx < 0) throw new IOException("bad URL: " + url);
        String elfFilePath = url.substring("x-elf:".length(), idx);
        String dbName = url.substring(idx + 1);
        ELFFile elfFile = new ELFFile(FileUtil.getFile(new URL(elfFilePath)).getPath());
        for (int i = 0; i < elfFile.sections.length; i++) {
            ELFFile.Section sect = elfFile.sections[i];
            if ((sect.sh_type == 0x80000001L) && sect.sh_name.equals(".tidbg_" + dbName)) {
                return new XElfInputStream(elfFile, sect.sh_offset, sect.sh_size);
            }
        }
        throw new FileNotFoundException(url);
    }
}

class XElfInputStream extends InputStream {

    private ELFFile elfFile;

    private long position;

    private long remaining;

    XElfInputStream(ELFFile elfFile, long off, long len) {
        this.elfFile = elfFile;
        this.position = off;
        this.remaining = len;
    }

    public int read() throws IOException {
        synchronized (elfFile.file) {
            int len = prepareToRead(1);
            if (len == 1) return elfFile.file.read();
        }
        return -1;
    }

    public int read(byte b[], int off, int len) throws IOException {
        synchronized (elfFile.file) {
            len = prepareToRead(len);
            if (len > 0) return elfFile.file.read(b, off, len);
        }
        return -1;
    }

    public void close() throws IOException {
        elfFile.close();
    }

    /** 
	 * truncates 'len' to max # of bytes that can be read, and seeks to the
	 * position to start reading from, and updates the position and remaining
	 * bytes for the next read.  (Is there any case where the read could fail?) 
	 * 
	 * @param len
	 * @return
	 * @throws IOException 
	 */
    private int prepareToRead(int len) throws IOException {
        len = Math.min(len, (int) remaining);
        elfFile.file.seek(position);
        position += len;
        remaining -= len;
        return len;
    }
}
