package lu.fisch.unimozer;

import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.type.ClassOrInterfaceType;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Vector;
import lu.fisch.structorizer.elements.Root;
import lu.fisch.unimozer.console.Console;
import lu.fisch.unimozer.dialogs.ClassEditor;
import lu.fisch.unimozer.visitors.ClassVisitor;
import lu.fisch.unimozer.visitors.ClassChanger;
import lu.fisch.unimozer.visitors.ConstructorChanger;
import lu.fisch.unimozer.visitors.FieldChanger;
import lu.fisch.unimozer.visitors.FieldVisitor;
import lu.fisch.unimozer.visitors.MethodChanger;
import lu.fisch.unimozer.visitors.MethodVisitor;
import lu.fisch.unimozer.visitors.StructorizerVisitor;
import lu.fisch.unimozer.visitors.UsageVisitor;
import lu.fisch.unimozer.utils.StringList;
import lu.fisch.unimozer.visitors.ExtendsVisitor;
import lu.fisch.unimozer.visitors.InterfaceVisitor;
import lu.fisch.unimozer.visitors.PackageVisitor;
import org.mozilla.intl.chardet.*;

/**
 *
 * @author robertfisch
 */
public class MyClass {

    public static final int PAD = 8;

    public static final String NO_SYNTAX_ERRORS = "No syntax errors";

    private CompilationUnit cu;

    private boolean validCode = true;

    private Point position = new Point(0, 0);

    private int width = 0;

    private int height = 0;

    private boolean selected = false;

    private boolean compiled = false;

    private String internalName = new String();

    private MyClass extendsMyClass = null;

    private Vector<MyClass> usesMyClass = new Vector<MyClass>();

    private String extendsClass = new String();

    private Vector<String> implementsClasses = new Vector<String>();

    private Vector<Element> classes = new Vector<Element>();

    private Vector<Element> methods = new Vector<Element>();

    private Vector<Element> fields = new Vector<Element>();

    private StringList content = new StringList();

    private lu.fisch.structorizer.gui.Diagram nsd = null;

    private boolean enabled = true;

    private boolean isUML = true;

    private boolean isInterface = false;

    private String packagename = Package.DEFAULT;

    public MyClass(String name) {
        cu = new CompilationUnit();
        ClassOrInterfaceDeclaration type = new ClassOrInterfaceDeclaration(ModifierSet.PUBLIC, false, name);
        ASTHelper.addTypeDeclaration(cu, type);
        content = StringList.explode(getJavaCode(), "\n");
        inspect();
    }

    public MyClass(ClassEditor ce) {
        cu = new CompilationUnit();
        ClassOrInterfaceDeclaration type = new ClassOrInterfaceDeclaration(ce.getModifier(), false, ce.getClassName());
        type.setInterface(ce.isInterface());
        ASTHelper.addTypeDeclaration(cu, type);
        if (!ce.getExtends().equals("")) {
            Vector<ClassOrInterfaceType> list = new Vector<ClassOrInterfaceType>();
            list.add(new ClassOrInterfaceType(ce.getExtends()));
            this.setExtendsClass(ce.getExtends());
            type.setExtends(list);
        }
        content = StringList.explode(getJavaCode(), "\n");
        inspect();
    }

    private MyClass(FileInputStream fis) throws FileNotFoundException, ParseException, IOException {
        StringBuffer buffer = new StringBuffer();
        Reader in = null;
        try {
            InputStreamReader isr = new InputStreamReader(fis, Unimozer.FILE_ENCODING);
            in = new BufferedReader(isr);
            int ch;
            while ((ch = in.read()) > -1) {
                buffer.append((char) ch);
            }
            content = StringList.explode(buffer.toString(), "\n");
            cu = JavaParser.parse(new ByteArrayInputStream(getContent().getText().getBytes()));
        } finally {
            in.close();
        }
        inspect();
    }

    public MyClass(String filename, String defaultEncoding) throws FileNotFoundException, ParseException, IOException, URISyntaxException {
        int lang = nsPSMDetector.UNIMOZER;
        nsDetector det = new nsDetector(lang);
        det.Init(new nsICharsetDetectionObserver() {

            public void Notify(String charset) {
                HtmlCharsetDetector.found = true;
            }
        });
        File f = new File(filename);
        URL url = f.toURI().toURL();
        BufferedInputStream imp = new BufferedInputStream(url.openStream());
        byte[] buf = new byte[1024];
        int len;
        boolean done = false;
        boolean isAscii = true;
        while ((len = imp.read(buf, 0, buf.length)) != -1) {
            if (isAscii) isAscii = det.isAscii(buf, len);
            if (!isAscii && !done) done = det.DoIt(buf, len, false);
        }
        det.DataEnd();
        imp.close();
        boolean found = false;
        String encoding = new String(Unimozer.FILE_ENCODING);
        if (isAscii) {
            encoding = "US-ASCII";
            found = true;
        }
        if (!found) {
            String prob[] = det.getProbableCharsets();
            if (prob.length > 0) {
                encoding = prob[0];
            } else {
                encoding = defaultEncoding;
            }
        }
        String filenameSmall = new File(filename).getName().replace(".java", "");
        this.setInternalName(filenameSmall);
        loadFromFileInputStream(new FileInputStream(filename), encoding);
    }

    public MyClass(FileInputStream fis, String encoding) throws FileNotFoundException, ParseException, IOException {
        loadFromFileInputStream(fis, encoding);
    }

    public boolean hasCyclicInheritance() {
        if (this.getExtendsClass().trim().equals("")) return false; else {
            if (this.getExtendsMyClass() == null) return false; else {
                StringList l = new StringList();
                MyClass other = this.getExtendsMyClass();
                l.add(this.getShortName());
                while ((other != null) && (!l.contains(other.getShortName()))) {
                    l.add(other.getShortName());
                    other = other.getExtendsMyClass();
                    if (other == null) break;
                }
                return (other != null);
            }
        }
    }

    private void loadFromFileInputStream(FileInputStream fis, String encoding) throws FileNotFoundException, ParseException, IOException {
        StringBuffer buffer = new StringBuffer();
        Reader in = null;
        setValidCode(true);
        try {
            InputStreamReader isr = new InputStreamReader(fis, encoding);
            in = new BufferedReader(isr);
            int ch;
            while ((ch = in.read()) > -1) {
                buffer.append((char) ch);
            }
            content = StringList.explode(buffer.toString(), "\n");
            parse();
        } catch (Exception ex) {
            setValidCode(false);
        } finally {
            in.close();
        }
        if (isValidCode()) {
            inspect();
        }
    }

    public String parse() {
        boolean OK = false;
        String ret = NO_SYNTAX_ERRORS;
        setValidCode(true);
        try {
            cu = JavaParser.parse(new ByteArrayInputStream(getContent().getText().getBytes()));
            OK = true;
        } catch (ParseException ex) {
            ret = ex.getMessage();
            setValidCode(false);
        } catch (Error ex) {
            ret = ex.getMessage();
            setValidCode(false);
        }
        return ret;
    }

    public String loadFromString(String code) {
        setContent(StringList.explode(code, "\n"));
        String ret = parse();
        if (isValidCode()) {
            inspect();
        }
        return ret;
    }

    public void update(Element ele, String name, int modifiers, String extendsClass) {
        ClassChanger cnc = new ClassChanger((ClassOrInterfaceDeclaration) ele.getNode(), name, modifiers, extendsClass);
        cnc.visit(cu, null);
        inspect();
    }

    public void update(Element ele, String fieldType, String fieldName, int modifier) {
        FieldChanger fnc = new FieldChanger((FieldDeclaration) ele.getNode(), fieldType, fieldName, modifier);
        fnc.visit(cu, null);
        inspect();
    }

    void update(Element ele, String methodType, String methodName, int modifier, Vector<Vector<String>> params) {
        MethodChanger mnc = new MethodChanger((MethodDeclaration) ele.getNode(), methodType, methodName, modifier, params);
        mnc.visit(cu, null);
        inspect();
    }

    void update(Element ele, int modifier, Vector<Vector<String>> params) {
        ConstructorChanger cnc = new ConstructorChanger((ConstructorDeclaration) ele.getNode(), modifier, params);
        cnc.visit(cu, null);
        inspect();
    }

    public ClassOrInterfaceDeclaration getNode() {
        if (classes.size() > 0) {
            return (ClassOrInterfaceDeclaration) classes.get(0).getNode();
        } else return null;
    }

    public int getModifiers() {
        if (classes.size() > 0) {
            return ((ClassOrInterfaceDeclaration) classes.get(0).getNode()).getModifiers();
        } else return 0;
    }

    public String getExtendsClass() {
        if (isValidCode()) {
            ExtendsVisitor cv = new ExtendsVisitor();
            cv.visit(cu, null);
            return cv.getExtends();
        } else return "";
    }

    private String insert(String what, String s, int start) {
        return s.substring(0, start - 1) + what + s.substring(start - 1, s.length());
    }

    public String getJavaCode() {
        if (isValidCode()) return cu.toString(); else return getContent().getText();
    }

    public String getJavaCodeCommentless() {
        String code = getContent().getText();
        String res = "";
        boolean inMComment = false;
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '/' && code.indexOf("/*", i) == i) {
                inMComment = true;
            }
            if (!inMComment == true) {
                res = res + code.charAt(i);
            }
            if (code.charAt(i) == '*' && code.indexOf("*/", i) == i) {
                i++;
                inMComment = false;
            }
        }
        return res;
    }

    public String getName() {
        if (isValidCode()) {
            ClassVisitor cv = new ClassVisitor();
            cv.visit(cu, null);
            return cv.getName();
        } else return getInternalName();
    }

    public StringList getUsesWho() {
        if (isValidCode()) {
            UsageVisitor cv = new UsageVisitor();
            cv.visit(cu, null);
            return cv.getUesedClasses();
        } else return new StringList();
    }

    public String getShortName() {
        if (classes.size() > 0) {
            internalName = ((ClassOrInterfaceDeclaration) classes.get(0).getNode()).getName();
            return internalName;
        } else return getInternalName();
    }

    public String getFullName() {
        String result = getShortName();
        if (!getPackagename().equals(Package.DEFAULT)) result = getPackagename() + "." + result;
        return result;
    }

    public void addField(String fieldType, String fieldName, int modifier, boolean javaDoc, boolean setter, boolean getter, boolean useThis) {
        int insertAt = getCodePositions().get(0);
        String mod = "";
        if (ModifierSet.isAbstract(modifier)) mod += " abstract";
        if (ModifierSet.isPrivate(modifier)) mod += " private";
        if (ModifierSet.isProtected(modifier)) mod += " protected";
        if (ModifierSet.isPublic(modifier)) mod += " public";
        if (ModifierSet.isStatic(modifier)) mod += " static";
        if (ModifierSet.isFinal(modifier)) mod += " final";
        if (ModifierSet.isSynchronized(modifier)) mod += " synchronized";
        mod = mod.trim();
        String field = "\t" + mod + " " + fieldType.trim() + " " + fieldName.trim() + ";";
        String jd = "";
        if (javaDoc) {
            jd = "\t/**\n" + "\t * Write a description of field \"" + fieldName + "\" here.\n" + "\t */\n";
        }
        if (getter == true) {
            Vector<Vector<String>> params = new Vector<Vector<String>>();
            String body = "\t\treturn " + fieldName + ";\n";
            this.addMethod(fieldType, (fieldType.trim().toLowerCase().equals("boolean") ? "is" : "get") + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), ModifierSet.PUBLIC + (ModifierSet.isStatic(modifier) ? ModifierSet.STATIC : 0), params, javaDoc, body);
        }
        if (setter == true) {
            if (useThis == false) {
                Vector<Vector<String>> params = new Vector<Vector<String>>();
                Vector<String> param = new Vector<String>();
                param.add(fieldType);
                param.add("p" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
                params.add(param);
                String body = "\t\t" + fieldName + " = " + "p" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1) + ";\n";
                this.addMethod("void", "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), ModifierSet.PUBLIC + (ModifierSet.isStatic(modifier) ? ModifierSet.STATIC : 0), params, javaDoc, body);
            } else {
                Vector<Vector<String>> params = new Vector<Vector<String>>();
                Vector<String> param = new Vector<String>();
                param.add(fieldType);
                param.add(fieldName);
                params.add(param);
                String body = "\t\tthis." + fieldName + " = " + fieldName + ";\n";
                this.addMethod("void", "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), ModifierSet.PUBLIC + (ModifierSet.isStatic(modifier) ? ModifierSet.STATIC : 0), params, javaDoc, body);
            }
        }
        String code = getContent().getText();
        code = code.substring(0, insertAt) + "\n\n" + jd + field + "" + code.substring(insertAt);
        loadFromString(code);
    }

    public String addMethod(String methodType, String methodName, int modifier, Vector<Vector<String>> params, boolean javaDoc) {
        return addMethod(methodType, methodName, modifier, params, javaDoc, "");
    }

    public String addMethod(String methodType, String methodName, int modifier, Vector<Vector<String>> params, boolean javaDoc, String body) {
        int insertAt = getCodePositions().get(2);
        String mod = "";
        if (ModifierSet.isAbstract(modifier)) mod += " abstract";
        if (ModifierSet.isPrivate(modifier)) mod += " private";
        if (ModifierSet.isProtected(modifier)) mod += " protected";
        if (ModifierSet.isPublic(modifier)) mod += " public";
        if (ModifierSet.isStatic(modifier)) mod += " static";
        if (ModifierSet.isFinal(modifier)) mod += " final";
        if (ModifierSet.isSynchronized(modifier)) mod += " synchronized";
        mod = mod.trim();
        String param = "";
        for (int i = 0; i < params.size(); i++) {
            if (!param.equals("")) param += ", ";
            param += params.get(i).get(0) + " " + params.get(i).get(1);
        }
        String field;
        String ret = mod + " " + methodType.trim() + " " + methodName.trim() + "(" + param.trim() + ")";
        if (!mod.contains("abstract")) {
            field = "\t" + mod + " " + methodType.trim() + " " + methodName.trim() + "(" + param.trim() + ")\n" + "\t{\n" + body + "\t}";
        } else {
            field = "    " + mod + " " + methodType.trim() + " " + methodName.trim() + "(" + param.trim() + ");";
        }
        String jd = "";
        if (javaDoc) {
            jd = "\t/**\n" + "\t * Write a description of method \"" + methodName.trim() + "\" here." + "\n" + "\t * " + "\n";
            int maxLength = 0;
            for (int i = 0; i < params.size(); i++) {
                int thisLength = params.get(i).get(1).length();
                if (thisLength > maxLength) maxLength = thisLength;
            }
            for (int i = 0; i < params.size(); i++) {
                String thisName = params.get(i).get(1);
                while (thisName.length() < maxLength) thisName += " ";
                jd += "\t * @param " + thisName + "    a description of the parameter \"" + thisName + "\"\n";
            }
            if (!methodType.trim().equals("void")) {
                jd += "\t * @return                a description of the returned result\n";
            }
            jd += "\t */\n";
            if (field.trim().startsWith("public static void main(String[] args)")) {
                jd = "\t/**\n" + "\t * The main entry point for executing this program." + "\n" + "\t */\n";
            }
        }
        String code = getContent().getText();
        code = code.substring(0, insertAt) + "\n\n" + jd + field + "" + code.substring(insertAt);
        loadFromString(code);
        return ret;
    }

    public void selectBySignature(String sign) {
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            if (ele.getFullName().equals(sign)) ele.setSelected(true);
        }
    }

    public String addConstructor(int modifier, Vector<Vector<String>> params, boolean javaDoc) {
        int insertAt = getCodePositions().get(1);
        String mod = "";
        if (ModifierSet.isAbstract(modifier)) mod += " abstract";
        if (ModifierSet.isPrivate(modifier)) mod += " private";
        if (ModifierSet.isProtected(modifier)) mod += " protected";
        if (ModifierSet.isPublic(modifier)) mod += " public";
        if (ModifierSet.isStatic(modifier)) mod += " static";
        if (ModifierSet.isFinal(modifier)) mod += " final";
        if (ModifierSet.isSynchronized(modifier)) mod += " synchronized";
        mod = mod.trim();
        String param = "";
        for (int i = 0; i < params.size(); i++) {
            if (!param.equals("")) param += ", ";
            param += params.get(i).get(0) + " " + params.get(i).get(1);
        }
        String ret = mod + " " + getInternalName().trim() + "(" + param.trim() + ")";
        String field = "\t" + mod + " " + getInternalName().trim() + "(" + param.trim() + ")\n" + "\t{\n" + "\t}";
        String jd = "";
        if (javaDoc) {
            jd = "\t/**\n" + "\t * Write a description of this constructor here." + "\n" + "\t * " + "\n";
            int maxLength = 0;
            for (int i = 0; i < params.size(); i++) {
                int thisLength = params.get(i).get(1).length();
                if (thisLength > maxLength) maxLength = thisLength;
            }
            for (int i = 0; i < params.size(); i++) {
                String thisName = params.get(i).get(1);
                while (thisName.length() < maxLength) thisName += " ";
                jd += "\t * @param " + thisName + "    a description of the parameter \"" + thisName + "\"\n";
            }
            jd += "\t */\n";
        }
        String code = getContent().getText();
        code = code.substring(0, insertAt) + "\n\n" + jd + field + "" + code.substring(insertAt);
        loadFromString(code);
        return ret;
    }

    public boolean hasMain() {
        for (int i = 0; i < methods.size(); i++) {
            Element element = methods.get(i);
            if (element.getFullName().equals("public static void main(String[] args)") || element.getFullName().equals("public static void main(String args[])")) return true;
        }
        return false;
    }

    public boolean hasMain1() {
        for (int i = 0; i < methods.size(); i++) {
            Element element = methods.get(i);
            if (element.getFullName().equals("public static void main(String[] args)")) return true;
        }
        return false;
    }

    public boolean hasMain2() {
        for (int i = 0; i < methods.size(); i++) {
            Element element = methods.get(i);
            if (element.getFullName().equals("public static void main(String args[])")) return true;
        }
        return false;
    }

    public String getFullSignatureBySignature(String sign) {
        String ret = "";
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            if (ele.getSignature().equals(sign)) ret = ele.getShortName();
        }
        if (ret.equals("") && extendsMyClass != null) ret = extendsMyClass.getFullSignatureBySignature(sign);
        return ret;
    }

    public int getFullSignatureBySignaturePos(String sign) {
        int ret = -1;
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            ret++;
            if (ele.getSignature().equals(sign)) return ret;
        }
        if (ret == -1 && extendsMyClass != null) ret = extendsMyClass.getFullSignatureBySignaturePos(sign);
        return ret;
    }

    public String getCompleteSignatureBySignature(String sign) {
        String ret = "";
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            if (ele.getSignature().equals(sign)) ret = ele.getName();
        }
        if (ret.equals("") && extendsMyClass != null) ret = extendsMyClass.getCompleteSignatureBySignature(sign);
        return ret;
    }

    public int getCompleteSignatureBySignaturePos(String sign) {
        int ret = -1;
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            ret++;
            if (ele.getSignature().equals(sign)) return ret;
        }
        if (ret == -1 && extendsMyClass != null) ret = extendsMyClass.getCompleteSignatureBySignaturePos(sign);
        return ret;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public String getSignatureByFullSignature(String sign) {
        String ret = "";
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            if (ele.getShortName().equals(sign)) ret = ele.getSignature();
        }
        if (ret.equals("") && extendsMyClass != null) ret = extendsMyClass.getSignatureByFullSignature(sign);
        return ret;
    }

    public LinkedHashMap<String, String> getInputsBySignature(String sign) {
        LinkedHashMap<String, String> ret = new LinkedHashMap<String, String>();
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            String eleSig = ele.getSignature();
            if ((hasMain2()) && (eleSig.equals("void main(String)"))) {
                eleSig = "void main(String[])";
            }
            if (eleSig.equals(sign) || (getPackagename() + "." + eleSig).equals(sign)) {
                if ((hasMain2()) && (eleSig.equals("void main(String[])"))) {
                    ret.put((String) ele.getParams().keySet().toArray()[0], "String[]");
                } else {
                    ret = ele.getParams();
                }
            }
        }
        if (ret.size() == 0 && extendsMyClass != null) ret = extendsMyClass.getInputsBySignature(sign);
        return ret;
    }

    public String getJavaDocBySignature(String sign) {
        String ret = "";
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            if (ele.getSignature().equals(sign)) ret = ele.getJavaDoc();
            if (ret == null) ret = "";
        }
        if (ret.length() == 0 && extendsMyClass != null) ret = extendsMyClass.getJavaDocBySignature(sign);
        return ret;
    }

    private void deselectAll() {
        for (int i = 0; i < classes.size(); i++) {
            classes.get(i).setSelected(false);
        }
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setSelected(false);
        }
        for (int i = 0; i < methods.size(); i++) {
            methods.get(i).setSelected(false);
        }
    }

    public Element getSelected() {
        Element sel = null;
        for (int i = 0; i < classes.size(); i++) {
            Element ele = classes.get(i);
            if (ele.isSelected()) sel = ele;
        }
        for (int i = 0; i < fields.size(); i++) {
            Element ele = fields.get(i);
            if (ele.isSelected()) sel = ele;
        }
        for (int i = 0; i < methods.size(); i++) {
            Element ele = methods.get(i);
            if (ele.isSelected()) sel = ele;
        }
        return sel;
    }

    public Vector<String> getImplements() {
        return implementsClasses;
    }

    public void updateContent() {
        setContent(StringList.explode(getJavaCode(), "\n"));
    }

    public void inspect() {
        if (isValidCode()) {
            String selectedSignature = null;
            if (getSelected() != null) selectedSignature = getSelected().getName();
            ClassVisitor cv = new ClassVisitor();
            cv.visit(cu, null);
            classes = cv.getElements();
            isInterface = cv.isInterface();
            InterfaceVisitor iv = new InterfaceVisitor();
            iv.visit(cu, null);
            implementsClasses = iv.getImplementsClasses();
            ExtendsVisitor ev = new ExtendsVisitor();
            ev.visit(cu, null);
            extendsClass = ev.getExtends();
            PackageVisitor pv = new PackageVisitor();
            pv.visit(cu, null);
            packagename = pv.getPackageName();
            FieldVisitor fv = new FieldVisitor();
            fv.visit(cu, null);
            fields = fv.getElements();
            MethodVisitor mv = new MethodVisitor();
            mv.visit(cu, getContent());
            methods = mv.getElements();
            setCompiled(false);
            for (int i = 0; i < classes.size(); i++) {
                Element ele = classes.get(i);
                ele.setUML(isUML);
                if (ele.getName().equals(selectedSignature)) ele.setSelected(true);
            }
            for (int i = 0; i < fields.size(); i++) {
                Element ele = fields.get(i);
                ele.setUML(isUML);
                if (ele.getName().equals(selectedSignature)) ele.setSelected(true);
            }
            for (int i = 0; i < methods.size(); i++) {
                Element ele = methods.get(i);
                ele.setUML(isUML);
                if (ele.getName().equals(selectedSignature)) ele.setSelected(true);
            }
        } else {
        }
        updateNSD();
    }

    public void draw(Graphics graphics, boolean showFields, boolean showMethods) {
        Graphics2D g = (Graphics2D) graphics;
        Color drawColor = new Color(255, 245, 235);
        if (!isValidCode()) {
            drawColor = Color.RED;
        } else if (selected == true) {
            drawColor = Color.YELLOW;
        } else if (isCompiled() == true) {
            drawColor = new Color(235, 255, 235);
        }
        boolean cleanIt = false;
        if (!isValidCode() && classes.isEmpty()) {
            cleanIt = true;
            Element ele = new Element(Element.CLASS);
            ele.setName("public class " + internalName);
            ele.setUmlName(internalName);
            ele.setUML(isUML);
            classes.add(ele);
        }
        int totalHeight = 0;
        int maxWidth = 0;
        int classesHeight = 0 * PAD;
        int fieldsHeight = 0 * PAD;
        int methodsHeight = 0 * PAD;
        if (isInterface()) {
            Element ele = new Element(Element.INTERFACE);
            ele.setUmlName("<interface>");
            ele.setName("<interface>");
            classes.add(0, ele);
        }
        for (int i = 0; i < classes.size(); i++) {
            Element ele = classes.get(i);
            g.setFont(new Font(g.getFont().getFamily(), ele.getFontStyle(), Unimozer.DRAW_FONT_SIZE));
            int h = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getHeight() + PAD;
            int w = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getWidth() + Element.ICONSIZE;
            g.setFont(new Font(g.getFont().getFamily(), Font.PLAIN, Unimozer.DRAW_FONT_SIZE));
            ele.setHeight(h);
            ele.setPosition(new Point(position.x, position.y + totalHeight));
            if (w > maxWidth) maxWidth = w;
            classesHeight += h;
            totalHeight += h;
        }
        if (showFields) {
            totalHeight += 0 * PAD;
            for (int i = 0; i < fields.size(); i++) {
                Element ele = fields.get(i);
                g.setFont(new Font(g.getFont().getFamily(), ele.getFontStyle(), Unimozer.DRAW_FONT_SIZE));
                int h = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getHeight() + PAD;
                int w = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getWidth() + Element.ICONSIZE;
                g.setFont(new Font(g.getFont().getFamily(), Font.PLAIN, Unimozer.DRAW_FONT_SIZE));
                ele.setHeight(h);
                ele.setPosition(new Point(position.x, position.y + totalHeight));
                if (w > maxWidth) maxWidth = w;
                fieldsHeight += h;
                totalHeight += h;
            }
        }
        if (showMethods) {
            totalHeight += 0 * PAD;
            if (fieldsHeight == 0) totalHeight += fieldsHeight = PAD;
            for (int i = 0; i < methods.size(); i++) {
                Element ele = methods.get(i);
                g.setFont(new Font(g.getFont().getFamily(), ele.getFontStyle(), Unimozer.DRAW_FONT_SIZE));
                int h = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getHeight() + PAD;
                int w = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getWidth() + Element.ICONSIZE;
                g.setFont(new Font(g.getFont().getFamily(), Font.PLAIN, Unimozer.DRAW_FONT_SIZE));
                ele.setHeight(h);
                ele.setPosition(new Point(position.x, position.y + totalHeight));
                if (w > maxWidth) maxWidth = w;
                methodsHeight += h;
                totalHeight += h;
            }
            totalHeight += 0;
            if (methodsHeight == 0) totalHeight += methodsHeight = PAD;
        }
        this.width = maxWidth + 2 * PAD;
        this.height = totalHeight;
        for (int i = 0; i < classes.size(); i++) {
            classes.get(i).setWidth(this.getWidth());
        }
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setWidth(this.getWidth());
        }
        for (int i = 0; i < methods.size(); i++) {
            methods.get(i).setWidth(this.getWidth());
        }
        g.setColor(drawColor);
        g.fillRect(position.x, position.y, this.getWidth(), this.getHeight());
        g.setColor(drawColor);
        for (int i = 0; i < classes.size(); i++) {
            classes.get(i).draw(g);
        }
        if (showFields) for (int i = 0; i < fields.size(); i++) {
            fields.get(i).draw(g);
        }
        if (showMethods) for (int i = 0; i < methods.size(); i++) {
            methods.get(i).draw(g);
        }
        Stroke oldStroke = g.getStroke();
        if (isInterface()) g.setStroke(Diagram.dashed);
        g.setColor(Color.BLACK);
        g.drawRect(position.x, position.y, this.getWidth(), classesHeight);
        g.drawRect(position.x, position.y + classesHeight, this.getWidth(), fieldsHeight);
        g.drawRect(position.x, position.y + classesHeight + fieldsHeight, this.getWidth(), methodsHeight);
        if (isCompiled() == true) {
            g.drawRect(position.x - 2, position.y - 2, this.getWidth() + 4, this.getHeight() + 4);
        }
        g.setStroke(oldStroke);
        if (!isValidCode() && cleanIt) {
            classes.clear();
        }
        if (!isEnabled()) {
            g.setColor(new Color(128, 128, 128, 128));
            g.fillRect(this.getPosition().x, this.getPosition().y, getWidth(), getHeight());
        }
        if (isInterface()) {
            classes.remove(0);
        }
    }

    public boolean isInside(Point pt) {
        return (position.x <= pt.x && pt.x <= position.x + getWidth() && position.y <= pt.y && pt.y <= position.y + getHeight());
    }

    public Point getRelative(Point pt) {
        if (isInside(pt)) {
            return new Point(pt.x - position.x, pt.y - position.y);
        } else return new Point(0, 0);
    }

    /**
     * @return the position
     */
    public Point getPosition() {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(Point position) {
        this.position = position;
    }

    /**
     * @return the selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void deselect() {
        deselectAll();
        this.selected = false;
    }

    public void select(Point pt) {
        deselectAll();
        this.selected = true;
        if (pt != null) {
            for (int i = 0; i < classes.size(); i++) {
                Element ele = classes.get(i);
                ele.setSelected(ele.isInside(pt));
            }
            for (int i = 0; i < fields.size(); i++) {
                Element ele = fields.get(i);
                ele.setSelected(ele.isInside(pt));
            }
            for (int i = 0; i < methods.size(); i++) {
                Element ele = methods.get(i);
                ele.setSelected(ele.isInside(pt));
            }
        }
    }

    public Element getHover(Point pt) {
        Element ret = null;
        if (pt != null) {
            for (int i = 0; i < classes.size(); i++) {
                Element ele = classes.get(i);
                if (ele.isInside(pt)) ret = ele;
            }
            for (int i = 0; i < fields.size(); i++) {
                Element ele = fields.get(i);
                if (ele.isInside(pt)) ret = ele;
            }
            for (int i = 0; i < methods.size(); i++) {
                Element ele = methods.get(i);
                if (ele.isInside(pt)) ret = ele;
            }
        }
        return ret;
    }

    public StringList getFieldTypes() {
        StringList sl = new StringList();
        for (int i = 0; i < fields.size(); i++) {
            Element ele = fields.get(i);
            String type = ((FieldDeclaration) ele.getNode()).getType().toString();
            sl.addIfNew(Unimozer.getTypesOf(type));
        }
        return sl;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the compiled
     */
    public boolean isCompiled() {
        return compiled;
    }

    /**
     * @return the extendsMyClass
     */
    public MyClass getExtendsMyClass() {
        if (extendsMyClass != null) {
            if (extendsMyClass.getShortName().equals(extendsClass)) return extendsMyClass; else return null;
        } else return null;
    }

    /**
     * @param extendsMyClass the extendsMyClass to set
     */
    public void setExtendsMyClass(MyClass extendsMyClass) {
        this.extendsMyClass = extendsMyClass;
    }

    /**
     * @param compiled the compiled to set
     */
    public void setCompiled(boolean compiled) {
        this.compiled = compiled;
    }

    /**
     * @param extendsClass the extendsClass to set
     */
    public void setExtendsClass(String extendsClass) {
        this.extendsClass = extendsClass;
    }

    /**
     * @return the usesMyClass
     */
    public Vector<MyClass> getUsesMyClass() {
        return usesMyClass;
    }

    /**
     * @param usesMyClass the usesMyClass to set
     */
    public void setUsesMyClass(Vector<MyClass> usesMyClass) {
        this.usesMyClass = usesMyClass;
    }

    /**
     * @return the connector
     */
    public StringList getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(StringList content) {
        this.content = content;
    }

    public static Root setErrorNSD() {
        Root root = new Root();
        root.setText("---[ please select a method ]---");
        return root;
    }

    void updateNSD(lu.fisch.structorizer.gui.Diagram nsd) {
        this.nsd = nsd;
        if (nsd != null) {
            boolean ERROR = false;
            if (getSelected() != null) {
                getSelected().getName();
                if ((getSelected().getType() == Element.METHOD) || (getSelected().getType() == Element.CONSTRUCTOR)) {
                    StructorizerVisitor sv = new StructorizerVisitor(getSelected().getName());
                    sv.visit(cu, null);
                    nsd.root = sv.root;
                    nsd.getParent().getParent().repaint();
                } else ERROR = true;
            } else ERROR = true;
            if (ERROR) {
                nsd.root = setErrorNSD();
                nsd.getParent().getParent().repaint();
            }
        }
    }

    private void updateNSD() {
        updateNSD(nsd);
    }

    /**
     * @return the isUML
     */
    public boolean isUML() {
        return isUML;
    }

    /**
     * @param isUML the isUML to set
     */
    public void setUML(boolean isUML) {
        this.isUML = isUML;
        for (int i = 0; i < classes.size(); i++) {
            classes.get(i).setUML(isUML);
        }
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setUML(isUML);
        }
        for (int i = 0; i < methods.size(); i++) {
            methods.get(i).setUML(isUML);
        }
    }

    /**
     * @return the validCode
     */
    public boolean isValidCode() {
        return validCode;
    }

    /**
     * @param validCode the validCode to set
     */
    public void setValidCode(boolean validCode) {
        this.validCode = validCode;
    }

    /**
     * @return the internalName
     */
    public String getInternalName() {
        if (classes.size() > 0) return getShortName(); else return internalName;
    }

    /**
     * @param internalName the internalName to set
     */
    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public Vector<Integer> getCodePositions() {
        String code = getContent().getText();
        int posFields = -1;
        int posConstructors = -1;
        int posMethods = -1;
        int posClass = -1;
        int posOver = -1;
        boolean inMethod = false;
        boolean inConstructor = false;
        String firstField = "";
        String firstConstructor = "";
        String firstMethod = "";
        String lastField = "";
        String lastConstructor = "";
        String lastMethod = "";
        int lastConstructorPos = -1;
        int lastMethodPos = -1;
        char lastNonBlank = ' ';
        boolean inSComment = false;
        boolean inMComment = false;
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '/' && code.indexOf("/*", i) == i) {
                inMComment = true;
            } else if (code.charAt(i) == '*' && code.indexOf("*/", i) == i) {
                code = code.substring(0, i) + "  " + code.substring(i + 2);
                inMComment = false;
            } else if (code.charAt(i) == '/' && code.indexOf("//", i) == i) {
                inSComment = true;
            }
            if (code.charAt(i) == '\n' && inSComment == true) {
                inSComment = false;
            }
            if (inSComment == true || inMComment == true) {
                code = code.substring(0, i) + " " + code.substring(i + 1);
            }
        }
        int open = 0;
        String tmp = "";
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '{') {
                open++;
                if ((open == 2) && (lastNonBlank != '=')) {
                    if (tmp.contains(" " + this.getShortName() + "(")) {
                        lastConstructor = tmp.substring(tmp.lastIndexOf("\n", tmp.indexOf(" " + this.getShortName() + "("))).trim();
                        if (firstConstructor.equals("")) firstConstructor = lastConstructor;
                        inConstructor = true;
                    } else {
                        if (tmp.trim().contains("\n")) lastMethod = tmp.trim().substring(tmp.trim().lastIndexOf("\n")).trim(); else lastMethod = tmp.trim();
                        if (firstMethod.equals("")) firstMethod = lastMethod;
                        inMethod = true;
                    }
                    tmp = "";
                } else if (open == 1) {
                    posClass = i + 1;
                }
            } else if (code.charAt(i) == '}') {
                open--;
                tmp = "";
                if (open == 0) posOver = i; else if (open == 1) {
                    if (inConstructor) {
                        posConstructors = i + 1;
                        inConstructor = false;
                    } else if (inMethod) {
                        posMethods = i + 1;
                        inMethod = false;
                    }
                }
            } else if ((code.charAt(i) == ';') && (open == 1)) {
                if (!tmp.contains("abstract")) {
                    tmp += code.charAt(i);
                    if (tmp.trim().contains("\n")) lastField = tmp.trim().substring(tmp.trim().lastIndexOf("\n")).trim(); else lastField = tmp.trim();
                    if (firstField.equals("")) firstField = lastField;
                    posFields = i + 1;
                    tmp = "";
                } else {
                    tmp += code.charAt(i);
                    if (tmp.trim().contains("\n")) lastMethod = tmp.trim().substring(tmp.trim().lastIndexOf("\n")).trim(); else lastMethod = tmp.trim();
                    if (firstMethod.equals("")) firstMethod = lastMethod;
                }
            } else tmp += code.charAt(i);
            String last = code.charAt(i) + "";
            if (!last.trim().equals("")) lastNonBlank = code.charAt(i);
        }
        if (firstField.equals("")) {
            posFields = posClass;
        }
        if (firstConstructor.equals("")) {
            if (lastField.equals("")) {
                posConstructors = posClass;
            } else {
                posConstructors = code.indexOf(lastField) + lastField.length();
            }
        }
        if (posMethods == -1) posMethods = posOver - 1;
        Vector<Integer> res = new Vector<Integer>();
        code = getContent().getText();
        if (posFields < code.length() - 1) while (!code.substring(posFields, posFields + 1).equals("\n") && posFields < code.length()) {
            posFields++;
        }
        if (posConstructors < code.length() - 1) while (!code.substring(posConstructors, posConstructors + 1).equals("\n") && posConstructors < code.length()) {
            posConstructors++;
        }
        if (posMethods < code.length() - 1) while (!code.substring(posMethods, posMethods + 1).equals("\n") && posMethods < code.length()) {
            posMethods++;
        }
        code = code.substring(0, posMethods) + "\n<METHOD>\n" + code.substring(posMethods);
        code = code.substring(0, posConstructors) + "\n<CONSTRUCTOR>" + code.substring(posConstructors);
        code = code.substring(0, posFields) + "\n<FIELD>" + code.substring(posFields);
        res.add(posFields);
        res.add(posConstructors);
        res.add(posMethods);
        return res;
    }

    public void addPackage(String myPack) {
        String code = getContent().getText();
        code = "package " + myPack + ";\n\n" + code;
        loadFromString(code);
    }

    public void addClassJavaDoc() {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        java.util.Date date = new java.util.Date();
        String today = dateFormat.format(date);
        String jd = "/**\n" + " * Write a description of " + (isInterface() ? "interface" : "class") + " \"" + getInternalName() + "\" here." + "\n" + " * " + "\n" + " * @author     " + System.getProperty("user.name") + "\n" + " * @version    " + today + "\n" + " */\n\n";
        String code = getContent().getText();
        code = jd + code;
        loadFromString(code);
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the packagename
     */
    public String getPackagename() {
        return packagename;
    }

    /**
     * @param packagename the packagename to set
     */
    public void setPackagename(String packagename) {
        this.packagename = packagename;
    }

    public boolean hasField(String myName) {
        boolean found = false;
        for (int i = 0; i < fields.size(); i++) {
            Element e = fields.get(i);
            if (e.getSimpleName().equals(myName)) found = true;
        }
        return found;
    }

    public void addExtends(String className) {
        if (className != null) if (!className.trim().equals("")) if (this.getExtendsClass().trim().equals("")) {
            ClassVisitor cv = new ClassVisitor();
            cv.visit(cu, null);
            int classLine = cv.getClassLine() - 1;
            String line = getContent().get(classLine);
            StringList words = StringList.explode(line, " ");
            int posi = words.indexOf("class");
            if (!words.contains("class")) {
                posi = words.indexOf("interface");
            }
            if (posi != -1) {
                String toMod = words.get(posi + 1).trim();
                if (toMod.endsWith("{")) {
                    toMod = toMod.subSequence(1, toMod.length() - 1) + " extends " + className + " {";
                } else {
                    toMod = toMod + " extends " + className;
                }
                words.delete(posi + 1);
                words.insert(toMod, posi + 1);
                line = words.getText().replace("\n", " ");
            }
            getContent().delete(classLine);
            getContent().insert(line, classLine);
            loadFromString(getContent().getText());
        }
    }
}
