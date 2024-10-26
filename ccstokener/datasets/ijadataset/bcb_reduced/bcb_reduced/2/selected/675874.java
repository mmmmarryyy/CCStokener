package org.joshy.countdown;

import org.glossitope.desklet.Desklet;
import org.glossitope.desklet.DeskletContext;
import org.glossitope.desklet.test.DeskletTester;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.ImagePainter;
import org.joda.time.*;
import java.net.URL;
import java.awt.*;
import org.joshy.util.u;

public class CountdownMain extends Desklet {

    JLabel daysleft;

    DateTime target;

    JLabel miniDaysLeft;

    JXPanel panel;

    public void init() throws Exception {
        this.panel = new JXPanel();
        DeskletContext context = getContext();
        context.getContainer().setBackgroundDraggable(true);
        context.getContainer().setShaped(true);
        context.getContainer().setResizable(false);
        BufferedImage img = ImageIO.read(getClass().getResource("/xmas.png"));
        panel.setBackgroundPainter(new ImagePainter(img));
        panel.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
        panel.setLayout(null);
        createComponents();
        createLayout();
        createEvents();
        context.getContainer().setContent(panel);
        context.getDockingContainer().setContent(miniDaysLeft);
        context.getContainer().setVisible(true);
    }

    private void createComponents() {
        int tracking = 26;
        int digits = 3;
        daysleft = new AALabel("888", tracking, digits);
        int year = 2007;
        int month = 12;
        int day = 25;
        target = new DateTime(year, month, day, 1, 0, 0, 0);
        miniDaysLeft = new JLabel("Xmas: 360 days");
    }

    private void createLayout() throws FontFormatException, IOException {
        int dx = 23;
        int dy = 105;
        daysleft.setLocation(dx, dy);
        int font_size = 85;
        String font_href = "none";
        String font_jar = "none";
        String font_name = "none";
        Font font = null;
        try {
            if (!font_jar.equals("none")) {
                u.p("font jar = " + font_jar);
                URL font_url = getClass().getResource("/fonts/" + font_jar);
                u.p("font url = " + font_url);
                font = Font.createFont(Font.TRUETYPE_FONT, font_url.openStream());
            } else if (!font_href.equals("none")) {
                u.p("font url = " + font_href);
                URL font_url = new URL(font_href);
                font = Font.createFont(Font.TRUETYPE_FONT, font_url.openStream());
            } else {
                font = new Font(font_name, Font.PLAIN, font_size);
            }
        } catch (Exception ex) {
            u.p("ex: " + ex);
            font = new Font("Arial", Font.PLAIN, font_size);
        }
        font = font.deriveFont((float) font_size);
        daysleft.setFont(font);
        daysleft.setPreferredSize(new Dimension(250, 200));
        daysleft.setSize(250, 200);
        panel.add(daysleft);
    }

    private void createEvents() {
        new Thread(new CountdownUpdater(this)).start();
    }

    public void updateTime() {
        DateTime now = new DateTime();
        Days days = Days.daysBetween(now, target);
        daysleft.setText("" + days.getDays());
        miniDaysLeft.setText("Christmas: " + days.getDays());
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
        getContext().notifyStopped();
    }

    public void destroy() throws Exception {
    }

    public static void main(String[] args) {
        DeskletTester.start(CountdownMain.class);
    }
}
