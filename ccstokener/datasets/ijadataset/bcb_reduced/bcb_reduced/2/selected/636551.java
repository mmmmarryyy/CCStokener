package larpplanner.database;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import larpplanner.gui.PlannerMain;
import larpplanner.logic.LARPManager;
import larpplanner.logic.LARPManagerImp;

public class Parser {

    private static LARPManager manager = LARPManagerImp.get();

    private static DBHandler dbHndlr = manager.getDBHandler();

    private static final int dataChunkSize = 20000;

    private static final String SCHEME = PlannerMain.SCHEME;

    private static final String URL = "http://download.freebase.com/datadumps/latest/browse/fictional_universe/fictional_character.tsv";

    private static final String InFileName = "sql_files/fictional_character.tsv";

    private static final String OutFileName = "sql_files/characters_tables_insert_script.sql";

    /**
	 * Read a file from filePath.
	 * @return the content of the file.
	 */
    public static String readFileAsString(String filePath) {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } catch (IOException e) {
            System.out.println("Cannot read file...\n" + e);
        } finally {
            if (f != null) try {
                f.close();
            } catch (IOException ignored) {
            }
        }
        return new String(buffer);
    }

    /**
	 * Updates DB with values from the web
	 * @param firstTime true if this first import and false if this is update to previous import 
	 * @return true if Update succeeded and false otherwise
	 */
    public static boolean UpdateDB(boolean firstTime) {
        Scanner scnr = null;
        String scheme = null;
        try {
            if (firstTime) {
                if (!DownloadDB()) return false;
                if (!Parse_New()) return false;
            } else {
                if (!deleteFile(InFileName)) return false;
                if (!DownloadDB()) return false;
                scheme = readFileAsString("sql_files/LARP-schema-script-update.sql");
                scheme = "USE " + SCHEME + ";" + "\n" + scheme;
                if (!dbHndlr.executeScript(scheme)) return false;
                if (!deleteFile(OutFileName)) return false;
                if (!Parse_Update()) return false;
            }
            scnr = new Scanner(new File(OutFileName)).useDelimiter("COMMIT;");
            while (scnr.hasNext()) {
                if (!dbHndlr.executeScript(scnr.next() + "COMMIT;")) return false;
            }
            scnr.close();
            if (!firstTime) {
                List<NewFBFicCharacter> temp_lst = dbHndlr.getAll(NewFBFicCharacter.class);
                scheme = "DROP TABLE " + SCHEME + ".fic_character_temp\n";
                if (!dbHndlr.executeScript(scheme)) return false;
                for (NewFBFicCharacter elem : temp_lst) {
                    List<String> powers = new ArrayList<String>();
                    List<String> species = new ArrayList<String>();
                    List<String> universes = new ArrayList<String>();
                    if (!elem.getPowers().equals("")) {
                        for (String power : removeDuplicates(elem.getPowers().split(","))) {
                            powers.add(power);
                        }
                    }
                    if (!elem.getSpecies().equals("")) {
                        for (String specie : removeDuplicates(elem.getSpecies().split(","))) {
                            species.add(specie);
                        }
                    }
                    if (!elem.getUniverses().equals("")) {
                        for (String universe : removeDuplicates(elem.getUniverses().split(","))) {
                            universes.add(universe);
                        }
                    }
                    if (dbHndlr.put(FicCharacter.class, new FicCharacter(elem.getFreebase_id(), elem.getName(), elem.getGender(), powers, species, universes)) == -1) return false;
                }
                scheme = "DROP VIEW " + SCHEME + ".fic_char_changes\n";
                if (!dbHndlr.executeScript(scheme)) return false;
            }
        } catch (FileNotFoundException e) {
            scnr.close();
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * Download DB from URL
	 * @return true if succeed and false otherwise
	 */
    private static boolean DownloadDB() {
        URL url = null;
        BufferedWriter inWriter = null;
        String line;
        try {
            url = new URL(URL);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            inWriter = new BufferedWriter(new FileWriter(InFileName));
            while ((line = reader.readLine()) != null) {
                inWriter.write(line);
                inWriter.newLine();
            }
            inWriter.close();
        } catch (Exception e) {
            try {
                inWriter.close();
            } catch (IOException ignored) {
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * Parses for the first time given InFile into OutFile 
	 * @return true if parsing succeeded and false otherwise
	 */
    private static boolean Parse_New() {
        BufferedWriter fout;
        try {
            fout = new BufferedWriter(new FileWriter(OutFileName));
            insertCharacter("fic_character", fout);
            insertGroup("char_power", fout);
            insertGroup("char_specie", fout);
            insertGroup("char_universe", fout);
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * Parses given InFile into OutFile
	 * @return true if parsing succeeded and false otherwise
	 */
    private static boolean Parse_Update() {
        BufferedWriter fout = null;
        try {
            fout = new BufferedWriter(new FileWriter(OutFileName));
            insertCharsToUpdate("fic_character_temp", fout);
            fout.close();
        } catch (IOException e) {
            try {
                fout.close();
            } catch (IOException ignored) {
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void insertCharsToUpdate(String charTableName, BufferedWriter fout) {
        BufferedReader fin = null;
        int count;
        boolean first;
        String rowToWrite;
        String line;
        String splittedLine[];
        count = 0;
        first = true;
        try {
            fin = new BufferedReader(new FileReader(InFileName));
            fout.write("use " + SCHEME + ";" + "\n");
            fout.write("SET AUTOCOMMIT=0;" + "\n");
            fout.write("INSERT INTO " + charTableName + " VALUES");
            line = fin.readLine();
            line = fin.readLine();
            while (line != null) {
                line = line.replace("'", "");
                line = line.replace(";", "/");
                splittedLine = line.split("\t");
                if (first == true) first = false; else fout.write(",");
                rowToWrite = "(" + "NULL" + "," + "'" + splittedLine[1] + "'";
                rowToWrite += "," + "'" + splittedLine[0] + "'";
                if (splittedLine.length >= 6) if (!splittedLine[5].equals("")) rowToWrite += "," + "'" + splittedLine[5] + "'"; else rowToWrite += "," + "''"; else rowToWrite += "," + "''";
                if (splittedLine.length >= 11) if (!splittedLine[10].equals("")) rowToWrite += "," + "'" + splittedLine[10] + "'"; else rowToWrite += "," + "''"; else rowToWrite += "," + "''";
                if (splittedLine.length >= 12) if (!splittedLine[11].equals("")) rowToWrite += "," + "'" + splittedLine[11] + "'"; else rowToWrite += "," + "''"; else rowToWrite += "," + "''";
                if (splittedLine.length >= 5) {
                    if (!splittedLine[4].equals("")) {
                        rowToWrite += "," + "'" + splittedLine[4] + "'";
                    } else {
                        rowToWrite += "," + "NULL";
                    }
                } else {
                    rowToWrite += "," + "NULL";
                }
                rowToWrite += ")";
                ++count;
                fout.write(rowToWrite);
                if (count == 1000) {
                    count = 0;
                    fout.write(";" + "\n");
                    fout.write("COMMIT;" + "\n");
                    first = true;
                    fout.write("USE " + SCHEME + ";" + "\n");
                    fout.write("SET AUTOCOMMIT=0;" + "\n");
                    fout.write("INSERT INTO " + charTableName + " VALUES");
                }
                line = fin.readLine();
            }
            fout.write(";" + "\n");
            fout.write("COMMIT;" + "\n");
            fin.close();
        } catch (IOException e) {
            try {
                fin.close();
            } catch (IOException ignored) {
            }
            try {
                fout.close();
            } catch (IOException ignored) {
            }
            e.printStackTrace();
        }
    }

    private static void insertCharacter(String charTableName, BufferedWriter fout) {
        BufferedReader fin;
        int count;
        boolean first;
        String line;
        String splittedLine[];
        String rowToWrite;
        count = 0;
        first = true;
        try {
            fin = new BufferedReader(new FileReader(InFileName));
            fout.write("use " + SCHEME + ";" + "\n");
            fout.write("SET AUTOCOMMIT=0;" + "\n");
            fout.write("INSERT INTO " + charTableName + " VALUES");
            line = fin.readLine();
            line = fin.readLine();
            while (line != null) {
                line = line.replace("'", "");
                line = line.replace(";", "/");
                splittedLine = line.split("\t");
                if (first == true) first = false; else fout.write(",");
                rowToWrite = "(" + "NULL" + ",";
                rowToWrite += "'" + splittedLine[1] + "'" + ",";
                rowToWrite += "'" + splittedLine[0] + "'" + ",";
                if (splittedLine.length >= 5) {
                    if (!splittedLine[4].equals("")) {
                        rowToWrite += "'" + splittedLine[4] + "'";
                    } else rowToWrite += "NULL";
                } else rowToWrite += "NULL";
                rowToWrite += ")";
                fout.write(rowToWrite);
                ++count;
                if (count == 1000) {
                    count = 0;
                    fout.write(";" + "\n");
                    fout.write("COMMIT;" + "\n");
                    first = true;
                    fout.write("USE " + SCHEME + ";" + "\n");
                    fout.write("SET AUTOCOMMIT=0;" + "\n");
                    fout.write("INSERT INTO " + charTableName + " VALUES");
                }
                line = fin.readLine();
            }
            fout.write(";" + "\n");
            fout.write("COMMIT;" + "\n");
            fin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void insertGroup(String tableName, BufferedWriter fout) {
        BufferedReader fin;
        String group[] = null;
        int count;
        int groupIndx;
        boolean first;
        String line;
        String splittedLine[];
        long char_id = 0;
        if (tableName.equals("char_specie")) groupIndx = 5; else if (tableName.equals("char_universe")) groupIndx = 11; else groupIndx = 10;
        try {
            fin = new BufferedReader(new FileReader(InFileName));
            fout.write("SET AUTOCOMMIT=0;" + "\n");
            fout.write("INSERT INTO " + tableName + " VALUES");
            count = 0;
            first = true;
            line = fin.readLine();
            line = fin.readLine();
            while (line != null) {
                line = line.replace("'", "");
                line = line.replace(";", "/");
                splittedLine = line.split("\t");
                if (splittedLine.length >= (groupIndx + 1)) if (splittedLine[groupIndx].equals("")) {
                    line = fin.readLine();
                    ++char_id;
                    continue;
                } else group = splittedLine[groupIndx].split(","); else {
                    line = fin.readLine();
                    ++char_id;
                    continue;
                }
                ++char_id;
                String[] groupWithoutDups = removeDuplicates(group);
                for (int j = 0; j < groupWithoutDups.length; j++) {
                    if (first == true) first = false; else fout.write(",");
                    fout.write("(" + char_id + ",'" + groupWithoutDups[j] + "')");
                    ++count;
                    if (count == 1000) {
                        count = 0;
                        fout.write(";" + "\n");
                        fout.write("COMMIT;" + "\n");
                        first = true;
                        fout.write("USE " + SCHEME + ";" + "\n");
                        fout.write("SET AUTOCOMMIT=0;" + "\n");
                        fout.write("INSERT INTO " + tableName + " VALUES");
                    }
                }
                line = fin.readLine();
            }
            fout.write(";" + "\n");
            fout.write("COMMIT;" + "\n");
            fin.close();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteFile(String FileName) {
        File f = new File(FileName);
        boolean success = false;
        if (f.exists()) {
            success = f.delete();
        }
        return success;
    }

    private static String[] removeDuplicates(String[] group) {
        List<String> list = Arrays.asList(group);
        Set<String> set = new HashSet<String>(list);
        String[] groupWithoutDups = new String[set.size()];
        set.toArray(groupWithoutDups);
        return groupWithoutDups;
    }
}
