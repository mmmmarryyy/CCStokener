package seismosurfer.update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import seismosurfer.data.QuakeData;
import seismosurfer.servlets.util.SessionHandler;
import seismosurfer.util.SeismoException;
import seismosurfer.util.Util;
import com.bbn.openmap.util.Debug;

/**
 * An extension to Updater for the PDE
 * catalog.
 *
 */
public class PDEUpdater extends NEICUpdater {

    public static final String TEMP = "pde";

    public PDEUpdater(int catalogID) {
        super(catalogID);
    }

    protected int readLines(BufferedReader in, int nlines) {
        String line;
        try {
            int count = 0;
            while (((line = in.readLine()) != null) && (count < nlines)) {
                count++;
                data.add(line);
            }
            if (line == null) {
                Util.close(in);
                return count;
            }
            return count;
        } catch (IOException e) {
            throw new SeismoException(e);
        }
    }

    public void doUpdate() {
        BufferedReader in = getDataReader();
        int count = 0;
        while ((count = readLines(in, 2000)) == 2000) {
            processData();
        }
        Debug.output("readLines:" + count);
        processData();
    }

    protected void processData() {
        parse();
        for (Iterator iter = filtered.iterator(); iter.hasNext(); ) {
            QuakeData item = (QuakeData) iter.next();
            if (check(item)) {
                insert(item);
            }
        }
        filtered.clear();
    }

    protected BufferedReader getDataReader() {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            String line;
            URL url = new URL(this.catalog.getCatalogURL());
            Debug.output("Catalog URL:" + url.toString());
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            File dir = (File) SessionHandler.getServletContext().getAttribute("javax.servlet.context.tempdir");
            File temp = new File(dir, TEMP);
            Debug.output("Temp file:" + temp.toString());
            out = new PrintWriter(new BufferedWriter(new FileWriter(temp)));
            while ((line = in.readLine()) != null) {
                out.println(line);
            }
            Debug.output("Temp file size:" + temp.length());
            return new BufferedReader(new FileReader(temp));
        } catch (IOException e) {
            throw new SeismoException(e);
        } finally {
            Util.close(in);
            Util.close(out);
        }
    }
}
