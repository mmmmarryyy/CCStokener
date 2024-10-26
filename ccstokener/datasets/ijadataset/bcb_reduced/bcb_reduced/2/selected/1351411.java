package fr.free.jchecs.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import fr.free.jchecs.core.Board;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.Piece;
import fr.free.jchecs.core.Square;
import static fr.free.jchecs.core.Constants.ENDGAMES_WEB_SERVICE;
import static fr.free.jchecs.core.FENUtils.toFEN;

/**
 * Thread prenant en charge l'accès au Web Service des fins de partie.
 * 
 * @author David Cotton
 */
final class EndgamesWSRequest extends Thread {

    /** Générateur de nombres aléatoires. */
    private static final Random RANDOMIZER = new Random();

    /** Etat du plateau. */
    private final Board _board;

    /** Contenu de la réponse du Web Service. */
    private String _response;

    /** Mouvement choisi dans la réponse. */
    private Move _move;

    /**
	 * Instancie un nouveau processus d'accèsau Web Service des fins de partie.
	 * 
	 * @param pEtat
	 *            Etat du plateau de jeu.
	 */
    EndgamesWSRequest(final Board pEtat) {
        assert pEtat != null;
        _board = pEtat;
        _response = null;
        _move = null;
    }

    /**
	 * Renvoi la réponse (éventuelle) du Web Service des fins de parties.
	 * 
	 * @return Réponse du Web Service (ou null).
	 */
    String getResponse() {
        return _response;
    }

    /**
	 * Renvoi le mouvement choisi à partir de la réponse du Web Service des fins
	 * de parties.
	 * 
	 * @return Mouvement choisi (ou null).
	 */
    Move getMove() {
        if ((_move == null) && (_response != null)) {
            if (!_response.startsWith("Error")) {
                final String[] evals = _response.split(";");
                final int choix = RANDOMIZER.nextInt(evals.length);
                final Square src = Square.valueOf(evals[choix].substring(0, 2));
                final Square dst = Square.valueOf(evals[choix].substring(3, 5));
                Piece promotion = null;
                if (evals[choix].length() >= 7) {
                    final char c = evals[choix].charAt(6);
                    if ("BNQR".indexOf(c) >= 0) {
                        promotion = _board.getPieceAt(src).isWhite() ? Piece.valueOf(c) : Piece.valueOf(Character.toLowerCase(c));
                    }
                }
                _move = new Move(_board.getPieceAt(src), src, dst, _board.getPieceAt(dst), promotion);
            }
        }
        return _move;
    }

    /**
	 * Accède au Web Service des fins de partie.
	 * 
	 * @see java.lang.Runnable#run()
	 */
    @Override
    public void run() {
        if (_response == null) {
            BufferedReader in = null;
            try {
                final URL url = new URL(ENDGAMES_WEB_SERVICE + URLEncoder.encode(toFEN(_board), "UTF-8"));
                final HttpURLConnection cnx = (HttpURLConnection) url.openConnection();
                cnx.setConnectTimeout(7500);
                cnx.setReadTimeout(2500);
                in = new BufferedReader(new InputStreamReader(cnx.getInputStream()));
                _response = in.readLine();
                cnx.disconnect();
            } catch (final IOException e) {
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (final IOException e) {
                    }
                }
            }
        }
    }
}
