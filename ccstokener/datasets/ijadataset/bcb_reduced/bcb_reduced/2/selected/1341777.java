package net.sf.jabref.imports;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;

/**
 * 
 * This class allows to access the Slac INSPIRE database. It is just a port of
 * the original SPIRES Fetcher.
 * 
 * It can either be a GeneralFetcher to pose requests to the database or fetch
 * individual entries.
 * 
 * @author Fedor Bezrukov
 * @author Sheer El-Showk
 * 
 * @version $Id$
 * 
 */
public class INSPIREFetcher implements EntryFetcher {

    private static String inspireHost = "inspirebeta.net";

    public INSPIREFetcher() {
    }

    /**
	 * Construct the query URL
	 *
	 * NOTE: we truncate at 1000 returned entries but its likely INSPIRE returns
	 * fewer anyway.  This shouldn't be a problem since users should probably do
	 * more specific searches.
	 * 
	 * @param key
	 *            The key of the OAI2 entry that the url should poitn to.
	 * 
	 * @return a String denoting the query URL
	 */
    public String constructUrl(String key) {
        String identifier = "";
        try {
            identifier = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
        StringBuffer sb = new StringBuffer("http://").append(inspireHost).append("/");
        sb.append("/search?ln=en&ln=en&p=find+");
        sb.append(identifier);
        sb.append("&action_search=Search&sf=&so=d&rm=&rg=1000&sc=0&of=hx");
        System.out.print("Inspire URL: " + sb.toString() + "\n");
        return sb.toString();
    }

    /**
	 * Import an entry from an OAI2 archive. The BibtexEntry provided has to
	 * have the field OAI2_IDENTIFIER_FIELD set to the search string.
	 * 
	 * @param key
	 *            The OAI2 key to fetch from ArXiv.
	 * @return The imnported BibtexEntry or null if none.
	 */
    private BibtexDatabase importInspireEntries(String key, OutputPrinter frame) {
        String url = constructUrl(key);
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            conn.setRequestProperty("User-Agent", "Jabref");
            InputStream inputStream = conn.getInputStream();
            INSPIREBibtexFilterReader reader = new INSPIREBibtexFilterReader(new InputStreamReader(inputStream));
            ParserResult pr = BibtexParser.parse(reader);
            return pr.getDatabase();
        } catch (IOException e) {
            frame.showMessage(Globals.lang("An Exception ocurred while accessing '%0'", url) + "\n\n" + e.toString(), Globals.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            frame.showMessage(Globals.lang("An Error occurred while fetching from INSPIRE source (%0):", new String[] { url }) + "\n\n" + e.getMessage(), Globals.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public String getHelpPage() {
        return "Spires.html";
    }

    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    public String getKeyName() {
        return "INSPIRE";
    }

    public JPanel getOptionsPanel() {
        return null;
    }

    public String getTitle() {
        return Globals.menuTitle(getKeyName());
    }

    public void cancelled() {
    }

    public void done(int entriesImported) {
    }

    public void stopFetching() {
    }

    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter frame) {
        try {
            frame.setStatus("Fetching entries from Inspire");
            BibtexDatabase bd = importInspireEntries(query, frame);
            frame.setStatus("Adding fetched entries");
            if (bd.getEntryCount() > 0) for (BibtexEntry entry : bd.getEntries()) dialog.addEntry(entry);
        } catch (Exception e) {
            frame.showMessage(Globals.lang("Error while fetching from Inspire: ") + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}
