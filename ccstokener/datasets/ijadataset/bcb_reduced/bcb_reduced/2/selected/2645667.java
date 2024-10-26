package org.neurox.esearch.chmoogle.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;
import org.neurox.esearch.chmoogle.internal.preferences.IPreferenceConstants;

public class ChmoogleSearchQuery implements ISearchQuery {

    private String pageHeader = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" + "\n" + "<html><head>" + "\n" + "<base href=\"http://www.chmoogle.com/\" >" + "\n" + "<title>Chmoogle Chemical Search Results</title>" + "\n" + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" + "\n" + "<script language=\"javascript\" type=\"text/javascript\" src=\"http://www.chmoogle.com/js/chmoogle.js\"></script> " + "\n" + "<script language=\"javascript\" type=\"text/javascript\" src=\"http://www.chmoogle.com/js/util.js\"></script> " + "\n" + "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://www.chmoogle.com/css/style.css\"></head><body>" + "\n";

    private String pageFooter = " </body></html>";

    private final ChmoogleSearchResult result = new ChmoogleSearchResult(this);

    String proxy = "167.62.62.7";

    String port = "8080";

    String username = "proxy";

    String password = "care";

    private URL url = null;

    private boolean exactMatch = true;

    private String searchString;

    /**
	 * substructure: http://chmoogle.com/cgi-bin/chemistry?t=ss&q=nicotine
	 * exact: http://chmoogle.com/cgi-bin/chemistry?t=ex&q=nicotine
	 */
    public ChmoogleSearchQuery(String searchString, boolean exactMatch) {
        this.searchString = searchString;
        this.exactMatch = exactMatch;
        try {
            setURL(createSearchURL(searchString));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public URL createSearchURL(String searchString) throws MalformedURLException {
        this.searchString = searchString;
        String urlString = "http://chmoogle.com/cgi-bin/chemistry?";
        urlString += exactMatch ? IPreferenceConstants.EXAXT : IPreferenceConstants.SUBSTRUCTURE;
        urlString += IPreferenceConstants.SEPARATOR;
        urlString += "q=" + searchString.trim();
        url = new URL(urlString);
        return url;
    }

    public ChmoogleSearchQuery() {
    }

    public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
        String content = pageHeader;
        InputStream is = null;
        try {
            Authenticator.setDefault(new SimpleAuthenticator(username, password));
            System.setProperty("http.proxyHost", proxy);
            System.setProperty("http.proxyPort", port);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            Parser p = new Parser(connection);
            HasAttributeFilter suppliertableFilter = new HasAttributeFilter();
            suppliertableFilter.setAttributeName("class");
            suppliertableFilter.setAttributeValue("suppliertable");
            HasAttributeFilter linksFilter = new HasAttributeFilter();
            linksFilter.setAttributeName("class");
            linksFilter.setAttributeValue("links");
            NotFilter notFilter = new NotFilter(linksFilter);
            AndFilter andFilter = new AndFilter(suppliertableFilter, notFilter);
            NodeList list = p.parse(andFilter);
            SimpleNodeIterator iterator = list.elements();
            while (iterator.hasMoreNodes()) {
                content += iterator.nextNode().toHtml();
            }
            content = content.replaceAll("/cgi-bin/", "http://www.chmoogle.com/cgi-bin/");
            content += pageFooter;
            result.setContent(content.trim());
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
            }
        }
        return Status.OK_STATUS;
    }

    public String getLabel() {
        return searchString;
    }

    public URL getURL() {
        return url;
    }

    public boolean canRerun() {
        return true;
    }

    public boolean canRunInBackground() {
        return true;
    }

    public ISearchResult getSearchResult() {
        return result;
    }
}
