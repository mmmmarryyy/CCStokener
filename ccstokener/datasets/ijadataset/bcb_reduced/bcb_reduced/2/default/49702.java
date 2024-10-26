import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;

class ImageHarvester {

    protected HttpURLConnection conn;

    protected int fail;

    public static void main(String[] args) {
        try {
            BufferedReader names = new BufferedReader(new FileReader("names.txt"));
            String name;
            DecimalFormat form = new DecimalFormat("000");
            while ((name = names.readLine()) != null) {
                File dir = new File("G:\\Entertainment\\HumanNature\\mm52\\other\\" + name);
                dir.mkdir();
                ImageHarvester session = new ImageHarvester();
                for (int i = 0; i < 999; i++) {
                    session.makeConn(name, form.format(i));
                    session.retrieveAndSave(name, form.format(i));
                    session.freeConn();
                    if (session.fail >= 5) break;
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private void makeConn(String filename1, String filename2) {
        String basename = "http://www.mm52.com/";
        String urlname = basename + "otheridols/" + filename1 + "/" + filename1 + filename2 + ".jpg";
        URL url = null;
        try {
            url = new URL(urlname);
        } catch (MalformedURLException e) {
            System.err.println("URL Format Error!");
            System.exit(1);
        }
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            System.err.println("Error IO");
            System.exit(2);
        }
    }

    private void freeConn() {
        conn.disconnect();
    }

    private void retrieveAndSave(String filename1, String filename2) {
        String filename = filename1 + "\\" + filename2 + ".jpg";
        try {
            System.out.print("Processing: " + filename);
            if (conn.getResponseCode() == 404) {
                System.out.println(" 404 Not Found");
                fail++;
            } else if (conn.getResponseCode() != 200) {
                System.out.println(" HTTP code is not 404/200");
                fail++;
            } else {
                InputStream stream = conn.getInputStream();
                FileOutputStream file = new FileOutputStream("G:\\Entertainment\\HumanNature\\mm52\\other\\" + filename);
                int c;
                while ((c = stream.read()) != -1) file.write(c);
                file.close();
                System.out.println(" OK! Complete");
                fail = 0;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }
}
