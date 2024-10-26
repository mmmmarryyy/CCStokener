package cz.razor.dzemuj.itemsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import javax.swing.SwingWorker;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.impl.SparseObjectMatrix2D;

public class LoadDataWorker extends SwingWorker<Integer, Integer> {

    private URL url;

    private HashMap<AnsweredQuestion, Integer> combinationMap;

    private HashMap<Integer, AnsweredQuestion> combinationMapReverse;

    private SparseObjectMatrix2D matrix;

    private int[] supportVector;

    private ItemSetsApplet applet;

    public LoadDataWorker(ItemSetsApplet applet, URL url, HashMap<AnsweredQuestion, Integer> combinationMap, HashMap<Integer, AnsweredQuestion> combinationMapReverse, SparseObjectMatrix2D matrix, int[] supportVector) {
        super();
        this.url = url;
        this.combinationMap = combinationMap;
        this.combinationMapReverse = combinationMapReverse;
        this.matrix = matrix;
        this.supportVector = supportVector;
        this.applet = applet;
    }

    @Override
    protected Integer doInBackground() throws Exception {
        int numOfRows = 0;
        combinationMap = new HashMap<AnsweredQuestion, Integer>();
        combinationMapReverse = new HashMap<Integer, AnsweredQuestion>();
        LinkedHashSet<AnsweredQuestion> answeredQuestionSet = new LinkedHashSet<AnsweredQuestion>();
        LinkedHashSet<Integer> studentSet = new LinkedHashSet<Integer>();
        final String delimiter = ";";
        final String typeToProcess = "F";
        String line;
        String[] chunks = new String[9];
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "ISO-8859-2"));
            in.readLine();
            while ((line = in.readLine()) != null) {
                chunks = line.split(delimiter);
                numOfRows++;
                if (chunks[2].equals(typeToProcess)) {
                    answeredQuestionSet.add(new AnsweredQuestion(chunks[4], chunks[5]));
                    studentSet.add(new Integer(chunks[0]));
                }
            }
            in.close();
            int i = 0;
            Integer I;
            for (AnsweredQuestion pair : answeredQuestionSet) {
                I = new Integer(i++);
                combinationMap.put(pair, I);
                combinationMapReverse.put(I, pair);
            }
            matrix = new SparseObjectMatrix2D(answeredQuestionSet.size(), studentSet.size());
            int lastStudentNumber = -1;
            AnsweredQuestion pair;
            in = new BufferedReader(new InputStreamReader(url.openStream(), "ISO-8859-2"));
            in.readLine();
            while ((line = in.readLine()) != null) {
                chunks = line.split(delimiter);
                pair = null;
                if (chunks[2].equals(typeToProcess)) {
                    if (Integer.parseInt(chunks[0]) != lastStudentNumber) {
                        lastStudentNumber++;
                    }
                    pair = new AnsweredQuestion(chunks[4], chunks[5]);
                    if (combinationMap.containsKey(pair)) {
                        matrix.setQuick(combinationMap.get(pair), lastStudentNumber, Boolean.TRUE);
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        supportVector = new int[combinationMap.size()];
        ObjectMatrix1D row = null;
        for (int i = 0; i < combinationMap.size(); i++) {
            row = matrix.viewRow(i);
            int sum = 0;
            for (int k = 0; k < row.size(); k++) {
                if (row.getQuick(k) != null && row.getQuick(k).equals(Boolean.TRUE)) {
                    sum++;
                }
            }
            supportVector[i] = sum;
        }
        applet.combinationMap = this.combinationMap;
        applet.combinationMapReverse = this.combinationMapReverse;
        applet.matrix = this.matrix;
        applet.supportVector = supportVector;
        System.out.println("data loaded.");
        return null;
    }

    @Override
    protected void done() {
        applet.switchGUI(ItemSetsApplet.GUI_MODE_COMPUTING);
    }
}
