package actualizacion;

import gui.BrowserLauncher;
import gui.Principal;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ChkVersion extends JInternalFrame implements Runnable, IMostrarMensaje {

    private static final long serialVersionUID = 5312929606165125506L;

    JLabel lblEstado;

    JTextArea txtTexto;

    StringBuffer texto;

    JButton btnActualizar;

    JButton btnReintentar;

    private ChkVersion instancia;

    public ChkVersion() {
        super("Buscar Actualizaciones");
        instancia = this;
        setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
        setResizable(true);
        setMaximizable(true);
        setClosable(true);
        btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    BrowserLauncher.openURL("http://apeiron.sourceforge.net");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btnReintentar = new JButton("Obtener Informaci�n");
        btnReintentar.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                empezar();
            }
        });
        btnActualizar.setEnabled(false);
        lblEstado = new JLabel("");
        texto = new StringBuffer();
        txtTexto = new JTextArea();
        txtTexto.setEditable(false);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(btnReintentar);
        panel.add(btnActualizar);
        getContentPane().add(panel, "North");
        getContentPane().add(new JScrollPane(txtTexto));
        getContentPane().add(lblEstado, "South");
        setSize(380, 200);
        empezar();
    }

    public void mostrar(String msg) {
        lblEstado.setText(msg);
    }

    protected void empezar() {
        new Thread(this).start();
    }

    protected synchronized void escribir(String s) {
        texto.append(s);
        txtTexto.setText(texto + "");
        txtTexto.setSelectionStart(texto.length());
    }

    public void run() {
        btnReintentar.setEnabled(false);
        try {
            lblEstado.setText("Conectando con servidor...");
            escribir("\nConectando con servidor...");
            URL url = new URL("http://apeiron.sourceforge.net/version.php");
            lblEstado.setText("Obteniendo informaci�n de versi�n...");
            escribir("Ok\n");
            escribir("Obteniendo informaci�n sobre �ltima versi�n...");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String linea = br.readLine();
            escribir("Ok\n");
            if (linea != null) {
                escribir("Versi�n mas reciente: " + linea + "\n");
                if (Principal.version < Double.parseDouble(linea)) {
                    lblEstado.setText("Hay una nueva versi�n: Apeiron " + linea);
                    escribir("Puede obtener la actualizaci�n de: http://apeiron.sourceforge.net\n");
                    btnActualizar.setEnabled(true);
                    setVisible(true);
                } else {
                    lblEstado.setText("Usted tiene la �ltima versi�n");
                }
            }
            br.close();
        } catch (MalformedURLException e) {
            escribir("Fall�\n" + e + "\n");
            e.printStackTrace();
        } catch (IOException e) {
            escribir("Fall�\n" + e + "\n");
            e.printStackTrace();
        }
        btnReintentar.setEnabled(true);
    }
}
