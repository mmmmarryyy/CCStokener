package gov.sns.apps.rocs;

import java.math.*;
import java.util.*;
import java.io.*;
import java.net.URL;

public class ChromSettings {

    private static int max = 100;

    private double temp_new, temp_prev;

    private String s;

    private int i = 0, j = 0;

    public double chrom_x[][] = new double[max][max];

    public double chrom_y[][] = new double[max][max];

    public double kfl, kfs, kd, kdee, kdc, kfc;

    public double sext1[][] = new double[max][max];

    public double sext2[][] = new double[max][max];

    public double sext3[][] = new double[max][max];

    public double sext4[][] = new double[max][max];

    public int imax = 0, jmax = 0;

    public double gridmin = 0, gridmax = 0;

    public void readData() throws IOException {
        i = 0;
        j = 0;
        URL url = getClass().getResource("resources/Chrom_623_620.dat");
        InputStream is = url.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        s = br.readLine();
        StringTokenizer st = new StringTokenizer(s);
        s = br.readLine();
        st = new StringTokenizer(s);
        chrom_x[i][j] = Double.parseDouble(st.nextToken());
        temp_prev = chrom_x[i][j];
        chrom_y[i][j] = Double.parseDouble(st.nextToken());
        gridmin = chrom_x[i][j];
        gridmax = chrom_x[i][j];
        sext1[i][j] = Double.parseDouble(st.nextToken());
        sext2[i][j] = Double.parseDouble(st.nextToken());
        sext3[i][j] = Double.parseDouble(st.nextToken());
        sext4[i][j] = Double.parseDouble(st.nextToken());
        j++;
        while ((s = br.readLine()) != null) {
            st = new StringTokenizer(s);
            temp_new = Double.parseDouble(st.nextToken());
            if (temp_new != temp_prev) {
                temp_prev = temp_new;
                i++;
                j = 0;
            }
            chrom_x[i][j] = temp_new;
            chrom_y[i][j] = Double.parseDouble(st.nextToken());
            sext1[i][j] = Double.parseDouble(st.nextToken());
            sext2[i][j] = Double.parseDouble(st.nextToken());
            sext3[i][j] = Double.parseDouble(st.nextToken());
            sext4[i][j] = Double.parseDouble(st.nextToken());
            imax = i;
            jmax = j;
            j++;
            if (chrom_x[i][j] <= gridmin) gridmin = chrom_x[i][j];
            if (chrom_x[i][j] >= gridmax) gridmax = chrom_x[i][j];
        }
    }

    public int getImax() {
        return imax;
    }

    public int getJmax() {
        return jmax;
    }

    public double getMin() {
        return gridmin;
    }

    public double getMax() {
        return gridmax;
    }
}
