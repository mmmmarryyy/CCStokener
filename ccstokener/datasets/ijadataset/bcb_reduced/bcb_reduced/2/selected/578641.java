package uk.ac.imperial.ma.metric.util;

import uk.ac.imperial.ma.metric.gui.MetricFrame;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import uk.ac.imperial.ma.metric.nav.NavigationTreeNode;
import uk.ac.imperial.ma.metric.nav.Document;
import uk.ac.imperial.ma.metric.nav.Exercise;
import uk.ac.imperial.ma.metric.nav.Exploration;
import uk.ac.imperial.ma.metric.nav.Parameter;
import java.io.IOException;
import java.net.MalformedURLException;
import uk.ac.imperial.ma.metric.gui.NotesPanel;
import uk.ac.imperial.ma.metric.gui.WorkingPanel;
import javax.swing.JPanel;
import uk.ac.imperial.ma.metric.exercises.ExerciseInterface;
import uk.ac.imperial.ma.metric.exercises.GraphicalExerciseInterface;
import uk.ac.imperial.ma.metric.gui.FreeformExercisePanel;
import uk.ac.imperial.ma.metric.gui.GraphicalMultipleChoiceExercisePanel;
import uk.ac.imperial.ma.metric.exercises.FreeformQuestionInterface;
import uk.ac.imperial.ma.metric.gui.MCExercisePanel;
import uk.ac.imperial.ma.metric.gui.GraphicalExercisePanel;
import uk.ac.imperial.ma.metric.explorations.ExplorationInterface;
import uk.ac.imperial.ma.metric.explorations.ParameterTakingExplorationInterface;
import java.awt.Cursor;
import javax.swing.text.html.HTMLDocument;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.Parser;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.BadLocationException;
import java.nio.charset.Charset;
import java.util.Vector;

/**
 *
 */
public class Loader implements Task {

    private MetricFrame metricFrame;

    private ClassLoader cl;

    private NavigationTreeNode ntn;

    public static final String STYLE_SHEET = "uk/ac/imperial/ma/metric/html/css/default.css";

    public static final String DOCUMENT_ICON = "uk/ac/imperial/ma/metric/images/icons/document.png";

    public static final String EXERCISE_ICON = "uk/ac/imperial/ma/metric/images/icons/exercise.png";

    public static final String GRAPHICAL_EXERCISE_ICON = "uk/ac/imperial/ma/metric/images/icons/graphicalExercise.png";

    public static final String EXPLORATION_ICON = "uk/ac/imperial/ma/metric/images/icons/exploration.png";

    public static final String INSTRUCTIONS_ICON = "uk/ac/imperial/ma/metric/images/icons/instructions.png";

    public static final String CORRECT_ANSWER_ICON = "uk/ac/imperial/ma/metric/images/icons/correct.png";

    public static final String ANALYSIS_ICON = "uk/ac/imperial/ma/metric/images/icons/analysis.png";

    public static final String FULL_WORKING_ICON = "uk/ac/imperial/ma/metric/images/icons/working.png";

    public static final String HELP_ICON = "uk/ac/imperial/ma/metric/images/icons/help.png";

    public static Icon documentIcon;

    public static Icon exerciseIcon;

    public static Icon graphicalExerciseIcon;

    public static Icon explorationIcon;

    public static Icon instructionsIcon;

    private boolean isAlive;

    private boolean cancel;

    private int value;

    private int min;

    private int max;

    private String name;

    private String status;

    private HTMLEditorKit htmlEditorKit = new HTMLEditorKit();

    private StyleSheet styleSheet;

    private String codebase;

    private Vector cachedResources;

    /**
     * 
     */
    public Loader() {
        cl = getClass().getClassLoader();
        styleSheet = new StyleSheet();
        styleSheet.importStyleSheet(getURL(STYLE_SHEET));
        htmlEditorKit.setStyleSheet(styleSheet);
        documentIcon = getIcon(DOCUMENT_ICON);
        exerciseIcon = getIcon(EXERCISE_ICON);
        graphicalExerciseIcon = getIcon(GRAPHICAL_EXERCISE_ICON);
        explorationIcon = getIcon(EXPLORATION_ICON);
        instructionsIcon = getIcon(INSTRUCTIONS_ICON);
        cachedResources = new Vector();
    }

    public void setMetricFrame(MetricFrame metricFrame) {
        this.metricFrame = metricFrame;
    }

    public URL getURL(String path) {
        return cl.getResource(path);
    }

    public Icon getIcon(String path) {
        return new ImageIcon(getURL(path));
    }

    public void setNavigationTreeNodeToLoad(NavigationTreeNode ntn) {
        this.ntn = ntn;
    }

    public void run() {
        try {
            isAlive = true;
            cancel = false;
            min = 0;
            value = 0;
            this.load(this.ntn);
        } catch (Exception e) {
            metricFrame.metricMain.EXCEPTION_PROCESSOR.process(e);
        } finally {
            isAlive = false;
        }
    }

    public void load(NavigationTreeNode ntn) throws Exception {
        System.out.println("Loading: " + ntn.toString());
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        switch(ntn.getType()) {
            case NavigationTreeNode.DOCUMENT:
                name = "Loading document.";
                max = 4;
                updateProgress("Initalising", 0);
                loadDocument((Document) ntn);
                break;
            case NavigationTreeNode.EXERCISE:
                name = "Loading exercise.";
                max = 7;
                updateProgress("Initalising", 0);
                loadExercise((Exercise) ntn);
                break;
            case NavigationTreeNode.EXPLORATION:
                name = "Loading exploration.";
                Exploration exploration = (Exploration) ntn;
                if (exploration.hasParameters()) {
                    max = 8;
                    updateProgress("Initalising", 0);
                    loadExploration(exploration, exploration.getParameters());
                } else {
                    max = 7;
                    updateProgress("Initalising", 0);
                    loadExploration(exploration);
                }
                break;
            default:
                throw new Exception("Programming error.");
        }
    }

    private void checkCacheStatus(URL url) {
    }

    public void loadDocument(Document document) throws Exception {
        NotesPanel np = metricFrame.getNotesPanel();
        updateProgress("Loading notes file.", 1);
        checkCacheStatus(getJarFileURLFromResourceString(document.getResource().getText()));
        HTMLDocument notes = getHTMLDocument(relativeJarURLStringToURL(document.getResource().getText()));
        updateProgress("Updating display.", 2);
        metricFrame.setNotesWorkingDividerLocation(0);
        updateProgress("Finishing.", 3);
    }

    public void loadExercise(Exercise exercise) throws Exception {
        NotesPanel np = metricFrame.getNotesPanel();
        WorkingPanel wp = metricFrame.getWorkingPanel();
        updateProgress("Loading notes file.", 1);
        checkCacheStatus(getJarFileURLFromResourceString(exercise.getNotes().getDocument().getResource().getText()));
        HTMLDocument notes = getHTMLDocument(relativeJarURLStringToURL(exercise.getNotes().getDocument().getResource().getText()));
        updateProgress("Loading instructions file.", 2);
        checkCacheStatus(getJarFileURLFromResourceString(exercise.getInstructions().getDocument().getResource().getText()));
        HTMLDocument instructions = getHTMLDocument(relativeJarURLStringToURL(exercise.getInstructions().getDocument().getResource().getText()));
        updateProgress("Loading excercise file.", 3);
        checkCacheStatus(getJarFileURLFromResourceString(exercise.getResource().getText()));
        Class exerciseClass = cl.loadClass(removeFileExtension(relativeJarURLStringToString(exercise.getResource().getText())));
        System.out.println("Done.");
        updateProgress("Constructing exercise.", 4);
        ExerciseInterface exerciseInterface = (ExerciseInterface) exerciseClass.newInstance();
        switch(exerciseInterface.getExerciseType()) {
            case ExerciseInterface.FREE_FORM_TYPE_1:
                FreeformExercisePanel ffep = new FreeformExercisePanel(exerciseInterface.getExercise());
                updateProgress("Updating display.", 5);
                wp.addExercise(exercise.getTitle().getText(), exercise.getSynopsis().getText(), ffep);
                break;
            case ExerciseInterface.GRAPHICAL_EXERCISE_TYPE_1:
                GraphicalExerciseInterface graphicalExerciseInterface = (GraphicalExerciseInterface) exerciseInterface;
                GraphicalExercisePanel gep = new GraphicalExercisePanel(graphicalExerciseInterface, np);
                updateProgress("Updating display.", 5);
                wp.addExercise(exercise.getTitle().getText(), exercise.getSynopsis().getText(), gep);
                gep.init();
                break;
            case ExerciseInterface.MULTIPLE_CHOICE_TYPE_1:
                MCExercisePanel mcep = new MCExercisePanel(exerciseInterface.getExercise());
                updateProgress("Updating display.", 5);
                wp.addExercise(exercise.getTitle().getText(), exercise.getSynopsis().getText(), mcep);
                break;
            case ExerciseInterface.MULTIPLE_CHOICE_GRAPHICAL_EXERCISE_TYPE_1:
                GraphicalExerciseInterface graphicalExerciseInterface2 = (GraphicalExerciseInterface) exerciseInterface;
                GraphicalMultipleChoiceExercisePanel gmcep = new GraphicalMultipleChoiceExercisePanel(graphicalExerciseInterface2, np);
                updateProgress("Updating display.", 5);
                wp.addExercise(exercise.getTitle().getText(), exercise.getSynopsis().getText(), gmcep);
                gmcep.init();
                break;
            default:
                throw new Exception("Unkown exercise type.");
        }
        metricFrame.setNotesWorkingDividerLocation();
        updateProgress("Finishing.", 6);
    }

    public void loadExploration(Exploration exploration) throws Exception {
        NotesPanel np = metricFrame.getNotesPanel();
        WorkingPanel wp = metricFrame.getWorkingPanel();
        updateProgress("Loading notes file.", 1);
        checkCacheStatus(getJarFileURLFromResourceString(exploration.getNotes().getDocument().getResource().getText()));
        HTMLDocument notes = getHTMLDocument(relativeJarURLStringToURL(exploration.getNotes().getDocument().getResource().getText()));
        updateProgress("Loading instructions file.", 2);
        checkCacheStatus(getJarFileURLFromResourceString(exploration.getInstructions().getDocument().getResource().getText()));
        HTMLDocument instructions = getHTMLDocument(relativeJarURLStringToURL(exploration.getInstructions().getDocument().getResource().getText()));
        updateProgress("Loading exploration file.", 3);
        checkCacheStatus(getJarFileURLFromResourceString(exploration.getResource().getText()));
        Class explorationClass = cl.loadClass(removeFileExtension(relativeJarURLStringToString(exploration.getResource().getText())));
        updateProgress("Constructing exploration.", 4);
        ExplorationInterface explorationInterface = (ExplorationInterface) explorationClass.newInstance();
        updateProgress("Updating display.", 5);
        wp.addExploration(exploration.getTitle().getText(), exploration.getSynopsis().getText(), explorationInterface.getComponent());
        metricFrame.setNotesWorkingDividerLocation();
        updateProgress("Finishing.", 6);
        explorationInterface.init();
    }

    public void loadExploration(Exploration exploration, Parameter[] parameters) throws Exception {
        NotesPanel np = metricFrame.getNotesPanel();
        WorkingPanel wp = metricFrame.getWorkingPanel();
        updateProgress("Loading notes file.", 1);
        checkCacheStatus(getJarFileURLFromResourceString(exploration.getNotes().getDocument().getResource().getText()));
        HTMLDocument notes = getHTMLDocument(relativeJarURLStringToURL(exploration.getNotes().getDocument().getResource().getText()));
        updateProgress("Loading instructions file.", 2);
        checkCacheStatus(getJarFileURLFromResourceString(exploration.getInstructions().getDocument().getResource().getText()));
        HTMLDocument instructions = getHTMLDocument(relativeJarURLStringToURL(exploration.getInstructions().getDocument().getResource().getText()));
        updateProgress("Loading exploration file.", 3);
        checkCacheStatus(getJarFileURLFromResourceString(exploration.getResource().getText()));
        Class explorationClass = cl.loadClass(removeFileExtension(relativeJarURLStringToString(exploration.getResource().getText())));
        updateProgress("Constructing exploration.", 4);
        ParameterTakingExplorationInterface explorationInterface = (ParameterTakingExplorationInterface) explorationClass.newInstance();
        updateProgress("Setting exploration parameters.", 5);
        explorationInterface.setParameters(parameters);
        updateProgress("Updating display.", 6);
        wp.addExploration(exploration.getTitle().getText(), exploration.getSynopsis().getText(), explorationInterface.getComponent());
        metricFrame.setNotesWorkingDividerLocation();
        updateProgress("Finishing.", 7);
        explorationInterface.init();
    }

    private URL getJarFileURLFromResourceString(String strResourceString) {
        try {
            System.out.println("In getJarFileURLFromResourceString.");
            String jarFileName = strResourceString.substring(0, strResourceString.indexOf("!"));
            System.out.println("jarFileName=" + jarFileName);
            URL url = new URL(codebase + jarFileName);
            System.out.println("url=" + url);
            return url;
        } catch (MalformedURLException murle) {
            murle.printStackTrace();
            return null;
        }
    }

    private String relativeJarURLStringToString(String strResourceString) {
        return strResourceString.substring(strResourceString.indexOf("!") + 2);
    }

    private String removeFileExtension(String strInput) {
        String str = strInput.substring(0, strInput.indexOf("."));
        return str.replace('/', '.');
    }

    public URL relativeJarURLStringToURL(String strResourceString) throws MalformedURLException {
        return getURL(relativeJarURLStringToString(strResourceString));
    }

    public HTMLDocument getHTMLDocument(URL url) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8")));
        HTMLDocument htmlDocument = new HTMLDocument(styleSheet);
        try {
            htmlEditorKit.read(bufferedReader, htmlDocument, 0);
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        } finally {
            bufferedReader.close();
        }
        htmlDocument.setBase(url);
        return htmlDocument;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public boolean getCanceled() {
        return cancel;
    }

    public void cancel() {
        cancel = true;
    }

    public int getValue() {
        return value;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    private void updateProgress(String newStatus, int newValue) throws Exception {
        status = newStatus;
        value = newValue;
        if (cancel) {
            throw new Exception("Canceled task.");
        }
        Thread.yield();
    }

    public HTMLEditorKit getHTMLEditorKit() {
        return htmlEditorKit;
    }
}
