package GUI;

import Deck_Builder.Dialog;
import Deck_Builder.str;
import Deck_Builder.Action;
import Data.c_CastingCost;
import Data.c_Card;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.event.EventListenerList;
import org.apache.commons.io.FileUtils;
import org.mozilla.browser.MozillaPanel;

/**
 *
 * @author Phillip
 */
public class WebBrowserPanel extends MozillaPanel {

    private JFrame m_frame;

    private ArrayList<String> m_content = new ArrayList<String>();

    private c_Card m_card = new c_Card();

    private EventListenerList m_listeners = new EventListenerList();

    private boolean m_loading = false;

    @Override
    public void finalize() throws Throwable {
        m_frame = null;
        m_content = null;
        m_card = null;
        m_listeners = null;
        super.finalize();
    }

    public WebBrowserPanel() {
        super();
        m_frame = null;
    }

    public WebBrowserPanel(JFrame frame) {
        super();
        m_frame = frame;
    }

    public WebBrowserPanel(boolean attachNewBrowserOnCreation, VisibilityMode toolbarVisMode, VisibilityMode statusbarVisMode) {
        super(attachNewBrowserOnCreation, toolbarVisMode, statusbarVisMode);
        m_frame = null;
    }

    public WebBrowserPanel(VisibilityMode toolbarVisMode, VisibilityMode statusbarVisMode) {
        super(toolbarVisMode, statusbarVisMode);
        m_frame = null;
    }

    public void addActionListener(ActionListener listener) {
        m_listeners.add(ActionListener.class, listener);
    }

    public void giveFrame(JFrame frame) {
        m_frame = frame;
    }

    public c_Card getCard() {
        return m_card;
    }

    public ArrayList<String> getContent() {
        return (ArrayList<String>) m_content.clone();
    }

    public boolean isLoading() {
        return m_loading;
    }

    @Override
    public void onLoadingStarted() {
        m_loading = true;
    }

    @Override
    public void onLoadingEnded() {
        if (m_frame != null) {
            try {
                String urltext = getDocument().getDocumentURI();
                URL url = new URL(urltext);
                InputStreamReader isr = new InputStreamReader(url.openStream());
                BufferedReader in = new BufferedReader(isr);
                String inputLine;
                urltext = null;
                url = null;
                m_content.clear();
                while ((inputLine = in.readLine()) != null) {
                    m_content.add(inputLine);
                }
                in.close();
                isr = null;
                in = null;
                inputLine = null;
                Action action = parseHtml();
                if (action.value() == Action.ACTION_BROWSER_LOADING_DONE && action.toString().equals(Action.COMMAND_CARD_PREVIEW)) {
                    FileUtils.copyURLToFile(new URL(getCardImageURL(m_card.MID)), new File(m_card.getImagePath()));
                    fireActionEvent(MainWindow.class, action.value(), action.toString());
                }
                action = null;
            } catch (Exception ex) {
                Dialog.ErrorBox(m_frame, ex.getStackTrace());
            }
        }
        m_loading = false;
    }

    public static String getCardImageURL(Integer MID) {
        return String.format("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=%d&type=card", MID);
    }

    private Action parseHtml() {
        Action action = new Action();
        boolean foundCard = false;
        if (this.getDocument().getDocumentURI().startsWith("http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=")) {
            m_card = new c_Card();
            foundCard = parseContent(m_content, m_card);
        }
        if (foundCard) {
            action = new Action(Action.ACTION_BROWSER_LOADING_DONE, Action.COMMAND_CARD_PREVIEW);
        }
        return action;
    }

    public static boolean parseContent(ArrayList<String> content, c_Card card) {
        boolean foundCard = false;
        String line;
        for (int i = 0; i < content.size(); i++) {
            line = content.get(i);
            if (line.contains("multiverseid=") && card.MID == 0) {
                foundCard = true;
                card.MID = Integer.parseInt(str.middleOf("multiverseid=", line, "\" id="));
                i += 300;
            } else if (line.contains("Card Name:")) {
                i += 2;
                card.Name = str.leftOf(content.get(i), "</div>").trim();
            } else if (line.contains("Mana Cost:")) {
                i += 2;
                line = content.get(i);
                String glyphs = "";
                do {
                    line = str.rightOf(line, "/Handlers/Image.ashx?size=medium&amp;name=");
                    glyphs += str.leftOf(line, "&amp;type=symbol") + ",";
                } while (line.contains("/Handlers/Image.ashx?size=medium&amp;name="));
                card.CastingCost = new c_CastingCost(glyphs.substring(0, glyphs.length() - 1));
                i += 5;
                glyphs = null;
            } else if (line.contains("Types:")) {
                i += 2;
                line = content.get(i);
                if (line.contains("—")) {
                    card.Type = str.leftOf(line, "—").trim();
                    card.SubType = str.middleOf("— ", line, "</div>");
                } else {
                    card.Type = str.leftOf(line, "</div>").trim();
                    card.SubType = "";
                }
            } else if (line.contains("P/T:")) {
                i += 2;
                line = content.get(i);
                card.PT = str.leftOf(line, "</div>").replaceAll(" ", "");
            } else if (line.contains("Expansion:")) {
                i += 4;
                line = content.get(i);
                card.Expansion = str.middleOf(">", line, "</a>");
                break;
            }
        }
        line = null;
        return foundCard;
    }

    private void fireActionEvent(Class thisClass, Integer action, String command) {
        Object listeners[] = m_listeners.getListenerList();
        for (int i = listeners.length - 1; i >= 0; i--) {
            if (listeners[i].getClass() == thisClass) {
                ((ActionListener) listeners[i]).actionPerformed(new ActionEvent(this, action, command));
                listeners = null;
                return;
            }
        }
        listeners = null;
    }
}
