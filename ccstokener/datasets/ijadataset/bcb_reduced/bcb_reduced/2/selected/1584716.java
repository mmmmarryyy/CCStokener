package gov.sns.apps.rocs;

import java.math.*;
import java.util.*;
import java.io.*;
import java.net.URL;

public class CalcSettings {

    private static int max = 100;

    private double temp_new, temp_prev;

    private String s;

    private double gridmin, gridmax;

    private int isize, jsize;

    private double data[][][];

    public double key_x = 0, key_y = 0;

    public int foundxi = 0, foundyj = 0;

    public double k[];

    public CalcSettings() {
    }

    void readData(URL url) throws IOException {
        int i = 0, j = 0, k = 0;
        double xvalue, yvalue;
        double xindex, yindex;
        InputStream is = url.openStream();
        is.mark(0);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        int columnsize = 0;
        double temp_prev = 0;
        double temp_new = 0;
        int first = 0;
        s = br.readLine();
        StringTokenizer st = new StringTokenizer(s);
        columnsize = Integer.parseInt(st.nextToken());
        data = new double[columnsize][100][100];
        isize = 0;
        jsize = 0;
        while ((s = br.readLine()) != null) {
            st = new StringTokenizer(s);
            for (k = 0; k < columnsize; k++) {
                temp_new = Double.parseDouble(st.nextToken());
                if (first == 0) {
                    temp_prev = temp_new;
                    first = 1;
                }
                if (k == 0) {
                    if (temp_new != temp_prev) {
                        temp_prev = temp_new;
                        i++;
                        j = 0;
                    }
                }
                data[k][i][j] = temp_new;
            }
            j++;
        }
        isize = i + 1;
        jsize = j;
    }

    void searchData(double key_X, double key_Y) {
        int iindex = 0;
        int jindex = 0;
        foundxi = 0;
        foundyj = 0;
        key_x = key_X;
        key_y = key_Y;
        for (iindex = 0; iindex < isize; iindex++) {
            if (key_x > data[0][iindex][0] && key_x <= data[0][iindex + 1][0]) {
                foundxi = iindex;
            }
        }
        for (jindex = 0; jindex < jsize; jindex++) {
            if (key_y > data[1][foundxi][jindex] && key_y <= data[1][foundxi][jindex + 1]) {
                foundyj = jindex;
            }
        }
    }

    double[] getMags(URL url, double inx, double iny) {
        try {
            readData(url);
            searchData(inx, iny);
        } catch (IOException e) {
            System.out.println("Unable to read the data file.");
        }
        double distx, disty;
        double deltax, deltay;
        double ptx, pty;
        int i = 0;
        int xi = foundxi;
        int yj = foundyj;
        k = new double[data.length - 2];
        for (i = 0; i < k.length; i++) k[i] = 0;
        ptx = data[0][xi][yj];
        pty = data[1][xi][yj];
        if (ptx == data[0][isize][yj]) {
            deltax = 0.0;
        } else {
            distx = data[0][xi + 1][yj] - ptx;
            deltax = (inx - data[0][xi][yj]) / distx;
        }
        if (pty == data[1][xi][jsize]) {
            deltay = 0.0;
        } else {
            disty = data[1][xi][yj + 1] - pty;
            deltay = (iny - data[1][xi][yj]) / disty;
        }
        for (i = 0; i < k.length; i++) {
            k[i] = Math.abs(((1 - deltax) * (1 - deltay) * (data[i + 2][xi][yj]) + (deltax) * (1 - deltay) * (data[i + 2][xi + 1][yj]) + (1 - deltax) * (deltay) * (data[i + 2][xi][yj + 1]) + (deltax * deltay) * (data[i + 2][xi + 1][yj + 1])));
        }
        return k;
    }

    public double getMinx() {
        return data[0][0][0];
    }

    public double getMiny() {
        return data[1][0][0];
    }

    public double getMaxx() {
        return data[0][isize - 1][jsize - 1];
    }

    public double getMaxy() {
        return data[1][isize - 1][jsize - 1];
    }
}
