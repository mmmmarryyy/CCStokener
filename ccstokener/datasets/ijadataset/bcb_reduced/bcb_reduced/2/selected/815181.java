package org.apache.solr.servlet;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.core.SolrCore;
import org.apache.solr.util.AbstractSolrTestCase;

public class SolrRequestParserTest extends AbstractSolrTestCase {

    public String getSchemaFile() {
        return "schema.xml";
    }

    public String getSolrConfigFile() {
        return "solrconfig.xml";
    }

    SolrRequestParsers parser;

    public void setUp() throws Exception {
        super.setUp();
        parser = new SolrRequestParsers(h.getCore().getSolrConfig());
    }

    public void testStreamBody() throws Exception {
        String body1 = "AMANAPLANPANAMA";
        String body2 = "qwertasdfgzxcvb";
        String body3 = "1234567890";
        SolrCore core = h.getCore();
        Map<String, String[]> args = new HashMap<String, String[]>();
        args.put(CommonParams.STREAM_BODY, new String[] { body1 });
        List<ContentStream> streams = new ArrayList<ContentStream>();
        parser.buildRequestFrom(core, new MultiMapSolrParams(args), streams);
        assertEquals(1, streams.size());
        assertEquals(body1, IOUtils.toString(streams.get(0).getStream()));
        streams = new ArrayList<ContentStream>();
        args.put(CommonParams.STREAM_BODY, new String[] { body1, body2, body3 });
        parser.buildRequestFrom(core, new MultiMapSolrParams(args), streams);
        assertEquals(3, streams.size());
        ArrayList<String> input = new ArrayList<String>();
        ArrayList<String> output = new ArrayList<String>();
        input.add(body1);
        input.add(body2);
        input.add(body3);
        output.add(IOUtils.toString(streams.get(0).getStream()));
        output.add(IOUtils.toString(streams.get(1).getStream()));
        output.add(IOUtils.toString(streams.get(2).getStream()));
        Collections.sort(input);
        Collections.sort(output);
        assertEquals(input.toString(), output.toString());
        String ctype = "text/xxx";
        streams = new ArrayList<ContentStream>();
        args.put(CommonParams.STREAM_CONTENTTYPE, new String[] { ctype });
        parser.buildRequestFrom(core, new MultiMapSolrParams(args), streams);
        for (ContentStream s : streams) {
            assertEquals(ctype, s.getContentType());
        }
    }

    public void testStreamURL() throws Exception {
        boolean ok = false;
        String url = "http://www.apache.org/dist/lucene/solr/";
        String txt = null;
        try {
            txt = IOUtils.toString(new URL(url).openStream());
        } catch (Exception ex) {
            fail("this test only works if you have a network connection.");
            return;
        }
        SolrCore core = h.getCore();
        Map<String, String[]> args = new HashMap<String, String[]>();
        args.put(CommonParams.STREAM_URL, new String[] { url });
        List<ContentStream> streams = new ArrayList<ContentStream>();
        parser.buildRequestFrom(core, new MultiMapSolrParams(args), streams);
        assertEquals(1, streams.size());
        assertEquals(txt, IOUtils.toString(streams.get(0).getStream()));
    }

    public void testUrlParamParsing() {
        String[][] teststr = new String[][] { { "this is simple", "this%20is%20simple" }, { "this is simple", "this+is+simple" }, { "ü", "%C3%BC" }, { "&", "%26" }, { "€", "%E2%82%AC" } };
        for (String[] tst : teststr) {
            MultiMapSolrParams params = SolrRequestParsers.parseQueryString("val=" + tst[1]);
            assertEquals(tst[0], params.get("val"));
        }
    }

    public void testStandardParseParamsAndFillStreams() throws Exception {
        ArrayList<ContentStream> streams = new ArrayList<ContentStream>();
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("q", new String[] { "hello" });
        String[] ct = new String[] { "application/x-www-form-urlencoded", "Application/x-www-form-urlencoded", "application/x-www-form-urlencoded; charset=utf-8", "application/x-www-form-urlencoded;" };
        for (String contentType : ct) {
            HttpServletRequest request = createMock(HttpServletRequest.class);
            expect(request.getMethod()).andReturn("POST").anyTimes();
            expect(request.getContentType()).andReturn(contentType).anyTimes();
            expect(request.getParameterMap()).andReturn(params).anyTimes();
            replay(request);
            MultipartRequestParser multipart = new MultipartRequestParser(1000000);
            RawRequestParser raw = new RawRequestParser();
            StandardRequestParser standard = new StandardRequestParser(multipart, raw);
            SolrParams p = standard.parseParamsAndFillStreams(request, streams);
            assertEquals("contentType: " + contentType, "hello", p.get("q"));
        }
    }
}
