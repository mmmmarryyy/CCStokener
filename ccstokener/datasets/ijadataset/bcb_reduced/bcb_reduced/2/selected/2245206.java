package edu.cmu.sphinx.jsgf.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import edu.cmu.sphinx.jsgf.rule.*;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammar;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammarFactory;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammarManager;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;

@SuppressWarnings("all")
class JSGFEncoding {

    public String version;

    public String encoding;

    public String locale;

    JSGFEncoding(String version, String encoding, String locale) {
        this.version = version;
        this.encoding = encoding;
        this.locale = locale;
    }
}

public class JSGFParser implements JSGFParserConstants {

    static final String version = "1.0";

    static JSGFParser parser = null;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("JSGF Parser Version " + version + ":  Reading from standard input . . .");
            parser = new JSGFParser(System.in);
        } else if (args.length > 0) {
            System.out.println("JSGF Parser Version " + version + ":  Reading from file " + args[0] + " . . .");
            try {
                URL codeBase = null;
                File f = new File(".");
                String path = f.getAbsolutePath() + "/" + args[0];
                try {
                    codeBase = new URL("file:" + path);
                } catch (MalformedURLException e) {
                    System.out.println("Could not get URL for current directory " + e);
                    return;
                }
                BufferedInputStream i = new BufferedInputStream(codeBase.openStream(), 256);
                JSGFEncoding encoding = getJSGFEncoding(i);
                Reader rdr;
                if ((encoding != null) && (encoding.encoding != null)) {
                    System.out.println("Grammar Character Encoding \"" + encoding.encoding + "\"");
                    rdr = new InputStreamReader(i, encoding.encoding);
                } else {
                    if (encoding == null) System.out.println("WARNING: Grammar missing self identifying header");
                    rdr = new InputStreamReader(i);
                }
                parser = new JSGFParser(rdr);
            } catch (Exception e) {
                System.out.println("JSGF Parser Version " + version + ":  File " + args[0] + " not found.");
                return;
            }
        } else {
            System.out.println("JSGF Parser Version " + version + ":  Usage is one of:");
            System.out.println("         java JSGFParser < inputfile");
            System.out.println("OR");
            System.out.println("         java JSGFParser inputfile");
            return;
        }
        try {
            parser.GrammarUnit(new JSGFRuleGrammarFactory(new JSGFRuleGrammarManager()));
            System.out.println("JSGF Parser Version " + version + ":  JSGF Grammar parsed successfully.");
        } catch (ParseException e) {
            System.out.println("JSGF Parser Version " + version + ":  Encountered errors during parse." + e.getMessage());
        }
    }

    /**
     * newGrammarFromJSGF - Once JavaCC supports Readers we will change this
     */
    public static JSGFRuleGrammar newGrammarFromJSGF(InputStream i, JSGFRuleGrammarFactory factory) throws JSGFGrammarParseException {
        JSGFRuleGrammar grammar = null;
        if (parser == null) {
            parser = new JSGFParser(i);
        } else {
            parser.ReInit(i);
        }
        try {
            grammar = parser.GrammarUnit(factory);
            return grammar;
        } catch (ParseException e) {
            Token etoken = e.currentToken;
            JSGFGrammarParseException ge = new JSGFGrammarParseException(etoken.beginLine, etoken.beginColumn, "Grammar Error", e.getMessage());
            throw ge;
        }
    }

    /**
     * newGrammarFromJSGF - Once JavaCC supports Readers we will change this
     */
    public static JSGFRuleGrammar newGrammarFromJSGF(Reader i, JSGFRuleGrammarFactory factory) throws JSGFGrammarParseException {
        JSGFRuleGrammar grammar = null;
        if (parser == null) {
            parser = new JSGFParser(i);
        } else {
            parser.ReInit(i);
        }
        try {
            grammar = parser.GrammarUnit(factory);
            return grammar;
        } catch (ParseException e) {
            Token etoken = e.currentToken;
            JSGFGrammarParseException ge = new JSGFGrammarParseException(etoken.beginLine, etoken.beginColumn, "Grammar Error", e.getMessage());
            throw ge;
        }
    }

    private static JSGFEncoding getJSGFEncoding(BufferedInputStream is) {
        int i = 0;
        byte[] b = new byte[2];
        byte[] c = new byte[80];
        is.mark(256);
        try {
            if (is.read(b, 0, 2) != 2) {
                is.reset();
                return null;
            }
            if ((b[0] == 0x23) && (b[1] == 0x4A)) {
                i = 0;
                c[i++] = b[0];
                c[i++] = b[1];
                while (i < 80) {
                    if (is.read(b, 0, 1) != 1) {
                        is.reset();
                        return null;
                    }
                    if ((b[0] == 0x0A) || (b[0] == 0x0D)) break;
                    c[i++] = b[0];
                }
            } else if ((b[0] == 0x23) && (b[1] == 0x00)) {
                i = 0;
                c[i++] = b[0];
                while (i < 80) {
                    if (is.read(b, 0, 2) != 2) {
                        is.reset();
                        return null;
                    }
                    if (b[1] != 0) return null;
                    if ((b[0] == 0x0A) || (b[0] == 0x0D)) break;
                    c[i++] = b[0];
                }
            } else if ((b[0] == 0x00) && (b[1] == 0x23)) {
                i = 0;
                c[i++] = b[1];
                while (i < 80) {
                    if (is.read(b, 0, 2) != 2) {
                        is.reset();
                        return null;
                    }
                    if (b[0] != 0) return null;
                    if ((b[1] == 0x0A) || (b[1] == 0x0D)) break;
                    c[i++] = b[1];
                }
            }
        } catch (IOException ioe) {
            try {
                is.reset();
            } catch (IOException ioe2) {
            }
            return null;
        }
        if (i == 0) {
            try {
                is.reset();
            } catch (IOException ioe2) {
            }
            return null;
        }
        String estr = new String(c, 0, i);
        StringTokenizer st = new StringTokenizer(estr, " \t\n\r\f;");
        String id = null;
        String ver = null;
        String enc = null;
        String loc = null;
        if (st.hasMoreTokens()) id = st.nextToken();
        if (!id.equals("#JSGF")) {
            try {
                is.reset();
            } catch (IOException ioe2) {
            }
            return null;
        }
        if (st.hasMoreTokens()) ver = st.nextToken();
        if (st.hasMoreTokens()) enc = st.nextToken();
        if (st.hasMoreTokens()) loc = st.nextToken();
        return new JSGFEncoding(ver, enc, loc);
    }

    /**
     * newGrammarFromURL
     */
    public static JSGFRuleGrammar newGrammarFromJSGF(URL url, JSGFRuleGrammarFactory factory) throws JSGFGrammarParseException, IOException {
        Reader reader;
        BufferedInputStream stream = new BufferedInputStream(url.openStream(), 256);
        JSGFEncoding encoding = getJSGFEncoding(stream);
        if ((encoding != null) && (encoding.encoding != null)) {
            System.out.println("Grammar Character Encoding \"" + encoding.encoding + "\"");
            reader = new InputStreamReader(stream, encoding.encoding);
        } else {
            if (encoding == null) System.out.println("WARNING: Grammar missing self identifying header");
            reader = new InputStreamReader(stream);
        }
        return newGrammarFromJSGF(reader, factory);
    }

    /**
     * ruleForJSGF
     */
    public static JSGFRule ruleForJSGF(String text) {
        JSGFRule r = null;
        try {
            StringReader sread = new StringReader(text);
            if (parser == null) parser = new JSGFParser(sread); else parser.ReInit(sread);
            r = parser.alternatives();
        } catch (ParseException e) {
            System.out.println("JSGF Parser Version " + version + ":  Encountered errors during parse.");
        }
        return r;
    }

    /**
    * extract @keywords from documentation comments
    */
    static void extractKeywords(JSGFRuleGrammar grammar, String rname, String comment) {
        int i = 0;
        while ((i = comment.indexOf("@example ", i) + 9) > 9) {
            int j = Math.max(comment.indexOf('\r', i), comment.indexOf('\n', i));
            if (j < 0) {
                j = comment.length();
                if (comment.endsWith(("*/"))) j -= 2;
            }
            grammar.addSampleSentence(rname, comment.substring(i, j).trim());
            i = j + 1;
        }
    }

    public final JSGFRuleGrammar GrammarUnit(JSGFRuleGrammarFactory factory) throws ParseException {
        JSGFRuleGrammar grammar = null;
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case IDENTIFIER:
                IdentHeader();
                break;
            default:
                jj_la1[0] = jj_gen;
                ;
        }
        grammar = GrammarDeclaration(factory);
        label_1: while (true) {
            switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case IMPORT:
                    ;
                    break;
                default:
                    jj_la1[1] = jj_gen;
                    break label_1;
            }
            ImportDeclaration(grammar);
        }
        label_2: while (true) {
            switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case PUBLIC:
                case 28:
                    ;
                    break;
                default:
                    jj_la1[2] = jj_gen;
                    break label_2;
            }
            RuleDeclaration(grammar);
        }
        jj_consume_token(0);
        {
            if (true) return grammar;
        }
        throw new Error("Missing return statement in function");
    }

    public final JSGFRuleGrammar GrammarDeclaration(JSGFRuleGrammarFactory factory) throws ParseException {
        String s;
        JSGFRuleGrammar grammar = null;
        Token t = null;
        t = jj_consume_token(GRAMMAR);
        s = Name();
        jj_consume_token(26);
        grammar = factory.newGrammar(s);
        if (grammar != null && t != null && t.specialToken != null) {
            if (t.specialToken.image != null && t.specialToken.image.startsWith("/**")) {
                JSGFRuleGrammar JG = (JSGFRuleGrammar) grammar;
                JG.addGrammarDocComment(t.specialToken.image);
            }
        }
        {
            if (true) return grammar;
        }
        throw new Error("Missing return statement in function");
    }

    public final void IdentHeader() throws ParseException {
        jj_consume_token(IDENTIFIER);
        jj_consume_token(27);
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case IDENTIFIER:
                jj_consume_token(IDENTIFIER);
                switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                    case IDENTIFIER:
                        jj_consume_token(IDENTIFIER);
                        break;
                    default:
                        jj_la1[3] = jj_gen;
                        ;
                }
                break;
            default:
                jj_la1[4] = jj_gen;
                ;
        }
        jj_consume_token(26);
    }

    public final void ImportDeclaration(JSGFRuleGrammar grammar) throws ParseException {
        boolean all = false;
        String name;
        Token t = null;
        t = jj_consume_token(IMPORT);
        jj_consume_token(28);
        name = Name();
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case 29:
                jj_consume_token(29);
                jj_consume_token(30);
                all = true;
                break;
            default:
                jj_la1[5] = jj_gen;
                ;
        }
        jj_consume_token(31);
        jj_consume_token(26);
        if (all) name = name + ".*";
        JSGFRuleName r = new JSGFRuleName(name);
        if (grammar != null) {
            grammar.addImport(r);
            if (grammar instanceof JSGFRuleGrammar && t != null && t.specialToken != null) {
                if (t.specialToken.image != null && t.specialToken.image.startsWith("/**")) {
                    JSGFRuleGrammar JG = (JSGFRuleGrammar) grammar;
                    JG.addImportDocComment(r, t.specialToken.image);
                }
            }
        }
    }

    public final String Name() throws ParseException {
        Token t1, t2;
        StringBuilder sb = new StringBuilder();
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case IDENTIFIER:
                t1 = jj_consume_token(IDENTIFIER);
                break;
            case PUBLIC:
                t1 = jj_consume_token(PUBLIC);
                break;
            case IMPORT:
                t1 = jj_consume_token(IMPORT);
                break;
            case GRAMMAR:
                t1 = jj_consume_token(GRAMMAR);
                break;
            default:
                jj_la1[6] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        sb.append(t1.image);
        label_3: while (true) {
            if (jj_2_1(2)) {
                ;
            } else {
                break label_3;
            }
            jj_consume_token(29);
            t2 = jj_consume_token(IDENTIFIER);
            sb.append('.');
            sb.append(t2.image);
        }
        {
            if (true) return sb.toString();
        }
        throw new Error("Missing return statement in function");
    }

    public final void RuleDeclaration(JSGFRuleGrammar grammar) throws ParseException {
        boolean pub = false;
        String s;
        JSGFRule r;
        Token t = null;
        Token t1 = null;
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case PUBLIC:
                t = jj_consume_token(PUBLIC);
                pub = true;
                break;
            default:
                jj_la1[7] = jj_gen;
                ;
        }
        t1 = jj_consume_token(28);
        s = ruleDef();
        jj_consume_token(31);
        jj_consume_token(32);
        r = alternatives();
        jj_consume_token(26);
        try {
            if (grammar != null) {
                grammar.setRule(s, r, pub);
                String docComment = null;
                if ((t != null) && (t.specialToken != null) && (t.specialToken.image != null)) docComment = t.specialToken.image; else if ((t1 != null) && (t1.specialToken != null) && (t1.specialToken.image != null)) docComment = t1.specialToken.image;
                if (docComment != null && docComment.startsWith("/**")) {
                    extractKeywords(grammar, s, docComment);
                    grammar.addRuleDocComment(s, docComment);
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR SETTING JSGFRule " + s);
        }
    }

    public final JSGFRuleAlternatives alternatives() throws ParseException {
        ArrayList<JSGFRule> ruleList = new ArrayList<JSGFRule>();
        JSGFRule r;
        float w;
        ArrayList<Float> weights = new ArrayList<Float>();
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case GRAMMAR:
            case IMPORT:
            case PUBLIC:
            case INTEGER_LITERAL:
            case FLOATING_POINT_LITERAL:
            case STRING_LITERAL:
            case IDENTIFIER:
            case 28:
            case 36:
            case 38:
                r = sequence();
                ruleList.add(r);
                label_4: while (true) {
                    switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                        case 33:
                            ;
                            break;
                        default:
                            jj_la1[8] = jj_gen;
                            break label_4;
                    }
                    jj_consume_token(33);
                    r = sequence();
                    ruleList.add(r);
                }
                break;
            case 34:
                w = weight();
                r = sequence();
                ruleList.add(r);
                weights.add(w);
                label_5: while (true) {
                    jj_consume_token(33);
                    w = weight();
                    r = sequence();
                    ruleList.add(r);
                    weights.add(w);
                    switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                        case 33:
                            ;
                            break;
                        default:
                            jj_la1[9] = jj_gen;
                            break label_5;
                    }
                }
                break;
            default:
                jj_la1[10] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        JSGFRuleAlternatives ra = new JSGFRuleAlternatives(ruleList);
        if (weights.size() > 0) {
            ra.setWeights(weights);
        }
        {
            if (true) return ra;
        }
        throw new Error("Missing return statement in function");
    }

    public final String ruleDef() throws ParseException {
        Token t;
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case IDENTIFIER:
                t = jj_consume_token(IDENTIFIER);
                break;
            case INTEGER_LITERAL:
                t = jj_consume_token(INTEGER_LITERAL);
                break;
            case PUBLIC:
                t = jj_consume_token(PUBLIC);
                break;
            case IMPORT:
                t = jj_consume_token(IMPORT);
                break;
            case GRAMMAR:
                t = jj_consume_token(GRAMMAR);
                break;
            default:
                jj_la1[11] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        {
            if (true) return t.image;
        }
        throw new Error("Missing return statement in function");
    }

    public final JSGFRuleSequence sequence() throws ParseException {
        JSGFRule JSGFRule;
        ArrayList<JSGFRule> ruleList = new ArrayList<JSGFRule>();
        label_6: while (true) {
            JSGFRule = item();
            ruleList.add(JSGFRule);
            switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case GRAMMAR:
                case IMPORT:
                case PUBLIC:
                case INTEGER_LITERAL:
                case FLOATING_POINT_LITERAL:
                case STRING_LITERAL:
                case IDENTIFIER:
                case 28:
                case 36:
                case 38:
                    ;
                    break;
                default:
                    jj_la1[12] = jj_gen;
                    break label_6;
            }
        }
        {
            if (true) return new JSGFRuleSequence(ruleList);
        }
        throw new Error("Missing return statement in function");
    }

    public final float weight() throws ParseException {
        Token t;
        jj_consume_token(34);
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case FLOATING_POINT_LITERAL:
                t = jj_consume_token(FLOATING_POINT_LITERAL);
                break;
            case INTEGER_LITERAL:
                t = jj_consume_token(INTEGER_LITERAL);
                break;
            default:
                jj_la1[13] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        jj_consume_token(34);
        {
            if (true) return Float.valueOf(t.image).floatValue();
        }
        throw new Error("Missing return statement in function");
    }

    public final JSGFRule item() throws ParseException {
        JSGFRule r;
        ArrayList<String> tags = null;
        int count = -1;
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case GRAMMAR:
            case IMPORT:
            case PUBLIC:
            case INTEGER_LITERAL:
            case FLOATING_POINT_LITERAL:
            case STRING_LITERAL:
            case IDENTIFIER:
            case 28:
                switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                    case GRAMMAR:
                    case IMPORT:
                    case PUBLIC:
                    case INTEGER_LITERAL:
                    case FLOATING_POINT_LITERAL:
                    case STRING_LITERAL:
                    case IDENTIFIER:
                        r = terminal();
                        break;
                    case 28:
                        r = ruleRef();
                        break;
                    default:
                        jj_la1[14] = jj_gen;
                        jj_consume_token(-1);
                        throw new ParseException();
                }
                switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                    case 30:
                    case 35:
                        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                            case 30:
                                jj_consume_token(30);
                                count = JSGFRuleCount.ZERO_OR_MORE;
                                break;
                            case 35:
                                jj_consume_token(35);
                                count = JSGFRuleCount.ONCE_OR_MORE;
                                break;
                            default:
                                jj_la1[15] = jj_gen;
                                jj_consume_token(-1);
                                throw new ParseException();
                        }
                        break;
                    default:
                        jj_la1[16] = jj_gen;
                        ;
                }
                switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                    case TAG:
                        tags = tags();
                        break;
                    default:
                        jj_la1[17] = jj_gen;
                        ;
                }
                break;
            case 36:
                jj_consume_token(36);
                r = alternatives();
                jj_consume_token(37);
                switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                    case 30:
                    case 35:
                        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                            case 30:
                                jj_consume_token(30);
                                count = JSGFRuleCount.ZERO_OR_MORE;
                                break;
                            case 35:
                                jj_consume_token(35);
                                count = JSGFRuleCount.ONCE_OR_MORE;
                                break;
                            default:
                                jj_la1[18] = jj_gen;
                                jj_consume_token(-1);
                                throw new ParseException();
                        }
                        break;
                    default:
                        jj_la1[19] = jj_gen;
                        ;
                }
                switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                    case TAG:
                        tags = tags();
                        break;
                    default:
                        jj_la1[20] = jj_gen;
                        ;
                }
                break;
            case 38:
                jj_consume_token(38);
                r = alternatives();
                jj_consume_token(39);
                count = JSGFRuleCount.OPTIONAL;
                switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                    case TAG:
                        tags = tags();
                        break;
                    default:
                        jj_la1[21] = jj_gen;
                        ;
                }
                break;
            default:
                jj_la1[22] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        if (count != -1) r = new JSGFRuleCount(r, count);
        if (tags != null) {
            for (String tag : tags) {
                if (tag.charAt(0) == '{') {
                    tag = tag.substring(1, tag.length() - 1);
                    tag = tag.replace('\\', ' ');
                }
                r = new JSGFRuleTag(r, tag);
            }
        }
        {
            if (true) return r;
        }
        throw new Error("Missing return statement in function");
    }

    public final ArrayList<String> tags() throws ParseException {
        Token token;
        ArrayList<String> tags = new ArrayList<String>();
        label_7: while (true) {
            token = jj_consume_token(TAG);
            tags.add(token.image);
            switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case TAG:
                    ;
                    break;
                default:
                    jj_la1[23] = jj_gen;
                    break label_7;
            }
        }
        {
            if (true) return tags;
        }
        throw new Error("Missing return statement in function");
    }

    public final JSGFRule terminal() throws ParseException {
        Token t;
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case IDENTIFIER:
                t = jj_consume_token(IDENTIFIER);
                break;
            case STRING_LITERAL:
                t = jj_consume_token(STRING_LITERAL);
                break;
            case INTEGER_LITERAL:
                t = jj_consume_token(INTEGER_LITERAL);
                break;
            case FLOATING_POINT_LITERAL:
                t = jj_consume_token(FLOATING_POINT_LITERAL);
                break;
            case PUBLIC:
                t = jj_consume_token(PUBLIC);
                break;
            case IMPORT:
                t = jj_consume_token(IMPORT);
                break;
            case GRAMMAR:
                t = jj_consume_token(GRAMMAR);
                break;
            default:
                jj_la1[24] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        String tn = t.image;
        if (tn.startsWith("\"") && tn.endsWith("\"")) tn = tn.substring(1, tn.length() - 1);
        JSGFRuleToken rt = new JSGFRuleToken(tn);
        {
            if (true) return rt;
        }
        throw new Error("Missing return statement in function");
    }

    public final JSGFRuleName ruleRef() throws ParseException {
        String s;
        jj_consume_token(28);
        s = Name();
        jj_consume_token(31);
        JSGFRuleName rn = new JSGFRuleName(s);
        {
            if (true) return rn;
        }
        throw new Error("Missing return statement in function");
    }

    public final JSGFRuleName importRef() throws ParseException {
        String s;
        boolean all = false;
        jj_consume_token(28);
        s = Name();
        switch((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case 29:
                jj_consume_token(29);
                jj_consume_token(30);
                all = true;
                break;
            default:
                jj_la1[25] = jj_gen;
                ;
        }
        jj_consume_token(31);
        if (all) s = s + ".*";
        JSGFRuleName rn = new JSGFRuleName(s);
        {
            if (true) return rn;
        }
        throw new Error("Missing return statement in function");
    }

    private boolean jj_2_1(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_1();
        } catch (LookaheadSuccess ls) {
            return true;
        } finally {
            jj_save(0, xla);
        }
    }

    private boolean jj_3_1() {
        if (jj_scan_token(29)) return true;
        if (jj_scan_token(IDENTIFIER)) return true;
        return false;
    }

    /** Generated Token Manager. */
    public JSGFParserTokenManager token_source;

    JavaCharStream jj_input_stream;

    /** Current token. */
    public Token token;

    /** Next token. */
    public Token jj_nt;

    private int jj_ntk;

    private Token jj_scanpos, jj_lastpos;

    private int jj_la;

    private int jj_gen;

    private final int[] jj_la1 = new int[26];

    private static int[] jj_la1_0;

    private static int[] jj_la1_1;

    static {
        jj_la1_init_0();
        jj_la1_init_1();
    }

    private static void jj_la1_init_0() {
        jj_la1_0 = new int[] { 0x800000, 0x4000, 0x10008000, 0x800000, 0x800000, 0x20000000, 0x80e000, 0x8000, 0x0, 0x0, 0x10a5e000, 0x81e000, 0x10a5e000, 0x50000, 0x10a5e000, 0x40000000, 0x40000000, 0x400000, 0x40000000, 0x40000000, 0x400000, 0x400000, 0x10a5e000, 0x400000, 0xa5e000, 0x20000000 };
    }

    private static void jj_la1_init_1() {
        jj_la1_1 = new int[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x2, 0x2, 0x54, 0x0, 0x50, 0x0, 0x0, 0x8, 0x8, 0x0, 0x8, 0x8, 0x0, 0x0, 0x50, 0x0, 0x0, 0x0 };
    }

    private final JJCalls[] jj_2_rtns = new JJCalls[1];

    private boolean jj_rescan = false;

    private int jj_gc = 0;

    /** Constructor with InputStream. */
    public JSGFParser(java.io.InputStream stream) {
        this(stream, null);
    }

    /** Constructor with InputStream and supplied encoding */
    public JSGFParser(java.io.InputStream stream, String encoding) {
        try {
            jj_input_stream = new JavaCharStream(stream, encoding, 1, 1);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source = new JSGFParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 26; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    /** Reinitialize. */
    public void ReInit(java.io.InputStream stream) {
        ReInit(stream, null);
    }

    /** Reinitialize. */
    public void ReInit(java.io.InputStream stream, String encoding) {
        try {
            jj_input_stream.ReInit(stream, encoding, 1, 1);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 26; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    /** Constructor. */
    public JSGFParser(java.io.Reader stream) {
        jj_input_stream = new JavaCharStream(stream, 1, 1);
        token_source = new JSGFParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 26; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    /** Reinitialize. */
    public void ReInit(java.io.Reader stream) {
        jj_input_stream.ReInit(stream, 1, 1);
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 26; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    /** Constructor with generated Token Manager. */
    public JSGFParser(JSGFParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 26; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    /** Reinitialize. */
    public void ReInit(JSGFParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 26; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    private Token jj_consume_token(int kind) throws ParseException {
        Token oldToken;
        if ((oldToken = token).next != null) token = token.next; else token = token.next = token_source.getNextToken();
        jj_ntk = -1;
        if (token.kind == kind) {
            jj_gen++;
            if (++jj_gc > 100) {
                jj_gc = 0;
                for (int i = 0; i < jj_2_rtns.length; i++) {
                    JJCalls c = jj_2_rtns[i];
                    while (c != null) {
                        if (c.gen < jj_gen) c.first = null;
                        c = c.next;
                    }
                }
            }
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }

    private static final class LookaheadSuccess extends java.lang.Error {
    }

    private final LookaheadSuccess jj_ls = new LookaheadSuccess();

    private boolean jj_scan_token(int kind) {
        if (jj_scanpos == jj_lastpos) {
            jj_la--;
            if (jj_scanpos.next == null) {
                jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
            } else {
                jj_lastpos = jj_scanpos = jj_scanpos.next;
            }
        } else {
            jj_scanpos = jj_scanpos.next;
        }
        if (jj_rescan) {
            int i = 0;
            Token tok = token;
            while (tok != null && tok != jj_scanpos) {
                i++;
                tok = tok.next;
            }
            if (tok != null) jj_add_error_token(kind, i);
        }
        if (jj_scanpos.kind != kind) return true;
        if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
        return false;
    }

    /** Get the next Token. */
    public final Token getNextToken() {
        if (token.next != null) token = token.next; else token = token.next = token_source.getNextToken();
        jj_ntk = -1;
        jj_gen++;
        return token;
    }

    /** Get the specific Token. */
    public final Token getToken(int index) {
        Token t = token;
        for (int i = 0; i < index; i++) {
            if (t.next != null) t = t.next; else t = t.next = token_source.getNextToken();
        }
        return t;
    }

    private int jj_ntk() {
        if ((jj_nt = token.next) == null) return (jj_ntk = (token.next = token_source.getNextToken()).kind); else return (jj_ntk = jj_nt.kind);
    }

    private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();

    private int[] jj_expentry;

    private int jj_kind = -1;

    private int[] jj_lasttokens = new int[100];

    private int jj_endpos;

    private void jj_add_error_token(int kind, int pos) {
        if (pos >= 100) return;
        if (pos == jj_endpos + 1) {
            jj_lasttokens[jj_endpos++] = kind;
        } else if (jj_endpos != 0) {
            jj_expentry = new int[jj_endpos];
            for (int i = 0; i < jj_endpos; i++) {
                jj_expentry[i] = jj_lasttokens[i];
            }
            jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext(); ) {
                int[] oldentry = (int[]) (it.next());
                if (oldentry.length == jj_expentry.length) {
                    for (int i = 0; i < jj_expentry.length; i++) {
                        if (oldentry[i] != jj_expentry[i]) {
                            continue jj_entries_loop;
                        }
                    }
                    jj_expentries.add(jj_expentry);
                    break jj_entries_loop;
                }
            }
            if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
        }
    }

    /** Generate ParseException. */
    public ParseException generateParseException() {
        jj_expentries.clear();
        boolean[] la1tokens = new boolean[40];
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 26; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                    if ((jj_la1_1[i] & (1 << j)) != 0) {
                        la1tokens[32 + j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 40; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.add(jj_expentry);
            }
        }
        jj_endpos = 0;
        jj_rescan_token();
        jj_add_error_token(0, 0);
        int[][] exptokseq = new int[jj_expentries.size()][];
        for (int i = 0; i < jj_expentries.size(); i++) {
            exptokseq[i] = jj_expentries.get(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

    /** Enable tracing. */
    public final void enable_tracing() {
    }

    /** Disable tracing. */
    public final void disable_tracing() {
    }

    private void jj_rescan_token() {
        jj_rescan = true;
        for (int i = 0; i < 1; i++) {
            try {
                JJCalls p = jj_2_rtns[i];
                do {
                    if (p.gen > jj_gen) {
                        jj_la = p.arg;
                        jj_lastpos = jj_scanpos = p.first;
                        switch(i) {
                            case 0:
                                jj_3_1();
                                break;
                        }
                    }
                    p = p.next;
                } while (p != null);
            } catch (LookaheadSuccess ls) {
            }
        }
        jj_rescan = false;
    }

    private void jj_save(int index, int xla) {
        JJCalls p = jj_2_rtns[index];
        while (p.gen > jj_gen) {
            if (p.next == null) {
                p = p.next = new JJCalls();
                break;
            }
            p = p.next;
        }
        p.gen = jj_gen + xla - jj_la;
        p.first = token;
        p.arg = xla;
    }

    static final class JJCalls {

        int gen;

        Token first;

        int arg;

        JJCalls next;
    }
}
