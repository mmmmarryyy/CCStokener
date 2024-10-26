package DB;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import DB.DatabaseTables.Table;

public class Importer {

    public ImportState state = ImportState.NonActive;

    DatabaseTables tables = DatabaseTables.getInstance();

    public Importer() {
    }

    public String importDatabases() {
        ArrayList<freebaseTable> files = categoryConnector.getInstance().getAllCategories();
        String retval = "";
        for (freebaseTable file : files) {
            retval += importTable(file);
        }
        state = ImportState.Done;
        return retval;
    }

    public String importTable(freebaseTable file) {
        int fileSizeInBytes = 0;
        int bytesRecieved = 0;
        int packetSize = 0;
        String row;
        Connection conn = PhraseConnector.getInstance().openConnection();
        try {
            conn.setAutoCommit(false);
            URL url = new URL(file.fileName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int result = connection.getResponseCode();
            if (result == HttpURLConnection.HTTP_OK) {
                fileSizeInBytes = connection.getContentLength();
                byte[] data = new byte[fileSizeInBytes];
                InputStream input = connection.getInputStream();
                state = ImportState.Downloading;
                while (bytesRecieved < fileSizeInBytes) {
                    bytesRecieved += input.read(data, bytesRecieved, fileSizeInBytes - bytesRecieved);
                }
                input.close();
                BufferedReader bufferReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
                bytesRecieved = 0;
                state = ImportState.UpdatingDB;
                Statement statement = conn.createStatement();
                bufferReader.readLine();
                do {
                    row = bufferReader.readLine();
                    if (row == null) break;
                    if (!row.replaceAll(" ", "").equals("")) {
                        String[] rowData = proccessLine(file, row);
                        statement.addBatch(buildCommand(rowData));
                        packetSize++;
                        if (packetSize == 100 && statement != null) {
                            try {
                                statement.executeBatch();
                                conn.commit();
                                statement.clearBatch();
                                packetSize = 0;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    bytesRecieved += row.length();
                } while (row != null);
                bufferReader.close();
                if (statement != null) {
                    statement.executeBatch();
                    conn.commit();
                    statement.clearBatch();
                    packetSize = 0;
                }
            } else {
                return "Import " + file.categoryName + " failed- Error Downloading File " + file.fileName + "\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Import " + file.categoryName + " failed- Error executing commands :" + e.toString() + "\n";
        } finally {
            PhraseConnector.getInstance().closeConnection(conn);
        }
        return "Import Category successed: " + file.categoryName + "\n";
    }

    private String[] proccessLine(freebaseTable file, String row) {
        String[] parts = row.split("	");
        String[] retval = new String[4];
        retval[0] = parts[0];
        retval[1] = parts[1];
        if (file.hintColumns > 0 && parts.length > file.hintColumns && !parts[file.hintColumns].isEmpty()) {
            retval[2] = file.HintStr + parts[file.hintColumns];
            if (retval[2].length() > 1000) {
                retval[2] = retval[2].substring(0, 990) + "...";
            }
        } else {
            retval[2] = "";
        }
        retval[3] = file.categoryName;
        return retval;
    }

    private String buildCommand(String[] data) {
        String command = new String();
        Table phraseTable = tables.phrases;
        String legalName = data[0].replace('\"', '\'');
        String legalHint = data[2].replace('\"', '\'');
        command += "INSERT INTO " + phraseTable.name + "(";
        command += phraseTable.columns[0] + ",";
        command += phraseTable.columns[1] + ",";
        command += phraseTable.columns[2] + ",";
        command += phraseTable.columns[3] + ",";
        command += phraseTable.columns[4] + ") VALUES (\"";
        command += data[1] + "\",\"";
        command += legalName + "\",\"";
        command += data[3] + "\",\"";
        command += legalHint + "\",";
        command += "0) ON DUPLICATE KEY UPDATE " + phraseTable.columns[4] + "=" + phraseTable.columns[4] + " +1";
        return command;
    }
}
