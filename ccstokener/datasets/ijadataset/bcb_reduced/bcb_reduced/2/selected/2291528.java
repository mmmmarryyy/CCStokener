package Factories;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Factories {

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        BufferedWriter txtwriter;
        txtwriter = new BufferedWriter(new FileWriter(new File("result.txt")));
        File loadFile = new File("cnda.txt");
        FileInputStream inputstream;
        InputStreamReader inpReader;
        inputstream = new FileInputStream(loadFile);
        inpReader = new InputStreamReader(inputstream);
        BufferedReader reader = new BufferedReader(inpReader);
        String input = null;
        while ((input = reader.readLine()) != null) {
            String[] temp = input.split("\t");
            System.out.print(temp[0] + "\t" + temp[1] + "\t");
            String[] id = temp[2].split("/");
            URL url = new URL("http://www.erepublik.com/en/citizen/profile/" + id[id.length - 1]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            InputStream stream = conn.getInputStream();
            BufferedReader netreader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuffer document = new StringBuffer();
            String line = null;
            while ((line = netreader.readLine()) != null) {
                document.append(line);
            }
            Pattern p = Pattern.compile(".*</strong>&nbsp;(.*) buildings</p>.*");
            Matcher m = p.matcher(document.toString());
            m.matches();
            System.out.println(m.group(1) + "\tbuildings");
        }
    }
}
