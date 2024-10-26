package de.lastfm.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import de.lastfm.db.DB_Groups;
import de.lastfm.db.DB_User;
import de.lastfm.gui.ActionEventHandler;
import de.lastfm.gui.Gui;

/**
 * @author User
 * 
 */
public class Groups {

    /**
	 * 
	 * Get the groupname, user are Member
	 */
    static void getGroups(String username) {
        try {
            Gui.getBalken().setValue(85);
            Gui.getBalken().setString("crawling Groups");
            Gui.getBalken().paint(Gui.getBalken().getGraphics());
            URL url = new URL("http://www.lastfm.de/user/" + username + "/groups/");
            URLConnection con = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            HTMLEditorKit editorKit = new HTMLEditorKit();
            HTMLDocument htmlDoc = new HTMLDocument();
            htmlDoc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            editorKit.read(br, htmlDoc, 0);
            Vector a_tags = new Vector();
            HTMLDocument.Iterator iter1 = htmlDoc.getIterator(HTML.Tag.A);
            while (iter1.isValid()) {
                a_tags.add((String) iter1.getAttributes().getAttribute(HTML.Attribute.HREF));
                iter1.next();
            }
            Vector gruppen = new Vector();
            for (int i = 0; i < a_tags.size(); i++) {
                String element = (String) a_tags.get(i);
                if (!gruppen.contains(element)) {
                    if (element.contains("/group/")) gruppen.add(element);
                }
            }
            for (int a = 0; a < gruppen.size(); a++) {
                String gruppe = gruppen.elementAt(a).toString().substring(7);
                if (gruppe.contains("Last.fm+auf+Deutsch")) {
                    System.out.println("Auschalten Last.fm.auf.Deutsch");
                } else {
                    System.out.println(gruppe + " gruppe ");
                    if (!DB_Groups.checkGroup(gruppe)) {
                        System.out.println(gruppe);
                        if (!DB_Groups.checkGroup(gruppe)) {
                            DB_Groups.addGroup(gruppe);
                            getGroupsImage(username);
                            getGroupMember(gruppe);
                        }
                        DB_Groups.addGroupRelation(username, gruppe);
                        getGroupsImage(username);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    static void getGroupMember(String groupname) {
        try {
            URL url = new URL("http://www.lastfm.de/group/" + groupname + "/members");
            URLConnection con = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            HTMLEditorKit editorKit = new HTMLEditorKit();
            HTMLDocument htmlDoc = new HTMLDocument();
            htmlDoc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            editorKit.read(br, htmlDoc, 0);
            Vector<String> a_tags = new Vector<String>();
            HTMLDocument.Iterator iter = htmlDoc.getIterator(HTML.Tag.A);
            while (iter.isValid()) {
                a_tags.add((String) iter.getAttributes().getAttribute(HTML.Attribute.HREF));
                iter.next();
            }
            Vector<String> members = new Vector<String>();
            for (int i = 0; i < a_tags.size(); i++) {
                String element = (String) a_tags.get(i);
                if (!members.contains(element)) {
                    if (element.contains("/user/")) {
                        members.add(element);
                    }
                }
            }
            for (int a = 0; a < members.size(); a++) {
                String gruppe = members.elementAt(a).toString().substring(6);
                int b = gruppe.length() - 1;
                String membername = gruppe.toString().substring(0, b);
                DB_Groups.addGroupRelation(membername, groupname);
                User.getUserProfile_Stop(membername);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void getGroupsImage(String username) {
        try {
            URL url = new URL("http://www.lastfm.de/user/" + username + "/groups/");
            URLConnection con = url.openConnection();
            HashMap hm = new HashMap();
            Parser parser = new Parser(con);
            NodeList images = parser.parse(new TagNameFilter("IMG"));
            System.out.println(images.size());
            for (int i = 0; i < images.size(); i++) {
                Node bild = images.elementAt(i);
                String bilder = bild.getText();
                if (bilder.contains("http://panther1.last.fm/groupava")) {
                    String bildurl = bilder.substring(9, 81);
                    StringTokenizer st = new StringTokenizer(bilder.substring(88), "\"");
                    String groupname = st.nextToken();
                    hm.put(groupname, bildurl);
                }
            }
            DB_Groups.addGroupImage(hm);
            System.out.println("log3");
        } catch (ParserException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
