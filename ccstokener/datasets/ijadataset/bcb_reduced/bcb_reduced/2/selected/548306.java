package com.guanda.swidgex.widgets.dict;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.guanda.swidgex.api.Configurator;
import com.guanda.swidgex.api.IConfiguration;
import com.guanda.swidgex.api.IDesktopContextListener;
import com.guanda.swidgex.api.IWidgetContext;
import com.guanda.swidgex.component.AbstractWidgetWithProgress;
import com.guanda.swidgex.component.WebBrowser;

public class DictCN extends AbstractWidgetWithProgress<String> implements IExecutableExtension, IDesktopContextListener {

    private static final long serialVersionUID = 1L;

    private static final int ICON_PAD = 16;

    private static final Color BACKCOLOR = new Color(58, 68, 78);

    private static final String CONFIG_ID = "com.guanda.swidgex.widgets.dict.word_list";

    private JCheckBoxMenuItem cmiAll = new JCheckBoxMenuItem(new AbstractAction("Reciting All Words") {

        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            reciting = !reciting;
            setWords();
        }
    });

    private JCheckBoxMenuItem cmiXml = new JCheckBoxMenuItem(new AbstractAction("Simle Text") {

        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            useXml = !useXml;
            DictCN.this.showWord();
        }
    });

    private boolean useXml;

    private boolean reciting = true;

    private String list_url;

    private List<String> words;

    private List<String> all_words;

    private List<String> recited_words;

    private int current;

    private int top;

    private WebBrowser browser;

    private JButton first;

    private JButton last;

    private JButton prev;

    private JButton next;

    private JTextField txtSearch;

    private Date start_date;

    private JPanel main;

    @SuppressWarnings("deprecation")
    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        browser = new WebBrowser();
        IConfigurationElement[] children = config.getChildren("parameter");
        for (IConfigurationElement child : children) {
            if ("list-url".equals(child.getAttribute("name"))) {
                list_url = child.getAttribute("value");
            }
            if ("start-date".equals(child.getAttribute("name"))) {
                start_date = new Date(child.getAttribute("value"));
            }
        }
    }

    @Override
    public void init(IWidgetContext context) {
        context.getDesktopContext().addContextListener(this);
        super.init(context);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void aboutToRun() {
        main = new JPanel();
        main.setBackground(BACKCOLOR);
        main.setLayout(new BorderLayout());
        main.add(browser, BorderLayout.CENTER);
        initSearchPanel();
        initCtrlPanel();
        if (list_url == null) {
            list_url = getClass().getResource("/resources/words.txt").toExternalForm();
        }
        if (start_date == null) {
            start_date = new Date("2010/10/28");
        }
        setIndeterminate(true);
        setMessage("Loading word list...");
    }

    private void initSearchPanel() {
        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BorderLayout());
        main.add(north, BorderLayout.NORTH);
        JLabel label = new JLabel("Search an English Word:");
        label.setForeground(Color.white);
        north.add(label, BorderLayout.NORTH);
        txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(1, 26));
        north.add(txtSearch, BorderLayout.CENTER);
        txtSearch.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String word = txtSearch.getText();
                if (word != null) {
                    word = word.trim();
                    if (word.length() > 0) {
                        showWord(word);
                    }
                }
            }
        });
    }

    private void initCtrlPanel() {
        JPanel ctrlPanel = new JPanel();
        ctrlPanel.setOpaque(false);
        main.add(ctrlPanel, BorderLayout.SOUTH);
        first = new NavButton();
        first.setToolTipText("First");
        first.setIcon(new ImageIcon(getClass().getResource("/icons/first.png")));
        first.setPreferredSize(new Dimension(ICON_PAD, ICON_PAD));
        first.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                current = 0;
                updateButtonState();
                showWord();
            }
        });
        last = new NavButton();
        last.setToolTipText("Last");
        last.setIcon(new ImageIcon(getClass().getResource("/icons/last.png")));
        last.setPreferredSize(new Dimension(ICON_PAD, ICON_PAD));
        last.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                current = top;
                updateButtonState();
                showWord();
            }
        });
        prev = new NavButton();
        prev.setToolTipText("Previous");
        prev.setIcon(new ImageIcon(getClass().getResource("/icons/prev.png")));
        prev.setPreferredSize(new Dimension(ICON_PAD, ICON_PAD));
        prev.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                current--;
                updateButtonState();
                showWord();
            }
        });
        next = new NavButton();
        next.setToolTipText("Next");
        next.setIcon(new ImageIcon(getClass().getResource("/icons/next.png")));
        next.setPreferredSize(new Dimension(ICON_PAD, ICON_PAD));
        next.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                current++;
                updateButtonState();
                showWord();
            }
        });
        ctrlPanel.add(first);
        ctrlPanel.add(prev);
        ctrlPanel.add(next);
        ctrlPanel.add(last);
    }

    private void showWord(String word) {
        if (useXml) {
            String query = "http://dict.cn/ws.php?q=" + word;
            String html = parseQuery(query);
            browser.setText(html);
        } else {
            String url = "http://dict.cn/mini.php?q=" + word;
            browser.setUrl(url);
        }
    }

    private String parseQuery(String sUrl) {
        try {
            URL url = new URL(sUrl);
            DocumentBuilder nBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = nBuilder.parse(url.openStream());
            Element root = document.getDocumentElement();
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append("<html><body><p>");
            String word = getElementValue(root, "key");
            if (word != null) sBuilder.append(word);
            String pron = getElementValue(root, "pron");
            if (pron != null) sBuilder.append("&nbsp;&nbsp;[" + pron + "]");
            sBuilder.append("</p>");
            NodeList nodes = root.getElementsByTagName("def");
            if (nodes != null && nodes.getLength() > 0) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element node = (Element) nodes.item(i);
                    String def = node.getTextContent();
                    sBuilder.append("<p>");
                    sBuilder.append(def);
                    sBuilder.append("</p>");
                }
            }
            nodes = root.getElementsByTagName("sent");
            if (nodes != null && nodes.getLength() > 0) {
                sBuilder.append("<p>Examples:</p>");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element node = (Element) nodes.item(i);
                    String orig = getElementValue(node, "orig");
                    sBuilder.append("<p>" + orig + "</p>");
                    String trans = getElementValue(node, "trans");
                    sBuilder.append("<p>" + trans + "</p>");
                }
            }
            sBuilder.append("</body></html>");
            return sBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getElementValue(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagName(name);
        if (nodes != null && nodes.getLength() > 0) {
            Node item = nodes.item(0);
            return item.getTextContent();
        }
        return null;
    }

    private synchronized void showWord() {
        if (current >= 0 && current < words.size()) {
            String word = words.get(current);
            showWord(word);
        }
    }

    private void updateButtonState() {
        first.setEnabled(current > 0);
        prev.setEnabled(current > 0);
        next.setEnabled(current < top);
        last.setEnabled(current < top);
    }

    @Override
    protected void done() {
        setWords();
        super.done();
    }

    private void setWords() {
        words = reciting ? recited_words : all_words;
        top = words.size() - 1;
        current = 0;
        updateButtonState();
        showWord();
        cmiAll.setEnabled(true);
        cmiXml.setEnabled(true);
    }

    @Override
    protected JComponent doInBackground() throws Exception {
        readWordList();
        return main;
    }

    @Override
    public void createContextAction(JPopupMenu popup) {
        cmiAll.setEnabled(isDone());
        cmiAll.setSelected(!reciting);
        popup.add(cmiAll);
        cmiXml.setEnabled(isDone());
        cmiXml.setSelected(useXml);
        popup.add(cmiXml);
        popup.addSeparator();
    }

    private synchronized void readWordList() {
        all_words = getWordList();
        int start = 25 * getNowIndex() - 1;
        int index = start;
        recited_words = new ArrayList<String>();
        int count = 0;
        int aSize = all_words.size();
        while (index >= 24) {
            for (int i = index; i > index - 25; i--) {
                if (i < aSize) recited_words.add(all_words.get(i));
            }
            int tmp = 1 << count;
            index = start - 25 * tmp;
            count++;
        }
        Collections.reverse(recited_words);
    }

    @SuppressWarnings("unchecked")
    private List<String> getWordList() {
        IConfiguration config = Configurator.getDefaultConfigurator().getConfig(CONFIG_ID);
        List<String> wList = (List<String>) config.getObject("word_list");
        if (wList == null) {
            wList = new ArrayList<String>();
            InputStream resrc = null;
            try {
                resrc = new URL(list_url).openStream();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (resrc != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(resrc));
                String line;
                try {
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.length() != 0) {
                            wList.add(line);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
        return wList;
    }

    @SuppressWarnings("deprecation")
    private int getNowIndex() {
        Date now = new Date();
        long past = now.getTime() - start_date.getTime();
        int days = (int) (past / (1000 * 60 * 60 * 24));
        if (now.getHours() < 12) return 2 * days + 1; else return 2 * days + 2;
    }

    @Override
    public void onExit() {
        browser.dispose();
        if (all_words != null && !all_words.isEmpty()) {
            IConfiguration config = Configurator.getDefaultConfigurator().getConfig(CONFIG_ID);
            config.setObject("word_list", all_words);
        }
    }
}
