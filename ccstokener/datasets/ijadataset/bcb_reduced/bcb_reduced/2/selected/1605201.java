package org.jmol.fah;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Manage contributions informations
 */
public class Contribution {

    private Vector _infos = null;

    private static final int PAGES_COUNT = 4;

    private static final int PROJECTS_BY_PAGE = 1000;

    /**
   * @return Contribution singleton
   */
    public static Contribution getContribution() {
        if (_contrib == null) {
            _contrib = new Contribution();
        }
        return _contrib;
    }

    /**
   * Constructor for Contribution
   */
    private Contribution() {
        _infos = new Vector(PAGES_COUNT * PROJECTS_BY_PAGE);
    }

    /**
   * Add information from stanford stats site
   * 
   * @param userName User name
   * @param teamNum Team number
   */
    public void addInformation(String userName, int teamNum) {
        for (int i = 0; i < PAGES_COUNT; i++) {
            addInformation(userName, teamNum, i * PROJECTS_BY_PAGE);
        }
    }

    /**
   * Add information from stanford stats site for a range
   * 
   * @param userName User name
   * @param teamNum Team number
   * @param range Range
   */
    private void addInformation(String userName, int teamNum, int range) {
        StringBuffer urlName = new StringBuffer();
        urlName.append("http://vspx27.stanford.edu/");
        urlName.append("cgi-bin/main.py?qtype=userpagedet");
        urlName.append("&username=");
        urlName.append(userName);
        urlName.append("&teamnum=");
        urlName.append(teamNum);
        urlName.append("&prange=");
        urlName.append(range);
        try {
            URL url = new URL(urlName.toString());
            InputStream stream = url.openStream();
            InputStreamReader reader = new InputStreamReader(stream);
            HTMLDocument htmlDoc = new HTMLDocumentContribution(this);
            HTMLEditorKit htmlEditor = new HTMLEditorKit() {

                protected HTMLEditorKit.Parser getParser() {
                    return new ParserDelegator() {

                        public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharSet) throws IOException {
                            super.parse(r, cb, true);
                        }
                    };
                }
            };
            htmlEditor.read(reader, htmlDoc, 0);
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
    }

    /**
   * Adds contribution informations for a project
   * 
   * @param project Project number
   * @param count Contributions
   */
    void addProjectInformation(String project, int count) {
        if ((project == null) || (project.length() == 0)) {
            System.out.println("Incorrect project: " + project);
            return;
        }
        int projectNum = 0;
        try {
            projectNum = Integer.parseInt(project.substring(1));
        } catch (NumberFormatException e) {
            System.out.println("Incorrect project: " + project);
        }
        Integer currentCount = null;
        if (projectNum < _infos.size()) {
            try {
                currentCount = (Integer) _infos.get(projectNum);
            } catch (ClassCastException e) {
                System.out.println("Error in infos: " + _infos.get(projectNum));
            }
        }
        Integer newCount = new Integer(count + (currentCount != null ? currentCount.intValue() : 0));
        if (projectNum >= _infos.size()) {
            _infos.setSize(projectNum + 1);
        }
        _infos.set(projectNum, newCount);
    }

    /**
   * Displays contribution informations 
   */
    public void displayContributions() {
        for (int i = 0; i < _infos.size(); i++) {
            try {
                Integer count = (Integer) _infos.get(i);
                if (count != null) {
                    System.out.println("P" + i + "\t" + count);
                }
            } catch (ClassCastException e) {
            }
        }
    }

    private static Contribution _contrib;

    /**
   * HTML Document for Contribution page
   */
    private class HTMLDocumentContribution extends HTMLDocument {

        private Contribution _contrib1 = null;

        public HTMLDocumentContribution(Contribution contrib) {
            super();
            _contrib1 = contrib;
        }

        public HTMLEditorKit.ParserCallback getReader(int pos) {
            return new ContributionReader(pos, _contrib1);
        }

        /**
     * Reader for Contribution
     */
        private class ContributionReader extends HTMLDocument.HTMLReader {

            private Contribution _contrib2;

            /**
       * @param offset
       * @param contrib
       */
            public ContributionReader(int offset, Contribution contrib) {
                super(offset);
                _contrib2 = contrib;
            }

            public void handleStartTag(HTML.Tag tag, MutableAttributeSet att, int pos) {
                if (tag.equals(HTML.Tag.TABLE)) {
                    this._table++;
                    this._tableNum++;
                    this._column = 0;
                }
                if (tag.equals(HTML.Tag.TR)) {
                    this._row++;
                    this._column = 0;
                }
                if (tag.equals(HTML.Tag.TD)) {
                    this._column++;
                }
                if ((this._table > 0) && (this._row == 1)) {
                }
                super.handleStartTag(tag, att, pos);
            }

            public void handleText(char[] data, int pos) {
                if ((this._table > 0) && (this._row > 1) && (this._tableNum == 8)) {
                    switch(this._column) {
                        case 1:
                            this._project = new String(data);
                            break;
                        case 2:
                            this._count = Integer.parseInt(new String(data));
                            break;
                    }
                }
                super.handleText(data, pos);
            }

            public void handleEndTag(HTML.Tag tag, int pos) {
                if ((this._table > 0) && (this._row == 1)) {
                }
                if (tag.equals(HTML.Tag.TABLE)) {
                    this._table--;
                    if (this._table == 0) {
                        this._row = 0;
                    }
                }
                if (tag.equals(HTML.Tag.TR)) {
                    if ((this._project != null) && (this._count > 0) && (_contrib2 != null)) {
                        _contrib2.addProjectInformation(this._project, this._count);
                    }
                    this._column = 0;
                    this._project = null;
                    this._count = 0;
                }
                super.handleEndTag(tag, pos);
            }

            private int _column = 0;

            private int _row = 0;

            private int _table = 0;

            private int _tableNum = 0;

            private String _project = null;

            private int _count = 0;
        }
    }

    /**
   * Main enabling checking getting contribution informations
   * 
   * @param args Command line arguments
   */
    public static void main(String[] args) {
        String userName = System.getProperty("org.jmol.fah.user");
        if (userName == null) {
            System.err.println("You must define org.jmol.fah.user");
            return;
        }
        String team = System.getProperty("org.jmol.fah.team");
        if (team == null) {
            System.err.println("You must define org.jmol.fah.team");
            return;
        }
        String[] teams = team.split(",");
        Contribution contrib = getContribution();
        for (int i = 0; i < teams.length; i++) {
            try {
                int teamNumber = Integer.parseInt(teams[i]);
                contrib.addInformation(userName, teamNumber);
            } catch (NumberFormatException e) {
                System.err.println("org.jmol.fah.team must be a comma separated list of integers");
            }
        }
        contrib.displayContributions();
    }
}
