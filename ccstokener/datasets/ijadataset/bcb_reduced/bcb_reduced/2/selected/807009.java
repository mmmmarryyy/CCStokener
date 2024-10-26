package org.form4G.script;

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JFrame;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.View;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.parser.ParserDelegator;
import org.form4G.gui.component.MsPane;

/**
 * <a href="WebBrowser.js">ver tanvien</a>
 * 
 * @author efrigerio
 *
 */
public class DHTMLDocument extends HTMLDocument implements Document {

    public static Tag TAG_IFRAME = new Tag() {

        public boolean isBlock() {
            return true;
        }

        public boolean breaksFlow() {
            return true;
        }

        public boolean isPreformatted() {
            return false;
        }

        public String toString() {
            return "IFRAME";
        }
    };

    private static final long serialVersionUID = -3483297927318331886L;

    private ScriptEngine scriptEngine = null;

    private View rootView;

    private Hashtable<Integer, Timer> hashTimerTask = null;

    @SuppressWarnings("unused")
    private DHTMLDocument() {
        super();
    }

    public DHTMLDocument(StyleSheet styles) {
        super(styles);
        hashTimerTask = new Hashtable<Integer, Timer>();
    }

    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public View getRootView() {
        return (this.rootView == null) ? null : gettRootView(this.rootView);
    }

    private View gettRootView(View view) {
        View rr = (view.getParent() == null) ? view : gettRootView(view.getParent());
        return rr;
    }

    public synchronized void nitializerRootView(View rootView) {
        this.rootView = rootView;
    }

    private class MyTimerTask extends TimerTask {

        private Integer id;

        private String code;

        public MyTimerTask(Integer id, String code) {
            this.id = id;
            this.code = code;
        }

        public void run() {
            try {
                eval(null, code);
            } catch (Exception e) {
                e.printStackTrace();
                clearInterval(id);
            }
        }
    }

    public void clearAllInterval() {
        for (Enumeration<Timer> e = hashTimerTask.elements(); e.hasMoreElements(); ) {
            e.nextElement().cancel();
        }
        hashTimerTask.clear();
    }

    public void clearInterval(int id) {
        Timer timer = hashTimerTask.remove(new Integer(id));
        if (timer != null) {
            timer.cancel();
        }
    }

    public void setTimeout(final String code, final long sleep) {
        Thread runTimeout = new Thread() {

            public void run() {
                try {
                    Thread.sleep(sleep);
                    eval(null, code);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        runTimeout.start();
    }

    public int setInterval(final String code, long period) {
        Random rand = new Random();
        int id = (rand.nextLong() + "-" + rand.nextLong() + "-" + rand.nextLong() + "-" + rand.nextLong() + "-" + System.currentTimeMillis()).hashCode();
        Integer ID = new Integer(id);
        Timer timer = new Timer(true);
        timer.schedule(new MyTimerTask(ID, code), 0, period);
        hashTimerTask.put(ID, timer);
        return id;
    }

    public void starScript(MsPane pane) throws IOException, ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("JavaScript");
        this.scriptEngine.getContext().setReader(new InputStreamReader(System.in));
        this.scriptEngine.getContext().setWriter(new PrintWriter(System.out));
        this.scriptEngine.getContext().setErrorWriter(new PrintWriter(System.err));
        this.scriptEngine.put("frame", getFrame(pane));
        this.scriptEngine.put("jEPane", pane);
        this.scriptEngine.put("printDoc", null);
        eval(null, DHTMLDocument.class.getResource("WebBrowser.js"));
        Vector<Mydata> vtrSrc = new Vector<Mydata>();
        for (DHTMLElementIterator iterator = getElementIterator(HTML.Tag.SCRIPT); iterator.isValid(); iterator.next()) {
            Element element = iterator.getElement();
            AttributeSet attributes = iterator.getAttributes();
            String strSrc = (String) attributes.getAttribute(HTML.Attribute.SRC);
            if (strSrc != null) {
                try {
                    vtrSrc.add(new Mydata(element, new URL(strSrc)));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        for (Enumeration<Mydata> e = vtrSrc.elements(); e.hasMoreElements(); ) {
            Mydata mydata = e.nextElement();
            eval(mydata.element, mydata.url);
        }
        Vector<Mydata> vtrCode = new Vector<Mydata>();
        for (DHTMLElementIterator iterator = getElementIterator(HTML.Tag.SCRIPT); iterator.isValid(); iterator.next()) {
            Element element = iterator.getElement();
            AttributeSet attributes = iterator.getAttributes();
            String strCode = (String) attributes.getAttribute(HTML.Attribute.CODE);
            if (strCode != null) {
                vtrCode.add(new Mydata(element, strCode));
            }
        }
        for (Enumeration<Mydata> e = vtrCode.elements(); e.hasMoreElements(); ) {
            Mydata mydata = e.nextElement();
            eval(mydata.element, mydata.strCode);
        }
        Vector<Mydata> vtrOnload = new Vector<Mydata>();
        ElementIterator iterator = new ElementIterator(this);
        Element element = iterator.first();
        while (element != null) {
            element.getStartOffset();
            AttributeSet attributeSet = element.getAttributes();
            for (Enumeration<?> e = attributeSet.getAttributeNames(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                if ((key.toString().equalsIgnoreCase("onload")) && (attributeSet.getAttribute(key) != null)) {
                    vtrOnload.add(new Mydata(element, attributeSet.getAttribute(key).toString()));
                }
            }
            element = iterator.next();
        }
        for (Enumeration<Mydata> e = vtrOnload.elements(); e.hasMoreElements(); ) {
            Mydata mydata = e.nextElement();
            eval(mydata.element, mydata.strCode);
        }
    }

    public ContextElement getContextElement(String id) {
        ContextElement rr = null;
        Element element = getElement(id);
        if (element != null) rr = new ContextElement(element);
        return rr;
    }

    public Element getElement(String id) {
        Element rr = super.getElement(id);
        if (rr == null) {
            View view = getView(getRootView(), id);
            rr = (view == null) ? null : view.getElement();
        }
        return rr;
    }

    private View getView(View view, String id) {
        View rr = null;
        int parentCount = view.getViewCount();
        for (int s_for = 0; (s_for != parentCount) && (rr == null); s_for++) {
            View viewChild = view.getView(s_for);
            Element element = viewChild.getElement();
            String propertyName = (String) element.getAttributes().getAttribute(HTML.getAttributeKey("id"));
            if ((propertyName != null) && (propertyName.equalsIgnoreCase(id))) rr = viewChild; else rr = getView(viewChild, id);
        }
        return rr;
    }

    private class Mydata {

        public Element element = null;

        public String strCode = null;

        public URL url = null;

        public Mydata(Element element, String strCode) {
            this.element = element;
            this.strCode = strCode;
        }

        public Mydata(Element element, URL url) {
            this.element = element;
            this.url = url;
        }
    }

    public DHTMLElementIterator getElementIterator(Tag tag) {
        return new DHTMLElementIterator(tag, this);
    }

    private void eval(Element element, String strCode) throws ScriptException, IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        this.scriptEngine.put("printDoc", new PrintStream(os));
        this.scriptEngine.eval(strCode);
        os.close();
        String str = os.toString();
        this.scriptEngine.put("printDoc", null);
        if ((str != null) && (str.length() > 0) && (element != null)) {
            try {
                setParser(new ParserDelegator());
                insertAfterEnd(element, str);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private void eval(Element element, URL url) throws IOException, ScriptException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        this.scriptEngine.put("printDoc", new PrintStream(os));
        InputStreamReader isr = new InputStreamReader(url.openStream());
        this.scriptEngine.eval(isr);
        isr.close();
        os.close();
        String str = os.toString();
        System.out.println("load: " + url);
        this.scriptEngine.put("printDoc", null);
        if ((str != null) && (str.length() > 0) && (element != null)) {
            try {
                setParser(new ParserDelegator());
                insertAfterEnd(element, str);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private JFrame getFrame(Component pane) {
        JFrame rr = null;
        if (pane instanceof JFrame) rr = (JFrame) pane; else if (pane.getParent() == null) rr = null; else rr = getFrame(pane.getParent());
        return rr;
    }

    public HTMLEditorKit.ParserCallback getReader(int pos) {
        return new MyParserCallback(pos);
    }

    private class MyParserCallback extends HTMLReader {

        public MyParserCallback(int offset) {
            super(offset);
            TagAction ba = new TagAction() {

                public void start(HTML.Tag t, MutableAttributeSet a) {
                    addSpecialElement(t, a);
                }

                public void end(HTML.Tag t) {
                }
            };
            super.registerTag(HTML.Tag.SCRIPT, ba);
            super.registerTag(TAG_IFRAME, new HiddenAction());
            Object desc = getProperty(Document.StreamDescriptionProperty);
            if (desc instanceof URL) {
                setBase((URL) desc);
            }
        }

        protected void addSpecialElement(HTML.Tag tag, MutableAttributeSet attribute) {
            boolean isAddSpecialElement = true;
            ElementSpec parse = null;
            if (parseBuffer.size() > 0) {
                parse = parseBuffer.get(parseBuffer.size() - 1);
            }
            for (Enumeration<?> e = attribute.getAttributeNames(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                Object val = attribute.getAttribute(key);
                if (key.toString().equalsIgnoreCase("src")) {
                    try {
                        URL url = new URL(getBase(), val.toString());
                        val = url.toString();
                    } catch (MalformedURLException e1) {
                        e1.printStackTrace();
                    }
                    attribute.removeAttribute(key);
                    attribute.addAttribute(key, val);
                }
                if ((key.toString().equalsIgnoreCase("comment")) && (parse != null)) {
                    Object valueType = (parse.getAttributes() == null) ? null : parse.getAttributes().getAttribute(HTML.Attribute.TYPE);
                    Object valueLanguage = (parse.getAttributes() == null) ? null : parse.getAttributes().getAttribute(HTML.Attribute.LANGUAGE);
                    if (((valueType != null) && (valueType.toString().equalsIgnoreCase("text/JavaScript"))) || ((valueLanguage != null) && (valueLanguage.toString().equalsIgnoreCase("JavaScript")))) {
                        ((MutableAttributeSet) parse.getAttributes()).addAttribute(HTML.Attribute.CODE, val);
                        isAddSpecialElement = true;
                    }
                }
            }
            if (isAddSpecialElement) {
                super.addSpecialElement(tag, attribute);
            }
        }

        protected void blockOpen(HTML.Tag tag, MutableAttributeSet attribute) {
            super.blockOpen(tag, attribute);
        }

        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attribute, int pos) {
            super.handleSimpleTag(tag, attribute, pos);
        }

        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attribute, int pos) {
            super.handleStartTag(tag, attribute, pos);
        }

        private void displayAttribute(String str, Tag tag, MutableAttributeSet attribute) {
            System.out.println("----------------");
            for (Enumeration<?> e = attribute.getAttributeNames(); e.hasMoreElements(); ) {
                Object aNames = e.nextElement();
                System.out.println(str + ", " + tag + " : " + aNames + "(" + aNames.getClass().getName() + ") = " + attribute.getAttribute(aNames) + "(" + attribute.getAttribute(aNames).getClass().getName() + ")");
            }
        }
    }
}
