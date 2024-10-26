package beans.csvsystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class CSVControl {

    public static class CSVData {

        public int matrNumber;

        public String fName;

        public String lName;

        public String email;

        public String stdyPath;

        public int sem;
    }

    public static LinkedList Import(String url) throws Exception {
        LinkedList data = new LinkedList();
        BufferedReader in = new BufferedReader(new InputStreamReader((new URL(url)).openStream()));
        String csvLine;
        while ((csvLine = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(csvLine, ",");
            CSVData cd = new CSVData();
            st.nextToken();
            st.nextToken();
            cd.matrNumber = Integer.parseInt(st.nextToken().trim());
            cd.fName = st.nextToken().trim();
            cd.lName = st.nextToken().trim();
            cd.email = st.nextToken().trim();
            cd.stdyPath = st.nextToken().trim();
            cd.sem = Integer.parseInt(st.nextToken().trim());
            data.add(cd);
        }
        in.close();
        return data;
    }
}
