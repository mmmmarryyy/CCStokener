package org.jessies.blogbaboon;

import com.google.gdata.data.*;
import com.google.gdata.data.blogger.*;
import e.forms.*;
import e.gui.*;
import e.util.*;
import e.ptextarea.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;

public class EditingWindow extends JFrame {

    private final BlogBaboon blogEditor;

    private final PostEntry post;

    private final JTextField titleField = new JTextField(40);

    private String originalTitle;

    private final PTextArea textArea = new PTextArea(20, 40);

    private final EStatusBar statusBar = new EStatusBar();

    public final Action closeAction;

    public final Action previewAction;

    private final Action publishAction;

    public final Action saveDraftAction;

    private String cachedTemplate;

    private File previewFile;

    public EditingWindow(BlogBaboon blogEditor, PostEntry post) {
        this.blogEditor = blogEditor;
        this.post = post;
        this.originalTitle = post.getTitle().getPlainText();
        this.closeAction = new CloseAction(this);
        this.previewAction = new PreviewAction(this);
        this.publishAction = new PublishAction(this);
        this.saveDraftAction = new SaveDraftAction(this);
        titleField.setText(originalTitle);
        titleField.setCaretPosition(0);
        titleField.setEditable(post.isDraft());
        titleField.addFocusListener(blogEditor.actionStateUpdater);
        titleField.getDocument().addDocumentListener(new DocumentAdapter() {

            public void documentChanged() {
                updateTitleBar();
            }
        });
        updateTitleBar();
        textArea.setText(decodeContent());
        textArea.getTextBuffer().getUndoBuffer().resetUndoBuffer();
        textArea.getTextBuffer().getUndoBuffer().setCurrentStateClean();
        textArea.setWrapStyleWord(true);
        textArea.setTextStyler(new PXmlTextStyler(textArea));
        textArea.getTextBuffer().addTextListener(new PTextListener() {

            public void textInserted(PTextEvent event) {
                textChanged();
            }

            public void textRemoved(PTextEvent event) {
                textChanged();
            }

            public void textCompletelyReplaced(PTextEvent event) {
                textChanged();
            }
        });
        textArea.addFocusListener(blogEditor.actionStateUpdater);
        preferencesChanged();
        setLayout(new BorderLayout());
        setJMenuBar(new BlogBaboonMenuBar(blogEditor, this));
        final JScrollPane scrollableText = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (GuiUtilities.isMacOs()) {
            scrollableText.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("TabbedPane.shadow")));
        }
        add(makeTopPanel(), BorderLayout.NORTH);
        add(scrollableText, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        pack();
        initCloseBehavior();
        initFocus();
        setLocationRelativeTo(blogEditor);
        previewAction.setEnabled(true);
        publishAction.setEnabled(post.isDraft());
        saveDraftAction.setEnabled(false);
        JFrameUtilities.setFrameIcon(this);
        setVisible(true);
        InstanceTracker.addInstance(this);
    }

    private void updateTitleBar() {
        textChanged();
        post.setTitle(new HtmlTextConstruct(titleField.getText()));
        setTitle(BlogUtils.displayableTitle(post));
    }

    private void initCloseBehavior() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                maybeClose();
            }
        });
        if (GuiUtilities.isMacOs()) {
            final KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_MASK, false);
            JFrameUtilities.closeOnKeyStroke(this, commandW);
        }
    }

    public boolean maybeClose() {
        if (!isDirty() || SimpleDialog.askQuestion(this, "Discard Changes", "Discard changes to post \"" + BlogUtils.displayableTitle(post) + "\"?", "Discard")) {
            dispose();
            return true;
        }
        return false;
    }

    private void initFocus() {
        titleField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                textArea.requestFocusInWindow();
            }
        });
        if (titleField.getText().length() == 0) {
            titleField.requestFocusInWindow();
        } else {
            textArea.requestFocusInWindow();
        }
    }

    private JComponent makeTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        topPanel.add(makeToolBar(), BorderLayout.NORTH);
        topPanel.add(makeHeaderForm(), BorderLayout.CENTER);
        return topPanel;
    }

    private JComponent makeHeaderForm() {
        FormPanel headerForm = new FormPanel();
        headerForm.addRow("Title:", titleField);
        return headerForm;
    }

    private JToolBar makeToolBar() {
        if (post.isDraft()) {
            return UiUtils.makeToolBar(saveDraftAction, previewAction, publishAction);
        } else {
            return UiUtils.makeToolBar(saveDraftAction, previewAction);
        }
    }

    public boolean isDirty() {
        return !originalTitle.equals(titleField.getText()) || !textArea.getTextBuffer().getUndoBuffer().isClean();
    }

    private void textChanged() {
        final boolean isDirty = isDirty();
        blogEditor.updateActionEnabledState();
        getRootPane().putClientProperty("windowModified", isDirty);
        getRootPane().putClientProperty("Window.documentModified", isDirty);
        saveDraftAction.setEnabled(isDirty);
    }

    public void preferencesChanged() {
        final Preferences preferences = blogEditor.getPreferences();
        textArea.setFont(preferences.getFont(BlogBaboonPreferences.EDITOR_FONT));
        textArea.setShouldHideMouseWhenTyping(preferences.getBoolean(BlogBaboonPreferences.HIDE_MOUSE_WHEN_TYPING));
    }

    private String decodeContent() {
        final TextConstruct contentConstruct = post.getTextContent().getContent();
        String content = ((HtmlTextConstruct) contentConstruct).getHtml();
        return new WikiCodec().fromHtml(content);
    }

    private String encodeContent() {
        return new WikiCodec().toHtml(textArea.getText());
    }

    private String retrieveTemplate() throws Exception {
        if (cachedTemplate == null) {
            final URL url = new URL(blogEditor.getBlogInfo().getBlogUrl());
            final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            final StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            in.close();
            cachedTemplate = result.toString();
        }
        return cachedTemplate;
    }

    public void preview() {
        try {
            String postContent = "";
            postContent += "<h2 class='date-header'>" + post.getPublished().toUiString() + "</h2>\n";
            postContent += "<div class='post hentry'>";
            postContent += " <h3 class='post-title entry-title'>";
            postContent += "  <a href=''>" + BlogUtils.displayableTitle(post) + "</a>";
            postContent += " </h3>\n";
            postContent += " <div class='post-header-line-1'></div>";
            postContent += " <div class='post-body entry-content'>\n";
            postContent += encodeContent() + "\n";
            postContent += "  <div style='clear: both;'></div>";
            postContent += " </div>";
            postContent += " <div class='post-footer'>";
            postContent += "  <div class='post-footer-line post-footer-line-1'>";
            postContent += "   <span class='post-icons'>";
            postContent += "   </span>";
            postContent += "   <span class='post-labels'>";
            postContent += "   </span>";
            postContent += "  </div>";
            postContent += "  <div class='post-footer-line post-footer-line-2'></div>";
            postContent += "  <div class='post-footer-line post-footer-line-3'></div>";
            postContent += " </div>";
            postContent += "</div>\n";
            final String preview = retrieveTemplate().replaceAll("<!-- google_ad_section_start.*<!-- google_ad_section_end -->", StringUtilities.replacementStringFromLiteral(postContent));
            if (previewFile == null) {
                final String fileDescription = "Preview of " + BlogUtils.displayableTitle(post);
                previewFile = FileUtilities.createTemporaryFile("preview-", ".html", fileDescription, preview);
            } else {
                StringUtilities.writeFile(previewFile, preview);
            }
            BrowserLauncher.openURL(previewFile.toURI().toString());
        } catch (Exception ex) {
            SimpleDialog.showDetails(this, "Failed to show preview.", ex);
        }
    }

    public void publish() {
        if (!SimpleDialog.askQuestion(this, "Publish Post?", "Publish the post \"" + BlogUtils.displayableTitle(post) + "\"?", "Publish")) {
            return;
        }
        saveOrPublish(true);
    }

    public void saveDraft() {
        saveOrPublish(false);
    }

    private void saveOrPublish(boolean shouldPublish) {
        post.setTitle(new HtmlTextConstruct(titleField.getText()));
        post.setContent(new HtmlTextConstruct(encodeContent()));
        post.setUpdated(DateTime.now());
        if (shouldPublish) {
            post.setDraft(false);
            post.setPublished(DateTime.now());
        }
        try {
            post.update();
            blogEditor.updatePostList();
            originalTitle = post.getTitle().getPlainText();
            textArea.getTextBuffer().getUndoBuffer().setCurrentStateClean();
            textChanged();
            publishAction.setEnabled(post.isDraft());
            if (shouldPublish) {
                maybeClose();
            }
        } catch (Exception ex) {
            SimpleDialog.showDetails(this, shouldPublish ? "Failed to publish post." : "Failed to save draft.", ex);
        }
    }
}
