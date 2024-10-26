package playground.fabrice.primloc;

import java.io.FileOutputStream;
import junit.framework.TestCase;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import Jama.Matrix;

public class SomeIO extends TestCase {

    static double[] readZoneAttribute(int numZ, String filename, TreeMap<Integer, Integer> zonemap) {
        double[] array = new double[numZ];
        try {
            URL url = filename.getClass().getResource(filename);
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = lnr.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                Integer zoneid = zonemap.get(Integer.parseInt(st.nextToken()));
                Double value = new Double(st.nextToken());
                array[zoneid] = value;
            }
        } catch (Exception xc) {
            xc.printStackTrace();
        }
        return array;
    }

    static Matrix readMatrix(String filename, int nrow, int ncol) {
        Matrix cij = new Matrix(nrow, ncol);
        try {
            URL url = filename.getClass().getResource(filename);
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(url.openStream()));
            for (int i = 0; i < nrow; i++) for (int j = 0; j < ncol; j++) cij.set(i, j, Double.parseDouble(lnr.readLine()));
        } catch (Exception xc) {
            xc.printStackTrace();
        }
        return cij;
    }

    static HashSet<Integer> readZoneIDs(String zoneFileName) {
        HashSet<Integer> zoneids = new HashSet<Integer>();
        try {
            URL url = zoneFileName.getClass().getResource(zoneFileName);
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = lnr.readLine()) != null) zoneids.add(Integer.parseInt(line));
        } catch (Exception xc) {
            xc.printStackTrace();
        }
        return zoneids;
    }

    static void saveResults(double[] X, TreeMap<Integer, Integer> zonemap, String filename) {
        double minix = Double.POSITIVE_INFINITY;
        for (int i = 0; i < X.length; i++) if (X[i] < minix) minix = X[i];
        try {
            FileWriter out = new FileWriter(filename);
            for (Integer id : zonemap.keySet()) {
                int zone = zonemap.get(id);
                double rent = Math.abs(X[zone] - minix);
                out.write(id + "\t" + rent + "\n");
            }
            out.close();
        } catch (Exception xc) {
            xc.printStackTrace();
        }
    }

    static void loadprop() {
        Properties props = new Properties();
        props.setProperty("zoneFileName", "/Users/fmarchal/archives/piz/fmarchal/research/transportation/landuse/experiments/Zurich/zones.txt");
        props.setProperty("costFileName", "/Users/fmarchal/archives/piz/fmarchal/research/transportation/landuse/experiments/Zurich/costs_IT0.txt");
        props.setProperty("homesFileName", "/Users/fmarchal/archives/piz/fmarchal/research/transportation/landuse/experiments/Zurich/homes.txt");
        props.setProperty("jobsFileName", "/Users/fmarchal/archives/piz/fmarchal/research/transportation/landuse/experiments/Zurich/jobs.txt");
        props.setProperty("maxiter", "100");
        try {
            props.storeToXML(new FileOutputStream("/tmp/meprop.xml"), "Hello");
        } catch (Exception xc) {
            xc.printStackTrace();
        }
    }

    public void testLinearSolver() {
        testSolver(10);
        testSolver(100);
        testSolver(1000);
    }

    static void testSolver(int n) {
        System.out.println("Testing matrix solver");
        System.out.println("Solving A.X = B for size:" + n);
        System.out.println("Comp. time is O(N3)");
        Matrix A = Matrix.random(n, n);
        Matrix b = Matrix.random(n, 1);
        long timeMill = System.currentTimeMillis();
        Matrix x = A.solve(b);
        timeMill = System.currentTimeMillis() - timeMill;
        double duration = timeMill / 1000.0;
        System.out.println("Solved in:\t" + duration + " s");
        Matrix Residual = A.times(x).minus(b);
        double rnorm = Residual.normInf();
        System.out.println("Residual:\t" + rnorm);
        System.out.println("Test over");
    }

    static Matrix readMatrix1(String filename, int nrow, int ncol) {
        String inputString = null, value = null;
        int col = 0;
        double b = 0;
        Matrix cij = new Matrix(nrow, ncol);
        try {
            URL url = filename.getClass().getResource(filename);
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(url.openStream()));
            for (int i = 0; i < nrow; i++) for (int j = 0; j < ncol; j++) {
                inputString = lnr.readLine();
                int found = -2;
                while (found != -1) {
                    found = inputString.indexOf(";");
                    if (found != -1) {
                        value = inputString.substring(0, found);
                        inputString = inputString.substring(found + 1);
                    } else {
                        value = inputString;
                    }
                    col = col + 1;
                    if (col == 3) {
                        if (value != null) cij.set(i, j, Double.parseDouble(value)); else cij.set(i, j, 0);
                    }
                }
                col = 0;
            }
        } catch (Exception xc) {
            xc.printStackTrace();
        }
        return cij;
    }
}
