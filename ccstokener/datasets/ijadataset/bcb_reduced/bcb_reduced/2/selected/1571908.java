package pl.taab.buildplmfe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author Administrator
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public abstract class WebWordValidator extends WordValidator {

    private ArrayList words;

    private String webQuery;

    private String nrResultsPattern;

    public WebWordValidator(String webQuery, String nrResultsPattern) throws IOException {
        super();
        this.webQuery = webQuery;
        this.nrResultsPattern = nrResultsPattern;
    }

    public final int wordFrequency(String word) {
        String replWebQuery = webQuery.replaceFirst("WORDREPLACE", word);
        try {
            URL url = new URL(replWebQuery);
            String content = url.toString();
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.matches(nrResultsPattern)) {
                    int fr = matchedLine(inputLine);
                    if (fr >= 0) {
                        return fr;
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
	 * @param inputLine
	 */
    protected abstract int matchedLine(String inputLine);
}
