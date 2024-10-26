package cz.razor.dzemuj.datamodels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import cz.razor.dzemuj.scatterplot.ProjectionChooser;

/**
 * Model of the exam, contains particular Questions
 * 
 * @author zdenek.kedaj@gmail.com
 * @version 20.5. 2008
 */
public class ExamModel implements Iterable<Question> {

    private static ExamModel instance = null;

    private ArrayList<Question> questions;

    private HashMap<String, Question> questionsMap;

    public static final String[] columnNames = { "Hash otázky", "Špatně", "Bez odpovědi", "Správně", "Průměrný bodový zisk", "Max. bodů", "Min. bodů", "Body za nezodpovězení" };

    public static final int ID = 0;

    public static final int WRONG = 1;

    public static final int NONE = 2;

    public static final int RIGHT = 3;

    public static final int AVERAGE = 4;

    public static final int MAXPTS = 5;

    public static final int MINPTS = 6;

    public static final int NULLPTS = 7;

    private static final int READING_HASH = 1;

    private static final int READING_QUESTION = 2;

    private String commentChar = "#";

    private static final Comparator<Question> COMPARATOR_WRONG = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            int compareWrong = b.getWrong().compareTo(a.getWrong());
            if (compareWrong != 0) return compareWrong;
            if (a.getRowNumber() < b.getRowNumber()) return -1;
            if (a.getRowNumber() == b.getRowNumber()) return 0; else return 1;
        }
    };

    private static final Comparator<Question> COMPARATOR_NONE = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            int compareNone = b.getNone().compareTo(a.getNone());
            if (compareNone != 0) return compareNone;
            if (a.getRowNumber() < b.getRowNumber()) return -1;
            if (a.getRowNumber() == b.getRowNumber()) return 0; else return 1;
        }
    };

    private static final Comparator<Question> COMPARATOR_RIGHT = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            int compareRight = b.getRight().compareTo(a.getRight());
            if (compareRight != 0) return compareRight;
            if (a.getRowNumber() < b.getRowNumber()) return -1;
            if (a.getRowNumber() == b.getRowNumber()) return 0; else return 1;
        }
    };

    @SuppressWarnings("unused")
    private static final Comparator<Question> COMPARATOR_ID = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            if (a.getRowNumber() < b.getRowNumber()) return -1;
            if (a.getRowNumber() == b.getRowNumber()) return 0; else return 1;
        }
    };

    private static final Comparator<Question> COMPARATOR_HEURISTICS_1 = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            int coeficientA = Math.max(a.getWrong(), a.getNone());
            int coeficientB = Math.max(b.getWrong(), b.getNone());
            if (coeficientA < coeficientB) return -1;
            if (coeficientA == coeficientB) return 0; else return 1;
        }
    };

    private static final Comparator<Question> COMPARATOR_AVERAGE = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            int compareResult = b.getAverage().compareTo(a.getAverage());
            if (compareResult != 0) return compareResult;
            if (a.getRowNumber() < b.getRowNumber()) return -1;
            if (a.getRowNumber() == b.getRowNumber()) return 0; else return 1;
        }
    };

    private static final Comparator<Question> COMPARATOR_MAXPTS = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            int compareResult = b.getPointsPerRight().compareTo(a.getPointsPerRight());
            if (compareResult != 0) return compareResult;
            if (a.getRowNumber() < b.getRowNumber()) return -1;
            if (a.getRowNumber() == b.getRowNumber()) return 0; else return 1;
        }
    };

    private static final Comparator<Question> COMPARATOR_MINPTS = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            int compareResult = b.getPointsPerWrong().compareTo(a.getPointsPerWrong());
            if (compareResult != 0) return compareResult;
            if (a.getRowNumber() < b.getRowNumber()) return -1;
            if (a.getRowNumber() == b.getRowNumber()) return 0; else return 1;
        }
    };

    private static final Comparator<Question> COMPARATOR_NULLPTS = new Comparator<Question>() {

        public int compare(Question a, Question b) {
            int compareResult = b.getPointsPerNone().compareTo(a.getPointsPerNone());
            if (compareResult != 0) return compareResult;
            if (a.getRowNumber() < b.getRowNumber()) return -1;
            if (a.getRowNumber() == b.getRowNumber()) return 0; else return 1;
        }
    };

    /**
	 * constructor loads data from file
	 * 
	 * @param filename
	 * @throws IOException
	 * @throws DataCoherencyException
	 * @throws DataFormatException
	 */
    private ExamModel(URL urlQuestions) throws IOException, DataCoherencyException {
        BufferedReader in = new BufferedReader(new InputStreamReader(urlQuestions.openStream()));
        String line;
        questions = new ArrayList<Question>();
        questionsMap = new HashMap<String, Question>();
        in = new BufferedReader(new InputStreamReader(urlQuestions.openStream(), "UTF-8"));
        int questionNumber = 0;
        Question question;
        String questText = "";
        String hash = "";
        int lookingFor = ExamModel.READING_HASH;
        while ((line = in.readLine()) != null) {
            switch(lookingFor) {
                case ExamModel.READING_HASH:
                    if (line.length() == 0 || line.trim().length() == 0) continue;
                    hash = line;
                    questionNumber++;
                    lookingFor = ExamModel.READING_QUESTION;
                    break;
                case ExamModel.READING_QUESTION:
                    if (line.equals("--")) {
                        question = new Question(questionNumber, hash, questText);
                        questions.add(question);
                        questionsMap.put(question.getHash(), question);
                        questText = "";
                        hash = null;
                        lookingFor = ExamModel.READING_HASH;
                    } else {
                        questText = questText.concat(line + Constants.nl);
                    }
                    break;
                default:
                    throw new DataCoherencyException("Neočekávaný konec souboru!");
            }
        }
        questions.trimToSize();
        in.close();
    }

    /**
	 * @param urlQuestions
	 * @param urlCetnosti
	 * @param urlScore
	 * @throws IOException
	 * @throws DataCoherencyException
	 */
    private ExamModel(URL urlQuestions, URL urlCetnosti, URL urlScore) throws IOException, DataCoherencyException {
        this(urlQuestions);
        CountModel cm = new CountModel(urlCetnosti);
        ScoreModel sm = new ScoreModel(urlScore);
        this.setCetnostiData(cm.getMap());
        this.setScoreData(sm.getMap());
    }

    public static ExamModel getInstance(URL urlQuestions, URL urlCetnosti, URL urlScore) throws IOException, DataCoherencyException {
        if (instance == null) {
            instance = new ExamModel(urlQuestions, urlCetnosti, urlScore);
        }
        return instance;
    }

    public static void destroyInstance() {
        instance = null;
    }

    public static ExamModel getInstance() {
        return instance;
    }

    /**
	 * takes model and adds its data to this data structure. Must be done before any
	 * sorting! relies on order of elements
	 * 
	 * @param model
	 * @throws DataCoherencyException
	 */
    public void setCetnostiData(Map<String, CountModelItem> map) throws DataCoherencyException {
        ArrayList<Integer> indicesToRemove = new ArrayList<Integer>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (map.containsKey(q.getHash())) {
                CountModelItem item = map.get(q.getHash());
                q.setWrong(item.getWrong());
                q.setNone(item.getNone());
                q.setRight(item.getRight());
            } else {
                indicesToRemove.add(i);
            }
        }
        removeIndices(indicesToRemove);
    }

    /**
	 * takes model and adds its data to this data structure. Must be done before any
	 * sorting! relies on order of elements
	 * 
	 * @param model
	 * @throws DataCoherencyException
	 */
    public void setScoreData(Map<String, ScoreModelItem> map) throws DataCoherencyException {
        ArrayList<Integer> indicesToRemove = new ArrayList<Integer>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (map.containsKey(q.getHash())) {
                ScoreModelItem item = map.get(q.getHash());
                q.setPointsPerWrong(item.getPointsPerWrong());
                q.setPointsPerNone(item.getPointsPerNone());
                q.setPointsPerRight(item.getPointsPerRight());
                q.setAverage(item.getAverage());
            } else {
                indicesToRemove.add(i);
            }
        }
        removeIndices(indicesToRemove);
    }

    private void removeIndices(List<Integer> arr) {
        int idx;
        for (int i = arr.size() - 1; i >= 0; i--) {
            idx = arr.get(i);
            questions.remove(idx);
        }
    }

    public Question getQuestion(int index) {
        return questions.get(index);
    }

    /**
	 * 
	 * @param hash
	 * @return raw question text exactly as it is in input files
	 */
    public Question getQuestion(String hash) {
        return questionsMap.get(hash);
    }

    public String getQuestionDetails(int index) {
        return getQuestion(index).getQuestionDetailsText();
    }

    public String getQuestionDetails(String hash) {
        return getQuestion(hash).getQuestionDetailsText();
    }

    public String getQuestionFullReport(int index) {
        return "" + getQuestion(index) + getQuestion(index).getQuestionDetailsText();
    }

    public String getQuestionFullReport(String hash) {
        return "" + getQuestion(hash) + getQuestion(hash).getQuestionDetailsText();
    }

    /**
	 * constructs XYSeriesCollection for Cetnosti applet
	 * 
	 * @return XYSeriesCollection
	 */
    public XYSeriesCollection getCetnostiDataSet() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(this.getXYSeries(ExamModel.WRONG));
        dataset.addSeries(this.getXYSeries(ExamModel.RIGHT));
        dataset.addSeries(this.getXYSeries(ExamModel.NONE));
        return dataset;
    }

    /**
	 * constructs XYSeriesCollection for Score applet
	 * 
	 * @return XYSeriesCollection
	 */
    public XYSeriesCollection getScoreDataSet() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(this.getXYSeries(ExamModel.AVERAGE));
        dataset.addSeries(this.getXYSeries(ExamModel.MAXPTS));
        dataset.addSeries(this.getXYSeries(ExamModel.MINPTS));
        dataset.addSeries(this.getXYSeries(ExamModel.NULLPTS));
        return dataset;
    }

    /**
	 * returns series for chart
	 * 
	 * @param int serieNumber
	 * @return XYSeries
	 */
    private XYSeries getXYSeries(int serieNumber) {
        XYSeries series = new XYSeries(columnNames[serieNumber]);
        int x = 0;
        for (Question o : questions) {
            x++;
            if (serieNumber == ExamModel.WRONG) series.add(x, o.getWrong());
            if (serieNumber == ExamModel.NONE) series.add(x, o.getNone());
            if (serieNumber == ExamModel.RIGHT) series.add(x, o.getRight());
            if (serieNumber == ExamModel.AVERAGE) series.add(x, o.getAverage());
            if (serieNumber == ExamModel.MAXPTS) series.add(x, o.getPointsPerRight());
            if (serieNumber == ExamModel.MINPTS) series.add(x, o.getPointsPerWrong());
            if (serieNumber == ExamModel.NULLPTS) series.add(x, o.getPointsPerNone());
        }
        return series;
    }

    /**
	 * Sorts data according to the given serieNumber, using proper COMPARATOR class
	 * 
	 * @param serieNumber
	 */
    public void sort(final int serieNumber) {
        switch(serieNumber) {
            case ExamModel.WRONG:
                Collections.sort(questions, COMPARATOR_WRONG);
                break;
            case ExamModel.NONE:
                Collections.sort(questions, COMPARATOR_NONE);
                break;
            case ExamModel.RIGHT:
                Collections.sort(questions, COMPARATOR_RIGHT);
                break;
            case ExamModel.AVERAGE:
                Collections.sort(questions, COMPARATOR_AVERAGE);
                break;
            case ExamModel.MAXPTS:
                Collections.sort(questions, COMPARATOR_MAXPTS);
                break;
            case ExamModel.MINPTS:
                Collections.sort(questions, COMPARATOR_MINPTS);
                break;
            case ExamModel.NULLPTS:
                Collections.sort(questions, COMPARATOR_NULLPTS);
                break;
            default:
                Collections.sort(questions, COMPARATOR_HEURISTICS_1);
                break;
        }
    }

    /**
	 * automatically selects "best" way of sorting
	 */
    public void sortImplicitly(int tabbedPaneSelectedIndex) {
        ArrayList<Integer> arrayOfWhatToChooseFrom = new ArrayList<Integer>();
        switch(tabbedPaneSelectedIndex) {
            case 1:
                arrayOfWhatToChooseFrom.add(Integer.valueOf(ExamModel.MAXPTS));
                arrayOfWhatToChooseFrom.add(Integer.valueOf(ExamModel.MINPTS));
                arrayOfWhatToChooseFrom.add(Integer.valueOf(ExamModel.NULLPTS));
                arrayOfWhatToChooseFrom.add(Integer.valueOf(ExamModel.AVERAGE));
                break;
            case -1:
            case 0:
            default:
                arrayOfWhatToChooseFrom.add(Integer.valueOf(ExamModel.NONE));
                arrayOfWhatToChooseFrom.add(Integer.valueOf(ExamModel.RIGHT));
                arrayOfWhatToChooseFrom.add(Integer.valueOf(ExamModel.WRONG));
                break;
        }
        sort(ProjectionChooser.chooseSortMethod(arrayOfWhatToChooseFrom));
    }

    public Iterator<Question> iterator() {
        return questions.iterator();
    }

    public String getCommentChar() {
        return commentChar;
    }

    public void setCommentChar(String commentCharField) {
        this.commentChar = commentCharField;
    }
}
