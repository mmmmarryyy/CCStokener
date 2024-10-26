package potd;

import java.io.*;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

final class DataLoader {

    Board bd;

    MoveGen mg;

    SANdata san;

    AppMain main;

    DataLoader(Board board, MoveGen movegen, SANdata sandata, AppMain appmain) {
        bd = board;
        mg = movegen;
        san = sandata;
        main = appmain;
    }

    public boolean loadData(URL url, String s) {
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        Object obj = null;
        String s2 = null;
        String s3 = "";
        try {
            int q = main.HOST.indexOf('q');
            String day = main.app.getParameter("day");
            s = main.HOST.substring(0, q - 1) + "files/chess/data/" + day + ".txt";
            URL url1 = new URL(s);
            java.io.StringBufferInputStream sbis = new java.io.StringBufferInputStream(s);
            DataInputStream datainputstream = new DataInputStream(url1.openStream());
            String s1;
            while ((s1 = datainputstream.readLine()) != null) if (!s1.startsWith("*") && s1.trim().length() != 0) if (s2 == null) s2 = new String(s1); else s3 += s1 + " ";
            datainputstream.close();
            parseFEN(new StringTokenizer(s2));
            parseSAN(new StringTokenizer(s3));
            return true;
        } catch (Exception exception) {
            System.err.println("Input File Error: " + exception);
            System.err.println("url: " + s);
        }
        return false;
    }

    private void parseFEN(StringTokenizer stringtokenizer) throws IOException {
        if (stringtokenizer.countTokens() < 4) throw new IOException("Insufficient number of fields in FEN data");
        char ac[] = stringtokenizer.nextToken().toCharArray();
        char ac1[] = stringtokenizer.nextToken().toCharArray();
        char ac2[] = stringtokenizer.nextToken().toCharArray();
        String s = stringtokenizer.nextToken();
        int j;
        int k;
        int i = j = k = 0;
        for (int k1 = 0; k1 < ac.length; k1++) if (ac[k1] == '/') {
            if (k < 8) throw new IOException("Invalid FEN board data - insufficient file data in rank " + (8 - j));
            if (++j > 7) throw new IOException("Invalid FEN board data - too many ranks specified");
            k = 0;
        } else if (ac[k1] >= '1' && ac[k1] <= '8') {
            int l = ac[k1] - 48;
            k += l;
            if (k > 8) throw new IOException("Invalid FEN board data - too many files specified in rank " + (8 - j));
            i += l;
        } else {
            switch(ac[k1]) {
                case 80:
                    bd.setInitPiece(0, 0, i);
                    break;
                case 112:
                    bd.setInitPiece(0, 1, i);
                    break;
                case 78:
                    bd.setInitPiece(1, 0, i);
                    break;
                case 110:
                    bd.setInitPiece(1, 1, i);
                    break;
                case 83:
                    bd.setInitPiece(1, 0, i);
                    break;
                case 115:
                    bd.setInitPiece(1, 1, i);
                    break;
                case 66:
                    bd.setInitPiece(2, 0, i);
                    break;
                case 98:
                    bd.setInitPiece(2, 1, i);
                    break;
                case 82:
                    bd.setInitPiece(3, 0, i);
                    break;
                case 114:
                    bd.setInitPiece(3, 1, i);
                    break;
                case 81:
                    bd.setInitPiece(4, 0, i);
                    break;
                case 113:
                    bd.setInitPiece(4, 1, i);
                    break;
                case 75:
                    bd.setInitPiece(5, 0, i);
                    break;
                case 107:
                    bd.setInitPiece(5, 1, i);
                    break;
                default:
                    throw new IOException("Invalid FEN board data - invalid character in rank " + (8 - j));
            }
            if (++k > 8) throw new IOException("Invalid FEN board data - too many files specified in rank " + (8 - j));
            i++;
        }
        if (k < 8) throw new IOException("Invalid FEN board data - insufficient file data in rank " + (8 - j));
        if (j < 7) throw new IOException("Invalid FEN board data - insufficient number of ranks specified");
        if (ac1.length != 1) throw new IOException("Invalid FEN active color data");
        if (ac1[0] == 'w' || ac1[0] == 'W') main.setStartSide(0); else if (ac1[0] == 'b' || ac1[0] == 'B') main.setStartSide(1); else throw new IOException("Invalid FEN active color specified");
        int i1 = 0;
        if (ac2[0] != '-') {
            for (int l1 = 0; l1 < ac2.length; l1++) switch(ac2[l1]) {
                case 75:
                    i1 |= 1;
                    break;
                case 81:
                    i1 |= 2;
                    break;
                case 107:
                    i1 |= 4;
                    break;
                case 113:
                    i1 |= 8;
                    break;
                default:
                    throw new IOException("Invalid FEN castling availability data");
            }
        }
        bd.setInitCastleStatus(i1);
        int j1 = -1;
        if (!s.equals("-")) {
            if (s.length() == 2) j1 = san.coordToSquare(s);
            if (j1 == -1) throw new IOException("Invalid FEN en passant data");
            if (main.getStartSide() == 1 && san.getRankNumber(s.substring(1)) != 5 || main.getStartSide() == 0 && san.getRankNumber(s.substring(1)) != 2) throw new IOException("Incorrect FEN en passant square specified");
        }
        bd.setInitEpSquare(j1);
    }

    private void parseSAN(StringTokenizer stringtokenizer) throws IOException {
        bd.initBoard(mg);
        int i = 1;
        main.setSide(main.getStartSide());
        int j = 0;
        do {
            String s;
            do {
                try {
                    s = stringtokenizer.nextToken();
                } catch (NoSuchElementException _ex) {
                    return;
                }
                if (s.length() < 2) throw new IOException("Invalid SAN data - single character field");
            } while (s.substring(s.length() - 1).equals("."));
            main.setPly(i);
            mg.genMoves(main.getSide());
            j++;
            Move move;
            if (s.equals("O-O") || s.equals("0-0")) {
                move = main.getSide() != 0 ? mg.findMove(4, 6) : mg.findMove(60, 62);
                if (move == null) throw new IOException("Invalid SAN data - Move #" + j + " - illegal move");
            } else if (s.equals("O-O-O") || s.equals("0-0-0")) {
                move = main.getSide() != 0 ? mg.findMove(4, 2) : mg.findMove(60, 58);
                if (move == null) throw new IOException("Invalid SAN data - Move #" + j + " - illegal move");
            } else {
                for (; s.substring(s.length() - 1).equals("+") || s.substring(s.length() - 1).equals("#") || s.substring(s.length() - 1).equals("!") || s.substring(s.length() - 1).equals("?"); s = s.substring(0, s.length() - 1)) ;
                int i1;
                if ((i1 = san.getPieceFromID(s.substring(s.length() - 1))) != -1) {
                    if (i1 == -1 || i1 == 0 || i1 == 5) throw new IOException("Invalid SAN data - Move #" + j + " promotion piece");
                    if (s.substring(s.length() - 2, s.length() - 1).equals("=")) s = s.substring(0, s.length() - 2); else s = s.substring(0, s.length() - 1);
                } else {
                    i1 = 0;
                }
                int k = san.coordToSquare(s.substring(s.length() - 2, s.length()));
                if (k == -1) throw new IOException("Invalid SAN data - Move #" + j + " target square");
                s = s.substring(0, s.length() - 2);
                if (s.length() > 0 && s.substring(s.length() - 1).equals("x")) s = s.substring(0, s.length() - 1);
                int l = s.length() != 0 ? san.getPieceFromID(s.substring(0, 1)) : 0;
                if (l == -1) throw new IOException("Invalid SAN data - Move #" + j + " source piece");
                if (l != 0) s = s.substring(1);
                int l1;
                int k1 = l1 = -1;
                if (s.length() == 2) {
                    k1 = san.getFileNumber(s.substring(0, 1));
                    l1 = san.getRankNumber(s.substring(1));
                    if (k1 == -1 || l1 == -1) throw new IOException("Invalid SAN data - Move #" + j + " - source piece");
                } else if (s.length() == 1) if (san.isFile(s)) {
                    if ((k1 = san.getFileNumber(s)) == -1) throw new IOException("Invalid SAN data - Move #" + j + " - source piece");
                } else if ((l1 = san.getRankNumber(s)) == -1) throw new IOException("Invalid SAN data - Move #" + j + " - source piece");
                int j1 = mg.parseSource(l, k1, l1, k);
                if (j1 == -1) throw new IOException("Invalid SAN data - Move #" + j + " - ambiguous move");
                if (j1 == -2) throw new IOException("Invalid SAN data - Move #" + j + " - invalid or illegal move");
                move = mg.getMove(j1);
                if ((move.type & 8) != 0) if (i1 != 0) move.promote = i1; else throw new IOException("Invalid SAN data - Move #" + j + " - promotion piece unspecified");
            }
            main.addToSolutionList(move);
            bd.makeMove(move);
            i++;
            main.switchSide();
        } while (true);
    }
}
