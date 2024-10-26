package fr.cephb.lindenb.bio.ncbo.bioportal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class NCBOSearch {

    protected static class NCBOSearchBeanImpl implements NCBOSearchBean {

        private int ontologyVersionId;

        private int ontologyId;

        private String ontologyLabel;

        private String recordType;

        private String conceptId;

        private String conceptIdShort;

        private String preferredName;

        private String contents;

        public int getOntologyVersionId() {
            return ontologyVersionId;
        }

        public int getOntologyId() {
            return ontologyId;
        }

        public String getOntologyLabel() {
            return ontologyLabel;
        }

        public String getRecordType() {
            return recordType;
        }

        public String getConceptId() {
            return conceptId;
        }

        public String getConceptIdShort() {
            return conceptIdShort;
        }

        public String getPreferredName() {
            return preferredName;
        }

        public String getContents() {
            return contents;
        }

        @Override
        public String toString() {
            return "{" + getOntologyLabel() + " " + getConceptId() + " " + getContents() + "}";
        }
    }

    private Set<Integer> ontologyId = new HashSet<Integer>();

    private boolean exactmatch = true;

    private int pageIndex = 1;

    /** the number of results to display in a single request (default: all)  */
    private int resultCount = -1;

    private boolean includeproperties = false;

    /** match the entire concept name */
    public void setExactmatch(boolean exactmatch) {
        this.exactmatch = exactmatch;
    }

    public boolean isExactmatch() {
        return exactmatch;
    }

    public void setOntologyId(Set<Integer> ontologyId) {
        this.ontologyId.clear();
        this.ontologyId.addAll(ontologyId);
    }

    public Set<Integer> getOntologyId() {
        return ontologyId;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    /** the page number to display (pages are calculated using <total results>/<pagesize>) (default: 1)  */
    public int getPageIndex() {
        return pageIndex;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    /** the number of results to display in a single request (default: all)  */
    public int getResultCount() {
        return resultCount;
    }

    public void setIncludeproperties(boolean includeproperties) {
        this.includeproperties = includeproperties;
    }

    public boolean isIncludeproperties() {
        return includeproperties;
    }

    public List<NCBOSearchBean> search(String term) throws IOException, XMLStreamException {
        List<NCBOSearchBean> items = new ArrayList<NCBOSearchBean>(Math.max(1, getResultCount()));
        StringBuilder builder = new StringBuilder(NCBO.BIOPORTAL_URL);
        builder.append("/search/");
        builder.append(URLEncoder.encode(term, "UTF-8")).append("/?");
        builder.append("isexactmatch=").append(exactmatch ? 1 : 0);
        builder.append("&includeproperties=").append(includeproperties ? 1 : 0);
        if (getPageIndex() != 1) builder.append("&pagenum=" + getPageIndex());
        if (getResultCount() != -1) builder.append("&pagesize=" + getResultCount());
        if (!getOntologyId().isEmpty()) {
            builder.append("&ontologyids=");
            boolean comma = false;
            for (Integer id : getOntologyId()) {
                if (comma) builder.append(",");
                comma = true;
                builder.append(id);
            }
        }
        XMLInputFactory xf = XMLInputFactory.newInstance();
        xf.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xf.setProperty(XMLInputFactory.IS_VALIDATING, false);
        URL url = new URL(builder.toString());
        InputStream in = url.openStream();
        XMLEventReader r = xf.createXMLEventReader(in);
        NCBOSearchBeanImpl bean = null;
        while (r.hasNext()) {
            XMLEvent evt = r.nextEvent();
            if (evt.isStartElement()) {
                String name = evt.asStartElement().getName().getLocalPart();
                if (name.equals("searchBean")) {
                    bean = new NCBOSearchBeanImpl();
                } else if (bean != null) {
                    String content = r.getElementText();
                    if (name.equals("ontologyVersionId")) {
                        bean.ontologyVersionId = Integer.parseInt(content);
                    } else if (name.equals("ontologyId")) {
                        bean.ontologyId = Integer.parseInt(content);
                    } else if (name.equals("ontologyDisplayLabel")) {
                        bean.ontologyLabel = content;
                    } else if (name.equals("recordType")) {
                        bean.recordType = content;
                    } else if (name.equals("conceptId")) {
                        bean.conceptId = content;
                    } else if (name.equals("conceptIdShort")) {
                        bean.conceptIdShort = content;
                    } else if (name.equals("preferredName")) {
                        bean.preferredName = content;
                    } else if (name.equals("contents")) {
                        bean.contents = content;
                    }
                }
            } else if (evt.isEndElement()) {
                String name = evt.asEndElement().getName().getLocalPart();
                if (name.equals("searchBean") && bean != null) {
                    items.add(bean);
                    bean = null;
                }
            }
        }
        r.close();
        return items;
    }

    public static void main(String[] args) {
        try {
            NCBOSearch app = new NCBOSearch();
            app.setExactmatch(true);
            for (NCBOSearchBean item : app.search("date")) {
                System.out.println(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
