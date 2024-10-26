package au.edu.educationau.opensource.dsm.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.ArrayUtils;
import au.edu.educationau.opensource.dsm.obj.SearchCriteria;
import au.edu.educationau.opensource.dsm.service.PropertiesService;

/** Utility methods for DSM */
public class EducationAuUtils {

    /**
	 * Used in DiskCache:
	 * 
	 * Obtain data from the url and store it with the prepend to the filename to
	 * the root.
	 * 
	 * @param urlStr
	 * @param root
	 * @param prepend
	 * @return Object [] - [0] = java.lang.Integer response code, [1] =
	 *         java.io.File Object
	 * 
	 * @exception IOException
	 */
    public static Object[] fetch(String urlStr, String hashCode, String root, String prepend, String encoding) throws IOException {
        if (urlStr == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        Object[] fetchObj = new Object[2];
        Integer responseCode = new Integer(0);
        File file = new File(root + File.separator + prepend + hashCode);
        try {
            URL url = null;
            if (urlStr.indexOf(":", 5) > 5 && urlStr.indexOf("@") > 4) {
                String tmpUrl = "http://" + urlStr.substring(urlStr.indexOf("@") + 1);
                String uname = urlStr.substring(7, urlStr.indexOf(":", 7));
                String pass = urlStr.substring(urlStr.indexOf(":", 7) + 1, urlStr.indexOf("@"));
                java.net.Authenticator.setDefault(new MilspecAuthenticator(uname, pass));
                url = new URL(tmpUrl);
            } else {
                url = new URL(urlStr);
            }
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.connect();
            responseCode = new Integer(urlConn.getResponseCode());
            String line = "";
            InputStreamReader isreader = new InputStreamReader(urlConn.getInputStream(), "UTF8");
            BufferedReader reader = new BufferedReader(isreader);
            OutputStreamWriter owriter = new OutputStreamWriter(new FileOutputStream(file, false), "UTF8");
            BufferedWriter writer = new BufferedWriter(owriter);
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<!DOCTYPE") || line.startsWith("<!doctype")) {
                    continue;
                }
                writer.write(line);
                writer.newLine();
            }
            try {
                reader.close();
            } catch (Exception ioe) {
            }
            try {
                writer.close();
            } catch (Exception ioe) {
            }
        } catch (IOException ie) {
            throw new IOException("" + responseCode.intValue() + ":" + ie.getMessage());
        } catch (Exception e) {
            Flog.warn(classStr(), "Could not complete fetch. " + e.getMessage());
        }
        fetchObj[0] = responseCode;
        fetchObj[1] = file;
        System.out.println(urlStr + " returned " + file.getAbsolutePath());
        return fetchObj;
    }

    /**
	 * Quick fetch, returns the data as a String
	 * 
	 * @param urlStr
	 * @exception IOException
	 */
    public static String fetchUrl(String urlStr) throws IOException {
        URL url = null;
        if (urlStr == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        if (urlStr.indexOf(":", 5) > 5 && urlStr.indexOf("@") > 4) {
            String tmpUrl = "http://" + urlStr.substring(urlStr.indexOf("@") + 1);
            String uname = urlStr.substring(7, urlStr.indexOf(":", 7));
            String pass = urlStr.substring(urlStr.indexOf(":", 7) + 1, urlStr.indexOf("@"));
            java.net.Authenticator.setDefault(new MilspecAuthenticator(uname, pass));
            url = new URL(tmpUrl);
        } else {
            url = new URL(urlStr);
        }
        URLConnection urlConn = url.openConnection();
        BufferedInputStream is = new BufferedInputStream(urlConn.getInputStream());
        BufferedReader asciiReader = new BufferedReader(new InputStreamReader(is, "ASCII"));
        StringBuffer data = new StringBuffer();
        String line = "";
        while ((line = asciiReader.readLine()) != null) {
            data.append(line);
        }
        is.close();
        return data.toString();
    }

    /**
	 * Compresses the URL to reduce file name length
	 * 
	 * @param url
	 */
    public static String compressUrl(String url) {
        StringBuffer shortened = new StringBuffer(url);
        shortened = StringUtils.replaceAll(shortened, "&action=", "&-ac=");
        shortened = StringUtils.replaceAll(shortened, "&keywords=", "&-ky=");
        shortened = StringUtils.replaceAll(shortened, "&sourceResource=", "&-sR=");
        shortened = StringUtils.replaceAll(shortened, "&constraint=", "&-co=");
        shortened = StringUtils.replaceAll(shortened, "&maxResults=", "&-mR=");
        shortened = StringUtils.replaceAll(shortened, "&batchSize=", "&-bS=");
        shortened = StringUtils.replaceAll(shortened, "&caseSensitive=", "&-cS=");
        shortened = StringUtils.replaceAll(shortened, "&thesaurusType=", "&-tT=");
        shortened = StringUtils.replaceAll(shortened, "&elementConstraint=", "&-eC=");
        return shortened.toString();
    }

    /**
	 * expands the compressed URL to its former glory
	 * 
	 * @param url
	 */
    public static String expandUrl(String url) {
        StringBuffer lengthened = new StringBuffer(url);
        lengthened = StringUtils.replaceAll(lengthened, "&-ac=", "&action=");
        lengthened = StringUtils.replaceAll(lengthened, "&-ky=", "&keywords=");
        lengthened = StringUtils.replaceAll(lengthened, "&-sR=", "&sourceResource=");
        lengthened = StringUtils.replaceAll(lengthened, "&-co=", "&constraint=");
        lengthened = StringUtils.replaceAll(lengthened, "&-mR=", "&maxResults=");
        lengthened = StringUtils.replaceAll(lengthened, "&-bS=", "&batchSize=");
        lengthened = StringUtils.replaceAll(lengthened, "&-cS=", "&caseSensitive=");
        lengthened = StringUtils.replaceAll(lengthened, "&-tT=", "&thesaurusType=");
        lengthened = StringUtils.replaceAll(lengthened, "&-eC=", "&elementConstraint=");
        return lengthened.toString();
    }

    /**
	 * returns an integer from a string and the min and max values are set to
	 * MIN_VALUE and MAX_VALUE
	 * 
	 * @param value
	 */
    public static int getInteger(String value) {
        return getInteger(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
	 * returns an integer from a string and the min and max values are set to
	 * min and max
	 * 
	 * @param value
	 * @param min
	 * @param max
	 */
    public static int getInteger(String value, int min, int max) {
        int retVal = min;
        try {
            if (null != value) {
                retVal = Integer.parseInt(value);
                if (retVal < min) {
                    retVal = min;
                }
                if (retVal > max) {
                    retVal = max;
                }
            }
        } catch (Exception o) {
            retVal = min;
        }
        return retVal;
    }

    /**
	 * returns a long value from a string and defaults to the oldValue if
	 * conversion is not possible
	 * 
	 * @param value
	 * @param oldValue
	 */
    public static long getLong(String value, long oldValue) {
        long retVal = oldValue;
        try {
            if (null != value) {
                retVal = Long.parseLong(value);
            }
        } catch (NumberFormatException o) {
            retVal = oldValue;
        }
        return retVal;
    }

    /**
	 * Sends an email from a user to the recipients with subject and message.
	 * 
	 * @param recipients
	 * @param subject
	 * @param message
	 * @param from
	 * @exception MessagingException
	 */
    public static void postMail(String recipients[], String subject, String message, String from) throws MessagingException {
        boolean debug = false;
        Properties props = new Properties();
        props.put("mail.smtp.host", PropertiesService.getStringProperty("mail.server", "127.0.0.1"));
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(debug);
        Message msg = new MimeMessage(session);
        InternetAddress addressFrom = new InternetAddress(from);
        msg.setFrom(addressFrom);
        InternetAddress[] addressTo = new InternetAddress[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            addressTo[i] = new InternetAddress(recipients[i]);
        }
        msg.setRecipients(Message.RecipientType.TO, addressTo);
        msg.addHeader("MyHeaderName", "myHeaderValue");
        msg.setSubject(subject);
        msg.setContent(message, "text/plain");
        Transport.send(msg);
    }

    /**
	 * SOAP Envelope builder. Opens connection and sends wrapped body
	 * 
	 * @param body
	 * @param host
	 * @param soapAction
	 * @exception IOException
	 */
    public static String SOAPTransation(String body, URL host, String soapAction) throws IOException {
        StringBuffer content = new StringBuffer();
        content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        content.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        content.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        content.append(" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        content.append("<soap:Body>");
        content.append(body);
        content.append("</soap:Body>");
        content.append("</soap:Envelope>");
        URLConnection conn = host.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setRequestProperty("SOAPAction", soapAction);
        DataOutputStream printout = new DataOutputStream(conn.getOutputStream());
        printout.writeBytes(content.toString());
        printout.flush();
        printout.close();
        BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
        BufferedReader asciiReader = new BufferedReader(new InputStreamReader(is, "ASCII"));
        StringBuffer data = new StringBuffer();
        while (asciiReader.ready()) {
            data.append(asciiReader.readLine());
        }
        return data.toString();
    }

    /**
	 * removeDuplicates - removes duplicate entries from the array
	 * 
	 * @param srcArray
	 *            the source array
	 * 
	 */
    public static String[] removeDuplicates(String[] srcArray) {
        Set srcSet = new TreeSet(Arrays.asList(srcArray));
        return (String[]) srcSet.toArray(new String[srcSet.size()]);
    }

    /**
	 * Returns true if the argument String array contains the argument String.
	 */
    public static boolean arrayContains(String[] srcArray, String toFind) {
        for (int i = 0; i < srcArray.length; i++) {
            if (toFind == null) {
                if (srcArray[i] == null) {
                    return true;
                }
            } else if (toFind.equals(srcArray[i])) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Returns all values in hash whose keys represent Strings with a given
	 * prefix. Map may be empty but will never be null.
	 */
    public static Map getSubmapByKeyPrefix(Map map, String prefix, boolean removePrefixFromKeys) {
        Map submap = new HashMap();
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = entry.getKey().toString();
            if (key.startsWith(prefix)) {
                if (removePrefixFromKeys) key = key.substring(prefix.length());
                submap.put(key, entry.getValue());
            }
        }
        return submap;
    }

    /**
	 * Returns all values for a named SearchCriteria custom param as a single
	 * delimited String.
	 * 
	 * @param criteria
	 *            The SearchCriteria containing the custom params.
	 * @param paramName
	 *            The name of the custom params to find. (this should be fully
	 *            qualified, eg "adapterCode.paramName").
	 * @param delimiter
	 *            The delimter to use between param values in the in the
	 *            returned String.
	 * 
	 * @return delimited String
	 */
    public static String concatCustomParamValues(SearchCriteria criteria, String paramName, String delimiter) {
        String[] values = criteria.getCustomParamValues(paramName);
        if (values != null) {
            StringBuffer sb = new StringBuffer();
            String value;
            for (int i = 0; i < values.length; i++) {
                value = values[i];
                if (value != null && value.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(delimiter);
                    }
                    sb.append(value);
                }
            }
            return sb.toString();
        }
        return null;
    }

    /**
	 * Returns the Verity Syntax for a query specified by particular custom
	 * params for an adapter code, if any are present.
	 * 
	 * The custom params used are: 1) 'quartets' of "mutliqueryText",
	 * "srcMetadata" and optionally "multiqueryOp" and "multiqueryKC" (multiple
	 * quartets can be present); 2) "country"; 3) "sector" (multiple sectors can
	 * be present); 4) "eventgroup" (multiple evengroups can be present).
	 * 
	 * A String array identifying relationalFields can be passed. This contains
	 * the names of fields (with leading '_' replaced by ':') which are to be
	 * searched using Verity's relational syntax for numeric and date fields.
	 * For these fields, the query text passed can contain a relational operator
	 * (eg. '> 2001/02/01'); and if it doesn't: '=' is assumed.
	 * 
	 * In addition, a Map of fieldScopes can be passed. This can be used to turn
	 * a search in a single field, into a search across multiple fields (ORed
	 * together).
	 * 
	 * The key of the map is the name of a field (with leading '_' replaced by
	 * ':'). The values are arrays of Strings, which identify the fields in
	 * which the search will actually occur (again with leading '_' replaced by
	 * ':').
	 * 
	 * Eg: this is used by EdNA2, to turn a search in dc:date into a search in
	 * dc:date or dc:date_created or dc:date_issued etc.
	 * 
	 * @param criteria
	 *            The SearchCriteria containing the custom params.
	 * @param adapterCode
	 *            The adapter code of custom params to use.
	 * @param relationalFields
	 *            Optional param specifying names of fields to be searched using
	 *            relational syntax for numerics and dates.
	 * @param fieldScopes
	 *            An optional Map containing actual fields to search on for a
	 *            srcMetadata value. Can be null.
	 * 
	 * @return String containing a Verity syntax query, or null if no
	 *         multiqueryText params are present in the criteria.
	 */
    public static String convertMQParamsToVeritySyntax(SearchCriteria criteria, String adapterCode, String[] relationalFields, Map fieldScopes) {
        String[] multiqueryTexts = criteria.getCustomParamValues(adapterCode + ".multiqueryText");
        if (ArrayUtils.isEmpty(multiqueryTexts)) {
            multiqueryTexts = criteria.getCustomParamValues(adapterCode + ".mq");
        }
        if (multiqueryTexts == null || multiqueryTexts.length == 0) {
            return null;
        }
        String[] multiqueryOps = criteria.getCustomParamValues(adapterCode + ".multiqueryOp");
        if (ArrayUtils.isEmpty(multiqueryOps)) {
            multiqueryOps = criteria.getCustomParamValues(adapterCode + ".qOp");
        }
        String[] multiqueryKCs = criteria.getCustomParamValues(adapterCode + ".multiqueryKC");
        String[] srcMetadatas = criteria.getCustomParamValues(adapterCode + ".srcMetadata");
        if (ArrayUtils.isEmpty(srcMetadatas)) {
            srcMetadatas = criteria.getCustomParamValues(adapterCode + ".srMd");
        }
        Collection fieldPhrases = new ArrayList();
        for (int i = 0; i < multiqueryTexts.length; i++) {
            String query;
            boolean containsQuotes = false;
            if (criteria.isCaseSensitive()) {
                query = multiqueryTexts[i].toLowerCase();
            } else {
                query = multiqueryTexts[i];
            }
            if (query.indexOf("\"") >= 0) {
                containsQuotes = true;
            }
            if (query != null && query.length() > 0) {
                String op = (multiqueryOps != null && multiqueryOps.length > i) ? multiqueryOps[i] : "and";
                String kc = (multiqueryKCs != null && multiqueryKCs.length > i) ? multiqueryKCs[i] : "all";
                String field = (srcMetadatas != null && srcMetadatas.length > i) ? srcMetadatas[i] : "";
                if (containsQuotes) {
                    kc = "phrase";
                    query = org.apache.commons.lang.StringUtils.remove(query, "\"");
                }
                field = field.replaceFirst("_", ":");
                boolean relational = (relationalFields != null && arrayContains(relationalFields, field));
                FieldPhrase fp = new FieldPhrase(field, query, kc, op, relational);
                fieldPhrases.add(fp);
            }
        }
        StringBuffer verityQry = new StringBuffer();
        verityQry.append('(');
        Iterator it = fieldPhrases.iterator();
        while (it.hasNext()) {
            FieldPhrase fp = (FieldPhrase) it.next();
            if (verityQry.length() > 1) {
                if ("or".equalsIgnoreCase(fp.getOp())) {
                    verityQry.append(" <OR> ");
                } else if ("not".equalsIgnoreCase(fp.getOp())) {
                    verityQry.append(" <AND><NOT> ");
                } else {
                    verityQry.append(" <AND> ");
                }
            } else if ("not".equalsIgnoreCase(fp.getOp())) {
                verityQry.append("<NOT> ");
            }
            String[] fieldScope = (fieldScopes != null) ? (String[]) fieldScopes.get(fp.getField()) : null;
            if (fieldScope == null || fieldScope.length == 0) {
                appendFieldConditionToVerityQry(verityQry, fp);
            } else if (fieldScope.length == 1) {
                fp.setField(fieldScope[0]);
                appendFieldConditionToVerityQry(verityQry, fp);
            } else {
                verityQry.append('(');
                for (int i = 0; i < fieldScope.length; i++) {
                    if (i > 0) {
                        verityQry.append(" <OR> ");
                    }
                    fp.setField(fieldScope[i]);
                    appendFieldConditionToVerityQry(verityQry, fp);
                }
                verityQry.append(')');
            }
        }
        verityQry.append(')');
        String country = criteria.getCustomParamValue(adapterCode + ".country");
        ;
        if (country != null && country.length() > 0 && !country.equalsIgnoreCase("all")) {
            if (verityQry.length() > 0) {
                verityQry.append(" <AND><YESNO>");
            }
            verityQry.append(" country_").append(country);
        }
        String[] sectors = criteria.getCustomParamValues(adapterCode + ".sector");
        if (sectors != null && sectors.length > 0) {
            if (verityQry.length() > 0) {
                verityQry.append(" <AND>");
            }
            boolean sectorsStarted = false;
            String sector;
            verityQry.append(" (");
            for (int i = 0; i < sectors.length; i++) {
                sector = sectors[i];
                if (sector != null && sector.length() > 0) {
                    if (sectorsStarted) {
                        verityQry.append(" <OR> ");
                    } else {
                        sectorsStarted = true;
                    }
                    verityQry.append("<YESNO> sector_").append(sector);
                    verityQry.append(" <OR> ").append(sector).append(" <IN> categories");
                }
            }
            verityQry.append(')');
        }
        String[] eventGroups = criteria.getCustomParamValues(adapterCode + ".eventgroup");
        if (eventGroups != null && eventGroups.length > 0) {
            if (verityQry.length() > 0) {
                verityQry.append(" <AND><YESNO>");
            }
            boolean eventGroupsStarted = false;
            String eventGroup;
            verityQry.append(" (");
            for (int i = 0; i < eventGroups.length; i++) {
                eventGroup = eventGroups[i];
                if (eventGroup != null && eventGroup.length() > 0) {
                    if (eventGroupsStarted) {
                        verityQry.append(" <OR>");
                    } else {
                        eventGroupsStarted = true;
                    }
                    verityQry.append(" eventgroup_").append(eventGroup);
                }
            }
            verityQry.append(')');
        }
        String[] resources = criteria.getCustomParamValues(adapterCode + ".rs");
        if (resources != null && resources.length > 0) {
            if (verityQry.length() > 0) {
                verityQry.append(" <AND><YESNO>");
            }
            boolean resourcesStarted = false;
            String resource;
            verityQry.append(" (");
            for (int i = 0; i < resources.length; i++) {
                resource = resources[i];
                if (resource != null && resource.length() > 0) {
                    if (resourcesStarted) {
                        verityQry.append(" <OR>");
                    } else {
                        resourcesStarted = true;
                    }
                    if (resource.equals("aud")) {
                        verityQry.append(" Sound <IN> DC:Type");
                    } else if (resource.equals("img")) {
                        verityQry.append(" ( Image <OR> StillImage ) <IN> DC:Type");
                    } else if (resource.equals("int")) {
                        verityQry.append(" InteractiveResource <IN> DC:Type");
                    } else if (resource.equals("txt")) {
                        verityQry.append(" ( <NOT> Image <NOT> Sound <NOT> InteractiveResource <NOT> MovingImage <NOT> StillImage ) <IN> DC:Type ");
                    } else if (resource.equals("vid")) {
                        verityQry.append(" MovingImage <IN> DC:Type");
                    } else {
                        verityQry.append(" ").append(resource).append(" <IN> DC:Type");
                    }
                }
            }
            verityQry.append(')');
        }
        String verityQryString = verityQry.toString();
        if (Flog.LOG_LEVEL <= Flog.DEBUG_LOG_LEVEL) {
            Flog.debug(classStr(), adapterCode + " adapter built verity syntax:" + verityQryString);
        }
        return verityQryString;
    }

    /**
	 * Appends a Verity syntax condition for a single field, based on a
	 * FieldPhrase object. Text search format: ("w1 w2" | w1 (<and>|<or>) w2)
	 * <in> field Relational search format: field (<|<=|=|>=|>) phrase
	 * 
	 * Worker method for convertMQParamsToVeritySyntax().
	 */
    private static void appendFieldConditionToVerityQry(StringBuffer verityQry, FieldPhrase fp) {
        verityQry.append('(');
        if (fp.isRelational()) {
            verityQry.append(fp.getField().replaceFirst(":", "_"));
            verityQry.append(' ');
            if (!(fp.getPhrase().startsWith("<") || fp.getPhrase().startsWith("=") || fp.getPhrase().startsWith("!=") || fp.getPhrase().startsWith(">"))) {
                verityQry.append("= ");
            }
            verityQry.append(fp.getPhrase());
        } else {
            if ("phrase".equals(fp.getKc())) {
                verityQry.append('\"').append(fp.getPhrase()).append('\"');
            } else {
                String[] words = fp.getPhraseWords();
                if (words.length == 1) {
                    verityQry.append(encodeVeritySearchWord(words[0]));
                } else {
                    verityQry.append('(');
                    boolean any = "any".equals(fp.getKc());
                    for (int i = 0; i < words.length; i++) {
                        if (i > 0) {
                            if (any) {
                                verityQry.append(" <or> ");
                            } else {
                                verityQry.append(" <and> ");
                            }
                        }
                        verityQry.append(encodeVeritySearchWord(words[i]));
                    }
                    verityQry.append(')');
                }
            }
            if (fp.getField() != null && fp.getField().length() > 0) {
                verityQry.append(" <in> ");
                verityQry.append(fp.getField());
            }
        }
        verityQry.append(')');
    }

    /**
	 * Ensures a search word is correctly quoted/escaped for user in Verity
	 * search syntax.
	 */
    private static String encodeVeritySearchWord(String s) {
        s = escapeVeritySpecialChars(s);
        if (isVerityKeyword(s)) {
            s = '\"' + s + '\"';
        }
        return s;
    }

    /**
	 * Returns true if the argument string represents a keyword in Verity.
	 */
    private static boolean isVerityKeyword(String s) {
        return "and".equals(s) || "or".equals(s);
    }

    /**
	 * Escapes Verity special chars in query text. Special chars are: * , ( ) [ = > < ! ` @ {
	 * �(=unicode:8216) These are escaped by preceding them with \.
	 */
    private static String escapeVeritySpecialChars(String s) {
        StringBuffer sbChars = new StringBuffer(s);
        char[] chars = s.toCharArray();
        for (int i = chars.length - 1; i > -1; i--) {
            char c = chars[i];
            if (c == '*' || c == ',' || c == '(' || c == ')' || c == '[' || c == '=' || c == '>' || c == '<' || c == '!' || c == '`' || c == '@' || c == '{' || c == '舖') {
                sbChars.insert(i, '\\');
            }
        }
        return (sbChars.length() > chars.length) ? sbChars.toString() : s;
    }

    /** Class name display */
    public static String classStr() {
        return EducationAuUtils.class.getName();
    }

    /**
	 * Inner class used to hold field-level query information when converting
	 * Multiquery params to Verity syntax.
	 */
    static class FieldPhrase {

        private String field;

        private String phrase;

        private String kc;

        private String op;

        private boolean relational;

        public FieldPhrase(String field, String phrase, String kc, String op, boolean relational) {
            this.field = field;
            this.phrase = phrase;
            this.kc = kc;
            this.op = op;
            this.relational = relational;
        }

        public String getKc() {
            return kc;
        }

        public void setKc(String kc) {
            this.kc = kc;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public boolean isRelational() {
            return relational;
        }

        public void setRelational(boolean relational) {
            this.relational = relational;
        }

        public String getPhrase() {
            return phrase;
        }

        public void setPhrase(String phrase) {
            this.phrase = phrase;
        }

        public String[] getPhraseWords() {
            if (phrase == null || phrase.trim().length() == 0) {
                return new String[0];
            } else {
                return phrase.trim().split("\\s+");
            }
        }
    }
}

/**
 * @author gsingh
 */
class MilspecAuthenticator extends java.net.Authenticator {

    private static String username = "username_not_set";

    private static String password = "password_not_set";

    /**
	 * @param username
	 * @param password
	 */
    public MilspecAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /***/
    public java.net.PasswordAuthentication getPasswordAuthentication() {
        return new java.net.PasswordAuthentication(username, (password.toCharArray()));
    }
}
