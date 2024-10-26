package net.sf.zorobot.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.sf.zorobot.core.Question;

public class FlashExReader {

    public FlashExReader() {
    }

    public FlashExObj get(String s, int page) {
        FlashExObj retVal = new FlashExObj();
        s = s.replaceAll("[^a-z0-9_]", "");
        ArrayList list = new ArrayList();
        retVal.list = list;
        try {
            String result = null;
            URL url = new URL("http://www.flashcardexchange.com/flashcards/list/" + URLEncoder.encode(s, "UTF-8") + "?page=" + page);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
            connection.setDoOutput(false);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String inputLine;
            int state = 2;
            StringBuilder sb = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                if (state == 0) {
                    int textPos = inputLine.indexOf("Number of Card");
                    if (textPos >= 0) {
                        state = 1;
                    }
                } else if (state == 1) {
                    int s1 = inputLine.indexOf(">");
                    int s2 = inputLine.indexOf("<", 1);
                    if (s1 >= 0 && s1 < s2) {
                        String numOfCardStr = inputLine.substring(s1 + 1, s2);
                        try {
                        } catch (Exception e) {
                        }
                        state = 2;
                    }
                } else if (state == 2) {
                    int textPos = inputLine.indexOf("tbody class=\"shaded\"");
                    if (textPos >= 0) {
                        state = 3;
                    }
                } else if (state == 3) {
                    int textPos = inputLine.indexOf("tbody");
                    if (textPos >= 0) {
                        break;
                    }
                    sb.append(inputLine);
                    sb.append(" ");
                }
            }
            in.close();
            Pattern myPattern = Pattern.compile("<td>(.*?)</td>");
            Matcher myMatcher = myPattern.matcher(sb);
            String str;
            int counter = 0;
            String buff[] = new String[4];
            while (myMatcher.find()) {
                int tt = counter % 4;
                buff[tt] = myMatcher.group(1);
                if (tt == 3) {
                    String toAdd[] = new String[2];
                    toAdd[0] = buff[1];
                    toAdd[1] = buff[2];
                    list.add(toAdd);
                }
                counter++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public ArrayList get(String num) {
        ArrayList al = new ArrayList();
        int maxLoop = 50;
        for (int i = 0; i < maxLoop; i++) {
            FlashExObj ff = get(num, 1 + i);
            al.addAll(ff.list);
            if (ff.list.size() < 50) break;
        }
        return al;
    }

    public ArrayList<Question> getQuestions(HashMap<String, String> option) {
        ArrayList<Question> result = new ArrayList();
        String id = option.get("id");
        if (id == null) return result;
        ArrayList al = get(id);
        String delimiter = option.get("delimiter");
        Iterator iter = al.iterator();
        boolean swap = "yes".equalsIgnoreCase((String) option.get("swap"));
        while (iter.hasNext()) {
            String item[] = (String[]) iter.next();
            Question tq = new Question();
            if (item[0] == null) item[0] = "";
            if (item[1] == null) item[1] = "";
            item[0] = item[0].trim();
            item[1] = item[1].trim();
            item[0] = HTMLUtil.convertCharacterEntities(HTMLUtil.stripHTMLTags(item[0]));
            item[1] = HTMLUtil.convertCharacterEntities(HTMLUtil.stripHTMLTags(item[1]));
            if (swap) {
                tq.question = item[1];
                if (delimiter != null) tq.ansDisplay = item[0].split(delimiter); else tq.ansDisplay = new String[] { item[0] };
                tq.answer = new String[tq.ansDisplay.length];
                for (int i = 0; i < tq.ansDisplay.length; i++) {
                    tq.answer[i] = tq.ansDisplay[i].replaceAll("([\\.\\-]|\\(.*\\))", "");
                }
            } else {
                tq.question = item[0];
                if (delimiter != null) tq.ansDisplay = item[1].split(delimiter); else tq.ansDisplay = new String[] { item[1] };
                tq.answer = new String[tq.ansDisplay.length];
                for (int i = 0; i < tq.ansDisplay.length; i++) {
                    tq.answer[i] = tq.ansDisplay[i].replaceAll("([\\.\\-]|\\(.*\\))", "");
                }
            }
            if (tq != null) {
                result.add(tq);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        FlashExReader tfr = new FlashExReader();
        ArrayList a = tfr.get("488229");
        for (int i = 0; i < a.size(); i++) {
            System.out.println((String) a.get(i));
        }
    }
}

class FlashExObj {

    public int page;

    public ArrayList list;
}
