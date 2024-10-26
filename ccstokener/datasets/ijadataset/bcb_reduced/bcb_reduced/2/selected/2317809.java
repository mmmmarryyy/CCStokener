package jinvest;

import java.io.*;
import java.net.*;
import javax.swing.JApplet;
import au.com.bytecode.opencsv.CSVReader;

public class DataHandler {

    JApplet ja;

    IndexSet allset;

    PortFolio pfset;

    int selectstart, selectend;

    String[] names;

    public DataHandler(JApplet ja, String[] names) {
        super();
        this.ja = ja;
        this.names = names;
        int lines = NumberOfLines.numberofLines(ja, names[0]);
        allset = new IndexSet(names, lines, names.length);
        readData();
        selectstart = 0;
        selectend = allset.values.getRowDimension() - 1;
        pfset = createPortfolio(0, allset.values.getRowDimension());
        double[] alloc = { .3, .6, .1 };
        pfset.setAllocation(alloc);
    }

    PortFolio createPortfolio(int startindex, int endindex) {
        int i, j;
        String[] temp = new String[names.length + 1];
        System.arraycopy(names, 0, temp, 0, names.length);
        temp[names.length] = "Portfolio";
        PortFolio pf = new PortFolio(temp, endindex - startindex, temp.length);
        for (i = 0; i < endindex - startindex; i++) {
            pf.months[i] = allset.months[i + startindex];
            pf.years[i] = allset.years[i + startindex];
            for (j = 0; j < temp.length - 1; j++) {
                pf.values.getDataRef()[i][j] = allset.values.getEntry(i + startindex, j);
            }
        }
        pf.normalize();
        pf.calcPortfolio();
        return pf;
    }

    void readData() {
        String[] nextLine;
        int line;
        double value;
        URL url = null;
        String FileToRead;
        try {
            for (int i = 0; i < names.length; i++) {
                FileToRead = "data/" + names[i] + ".csv";
                url = new URL(ja.getCodeBase(), FileToRead);
                System.out.println(url.toString());
                InputStream in = url.openStream();
                CSVReader reader = new CSVReader(new InputStreamReader(in));
                line = 0;
                while ((nextLine = reader.readNext()) != null) {
                    allset.months[line] = Integer.parseInt(nextLine[0].substring(0, 2));
                    allset.years[line] = Integer.parseInt(nextLine[0].substring(6, 10));
                    value = Double.parseDouble(nextLine[1]);
                    allset.values.getDataRef()[line][i] = value;
                    line++;
                }
            }
        } catch (IOException e) {
            System.err.println("File Read Exception");
        }
    }
}
