package scratchcomp.util.ini;

import java.io.*;
import java.net.URL;
import java.util.zip.*;
import scratchcomp.util.Helper;

public class INIParser {

    private String INPUT_CHARSET = "UTF-8";

    private final char commentMark = ';';

    private final char declarationMark = '=';

    private final char lineend = (char) 10;

    private char[] newline = new String("\n").toCharArray();

    private static final int FILE = 1;

    private static final int STREAM = 2;

    private static final int STRING = 3;

    private static final int NET = 4;

    private InputStream is;

    private InputStreamReader isr;

    private URL url;

    private String filename, fileStr;

    private int mode;

    public INIParser() {
    }

    public INIParser(String fn) {
        filename = fn;
        mode = FILE;
    }

    public INIParser(InputStream is) {
        this.is = is;
        mode = STREAM;
    }

    public INIParser(URL url) {
        this.url = url;
        mode = NET;
    }

    public void setInputString(String s) {
        fileStr = s;
        mode = STRING;
    }

    public void setInputFile(String s) {
        filename = s;
        mode = FILE;
    }

    public void setInputStream(InputStream is) {
        this.is = is;
        mode = STREAM;
    }

    public synchronized INI read() throws IOException {
        INI ini = new INI();
        int avail = streamConf();
        char buffer;
        boolean sectionStart = false;
        boolean comment = false;
        boolean escapeSequence = false;
        boolean keyRead = false;
        boolean valueRead = false;
        boolean error = false;
        String sectionName = "";
        String key = "";
        String value = "";
        String esc = "";
        INISection section = null;
        for (int i = 0; i < avail; i++) {
            if (error) {
                ini = null;
                break;
            }
            if (mode != STRING) buffer = (char) isr.read(); else buffer = fileStr.charAt(i);
            switch(buffer) {
                case '[':
                    if (!sectionStart && !valueRead && !escapeSequence && !comment) {
                        sectionStart = true;
                        if (section != null && section.getContentSize() > 0) {
                            ini.addSection(sectionName.trim(), section);
                            sectionName = "";
                            section = null;
                        }
                        keyRead = false;
                    } else {
                        if (comment) {
                            break;
                        } else if (escapeSequence) {
                            if (valueRead) {
                                value += "[";
                            } else if (keyRead) {
                                key += "[";
                            } else if (sectionStart) {
                                sectionName += "[";
                            } else {
                            }
                            escapeSequence = false;
                        } else {
                        }
                    }
                    break;
                case ']':
                    if (sectionStart && !valueRead && !escapeSequence && !comment) {
                        if (sectionName.length() > 0) {
                            section = new INISection(sectionName.trim());
                        } else {
                        }
                        keyRead = true;
                        sectionStart = false;
                    } else {
                        if (comment) {
                            break;
                        } else if (escapeSequence) {
                            if (valueRead) {
                                value += "]";
                            } else if (keyRead) {
                                key += "]";
                            } else if (sectionStart) {
                                sectionName += "]";
                            } else {
                            }
                            escapeSequence = false;
                        } else {
                        }
                    }
                    break;
                case ' ':
                case 9:
                    if (valueRead) {
                        value += buffer;
                    } else if (comment) {
                    }
                    break;
                case commentMark:
                    if (escapeSequence) {
                        if (keyRead) {
                            key += commentMark;
                            escapeSequence = false;
                        } else if (valueRead) {
                            value += commentMark;
                            escapeSequence = false;
                        } else if (sectionStart) {
                            sectionName += commentMark;
                            escapeSequence = false;
                        } else {
                        }
                    } else {
                        comment = true;
                    }
                    break;
                case declarationMark:
                    if (escapeSequence) {
                        if (keyRead) {
                            key += declarationMark;
                            escapeSequence = false;
                        } else if (valueRead) {
                            value += declarationMark;
                            escapeSequence = false;
                        } else if (sectionStart) {
                            sectionName += declarationMark;
                            escapeSequence = false;
                        } else {
                        }
                    } else {
                        if (keyRead) {
                            keyRead = false;
                            valueRead = true;
                        } else if (comment) {
                        } else {
                        }
                    }
                    break;
                case '\\':
                    if (!comment) {
                        if (escapeSequence) {
                            if (valueRead) value += "\\"; else if (keyRead) key += "\\"; else {
                            }
                        } else escapeSequence = true;
                    }
                    break;
                case lineend:
                    if (valueRead) {
                        if (key.length() > 0) {
                            section.addValue(key.trim(), value.trim());
                        }
                        key = "";
                        value = "";
                        valueRead = false;
                        keyRead = true;
                    }
                    if (comment) {
                        comment = false;
                    }
                    if (sectionStart) {
                    }
                    break;
                default:
                    if (!comment && !escapeSequence) {
                        if (sectionStart) sectionName += buffer; else if (keyRead) key += buffer; else if (valueRead) value += buffer;
                    } else {
                        if (escapeSequence) {
                            esc += buffer;
                            if (esc.startsWith("x") && esc.length() < 5) esc += buffer; else {
                                if (sectionStart) sectionName += escape(esc); else if (keyRead) key += escape(esc); else if (valueRead) value += escape(esc);
                                esc = "";
                                escapeSequence = false;
                            }
                        } else if (comment) {
                        }
                    }
                    break;
            }
        }
        if (section != null) ini.addSection(sectionName, section);
        if (mode != STRING) {
            isr.close();
            is.close();
        }
        return ini;
    }

    private String escape(String esc) {
        switch(esc.charAt(0)) {
            case 'n':
                return "\n";
            case 't':
                return "\t";
            case 'x':
                if (esc.length() == 5) return "" + (char) (Helper.hex2int(esc.substring(1, esc.length())));
            default:
                return esc;
        }
    }

    private int streamConf() throws IOException {
        int avail = 0;
        switch(mode) {
            case FILE:
                if (!Helper.findInString(filename, ".jar")) {
                    is = new FileInputStream(filename);
                    isr = new InputStreamReader(is, INPUT_CHARSET);
                } else {
                    is = Helper.getZipInput(filename);
                    isr = new InputStreamReader(is, INPUT_CHARSET);
                }
                break;
            case STREAM:
                isr = new InputStreamReader(is, INPUT_CHARSET);
                break;
            case NET:
                is = url.openStream();
                isr = new InputStreamReader(is, INPUT_CHARSET);
                break;
        }
        switch(mode) {
            case FILE:
            case STREAM:
                avail = is.available();
                break;
            case NET:
                while (is.read() != -1) avail++;
                isr.close();
                is.close();
                is = url.openStream();
                isr = new InputStreamReader(is, INPUT_CHARSET);
                break;
            case STRING:
                avail = fileStr.length();
                break;
        }
        return avail;
    }

    public static void main(String[] args) throws Exception {
        INIParser inip = new INIParser(args[0]);
        INI ini = inip.read();
        ini.output();
    }
}
