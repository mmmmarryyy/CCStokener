package app;

import info.clearthought.layout.TableLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.MasonTagTypes;
import net.htmlparser.jericho.MicrosoftConditionalCommentTagTypes;
import net.htmlparser.jericho.PHPTagTypes;
import net.htmlparser.jericho.Source;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
public class ConfigGUI extends Config {

    /**
	 * 
	 */
    private static final long serialVersionUID = -2135813929274175464L;

    private JTabbedPane tab;

    private JList jListImagenes;

    private JToggleButton btnItalica;

    private JButton preview1;

    private JLabel jLabel3;

    private JLabel previsualizacion;

    private Font fuenteBoton = new java.awt.Font("Arial", 0, 12);

    private JLabel descLabel;

    private JTextField url;

    private JLabel labelPrev;

    private JLabel jLabel1;

    private JLabel jLabel9;

    private JTextField pathBrowser;

    private JButton selectBrowser;

    private JButton testBrowser;

    private JTextField selFuente;

    private JButton u;

    private JButton b;

    private JButton i;

    private JTextField desc;

    private JButton aceptar;

    private JSeparator jSeparator1;

    private JButton nuevo;

    private JButton borrarFuente;

    private JButton borrarColor;

    private JButton borrarTam;

    private JButton borrarAli;

    private JButton borrar;

    private JLabel selColor;

    private JLabel selTamanio;

    private JLabel selAlineacion;

    private JToggleButton btnNegrita;

    private JToggleButton btnSub;

    private JLabel Desc;

    private JList jListEstilos;

    private JScrollPane jScrollPane1;

    private JTextField descripcion;

    private JButton guardar;

    private JComboBox tamanio;

    private JComboBox fuente;

    private JComboBox color;

    private JComboBox alineacion;

    private JPanel jPanel1;

    private JPanel jPanel5;

    private JPanel panel1;

    private JPanel panel3;

    CookiesInJava cook;

    private boolean pulsado = false;

    private boolean[] pulsadoCheckbox = { false, false, false, false, false };

    private DefaultListModel fuentesModel;

    private DefaultListModel estilosModel;

    private DefaultListModel listaImagenesModel;

    private DefaultListModel listaComunidadesModel;

    private JSeparator jSeparator2;

    private JLabel jLabel5;

    private JCheckBox centrarImagenesPI;

    private JLabel jLabel6;

    private JButton btnActualizar;

    private JList listaComunidades;

    private JScrollPane jScrollPane4;

    private JButton btnUnir;

    private JPasswordField jContrasenia;

    private JTextField jUsuario;

    private JLabel lContrasenia;

    private JLabel lUsuario;

    private JPanel jPanel6;

    private JCheckBox updates;

    private JLabel jLabel8;

    private JList jListFuentes;

    private JButton btnNuevoFuente;

    private JButton btnEliminarFuente;

    private JButton btnGuardarFuente;

    private JTextField txtFuente;

    private JScrollPane jScrollPane3;

    private JPanel jPanel4;

    private JCheckBox activarInicioPI;

    private JCheckBox incluirLinkPI;

    private JPanel jPanel3;

    private JLabel jLabel4;

    private JLabel jLabel2;

    private JPanel jPanel2;

    private JButton guardarImg;

    private JButton eliminarImg;

    private JButton nuevoImg;

    private JScrollPane jScrollPane2;

    private JLabel jLabel7;

    private ArrayList<String> fuentesPTemp;

    private ArrayList<String[]> estilosTemp;

    protected ArrayList<String[]> listaImagenesTemp;

    private JSpinner timer;

    private JCheckBox checkNumLineas;

    private boolean change = false;

    @SuppressWarnings("unused")
    private static final String ACTION_CLOSE = "ACTION_CLOSE";

    KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    /**
	 * 
	 * Auto-generated main method to display this JFrame
	 */
    public ConfigGUI(Main m, CookiesInJava cook) {
        super(m);
        fuentes = main.getStringFuentes();
        colores = main.getStringColores();
        sizes = main.getStringSizes();
        addEscapeKey();
        this.cook = cook;
        this.addWindowListener(new WindowEventHandler());
        initGUI();
        cargar();
        updateGUI();
    }

    {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetSelection() {
        tab.setSelectedIndex(0);
    }

    public void resetBoolean() {
        pulsado = false;
    }

    private void initGUI() {
        try {
            TableLayout thisLayout = new TableLayout(new double[][] { { TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL }, { TableLayout.FILL, TableLayout.FILL, 208.0, TableLayout.FILL } });
            thisLayout.setHGap(5);
            thisLayout.setVGap(5);
            getContentPane().setLayout(thisLayout);
            this.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("app/postinga16x16.png")).getImage());
            this.setTitle("Preferencias");
            {
                tab = new JTabbedPane();
                getContentPane().add(tab, "0, 0, 3, 2");
                tab.setTabPlacement(JTabbedPane.LEFT);
                {
                    jPanel6 = new JPanel();
                    tab.addTab("<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Usuario</body></html>", null, jPanel6, null);
                    TableLayout jPanel6Layout = new TableLayout(new double[][] { { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 }, { 7.0, 29.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 52.0, 28.0, 7.0 } });
                    jPanel6Layout.setHGap(5);
                    jPanel6Layout.setVGap(5);
                    jPanel6.setLayout(jPanel6Layout);
                    {
                        lUsuario = new JLabel();
                        jPanel6.add(getJScrollPane4(), "4, 2, 4, 5");
                        jPanel6.add(lUsuario, "1, 2, r, c");
                        lUsuario.setText("Usuario");
                        lUsuario.setFont(new java.awt.Font("Arial", 0, 14));
                    }
                    {
                        lContrasenia = new JLabel();
                        jPanel6.add(lContrasenia, "1, 3, r, c");
                        lContrasenia.setText("Contraseña");
                        lContrasenia.setFont(new java.awt.Font("Arial", 0, 14));
                    }
                    {
                        jUsuario = new JTextField();
                        jPanel6.add(jUsuario, "2, 2, f, c");
                        jUsuario.addKeyListener(new KeyAdapter() {

                            public void keyPressed(KeyEvent evt) {
                                jUsuarioKeyPressed(evt);
                            }
                        });
                    }
                    {
                        jContrasenia = new JPasswordField();
                        jPanel6.add(jContrasenia, "2, 3, f, c");
                        jContrasenia.addFocusListener(new FocusAdapter() {

                            public void focusLost(FocusEvent evt) {
                                jContraseniaFocusLost(evt);
                            }

                            public void focusGained(FocusEvent evt) {
                                jContraseniaFocusGained(evt);
                            }
                        });
                    }
                    {
                        btnUnir = new JButton();
                        jPanel6.add(btnUnir, "2, 4, 3, 4, c, c");
                        jPanel6.add(getBtnActualizar(), "4, 6, c, b");
                        jPanel6.add(getJLabel6(), "4,1,c,t");
                        jPanel6.add(getJLabel9(), "2, 2, 3, 2, f, b");
                        btnUnir.setText("Unite a la Comunidad PosT!NGA");
                        btnUnir.setFont(new java.awt.Font("Arial", 0, 12));
                        btnUnir.setSize(210, 26);
                        btnUnir.setPreferredSize(new java.awt.Dimension(240, 26));
                        btnUnir.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                btnUnirActionPerformed(evt);
                            }
                        });
                    }
                }
                {
                    panel1 = new JPanel();
                    tab.addTab("<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Navegador</body></html>", null, panel1, null);
                    TableLayout panel1Layout = new TableLayout(new double[][] { { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 }, { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 } });
                    panel1Layout.setHGap(5);
                    panel1Layout.setVGap(5);
                    panel1.setLayout(panel1Layout);
                    tab.addTab("<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Navegador</body></html>", null, panel1, null);
                    panel1.setFont(new java.awt.Font("Arial", 1, 12));
                    {
                        testBrowser = new JButton();
                        panel1.add(testBrowser, "4, 3, c, f");
                        testBrowser.setText("Probar");
                        testBrowser.setIcon(new ImageIcon(Main.class.getResource("probar.png")));
                        testBrowser.setFont(fuenteBoton);
                        testBrowser.setBackground(new java.awt.Color(255, 255, 255));
                        testBrowser.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                testBrowserActionPerformed(evt);
                            }
                        });
                    }
                    {
                        selectBrowser = new JButton();
                        panel1.add(selectBrowser, "4,2,c,f");
                        selectBrowser.setText("Buscar");
                        selectBrowser.setIcon(new ImageIcon(Main.class.getResource("buscar.png")));
                        selectBrowser.setFont(fuenteBoton);
                        selectBrowser.setBackground(new java.awt.Color(255, 255, 255));
                        selectBrowser.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                selectBrowserActionPerformed(evt);
                            }
                        });
                    }
                    {
                        pathBrowser = new JTextField();
                        panel1.add(pathBrowser, "2, 2, 3, 2");
                        pathBrowser.setFont(new java.awt.Font("Ubuntu", 0, 12));
                        pathBrowser.setText("");
                        pathBrowser.setName("pathBrowser");
                    }
                    {
                        jLabel1 = new JLabel();
                        panel1.add(jLabel1, "1,2,c,f");
                        jLabel1.setText("Ejecutable");
                        jLabel1.setFont(new java.awt.Font("Arial", 0, 14));
                    }
                    {
                        jLabel2 = new JLabel();
                        panel1.add(jLabel2, "2, 3, 3, 3");
                        jLabel2.setText("Ingresa el path del ejecutable del navegador");
                        jLabel2.setFont(new java.awt.Font("Arial", 0, 12));
                    }
                    {
                        jLabel4 = new JLabel();
                        panel1.add(jLabel4, "2, 4, 3, 4");
                        jLabel4.setText("Por ej: C:\\Archivos de programa\\Mozilla\\firefox.exe\n ó /usr/bin/firefox");
                        jLabel4.setFont(new java.awt.Font("Arial", 0, 12));
                    }
                }
                {
                    jPanel1 = new JPanel();
                    tab.addTab("<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Estilos</body></html>", null, jPanel1, null);
                    TableLayout jPanel1Layout = new TableLayout(new double[][] { { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 130.0, 117.0, 7.0 }, { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 } });
                    jPanel1Layout.setHGap(5);
                    jPanel1Layout.setVGap(5);
                    jPanel1.setPreferredSize(new java.awt.Dimension(894, 341));
                    jPanel1.setLayout(jPanel1Layout);
                    {
                        ComboBoxModel alineacionModel = new DefaultComboBoxModel(new String[] { "Alineacion", "Izquierda", "Centro", "Derecha" });
                        alineacion = new JComboBox();
                        jPanel1.add(alineacion, "1, 7, c, c");
                        alineacion.setModel(alineacionModel);
                        alineacion.setSelectedIndex(0);
                        alineacion.setFont(new java.awt.Font("Ubuntu", 0, 12));
                        alineacion.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                alineacionActionPerformed(evt);
                            }
                        });
                    }
                    {
                        ComboBoxModel colorModel = new DefaultComboBoxModel(colores);
                        color = new JComboBox();
                        jPanel1.add(color, "3, 7, c, c");
                        color.setModel(colorModel);
                        color.setSelectedIndex(0);
                        color.setVisible(true);
                        color.setFont(new java.awt.Font("Ubuntu", 0, 12));
                        color.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                colorActionPerformed(evt);
                            }
                        });
                    }
                    {
                        ComboBoxModel fuenteModel = new DefaultComboBoxModel(fuentes);
                        fuente = new JComboBox();
                        jPanel1.add(fuente, "4, 7, c, c");
                        fuente.setModel(fuenteModel);
                        fuente.setSelectedIndex(0);
                        fuente.setFont(new java.awt.Font("Ubuntu", 0, 12));
                        fuente.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                fuenteActionPerformed(evt);
                            }
                        });
                    }
                    {
                        ComboBoxModel tamanioModel = new DefaultComboBoxModel(sizes);
                        tamanio = new JComboBox();
                        jPanel1.add(tamanio, "2, 7, c, c");
                        tamanio.setModel(tamanioModel);
                        tamanio.setSelectedIndex(0);
                        tamanio.setFont(new java.awt.Font("Ubuntu", 0, 12));
                        tamanio.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                tamanioActionPerformed(evt);
                            }
                        });
                    }
                    {
                        descripcion = new JTextField();
                        jPanel1.add(descripcion, "2, 5, 4, 5");
                        descripcion.setFont(new java.awt.Font("Arial", 0, 12));
                        descripcion.setText(null);
                    }
                    {
                        jScrollPane1 = new JScrollPane();
                        jPanel1.add(jScrollPane1, "2, 1, 4, 4");
                        jScrollPane1.setPreferredSize(new java.awt.Dimension(10, 10));
                        jScrollPane1.setSize(10, 10);
                        {
                            jListEstilos = new JList();
                            jScrollPane1.setViewportView(jListEstilos);
                            jListEstilos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            jListEstilos.setFont(new java.awt.Font("Arial", 0, 12));
                            jListEstilos.addListSelectionListener(new ListSelectionListener() {

                                public void valueChanged(ListSelectionEvent evt) {
                                    jListEstilosValueChanged(evt);
                                }
                            });
                        }
                    }
                    {
                        Desc = new JLabel();
                        jPanel1.add(Desc, "1, 5, r, f");
                        Desc.setText("Descripcion  ");
                        Desc.setFont(new java.awt.Font("Arial", 0, 14));
                    }
                    {
                        btnItalica = new JToggleButton();
                        jPanel1.add(btnItalica, "5, 7, l, c");
                        btnItalica.setIcon(new ImageIcon(Main.class.getResource("italic-20x20.jpg")));
                        btnItalica.setSelected(false);
                        btnItalica.setFont(new java.awt.Font("Arial", 2, 12));
                        btnItalica.setBackground(new java.awt.Color(255, 255, 255));
                        btnItalica.setPreferredSize(new java.awt.Dimension(20, 20));
                        btnItalica.setToolTipText("Italica");
                        btnItalica.setSize(20, 20);
                        btnItalica.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                btnItalicaActionPerformed(evt);
                            }
                        });
                    }
                    {
                        btnSub = new JToggleButton();
                        jPanel1.add(btnSub, "5, 7, r, c");
                        btnSub.setIcon(new ImageIcon(Main.class.getResource("underline-20x20.jpg")));
                        btnSub.setSelected(false);
                        btnSub.setFont(new java.awt.Font("Arial", 0, 12));
                        btnSub.setBackground(new java.awt.Color(255, 255, 255));
                        btnSub.setPreferredSize(new java.awt.Dimension(20, 20));
                        btnSub.setToolTipText("Subrayado");
                        btnSub.setSize(20, 20);
                        btnSub.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                btnSubActionPerformed(evt);
                            }
                        });
                    }
                    {
                        btnNegrita = new JToggleButton();
                        jPanel1.add(btnNegrita, "5, 7, c, c");
                        btnNegrita.setIcon(new ImageIcon(Main.class.getResource("bold-20x20.jpg")));
                        btnNegrita.setSelected(false);
                        btnNegrita.setFont(new java.awt.Font("Arial", 1, 12));
                        btnNegrita.setBackground(new java.awt.Color(255, 255, 255));
                        btnNegrita.setPreferredSize(new java.awt.Dimension(20, 20));
                        btnNegrita.setToolTipText("Negrita");
                        btnNegrita.setSize(20, 20);
                        btnNegrita.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                btnNegritaActionPerformed(evt);
                            }
                        });
                    }
                    {
                        selAlineacion = new JLabel();
                        jPanel1.add(selAlineacion, "1, 8, c, f");
                        selAlineacion.setText("");
                    }
                    {
                        selTamanio = new JLabel();
                        jPanel1.add(selTamanio, "2, 8, c, f");
                        selTamanio.setText("");
                    }
                    {
                        selColor = new JLabel();
                        jPanel1.add(selColor, "3, 8, c, f");
                        selColor.setText("");
                    }
                    {
                        borrar = new JButton();
                        jPanel1.add(borrar, "5,2,r,f");
                        borrar.setText("Eliminar");
                        borrar.setIcon(new ImageIcon(Main.class.getResource("minus.png")));
                        borrar.setFont(fuenteBoton);
                        borrar.setBackground(new java.awt.Color(255, 255, 255));
                        borrar.setPreferredSize(new java.awt.Dimension(105, 12));
                        borrar.setSize(90, 28);
                        borrar.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                borrarActionPerformed(evt);
                            }
                        });
                    }
                    {
                        borrarAli = new JButton();
                        jPanel1.add(borrarAli, "1, 9, c, c");
                        borrarAli.setText("Limpiar");
                        borrarAli.setIcon(new ImageIcon(Main.class.getResource("clean-20x20.png")));
                        borrarAli.setFont(new java.awt.Font("Arial", 0, 12));
                        borrarAli.setBackground(new java.awt.Color(255, 255, 255));
                        borrarAli.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                borrarAliActionPerformed(evt);
                            }
                        });
                    }
                    {
                        borrarTam = new JButton();
                        jPanel1.add(borrarTam, "2, 9, c, c");
                        borrarTam.setText("Limpiar");
                        borrarTam.setIcon(new ImageIcon(Main.class.getResource("clean-20x20.png")));
                        borrarTam.setFont(new java.awt.Font("Arial", 0, 12));
                        borrarTam.setBackground(new java.awt.Color(255, 255, 255));
                        borrarTam.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                borrarTamActionPerformed(evt);
                            }
                        });
                    }
                    {
                        borrarColor = new JButton();
                        jPanel1.add(borrarColor, "3, 9, c, c");
                        borrarColor.setText("Limpiar");
                        borrarColor.setIcon(new ImageIcon(Main.class.getResource("clean-20x20.png")));
                        borrarColor.setFont(new java.awt.Font("Arial", 0, 12));
                        borrarColor.setBackground(new java.awt.Color(255, 255, 255));
                        borrarColor.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                borrarColorActionPerformed(evt);
                            }
                        });
                    }
                    {
                        borrarFuente = new JButton();
                        jPanel1.add(borrarFuente, "4, 9, c, c");
                        borrarFuente.setText("Limpiar");
                        borrarFuente.setIcon(new ImageIcon(Main.class.getResource("clean-20x20.png")));
                        borrarFuente.setFont(new java.awt.Font("Arial", 0, 12));
                        borrarFuente.setBackground(new java.awt.Color(255, 255, 255));
                        borrarFuente.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                borrarFuenteActionPerformed(evt);
                            }
                        });
                    }
                    {
                        nuevo = new JButton();
                        jPanel1.add(nuevo, "5, 1, r, f");
                        nuevo.setText(" Nuevo   ");
                        nuevo.setIcon(new ImageIcon(Main.class.getResource("plus.png")));
                        nuevo.setFont(fuenteBoton);
                        nuevo.setBackground(new java.awt.Color(255, 255, 255));
                        nuevo.setPreferredSize(new java.awt.Dimension(105, 12));
                        nuevo.setSize(90, 28);
                        nuevo.setOpaque(false);
                        nuevo.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                nuevoActionPerformed(evt);
                            }
                        });
                    }
                    {
                        jSeparator1 = new JSeparator();
                        jPanel1.add(jSeparator1, "1, 6, 5, 6, f, t");
                    }
                    {
                        aceptar = new JButton();
                        jPanel1.add(aceptar, "5, 3, r, f");
                        aceptar.setText("Agregar ");
                        aceptar.setIcon(new ImageIcon(Main.class.getResource("ok.png")));
                        aceptar.setFont(fuenteBoton);
                        aceptar.setBackground(new java.awt.Color(255, 255, 255));
                        aceptar.setPreferredSize(new java.awt.Dimension(105, 12));
                        aceptar.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                aceptarActionPerformed(evt);
                            }
                        });
                    }
                    {
                        i = new JButton();
                        jPanel1.add(i, "5, 6, l, b");
                        i.setIcon(new ImageIcon(getClass().getResource("itemSelected.png")));
                        i.setBorderPainted(false);
                        i.setVisible(false);
                        i.setPreferredSize(new java.awt.Dimension(10, 10));
                        i.setSize(10, 10);
                    }
                    {
                        b = new JButton();
                        jPanel1.add(b, "5, 6, c, b");
                        b.setIcon(new ImageIcon(getClass().getResource("itemSelected.png")));
                        b.setBorderPainted(false);
                        b.setVisible(false);
                        b.setPreferredSize(new java.awt.Dimension(10, 10));
                        b.setSize(10, 10);
                    }
                    {
                        u = new JButton();
                        jPanel1.add(u, "5, 6, r, b");
                        u.setIcon(new ImageIcon(getClass().getResource("itemSelected.png")));
                        u.setBorderPainted(false);
                        u.setVisible(false);
                        u.setPreferredSize(new java.awt.Dimension(10, 10));
                        u.setSize(10, 10);
                    }
                    {
                        selFuente = new JTextField();
                        jPanel1.add(selFuente, "4, 8");
                        selFuente.setText("");
                        selFuente.setHorizontalAlignment(JTextField.CENTER);
                        selFuente.setBorder(BorderFactory.createCompoundBorder(null, null));
                    }
                }
                {
                    panel3 = new JPanel();
                    TableLayout panel3Layout = new TableLayout(new double[][] { { 7.0, 231.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 }, { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 } });
                    panel3Layout.setHGap(5);
                    panel3Layout.setVGap(5);
                    panel3.setLayout(panel3Layout);
                    tab.addTab("<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Imagenes</body></html>", null, panel3, null);
                    panel3.setPreferredSize(new java.awt.Dimension(879, 335));
                    {
                        labelPrev = new JLabel();
                        panel3.add(labelPrev, "2,4,4,9,c,f");
                    }
                    {
                        descLabel = new JLabel();
                        panel3.add(descLabel, "2, 2");
                        descLabel.setText("Descripcion");
                        descLabel.setFont(new java.awt.Font("Arial", 0, 14));
                    }
                    {
                        desc = new JTextField();
                        panel3.add(desc, "3, 2, 4, 2");
                        desc.setText("");
                        desc.setName("desc");
                        desc.setFont(new java.awt.Font("Arial", 0, 12));
                    }
                    {
                        previsualizacion = new JLabel();
                        panel3.add(previsualizacion, "2, 3, 4, 3, c, f");
                        previsualizacion.setText("Previsualizacion");
                        previsualizacion.setFont(new java.awt.Font("Arial", 1, 14));
                    }
                    {
                        jLabel3 = new JLabel();
                        panel3.add(jLabel3, "2, 1");
                        jLabel3.setText("URL de la imagen");
                        jLabel3.setFont(new java.awt.Font("Arial", 0, 14));
                    }
                    {
                        preview1 = new JButton();
                        panel3.add(preview1, "5,4,r,c");
                        preview1.setIcon(new ImageIcon(Main.class.getResource("find.png")));
                        preview1.setBackground(new java.awt.Color(255, 255, 255));
                        preview1.setPreferredSize(new java.awt.Dimension(30, 30));
                        preview1.setToolTipText("Previsualizar");
                        preview1.setSize(30, 30);
                        preview1.setFont(new java.awt.Font("Ubuntu", 0, 12));
                        preview1.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                try {
                                    preview1ActionPerformed(evt);
                                } catch (IOException e) {
                                }
                            }
                        });
                    }
                    {
                        url = new JTextField();
                        panel3.add(url, "3, 1, 4, 1");
                        url.setText("");
                        url.setName("url");
                        url.setFont(new java.awt.Font("Arial", 0, 12));
                    }
                    {
                        jScrollPane2 = new JScrollPane();
                        panel3.add(jScrollPane2, "1, 0, 1, 10");
                        {
                            jListImagenes = new JList();
                            jScrollPane2.setViewportView(jListImagenes);
                            jListImagenes.setPreferredSize(new java.awt.Dimension(222, 271));
                            jListImagenes.setFont(new java.awt.Font("Arial", 0, 12));
                            jListImagenes.addListSelectionListener(new ListSelectionListener() {

                                public void valueChanged(ListSelectionEvent evt) {
                                    jListImagenesValueChanged(evt);
                                }
                            });
                        }
                    }
                    {
                        nuevoImg = new JButton();
                        panel3.add(nuevoImg, "5, 1, r, f");
                        nuevoImg.setText(" Nuevo   ");
                        nuevoImg.setIcon(new ImageIcon(Main.class.getResource("plus.png")));
                        nuevoImg.setFont(fuenteBoton);
                        nuevoImg.setBackground(new java.awt.Color(255, 255, 255));
                        nuevoImg.setSize(90, 28);
                        nuevoImg.setPreferredSize(new java.awt.Dimension(105, 12));
                        nuevoImg.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                nuevoImgActionPerformed(evt);
                            }
                        });
                    }
                    {
                        eliminarImg = new JButton();
                        panel3.add(eliminarImg, "5, 2, r, f");
                        eliminarImg.setText("Eliminar");
                        eliminarImg.setIcon(new ImageIcon(Main.class.getResource("minus.png")));
                        eliminarImg.setFont(fuenteBoton);
                        eliminarImg.setBackground(new java.awt.Color(255, 255, 255));
                        eliminarImg.setSize(90, 28);
                        eliminarImg.setPreferredSize(new java.awt.Dimension(105, 12));
                        eliminarImg.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                eliminarImgActionPerformed(evt);
                            }
                        });
                    }
                    {
                        guardarImg = new JButton();
                        panel3.add(guardarImg, "5, 3, r, f");
                        guardarImg.setText("Agregar ");
                        guardarImg.setIcon(new ImageIcon(Main.class.getResource("ok.png")));
                        guardarImg.setFont(fuenteBoton);
                        guardarImg.setBackground(new java.awt.Color(255, 255, 255));
                        guardarImg.setPreferredSize(new java.awt.Dimension(105, 12));
                        guardarImg.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                guardarImgActionPerformed(evt);
                            }
                        });
                    }
                }
                {
                    jPanel4 = new JPanel();
                    tab.addTab("<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Fuentes</body></html>", null, jPanel4, null);
                    TableLayout jPanel4Layout = new TableLayout(new double[][] { { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 }, { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 } });
                    jPanel4Layout.setHGap(5);
                    jPanel4Layout.setVGap(5);
                    jPanel4.setLayout(jPanel4Layout);
                    {
                        jScrollPane3 = new JScrollPane();
                        jPanel4.add(jScrollPane3, "3, 2, 4, 8");
                        {
                            jListFuentes = new JList();
                            jScrollPane3.setViewportView(jListFuentes);
                            jListFuentes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            jListFuentes.setPreferredSize(new java.awt.Dimension(284, 190));
                            jListFuentes.setFont(new java.awt.Font("Arial", 0, 12));
                            jListFuentes.addListSelectionListener(new ListSelectionListener() {

                                public void valueChanged(ListSelectionEvent evt) {
                                    jListFuentesValueChanged(evt);
                                }
                            });
                        }
                    }
                    {
                        txtFuente = new JTextField();
                        jPanel4.add(txtFuente, "3, 9, 4, 9, f, c");
                    }
                    {
                        jLabel8 = new JLabel();
                        jPanel4.add(jLabel8, "3,1,4,1,c,f");
                        jLabel8.setText("Fuentes personalizadas");
                        jLabel8.setFont(new java.awt.Font("Arial", 1, 14));
                    }
                    {
                        btnGuardarFuente = new JButton();
                        jPanel4.add(btnGuardarFuente, "6,3,r,f");
                        btnGuardarFuente.setText("Agregar ");
                        btnGuardarFuente.setIcon(new ImageIcon(Main.class.getResource("ok.png")));
                        btnGuardarFuente.setFont(fuenteBoton);
                        btnGuardarFuente.setBackground(new java.awt.Color(255, 255, 255));
                        btnGuardarFuente.setPreferredSize(new java.awt.Dimension(105, 12));
                        btnGuardarFuente.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                btnGuardarFuenteActionPerformed(evt);
                            }
                        });
                    }
                    {
                        btnEliminarFuente = new JButton();
                        jPanel4.add(btnEliminarFuente, "6,2,r,f");
                        btnEliminarFuente.setText("Eliminar");
                        btnEliminarFuente.setIcon(new ImageIcon(Main.class.getResource("minus.png")));
                        btnEliminarFuente.setFont(fuenteBoton);
                        btnEliminarFuente.setBackground(new java.awt.Color(255, 255, 255));
                        btnEliminarFuente.setPreferredSize(new java.awt.Dimension(105, 12));
                        btnEliminarFuente.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                btnEliminarFuenteActionPerformed(evt);
                            }
                        });
                    }
                    {
                        btnNuevoFuente = new JButton();
                        jPanel4.add(btnNuevoFuente, "6,1,r,f");
                        btnNuevoFuente.setText(" Nuevo   ");
                        btnNuevoFuente.setIcon(new ImageIcon(Main.class.getResource("plus.png")));
                        btnNuevoFuente.setFont(fuenteBoton);
                        btnNuevoFuente.setBackground(new java.awt.Color(255, 255, 255));
                        btnNuevoFuente.setPreferredSize(new java.awt.Dimension(105, 12));
                        btnNuevoFuente.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                btnNuevoFuenteActionPerformed(evt);
                            }
                        });
                    }
                }
                {
                    jPanel3 = new JPanel();
                    tab.addTab("<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Auto</body></html>", null, jPanel3, null);
                    TableLayout jPanel3Layout = new TableLayout(new double[][] { { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 }, { 7.0, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, 7.0 } });
                    jPanel3Layout.setHGap(5);
                    jPanel3Layout.setVGap(5);
                    jPanel3.setLayout(jPanel3Layout);
                    jPanel3.setPreferredSize(new java.awt.Dimension(898, 292));
                    {
                        incluirLinkPI = new JCheckBox();
                        jPanel3.add(incluirLinkPI, "1, 3");
                        incluirLinkPI.setText("Incluir links por debajo");
                        incluirLinkPI.setFont(new java.awt.Font("Arial", 0, 12));
                        incluirLinkPI.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                incluirLinkPIActionPerformed(evt);
                            }
                        });
                    }
                    {
                        centrarImagenesPI = new JCheckBox();
                        jPanel3.add(centrarImagenesPI, "1, 2");
                        centrarImagenesPI.setText("Centrar Imagenes");
                        centrarImagenesPI.setFont(new java.awt.Font("Arial", 0, 12));
                        centrarImagenesPI.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                centrarImagenesPIActionPerformed(evt);
                            }
                        });
                    }
                    {
                        jLabel5 = new JLabel();
                        jPanel3.add(jLabel5, "1, 1");
                        jLabel5.setText("Pegado Inteligente");
                        jLabel5.setFont(new java.awt.Font("Arial", 1, 14));
                    }
                    {
                        jSeparator2 = new JSeparator();
                        jPanel3.add(jSeparator2, "1, 4, 4, 4, f, c");
                    }
                    {
                        activarInicioPI = new JCheckBox();
                        jPanel3.add(activarInicioPI, "2, 2");
                        activarInicioPI.setText("Activar al inicio");
                        activarInicioPI.setFont(new java.awt.Font("Arial", 0, 12));
                        activarInicioPI.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                activarInicioPIActionPerformed(evt);
                            }
                        });
                    }
                    {
                        updates = new JCheckBox();
                        jPanel3.add(updates, "1, 5, 2, 5");
                        jPanel3.add(getJLabel7(), "1,6,l,c");
                        jPanel3.add(getTimer(), "2,6,l,c");
                        jPanel3.add(getCheckNumLineas(), "1, 7, 2, 7");
                        updates.setText("Chequear actualizaciones al iniciar");
                        updates.setFont(new java.awt.Font("Arial", 0, 12));
                    }
                }
                {
                    jPanel5 = new JPanel();
                }
            }
            {
                jPanel2 = new JPanel();
                getContentPane().add(jPanel2, "0,3,3,3,r,c");
                {
                    guardar = new JButton();
                    jPanel2.add(guardar);
                    guardar.setText("Guardar");
                    guardar.setIcon(new ImageIcon(Main.class.getResource("save-20x20.gif")));
                    guardar.setFont(new java.awt.Font("Arial", 0, 12));
                    guardar.setBackground(new java.awt.Color(255, 255, 255));
                    guardar.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            guardarActionPerformed(evt);
                        }
                    });
                }
            }
            pack();
            getPopup1();
            setDefaultCloseOperation(ConfigGUI.DO_NOTHING_ON_CLOSE);
            this.setSize(986, 404);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void alineacionActionPerformed(ActionEvent evt) {
        if ((String) alineacion.getSelectedItem() != "Alineacion") selAlineacion.setText((String) alineacion.getSelectedItem()); else selAlineacion.setText("");
        alineacion.setSelectedIndex(0);
    }

    private void tamanioActionPerformed(ActionEvent evt) {
        if ((String) tamanio.getSelectedItem() != "Tamaño") selTamanio.setText((String) tamanio.getSelectedItem()); else selTamanio.setText("");
        tamanio.setSelectedIndex(0);
    }

    private void fuenteActionPerformed(ActionEvent evt) {
        if ((String) fuente.getSelectedItem() != "Fuente") selFuente.setText((String) fuente.getSelectedItem()); else selFuente.setText("");
        fuente.setSelectedIndex(0);
    }

    private void colorActionPerformed(ActionEvent evt) {
        if ((String) color.getSelectedItem() == "Personalizado") {
            color.setVisible(false);
            Color bgColor = JColorChooser.showDialog(this, "Elegi color", getBackground());
            if (bgColor != null) selColor.setText("#" + toHexString(bgColor));
        } else {
            if ((String) color.getSelectedItem() != "Color") selColor.setText((String) color.getSelectedItem()); else selColor.setText("");
        }
        color.setSelectedIndex(0);
        color.setVisible(true);
    }

    public static String toHexString(Color c) {
        StringBuilder sb = new StringBuilder('#');
        if (c.getRed() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getRed()));
        if (c.getGreen() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getGreen()));
        if (c.getBlue() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getBlue()));
        return sb.toString();
    }

    private void jListImagenesValueChanged(ListSelectionEvent evt) {
        if (jListImagenes.getSelectedIndex() != -1) rellenarImagenes();
    }

    private void rellenarEstilo() {
        int seleccion;
        seleccion = jListEstilos.getSelectedIndex();
        if (seleccion > -1) {
            this.descripcion.setText(estilosTemp.get(seleccion)[0]);
            if (estilosTemp.get(seleccion)[1].equals("S")) {
                btnItalica.setSelected(true);
                i.setVisible(true);
            } else {
                btnItalica.setSelected(false);
                i.setVisible(false);
            }
            if (estilosTemp.get(seleccion)[2].equals("S")) {
                btnSub.setSelected(true);
                u.setVisible(true);
            } else {
                u.setVisible(false);
                btnSub.setSelected(false);
            }
            if (estilosTemp.get(seleccion)[3].equals("S")) {
                btnNegrita.setSelected(true);
                b.setVisible(true);
            } else {
                btnNegrita.setSelected(false);
                b.setVisible(false);
            }
            selFuente.setText(estilosTemp.get(seleccion)[4]);
            selColor.setText(estilosTemp.get(seleccion)[5]);
            selTamanio.setText(estilosTemp.get(seleccion)[6]);
            selAlineacion.setText(estilosTemp.get(seleccion)[7]);
        }
    }

    private void btnItalicaActionPerformed(ActionEvent evt) {
        if (btnItalica.isSelected()) i.setVisible(true); else i.setVisible(false);
    }

    private void btnNegritaActionPerformed(ActionEvent evt) {
        if (btnNegrita.isSelected()) b.setVisible(true); else b.setVisible(false);
    }

    private void btnSubActionPerformed(ActionEvent evt) {
        if (btnSub.isSelected()) u.setVisible(true); else u.setVisible(false);
    }

    public void setColorPersonalizado(String str) {
        selColor.setText(str);
    }

    private void borrarActionPerformed(ActionEvent evt) {
        pulsado = true;
        int pos = jListEstilos.getSelectedIndex();
        System.out.println(pos);
        if (pos > -1) {
            estilosModel.remove(pos);
            estilosTemp.remove(pos);
            resetTotal();
        }
    }

    private void resetTotal() {
        reset();
        descripcion.setText(null);
        jListEstilos.clearSelection();
    }

    private void reset() {
        selColor.setText("");
        selTamanio.setText("");
        selFuente.setText("");
        selAlineacion.setText("");
        btnItalica.setSelected(false);
        btnNegrita.setSelected(false);
        btnSub.setSelected(false);
        i.setVisible(false);
        b.setVisible(false);
        u.setVisible(false);
        alineacion.setSelectedIndex(0);
        color.setSelectedIndex(0);
        tamanio.setSelectedIndex(0);
        fuente.setSelectedIndex(0);
    }

    private void borrarAliActionPerformed(ActionEvent evt) {
        selAlineacion.setText("");
    }

    private void borrarTamActionPerformed(ActionEvent evt) {
        selTamanio.setText("");
    }

    private void borrarColorActionPerformed(ActionEvent evt) {
        selColor.setText("");
    }

    private void borrarFuenteActionPerformed(ActionEvent evt) {
        selFuente.setText("");
    }

    private void nuevoActionPerformed(ActionEvent evt) {
        resetTotal();
        descripcion.requestFocus();
    }

    private void aceptarActionPerformed(ActionEvent evt) {
        pulsado = true;
        int pos = jListEstilos.getModel().getSize();
        String[] arreglo = new String[8];
        if (!(descripcion.getText().equals(""))) {
            arreglo[0] = descripcion.getText();
            if (btnItalica.isSelected()) arreglo[1] = "S"; else arreglo[1] = "N";
            if (btnSub.isSelected()) arreglo[2] = "S"; else arreglo[2] = "N";
            if (btnNegrita.isSelected()) arreglo[3] = "S"; else arreglo[3] = "N";
            arreglo[4] = selFuente.getText();
            arreglo[5] = selColor.getText();
            arreglo[6] = selTamanio.getText();
            arreglo[7] = selAlineacion.getText();
            if (jListEstilos.getSelectedIndex() != -1) {
                pos = jListEstilos.getSelectedIndex();
                estilosTemp.remove(pos);
                estilosModel.remove(pos);
            }
            estilosModel.add(pos, descripcion.getText());
            estilosTemp.add(pos, arreglo);
            resetTotal();
        }
    }

    private void preview1ActionPerformed(ActionEvent evt) throws IOException {
        try {
            mostrarImagen(url.getText(), false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarImagen(String url, boolean perfil) throws Exception {
        URL imageurl;
        Image newimg = null;
        ImageIcon icon = null;
        BufferedImage imagen = null;
        imageurl = new URL(url);
        System.out.println("Obteniendo imagen...");
        System.out.println(url);
        imagen = ImageIO.read(imageurl);
        System.out.println("Listo");
        int alto = imagen.getHeight();
        int ancho = imagen.getWidth();
        if (ancho > 450 || alto > 179) newimg = scaleImage(imagen, BufferedImage.TYPE_INT_ARGB, 450, 179); else newimg = imagen;
        icon = new ImageIcon(newimg);
        if (!perfil) labelPrev.setIcon(icon);
    }

    private BufferedImage scaleImage(BufferedImage image, int imageType, int newWidth, int newHeight) {
        double thumbRatio = (double) newWidth / (double) newHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double aspectRatio = (double) imageWidth / (double) imageHeight;
        if (thumbRatio < aspectRatio) {
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            newWidth = (int) (newHeight * aspectRatio);
        }
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, imageType);
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, newWidth, newHeight, null);
        return newImage;
    }

    private void selectBrowserActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        int seleccion = fileChooser.showOpenDialog(jLabel1);
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File fichero = fileChooser.getSelectedFile();
            pathBrowser.setText(fichero.getPath());
        }
    }

    private void testBrowserActionPerformed(ActionEvent evt) {
        String path = "";
        path = pathBrowser.getText();
        Process miProceso;
        try {
            if (path.equals("")) JOptionPane.showMessageDialog(null, "Es necesario setear el path para la operación", "Error", JOptionPane.ERROR_MESSAGE); else miProceso = Runtime.getRuntime().exec(path);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Se produjo un error al ejecutar el navegador. Revisa el path", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setBrowser(String str) {
        pathBrowser.setText(str);
    }

    public void guardar() {
        int i = JOptionPane.showConfirmDialog(this, "No guardaste los cambios,si continuas se perderan\n ¿Salis de todas formas?", "", JOptionPane.YES_NO_OPTION);
        if (i == 0) {
            updateGUI();
            dispose();
        }
    }

    private void guardarCambios() {
        pulsado = false;
        browser = pathBrowser.getText();
        arregloOpciones[0] = updates.isSelected();
        arregloOpciones[1] = checkNumLineas.isSelected();
        pegadoIntel[0] = centrarImagenesPI.isSelected();
        pegadoIntel[1] = incluirLinkPI.isSelected();
        pegadoIntel[2] = activarInicioPI.isSelected();
        timerCont = (Integer) timer.getValue();
        System.out.println(timerCont);
        fuentesP = fuentesPTemp;
        estilos = estilosTemp;
        listaImagenes = listaImagenesTemp;
        usrPass[0] = jUsuario.getText();
        usrPass[1] = jContrasenia.getText();
        if (change) cargarComunidades();
        change = false;
        try {
            super.guardarConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.dispose();
    }

    private void guardarActionPerformed(ActionEvent evt) {
        guardarCambios();
        try {
            super.cargarConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEstilos(ArrayList<String[]> arreglo) {
        if (arreglo == null) estilosTemp = new ArrayList<String[]>(); else {
            estilosTemp = arreglo;
            array2list();
        }
    }

    private void rellenarImagenes() {
        int i = jListImagenes.getSelectedIndex();
        desc.setText(listaImagenesTemp.get(i)[0]);
        url.setText(listaImagenesTemp.get(i)[1]);
    }

    private void updateGUI() {
        if (pegadoIntel != null) {
            centrarImagenesPI.setSelected(pegadoIntel[0]);
            incluirLinkPI.setSelected(pegadoIntel[1]);
            activarInicioPI.setSelected(pegadoIntel[2]);
            if (timerCont != 0) timer.setValue(timerCont); else timer.setValue(1);
            getTimer().getEditor().setFont(new java.awt.Font("Arial", 0, 12));
        }
        pathBrowser.setText(browser);
        updates.setSelected(arregloOpciones[0]);
        checkNumLineas.setSelected(arregloOpciones[1]);
        updateListaFuentes();
        listaImagenesModel = updateListaEstilos(listaImagenesTemp);
        estilosModel = updateListaEstilos(estilosTemp);
        jUsuario.setText(getUsuario());
        jContrasenia.setText(getContrasenia());
        array2ListComu();
    }

    private void array2ListComu() {
        listaComunidadesModel = new DefaultListModel();
        for (int i = 0; i < comunidades.size(); i++) {
            listaComunidadesModel.add(i, comunidades.get(i)[1]);
        }
        listaComunidades.setModel(listaComunidadesModel);
    }

    private void array2list() {
        estilosModel = new DefaultListModel();
        if (!(estilosTemp.isEmpty())) {
            estilosModel.clear();
            for (int i = 0; i < estilosTemp.size(); i++) estilosModel.add(i, estilosTemp.get(i)[0]);
        }
        jListEstilos.setModel(estilosModel);
    }

    private DefaultListModel updateListaEstilos(ArrayList<String[]> lista) {
        DefaultListModel modelo = new DefaultListModel();
        if (!(estilosTemp.isEmpty())) {
            for (int i = 0; i < lista.size(); i++) modelo.add(i, lista.get(i)[0]);
        }
        return modelo;
    }

    private void updateListaFuentes() {
        fuentesModel = new DefaultListModel();
        if (!(fuentesPTemp.isEmpty())) {
            fuentesModel.clear();
            for (int i = 0; i < fuentesPTemp.size(); i++) fuentesModel.add(i, fuentesPTemp.get(i));
        }
        jListFuentes.setModel(fuentesModel);
    }

    @SuppressWarnings("unchecked")
    public void cargar() {
        fuentesPTemp = (ArrayList<String>) fuentesP.clone();
        estilosTemp = (ArrayList<String[]>) estilos.clone();
        listaImagenesTemp = (ArrayList<String[]>) listaImagenes.clone();
        updateGUI();
        jListEstilos.setModel(estilosModel);
        jListImagenes.setModel(listaImagenesModel);
        pulsadoCheckbox[0] = pulsadoCheckbox[1] = pulsadoCheckbox[2] = pulsadoCheckbox[3] = pulsadoCheckbox[4] = false;
    }

    private void nuevoImgActionPerformed(ActionEvent evt) {
        url.setText("");
        desc.setText("");
        jListImagenes.clearSelection();
        pulsado = true;
    }

    private void eliminarImgActionPerformed(ActionEvent evt) {
        int pos = jListImagenes.getSelectedIndex();
        if (pos > -1) {
            listaImagenesTemp.remove(pos);
            listaImagenesModel.remove(pos);
            url.setText("");
            desc.setText("");
        }
        pulsado = true;
    }

    private void guardarImgActionPerformed(ActionEvent evt) {
        int pos = jListImagenes.getModel().getSize();
        boolean reemplazo = false;
        String[] arreglo = new String[2];
        if (!(desc.getText().equals(""))) {
            arreglo[0] = desc.getText();
            arreglo[1] = url.getText();
            if (jListImagenes.getSelectedIndex() != -1) reemplazo = true;
            if (reemplazo) {
                pos = jListImagenes.getSelectedIndex();
                listaImagenesTemp.remove(pos);
                listaImagenesModel.remove(pos);
            }
            listaImagenesModel.add(pos, desc.getText());
            listaImagenesTemp.add(pos, arreglo);
            jListImagenes.clearSelection();
            url.setText("");
            desc.setText("");
        }
        pulsado = true;
    }

    private void jListEstilosValueChanged(ListSelectionEvent evt) {
        rellenarEstilo();
    }

    private void btnGuardarFuenteActionPerformed(ActionEvent evt) {
        if (fuentesPTemp.size() < 10) {
            pulsado = true;
            int pos = jListFuentes.getModel().getSize();
            if (jListFuentes.getSelectedIndex() != -1) {
                pos = jListFuentes.getSelectedIndex();
                fuentesPTemp.remove(pos);
                fuentesModel.remove(pos);
            }
            fuentesModel.add(pos, txtFuente.getText());
            fuentesPTemp.add(pos, txtFuente.getText());
            txtFuente.setText("");
        }
    }

    private void btnEliminarFuenteActionPerformed(ActionEvent evt) {
        pulsado = true;
        int pos = jListFuentes.getSelectedIndex();
        if (pos > -1) {
            fuentesModel.remove(pos);
            fuentesPTemp.remove(pos);
            txtFuente.setText("");
        }
    }

    private void btnNuevoFuenteActionPerformed(ActionEvent evt) {
        if (fuentesPTemp.size() < 10) {
            jListFuentes.clearSelection();
            txtFuente.setText("");
            txtFuente.requestFocus();
        }
    }

    private void jListFuentesValueChanged(ListSelectionEvent evt) {
        int seleccion;
        seleccion = jListFuentes.getSelectedIndex();
        if (seleccion > -1) txtFuente.setText(fuentesPTemp.get(seleccion));
    }

    private void addEscapeKey() {
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (pulsado | pulsadoCheckbox()) guardar(); else dispose();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }

    private void centrarImagenesPIActionPerformed(ActionEvent evt) {
        pulsadoCheckbox[0] = !pulsadoCheckbox[0];
    }

    private void activarInicioPIActionPerformed(ActionEvent evt) {
        pulsadoCheckbox[1] = !pulsadoCheckbox[1];
    }

    private void incluirLinkPIActionPerformed(ActionEvent evt) {
        pulsadoCheckbox[2] = !pulsadoCheckbox[2];
    }

    private void centrarImagenesCAActionPerformed(ActionEvent evt) {
        pulsadoCheckbox[3] = !pulsadoCheckbox[3];
    }

    private void incluirLinksCAActionPerformed(ActionEvent evt) {
        pulsadoCheckbox[4] = !pulsadoCheckbox[4];
    }

    private boolean pulsadoCheckbox() {
        return (pulsadoCheckbox[0] | pulsadoCheckbox[1] | pulsadoCheckbox[2] | pulsadoCheckbox[3] | pulsadoCheckbox[4]);
    }

    private void btnUnirActionPerformed(ActionEvent evt) {
        try {
            unirComunidadPostinga(main.getUserKey());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo establecer la conexión con el servidor.\n Chequea tu conexión a internet", "Ups", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void unirComunidadPostinga(String key) throws Exception {
        if (main.login(jUsuario.getText(), jContrasenia.getText())) {
            DataOutputStream out;
            String url = "http://www.taringa.net/comunidades/miembro-add.php";
            HttpURLConnection conn;
            URL siteUrl;
            main.getUserKey(getUsuario(), getContrasenia());
            String keyTemp = main.getKey();
            System.out.println("key: " + main.getKey());
            String contenido = "comid=39405&key=" + keyTemp + "&aceptar=1";
            System.out.println(contenido);
            siteUrl = new URL(url);
            conn = (HttpURLConnection) siteUrl.openConnection();
            conn.setRequestMethod("POST");
            cook.writeCookies(conn, false);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(contenido);
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            line = in.readLine();
            String[] respuesta = line.split(":");
            System.out.println(respuesta[1]);
            JOptionPane.showMessageDialog(this, respuesta[1].trim().replaceAll("<br />", ""), "Unirse a la comu PosT!NGA", JOptionPane.PLAIN_MESSAGE);
            in.close();
        }
    }

    /********************************PopUp*******************************************/
    JPopupMenu popup1;

    JMenuItem PopCortar1;

    JMenuItem PopPegar1;

    JMenuItem PopCopiar1;

    JMenuItem PopSelAll1;

    private String icon_copy = "copy.gif";

    private String icon_paste = "paste.gif";

    private String icon_cut = "cut.gif";

    private String icon_select_all = "selectall.gif";

    private JMenuItem getPopCortar1() {
        if (PopCortar1 == null) {
            PopCortar1 = new JMenuItem("Cortar");
            PopCortar1.setIcon(new ImageIcon(getClass().getResource("cut.gif")));
            PopCortar1.setFont(new java.awt.Font("Ubuntu", 0, 12));
            PopCortar1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    PopCortar1ActionPerformed(evt);
                }
            });
        }
        return PopCortar1;
    }

    private JMenuItem getPopCopiar1() {
        if (PopCopiar1 == null) {
            PopCopiar1 = new JMenuItem("Copiar");
            PopCopiar1.setFont(new java.awt.Font("Ubuntu", 0, 12));
            PopCopiar1.setIcon(new ImageIcon(getClass().getResource("copy.gif")));
            PopCopiar1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    PopCopiar1ActionPerformed(evt);
                }
            });
        }
        return PopCopiar1;
    }

    private JMenuItem getPopPegar1() {
        if (PopPegar1 == null) {
            PopPegar1 = new JMenuItem("Pegar");
            PopPegar1.setIcon(new ImageIcon(getClass().getResource("paste.gif")));
            PopPegar1.setFont(new java.awt.Font("Ubuntu", 0, 12));
            PopPegar1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    PopPegar1ActionPerformed(evt);
                }
            });
        }
        return PopPegar1;
    }

    private JMenuItem getPopSelAll1() {
        if (PopSelAll1 == null) {
            PopSelAll1 = new JMenuItem("Seleccionar todo");
            PopSelAll1.setIcon(new ImageIcon(getClass().getResource(icon_select_all)));
            PopSelAll1.setFont(new java.awt.Font("Ubuntu", 0, 12));
            PopSelAll1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    PopSelAll1ActionPerformed(evt);
                }
            });
        }
        return PopSelAll1;
    }

    private void PopPegar1ActionPerformed(ActionEvent evt) {
        if (popup1.getInvoker().getName() == "desc") {
            desc.paste();
            desc.requestFocus();
        }
        if (popup1.getInvoker().getName() == "pathBrowser") {
            pathBrowser.paste();
            pathBrowser.requestFocus();
        }
        if (popup1.getInvoker().getName() == "url") {
            url.paste();
            url.requestFocus();
        }
    }

    private void PopCopiar1ActionPerformed(ActionEvent evt) {
        if (popup1.getInvoker().getName() == "url") url.copy();
        if (popup1.getInvoker().getName() == "pathBrowser") pathBrowser.copy();
        if (popup1.getInvoker().getName() == "desc") desc.copy();
    }

    private void PopSelAll1ActionPerformed(ActionEvent evt) {
        if (popup1.getInvoker().getName() == "url") {
            url.selectAll();
            url.requestFocus();
        }
        if (popup1.getInvoker().getName() == "pathBrowser") {
            pathBrowser.selectAll();
            pathBrowser.requestFocus();
        }
        if (popup1.getInvoker().getName() == "desc") {
            desc.selectAll();
            desc.requestFocus();
        }
    }

    private void PopCortar1ActionPerformed(ActionEvent evt) {
        if (popup1.getInvoker().getName() == "desc") {
            desc.cut();
            desc.requestFocus();
        }
        if (popup1.getInvoker().getName() == "pathBrowser") {
            pathBrowser.cut();
            pathBrowser.requestFocus();
        }
        if (popup1.getInvoker().getName() == "url") {
            url.cut();
            url.requestFocus();
        }
    }

    private JPopupMenu getPopup1() {
        if (popup1 == null) {
            popup1 = new JPopupMenu();
            url.setComponentPopupMenu(popup1);
            desc.setComponentPopupMenu(popup1);
            pathBrowser.setComponentPopupMenu(popup1);
            popup1.add(getPopCortar1());
            popup1.add(getPopCopiar1());
            popup1.add(getPopPegar1());
            popup1.add(getPopSelAll1());
        }
        return popup1;
    }

    private JScrollPane getJScrollPane4() {
        if (jScrollPane4 == null) {
            jScrollPane4 = new JScrollPane();
            jScrollPane4.setViewportView(getListaComunidades());
        }
        return jScrollPane4;
    }

    private JList getListaComunidades() {
        if (listaComunidades == null) {
            listaComunidades = new JList();
            listaComunidades.setFont(new java.awt.Font("Arial", 0, 12));
        }
        return listaComunidades;
    }

    private JButton getBtnActualizar() {
        if (btnActualizar == null) {
            btnActualizar = new JButton();
            btnActualizar.setText("Actualizar");
            btnActualizar.setFont(new java.awt.Font("Arial", 0, 12));
            btnActualizar.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    btnActualizarActionPerformed(evt);
                }
            });
        }
        return btnActualizar;
    }

    /********************************PopUp*******************************************/
    private String[] refinarResultado(String str) {
        String[] resultado;
        resultado = str.split("<div class=\"listado-avatar\">");
        for (int i = 1; i < resultado.length; i++) {
            resultado[i] = resultado[i].trim().replaceAll("<a href=\"/", "");
            resultado[i] = resultado[i].substring(0, resultado[i].indexOf('"') - 1).replaceAll("comunidades/", "");
        }
        return resultado;
    }

    private ArrayList<String[]> obtenerComunidades(String usuario) {
        ArrayList<String[]> value = new ArrayList<String[]>();
        String nextLine, comu = "";
        URL url = null;
        URLConnection urlConn = null;
        InputStreamReader inStream = null;
        BufferedReader buff = null;
        try {
            url = new URL("http://www.taringa.net/" + usuario + "/comunidades");
            System.out.println("obteniendo comunidades de " + usuario);
            urlConn = url.openConnection();
            inStream = new InputStreamReader(urlConn.getInputStream());
            buff = new BufferedReader(inStream);
            while (true) {
                nextLine = buff.readLine();
                if (nextLine != null) {
                    if (nextLine.contains("<div class=\"listado-content clearfix list-element\">")) {
                        while (!nextLine.contains("<div id=\"sidebar\">")) {
                            comu = comu + nextLine;
                            nextLine = buff.readLine();
                        }
                        break;
                    }
                } else {
                    break;
                }
            }
            value = refinar(comu);
        } catch (MalformedURLException e) {
            System.out.println("Please check the URL:" + e.toString());
        } catch (IOException e1) {
            System.out.println("Can't read  from the Internet: " + e1.toString());
        }
        return value;
    }

    public ArrayList<String[]> refinar(String url) throws MalformedURLException, IOException {
        ArrayList<String[]> value = new ArrayList<String[]>();
        MicrosoftConditionalCommentTagTypes.register();
        PHPTagTypes.register();
        PHPTagTypes.PHP_SHORT.deregister();
        MasonTagTypes.register();
        Source source = new Source(url);
        List<Element> linkElements = source.getAllElements(HTMLElementName.A);
        for (Element linkElement : linkElements) {
            String href = linkElement.getAttributeValue("href");
            if (href == null) continue;
            String label = linkElement.getContent().getTextExtractor().toString();
            if (!label.equals("")) {
                String b[] = new String[3];
                b[1] = label;
                b[0] = href;
                value.add(b);
            }
        }
        return value;
    }

    private ArrayList obtenerComuIds(ArrayList<String[]> arreglo) {
        for (int a = 0; a < arreglo.size(); a++) try {
            arreglo.get(a)[2] = obtenerComuId(arreglo.get(a)[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return arreglo;
    }

    private String obtenerComuId(String com) throws IOException {
        String nextLine = "";
        URL url = null;
        URLConnection urlConn = null;
        InputStreamReader inStream = null;
        BufferedReader buff = null;
        url = new URL("http://www.taringa.net/" + this.getUsuario() + "com");
        System.out.println("http://www.taringa.net/" + this.getUsuario() + "com");
        System.out.println("obteniendo ID de las comunidades de " + this.getUsuario());
        urlConn = url.openConnection();
        inStream = new InputStreamReader(urlConn.getInputStream());
        buff = new BufferedReader(inStream);
        while (true) {
            nextLine = buff.readLine();
            if (nextLine != null) {
                System.out.println(nextLine);
                if (nextLine.contains("comid:")) {
                }
            } else System.out.println("nulo");
        }
    }

    private void btnActualizarActionPerformed(ActionEvent evt) {
        cargarComunidades();
    }

    public void cargarComunidades() {
        comunidades = (ArrayList<String[]>) obtenerComunidades(jUsuario.getText()).clone();
        array2ListComu();
    }

    private JLabel getJLabel6() {
        if (jLabel6 == null) {
            jLabel6 = new JLabel();
            jLabel6.setText("Mis comunidades");
            jLabel6.setFont(new java.awt.Font("Arial", 1, 14));
        }
        return jLabel6;
    }

    private void jContraseniaFocusGained(FocusEvent evt) {
        jContrasenia.selectAll();
    }

    private void jContraseniaFocusLost(FocusEvent evt) {
        jContrasenia.select(0, 0);
    }

    private JLabel getJLabel7() {
        if (jLabel7 == null) {
            jLabel7 = new JLabel();
            jLabel7.setText("  Autoguardar cada (min)");
            jLabel7.setFont(new java.awt.Font("Arial", 0, 12));
        }
        return jLabel7;
    }

    private JSpinner getTimer() {
        if (timer == null) {
            SpinnerModel sm = new SpinnerNumberModel(1, 1, 60, 1);
            timer = new JSpinner(sm);
            timer.setFont(new java.awt.Font("Arial", 0, 12));
        }
        return timer;
    }

    private void jUsuarioKeyPressed(KeyEvent evt) {
        change = true;
        System.out.println("Cambios");
    }

    private JCheckBox getCheckNumLineas() {
        if (checkNumLineas == null) {
            checkNumLineas = new JCheckBox();
            checkNumLineas.setText("Mostrar nº de lineas en el editor (es necesario reiniciar)");
            checkNumLineas.setFont(new java.awt.Font("Arial", 0, 12));
        }
        return checkNumLineas;
    }

    private JLabel getJLabel9() {
        if (jLabel9 == null) {
            jLabel9 = new JLabel();
            jLabel9.setText("El nombre de usuario es sensible a las mayusculas");
            jLabel9.setFont(new java.awt.Font("Arial", 0, 10));
        }
        return jLabel9;
    }

    class WindowEventHandler extends WindowAdapter {

        public void windowClosing(WindowEvent evt) {
            if (pulsado | pulsadoCheckbox()) guardar(); else dispose();
        }
    }
}
