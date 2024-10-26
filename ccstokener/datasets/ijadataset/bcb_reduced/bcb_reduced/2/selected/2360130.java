package org.xul.samples.bsh;

import bsh.BshMethod;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.UtilEvalError;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.xul.script.scripts.AbstractScript;
import org.xul.script.scripts.ScriptTag;

/** A Beanshell Script.
 *
 * @version 0.4.4
 */
public class BSHScript extends AbstractScript implements ScriptTag {

    public static final String MIME = "text/bsh";

    private ScriptTag script;

    private Map<String, BshMethod> declaredMethods = new HashMap(10);

    /** The BSH intepreter for the script.
     */
    private Interpreter bsh = null;

    public BSHScript(URL url) {
        super(MIME, url);
    }

    public Interpreter getInterpreter() {
        return bsh;
    }

    @Override
    public Set<String> getDeclaredMethods() {
        return declaredMethods.keySet();
    }

    @Override
    public Object evalMethod(String name, Object[] args) throws Exception {
        if (declaredMethods.containsKey(name)) {
            BshMethod method = declaredMethods.get(name);
            return method.invoke(args, bsh);
        } else {
            throw new Exception("Impossible to find BSH method of name " + name);
        }
    }

    @Override
    public Object init() throws Exception {
        if (url != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            bsh = new Interpreter();
            bsh.eval(reader);
            script = (ScriptTag) bsh.getInterface(ScriptTag.class);
            reader.close();
            initDeclaredMethods(bsh);
            return bsh;
        } else return null;
    }

    private void initDeclaredMethods(Interpreter bsh) throws UtilEvalError {
        NameSpace nameSpace = bsh.getNameSpace();
        BshMethod[] methods = nameSpace.getMethods();
        for (int i = 0; i < methods.length; i++) {
            String _name = methods[i].getName();
            if (!(_name.equals("init"))) {
                declaredMethods.put(_name, methods[i]);
            }
        }
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
