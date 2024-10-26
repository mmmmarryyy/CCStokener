package PackageJInO;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.StringTokenizer;
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

public class LoadClassifier implements NeuralNetListener {

    private NeuralNet nnet = null;

    LinearLayer input, output;

    SigmoidLayer hidden;

    boolean singleThreadMode = true;

    private String netFile = "mn.snet";

    private double[][] testArray;

    private MemoryInputSynapse inputSynapse;

    MemoryOutputSynapse memOut;

    double[] pattern;

    public LoadClassifier() {
        netInit();
    }

    /**
	 * @param netFile Path of the classifier neural net
	 * @param testArray Input array to test
	 */
    public LoadClassifier(String netFile, double[][] testArray) {
        this.netFile = netFile;
        this.testArray = testArray;
        netInit();
    }

    public static void main(String[] args) {
        LoadClassifier neurnet = new LoadClassifier();
        double res = neurnet.interrogate();
        System.out.println("Result: " + res);
    }

    /**
	 * Test the net to get the response. Only for debug
	 * @return
	 */
    public double interrogate() {
        pattern = memOut.getNextPattern();
        return pattern[0];
    }

    /**
	 * Initializing the neural net. Set the input & output array
	 */
    private void netInit() {
        NeuralNetLoader loader = new NeuralNetLoader(netFile);
        nnet = loader.getNeuralNet();
        restoreNeuralNet(netFile);
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
        monitor.setTrainingPatterns(1);
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
                URL url = LoadClassifier.class.getResource(fileName);
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
