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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
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

public class KanjidicTheme extends Theme {

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

    public KanjidicTheme(String name, String description) {
        super(name, description, true);
        this.addOption("kanji", "Kanji");
        this.addOption("on", "On Reading");
        this.addOption("kun", "Kun Reading");
        this.addOption("meaning", "Meaning");
        this.addOption("grade", "Grade");
        this.addOption("extra", "Extra");
        this.addOption("freq", "Frequency");
        this.addOption("stroke", "Stroke");
        this.addOption("question", "Question");
        this.addOption("radical", "Radical");
        this.addOption("qModifier", "Question modifier");
        this.addOption("aModifier", "Answer modifier");
        this.addOption("limit", "Limit");
        this.addOption("url", "Url containing list of kanji");
        this.addOption("encoding", "Encoding of external site specified by url");
        this.addOption("exclude_url", "Url containing list of kanji to be excluded");
        this.addOption("exclude_encoding", "Encoding or external site specified by exclude_url");
    }

    public boolean verify() {
        return (kanjiDAO != null && kanjiDAO.checkKanjidic());
    }

    @Override
    public String getQuestions(String fullName, QuestionCollection qSet, HashMap<String, String> options, String nick) {
        if (qSet.size() < 10000) {
            String errMsg = null;
            processKanjiProfile(options, nick);
            try {
                String kanji = (String) options.get("kanji");
                String url = (String) options.get("url");
                String encoding = (String) options.get("encoding");
                String excludeUrl = (String) options.get("exclude_url");
                String excludeEncoding = (String) options.get("exclude_encoding");
                if (url != null) {
                    HashSet<Character> includedKanji = getKanji(url, encoding);
                    HashSet<Character> excludedKanji = null;
                    if (excludeUrl != null) excludedKanji = getKanji(url, encoding);
                    StringBuffer sb = new StringBuffer();
                    if (includedKanji != null) {
                        if (excludedKanji != null) {
                            Iterator<Character> iter = includedKanji.iterator();
                            while (iter.hasNext()) {
                                Character c = iter.next();
                                if (!excludedKanji.contains(c)) sb.append(c);
                            }
                        } else {
                            Iterator<Character> iter = includedKanji.iterator();
                            while (iter.hasNext()) {
                                sb.append(iter.next());
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        if (kanji != null) kanji += sb.toString(); else kanji = sb.toString();
                    }
                }
                ArrayList<Question> qArray = kanjiDAO.getKanjiQuestion((String) options.get("question"), (String) options.get("extra"), (String) options.get("grade"), (String) options.get("stroke"), (String) options.get("freq"), (String) options.get("limit"), (String) options.get("radical"), kanji);
                QuestionModifier.modify(qArray, options.get("qmodifier"), options.get("amodifier"));
                qSet.addQuestion(qArray);
            } catch (Exception e) {
                errMsg = e.getMessage();
            }
            return errMsg;
        }
        return "";
    }

    public HashSet<Character> getKanji(String s, String encoding) throws Exception {
        HashSet<Character> hs = new HashSet<Character>();
        if (encoding == null) encoding = "UTF-8";
        if (!s.startsWith("http")) {
            throw new Exception("Url must point to a text or html file, and must use http protocol");
        }
        try {
            String result = null;
            URL url = new URL(s);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
            connection.setDoOutput(false);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
            String inputLine;
            String contentType = connection.getContentType();
            if (!(contentType.startsWith("text") || contentType.startsWith("application/xml"))) {
                in.close();
                throw new Exception("Url must point to a text or html file, and must use http protocol");
            }
            while ((inputLine = in.readLine()) != null) {
                char[] arr = inputLine.toCharArray();
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i] >= '々' && arr[i] <= '〇' || arr[i] >= '一' && arr[i] <= '龥') {
                        if (!hs.contains(arr[i])) {
                            hs.add(arr[i]);
                        }
                    }
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return hs;
    }

    private void processKanjiProfile(HashMap<String, String> options, String nick) {
        if (profileDAO == null) return;
        String kanji = (String) options.get("kanji");
        if (kanji != null) {
            if (kanji.matches("(\\[)?profile\\(.*\\)(\\])?")) {
                int startLB = kanji.indexOf("(");
                int startRB = kanji.indexOf(")");
                if (startLB < startRB) {
                    String profCmd = kanji.substring(startLB + 1, startRB);
                    String profElem[] = profCmd.split(",");
                    int minRate = 0, maxRate = 100;
                    boolean firstNum = true;
                    String nickList = nick;
                    int type = 2;
                    for (int i = 0; i < profElem.length; i++) {
                        if (profElem[i].matches("[0-9]+")) {
                            int num = Integer.parseInt(profElem[i]);
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
                        } else if (profElem[i].length() > 0) {
                            if (profElem[i].equalsIgnoreCase("kun")) type = 0; else if (profElem[i].equalsIgnoreCase("on")) type = 1; else if (profElem[i].equalsIgnoreCase("meaning")) type = 2; else nickList = profElem[i];
                        }
                    }
                    String toReplace = profileDAO.getProfKanjiList(nickList, type, minRate, maxRate);
                    if (toReplace.length() == 0) toReplace = "x";
                    options.put("kanji", toReplace);
                }
            }
        }
    }
}
