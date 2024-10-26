package net.sf.jannot.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import net.sf.jannot.Entry;
import net.sf.jannot.EntrySet;
import net.sf.jannot.exception.ReadFailedException;
import net.sf.jannot.picard.SeekableFileCachedHTTPStream;
import net.sf.jannot.refseq.FaidxData;
import net.sf.jannot.refseq.FaidxIndex;
import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableStream;
import be.abeel.net.URIFactory;

/**
 * 
 * @author Thomas Abeel
 * 
 */
public class IndexedFastaDataSource extends DataSource {

    private SeekableStream content;

    private Locator index;

    private Locator data;

    /**
	 * @param data
	 * @param index
	 * @throws URISyntaxException
	 * @throws ReadFailedException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
    public IndexedFastaDataSource(Locator data, Locator index) throws MalformedURLException, IOException, ReadFailedException, URISyntaxException {
        super(data);
        if (data.isURL()) content = new SeekableFileCachedHTTPStream(data.url()); else content = new SeekableFileStream(data.file());
        this.index = index;
        this.data = data;
    }

    @Override
    public EntrySet read(EntrySet set) throws ReadFailedException {
        if (content == null) throw new RuntimeException("Boenk!");
        if (set == null) set = new EntrySet();
        InputStream iis = null;
        if (index.isURL()) try {
            iis = index.url().openStream();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        } else try {
            iis = new FileInputStream(index.file());
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        FaidxIndex index = new FaidxIndex(iis);
        for (String name : index.names()) {
            Entry e = set.getOrCreateEntry(name);
            try {
                e.setSequence(new FaidxData(index, content, name));
            } catch (Exception ex) {
                System.err.println("Faidx error locator: " + data);
                System.err.println("Faidx error index locator: " + index);
                throw new ReadFailedException(ex);
            }
        }
        return set;
    }

    @Override
    public String toString() {
        return content.toString();
    }

    @Override
    public void finalize() {
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public long size() {
        return data.length();
    }
}
