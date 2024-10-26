package playground.fabrice.primloc;

import java.net.URL;
import junit.framework.TestCase;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeMap;
import java.io.File;
import org.matsim.testcases.MatsimTestCase;
import Jama.Matrix;

public class PrimlocEngine extends MatsimTestCase {

    int numZ;

    double N;

    double P[];

    double J[];

    double X[];

    Matrix cij;

    Matrix ecij;

    Matrix trips;

    Matrix calib;

    int maxiter = 100;

    double mu = 1.0;

    double theta = 0.5;

    double threshold1 = 1E-2;

    double threshold2 = 1E-6;

    double threshold3 = 1E-2;

    DecimalFormat df;

    boolean verbose;

    boolean calibration;

    public void testZurich() {
        System.out.println("Starting Primary Location Choice Model (PLCM)");
        System.out.println("  -- test on Zurich sample data set");
        TreeMap<Integer, Integer> zonemap;
        zonemap = initProperties(this.getClass().getResource("/playground/fabrice/primloc/resources/ZurichTestProperties.xml"));
        calib = gravityModelMatrix();
        calibrationProcess();
        String outputDirectory = "test/output/" + this.getClass().getCanonicalName().replace('.', '/') + "/" + getName() + "/";
        String filename = outputDirectory + "/rent_test_Zurich.txt";
        SomeIO.saveResults(X, zonemap, getOutputDirectory() + "/rent_test_Zurich.txt");
        System.out.println("PLCM done");
    }

    public PrimlocEngine() {
        df = (DecimalFormat) DecimalFormat.getInstance();
        df.applyPattern("0.###E0");
    }

    void calibrationProcess() {
        double muC = mu;
        double muL = mu;
        double muR = mu;
        double errC = 0.0;
        double errL = Double.NEGATIVE_INFINITY;
        double errR = Double.NEGATIVE_INFINITY;
        runModel();
        errC = calibrationError();
        System.out.println("Mu: " + df.format(mu) + "\tError: " + df.format(errC));
        while (errL < errC) {
            mu = muL = mu / 2;
            setupECij();
            runModel();
            errL = calibrationError();
            System.out.println("Mu inf: " + df.format(mu) + "\tError: " + df.format(errL));
            if (errL < errC) {
                errC = errL;
                errL = Double.NEGATIVE_INFINITY;
                muR = muC;
                errR = errC;
                mu = muC = muL;
            }
        }
        mu = muR;
        while (errR < errC) {
            mu = muR = 2 * mu;
            setupECij();
            runModel();
            errR = calibrationError();
            System.out.println("Mu sup: " + df.format(mu) + "\tError: " + df.format(errR));
        }
        while (Math.min(errR - errC, errL - errC) / errC > threshold3) {
            mu = (muC + muL) / 2;
            setupECij();
            runModel();
            double err = calibrationError();
            System.out.println("Mu : " + df.format(mu) + "\tError: " + df.format(err));
            if (err < errC) {
                muR = muC;
                errR = errC;
                muC = mu;
                errC = err;
            } else if (err < errL) {
                muL = mu;
                errL = err;
            }
            mu = (muC + muR) / 2;
            setupECij();
            runModel();
            err = calibrationError();
            System.out.println("Mu : " + df.format(mu) + "\tError: " + df.format(err));
            if (err < errC) {
                muL = muC;
                errL = errC;
                muC = mu;
                errC = err;
            } else if (err < errR) {
                muR = mu;
                errR = err;
            }
        }
        System.out.println("Final value : " + muC);
    }

    double calibrationError() {
        double sum = 0.0;
        for (int i = 0; i < numZ; i++) {
            for (int j = 0; j < numZ; j++) {
                double s = calib.get(i, j);
                if (s > 0.0) {
                    double dt = calib.get(i, j) - trips.get(i, j);
                    sum += dt * dt;
                }
            }
        }
        return Math.sqrt(sum / (numZ * numZ));
    }

    void normalizeJobVector() {
        double sumJ = 0.0;
        for (int i = 0; i < numZ; i++) sumJ += J[i];
        sumJ = N / sumJ;
        for (int i = 0; i < numZ; i++) J[i] = J[i] * sumJ;
    }

    void runModel() {
        initRent();
        if (verbose) {
            System.out.println("========");
            System.out.println("Initial rent values statistics");
            rentStatistics();
            System.out.println("========");
        }
        iterativeSubstitutions();
        solvequs();
        if (verbose) {
            System.out.println("========");
            System.out.println("Final rent values statistics");
            rentStatistics();
            System.out.println("========");
        }
        computeTripMatrix();
        computeFinalRents();
    }

    void computeTripMatrix() {
        double Z[] = new double[numZ];
        for (int j = 0; j < numZ; j++) for (int k = 0; k < numZ; k++) Z[j] += ecij.get(k, j) * X[k];
        trips = new Matrix(numZ, numZ);
        for (int i = 0; i < numZ; i++) for (int j = 0; j < numZ; j++) trips.set(i, j, J[j] * ecij.get(i, j) * X[i] / Z[j]);
    }

    void computeFinalRents() {
        double minir = Double.POSITIVE_INFINITY;
        for (int i = 0; i < numZ; i++) {
            X[i] = -mu * Math.log(X[i]);
            if (minir > X[i]) minir = X[i];
        }
        for (int i = 0; i < numZ; i++) X[i] -= minir;
    }

    TreeMap<Integer, Integer> initProperties(URL propurl) {
        String zoneFileName = null;
        String costFileName = null;
        String homesFileName = null;
        String jobsFileName = null;
        Properties props = new Properties();
        try {
            props.loadFromXML(propurl.openStream());
            zoneFileName = props.getProperty("zoneFileName");
            costFileName = props.getProperty("costFileName");
            homesFileName = props.getProperty("homesFileName");
            jobsFileName = props.getProperty("jobsFileName");
            maxiter = Integer.parseInt(props.getProperty("maxiter"));
            mu = Double.parseDouble(props.getProperty("mu"));
            theta = Double.parseDouble(props.getProperty("theta"));
            threshold1 = Double.parseDouble(props.getProperty("threshold1"));
            threshold2 = Double.parseDouble(props.getProperty("threshold2"));
            verbose = Boolean.parseBoolean(props.getProperty("verbose"));
        } catch (Exception xc) {
            xc.printStackTrace();
            System.exit(-1);
        }
        HashSet<Integer> zoneids = SomeIO.readZoneIDs(zoneFileName);
        numZ = zoneids.size();
        if (verbose) {
            System.out.println("Data:");
            System.out.println(" . #zones:" + numZ);
        }
        int idx = 0;
        TreeMap<Integer, Integer> zonemap = new TreeMap<Integer, Integer>();
        for (Integer id : zoneids) zonemap.put(id, idx++);
        cij = SomeIO.readMatrix(costFileName, numZ, numZ);
        for (int i = 0; i < numZ; i++) {
            double mincij = Double.POSITIVE_INFINITY;
            for (int j = 0; j < numZ; j++) {
                double v = cij.get(i, j);
                if ((v < mincij) && (v > 0.0)) mincij = v;
            }
            if (cij.get(i, i) == 0.0) cij.set(i, i, mincij);
        }
        setupECij();
        double meanCost = 0.0;
        double stdCost = 0.0;
        for (int i = 0; i < numZ; i++) {
            for (int j = 0; j < numZ; j++) {
                double v = cij.get(i, j);
                meanCost += v;
                stdCost += v * v;
            }
        }
        meanCost = meanCost / (numZ * numZ);
        stdCost = stdCost / (numZ * numZ) - meanCost * meanCost;
        if (verbose) System.out.println(" . Travel costs  mean=" + meanCost + " std.dev.= " + Math.sqrt(stdCost));
        P = SomeIO.readZoneAttribute(numZ, homesFileName, zonemap);
        J = SomeIO.readZoneAttribute(numZ, jobsFileName, zonemap);
        double maxpj = 0.0;
        double sp = 0.0;
        double sj = 0.0;
        for (int i = 0; i < numZ; i++) {
            sp += P[i];
            sj += J[i];
            if (P[i] > maxpj) maxpj = P[i];
            if (J[i] > maxpj) maxpj = J[i];
        }
        if (Math.abs(sp - sj) > 1.0) {
            System.err.println("Error: #jobs(" + sj + ")!= #homes(" + sp + ")");
            System.exit(-1);
        }
        N = sp;
        if (verbose) System.out.println(" . Trip tables: #jobs=#homes= " + N);
        return zonemap;
    }

    void initRent() {
        X = new double[numZ];
        int undefs = 0;
        for (int i = 0; i < numZ; i++) {
            X[i] = P[i];
            if (X[i] == 0.0) undefs++;
        }
        if (undefs > 0) System.err.println("Warning: rent undefined in " + undefs + " locations");
    }

    void rentStatistics() {
        double minix = Double.POSITIVE_INFINITY;
        double maxix = Double.NEGATIVE_INFINITY;
        double sumR1 = 0.0;
        double sumR2 = 0.0;
        int count = 0;
        for (int i = 0; i < numZ; i++) {
            if (X[i] == 0) continue;
            if (X[i] < minix) minix = X[i];
            if (X[i] > maxix) maxix = X[i];
            double ri = Math.log(X[i]);
            sumR1 += ri;
            sumR2 += ri * ri;
            count++;
        }
        double sigmaR = mu * Math.sqrt(1.0 / count * sumR2 - 1.0 / (count * count) * sumR1 * sumR1);
        double minR = -mu * Math.log(maxix);
        double maxR = -mu * Math.log(minix);
        System.out.println("Rent: [ " + df.format(minR) + ", " + df.format(maxR) + " ]\t sigma: " + df.format(sigmaR));
    }

    void setupECij() {
        ecij = new Matrix(numZ, numZ);
        for (int i = 0; i < numZ; i++) for (int j = 0; j < numZ; j++) ecij.set(i, j, Math.exp(-cij.get(i, j) / mu));
    }

    void iterativeSubstitutions() {
        Matrix PR = new Matrix(numZ, numZ);
        double[] Z = new double[numZ];
        double[] DX = new double[numZ];
        for (int iterations = 0; iterations < maxiter; iterations++) {
            int i;
            for (i = 0; i < numZ; i++) {
                for (int j = 0; j < numZ; j++) PR.set(i, j, ecij.get(i, j) * X[i]);
                Z[i] = 0.0;
            }
            for (i = 0; i < numZ; i++) for (int k = 0; k < numZ; k++) Z[i] += PR.get(k, i);
            for (i = 0; i < numZ; i++) {
                DX[i] = 0.0;
                for (int j = 0; j < numZ; j++) DX[i] += J[j] * PR.get(i, j) / Z[j];
                DX[i] = DX[i] - P[i];
            }
            double sumx = 0.0;
            double residual = 0.0;
            for (i = 0; i < numZ; i++) {
                if (X[i] == 0.0) continue;
                sumx += X[i] * X[i];
                residual += (DX[i] * DX[i]);
                double fac = 1.0;
                while (fac * theta * DX[i] >= X[i]) fac = fac / 10.0;
                X[i] = X[i] - fac * theta * DX[i];
            }
            residual = Math.sqrt(residual / sumx);
            if (verbose) {
                if ((iterations % 10 == 0) || (residual < threshold1)) {
                    System.out.println("Iteration: " + iterations + " Residual:" + df.format(residual));
                    rentStatistics();
                }
            }
            if (residual < threshold1) break;
        }
    }

    void solvequs() {
        int numR = numZ - 1;
        int count = 0;
        while (count++ < maxiter) {
            double[] Z = new double[numZ];
            Matrix A = new Matrix(numR, numR);
            double[] B = new double[numR];
            for (int j = 0; j < numZ; j++) {
                Z[j] = 0.0;
                for (int k = 0; k < numZ; k++) Z[j] += ecij.get(k, j) * X[k];
            }
            for (int i = 0; i < numR; i++) {
                B[i] = 0.0;
                for (int j = 0; j < numZ; j++) B[i] += (ecij.get(i, j) * J[j]) / Z[j];
                B[i] = B[i] * X[i];
                B[i] = B[i] - P[i];
            }
            double sumx = 0.0;
            double residual = 0.0;
            for (int i = 0; i < numR; i++) {
                sumx += X[i] * X[i];
                residual = residual + B[i] * B[i];
            }
            residual = Math.sqrt(residual / sumx);
            if (verbose) System.out.println("Residual:\t" + df.format(residual));
            if (residual < threshold2) break;
            for (int i = 0; i < numR; i++) {
                for (int j = 0; j < numR; j++) {
                    if (i == j) {
                        double sum = 0.0;
                        for (int k = 0; k < numZ; k++) sum += (ecij.get(i, k) * J[k] / Z[k]);
                        A.set(i, j, sum);
                    }
                    double tsum = 0.0;
                    for (int k = 0; k < numZ; k++) tsum += (ecij.get(i, k) * ecij.get(j, k) * J[k]) / (Z[k] * Z[k]);
                    A.set(i, j, A.get(i, j) - tsum * X[i]);
                }
            }
            Matrix b = new Matrix(B, numR);
            long timeMill = System.currentTimeMillis();
            Matrix x = A.solve(b);
            timeMill = System.currentTimeMillis() - timeMill;
            double duration = timeMill / 1000.0;
            if (verbose) System.out.println("Solved in:\t" + duration + " s");
            for (int i = 0; i < numR; i++) X[i] = X[i] - theta * x.get(i, 0);
            if (verbose) rentStatistics();
        }
    }

    Matrix gravityModelMatrix() {
        Matrix grav = new Matrix(numZ, numZ);
        double sum1 = 0.0;
        for (int i = 0; i < numZ; i++) {
            for (int j = 0; j < numZ; j++) {
                double v = P[i] * J[j] / cij.get(i, j);
                grav.set(i, j, v);
                sum1 += v;
            }
        }
        double v = 0.0;
        for (int i = 0; i < numZ; i++) for (int j = 0; j < numZ; j++) {
            grav.set(i, j, grav.get(i, j) * N / sum1);
            v += grav.get(i, j);
        }
        return grav;
    }

    double sumofel(Matrix A) {
        double z = 0.0;
        for (int i = 0; i < A.getRowDimension(); i++) for (int j = 0; j < A.getColumnDimension(); j++) z += A.get(i, j);
        return z;
    }

    double[] getCijScale(int n) {
        double[] vals = new double[n];
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < numZ; i++) {
            for (int j = 0; j < numZ; j++) {
                double v = cij.get(i, j);
                if (v > max) max = v;
                if (v < min) min = v;
            }
        }
        for (int i = 0; i < n - 1; i++) vals[i] = min + (i * (max - min)) / n;
        return vals;
    }

    double[] histogram(Matrix x, double[] vals) {
        double[] bins = new double[vals.length];
        for (int i = 0; i < numZ; i++) {
            for (int j = 0; j < numZ; j++) {
                double v = x.get(i, j);
                int k = 1;
                while ((v > vals[k]) && (k < vals.length - 1)) k++;
                bins[k - 1] += x.get(i, j);
            }
        }
        return bins;
    }

    double getHistogramError(Matrix x, Matrix y) {
        double error = 0.0;
        double[] scale = getCijScale(100);
        double[] u = histogram(x, scale);
        double[] v = histogram(y, scale);
        for (int i = 0; i < scale.length; i++) error += (u[i] - v[i]) * (u[i] - v[i]);
        return Math.sqrt(error);
    }
}
