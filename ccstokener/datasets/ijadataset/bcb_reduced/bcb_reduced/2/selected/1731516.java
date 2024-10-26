package at.ofai.gate.japeutils;

import gate.*;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.jape.DefaultActionContext;
import gate.jape.constraint.ConstraintPredicate;
import gate.util.Files;
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
@CreoleResource(name = "Jape Extended", helpURL = "http://code.google.com/p/gateplugin-japeutils/wiki/JapeExtended", icon = "jape", comment = "Extended JAPE Transducer with several operators preconfigured")
public class JapeExtended extends gate.creole.Transducer implements ProcessingResource {

    private static final long serialVersionUID = 7115597236650678961L;

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
    @CreoleParameter(comment = "DEPRECATED!!! Template variables if grammar is a template")
    public void setVariables(FeatureMap vars) {
        variables = vars;
    }

    public FeatureMap getVariables() {
        return variables;
    }

    protected FeatureMap variables = null;

    @Override
    @CreoleParameter(comment = "The URL of the grammar", disjunction = "grammar", priority = 1)
    public void setGrammarURL(URL url) {
        if (ourGrammarURL == null) {
            ourGrammarURL = url;
        }
        grammarURL = url;
    }

    protected URL ourGrammarURL = null;

    protected File generatedGrammarFile;

    protected java.net.URL generatedFileURL = null;

    @Override
    public Resource init() throws ResourceInstantiationException {
        initOP("at.ofai.gate.japeutils.ops.ValueRef");
        initOP("at.ofai.gate.japeutils.ops.StartsAt");
        initOP("at.ofai.gate.japeutils.ops.EndsAt");
        initOP("at.ofai.gate.japeutils.ops.Coextensive");
        initOP("at.ofai.gate.japeutils.ops.Overlaps");
        if (ourGrammarURL != null) {
            if (variables != null && variables.keySet().size() > 0) {
                FeatureMap vs = variables;
                if (parameters == null) {
                    parameters = Factory.newFeatureMap();
                }
                parameters.putAll(variables);
                grammarURL = regenerateInputFile(ourGrammarURL, parameters);
                super.setGrammarURL(grammarURL);
                this.setParameterValue("grammarURL", grammarURL);
            } else {
                super.setGrammarURL(ourGrammarURL);
            }
        }
        super.init();
        return this;
    }

    protected URL regenerateInputFile(java.net.URL originalURL, FeatureMap vs) throws ResourceInstantiationException {
        if (generatedFileURL == null) {
            File tmpDir = new File(System.getProperty("gate.user.filechooser.defaultdir"));
            String tmplocation = System.getProperty("java.io.tmpdir");
            if (tmplocation != null) {
                tmpDir = new File(tmplocation);
            }
            File grammarfile = gate.util.Files.fileFromURL(ourGrammarURL);
            String fileName = "JapeExtended_" + grammarfile.getName().replaceAll("\\.jape$", "") + Gate.genSym() + ".jape";
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
