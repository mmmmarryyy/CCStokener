import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.applet.*;
import java.net.*;
import java.io.*;

class Time {

    int from;

    int to;

    int m, tu, w, th, f;
}

class entry {

    String name;

    Time[] time;
}

class answerEntry {

    entry[] classes;

    int gap;
}

public class FinalDistance extends Applet implements ActionListener, KeyListener, Runnable {

    private String[][] orBox = new String[5][5];

    boolean containsOR;

    int PRINTINTERVAL = 5000;

    Thread t = null;

    private int SHIFT = -75;

    entry[][] matrix = new entry[30][20];

    answerEntry[] answer = new answerEntry[10];

    entry[] val;

    int clss, timeEntry, nextTime;

    int cNumX = 50, cNumY = 100;

    int beat10[] = new int[10];

    private boolean buttonPressed = false;

    int time, worstTime, worstIndex;

    String textArea2append = "";

    private TextField courseNum;

    private Choice lst;

    private Choice term;

    private TextArea textArea1, textArea2;

    private Button button1, net;

    private Button instructions;

    private Button webSite;

    private Button print;

    private Button[] imageButtons;

    private TextField idField;

    private Button loadButton;

    private Button saveButton;

    private Checkbox merge;

    private Checkbox sectionBind;

    boolean drawWarning = false;

    private int displayNum = 0;

    private Image offImage;

    private Graphics offGraphics;

    private boolean rePrintAnswer = false;

    private int percentDone = 0;

    private int oldPercentDone = 0;

    private int amountToReach = 1;

    private int amountDone = 0;

    private int oldAmountDone = 0;

    private String publicSign = "";

    private String errorMessage;

    private Label selectTerm;

    private boolean badInput = false;

    public void copyTime(Time a, Time b) {
        a.from = b.from;
        a.to = b.to;
        a.m = b.m;
        a.tu = b.tu;
        a.w = b.w;
        a.th = b.th;
        a.f = b.f;
    }

    public String removeOR(String arg) {
        StringTokenizer st = new StringTokenizer(arg, "\n\r");
        String token = "", orText = "";
        String total = "";
        while (st.hasMoreTokens() == true) {
            token = st.nextToken();
            if (token.indexOf("OR:") != -1) {
                orText += token + "\n";
            } else if (token.indexOf("or:") != -1) {
                orText += token + "\n";
            } else total += token + "\n";
        }
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                orBox[i][j] = "NOT IN ORBOX";
            }
        }
        StringTokenizer a = new StringTokenizer(orText, "\n\r");
        String line = "", piece = "";
        int across = 0, down = 0;
        while (a.hasMoreTokens() == true) {
            line = a.nextToken();
            if (line.indexOf("OR:") != -1) line = line.substring(line.indexOf("OR:") + 3, line.length()); else line = line.substring(line.indexOf("or:") + 3, line.length());
            StringTokenizer b = new StringTokenizer(line, ",");
            down = 0;
            while (b.hasMoreTokens() == true) {
                piece = b.nextToken();
                piece = piece.trim();
                if (across == 5 || down == 5) {
                    rePrintAnswer = true;
                    errorMessage = "Error: Exceeded 5 OR statements!";
                    badInput = true;
                    repaint();
                    return "";
                }
                orBox[across][down] = piece;
                down++;
            }
            across++;
        }
        return total;
    }

    public void run() {
        loop(0, 0, val);
        epilogue();
    }

    public void epilogue() {
        button1.setLabel("Close the Distance");
        for (int i = 0; i < 10; i++) beat10[i] = 0;
        if (!badInput) {
            sortAnswer();
            printAnswer();
            textArea2.setCaretPosition(0);
            for (int i = 0; i < 10; i++) {
                if (answer[i].gap != -1 && answer[i].gap != 9999 && answer[i].gap != 10000) {
                    imageButtons[i].setVisible(true);
                } else imageButtons[i].setVisible(false);
            }
            if (!imageButtons[0].isVisible()) {
                print.setVisible(false);
                selectTerm.setVisible(false);
            } else {
                print.setVisible(true);
                selectTerm.setVisible(true);
            }
            displayNum = 0;
            repaint();
        }
        if (badInput) {
            if (errorMessage.equals("")) errorMessage = "Error:  check input.";
            rePrintAnswer = true;
            textArea2.setText("ERROR: " + errorMessage);
        }
        if (textArea2.getText().equals("DONE")) textArea2.append(" - ALL CONFLICTS");
        textArea2.insert("", 0);
    }

    public void init() {
        FinalDistance f = new FinalDistance();
        offImage = this.createImage(960, 720);
        offGraphics = offImage.getGraphics();
        merge = new Checkbox("Bind lab/dis", false);
        add(merge);
        sectionBind = new Checkbox("Bind section", false);
        add(sectionBind);
        sectionBind.setState(true);
        imageButtons = new Button[10];
        for (int i = 0; i < 10; i++) {
            imageButtons[i] = new Button("" + (i + 1));
            imageButtons[i].setBounds(450, 490 + 20 * i + SHIFT, 45, 20);
            add(imageButtons[i]);
            imageButtons[i].addActionListener(this);
            imageButtons[i].setVisible(false);
        }
        for (int i = 0; i < 10; i++) beat10[i] = 0;
        val = new entry[30];
        for (int i = 0; i < 30; i++) {
            val[i] = new entry();
            val[i].time = new Time[4];
            for (int j = 0; j < 4; j++) {
                val[i].time[j] = new Time();
                val[i].time[j].from = 0;
            }
        }
        for (int m = 0; m < 10; m++) {
            answer[m] = new answerEntry();
            answer[m].classes = new entry[30];
            for (int j = 0; j < 30; j++) answer[m].classes[j] = new entry();
            answer[m].gap = -1;
            for (int n = 0; n < 30; n++) {
                answer[m].classes[n].time = new Time[4];
                for (int o = 0; o < 4; o++) {
                    answer[m].classes[n].time[o] = new Time();
                }
            }
        }
        for (int i = 0; i < 30; i++) for (int j = 0; j < 20; j++) {
            matrix[i][j] = new entry();
            matrix[i][j].time = new Time[4];
            for (int k = 0; k < 4; k++) {
                matrix[i][j].time[k] = new Time();
                matrix[i][j].time[k].from = 0;
            }
        }
        clss = -1;
        timeEntry = -1;
        nextTime = -1;
        setLayout(null);
        net = new Button("Add Class");
        lst = new Choice();
        term = new Choice();
        courseNum = new TextField("Course Number");
        add(courseNum);
        print = new Button("Pretty HTML");
        webSite = new Button("More Info");
        button1 = new Button("Close the Distance");
        textArea1 = new TextArea(7, 30);
        textArea2 = new TextArea("", 7, 30, TextArea.SCROLLBARS_VERTICAL_ONLY);
        idField = new TextField("ID NUMBER");
        loadButton = new Button("Load state");
        saveButton = new Button("Save state");
        loadButton.addActionListener(this);
        saveButton.addActionListener(this);
        add(loadButton);
        add(saveButton);
        add(idField);
        idField.setBounds(162, 742 + SHIFT, 100, 25);
        loadButton.setBounds(262, 730 + SHIFT, 75, 25);
        saveButton.setBounds(262, 755 + SHIFT, 75, 25);
        instructions = new Button("Display Instructions");
        instructions.setBounds(50, 690 + SHIFT, 400, 20);
        add(instructions);
        add(webSite);
        add(print);
        print.addActionListener(this);
        webSite.addActionListener(this);
        instructions.addActionListener(this);
        net.setBounds(270, 85, 180, 40);
        webSite.setBounds(160, 112, 100, 25);
        selectTerm = new Label("(Select the correct season)");
        selectTerm.setBounds(966 - 135, 190, 200, 20);
        add(selectTerm);
        selectTerm.setBackground(new Color(233, 238, 242));
        print.setBounds(956 - 100, 210, 100, 25);
        print.setVisible(false);
        selectTerm.setVisible(false);
        courseNum.setBounds(cNumX, cNumY, 100, 25);
        button1.setBounds(100, 440 + SHIFT, 300, 50);
        lst.setBounds(50, 50, 300, 20);
        lst.addKeyListener(this);
        sectionBind.setBackground(Color.white);
        sectionBind.setBounds(160, 72, 100, 20);
        merge.setBackground(Color.white);
        merge.setBounds(160, 92, 100, 20);
        term.setBounds(350, 50, 100, 20);
        lst.addItem("Aerospace Studies");
        lst.addItem("African American Studies");
        lst.addItem("Afrikaans");
        lst.addItem("Agricultural and Environmental Chemistry");
        lst.addItem("Agricultural and Resource Economics and Policy");
        lst.addItem("Altaic");
        lst.addItem("American Studies");
        lst.addItem("Ancient History and Mediterranean Archaeology");
        lst.addItem("Anthropology");
        lst.addItem("Applied Science and Technology");
        lst.addItem("Arabic");
        lst.addItem("Architecture");
        lst.addItem("Asian American Studies Program");
        lst.addItem("Asian Studies");
        lst.addItem("Astronomy");
        lst.addItem("Bioengineering");
        lst.addItem("Biology");
        lst.addItem("Buddhism");
        lst.addItem("Business Administration Undergraduate Program");
        lst.addItem("Business Administration Doctoral Program");
        lst.addItem("Catalan");
        lst.addItem("Celtic Studies");
        lst.addItem("Chemical Engineering");
        lst.addItem("Chemistry");
        lst.addItem("Chicano Studies Program");
        lst.addItem("Chinese");
        lst.addItem("City and Regional Planning");
        lst.addItem("Civil and Environmental Engineering");
        lst.addItem("Classics");
        lst.addItem("Cognitive Science");
        lst.addItem("College Writing Program");
        lst.addItem("Comparative Biochemistry");
        lst.addItem("Comparative Literature");
        lst.addItem("Computer Science");
        lst.addItem("Cuneiform");
        lst.addItem("Demography");
        lst.addItem("Development Studies");
        lst.addItem("Dutch");
        lst.addItem("Earth and Planetary Science");
        lst.addItem("East Asian Languages and Cultures");
        lst.addItem("East European Studies");
        lst.addItem("Economics");
        lst.addItem("Education");
        lst.addItem("Egyptian");
        lst.addItem("Electrical Engineering");
        lst.addItem("Energy and Resources Program");
        lst.addItem("Engineering");
        lst.addItem("English");
        lst.addItem("Environmental Design");
        lst.addItem("Environmental Economics and Policy");
        lst.addItem("Environmental Science, Policy, and Management");
        lst.addItem("Environmental Sciences");
        lst.addItem("Ethnic Studies");
        lst.addItem("Ethnic Studies Graduate Group");
        lst.addItem("Film Studies");
        lst.addItem("Folklore");
        lst.addItem("French");
        lst.addItem("Geography");
        lst.addItem("German");
        lst.addItem("Greek");
        lst.addItem("Health and Medical Sciences");
        lst.addItem("Hebrew");
        lst.addItem("Hindi-Urdu");
        lst.addItem("History");
        lst.addItem("History of Art");
        lst.addItem("Industrial Engineering and Operations Research");
        lst.addItem("Information Management and Systems");
        lst.addItem("Integrative Biology");
        lst.addItem("Interdepartmental Studies");
        lst.addItem("Interdisciplinary Studies Field Major");
        lst.addItem("International and Area Studies");
        lst.addItem("Iranian");
        lst.addItem("Italian Studies");
        lst.addItem("Japanese");
        lst.addItem("Journalism");
        lst.addItem("Khmer");
        lst.addItem("Korean");
        lst.addItem("Landscape Architecture");
        lst.addItem("Language Proficiency Program");
        lst.addItem("Latin");
        lst.addItem("Latin American Studies");
        lst.addItem("Law");
        lst.addItem("Legal Studies");
        lst.addItem("Letters and Science");
        lst.addItem("Linguistics");
        lst.addItem("Malay/Indonesian");
        lst.addItem("Mass Communications");
        lst.addItem("Materials Science and Engineering");
        lst.addItem("Mathematics");
        lst.addItem("Mechanical Engineering");
        lst.addItem("Medieval Studies");
        lst.addItem("Middle Eastern Studies");
        lst.addItem("Military Affairs");
        lst.addItem("Military Science");
        lst.addItem("Molecular and Cell Biology");
        lst.addItem("Music");
        lst.addItem("Native American Studies");
        lst.addItem("Natural Resources");
        lst.addItem("Naval Science");
        lst.addItem("Near Eastern Studies");
        lst.addItem("Neuroscience");
        lst.addItem("Nuclear Engineering");
        lst.addItem("Nutritional Sciences and Toxicology");
        lst.addItem("Ocean Engineering");
        lst.addItem("Optometry");
        lst.addItem("Peace and Conflict Studies");
        lst.addItem("Persian");
        lst.addItem("Philosophy");
        lst.addItem("Physical Education");
        lst.addItem("Physics");
        lst.addItem("Plant and Microbial Biology");
        lst.addItem("Political Economy of Industrial Societies");
        lst.addItem("Political Science");
        lst.addItem("Portuguese");
        lst.addItem("Practice of Art");
        lst.addItem("Psychology");
        lst.addItem("Public Health");
        lst.addItem("Public Policy");
        lst.addItem("Punjabi");
        lst.addItem("Religious Studies");
        lst.addItem("Rhetoric");
        lst.addItem("Sanskrit");
        lst.addItem("Scandinavian");
        lst.addItem("Science and Mathematics Education");
        lst.addItem("Semitics");
        lst.addItem("Slavic Languages and Literatures");
        lst.addItem("Social Welfare");
        lst.addItem("Sociology");
        lst.addItem("South and Southeast Asian Studies");
        lst.addItem("South Asian");
        lst.addItem("Southeast Asian");
        lst.addItem("Spanish");
        lst.addItem("Statistics");
        lst.addItem("Tagalog");
        lst.addItem("Tamil");
        lst.addItem("Thai");
        lst.addItem("Theater, Dance, and Performance Studies");
        lst.addItem("Tibetan");
        lst.addItem("Turkish");
        lst.addItem("Undergraduate and Interdisciplinary Studies");
        lst.addItem("Vietnamese");
        lst.addItem("Vision Science");
        lst.addItem("Visual Studies");
        lst.addItem("Women's Studies");
        lst.addItem("Yiddish");
        lst.select(0);
        term.addItem("Fall");
        term.addItem("Spring");
        term.addItem("Summer");
        term.select(0);
        add(lst);
        add(term);
        textArea1.setBounds(50, 240 + SHIFT, 400, 200);
        textArea2.setBounds(50, 490 + SHIFT, 400, 200);
        textArea1.setEditable(true);
        textArea2.setEditable(false);
        button1.addActionListener(this);
        net.addActionListener(this);
        textArea2.setBackground(Color.lightGray);
        add(textArea1);
        add(button1);
        add(net);
        add(textArea2);
        textArea1.setFont(new Font("Verdana", Font.PLAIN, 12));
        textArea2.setFont(new Font("Verdana", Font.PLAIN, 12));
        showInstructions();
        textArea1.setText("ELECTRICAL ENGINEERING 40 DIS \n8-9A Tu\n11-12P W\n9-10A Th\n3-4P F\n1-2P M\n2-3P W\n\nELECTRICAL ENGINEERING 40 LAB \n9-12P M\n12-3P M\n1-4P Tu\n9-12P W\n12-3P W\n1-4P Th\n4-7P Tu\n9-12P F\n12-3P F\n3-6P W\n3-6P M\n6-9P W\n\nELECTRICAL ENGINEERING 40 LEC\n11-1230P TuTh\n\nFILM STUDIES 108 DIS \n5-7P Tu\n\nFILM STUDIES 108 LEC\n10-11A MWF\n\nJAPANESE 10A LEC\n8-9A MTuWThF\n9-10A MTuWThF\n10-11A MTuWThF\n11-12P MTuWThF\n\nJAPANESE 10AS LEC\n3-4P W\n\nJOB\n1200-6p mf\n");
        textArea1.insert("", 0);
    }

    public void printWarn() {
        if (amountDone - oldAmountDone >= PRINTINTERVAL) {
            updateImageButtons();
            textArea2.setText("Reaching... Click to stop.  \n" + amountDone + "/" + amountToReach + " paths filtered. (" + percentDone + "%)" + "\n\n...Current top 10 --->");
            oldPercentDone = percentDone;
            oldAmountDone = amountDone;
            update(getGraphics());
        }
    }

    public void showInstructions() {
        rePrintAnswer = true;
        textArea2.setText("");
        textArea2.append("FINAL DISTANCE - UCB PERFECT SCHEDULE GENERATOR\n");
        textArea2.append("(C) Copyright 2002 Patrick Shyu\n\n");
        textArea2.append("DESCRIPTION: Generates course schedules and schedule charts.  Minimizes schedule gaps.\n");
        textArea2.append("\nINSTRUCTIONS:\n(1) Add/edit classes with the above two forms.\n");
        textArea2.append("(2) \"Close the Distance\" to generate the top 10 schedules");
        textArea2.append("\n(3) Click buttons 1-10 (invisible) to compare schedule-sets.\n(4) \"Save\" and \"Load\" to maintain input and generated schedules.\n(5) Click \"Pretty HTML\" (invisible) to see the schedule in HTML with LOC/CCN added in (LOC/CCN are calculated based on TERM, TITLE, and TIME)\n(6)For most users, this is all you need to know!  (Click the top \"instructions\" link for tips and advanced features.)\n");
        textArea2.insert("", 0);
        repaint();
    }

    public String readURL(String fileName) {
        String text = "";
        URL url;
        try {
            url = new URL(getCodeBase(), "fdFiles/" + fileName);
            try {
                URLConnection con = url.openConnection();
                String inputLine = "";
                StringBuffer buf = new StringBuffer("");
                DataInputStream in = new DataInputStream(new BufferedInputStream(con.getInputStream()));
                while ((inputLine = in.readLine()) != null) {
                    buf.append(inputLine + "\n");
                }
                text = buf.toString();
            } catch (IOException erre) {
                printSign("Error loading.");
            }
        } catch (MalformedURLException er) {
        }
        return text;
    }

    public void writeURL(String fileName, int which) {
        if (rePrintAnswer) printAnswer();
        try {
            URL url = new URL(getCodeBase().toString() + "fdWrite.cgi");
            URLConnection con = url.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            String content = "name=" + URLEncoder.encode(fileName) + "&text=" + URLEncoder.encode(which == 1 ? textArea1.getText() : textArea2.getText());
            out.writeBytes(content);
            out.flush();
            out.close();
            DataInputStream in = new DataInputStream(con.getInputStream());
            String s;
            while ((s = in.readLine()) != null) {
            }
            in.close();
        } catch (IOException err) {
        }
    }

    public void absorb(String text) {
        if (text != "" && text.charAt(0) != '#') return;
        for (int m = 0; m < 10; m++) {
            answer[m] = new answerEntry();
            answer[m].classes = new entry[30];
            for (int j = 0; j < 30; j++) answer[m].classes[j] = new entry();
            answer[m].gap = -1;
            for (int n = 0; n < 30; n++) {
                answer[m].classes[n].time = new Time[4];
                for (int o = 0; o < 4; o++) {
                    answer[m].classes[n].time[o] = new Time();
                }
            }
        }
        StringTokenizer st = new StringTokenizer(text, "\n\r");
        int enterIndex = -1;
        char first;
        String line = "";
        clss = -1;
        while (st.hasMoreTokens()) {
            line = st.nextToken().toString();
            first = line.charAt(0);
            if (first >= '1' && first <= '9') {
                nextTime = -1;
                String entry;
                String temp;
                int index, fromTime, toTime;
                nextTime++;
                StringTokenizer timeST = new StringTokenizer(line);
                temp = timeST.nextToken().toString();
                entry = "";
                index = 0;
                while (temp.charAt(index) != '-') {
                    entry += temp.charAt(index);
                    index++;
                }
                try {
                    fromTime = Integer.parseInt(entry);
                } catch (NumberFormatException z) {
                    break;
                }
                index++;
                entry = "";
                while (temp.charAt(index) >= '0' && temp.charAt(index) <= '9') {
                    entry += temp.charAt(index);
                    index++;
                }
                try {
                    toTime = Integer.parseInt(entry);
                } catch (NumberFormatException z) {
                    break;
                }
                if (temp.charAt(index) == 'a' || temp.charAt(index) == 'A') {
                } else {
                    if (isLesse(fromTime, toTime) && !timeEq(toTime, 1200)) {
                        if (String.valueOf(fromTime).length() == 4 || String.valueOf(fromTime).length() == 3) {
                            fromTime += 1200;
                        } else fromTime += 12;
                    }
                    if (!timeEq(toTime, 1200)) {
                        if (String.valueOf(toTime).length() == 4 || String.valueOf(toTime).length() == 3) {
                            toTime += 1200;
                        } else toTime += 12;
                    }
                }
                if (String.valueOf(fromTime).length() == 2 || String.valueOf(fromTime).length() == 1) fromTime *= 100;
                if (String.valueOf(toTime).length() == 2 || String.valueOf(toTime).length() == 1) toTime *= 100;
                answer[enterIndex].classes[clss].time[0].from = fromTime;
                answer[enterIndex].classes[clss].time[0].to = toTime;
                String days = "";
                days = timeST.nextToken().toString();
                if (days.indexOf("M") != -1 || days.indexOf("m") != -1) answer[enterIndex].classes[clss].time[0].m = 1;
                if (days.indexOf("TU") != -1 || days.indexOf("Tu") != -1 || days.indexOf("tu") != -1) answer[enterIndex].classes[clss].time[0].tu = 1;
                if (days.indexOf("W") != -1 || days.indexOf("w") != -1) answer[enterIndex].classes[clss].time[0].w = 1;
                if (days.indexOf("TH") != -1 || days.indexOf("Th") != -1 || days.indexOf("th") != -1) answer[enterIndex].classes[clss].time[0].th = 1;
                if (days.indexOf("F") != -1 || days.indexOf("f") != -1) answer[enterIndex].classes[clss].time[0].f = 1;
            } else if (line.length() > 3 && line.substring(0, 4).equals("gap:")) {
                answer[enterIndex].gap = Integer.parseInt(line.substring(4, line.indexOf(' ')));
            } else if (line.length() < 4 && first == '#') {
                enterIndex++;
                clss = -1;
            } else if (line.length() > 4 && line.substring(0, 5).equals("=====")) continue; else if (line.length() > 5 && line.substring(0, 6).equals("------")) continue; else {
                clss++;
                timeEntry = -1;
                answer[enterIndex].classes[clss].name = line;
            }
        }
    }

    public void printSign(String sign) {
        Graphics g = getGraphics();
        g.setColor(new Color(233, 238, 242));
        g.fillRect(300, 700 + SHIFT, 150, 100);
        g.setColor(Color.black);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString(sign, 340, 755 + SHIFT);
        publicSign = sign;
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
        if (e.getSource() == lst) {
            char ch = e.getKeyChar();
            Character c = new Character(ch);
            if (!Character.isLetterOrDigit(ch)) {
                return;
            }
            int ind = 0;
            if (lst.getSelectedItem().toString().toUpperCase().startsWith(c.toString().toUpperCase())) {
                ind = lst.getSelectedIndex() + 1;
            }
            boolean matchfound = false;
            for (int i = ind; i < lst.getItemCount(); i++) {
                if (lst.getItem(i).toString().toUpperCase().startsWith(c.toString().toUpperCase())) {
                    matchfound = true;
                    lst.select(i);
                    break;
                }
            }
            if (!matchfound) {
                for (int i = 0; i < lst.getItemCount(); i++) {
                    if (lst.getItem(i).toString().toUpperCase().startsWith(c.toString().toUpperCase())) {
                        lst.select(i);
                        break;
                    }
                }
            }
        }
    }

    public void stop() {
        t = null;
    }

    public String parseData(String text, boolean mustSep) {
        String[] types = new String[10];
        int[] typesCount = new int[10];
        int indexA = 0, indexB = 0;
        int limit = 0;
        String passage = "";
        String output = "";
        for (int i = 0; i < typesCount.length; i++) typesCount[i] = -1;
        for (int i = 0; i < types.length; i++) types[i] = "";
        while (indexA != -1) {
            indexA = text.indexOf("Course:", indexB);
            if (indexA == -1) break;
            indexA = text.indexOf("<B>", indexA);
            indexA += 3;
            indexB = text.indexOf("<", indexA);
            limit = text.indexOf("Enrollment on", indexA);
            passage = text.substring(indexA, limit);
            if (passage.indexOf("CANCELLED") != -1 || passage.indexOf("NOT OPEN") != -1) continue;
            String name = text.substring(indexA, indexB);
            int nameEnd = 0;
            int currentI = 0, backCurrent = 0, deltaCurrent = 0;
            while (!(name.charAt(currentI) >= '0' && name.charAt(currentI) <= '9')) {
                if (name.charAt(currentI) == ' ') {
                    backCurrent += deltaCurrent;
                    deltaCurrent = 0;
                }
                deltaCurrent++;
                currentI++;
            }
            backCurrent++;
            String numIn = "";
            int numEnd = 0;
            numEnd = name.indexOf(" ", backCurrent);
            numIn = name.substring(backCurrent, numEnd);
            if (!numIn.equals(courseNum.getText().toUpperCase())) continue;
            String section = "";
            if (mustSep) {
                String txt = name.substring(numEnd + 1, name.length());
                int i = 0, j;
                while (txt.charAt(i) != ' ') i++;
                i++;
                j = i + 1;
                while (txt.charAt(j) != ' ') j++;
                if (txt.indexOf("LEC") == -1) {
                    section = "(" + txt.substring(i, i + 1) + ") ";
                } else section = "(" + txt.substring(j - 1, j) + ") ";
            }
            String tag = name.substring(name.length() - 4, name.length());
            name = name.substring(0, backCurrent) + numIn + " " + section + tag;
            int index = -1;
            int written = 0;
            String time = "";
            int timeA, timeB;
            timeA = text.indexOf("Location", indexA);
            timeA += 46;
            timeB = text.indexOf(",", timeA);
            time = (text.substring(timeA, timeB));
            if (time.length() > 40 || time.length() == 3) continue;
            int cut = 0;
            for (int i = 0; i < time.length(); i++) if (time.charAt(i) == ' ') cut = i;
            time = time.substring(cut + 1, time.length()) + " " + time.substring(0, cut);
            if (time.substring(time.length() - 5, time.length()).equals("MTWTF")) {
                time = time.substring(0, time.length() - 5) + "MTuWThF";
            }
            for (int i = 0; i < 10; i++) {
                if ((types[i].length() >= name.length()) && types[i].substring(0, name.length()).equals(name)) {
                    int a = 1, b = 1;
                    int write = 1;
                    String given = "";
                    while (a != 0) {
                        a = types[i].indexOf("\n", b) + 1;
                        b = types[i].indexOf("\n", a);
                        if (b == -1) break;
                        given = types[i].substring(a, b);
                        if (time.equals(given) && merge.getState() == false) {
                            write = 0;
                        }
                    }
                    if (write == 1) {
                        typesCount[i]++;
                        types[i] += (time + "\n");
                    }
                    written = 1;
                }
            }
            if (written == 0) {
                int writeIndex = 0;
                for (int i = 0; i < 10; i++) if (types[i] == "") writeIndex = i;
                types[writeIndex] = name + "\n" + time + "\n";
                typesCount[writeIndex] = 1;
            }
        }
        boolean skip = false;
        if (merge.getState()) {
            int lab = -1, dis = -1;
            String prefix = "";
            for (int p = 0; p < 10; p++) {
                lab = -1;
                dis = -1;
                if (types[p].indexOf("MIX") == -1 && (types[p].indexOf("LAB") != -1 || types[p].indexOf("DIS") != -1)) ; else continue;
                if (!skip) {
                    prefix = types[p].trim().substring(0, types[p].indexOf("\n"));
                    prefix = prefix.substring(0, prefix.length() - 4);
                    lab = -1;
                    dis = -1;
                    for (int i = 0; i < 10; i++) {
                        if (types[i].indexOf(prefix) != -1) {
                            if (types[i].indexOf("LAB") != -1) lab = i;
                            if (types[i].indexOf("DIS") != -1) dis = i; else if (types[i].indexOf("DEM") != -1) dis = i;
                        }
                    }
                    if (lab == -1 || dis == -1) {
                        badInput = true;
                        rePrintAnswer = true;
                        textArea2.setText("Error: Lab/Dis unbindable.  (Manually bind it: use commas to place each LAB next to each DIS.  The first LAB corresponds to the first DIS, second LAB to second DIS, and so forth...)");
                        skip = true;
                    }
                    if (!skip) {
                        StringTokenizer a = new StringTokenizer(types[lab], "\n");
                        StringTokenizer b = new StringTokenizer(types[dis], "\n");
                        String inLine = "";
                        String inPiece1 = "", inPiece2 = "";
                        String tokenIn = "";
                        boolean xOut;
                        inLine += a.nextToken() + "/" + b.nextToken() + "MIX\n";
                        while (a.hasMoreTokens()) {
                            if (b.hasMoreTokens() == false) {
                                rePrintAnswer = true;
                                badInput = true;
                                textArea2.setText("Error: Lab/Dis unbindable.  (Manually bind it: use commas to place each LAB next to each DIS.  The first LAB corresponds to the first DIS, second LAB to second DIS, and so forth...)");
                                skip = true;
                            }
                            if (skip) break; else {
                                inPiece1 = a.nextToken();
                                inPiece2 = b.nextToken();
                                StringTokenizer z = new StringTokenizer(inLine, "\n");
                                z.nextToken();
                                xOut = false;
                                while (z.hasMoreTokens()) {
                                    tokenIn = z.nextToken();
                                    if (tokenIn.indexOf(inPiece1) != -1 && tokenIn.indexOf(inPiece2) != -1) xOut = true;
                                }
                                if (!xOut) inLine += inPiece1 + ", " + inPiece2 + "\n";
                            }
                        }
                        if (!skip) {
                            types[lab] = inLine;
                            types[dis] = "";
                            typesCount[dis] = 0;
                        }
                    }
                }
            }
        }
        output += "\n";
        for (int i = 0; i < 10; i++) {
            if (types[i] != "") output += types[i] + "\n";
        }
        if (!mustSep) {
            boolean combineLecs = false;
            for (int i = 0; i < 10; i++) {
                String val = nameSub(types[i]);
                if (val.equals("LEC")) {
                    if (typesCount[i] > 1 && sectionBind.getState() == true) combineLecs = true;
                }
            }
            int numTypes = 0;
            if (combineLecs) {
                for (int i = 0; i < 10; i++) {
                    if (typesCount[i] > 1) numTypes++;
                }
            }
            if (numTypes > 1) {
                return "-1";
            }
        } else {
            String nameline, orline = "OR: ";
            for (int i = 0; i < types.length; i++) {
                if (types[i].length() > 4) {
                    if (types[i].indexOf(")") != -1) nameline = types[i].substring(0, types[i].indexOf(")") + 1); else nameline = types[i].trim().substring(0, types[i].indexOf("\n") - 4);
                    if (orline.indexOf(nameline) == -1) orline += nameline + ", ";
                }
            }
            orline = orline.substring(0, orline.length() - 2);
            output = "\n" + orline + "\n" + output;
        }
        rePrintAnswer = true;
        if (!skip) textArea2.setText("Completed."); else textArea2.setText("Warning: Lab/Dis unbindable.  (Manually bind it: use commas to place each LAB next to each DIS.  The first LAB corresponds to the first DIS, second LAB to second DIS, and so forth...)  Not binded.");
        return output;
    }

    public void actionPerformed(ActionEvent e) {
        String line, days;
        String oldType, newType;
        String dept = "";
        buttonPressed = true;
        char first;
        int caretIndex;
        int tempIndex;
        int oldDisplayNum = displayNum;
        for (int i = 0; i < 10; i++) {
            if (e.getSource() == imageButtons[i]) {
                if (rePrintAnswer) printAnswer();
                print.setVisible(true);
                selectTerm.setVisible(true);
                displayNum = i;
                textArea2.setCaretPosition(textArea2.getText().length() - 1);
                caretIndex = textArea2.getText().indexOf("#" + (i + 1));
                if (caretIndex != -1) textArea2.setCaretPosition(caretIndex);
                repaint();
            }
        }
        if (e.getSource() == print) {
            if (textArea2.getText().charAt(0) != '#') printAnswer();
            String data = textArea2.getText();
            int start = data.indexOf("#" + (displayNum + 1));
            start = data.indexOf("\n", start);
            start++;
            int end = data.indexOf("\n---------", start);
            data = data.substring(start, end);
            String tr = "";
            if (term.getSelectedItem() == "Spring") tr = "SP"; else if (term.getSelectedItem() == "Summer") tr = "SU"; else tr = "FL";
            String s = getCodeBase().toString() + "schedule.cgi?term=" + tr + "&data=" + URLEncoder.encode(data);
            try {
                AppletContext a = getAppletContext();
                URL u = new URL(s);
                a.showDocument(u, "_blank");
            } catch (MalformedURLException rea) {
            }
        }
        if (e.getSource() == webSite) {
            String tr;
            if (term.getSelectedItem() == "Spring") tr = "SP"; else if (term.getSelectedItem() == "Summer") tr = "SU"; else tr = "FL";
            String num = courseNum.getText().toUpperCase();
            String s = "http://sis450.berkeley.edu:4200/OSOC/osoc?p_term=" + tr + "&p_deptname=" + URLEncoder.encode(lst.getSelectedItem().toString()) + "&p_course=" + num;
            try {
                AppletContext a = getAppletContext();
                URL u = new URL(s);
                a.showDocument(u, "_blank");
            } catch (MalformedURLException rea) {
            }
        }
        if (e.getSource() == loadButton) {
            printSign("Loading...");
            String fileName = idField.getText();
            fileName = fileName.replace(' ', '_');
            String text = readURL(fileName);
            if (!publicSign.equals("Error loading.")) {
                textArea1.setText(text);
                fileName += ".2";
                text = readURL(fileName);
                absorb(text);
                printAnswer();
                for (int i = 0; i < 10; i++) {
                    if (answer[i].gap != -1 && answer[i].gap != 9999 && answer[i].gap != 10000) {
                        imageButtons[i].setVisible(true);
                    } else imageButtons[i].setVisible(false);
                }
                if (!imageButtons[0].isVisible()) {
                    print.setVisible(false);
                    selectTerm.setVisible(false);
                } else {
                    print.setVisible(true);
                    selectTerm.setVisible(true);
                }
                printSign("Load complete.");
            }
            displayNum = 0;
            repaint();
        }
        if (e.getSource() == saveButton) {
            String fileName = idField.getText();
            fileName = fileName.replace(' ', '_');
            printSign("Saving...");
            writeURL(fileName, 1);
            printSign("Saving......");
            fileName += ".2";
            writeURL(fileName, 2);
            printSign("Save complete.");
        }
        if (e.getSource() == instructions) {
            showInstructions();
        }
        if (e.getSource() == net) {
            drawWarning = false;
            String inputLine = "";
            String text = "";
            String out;
            String urlIn = "";
            textArea2.setText("Retrieving Data...");
            try {
                String tr;
                if (term.getSelectedItem() == "Spring") tr = "SP"; else if (term.getSelectedItem() == "Summer") tr = "SU"; else tr = "FL";
                String num = courseNum.getText().toUpperCase();
                dept = lst.getSelectedItem().toString();
                {
                    urlIn = "http://sis450.berkeley.edu:4200/OSOC/osoc?p_term=" + tr + "&p_deptname=" + URLEncoder.encode(dept) + "&p_course=" + num;
                    try {
                        URL url = new URL(getCodeBase().toString() + "getURL.cgi");
                        URLConnection con = url.openConnection();
                        con.setDoOutput(true);
                        con.setDoInput(true);
                        con.setUseCaches(false);
                        con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                        DataOutputStream out2 = new DataOutputStream(con.getOutputStream());
                        String content = "url=" + URLEncoder.encode(urlIn);
                        out2.writeBytes(content);
                        out2.flush();
                        DataInputStream in = new DataInputStream(con.getInputStream());
                        String s;
                        while ((s = in.readLine()) != null) {
                        }
                        in.close();
                    } catch (IOException err) {
                    }
                }
                URL yahoo = new URL(this.getCodeBase(), "classData.txt");
                URLConnection yc = yahoo.openConnection();
                StringBuffer buf = new StringBuffer("");
                DataInputStream in = new DataInputStream(new BufferedInputStream(yc.getInputStream()));
                while ((inputLine = in.readLine()) != null) {
                    buf.append(inputLine);
                }
                text = buf.toString();
                in.close();
            } catch (IOException errr) {
            }
            String inText = (parseData(text, false));
            if (inText.equals("-1")) inText = parseData(text, true);
            if (inText.equals("\n")) {
                textArea2.append("\nNO DATA FOUND \n(" + urlIn + ")");
            } else textArea1.append(inText);
            repaint();
        }
        badInput = false;
        if (e.getSource() == button1) {
            if (t != null && t.isAlive()) {
                t.stop();
                epilogue();
                return;
            }
            displayNum = 0;
            textArea2.setCaretPosition(0);
            for (int i = 0; i < 30; i++) for (int j = 0; j < 20; j++) {
                matrix[i][j] = new entry();
                matrix[i][j].time = new Time[4];
                for (int k = 0; k < 4; k++) {
                    matrix[i][j].time[k] = new Time();
                    matrix[i][j].time[k].from = 0;
                }
            }
            val = new entry[30];
            for (int i = 0; i < 30; i++) {
                val[i] = new entry();
                val[i].time = new Time[4];
                for (int j = 0; j < 4; j++) {
                    val[i].time[j] = new Time();
                    val[i].time[j].from = 0;
                }
            }
            oldPercentDone = -5;
            oldAmountDone = -1 * PRINTINTERVAL;
            percentDone = 0;
            amountDone = 0;
            drawWarning = false;
            errorMessage = "";
            String text1 = textArea1.getText();
            if (text1.toUpperCase().indexOf("OR:") == -1) containsOR = false; else containsOR = true;
            text1 = removeOR(text1.toUpperCase());
            StringTokenizer st = new StringTokenizer(text1, "\n");
            clss = -1;
            timeEntry = -1;
            boolean noTimesListed = false;
            while (st.hasMoreTokens()) {
                line = st.nextToken().toString();
                if (line.equals("")) break; else first = line.charAt(0);
                if (first == '0') {
                    badInput = true;
                    repaint();
                    break;
                }
                if (first >= '1' && first <= '9') {
                    noTimesListed = false;
                    timeEntry++;
                    if (timeEntry == 30) {
                        rePrintAnswer = true;
                        textArea2.setText("Error: Exceeded 30 time entries per class.");
                        badInput = true;
                        repaint();
                        return;
                    }
                    nextTime = -1;
                    StringTokenizer andST = new StringTokenizer(line, ",");
                    while (andST.hasMoreTokens()) {
                        String temp;
                        String entry;
                        int index, fromTime, toTime;
                        nextTime++;
                        if (nextTime == 4) {
                            rePrintAnswer = true;
                            textArea2.setText("Error: Exceeded 4 time intervals per entry!");
                            badInput = true;
                            repaint();
                            return;
                        }
                        StringTokenizer timeST = new StringTokenizer(andST.nextToken());
                        temp = timeST.nextToken().toString();
                        entry = "";
                        index = 0;
                        if (temp.equals("")) break;
                        while (temp.charAt(index) != '-') {
                            entry += temp.charAt(index);
                            index++;
                            if (index >= temp.length()) {
                                rePrintAnswer = true;
                                textArea2.setText("Error: There should be no space before hyphens.");
                                badInput = true;
                                repaint();
                                return;
                            }
                        }
                        try {
                            fromTime = Integer.parseInt(entry);
                        } catch (NumberFormatException re) {
                            rePrintAnswer = true;
                            textArea2.setText("Error: There should be no a/p sign after FROM_TIME.");
                            badInput = true;
                            repaint();
                            return;
                        }
                        index++;
                        entry = "";
                        if (index >= temp.length()) {
                            badInput = true;
                            repaint();
                            rePrintAnswer = true;
                            textArea2.setText("Error: am/pm sign missing??");
                            return;
                        }
                        while (temp.charAt(index) >= '0' && temp.charAt(index) <= '9') {
                            entry += temp.charAt(index);
                            index++;
                            if (index >= temp.length()) {
                                badInput = true;
                                repaint();
                                rePrintAnswer = true;
                                textArea2.setText("Error: am/pm sign missing??");
                                return;
                            }
                        }
                        toTime = Integer.parseInt(entry);
                        if (temp.charAt(index) == 'a' || temp.charAt(index) == 'A') {
                        } else {
                            if (isLesse(fromTime, toTime) && !timeEq(toTime, 1200)) {
                                if (String.valueOf(fromTime).length() == 4 || String.valueOf(fromTime).length() == 3) {
                                    fromTime += 1200;
                                } else fromTime += 12;
                            }
                            if (!timeEq(toTime, 1200)) {
                                if (String.valueOf(toTime).length() == 4 || String.valueOf(toTime).length() == 3) {
                                    toTime += 1200;
                                } else toTime += 12;
                            }
                        }
                        if (String.valueOf(fromTime).length() == 2 || String.valueOf(fromTime).length() == 1) fromTime *= 100;
                        if (String.valueOf(toTime).length() == 2 || String.valueOf(toTime).length() == 1) toTime *= 100;
                        matrix[timeEntry][clss].time[nextTime].from = fromTime;
                        matrix[timeEntry][clss].time[nextTime].to = toTime;
                        if (timeST.hasMoreTokens()) days = timeST.nextToken().toString(); else {
                            rePrintAnswer = true;
                            textArea2.setText("Error: days not specified?");
                            badInput = true;
                            repaint();
                            return;
                        }
                        if (days.equals("")) return;
                        if (days.indexOf("M") != -1 || days.indexOf("m") != -1) matrix[timeEntry][clss].time[nextTime].m = 1;
                        if (days.indexOf("TU") != -1 || days.indexOf("Tu") != -1 || days.indexOf("tu") != -1) matrix[timeEntry][clss].time[nextTime].tu = 1;
                        if (days.indexOf("W") != -1 || days.indexOf("w") != -1) matrix[timeEntry][clss].time[nextTime].w = 1;
                        if (days.indexOf("TH") != -1 || days.indexOf("Th") != -1 || days.indexOf("th") != -1) matrix[timeEntry][clss].time[nextTime].th = 1;
                        if (days.indexOf("F") != -1 || days.indexOf("f") != -1) matrix[timeEntry][clss].time[nextTime].f = 1;
                    }
                } else {
                    if (noTimesListed) clss--;
                    clss++;
                    if (clss == 20) {
                        rePrintAnswer = true;
                        textArea2.setText("Error: No more than 20 class entries!");
                        badInput = true;
                        repaint();
                        return;
                    }
                    timeEntry = -1;
                    line = line.trim();
                    for (int i = 0; i < 30; i++) matrix[i][clss].name = line;
                    noTimesListed = true;
                }
            }
            for (int i = 0; i < 30; i++) {
                for (int j = 0; j < 4; j++) {
                    val[i].time[j].from = 0;
                }
            }
            for (int i = 0; i < 10; i++) {
                beat10[i] = 10000;
                answer[i].gap = 10000;
                for (int j = 0; j < 30; j++) answer[i].classes[j].name = "";
            }
            time = 0;
            calcTotal = 0;
            int k = 0;
            calculateTotalPercent(0, "\n");
            amountToReach = calcTotal;
            button1.setLabel("...HALT GENERATION...");
            printWarn();
            if (t != null && t.isAlive()) t.stop();
            t = new Thread(this, "Generator");
            t.start();
        }
    }

    String nameSub(String val) {
        if (val == "") return "";
        int n = val.indexOf("\n");
        String name = val.substring(0, n);
        return name.substring(name.length() - 3, name.length());
    }

    boolean timeEq(int a, int b) {
        if (a / 100 == 0) a *= 100;
        a /= 100;
        b /= 100;
        return a == b;
    }

    boolean isLesse(int a, int b) {
        if (a / 100 == 0) {
            a = a * 100;
        }
        if (b / 100 == 0) {
            b *= 100;
        }
        return (a < b);
    }

    void sortAnswer() {
        answerEntry temp = new answerEntry();
        for (int i = 0; i < answer.length; i++) {
            for (int j = 0; j < i; j++) {
                if (answer[i].gap < answer[j].gap) {
                    temp = answer[i];
                    answer[i] = answer[j];
                    answer[j] = temp;
                }
            }
        }
    }

    void printAnswer() {
        rePrintAnswer = false;
        textArea2.setText("");
        for (int i = 0; i < 10; i++) {
            if (answer[i].gap != -1 && answer[i].gap != 9999 && answer[i].gap != 10000) {
                textArea2.append("#" + (i + 1) + "\n");
                output(answer[i].classes);
                textArea2.append(textArea2append);
                textArea2append = "";
                textArea2.append("gap:" + answer[i].gap + " minutes. \n");
                textArea2.append("============================\n\n");
            }
        }
        textArea2.append("DONE");
    }

    int createChart(int[][] week, entry[] classes, String[][] weekNames) {
        int fillIndex, endIndex;
        int total = 0;
        boolean count;
        int gapTime;
        for (int i = 0; i < 91; i++) {
            for (int j = 0; j < 5; j++) {
                week[i][j] = 0;
                weekNames[i][j] = "";
            }
        }
        for (int i = 0; i < classes.length; i++) {
            for (int k = 0; k < 4; k++) {
                if (classes[i].time[k].m == 1) {
                    if (classes[i].time[k].from != 0) {
                        fillIndex = timeConvert(classes[i].time[k].from);
                        endIndex = timeConvert(classes[i].time[k].to);
                        if (fillIndex < 0 || fillIndex > week.length || endIndex < 0 || endIndex > week.length) {
                            errorMessage = "Error: time out of range??";
                            badInput = true;
                            repaint();
                            return -1;
                        }
                        while (fillIndex != endIndex) {
                            if (week[fillIndex][0] == 0) {
                                week[fillIndex][0] = 1;
                                weekNames[fillIndex][0] = classes[i].name;
                            } else return 9999;
                            fillIndex++;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < classes.length; i++) {
            for (int k = 0; k < 4; k++) {
                if (classes[i].time[k].tu == 1) {
                    if (classes[i].time[k].from != 0) {
                        fillIndex = timeConvert(classes[i].time[k].from);
                        endIndex = timeConvert(classes[i].time[k].to);
                        if (fillIndex < 0 || fillIndex > week.length || endIndex < 0 || endIndex > week.length) {
                            errorMessage = "Error: time out of range??";
                            badInput = true;
                            repaint();
                            return -1;
                        }
                        while (fillIndex != endIndex) {
                            if (week[fillIndex][1] == 0) {
                                week[fillIndex][1] = 1;
                                weekNames[fillIndex][1] = classes[i].name;
                            } else return 9999;
                            fillIndex++;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < classes.length; i++) {
            for (int k = 0; k < 4; k++) {
                if (classes[i].time[k].w == 1) {
                    if (classes[i].time[k].from != 0) {
                        fillIndex = timeConvert(classes[i].time[k].from);
                        endIndex = timeConvert(classes[i].time[k].to);
                        if (fillIndex < 0 || fillIndex > week.length || endIndex < 0 || endIndex > week.length) {
                            errorMessage = "Error: time out of range??";
                            badInput = true;
                            repaint();
                            return -1;
                        }
                        while (fillIndex != endIndex) {
                            if (week[fillIndex][2] == 0) {
                                week[fillIndex][2] = 1;
                                weekNames[fillIndex][2] = classes[i].name;
                            } else return 9999;
                            fillIndex++;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < classes.length; i++) {
            for (int k = 0; k < 4; k++) {
                if (classes[i].time[k].th == 1) {
                    if (classes[i].time[k].from != 0) {
                        fillIndex = timeConvert(classes[i].time[k].from);
                        endIndex = timeConvert(classes[i].time[k].to);
                        if (fillIndex < 0 || fillIndex > week.length || endIndex < 0 || endIndex > week.length) {
                            errorMessage = "Error: time out of range??";
                            badInput = true;
                            repaint();
                            return -1;
                        }
                        while (fillIndex != endIndex) {
                            if (week[fillIndex][3] == 0) {
                                week[fillIndex][3] = 1;
                                weekNames[fillIndex][3] = classes[i].name;
                            } else return 9999;
                            fillIndex++;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < classes.length; i++) {
            for (int k = 0; k < 4; k++) {
                if (classes[i].time[k].f == 1) {
                    if (classes[i].time[k].from != 0) {
                        fillIndex = timeConvert(classes[i].time[k].from);
                        endIndex = timeConvert(classes[i].time[k].to);
                        if (fillIndex < 0 || fillIndex > week.length || endIndex < 0 || endIndex > week.length) {
                            errorMessage = "Error: time out of range??";
                            badInput = true;
                            repaint();
                            return -1;
                        }
                        while (fillIndex != endIndex) {
                            if (week[fillIndex][4] == 0) {
                                week[fillIndex][4] = 1;
                                weekNames[fillIndex][4] = classes[i].name;
                            } else return 9999;
                            fillIndex++;
                        }
                    }
                }
            }
        }
        return -1;
    }

    int calcGap(entry[] classes) {
        int[][] week = new int[91][5];
        int fillIndex, endIndex;
        int total = 0;
        boolean count;
        int gapTime;
        String[][] filler = new String[91][5];
        int returnValue = createChart(week, classes, filler);
        if (returnValue > 0) return returnValue;
        for (int i = 0; i < 5; i++) {
            count = false;
            gapTime = 0;
            for (int j = 0; j < 91; j++) {
                if (week[j][i] == 1 && count) {
                    total += gapTime;
                    gapTime = 0;
                }
                if (week[j][i] == 1) count = true;
                if (week[j][i] == 0 && count) gapTime += 1;
            }
        }
        return total * 10;
    }

    int timeConvert(int t) {
        return ((t - 700) / 100) * 6 + ((t - 700) % 100) / 10;
    }

    int timeReverse(int t) {
        int time = (t / 6) * 100 + 700 + 10 * (t % 6);
        if (t >= 36) time -= 1200;
        return time;
    }

    boolean goodToGo(String name, entry[] val) {
        if (name.equals("")) return false;
        int inIndex = -1;
        int i = 0, j = 0, z = 0, w = 0;
        i = 0;
        while (i != 5 && !orBox[i][0].equals("NOT IN ORBOX")) {
            j = 0;
            while (j != 5 && !orBox[i][j].equals("NOT IN ORBOX")) {
                if (name.indexOf(orBox[i][j]) == 0) {
                    inIndex = i;
                    for (z = 0; z < val.length; z++) {
                        w = 0;
                        while (w != 5 && !orBox[inIndex][w].equals("NOT IN ORBOX")) {
                            if (val[z].name != null && val[z].name.indexOf(orBox[inIndex][w]) == 0 && name.indexOf(orBox[inIndex][w]) != 0) return false;
                            w++;
                        }
                    }
                }
                j++;
            }
            i++;
        }
        return true;
    }

    void resetVal(int index) {
        if (index < val.length && index > -1) {
            val[index].name = "N//A";
            for (int i = 0; i < 4; i++) {
                val[index].time[i].from = 0;
            }
        }
    }

    boolean uniqueAnswer() {
        String valText = "", answerText = "";
        output(val);
        valText = textArea2append;
        textArea2append = "";
        for (int i = 0; i < answer.length; i++) {
            output(answer[i].classes);
            answerText = textArea2append;
            textArea2append = "";
            if (answerText.equals(valText)) return false;
        }
        return true;
    }

    boolean hasEnough() {
        int mustMatchNo, indexA;
        int marked;
        int marked2, mustMark2;
        int x, y;
        String valText = "";
        boolean coveredByOR = false;
        boolean markedOnce;
        int totalMustMatch = 0;
        int totalMustMatched = 0;
        output(val);
        valText = "\n" + textArea2append;
        textArea2append = "";
        int i, j;
        int z2;
        int zx;
        i = 0;
        for (x = 0; x < 5; x++) {
            if (!orBox[x][0].equals("NOT IN ORBOX")) totalMustMatch++;
        }
        while (i != 5 && !orBox[i][0].equals("NOT IN ORBOX")) {
            markedOnce = false;
            marked = 0;
            j = 0;
            while (j != 5 && !orBox[i][j].equals("NOT IN ORBOX")) {
                if (valText.indexOf("\n" + orBox[i][j]) != -1) {
                    markedOnce = true;
                    coveredByOR = true;
                    mustMatchNo = 0;
                    for (int z = 0; z < matrix[0].length; z++) {
                        if (matrix[0][z].name != null && matrix[0][z].name.indexOf(orBox[i][j]) == 0) {
                            zx = 0;
                            while (matrix[0][z].time[zx++].from != 0) mustMatchNo++;
                        }
                    }
                    indexA = 0;
                    marked = 0;
                    while ((indexA = valText.indexOf("\n" + orBox[i][j], indexA) + 1) != 0) marked++;
                    if (marked == mustMatchNo) {
                        totalMustMatched++;
                        if (totalMustMatched == totalMustMatch) {
                            return true;
                        }
                    }
                }
                j++;
            }
            if (!markedOnce) {
                x = i + 1;
                while (x != 5 && !orBox[x][0].equals("NOT IN ORBOX")) {
                    y = 0;
                    while (y != 5 && !orBox[x][y].equals("NOT IN ORBOX")) {
                        marked2 = 0;
                        mustMark2 = 0;
                        z2 = 0;
                        while (z2 != 5 && !orBox[i][z2].equals("NOT IN ORBOX")) {
                            mustMark2++;
                            if (orBox[i][z2].indexOf(orBox[x][y]) == 0) marked2++;
                            z2++;
                        }
                        if (marked2 == mustMark2) return true;
                        y++;
                    }
                    x++;
                }
                return false;
            }
            i++;
        }
        if (coveredByOR) return false; else return true;
    }

    void loop(int col, int row, entry[] val) {
        boolean goodToWent = false;
        if (badInput) {
            epilogue();
            t.stop();
        }
        if (!containsOR || (row < 30 && goodToGo(matrix[row][col].name, val))) {
            goodToWent = true;
            val[col].name = matrix[row][col].name;
            for (int i = 0; i < 4; i++) {
                copyTime(val[col].time[i], matrix[row][col].time[i]);
            }
            if (val[col].time[0].from == 0) val[col].name = "N//A";
        }
        if (badInput) {
            epilogue();
            t.stop();
        }
        boolean partOfOR = false;
        int p, q;
        p = 0;
        while (p != 5 && !orBox[p][0].equals("NOT IN ORBOX")) {
            q = 0;
            while (q != 5 && !orBox[p][q].equals("NOT IN ORBOX")) {
                if (row < 30 && matrix[row][col].time[0].from != 0 && matrix[row][col].name.indexOf(orBox[p][q]) == 0) {
                    partOfOR = true;
                }
                q++;
            }
            p++;
        }
        if (col + 1 < 20 && matrix[0][col + 1].time[0].from != 0) {
            if (!goodToWent && partOfOR) ; else loop(col + 1, 0, val);
        }
        if (col == 19 || (col + 1 < 20 && matrix[0][col + 1].time[0].from == 0)) {
            amountDone++;
            if (amountToReach != 0) percentDone = (amountDone * 100) / amountToReach; else percentDone = -1;
            if (!containsOR || (hasEnough() && !(!goodToWent && partOfOR))) {
                worstTime = answer[0].gap;
                worstIndex = 0;
                for (int x = 1; x < 10; x++) {
                    if (answer[x].gap > worstTime) {
                        worstIndex = x;
                        worstTime = answer[x].gap;
                    }
                }
                time = calcGap(val);
                if (time < worstTime) {
                    beat10[worstIndex] = time;
                    for (int i = 0; i < answer[worstIndex].classes.length; i++) {
                        answer[worstIndex].classes[i].name = val[i].name;
                        for (int k = 0; k < 4; k++) {
                            copyTime(answer[worstIndex].classes[i].time[k], val[i].time[k]);
                        }
                    }
                    answer[worstIndex].gap = time;
                }
            }
        }
        printWarn();
        if (row + 1 == 30 || (row + 1 < 30 && matrix[row][col].time[0].from != 0)) {
            if (row + 1 < 30 && matrix[row + 1][col].time[0].from == 0) {
                if (containsOR && partOfOR) {
                    loop(col, row + 1, val);
                    resetVal(col);
                }
            } else {
                loop(col, row + 1, val);
                resetVal(col);
            }
        }
    }

    int calculateTotalPercent0() {
        int total = 1;
        int subTotal = 0;
        int inIndex = -1;
        boolean increment1;
        for (int i = 0; i < matrix[0].length; i++) {
            subTotal = 0;
            for (int j = 0; j < matrix.length; j++) {
                if (matrix[j][i].time[0].from != 0) subTotal++;
            }
            increment1 = false;
            for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) if (matrix[0][i].name != null && matrix[0][i].name.indexOf(orBox[x][y]) == 0) {
                increment1 = true;
                inIndex = x;
            }
            if (increment1) subTotal++;
            if (subTotal != 0) total *= subTotal;
            if (increment1) {
                int p, q;
                boolean found = false;
                p = 0;
                while (!found && p != 5 && !orBox[p][0].equals("NOT IN ORBOX")) {
                    q = 0;
                    while (!found && q != 5 && !orBox[p][q].equals("NOT IN ORBOX")) {
                        for (int u = 0; !found && u < i; u++) {
                            if (matrix[0][u].name != null && matrix[0][u].name.indexOf(orBox[p][q]) == 0 && matrix[0][u].time[0].from != 0 && matrix[0][i].name.indexOf(orBox[p][q]) == -1) {
                                found = true;
                                int v;
                                int v2 = 0;
                                if (matrix[0][i + 1].time[0].from != 0) while (v2 != matrix.length && matrix[v2][u].time[0].from != 0) {
                                    v = 0;
                                    while (v != matrix.length && matrix[v][i].time[0].from != 0) {
                                        total--;
                                        v++;
                                    }
                                    v2++;
                                }
                            }
                        }
                        q++;
                    }
                    p++;
                }
            }
        }
        return total;
    }

    int calcTotal;

    void calculateTotalPercent(int col, String history) {
        if (col == matrix[0].length) return;
        boolean partOfOR = false, usedOR = false;
        int p, q;
        p = 0;
        while (p != 5 && !orBox[p][0].equals("NOT IN ORBOX")) {
            q = 0;
            while (q != 5 && !orBox[p][q].equals("NOT IN ORBOX")) {
                if (matrix[0][col].time[0].from != 0 && matrix[0][col].name.indexOf(orBox[p][q]) == 0) {
                    partOfOR = true;
                }
                q++;
            }
            p++;
        }
        boolean goodToWent = false;
        boolean leave = false;
        if (matrix[0][col].name == null) goodToWent = false; else if (matrix[0][col].name != null && matrix[0][col].name.equals("")) goodToWent = false; else {
            int inIndex = -1;
            int x = 0, y = 0, z = 0, w = 0;
            x = 0;
            while (x != 5 && !orBox[x][0].equals("NOT IN ORBOX")) {
                y = 0;
                while (y != 5 && !orBox[x][y].equals("NOT IN ORBOX")) {
                    if (matrix[0][col].name.indexOf(orBox[x][y]) == 0) {
                        inIndex = x;
                        w = 0;
                        while (w != 5 && !orBox[inIndex][w].equals("NOT IN ORBOX")) {
                            if (history.indexOf("\n" + orBox[inIndex][w]) != -1 && matrix[0][col].name.indexOf(orBox[inIndex][w]) != 0) {
                                leave = true;
                                goodToWent = false;
                            }
                            w++;
                        }
                    }
                    y++;
                }
                x++;
            }
            if (!leave) goodToWent = true;
        }
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i][col].time[0].from != 0 && (goodToWent || matrix[0][col + 1].time[0].from == 0)) {
                calculateTotalPercent(col + 1, history + "\n" + matrix[i][col].name);
            } else if (partOfOR && !usedOR) {
                calculateTotalPercent(col + 1, history);
                usedOR = true;
            }
        }
        if (col + 1 == matrix[0].length || matrix[0][col].time[0].from == 0) calcTotal++;
    }

    String dayOutputToString(Time time) {
        String string = "";
        if (time.m == 1) string += "M";
        if (time.tu == 1) string += "Tu";
        if (time.w == 1) string += "W";
        if (time.th == 1) string += "Th";
        if (time.f == 1) string += "F";
        return string;
    }

    void output(entry[] va) {
        int from = 0, to = 0;
        int j;
        for (int i = 0; i < 30; i++) {
            j = 0;
            while (va[i].time[j].from != 0) {
                textArea2append += va[i].name;
                textArea2append += '\n';
                if ((from = va[i].time[j].from) >= 1300) from = va[i].time[j].from - 1200;
                if ((to = va[i].time[j].to) >= 1300) to = va[i].time[j].to - 1200;
                textArea2append += from + "-" + to;
                if (va[i].time[j].to >= 1300) textArea2append += "p"; else textArea2append += "a";
                textArea2append += " " + dayOutputToString(va[i].time[j]) + "\n";
                j++;
            }
        }
        textArea2append += ("--------------------------\n");
    }

    public void paint(Graphics g) {
        offGraphics.setColor(new Color(233, 238, 242));
        offGraphics.fillRect(0, 0, 1000, 1000);
        render(offGraphics);
        g.drawImage(offImage, 0, 0, this);
    }

    public void updateImageButtons() {
        for (int i = 0; i < 10; i++) {
            if (answer[i].gap != -1 && answer[i].gap != 9999 && answer[i].gap != 10000) {
                imageButtons[i].setVisible(true);
            } else imageButtons[i].setVisible(false);
        }
    }

    public void update(Graphics g) {
        offGraphics.setColor(new Color(233, 238, 242));
        offGraphics.fillRect(0, 0, 1000, 1000);
        render(offGraphics);
        g.drawImage(offImage, 0, 0, this);
    }

    public void drawFeatures(Graphics g) {
        g.setColor(Color.black);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("[ Final Distance ]", 550, 210);
        g.setColor(Color.black);
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.drawString("(see About)", 710, 210);
        g.drawString("Minimizes schedule gaps", 550, 230);
        g.drawString("Handles class conflicts ", 550, 250);
        g.drawString("Top 10 schedule-sets in easy access fashion ", 550, 270);
        g.drawString("Color-coded visual display ", 550, 290);
        g.drawString("Active schedule-generation", 550, 310);
        g.drawString("Glamorous printable graph ", 550, 330);
        g.drawString("Trans Final Distance (CCN/LOC provided)", 550, 350);
        g.setColor(Color.red);
        g.drawString("Final Exam conflicts handled", 550, 370);
        g.setColor(Color.black);
        g.drawString("Load/Save ", 550, 390);
        g.drawString("Bindable LAB/DIS", 550, 410);
        g.drawString("100% customizability ", 550, 430);
        g.drawString("Ultimatum (OR feature)", 550, 450);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString("The only resource necessary for Telebears season! ", 550, 490);
    }

    public void render(Graphics g) {
        setBackground(new Color(233, 238, 242));
        g.setColor(Color.black);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString(publicSign, 340, 755 + SHIFT);
        g.setColor(new Color(233, 238, 242));
        g.fillRect(230, 220 + SHIFT, 100, 20);
        g.setColor(Color.white);
        g.fillRect(20, 25, 450, 190 + SHIFT);
        g.setColor(Color.black);
        g.drawRect(20, 25, 450 + 1, 190 + SHIFT + 1);
        Font values = new Font("Arial", Font.BOLD + Font.ITALIC, 20);
        Font values2 = new Font("Arial", Font.BOLD + Font.ITALIC, 12);
        g.setFont(values);
        g.setColor(Color.black);
        g.drawString("Final Distance: Perfect Schedule Generator", 20, 20);
        Font values3 = new Font("Arial", Font.BOLD, 13);
        g.setFont(values3);
        g.drawString("Course Number", cNumX, cNumY - 16);
        g.drawString("Class/Possible Times", 50, 240 + SHIFT);
        g.drawString("Automatic Class Entry Form", 50, 45);
        g.drawString("Season:", 360, 45);
        g.setColor(new Color(255, 0, 0));
        if (drawWarning) {
            g.drawString("WARNING: LEC may not match DIS/LAB...", 50, 223 + SHIFT);
        }
        g.setColor(new Color(0, 0, 0));
        Font values4 = new Font("Arial", Font.PLAIN, 11);
        g.setFont(values4);
        g.drawString("(R1A, 7a, 184)", cNumX, cNumY - 6);
        imager(this.answer[displayNum].classes, g);
        if (!buttonPressed) drawFeatures(g);
    }

    public void imager(entry[] classes, Graphics g) {
        int[][] week = new int[91][5];
        int left = 500;
        int top = 230;
        int width = 76;
        int height = 5;
        int color = 2;
        int highestColorUsed = 0;
        if (answer[0].gap != -1 && answer[0].gap != 9999 && answer[0].gap != 10000) {
            String[][] classNames = new String[91][5];
            createChart(week, classes, classNames);
            String n;
            boolean wrote = false;
            for (int i = 0; i < this.answer[this.displayNum].classes.length; i++) {
                if (answer[displayNum] != null && answer[displayNum].classes[i] != null) {
                    n = answer[displayNum].classes[i].name;
                    wrote = false;
                    for (int j = 0; j < week.length; j++) {
                        for (int k = 0; k < week[j].length; k++) {
                            if (classNames[j][k] != null && classNames[j][k].equals(n) && week[j][k] < 2) {
                                week[j][k] = color;
                                highestColorUsed = color;
                                wrote = true;
                            }
                        }
                    }
                    if (wrote) color++;
                }
            }
        }
        if (answer[0].gap != -1 && answer[0].gap != 9999 && answer[0].gap != 10000) {
            Font old = this.getFont();
            Font values3 = new Font("Arial", Font.BOLD, 13);
            g.setFont(values3);
            g.drawString("Display", 445, 473 + SHIFT);
            g.drawString("schedule", 445, 488 + SHIFT);
            Font values = new Font("Arial", Font.BOLD, 15);
            g.setFont(values);
            g.drawString("Schedule #" + (displayNum + 1), left + 50, top);
            g.drawString("Gap: " + answer[displayNum].gap + " min", left + 200, top);
            Font values2 = new Font("Arial", Font.BOLD, 12);
            g.setFont(values2);
            g.drawString("Legend:", left, top - 150);
            g.setFont(old);
        }
        int[] noPrint = new int[30];
        for (int i = 0; i < 30; i++) noPrint[i] = 0;
        for (int i = 0; i < answer[displayNum].classes.length - 1; i++) for (int j = i + 1; j < answer[displayNum].classes.length; j++) if (answer[displayNum].classes[i].name != null && answer[displayNum].classes[j].name != null) if (answer[displayNum].classes[i].name.equals(answer[displayNum].classes[j].name)) {
            noPrint[i] = 1;
        }
        Color[] colorTable = createColorTable(highestColorUsed);
        int goIndex = 0;
        int across = 60;
        int up = 150;
        String[] legends = new String[30];
        for (int i = 0; i < answer[displayNum].classes.length; i++) {
            if (noPrint[i] != 1) {
                if (goIndex > 9) {
                    across = 310;
                    up = 300;
                }
                if (answer[displayNum].classes[i].name != null && answer[displayNum].classes[i].time[0].from != 0 && answer[displayNum].gap != -1 && answer[displayNum].gap != 9999 && answer[displayNum].gap != 10000) {
                    g.setColor(colorTable[goIndex]);
                    g.drawString(answer[displayNum].classes[i].name, left + across, top - up + goIndex * 15);
                    legends[goIndex] = answer[displayNum].classes[i].name;
                    goIndex++;
                }
            }
        }
        top += 30;
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.setColor(Color.black);
        if (!buttonPressed) g.setColor(new Color(213, 218, 222));
        for (int i = 0; i < 91; i++) {
            if (i % 3 == 0) {
                g.drawLine(left + 60, top + i * height, left + width * 6, top + i * height);
                g.drawString("" + timeReverse(i), left + 15, top + i * height + 5);
            }
        }
        for (int i = 1; i < 7; i++) {
            g.drawLine(left + width * i, top, left + width * i, top + height * 90);
        }
        left += width;
        String note = "";
        for (int i = 0; i < week.length; i++) {
            for (int j = 0; j < week[i].length; j++) {
                if (week[i][j] > 0) {
                    g.setColor(colorTable[week[i][j] - 2]);
                    if (i != 0 && week[i - 1][j] > 0) note = legends[week[i - 1][j] - 2]; else note = "";
                    if (note.equals("") || !note.equals(legends[week[i][j] - 2])) g.fillRect(left + j * width + 1, 1 + top + i * height, width - 1, height - 1); else g.fillRect(left + 1 + j * width, top + i * height, width - 1, height);
                }
            }
        }
        Font tmp = g.getFont();
        g.setFont(new Font("Courier", Font.PLAIN, 14));
        for (int i = 0; i < week.length; i++) {
            for (int j = 0; j < week[i].length; j++) {
                if (week[i][j] > 0) {
                    if (i % 3 == 0) {
                        note = legends[week[i][j] - 2];
                        note = note.trim();
                        if (note.length() < 9) ; else note = note.substring(0, 4) + "~" + note.substring(note.length() - 3, note.length());
                        g.setColor(Color.black);
                        g.drawString(note, 2 + left + j * width + 3, top + i * height + 13);
                    }
                }
            }
        }
        left -= width;
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.setColor(Color.black);
        if (!buttonPressed) g.setColor(new Color(213, 218, 222));
        left += width;
        g.drawString("M", left + width / 3, top);
        g.drawString("Tu", left + width / 3 + width, top);
        g.drawString("W", left + width / 3 + width * 2, top);
        g.drawString("Th", left + width / 3 + width * 3, top);
        g.drawString("F", left + width / 3 + width * 4, top);
        left -= width;
    }

    private Color[] createColorTable(int numColors) {
        Color[] table = new Color[numColors];
        float hueMax = (float) 1;
        float sat = (float) 0.5;
        for (int i = 0; i < numColors; i++) {
            float hue = hueMax * i / (numColors);
            table[i] = Color.getHSBColor(hue, sat, (float) 0.95);
        }
        return table;
    }
}
