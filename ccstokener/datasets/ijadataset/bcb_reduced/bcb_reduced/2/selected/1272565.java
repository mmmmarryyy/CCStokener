package cz.razor.dzemuj.clustering;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JApplet;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.set.SimpleExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.NumericalAttribute;
import com.rapidminer.example.table.PolynominalAttribute;
import com.rapidminer.example.table.SimpleArrayData;
import com.rapidminer.example.table.SimpleArrayDataRowReader;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.clustering.ClusterModel;
import com.rapidminer.operator.learner.clustering.SimpleHierarchicalClusterModel;
import com.rapidminer.operator.learner.clustering.hierarchical.AgglomerativeHierarchicalClusterer;
import com.rapidminer.tools.ObjectVisualizerService;
import cz.razor.dzemuj.Texts;
import cz.razor.dzemuj.datamodels.Constants;
import cz.razor.dzemuj.datamodels.DataCoherencyException;
import cz.razor.dzemuj.datamodels.ExamModel;
import cz.razor.dzemuj.gui.CommonMenuElementsFactory;

public class HierarchicalClusteringApplet extends JApplet {

    private static Logger logger = Logger.getAnonymousLogger();

    private JPanel contentPane;

    private String errors;

    private JPanel clusterGraphPanel;

    private ClusterAppletModelVisualization clusterVisualiserPanel;

    private ExamModel examModel;

    private AgglomerativeHierarchicalClusterer clusterer;

    public void init() {
        Boolean inputError = new Boolean(Boolean.FALSE);
        final String language = getParameter("language");
        if (language == null) {
            logger.log(Level.INFO, Texts.getString("PARAM_missing") + "language");
            inputError = Boolean.TRUE;
        }
        Texts.init(new Locale(language));
        if (contentPane == null) {
            try {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        contentPane = new JPanel(new BorderLayout());
                        setContentPane(contentPane);
                    }
                });
            } catch (Exception e) {
                System.err.println(Texts.getString("GUI_contentPane_init_error"));
                e.printStackTrace();
            }
        }
        final String fileQuestions = getParameter("fileQuestions");
        if (fileQuestions == null) {
            logger.log(Level.INFO, Texts.getString("PARAM_missing") + "fileQuestions");
            inputError = Boolean.TRUE;
        }
        final String fileAnswersCount = getParameter("fileAnswersCount");
        if (fileAnswersCount == null) {
            logger.log(Level.INFO, Texts.getString("PARAM_missing") + "fileAnswersCount");
            inputError = Boolean.TRUE;
        }
        final String fileMaxMinPts = getParameter("fileMaxMinPts");
        if (fileQuestions == null) {
            logger.log(Level.INFO, Texts.getString("PARAM_missing") + "fileMaxMinPts");
            inputError = Boolean.TRUE;
        }
        try {
            URL url = new URL(this.getCodeBase(), fileAnswersCount);
            ExampleSet es = loadData(url);
            clusterer = getLearner();
            ClusterModel model = clusterer.createClusterModel(es);
            Component textComponent = new JTextArea(model.toString());
            clusterVisualiserPanel = new ClusterAppletModelVisualization((SimpleHierarchicalClusterModel) model, textComponent);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (OperatorException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            examModel = ExamModel.getInstance(new URL(this.getCodeBase(), fileQuestions), new URL(this.getCodeBase(), fileAnswersCount), new URL(this.getCodeBase(), fileMaxMinPts));
        } catch (MalformedURLException e) {
            inputError = Boolean.TRUE;
            errors = errors + Texts.getString("FILE_inputNotFound");
            JOptionPane.showMessageDialog(contentPane, Texts.getString("FILE_inputNotFound"), Texts.getString("ERROR_inputError"), JOptionPane.ERROR_MESSAGE);
            logger.log(Level.INFO, Texts.getString("LOG_MalformedURLException"), e);
        } catch (FileNotFoundException e) {
            inputError = Boolean.TRUE;
            errors = errors + Texts.getString("FILE_inputNotFound");
            JOptionPane.showMessageDialog(contentPane, Texts.getString("FILE_inputNotFound"), Texts.getString("ERROR_inputError"), JOptionPane.ERROR_MESSAGE);
            logger.log(Level.INFO, Texts.getString("LOG_FileNotFoundException"), e);
        } catch (IOException e) {
            inputError = Boolean.TRUE;
            errors = errors + Texts.getString("FILE_inputNotFound");
            JOptionPane.showMessageDialog(contentPane, Texts.getString("FILE_inputNotFound"), Texts.getString("ERROR_inputError"), JOptionPane.ERROR_MESSAGE);
            logger.log(Level.INFO, Texts.getString("LOG_IOException"), e);
        } catch (DataCoherencyException e) {
            logger.log(Level.INFO, e.getMessage(), e);
        }
        ObjectVisualizerService.addObjectVisualizer(new QuestionVisualizer(contentPane));
        try {
            if (!inputError) {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        createGUI();
                    }
                });
            } else {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        createErrorGUI();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println(Texts.getString("GUI_createError"));
            e.printStackTrace();
        }
    }

    private ExampleSet loadData(URL url) {
        List<Attribute> attibuteList = new ArrayList<Attribute>();
        ExampleTable table = null;
        ExampleSet es = null;
        SimpleAttributes attributes = new SimpleAttributes();
        Attribute idAtt = new PolynominalAttribute("Hash");
        attributes.addRegular(idAtt);
        attributes.addRegular(new NumericalAttribute("NOK"));
        attributes.addRegular(new NumericalAttribute("NULL"));
        attributes.addRegular(new NumericalAttribute("OK"));
        attributes.addRegular(new NumericalAttribute("průměr"));
        attributes.setId(idAtt);
        attibuteList.add(attributes.getId());
        for (Attribute attribute : attributes) {
            attibuteList.add(attribute);
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            String line;
            line = in.readLine();
            Attribute[] attributeArray = new Attribute[4];
            attributeArray = attibuteList.toArray(attributeArray);
            DataRowFactory dataRowFactory = new DataRowFactory(DataRowFactory.TYPE_SHORT_ARRAY, '.');
            Iterator<SimpleArrayData> simpleArrayDataIterator = new MemorySimpleArrayIterator(in).iterator();
            SimpleArrayDataRowReader dataRowReader = new SimpleArrayDataRowReader(dataRowFactory, attributeArray, simpleArrayDataIterator);
            table = new MemoryExampleTable(attibuteList, dataRowReader);
            in.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<Attribute, String> specialAttributes = new HashMap<Attribute, String>();
        specialAttributes.put(attributes.getId(), "id");
        List<Attribute> regularAttributes = new ArrayList<Attribute>();
        for (Attribute attribute : attributes) {
            regularAttributes.add(attribute);
        }
        es = new SimpleExampleSet(table, regularAttributes, specialAttributes);
        return es;
    }

    private void createGUI() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(CommonMenuElementsFactory.getSettingMenu(contentPane));
        this.setJMenuBar(menuBar);
        contentPane.add(clusterVisualiserPanel, BorderLayout.CENTER);
        contentPane.updateUI();
    }

    /**
	 * Creates the learner used to create the learner. Sets its initial parameters
	 * 
	 * @return initialized learner
	 * @throws ClassNotFoundException
	 */
    private AgglomerativeHierarchicalClusterer getLearner() throws ClassNotFoundException {
        OperatorDescription operatorDescription = new OperatorDescription("AgglomerativeHierarchicalClusterer", "com.rapidminer.operator.learner.clustering.hierarchical.AgglomerativeHierarchicalClusterer", "desc", null, null, null);
        AgglomerativeHierarchicalClusterer l = new AgglomerativeHierarchicalClusterer(operatorDescription);
        l.setParameter("measure", "EuclideanDistance");
        l.setParameter("mode", "SingleLink");
        l.setParameter("min_items", "2");
        return l;
    }

    private void createErrorGUI() {
        JTextPane text = new JTextPane();
        text.setText(errors);
        text.setEditable(Boolean.FALSE);
        JScrollPane sp = new JScrollPane(text);
        contentPane.add(sp, BorderLayout.CENTER);
    }
}
