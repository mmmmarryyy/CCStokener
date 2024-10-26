package yacc;

import java.lang.Math;
import java.io.*;
import java.net.URL;

public class Parser {

    boolean yydebug;

    int yynerrs;

    int yyerrflag;

    int yychar;

    void debug(String msg) {
        if (yydebug) System.out.println(msg);
    }

    static final int YYSTACKSIZE = 500;

    int statestk[] = new int[YYSTACKSIZE];

    int stateptr;

    int stateptrmax;

    int statemax;

    final void state_push(int state) {
        try {
            stateptr++;
            statestk[stateptr] = state;
        } catch (ArrayIndexOutOfBoundsException e) {
            int oldsize = statestk.length;
            int newsize = oldsize * 2;
            int[] newstack = new int[newsize];
            System.arraycopy(statestk, 0, newstack, 0, oldsize);
            statestk = newstack;
            statestk[stateptr] = state;
        }
    }

    final int state_pop() {
        return statestk[stateptr--];
    }

    final void state_drop(int cnt) {
        stateptr -= cnt;
    }

    final int state_peek(int relative) {
        return statestk[stateptr - relative];
    }

    final boolean init_stacks() {
        stateptr = -1;
        val_init();
        return true;
    }

    void dump_stacks(int count) {
        int i;
        System.out.println("=index==state====value=     s:" + stateptr + "  v:" + valptr);
        for (i = 0; i < count; i++) System.out.println(" " + i + "    " + statestk[i] + "      " + valstk[i]);
        System.out.println("======================");
    }

    String yytext;

    ParserVal yyval;

    ParserVal yylval;

    ParserVal valstk[];

    int valptr;

    void val_init() {
        valstk = new ParserVal[YYSTACKSIZE];
        yyval = new ParserVal();
        yylval = new ParserVal();
        valptr = -1;
    }

    void val_push(ParserVal val) {
        if (valptr >= YYSTACKSIZE) return;
        valstk[++valptr] = val;
    }

    ParserVal val_pop() {
        if (valptr < 0) return new ParserVal();
        return valstk[valptr--];
    }

    void val_drop(int cnt) {
        int ptr;
        ptr = valptr - cnt;
        if (ptr < 0) return;
        valptr = ptr;
    }

    ParserVal val_peek(int relative) {
        int ptr;
        ptr = valptr - relative;
        if (ptr < 0) return new ParserVal();
        return valstk[ptr];
    }

    public static final short NUM = 257;

    public static final short WORD = 258;

    public static final short NEG = 259;

    public static final short FILE_COOKIE = 260;

    public static final short CAMERA = 261;

    public static final short COLOR = 262;

    public static final short NORM = 263;

    public static final short DIRECTION = 264;

    public static final short UP = 265;

    public static final short LEFT = 266;

    public static final short SCENE = 267;

    public static final short SPHERE = 268;

    public static final short PLANE = 269;

    public static final short GROUP = 270;

    public static final short POSITION = 271;

    public static final short ROTATE = 272;

    public static final short SCALE = 273;

    public static final short TRANSLATE = 274;

    public static final short YYERRCODE = 256;

    static final short yylhs[] = { -1, 1, 0, 2, 4, 4, 5, 5, 5, 5, 3, 8, 8, 9, 9, 9, 10, 13, 13, 14, 14, 14, 15, 16, 17, 11, 12, 20, 6, 19, 7, 18, 21, 21, 21, 21, 21, 21, 21, 21 };

    static final short yylen[] = { 2, 0, 6, 4, 0, 2, 1, 2, 2, 2, 4, 0, 2, 1, 1, 1, 5, 0, 2, 1, 1, 1, 2, 2, 2, 6, 5, 2, 2, 2, 5, 1, 1, 3, 3, 3, 3, 2, 3, 3 };

    static final short yydefred[] = { 0, 1, 0, 0, 0, 0, 0, 4, 0, 0, 0, 11, 2, 0, 0, 0, 0, 3, 5, 6, 0, 0, 7, 8, 9, 28, 0, 0, 0, 10, 12, 13, 14, 15, 32, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 19, 20, 21, 30, 0, 0, 27, 26, 22, 23, 24, 16, 29, 25 };

    static final short yydgoto[] = { 2, 3, 6, 9, 10, 18, 19, 22, 20, 30, 31, 32, 33, 52, 67, 68, 69, 70, 37, 73, 62, 38 };

    static final short yysindex[] = { -246, 0, 0, -78, -189, -50, -193, 0, -43, -44, -121, 0, 0, 22, 22, 22, 22, 0, 0, 0, -113, -4, 0, 0, 0, 0, -39, -38, -37, 0, 0, 0, 0, 0, 0, -4, -4, -4, 17, -188, -188, 0, -7, 10, -4, -4, -4, -4, -4, -4, -4, -175, -205, 0, 27, 16, 16, -7, -7, -7, -172, 22, -33, 22, 22, 22, -105, 0, 0, 0, 0, 0, 22, -30, 0, 0, 0, 0, 0, 0, 0, 0 };

    static final short yyrindex[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -34, 0, 0, 0, -40, 0, 0, 0, 0, 0, 0, 0, 0, 0, -91, 0, 0, -8, -1, -32, -24, -16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    static final short yygindex[] = { 0, 0, 0, 0, 0, 0, 31, 33, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 30 };

    static final int YYTABLESIZE = 261;

    static short yytable[];

    static {
        yytable();
    }

    static void yytable() {
        yytable = new short[] { 37, 37, 37, 37, 17, 37, 31, 37, 35, 35, 35, 35, 29, 35, 1, 35, 36, 36, 36, 36, 79, 36, 37, 36, 38, 38, 38, 38, 31, 38, 35, 38, 34, 34, 11, 34, 36, 34, 36, 33, 33, 35, 33, 44, 33, 4, 38, 23, 24, 25, 54, 53, 47, 46, 34, 45, 60, 48, 47, 47, 46, 33, 45, 48, 48, 42, 43, 63, 64, 65, 50, 51, 5, 7, 8, 55, 56, 57, 58, 59, 11, 12, 21, 16, 39, 40, 41, 49, 61, 71, 72, 66, 75, 0, 74, 81, 76, 77, 78, 0, 0, 0, 0, 0, 49, 80, 0, 0, 0, 0, 49, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 14, 15, 0, 0, 0, 0, 16, 0, 0, 0, 0, 26, 27, 28, 0, 0, 0, 0, 0, 26, 27, 28, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11, 11, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 0, 0, 0, 0, 37, 31, 0, 35, 0, 0, 31, 0, 35, 0, 0, 36, 0, 0, 0, 0, 36, 0, 0, 38, 0, 0, 0, 0, 38, 0, 0, 34, 0, 0, 0, 34, 34, 0, 33, 0, 0, 0, 0, 33 };
    }

    static short yycheck[];

    static {
        yycheck();
    }

    static void yycheck() {
        yycheck = new short[] { 40, 41, 42, 43, 125, 45, 40, 47, 40, 41, 42, 43, 125, 45, 260, 47, 40, 41, 42, 43, 125, 45, 62, 47, 40, 41, 42, 43, 62, 45, 62, 47, 40, 41, 125, 43, 40, 45, 62, 40, 41, 45, 43, 37, 45, 123, 62, 14, 15, 16, 44, 41, 42, 43, 62, 45, 50, 47, 42, 42, 43, 62, 45, 47, 47, 35, 36, 272, 273, 274, 39, 40, 261, 123, 267, 45, 46, 47, 48, 49, 123, 125, 60, 271, 123, 123, 123, 94, 263, 62, 262, 52, 125, -1, 61, 125, 63, 64, 65, -1, -1, -1, -1, -1, 94, 72, -1, -1, -1, -1, 94, 94, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 264, 265, 266, -1, -1, -1, -1, 271, -1, -1, -1, -1, 268, 269, 270, -1, -1, -1, -1, -1, 268, 269, 270, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 268, 269, 270, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 257, -1, -1, -1, -1, 262, 257, -1, 257, -1, -1, 262, -1, 262, -1, -1, 257, -1, -1, -1, -1, 262, -1, -1, 257, -1, -1, -1, -1, 262, -1, -1, 257, -1, -1, -1, 257, 262, -1, 257, -1, -1, -1, -1, 262 };
    }

    static final short YYFINAL = 2;

    static final short YYMAXTOKEN = 274;

    static final String yyname[] = { "end-of-file", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "'('", "')'", "'*'", "'+'", null, "'-'", null, "'/'", null, null, null, null, null, null, null, null, null, null, null, null, "'<'", null, "'>'", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "'^'", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "'{'", null, "'}'", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "NUM", "WORD", "NEG", "FILE_COOKIE", "CAMERA", "COLOR", "NORM", "DIRECTION", "UP", "LEFT", "SCENE", "SPHERE", "PLANE", "GROUP", "POSITION", "ROTATE", "SCALE", "TRANSLATE" };

    static final String yyrule[] = { "$accept : world", "$$1 :", "world : FILE_COOKIE $$1 '{' camera scene '}'", "camera : CAMERA '{' camitems '}'", "camitems :", "camitems : camitems camitem", "camitem : pos", "camitem : DIRECTION vector", "camitem : UP vector", "camitem : LEFT vector", "scene : SCENE '{' objects '}'", "objects :", "objects : objects object", "object : group", "object : sphere", "object : plane", "group : GROUP '{' transforms objects '}'", "transforms :", "transforms : transforms transform", "transform : rotate", "transform : scale", "transform : translate", "rotate : ROTATE vector", "scale : SCALE vector", "translate : TRANSLATE vector", "sphere : SPHERE '{' pos scalar color '}'", "plane : PLANE '{' pos norm '}'", "norm : NORM vector", "pos : POSITION vector", "color : COLOR vector", "vector : '<' scalar scalar scalar '>'", "scalar : exp", "exp : NUM", "exp : exp '+' exp", "exp : exp '-' exp", "exp : exp '*' exp", "exp : exp '/' exp", "exp : '-' exp", "exp : exp '^' exp", "exp : '(' exp ')'" };

    StreamTokenizer st;

    boolean dflag;

    void pout(String s) {
        if (dflag) System.out.println(s);
    }

    void yyerror(String s) {
        System.out.println("par:" + s);
    }

    int yylex() {
        int tok;
        try {
            tok = st.nextToken();
        } catch (Exception e) {
            yyerror("tokenizer:" + e.toString());
            return -1;
        }
        if (tok == st.TT_EOF) return 0;
        yytext = st.sval;
        pout("yy:" + yytext);
        if (tok == st.TT_NUMBER) {
            yylval = new ParserVal(st.nval);
            return NUM;
        } else if (tok == st.TT_WORD) {
            if (yytext.equals("objectviewer")) return FILE_COOKIE; else if (yytext.equals("camera")) return CAMERA; else if (yytext.equals("position") || yytext.equals("pos")) return POSITION; else if (yytext.equals("normal") || yytext.equals("norm")) return NORM; else if (yytext.equals("color") || yytext.equals("col")) return COLOR; else if (yytext.equals("direction") || yytext.equals("dir")) return DIRECTION; else if (yytext.equals("up")) return UP; else if (yytext.equals("left")) return LEFT; else if (yytext.equals("scene")) return SCENE; else if (yytext.equals("sphere")) return SPHERE; else if (yytext.equals("plane")) return PLANE; else if (yytext.equals("group")) return GROUP; else if (yytext.equals("rotate")) return ROTATE; else if (yytext.equals("scale")) return SCALE; else if (yytext.equals("translate")) return TRANSLATE; else return yytext.charAt(0);
        }
        return tok;
    }

    int parseURL(URL url) {
        DataInputStream in;
        InputStream ins;
        int ret;
        dflag = true;
        try {
            ins = url.openStream();
            st = new StreamTokenizer(ins);
            st.slashSlashComments(true);
            st.slashStarComments(true);
        } catch (Exception e) {
            yyerror("could not open " + url.toString());
            return 0;
        }
        ret = yyparse();
        try {
            ins.close();
        } catch (Exception e) {
            yyerror("could not open source data");
            return 0;
        }
        return ret;
    }

    int parseURL(String urlspec) {
        int ret;
        try {
            URL url = new URL(urlspec);
            ret = parseURL(url);
        } catch (Exception e) {
            yyerror("invalid URL format");
            return 0;
        }
        return ret;
    }

    public static void main(String args[]) {
        Parser par = new Parser(false);
        par.parseURL("file:/home/becase/workspace/sevents/src/yacc/3d.db");
    }

    void yylexdebug(int state, int ch) {
        String s = null;
        if (ch < 0) ch = 0;
        if (ch <= YYMAXTOKEN) s = yyname[ch];
        if (s == null) s = "illegal-symbol";
        debug("state " + state + ", reading " + ch + " (" + s + ")");
    }

    int yyn;

    int yym;

    int yystate;

    String yys;

    int yyparse() {
        boolean doaction;
        init_stacks();
        yynerrs = 0;
        yyerrflag = 0;
        yychar = -1;
        yystate = 0;
        state_push(yystate);
        val_push(yylval);
        while (true) {
            doaction = true;
            if (yydebug) debug("loop");
            for (yyn = yydefred[yystate]; yyn == 0; yyn = yydefred[yystate]) {
                if (yydebug) debug("yyn:" + yyn + "  state:" + yystate + "  yychar:" + yychar);
                if (yychar < 0) {
                    yychar = yylex();
                    if (yydebug) debug(" next yychar:" + yychar);
                    if (yychar < 0) {
                        yychar = 0;
                        if (yydebug) yylexdebug(yystate, yychar);
                    }
                }
                yyn = yysindex[yystate];
                if ((yyn != 0) && (yyn += yychar) >= 0 && yyn <= YYTABLESIZE && yycheck[yyn] == yychar) {
                    if (yydebug) debug("state " + yystate + ", shifting to state " + yytable[yyn]);
                    yystate = yytable[yyn];
                    state_push(yystate);
                    val_push(yylval);
                    yychar = -1;
                    if (yyerrflag > 0) --yyerrflag;
                    doaction = false;
                    break;
                }
                yyn = yyrindex[yystate];
                if ((yyn != 0) && (yyn += yychar) >= 0 && yyn <= YYTABLESIZE && yycheck[yyn] == yychar) {
                    if (yydebug) debug("reduce");
                    yyn = yytable[yyn];
                    doaction = true;
                    break;
                } else {
                    if (yyerrflag == 0) {
                        yyerror("syntax error");
                        yynerrs++;
                    }
                    if (yyerrflag < 3) {
                        yyerrflag = 3;
                        while (true) {
                            if (stateptr < 0) {
                                yyerror("stack underflow. aborting...");
                                return 1;
                            }
                            yyn = yysindex[state_peek(0)];
                            if ((yyn != 0) && (yyn += YYERRCODE) >= 0 && yyn <= YYTABLESIZE && yycheck[yyn] == YYERRCODE) {
                                if (yydebug) debug("state " + state_peek(0) + ", error recovery shifting to state " + yytable[yyn] + " ");
                                yystate = yytable[yyn];
                                state_push(yystate);
                                val_push(yylval);
                                doaction = false;
                                break;
                            } else {
                                if (yydebug) debug("error recovery discarding state " + state_peek(0) + " ");
                                if (stateptr < 0) {
                                    yyerror("Stack underflow. aborting...");
                                    return 1;
                                }
                                state_pop();
                                val_pop();
                            }
                        }
                    } else {
                        if (yychar == 0) return 1;
                        if (yydebug) {
                            yys = null;
                            if (yychar <= YYMAXTOKEN) yys = yyname[yychar];
                            if (yys == null) yys = "illegal-symbol";
                            debug("state " + yystate + ", error recovery discards token " + yychar + " (" + yys + ")");
                        }
                        yychar = -1;
                    }
                }
            }
            if (!doaction) continue;
            yym = yylen[yyn];
            if (yydebug) debug("state " + yystate + ", reducing " + yym + " by rule " + yyn + " (" + yyrule[yyn] + ")");
            if (yym > 0) yyval = val_peek(yym - 1);
            switch(yyn) {
                case 1:
                    {
                        pout("got cookie");
                    }
                    break;
                case 2:
                    {
                        pout("parsed correctly");
                    }
                    break;
                case 16:
                    {
                        pout("group");
                    }
                    break;
                case 22:
                    {
                        pout("rotate");
                    }
                    break;
                case 23:
                    {
                        pout("scale");
                    }
                    break;
                case 24:
                    {
                        pout("translate");
                    }
                    break;
                case 25:
                    {
                        pout("sphere");
                    }
                    break;
                case 26:
                    {
                        pout("Plane");
                    }
                    break;
                case 27:
                    {
                        pout("norm");
                    }
                    break;
                case 28:
                    {
                        pout("pos");
                    }
                    break;
                case 29:
                    {
                        pout("color");
                    }
                    break;
                case 30:
                    {
                        pout("vector");
                    }
                    break;
                case 31:
                    {
                        pout("exp");
                    }
                    break;
                case 32:
                    {
                        yyval = val_peek(0);
                    }
                    break;
                case 33:
                    {
                        yyval = new ParserVal(val_peek(2).dval + val_peek(0).dval);
                    }
                    break;
                case 34:
                    {
                        yyval = new ParserVal(val_peek(2).dval - val_peek(0).dval);
                    }
                    break;
                case 35:
                    {
                        yyval = new ParserVal(val_peek(2).dval * val_peek(0).dval);
                    }
                    break;
                case 36:
                    {
                        yyval = new ParserVal(val_peek(2).dval / val_peek(0).dval);
                    }
                    break;
                case 37:
                    {
                        yyval = new ParserVal(-val_peek(0).dval);
                    }
                    break;
                case 38:
                    {
                        yyval = new ParserVal(Math.pow(val_peek(2).dval, val_peek(0).dval));
                    }
                    break;
                case 39:
                    {
                        yyval = val_peek(1);
                    }
                    break;
            }
            if (yydebug) debug("reduce");
            state_drop(yym);
            yystate = state_peek(0);
            val_drop(yym);
            yym = yylhs[yyn];
            if (yystate == 0 && yym == 0) {
                if (yydebug) debug("After reduction, shifting from state 0 to state " + YYFINAL + "");
                yystate = YYFINAL;
                state_push(YYFINAL);
                val_push(yyval);
                if (yychar < 0) {
                    yychar = yylex();
                    if (yychar < 0) yychar = 0;
                    if (yydebug) yylexdebug(yystate, yychar);
                }
                if (yychar == 0) break;
            } else {
                yyn = yygindex[yym];
                if ((yyn != 0) && (yyn += yystate) >= 0 && yyn <= YYTABLESIZE && yycheck[yyn] == yystate) yystate = yytable[yyn]; else yystate = yydgoto[yym];
                if (yydebug) debug("after reduction, shifting from state " + state_peek(0) + " to state " + yystate + "");
                state_push(yystate);
                val_push(yyval);
            }
        }
        return 0;
    }

    /**
 * A default run method, used for operating this parser
 * object in the background.  It is intended for extending Thread
 * or implementing Runnable.  Turn off with -Jnorun .
 */
    public void run() {
        yyparse();
    }

    /**
 * Default constructor.  Turn off with -Jnoconstruct .

 */
    public Parser() {
    }

    /**
 * Create a parser, setting the debug to true or false.
 * @param debugMe true for debugging, false for no debug.
 */
    public Parser(boolean debugMe) {
        yydebug = debugMe;
    }
}
