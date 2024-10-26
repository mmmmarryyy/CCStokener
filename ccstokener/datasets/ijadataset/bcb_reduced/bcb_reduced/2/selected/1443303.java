package at.ofai.gate.japeutils;

import gate.*;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.*;
import gate.jape.DefaultActionContext;
import gate.jape.constraint.ConstraintPredicate;
import gate.util.Files;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.stringtemplate.v4.ST;

/** 
 * A version of the GATE JAPE Transducer that already has the necessary
 * constraint predicate predefined to allow backwards value references using
 * the language construct
 * <pre>{Token valueref {Token.string != "ref1"}} {Token} {Token valueref {Token.string == "ref1"}}</pre>
 *
 * @author Johann Petrak
 */
@CreoleResource(name = "Jape Extended Template", helpURL = "http://code.google.com/p/gateplugin-japeutils/wiki/JapeExtendedTempl", icon = "jape", comment = "Extended JAPE Transducer for JAPE template with several operators preconfigured")
public class JapeExtendedTempl extends gate.creole.Transducer implements ProcessingResource {

    private static final long serialVersionUID = 7115945236650678961L;

    @RunTime
    @Optional
    @CreoleParameter(comment = "Parameter for use in the JAPE Java code")
    public void setParameters(FeatureMap parms) {
        parameters = parms;
    }

    public FeatureMap getParameters() {
        return parameters;
    }

    protected FeatureMap parameters = null;

    @Optional
    @CreoleParameter(comment = "Template variables if grammar is a template")
    public void setVariables(FeatureMap vars) {
        variables = vars;
    }

    public FeatureMap getVariables() {
        return variables;
    }

    protected FeatureMap variables = null;

    @CreoleParameter(comment = "The URL of the grammar template")
    public void setGrammarTemplateURL(URL url) {
        grammarTemplateURL = url;
    }

    public URL getGrammarTemplateURL() {
        return grammarTemplateURL;
    }

    private URL grammarTemplateURL;

    @Optional
    @CreoleParameter(comment = "The URL of the directory where to generate the grammar file (default: system temp dir)")
    public void setGeneratedJapeDirUrl(URL url) {
        if (url != null && !url.getProtocol().startsWith("file:")) {
            throw new GateRuntimeException("generatedJapeDirUrl parameter must be a file URL not " + url.getProtocol());
        }
        generatedJapeDirUrl = url;
    }

    public URL getGeneratedJapeDirUrl() {
        return generatedJapeDirUrl;
    }

    private URL generatedJapeDirUrl = null;

    @HiddenCreoleParameter
    @Override
    public void setGrammarURL(URL url) {
    }

    protected File generatedGrammarFile;

    protected java.net.URL generatedFileURL = null;

    @Override
    public Resource init() throws ResourceInstantiationException {
        initOP("at.ofai.gate.japeutils.ops.ValueRef");
        initOP("at.ofai.gate.japeutils.ops.StartsAt");
        initOP("at.ofai.gate.japeutils.ops.EndsAt");
        initOP("at.ofai.gate.japeutils.ops.Coextensive");
        initOP("at.ofai.gate.japeutils.ops.Overlaps");
        FeatureMap vs = null;
        if (variables != null && variables.keySet().size() > 0) {
            vs = variables;
        } else {
            vs = Factory.newFeatureMap();
        }
        if (parameters == null) {
            parameters = Factory.newFeatureMap();
        }
        parameters.putAll(vs);
        grammarURL = regenerateInputFile(grammarTemplateURL, parameters);
        super.setGrammarURL(grammarURL);
        this.setParameterValue("grammarURL", grammarURL);
        super.init();
        return this;
    }

    protected URL regenerateInputFile(java.net.URL originalURL, FeatureMap vs) throws ResourceInstantiationException {
        if (generatedFileURL == null) {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            if (getGeneratedJapeDirUrl() != null) {
                tmpDir = gate.util.Files.fileFromURL(getGeneratedJapeDirUrl());
            }
            File grammarfile = gate.util.Files.fileFromURL(getGrammarTemplateURL());
            String fileName = grammarfile.getName().replaceAll("\\.jape$", "") + Gate.genSym() + ".jape";
            try {
                generatedFileURL = new File(tmpDir, fileName).toURI().toURL();
            } catch (MalformedURLException ex) {
                throw new ResourceInstantiationException("Cannot create generated file URL for " + tmpDir + " / " + fileName);
            }
        }
        String jape = "";
        try {
            jape = readURL2String(originalURL, encoding);
        } catch (IOException ex) {
            throw new ResourceInstantiationException("Could not read grammar file " + originalURL, ex);
        }
        ST template = new ST(jape, '$', '$');
        for (Object key : vs.keySet()) {
            if (key instanceof String) {
                template.add((String) key, vs.get(key));
            } else {
                System.err.println("Ignored parameter for template (not a String): " + key + "=" + vs.get(key).toString());
            }
        }
        String result = template.render();
        File generatedFile = Files.fileFromURL(generatedFileURL);
        System.out.println("Generated grammar file: " + generatedFile);
        try {
            FileUtils.writeStringToFile(generatedFile, result, encoding);
        } catch (IOException ex) {
            throw new ResourceInstantiationException("Could not write to file " + generatedFile, ex);
        }
        return generatedFileURL;
    }

    protected String readURL2String(java.net.URL url, String encoding) throws IOException {
        InputStream is = url.openStream();
        String result = IOUtils.toString(is, encoding);
        is.close();
        return result;
    }

    protected void initOP(String opName) throws ResourceInstantiationException {
        Class<? extends ConstraintPredicate> clazz = null;
        try {
            clazz = Class.forName(opName, true, Gate.getClassLoader()).asSubclass(ConstraintPredicate.class);
        } catch (ClassNotFoundException e) {
            try {
                clazz = Class.forName(opName, true, Thread.currentThread().getContextClassLoader()).asSubclass(ConstraintPredicate.class);
            } catch (ClassNotFoundException e1) {
                throw new ResourceInstantiationException("Cannot load class for operator: " + opName, e1);
            }
        } catch (ClassCastException cce) {
            throw new ResourceInstantiationException("Operator class '" + opName + "' must implement ConstraintPredicate");
        }
        try {
            ConstraintPredicate predicate = clazz.newInstance();
            String opSymbol = predicate.getOperator();
            Factory.getConstraintFactory().addOperator(opSymbol, clazz);
        } catch (Exception e) {
            throw new ResourceInstantiationException("Cannot instantiate class for operator: " + opName, e);
        }
    }

    @Override
    protected DefaultActionContext initActionContext() {
        ExtendedActionContext ctx = new ExtendedActionContext();
        return ctx;
    }

    @Override
    public void execute() throws ExecutionException {
        ((ExtendedActionContext) actionContext).setParameters(parameters);
        super.execute();
    }
}
