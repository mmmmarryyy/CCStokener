package net.sf.refactorit.ui.javadoc;

import net.sf.refactorit.classfile.ClassUtil;
import net.sf.refactorit.classmodel.BinArrayType;
import net.sf.refactorit.classmodel.BinCIType;
import net.sf.refactorit.classmodel.BinConstructor;
import net.sf.refactorit.classmodel.BinField;
import net.sf.refactorit.classmodel.BinInterface;
import net.sf.refactorit.classmodel.BinMember;
import net.sf.refactorit.classmodel.BinMethod;
import net.sf.refactorit.classmodel.BinModifier;
import net.sf.refactorit.classmodel.BinParameter;
import net.sf.refactorit.classmodel.BinTypeRef;
import net.sf.refactorit.classmodel.Project;
import net.sf.refactorit.common.util.StringUtil;
import net.sf.refactorit.loader.Comment;
import net.sf.refactorit.refactorings.javadoc.Javadoc;
import net.sf.refactorit.source.format.BinFormatter;
import net.sf.refactorit.source.format.BinModifierFormatter;
import net.sf.refactorit.ui.DialogManager;
import net.sf.refactorit.ui.UIResources;
import net.sf.refactorit.ui.dialog.AWTContext;
import net.sf.refactorit.ui.errors.ErrorsTab;
import net.sf.refactorit.ui.module.RefactorItContext;
import net.sf.refactorit.ui.tree.JTypeInfoJavaDoc;
import net.sf.refactorit.utils.ClasspathUtil;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class TypeInfoJavadoc implements JTypeInfoJavaDoc {

    static int maxWidth = 500;

    static int maxHeight = 300;

    private static List workingPaths = new ArrayList();

    String urlName = "";

    JEditorPane textHolder;

    private boolean typeInfo;

    private JScrollPane javadoc;

    private BinMember target;

    private BinTypeRef currentType;

    private RootPaneContainer container;

    private RefactorItContext context;

    private static final String[] docDirs = new String[] { "api", "docs", "doc", "documentation", "javadoc" };

    private static final List visibleInstances = new ArrayList();

    static {
        UIResources.addHidePopupsListener(new Runnable() {

            public void run() {
                hideAllVisibleInstances();
            }
        });
    }

    public static void clearCache() {
        workingPaths.clear();
    }

    public TypeInfoJavadoc(RefactorItContext context, BinMember target) {
        this.context = context;
        this.target = target;
        if (target instanceof BinCIType) {
            currentType = ((BinCIType) target).getTypeRef();
        } else {
            currentType = target.getOwner();
        }
    }

    public void showJavadocAction() {
        if (!(context instanceof AWTContext)) {
            return;
        }
        Window window = ((AWTContext) context).getWindow();
        if (window == null) {
            return;
        }
        if (!(window instanceof RootPaneContainer)) {
            return;
        }
        container = (RootPaneContainer) window;
        showJavadoc(false);
        constructJavadoc(window);
        if (updateJavadocText(target, context.getProject())) {
            updateJavadocBounds(context.getPoint(), window, container);
            showJavadoc(true);
        }
        ErrorsTab.addNew(context);
    }

    public static void hideAllVisibleInstances() {
        while (visibleInstances.size() > 0) {
            ((TypeInfoJavadoc) visibleInstances.get(0)).showJavadoc(false);
        }
    }

    synchronized void showJavadoc(boolean show) {
        if (this.javadoc != null) {
            java.awt.Container parent;
            if (show) {
                container.getRootPane().getLayeredPane().add(this.javadoc, JLayeredPane.POPUP_LAYER);
                parent = this.javadoc.getParent();
                visibleInstances.add(this);
            } else {
                parent = this.javadoc.getParent();
                container.getRootPane().getLayeredPane().remove(this.javadoc);
                visibleInstances.remove(this);
            }
            this.textHolder.setCaretPosition(0);
            parent.invalidate();
            parent.validate();
            parent.repaint();
        }
    }

    /**
   * @param member for which we want JavaDoc
   * @param project project
   *
   * @return true if it is worth of showing popup
   */
    private boolean updateJavadocText(final BinMember member, final Project project) {
        String text = getJavadocInfo(member, project);
        this.textHolder.setText(text);
        return true;
    }

    /**
   * @param member for which we want JavaDoc
   * @param project project
   *
   * @return String containing javadoc information as html document
   * String
   */
    private String getJavadocInfo(final BinMember member, final Project project) {
        String text;
        String comment = findInternalJavadoc(member);
        BinMember searchMember = member;
        while (comment == null && searchMember != null && searchMember instanceof BinMethod) {
            List overrides = ((BinMethod) searchMember).findOverrides();
            if (overrides.size() > 0) {
                searchMember = (BinMethod) overrides.get(0);
                comment = findInternalJavadoc(searchMember);
            } else {
                break;
            }
        }
        if (comment != null) {
            text = getMemberHeader(member);
            if (comment.length() > 0) {
                comment = parseJavadoc(comment, member);
                if (comment != null) {
                    text += comment;
                }
            } else {
                text += generateThrowsHeader(member);
            }
        } else {
            text = getPackageLink(member);
            if (member.getCompilationUnit() == null) {
                comment = findExternalJavadoc(project, member);
            }
            if (comment != null && comment.length() > 0) {
                text += comment;
            } else {
                text = getMemberHeader(member) + generateThrowsHeader(member);
            }
        }
        text = wrapStringWithHTML(text);
        return text;
    }

    private String wrapStringWithHTML(String text) {
        return "<HTML><BODY>" + text + "</BODY></HTML>";
    }

    private String getPackageLink(BinMember member) {
        String owner;
        if (member instanceof BinCIType) {
            if (((BinCIType) member).isInnerType()) {
                owner = member.getOwner().getQualifiedName();
            } else {
                owner = member.getPackage().getQualifiedName();
                return owner.length() > 0 ? (owner + "\n") : "";
            }
        } else {
            if (member.getOwner().isArray()) {
                owner = ((BinArrayType) member.getOwner().getBinType()).getArrayType().getQualifiedName();
            } else {
                owner = member.getOwner().getQualifiedName();
            }
        }
        if (owner.length() > 0) {
            return "<A HREF=\"" + owner + "\">" + owner + "</A>\n";
        }
        return "";
    }

    private String createAnchorForType(final String type, String name) {
        boolean found = false;
        for (int i = 0, max = ClassUtil.primitiveNames.length; i < max; i++) {
            if (ClassUtil.primitiveNames[i].equals(type)) {
                found = true;
                break;
            }
        }
        if (!found) {
            return "<A HREF=\"" + type + "\">" + name + "</A>";
        }
        return type;
    }

    private String formatType(BinTypeRef ref) {
        StringBuffer buf = new StringBuffer();
        if (ref.isArray()) {
            BinArrayType type = (BinArrayType) ref.getBinType();
            buf.append(createAnchorForType(type.getArrayType().getQualifiedName(), type.getArrayType().getName()));
            buf.append(type.getDimensionString());
        } else {
            buf.append(createAnchorForType(ref.getQualifiedName(), ref.getName()));
        }
        return buf.toString();
    }

    private String getMemberHeader(BinMember member) {
        String memberHeader = getPackageLink(member) + "<PRE>";
        String modTypeAndName;
        modTypeAndName = new BinModifierFormatter(getCommonMemberModifiers(member)).print();
        int offset = modTypeAndName.length();
        if (modTypeAndName.length() > 0) {
            modTypeAndName += "&nbsp;";
            offset += 1;
        }
        if (member instanceof BinCIType) {
            modTypeAndName += member.getMemberType();
            offset += member.getMemberType().length();
        } else if (member instanceof BinField) {
            modTypeAndName += formatType(((BinField) member).getTypeRef());
            offset += ((BinField) member).getTypeRef().getName().length();
        } else if (member instanceof BinMethod && !(member instanceof BinConstructor)) {
            modTypeAndName += formatType(((BinMethod) member).getReturnType());
            offset += ((BinMethod) member).getReturnType().getName().length();
        }
        String name;
        if (member instanceof BinCIType) {
            name = "&nbsp;<B>" + BinFormatter.formatNotQualified(((BinCIType) member).getTypeRef()) + "</B>";
        } else {
            name = "&nbsp;<B>" + member.getName() + "</B>";
        }
        offset += name.length();
        modTypeAndName += name;
        memberHeader += modTypeAndName;
        if (member instanceof BinCIType) {
            BinTypeRef[] supers = ((BinCIType) member).getTypeRef().getSupertypes();
            String extend = "";
            String implement = "";
            for (int i = 0, max = supers.length; i < max; i++) {
                final BinCIType type = supers[i].getBinCIType();
                if (type.isClass() || ((BinCIType) member).isInterface() || ((BinCIType) member).isEnum()) {
                    if (extend.length() == 0) {
                        extend = "\nextends&nbsp;";
                    } else {
                        extend += ",&nbsp;";
                    }
                    extend += "<A HREF=\"" + type.getQualifiedName() + "\">" + type.getNameWithAllOwners() + "</A>";
                } else {
                    if (((BinCIType) member).isClass()) {
                        if (implement.length() == 0) {
                            implement = "\nimplements&nbsp;";
                        } else {
                            implement += ",&nbsp;";
                        }
                        implement += "<A HREF=\"" + type.getQualifiedName() + "\">" + type.getNameWithAllOwners() + "</A>";
                    }
                }
            }
            memberHeader += extend + implement;
        } else if (member instanceof BinMethod) {
            memberHeader += "(";
            offset++;
            BinParameter[] params = ((BinMethod) member).getParameters();
            final String nbspLine = generateNbsp(offset);
            for (int i = 0, max = params.length; i < max; i++) {
                if (i > 0) {
                    memberHeader += ",&nbsp;";
                    if (params.length > 2) {
                        memberHeader += "\n" + nbspLine;
                    }
                }
                memberHeader += formatType(params[i].getTypeRef());
                if (params[i].getName() != null) {
                    memberHeader += "&nbsp;" + params[i].getName();
                }
            }
            memberHeader += ")";
        } else if (member instanceof BinField) {
        }
        memberHeader += "</PRE>\n";
        return memberHeader;
    }

    private static int getCommonMemberModifiers(BinMember member) {
        int modifiers = member.getModifiers();
        if (!(member instanceof BinMethod)) {
            modifiers &= ~BinModifier.SYNCHRONIZED;
        }
        if (member instanceof BinInterface) {
            modifiers &= ~BinModifier.ABSTRACT;
        }
        return modifiers;
    }

    private String generateThrowsHeader(BinMember member) {
        String result = "";
        if (!(member instanceof BinMethod)) {
            return result;
        }
        BinMethod.Throws[] throwses = ((BinMethod) member).getThrows();
        for (int i = 0; i < throwses.length; i++) {
            if (i == 0) {
                result += "\n" + "throws ";
            } else {
                result += ", ";
                if (i > 3) {
                    result += "\n";
                }
            }
            result += formatType(throwses[i].getException());
        }
        return result;
    }

    private String generateThrowsSection(BinMember member) {
        if (!(member instanceof BinMethod)) {
            return "";
        }
        String result = "";
        BinMethod.Throws[] throwses = ((BinMethod) member).getThrows();
        for (int i = 0; i < throwses.length; i++) {
            if (i == 0) {
                result = "<DT><B>Throws:</B></DT><DD>";
            } else {
                result += ", ";
            }
            result += formatType(throwses[i].getException());
        }
        return result;
    }

    private String generateNbsp(int amount) {
        if (amount <= 0) {
            return "";
        }
        StringBuffer result = new StringBuffer(amount * 6 + 1);
        for (int i = 0; i < amount; i++) {
            result.append("&nbsp;");
        }
        return result.toString();
    }

    private void updateJavadocBounds(final Point point, final Component owner, final RootPaneContainer frame) {
        if (owner.getParent() != null) {
            this.javadoc.setMaximumSize(owner.getParent().getSize());
        } else {
            this.javadoc.setMaximumSize(owner.getSize());
        }
        int x, y;
        if (point == null) {
            x = 30;
            y = 60;
        } else {
            x = (int) point.getX();
            y = (int) point.getY();
        }
        Component parent = frame.getLayeredPane();
        Dimension viewportSize = textHolder.getPreferredScrollableViewportSize();
        textHolder.setSize(viewportSize);
        Dimension parentSize = parent.getSize();
        Point newPoint = SwingUtilities.convertPoint(owner, x, y, parent);
        x = (int) newPoint.getX();
        y = (int) newPoint.getY() + 7;
        maxWidth = (int) parentSize.getWidth() - (int) this.javadoc.getVerticalScrollBar().getSize().getWidth() - 1;
        maxHeight = (int) parentSize.getHeight() - (int) this.javadoc.getHorizontalScrollBar().getSize().getHeight() - 1;
        Dimension prefSize = this.javadoc.getPreferredSize();
        if (x + prefSize.getWidth() > parentSize.getWidth()) {
            x = (int) (parentSize.getWidth() - prefSize.getWidth());
        }
        if (x < 0) {
            x = 0;
        }
        if (y + prefSize.getHeight() > parentSize.getHeight()) {
            y = (int) (parentSize.getHeight() - prefSize.getHeight());
        }
        if (y < 0) {
            y = 0;
        }
        this.javadoc.setLocation(x, y);
        this.javadoc.setSize(prefSize);
    }

    private void constructJavadoc(Window frame) {
        if (this.javadoc != null) {
            Window oldFrame = DialogManager.findOwnerWindow(this.javadoc.getParent());
            if (frame == oldFrame) {
                return;
            }
        }
        this.javadoc = createJavaDocScrollPane();
    }

    protected JScrollPane createJavaDocScrollPane() {
        this.textHolder = createJavadocEditorPane();
        JScrollPane javadoc = new JScrollPane(this.textHolder);
        javadoc.setBackground(new Color(255, 255, 217));
        javadoc.setOpaque(true);
        javadoc.setRequestFocusEnabled(true);
        javadoc.setBorder(BorderFactory.createLineBorder(Color.black));
        return javadoc;
    }

    private JEditorPane createJavadocEditorPane() {
        final JEditorPane textHolder = new JEditorPane() {

            public Dimension getPreferredScrollableViewportSize() {
                Dimension dimension = getSize();
                if (dimension.height <= 0 || dimension.width <= 0) {
                    setSize(500, 300);
                }
                int i = getDocument().getLength();
                if (i >= 0) {
                    try {
                        Rectangle rectangle = modelToView(i);
                        if (rectangle != null) {
                            int width = Math.min(500, maxWidth);
                            int height = Math.max(55, Math.min(300, rectangle.y));
                            return new Dimension(width, height);
                        }
                    } catch (BadLocationException e) {
                    }
                }
                return new Dimension(500, 300);
            }
        };
        textHolder.setEditorKit(new HTMLEditorKit());
        textHolder.setEditable(false);
        textHolder.setBackground(new Color(255, 255, 215));
        textHolder.setOpaque(true);
        textHolder.setRequestFocusEnabled(true);
        textHolder.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent hyperlinkevent) {
                HyperlinkEvent.EventType eventtype = hyperlinkevent.getEventType();
                if (eventtype == HyperlinkEvent.EventType.ACTIVATED) {
                    urlName = hyperlinkevent.getDescription();
                    if (urlName.length() > 0) {
                        resolveHTMLLink(urlName);
                        urlName = "";
                        textHolder.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                } else if (eventtype == HyperlinkEvent.EventType.ENTERED) {
                    urlName = hyperlinkevent.getDescription();
                    textHolder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else if (eventtype == HyperlinkEvent.EventType.EXITED) {
                    urlName = "";
                    textHolder.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        textHolder.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent evt) {
                if (urlName.length() <= 0) {
                    showJavadoc(false);
                }
            }
        });
        return textHolder;
    }

    void resolveHTMLLink(String name) {
        BinMember member = null;
        BinTypeRef ref = null;
        while (name.startsWith("../")) {
            name = name.substring(3);
        }
        if (name.endsWith(".html")) {
            name = name.substring(0, name.length() - ".html".length());
        } else {
            name = StringUtil.replace(name, ".html#", "#");
        }
        name = name.replace('\\', '.');
        name = name.replace('/', '.');
        Javadoc.SplitName splitName = new Javadoc.SplitName(name);
        if (!splitName.isSplitNameValid()) {
            return;
        }
        if (splitName.ownerIsPresent()) {
            String ownerName = splitName.getOwnerName();
            int pos;
            do {
                ref = Javadoc.tryToResolveName(ownerName, currentType, this.target.getCompilationUnit(), this.target.getProject());
                if (ref != null) {
                    break;
                }
                pos = ownerName.lastIndexOf('.');
                if (pos != -1) {
                    ownerName = ownerName.substring(0, pos) + '$' + ownerName.substring(pos + 1);
                }
            } while (pos != -1);
            if (ref == null) {
                return;
            }
            currentType = ref;
        }
        if (splitName.memberIsPresent()) {
            if (!splitName.isMethod()) {
                member = lookForSameField(splitName);
            }
            if (member == null) {
                member = lookForSameMethod(splitName);
                if (member == null) {
                    return;
                }
            }
        } else {
            member = currentType.getBinCIType();
        }
        if (!typeInfo) {
            Window window = (Window) container;
            showJavadoc(false);
            constructJavadoc(window);
            if (updateJavadocText(member, this.target.getProject())) {
                updateJavadocBounds(context.getPoint(), window, container);
                showJavadoc(true);
            }
        } else {
            updateJavaDoc(member, this.target.getProject());
        }
    }

    private BinMember lookForSameMethod(Javadoc.SplitName splitName) {
        String name = splitName.getMemberName();
        String params = splitName.getParams();
        BinMethod[] methods = currentType.getBinCIType().getAccessibleMethods(name, currentType.getBinCIType());
        for (int i = 0; i < methods.length; i++) {
            if (Javadoc.equalParameters(methods[i], params)) {
                return methods[i];
            }
        }
        return null;
    }

    private BinMember lookForSameField(Javadoc.SplitName splitName) {
        String name = splitName.getMemberName();
        List fields = currentType.getBinCIType().getAccessibleFields(currentType.getBinCIType());
        BinField field;
        for (int i = 0, max = fields.size(); i < max; i++) {
            field = (BinField) fields.get(i);
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private String parseJavadoc(String comment, BinMember member) {
        final BufferedReader source = new BufferedReader(new StringReader(comment));
        try {
            final Javadoc javadoc = Javadoc.parse(source, 0, 0);
            if (javadoc == null) {
                return null;
            }
            StringBuffer result = new StringBuffer(32);
            Iterator tags = javadoc.getStandaloneTags().iterator();
            StringBuffer description = null;
            StringBuffer deprecated = null;
            StringBuffer params = null;
            StringBuffer returns = null;
            StringBuffer exceptions = null;
            StringBuffer seealso = null;
            StringBuffer since = null;
            StringBuffer version = null;
            StringBuffer author = null;
            while (tags.hasNext()) {
                final Javadoc.Tag tag = (Javadoc.Tag) tags.next();
                if ("description".equals(tag.getName())) {
                    description = new StringBuffer("");
                    description.append(tag.getHTMLRepresentation());
                } else if ("see".equals(tag.getName())) {
                    if (seealso == null) {
                        seealso = new StringBuffer("<DT><B>See Also:</B></DT><DD>");
                    } else {
                        seealso.append(", ");
                    }
                    seealso.append(tag.getHTMLRepresentation());
                } else if ("param".equals(tag.getName())) {
                    if (params == null) {
                        params = new StringBuffer("<DT><B>Parameters:</B></DT><DD>");
                    } else {
                        params.append("<DD>");
                    }
                    params.append(tag.getHTMLRepresentation());
                } else if ("exception".equals(tag.getName()) || "throws".equals(tag.getName())) {
                    if (exceptions == null) {
                        exceptions = new StringBuffer("<DT><B>Throws:</B></DT><DD>");
                    } else {
                        exceptions.append("<DD>");
                    }
                    exceptions.append(tag.getHTMLRepresentation());
                } else if ("deprecated".equals(tag.getName())) {
                    if (deprecated == null) {
                        deprecated = new StringBuffer("<DD><B>Deprecated.</B>&nbsp;");
                    }
                    deprecated.append("<I>");
                    deprecated.append(tag.getHTMLRepresentation());
                    deprecated.append("</I>\n");
                } else if ("return".equals(tag.getName())) {
                    if (returns == null) {
                        returns = new StringBuffer("<DT><B>Returns:</B><DD>");
                    }
                    returns.append(tag.getHTMLRepresentation());
                } else if ("since".equals(tag.getName())) {
                    if (since == null) {
                        since = new StringBuffer("<DT><B>Since:</B>");
                    }
                    since.append("<DD>");
                    since.append(tag.getHTMLRepresentation());
                    since.append("</DD>\n");
                } else if ("author".equals(tag.getName())) {
                    if (author == null) {
                        author = new StringBuffer("<DT><B>Author:</B>");
                    }
                    author.append("<DD>");
                    author.append(tag.getHTMLRepresentation());
                    author.append("</DD>\n");
                } else if ("version".equals(tag.getName())) {
                    if (version == null) {
                        version = new StringBuffer("<DT><B>Version:</B>");
                    }
                    version.append("<DD>");
                    version.append(tag.getHTMLRepresentation());
                    version.append("</DD>\n");
                }
            }
            boolean hasExtra = params != null || returns != null || since != null || exceptions != null || seealso != null || version != null || author != null || description != null;
            if (hasExtra) {
                result.append("<DD><DL>\n");
            }
            if (deprecated != null) {
                result.insert(0, deprecated);
            }
            if (description != null) {
                result.append(description.toString());
            }
            if (params != null) {
                result.append(params.toString());
            }
            if (returns != null) {
                result.append(returns.toString());
            }
            if (exceptions != null) {
                result.append(exceptions.toString());
            } else {
                if (!hasExtra) {
                    result.append("<DD><DL>\n");
                    hasExtra = true;
                }
                result.append(generateThrowsSection(member));
            }
            if (since != null) {
                result.append(since.toString());
            }
            if (version != null) {
                result.append(version.toString());
            }
            if (author != null) {
                result.append(author.toString());
            }
            if (seealso != null) {
                result.append(seealso.toString());
            }
            if (hasExtra) {
                result.append("</DL></DD>\n");
            }
            return result.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private String findInternalJavadoc(BinMember member) {
        Comment comment = Comment.findFor(member);
        if (comment != null) {
            return comment.getText();
        }
        return null;
    }

    private String findExternalJavadoc(final Project project, final BinMember member) {
        File docFile = null;
        if (project.getPaths().getJavadocPath() != null) {
            docFile = findJavadocInPath(project.getPaths().getJavadocPath().getStringForm(), member);
        }
        if (docFile == null) {
            docFile = findJavadocInPath(project.getPaths().getClassPath().getStringForm(), member);
        }
        if (docFile == null) {
            docFile = findJavadocInPath(ClasspathUtil.getDefaultClasspath(), member);
        }
        if (docFile == null && (member.getPackage().getQualifiedName().startsWith("java.") || member.getPackage().getQualifiedName().startsWith("javax."))) {
            DialogManager.getInstance().showInformation(context, "javadoc.jdk_docs_missing", "The RefactorIT can not show detailed information for\n" + "the item given, because Javadoc search path\n" + "does not point to any directory containing JDK documentation.");
        }
        String result = null;
        if (docFile != null) {
            result = new JavaHtmlDocParser().getJavadoc(docFile, member);
        } else {
        }
        return result;
    }

    private File findJavadocInPath(String searchPath, BinMember member) {
        boolean isUrl = false;
        String memberPath = File.separatorChar + member.getPackage().getQualifiedName().replace('.', File.separatorChar) + File.separatorChar;
        if (member instanceof BinCIType) {
            memberPath += member.getNameWithAllOwners() + ".html";
        } else {
            memberPath += member.getOwner().getBinCIType().getNameWithAllOwners() + ".html";
        }
        memberPath = memberPath.replace('$', '.');
        File docFile = null;
        for (int i = 0, max = workingPaths.size(); i < max; i++) {
            docFile = isExisting(((String) workingPaths.get(i)) + memberPath);
            if (docFile != null) {
                return docFile;
            }
        }
        final StringTokenizer tokens = new StringTokenizer(searchPath, File.pathSeparator);
        upper: while (tokens.hasMoreTokens()) {
            String path = tokens.nextToken();
            if (path.equals("http") || path.equals("https")) {
                if (tokens.hasMoreTokens()) {
                    String pathSecondPart = tokens.nextToken();
                    path = path + File.separator + pathSecondPart;
                }
            }
            isUrl = isUrl(path);
            char separator = File.separatorChar;
            if (isUrl) {
                separator = '/';
                memberPath = memberPath.replace(File.separatorChar, '/');
            }
            if (path.endsWith(separator + "")) {
                path = path.substring(0, path.length() - 1);
            }
            if ((docFile = applyHeuristicsToPath(path, memberPath, separator)) != null) {
                break upper;
            }
            if (isUrl) {
                continue upper;
            }
            int ind = 0;
            while ((ind = path.lastIndexOf(separator)) >= 0) {
                path = path.substring(0, ind);
                if ((docFile = applyHeuristicsToPath(path, memberPath, separator)) != null) {
                    break upper;
                }
            }
        }
        if (docFile != null) {
            String path = docFile.getAbsolutePath();
            if (!isUrl) {
                path = path.substring(0, path.length() - memberPath.length());
            }
            if (!workingPaths.contains(path)) {
                workingPaths.add(path);
            }
        }
        return docFile;
    }

    private File applyHeuristicsToPath(String path, final String memberPath, final char separator) {
        File docFile = null;
        if ((docFile = isExisting(path + memberPath)) == null) {
            for (int i = 0; i < docDirs.length; i++) {
                if ((docFile = isExisting(path + separator + docDirs[i] + memberPath)) != null) {
                    break;
                } else if ((docFile = isExisting(path + separator + docDirs[i] + separator + "api" + memberPath)) != null) {
                    break;
                }
            }
        }
        return docFile;
    }

    private boolean isUrl(String filePathName) {
        return filePathName.toLowerCase().startsWith("http:") || filePathName.toLowerCase().startsWith("https:") || filePathName.toLowerCase().startsWith("www.");
    }

    /** The streams should be closed by the caller after calling this method
   * @param in input stream
   * @param out outpuit stream
   * @throws IOException
   */
    private void copy(InputStream in, OutputStream out) throws IOException {
        int count = 0;
        byte buffer[] = new byte[50000];
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        out.flush();
    }

    private File downloadIntoTempFile(String filePathName) {
        try {
            File tempFile = File.createTempFile("refactorit", "cache");
            tempFile.deleteOnExit();
            URL url = new URL(filePathName);
            InputStream in = null;
            OutputStream out = null;
            tuneTimeouts();
            try {
                in = url.openStream();
                out = new FileOutputStream(tempFile);
                copy(in, out);
            } catch (Exception e) {
                System.err.println("missing doc: " + filePathName);
                tempFile = null;
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                restoreTimeouts();
            }
            return tempFile;
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private File isExisting(final String filePathName) {
        if (isUrl(filePathName)) {
            return downloadIntoTempFile(filePathName);
        } else {
            File docFile = new File(filePathName);
            if (docFile.canRead()) {
                return docFile;
            } else {
                return null;
            }
        }
    }

    private String oldConnectTimeout;

    private String oldReadTimeout;

    private void tuneTimeouts() {
        oldConnectTimeout = System.getProperty("sun.net.client.defaultConnectTimeout");
        oldReadTimeout = System.getProperty("sun.net.client.defaultReadTimeout");
        System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");
    }

    private void restoreTimeouts() {
        Properties properties = System.getProperties();
        if (oldConnectTimeout == null) {
            properties.remove("sun.net.client.defaultConnectTimeout");
        } else {
            properties.setProperty("sun.net.client.defaultConnectTimeout", oldConnectTimeout);
        }
        if (oldReadTimeout == null) {
            properties.remove("sun.net.client.defaultReadTimeout");
        } else {
            properties.setProperty("sun.net.client.defaultReadTimeout", oldReadTimeout);
        }
        System.setProperties(properties);
    }

    /**
   * Update javadoc info on the panel where this instance of
   * JTypeInfoJavaDoc was docked into.
   *
   * @param member the member for what the javadoc info
   * is needed to show.
   * @param project used to get javadoc info for specified
   * BinMember
   */
    public void updateJavaDoc(BinMember member, Project project) {
        if (member instanceof BinCIType) {
            currentType = ((BinCIType) member).getTypeRef();
        } else {
            currentType = member.getOwner();
        }
        updateJavadocText(member, project);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                textHolder.scrollRectToVisible(new Rectangle(0, 0));
            }
        });
    }

    /**
   * Dock this instance of JTypeInfoJavaDoc into panel,
   * then use {@link #updateJavaDoc(BinMember, Project)} to show
   * javadoc info on that panel.
   *
   * @param panel into where this instance of JTypeInfoJavaDoc
   * is being docked. i.e. Added into that panel.
   */
    public void dockInto(JPanel panel) {
        typeInfo = true;
        panel.removeAll();
        panel.add(createJavaDocScrollPane());
    }
}
