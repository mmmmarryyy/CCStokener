package net.homeip.yann_lab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Macallan
 */
public class HSDataTypeVector {

    private static final Log log = LogFactory.getLog(HSDataTypeVector.class);

    private String dataTypeFileName = "DataTypes.txt";

    private File dataTypeFile = null;

    private FileInputStream dataTypeFis = null;

    private InputStream dataTypeIs = null;

    private InputStreamReader dataTypeIsr = null;

    private BufferedReader dataTypeReader = null;

    private ArrayList stringDataTypeArray = new ArrayList();

    private static HSDataTypeVector instance = null;

    public static HSDataTypeVector getInstance() {
        if (instance == null) {
            instance = new HSDataTypeVector();
        }
        return instance;
    }

    private Vector dataTypesVector;

    private HSDataTypeVector() {
        dataTypesVector = new Vector();
        dataTypesVector.add(new HSDataType(java.sql.Types.BIT, "java.sql.Types.BIT"));
        dataTypesVector.add(new HSDataType(java.sql.Types.TINYINT, "java.sql.Types.TINYINT"));
        dataTypesVector.add(new HSDataType(java.sql.Types.SMALLINT, "java.sql.Types.SMALLINT"));
        dataTypesVector.add(new HSDataType(java.sql.Types.INTEGER, "java.sql.Types.INTEGER"));
        dataTypesVector.add(new HSDataType(java.sql.Types.BIGINT, "java.sql.Types.BIGINT"));
        dataTypesVector.add(new HSDataType(java.sql.Types.FLOAT, "java.sql.Types.FLOAT"));
        dataTypesVector.add(new HSDataType(java.sql.Types.REAL, "java.sql.Types.REAL"));
        dataTypesVector.add(new HSDataType(java.sql.Types.DOUBLE, "java.sql.Types.DOUBLE"));
        dataTypesVector.add(new HSDataType(java.sql.Types.NUMERIC, "java.sql.Types.NUMERIC"));
        dataTypesVector.add(new HSDataType(java.sql.Types.DECIMAL, "java.sql.Types.DECIMAL"));
        dataTypesVector.add(new HSDataType(java.sql.Types.CHAR, "java.sql.Types.CHAR"));
        dataTypesVector.add(new HSDataType(java.sql.Types.VARCHAR, "java.sql.Types.VARCHAR"));
        dataTypesVector.add(new HSDataType(java.sql.Types.LONGVARCHAR, "java.sql.Types.LONGVARCHAR"));
        dataTypesVector.add(new HSDataType(java.sql.Types.DATE, "java.sql.Types.DATE"));
        dataTypesVector.add(new HSDataType(java.sql.Types.TIME, "java.sql.Types.TIME"));
        dataTypesVector.add(new HSDataType(java.sql.Types.TIMESTAMP, "java.sql.Types.TIMESTAMP"));
        dataTypesVector.add(new HSDataType(java.sql.Types.BINARY, "java.sql.Types.BINARY"));
        dataTypesVector.add(new HSDataType(java.sql.Types.VARBINARY, "java.sql.Types.VARBINARY"));
        dataTypesVector.add(new HSDataType(java.sql.Types.LONGVARBINARY, "java.sql.Types.LONGVARBINARY"));
        dataTypesVector.add(new HSDataType(java.sql.Types.NULL, "java.sql.Types.NULL"));
        dataTypesVector.add(new HSDataType(java.sql.Types.OTHER, "java.sql.Types.OTHER"));
        dataTypesVector.add(new HSDataType(java.sql.Types.BLOB, "java.sql.Types.BLOB"));
        dataTypesVector.add(new HSDataType(java.sql.Types.CLOB, "java.sql.Types.CLOB"));
        try {
            openDataTypeFile();
            readDataTypeFile();
            parseDataTypeFile();
            closeDataTypeFile();
        } catch (Exception ex) {
            log.error("A problem occurs with '" + dataTypeFileName + "'", ex);
            throw new RuntimeException(ex);
        }
    }

    void openDataTypeFile() throws UnsupportedEncodingException, IOException {
        dataTypeFileName = "DataTypes.txt";
        try {
            dataTypeFile = new File(dataTypeFileName);
            dataTypeFis = new FileInputStream(dataTypeFile);
            dataTypeIsr = new InputStreamReader(dataTypeFis, "UTF-8");
        } catch (FileNotFoundException ex) {
            URL url = ClassLoader.getSystemResource(dataTypeFileName);
            dataTypeIs = url.openStream();
            dataTypeIsr = new InputStreamReader(dataTypeIs, "UTF-8");
        }
        dataTypeReader = new BufferedReader(dataTypeIsr);
    }

    void closeDataTypeFile() throws IOException {
        if (dataTypeReader != null) {
            dataTypeReader.close();
            dataTypeReader = null;
        }
        if (dataTypeIsr != null) {
            dataTypeIsr.close();
            dataTypeIsr = null;
        }
        if (dataTypeIs != null) {
            dataTypeIs.close();
            dataTypeIs = null;
        }
        if (dataTypeFis != null) {
            dataTypeFis.close();
            dataTypeFis = null;
        }
    }

    void readDataTypeFile() throws IOException {
        while (dataTypeReader.ready()) {
            String line = dataTypeReader.readLine();
            stringDataTypeArray.add(line);
        }
    }

    void setArray4(String dataTypeString, String sqlDataTypeName, String javaDataTypeName, String hibernateDataTypeName) {
        for (int i = 0; i < dataTypesVector.size(); i++) {
            HSDataType dataType = (HSDataType) dataTypesVector.get(i);
            if (dataType.getDataTypeSting().equals(dataTypeString)) {
                dataType.setSqlDataTypeName(sqlDataTypeName);
                dataType.setJavaDataTypeName(javaDataTypeName);
                dataType.setHibernateDataTypeName(hibernateDataTypeName);
                dataTypesVector.set(i, dataType);
                return;
            }
        }
    }

    void parseDataTypeFile() throws IOException {
        for (int i = 0; i < stringDataTypeArray.size(); i++) {
            String line = (String) stringDataTypeArray.get(i);
            String[] array = line.split(",");
            if (array.length == 4) {
                setArray4(array[0], array[1], array[2], array[3]);
            }
        }
    }

    private Vector getDataTypesVector() {
        return dataTypesVector;
    }

    private void setDataTypesVector(Vector aDataTypesVector) {
        dataTypesVector = aDataTypesVector;
    }

    public int size() {
        if (getInstance().getDataTypesVector() != null) {
            return getInstance().getDataTypesVector().size();
        }
        return 0;
    }

    public HSDataType get(int pos) {
        if (getInstance().getDataTypesVector() != null) {
            return (HSDataType) getInstance().getDataTypesVector().get(pos);
        }
        return null;
    }

    public String getJavaDataType(int numericDataType) {
        int pos = 0;
        while (pos < this.size()) {
            HSDataType dataType = getInstance().get(pos);
            if (dataType.getDataType() == numericDataType) {
                return dataType.getJavaDataTypeName();
            }
            pos = pos + 1;
        }
        return "";
    }

    public String getHibernateDataType(int numericDataType) {
        int pos = 0;
        while (pos < this.size()) {
            HSDataType dataType = (HSDataType) getInstance().get(pos);
            if (dataType.getDataType() == numericDataType) {
                return dataType.getHibernateDataTypeName();
            }
            pos = pos + 1;
        }
        return "";
    }
}
