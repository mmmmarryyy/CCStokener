package PackageJInO;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.joone.engine.Layer;
import org.joone.engine.LinearLayer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.engine.SigmoidLayer;
import org.joone.io.MemoryInputSynapse;
import org.joone.io.MemoryOutputSynapse;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralNetLoader;

public class Load implements NeuralNetListener {

    private NeuralNet nnet = null;

    LinearLayer input, output;

    SigmoidLayer hidden;

    boolean singleThreadMode = true;

    private String netFile;

    private double[][] testArray;

    private MemoryInputSynapse inputSynapse;

    MemoryOutputSynapse memOut;

    double thresholdConst = 3.6;

    double threshold;

    int numRows;

    double target[][];

    public Load() {
    }

    /**
	 * @param netFile path of the stored neural net
	 * @param testArray input values to test
	 * @param numRows number of characters
	 */
    public Load(String netFile, double[][] testArray, int numRows) {
        this.netFile = netFile;
        this.testArray = testArray;
        this.target = testArray;
        this.numRows = numRows;
        netInit();
    }

    public static void main(String args[]) {
        Load ex = new Load();
        ex.netInit();
        ex.getMatching();
    }

    /**
	 * Calculate RMSE values for characters contained in the testArray
	 * @return An array of double - Each element is the RMSE value for a character
	 */
    public double[] getMatching() {
        double[] myRMSE = new double[numRows];
        double outNet[][];
        outNet = executeRecognition();
        for (int k = 0; k < outNet.length; k++) {
            System.out.println("RMSE[" + k + "] : " + utils.calculateRMSE(outNet[k], target[k]));
            myRMSE[k] = (threshold - utils.calculateRMSE(outNet[k], target[k]));
        }
        return myRMSE;
    }

    /**
	 * Calculate the RMSE value for a character
	 * @param index index of the character in the input list
	 * @return RMSE value for the desired character 
	 */
    public double getRmse(int index) {
        double outNet[][] = executeRecognition();
        return utils.calculateRMSE(outNet[index], target[index]);
    }

    /**
	 * Run the neural net to get the output
	 * @return output values of the net
	 */
    public double[][] executeRecognition() {
        List<double[]> outNetList = new LinkedList<double[]>();
        double[] pattern;
        int i = 0;
        int numPattern = numRows;
        for (i = 0; i < numPattern; i++) {
            pattern = memOut.getNextPattern();
            outNetList.add(pattern);
        }
        double outNet[][] = new double[outNetList.size()][];
        i = 0;
        for (double p[] : outNetList) {
            outNet[i++] = p;
        }
        return outNet;
    }

    /**
	 * Initializing the neural net. Set the input & output array
	 */
    private void netInit() {
        NeuralNetLoader loader = new NeuralNetLoader(netFile);
        nnet = loader.getNeuralNet();
        restoreNeuralNet(netFile);
        threshold = (nnet.getMonitor().getGlobalError() * thresholdConst);
        LinearLayer Linput = (LinearLayer) nnet.getInputLayer();
        Linput.removeAllInputs();
        inputSynapse = new MemoryInputSynapse();
        inputSynapse.setInputArray(testArray);
        inputSynapse.setAdvancedColumnSelector("1-" + testArray[0].length);
        Linput.addInputSynapse(inputSynapse);
        Layer Loutput = nnet.getOutputLayer();
        Loutput.removeAllOutputs();
        memOut = new MemoryOutputSynapse();
        Loutput.addOutputSynapse(memOut);
        Monitor monitor = nnet.getMonitor();
        monitor.setTrainingPatterns(numRows);
        monitor.setTotCicles(1);
        monitor.setLearning(false);
        if (nnet != null) {
            System.out.println(nnet.check());
            nnet.getMonitor().setSingleThreadMode(singleThreadMode);
            nnet.go();
        }
    }

    public NeuralNet restoreNeuralNet(String fileName) {
        try {
            FileInputStream stream = new FileInputStream(fileName);
            ObjectInputStream inp = new ObjectInputStream(stream);
            return (NeuralNet) inp.readObject();
        } catch (Exception excp) {
            try {
                String fn[] = fileName.split("/");
                System.out.println(fn[fn.length - 1]);
                URL url = Load.class.getResource(fileName);
                ObjectInputStream ins = new ObjectInputStream(url.openStream());
                return (NeuralNet) ins.readObject();
            } catch (Exception excp2) {
                excp2.printStackTrace();
                return null;
            }
        }
    }

    @Override
    public void cicleTerminated(NeuralNetEvent arg0) {
    }

    @Override
    public void errorChanged(NeuralNetEvent arg0) {
    }

    @Override
    public void netStarted(NeuralNetEvent e) {
        Monitor mon = (Monitor) e.getSource();
        System.out.print("Network started for ");
        if (mon.isLearning()) System.out.println("training."); else System.out.println("interrogation.");
    }

    @Override
    public void netStopped(NeuralNetEvent e) {
        Monitor mon = (Monitor) e.getSource();
        System.out.println("Network stopped. Last RMSE=" + mon.getGlobalError());
    }

    @Override
    public void netStoppedError(NeuralNetEvent e, String error) {
        System.out.println("Network stopped due the following error: " + error);
    }
}
