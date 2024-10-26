package it.paolomind.pwge.python;

import it.paolomind.pwge.abstracts.bo.AActionBO;
import it.paolomind.pwge.exceptions.GameException;
import it.paolomind.pwge.exceptions.ScriptException;
import it.paolomind.pwge.interfaces.bo.IActionEventBO;
import it.paolomind.pwge.interfaces.bo.IMapStatusBO;
import it.paolomind.pwge.interfaces.bo.IResourceStatusBO;
import it.paolomind.pwge.utils.AURLUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import org.python.core.PyException;
import org.python.core.PyInstance;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class JythonWrapperAction extends AActionBO {

    /** */
    private static final long serialVersionUID = -7269558900516756738L;

    private transient PyInstance paction;

    private String script;

    public JythonWrapperAction(AActionBO.ActionDTO dto, URL url) throws IOException {
        super(dto);
        InputStream in = url.openStream();
        InputStreamReader rin = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(rin);
        StringBuffer s = new StringBuffer();
        String str;
        while ((str = reader.readLine()) != null) {
            s.append(str);
            s.append("\n");
        }
        in.close();
        script = s.toString();
    }

    public JythonWrapperAction(AActionBO.ActionDTO dto, String url) throws IOException {
        this(dto, AURLUtil.toURL_notNull(url));
    }

    private PyObject toPython(Object obj) {
        return (obj != null) ? new PyJavaInstance(obj) : null;
    }

    private PyJavaInstance doAction(Object A, Object T, Object M) {
        try {
            return (PyJavaInstance) paction.invoke("doAction", new PyObject[] { toPython(A), toPython(T), toPython(M) });
        } catch (Exception e) {
            throw new GameException(e);
        }
    }

    public IActionEventBO doAction(IResourceStatusBO active, Serializable target, IMapStatusBO map) {
        PyJavaInstance o = doAction((Object) active, (Object) target, (Object) map);
        return (IActionEventBO) (o).__tojava__(IActionEventBO.class);
    }

    public void setEngine(PythonInterpreter python) {
        String classname = getId();
        if (python.get(classname) == null) python.exec(script);
        try {
            paction = (PyInstance) python.eval(classname + "()");
        } catch (PyException e) {
            throw new ScriptException("The action ID '" + classname + "' and the name of the class must be the same", e);
        }
    }
}
