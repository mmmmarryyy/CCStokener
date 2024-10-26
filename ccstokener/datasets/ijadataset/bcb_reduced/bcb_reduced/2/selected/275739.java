package net.sf.borg.common;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

public class XTree {

    private String name_, value_;

    private XTree firstChild_, sibling_, parent_, lastChild_;

    XTree newChild() {
        return (appendChild("", ""));
    }

    public String name() {
        return (name_);
    }

    public String value() {
        return (value_);
    }

    public boolean exists() {
        return (this != null_one);
    }

    private static final XTree null_one = new XTree();

    public XTree() {
        name_ = "ROOT";
        sibling_ = null;
        parent_ = null;
        firstChild_ = null;
        lastChild_ = null;
        value_ = "";
    }

    public XTree parent(int n) {
        XTree ret;
        for (ret = parent_; ret != null && n > 1; ret = ret.parent_, n--) ;
        return (ret);
    }

    public XTree child(int n) {
        XTree ret;
        for (ret = firstChild_; ret != null && n > 1; ret = ret.sibling_, n--) ;
        return (ret);
    }

    public XTree name(String newt) {
        name_ = newt;
        return (this);
    }

    public XTree value(String newt) {
        value_ = newt;
        return (this);
    }

    public XTree valueUnEscape(String s) {
        if (s.indexOf('&') != -1) {
            s = s.replaceAll("&amp;", "&");
            s = s.replaceAll("&lt;", "<");
            s = s.replaceAll("&gt;", ">");
        }
        value_ = s;
        return (this);
    }

    public XTree remove() {
        XTree par = parent_;
        if (parent_ != null) {
            if (par.firstChild_ == this) {
                par.firstChild_ = sibling_;
                if (par.lastChild_ == this) {
                    par.lastChild_ = null;
                }
            } else {
                XTree c;
                for (c = par.firstChild_; c != null; c = c.sibling_) {
                    if (c.sibling_ == this) {
                        c.sibling_ = sibling_;
                        if (par.lastChild_ == this) {
                            par.lastChild_ = c;
                        }
                        break;
                    }
                }
            }
            return (par);
        }
        return (null);
    }

    public int index() {
        int i;
        XTree t;
        if (parent_ == null) return (0);
        for (i = 0, t = parent_.firstChild_; t != this; t = t.sibling_, i++) ;
        return (i);
    }

    public int numChildren() {
        int i;
        XTree t;
        for (i = 0, t = firstChild_; t != null; i++, t = t.sibling_) ;
        return (i);
    }

    public XTree appendChild(String t) {
        if (t == null) return null;
        XTree l = lastChild_;
        XTree n = new XTree();
        if (!t.equals("")) n.name(t);
        n.parent_ = this;
        if (l != null) l.sibling_ = n; else firstChild_ = n;
        lastChild_ = n;
        return (n);
    }

    public XTree appendChild(String t, String v) {
        if (t == null || v == null || v.equals("")) return null;
        XTree l = lastChild_;
        XTree n = new XTree();
        if (!t.equals("")) n.name(t);
        n.value(v);
        n.parent_ = this;
        if (l != null) l.sibling_ = n; else firstChild_ = n;
        lastChild_ = n;
        return (n);
    }

    public String value(boolean esc) {
        if (esc) {
            String ret = value_;
            ret = ret.replaceAll("&", "&amp;");
            ret = ret.replaceAll(">", "&gt;");
            ret = ret.replaceAll("<", "&lt;");
            return (ret);
        }
        return (value_);
    }

    public static XTree readFromFile(String filename) throws Exception {
        InputStream fp;
        if (!filename.equals("")) {
            fp = new FileInputStream(filename);
        } else {
            fp = System.in;
        }
        XTree tree = parse_xml(new InputStreamReader(fp, "UTF-8"));
        return (tree);
    }

    public static XTree readFromStream(InputStream istr) throws Exception {
        XTree tree = parse_xml(new InputStreamReader(istr, "UTF-8"));
        return (tree);
    }

    public static XTree readFromURL(URL url) throws Exception {
        return readFromStream(url.openStream());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        toString(0, buf, this);
        return (buf.toString());
    }

    private void toString(int level, StringBuffer buf, XTree root) {
        XTree cur;
        for (cur = this; cur != null; cur = cur.sibling_) {
            if (cur.firstChild_ == null && cur.value_.equals("")) {
                buf.append(indent(level) + "<" + cur.name() + "/>\n");
                continue;
            }
            buf.append(indent(level) + "<" + cur.name() + ">");
            buf.append(cur.value(true));
            if (cur.firstChild_ != null) {
                buf.append("\n");
                cur.firstChild_.toString(level + 1, buf, root);
                buf.append(indent(level) + "</" + cur.name() + ">\n");
            } else {
                buf.append("</" + cur.name() + ">\n");
            }
            if (root == cur) break;
        }
        return;
    }

    static String indent(int lv) {
        int sp = lv * 4;
        StringBuffer s = new StringBuffer(10);
        for (int i = 0; i < sp; i++) s.append(" ");
        return (s.toString());
    }

    public static XTree readFromBuffer(String buf) throws Exception {
        return (parse_xml(new StringReader(buf)));
    }

    public XTree child(String tg) {
        return (child(tg, 1));
    }

    public XTree child(String tg, int n) {
        XTree ret;
        for (ret = firstChild_; ret != null; ret = ret.sibling_) {
            if (tg.equals(ret.name_)) {
                if (n == 1) {
                    return (ret);
                }
                n--;
            }
        }
        return (null_one);
    }

    public XTree deleteChildren() {
        XTree c;
        for (c = firstChild_; c != null; c = c.sibling_) {
            c.remove();
        }
        return (this);
    }

    private static final int T_OPEN = 1;

    private static final int T_CLOSE = 2;

    private static final int T_EMPTY = 3;

    private static final int T_STRING = 4;

    private static final int T_EOF = 5;

    private static Token get_token(Reader r, StringBuffer buf, boolean open_found) throws Exception {
        buf.setLength(0);
        boolean all_white = true;
        while (true) {
            int c;
            try {
                c = r.read();
            } catch (Exception e) {
                throw e;
            }
            if (c == -1) return new Token(T_EOF, open_found);
            if (c == 0) continue;
            char ch = (char) c;
            if (ch == '>') {
                if (!open_found) {
                    throw new Exception("Unmatched close bracket");
                }
                if (buf.length() == 0) {
                    throw new Exception("Empty element name found");
                }
                open_found = false;
                if (buf.charAt(buf.length() - 1) == '/') {
                    buf.deleteCharAt(buf.length() - 1);
                    return new Token(T_EMPTY, open_found);
                }
                if (buf.charAt(0) == '/') {
                    buf.deleteCharAt(0);
                    return new Token(T_CLOSE, open_found);
                }
                return new Token(T_OPEN, open_found);
            } else if (ch == '<') {
                if (open_found) {
                    throw new Exception("Illegal Open Bracket");
                }
                open_found = true;
                if (all_white) {
                    buf.setLength(0);
                } else {
                    return new Token(T_STRING, open_found);
                }
            } else {
                buf.append(ch);
                if (!Character.isWhitespace(ch)) {
                    all_white = false;
                }
            }
        }
    }

    private static XTree parse_xml(Reader r) throws Exception {
        XTree tree = null;
        String data = "";
        XTree cur = null;
        StringBuffer buf = new StringBuffer();
        boolean open_found = false;
        while (true) {
            Token nextToken = get_token(r, buf, open_found);
            int tok = nextToken.tokenType;
            open_found = nextToken.open_found;
            data = buf.toString();
            if (tok == T_OPEN) {
                if (tree == null) {
                    tree = new XTree();
                    tree.name(data);
                    cur = tree;
                } else if (cur != null) {
                    cur = cur.appendChild(data);
                }
            } else if (tok == T_CLOSE) {
                if (tree == null) {
                    throw new Exception("Unexpected element close");
                }
                if (cur != null && !data.equals(cur.name())) {
                    throw new Exception("Open name [" + cur.name() + "] does not match close name [" + data + "]");
                }
                if (cur != null) cur = cur.parent(1);
                if (cur == null) {
                    return (tree);
                }
            } else if (tok == T_EMPTY) {
                if (tree == null) {
                    tree = new XTree();
                    tree.name(data);
                    cur = tree;
                    return (tree);
                }
                if (cur != null) cur.appendChild(data);
            } else if (tok == T_STRING) {
                if (tree == null) {
                    throw new Exception("Illegal non-whitespace before XML start");
                }
                if (cur != null) cur.valueUnEscape(data);
            } else if (tok == T_EOF) {
                if (cur != tree || tree == null) {
                    throw new Exception("Premature end of input: " + buf.toString());
                }
                return (tree);
            }
        }
    }

    public void adopt(XTree donor) {
        XTree c;
        if (donor.firstChild_ == null) {
            return;
        }
        for (c = donor.firstChild_; c != null; c = c.sibling_) {
            c.parent_ = this;
        }
        if (lastChild_ != null) lastChild_.sibling_ = donor.firstChild_;
        if (firstChild_ == null) firstChild_ = donor.firstChild_;
        donor.firstChild_ = null;
        lastChild_ = donor.lastChild_;
        donor.lastChild_ = null;
    }

    public static void main(String argv[]) throws Exception {
        XTree tvo = XTree.readFromFile("");
        String s = tvo.toString();
        System.out.println(s);
    }

    private static class Token {

        int tokenType;

        boolean open_found;

        Token(int tokenType, boolean open_found) {
            this.tokenType = tokenType;
            this.open_found = open_found;
        }
    }
}
