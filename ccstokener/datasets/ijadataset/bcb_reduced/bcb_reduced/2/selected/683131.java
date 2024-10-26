package br.ufrj.nce.linkit;

import java.awt.Container;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class EscolhedorDeArquivos extends JFrame implements ActionListener {

    public static JTextField texto;

    public static JButton botao;

    public static JPanel painel;

    public static JComboBox combo;

    public static String nomeArq;

    public static int status;

    public static Container cont;

    public static JFrame eu;

    public static JApplet appletex;

    public static JList teste;

    public static JScrollPane barra;

    public static JLabel rotulo;

    public static JButton cancel;

    public EscolhedorDeArquivos(JApplet japarent, int stat) throws HeadlessException {
        setBounds(390, 200, 230, 300);
        setResizable(false);
        requestFocus(true);
        PainelModelos.pn.setFocusable(false);
        status = stat;
        appletex = japarent;
        eu = this;
        painel = new JPanel();
        String[] todosSemNenhum = listArquivo().split("\\n");
        String[] todosArquiv = new String[todosSemNenhum.length + 1];
        todosArquiv[0] = "                                                  ";
        for (int i = 0; i < todosSemNenhum.length; i++) {
            todosArquiv[i + 1] = todosSemNenhum[i];
        }
        texto = new JTextField(15);
        rotulo = new JLabel("Nome do Arquivo:");
        teste = new JList(todosArquiv);
        JScrollPane barra = new JScrollPane(teste);
        ListSelectionListener conteudoLista = new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (teste.getSelectedIndex() == 0) {
                    texto.setText("");
                } else {
                    texto.setText((String) teste.getSelectedValue());
                }
            }
        };
        teste.addListSelectionListener(conteudoLista);
        botao = new JButton("OK");
        cancel = new JButton("Cancelar");
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PainelModelos.pn.setFocusable(true);
                eu.dispose();
            }
        };
        cancel.addActionListener(al);
        texto.addActionListener((ActionListener) japarent);
        botao.addActionListener((ActionListener) japarent);
        botao.addActionListener(al);
        painel.add(barra);
        painel.add(rotulo);
        painel.add(texto);
        painel.add(botao);
        painel.add(cancel);
        cont = getContentPane();
        cont.add(painel);
        if ((status == 1) || (status == 3) || (status == 4) || (status == 5)) {
            setTitle("Salvar");
            teste.disable();
        }
        if (status == 0) {
            setTitle("Abrir");
        }
        if (status == 2) {
            setTitle("Salvar Como");
        }
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                PainelModelos.pn.setFocusable(true);
            }
        });
    }

    public static void main(String[] args) {
        EscolhedorDeArquivos eda = new EscolhedorDeArquivos(appletex, 0);
        eda.setVisible(true);
    }

    private String listArquivo() {
        String arquivo = "";
        String linha = "";
        try {
            URL url = new URL(appletex.getCodeBase(), "./listador?dir=" + "arquivos/" + JLinkitFrame.user);
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

    public void actionPerformed(ActionEvent arg0) {
    }
}
