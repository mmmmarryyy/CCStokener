package net.sf.zorobot.theme;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;
import net.sf.zorobot.core.Question;
import net.sf.zorobot.core.QuestionCollection;
import net.sf.zorobot.core.Theme;
import net.sf.zorobot.core.ZorobotSystem;
import net.sf.zorobot.ext.db.GameDAO;
import net.sf.zorobot.ext.db.GameDAOFactory;
import net.sf.zorobot.ext.db.KanjiDAO;
import net.sf.zorobot.ext.db.KanjiDAOFactory;
import net.sf.zorobot.ext.db.ProfileDAO;
import net.sf.zorobot.ext.db.ProfileDAOFactory;
import net.sf.zorobot.game.QuestionModifier;
import net.sf.zorobot.util.JapaneseString;
import net.sf.zorobot.util.StringUtility;

public class XmlTheme extends Theme {

    public int mode;

    static KanjiDAO kanjiDAO = null;

    static ProfileDAO profileDAO = null;

    static GameDAO gameDAO = null;

    static {
        try {
            kanjiDAO = KanjiDAOFactory.getDAO();
        } catch (Exception e) {
        }
        try {
            profileDAO = ProfileDAOFactory.getDAO();
        } catch (Exception e) {
        }
        try {
            gameDAO = GameDAOFactory.getDAO();
        } catch (Exception e) {
        }
    }

    public XmlTheme(String name, String description, int mode) {
        super(name, description, true);
        this.mode = mode;
        this.addOption("url", "Url of the xml file");
        this.addOption("gameXpath", "xpath of the game");
        this.addOption("qXpath", "xpath of the question relative to the gameXpath, multiple xpath are separated by comma");
        this.addOption("aXpath", "xpath of the answer relative to the gameXpath");
        this.addOption("eXpath", "xpath of the extra relative to the gameXpath, multiple xpath are separated by comma");
        this.addOption("ns", "namespace of the xml");
        this.addOption("preprocess", "flag (yes/no) indicating that the document may not be well formed xml and have to be preprocessed");
        this.addOption("encoding", "encoding used by the page, e.g. UTF8, SJIS, etc. Refers to java encoding names.");
        this.addOption("showelement", "Show element name from the xml");
        this.addOption("question", "Question");
        this.addOption("answer", "Answer");
        this.addOption("qModifier", "Question modifier");
        this.addOption("aModifier", "Answer modifier");
        this.addOption("limit", "Limit");
        this.addOption("delimiter", "Regular expression for answer delimiter");
    }

    public boolean verify() {
        return true;
    }

    @Override
    public String getQuestions(String fullName, QuestionCollection qSet, HashMap<String, String> options, String nick) {
        if (qSet.size() < 10000) {
            String errMsg = null;
            ArrayList<Question> qArr = null;
            try {
                qArr = readEdict(options, nick);
                QuestionModifier.modify(qArr, options.get("qmodifier"), options.get("amodifier"));
                qSet.addQuestion(qArr);
            } catch (Exception e) {
                errMsg = e.getMessage();
            }
            return errMsg;
        }
        return "";
    }

    public String get(String s, String encoding) throws Exception {
        if (!s.startsWith("http")) return "";
        StringBuilder sb = new StringBuilder();
        try {
            String result = null;
            URL url = new URL(s);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
            connection.setDoOutput(false);
            if (encoding == null) encoding = "UTF-8";
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
            String inputLine;
            String contentType = connection.getContentType();
            if (contentType.startsWith("text") || contentType.startsWith("application/xml")) {
                while ((inputLine = in.readLine()) != null) {
                    sb.append(inputLine);
                    sb.append("\n");
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return sb.toString();
    }

    private ArrayList<Question> readEdict(HashMap<String, String> options, String nick) throws Exception {
        String filename = "";
        int theType = 0;
        ArrayList<Question> result = new ArrayList<Question>();
        String url = options.get("url");
        String ns = options.get("ns");
        String limit = options.get("limit");
        String type = options.get("type");
        String gameXpath = options.get("gamexpath");
        String qXpathFull = options.get("qxpath");
        String aXpath = options.get("axpath");
        String eXpathFull = options.get("expath");
        String showElem = options.get("showelement");
        String delimiter = options.get("delimiter");
        String encoding = options.get("encoding");
        boolean preprocess = "yes".equalsIgnoreCase(options.get("preprocess"));
        boolean bShowElem = "yes".equalsIgnoreCase(showElem);
        String qXpath[] = null;
        String eXpath[] = null;
        ArrayList<String[]> typeCheck = new ArrayList<String[]>();
        int lolimit = 0, hilimit = 1000000;
        try {
            if (gameXpath == null) gameXpath = "/trivia/quiz";
            if (qXpathFull == null) qXpathFull = "./item[0]/p";
            if (aXpath == null) aXpath = "./item[1]/p";
            if (eXpathFull == null) eXpathFull = "";
            if (limit == null) limit = "";
            qXpath = qXpathFull.split(",");
            eXpath = eXpathFull.split(",");
            for (int jj = 0; jj < qXpath.length; jj++) {
                if (!qXpath[jj].startsWith(".")) qXpath[jj] = "." + qXpath[jj];
            }
            for (int jj = 0; jj < eXpath.length; jj++) {
                if (!eXpath[jj].startsWith(".")) eXpath[jj] = "." + eXpath[jj];
            }
            if (!aXpath.startsWith(".")) aXpath = "." + aXpath;
            String showKanji = options.get("showkanji");
            boolean showK = !(showKanji != null && showKanji.equalsIgnoreCase("no"));
            String extra = options.get("extra");
            String extraArr[] = new String[0];
            if (extra != null) {
                extraArr = extra.split(",");
            }
            Pattern kanjiPat = null;
            int dash = limit.indexOf("-");
            if (dash < 0) {
                dash = limit.indexOf("..");
                if (dash < 0) {
                    if (limit.length() > 0) hilimit = StringUtility.toInt(limit);
                } else {
                    String f = limit.substring(0, dash);
                    String b = limit.substring(dash + 2);
                    lolimit = StringUtility.toInt(f);
                    hilimit = StringUtility.toInt(b);
                }
            } else {
                String f = limit.substring(0, dash);
                String b = limit.substring(dash + 1);
                lolimit = StringUtility.toInt(f);
                hilimit = StringUtility.toInt(b);
            }
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        boolean bShowReading;
        Hashtable combiner = new Hashtable();
        int bpos;
        String quest = (String) options.get("question");
        String answe = (String) options.get("answer");
        Pattern qPattern = null;
        Pattern aPattern = null;
        if (quest != null) {
            quest = StringUtility.processRegex(quest);
            qPattern = Pattern.compile(quest);
        }
        if (answe != null) {
            answe = StringUtility.processRegex(answe);
            aPattern = Pattern.compile(answe);
        }
        try {
            if (url.length() > 100) throw new Exception("The url is too long. Please shorten the url.");
            if (!url.startsWith("http://")) throw new Exception("Invalid url. Url must be an xml file, and use http protocol.");
            String ss = get(url, encoding);
            if (preprocess) {
                Tidy tidy = new Tidy();
                ByteArrayInputStream bais = new ByteArrayInputStream(ss.getBytes("UTF8"));
                tidy.setCharEncoding(Configuration.UTF8);
                tidy.setXHTML(true);
                tidy.setXmlOut(true);
                tidy.setXmlTags(true);
                org.w3c.dom.Document doc = tidy.parseDOM(bais, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                tidy.pprint(doc, baos);
                ss = baos.toString("UTF8");
            }
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(ss));
            XPath gameXPath = XPath.newInstance(gameXpath);
            if (ns != null) {
                gameXPath.addNamespace("ns", ns);
            }
            XPath qXPath[] = new XPath[qXpath.length];
            for (int ii = 0; ii < qXpath.length; ii++) {
                qXPath[ii] = XPath.newInstance(qXpath[ii]);
                if (ns != null) {
                    qXPath[ii].addNamespace("ns", ns);
                }
            }
            XPath eXPath[] = new XPath[eXpath.length];
            for (int ii = 0; ii < eXpath.length; ii++) {
                eXPath[ii] = XPath.newInstance(eXpath[ii]);
                if (ns != null) {
                    eXPath[ii].addNamespace("ns", ns);
                }
            }
            XPath aXPath = XPath.newInstance(aXpath);
            if (ns != null) {
                aXPath.addNamespace("ns", ns);
            }
            List nodes = gameXPath.selectNodes(doc.getRootElement());
            String q;
            String[] mea;
            ArrayList typ;
            int i = 0;
            int numQ = 0;
            int mCount = 0;
            Iterator iter = nodes.iterator();
            while (iter.hasNext()) {
                Object ctx = iter.next();
                StringBuilder qSb = new StringBuilder();
                for (int j = 0; j < qXPath.length; j++) {
                    List questions = qXPath[j].selectNodes(ctx);
                    Iterator qIter = questions.iterator();
                    while (qIter.hasNext()) {
                        Object question = qIter.next();
                        if (question instanceof Element) {
                            Element questionElem = (Element) question;
                            if (qSb.length() > 0) qSb.append(";  ");
                            if (bShowElem) {
                                qSb.append(questionElem.getName());
                                qSb.append(": ");
                            }
                            qSb.append(questionElem.getTextTrim());
                        }
                    }
                }
                List anss = aXPath.selectNodes(ctx);
                Iterator ansIter = anss.iterator();
                ArrayList<String> ansArray = new ArrayList<String>();
                while (ansIter.hasNext()) {
                    String ans = ((Element) ansIter.next()).getTextTrim();
                    if (delimiter != null) {
                        String splitted[] = ans.split(delimiter);
                        for (int j = 0; j < splitted.length; j++) {
                            if (!ansArray.contains(splitted[j])) ansArray.add(splitted[j]);
                        }
                    } else {
                        if (!ansArray.contains(ans)) ansArray.add(ans);
                    }
                }
                mea = new String[ansArray.size()];
                for (int j = 0; j < mea.length; j++) {
                    String slashed = ansArray.get(j);
                    mea[j] = "";
                    int startI = 0;
                    loop: for (; ; ) {
                        int leftBra = slashed.indexOf("(", startI);
                        if (leftBra >= 0) {
                            if (leftBra - startI > 1) {
                                if (mea[j].length() > 0) {
                                    mea[j] += " " + slashed.substring(startI, leftBra).trim();
                                } else {
                                    mea[j] += slashed.substring(startI, leftBra).trim();
                                }
                            }
                            int rightBra = slashed.indexOf(")", leftBra + 1);
                            if (rightBra < 0) break;
                            int middleLeftBra = leftBra;
                            while ((middleLeftBra = slashed.indexOf("(", middleLeftBra + 1)) < rightBra && middleLeftBra >= 0) {
                                rightBra = slashed.indexOf(")", rightBra + 1);
                                if (rightBra < 0) break loop;
                            }
                            if (mea[j].length() > 0) mea[j] += " ";
                            mea[j] += ("(" + slashed.substring(leftBra + 1, rightBra) + ")");
                            startI = rightBra + 1;
                        } else {
                            String sub = slashed.substring(startI).trim();
                            if (mea[j].length() > 0 && sub.length() > 0) {
                                mea[j] += " ";
                            }
                            mea[j] += sub;
                            break;
                        }
                    }
                }
                StringBuilder eSb = null;
                if (eXPath.length > 0) {
                    eSb = new StringBuilder();
                    for (int j = 0; j < eXPath.length; j++) {
                        List questions = eXPath[j].selectNodes(ctx);
                        Iterator eIter = questions.iterator();
                        while (eIter.hasNext()) {
                            Object question = eIter.next();
                            if (question instanceof Element) {
                                Element questionElem = (Element) question;
                                if (eSb.length() > 0) eSb.append(";  ");
                                if (bShowElem) {
                                    eSb.append(questionElem.getName());
                                    eSb.append(": ");
                                }
                                eSb.append(questionElem.getTextTrim());
                            }
                        }
                    }
                }
                boolean matched = true;
                String questionStr = qSb.toString();
                boolean qf = true;
                if (qPattern != null) {
                    if (!qPattern.matcher(questionStr).matches()) qf = false;
                }
                boolean af = true;
                if (aPattern != null) {
                    af = false;
                }
                for (int ii = 0; ii < mea.length; ii++) {
                    String at = mea[ii].trim();
                    if (!at.equals("")) {
                        if (aPattern != null) {
                            if (aPattern.matcher(at).matches()) af = true;
                        }
                    }
                }
                matched = qf && af;
                if (matched) {
                    boolean imat = (type == null);
                    for (int j = 0; j < typeCheck.size(); j++) {
                        String[] tc = (String[]) typeCheck.get(j);
                        boolean mtc = true;
                        outer: for (int k = 0; k < tc.length; k++) {
                            boolean ltc = false;
                            if (!ltc) {
                                mtc = false;
                                break;
                            }
                        }
                        if (mtc) {
                            imat = true;
                            break;
                        }
                    }
                    matched = imat;
                }
                if (matched) {
                    if (questionStr.length() == 0 || mea.length == 0) matched = false;
                }
                if (matched) {
                    mCount++;
                    if (mCount >= lolimit && mCount <= hilimit) {
                        numQ++;
                        if (numQ <= 5000 && qSb.length() > 0) {
                            Question tq = new Question();
                            tq.question = questionStr;
                            tq.ansDisplay = mea;
                            String answer[] = new String[mea.length];
                            int reduced = 0;
                            for (int n = 0; n < answer.length; n++) {
                                answer[n] = mea[n].replaceAll("([\\.\\-]|\\(.*?\\))", "").trim();
                                if (answer[n].length() == 0) {
                                    answer[n] = null;
                                    reduced++;
                                }
                            }
                            if (reduced > 0) {
                                String[] rSt = new String[mea.length - reduced];
                                String[] rStd = new String[mea.length - reduced];
                                int jj = 0;
                                for (int n = 0; n < answer.length; n++) {
                                    if (answer[n] != null) {
                                        rStd[jj] = tq.ansDisplay[n];
                                        rSt[jj++] = answer[n];
                                    }
                                }
                                answer = rSt;
                                tq.ansDisplay = rStd;
                            }
                            tq.answer = answer;
                            if (eSb != null) {
                                tq.extra = new String[tq.answer.length];
                                String extraStr = eSb.toString();
                                for (int jj = 0; jj < tq.answer.length; jj++) {
                                    tq.extra[jj] = extraStr;
                                }
                            }
                            Question existing = (Question) combiner.get(tq.question);
                            if (existing == null) {
                                result.add(tq);
                                combiner.put(tq.question, tq);
                            } else {
                                String tmpDisplay[] = existing.ansDisplay;
                                String tmp[] = existing.answer;
                                String tmpExtra[] = existing.extra;
                                if (tq.ansDisplay != null) {
                                    existing.ansDisplay = new String[existing.ansDisplay.length + tq.ansDisplay.length];
                                    for (int j = 0; j < tmpDisplay.length; j++) existing.ansDisplay[j] = tmpDisplay[j];
                                    for (int j = 0; j < tq.ansDisplay.length; j++) existing.ansDisplay[tmpDisplay.length + j] = tq.ansDisplay[j];
                                }
                                if (tq.answer != null) {
                                    existing.answer = new String[existing.answer.length + tq.answer.length];
                                    for (int j = 0; j < tmp.length; j++) existing.answer[j] = tmp[j];
                                    for (int j = 0; j < tq.answer.length; j++) existing.answer[tmp.length + j] = tq.answer[j];
                                }
                                if (tq.extra != null) {
                                    existing.extra = new String[existing.extra.length + tq.extra.length];
                                    for (int j = 0; j < tmpExtra.length; j++) existing.extra[j] = tmpExtra[j];
                                    for (int j = 0; j < tq.extra.length; j++) existing.extra[tmpExtra.length + j] = tq.extra[j];
                                }
                            }
                        }
                    }
                }
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } catch (Error e) {
            e.printStackTrace();
            throw new Exception("Error parsing xml... (maybe the file is too big?)");
        }
        return result;
    }
}
