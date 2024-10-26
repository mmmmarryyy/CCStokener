package quebralink;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author ranishot
 */
public class UpdateCheck {

    private String link;

    private String versao = GUIQuebraLink.VERSAO;

    private JDialog update;

    public static boolean checkVersion(String link, String vers) throws IOException {
        try {
            link = Encoder.EncodeHex(link);
            String tmp = "";
            URL url = new URL("http://rbmsoft.com.br/apis/ql/index.php?url=" + link + "&versao=" + vers);
            BufferedInputStream buf = new BufferedInputStream(url.openStream());
            int dado = 0;
            char letra;
            while ((dado = buf.read()) != -1) {
                letra = (char) dado;
                tmp += letra;
            }
            if (tmp.contains("FALSE")) {
                return false;
            } else if (tmp.contains("TRUE")) {
                new UpdateCheck().updateDialog();
                return true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkVersion(String vers) throws IOException {
        try {
            String tmp = "";
            URL url = new URL("http://rbmsoft.com.br/apis/ql/index.php?url=null&versao=" + vers);
            BufferedInputStream buf = new BufferedInputStream(url.openStream());
            int dado = 0;
            char letra;
            while ((dado = buf.read()) != -1) {
                letra = (char) dado;
                tmp += letra;
            }
            if (tmp.contains("FALSE")) {
                return false;
            } else if (tmp.contains("TRUE")) {
                new UpdateCheck().updateDialog();
                return true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void updateDialog() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        update = new JDialog();
        update.setLayout(new BorderLayout(2, 3));
        JLabel dialogQuestion = new JLabel("<html><h3>Existe uma versão mais recente do quebralink, deseja baixa-lá agora?</html>");
        ImageIcon a = new ImageIcon(getClass().getResource("alerta.png"));
        dialogQuestion.setIcon(a);
        JPanel panelButtons = new JPanel(new GridLayout(1, 2, 2, 2));
        JButton updat_yes = new JButton("Sim");
        JButton updat_no = new JButton("Não");
        updat_yes.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                GUIQuebraLink.irURL("http://code.google.com/p/quebra-link/downloads/list");
                update.dispose();
            }
        });
        update.getRootPane().setDefaultButton(updat_yes);
        updat_no.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                update.dispose();
            }
        });
        panelButtons.add(updat_yes);
        panelButtons.add(updat_no);
        update.add(dialogQuestion, BorderLayout.NORTH);
        update.add(panelButtons, BorderLayout.SOUTH);
        update.setSize(350, 160);
        update.setVisible(true);
        update.setModal(true);
        update.setLocationRelativeTo(null);
        update.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
