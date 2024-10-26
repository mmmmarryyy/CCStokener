package org.form4G.net.JSP2microServlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class JSPTranslator {

    private URL urlSource = null;

    private String strOutput = null;

    private String strSource = null;

    private String strProcedure = null;

    public JSPTranslator() {
        super();
    }

    public void translator(URL urlSource, String javaPackage, String className, OutputStream outputStream) throws IOException {
        this.urlSource = urlSource;
        this.strOutput = getLoadStream(this.getClass().getResource("jspTagLib.txt"));
        this.strProcedure = "";
        this.strSource = getLoadStream(urlSource);
        if (this.strSource.indexOf("<%") == -1) ;
        this.strSource += "<% %>";
        getIncludeTag();
        this.strSource = setTagAll(this.strSource, "<%--", "<% /** ");
        this.strSource = setTagAll(this.strSource, "--%>", " */\n %>");
        Properties pageProperties = getPageTag();
        String strDeclaration = "";
        Object[] aryDeclaration = getDeclaration().toArray();
        for (int s_for = 0; s_for != aryDeclaration.length; s_for++) {
            strDeclaration = strDeclaration + aryDeclaration[s_for] + "\n";
        }
        strOutput = setTag(strOutput, "<declaration>", strDeclaration);
        String strImport = "";
        String strTagImport = pageProperties.getProperty("import");
        if (strTagImport != null) {
            if (strTagImport.indexOf(",") > -1) {
                String[] ari = strTagImport.split(",");
                for (int s_for = 0; s_for < ari.length; s_for++) {
                    String key = " " + ari[s_for] + " ";
                    key = key.trim();
                    if ((key.indexOf("java.io.") > -1) || (key.indexOf("java.lang.") > -1) || (key.indexOf("java.net.") > -1) || (key.indexOf("org.form4G.net.microServlet.*") > -1)) {
                    } else strImport = strImport + "import " + key + ";\n";
                }
            }
        }
        strImport = strImport + "import java.io.*;\n";
        strImport = strImport + "import java.lang.*;\n";
        strImport = strImport + "import java.net.*;\n";
        strImport = strImport + "import org.form4G.net.microServlet.*;\n";
        strOutput = setTag(strOutput, "<import>", strImport);
        strOutput = setTagAll(strOutput, "<className>", className);
        strOutput = setTag(strOutput, "<package>", javaPackage);
        String strExtends = "implements MicroServlet";
        if (pageProperties.getProperty("extends") != null) strExtends = "extends " + pageProperties.getProperty("extends") + " " + strExtends;
        strOutput = setTag(strOutput, "<extends>", strExtends);
        String strInfo = "";
        if (pageProperties.getProperty("info") != null) strInfo = pageProperties.getProperty("info");
        strOutput = setTag(strOutput, "<info>", strInfo);
        String strContentType = "text/html";
        if (pageProperties.getProperty("contentType") != null) {
            strContentType = pageProperties.getProperty("contentType");
            int index = strContentType.indexOf(";");
            if (index > -1) {
                String resto = strContentType.substring(index + 1, strContentType.length());
                strContentType = strContentType.substring(0, index);
                strContentType = (" " + strContentType + " ").trim();
                String[] split = resto.split("=");
                for (int s_for = 0; s_for < split.length; s_for += 2) {
                    String key = (" " + split[s_for] + " ").trim();
                    String value = (" " + split[s_for + 1] + " ").trim();
                    pageProperties.setProperty(key, value);
                }
            }
        }
        strOutput = setTag(strOutput, "<contentType>", strContentType);
        String charset = (pageProperties.getProperty("charset") == null) ? "ISO-8859-1" : pageProperties.getProperty("charset");
        strOutput = setTag(strOutput, "<charset>", charset);
        String strIsThreadSafe = "true";
        if (pageProperties.getProperty("isThreadSafe") != null) strIsThreadSafe = pageProperties.getProperty("isThreadSafe");
        strOutput = setTag(strOutput, "<isThreadSafe>", strIsThreadSafe);
        setProcedure();
        strOutput = setTag(strOutput, "<procedure>", this.strProcedure);
        byte[] buf = this.strOutput.getBytes();
        outputStream.write(buf, 0, buf.length);
    }

    private void getIncludeTag() throws IOException {
        int index = this.strSource.indexOf("<%@ include");
        for (; index > -1; ) {
            String strTag = this.strSource.substring(index, this.strSource.length());
            strTag = strTag.substring(0, strTag.indexOf("%>") + 2);
            String value = "";
            String[] ariPageTag = strTag.split("\"");
            for (int s_for = 0; s_for < ariPageTag.length; s_for++) {
                String key = " " + ariPageTag[s_for] + " ";
                key = key.trim();
                if (key.indexOf("=") > 1) {
                    value = " " + ariPageTag[s_for + 1] + " ";
                    value = value.trim();
                    s_for = ariPageTag.length;
                }
            }
            String strInclude = null;
            try {
                strInclude = getLoadStream(new URL(urlSource, value));
            } catch (MalformedURLException mue) {
                throw new IOException("Error in acces " + strTag + "\n" + mue);
            }
            this.strSource = setTagAll(this.strSource, strTag, strInclude);
            index = this.strSource.indexOf("<%@ include");
        }
    }

    private String getLoadStream(URL url) throws IOException {
        String rr = null;
        try {
            java.net.URLConnection urlConnection = url.openConnection();
            int size = (int) urlConnection.getContentLength();
            InputStream inputStream = urlConnection.getInputStream();
            byte[] buf = new byte[size];
            int bufSize = inputStream.read(buf, 0, buf.length);
            inputStream.close();
            if (bufSize != size) throw new NullPointerException("Error size in read " + bufSize + " != " + size);
            rr = new String(buf, 0, bufSize);
            rr = new String(" " + rr + " ");
            rr = rr.trim();
        } catch (NullPointerException npe) {
            throw new IOException("Ilegal Argument in Filer " + npe);
        } catch (IndexOutOfBoundsException ioe) {
            throw new IOException("Ilegal Argument in Filer " + ioe);
        }
        return (rr);
    }

    private String setTag(String strSours, String strInp, String strOut) {
        String rr = new String(strSours);
        if (rr.indexOf(strInp) > -1) {
            int index = rr.indexOf(strInp);
            String str1 = "";
            if (index > 0) str1 = rr.substring(0, index);
            String str2 = rr.substring(rr.indexOf(strInp) + strInp.length(), rr.length());
            rr = str1 + strOut + str2;
            rr = (" " + rr + " ").trim();
        }
        return (rr);
    }

    private String setTagAll(String strSours, String strInp, String strOut) {
        String rr = new String(strSours);
        int index = rr.indexOf(strInp);
        for (; index > -1; ) {
            String str1 = "";
            if (index > 0) str1 = rr.substring(0, index);
            String str2 = rr.substring(rr.indexOf(strInp) + strInp.length(), rr.length());
            rr = str1 + strOut + str2;
            index = rr.indexOf(strInp);
        }
        return (rr);
    }

    private Properties getPageTag() {
        Properties rr = new Properties();
        int index = this.strSource.indexOf("<%@page");
        if (index == -1) index = this.strSource.indexOf("<%@ page");
        if (index > -1) {
            String str1 = "";
            if (index > 0) str1 = this.strSource.substring(0, index);
            String strPageTag = this.strSource.substring(index, this.strSource.length());
            strPageTag = strPageTag.substring(0, strPageTag.indexOf("%>") + 2);
            String str2 = this.strSource.substring(this.strSource.indexOf(strPageTag) + strPageTag.length(), this.strSource.length());
            this.strSource = str1 + str2;
            strPageTag = strPageTag.substring(strPageTag.indexOf("page") + "page".length(), strPageTag.indexOf("%>") + 2);
            String[] ariPageTag = strPageTag.split("\"");
            for (int s_for = 0; s_for < ariPageTag.length; s_for++) {
                String key = " " + ariPageTag[s_for] + " ";
                key = key.trim();
                if (key.indexOf("=") > 1) {
                    key = " " + key.substring(0, key.indexOf("=")) + " ";
                    key = key.trim();
                    String value = " " + ariPageTag[s_for + 1] + " ";
                    value = value.trim();
                    rr.put(key, value);
                }
            }
        }
        return (rr);
    }

    private List<String> getDeclaration() {
        Vector<String> rr = new Vector<String>();
        int index = this.strSource.indexOf("<%!");
        for (; index > -1; ) {
            String str1 = "";
            if (index > 0) str1 = this.strSource.substring(0, index);
            String strTag = this.strSource.substring(index, this.strSource.length());
            strTag = strTag.substring(0, strTag.indexOf("%>") + 2);
            String str2 = this.strSource.substring(this.strSource.indexOf(strTag) + strTag.length(), this.strSource.length());
            this.strSource = str1 + str2;
            strTag = strTag.substring(strTag.indexOf("<%!") + "<%!".length(), strTag.indexOf("%>"));
            strTag = " " + strTag + " ";
            strTag = strTag.trim();
            if (strTag.length() > 0) rr.add(strTag);
            index = this.strSource.indexOf("<%!");
        }
        return (rr);
    }

    private void setProcedure() {
        int index = this.strSource.indexOf("<%");
        String str2 = "";
        for (; index > -1; ) {
            String strHtml = "";
            if (index > 0) strHtml = this.strSource.substring(0, index);
            String strTag = this.strSource.substring(index, this.strSource.length());
            strTag = strTag.substring(0, strTag.indexOf("%>") + 2);
            str2 = this.strSource.substring(this.strSource.indexOf(strTag) + strTag.length(), this.strSource.length());
            this.strSource = str2;
            strTag = strTag.substring(strTag.indexOf("<%") + "<%".length(), strTag.indexOf("%>"));
            strTag = (" " + strTag + " ").trim();
            if (strTag.length() > 0) {
                if (strTag.indexOf("=") == 0) {
                    strTag = " " + strTag.substring(1, strTag.length()) + " ";
                    strTag = "response.getWriter().print(" + strTag.trim() + "+\"\");\n";
                }
            }
            String s_strHtml = " " + strHtml + " ";
            s_strHtml = s_strHtml.trim();
            if (s_strHtml.length() > 0) {
                this.strProcedure = this.strProcedure + htmlFromJava(strHtml) + strTag + "\n";
            } else {
                this.strProcedure = this.strProcedure + strTag;
            }
            index = this.strSource.indexOf("<%");
        }
        if (str2.length() > 0) str2 = htmlFromJava(str2);
        this.strProcedure = this.strProcedure + str2;
    }

    private String htmlFromJava(String strSours) {
        String rr = "";
        String str = setTagAll(strSours, "\"", "<%''%>");
        str = setTagAll(str, "<%''%>", "\\\"");
        str = setTagAll(str, "\r", "");
        String[] ariStr = str.split("\n");
        for (int s_for = 0; s_for < ariStr.length; s_for++) {
            String key = ariStr[s_for];
            rr = rr + "response.getWriter().print(\"" + key + "\");\n";
        }
        return (rr);
    }
}
