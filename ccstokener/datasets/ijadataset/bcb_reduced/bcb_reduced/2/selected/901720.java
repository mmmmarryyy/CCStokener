package ejb.bprocess.opac.xcql;

/**
 *
 * @author  Administrator
 */
public class SearchSRUServer implements Runnable {

    private org.jdom.output.XMLOutputter output = new org.jdom.output.XMLOutputter();

    private java.util.Hashtable records = new java.util.Hashtable();

    /** Creates a new instance of SearchSRUServer */
    public SearchSRUServer(String url, String startRecord, String maximumRecords, String query, String recordSchema, String serverId, String libraryId, java.util.Hashtable records, String serverName) {
        this.records = records;
        String error = "";
        String operation = "searchRetrieve";
        String recordPacking = "XML";
        String recordData = "XML";
        String version = "1.1";
        url += "?operation=searchRetrieve&recordSchema=" + recordSchema + "&version=" + version + "&query=" + query + "&startRecord=" + startRecord + "&maximumRecords=" + maximumRecords;
        java.net.URL urlJ = null;
        try {
            urlJ = new java.net.URL(url);
        } catch (Exception exp) {
            exp.printStackTrace();
            error = "INVALID_URL";
        }
        if (urlJ != null) {
            java.io.InputStream os = null;
            try {
                java.net.URLConnection urlconn = (java.net.URLConnection) urlJ.openConnection();
                urlconn.setDoOutput(true);
                os = urlconn.getInputStream();
            } catch (Exception exp) {
                exp.printStackTrace();
                error = "CANNOT_CONNECT_TO_SERVER";
            }
            if (os != null) {
                org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
                org.jdom.Document doc = null;
                try {
                    doc = sb.build(os);
                } catch (Exception exp) {
                    exp.printStackTrace();
                    error = "EXPLAIN_XML_INVALID";
                }
                if (doc != null) {
                    org.jdom.Namespace sruns = org.jdom.Namespace.getNamespace("http://www.loc.gov/zing/srw/");
                    java.util.List lirecords = doc.getRootElement().getChild("records", sruns).getChildren("record", sruns);
                    for (int i = 0; i < lirecords.size(); i++) {
                        org.jdom.Element eleRecord = (org.jdom.Element) lirecords.get(i);
                        String schemaSent = eleRecord.getChildTextTrim("recordSchema", sruns);
                        String originStr = ejb.bprocess.opac.RecordSchemas.getInstance().getOriginString(schemaSent);
                        org.jdom.Element recordRootOrig = (org.jdom.Element) eleRecord.getChild("recordData", sruns).getChildren().get(0);
                        org.jdom.Element recordRoot = (new ejb.bprocess.opac.AnySchemaToMARCConverter()).convert((org.jdom.Element) recordRootOrig.clone(), originStr);
                        org.jdom.Namespace marcns = recordRoot.getNamespace();
                        records.put(serverId + "_" + libraryId + "_" + i, getInitialDataFromMarcXML(recordRoot, marcns));
                        java.util.Hashtable htForLib = (java.util.Hashtable) records.get(serverId + "_" + libraryId + "_" + i);
                        htForLib.put("SERVER_NAME", serverName);
                    }
                }
            }
        }
    }

    public void run() {
    }

    public java.util.Hashtable getInitialDataFromMarcXML(org.jdom.Element recordRoot, org.jdom.Namespace marcns) {
        java.util.List li = recordRoot.getChildren("datafield", marcns);
        String title = "";
        String author = "";
        String yop = "";
        String url = "";
        for (int i = 0; i < li.size(); i++) {
            org.jdom.Element dataele = (org.jdom.Element) li.get(i);
            String tag = dataele.getAttributeValue("tag");
            if (tag.equals("245")) {
                java.util.List lisubs = dataele.getChildren();
                String a = "";
                String b = "";
                String c = "";
                for (int j = 0; j < lisubs.size(); j++) {
                    org.jdom.Element elesub = (org.jdom.Element) lisubs.get(j);
                    String code = elesub.getAttributeValue("code");
                    if (code.equals("a")) {
                        a = elesub.getTextTrim();
                    } else if (code.equals("b")) {
                        b = elesub.getTextTrim();
                    } else if (code.equals("c")) {
                        c = elesub.getTextTrim();
                    }
                }
                title = a;
                if (!b.equals("")) {
                    title += " : " + b;
                }
                if (!c.equals("")) {
                    title += " / " + c;
                }
            }
            if (tag.equals("100")) {
                java.util.List lisubs = dataele.getChildren();
                String a = "";
                String rest = "";
                for (int j = 0; j < lisubs.size(); j++) {
                    org.jdom.Element elesub = (org.jdom.Element) lisubs.get(j);
                    String code = elesub.getAttributeValue("code");
                    if (code.equals("a")) {
                        a = elesub.getTextTrim();
                    } else {
                        rest += " -- " + elesub.getTextTrim();
                    }
                }
                author = a;
                if (!rest.equals("")) {
                    author += " -- " + rest;
                }
            }
            if (tag.equals("110")) {
                java.util.List lisubs = dataele.getChildren();
                String a = "";
                String rest = "";
                for (int j = 0; j < lisubs.size(); j++) {
                    org.jdom.Element elesub = (org.jdom.Element) lisubs.get(j);
                    String code = elesub.getAttributeValue("code");
                    if (code.equals("a")) {
                        a = elesub.getTextTrim();
                    } else {
                        rest += " -- " + elesub.getTextTrim();
                    }
                }
                author = a;
                if (!rest.equals("")) {
                    author += " -- " + rest;
                }
            }
            if (tag.equals("111")) {
                java.util.List lisubs = dataele.getChildren();
                String a = "";
                String rest = "";
                for (int j = 0; j < lisubs.size(); j++) {
                    org.jdom.Element elesub = (org.jdom.Element) lisubs.get(j);
                    String code = elesub.getAttributeValue("code");
                    if (code.equals("a")) {
                        a = elesub.getTextTrim();
                    } else {
                        rest += " -- " + elesub.getTextTrim();
                    }
                }
                author = a;
                if (!rest.equals("")) {
                    author += " -- " + rest;
                }
            }
            if (tag.equals("260")) {
                java.util.List lisubs = dataele.getChildren();
                String a = "";
                for (int j = 0; j < lisubs.size(); j++) {
                    org.jdom.Element elesub = (org.jdom.Element) lisubs.get(j);
                    String code = elesub.getAttributeValue("code");
                    if (code.equals("c")) {
                        a = elesub.getTextTrim();
                    }
                }
                yop = a;
            }
            if (tag.equals("856")) {
                java.util.List lisubs = dataele.getChildren();
                String a = "";
                for (int j = 0; j < lisubs.size(); j++) {
                    org.jdom.Element elesub = (org.jdom.Element) lisubs.get(j);
                    String code = elesub.getAttributeValue("code");
                    if (code.equals("u")) {
                        a = elesub.getTextTrim();
                    }
                }
                url = a;
            }
        }
        java.util.Hashtable htReturn = new java.util.Hashtable();
        org.jdom.Document doc = new org.jdom.Document((org.jdom.Element) recordRoot.clone());
        htReturn.put("TITLE", title);
        htReturn.put("AUTHOR", author);
        htReturn.put("YEAR", yop);
        htReturn.put("URL", url);
        htReturn.put("XMLDUMP", output.outputString(doc));
        return htReturn;
    }

    /** Getter for property records.
     * @return Value of property records.
     *
     */
    public java.util.Hashtable getRecords() {
        return records;
    }

    /** Setter for property records.
     * @param records New value of property records.
     *
     */
    public void setRecords(java.util.Hashtable records) {
        this.records = records;
    }
}
