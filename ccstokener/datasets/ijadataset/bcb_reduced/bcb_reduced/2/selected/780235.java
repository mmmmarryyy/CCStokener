package org.eclipse.help.internal.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.xhtml.XHTMLContentDescriber;
import org.eclipse.help.search.ISearchIndex;
import org.eclipse.help.search.LuceneSearchParticipant;

public class HTMLSearchParticipant extends LuceneSearchParticipant {

    private static final String HELP_BASE_XHTML = "org.eclipse.help.base.xhtml";

    private HTMLDocParser parser;

    private String indexPath;

    private IContentDescriber xhtmlDescriber;

    private XHTMLSearchParticipant xhtmlParticipant;

    public HTMLSearchParticipant(String indexPath) {
        parser = new HTMLDocParser();
        this.indexPath = indexPath;
    }

    public IStatus addDocument(ISearchIndex index, String pluginId, String name, URL url, String id, Document doc) {
        if (isXHTML(pluginId, url)) {
            LocalSearchManager manager = BaseHelpSystem.getLocalSearchManager();
            LuceneSearchParticipant participant = manager.getParticipant(HELP_BASE_XHTML);
            if (participant == null) {
                participant = getXhtmlParticipant();
            }
            return participant.addDocument(index, pluginId, name, url, id, doc);
        } else {
            try {
                try {
                    try {
                        parser.openDocument(url);
                    } catch (IOException ioe) {
                        return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR, "Help document " + name + " cannot be opened.", null);
                    }
                    ParsedDocument parsed = new ParsedDocument(parser.getContentReader());
                    doc.add(new Field("contents", parsed.newContentReader()));
                    doc.add(new Field("exact_contents", parsed.newContentReader()));
                    String title = parser.getTitle();
                    doc.add(new Field("title", title, Field.Store.NO, Field.Index.TOKENIZED));
                    doc.add(new Field("exact_title", title, Field.Store.NO, Field.Index.TOKENIZED));
                    doc.add(new Field("raw_title", title, Field.Store.YES, Field.Index.NO));
                    doc.add(new Field("summary", parser.getSummary(title), Field.Store.YES, Field.Index.NO));
                } finally {
                    parser.closeDocument();
                }
            } catch (IOException e) {
                return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR, "IO exception occurred while adding document " + name + " to index " + indexPath + ".", e);
            }
            return Status.OK_STATUS;
        }
    }

    private XHTMLSearchParticipant getXhtmlParticipant() {
        if (xhtmlParticipant == null) {
            xhtmlParticipant = new XHTMLSearchParticipant();
        }
        return xhtmlParticipant;
    }

    /**
	 * Returns whether or not the given content should be treated as XHTML.
	 * 
	 * @param pluginId the plugin id containing the content
	 * @param url the URL to the content
	 * @return whether the content should be treated as XHTML
	 */
    private boolean isXHTML(String pluginId, URL url) {
        if (xhtmlDescriber == null) {
            xhtmlDescriber = new XHTMLContentDescriber();
        }
        InputStream in = null;
        try {
            in = url.openStream();
            return (xhtmlDescriber.describe(in, null) == IContentDescriber.VALID);
        } catch (Exception e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }
}
