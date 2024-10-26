package reversi;

import java.awt.*;
import java.applet.*;
import java.lang.*;
import java.net.*;
import java.io.*;

public class KReversi extends Applet {

    static final String VERSION = "2.00";

    static final int YSHIFT = 33;

    static final int ENGLISH = 0;

    static final int ITALIAN = 1;

    static final int EXTERNAL = 2;

    static final Color BLACK = Color.black;

    static final Color BLACK_S = Color.darkGray;

    static final Color WHITE = Color.white;

    static final Color WHITE_S = Color.lightGray;

    static final Color BOARD = new Color(0, 128, 0);

    static final int NOMOVE = 0;

    static final int REALMOVE = 1;

    static final int PSEUDOMOVE = 2;

    static final int Empty = 0;

    static final int User = 1;

    static final int Computer = 2;

    boolean UserMove = true;

    boolean GameOver = false;

    boolean CopyWinOn = false;

    boolean StillInitiated = false;

    int TheBoard[][];

    int Score[][];

    int OpponentScore[][];

    int OldBoard[][][];

    int move = 0;

    int Language;

    String WinMsg_W, WinMsg_T, WinMsg_L, BtnNew, BtnUndo, BtnCopy, MyMove, YourMove, PressWin;

    String getParameter(String p, String def) {
        p = getParameter(p);
        if (p == null) {
            return def;
        }
        return p;
    }

    int SetLanguage(String TheLang) {
        if ((TheLang.compareTo("external") == 0) && (GetExternalLanguage())) {
            return EXTERNAL;
        } else if (TheLang.compareTo("italian") == 0) {
            WinMsg_W = "HO VINTO !";
            WinMsg_T = "PATTA !";
            WinMsg_L = "HAI VINTO !";
            BtnNew = "Nuova Partita";
            BtnCopy = "Copyright";
            BtnUndo = "Annulla mossa";
            MyMove = "Tocca a me";
            YourMove = "Tocca a te";
            PressWin = "Click del mouse per riprendere il gioco";
            return ITALIAN;
        }
        WinMsg_W = "I WON !";
        WinMsg_T = "IT'S TIE !";
        WinMsg_L = "YOU WON !";
        BtnNew = "New Game";
        BtnCopy = "Copyright";
        BtnUndo = "Undo";
        MyMove = "My turn";
        YourMove = "Your turn";
        PressWin = "Mouse click to resume game";
        return ENGLISH;
    }

    public boolean ExternalLanguageVariable(String WorkLine) {
        String lTag = "";
        String lValue = "";
        int lEqPos = -1;
        boolean retVal = false;
        WorkLine = WorkLine.substring(1);
        lEqPos = WorkLine.indexOf("=");
        if (lEqPos == -1) {
            return false;
        }
        lTag = WorkLine.substring(0, lEqPos);
        lValue = WorkLine.substring(lEqPos + 1);
        if (lTag.compareTo("NEW_GAME_BUTTON") == 0) {
            BtnNew = lValue;
            retVal = true;
        } else if (lTag.compareTo("COPYRIGHT_BUTTON") == 0) {
            BtnCopy = lValue;
            retVal = true;
        } else if (lTag.compareTo("CLICK_MESSAGE") == 0) {
            PressWin = lValue;
            retVal = true;
        } else if (lTag.compareTo("COMPUTER_WON") == 0) {
            WinMsg_W = lValue;
            retVal = true;
        } else if (lTag.compareTo("TIE_GAME") == 0) {
            WinMsg_T = lValue;
            retVal = true;
        } else if (lTag.compareTo("USER_WON") == 0) {
            WinMsg_L = lValue;
            retVal = true;
        } else if (lTag.compareTo("COMPUTER_MOVE") == 0) {
            MyMove = lValue;
            retVal = true;
        } else if (lTag.compareTo("USER_MOVE") == 0) {
            YourMove = lValue;
            retVal = true;
        }
        return retVal;
    }

    public boolean GetExternalLanguage() {
        String thisURL, newURL, TheLine;
        boolean ReadOK = true;
        int SlashPos = -1;
        thisURL = getDocumentBase().toString();
        SlashPos = thisURL.lastIndexOf("/");
        newURL = thisURL.substring(0, (SlashPos + 1)) + "language.txt";
        try {
            URL url = new URL(newURL);
            try {
                InputStream TheFile = url.openStream();
                try {
                    DataInputStream MyData = new DataInputStream(TheFile);
                    try {
                        while ((TheLine = MyData.readLine()) != null) {
                            if (TheLine.substring(0, 1).compareTo("*") == 0) {
                                if (!ExternalLanguageVariable(TheLine)) {
                                    ReadOK = false;
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error " + e.toString());
                        ReadOK = false;
                    }
                } catch (Exception e) {
                    System.out.println("Error " + e.toString());
                    ReadOK = false;
                }
            } catch (Exception f) {
                System.out.println("Error " + f.toString());
                ReadOK = false;
            }
        } catch (Exception g) {
            System.out.println("Error " + g.toString());
            ReadOK = false;
        }
        return ReadOK;
    }

    public void DrawBoard(Graphics g) {
        g.setColor(new Color(0, 64, 0));
        g.fillRect(0, 0, 321, 33);
        g.fillRect(0, 354, 321, 33);
        g.setColor(BOARD);
        g.fillRect(0, 33, 321, 321);
        g.setColor(BLACK);
        for (int i = 0; i <= 8; i++) {
            g.drawLine((40 * i), 33, (40 * i), 353);
            g.drawLine(0, YSHIFT + (40 * (i)), 319, YSHIFT + (40 * (i)));
        }
    }

    public void DrawPiece(int Who, int Col, int Row) {
        Graphics g = getGraphics();
        int pCol = (40 * (Col - 1) + 1);
        int pRow = YSHIFT + (40 * (Row - 1) + 1);
        Color pColor, pShadow;
        if (Who == User) {
            pColor = BLACK;
            pShadow = BLACK_S;
        } else {
            pColor = WHITE;
            pShadow = WHITE_S;
        }
        TheBoard[Col - 1][Row - 1] = Who;
        g.setColor(pShadow);
        g.fillOval(pCol + 6, pRow + 6, 29, 29);
        g.setColor(pColor);
        g.fillOval(pCol + 5, pRow + 5, 29, 29);
    }

    public void MsgWhoMove(boolean UM) {
        String s = "";
        Graphics g = getGraphics();
        g.setColor(new Color(0, 64, 0));
        g.fillRect(0, 354, 321, 33);
        g.setColor(Color.green);
        g.drawRect(2, 356, 26, 26);
        g.drawRect(290, 356, 26, 26);
        g.setColor(BOARD);
        g.fillRect(3, 357, 25, 25);
        g.fillRect(291, 357, 25, 25);
        if (UM) {
            g.setColor(BLACK_S);
            g.fillOval(6, 360, 20, 20);
            g.fillOval(294, 360, 20, 20);
            g.setColor(BLACK);
            g.fillOval(5, 359, 20, 20);
            g.fillOval(293, 359, 20, 20);
            g.setColor(Color.green);
            g.drawString("Bianco: " + NumberPiece(Computer, TheBoard) + " " + YourMove + " Nero: " + NumberPiece(User, TheBoard), ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth("Bianco: " + NumberPiece(Computer, TheBoard) + " " + YourMove + " Nero: " + NumberPiece(User, TheBoard)))) / 2)) + 1, 380);
        } else {
            g.setColor(WHITE_S);
            g.fillOval(6, 360, 20, 20);
            g.fillOval(294, 360, 20, 20);
            g.setColor(WHITE);
            g.fillOval(5, 359, 20, 20);
            g.fillOval(293, 359, 20, 20);
            g.setColor(Color.green);
            g.drawString("Bianco: " + NumberPiece(Computer, TheBoard) + " " + MyMove + " Nero: " + NumberPiece(User, TheBoard), ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth("Bianco: " + NumberPiece(Computer, TheBoard) + " " + MyMove + " Nero: " + NumberPiece(User, TheBoard)))) / 2)) + 1, 380);
        }
    }

    public int FlipRow(int Who, int[][] WhichBoard, int C, int R, int CInc, int RInc, int MakeMove) {
        int NewCol;
        int NewRow;
        int Opponent = User + Computer - Who;
        int CNT = 0;
        NewCol = C - 1;
        NewRow = R - 1;
        while (true) {
            if (((NewCol + CInc) < 0) || ((NewCol + CInc) > 7) || ((NewRow + RInc) < 0) || ((NewRow + RInc) > 7)) {
                return 0;
            }
            if (WhichBoard[NewCol + CInc][NewRow + RInc] == Opponent) {
                CNT++;
                NewCol += CInc;
                NewRow += RInc;
            } else if (WhichBoard[NewCol + CInc][NewRow + RInc] == Empty) {
                return 0;
            } else {
                break;
            }
        }
        if (MakeMove != NOMOVE) {
            C--;
            R--;
            for (int v = 0; v <= CNT; v++) {
                if (MakeMove == REALMOVE) {
                    DrawPiece(Who, C + 1, R + 1);
                } else {
                    WhichBoard[C][R] = Who;
                }
                C += CInc;
                R += RInc;
            }
        }
        return CNT;
    }

    public boolean IsLegalMove(int Who, int[][] WhichBoard, int C, int R) {
        if (WhichBoard[C - 1][R - 1] != Empty) {
            return false;
        }
        for (int CInc = -1; CInc < 2; CInc++) {
            for (int RInc = -1; RInc < 2; RInc++) {
                if (FlipRow(Who, WhichBoard, C, R, CInc, RInc, NOMOVE) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public int NumberPiece(int Who, int[][] WhichBoard) {
        int retval = 0;
        for (int i = 0; i < 8; i++) {
            for (int id = 0; id < 8; id++) {
                if (WhichBoard[i][id] == Who) {
                    retval++;
                }
            }
        }
        return retval;
    }

    public boolean MakeMove(int Who, int C, int R) {
        if (Who == User) {
            AggOld();
            move++;
        }
        for (int CInc = -1; CInc < 2; CInc++) {
            for (int RInc = -1; RInc < 2; RInc++) {
                FlipRow(Who, TheBoard, C, R, CInc, RInc, REALMOVE);
            }
        }
        if (IsBoardComplete() || ((!ThereAreMoves(Computer, TheBoard)) && (!ThereAreMoves(User, TheBoard)))) {
            return false;
        }
        int Opponent = (User + Computer) - Who;
        if (ThereAreMoves(Opponent, TheBoard)) {
            UserMove = !UserMove;
        }
        return true;
    }

    public void EndGame() {
        int CompPieces = 0;
        int UserPieces = 0;
        String TheMsg;
        Graphics g = getGraphics();
        int StrWidth;
        MsgWhoMove(true);
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                if (TheBoard[c][r] == Computer) {
                    CompPieces++;
                } else {
                    UserPieces++;
                }
            }
        }
        if (CompPieces > UserPieces) {
            TheMsg = WinMsg_W;
        } else if (UserPieces > CompPieces) {
            TheMsg = WinMsg_L;
        } else {
            TheMsg = WinMsg_T;
        }
        g.setFont(new Font("Helvetica", Font.BOLD, 48));
        StrWidth = g.getFontMetrics(g.getFont()).stringWidth(TheMsg);
        g.setColor(new Color(0, 0, 128));
        g.drawString(TheMsg, ((int) ((321 - StrWidth) / 2)) + 1, 209);
        g.setColor(new Color(0, 255, 255));
        g.drawString(TheMsg, ((int) ((321 - StrWidth) / 2)), 208);
    }

    public boolean IsBoardComplete() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (TheBoard[i][j] == Empty) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean ThereAreMoves(int Who, int[][] WhichBoard) {
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                if (IsLegalMove(Who, WhichBoard, i, j)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int CalcOpponentScore(int CP, int RP) {
        int OpScore = 0;
        int tempBoard[][] = new int[8][8];
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                tempBoard[c][r] = TheBoard[c][r];
            }
        }
        for (int CInc = -1; CInc < 2; CInc++) {
            for (int RInc = -1; RInc < 2; RInc++) {
                FlipRow(Computer, tempBoard, CP + 1, RP + 1, CInc, RInc, PSEUDOMOVE);
            }
        }
        if (ThereAreMoves(User, tempBoard)) {
            for (int C = 0; C < 8; C++) {
                for (int R = 0; R < 8; R++) {
                    OpScore += RankMove(User, tempBoard, C, R);
                }
            }
        }
        return OpScore;
    }

    public void RankMoves() {
        for (int C = 0; C < 8; C++) {
            for (int R = 0; R < 8; R++) {
                Score[C][R] = RankMove(Computer, TheBoard, C, R);
                if (Score[C][R] != 0) {
                    OpponentScore[C][R] = CalcOpponentScore(C, R);
                } else {
                    OpponentScore[C][R] = 0;
                }
            }
        }
    }

    public int RankMove(int Who, int[][] WhichBoard, int Col, int Row) {
        int CNT = 0;
        int MV = 0;
        if (WhichBoard[Col][Row] != Empty) {
            return 0;
        }
        for (int CInc = -1; CInc < 2; CInc++) {
            for (int RInc = -1; RInc < 2; RInc++) {
                MV = FlipRow(Who, WhichBoard, Col + 1, Row + 1, CInc, RInc, NOMOVE);
                CNT += MV;
            }
        }
        if (CNT > 0) {
            if (((Col == 0) || (Col == 7)) || ((Row == 0) || (Row == 7))) {
                CNT = 63;
            }
            if (((Col == 0) || (Col == 7)) && ((Row == 0) || (Row == 7))) {
                CNT = 64;
            }
            if ((((Col == 0) || (Col == 7)) && (Row == 1) || (Row == 6)) && (((Col == 1) || (Col == 6)) && (Row == 0) || (Row == 7)) && (((Col == 1) || (Col == 6)) && (Row == 1) || (Row == 6))) {
                CNT = 1;
            }
        }
        return CNT;
    }

    public int[] BestMove() {
        int retval[];
        retval = new int[3];
        retval[0] = -998;
        retval[1] = 0;
        retval[2] = 0;
        RankMoves();
        for (int C = 0; C < 8; C++) {
            for (int R = 0; R < 8; R++) {
                if ((Score[C][R] == 0) && (OpponentScore[C][R] == 0)) {
                    Score[C][R] = -999;
                } else if (Score[C][R] != 64) {
                    Score[C][R] = Score[C][R] - OpponentScore[C][R];
                }
            }
        }
        for (int C = 0; C < 8; C++) {
            for (int R = 0; R < 8; R++) {
                if (Score[C][R] > retval[0]) {
                    retval[1] = C;
                    retval[2] = R;
                    retval[0] = Score[C][R];
                }
            }
        }
        retval[1]++;
        retval[2]++;
        return retval;
    }

    public void ShowAbout() {
        Graphics g = getGraphics();
        g.setColor(Color.lightGray);
        g.fill3DRect(20, 53, 281, 281, true);
        g.setColor(Color.black);
        g.setFont(new Font("Helvetica", Font.BOLD, 24));
        String TheMsg = "K-Reversi (v." + VERSION + ")";
        g.drawString(TheMsg, ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth(TheMsg))) / 2)) + 1, 110);
        g.setFont(new Font("Helvetica", Font.BOLD, 12));
        TheMsg = "By: Alessandro A. 'Kazuma' Garbagnati";
        g.drawString(TheMsg, ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth(TheMsg))) / 2)) + 1, 150);
        TheMsg = "kazuma@energy.it";
        g.drawString(TheMsg, ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth(TheMsg))) / 2)) + 1, 165);
        g.setFont(new Font("Helvetica", Font.PLAIN, 12));
        TheMsg = "thanks to: Muffy Barkocy";
        g.drawString(TheMsg, ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth(TheMsg))) / 2)) + 1, 185);
        TheMsg = "muffy@fish.com";
        g.drawString(TheMsg, ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth(TheMsg))) / 2)) + 1, 200);
        g.setFont(new Font("Helvetica", Font.ITALIC, 16));
        TheMsg = "Paolo Ricciuti release";
        g.drawString(TheMsg, ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth(TheMsg))) / 2)) + 1, 270);
        g.setFont(new Font("System", Font.BOLD, 12));
        g.drawString(PressWin, ((int) ((321 - (g.getFontMetrics(g.getFont()).stringWidth(PressWin))) / 2)) + 1, 320);
        CopyWinOn = true;
    }

    public void Undo() {
        if (move != 0) {
            for (int i = 0; i < 8; i++) {
                for (int id = 0; id < 8; id++) {
                    TheBoard[i][id] = OldBoard[i][id][move - 1];
                }
            }
            move--;
            repaint();
        }
    }

    public void AggOld() {
        for (int i = 0; i < 8; i++) {
            for (int id = 0; id < 8; id++) {
                OldBoard[i][id][move] = TheBoard[i][id];
            }
        }
    }

    public void paint(Graphics g) {
        DrawBoard(g);
        MsgWhoMove(UserMove);
        if (!CopyWinOn) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (TheBoard[i][j] != Empty) {
                        DrawPiece(TheBoard[i][j], i + 1, j + 1);
                    }
                }
            }
        } else {
            ShowAbout();
        }
    }

    @Override
    public void init() {
        resize(321, 387);
        Language = SetLanguage("italian");
        if (!StillInitiated) {
            setFont(new Font("System", Font.PLAIN, 12));
            add(new Button(BtnNew));
            add(new Button(BtnCopy));
            add(new Button(BtnUndo));
            StillInitiated = true;
        }
        TheBoard = new int[8][8];
        OldBoard = new int[8][8][63];
        Score = new int[8][8];
        OpponentScore = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                TheBoard[i][j] = Empty;
                for (int id = 0; id < 63; id++) {
                    OldBoard[i][j][id] = Empty;
                }
            }
        }
        TheBoard[3][3] = User;
        TheBoard[3][4] = Computer;
        TheBoard[4][3] = Computer;
        TheBoard[4][4] = User;
        UserMove = true;
        MsgWhoMove(true);
        repaint();
    }

    public boolean handleEvent(Event evt) {
        int TheCol, TheRow;
        int BMove[];
        int BX, BY;
        boolean retval = false;
        if (BtnNew.equals(evt.arg) && (!CopyWinOn)) {
            init();
            return true;
        }
        if (BtnUndo.equals(evt.arg) && (!CopyWinOn)) {
            Undo();
            repaint();
            return true;
        }
        if (BtnCopy.equals(evt.arg) && (!CopyWinOn)) {
            ShowAbout();
            return true;
        }
        if ((CopyWinOn) && (evt.id == Event.MOUSE_UP)) {
            CopyWinOn = false;
            repaint();
            return true;
        }
        BX = evt.x;
        BY = evt.y - YSHIFT;
        if ((BY >= 0) && (BY <= 321) && (evt.id == Event.MOUSE_UP) && (UserMove)) {
            TheCol = (int) ((BX / 40) + 1);
            TheRow = (int) ((BY / 40) + 1);
            if (IsLegalMove(User, TheBoard, TheCol, TheRow)) {
                retval = MakeMove(User, TheCol, TheRow);
                while (retval && (!UserMove)) {
                    MsgWhoMove(UserMove);
                    BMove = BestMove();
                    retval = MakeMove(Computer, BMove[1], BMove[2]);
                    MsgWhoMove(UserMove);
                }
                if (!retval) {
                    EndGame();
                }
            }
            return true;
        }
        return false;
    }
}
