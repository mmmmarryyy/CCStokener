package hermes.browser.actions;

import hermes.browser.IconCache;
import hermes.browser.tasks.BrowseFIXFileTask;
import hermes.browser.tasks.Task;
import hermes.fix.quickfix.QuickFIXMessageCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.swing.Icon;
import org.apache.log4j.Logger;

/**
 * @author colincrist@hermesjms.com last changed by: $Author: colincrist $
 * @version $Id: FIXFileBrowserAction.java,v 1.6 2006/05/13 21:27:48 colincrist
 *          Exp $
 */
public class FIXFileBrowserAction {

    private static final Logger log = Logger.getLogger(FIXFileBrowserAction.class);

    private InputStream istream;

    private String title;

    private QuickFIXMessageCache messageCache = new QuickFIXMessageCache();

    public FIXFileBrowserAction(File file, int maxMessages) throws FileNotFoundException {
        super();
        this.istream = new FileInputStream(file);
        this.title = file.getName();
    }

    public FIXFileBrowserAction(URL url, int maxMessages) throws IOException {
        super();
        this.istream = url.openStream();
        this.title = url.toString();
    }

    public void start() {
        final Task task = new BrowseFIXFileTask(messageCache, istream, title);
        task.start();
    }

    public Icon getIcon() {
        return IconCache.getIcon("hermes.file.xml");
    }
}
