package org.somprocessing.viz;

import java.awt.*;
import java.applet.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

/** 
 * This is a Java Applet to read data from the directories:
 * ./images/"username".png
 * ./data/"username_labeled_codebook".png
 * 
 * @author Cedric Gabathuler
 * @version 0.2
 */
@SuppressWarnings("serial")
public class JAviewer extends Applet implements Runnable {

    private int width = 640;

    private int height = 400;

    private Image umat;

    private URL base;

    private MediaTracker mt;

    private String username;

    private int xunit;

    private int yunit;

    private String topol = "";

    private int[][] xposit;

    private int[][] yposit;

    private String[][] labels;

    private Thread thisThread;

    private int i;

    private String updateLoc;

    private boolean updateL = false;

    private boolean alllabel = false;

    private boolean mouseclicked = false;

    private int mouseX, mouseY;

    /** 
	 * Method initializes the applet and reads image (png) and codebook file.
	 */
    public void init() {
        updateLoc = "none";
        mt = new MediaTracker(this);
        thisThread = new Thread(this);
        i = 0;
        thisThread.start();
        try {
            base = getDocumentBase();
            username = getParameter("username");
        } catch (Exception e) {
        }
        String userpng = "images/" + username + ".png";
        String userdat = "data/" + username + "_l.cod";
        URL url = null;
        try {
            url = new URL(base, userdat);
        } catch (MalformedURLException e1) {
        }
        InputStream in = null;
        try {
            in = url.openStream();
        } catch (IOException e1) {
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
        } catch (Exception r) {
        }
        try {
            String line = reader.readLine();
            StringTokenizer tokenizer = new StringTokenizer(line, " ");
            int dim = Integer.parseInt(tokenizer.nextToken().trim().toLowerCase());
            this.topol = tokenizer.nextToken().trim().toLowerCase();
            xunit = Integer.parseInt(tokenizer.nextToken().trim().toLowerCase());
            yunit = Integer.parseInt(tokenizer.nextToken().trim().toLowerCase());
            @SuppressWarnings("unused") String neigh = tokenizer.nextToken().trim().toLowerCase();
            String label = null;
            labels = new String[xunit][yunit];
            for (int e = 0; e < yunit; e++) {
                for (int r = 0; r < xunit; r++) {
                    line = reader.readLine();
                    StringTokenizer tokenizer2 = new StringTokenizer(line, " ");
                    for (int w = 0; w < dim; w++) {
                        if (tokenizer2.countTokens() > 0) tokenizer2.nextToken();
                    }
                    while (tokenizer2.countTokens() > 0) {
                        label = tokenizer2.nextToken() + " ";
                    }
                    if (label == null) {
                        labels[r][e] = "none";
                    } else {
                        labels[r][e] = label;
                    }
                    label = null;
                }
            }
            reader.close();
            if (topol.equals("hexa")) {
                xposit = new int[xunit][yunit];
                yposit = new int[xunit][yunit];
                double divisor1 = xunit;
                double divisor2 = yunit;
                for (int p = 0; p < xunit; p++) {
                    for (int q = 0; q < yunit; q++) {
                        if (q % 2 == 0) {
                            double nenner = (p * width);
                            xposit[p][q] = (int) Math.round(nenner / divisor1);
                        }
                        if (q % 2 != 0) {
                            double nenner = (width * 0.5) + (p * width);
                            xposit[p][q] = (int) Math.round(nenner / divisor1);
                        }
                        yposit[p][q] = (int) Math.round(((height * 0.5) + q * height) / divisor2);
                    }
                }
            }
            if (topol.equals("rect")) {
                xposit = new int[xunit][yunit];
                yposit = new int[xunit][yunit];
                double divisor1 = xunit;
                double divisor2 = yunit;
                for (int p = 0; p < xunit; p++) {
                    for (int q = 0; q < yunit; q++) {
                        double nenner = (width * 0.5) + (p * width);
                        xposit[p][q] = (int) Math.round((nenner / divisor1));
                        yposit[p][q] = (int) Math.round(((height * 0.5) + q * height) / divisor2);
                    }
                }
            }
        } catch (IOException o) {
        }
        umat = getImage(base, userpng);
        mt.addImage(umat, 1);
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
        }
        addMouseListener(new CircleInfo());
    }

    /** 
	 * Method initializes thr run state of an applet and executes thread
	 */
    @SuppressWarnings("static-access")
    public void run() {
        while (true) {
            try {
                thisThread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.print("no connection");
            }
            ++i;
            umat.flush();
            umat = null;
            try {
                base = getDocumentBase();
                username = getParameter("username");
            } catch (Exception e) {
            }
            String userdat = "data/" + username + "_l.cod";
            URL url = null;
            try {
                url = new URL(base, userdat);
            } catch (MalformedURLException e1) {
            }
            InputStream in = null;
            try {
                in = url.openStream();
            } catch (IOException e1) {
            }
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(in));
            } catch (Exception r) {
            }
            try {
                String line = reader.readLine();
                StringTokenizer tokenizer = new StringTokenizer(line, " ");
                int dim = Integer.parseInt(tokenizer.nextToken().trim().toLowerCase());
                this.topol = tokenizer.nextToken().trim().toLowerCase();
                xunit = Integer.parseInt(tokenizer.nextToken().trim().toLowerCase());
                yunit = Integer.parseInt(tokenizer.nextToken().trim().toLowerCase());
                @SuppressWarnings("unused") String neigh = tokenizer.nextToken().trim().toLowerCase();
                String label = null;
                labels = new String[xunit][yunit];
                for (int e = 0; e < yunit; e++) {
                    for (int r = 0; r < xunit; r++) {
                        line = reader.readLine();
                        StringTokenizer tokenizer2 = new StringTokenizer(line, " ");
                        for (int w = 0; w < dim; w++) {
                            if (tokenizer2.countTokens() > 0) tokenizer2.nextToken();
                        }
                        while (tokenizer2.countTokens() > 0) {
                            label = tokenizer2.nextToken() + " ";
                        }
                        if (label == null) {
                            labels[r][e] = "none";
                        } else {
                            labels[r][e] = label;
                        }
                        label = null;
                    }
                }
                reader.close();
                if (topol.equals("hexa")) {
                    xposit = new int[xunit][yunit];
                    yposit = new int[xunit][yunit];
                    double divisor1 = xunit;
                    double divisor2 = yunit;
                    for (int p = 0; p < xunit; p++) {
                        for (int q = 0; q < yunit; q++) {
                            if (q % 2 == 0) {
                                double nenner = (p * width);
                                xposit[p][q] = (int) Math.round(nenner / divisor1);
                            }
                            if (q % 2 != 0) {
                                double nenner = (width * 0.5) + (p * width);
                                xposit[p][q] = (int) Math.round(nenner / divisor1);
                            }
                            yposit[p][q] = (int) Math.round(((height * 0.5) + q * height) / divisor2);
                        }
                    }
                }
                if (topol.equals("rect")) {
                    xposit = new int[xunit][yunit];
                    yposit = new int[xunit][yunit];
                    double divisor1 = xunit;
                    double divisor2 = yunit;
                    for (int p = 0; p < xunit; p++) {
                        for (int q = 0; q < yunit; q++) {
                            double nenner = (width * 0.5) + (p * width);
                            xposit[p][q] = (int) Math.round((nenner / divisor1));
                            yposit[p][q] = (int) Math.round(((height * 0.5) + q * height) / divisor2);
                        }
                    }
                }
            } catch (IOException o) {
            }
            String userpng = "images/" + username + ".png";
            mt.removeImage(umat);
            umat = getImage(base, userpng);
            mt.addImage(umat, 0);
            try {
                mt.waitForID(0);
            } catch (InterruptedException i) {
                showStatus("Interrupted");
            }
            repaint();
        }
    }

    /** 
	 * Paint method runs after initialization or after a the thread or action updated 
	 * the input data.
	 * 
	 * @param g Graphics 
	 */
    public void paint(Graphics g) {
        g.drawImage(umat, 0, 0, width, height, this);
        Font f = new Font("Verdana", Font.BOLD, 12);
        g.setFont(f);
        for (int p = 0; p < xunit; p++) {
            for (int q = 0; q < yunit; q++) {
                Color white = new Color(255, 255, 255, 30);
                Color red = new Color(255, 40, 40, 120);
                Color black = new Color(0, 0, 0, 30);
                g.setColor(black);
                g.fillOval(xposit[p][q], yposit[p][q], 6, 6);
                String testl = labels[p][q];
                if (testl.equals("none")) g.setColor(white); else {
                    g.setColor(red);
                }
                g.fillOval(xposit[p][q] + 1, yposit[p][q] + 1, 4, 4);
            }
        }
        if (updateL) {
            int xaxis = 1;
            int yaxis = 1;
            for (int s = 0; s < xunit; s++) {
                for (int d = 0; d < yunit; d++) {
                    if (labels[s][d].contains(updateLoc)) {
                        xaxis = s;
                        yaxis = d;
                    }
                }
            }
            Color yellow = new Color(255, 255, 0, 180);
            g.setColor(yellow);
            g.fillOval((xposit[xaxis][yaxis]), (yposit[xaxis][yaxis]), 6, 6);
            Label labtxt = new Label(labels[xaxis][yaxis]);
            Font bigFont = new Font("SanSerif", Font.BOLD, 12);
            g.setFont(bigFont);
            if (((xposit[xaxis][yaxis] + 6) > (width - 50)) && ((yposit[xaxis][yaxis]) <= 12)) g.drawString(labtxt.getText(), (xposit[xaxis][yaxis] - 50), ((yposit[xaxis][yaxis]) + 18)); else if ((yposit[xaxis][yaxis]) <= 12) g.drawString(labtxt.getText(), (xposit[xaxis][yaxis] + 6), ((yposit[xaxis][yaxis]) + 18)); else if ((xposit[xaxis][yaxis] + 6) > (width - 50)) g.drawString(labtxt.getText(), (xposit[xaxis][yaxis] - 50), yposit[xaxis][yaxis]); else g.drawString(labtxt.getText(), (xposit[xaxis][yaxis] + 6), yposit[xaxis][yaxis]);
            mouseclicked = true;
            alllabel = false;
            updateL = true;
        } else if (alllabel) {
            Font bigFont = new Font("SanSerif", Font.BOLD, 12);
            g.setFont(bigFont);
            for (int s = 0; s < xunit; s++) {
                for (int d = 0; d < yunit; d++) {
                    if (labels[s][d].equals("none")) ; else {
                        Label labtxt = new Label(labels[s][d]);
                        if (((xposit[s][d] + 6) > (width - 50)) && ((yposit[s][d]) <= 12)) g.drawString(labtxt.getText(), (xposit[s][d] - 50), ((yposit[s][d]) + 18)); else if ((yposit[s][d]) <= 12) g.drawString(labtxt.getText(), (xposit[s][d] + 6), ((yposit[s][d]) + 18)); else if ((xposit[s][d] + 6) > (width - 50)) g.drawString(labtxt.getText(), (xposit[s][d] - 50), yposit[s][d]); else g.drawString(labtxt.getText(), (xposit[s][d] + 6), yposit[s][d]);
                    }
                }
            }
            mouseclicked = false;
            alllabel = true;
            updateL = false;
        } else if (mouseclicked) {
            Color yellow = new Color(255, 255, 0, 180);
            g.setColor(yellow);
            g.fillOval((mouseX), (mouseY), 6, 6);
            int xaxis = 1;
            int yaxis = 1;
            for (int p = 0; p < xunit; p++) {
                for (int q = 0; q < yunit; q++) {
                    int testx = xposit[p][q];
                    int testy = yposit[p][q];
                    if (testx == mouseX) xaxis = p;
                    if (testy == mouseY) yaxis = q;
                }
            }
            Label labtxt = new Label(labels[xaxis][yaxis]);
            Font bigFont = new Font("SanSerif", Font.BOLD, 12);
            g.setFont(bigFont);
            if (((xposit[xaxis][yaxis] + 6) > (width - 50)) && ((yposit[xaxis][yaxis]) <= 12)) g.drawString(labtxt.getText(), (xposit[xaxis][yaxis] - 50), ((yposit[xaxis][yaxis]) + 18)); else if ((yposit[xaxis][yaxis]) <= 12) g.drawString(labtxt.getText(), (xposit[xaxis][yaxis] + 6), ((yposit[xaxis][yaxis]) + 18)); else if ((xposit[xaxis][yaxis] + 6) > (width - 50)) g.drawString(labtxt.getText(), (xposit[xaxis][yaxis] - 50), yposit[xaxis][yaxis]); else g.drawString(labtxt.getText(), (xposit[xaxis][yaxis] + 6), yposit[xaxis][yaxis]);
        }
    }

    /** 
	 * Cirlce Info is a MouseAdapter action executed by a pressed mouse.
	 * A label of the next neuron is drawn in yellow with repaint().
	 */
    private class CircleInfo extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            mouseX = -10;
            mouseY = -10;
            for (int p = 0; p < xunit; p++) {
                for (int q = 0; q < yunit; q++) {
                    int testx = xposit[p][q];
                    int testy = yposit[p][q];
                    if ((Math.abs((x - testx)) <= ((width / (xunit)) / 2)) && (Math.abs((x - testx)) >= 0)) mouseX = testx;
                    if ((Math.abs((y - testy)) <= ((height / (yunit)) / 2)) && (Math.abs((y - testy)) >= 0)) mouseY = testy;
                }
            }
            if (topol.equals("rect")) {
            }
            if (topol.equals("hexa")) {
                int xaxis = 1;
                int yaxis = 1;
                for (int p = 0; p < xunit; p++) {
                    for (int q = 0; q < yunit; q++) {
                        int testx = xposit[p][q];
                        int testy = yposit[p][q];
                        if (testx == mouseX) xaxis = p;
                        if (testy == mouseY) yaxis = q;
                    }
                }
                mouseX = xposit[xaxis][yaxis];
                mouseY = yposit[xaxis][yaxis];
            }
            mouseclicked = true;
            alllabel = false;
            updateL = false;
            repaint();
        }
    }

    /** 
	 * Set Label is a Method and an action, which can be executed by Java Script.
	 * A label of the neuron is drawn in yellow with repaint().
	 * 
	 * @param aString String of the searched neuron.
	 */
    public void setLabel(String aString) {
        updateLoc = aString;
        updateL = true;
        repaint();
    }

    /** 
	 * Set Labels is a Method and an action, which can be executed by Java Script.
	 * All labels of the neurons are drawn in a transparent red with repaint().
	 * 
	 * @param bString String which is not used but created by some web browsers.
	 */
    public void setLabels(String bString) {
        mouseclicked = false;
        updateL = false;
        alllabel = true;
        repaint();
    }
}
