package nu.mine.tbje.easyEDT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.html.parser.ParserDelegator;

/***
 * Class responsable for the communication with the server
 */
public class Edt {

    String sessionId;

    String login;

    String pass;

    String displayCookie;

    private InputStream i;

    ArrayList<Node> trainee = new ArrayList<Node>();

    /***
	 * Sets the website username
	 * @param login
	 */
    public void setLogin(String login) {
        this.login = login;
    }

    /***
	 * Sets the website password
	 * @param pass
	 */
    public void setPass(String pass) {
        this.pass = pass;
    }

    /***
	 * Performs the login action
	 *
	 */
    public void doLogin() {
        String body = "login=" + login + "&password=" + pass + "&x=14&y=49";
        URL url;
        HttpURLConnection conn;
        try {
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/standard/index.jsp");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.getInputStream();
            String cookie = conn.getHeaderField("Set-Cookie");
            sessionId = cookie.split(";")[0];
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/standard/gui/interface.jsp");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Cookie", sessionId);
            OutputStream rawOutStream = conn.getOutputStream();
            PrintWriter pw = new PrintWriter(rawOutStream);
            pw.print(body);
            pw.flush();
            pw.close();
            i = conn.getInputStream();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public void open(String s_url) {
        URL url;
        HttpURLConnection conn;
        try {
            url = new URL(s_url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Cookie", sessionId);
            conn.getInputStream();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public List tree(String cat, int branch) {
        Pattern p = Pattern.compile("<a href=\"javascript:checkBranch\\(([0-9]+), 'true'\\)\">([^<]*)</a>");
        Matcher m;
        List res = new ArrayList();
        URL url;
        HttpURLConnection conn;
        System.out.println();
        try {
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/standard/gui/tree.jsp?category=trainee&expand=false&forceLoad=false&reload=false&scroll=0");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Cookie", sessionId);
            BufferedReader i = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = i.readLine()) != null) {
                m = p.matcher(line);
                if (m.find()) {
                    trainee.add(new Node(Integer.parseInt(m.group(1)), m.group(2)));
                    System.out.println(m.group(1) + " - " + m.group(2));
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return res;
    }

    private String createIdent(int level) {
        String res = "";
        while (level > 0) {
            res += "&nbsp;&nbsp;&nbsp;";
            level--;
        }
        return res;
    }

    public void explore(ArrayList<Node> nodes, int level) {
        Iterator<Node> i = nodes.iterator();
        while (i.hasNext()) {
            process(i.next().id, level);
        }
    }

    public void process(int branch, int level) {
        Pattern p1 = Pattern.compile("<DIV class=\"treeline\">([^<]*)");
        Pattern p = Pattern.compile("<a href=\"javascript:checkBranch\\(([0-9]+), 'true'\\)\">([^<]*)</a>");
        Matcher m, m1;
        URL url;
        HttpURLConnection conn;
        try {
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/standard/gui/tree.jsp?branchId=" + branch + "&expand=false&forceLoad=false&reload=false&scroll=0");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Cookie", sessionId);
            BufferedReader i = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            boolean doAdd = false;
            while ((line = i.readLine()) != null) {
                m1 = p1.matcher(line);
                m = p.matcher(line);
                if (m1.find()) {
                    if (m1.group(1).equals(createIdent(level))) {
                        doAdd = true;
                    } else {
                        doAdd = false;
                    }
                }
                if (m.find()) {
                    if (doAdd) {
                        trainee.add(new Node(Integer.parseInt(m.group(1)), m.group(2)));
                        System.out.println(m.group(1) + " - " + m.group(2));
                    }
                }
            }
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/standard/gui/tree.jsp?branchId=" + branch + "&expand=false&forceLoad=false&reload=false&scroll=0");
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public void doInit() throws Exception {
        if (sessionId == null) {
            throw new Exception("Not connected");
        }
        URL url;
        HttpURLConnection conn;
        try {
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/standard/projects.jsp");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Cookie", sessionId);
            i = conn.getInputStream();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public void setIdAndCat(String cat, int id) {
        URL url;
        HttpURLConnection conn;
        try {
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/standard/gui/tree.jsp?category=" + cat + "&selectId=" + id);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Cookie", sessionId);
            i = conn.getInputStream();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (ProtocolException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void setDisplayOptions() {
        URL url;
        HttpURLConnection conn;
        String cookie;
        try {
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/custom/modules/plannings/plannings.jsp?isClickable=true&showTabStage=true&showTabRooms=true&showTabDuration=true&displayConfId=1&showTabHour=true&showTabInstructors=true&showTabActivity=true&showPianoDays=true&y=&showTreeCategory8=true&x=&showTab=true&displayType=0&showTreeCategory7=true&showTreeCategory6=true&showTreeCategory5=true&showTreeCategory4=true&showTreeCategory3=true&showTreeCategory2=true&showTabTrainees=true&showTreeCategory1=true&showTabCategory8=true&showTabCategory7=true&showTabCategory6=true&showTabCategory5=true&showPianoWeeks=true&display=true&showTabDate=true&showLoad=false&showTabResources=true&changeOptions=true");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Cookie", sessionId);
            cookie = conn.getHeaderField("Set-Cookie");
            displayCookie = cookie.split(";")[0];
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (ProtocolException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void setWeek(int week) {
        week = week + 17;
        if (week > 52) {
            week = week - 52;
        }
        HttpURLConnection conn;
        URL url;
        try {
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/custom/modules/plannings/info.jsp?week=" + week + "&reset=true");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Cookie", sessionId);
            i = conn.getInputStream();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (ProtocolException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public InputStream getData() throws Exception {
        URL url;
        HttpURLConnection conn;
        InputStream is = null;
        try {
            if (sessionId == null) {
                throw new Exception("Not connected");
            }
            url = new URL("http://cri-srv-ade.insa-toulouse.fr:8080/ade/custom/modules/plannings/info.jsp");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Cookie", sessionId + "; " + displayCookie);
            is = conn.getInputStream();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return is;
    }

    public static List getClasses(Date d) {
        List res = new ArrayList<Class>();
        return res;
    }

    public static List parse(InputStream html) {
        ArrayList res = new ArrayList();
        try {
            HTMLCallbackHandler handler = new HTMLCallbackHandler();
            new ParserDelegator().parse(new InputStreamReader(html), handler, true);
            res = handler.listeCours;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
}
