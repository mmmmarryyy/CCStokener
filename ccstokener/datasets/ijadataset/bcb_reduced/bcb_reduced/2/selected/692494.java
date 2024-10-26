package org.mitre.jsip.autoresponder;

import org.mitre.jsip.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.io.DataInputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

/**
 * example usage:
 *
 * the following example would match the strings:
 * "hello i am the cat"  (returns "i am the")
 * "hello a cat"         (returns "a")
 *
 * but would not match:
 * "hello cat"           (two spaces specified in pattern)
 * "hello concat"        (two spaces specified in pattern)
 * 
 * <code>
 * 	Pattern p = new Pattern("hello ", " cat");
 *	boolean b = false;
 *      for (int i = 0; i < argv[0].length(); i++) {
 *	    b = p.complete(argv[0].charAt(i));
 *	}
 *	if (b)
 *	    System.out.println("Matched: '" + p.getMatch() + "'");
 *	else
 *	    System.out.println("Didn't match");
 * </code>
 *
 *
 * @author mmeridet
 */
class Pattern {

    Pattern(String pattern, String end) {
        _pattern = pattern;
        _end = end;
        reset();
    }

    public void reset() {
        _current = new StringBuffer();
        _match = new StringBuffer();
        _terminating = new StringBuffer();
        _state = 0;
    }

    public boolean complete(char next) {
        switch(_state) {
            case 0:
            case 1:
                if (next == _pattern.charAt(_current.length())) {
                    _current.append(next);
                    if (_current.length() == _pattern.length()) {
                        _state = 2;
                    } else if (_state == 0) _state = 1;
                } else {
                    reset();
                }
                break;
            case 2:
                _current.append(next);
                if (next != _end.charAt(0)) {
                    _match.append(next);
                    break;
                } else {
                    _state = 3;
                    _terminating.append(next);
                    if (_terminating.length() == _end.length()) {
                        _state = 4;
                        return true;
                    } else {
                        break;
                    }
                }
            case 3:
                if (next != _end.charAt(_terminating.length())) {
                    _state = 2;
                    _match.append(_terminating);
                    _terminating.delete(0, _terminating.length());
                    _match.append(next);
                    _current.append(next);
                } else {
                    _terminating.append(next);
                    _current.append(next);
                    if (_terminating.length() == _end.length()) {
                        _state = 4;
                        return true;
                    }
                }
                break;
            case 4:
                return true;
        }
        return false;
    }

    public String getMatch() {
        if (_state == 4) return _match.toString(); else return null;
    }

    private String _pattern;

    private String _end;

    private StringBuffer _current;

    private StringBuffer _match;

    private StringBuffer _terminating;

    private int _state;
}

/**
 * Uses Babelfish for translation.
 *
 * Does not specify a proxy server for http access.
 * If necessary, specify system properties
 *    http.proxyHost and http.proxyPort
 *
 * For example:
 * java -Dhttp.proxyHost=myHost.myDomain.org org.mitre.jsip.BabelTranslator "hello"
 *
 * See http://babel.altavista.com/sites/babelfish/tr
 *
 * @author mmeridet
 */
public class BabelTranslator extends Translator {

    public String englishSpanish(String text) {
        return translate(text, "en_es");
    }

    public String spanishEnglish(String text) {
        return translate(text, "es_en");
    }

    public String englishGerman(String text) {
        return translate(text, "en_de");
    }

    public String germanEnglish(String text) {
        return translate(text, "de_en");
    }

    public String englishChinese(String text) {
        return translate(text, "en_zh");
    }

    public String chineseEnglish(String text) {
        return translate(text, "zh_en");
    }

    public String englishFrench(String text) {
        return translate(text, "en_fr");
    }

    public String frenchEnglish(String text) {
        return translate(text, "fr_en");
    }

    private String translate(String sourceText, String code) {
        StringBuffer translation = new StringBuffer();
        StringBuffer chunk = null;
        StringTokenizer st;
        st = new StringTokenizer(sourceText, " \t\n\r\f", true);
        while (st.hasMoreElements()) {
            chunk = new StringBuffer();
            int i = 0;
            try {
                for (i = 0; i < 300; i++) {
                    chunk.append(st.nextToken());
                }
            } catch (NoSuchElementException e) {
            }
            System.out.println("Token length: " + i);
            System.out.println("Translating: " + chunk.toString());
            translation.append(doTranslate(chunk.toString(), code));
        }
        return translation.toString();
    }

    private String doTranslate(String sourceText, String code) {
        String source = URLEncoder.encode(sourceText);
        String translated = null;
        try {
            URL url = new URL("http://babel.altavista.com/sites/babelfish/tr");
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            StringBuffer postData = new StringBuffer();
            postData.append("doit=done&tt=urltext&urltext=");
            postData.append(source);
            postData.append("&lp=");
            postData.append(code);
            out.print(postData.toString());
            out.close();
            BufferedReader input = new BufferedReader(new InputStreamReader(new DataInputStream(connection.getInputStream()), "UTF-8"));
            translated = parseTranslation(input);
            input.close();
        } catch (Exception e) {
            System.out.println("Problem connecting to URL");
            System.out.println(e);
            e.printStackTrace();
        }
        return translated;
    }

    private String parseTranslation(Reader input) {
        Pattern[] patterns = { new Pattern("<textarea rows=\"3\" wrap=virtual cols=\"56\" name=\"q\">", "\n"), new Pattern("<td bgcolor=white>", "</td") };
        boolean matched = false;
        try {
            for (int c = input.read(); c != -1; c = input.read()) {
                for (int i = 0; i < patterns.length; i++) {
                    matched = patterns[i].complete((char) c);
                    if (matched) {
                        return patterns[i].getMatch();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Program exception");
            System.out.println(e);
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] argv) {
        BabelTranslator trans = new BabelTranslator();
        System.out.println(trans.englishSpanish(argv[0]));
    }
}
