package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import tree.EvalException;
import tree.ParseException;
import tree.ValueNotStoredException;
import calc.VarStorage;
import calc.Calc;
import fw.OCValidationException;
import java.io.*;
import java.net.*;

/**
 * A NewCalc object represents the entire user interface. It uses the standard
 * GridBag Layout manager. 
 * @author jason
 *
 */
public class NewCalc extends JApplet {

    private static final long serialVersionUID = 1L;

    private Calc CALC1;

    private JScrollPane varScrollPane, constScrollPane;

    private OCTextField textWithFocus;

    private JPanel graph;

    private CalcPanel text;

    private ElmStoragePanel varPanel, constPanel;

    private JTabbedPane graphCalcDraw, mathFunc;

    private FunctionsPane graphFunctions;

    private GridPropsPanel gridProps;

    private Graph g;

    private KeyListener keys;

    private static int textWithFocusCaretPos;

    private static JFrame frame;

    private JMenuBar menuBar;

    private JMenu help;

    private NewCalc thisCalc;

    private JFrame tutorialFrame, licenseFrame;

    private JTextArea terminal;

    private int xSize;

    private int ySize;

    private TreeCalcPanel treePan;

    /**
	 * @throws ValueNotStoredException 
	 * @throws ParseException 
	 * @throws EvalException 
	 * 
	 */
    public NewCalc() throws ParseException, ValueNotStoredException, EvalException {
        thisCalc = this;
        textWithFocus = new OCTextField();
        super.setLayout(new GridBagLayout());
        menuBar = new JMenuBar();
        help = new JMenu("Help");
        menuBar.add(help);
        JMenuItem tutorial = new JMenuItem("Tutorial");
        tutorial.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (tutorialFrame == null) {
                    tutorialFrame = new JFrame("Tutorial");
                    tutorialFrame.setPreferredSize(new Dimension(450, 400));
                    terminal = new JTextArea(14, 20);
                    Font terminalFont = new Font("newFont", 1, 12);
                    terminal.setFont(terminalFont);
                    terminal.setEditable(false);
                    final JScrollPane termScrollPane = new JScrollPane(terminal);
                    termScrollPane.setWheelScrollingEnabled(true);
                    terminal.append(readTextDoc("README.txt"));
                    tutorialFrame.add(termScrollPane);
                    tutorialFrame.pack();
                    tutorialFrame.setVisible(true);
                    JScrollBar tempScroll = termScrollPane.getVerticalScrollBar();
                    tempScroll.setValue(0);
                } else {
                    tutorialFrame.setVisible(true);
                }
            }
        });
        JMenuItem license = new JMenuItem("License");
        license.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (licenseFrame == null) {
                    licenseFrame = new JFrame("License");
                    licenseFrame.setPreferredSize(new Dimension(450, 400));
                    JTextArea terminal = new JTextArea(14, 20);
                    Font terminalFont = new Font("newFont", 1, 12);
                    terminal.setFont(terminalFont);
                    terminal.setEditable(false);
                    JScrollPane termScrollPane = new JScrollPane(terminal);
                    termScrollPane.setWheelScrollingEnabled(true);
                    licenseFrame.add(termScrollPane);
                    licenseFrame.pack();
                    licenseFrame.setVisible(true);
                    terminal.append(readTextDoc("COPYING.txt"));
                    termScrollPane.revalidate();
                    JScrollBar tempScroll = termScrollPane.getVerticalScrollBar();
                    tempScroll.setValue(0);
                } else {
                    licenseFrame.setVisible(true);
                }
            }
        });
        help.add(tutorial);
        help.add(license);
        this.setJMenuBar(menuBar);
        graph = new JPanel();
        CALC1 = new Calc(this);
        graphCalcDraw = new JTabbedPane(JTabbedPane.TOP);
        NumsAndOppsPanel Nums = new NumsAndOppsPanel(this);
        mathFunc = new JTabbedPane();
        varPanel = new ElmStoragePanel(this, CALC1.getVarList());
        varScrollPane = new JScrollPane(varPanel);
        constPanel = new ElmStoragePanel(this, CALC1.getConstantList());
        constScrollPane = new JScrollPane(constPanel);
        mathFunc.add(Nums, "Math");
        mathFunc.add(new TrigPanel(this), "Trig");
        mathFunc.add(varScrollPane, "Vars");
        mathFunc.add(constScrollPane, "Const");
        text = new CalcPanel(this);
        graphCalcDraw.add("Calculator", text);
        g = new Graph(360, 360, this);
        graphCalcDraw.add("Graph", g);
        Graph3D g3d = new Graph3D(360, 360, this);
        graphCalcDraw.add("3DGraph", g3d);
        treePan = new TreeCalcPanel(this);
        graphCalcDraw.add("treeSolver", treePan);
        graphFunctions = new FunctionsPane(this);
        graphCalcDraw.add("Func", graphFunctions);
        gridProps = new GridPropsPanel(this);
        graphCalcDraw.add("Grid", gridProps);
        graphCalcDraw.add(new DrawPad(500, 500, this), "Draw");
        graphCalcDraw.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent arg0) {
                int selected = graphCalcDraw.getSelectedIndex();
                String nameSelected = graphCalcDraw.getTitleAt(selected);
                if (nameSelected.equals("Calculator")) {
                    try {
                        setCurrTextField(text.getTextTerminal());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (ValueNotStoredException e) {
                        e.printStackTrace();
                    } catch (EvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        GridBagConstraints pCon = new GridBagConstraints();
        pCon.fill = GridBagConstraints.BOTH;
        pCon.insets = new Insets(0, 0, 6, 0);
        pCon.weightx = 1;
        pCon.weighty = 1;
        pCon.gridheight = 5;
        pCon.gridwidth = 3;
        pCon.weightx = 1;
        pCon.weighty = 1;
        pCon.gridx = 0;
        pCon.gridy = 0;
        this.add(graphCalcDraw, pCon);
        pCon.fill = GridBagConstraints.BOTH;
        pCon.insets = new Insets(6, 0, 0, 0);
        pCon.gridheight = 5;
        pCon.gridwidth = 3;
        pCon.weightx = 1;
        pCon.weighty = .1;
        pCon.gridx = 0;
        pCon.gridy = 5;
        this.add(mathFunc, pCon);
        graphCalcDraw.setSelectedIndex(0);
        this.repaint();
    }

    public int getFocusedComp() {
        int currPos = graphCalcDraw.getSelectedIndex();
        return currPos;
    }

    public String readTextDoc(String docName) {
        String line;
        URL url = null;
        try {
            url = new URL(getCodeBase(), docName);
        } catch (MalformedURLException e) {
        }
        try {
            InputStream in = url.openStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(in));
            StringBuffer strBuff = new StringBuffer();
            while ((line = bf.readLine()) != null) {
                strBuff.append(line + "\n");
            }
            return strBuff.toString();
        } catch (IOException e) {
            System.out.println("error");
            e.printStackTrace();
        }
        return null;
    }

    public void updateGraph() {
        g.repaint();
    }

    public void updateGraph(String func) {
        g.repaint();
    }

    public Calc getBasicCalc() {
        return CALC1;
    }

    public OCTextField getCurrTextField() {
        return textWithFocus;
    }

    public ElmStoragePanel getVarsPanel() {
        return varPanel;
    }

    public void setCurrTextField(OCTextField focused) throws ParseException, ValueNotStoredException, EvalException {
        if (!textWithFocus.equals(focused)) {
            textWithFocus.associatedAction();
            textWithFocus = focused;
            textWithFocus.requestFocus();
        }
    }

    public int getCurrCaretPos() {
        return textWithFocusCaretPos;
    }

    public void setCurrCaretPos(int pos) {
        textWithFocusCaretPos = pos;
    }

    public void addToCaretPos(int i) {
        textWithFocusCaretPos += i;
        updateCaretPos();
    }

    public void updateCaretPos() {
        textWithFocus.setCaretPosition(textWithFocusCaretPos);
    }

    public Calc getBasicCalcObj() {
        return CALC1;
    }

    public VarStorage getVarsObj() {
        return CALC1.getVarList();
    }

    public Graph getGraphObj() {
        return g;
    }

    public GridPropsPanel getGridProps() {
        return gridProps;
    }

    public String evalCalc(String eqtn) {
        String tempString = new String();
        double ans;
        CALC1.parse(eqtn);
        ans = CALC1.solve();
        if (ans == Double.MAX_VALUE) return "error"; else tempString += ans;
        return tempString;
    }

    public String evalCalc_mod(String eqtn) throws Exception {
        String tempString = new String();
        double ans;
        try {
            CALC1.parse_mod(eqtn);
            ans = CALC1.solve();
            tempString += ans;
        } catch (OCValidationException oc) {
            throw oc;
        }
        return tempString;
    }

    private static void createAndShowGUI() throws ParseException, ValueNotStoredException, EvalException {
        frame = new JFrame("OpenCalc");
        Dimension frameDim = new Dimension(450, 600);
        frame.setPreferredSize(frameDim);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        NewCalc currCalc = new NewCalc();
        frame.add(currCalc);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    createAndShowGUI();
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (ValueNotStoredException e) {
                    e.printStackTrace();
                } catch (EvalException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
