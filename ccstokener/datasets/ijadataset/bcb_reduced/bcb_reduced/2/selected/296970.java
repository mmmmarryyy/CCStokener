package com.m4f.utils.search.impl;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.m4f.utils.StackTraceUtil;
import com.m4f.business.service.ifc.URLShortenerService;
import com.m4f.utils.search.ifc.ISearchEngine;
import com.m4f.utils.search.ifc.ISearchParams;
import com.m4f.utils.search.ifc.ISearchResults;
import com.m4f.utils.search.ifc.ISearchParams.PARAM;
import com.m4f.utils.search.ifc.ISearchResults.METADATA;
import java.util.StringTokenizer;

public class GSASearchEngine implements ISearchEngine {

    private ISearchResults mResults;

    private ISearchParams mParameters;

    private URLShortenerService urlShortener;

    private static final Logger LOGGER = Logger.getLogger(GSASearchEngine.class.getName());

    public GSASearchEngine(ISearchResults mResults, URLShortenerService urlShortener) {
        super();
        this.mResults = mResults;
        this.urlShortener = urlShortener;
    }

    @Override
    public ISearchParams getSearchParemeters() {
        return mParameters;
    }

    @Override
    public void search(StringTokenizer terms, ISearchParams parameters) throws Exception {
        String query = "";
        while (terms.hasMoreElements()) {
            if ("".equals(query)) {
                query += terms.nextElement();
            } else {
                query += "+" + terms.nextElement();
            }
        }
        query = URLEncoder.encode(query, "UTF-8");
        parameters.addParam(PARAM.QUERY, query);
        mResults = new SearchResultsImpl();
        this.mParameters = parameters;
        URL urlGSA = new URL(this.getRequestSearchURL(parameters));
        URLConnection connection = urlGSA.openConnection();
        connection.setAllowUserInteraction(false);
        connection.setDoOutput(true);
        InputSource input = new InputSource(connection.getInputStream());
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        sp.parse(input, new ResultsGSAXMLParser());
    }

    @Override
    public ISearchResults getSearchResults() {
        return mResults;
    }

    private String getRequestSearchURL(ISearchParams parameters) {
        StringBuffer url = new StringBuffer(parameters.getParam(PARAM.SEARCH_URI));
        if (!url.toString().endsWith("?")) url.append("?");
        for (PARAM name : parameters) {
            String value = mParameters.getParam(name);
            if (PARAM.QUERY.equals(name)) {
                url.append("as_q=").append(value);
            } else if (PARAM.LANG.equals(name)) {
                url.append("&site=").append(parameters.getParam(PARAM.BASE_COLLECTION_NAME)).append(value);
            } else if (PARAM.START.equals(name)) {
                url.append("start=").append(value);
            } else if (PARAM.INMETA.equals(name)) {
                url.append("partialfields=").append(value);
            }
            if (!url.toString().endsWith("&")) url.append("&");
        }
        url.append("output=").append("xml_no_dtd");
        url.append("&");
        url.append("filter=0");
        url.append("&");
        url.append("client=" + parameters.getParam(PARAM.CLIENT));
        url.append("&date:D:S:d1");
        LOGGER.severe("### GSA search url: " + url);
        return url.toString();
    }

    private class ResultsGSAXMLParser extends DefaultHandler {

        private static final String ROOT = "";

        private static final String TM = "TM";

        private static final String PARAM = "PARAM";

        private static final String PARAM_NAME_ATT = "name";

        private static final String PARAM_VALUE_ATT = "value";

        private static final String PARAM_ORIGINALVALUE_ATT = "original_value";

        private static final String NAME_Q = "q";

        private static final String NAME_OUTPUT = "output";

        private static final String RES = "RES";

        private static final String RES_SN_ATT = "SN";

        private static final String RES_EN_ATT = "EN";

        private static final String RES_M = "M";

        private static final String RESULT = "R";

        private static final String RESULT_NUM = "";

        private static final String RESULT_SNIPPET = "S";

        private static final String RESULT_TITLE = "T";

        private static final String RESULT_SCORE = "SCOREBIAS";

        private static final String RESULT_LINK = "U";

        private static final String RESULT_LINK_ENCODED = "UE";

        private static final String RESULT_LANG = "LANG";

        private static final String RESULT_N_ATT = "N";

        private static final String RESULT_L_ATT = "L";

        private static final String RESULT_MIME_ATT = "MIME";

        private SearchResultImpl r;

        private StringBuffer sb;

        public ResultsGSAXMLParser() {
            super();
        }

        @Override
        public void startDocument() throws SAXException {
            this.sb = new StringBuffer();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (PARAM.equals(qName)) {
                if (NAME_Q.equals(attributes.getValue(PARAM_NAME_ATT))) {
                    mResults.addMetadata(METADATA.QUERY, attributes.getValue(PARAM_VALUE_ATT));
                } else if (NAME_OUTPUT.equals(attributes.getValue(PARAM_NAME_ATT))) {
                    mResults.addMetadata(METADATA.OUT_FORMAT, attributes.getValue(PARAM_VALUE_ATT));
                }
            } else if (RESULT.equals(qName)) {
                r = new SearchResultImpl();
                if (attributes.getValue(RESULT_MIME_ATT) != null) r.setMime(attributes.getValue(RESULT_MIME_ATT));
            }
            this.sb.delete(0, this.sb.length());
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (TM.equals(qName)) mResults.addMetadata(METADATA.TOTAL_TIME, this.sb.toString().trim()); else if (RES_M.equals(qName)) mResults.addMetadata(METADATA.TOTAL_RESULTS, this.sb.toString().trim()); else if (RESULT.equals(qName)) mResults.add(r); else if (RESULT_TITLE.equals(qName)) r.setTitle(this.sb.toString().trim()); else if (RESULT_LINK.equals(qName)) {
                String url = this.sb.toString().trim();
                try {
                    r.setLink(urlShortener.shortURL(url));
                } catch (Exception e) {
                    r.setLink(url);
                    LOGGER.severe(StackTraceUtil.getStackTrace(e));
                }
            } else if (RESULT_SNIPPET.equals(qName)) r.setDescription(this.sb.toString().trim()); else if (RESULT_LANG.equals(qName)) r.setLang(this.sb.toString().trim());
        }

        @Override
        public void endDocument() throws SAXException {
            this.sb = null;
            this.r = null;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            this.sb.append(new String(ch, start, length));
        }
    }
}
