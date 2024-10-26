package scratchcomp.util.xml;

import java.io.*;
import java.net.URL;
import java.util.zip.*;
import java.util.Stack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import scratchcomp.util.Helper;

public class XMLParser {

    private String INPUT_CHARSET = "UTF-8";

    private String OUTPUT_CHARSET = "UTF-8";

    private static final int FILE = 1;

    private static final int STREAM = 2;

    private static final int STRING = 3;

    private static final int NET = 4;

    private static final int MAXSTRSIZE = 128;

    private InputStream is;

    private BufferedReader bisr;

    private URL url;

    private String filename, fileStr;

    private int mode;

    private boolean htmllike = false;

    public XMLParser() {
    }

    public XMLParser(String fn) {
        filename = fn;
        mode = FILE;
    }

    public XMLParser(InputStream is) {
        this.is = is;
        mode = STREAM;
    }

    public XMLParser(URL url) {
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

    public void allowHtmlLike(boolean b) {
        htmllike = b;
    }

    public final XMLElement read() throws IOException {
        int avail = streamConf();
        boolean tagread = false;
        boolean tagstart = false;
        boolean tagend = false;
        boolean valueread = false;
        boolean exclamation_mark = false;
        boolean next_comment = false;
        boolean argread = false;
        boolean comment = false;
        boolean comment_start = false;
        boolean comment_end = false;
        boolean funct = false;
        boolean empty = false;
        boolean entread = false;
        boolean subparsetrigger = false;
        boolean subparse = false;
        boolean subparse_second = false;
        boolean subparse_third = false;
        String tag = "";
        String value = "";
        String args = "";
        String entity = "";
        String subval = "";
        String subtype = "";
        String mixcnt = "";
        Vector strpool = new Vector();
        Vector orphan_elements = new Vector();
        boolean prolog = false, prologread = true, clean = false;
        char buffer;
        int j = -1;
        while (prologread) {
            if (mode != STRING) {
                buffer = (char) bisr.read();
            } else {
                j++;
                buffer = fileStr.charAt(j);
            }
            if (j < avail) {
                switch(buffer) {
                    case '?':
                        if (tagread & !prolog) {
                            prolog = true;
                            tagstart = true;
                        } else {
                            clean = true;
                            prologread = false;
                        }
                        break;
                    case '<':
                        tagread = true;
                        break;
                    case '>':
                        tagread = false;
                        break;
                    case ' ':
                        if (tagstart) tagstart = false; else args += buffer;
                        break;
                    default:
                        if (tagstart && prolog) tag += buffer; else if (!tagstart && prolog) args += buffer; else prologread = false;
                        break;
                    case (char) -1:
                        prologread = false;
                        break;
                }
            }
        }
        if (mode != STRING) {
            bisr.close();
            is.close();
        }
        if (args.length() > 0 && clean) {
            declare(args);
        }
        streamConf();
        tagread = false;
        prolog = false;
        tag = "";
        args = "";
        int occurance;
        int parents;
        XMLElement element = null;
        XMLElement current = null;
        XMLElement lastParent = null;
        XMLElement root = null;
        Stack stack = new Stack();
        for (int i = 0; i < avail; i++) {
            if (mode != STRING) buffer = (char) bisr.read(); else buffer = fileStr.charAt(i);
            if (tagread) {
                switch(buffer) {
                    case '/':
                        if (!comment) {
                            if (tag.length() < 1) {
                                tagstart = false;
                                valueread = false;
                            } else {
                                if (!argread) tag += buffer; else args += buffer;
                            }
                        }
                        break;
                    case '!':
                        if (!comment) {
                            if (tag.length() < 1) {
                                tagstart = false;
                                exclamation_mark = true;
                            } else {
                                if (!argread) tag += buffer; else args += buffer;
                            }
                        }
                        break;
                    case '?':
                        if (!comment) {
                            if (tag.length() < 1) {
                                funct = true;
                            } else {
                                if (!argread) tag += buffer; else args += buffer;
                            }
                        }
                        break;
                    case ' ':
                    case 10:
                    case 13:
                    case 9:
                        if (!comment) argread = true;
                        if (argread) args += buffer;
                        break;
                    case '>':
                        if (!comment && !funct && !exclamation_mark) {
                            if (tagstart) {
                                if (tag.endsWith("/") || args.endsWith("/")) {
                                    if (tag.endsWith("/")) tag = tag.substring(0, tag.length() - 1);
                                    empty = true;
                                    valueread = false;
                                } else valueread = true;
                                element = new XMLElement(tag, (String) null, stack.size());
                                element.addArgs(args);
                            } else {
                                if (tag.equals(element.getName())) {
                                    stack.pop();
                                    if (!stack.empty()) {
                                        current = (XMLElement) stack.pop();
                                        current.addElement(element);
                                        if (htmllike && orphan_elements.size() > 0) {
                                            for (int e = 0; e < orphan_elements.size(); e++) {
                                                System.out.println("-> adding orphan <" + ((XMLElement) orphan_elements.elementAt(e)).getName() + "> to <" + current.getName() + ">");
                                                current.addElement((XMLElement) orphan_elements.elementAt(e));
                                            }
                                            orphan_elements.removeAll(orphan_elements);
                                        }
                                        element = current;
                                        stack.push(element);
                                    } else {
                                        if (root == null) root = element; else {
                                            XMLElement etemp = new XMLElement("XML", (String) null, 0);
                                            etemp.addElement(root);
                                            etemp.addElement(element);
                                            root = etemp;
                                            etemp = null;
                                        }
                                    }
                                } else {
                                    if (htmllike) {
                                        System.err.println("don't panic : element <" + element.getName() + "> is not closing previous element <" + current.getName() + ">");
                                        orphan_elements.add(element);
                                    }
                                }
                            }
                            tagread = false;
                            tag = "";
                            args = "";
                            argread = false;
                            tagstart = false;
                        } else {
                            funct = false;
                            tagread = false;
                            tag = "";
                            args = "";
                            argread = false;
                            tagstart = false;
                        }
                        if (!comment) exclamation_mark = false;
                        break;
                    default:
                        if (!argread && tagread & !comment) tag += buffer; else if (!comment) {
                            if (buffer == '&') entread = true; else if (buffer == ';' && entread) entread = false;
                            if (!entread && buffer != ';') args += buffer; else if (!entread && entity.length() > 0) {
                                args += Helper.decodeHTMLEntity(entity);
                                entity = "";
                            } else if (buffer != '&') entity += buffer;
                        }
                        break;
                }
            } else {
                if (buffer != '<' && buffer != '>' & !comment & !valueread & !argread) {
                    if (mixcnt.length() < MAXSTRSIZE) {
                        mixcnt += buffer;
                    } else {
                        strpool.add(mixcnt + buffer);
                        mixcnt = "";
                    }
                }
            }
            if (buffer == '<' && empty & !tagread & !exclamation_mark & !subparse & !subparse_second & !subparse_third & !comment) {
                if (strpool.size() > 0) {
                    String st = ((String) strpool.elementAt(0)).trim();
                    strpool.set(0, st);
                } else {
                    mixcnt = mixcnt.trim();
                }
                if (!stack.empty()) {
                    current = (XMLElement) stack.pop();
                    if (mixcnt.length() > 0) {
                        if (strpool.size() > 0) {
                            strpool.add(mixcnt);
                            current.addElement(new XMLElement("#text", strpool, 0));
                            strpool = new Vector();
                        } else {
                            current.addElement(new XMLElement("#text", mixcnt, 0));
                        }
                        mixcnt = "";
                        stack.push(current);
                    } else {
                        current.addElement(element);
                        element = current;
                        stack.push(element);
                    }
                } else {
                    if (root == null) root = element; else {
                        XMLElement etemp = new XMLElement("XML", (String) null, 0);
                        etemp.addElement(root);
                        etemp.addElement(element);
                        root = etemp;
                        etemp = null;
                    }
                }
                empty = false;
                tag = "";
                args = "";
                value = "";
                argread = false;
                valueread = false;
                tagread = true;
                tagstart = true;
                valueread = false;
            }
            if (buffer == '<' & !tagread & !exclamation_mark & !subparse & !subparse_second & !subparse_third & !empty & !comment) {
                if (element != null && valueread) {
                    if (strpool.size() > 0) {
                        strpool.add(value);
                        element.setValue(strpool);
                        strpool = new Vector();
                    } else {
                        element.setValue(value);
                    }
                    stack.push(element);
                    value = "";
                } else {
                    mixcnt = mixcnt.trim();
                    if (mixcnt.length() > 0) {
                        current = (XMLElement) stack.pop();
                        current.addElement(new XMLElement("#text", mixcnt, 0));
                        stack.push(current);
                        mixcnt = "";
                    }
                }
                tagread = true;
                tagstart = true;
                valueread = false;
            }
            if (!tagread && valueread && buffer != '>' && buffer != 10 && buffer != 12 & !subparse & !subparse_second & !subparse_third & !empty & !comment) {
                if (buffer == '&') entread = true; else if (buffer == ';' && entread) entread = false;
                if (!entread && buffer != ';') {
                    if (value.length() < MAXSTRSIZE) {
                        value += buffer;
                    } else {
                        strpool.add(value + buffer);
                        value = "";
                    }
                } else if (!entread && entity.length() > 0) {
                    value += Helper.decodeHTMLEntity(entity);
                    entity = "";
                } else if (buffer != '&') entity += buffer;
            }
            if (!tagstart && exclamation_mark && buffer != '>' & !subparse & !subparse_second & !subparse_third & !comment & !comment_start) {
                if (buffer == '[') {
                    subparse = true;
                    subparsetrigger = true;
                } else if (buffer == '-' & !comment & !comment_start & !comment_end) {
                    comment_start = true;
                }
            } else if (!tagstart && subparse && !subparse_second && !subparse_third) {
                if (buffer == '[' & !subparsetrigger) {
                    subparse_second = true;
                } else if (buffer == ']') {
                    subparse = false;
                    {
                        if (strpool.size() > 0) {
                            strpool.add(subval);
                            element.setValue(strpool);
                            strpool = new Vector();
                        } else {
                            element.setValue(subval);
                        }
                    }
                    subtype = "";
                    subval = "";
                } else {
                    if (subparsetrigger) subparsetrigger = false;
                    subtype += buffer;
                }
            } else if (!tagstart && subparse && subparse_second & !comment) {
                if (subparse_third) {
                    if (buffer == ']') {
                        subparse = false;
                        subparse_second = false;
                        subparse_third = false;
                        if (subtype.equals("CDATA")) {
                            if (strpool.size() > 0) {
                                strpool.add(subval);
                                element.setValue(strpool);
                                strpool = new Vector();
                            } else {
                                element.setValue(subval);
                            }
                        }
                        subtype = "";
                        subval = "";
                    } else {
                        if (subval.length() < MAXSTRSIZE) {
                            subval += buffer;
                        } else {
                            strpool.add(subval + buffer);
                            subval = "";
                        }
                        subparse_third = false;
                    }
                } else {
                    if (buffer == ']') {
                        subparse_third = true;
                    } else {
                        if (subval.length() < MAXSTRSIZE) {
                            subval += buffer;
                        } else {
                            strpool.add(subval + buffer);
                            subval = "";
                        }
                    }
                }
            } else if (!tagstart && !subparse && !subparse_second && !subparse_third && comment_start & !comment & !comment_end) {
                if (buffer == '-') {
                    comment = true;
                } else comment = false;
            } else if (!tagstart && !subparse && !subparse_second && !subparse_third && comment_start && comment & !comment_end) {
                if (buffer == '-') {
                    comment_start = false;
                } else comment_start = true;
            } else if (!tagstart && !subparse && !subparse_second && !subparse_third & !comment_start && comment & !comment_end) {
                if (buffer == '-') {
                    comment_end = true;
                    comment_start = false;
                } else {
                    comment_start = true;
                    comment_end = false;
                }
            }
            if (!tagstart && exclamation_mark && buffer == '>' & !subparse & !subparse_second & !subparse_third & !comment) {
                exclamation_mark = false;
                valueread = true;
            } else if (!tagstart && exclamation_mark && buffer == '>' && comment_end && comment & !subparse & !subparse_second & !subparse_third) {
                comment_end = false;
                comment = false;
                exclamation_mark = false;
            }
        }
        if (mode != STRING) {
            bisr.close();
            is.close();
        }
        return root;
    }

    private void declare(String value) {
        String name = "";
        String val = "";
        HashMap pseudoArgs = new HashMap();
        boolean nameread = true;
        boolean valueread = false;
        for (int i = 0; i < value.length(); i++) {
            if (nameread) {
                if (value.charAt(i) != '=') {
                    if (value.charAt(i) != ' ') name += value.charAt(i);
                } else nameread = false;
            } else {
                if (valueread) {
                    if (value.charAt(i) != '"') val += value.charAt(i); else {
                        pseudoArgs.put(name, val);
                        name = "";
                        val = "";
                        valueread = false;
                        nameread = true;
                    }
                }
                if (name.length() > 0 && value.charAt(i) == '"') {
                    valueread = true;
                }
            }
        }
        if (pseudoArgs.containsKey("encoding")) INPUT_CHARSET = (String) pseudoArgs.get("encoding");
        name = null;
        val = null;
    }

    private int streamConf() throws IOException {
        int avail = 0;
        switch(mode) {
            case FILE:
                if (!Helper.findInString(filename, ".jar")) {
                    is = new FileInputStream(filename);
                    bisr = new BufferedReader(new InputStreamReader(is, INPUT_CHARSET));
                } else {
                    is = Helper.getZipInput(filename.substring(0, filename.indexOf(".jar") + 4), filename.substring(filename.indexOf(".jar") + 5, filename.length()));
                    bisr = new BufferedReader(new InputStreamReader(is, INPUT_CHARSET));
                }
                break;
            case STREAM:
                bisr = new BufferedReader(new InputStreamReader(is, INPUT_CHARSET));
                break;
            case NET:
                is = url.openStream();
                bisr = new BufferedReader(new InputStreamReader(is, INPUT_CHARSET));
                break;
        }
        switch(mode) {
            case FILE:
            case STREAM:
                avail = is.available();
                break;
            case NET:
                while (is.read() != -1) avail++;
                bisr.close();
                is.close();
                is = url.openStream();
                bisr = new BufferedReader(new InputStreamReader(is, INPUT_CHARSET));
                break;
            case STRING:
                avail = fileStr.length();
                break;
        }
        return avail;
    }
}
