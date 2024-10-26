package com.googlecode.harapeko.demos;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.PinstripePainter;
import com.googlecode.harapeko.ContainerContext;
import com.googlecode.harapeko.annotation.BorderLayout;
import com.googlecode.harapeko.annotation.DefaultLayout;
import com.googlecode.harapeko.annotation.JPanelResource;
import com.googlecode.harapeko.annotation.JScrollPaneResource;
import com.googlecode.harapeko.annotation.JSplitPaneResource;
import com.googlecode.harapeko.annotation.JTabbedPaneResource;
import com.googlecode.harapeko.annotation.JXPanelResource;
import com.googlecode.harapeko.annotation.JXTaskPaneContainerResource;
import com.googlecode.harapeko.annotation.JXTaskPaneResource;
import com.googlecode.harapeko.annotation.RSyntaxTextAreaResource;
import com.googlecode.harapeko.annotation.Scrollable;
import com.googlecode.harapeko.annotation.TabLayout;
import com.googlecode.harapeko.annotation.TabLayout.Tab;
import com.googlecode.harapeko.annotation.processor.Processor;
import com.googlecode.harapeko.demos.swing.Action1;
import com.googlecode.harapeko.demos.swing.ActionJS;
import com.googlecode.harapeko.demos.swing.AddressBook;
import com.googlecode.harapeko.demos.swing.Binding;
import com.googlecode.harapeko.demos.swing.Borders;
import com.googlecode.harapeko.demos.swing.CSSDemo1;
import com.googlecode.harapeko.demos.swing.CSSDemo2;
import com.googlecode.harapeko.demos.swing.DefaultLayoutDemo;
import com.googlecode.harapeko.demos.swing.Fonts;
import com.googlecode.harapeko.demos.swing.LayoutBorderDemo;
import com.googlecode.harapeko.demos.swing.LayoutFlowDemo;
import com.googlecode.harapeko.demos.swing.LayoutGridBagTable;
import com.googlecode.harapeko.demos.swing.WindowDragPanel;
import com.googlecode.harapeko.demos.swingx.Busy;
import com.googlecode.harapeko.demos.swingx.Date;
import com.googlecode.harapeko.demos.swingx.Find;
import com.googlecode.harapeko.demos.swingx.Graph;
import com.googlecode.harapeko.demos.swingx.MonthView;

@BorderLayout(center = "pane")
@JXPanelResource(minimumSize = "600,600")
public class MainPanel extends JXPanel {

    Painter painter = new CompoundPainter(new MattePainter(new Color(51, 51, 51)), new PinstripePainter(new Color(1f, 1f, 1f, 0.17f), 45.0));

    @JSplitPaneResource(leftComponent = "taskPane", rightComponent = "tab")
    JSplitPane pane;

    @DefaultLayout(layout = { "swing", "swingX" })
    @JXTaskPaneContainerResource(backgroundPainter = "painter")
    JXTaskPaneContainer taskPane;

    @TabLayout(tabs = { @Tab(name = "demo", component = "demo"), @Tab(name = "source", component = "source") })
    @JTabbedPaneResource(tabPlacement = JTabbedPane.BOTTOM)
    JTabbedPane tab;

    @JScrollPaneResource(viewportView = "HarapekoSwing")
    JScrollPane demo;

    @Scrollable
    @RSyntaxTextAreaResource(font = "Monospaced-12", syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA)
    RSyntaxTextArea source;

    @DefaultLayout(processor = HyperLinkProcessor.class, layout = { "action", "action_js", "binding", "layout_default", "layout_flow", "layout_border", "layout_gridbagtable", "css_demo1", "css_demo2", "borders", "fonts", "addressBook" })
    @JXTaskPaneResource(title = "Swing")
    JXTaskPane swing;

    @DefaultLayout(processor = HyperLinkProcessor.class, layout = { "monthView", "find", "date", "graph" })
    @JXTaskPaneResource(title = "SwingX")
    JXTaskPane swingX;

    HarapekoSwing HarapekoSwing;

    Action1 action;

    ActionJS action_js;

    Binding binding;

    DefaultLayoutDemo layout_default;

    LayoutFlowDemo layout_flow;

    LayoutBorderDemo layout_border;

    LayoutGridBagTable layout_gridbagtable;

    CSSDemo1 css_demo1;

    CSSDemo2 css_demo2;

    Borders borders;

    Fonts fonts;

    AddressBook addressBook;

    WindowDragPanel windowDrag;

    MonthView monthView;

    Find find;

    Graph graph;

    Date date;

    Busy busy;

    public static class HyperLinkProcessor implements Processor {

        @Override
        public Container process(final ContainerContext context, Container container, AnnotatedElement element, Object annotation) throws Exception {
            DefaultLayout dl = (DefaultLayout) annotation;
            final MainPanel pane = (MainPanel) context.getRoot();
            for (final String name : dl.layout()) {
                JXHyperlink link = new JXHyperlink(new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        Container c = context.lookup(name);
                        pane.demo.setViewportView(c);
                        try {
                            pane.updateSource(c.getClass());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                String text = name.substring(0, 1).toUpperCase() + name.substring(1);
                text = text.replace('_', ' ');
                link.setText(text);
                container.add(link);
            }
            return container;
        }
    }

    void updateSource(Class<?> type) throws Exception {
        final String path = type.getName().replaceAll("\\.", "/") + ".java";
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    URL url = Thread.currentThread().getContextClassLoader().getResource(path);
                    InputStream fis = url.openStream();
                    InputStreamReader r = new InputStreamReader(fis, "UTF-8");
                    BufferedReader br = new BufferedReader(r);
                    String line;
                    StringBuilder sb = new StringBuilder();
                    while (null != (line = br.readLine())) {
                        sb.append(line);
                        sb.append("\r\n");
                    }
                    br.close();
                    r.close();
                    fis.close();
                    final String text = sb.toString();
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            MainPanel.this.source.setText(text);
                            MainPanel.this.source.setCaretPosition(0);
                        }
                    });
                } catch (Exception ex) {
                }
            }
        }).start();
    }
}
