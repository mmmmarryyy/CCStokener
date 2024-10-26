package saadadb.util.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import saadadb.util.Messenger;
import saadadb.util.WorkDirectory;

/**
 * @author laurent
 * @version $Id: ZipMap.java 118 2012-01-06 14:33:51Z laurent.mistahl $
 */
public class ZipMap {

    private LinkedHashMap<String, Set<ZipEntryRef>> zipMap = new LinkedHashMap<String, Set<ZipEntryRef>>();

    public void put(String folder, Set<ZipEntryRef> entries) {
        zipMap.put(folder, entries);
    }

    /**
	 * Ad a Zip ref entry to the map node
	 * @param folder
	 * @param entry
	 */
    public void add(String folder, ZipEntryRef entry) {
        Set<ZipEntryRef> es = zipMap.get(folder);
        if (es == null) {
            es = new LinkedHashSet<ZipEntryRef>();
            this.zipMap.put(folder, es);
        }
        es.add(entry);
    }

    public Set<ZipEntryRef> get(String folder) {
        return zipMap.get(folder);
    }

    public Set<String> keySet() {
        return zipMap.keySet();
    }

    /**
	 * Copy and rename all files of the map into the reportdir.
	 * @param baseDir
	 * @throws Exception
	 */
    public void prepareDataFiles(String baseDir, String reportDir) throws Exception {
        WorkDirectory.emptyDirectory(new File(reportDir));
        for (Entry<String, Set<ZipEntryRef>> e : zipMap.entrySet()) {
            String node = e.getKey();
            String nodeDir = baseDir + File.separator + node;
            Set<ZipEntryRef> zers = e.getValue();
            LinkedHashSet<ZipEntryRef> statusZers = new LinkedHashSet<ZipEntryRef>();
            for (ZipEntryRef zer : zers) {
                if (zer.getType() == ZipEntryRef.QUERY_RESULT) {
                    this.prepareQueryResultData(zer, nodeDir, reportDir, statusZers);
                } else {
                    this.prepareSingleFileData(zer, nodeDir, reportDir);
                }
            }
            zers.addAll(statusZers);
        }
    }

    private void prepareSingleFileData(ZipEntryRef zer, String nodeDir, String reportDir) throws Exception {
        URL url = new URL(zer.getUri());
        URLConnection conn = url.openConnection();
        String fcopyName = reportDir + File.separator + zer.getFilenameFromHttpHeader(conn.getHeaderFields());
        if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "download " + zer.getUri() + " in " + fcopyName);
        BufferedOutputStream bw;
        bw = new BufferedOutputStream(new FileOutputStream(fcopyName));
        BufferedInputStream reader = new BufferedInputStream(conn.getInputStream());
        byte[] inputLine = new byte[100000];
        ;
        while (reader.read(inputLine) > 0) {
            bw.write(inputLine);
        }
        bw.close();
        reader.close();
        zer.setUri(fcopyName);
    }

    /**
	 * @param zer		 Zip Entry reference
	 * @param nodeDir    Base directory of the TAP node
	 * @param reportDir  Working directory of the ZIP builder
	 * @param statusZers Set of ZIP entries associated with status files added to the ZIP ball
	 * @throws Exception
	 */
    private void prepareQueryResultData(ZipEntryRef zer, String nodeDir, String reportDir, Set<ZipEntryRef> statusZers) throws Exception {
        String jobDir = nodeDir + File.separator + "job_" + zer.getUri();
        if (!WorkDirectory.isWorkingDirectoryValid(jobDir)) {
            throw new Exception("Cannot acces to " + jobDir);
        }
        File f = new File(jobDir + File.separator + "result.xml");
        if (!f.exists() || !f.isFile() || !f.canRead()) {
            throw new Exception("Cannot acces to result file " + f.getAbsolutePath());
        }
        String fcopyName = reportDir + File.separator + zer.getName() + ".xml";
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fcopyName));
        IOUtils.copy(bis, bos);
        bis.close();
        bos.close();
        zer.setUri(fcopyName);
        f = new File(jobDir + File.separator + "status.xml");
        if (!f.exists() || !f.isFile() || !f.canRead()) {
            throw new Exception("Cannot acces to status file " + f.getAbsolutePath());
        }
        fcopyName = reportDir + File.separator + zer.getName() + "_status.xml";
        bis = new BufferedInputStream(new FileInputStream(f));
        bos = new BufferedOutputStream(new FileOutputStream(fcopyName));
        IOUtils.copy(bis, bos);
        bis.close();
        bos.close();
        statusZers.add(new ZipEntryRef(ZipEntryRef.SINGLE_FILE, zer.getName(), fcopyName, ZipEntryRef.WITH_REL));
    }
}
