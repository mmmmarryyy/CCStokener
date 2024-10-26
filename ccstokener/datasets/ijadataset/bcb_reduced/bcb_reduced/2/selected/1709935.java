package Lab4a.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import gui.gameBoard;

/**
 *
 * @author Jerome
 */
public class gui extends gameBoard {

    private int round = 0;

    /** Creates a new instance of gui */
    public gui() {
        init();
    }

    @Override
    public void resetGameBoard() {
        try {
            cleanUp();
            getScores();
            getWord();
            System.out.println("Received: " + theWord);
            System.out.println("Current Round Number: " + round);
            round++;
        } catch (Exception e) {
            System.out.println("Exception caught in Client " + e.getMessage());
        }
    }

    private void getWord() throws IOException {
        URL URL = new URL("http://www.deskteam.net/sysc4504/gameServer.jsp?games=" + round);
        InputStream in = URL.openStream();
        byte[] buffer = new byte[1096];
        int receivedBytes;
        String pageContent = "";
        while ((receivedBytes = in.read(buffer)) != -1) {
            pageContent += new String(buffer);
        }
        in.close();
        theWord = parseWord(pageContent);
        setNewWord();
    }

    private void getScores() throws IOException {
        URL url = new URL("http://www.deskteam.net/sysc4504/gameServer.jsp?player=" + getPlayerName() + "&score=" + getCurrentScore());
        InputStream in = url.openStream();
        byte[] buffer = new byte[1096];
        int receivedBytes;
        String pageContent = "";
        while ((receivedBytes = in.read(buffer)) != -1) {
            pageContent += new String(buffer);
        }
        in.close();
        updateAllPlayersScore((parseWord(pageContent)).replace(";", "\n").replace(":", "\t"));
    }

    private String parseWord(String pageContent) {
        pageContent = pageContent.trim();
        int start = pageContent.indexOf("<response>") + 10;
        int end = pageContent.indexOf("</response>");
        return pageContent.substring(start, end);
    }
}
