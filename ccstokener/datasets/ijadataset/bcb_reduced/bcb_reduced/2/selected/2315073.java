package com.microfly.index;

import com.microfly.core.Config;
import com.microfly.core.Attach;
import com.microfly.exception.NpsException;
import com.microfly.exception.ErrorHelper;
import com.microfly.compiler.WordLimitWriter;
import com.microfly.util.DefaultLog;
import com.microfly.util.Utils;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.math.BigDecimal;

/**
 *  SOLR����Ļ�����ʵ���������ܣ�
 *    1.Ϊ�ϲ����ṩ���XML�ļ���ϵ�з���
 *    2 ΪIndexScheduler����XML�ļ�����SOLR�ύ��������
 * a new publishing system
 * Copyright: Copyright (c) 2007
 *
 <add>
   <doc>
   <field name="id">3007WFP</field>
   </doc>
 </add>
 <delete>
   <id>SP2514N</id>
 </delete>
 * @author jialin
 * @version 1.0
*/
public class Index2Solr {

    protected int mode = 0;

    private URL solrUrl = null;

    private String solrCore = null;

    protected java.io.File xml_file = null;

    protected OutputStreamWriter xml_osw = null;

    public Index2Solr(String core, int mode) throws NpsException {
        this.solrCore = (core == null ? "flycore" : core);
        this.mode = mode;
        CreateXMLFile();
    }

    public Index2Solr(File temp_file) {
        this.xml_file = temp_file;
    }

    public void AddSolrCore() throws NpsException {
        if (xml_osw == null) return;
        try {
            xml_osw.write(solrCore + "\r\n");
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        }
    }

    public void AddXMLHeader() throws NpsException {
        if (xml_osw == null) return;
        try {
            switch(mode) {
                case 0:
                    xml_osw.write("<add>\r\n");
                    break;
                case 1:
                    xml_osw.write("<delete>\r\n");
                    break;
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        }
    }

    public void AddXMLDocHeader() throws NpsException {
        if (xml_osw == null) return;
        if (mode != 0) return;
        try {
            xml_osw.write("<doc>\r\n");
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        }
    }

    public void AddField(String fieldname, Object fieldvalue) throws NpsException {
        if (xml_osw == null) return;
        switch(mode) {
            case 0:
                AddField0(fieldname, fieldvalue);
                break;
            case 1:
                AddField1(fieldname, fieldvalue);
                break;
        }
    }

    public void AddAttachField(Attach att) throws NpsException {
        if (xml_osw == null) return;
        if (att == null) return;
        try {
            xml_osw.write("<field name=\"attach\">");
            xml_osw.write("<![CDATA[");
        } catch (Exception e) {
        }
        try {
            TikaExtractor.Extract(att, xml_osw);
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error_noexception(e);
        }
        try {
            xml_osw.write("]]>");
            xml_osw.write("</field>");
            xml_osw.write("\r\n");
        } catch (Exception e) {
        }
    }

    protected boolean IsReserved(String fieldname) {
        return false;
    }

    private String FixedFieldName(String fieldname, Object fieldvalue) {
        if (IsReserved(fieldname)) return fieldname;
        if (fieldvalue instanceof Integer) {
            fieldname += "_i";
        } else if (fieldvalue instanceof String) {
            fieldname += "_s";
        } else if (fieldvalue instanceof Long) {
            fieldname += "_l";
        } else if (fieldvalue instanceof java.io.Reader) {
            fieldname += "_t";
        } else if (fieldvalue instanceof Boolean) {
            fieldname += "_b";
        } else if (fieldvalue instanceof Float) {
            fieldname += "_f";
        } else if (fieldvalue instanceof Double) {
            fieldname += "_d";
        } else if (fieldvalue instanceof BigDecimal) {
            fieldname += "_d";
        } else if (fieldvalue instanceof java.util.Date) {
            fieldname += "_dt";
        }
        return fieldname;
    }

    private void AddField0(String fieldname, Object fieldvalue) throws NpsException {
        try {
            fieldname = fieldname.toLowerCase();
            xml_osw.write("<field name=\"");
            fieldname = FixedFieldName(fieldname, fieldvalue);
            xml_osw.write(fieldname);
            xml_osw.write("\"");
            if (fieldvalue == null) {
                xml_osw.write("/>");
            } else {
                xml_osw.write(">");
                if (fieldvalue instanceof java.io.Reader) {
                    xml_osw.write("<![CDATA[");
                    WordLimitWriter aLimitWriter = new WordLimitWriter(new PrintWriter(xml_osw));
                    java.io.Reader r = (Reader) fieldvalue;
                    com.microfly.util.Utils.GetFlatText(r, aLimitWriter);
                    try {
                        r.close();
                    } catch (Exception e) {
                    }
                    xml_osw.write("]]>");
                } else if (fieldvalue instanceof java.util.Date) {
                    xml_osw.write(Utils.FormateDate((java.util.Date) fieldvalue, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                } else if (fieldvalue instanceof String) {
                    xml_osw.write(Utils.TranferToXmlEntity((String) fieldvalue));
                } else {
                    xml_osw.write(fieldvalue.toString());
                }
                xml_osw.write("</field>");
            }
            xml_osw.write("\r\n");
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        }
    }

    private void AddField1(String fieldname, Object fieldvalue) throws NpsException {
        try {
            fieldname = fieldname.toLowerCase();
            xml_osw.write("<");
            xml_osw.write(fieldname);
            xml_osw.write(">");
            if (fieldvalue != null) xml_osw.write(fieldvalue.toString());
            xml_osw.write("</");
            xml_osw.write(fieldname);
            xml_osw.write(">");
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        }
    }

    public void AddXMLDocFooter() throws NpsException {
        if (xml_osw == null) return;
        if (mode != 0) return;
        try {
            xml_osw.write("</doc>\r\n");
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        }
    }

    public void AddXMLFooter() throws NpsException {
        if (xml_osw == null) return;
        try {
            switch(mode) {
                case 0:
                    xml_osw.write("</add>\r\n");
                    break;
                case 1:
                    xml_osw.write("</delete>\r\n");
                    break;
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        }
    }

    public void Index() throws NpsException {
        Clear();
        if (Config.SOLR_URL == null) return;
        java.io.BufferedReader fr = null;
        try {
            fr = new BufferedReader(new InputStreamReader(new FileInputStream(xml_file), "UTF-8"));
            solrCore = fr.readLine();
            PostData(fr);
            try {
                fr.close();
            } catch (Exception e) {
            }
            fr = null;
            Commit();
            DeleteXMLFile();
        } catch (FileNotFoundException fe) {
            com.microfly.util.DefaultLog.error_noexception(fe);
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (fr != null) try {
                fr.close();
            } catch (Exception e) {
            }
        }
    }

    private void PostData(Reader data) throws NpsException {
        if (Config.SOLR_URL == null) return;
        HttpURLConnection urlc = null;
        OutputStream out = null;
        InputStream in = null;
        try {
            if (solrUrl == null) solrUrl = new URL(Config.SOLR_URL + "/" + solrCore + "/update");
            urlc = (HttpURLConnection) solrUrl.openConnection();
            urlc.setRequestMethod("POST");
            urlc.setDoOutput(true);
            urlc.setDoInput(true);
            urlc.setUseCaches(false);
            urlc.setAllowUserInteraction(false);
            urlc.setRequestProperty("Content-type", "text/xml;charset=UTF-8");
            out = urlc.getOutputStream();
            Writer writer = new OutputStreamWriter(out, "UTF-8");
            pipe(data, writer);
            try {
                writer.close();
            } catch (Exception e1) {
            }
            in = urlc.getInputStream();
            StringWriter output = new StringWriter();
            Reader reader = new InputStreamReader(in, "UTF-8");
            pipe(reader, output);
            try {
                reader.close();
            } catch (Exception e1) {
            }
            String SOLR_OK_RESPONSE_EXCERPT = "<int name=\"status\">0</int>";
            if (output.toString().indexOf(SOLR_OK_RESPONSE_EXCERPT) < 0) {
                com.microfly.util.DefaultLog.error(output.toString(), ErrorHelper.INDEX_POST_ERROR);
            }
        } catch (NpsException e) {
            throw e;
        } catch (Exception e) {
            DefaultLog.error(e, ErrorHelper.INDEX_POST_ERROR);
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception e1) {
            }
            if (out != null) try {
                out.close();
            } catch (Exception e1) {
            }
            if (urlc != null) urlc.disconnect();
        }
    }

    private void Commit() throws NpsException {
        PostData(new StringReader("<commit/>"));
    }

    private void pipe(Reader reader, Writer writer) throws IOException {
        char[] buf = new char[1024];
        int read = 0;
        while ((read = reader.read(buf)) >= 0) {
            writer.write(buf, 0, read);
        }
        writer.flush();
    }

    private File CreateXMLFile() throws NpsException {
        java.util.UUID uuid = java.util.UUID.randomUUID();
        String file_name = uuid.toString() + ".xml";
        xml_file = new File(Config.OUTPATH_INDEX + "/" + file_name);
        try {
            xml_osw = new OutputStreamWriter(new FileOutputStream(xml_file, true), "UTF-8");
        } catch (Exception e) {
            xml_file = null;
            xml_osw = null;
            com.microfly.util.DefaultLog.error(e);
        }
        return xml_file;
    }

    public void DeleteXMLFile() {
        if (xml_file != null) xml_file.delete();
    }

    public void Clear() {
        if (xml_osw != null) {
            try {
                xml_osw.flush();
            } catch (Exception e) {
            }
            try {
                xml_osw.close();
            } catch (Exception e) {
            }
        }
        xml_osw = null;
    }
}
