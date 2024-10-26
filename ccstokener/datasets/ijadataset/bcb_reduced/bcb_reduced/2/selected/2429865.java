package net.sf.zorobot.theme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Pattern;
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

public class GoogledocTheme extends Theme {

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

    public GoogledocTheme(String name, String description, int mode) {
        super(name, description, true);
        this.mode = mode;
        this.addOption("id", "Google Doc Id");
        this.addOption("kanji", "Kanji");
        this.addOption("reading", "Reading");
        this.addOption("meaning", "Meaning");
        this.addOption("type", "Type");
        this.addOption("haskanji", "Has kanji");
        this.addOption("showkanji", "Show kanji");
        this.addOption("showreading", "Show reading");
        this.addOption("showmeaning", "Show meaning");
        this.addOption("question", "Question");
        this.addOption("list", "List");
        this.addOption("qModifier", "Question modifier");
        this.addOption("aModifier", "Answer modifier");
        this.addOption("limit", "Limit");
    }

    public boolean verify() {
        return true;
    }

    @Override
    public String getQuestions(String fullName, QuestionCollection qSet, HashMap<String, String> options, String nick) {
        if (qSet.size() < 10000) {
            ArrayList<Question> qArray = readEdict(options, nick);
            QuestionModifier.modify(qArray, options.get("qmodifier"), options.get("amodifier"));
            qSet.addQuestion(qArray);
            return null;
        }
        return "";
    }

    public String get(String s) {
        s = s.replaceAll("[^a-z0-9_]", "");
        StringBuilder sb = new StringBuilder();
        try {
            String result = null;
            URL url = new URL("http://docs.google.com/Doc?id=" + URLEncoder.encode(s, "UTF-8"));
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
            connection.setDoOutput(false);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String inputLine;
            int state = 0;
            while ((inputLine = in.readLine()) != null) {
                if (state == 0) {
                    int textPos = inputLine.indexOf("id=\"doc-contents");
                    if (textPos >= 0) {
                        state = 1;
                    }
                } else if (state == 1) {
                    int textPos = inputLine.indexOf("</div>");
                    if (textPos >= 0) break;
                    inputLine = inputLine.replaceAll("[\\u0000-\\u001F]", "");
                    sb.append(inputLine);
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private ArrayList<Question> readEdict(HashMap<String, String> options, String nick) {
        String filename = "";
        int theType = 0;
        ArrayList<Question> result = new ArrayList<Question>();
        try {
            String googleDocId = options.get("id");
            String kanji = options.get("kanji");
            String reading = options.get("reading");
            String meaning = options.get("meaning");
            String type = options.get("type");
            String hasKanji = options.get("haskanji");
            String showReading = options.get("showreading");
            String showMeaning = options.get("showmeaning");
            String question = options.get("question");
            String limit = options.get("limit");
            String list = options.get("list");
            if (hasKanji == null) hasKanji = "";
            if (showReading == null) showReading = "";
            if (showMeaning == null) showMeaning = "";
            if (question == null) question = "";
            if (limit == null) limit = "";
            if (list == null) list = "";
            String showKanji = options.get("showkanji");
            boolean showK = !(showKanji != null && showKanji.equalsIgnoreCase("no"));
            String extra = options.get("extra");
            String extraArr[] = new String[0];
            if (extra != null) {
                extraArr = extra.split(",");
            }
            if (kanji != null && kanjiDAO != null) kanji = StringUtility.expandKanji(kanji, kanjiDAO);
            kanji = StringUtility.processRegex(kanji);
            reading = StringUtility.processRegex(reading);
            meaning = StringUtility.processRegex(meaning);
            ArrayList<String[]> typeCheck = new ArrayList<String[]>();
            String ta[];
            if (type == null) ta = new String[0]; else ta = type.replaceAll("_", " ").split("\\|");
            for (int j = 0; j < ta.length; j++) {
                String ty[] = ta[j].split(",");
                typeCheck.add(ty);
            }
            Pattern kanjiPat = null;
            if (kanji != null) kanjiPat = Pattern.compile(kanji);
            ArrayList listFilter = null;
            if (list.length() > 0 && (filename.indexOf("edict") >= 0)) {
                if (list.startsWith("[profile")) {
                    String profList[] = list.replaceAll("\\[|\\]|profile", "").split("\\,|\\(|\\)");
                    int minRate = 0, maxRate = 100;
                    boolean firstNum = true;
                    String nickList = nick;
                    for (int i = 0; i < profList.length; i++) {
                        if (profList[i].matches("[0-9]+")) {
                            int num = Integer.parseInt(profList[i]);
                            if (num >= 0 && num <= 100) {
                                if (firstNum) {
                                    minRate = num;
                                    maxRate = num;
                                    firstNum = false;
                                } else {
                                    if (num > minRate) maxRate = num;
                                    if (num < maxRate) minRate = num;
                                }
                            }
                        } else {
                            if (profList[i].length() > 0) nickList = profList[i];
                        }
                    }
                    System.out.println(nickList + " " + minRate + " " + maxRate);
                    listFilter = profileDAO.getProfList(nickList, 3, minRate, maxRate);
                } else listFilter = kanjiDAO.getSelectedEdict(list);
            }
            int dash = limit.indexOf("-");
            int lolimit = 0, hilimit = 1000000;
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
            boolean bHasKanjiFilter = !hasKanji.equalsIgnoreCase("");
            boolean bHasKanji = hasKanji.equalsIgnoreCase("yes");
            boolean bShowReading;
            if (filename.contains("enamdict") || filename.contains("jplaces")) bShowReading = showReading.equalsIgnoreCase("yes"); else {
                bShowReading = !showReading.equalsIgnoreCase("no");
            }
            boolean bShowMeaning = showMeaning.equalsIgnoreCase("yes");
            boolean bQuestion = question.equalsIgnoreCase("reading");
            boolean bRomaji = question.equalsIgnoreCase("romaji");
            boolean bKanji = question.equalsIgnoreCase("kanji");
            Hashtable combiner = new Hashtable();
            int bpos;
            try {
                String ss = get(googleDocId);
                String lines[] = ss.replaceAll("[\r\n]", "").replaceAll("(?i)<BR.*?>", "\n").replaceAll("<.*?>", "").split("\n");
                String toRead, kan, rea;
                String[] mea;
                ArrayList typ;
                int i = 0;
                int numQ = 0;
                int mCount = 0;
                int listPos = 0;
                int listToCompare = 0;
                if (listFilter != null) {
                    if (listFilter.size() == 0) return result;
                    listToCompare = ((Integer) listFilter.get(listPos)).intValue();
                    listPos++;
                }
                for (int ii = 0; ii < lines.length; ii++) {
                    toRead = lines[ii];
                    if (listFilter == null || (listFilter != null && i == listToCompare)) {
                        if (i > 0 || theType == 1) {
                            String slashed[] = toRead.split("/");
                            if ((bpos = slashed[0].indexOf("[")) > -1) {
                                int lpos = slashed[0].indexOf("]");
                                if (lpos < 0) lpos = bpos + 1;
                                kan = slashed[0].substring(0, bpos).trim();
                                rea = slashed[0].substring(bpos + 1, lpos).trim();
                            } else {
                                kan = "";
                                rea = slashed[0].trim();
                            }
                            mea = new String[slashed.length - 1];
                            typ = new ArrayList();
                            for (int j = 0; j < mea.length; j++) {
                                mea[j] = "";
                                int startI = 0;
                                loop: for (; ; ) {
                                    int leftBra = slashed[j + 1].indexOf("(", startI);
                                    if (leftBra >= 0) {
                                        if (leftBra - startI > 1) {
                                            if (mea[j].length() > 0) {
                                                mea[j] += " " + slashed[j + 1].substring(startI, leftBra).trim();
                                            } else {
                                                mea[j] += slashed[j + 1].substring(startI, leftBra).trim();
                                            }
                                        }
                                        int rightBra = slashed[j + 1].indexOf(")", leftBra + 1);
                                        if (rightBra < 0) break;
                                        int middleLeftBra = leftBra;
                                        while ((middleLeftBra = slashed[j + 1].indexOf("(", middleLeftBra + 1)) < rightBra && middleLeftBra >= 0) {
                                            rightBra = slashed[j + 1].indexOf(")", rightBra + 1);
                                            if (rightBra < 0) break loop;
                                        }
                                        if (mea[j].length() > 0) mea[j] += " ";
                                        mea[j] += ("(" + slashed[j + 1].substring(leftBra + 1, rightBra) + ")");
                                        typ.add(slashed[j + 1].substring(leftBra + 1, rightBra));
                                        startI = rightBra + 1;
                                    } else {
                                        String sub = slashed[j + 1].substring(startI).trim();
                                        if (mea[j].length() > 0 && sub.length() > 0) {
                                            mea[j] += " ";
                                        }
                                        mea[j] += sub;
                                        break;
                                    }
                                }
                            }
                            ArrayList newType = new ArrayList();
                            for (int z = 0; z < typ.size(); z++) {
                                String s = (String) typ.get(z);
                                String[] sp = s.split(",");
                                for (int y = 0; y < sp.length; y++) {
                                    newType.add(sp[y].trim());
                                }
                            }
                            typ = newType;
                            boolean matched = false;
                            try {
                                boolean imat = (kanji == null);
                                if (kanji != null) {
                                    if (kanjiPat.matcher(kan).matches()) {
                                        imat = true;
                                    }
                                }
                                if (imat) {
                                    imat = (reading == null);
                                    if (reading != null) {
                                        if (rea.matches(reading)) {
                                            imat = true;
                                        }
                                    }
                                    if (imat) {
                                        imat = (meaning == null);
                                        if (meaning != null) {
                                            for (int k = 0; k < mea.length; k++) {
                                                if (mea[k].matches(meaning)) {
                                                    imat = true;
                                                    break;
                                                }
                                            }
                                        }
                                        matched = imat;
                                    }
                                }
                            } catch (Exception e) {
                                matched = false;
                            }
                            if (matched) {
                                boolean imat = (type == null);
                                for (int j = 0; j < typeCheck.size(); j++) {
                                    String[] tc = (String[]) typeCheck.get(j);
                                    boolean mtc = true;
                                    outer: for (int k = 0; k < tc.length; k++) {
                                        boolean ltc = false;
                                        for (int l = 0; l < typ.size(); l++) {
                                            if (tc[k].endsWith("*")) {
                                                if (((String) typ.get(l)).startsWith(tc[k].substring(0, tc[k].length() - 1))) {
                                                    ltc = true;
                                                    break;
                                                }
                                            } else {
                                                if (tc[k].equals((String) typ.get(l))) {
                                                    ltc = true;
                                                    break;
                                                }
                                            }
                                        }
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
                            if (matched && bHasKanjiFilter) {
                                matched = (bHasKanji && !kan.equals("") || !bHasKanji && kan.equals(""));
                            }
                            if (matched && !bShowReading && !bShowMeaning && kan.equals("")) matched = false;
                            if (matched && bQuestion && !bShowMeaning && kan.equals("")) matched = false;
                            if (matched) {
                                mCount++;
                                if (mCount >= lolimit && mCount <= hilimit) {
                                    numQ++;
                                    if (numQ <= 5000) {
                                        Question tq = new Question();
                                        if (bQuestion) {
                                            if (showK) tq.question = kan; else tq.question = "";
                                            if (bShowReading) {
                                                if (showK) tq.question += " [" + rea + "]"; else tq.question += rea;
                                            }
                                            if (bShowMeaning) {
                                                tq.question += " (";
                                                for (int j = 0; j < mea.length; j++) {
                                                    if (!mea[j].equals("")) {
                                                        if (j > 0) tq.question += ", ";
                                                        tq.question += mea[j];
                                                    }
                                                }
                                                tq.question += ")";
                                            }
                                            tq.answer = new String[1];
                                            tq.answer[0] = rea;
                                        } else if (bRomaji) {
                                            if (bShowReading) {
                                                if (kan.length() == 0 || !showK) tq.question = rea; else {
                                                    if (showK) tq.question = kan + " [" + rea + "]"; else tq.question = rea;
                                                }
                                            } else if (showK) tq.question = kan; else tq.question = "";
                                            if (bShowMeaning) {
                                                tq.question += " (";
                                                for (int j = 0; j < mea.length; j++) {
                                                    if (!mea[j].equals("")) {
                                                        if (j > 0) tq.question += ", ";
                                                        tq.question += mea[j];
                                                    }
                                                }
                                                tq.question += ")";
                                            }
                                            tq.ansDisplay = new String[1];
                                            tq.ansDisplay[0] = JapaneseString.toRomaji(rea);
                                            tq.answer = new String[1];
                                            tq.answer[0] = tq.ansDisplay[0].replaceAll("([\\.\\-]|\\(.*?\\))", "").trim();
                                        } else if (bKanji) {
                                            if (bShowReading) {
                                                if (showK) tq.question = kan + " [" + rea + "]"; else tq.question = rea;
                                            } else if (showK) tq.question = kan; else tq.question = "";
                                            if (bShowMeaning) {
                                                tq.question += " (";
                                                for (int j = 0; j < mea.length; j++) {
                                                    if (!mea[j].equals("")) {
                                                        if (j > 0) tq.question += ", ";
                                                        tq.question += mea[j];
                                                    }
                                                }
                                                tq.question += ")";
                                            }
                                            tq.answer = new String[1];
                                            tq.answer[0] = kan;
                                        } else {
                                            if (kan.equals("")) {
                                                tq.question = rea;
                                            } else {
                                                if (showK) tq.question = kan; else tq.question = "";
                                                if (bShowReading) {
                                                    if (showK) tq.question += " [" + rea + "]"; else tq.question = rea;
                                                }
                                            }
                                            if (bShowMeaning) {
                                                tq.question += " (";
                                                for (int j = 0; j < mea.length; j++) {
                                                    if (!mea[j].equals("")) {
                                                        if (j > 0) tq.question += ", ";
                                                        tq.question += mea[j];
                                                    }
                                                }
                                                tq.question += ")";
                                            }
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
                                        }
                                        if (extraArr.length > 0) {
                                            StringBuilder ssb = new StringBuilder();
                                            for (int n = 0; n < extraArr.length; n++) {
                                                if ("meaning".equals(extraArr[n])) {
                                                    if (ssb.length() > 0) ssb.append(";  ");
                                                    ssb.append("Meaning: ");
                                                    for (int j = 0; j < mea.length; j++) {
                                                        if (j > 0) ssb.append(", ");
                                                        ssb.append(mea[j]);
                                                    }
                                                } else if ("kanji".equals(extraArr[n])) {
                                                    if (ssb.length() > 0) ssb.append(";  ");
                                                    ssb.append("Kanji: ");
                                                    ssb.append(kan);
                                                } else if ("reading".equals(extraArr[n])) {
                                                    if (ssb.length() > 0) ssb.append(";  ");
                                                    ssb.append("Reading: ");
                                                    ssb.append(rea);
                                                }
                                            }
                                            if (ssb.length() > 0) {
                                                tq.extra = new String[tq.answer.length];
                                                String ex = ssb.toString();
                                                if (ex.length() > 150) ex = ex.substring(0, 148) + "...";
                                                for (int k = 0; k < tq.extra.length; k++) {
                                                    tq.extra[k] = ex;
                                                }
                                            }
                                        }
                                        Question existing = (Question) combiner.get(tq.question);
                                        if (existing == null) {
                                            result.add(tq);
                                            combiner.put(tq.question, tq);
                                        } else {
                                            String tmpDisplay[] = existing.ansDisplay;
                                            String tmp[] = existing.answer;
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
                                        }
                                    }
                                }
                            }
                        }
                        if (listFilter != null) {
                            if (listPos == listFilter.size()) break;
                            listToCompare = ((Integer) listFilter.get(listPos)).intValue();
                            listPos++;
                        }
                    }
                    i++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return result;
    }
}
