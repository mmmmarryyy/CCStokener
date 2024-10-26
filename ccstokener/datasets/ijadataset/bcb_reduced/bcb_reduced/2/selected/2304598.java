package net.sf.mailsomething.help;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.sf.mailsomething.util.Module;
import net.sf.mailsomething.Mailsomething;
import net.sf.mailsomething.core.RessourceManager;
import net.sf.mailsomething.core.Service;
import net.sf.mailsomething.gui.*;
import net.sf.mailsomething.gui.core.*;
import net.sf.mailsomething.gui.help.*;
import net.sf.mailsomething.gui.mail.MailGuiActions;
import net.sf.mailsomething.gui.standard.CrossIcon;
import net.sf.mailsomething.mail.MailService;
import net.sf.mailsomething.search.Match;
import net.sf.mailsomething.search.SearchDefinition;
import net.sf.mailsomething.search.ResultList;
import net.sf.mailsomething.search.Searchable;

/**
 * 
 * Its probably not good to let this implement HelpController, since it should
 * be a strictly gui-related class. Im just keeping it here so far.
 * 
 * @author Stig Tanggaard
 *
 */
public class HelpService implements Service, HelpController, Searchable {

    private HelpSystem module = null;

    private ETabbedPane tabbedpane;

    private InternalJPanel helppane;

    private Vector pagesDisplayed;

    /**
	 * @see java.lang.Object#Object()
	 */
    public HelpService() {
        if (module == null) module = new HelpSystem();
    }

    public void init() {
        RessourceManager ressourceManager = Mailsomething.getUser().getRessourceManager().getRessourceManager("help");
        ressourceManager.add("updatepages", new DownloadHelp());
        ressourceManager.add("mailwizzarddialog", new MailGuiActions.MailWizzard());
        ((HelpSystem) getModule()).setRessourceManager(ressourceManager);
        HelpPage[] pages = ((HelpSystem) getModule()).getIndex();
        JTextPane pane = new HelpIndexPanel(pages, this);
        pane.setBorder(new EmptyBorder(20, 15, 10, 12));
        ComponentHints hints = new ComponentHints() {

            Icon icon = GuiUser.getImageIcon("gifs/unknownicon.gif");

            public Icon getSmallIcon() {
                return icon;
            }

            public String getTitle() {
                return "Help";
            }
        };
        GuiUser.getDefaultGui().addComponent(new JScrollPane(pane), DefaultGui.TREE_VIEW, hints);
        if (MailService.getInstance().getAccountCount() == 0) {
            showHelpPage(HelpIndexPanel.getOverviewPage());
        }
    }

    /**
	 * @see net.sf.mailsomething.util.Service#getDescribtion()
	 */
    public String getDescription() {
        return "The help service for the app, allows modules of help for different " + "subsystems of the app.";
    }

    /**
	 * @see net.sf.mailsomething.util.Service#getTitle()
	 */
    public String getTitle() {
        return "Help";
    }

    public void setModule(Module mod) {
    }

    /**
	 * @see net.sf.mailsomething.util.Service#getModule()
	 */
    public Module getModule() {
        if (module == null) module = new HelpSystem();
        return module;
    }

    /**
	 * @see net.sf.mailsomething.util.Service#getActions()
	 */
    public Action[] getActions() {
        return new Action[] { new ShowIndex() };
    }

    /**
	 * 
	 * Action for showing the helpindex.
	 * 
	 * @author Stig Tanggaard
	 * @created 26-04-2003
	 * 
	 */
    class ShowIndex extends AbstractAction {

        JScrollPane p;

        public ShowIndex() {
            super("Show index");
        }

        public void actionPerformed(ActionEvent e) {
            ((HelpSystem) getModule()).getRessourceManager().add("updatepages", new DownloadHelp());
            ((HelpSystem) getModule()).getRessourceManager().add("mailwizzarddialog", new MailGuiActions.MailWizzard());
            HelpPage[] pages = ((HelpSystem) getModule()).getIndex();
            tabbedpane = new ETabbedPane();
            JTextPane pane = new HelpIndexPanel(pages, HelpService.this);
            p = new JScrollPane(pane);
            p.setName("Index");
            tabbedpane.add(p);
            InternalJPanel p2 = new InternalJPanel();
            p2.setLayout(new BorderLayout());
            p2.add(tabbedpane, BorderLayout.CENTER);
            p2.setName("Help");
            GuiUser.getDefaultGui().addComponent(p2, DefaultGui.FULL_VIEW);
        }
    }

    /**
	 * Method for showing a helppage. It is better to call this,
	 * than creating ur own helppageitempanels or similar, since
	 * this way it is controlled from a central place HOW to show
	 * the page. One could imagine another possibility than adding
	 * the helppage to a tabbedpane.
	 * 
	 * @param page the helppage to be displayed
	 */
    public void showHelpPage(HelpPage page) {
        if (module == null) return;
        if (tabbedpane == null) {
            pagesDisplayed = new Vector();
            tabbedpane = new ETabbedPane();
            tabbedpane.addTabListener(new TabListener());
            helppane = new InternalJPanel();
            helppane.setLayout(new BorderLayout());
            helppane.add(tabbedpane, BorderLayout.CENTER);
            helppane.setName("Help");
            GuiUser.getDefaultGui().addComponent(helppane, DefaultGui.FULL_VIEW);
        }
        if (pagesDisplayed.contains(page)) {
            tabbedpane.setSelectedIndex(pagesDisplayed.indexOf(page));
            return;
        } else {
            pagesDisplayed.add(page);
        }
        HelpItemPanel panel = new HelpItemPanel(page, this);
        panel.setShowInline(false);
        JTextPane pane = panel.getTextPane();
        JScrollPane scrollpane = new JScrollPane(pane);
        tabbedpane.addTab(page.getTitle(), new CrossIcon(), scrollpane);
        tabbedpane.setSelectedComponent(scrollpane);
        GuiUser.getDefaultGui().requestView(DefaultGui.FULL_VIEW, helppane);
    }

    class DownloadHelp extends AbstractAction {

        public DownloadHelp() {
            super("Update helppages");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                URL url = new URL("http://mailsomething.sf.net/helppages.xml");
                try {
                    File file = File.createTempFile("temp", "xml");
                    InputStream in = new BufferedInputStream(url.openStream());
                    OutputStream out = new FileOutputStream(file);
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    out.flush();
                    out.close();
                    module.merge(file);
                } catch (IOException f) {
                }
            } catch (MalformedURLException f) {
            }
        }
    }

    /**
	 * @see net.sf.mailsomething.util.Service#getDependencies()
	 */
    public String[] getDependencies() {
        return new String[] {};
    }

    /**
	 * 
	 * 
	 * 
	 * @param search
	 * @param list
	 * @return ResultList
	 */
    public ResultList search(SearchDefinition search, ResultList list) {
        HelpItem[] pages = module.getSortedItems();
        for (int i = 0; i < pages.length; i++) {
            searchPage((HelpPage) pages[i], search, list);
        }
        return list;
    }

    public boolean searchObject(SearchDefinition search, Object object) {
        return false;
    }

    private void searchPage(HelpPage page, SearchDefinition search, ResultList list) {
        if (search.isDeepSearch()) {
            if (search.isMatch(page.getLongDescribtion()) || search.isMatch(page.getShortDescribtion()) || search.isMatch(page.getTitle())) {
                Match match = search.getMatch(page.getTitle(), page.getLongDescribtion(), new String[] {});
                match.setObject(page);
                list.add(match);
            }
        } else {
            if (search.isMatch(page.getShortDescribtion()) || search.isMatch(page.getTitle())) {
                Match match = search.getMatch(page.getTitle(), page.getLongDescribtion(), new String[] {});
                match.setObject(page);
                list.add(match);
            }
        }
    }

    public HelpSystem getHelpSystem() {
        return module;
    }

    /**
		 * Method which should return the class names of those objects which
		 * the searchable allows to be searched for. This is being used in 
		 * conjunction with search(SearchObject, Object) to make sure we
		 * dont use the search method for the object and suplies an object
		 * of a wrong type. 
		 * @return String[]
		 */
    public String[] getSearchTypes() {
        return new String[] {};
    }

    /**
	 *  
	 * This doesnt necessarely needs to return an accurate value, hence the
	 * name, estimate. Its to avoid having implementations go through unreasonable
	 * big efforts to calculate the value. 
	 * 
	 * @return int
	 */
    public int getSearchLengthEstimate(String[] classnames) {
        return 0;
    }

    public boolean isSearchable() {
        return true;
    }

    class TabListener implements ETabListener {

        public void tabComponentRemoved(int index) {
            if (index < pagesDisplayed.size()) pagesDisplayed.remove(index);
        }
    }

    public void shutdown() {
    }
}
