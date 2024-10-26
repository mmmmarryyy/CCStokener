package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import model.BCFisierText;
import model.BazaCunostinte;
import model.Regula;
import controller.MasinaForwardChaining;
import controller.MasinaInferenta;

public class AppletUI extends JApplet implements InterfataUtilizator {

    /**
	 * 
	 */
    private static final long serialVersionUID = 8391158701369902868L;

    private BazaCunostinte bazaCunostinte;

    private MasinaInferenta inferenta;

    private Collection<Regula> reguli;

    private List<Intrebare> intrebari;

    private int indexIntrebareCurenta;

    private JButton urm;

    private JLabel titlu;

    private JCheckBox[] variante = new JCheckBox[4];

    private JPanel southPanel;

    private Collection<String> premise = new ArrayList<String>();

    private ImagePanel panelCentral;

    public void init() {
        bazaCunostinte = new BCFisierText(getCodeBase());
        try {
            intrebari = citesteIntrebari();
            inferenta = new MasinaForwardChaining(bazaCunostinte);
        } catch (IOException e) {
            System.out.println("EROARE BAZA DE CUNOSTINTE!");
            e.printStackTrace();
            System.exit(1);
        }
        try {
            reguli = bazaCunostinte.citesteReguli();
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createGUI();
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    ;

    private List<Intrebare> citesteIntrebari() throws IOException {
        ArrayList<Intrebare> intrebari = new ArrayList<Intrebare>();
        try {
            URL url = new URL(getCodeBase(), "../intrebari.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader((url.openStream())));
            String intrebare;
            while ((intrebare = reader.readLine()) != null) {
                Collection<String> raspunsuri = new ArrayList<String>();
                Collection<String> predicate = new ArrayList<String>();
                String raspuns = "";
                while (!"".equals(raspuns = reader.readLine())) {
                    raspunsuri.add(raspuns);
                    predicate.add(reader.readLine());
                }
                Intrebare i = new Intrebare(intrebare, raspunsuri.toArray(new String[raspunsuri.size()]), predicate.toArray(new String[predicate.size()]));
                intrebari.add(i);
            }
        } catch (ArgumentExcetpion e) {
            e.printStackTrace();
        }
        return intrebari;
    }

    private void createGUI() {
        setSize(new Dimension(500, 400));
        setLayout(new BorderLayout());
        String reguliAsString = "In baza de cunostinta sunt " + reguli.size() + " reguli:\n";
        for (Regula regula : reguli) reguliAsString += regula + "\n";
        titlu = new JLabel("");
        titlu.setHorizontalAlignment(JLabel.CENTER);
        getContentPane().add(titlu, BorderLayout.NORTH);
        panelCentral = new ImagePanel();
        panelCentral.setLayout(new GridLayout(2, 2));
        for (int i = 0; i < 4; i++) {
            variante[i] = new JCheckBox("Test");
            variante[i].setHorizontalAlignment(JCheckBox.CENTER);
            panelCentral.add(variante[i], BorderLayout.CENTER);
        }
        getContentPane().add(panelCentral, BorderLayout.CENTER);
        southPanel = new JPanel();
        urm = new JButton("Urmatoare");
        urm.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                actualizarePremise();
                indexIntrebareCurenta++;
                afiseazaIntrebarea();
            }
        });
        urm.setPreferredSize(new Dimension(100, 20));
        southPanel.add(urm);
        getContentPane().add(southPanel, BorderLayout.SOUTH);
        afiseazaIntrebarea();
    }

    private void actualizarePremise() {
        for (int i = 0; i < variante.length; i++) if (variante[i].isVisible() && variante[i].isSelected()) {
            String premisa = intrebari.get(indexIntrebareCurenta).predicate[i];
            if (!premise.contains(premisa)) premise.add(premisa);
        }
    }

    public void afiseazaIntrebarea() {
        if (indexIntrebareCurenta == intrebari.size() - 1) {
            southPanel.removeAll();
            final JButton finish = new JButton("Surprinde-ma!");
            finish.setPreferredSize(new Dimension(150, 20));
            finish.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    actualizarePremise();
                    finish.setVisible(false);
                    southPanel.removeAll();
                    panelCentral.removeAll();
                    String solutie = inferenta.cautaSolutie(premise);
                    if (solutie == null) {
                        titlu.setText("Ne pare rau, dar nu am putut trage o concluzie din datele culese!");
                        try {
                            URL url = new URL(getCodeBase(), "../poze/sad_face.jpg");
                            panelCentral.setImage(url);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    } else {
                        titlu.setText("Locul unde ar trebui sa mergi este: " + solutie);
                        try {
                            URL url = new URL(getCodeBase(), "../poze/" + solutie + ".jpg");
                            panelCentral.setImage(url);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            southPanel.add(finish);
        }
        titlu.setText(intrebari.get(indexIntrebareCurenta).intrebare);
        int i = 0;
        for (String rasp : intrebari.get(indexIntrebareCurenta).raspunsuri) {
            variante[i].setVisible(true);
            variante[i].setSelected(false);
            variante[i++].setText(rasp);
        }
        for (; i < variante.length; i++) {
            variante[i].setVisible(false);
        }
        this.repaint();
    }
}
