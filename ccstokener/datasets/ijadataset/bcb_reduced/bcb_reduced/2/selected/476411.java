package no.klikkespillet.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import no.klikkespillet.core.Timer;
import no.klikkespillet.lang.Language;
import no.klikkespillet.util.BrowserControl;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = -5025598017239257131L;

    private int currentTime;

    private int currentClicks;

    private boolean started = false;

    private int maxTime = 30;

    private JLabel timerLabel = new JLabel("Tid igjen" + ": 30");

    private JLabel counterLabel = new JLabel("Antall klikk: 0");

    private JButton clickButton = new JButton("Klikk!!");

    private Language lang;

    private Timer t = new Timer(this);

    protected Thread thread = new Thread(t);

    public MainWindow(Language l) {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        clickButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    currentClicks++;
                    updateClicks();
                    if (!started) {
                        started = true;
                        currentClicks = 0;
                        currentTime = maxTime;
                        updateTime();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }
        });
        this.setLayout(new BorderLayout());
        this.add(timerLabel, BorderLayout.NORTH);
        this.add(counterLabel, BorderLayout.SOUTH);
        this.add(clickButton, BorderLayout.CENTER);
        super.setSize(300, 300);
        super.setTitle("Klikkespillet");
        lang = l;
        thread.start();
    }

    public void timeEvent() {
        if (started) updateTime();
        if (started && currentTime == 0) {
            started = false;
            finishEvent();
        }
    }

    private void finishEvent() {
        saveScore(currentClicks);
        JOptionPane.showMessageDialog(this, "Du klarte " + currentClicks + " klikk! Bravo!!");
    }

    private void saveScore(int score) {
        String name = JOptionPane.showInputDialog(this, "Skriv navn for å komme på highscorelisten!", "Lagre score!", JOptionPane.INFORMATION_MESSAGE);
        URL url;
        try {
            url = new URL("http://129.177.17.51:8080/GuestBook/TheOnlyServlet?name=" + name + "&score=" + score);
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();
            urlConnection.getInputStream();
            BrowserControl.openUrl("http://129.177.17.51:8080/GuestBook/TheOnlyServlet");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateTime() {
        currentTime--;
        timerLabel.setText(lang.getLocal(0) + ": " + currentTime);
    }

    private void updateClicks() {
        counterLabel.setText(lang.getLocal(1) + ": " + currentClicks);
    }
}
