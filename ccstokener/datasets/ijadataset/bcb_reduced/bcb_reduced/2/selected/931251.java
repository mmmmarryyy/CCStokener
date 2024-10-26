package kosoft.dbgen.util;

import java.net.*;
import java.io.*;
import java.util.*;

public abstract class QueryBase implements ResultSet {

    protected String sQueryName = "";

    protected String sQueryTemplate = "";

    String sWhere = "";

    String sOrderBy = "";

    String sParameters = "";

    protected DataInputStream in;

    protected int iNumRows = 0;

    protected int iCurrentRow = 0;

    URL url;

    URLConnection connection;

    protected RowBase[] oRowData;

    protected ResultSet oDelegatedResultSet;

    String sCurrentLine;

    static String sDBHost = "exchange.ludwig.edu.au";

    static String sDBPath = "purchase/";

    public static boolean bTraceSqlFlg = false;

    public int FieldType(int iFieldNum) {
        return oDelegatedResultSet.FieldType(iFieldNum);
    }

    ;

    public int FieldDataType(int iFieldNum) {
        return oDelegatedResultSet.FieldDataType(iFieldNum);
    }

    ;

    public String FieldName(int iFieldNum) {
        return oDelegatedResultSet.FieldName(iFieldNum);
    }

    ;

    public abstract void AllocateArray();

    public abstract void ReadRecord() throws IOException;

    public static void SetTraceSql(boolean bNewTraceFlg) {
        bTraceSqlFlg = bNewTraceFlg;
    }

    public static boolean DoTraceSQL() {
        return bTraceSqlFlg;
    }

    public void AddWhere(String sValue) {
        String sURLAnd = URLEncoder.encode(" and ");
        if (sWhere.length() != 0) sWhere = sWhere + sURLAnd;
        sWhere = sWhere + sValue;
    }

    public void AddWhere(int iColumn, String sValue) {
        String sURLEqual = URLEncoder.encode("=");
        if (FieldDataType(iColumn) != FDATATYPE_String) {
            Reporter.ShowMessage("Invalid Select where clause " + FieldName(iColumn) + " " + sValue);
            return;
        }
        String sNewValue = URLEncoder.encode(Util.CheckForQuotes(sValue));
        String sNewWhere = FieldName(iColumn) + sURLEqual + Util.Quote(sNewValue);
        AddWhere(sNewWhere);
    }

    public void AddWhere(int iColumn, String sValue, String sOperator) {
        String sURLEqual = URLEncoder.encode(" " + sOperator + " ");
        String sNewWhere;
        if (FieldDataType(iColumn) == FDATATYPE_String) {
            String sNewValue = URLEncoder.encode(Util.CheckForQuotes(sValue));
            sNewWhere = FieldName(iColumn) + sURLEqual + Util.Quote(sNewValue);
        } else {
            String sNewValue = URLEncoder.encode(sValue);
            sNewWhere = FieldName(iColumn) + sURLEqual + sNewValue;
        }
        AddWhere(sNewWhere);
    }

    public void AddWhereWithUnquotedValue(int iColumn, String sValue, String sOperator) {
        String sURLEqual = URLEncoder.encode(" " + sOperator + " ");
        String sNewWhere;
        if (FieldDataType(iColumn) == FDATATYPE_String) {
            String sNewValue = URLEncoder.encode(Util.CheckForQuotes(sValue));
            sNewWhere = FieldName(iColumn) + sURLEqual + sNewValue;
        } else {
            String sNewValue = URLEncoder.encode(sValue);
            sNewWhere = FieldName(iColumn) + sURLEqual + sNewValue;
        }
        AddWhere(sNewWhere);
    }

    public void AddWhere(int iColumn, long lValue) {
        String sURLEqual = URLEncoder.encode("=");
        String sNewWhere = FieldName(iColumn) + sURLEqual + Long.toString(lValue);
        AddWhere(sNewWhere);
    }

    public void AddWhere(int iColumn, long lValue, String sOperator) {
        String sURLOperator = URLEncoder.encode(" " + sOperator + " ");
        String sNewWhere = FieldName(iColumn) + sURLOperator + Long.toString(lValue);
        AddWhere(sNewWhere);
    }

    public void AddWhere(String sColumn, boolean bValue) {
    }

    public void AddWhere(String sColumn, Date oValue) {
    }

    public void AddOrderBy(String sNewValue) {
        if (sOrderBy == "") {
            sOrderBy = URLEncoder.encode(" Order by ");
        } else {
            sOrderBy += URLEncoder.encode(", ");
        }
        sOrderBy += URLEncoder.encode(sNewValue);
    }

    public void AddOrderBy(int iColumn) {
        String sNewOrderBy = FieldName(iColumn);
        AddOrderBy(sNewOrderBy);
    }

    public void AddParameter(String sParmName, String sParmValue) {
        sParameters = sParameters + "&" + sParmName + "=" + sParmValue;
    }

    public void FetchAll() {
        try {
            BuildAndSendQuery();
            ReadQueryHeader();
            AllocateArray();
            while (FindRecordHeader()) {
                ReadRecord();
                ReadRecordTrailer();
            }
            ReadQueryTrailer();
            in.close();
        } catch (IOException e) {
        }
    }

    public int GetNumRows() {
        try {
            BuildAndSendQuery();
            ReadQueryHeader();
            in.close();
            return iNumRows;
        } catch (IOException e) {
            return 0;
        }
    }

    public static String GetDBUrl() {
        return "http://" + sDBHost + "/cgi-shl/dbml.exe?template=";
    }

    public static String GetDBUpdateUrl() {
        return "http://" + sDBHost + "/cgi-shl/dbml.exe?template=" + sDBPath;
    }

    public static void SetDBHost(String sNewHost) {
        sDBHost = sNewHost;
    }

    public static void SetDBPath(String sNewPath) {
        sDBPath = sNewPath;
    }

    public void BuildAndSendQuery() throws IOException {
        String sURL;
        sURL = GetDBUrl();
        sURL = sURL + sQueryTemplate;
        if (sQueryName != "") {
            sURL = sURL + "&QueryName=" + sQueryName;
        }
        sURL = sURL + "&sWhere=";
        if (sWhere != "") {
            sURL = sURL + URLEncoder.encode("where ") + sWhere;
        }
        if (sOrderBy != "") {
            sURL = sURL + sOrderBy;
        }
        if (sParameters != "") {
            sURL = sURL + "&" + sParameters;
        }
        if (DoTraceSQL()) {
            System.out.println("loading url - " + sURL);
        }
        try {
            url = new URL(sURL);
            connection = url.openConnection();
        } catch (MalformedURLException e) {
            System.out.println("url not found " + sURL);
        }
        ;
        in = new DataInputStream(connection.getInputStream());
        ReadLine();
    }

    void ReadLine() throws IOException {
        sCurrentLine = in.readLine();
        if (DoTraceSQL()) {
            System.out.println("got line - " + sCurrentLine);
        }
        if (sCurrentLine.startsWith("<HTML><HEAD><TITLE>Error")) throw new SQLExecException(sCurrentLine);
    }

    void ReadQueryHeader() throws IOException {
        do {
            ReadLine();
            if (sCurrentLine == null) return;
            if (sCurrentLine.startsWith("<HTML><HEAD><TITLE>Error")) throw new SQLExecException(sCurrentLine);
        } while (!sCurrentLine.startsWith("BeginQuery"));
        iNumRows = Integer.parseInt(Util.GetWord(sCurrentLine, 1));
    }

    boolean FindRecordHeader() throws IOException {
        do {
            ReadLine();
            if (sCurrentLine == null) return false;
            if (sCurrentLine.startsWith("<HTML><HEAD><TITLE>Error")) throw new SQLExecException(sCurrentLine);
            if (sCurrentLine.startsWith("EndQuery")) return false;
        } while (!sCurrentLine.startsWith("BeginRow"));
        iCurrentRow = Integer.parseInt(Util.GetWord(sCurrentLine, 1));
        iCurrentRow -= 1;
        return true;
    }

    void ReadRecordTrailer() throws IOException {
        do {
            ReadLine();
        } while (!sCurrentLine.startsWith("EndRow"));
    }

    void ReadQueryTrailer() throws IOException {
    }
}
