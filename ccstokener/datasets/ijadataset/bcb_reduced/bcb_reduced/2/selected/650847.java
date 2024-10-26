package br.ufrj.nce.linkit;

import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.EtchedBorder;
import netscape.javascript.JSObject;

public class JLinkitFrame extends JApplet implements ActionListener {

    public static final String metodos[] = { "M�todo de Euler", "M�todo Runge Kutta" };

    protected static String user = "";

    public static final Font arial9Negrito = new Font("Arial", Font.BOLD, 12);

    public static final Font arial10Normal = new Font("Arial", Font.PLAIN, 13);

    public static final Font arial7Normal = new Font("Arial", Font.PLAIN, 10);

    public static final Font arial8Normal = new Font("Arial", Font.PLAIN, 11);

    public static final Font arial7Negrito = new Font("Arial", Font.BOLD, 10);

    public static final Font arial5Negrito = new Font("Arial", Font.BOLD, 8);

    public static final Font arial5Normal = new Font("Arial", Font.PLAIN, 8);

    public static final Font arial6Normal = new Font("Arial", Font.PLAIN, 9);

    public PainelModelos pModelo;

    private JScrollPane pModeloScrollPane;

    private JTabbedPane tabbedPane;

    public static JMenuItem[] m_Metodos;

    public static JFileChooser m_chooser;

    public static FileFilter filter;

    public static JToolBar toolBar;

    public static JMenuBar menuBar;

    public Velocidade veloc;

    public Sobre sobre;

    public Ajuda janelaContato;

    public static JLabel label;

    public Grafico pGrafico;

    public static JSplitPane splitter;

    public static JPanel painel;

    public static JCheckBoxMenuItem chkGrade;

    static int posAntDivider;

    static JToggleButton bNovo;

    static JToggleButton bAbrir;

    static JButton bRich;

    static JToggleButton bSalvar;

    static JToggleButton bImprimir;

    static JToggleButton bSelecao;

    static JToggleButton bVarContinua;

    static JToggleButton bVarLigaDesliga;

    static JToggleButton bRelTaxa;

    static JToggleButton bRelProporcao;

    static JToggleButton bPlay;

    static JToggleButton bPause;

    static JToggleButton bReinicializar;

    static JToggleButton bZerar;

    static JToggleButton bRelogio;

    static JLabel label2;

    static JLabel label3;

    static JLabel lTempo;

    static JLabel lBrancos;

    static JLabel lLogin;

    static JLabel lNomeArq;

    static JComboBox cBoxUnidades;

    static JComboBox cBoxCenarios;

    static JTextField nomeDiretorio;

    static ImageIcon iconNew;

    static ImageIcon iconOpen;

    static ImageIcon iconSave;

    static ImageIcon iconPrint;

    static ImageIcon iconSelecao;

    static ImageIcon iconVarCont;

    static ImageIcon iconVarLD;

    static ImageIcon iconRelTX;

    static ImageIcon iconRelP;

    static ImageIcon iconPlay;

    static ImageIcon iconParar;

    static ImageIcon iconReinic;

    static ImageIcon iconZerar;

    static ImageIcon iconRelogio;

    static ImageIcon iconRich;

    static Image imgFundo;

    static InputStream strImagS;

    JEditorPane jep = new JEditorPane();

    JFileChooser urlFile = new JFileChooser();

    JTextField urlFileText = new JTextField();

    public static boolean botaoVar;

    public static boolean botaoRel;

    public static boolean mantemDivider;

    static int clickVarel;

    public static boolean botaoReinic;

    public static boolean bLGradeGrafico;

    public static boolean bLGradeGAnt;

    public static boolean selArq;

    static String metodoCalc;

    static String nomeArq;

    static String[] unidadesTempo = { "segundos", "minutos", "horas", "dias", "semanas", "meses", "anos" };

    static String unidades;

    static String[] cenariosMod;

    static Vector linhaMod;

    static int ilMod;

    static Vector vArqDir;

    File fChoosen;

    static Dimension resolAp;

    static int resolLarg;

    static int resolAlt;

    static Dimension telaAp;

    static int largAp;

    static int altAp;

    public static String nomeDoArquivo;

    public static JApplet euApplet;

    public static boolean jaSeFoi;

    static AppletContext contexto;

    public static final String versao = "JLinkit 2.0";

    public static ArrayList comentAbertos;

    public JLinkitFrame() {
    }

    public void init() {
        comentAbertos = new ArrayList();
        euApplet = this;
        String nomeA = "SemNome";
        nomeDoArquivo = nomeA;
        lNomeArq = new JLabel(nomeArq, JLabel.LEFT);
        iconNew = criarImagemIcon("bNovo.gif");
        iconOpen = criarImagemIcon("bAbrir.gif");
        iconRich = criarImagemIcon("bAbrir.gif");
        iconSave = criarImagemIcon("bSalvar.gif");
        iconPrint = criarImagemIcon("bImprimir.gif");
        iconSelecao = criarImagemIcon("bSelecao.gif");
        iconVarCont = criarImagemIcon("bVarContinua.gif");
        iconVarLD = criarImagemIcon("bVarLigaDesliga.gif");
        iconRelTX = criarImagemIcon("bRelacionamentoTaxa.gif");
        iconRelP = criarImagemIcon("bRelacionamentoProporcao.gif");
        iconPlay = criarImagemIcon("bPlay.gif");
        iconParar = criarImagemIcon("bPause.gif");
        iconReinic = criarImagemIcon("bReinicializar.gif");
        iconZerar = criarImagemIcon("bZerar.gif");
        iconRelogio = criarImagemIcon("bRelogio.gif");
        resolAp = new Dimension(1024, 768);
        resolLarg = (int) resolAp.getWidth();
        resolAlt = (int) resolAp.getHeight();
        resolLarg = 1024;
        resolAlt = 768;
        largAp = 1024;
        altAp = 768;
        int altMod = altAp * 7 / 10;
        if (resolLarg == 1024) altMod = altAp * 8 / 10;
        menuBar = criaBarraMenus();
        setJMenuBar(menuBar);
        toolBar = new JToolBar();
        barraFerramentas(toolBar);
        toolBar.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        linhaMod = new Vector(0);
        vArqDir = new Vector(0);
        pModelo = new PainelModelos();
        pModelo.setBackground(Color.white);
        pModelo.setPreferredSize(new Dimension(resolLarg, altMod));
        pModelo.setMinimumSize(new Dimension(resolLarg, 100));
        pModeloScrollPane = new JScrollPane(pModelo);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Modelo", pModeloScrollPane);
        jep = new JEditorPane();
        jep.setText("Conteudo HTML");
        jep.setEditable(false);
        JPanel painelInterface = new JPanel();
        painelInterface.setLayout(new BorderLayout());
        JPanel painelNavegacao = new JPanel();
        painelNavegacao.setLayout(new BorderLayout());
        urlFile.setPreferredSize(new Dimension(600, 600));
        urlFile.addChoosableFileFilter(new HtmlFilter());
        urlFile.setAcceptAllFileFilterUsed(false);
        urlFileText.setEditable(false);
        painelNavegacao.add(bRich, BorderLayout.WEST);
        painelNavegacao.add(urlFileText, BorderLayout.CENTER);
        painelInterface.setAlignmentY(JPanel.LEFT_ALIGNMENT);
        painelInterface.add(painelNavegacao, BorderLayout.PAGE_START);
        JScrollPane interfaceScrollPane = new JScrollPane(jep);
        interfaceScrollPane.setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        painelInterface.add(interfaceScrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Interface", painelInterface);
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        pGrafico = new Grafico(pModelo.alVarCont);
        pGrafico.setBackground(Color.white);
        pGrafico.setPreferredSize(new Dimension(resolLarg, altAp - altMod));
        pGrafico.setMinimumSize(new Dimension(resolLarg, 20));
        pModelo.atribuiGrafico(pGrafico);
        splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, pGrafico);
        splitter.setContinuousLayout(true);
        splitter.setOneTouchExpandable(false);
        splitter.setDividerSize(4);
        painel = new JPanel();
        painel.setLayout(new BorderLayout());
        painel.add(toolBar, BorderLayout.NORTH);
        painel.add(splitter, BorderLayout.CENTER);
        setContentPane(painel);
        updateMonitor();
        setVisible(true);
        limpaFrame();
        if (!"SemNome".equals(nomeA)) {
            abreModeloWeb();
        } else {
        }
    }

    private static void addURL(JPanel painel, String label, JComponent caixaDeTexto, JButton botao) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);
        painel.add(new JLabel(label), constraints);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        painel.add(caixaDeTexto, constraints);
        painel.add(botao);
    }

    public static void main(String s[]) {
        JFrame f = new JFrame("JLinkit");
        f.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        JApplet applet = new JLinkitFrame();
        f.getContentPane().add(applet, BorderLayout.CENTER);
        applet.init();
        f.setSize(new Dimension(1024, 600));
        f.setVisible(true);
    }

    public void limpaFrame() {
        metodoCalc = "Runge Kutta";
        botaoVar = false;
        botaoRel = false;
        botaoReinic = false;
        mantemDivider = false;
        unidades = "segundos";
        imgFundo = null;
        clickVarel = 0;
        bLGradeGrafico = true;
        linhaMod.clear();
        ilMod = 0;
        cBoxUnidades.setSelectedIndex(0);
    }

    public Image criarImagemArqExt(String nomeDoArquivo) {
        Image imagem = null;
        int MAX_IMAGE_SIZE = 153600;
        int count = 0;
        strImagS = null;
        try {
            InputStream strImag = getArquivoS(nomeDoArquivo);
            strImagS = strImag;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "O arquivo especificado n�o existe", "Erro", 0);
            selArq = false;
            nomeDoArquivo = "SemNome";
            linhaMod.clear();
        }
        try {
            BufferedInputStream imgStream = new BufferedInputStream(strImagS, MAX_IMAGE_SIZE);
            if (imgStream != null) {
                byte buf[] = new byte[MAX_IMAGE_SIZE];
                try {
                    count = imgStream.read(buf);
                } catch (IOException ieo) {
                    System.err.println("Couldn't read stream from file: " + nomeDoArquivo);
                }
                try {
                    imgStream.close();
                } catch (IOException ieo) {
                    System.err.println("Can't close file " + nomeDoArquivo);
                }
                if (count <= 0) {
                    System.err.println("Empty file: " + nomeDoArquivo);
                    return null;
                }
                imagem = (Toolkit.getDefaultToolkit().createImage(buf));
                return imagem;
            } else {
                System.out.println("Arquivo de imagem n�o encontrado. path=" + nomeDoArquivo);
                System.err.println("Arquivo de imagem n�o encontrado: " + nomeDoArquivo);
                return imagem;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return imagem;
        }
    }

    public static Image criarImagem(String path) {
        Image imagem = null;
        int MAX_IMAGE_SIZE = 153600;
        int count = 0;
        BufferedInputStream imgStream = new BufferedInputStream(JLinkitFrame.class.getResourceAsStream(path));
        if (imgStream != null) {
            byte buf[] = new byte[MAX_IMAGE_SIZE];
            try {
                count = imgStream.read(buf);
            } catch (IOException ieo) {
                System.err.println("Couldn't read stream from file: " + path);
            }
            try {
                imgStream.close();
            } catch (IOException ieo) {
                System.err.println("Can't close file " + path);
            }
            if (count <= 0) {
                System.err.println("Empty file: " + path);
                return null;
            }
            imagem = (Toolkit.getDefaultToolkit().createImage(buf));
        } else {
            System.err.println("Arquivo de imagem n�o encontrado: " + path);
        }
        return imagem;
    }

    public static ImageIcon criarImagemIcon(String path) {
        ImageIcon icone = null;
        Image imagem = null;
        imagem = criarImagem(path);
        if (imagem != null) icone = new ImageIcon(imagem);
        return icone;
    }

    protected JMenuBar criaBarraMenus() {
        final JMenuBar menuBar = new JMenuBar();
        JMenu mArq = new JMenu("Arquivo");
        mArq.setMnemonic('a');
        Action actionNew = new AbstractAction("Novo", iconNew) {

            public void actionPerformed(ActionEvent e) {
                testaAntesAbrirNovo();
            }
        };
        JMenuItem item = mArq.add(actionNew);
        mArq.add(item);
        Action actionOpen = new AbstractAction("Abrir", iconOpen) {

            public void actionPerformed(ActionEvent e) {
                testaAntesAbrirModelo();
            }
        };
        item = mArq.add(actionOpen);
        mArq.add(item);
        Action actionSave = new AbstractAction("Salvar", iconSave) {

            public void actionPerformed(ActionEvent e) {
                JLinkitFrame.this.repaint();
                testaAntesSalvarModelo();
            }
        };
        item = mArq.add(actionSave);
        mArq.add(item);
        Action actionScomo = new AbstractAction("Salvar Como") {

            public void actionPerformed(ActionEvent e) {
                EscolhedorDeArquivos eda1 = new EscolhedorDeArquivos(euApplet, 2);
                eda1.setVisible(true);
            }
        };
        item = mArq.add(actionScomo);
        mArq.add(item);
        mArq.addSeparator();
        Action actionPrint = new AbstractAction("Imprimir", iconPrint) {

            public void actionPerformed(ActionEvent e) {
            }
        };
        item = mArq.add(actionPrint);
        mArq.add(item);
        mArq.addSeparator();
        Action actionExit = new AbstractAction("Sair") {

            public void actionPerformed(ActionEvent e) {
                if (pModelo.bModificado) {
                    int result = dialogoSalvaModelo();
                    switch(result) {
                        case 0:
                            if (selArq) {
                                eventoGravar();
                                fechaJanelaJS();
                            } else {
                                EscolhedorDeArquivos ed1 = new EscolhedorDeArquivos(euApplet, 5);
                                ed1.setVisible(true);
                            }
                            break;
                        case 1:
                            fechaJanelaJS();
                            break;
                    }
                } else {
                    fechaJanelaJS();
                }
            }
        };
        item = mArq.add(actionExit);
        item.setMnemonic('x');
        menuBar.add(mArq);
        ActionListener fontListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (pModelo.estadoSimulacao.equals("ePlay")) {
                    bPauseClick();
                }
                for (int k = 0; k < metodos.length; k++) if (e.getSource() == m_Metodos[k]) {
                    if (k == 0) metodoCalc = "Euler"; else metodoCalc = "Runge Kutta";
                }
            }
        };
        JMenu mConfig = new JMenu("Configura��es");
        mConfig.setMnemonic('c');
        Action actionVeloc = new AbstractAction("Velocidade") {

            public void actionPerformed(ActionEvent e) {
                if (pModelo.estadoSimulacao.equals("ePlay")) {
                    bPauseClick();
                }
                veloc = new Velocidade(pModelo);
                veloc.setSize(new Dimension(177, 117));
                veloc.pack();
                veloc.setVisible(true);
                veloc.requestFocus();
                pModelo.bFocoModelo = false;
            }
        };
        item = mConfig.add(actionVeloc);
        item.setMnemonic('v');
        mConfig.addSeparator();
        chkGrade = new JCheckBoxMenuItem("Linhas de Grade do Gr�fico");
        chkGrade.setSelected(true);
        chkGrade.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (chkGrade.isSelected()) {
                    bLGradeGrafico = true;
                } else {
                    bLGradeGrafico = false;
                }
                pGrafico.inicializarGrafico(pModelo.alVarCont);
                if (pModelo.estadoSimulacao.equals("ePlay")) {
                    pGrafico.desenharGrafico(pModelo.alVarCont);
                }
            }
        });
        mConfig.add(chkGrade);
        mConfig.addSeparator();
        ButtonGroup group = new ButtonGroup();
        m_Metodos = new JMenuItem[metodos.length];
        for (int k = 0; k < metodos.length; k++) {
            int m = k + 1;
            m_Metodos[k] = new JRadioButtonMenuItem(m + " " + metodos[k]);
            boolean selected = (k == 1);
            m_Metodos[k].setSelected(selected);
            m_Metodos[k].setMnemonic('1' + k);
            m_Metodos[k].addActionListener(fontListener);
            group.add(m_Metodos[k]);
            mConfig.add(m_Metodos[k]);
        }
        menuBar.add(mConfig);
        JMenu mAjuda = new JMenu("Ajuda");
        mAjuda.setMnemonic('j');
        Action actionSobre = new AbstractAction("Sobre") {

            public void actionPerformed(ActionEvent e) {
                sobre = new Sobre("Linkit");
                sobre.setSize(new Dimension(243, 230));
                sobre.pack();
                sobre.setVisible(true);
            }
        };
        item = mAjuda.add(actionSobre);
        Action actionHelp = new AbstractAction("Tutorial JLinkit") {

            public void actionPerformed(ActionEvent e) {
                try {
                    getAppletContext().showDocument(new URL("http://www.nce.ufrj.br/ginape/jlinkit/index.htm"), "_blank");
                } catch (MalformedURLException exception) {
                    exception.printStackTrace();
                }
            }
        };
        item = mAjuda.add(actionHelp);
        Action actionContato = new AbstractAction("Contacte o administrador") {

            public void actionPerformed(ActionEvent e) {
                janelaContato = new Ajuda("Contato");
                janelaContato.setSize(new Dimension(243, 230));
                janelaContato.pack();
                janelaContato.setVisible(true);
            }
        };
        item = mAjuda.add(actionContato);
        menuBar.add(mAjuda);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)));
        return menuBar;
    }

    protected void barraFerramentas(JToolBar toolBar) {
        JButton button = null;
        bNovo = new JToggleButton(iconNew);
        bNovo.setToolTipText("Novo");
        bNovo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zeraBotoes();
                testaAntesAbrirNovo();
            }
        });
        toolBar.add(bNovo);
        bAbrir = new JToggleButton(iconOpen);
        bAbrir.setToolTipText("Abrir");
        bAbrir.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zeraBotoes();
                testaAntesAbrirModelo();
            }
        });
        toolBar.add(bAbrir);
        bSalvar = new JToggleButton(iconSave);
        bSalvar.setToolTipText("Salvar");
        bSalvar.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JLinkitFrame.this.repaint();
                zeraBotoes();
                testaAntesSalvarModelo();
            }
        });
        toolBar.add(bSalvar);
        bImprimir = new JToggleButton(iconPrint);
        bImprimir.setToolTipText("Imprimir");
        bImprimir.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zeraBotoes();
                botaoVar = false;
                botaoRel = false;
                selecionaUM(5);
            }
        });
        toolBar.add(bImprimir);
        bSelecao = new JToggleButton(iconSelecao);
        bSelecao.setSelected(true);
        bSelecao.setToolTipText("Sele��o");
        bSelecao.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoVar = false;
                botaoRel = false;
                selecionaUM(5);
            }
        });
        toolBar.add(bSelecao);
        bVarContinua = new JToggleButton(iconVarCont);
        bVarContinua.setToolTipText("Vari�vel Cont�nua");
        bVarContinua.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoRel = false;
                pModelo.desSelecionar();
                botaoVar = true;
                selecionaUM(6);
                pModelo.repaint();
            }
        });
        toolBar.add(bVarContinua);
        bVarLigaDesliga = new JToggleButton(iconVarLD);
        bVarLigaDesliga.setToolTipText("Vari�vel Liga-Desliga");
        bVarLigaDesliga.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoRel = false;
                pModelo.desSelecionar();
                botaoVar = true;
                selecionaUM(7);
                pModelo.repaint();
            }
        });
        toolBar.add(bVarLigaDesliga);
        bRelTaxa = new JToggleButton(iconRelTX);
        bRelTaxa.setToolTipText("Relacionamento de taxa");
        bRelTaxa.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoVar = false;
                pModelo.desSelecionar();
                botaoRel = true;
                clickVarel = 0;
                selecionaUM(8);
                pModelo.repaint();
            }
        });
        toolBar.add(bRelTaxa);
        bRelProporcao = new JToggleButton(iconRelP);
        bRelProporcao.setToolTipText("Relacionamento de propor��o");
        bRelProporcao.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoVar = false;
                pModelo.desSelecionar();
                botaoRel = true;
                clickVarel = 0;
                selecionaUM(9);
                pModelo.repaint();
            }
        });
        toolBar.add(bRelProporcao);
        bRich = new JButton("Selecione um arquivo de fundo...");
        bRich.setToolTipText("Inserir arquivo HTML com texto rico para o modelo");
        bRich.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int returnVal = urlFile.showOpenDialog(JLinkitFrame.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = urlFile.getSelectedFile();
                    try {
                        System.out.println("Adicionando conteudo HTML");
                        jep.setPage(file.toURI().toURL());
                        urlFileText.setText(file.getAbsolutePath());
                    } catch (MalformedURLException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    System.out.println("Opera��o cancelada!");
                }
            }
        });
        toolBar.add(bRich);
        bPlay = new JToggleButton(iconPlay);
        bPlay.setToolTipText("Animar");
        bPlay.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoVar = false;
                botaoRel = false;
                pModelo.desSelecionar();
                selecionaUM(10);
                bPlayClick();
            }
        });
        toolBar.add(bPlay);
        bPause = new JToggleButton(iconParar);
        bPause.setToolTipText("Parar");
        bPause.setEnabled(false);
        bPause.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                bPauseClick();
            }
        });
        toolBar.add(bPause);
        bReinicializar = new JToggleButton(iconReinic);
        bReinicializar.setToolTipText("Valores antes de animar");
        bReinicializar.setEnabled(false);
        bReinicializar.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoVar = false;
                botaoRel = false;
                bReinicializarClick();
            }
        });
        toolBar.add(bReinicializar);
        bZerar = new JToggleButton(iconZerar);
        bZerar.setToolTipText("Zerar valores");
        bZerar.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoVar = false;
                botaoRel = false;
                bZerarClick();
            }
        });
        toolBar.add(bZerar);
        bRelogio = new JToggleButton(iconRelogio);
        bRelogio.setToolTipText("Zerar rel�gio");
        bRelogio.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                botaoVar = false;
                botaoRel = false;
                bRelogioClick();
            }
        });
        toolBar.add(bRelogio);
        label2 = new JLabel("  TEMPO:", JLabel.CENTER);
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);
        label2.setPreferredSize(new Dimension(53, 23));
        if (resolLarg == 1024) label2.setFont(arial9Negrito); else label2.setFont(arial5Negrito);
        toolBar.add(label2);
        lTempo = new JLabel(" 0        ", JLabel.LEFT);
        lTempo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lTempo.setPreferredSize(new Dimension(56, 23));
        if (resolLarg == 1024) lTempo.setFont(arial10Normal); else lTempo.setFont(arial6Normal);
        toolBar.add(lTempo);
        lBrancos = new JLabel("   ", JLabel.LEFT);
        lBrancos.setAlignmentX(Component.LEFT_ALIGNMENT);
        unidades = "segundos";
        cBoxUnidades = new JComboBox(unidadesTempo);
        cBoxUnidades.setSelectedIndex(0);
        cBoxUnidades.setPreferredSize(new Dimension(72, 20));
        cBoxUnidades.setMaximumSize(new Dimension(72, 20));
        if (resolLarg == 1024) cBoxUnidades.setFont(arial7Normal); else cBoxUnidades.setFont(arial5Normal);
        cBoxUnidades.setAlignmentX(Component.CENTER_ALIGNMENT);
        cBoxUnidades.setBackground(Color.white);
        ActionListener comboAL = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                unidades = (String) cBoxUnidades.getSelectedItem();
            }
        };
        cBoxUnidades.addActionListener(comboAL);
        toolBar.add(cBoxUnidades);
        label3 = new JLabel("    CEN�RIO DO MODELO:", JLabel.CENTER);
        label3.setAlignmentX(Component.CENTER_ALIGNMENT);
        label3.setPreferredSize(new Dimension(53, 23));
        if (resolLarg == 1024) label3.setFont(arial9Negrito); else {
            label3.setText(" CEN�RIO:");
            label3.setFont(arial5Negrito);
            label3.setPreferredSize(new Dimension(48, 23));
        }
        toolBar.add(label3);
        imgFundo = null;
        String[] todosSemNenhum = listaCenarios().split("\\n");
        cenariosMod = new String[todosSemNenhum.length + 2];
        cenariosMod[0] = "nenhum";
        cenariosMod[todosSemNenhum.length] = "Adicionar...";
        if (todosSemNenhum.length > 1) {
            for (int k = 0; k < todosSemNenhum.length; k++) {
                cenariosMod[k + 1] = todosSemNenhum[k].substring(0, (todosSemNenhum[k].length()) - 4);
            }
        }
        cBoxCenarios = new JComboBox(cenariosMod);
        cBoxCenarios.setSelectedIndex(0);
        cBoxCenarios.setPreferredSize(new Dimension(72, 20));
        cBoxCenarios.setMaximumSize(new Dimension(72, 20));
        if (resolLarg == 1024) cBoxCenarios.setFont(arial7Normal); else {
            cBoxCenarios.setFont(arial5Normal);
            cBoxCenarios.setPreferredSize(new Dimension(50, 20));
            cBoxCenarios.setMaximumSize(new Dimension(50, 20));
        }
        cBoxCenarios.setAlignmentX(Component.CENTER_ALIGNMENT);
        cBoxCenarios.setBackground(Color.white);
        ActionListener comboC = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String sCenario;
                sCenario = (String) cBoxCenarios.getSelectedItem();
                if (cBoxCenarios.getSelectedIndex() == 0) {
                    imgFundo = null;
                } else if (sCenario.equalsIgnoreCase("Adicionar...")) {
                    JFileChooser imagemFundo = new JFileChooser();
                    if (imagemFundo.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
                    fChoosen = imagemFundo.getSelectedFile();
                    System.out.println("Lendo arquivo de imagem: " + fChoosen.getAbsolutePath());
                    imgFundo = createImage(fChoosen);
                    pModelo.repaint();
                } else {
                    imgFundo = criarImagem(cenariosMod[cBoxCenarios.getSelectedIndex()] + ".gif");
                    pModelo.repaint();
                }
            }
        };
        cBoxCenarios.addActionListener(comboC);
        toolBar.add(cBoxCenarios);
        lLogin = new JLabel("   LOGIN: ", JLabel.LEFT);
        lLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        lLogin.setPreferredSize(new Dimension(56, 23));
        if (resolLarg == 1024) lLogin.setFont(arial9Negrito); else lLogin.setFont(arial5Negrito);
        toolBar.add(lLogin);
        nomeDiretorio = new JTextField(20);
        toolBar.add(nomeDiretorio);
        nomeDiretorio.setPreferredSize(new Dimension(60, 20));
        nomeDiretorio.setMaximumSize(new Dimension(60, 20));
        if (resolLarg == 1024) nomeDiretorio.setFont(arial7Normal); else nomeDiretorio.setFont(arial5Normal);
        nomeDiretorio.setAlignmentX(Component.CENTER_ALIGNMENT);
        ActionListener diretorioListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zeraBotoes();
                testaAntesAbrirNovo();
                user = nomeDiretorio.getText();
            }
        };
        nomeDiretorio.addActionListener(diretorioListener);
        toolBar.add(nomeDiretorio);
    }

    public Image createImage(File arquivo) {
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(arquivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bi;
    }

    private String listaArquivo() {
        String arquivo = "";
        String linha = "";
        try {
            URL url = new URL(this.getCodeBase(), "./listador?dir=" + "cenarios" + "/" + user);
            URLConnection con = url.openConnection();
            con.setUseCaches(false);
            InputStream in = con.getInputStream();
            DataInputStream result = new DataInputStream(new BufferedInputStream(in));
            while ((linha = result.readLine()) != null) {
                arquivo += linha + "\n";
            }
            return arquivo;
        } catch (Exception e) {
            return null;
        }
    }

    private String listaCenarios() {
        String arquivo = "";
        String linha = "";
        try {
            URL url = new URL(this.getCodeBase(), "./listador?dir=" + "arquivos/cenarios/");
            URLConnection con = url.openConnection();
            con.setUseCaches(false);
            InputStream in = con.getInputStream();
            DataInputStream result = new DataInputStream(new BufferedInputStream(in));
            while ((linha = result.readLine()) != null) {
                arquivo += linha + "\n";
            }
            return arquivo;
        } catch (Exception e) {
            try {
                URL url = new URL(this.getCodeBase(), "./cenarios/");
                URLConnection con = url.openConnection();
                con.setUseCaches(false);
                InputStream in = con.getInputStream();
                DataInputStream result = new DataInputStream(new BufferedInputStream(in));
                while ((linha = result.readLine()) != null) {
                    arquivo += linha + "\n";
                }
                return arquivo;
            } catch (Exception ex) {
                return "";
            }
        }
    }

    public void testaAntesAbrirNovo() {
        if (pModelo.bModificado) {
            int result = dialogoSalvaModelo();
            switch(result) {
                case 0:
                    if (selArq) {
                        eventoGravar();
                        eventoNovoModelo();
                    } else {
                        EscolhedorDeArquivos ed1 = new EscolhedorDeArquivos(euApplet, 4);
                        ed1.setVisible(true);
                    }
                    break;
                case 1:
                    eventoNovoModelo();
                    break;
            }
        } else {
            eventoNovoModelo();
        }
    }

    public void testaAntesSalvarModelo() {
        if (selArq) {
            eventoGravar();
        } else {
            EscolhedorDeArquivos ed2 = new EscolhedorDeArquivos(euApplet, 1);
            ed2.setVisible(true);
        }
    }

    public void abreModeloWeb() {
        try {
            String modelo = getArquivo(nomeDoArquivo);
            System.out.println(modelo);
            String linhamodels[] = modelo.split("\\n");
            if (linhamodels[0].equalsIgnoreCase("N�o deu pra ler!")) {
                throw new Exception();
            }
            linhaMod.clear();
            for (int i = 0; i < linhamodels.length; i++) {
                linhaMod.addElement(linhamodels[i]);
            }
            selArq = true;
            bRelogioClick();
        } catch (Exception e) {
            System.out.println("N�o consegui abrir modelo web.");
            JOptionPane.showMessageDialog(null, "O arquivo especificado n�o existe", "Erro", 0);
            selArq = false;
            nomeDoArquivo = "SemNome";
            linhaMod.clear();
        }
        try {
            JSObject jso = JSObject.getWindow(this);
            jso.eval("document.title=\"" + versao + " - " + nomeDoArquivo + "\";");
        } catch (Exception e) {
            System.out.println("N�o estou no IE");
        }
        criaModelo();
        pModelo.bModificado = false;
        pModelo.repaint();
    }

    public void testaAntesAbrirModelo() {
        if (pModelo.bModificado) {
            int result = dialogoSalvaModelo();
            switch(result) {
                case 0:
                    if (selArq) {
                        eventoGravar();
                        EscolhedorDeArquivos ed3 = new EscolhedorDeArquivos(euApplet, 0);
                        ed3.setVisible(true);
                    } else {
                        EscolhedorDeArquivos ed4 = new EscolhedorDeArquivos(euApplet, 3);
                        ed4.setVisible(true);
                    }
                    break;
                case 1:
                    EscolhedorDeArquivos ed1 = new EscolhedorDeArquivos(euApplet, 0);
                    ed1.setVisible(true);
                    break;
            }
        } else {
            EscolhedorDeArquivos ed1 = new EscolhedorDeArquivos(euApplet, 0);
            ed1.setVisible(true);
        }
    }

    public int dialogoSalvaModelo() {
        JOptionPane pane = new JOptionPane("Deseja gravar o modelo antes de fech�-lo?");
        Object[] options = new String[] { "Sim", "N�o", "Cancelar" };
        pane.setOptions(options);
        JDialog dialog = pane.createDialog(JLinkitFrame.this, "Confirme:");
        dialog.show();
        Object obj = pane.getValue();
        int result = -1;
        for (int k = 0; k < options.length; k++) if (options[k].equals(obj)) result = k;
        return result;
    }

    protected void eventoNovoModelo() {
        selecionaUM(5);
        limpaFrame();
        nomeDoArquivo = "SemNome";
        selArq = false;
        try {
            JSObject jso = JSObject.getWindow(this);
            jso.eval("document.title=\"" + versao + " - " + nomeDoArquivo + "\";");
        } catch (Exception e) {
            System.out.println("N�o estou no IE");
        }
        pModelo.novoModelo();
        pModelo.repaint();
        bRelogioClick();
    }

    protected void abreModelo() {
        JLinkitFrame.this.repaint();
        if (selArq) {
            if (m_chooser.showOpenDialog(JLinkitFrame.this) != JFileChooser.APPROVE_OPTION) return;
        }
        try {
            String fChoosenS;
            BufferedReader in = null;
            if (selArq) {
                eventoNovoModelo();
                fChoosenS = m_chooser.getSelectedFile().getAbsolutePath();
                in = new BufferedReader(new FileReader(fChoosenS));
            } else {
                fChoosenS = nomeArq + ".wli";
                BufferedInputStream bufStream = new BufferedInputStream(JLinkitFrame.class.getResourceAsStream(fChoosenS));
                in = new BufferedReader(new InputStreamReader(bufStream));
            }
            try {
                JSObject jso = JSObject.getWindow(this);
                jso.eval("document.title=\"" + fChoosenS + "\";");
            } catch (Exception e) {
                System.out.println("N�o estou no IE");
            }
            String lin = " ";
            int i = 0;
            linhaMod.clear();
            while ((lin = in.readLine()) != null) {
                i++;
                linhaMod.addElement(lin);
            }
            in.close();
            criaModelo();
            pModelo.repaint();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void abreModeloRuim() {
        JLinkitFrame.this.repaint();
        String modelo = getArquivo("teste3.wli");
        String linhamodels[] = modelo.split("\\n");
        linhaMod.clear();
        for (int i = 0; i < linhamodels.length; i++) {
            linhaMod.addElement(linhamodels[i]);
        }
        criaModelo();
        pModelo.repaint();
    }

    protected void eventoGravarBackup() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(fChoosen));
            salvaModelo();
            for (int j = 0; j < linhaMod.size(); j++) {
                bw.write((String) linhaMod.elementAt(j));
                bw.newLine();
            }
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void eventoGravar() {
        String modelo = "";
        salvaModelo();
        for (int j = 0; j < linhaMod.size(); j++) {
            modelo += (String) linhaMod.elementAt(j) + "\n";
        }
        saveArquivo(nomeDoArquivo, modelo);
        selArq = true;
        try {
            JSObject jso = JSObject.getWindow(this);
            jso.eval("document.title=\"" + versao + " - " + nomeDoArquivo + "\";");
        } catch (Exception e) {
            System.out.println("N�o estou no IE");
        }
        System.out.println(modelo);
    }

    protected void updateMonitor() {
        int index = -1;
        for (int k = 0; k < m_Metodos.length; k++) {
            if (m_Metodos[k].isSelected()) {
                index = k;
                break;
            }
        }
    }

    public void salvaModelo() {
        pModelo.bModificado = false;
        linhaMod.clear();
        linhaMod.addElement("JLINKIT - VERSAO 1.0");
        linhaMod.addElement(" ");
        int i = 0;
        while (i < pModelo.alVarCont.size()) {
            pModelo.vart = (Var) pModelo.alVarCont.get(i);
            linhaMod.addElement("VARIAVEL------------------------------------------");
            if (pModelo.vart.tipo.equals("VarLigaDesliga") & pModelo.vart.tipoLigaDesliga.equals("QualquerValor")) linhaMod.addElement("VARIAVEL LIGA-DESLIGA QUALQUER VALOR");
            if (pModelo.vart.tipo.equals("VarLigaDesliga") & pModelo.vart.tipoLigaDesliga.equals("")) linhaMod.addElement("VARIAVEL LIGA-DESLIGA");
            if (pModelo.vart.tipo.equals("VarContinua") & pModelo.vart.tipoVarContinua.equals("QualquerValor")) linhaMod.addElement("VARIAVEL CONTINUA QUALQUER VALOR");
            if (pModelo.vart.tipo.equals("VarContinua") & pModelo.vart.tipoVarContinua.equals("")) linhaMod.addElement("VARIAVEL CONTINUA");
            linhaMod.addElement(pModelo.vart.nome);
            linhaMod.addElement(Integer.toString(pModelo.vart.x));
            linhaMod.addElement(Integer.toString(pModelo.vart.y));
            linhaMod.addElement(Double.toString(pModelo.vart.posBarra));
            linhaMod.addElement(Integer.toString(pModelo.vart.corBarra.getRed()));
            linhaMod.addElement(Integer.toString(pModelo.vart.corBarra.getGreen()));
            linhaMod.addElement(Integer.toString(pModelo.vart.corBarra.getBlue()));
            if (pModelo.vart.bGrafico) linhaMod.addElement("TRACAR GRAFICO"); else linhaMod.addElement("NAO TRACAR GRAFICO");
            linhaMod.addElement(Integer.toString(pModelo.vart.tipoRel));
            if (pModelo.vart.bAcordada) linhaMod.addElement("ACORDADA"); else linhaMod.addElement("DORMINDO");
            if (pModelo.vart.bAutoMudancaDiminui) linhaMod.addElement("AUTOMUDANCA: DIMINUI"); else linhaMod.addElement("AUTOMUDANCA: AUMENTA");
            linhaMod.addElement(Double.toString(pModelo.vart.valorAutoMudanca));
            if (pModelo.vart.tipoCombinacao == 1) linhaMod.addElement("COMBINACAO DE RELACIONAMENTOS: SOMA");
            if (pModelo.vart.tipoCombinacao == 2) linhaMod.addElement("COMBINACAO DE RELACIONAMENTOS: MULTIPLICACAO");
            if (pModelo.vart.tipoCombinacao == 3) linhaMod.addElement("COMBINACAO DE RELACIONAMENTOS: MEDIA ARITIMETICA");
            linhaMod.addElement(Integer.toString(pModelo.vart.corGraf.getRed()));
            linhaMod.addElement(Integer.toString(pModelo.vart.corGraf.getGreen()));
            linhaMod.addElement(Integer.toString(pModelo.vart.corGraf.getBlue()));
            linhaMod.addElement(Integer.toString(pModelo.vart.lComentario));
            if (pModelo.vart.lComentario != 0) linhaMod.addElement(pModelo.vart.comentario);
            if (pModelo.vart.tipo.equals("VarLigaDesliga")) {
                linhaMod.addElement(Double.toString(pModelo.vart.posControle));
                if (pModelo.vart.bLigarAbaixo) linhaMod.addElement("LIGAR ABAIXO"); else linhaMod.addElement("LIGAR ACIMA");
                if (pModelo.vart.tipoEfeito == 1) linhaMod.addElement("QUANDO LIGADA: IGUAL A");
                if (pModelo.vart.tipoEfeito == 2) linhaMod.addElement("QUANDO LIGADA: FRACO");
                if (pModelo.vart.tipoEfeito == 3) linhaMod.addElement("QUANDO LIGADA: FORTE");
            }
            linhaMod.addElement(" ");
            i++;
        }
        i = 0;
        while (i < pModelo.alRelac.size()) {
            pModelo.relt = (Rel) pModelo.alRelac.get(i);
            linhaMod.addElement("RELACIONAMENTO------------------------------------");
            linhaMod.addElement(Integer.toString(pModelo.relt.x));
            linhaMod.addElement(Integer.toString(pModelo.relt.y));
            linhaMod.addElement(Integer.toString(pModelo.relt.xOrigem));
            linhaMod.addElement(Integer.toString(pModelo.relt.yOrigem));
            linhaMod.addElement(Integer.toString(pModelo.relt.xDestino));
            linhaMod.addElement(Integer.toString(pModelo.relt.yDestino));
            linhaMod.addElement(Integer.toString(pModelo.relt.xOrigemOrig));
            linhaMod.addElement(Integer.toString(pModelo.relt.yOrigemOrig));
            linhaMod.addElement(Integer.toString(pModelo.relt.xDestinoOrig));
            linhaMod.addElement(Integer.toString(pModelo.relt.yDestinoOrig));
            linhaMod.addElement(Integer.toString(pModelo.relt.tipoRel));
            if (pModelo.relt.bDirecaoMesma) linhaMod.addElement("DIRECAO: MESMA"); else linhaMod.addElement("DIRECAO: OPOSTA");
            if (pModelo.relt.bAcordado) linhaMod.addElement("ACORDADO"); else linhaMod.addElement("DORMINDO");
            linhaMod.addElement(Double.toString(pModelo.relt.forca));
            linhaMod.addElement(Integer.toString(pModelo.relt.corGraf.getRed()));
            linhaMod.addElement(Integer.toString(pModelo.relt.corGraf.getGreen()));
            linhaMod.addElement(Integer.toString(pModelo.relt.corGraf.getBlue()));
            linhaMod.addElement(" ");
            i++;
        }
        linhaMod.addElement("AMBIENTE------------------------------------------");
        linhaMod.addElement(metodoCalc);
        linhaMod.addElement(Integer.toString(pModelo.nIteracoes));
        linhaMod.addElement(unidades);
        linhaMod.addElement(Integer.toString(pModelo.delay));
        linhaMod.addElement(" ");
        linhaMod.addElement("FIM");
    }

    public static void selecionaUM(int pos) {
        bNovo.setSelected(false);
        bAbrir.setSelected(false);
        bRich.setSelected(false);
        bSalvar.setSelected(false);
        bImprimir.setSelected(false);
        bSelecao.setSelected(false);
        bVarContinua.setSelected(false);
        bVarLigaDesliga.setSelected(false);
        bRelTaxa.setSelected(false);
        bRelProporcao.setSelected(false);
        bPlay.setSelected(false);
        bPause.setSelected(false);
        bReinicializar.setSelected(false);
        bZerar.setSelected(false);
        if (pos == 1) bNovo.setSelected(true);
        if (pos == 2) bAbrir.setSelected(true);
        if (pos == 3) bSalvar.setSelected(true);
        if (pos == 4) bImprimir.setSelected(true);
        if (pos == 5) {
            bSelecao.setSelected(true);
            if (botaoReinic) bReinicializar.setEnabled(true); else bReinicializar.setEnabled(false);
        }
        if (pos == 6) bVarContinua.setSelected(true);
        if (pos == 7) bVarLigaDesliga.setSelected(true);
        if (pos == 8) bRelTaxa.setSelected(true);
        if (pos == 9) bRelProporcao.setSelected(true);
        if (pos == 10) bPlay.setSelected(true);
        if (pos == 11) {
            bPause.setSelected(true);
            bPause.setEnabled(true);
        }
        if (pos == 12) bReinicializar.setSelected(true);
        if (pos == 13) bZerar.setSelected(true);
    }

    public void mostraTamanho(int cont) {
        System.out.println("******painel modelos.cont=*****" + cont);
        System.out.print("Resolu��o.largura=" + resolLarg + " altura=" + resolAlt);
        Dimension sz_frame = this.getSize();
        Dimension sz_toolBar = toolBar.getSize();
        Dimension sz_splitter = splitter.getSize();
        Dimension sz_painel = painel.getSize();
        Dimension sz_pModelo = this.getSize();
        Dimension sz_pGrafico = pGrafico.getSize();
        System.out.println("sz_frame=" + sz_frame);
        System.out.println("sz_toolbar=" + sz_toolBar);
        System.out.println("sz_splitter=" + sz_splitter);
        System.out.println("sz_painel=" + sz_painel);
        System.out.println("sz_pModelo=" + sz_pModelo);
        System.out.println("sz_pGrafico=" + sz_pGrafico);
        int xx_frame = this.getX();
        int yy_frame = this.getY();
        int xx_toolBar = toolBar.getX();
        int xx_splitter = splitter.getX();
        int xx_painel = painel.getX();
        int xx_pModelo = pModelo.getX();
        int xx_pGrafico = pGrafico.getX();
        int yy_toolBar = toolBar.getY();
        int yy_splitter = splitter.getY();
        int yy_painel = painel.getY();
        int yy_pModelo = pModelo.getY();
        int yy_pGrafico = pGrafico.getY();
        int xx_divider = splitter.getDividerLocation();
        System.out.println("xx_frame=" + xx_frame + " yy_frame=" + yy_frame);
        System.out.println("xx_toolBar=" + xx_toolBar + " yy_toolBar=" + yy_toolBar);
        System.out.println("xx_splitter=" + xx_splitter + " yy_splitter=" + yy_splitter);
        System.out.println("xx_painel=" + xx_painel + " yy_painel=" + yy_painel);
        System.out.println("xx_pModelo=" + xx_pModelo + " yy_pModelo=" + yy_pModelo);
        System.out.println("xx_pGrafico=" + xx_pGrafico + " yy_pGrafico=" + yy_pGrafico);
        System.out.println("xx_divider=" + xx_divider);
    }

    public void bPauseClick() {
        bNovo.setEnabled(true);
        bAbrir.setEnabled(true);
        bSalvar.setEnabled(true);
        bImprimir.setEnabled(true);
        bSelecao.setEnabled(true);
        bVarContinua.setEnabled(true);
        bVarLigaDesliga.setEnabled(true);
        bRelTaxa.setEnabled(true);
        bRelProporcao.setEnabled(true);
        bPlay.setEnabled(true);
        bPause.setEnabled(false);
        bReinicializar.setEnabled(true);
        bZerar.setEnabled(true);
        pModelo.eventoPause();
    }

    public void bZerarClick() {
        botaoReinic = true;
        bPauseClick();
        pModelo.eventoZerar();
    }

    public void bReinicializarClick() {
        bPauseClick();
        botaoReinic = false;
        bReinicializar.setSelected(false);
        pModelo.setValoresAntesdoPlay();
    }

    public void bRelogioClick() {
        pModelo.nIteracoes = 0;
        lTempo.setText(" " + pModelo.nIteracoes);
        pGrafico.inicializarGrafico(pModelo.alVarCont);
    }

    public void bPlayClick() {
        bNovo.setEnabled(false);
        bAbrir.setEnabled(false);
        bRich.setEnabled(false);
        bSalvar.setEnabled(false);
        bImprimir.setEnabled(false);
        bSelecao.setEnabled(false);
        bVarContinua.setEnabled(false);
        bVarLigaDesliga.setEnabled(false);
        bRelTaxa.setEnabled(false);
        bRelProporcao.setEnabled(false);
        bPlay.setEnabled(false);
        bPause.setEnabled(true);
        bReinicializar.setEnabled(false);
        bZerar.setEnabled(false);
        botaoReinic = true;
        pModelo.estado = "eSelecao";
        selecionaUM(11);
        pModelo.eventoPlay();
        pModelo.armazenarValoresAntesDoPlay();
        pModelo.tinicio = System.currentTimeMillis();
        pModelo.timer.start();
    }

    public void criaModelo() {
        String linha;
        int numLinAmb;
        pModelo.alVarCont.clear();
        pModelo.alRelac.clear();
        ilMod = 0;
        linha = leProximo();
        while (ilMod < linhaMod.size()) {
            if (linha.equals("VARIAVEL------------------------------------------")) {
                linha = leProximo();
                pModelo.incluiVar(linha);
            }
            if (linha.equals("RELACIONAMENTO------------------------------------")) {
                linha = leProximo();
                pModelo.incluiRel(linha);
            }
            if (linha.equals("AMBIENTE------------------------------------------")) {
                numLinAmb = 0;
                linha = leProximo();
                while ((!linha.equals(" ")) && (ilMod < linhaMod.size())) {
                    numLinAmb = numLinAmb + 1;
                    if (numLinAmb == 1 && linha.equals("Euler")) {
                        m_Metodos[0].setSelected(true);
                        m_Metodos[1].setSelected(false);
                        metodoCalc = linha;
                    }
                    if (numLinAmb == 1 && linha.equals("Runge Kutta")) {
                        m_Metodos[0].setSelected(false);
                        m_Metodos[1].setSelected(true);
                        metodoCalc = linha;
                    }
                    if (numLinAmb == 2) {
                        pModelo.nIteracoes = Integer.parseInt(linha);
                        lTempo.setText(" " + pModelo.nIteracoes);
                    }
                    if (numLinAmb == 3) {
                        unidades = linha;
                        boolean achou = false;
                        int k = 0;
                        while (!achou && (k < unidadesTempo.length)) {
                            if (unidadesTempo[k].equals(unidades)) achou = true; else k++;
                        }
                        if (achou) cBoxUnidades.setSelectedIndex(k);
                    }
                    if (numLinAmb == 4) {
                        pModelo.delay = Integer.parseInt(linha);
                        pModelo.timer.setDelay(pModelo.delay);
                    }
                    linha = leProximo();
                }
            }
            linha = leProximo();
        }
    }

    public static String leProximo() {
        ilMod++;
        if (ilMod < linhaMod.size()) {
            return (String) linhaMod.elementAt(ilMod);
        } else return "";
    }

    public String getArquivo(String nomeArq) {
        String arquivo = "";
        String linha = "";
        try {
            URL url = new URL(getCodeBase(), "./abridor?arq=" + user + "/" + nomeArq);
            URLConnection con = url.openConnection();
            con.setUseCaches(false);
            InputStream in = con.getInputStream();
            DataInputStream result = new DataInputStream(new BufferedInputStream(in));
            while ((linha = result.readLine()) != null) {
                arquivo += linha + "\n";
            }
            return arquivo;
        } catch (Exception e) {
            return null;
        }
    }

    public InputStream getArquivoS(String nomeArq) {
        try {
            URL url = new URL(getCodeBase(), "./abridor?arq=" + user + "/" + nomeArq);
            URLConnection con = url.openConnection();
            con.setUseCaches(false);
            InputStream in = con.getInputStream();
            return in;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveArquivo(String nomeArq, String modelo) {
        try {
            URL urlServlet = new URL(getCodeBase(), "./salvador?arq=" + user + "/" + nomeArq);
            System.out.println("user " + user);
            URLConnection con = urlServlet.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
            OutputStream outstream = con.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(outstream);
            oos.writeObject(modelo);
            oos.flush();
            oos.close();
            InputStream instr = con.getInputStream();
            ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
            String result = (String) inputFromServlet.readObject();
            inputFromServlet.close();
            instr.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void actionPerformed2(ActionEvent arg0) {
        if ((EscolhedorDeArquivos.combo.getSelectedIndex() == 0) && (EscolhedorDeArquivos.texto.getText().equalsIgnoreCase(""))) {
            EscolhedorDeArquivos.nomeArq = null;
            JOptionPane.showMessageDialog(null, "Nenhum arquivo selecionado", "Erro", JOptionPane.ERROR_MESSAGE);
            System.out.println("Nenhum arquivo selecionado");
        } else {
            if (EscolhedorDeArquivos.combo.getSelectedIndex() == 0) {
                EscolhedorDeArquivos.nomeArq = EscolhedorDeArquivos.texto.getText();
            }
            System.out.println(EscolhedorDeArquivos.nomeArq);
            JLinkitFrame.nomeDoArquivo = EscolhedorDeArquivos.nomeArq.trim();
            EscolhedorDeArquivos.eu.dispose();
            if (EscolhedorDeArquivos.status == 1) {
                System.out.println("salva modelo");
                eventoGravar();
            }
            if (EscolhedorDeArquivos.status == 0) {
                System.out.println("abre modelo");
                abreModeloWeb();
            }
            if (EscolhedorDeArquivos.status == 4) {
                System.out.println("salva modelo e novo");
                eventoGravar();
                eventoNovoModelo();
            }
            if (EscolhedorDeArquivos.status == 3) {
                System.out.println("salva modelo e abre modelo");
                eventoGravar();
                EscolhedorDeArquivos ed2 = new EscolhedorDeArquivos(euApplet, 0);
                ed2.setVisible(true);
            }
            if (EscolhedorDeArquivos.status == 5) {
                System.out.println("salva modelo e sai");
                eventoGravar();
                fechaJanelaJS();
            }
            if (EscolhedorDeArquivos.status == 2) {
                System.out.println("Salva arquivo como");
                eventoGravar();
                abreModeloWeb();
            }
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        if (EscolhedorDeArquivos.texto.getText().equalsIgnoreCase("")) {
            EscolhedorDeArquivos.nomeArq = null;
            JOptionPane.showMessageDialog(null, "Nenhum arquivo selecionado", "Erro", JOptionPane.ERROR_MESSAGE);
            System.out.println("Nenhum arquivo selecionado");
        } else {
            EscolhedorDeArquivos.texto.setText(EscolhedorDeArquivos.texto.getText().replaceAll(" ", ""));
            JLinkitFrame.nomeDoArquivo = EscolhedorDeArquivos.texto.getText();
            EscolhedorDeArquivos.eu.dispose();
            if (EscolhedorDeArquivos.status == 1) {
                System.out.println("salva modelo");
                eventoGravar();
            }
            if (EscolhedorDeArquivos.status == 0) {
                System.out.println("abre modelo");
                abreModeloWeb();
            }
            if (EscolhedorDeArquivos.status == 4) {
                System.out.println("salva modelo e novo");
                eventoGravar();
                eventoNovoModelo();
            }
            if (EscolhedorDeArquivos.status == 3) {
                System.out.println("salva modelo e abre modelo");
                eventoGravar();
                EscolhedorDeArquivos ed2 = new EscolhedorDeArquivos(euApplet, 0);
                ed2.setVisible(true);
            }
            if (EscolhedorDeArquivos.status == 5) {
                System.out.println("salva modelo e sai");
                eventoGravar();
                fechaJanelaJS();
            }
            if (EscolhedorDeArquivos.status == 2) {
                System.out.println("Salva arquivo como");
                eventoGravar();
                abreModeloWeb();
            }
        }
    }

    public void fechaJanelaJS() {
        try {
            JSObject jso = JSObject.getWindow(euApplet);
            jso.eval("window.close()");
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public void zeraBotoes() {
        bAbrir.setSelected(false);
        bNovo.setSelected(false);
        bRich.setSelected(false);
        bImprimir.setSelected(false);
        bSalvar.setSelected(false);
    }

    public class HtmlFilter extends javax.swing.filechooser.FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = Utils.getExtension(f);
            return extension != null && (extension.equals("rtf") || extension.equals("html") || extension.equals("htm"));
        }

        public String getDescription() {
            return "*.html";
        }
    }
}
