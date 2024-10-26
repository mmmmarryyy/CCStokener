import java.io.*;
import java.util.*;
import java.net.*;
import java.applet.*;

public class e2game {

    e2board board;

    int rownb;

    int colnb;

    int patternnb;

    e2piecelist allpieces;

    e2piecelist unplacedpieces;

    e2piecelist placedpieces;

    e2piecelist[][][][] precalculated_lists;

    URL baseURL;

    e2_basicbacktracker solver;

    int max_score;

    int score;

    public e2game(URL in_URL) {
        int i, j, k, l;
        e2piece temppiece;
        e2colorpattern temppattern;
        rownb = 16;
        colnb = 16;
        patternnb = 23;
        board = new e2board(colnb, rownb);
        allpieces = new e2piecelist();
        unplacedpieces = new e2piecelist();
        placedpieces = new e2piecelist();
        precalculated_lists = new e2piecelist[patternnb][patternnb][patternnb][patternnb];
        baseURL = in_URL;
        import_pieces("e2files/e2pieces.txt");
        import_hints("e2files/e2hints.txt");
        solver = new e2_basicbacktracker(this);
    }

    public e2game(URL in_URL, int in_col, int in_row, String e2pieces, String e2hints) {
        int i, j, k, l;
        e2piece temppiece;
        e2colorpattern temppattern;
        rownb = in_row;
        colnb = in_col;
        patternnb = 23;
        board = new e2board(colnb, rownb);
        allpieces = new e2piecelist();
        unplacedpieces = new e2piecelist();
        placedpieces = new e2piecelist();
        precalculated_lists = new e2piecelist[patternnb][patternnb][patternnb][patternnb];
        baseURL = in_URL;
        import_pieces(e2pieces);
        import_hints(e2hints);
        solver = new e2_basicbacktracker(this);
    }

    public boolean import_pieces(String filename) {
        int pieceId;
        int i;
        int n;
        int[] color;
        boolean byurl = true;
        e2piece temppiece;
        color = new int[4];
        BufferedReader entree;
        try {
            if (byurl == true) {
                URL url = new URL(baseURL, filename);
                InputStream in = url.openStream();
                entree = new BufferedReader(new InputStreamReader(in));
            } else {
                entree = new BufferedReader(new FileReader(filename));
            }
            pieceId = 0;
            while (true) {
                String lineread = entree.readLine();
                if (lineread == null) {
                    break;
                }
                StringTokenizer tok = new StringTokenizer(lineread, " ");
                n = tok.countTokens();
                if (n == 2) {
                } else {
                    for (i = 0; i < 4; i++) {
                        color[i] = Integer.parseInt(tok.nextToken());
                    }
                    pieceId++;
                    System.out.println("Read Piece : " + pieceId + ":" + color[0] + " " + color[1] + " " + color[2] + " " + color[3]);
                    temppiece = new e2piece(pieceId, color[0] + 1, color[1] + 1, color[2] + 1, color[3] + 1);
                    allpieces.add_piece(temppiece);
                    unplacedpieces.add_piece(temppiece);
                }
            }
            return true;
        } catch (IOException err) {
            return false;
        }
    }

    public boolean export_pieces(String filename) {
        return true;
    }

    public boolean import_hints(String filename) {
        int pieceId;
        int i, col, row;
        int rotation;
        int number;
        boolean byurl = true;
        e2piece temppiece;
        String lineread;
        StringTokenizer tok;
        BufferedReader entree;
        try {
            if (byurl == true) {
                URL url = new URL(baseURL, filename);
                InputStream in = url.openStream();
                entree = new BufferedReader(new InputStreamReader(in));
            } else {
                entree = new BufferedReader(new FileReader(filename));
            }
            pieceId = 0;
            lineread = entree.readLine();
            tok = new StringTokenizer(lineread, " ");
            number = Integer.parseInt(tok.nextToken());
            for (i = 0; i < number; i++) {
                lineread = entree.readLine();
                if (lineread == null) {
                    break;
                }
                tok = new StringTokenizer(lineread, " ");
                pieceId = Integer.parseInt(tok.nextToken());
                col = Integer.parseInt(tok.nextToken()) - 1;
                row = Integer.parseInt(tok.nextToken()) - 1;
                rotation = Integer.parseInt(tok.nextToken());
                System.out.println("placing hint piece : " + pieceId);
                place_piece_at(pieceId, col, row, 0);
                temppiece = board.get_piece_at(col, row);
                temppiece.reset_rotation();
                temppiece.rotate(rotation);
                temppiece.set_as_hint();
            }
            return true;
        } catch (IOException err) {
            return false;
        }
    }

    public boolean export_hints(String filename) {
        return true;
    }

    public boolean import_status(String filename) {
        int pieceId;
        int i, j, col, row;
        int rotation;
        int number;
        boolean byurl = false;
        e2piece temppiece;
        String lineread;
        StringTokenizer tok;
        BufferedReader entree;
        try {
            if (byurl == true) {
                URL url = new URL(baseURL, filename);
                InputStream in = url.openStream();
                entree = new BufferedReader(new InputStreamReader(in));
            } else {
                entree = new BufferedReader(new FileReader(filename));
            }
            pieceId = 0;
            for (i = 0; i < board.colnb; i++) {
                for (j = 0; j < board.rownb; j++) {
                    unplace_piece_at(i, j);
                }
            }
            while (true) {
                lineread = entree.readLine();
                if (lineread == null) {
                    break;
                }
                tok = new StringTokenizer(lineread, " ");
                pieceId = Integer.parseInt(tok.nextToken());
                col = Integer.parseInt(tok.nextToken()) - 1;
                row = Integer.parseInt(tok.nextToken()) - 1;
                rotation = Integer.parseInt(tok.nextToken());
                place_piece_at(pieceId, col, row, 0);
                temppiece = board.get_piece_at(col, row);
                temppiece.reset_rotation();
                temppiece.rotate(rotation);
            }
            return true;
        } catch (IOException err) {
            return false;
        }
    }

    public boolean export_status(String filename) {
        int pieceId;
        int i, col, row;
        int rotation;
        int number;
        e2piecelist templist;
        boolean byurl = false;
        e2piece temppiece;
        String lineread;
        StringTokenizer tok;
        BufferedWriter bufwrite;
        PrintWriter sortie;
        try {
            if (byurl == true) {
                URL url = new URL(baseURL, filename);
                FileOutputStream out = new FileOutputStream(filename);
                bufwrite = new BufferedWriter(new OutputStreamWriter(out));
            } else {
                bufwrite = new BufferedWriter(new FileWriter(filename));
            }
            sortie = new PrintWriter(bufwrite);
            pieceId = 0;
            templist = get_placed_pieces();
            templist.restart();
            while (templist.hasNext()) {
                temppiece = templist.next();
                col = temppiece.position.col + 1;
                row = temppiece.position.row + 1;
                sortie.println(temppiece.id + " " + col + " " + row + " " + temppiece.position.rotation);
            }
            sortie.flush();
            sortie.close();
            return true;
        } catch (IOException err) {
            return false;
        }
    }

    public boolean is_piece_matching_pattern(int in_id, e2colorpattern in_pattern) {
        boolean result;
        e2piece temppiece;
        result = false;
        temppiece = allpieces.get_piece(in_id);
        if (temppiece.is_matching_pattern(in_pattern)) {
            result = true;
        }
        return result;
    }

    public boolean is_piece_placed(int in_id) {
        boolean result;
        if (placedpieces.is_piece_in_list(in_id) == true) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    public boolean place_piece_at(int in_id, int in_col, int in_row, int in_rotation) {
        boolean result;
        e2piece temppiece;
        result = false;
        temppiece = unplacedpieces.get_piece(in_id);
        if (temppiece == null) {
            temppiece = placedpieces.get_piece(in_id);
            placedpieces.remove_piece(in_id);
            unplacedpieces.add_piece(temppiece);
        }
        if (temppiece != null) {
            if (board.place_piece_at(temppiece, in_col, in_row, in_rotation)) {
                unplacedpieces.remove_piece(in_id);
                placedpieces.add_piece(temppiece);
                result = true;
            }
        }
        return result;
    }

    public boolean unplace_piece_at(int in_col, int in_row) {
        boolean result;
        e2piece temppiece;
        result = false;
        temppiece = board.get_piece_at(in_col, in_row);
        if (temppiece != null) {
            if (board.unplace_piece(temppiece.id)) {
                placedpieces.remove_piece(temppiece.id);
                unplacedpieces.add_piece(temppiece);
                result = true;
            }
        }
        return result;
    }

    public boolean unplace_piece(int in_id) {
        boolean result;
        e2piece temppiece;
        result = false;
        if (placedpieces.is_piece_in_list(in_id)) {
            temppiece = placedpieces.get_piece(in_id);
            placedpieces.remove_piece(in_id);
            unplacedpieces.add_piece(temppiece);
            result = true;
        }
        return result;
    }

    public e2colorpattern get_pattern_at(int in_col, int in_row) {
        return board.get_color_pattern_at(in_col, in_row);
    }

    public e2piecelist get_all_matching_pieces(e2colorpattern in_pattern) {
        e2piecelist returned_list;
        e2piece temppiece;
        returned_list = new e2piecelist();
        allpieces.restart();
        while (allpieces.hasNext()) {
            temppiece = allpieces.next();
            if (temppiece.is_matching_pattern(in_pattern)) {
                returned_list.add_piece(temppiece);
            }
        }
        return returned_list;
    }

    public int get_all_matching_pieces_nb(e2colorpattern in_pattern) {
        return get_all_matching_pieces(in_pattern).get_number_of_elements();
    }

    public e2piecelist get_unplaced_matching_pieces(e2colorpattern in_pattern) {
        e2piecelist returned_list;
        e2piece temppiece;
        returned_list = new e2piecelist();
        unplacedpieces.restart();
        while (unplacedpieces.hasNext()) {
            temppiece = unplacedpieces.next();
            if (temppiece.is_matching_pattern(in_pattern)) {
                returned_list.add_piece(temppiece);
            }
        }
        return returned_list;
    }

    public int get_unplaced_matching_pieces_nb(e2colorpattern in_pattern) {
        return get_unplaced_matching_pieces(in_pattern).get_number_of_elements();
    }

    public e2piecelist get_unplaced_pieces() {
        e2piecelist returned_list;
        e2piece temppiece;
        returned_list = new e2piecelist();
        unplacedpieces.restart();
        while (unplacedpieces.hasNext()) {
            temppiece = unplacedpieces.next();
            returned_list.add_piece(temppiece);
        }
        return returned_list;
    }

    public e2piecelist get_placed_pieces() {
        e2piecelist returned_list;
        e2piece temppiece;
        returned_list = new e2piecelist();
        placedpieces.restart();
        while (placedpieces.hasNext()) {
            temppiece = placedpieces.next();
            returned_list.add_piece(temppiece);
        }
        return returned_list;
    }

    public e2piecelist get_all_pieces() {
        e2piecelist returned_list;
        e2piece temppiece;
        returned_list = new e2piecelist();
        allpieces.restart();
        while (allpieces.hasNext()) {
            temppiece = allpieces.next();
            returned_list.add_piece(temppiece);
        }
        return returned_list;
    }

    public e2position get_piece_position(int in_id) {
        e2piece temppiece;
        temppiece = allpieces.get_piece(in_id);
        return temppiece.get_position();
    }
}
