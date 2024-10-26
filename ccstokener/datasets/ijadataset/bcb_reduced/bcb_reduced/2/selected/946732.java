package org.archive.crawler.extractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.HttpRecorder;

/**
 * Test html extractor.
 *
 * @author stack
 * @version $Revision: 4703 $, $Date: 2006-10-18 15:26:56 +0000 (Wed, 18 Oct 2006) $
 */
public class JerichoExtractorHTMLTest extends ExtractorHTMLTest implements CoreAttributeConstants {

    private final String ARCHIVE_DOT_ORG = "archive.org";

    private final String LINK_TO_FIND = "http://www.hewlett.org/";

    private HttpRecorder recorder = null;

    private JerichoExtractorHTML extractor = null;

    protected JerichoExtractorHTML createExtractor() throws InvalidAttributeValueException, AttributeNotFoundException, MBeanException, ReflectionException {
        final String name = this.getClass().getName();
        SettingsHandler handler = new XMLSettingsHandler(new File(getTmpDir(), name + ".order.xml"));
        handler.initialize();
        return (JerichoExtractorHTML) ((MapType) handler.getOrder().getAttribute(CrawlOrder.ATTR_RULES)).addElement(handler.getSettingsObject(null), new JerichoExtractorHTML(name));
    }

    protected void setUp() throws Exception {
        super.setUp();
        this.extractor = createExtractor();
        final boolean USE_NET = false;
        URL url = null;
        if (USE_NET) {
            url = new URL("http://" + this.ARCHIVE_DOT_ORG);
        } else {
            File f = new File(getTmpDir(), this.ARCHIVE_DOT_ORG + ".html");
            url = new URL("file://" + f.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(("<html><head><title>test</title><body>" + "<a href=" + this.LINK_TO_FIND + ">Hewlett Foundation</a>" + "</body></html>").getBytes());
            fos.flush();
            fos.close();
        }
        this.recorder = HttpRecorder.wrapInputStreamWithHttpRecord(getTmpDir(), this.getClass().getName(), url.openStream(), null);
    }

    public void testInnerProcess() throws IOException {
        UURI uuri = UURIFactory.getInstance("http://" + this.ARCHIVE_DOT_ORG);
        CrawlURI curi = setupCrawlURI(this.recorder, uuri.toString());
        this.extractor.innerProcess(curi);
        Collection links = curi.getOutLinks();
        boolean foundLinkToHewlettFoundation = false;
        for (Iterator i = links.iterator(); i.hasNext(); ) {
            Link link = (Link) i.next();
            if (link.getDestination().toString().equals(this.LINK_TO_FIND)) {
                foundLinkToHewlettFoundation = true;
                break;
            }
        }
        assertTrue("Did not find gif url", foundLinkToHewlettFoundation);
    }

    private CrawlURI setupCrawlURI(HttpRecorder rec, String url) throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance(url));
        curi.setContentSize(this.recorder.getRecordedInput().getSize());
        curi.setContentType("text/html");
        curi.setFetchStatus(200);
        curi.setHttpRecorder(rec);
        curi.putObject(CoreAttributeConstants.A_HTTP_TRANSACTION, new Object());
        return curi;
    }

    /**
     * Test a forms link extraction
     * 
     * @throws URIException
     */
    public void testFormsLink() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance("http://www.example.org"));
        CharSequence cs = "<form name=\"testform\" method=\"POST\" action=\"redirect_me?form=true\"> " + "  <INPUT TYPE=CHECKBOX NAME=\"checked[]\" VALUE=\"1\" CHECKED> " + "  <INPUT TYPE=CHECKBOX NAME=\"unchecked[]\" VALUE=\"1\"> " + "  <select name=\"selectBox\">" + "    <option value=\"selectedOption\" selected>option1</option>" + "    <option value=\"nonselectedOption\">option2</option>" + "  </select>" + "  <input type=\"submit\" name=\"test\" value=\"Go\">" + "</form>";
        this.extractor.extract(curi, cs);
        curi.getOutLinks();
        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {

            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf("/redirect_me?form=true&checked[]=1&unchecked[]=&selectBox=selectedOption&test=Go") >= 0;
            }
        }));
    }
}
