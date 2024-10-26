package com.jane16.api.examples;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import com.jane16.api.examples.parser.Jane16Results;
import com.jane16.api.examples.parser.ScopeParser;
import com.jane16.api.examples.parser.SubjectParser;
import com.jane16.api.examples.parser.SummaryParser;
import com.jane16.api.examples.parser.XMLParser;

public class Jane16Gateway {

    private static final String jane16Server = "http://www.jane16.com/api/";

    private static final String subjectURL = "subject";

    private static final String scopeURL = "scope";

    private static final String summaryURL = "summary";

    public ArrayList<Jane16Results> callExternalService(ServiceType type, HashMap<String, String> params) throws Exception {
        URL url = initURL(type, params);
        XMLParser parser = initParser(type);
        InputStream in = url.openStream();
        ArrayList<Jane16Results> results = new ArrayList<Jane16Results>();
        byte[] buf = new byte[1024];
        ArrayList<Byte> arrByte = new ArrayList<Byte>();
        int len;
        while ((len = in.read(buf)) > 0) {
            for (int i = 0; i < len; i++) {
                arrByte.add(buf[i]);
            }
        }
        in.close();
        byte[] data = new byte[arrByte.size()];
        int i = 0;
        for (Byte b : arrByte) {
            data[i++] = b;
        }
        results = parser.parse(data);
        return results;
    }

    private URL initURL(ServiceType type, HashMap params) throws Exception {
        URL ret = null;
        String prm = parametersToLine(params);
        if (type == ServiceType.SUBJECT) {
            ret = new URL(jane16Server + subjectURL + "?" + prm);
        } else if (type == ServiceType.SCOPE) {
            ret = new URL(jane16Server + scopeURL + "?" + prm);
        } else if (type == ServiceType.SUMMARY) {
            ret = new URL(jane16Server + summaryURL + "?" + prm);
        } else {
            throw new Exception("Unrecognized service to be called.");
        }
        return ret;
    }

    private XMLParser initParser(ServiceType type) throws Exception {
        XMLParser ret = null;
        if (type == ServiceType.SUBJECT) {
            ret = new SubjectParser();
        } else if (type == ServiceType.SCOPE) {
            ret = new ScopeParser();
        } else if (type == ServiceType.SUMMARY) {
            ret = new SummaryParser();
        } else {
            throw new Exception("Unrecognized parser to be instanciated.");
        }
        return ret;
    }

    private String parametersToLine(HashMap<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String name : params.keySet()) {
            sb.append(name).append("=").append(params.get(name)).append("&");
        }
        return sb.toString();
    }

    public enum ServiceType {

        SUBJECT, SCOPE, SUMMARY
    }

    /**
	 * Main starter , for console testing 
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
        System.out.println("======= JANE16 START - Java Associative Nervous Engine =========");
        if (args.length < 2) {
            System.out.println("USAGE:");
            System.out.println(" args[0] = name of the service[subject,scope,summary]");
            System.out.println(" args[1] = text to be analysed");
            System.out.println(" args[2] = max number of keywords returned (acceptable only by subject service)");
            System.out.println("EXAMPLES:");
            System.out.println("java.exe com.jane16.api.examples.Jane16Gateway subject 'Who is the president of the USA?' 8");
            System.out.println("java.exe com.jane16.api.examples.Jane16Gateway scope 'Who is the president of the USA?'");
            return;
        }
        String strService = args[0];
        String prm_1 = args[1];
        String prm_2 = null;
        ServiceType service = null;
        if ("subject".equalsIgnoreCase(strService)) {
            service = ServiceType.SUBJECT;
            prm_2 = args[2];
        } else if ("scope".equalsIgnoreCase(strService)) {
            service = ServiceType.SCOPE;
        } else if ("summary".equalsIgnoreCase(strService)) {
            service = ServiceType.SUMMARY;
        } else {
            throw new Exception("Unrecognized service to be called - " + strService);
        }
        if (prm_1 == null) {
            throw new Exception("Text for analysis must be provided.");
        }
        Jane16Gateway gtw = new Jane16Gateway();
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("text", URLEncoder.encode(prm_1));
        if (service == ServiceType.SUBJECT) {
            params.put("maxFoundKeywords", prm_2);
        }
        ArrayList<Jane16Results> results = gtw.callExternalService(service, params);
        if (results.size() == 0) {
            System.out.println("Service call returned " + results.size() + " results.");
        }
        int i = 0;
        for (Jane16Results entries : results) {
            System.out.print((++i) + " ENTRY: " + entries.entry);
            if (service == ServiceType.SUBJECT) {
                System.out.print(" SCORE: " + entries.score);
            }
            System.out.println("");
        }
        System.out.println("======= JANE16 FINISH - End of service call =========");
    }
}
