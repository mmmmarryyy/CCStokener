package org.asianclassics.unicoder;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
* Interfaces between Extended Wylie and the TibetanMachineWeb fonts.
* To do this this must first read the code table, which lives in
* "tibwn.ini", and which must be found in the same directory as this
* class.  Note that WylieWord has its own copy of this file, so edit
* both or neither.
*
* <p>In addition, this class optionally loads the TibetanMachineWeb
* fonts manually via {@link #readInTMWFontFiles()}.  When we do that,
* it means that users don't have to install the fonts on their
* systems, so installation of Jskad becomes easier.
* @author Edward Garrett, Tibetan and Himalayan Digital Library
* @author David Chandler
*/
public class TibetanMachineWeb implements THDLWylieConstants {

    /** This addresses bug 624133, "Input freezes after impossible
     *  character".  The input sequences that are valid in Extended
     *  Wylie.  For example, "Sh" will be in this container, but "S"
     *  will not be. */
    private static Trie validInputSequences = new Trie();

    /** needed because a Trie cannot have a null value associated with
     *  a key */
    private static final String anyOldObjectWillDo = "this placeholder is useful for debugging; we need a nonnull Object anyway";

    private static Set charSet = null;

    private static Set tibSet = null;

    private static Set sanskritStackSet = null;

    private static Set numberSet = null;

    private static Set vowelSet = null;

    private static int maxEwtsVowelLength = -1;

    private static Set puncSet = null;

    private static Set topSet = null;

    private static Set leftSet = null;

    private static Set rightSet = null;

    private static Set farRightSet = null;

    private static Map tibHash = new HashMap();

    private static Map binduMap = new HashMap();

    private static String[][] toHashKey = new String[11][95];

    private static DuffCode[][] TMtoTMW = new DuffCode[5][255 - 32];

    private static DuffCode[][] TMWtoTM = new DuffCode[10][127 - 32];

    private static String[][] TMWtoUnicode = new String[10][127 - 32];

    /** For mapping single codepoints U+0F00..U+0FFF to TMW.  This
        won't handle 0F00, 0F02, 0F03, or 0F0E, which are made by
        using multiple glyphs from TMW, but it handles all the rest.
        It handles U+0F90-U+0FBC rather poorly, in that you have to
        use special formatting to get those right (FIXME: warn
        whenever they're used). */
    private static DuffCode[][] UnicodeToTMW = new DuffCode[256][1];

    /** For mapping codepoints U+F021..U+0FFF to TMW. */
    private static DuffCode[][] NonUnicodeToTMW = new DuffCode[256][1];

    private static String fileName = "tibwn.ini";

    private static final String DELIMITER = "~";

    /** vowels that appear over the glyph: */
    private static Set top_vowels;

    private static boolean hasDisambiguatingKey;

    private static char disambiguating_key;

    private static boolean hasSanskritStackingKey;

    private static boolean hasTibetanStackingKey;

    private static boolean isStackingMedial;

    private static char stacking_key;

    private static boolean isAChenRequiredBeforeVowel;

    private static boolean isAChungConsonant;

    private static boolean hasAVowel;

    private static String aVowel;

    public static final String[] tmFontNames = { null, "TibetanMachine".intern(), "TibetanMachineSkt1".intern(), "TibetanMachineSkt2".intern(), "TibetanMachineSkt3".intern(), "TibetanMachineSkt4".intern() };

    public static final String[] tmwFontNames = { null, "TibetanMachineWeb".intern(), "TibetanMachineWeb1".intern(), "TibetanMachineWeb2".intern(), "TibetanMachineWeb3".intern(), "TibetanMachineWeb4".intern(), "TibetanMachineWeb5".intern(), "TibetanMachineWeb6".intern(), "TibetanMachineWeb7".intern(), "TibetanMachineWeb8".intern(), "TibetanMachineWeb9".intern() };

    /**
* represents where in an array of DuffCodes you
* find the TibetanMachine equivalence of a glyph
*/
    public static final int TM = 0;

    /**
* represents where in an array of DuffCodes you
* find the reduced character equivalent of a TMW glyph
*/
    public static final int REDUCED_C = 1;

    /**
* represents where in an array of DuffCodes you
* find the TibetanMachineWeb glyph
*/
    public static final int TMW = 2;

    /**
* represents where in an array of DuffCodes you
* find the gigu value for a given glyph
*/
    public static final int VOWEL_i = 3;

    /**
* represents where in an array of DuffCodes you
* find the zhebju value for a given glyph
*/
    public static final int VOWEL_u = 4;

    /**
* represents where in an array of DuffCodes you
* find the drengbu value for a given glyph
*/
    public static final int VOWEL_e = 5;

    /**
* represents where in an array of DuffCodes you
* find the naro value for a given glyph
*/
    public static final int VOWEL_o = 6;

    /**
* represents where in an array of DuffCodes you
* find the achung value for a given glyph
*/
    public static final int VOWEL_A = 7;

    /**
* represents where in an array of DuffCodes you
* find the achung + zhebju value for a given glyph
*/
    public static final int VOWEL_U = 8;

    /**
* represents where in an array of DuffCodes you
* find the Unicode equivalence of a given glyph
*/
    public static final int UNICODE = 9;

    /**
* represents where in an array of DuffCodes you
* find the half height equivalence of a given glyph
*/
    public static final int HALF_C = 10;

    /** comma-delimited list of supported Tibetan consonants: */
    private static final String tibetanConsonants = "k,kh,g,ng,c,ch,j,ny,t,th,d,n,p,ph,b,m,ts,tsh,dz,w,zh,z,',y,r,l,sh,s,h,a";

    /** comma-delimited list of supported non-Tibetan consonants, such
     *  as Sanskrit consonants: */
    private static final String otherConsonants = "T,Th,D,N,Sh,v,f";

    /** comma-delimited list of supported numbers (superscribed,
        subscribed, normal, half-numerals): */
    private static final String numbers = "0,1,2,3,4,5,6,7,8,9";

    /** comma-delimited list of supported punctuation and
        miscellaneous characters: */
    private static final String others = "_, ,/,|,!,:,;,@,#,$,%,(,),H,M,&,@#,?,=,{,}, ,~X,X";

    /** comma-delimited list of supported vowels: */
    private static final String vowels = "a,i,u,e,o,I,U,ai,au,A,-i,-I";

    /** comma-delimited list of head letters (superscribed letters) */
    private static final String tops = "r,s,l";

    /** comma-delimited list of prefixes */
    private static final String lefts = "g,d,b,m,'";

    /** comma-delimited list of suffixes */
    private static final String rights = "g,ng,d,n,b,m,r,l,s,',T";

    /** comma-delimited list of postsuffixes.  nga was here in the
     *  past, according to Edward, to handle cases like ya'ng.  pa'am
     *  wasn't considered, but had it been, ma probably would've gone
     *  here too.  We now handle 'am, 'ang, etc. specially, so now
     *  this set is now just the postsuffixes.  */
    private static final String farrights = "d,s";

    static {
        readData();
    }

    /** If the TMW font files are resources associated with this
     *  class, those font files are loaded.  This means that the user
     *  need not install the fonts on their system, but it does make
     *  the JAR bigger and takes time at startup.
     *  @return true upon successful loading, false otherwise */
    private static boolean readInTMWFontFiles() {
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn1.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn2.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn3.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn4.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn5.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn6.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn7.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn8.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachineWeb/timwn9.ttf")) return false;
        return true;
    }

    /** If the TM font files are resources associated with this
     *  class, those font files are loaded.  This means that the user
     *  need not install the fonts on their system, but it does make
     *  the JAR bigger and takes time at startup.
     *  @return true upon successful loading, false otherwise */
    private static boolean readInTMFontFiles() {
        if (!readInFontFile("/Fonts/TibetanMachine/Timn.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachine/Tims1.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachine/Tims2.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachine/Tims3.ttf")) return false;
        if (!readInFontFile("/Fonts/TibetanMachine/Tims4.ttf")) return false;
        return true;
    }

    /** If the TMW font file at the given path is a resource
     *  associated with this class, that font file is loaded.
     *  @param path a path within the JAR containing this class file
     *  @return true upon successful loading, false otherwise */
    private static boolean readInFontFile(String path) {
        try {
            InputStream is = TibetanMachineWeb.class.getResourceAsStream(path);
            if (null == is) {
                return false;
            }
            Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (Exception e) {
            e.printStackTrace();
            ThdlDebug.noteIffyCode();
            return false;
        }
        return true;
    }

    /** Returns the next token in st with the first occurrence of
        __TILDE__ replaced with ~.  Needed because the DELIMITER is ~.
        Appends the escaped token to sb iff an escape sequence
        occurred. */
    private static String getEscapedToken(StringTokenizer st, StringBuffer sb) {
        String unescaped = st.nextToken();
        int start;
        if ((start = unescaped.indexOf("__TILDE__")) >= 0) {
            StringBuffer x = new StringBuffer(unescaped);
            x.replace(start, "__TILDE__".length(), "~");
            sb.append(x.toString());
            return x.toString();
        } else {
            return unescaped;
        }
    }

    /**
* This method reads the data file ("tibwn.ini"), constructs
* the character, punctuation, and vowel lists, as well as
* performing other acts of initialization.
*/
    private static void readData() {
        StringTokenizer sTok;
        topSet = new HashSet();
        sTok = new StringTokenizer(tops, ",");
        while (sTok.hasMoreTokens()) topSet.add(sTok.nextToken());
        leftSet = new HashSet();
        sTok = new StringTokenizer(lefts, ",");
        while (sTok.hasMoreTokens()) leftSet.add(sTok.nextToken());
        rightSet = new HashSet();
        sTok = new StringTokenizer(rights, ",");
        while (sTok.hasMoreTokens()) rightSet.add(sTok.nextToken());
        farRightSet = new HashSet();
        sTok = new StringTokenizer(farrights, ",");
        while (sTok.hasMoreTokens()) farRightSet.add(sTok.nextToken());
        vowelSet = new HashSet();
        sTok = new StringTokenizer(vowels, ",");
        while (sTok.hasMoreTokens()) {
            String ntk;
            vowelSet.add(ntk = sTok.nextToken());
            if (maxEwtsVowelLength < ntk.length()) maxEwtsVowelLength = ntk.length();
            validInputSequences.put(ntk, anyOldObjectWillDo);
        }
        puncSet = new HashSet();
        sTok = new StringTokenizer(others, ",");
        while (sTok.hasMoreTokens()) {
            String ntk;
            puncSet.add(ntk = sTok.nextToken());
            validInputSequences.put(ntk, anyOldObjectWillDo);
        }
        charSet = new HashSet();
        tibSet = new HashSet();
        sTok = new StringTokenizer(tibetanConsonants, ",");
        while (sTok.hasMoreTokens()) {
            String ntk;
            charSet.add(ntk = sTok.nextToken());
            tibSet.add(ntk);
            validInputSequences.put(ntk, anyOldObjectWillDo);
        }
        sanskritStackSet = new HashSet();
        sTok = new StringTokenizer(otherConsonants, ",");
        while (sTok.hasMoreTokens()) {
            String ntk;
            charSet.add(ntk = sTok.nextToken());
            sanskritStackSet.add(ntk);
            validInputSequences.put(ntk, anyOldObjectWillDo);
        }
        numberSet = new HashSet();
        sTok = new StringTokenizer(numbers, ",");
        while (sTok.hasMoreTokens()) {
            String ntk;
            charSet.add(ntk = sTok.nextToken());
            numberSet.add(ntk);
            validInputSequences.put(ntk, anyOldObjectWillDo);
        }
        charSet.add("Y");
        charSet.add("R");
        charSet.add("W");
        validInputSequences.put("Y", anyOldObjectWillDo);
        validInputSequences.put("R", anyOldObjectWillDo);
        validInputSequences.put("W", anyOldObjectWillDo);
        sTok = null;
        top_vowels = new HashSet();
        top_vowels.add(i_VOWEL);
        top_vowels.add(e_VOWEL);
        top_vowels.add(o_VOWEL);
        top_vowels.add(ai_VOWEL);
        top_vowels.add(au_VOWEL);
        top_vowels.add(reverse_i_VOWEL);
        try {
            URL url = TibetanMachineWeb.class.getResource(fileName);
            if (url == null) {
                System.err.println("Cannot find " + fileName + "; aborting.");
                System.exit(1);
            }
            InputStreamReader isr = new InputStreamReader(url.openStream());
            BufferedReader in = new BufferedReader(isr);
            System.out.println("Reading Tibetan Machine Web code table " + fileName);
            String line;
            boolean hashOn = false;
            boolean isTibetan = false;
            boolean isSanskrit = false;
            boolean ignore = false;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("<?")) {
                    if (line.equalsIgnoreCase("<?Consonants?>")) {
                        isSanskrit = false;
                        isTibetan = true;
                        hashOn = false;
                        ignore = false;
                        do {
                            line = in.readLine();
                        } while (line.startsWith("//") || line.equals(""));
                    } else if (line.equalsIgnoreCase("<?Numbers?>")) {
                        isSanskrit = false;
                        isTibetan = false;
                        hashOn = false;
                        ignore = false;
                        do {
                            line = in.readLine();
                        } while (line.startsWith("//") || line.equals(""));
                    } else if (line.equalsIgnoreCase("<?Vowels?>")) {
                        isSanskrit = false;
                        isTibetan = false;
                        hashOn = false;
                        ignore = false;
                        do {
                            line = in.readLine();
                        } while (line.startsWith("//") || line.equals(""));
                    } else if (line.equalsIgnoreCase("<?Other?>")) {
                        isSanskrit = false;
                        isTibetan = false;
                        hashOn = false;
                        ignore = false;
                        do {
                            line = in.readLine();
                        } while (line.startsWith("//") || line.equals(""));
                    } else if (line.equalsIgnoreCase("<?Input:Punctuation?>") || line.equalsIgnoreCase("<?Input:Vowels?>")) {
                        isSanskrit = false;
                        isTibetan = false;
                        hashOn = true;
                        ignore = false;
                    } else if (line.equalsIgnoreCase("<?Input:Tibetan?>")) {
                        isSanskrit = false;
                        isTibetan = true;
                        hashOn = true;
                        ignore = false;
                    } else if (line.equalsIgnoreCase("<?Input:Numbers?>")) {
                        isSanskrit = false;
                        isTibetan = false;
                        hashOn = true;
                        ignore = false;
                    } else if (line.equalsIgnoreCase("<?Input:Sanskrit?>")) {
                        isSanskrit = true;
                        isTibetan = false;
                        hashOn = true;
                        ignore = false;
                    } else if (line.equalsIgnoreCase("<?ToWylie?>")) {
                        isSanskrit = false;
                        isTibetan = false;
                        hashOn = false;
                        ignore = false;
                    } else if (line.equalsIgnoreCase("<?Ignore?>")) {
                        isSanskrit = false;
                        ignore = true;
                    }
                } else if (line.startsWith("//")) {
                    ;
                } else if (line.equals("")) {
                    ;
                } else {
                    StringTokenizer st = new StringTokenizer(line, DELIMITER, true);
                    String wylie = null;
                    DuffCode[] duffCodes;
                    duffCodes = new DuffCode[11];
                    int k = 0;
                    StringBuffer escapedToken = new StringBuffer("");
                    ThdlDebug.verify(escapedToken.length() == 0);
                    while (st.hasMoreTokens()) {
                        String val = getEscapedToken(st, escapedToken);
                        if (val.equals(DELIMITER) && escapedToken.length() == 0) {
                            k++;
                        } else if (!val.equals("")) {
                            if (escapedToken.length() != 0) {
                                escapedToken = new StringBuffer("");
                                ThdlDebug.verify(escapedToken.length() == 0);
                            }
                            switch(k) {
                                case 0:
                                    wylie = val;
                                    break;
                                case 1:
                                    duffCodes[TM] = new DuffCode(val, false);
                                    break;
                                case 2:
                                    if (!ignore) {
                                        duffCodes[REDUCED_C] = new DuffCode(val, true);
                                    }
                                    break;
                                case 3:
                                    duffCodes[TMW] = new DuffCode(val, true);
                                    if (null != duffCodes[TM]) {
                                        TMtoTMW[duffCodes[TM].getFontNum() - 1][duffCodes[TM].getCharNum() - 32] = duffCodes[TMW];
                                    }
                                    if (null != TMWtoTM[duffCodes[TMW].getFontNum() - 1][duffCodes[TMW].getCharNum() - 32]) throw new Error("tibwn.ini is supposed to use the TibetanMachineWeb glyph as the unique key, but " + val + " appears two or more times.");
                                    TMWtoTM[duffCodes[TMW].getFontNum() - 1][duffCodes[TMW].getCharNum() - 32] = duffCodes[TM];
                                    if (wylie.toLowerCase().startsWith("\\uf0")) {
                                        int x = Integer.parseInt(wylie.substring("\\u".length()), 16);
                                        ThdlDebug.verify((x >= 0xF000 && x <= 0xF0FF));
                                        NonUnicodeToTMW[x - ''] = new DuffCode[] { duffCodes[TMW] };
                                    }
                                    break;
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                case 8:
                                case 9:
                                    if (!ignore) {
                                        try {
                                            duffCodes[k - 1] = new DuffCode(val, true);
                                        } catch (Exception e) {
                                            System.err.println("Couldn't make a DuffCode out of " + val + "; line is " + line + "; k is " + k);
                                        }
                                    }
                                    break;
                                case 10:
                                    if (!val.equals("none")) {
                                        StringBuffer unicodeBuffer = new StringBuffer();
                                        StringTokenizer uTok = new StringTokenizer(val, ",");
                                        while (uTok.hasMoreTokens()) {
                                            String subval = uTok.nextToken();
                                            ThdlDebug.verify(subval.length() == 4 || subval.length() == 3);
                                            try {
                                                int x = Integer.parseInt(subval, 16);
                                                ThdlDebug.verify((x >= 0x0F00 && x <= 0x0FFF) || x == 0x5350 || x == 0x534D || x == 0x0020 || x == 0x00A0 || x == 0x2003);
                                                unicodeBuffer.append((char) x);
                                            } catch (NumberFormatException e) {
                                                ThdlDebug.verify(false);
                                            }
                                        }
                                        TMWtoUnicode[duffCodes[TMW].getFontNum() - 1][duffCodes[TMW].getCharNum() - 32] = unicodeBuffer.toString();
                                        char ch;
                                        if (unicodeBuffer.length() == 1 && UnicodeUtils.isInTibetanRange(ch = unicodeBuffer.charAt(0))) {
                                            if (null != UnicodeToTMW[ch - 'ༀ'][0] && 'ༀ' != ch && '༂' != ch && '༃' != ch && '་' != ch && '༎' != ch && 'ཀ' != ch && 'ག' != ch && 'ཉ' != ch && 'ཏ' != ch && 'ད' != ch && 'ན' != ch && 'ཞ' != ch && 'ར' != ch && 'ཤ' != ch && 'ཧ' != ch && 'ཪ' != ch && 'ཱ' != ch && 'ི' != ch && 'ཱི' != ch && 'ུ' != ch && 'ཱུ' != ch && 'ྲྀ' != ch && 'ཷ' != ch && 'ླྀ' != ch && 'ཹ' != ch && 'ེ' != ch && 'ོ' != ch && 'ཾ' != ch && 'ཱྀ' != ch) {
                                                throw new Error("tibwn.ini has more than one TMW fellow listed that has the Unicode " + val + ", but it's not on the list of specially handled glyphs");
                                            }
                                            UnicodeToTMW[ch - 'ༀ'][0] = duffCodes[TMW];
                                        }
                                    }
                                    break;
                                case 11:
                                    if (!ignore) {
                                        duffCodes[HALF_C] = new DuffCode(val, true);
                                    }
                                    break;
                                case 12:
                                    if (!ignore) {
                                        DuffCode binduCode = new DuffCode(val, true);
                                        binduMap.put(duffCodes[TMW], binduCode);
                                    }
                                    break;
                                case 13:
                                    throw new Error("tibwn.ini has only 13 columns, you tried to use a 14th column.");
                            }
                        } else {
                            if (k == 10) {
                                throw new Error("needed none or some unicode; line is " + line);
                            }
                        }
                    }
                    if (k < 10) {
                        throw new Error("needed none or some unicode; line is " + line);
                    }
                    if (!ignore) {
                        if (null == wylie) throw new Error(fileName + " has a line ^" + DELIMITER + " which means that no Wylie is assigned.  That isn't supported.");
                        if (hashOn) {
                            tibHash.put(Manipulate.unescape(wylie), duffCodes);
                        }
                        if (isTibetan) {
                            StringBuffer wylieWithoutDashes = new StringBuffer(wylie);
                            for (int wl = 0; wl < wylieWithoutDashes.length(); wl++) {
                                if (wylieWithoutDashes.charAt(wl) == '-') {
                                    wylieWithoutDashes.deleteCharAt(wl);
                                    --wl;
                                }
                            }
                            tibSet.add(wylieWithoutDashes.toString());
                        }
                        if (isSanskrit) {
                            sanskritStackSet.add(wylie);
                        }
                        if (null == duffCodes[TMW]) throw new Error(fileName + " has a line with wylie " + wylie + " but no TMW; that's not allowed");
                        int font = duffCodes[TMW].getFontNum();
                        int code = duffCodes[TMW].getCharNum() - 32;
                        toHashKey[font][code] = Manipulate.unescape(wylie);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("file Disappeared");
            ThdlDebug.noteIffyCode();
        }
    }

    /**
* Says whether or not the character is formatting.
* @param c the character to be checked
* @return true if c is formatting (TAB or
* ENTER), false if not
*/
    public static boolean isFormatting(char c) {
        if (c < 32 || c > 126) return true; else return false;
    }

    /**
* Checks to see if the passed string is a character (a single
* [possibly Sanskrit or va or fa] consonant or a number [possibly
* super- or subscribed]) in Extended Wylie.
* @param s the string to be checked
* @return true if s is a character in Extended Wylie transliteration,
* false if not */
    public static boolean isWylieChar(String s) {
        return charSet.contains(s);
    }

    /**
* Checks to see if the passed string is a consonant or unadorned
* consonant stack in Extended Wylie.  The string shouldn't have any
* '+' or '.' characters in it if you wnat this to return true.
* @param s the string to be checked
* @return true if s is such in Extended Wylie transliteration, false
* if not */
    public static boolean isWylieTibetanConsonantOrConsonantStack(String s) {
        return tibSet.contains(s);
    }

    /**
* Returns true if and only if s is necessarily the THDL Extended Wylie
* for a Sanskrit (non-Tibetan, to be more correct) multi-consonant
* stack.  If s is "w", then it might be the EWTS for TWM7.69, and that
* glyph is only used in non-Tibetan stacks, but "w" also stands for
* TMW.53, which is Tibetan, so this will return false for such a
* glyph. */
    public static boolean isWylieSanskritConsonantStack(String s) {
        return sanskritStackSet.contains(s);
    }

    /** Returns true if and only if s is the THDL Extended Wylie
    representation of a legal tsheg-bar appendage like 'i, 'u, 'am,
    etc.  The word le'u (chapter) contains such an appendage, e.g. */
    public static boolean isWylieAchungAppendage(String s) {
        return (s.equals("'e") || s.equals("'i") || s.equals("'o") || s.equals("'u") || s.equals("'us") || s.equals("'ur") || s.equals("'um") || s.equals("'ong") || s.equals("'ongs") || s.equals("'os") || s.equals("'is") || s.equals("'ung") || s.equals("'ang") || s.equals("'am"));
    }

    /**
* Checks to see if the passed string is a number [possibly super- or
* subscribed]) in Extended Wylie.
* @param s the string to be checked
* @return true if s is a number in Extended Wylie transliteration,
* false if not */
    public static boolean isWylieNumber(String s) {
        return numberSet.contains(s);
    }

    /**
* This method checks to see if the passed string
* is punctuation in Extended Wylie.
* @param s the string to be checked
* @return true if s is punctuation in
* Extended Wylie transliteration, false if not
*/
    public static boolean isWyliePunc(String s) {
        return puncSet.contains(s);
    }

    /** By example, this returns true for le, lA-i, lA-iM, luM, l-i, etc.,
    and for l, but false for lc, lj, lt, ld, lp, lb, and lh.  Thus,
    this is useful for seeing if something is truly disambiguous,
    because blta is unambiguous but blan/b.lan is ambiguous. */
    private static boolean isAmbHelper(String y) {
        return (y.length() == 1 || isWylieVowel(y.substring(1, 2)) || (y.length() > 2 && isWylieVowel(y.substring(1, 3))) || (y.length() > 3 && isWylieVowel(y.substring(1, 4))) || (y.length() > 4 && isWylieVowel(y.substring(1, 5))));
    }

    /**
* Checks to see if the concatenation of x and y is ambiguous in
* Extended Wylie.  gya and g.ya, bla and b.la, and bra and b.ra are
* the only syntactically legal ambiguous fellows, as stacks like blha,
* blda, brla, brkya, brgya, brka, etc. are unambiguous.  However, we
* generate b.lha instead of blha because it's easier
* implementation-wise.
* @param x the prefix
* @param y the root stack
* @return true if x + y is ambiguous in the Extended Wylie
* transliteration, false if not */
    public static boolean isAmbiguousWylie(String x, String y) {
        return (("g".equals(x) && y.startsWith("y") && isAmbHelper(y)) || ("g".equals(x) && y.startsWith("w") && isAmbHelper(y)) || ("d".equals(x) && y.startsWith("w") && isAmbHelper(y)) || ("d".equals(x) && y.startsWith("z") && isAmbHelper(y)) || ("b".equals(x) && y.startsWith("l") && isAmbHelper(y)) || ("b".equals(x) && y.startsWith("r") && isAmbHelper(y)) || ("m".equals(x) && y.startsWith("y") && isAmbHelper(y)) || ("b".equals(x) && y.startsWith("y") && isAmbHelper(y)) || ("g".equals(x) && y.startsWith("rw")) || ("d".equals(x) && y.startsWith("rw")) || isWylieVowel(y));
    }

    /** Returns the length in characters of the longest EWTS vowel. */
    public static int getMaxEwtsVowelLength() {
        ThdlDebug.verify(maxEwtsVowelLength > 0);
        return maxEwtsVowelLength;
    }

    /**
* Checks to see if the passed string
* is a vowel in Extended Wylie.
* @param s the string to be checked
* @return true if s is a vowel in
* Extended Wylie transliteration, false if not
*/
    public static boolean isWylieVowel(String s) {
        return vowelSet.contains(s);
    }

    /**
* Checks to see if the passed string begins with an EWTS vowel.
* @param s the string to be checked
* @return true if s is a vowel in
* Extended Wylie transliteration, false if not */
    public static boolean startsWithWylieVowelSequence(String s) {
        for (int i = 0; i < maxEwtsVowelLength; i++) {
            if (i == s.length()) return false;
            if (isWylieVowel(s.substring(0, i + 1))) return true;
        }
        return false;
    }

    /** Returns true if and only if wylie is the THDL Extended Wylie for
    an adornment.  An adornment is something that is part of a stack
    but is not a consonant, such as a Tibetan or Sanskrit vowel or a
    bindu.  Note that an adornment might be both an adornment and a
    vowel, or an adornment and punctuation. */
    public static boolean isWylieAdornment(String wylie) {
        return (vowelSet.contains(wylie) || (wylie.equals("M") || wylie.equals("~M") || wylie.equals("~M`") || wylie.equals("iM") || wylie.equals("-iM") || wylie.equals("eM") || wylie.equals("aiM") || wylie.equals("oM") || wylie.equals("auM")));
    }

    /** Returns true if and only if wylie is the THDL Extended Wylie for
    an adornment {@link #isWylieAdornment(String)} that contains a
    vowel within it. */
    public static boolean isWylieAdornmentAndContainsVowel(String wylie) {
        return (isWylieAdornment(wylie) && !wylie.equals("M") && !wylie.equals("~M") && !wylie.equals("~M`"));
    }

    /**
* Returns true iff this Wylie is valid as a leftmost character in a
* Tibetan syllable.  For example, in the syllable 'brgyad', 'b' is the
* leftmost character. Valid leftmost characters include g, d, b, ',
* and m.
* @param s the (Wylie) string to be checked
* @return true if s is a possible leftmost character in a Tibetan
* syllable, false if not.  */
    public static boolean isWylieLeft(String s) {
        return leftSet.contains(s);
    }

    /**
* Returns true iff this Wylie is valid as a suffix (i.e., a right
* (post-vowel) character) in a Tibetan syllable.  For example, in the
* syllable 'lags', 'g' is in the right character position. Valid right
* characters include g, ng, d, n, b, m, r, l, s, ', and T.
* @param s the (Wylie) string to be checked
* @return true if s is a possible right character in a Tibetan
* syllable, false if not.  */
    public static boolean isWylieRight(String s) {
        return rightSet.contains(s);
    }

    /**
* Returns true iff this Wylie is valid as a postsuffix in a
* Tibetan syllable.
* @param s the string to be checked
* @return true if s is a possible postsuffix in a Tibetan
* syllable, false if not.  */
    public static boolean isWylieFarRight(String s) {
        return farRightSet.contains(s);
    }

    /**
* Returns true iff this Wylie is valid as a head letter in a Tibetan
* syllable.
* @param s the string to be checked
* @return true if s is a possible superscribed letter in a Tibetan
* syllable, false if not.  */
    public static boolean isWylieTop(String s) {
        return topSet.contains(s);
    }

    /**
* Gets the DuffCode required for a vowel, if affixed to the given
* hashKey.  Not as pretty as {@link
* TibTextUtils#getVowel(List,DuffCode,DuffCode,String)}.
* @param hashKey the key for the character the vowel is to be affixed
* to; see {@link #getGlyph(String)} to learn about hash keys.
* @param vowel the vowel you want the DuffCode for
* @return the DuffCode for the vowel in the given
* context, or null if there is no such vowel in
* the context
* @see DuffCode
* @see TibTextUtils#getVowel(List,DuffCode,DuffCode,String) */
    public static DuffCode getVowel(String hashKey, int vowel) {
        DuffCode[] dc = (DuffCode[]) tibHash.get(hashKey);
        if (null == dc) return null;
        return dc[vowel];
    }

    /**
* Checks to see if a glyph exists for this hash key.
* @param hashKey the key to be checked; see {@link #getGlyph(String)}
* to learn about hash keys.
* @return true if there is a glyph corresponding to
* hashKey, false if not
*/
    public static boolean hasGlyph(String hashKey) {
        if (tibHash.get(hashKey) == null) return false; else return true;
    }

    /** Returns the Unicode correspondence for the Wylie wylie, which must
 *  be Wylie returned by getWylieForGlyph(int, int, boolean[]).
 *  Returns null if the Unicode correspondence is nonexistent or
 *  unknown. */
    public static String getUnicodeForWylieForGlyph(String wylie) {
        DuffCode dc = getGlyph(wylie);
        return mapTMWtoUnicode(dc.getFontNum() - 1, dc.getCharNum());
    }

    /**
* Returns true if and only if hashKey is a known hash key from tibwn.ini.
*/
    public static boolean isKnownHashKey(String hashKey) {
        DuffCode[] dc = (DuffCode[]) tibHash.get(hashKey);
        return (null != dc);
    }

    /**
* Gets a glyph for this hash key. Hash keys are not identical to Extended
* Wylie. The hash key for a Tibetan stack separates the members of the stack
* with '-', for example, 's-g-r'. In Sanskrit stacks, '+' is used, e.g. 'g+h+g+h'.
* @param hashKey the key for which you want a DuffCode
* @return the TibetanMachineWeb DuffCode value for hashKey
* @see DuffCode
*/
    public static DuffCode getGlyph(String hashKey) {
        DuffCode dc = maybeGetGlyph(hashKey);
        if (null == dc) throw new Error("Hash key " + hashKey + " not found; it is likely that you misconfigured tibwn.ini such that, say, M is expected (i.e., it is listed as, e.g. punctuation), but no 'M~...' line appears.");
        return dc;
    }

    /**
* Gets a glyph for this hash key if possible; returns null
* otherwise.
* @see #getGlyph(String)
*/
    public static DuffCode maybeGetGlyph(String hashKey) {
        DuffCode[] dc = (DuffCode[]) tibHash.get(hashKey);
        if (null == dc) return null;
        return dc[TMW];
    }

    /**
* Gets the half height character for this hash key.
* @param hashKey the key you want a half height glyph for; see {@link
* #getGlyph(String)} to learn about hash keys.
* @return the TibetanMachineWeb DuffCode of hashKey's
* reduced height glyph, or null if there is no such glyph
* @see DuffCode
*/
    public static DuffCode getHalfHeightGlyph(String hashKey) {
        DuffCode[] dc = (DuffCode[]) tibHash.get(hashKey);
        if (dc == null) return null;
        return dc[REDUCED_C];
    }

    private static final DuffCode TMW_cr = new DuffCode(1, '\r');

    private static final DuffCode TMW_lf = new DuffCode(1, '\n');

    private static final DuffCode TMW_tab = new DuffCode(1, '\t');

    /** An identity function used merely for testing. */
    public static DuffCode mapTMWtoItself(int font, int ordinal, int suggestedFont) {
        if (font < 0 || font > 9) return null;
        if (ordinal >= 255) {
            throw new Error("I didn't know that ever happened.");
        }
        if (ordinal < 32) {
            if (ordinal == (int) '\r') {
                if (0 == suggestedFont) return TMW_cr; else return new DuffCode(suggestedFont, (char) ordinal);
            } else if (ordinal == (int) '\n') {
                if (0 == suggestedFont) return TMW_lf; else return new DuffCode(suggestedFont, (char) ordinal);
            } else if (ordinal == (int) '\t') {
                if (0 == suggestedFont) return TMW_tab; else return new DuffCode(suggestedFont, (char) ordinal);
            } else {
                ThdlDebug.noteIffyCode();
                return null;
            }
        }
        return new DuffCode(font + 1, (char) ordinal);
    }

    private static final DuffCode TM_cr = new DuffCode(1, '\r');

    private static final DuffCode TM_lf = new DuffCode(1, '\n');

    private static final DuffCode TM_tab = new DuffCode(1, '\t');

    /** Returns the DuffCode for the TibetanMachine glyph corresponding to
    the given TibetanMachineWeb font
    (0=TibetanMachineWeb,1=TibetanMachineWeb1,...) and character(32-127).

    Null is returned for an existing TibetanMachineWeb glyph only if
    that glyph is TibetanMachineWeb7.91, because every other
    TibetanMachineWeb glyph has a corresponding TibetanMachine glyph.
    Null is returned if the input isn't valid.

    Only a few control characters are supported: '\r' (carriage
    return), '\n' (line feed), and '\t' (tab).

    If suggestedFont is not zero, then any ordinals that are the same
    in all fonts ('\n', '-', ' ', '\r', and '\t') will be converted to
    the font named tmwFontNames[suggestedFont].
*/
    public static DuffCode mapTMWtoTM(int font, int ordinal, int suggestedFont) {
        if (font < 0 || font > 9) return null;
        if (ordinal >= 127) return null;
        if (ordinal < 32) {
            if (ordinal == (int) '\r') {
                if (0 == suggestedFont) return TM_cr; else return new DuffCode(suggestedFont, (char) ordinal);
            } else if (ordinal == (int) '\n') {
                if (0 == suggestedFont) return TM_lf; else return new DuffCode(suggestedFont, (char) ordinal);
            } else if (ordinal == (int) '\t') {
                if (0 == suggestedFont) return TM_tab; else return new DuffCode(suggestedFont, (char) ordinal);
            } else {
                ThdlDebug.noteIffyCode();
                return null;
            }
        }
        if (45 == ordinal) {
            return new DuffCode(1, (char) ordinal);
        }
        if ((0 != suggestedFont) && (32 == ordinal)) {
            return new DuffCode(suggestedFont, (char) ordinal);
        }
        DuffCode ans = TMWtoTM[font][ordinal - 32];
        return ans;
    }

    private static final String Unicode_cr = "\r";

    private static final String Unicode_lf = "\n";

    private static final String Unicode_tab = "\t";

    private static final DuffCode[] tmwFor0F00 = new DuffCode[] { new DuffCode(1, (char) 63), new DuffCode(8, (char) 102) };

    private static final DuffCode[] tmwFor0F02 = new DuffCode[] { new DuffCode(1, (char) 56), new DuffCode(1, (char) 118), new DuffCode(8, (char) 95), new DuffCode(8, (char) 92) };

    private static final DuffCode[] tmwFor0F03 = new DuffCode[] { new DuffCode(1, (char) 56), new DuffCode(1, (char) 118), new DuffCode(8, (char) 95), new DuffCode(1, (char) 105) };

    private static final DuffCode[] tmwFor0F0E = new DuffCode[] { new DuffCode(1, (char) 107), new DuffCode(1, (char) 107) };

    private static final DuffCode[] tmwFor0F40 = new DuffCode[] { new DuffCode(1, (char) 92) };

    private static final DuffCode[] tmwFor0F42 = new DuffCode[] { new DuffCode(1, (char) 93) };

    private static final DuffCode[] tmwFor0F49 = new DuffCode[] { new DuffCode(1, (char) 94) };

    private static final DuffCode[] tmwFor0F4F = new DuffCode[] { new DuffCode(1, (char) 95) };

    private static final DuffCode[] tmwFor0F51 = new DuffCode[] { new DuffCode(1, (char) 96) };

    private static final DuffCode[] tmwFor0F53 = new DuffCode[] { new DuffCode(1, (char) 97) };

    private static final DuffCode[] tmwFor0F5E = new DuffCode[] { new DuffCode(1, (char) 98) };

    private static final DuffCode[] tmwFor0F62 = new DuffCode[] { new DuffCode(8, (char) 66) };

    private static final DuffCode[] tmwFor0F64 = new DuffCode[] { new DuffCode(1, (char) 99) };

    private static final DuffCode[] tmwFor0F67 = new DuffCode[] { new DuffCode(1, (char) 100) };

    private static final DuffCode[] tmwFor0F6A = new DuffCode[] { new DuffCode(1, (char) 58) };

    private static final DuffCode[] tmwFor0F73 = new DuffCode[] { new DuffCode(4, (char) 106), new DuffCode(1, (char) 109) };

    private static final DuffCode[] tmwFor0F75 = new DuffCode[] { new DuffCode(10, (char) 126) };

    private static final DuffCode[] tmwFor0F76 = new DuffCode[] { new DuffCode(8, (char) 71), new DuffCode(8, (char) 87) };

    private static final DuffCode[] tmwFor0F77 = new DuffCode[] { new DuffCode(8, (char) 71), new DuffCode(4, (char) 106), new DuffCode(8, (char) 87) };

    private static final DuffCode[] tmwFor0F78 = new DuffCode[] { new DuffCode(10, (char) 105), new DuffCode(8, (char) 87) };

    private static final DuffCode[] tmwFor0F79 = new DuffCode[] { new DuffCode(10, (char) 105), new DuffCode(4, (char) 106), new DuffCode(8, (char) 87) };

    private static final DuffCode[] tmwFor0F7E = new DuffCode[] { new DuffCode(8, (char) 91) };

    private static final DuffCode[] tmwFor0F81 = new DuffCode[] { new DuffCode(4, (char) 106), new DuffCode(8, (char) 87) };

    /** Returns an array of one, two, three, or four DuffCodes that
        together represent the Tibetan Unicode character <i>ch</i>.
        Returns null if there is no mapping for <i>ch</i>.  For
        certain codepoints, multiple TMW glyphs are appropriate, and
        we return an arbitrary one. */
    public static DuffCode[] mapUnicodeToTMW(char ch) {
        if ('ༀ' == ch) {
            return tmwFor0F00;
        } else if ('༂' == ch) {
            return tmwFor0F02;
        } else if ('༃' == ch) {
            return tmwFor0F03;
        } else if ('༎' == ch) {
            return tmwFor0F0E;
        } else if ('ཀ' == ch) {
            return tmwFor0F40;
        } else if ('ག' == ch) {
            return tmwFor0F42;
        } else if ('ཉ' == ch) {
            return tmwFor0F49;
        } else if ('ཏ' == ch) {
            return tmwFor0F4F;
        } else if ('ད' == ch) {
            return tmwFor0F51;
        } else if ('ན' == ch) {
            return tmwFor0F53;
        } else if ('ཞ' == ch) {
            return tmwFor0F5E;
        } else if ('ར' == ch) {
            return tmwFor0F62;
        } else if ('ཤ' == ch) {
            return tmwFor0F64;
        } else if ('ཧ' == ch) {
            return tmwFor0F67;
        } else if ('ཪ' == ch) {
            return tmwFor0F6A;
        } else if ('ཱི' == ch) {
            return tmwFor0F73;
        } else if ('ཱུ' == ch) {
            return tmwFor0F75;
        } else if ('ྲྀ' == ch) {
            return tmwFor0F76;
        } else if ('ཷ' == ch) {
            return tmwFor0F77;
        } else if ('ླྀ' == ch) {
            return tmwFor0F78;
        } else if ('ཹ' == ch) {
            return tmwFor0F79;
        } else if ('ཾ' == ch) {
            return tmwFor0F7E;
        } else if ('ཱྀ' == ch) {
            return tmwFor0F81;
        } else {
            if (ch >= 'ༀ' && ch <= '࿿') {
                DuffCode[] x = UnicodeToTMW[ch - 'ༀ'];
                if (null != x[0]) return x;
            } else if (ch >= '' && ch <= '') {
                DuffCode[] x = NonUnicodeToTMW[ch - ''];
                if (null != x[0]) return x;
            }
            return null;
        }
    }

    /** Returns the sequence of Unicode corresponding to the given
    TibetanMachineWeb font
    (0=TibetanMachineWeb,1=TibetanMachineWeb1,...) and
    character(32-127).

    Null is returned for an existing TibetanMachineWeb glyph if and
    only if that glyph has no corresponding Unicode mapping.  Null is
    returned if the input isn't valid.

    Only a few control characters are supported: '\r' (carriage
    return), '\n' (line feed), and '\t' (tab).
 */
    public static String mapTMWtoUnicode(int font, int ordinal) {
        if (font < 0 || font > 9) return null;
        if (ordinal > 127) return null;
        if (ordinal < 32) {
            if (ordinal == (int) '\r') return Unicode_cr; else if (ordinal == (int) '\n') return Unicode_lf; else if (ordinal == (int) '\t') return Unicode_tab; else {
                ThdlDebug.noteIffyCode();
                return null;
            }
        }
        return TMWtoUnicode[font][ordinal - 32];
    }

    /**
* Gets the TibetanMachine font number for this font name.
* @param name a font name
* @return between 1 and 5 if the font is one
* of the TibetanMachine fonts, otherwise 0 */
    public static int getTMFontNumber(String name) {
        String internedName = name.intern();
        for (int i = 1; i < tmFontNames.length; i++) {
            if (internedName == tmFontNames[i]) return i;
        }
        return 0;
    }

    /**
* Gets the TibetanMachineWeb font number for this font name.
* @param name a font name
* @return between 1 and 10 if the font is one
* of the TibetanMachineWeb fonts, otherwise 0
*/
    public static int getTMWFontNumber(String name) {
        String internedName = name.intern();
        for (int i = 1; i < tmwFontNames.length; i++) {
            if (internedName == tmwFontNames[i]) return i;
        }
        return 0;
    }

    /**
* Gets the hash key associated with this glyph.
* @param font a TibetanMachineWeb font number
* @param code an ASCII character code minus 32
* @return the hashKey corresponding to the character at font, code;
* see {@link #getGlyph(String)} to learn about hash keys.
*/
    public static String getHashKeyForGlyph(int font, int code) {
        code = code - 32;
        return toHashKey[font][code];
    }

    /**
* Gets the hash key associated with this glyph, or null if there is
* none (probably because this glyph has no THDL Extended Wylie
* transcription).
* @param dc a DuffCode denoting a TibetanMachineWeb glyph
* @return the hashKey corresponding to the character at dc; see {@link
* #getGlyph(String)} to learn about hash keys. */
    public static String getHashKeyForGlyph(DuffCode dc) {
        int font = dc.getFontNum();
        int code = dc.getCharNum() - 32;
        if (code < 0) return null;
        return toHashKey[font][code];
    }

    /**
* Gets the Extended Wylie for a given hash key.
* The hash keys in charList and so on may include separating
* characters. For example, Wylie 'sgr' has the hash key 's-g-r'.
* This method takes a hash key and converts it its correct
* Wylie value, and therefore is useful in conversions from
* TibetanMachineWeb to Wylie.
* @param hashKey the hash key for a glyph; see {@link
* #getGlyph(String)} to learn about hash keys.
* @return the Wylie value of that hash key
*/
    public static String wylieForGlyph(String hashKey) {
        if (hashKey.indexOf(WYLIE_SANSKRIT_STACKING_KEY) > -1) return hashKey;
        if (hashKey.charAt(0) == '-') return hashKey;
        StringTokenizer st = new StringTokenizer(hashKey, "-");
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) sb.append(st.nextToken());
        return sb.toString();
    }

    /** Error that appears in a document when some TMW cannot be
 *  transcribed in THDL Extended Wylie.  This error message is
 *  documented in www/htdocs/TMW_RTF_TO_THDL_WYLIE.html, so change
 *  them both when you change this. */
    private static String getTMWToWylieErrorString(DuffCode dc) {
        return "<<[[JSKAD_TMW_TO_WYLIE_ERROR_NO_SUCH_WYLIE: Cannot convert " + dc.toString(true) + " to THDL Extended Wylie.  Please see the documentation for the TM or TMW font and transcribe this yourself.]]>>";
    }

    /** Error that appears in a document when some TMW cannot be
 *  transcribed in ACIP.  This error message is documented in
 *  www/htdocs/TMW_or_TM_To_X_Converters.html, so change them both
 *  when you change this. */
    static String getTMWToACIPErrorString(String it, String explanation) {
        return "[# JSKAD_TMW_TO_ACIP_ERROR_NO_SUCH_ACIP: Cannot convert " + it + " to ACIP" + explanation + ".  Please transcribe this yourself.]";
    }

    private static String getTMWToACIPErrorString(DuffCode dc, String explanation) {
        return getTMWToACIPErrorString(dc.toString(true), explanation);
    }

    /**
* Gets the Extended Wylie value for this glyph.
* @param font the font of the TibetanMachineWeb
* glyph you want the Wylie of
* @param code the ordinal, minus 32, of the TibetanMachineWeb glyph
* you want the Wylie of
* @param noSuchWylie an array which will not be touched if this is
* successful; however, if there is no THDL Extended Wylie
* corresponding to the glyph, then noSuchWylie[0] will be set to true
* @return the Wylie value corresponding to the
* glyph denoted by font, code
*/
    public static String getWylieForGlyph(int font, int code, boolean noSuchWylie[]) {
        String hashKey = getHashKeyForGlyph(font, code);
        if (hashKey == null) {
            noSuchWylie[0] = true;
            return getTMWToWylieErrorString(new DuffCode(font, (char) code));
        }
        return wylieForGlyph(hashKey);
    }

    /**
* Gets the Extended Wylie value for this glyph.
* @param dc the DuffCode of the glyph you want
* the Wylie of
* @param noSuchWylie an array which will not be touched if this is
* successful; however, if there is no THDL Extended Wylie
* corresponding to the glyph, then noSuchWylie[0] will be set to true
* @return the Wylie value corresponding to the
* glyph denoted by dc */
    public static String getWylieForGlyph(DuffCode dc, boolean noSuchWylie[]) {
        String hashKey = getHashKeyForGlyph(dc);
        if (hashKey == null) {
            noSuchWylie[0] = true;
            return getTMWToWylieErrorString(dc);
        }
        return wylieForGlyph(hashKey);
    }

    /**
* Says whether or not this glyph involves a Sanskrit stack.
* @param font the font of a TibetanMachineWeb glyph
* @param code the ASCII value of a TibetanMachineWeb glyph minus 32
* @return true if this glyph is a Sanskrit stack,
* false if not
*/
    public static boolean isSanskritStack(int font, int code) {
        String val = toHashKey[font][code];
        if (val.indexOf(WYLIE_SANSKRIT_STACKING_KEY) == -1) return false; else return true;
    }

    /**
* Says whether or not this glyph involves a Sanskrit stack.
* @param dc the DuffCode of a TibetanMachineWeb glyph
* @return true if this glyph is a Sanskrit stack,
* false if not
*/
    public static boolean isSanskritStack(DuffCode dc) {
        int font = dc.getFontNum();
        int code = dc.getCharNum() - 32;
        if (isSanskritStack(font, code)) return true; else return false;
    }

    /**
* Says whether or not this glyph involves a Tibetan stack.
* @param font the font of a TibetanMachineWeb glyph
* @param code the ASCII value of a TibetanMachineWeb glyph minus 32
* @return true if this glyph is a Tibetan stack,
* false if not
*/
    public static boolean isStack(int font, int code) {
        String val = toHashKey[font][code];
        if (val.indexOf('-') < 1) return false; else return true;
    }

    /**
* Says whether or not this glyph involves a Tibetan stack.
* @param dc the DuffCode of a TibetanMachineWeb glyph
* @return true if this glyph is a Tibetan stack,
* false if not
*/
    public static boolean isStack(DuffCode dc) {
        int font = dc.getFontNum();
        int code = dc.getCharNum() - 32;
        return isStack(font, code);
    }

    /**
* Gets the hash with information about each character and stack.
* @return a hash containing a key for each
* entity defined in Wylie, whose object is the
* DuffCode for that key
*/
    public static Map getTibHash() {
        return tibHash;
    }

    /**
* Gets the hash for characters that require special bindus.
* @return a hash whose keys are all vowel glyphs (DuffCodes)
* that require a special bindu, and whose objects
* are the vowel+bindu glyph (DuffCode) corresponding to each
* such vowel glyph
*/
    public static Map getBinduMap() {
        return binduMap;
    }

    /**
* Returns true iff the keyboard has a disambiguating key.
* @return true if the installed keyboard has a disambiguating key,
* false if not
* @see TibetanKeyboard */
    public static boolean hasDisambiguatingKey() {
        return hasDisambiguatingKey;
    }

    /**
* Gets the disambiguating key.
* @return the disambiguating key for the installed
* keyboard, or ' ' if there is no such key
* @see TibetanKeyboard
*/
    public static char getDisambiguatingKey() {
        return disambiguating_key;
    }

    /**
* Returns true iff the keyboard has a Sanksrit stacking key.
* @return true if a stacking key is required to type Sanskrit stacks,
* false if not
* @see TibetanKeyboard */
    public static boolean hasSanskritStackingKey() {
        return hasSanskritStackingKey;
    }

    /**
* Returns true iff the keyboard has a Tibetan stacking key.
* @return true if a stacking key is required to type Tibetan stacks,
* false if not
* @see TibetanKeyboard */
    public static boolean hasTibetanStackingKey() {
        return hasTibetanStackingKey;
    }

    /**
* Returns true iff stacking is medial.
* @return true if the stacking key is medial, false if not, or if
* there is no stacking key
* @see TibetanKeyboard */
    public static boolean isStackingMedial() {
        return isStackingMedial;
    }

    /**
* Gets the stacking key.
* @return the stacking key, or ' ' if there
* isn't one
* @see TibetanKeyboard
*/
    public static char getStackingKey() {
        return stacking_key;
    }

    /**
* Returns true iff achen is required before vowels.
* @return true if you have to type achen first before you can get a
* vowel with achen, false if you can just type the vowel by itself (as
* in Wylie)
* @see TibetanKeyboard */
    public static boolean isAChenRequiredBeforeVowel() {
        return isAChenRequiredBeforeVowel;
    }

    /**
* Returns true iff achung is treated as a consonant.
* @return true if a-chung is considered a consonant for the purposes
* of stacking, false if not (as in Wylie)
* @see TibetanKeyboard */
    public static boolean isAChungConsonant() {
        return isAChungConsonant;
    }

    /**
* Returns true iff there is a key for the invisible 'a' vowel in this
* keyboard.
* @return true if the installed keyboard has a dummy a vowel, false if
* not
* @see TibetanKeyboard */
    public static boolean hasAVowel() {
        return hasAVowel;
    }

    /**
* Gets the invisible 'a' vowel.
* @return the dummy 'a'-vowel for the installed
* keyboard, or "" if there is no such vowel
* @see TibetanKeyboard
*/
    public static String getAVowel() {
        return aVowel;
    }

    /** Returns true if and only if ch, which is an ASCII character
        that you can think of as an arbitrary index into one of the
        Tibetan fonts, is a character that is appropriate for ending a
        line of Tibetan.  <code>'-'</code>, for example, represents
        the tsheg (the little dot after a syllable) in (FIXME: Edward,
        is this true?) all of the TMW fonts.  Thus, this would return
        true for <code>'-'</code>.

        Note that ch is <b>not</b> the Wylie transliteration; it is an
        arbitrary character (well, not quite, since ' ', '\t', '\n' et
        cetera seem to have been wisely chosen to represent Tibetan
        whitespace, but pretty arbitrary).  If you open up MS Word,
        select TibetanMachineWeb1, and type a hyphen,
        i.e. <code>'-'</code>, you'll see a tsheg appear.  If you open
        Jskad and type a hyphen, you won't see a tsheg.
                    
        @param ch the ASCII character "index" into the TMW font

        @return true iff this is a tsheg or whitespace or the like */
    public static boolean isTMWFontCharBreakable(char ch) {
        if (false) {
            int ord = (int) ch;
            if (32 == ord) return true;
            if (45 == ord) return true;
            if (107 == ord) return true;
            if (103 == ord) return true;
            if (104 == ord) return true;
            if (105 == ord) return true;
            if (43 == ord) return true;
            if (40 == ord) return true;
            if (41 == ord) return true;
            if (38 == ord) return true;
            if (39 == ord) return true;
            if (93 == ord) return true;
            if (94 == ord) return true;
            if (92 == ord) return true;
            if (91 == ord) return true;
        }
        return ('-' == ch || ' ' == ch || '\t' == ch || '\n' == ch || '/' == ch);
    }
}
