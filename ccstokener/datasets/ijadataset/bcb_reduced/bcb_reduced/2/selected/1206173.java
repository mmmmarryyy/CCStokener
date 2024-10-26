package UserInterface;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import Main.*;
import javax.swing.ImageIcon;

@SuppressWarnings("serial")
class HighScoreScreen extends Screen {

    private String[] highScores;

    public HighScoreScreen(ImageIcon imageloc, Link[] links) {
        super(imageloc, null);
        highScores = null;
    }

    /**
	 * Retrieves the high scores on a web page.
	 */
    public void showHighScore() {
        String highScoreAll = sendRequest("http://www.bciproject.com/cs319/highScore2.asp?list=1", "");
        if (highScoreAll.equals("null")) {
            highScores = null;
        }
        highScores = highScoreAll.split("\\|");
    }

    /**
	 * Renders the high score list on the given graphics object.
	 */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (highScores != null) {
            int upperLimit = (highScores.length < 10) ? highScores.length : 10;
            int rowHeight = 25;
            g.setFont(new Font("Arial", Font.BOLD, 13));
            g.drawString("Nation", 90, 170);
            g.drawString("Rank", 140, 170);
            g.drawString("Name", 180, 170);
            g.drawString("Points", 350, 170);
            g.drawString("Date/Time", 430, 170);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            for (int i = 0; i < upperLimit; i++) {
                String[] scoreDetail = highScores[i].split(",");
                if (i % 2 == 0) g.setColor(new Color(200, 190, 190, 200)); else g.setColor(new Color(190, 200, 190, 200));
                g.fillRect(82, 177 + i * rowHeight, 560, rowHeight);
                g.setColor(new Color(47, 47, 63, 255));
                g.drawString((i + 1) + ".       " + scoreDetail[0], 140, 190 + i * rowHeight);
                g.drawString(scoreDetail[2], 350, 190 + i * rowHeight);
                g.drawString(scoreDetail[3] + " UTC", 430, 190 + i * rowHeight);
                try {
                    Image flag = new ImageIcon(new URL("http://iplocationtools.com/flags/" + scoreDetail[1].toLowerCase() + ".png")).getImage();
                    g.drawImage(flag, 90, 177 + i * rowHeight, null);
                } catch (Exception e) {
                    System.out.println("Flag not found: " + scoreDetail[1]);
                }
            }
        } else {
            g.setColor(new Color(47, 47, 63, 255));
            g.drawString("There are not currently any highscore..", 100, 190);
        }
        this.repaint();
    }

    /**
	 * Send a get or post request via TCP/IP on given URL.
	 * @param myurl is the URL address of the request
	 * @param data is the additional data during the post request.
	 */
    public static String sendRequest(String myurl, String data) {
        StringBuffer answer = new StringBuffer();
        try {
            URL url = new URL(myurl);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            if (data != "") {
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(data);
                writer.flush();
                writer.close();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                answer.append(line);
            }
            reader.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return answer.toString();
    }
}
