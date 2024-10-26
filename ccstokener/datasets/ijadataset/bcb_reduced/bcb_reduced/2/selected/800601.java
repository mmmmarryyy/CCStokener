package cspom;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.tools.bzip2.CBZip2InputStream;
import cspom.compiler.ConstraintParser;
import cspom.compiler.PredicateParseException;
import cspom.constraint.CSPOMConstraint;
import cspom.constraint.GeneralConstraint;
import cspom.dimacs.CNFParser;
import cspom.variable.BooleanDomain;
import cspom.variable.CSPOMDomain;
import cspom.variable.CSPOMVariable;
import cspom.xcsp.XCSPParser;

/**
 * 
 * CSPOM is the central class of the Constraint Satisfaction Problem Object
 * Model. You can create a problem from scratch by instantiating this class and
 * then using the ctr and var methods. The CSPOM.load() method can be used in
 * order to create a CSPOM object from an XCSP file.
 * <p>
 * The CSPOM class adheres to the following definition :
 * 
 * A CSP is defined as a pair (X, C), X being a finite set of variables, and C a
 * finite set of constraints.
 * 
 * A domain is associated to each variable x in X. Each constraint involves a
 * finite set of variables (its scope) and defines a set of allowed and
 * forbidden instantiations of these variables.
 * 
 * @author Julien Vion
 * @see CSPOMConstraint
 * @see CSPOMVariable
 * 
 */
public final class CSPOM {

    /**
	 * Version obtained from SVN Info.
	 */
    public static final String VERSION;

    static {
        final Matcher matcher = Pattern.compile("Rev:\\ (\\d+)").matcher("$Rev: 564 $");
        matcher.find();
        VERSION = matcher.group(1);
    }

    /**
	 * Map used to easily retrieve a variable according to its name.
	 */
    private final LinkedHashMap<String, CSPOMVariable> variables;

    /**
	 * Collection of all constraints of the problem.
	 */
    private final LinkedHashSet<CSPOMConstraint> constraints;

    /**
	 * The constraint compiler used by this CSPOM instance.
	 */
    private ConstraintParser constraintCompiler;

    /**
	 * Creates an empty problem, without any initial variables nor constraints.
	 */
    public CSPOM() {
        variables = new LinkedHashMap<String, CSPOMVariable>();
        constraints = new LinkedHashSet<CSPOMConstraint>();
    }

    /**
	 * Opens an InputStream according to the given URL. If URL ends with ".gz"
	 * or ".bz2", the stream is inflated accordingly.
	 * 
	 * @param url
	 *            URL to open
	 * @return An InputStream corresponding to the given URL
	 * @throws IOException
	 *             If the InputStream could not be opened
	 */
    public static InputStream problemInputStream(final URL url) throws IOException {
        final String path = url.getPath();
        if (path.endsWith(".gz")) {
            return new GZIPInputStream(url.openStream());
        }
        if (path.endsWith(".bz2")) {
            final InputStream is = url.openStream();
            is.read();
            is.read();
            return new CBZip2InputStream(is);
        }
        return url.openStream();
    }

    /**
	 * Loads a CSPOM from a given XCSP file.
	 * 
	 * @param xcspFile
	 *            Either a filename or an URI. Filenames ending with .gz or .bz2
	 *            will be inflated accordingly.
	 * @return The loaded CSPOM
	 * @throws CSPParseException
	 *             If the given file could not be parsed.
	 * @throws IOException
	 *             If the given file could not be read.
	 * @throws DimacsParseException
	 */
    public static CSPOM load(final String xcspFile) throws CSPParseException, IOException {
        final URI uri;
        try {
            uri = new URI(xcspFile);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI", e);
        }
        if (uri.getScheme() == null) {
            return load(new URL("file://" + uri));
        }
        return load(uri.toURL());
    }

    /**
	 * Loads a CSPOM from a given XCSP file.
	 * 
	 * @param url
	 *            An URL locating the XCSP file. Filenames ending with .gz or
	 *            .bz2 will be inflated accordingly.
	 * @return The loaded CSPOM
	 * @throws CSPParseException
	 *             If the given file could not be parsed.
	 * @throws IOException
	 *             If the given file could not be read.
	 * @throws DimacsParseException
	 */
    public static CSPOM load(final URL url) throws CSPParseException, IOException {
        final CSPOM problem = new CSPOM();
        final InputStream problemIS = problemInputStream(url);
        if (url.getFile().contains(".xml")) {
            new XCSPParser(problem).parse(problemIS);
        } else if (url.getFile().contains(".cnf")) {
            new CNFParser(problem).parse(problemIS);
        } else {
            throw new IllegalArgumentException("Unhandled file format");
        }
        return problem;
    }

    public void removeConstraint(final CSPOMConstraint c) {
        for (CSPOMVariable v : c) {
            v.removeConstraint(c);
        }
        constraints.remove(c);
    }

    public void removeVariable(final CSPOMVariable v) {
        if (v.getConstraints().isEmpty()) {
            variables.remove(v.getName());
        } else {
            throw new IllegalArgumentException(v + " is still implied by constraints");
        }
    }

    /**
	 * @return The variables of this problem.
	 */
    public Collection<CSPOMVariable> getVariables() {
        return variables.values();
    }

    /**
	 * Adds a variable to the problem.
	 * 
	 * @param variable
	 *            The variable to add. It must have an unique name.
	 * @throws DuplicateVariableException
	 *             If a variable with the same name already exists.
	 */
    public void addVariable(final CSPOMVariable variable) throws DuplicateVariableException {
        if (variables.put(variable.getName(), variable) != null) {
            throw new DuplicateVariableException(variable.getName());
        }
    }

    /**
	 * Adds a constraint to the problem.
	 * 
	 * @param constraint
	 *            The constraint to add.
	 */
    public void addConstraint(final CSPOMConstraint constraint) {
        if (!constraints.add(constraint)) {
            throw new IllegalArgumentException("This constraint already belongs to the problem");
        }
        for (CSPOMVariable v : constraint) {
            v.registerConstraint(constraint);
        }
    }

    /**
	 * @return The constraints of this problem.
	 */
    public Collection<CSPOMConstraint> getConstraints() {
        return constraints;
    }

    /**
	 * Adds a bounded, unnamed variable in the problem.
	 * 
	 * @param lb
	 *            lower bound
	 * @param ub
	 *            upper bound
	 * @return The added variable.
	 */
    public CSPOMVariable var(final int lb, final int ub) {
        final CSPOMVariable variable = new CSPOMVariable(lb, ub);
        try {
            addVariable(variable);
        } catch (DuplicateVariableException e) {
            throw new IllegalStateException(e);
        }
        return variable;
    }

    /**
	 * Adds a bounded, named variable in the problem.
	 * 
	 * @param name
	 *            name of the variable
	 * @param lb
	 *            lower bound
	 * @param ub
	 *            upper bound
	 * @return The added variable.
	 * @throws DuplicateVariableException
	 *             if a variable of the same name already exists
	 */
    public CSPOMVariable var(final String name, final int lb, final int ub) throws DuplicateVariableException {
        final CSPOMVariable variable = new CSPOMVariable(name, lb, ub);
        addVariable(variable);
        return variable;
    }

    public CSPOMVariable var(final List<Integer> values) {
        final CSPOMVariable variable = new CSPOMVariable(values);
        try {
            addVariable(variable);
        } catch (DuplicateVariableException e) {
            throw new IllegalStateException(e);
        }
        return variable;
    }

    public CSPOMVariable var(final String name, final List<Integer> values) throws DuplicateVariableException {
        final CSPOMVariable variable = new CSPOMVariable(name, values);
        addVariable(variable);
        return variable;
    }

    public CSPOMVariable boolVar(final String name) throws DuplicateVariableException {
        final CSPOMVariable variable = new CSPOMVariable(name, BooleanDomain.DOMAIN);
        addVariable(variable);
        return variable;
    }

    public CSPOMVariable boolVar() {
        final CSPOMVariable variable = new CSPOMVariable(BooleanDomain.DOMAIN);
        try {
            addVariable(variable);
        } catch (DuplicateVariableException e) {
            throw new IllegalStateException(e);
        }
        return variable;
    }

    /**
	 * Adds functional constraints to the problem. The given predicate will be
	 * parsed and the appropriate constraints added to the problem. The
	 * predicate may be complex and is usually translated to a set of
	 * constraints and auxiliary variables. Use variable names in the predicate
	 * to reference them. These variables must already be added to the problem.
	 * 
	 * @param string
	 *            A predicate.
	 * @throws PredicateParseException
	 *             If an error was encountered parsing the predicate.
	 */
    public void ctr(final String string) throws PredicateParseException {
        if (constraintCompiler == null) {
            constraintCompiler = new ConstraintParser(this);
        }
        constraintCompiler.split(string);
    }

    public CSPOMConstraint le(final CSPOMVariable v0, final CSPOMVariable v1) {
        final CSPOMConstraint constraint = new GeneralConstraint("le", null, v0, v1);
        addConstraint(constraint);
        return constraint;
    }

    /**
	 * @param variableName
	 *            A variable name.
	 * @return The variable with the corresponding name.
	 */
    public CSPOMVariable getVariable(final String variableName) {
        return variables.get(variableName);
    }

    @Override
    public String toString() {
        final StringBuilder stb = new StringBuilder();
        for (CSPOMVariable v : variables.values()) {
            stb.append(v).append(" = ");
            final CSPOMDomain<?> d = v.getDomain();
            if (d == null) {
                stb.append('?');
            } else {
                stb.append(d);
            }
            stb.append('\n');
        }
        for (CSPOMConstraint c : constraints) {
            stb.append(c).append('\n');
        }
        return stb.toString();
    }

    /**
	 * Generates the constraint network graph in the GML format. N-ary
	 * constraints are represented as nodes.
	 * 
	 * @return a String containing the GML representation of the constraint
	 *         network.
	 */
    public String toGML() {
        final StringBuilder stb = new StringBuilder();
        stb.append("graph [\n");
        stb.append("directed 0\n");
        for (CSPOMVariable v : variables.values()) {
            stb.append("node [\n");
            stb.append("id \"").append(v.getName()).append("\"\n");
            stb.append("label \"").append(v.getName()).append("\"\n");
            stb.append("]\n");
        }
        int gen = 0;
        for (CSPOMConstraint c : constraints) {
            if (c.getArity() > 2) {
                stb.append("node [\n");
                stb.append("id \"cons").append(gen).append("\"\n");
                stb.append("label \"").append(c.getDescription()).append("\"\n");
                stb.append("graphics [\n");
                stb.append("fill \"#FFAA00\"\n");
                stb.append("]\n");
                stb.append("]\n");
                for (CSPOMVariable v : c) {
                    stb.append("edge [\n");
                    stb.append("source \"cons").append(gen).append("\"\n");
                    stb.append("target \"").append(v.getName()).append("\"\n");
                    stb.append("]\n");
                }
                gen++;
            } else if (c.getArity() == 2) {
                stb.append("edge [\n");
                stb.append("source \"").append(c.getScope().get(0)).append("\"\n");
                stb.append("target \"").append(c.getScope().get(1)).append("\"\n");
                stb.append("label \"").append(c.getDescription()).append("\"\n");
                stb.append("]\n");
            }
        }
        stb.append("]\n");
        return stb.toString();
    }

    public Collection<CSPOMConstraint> control(final Map<String, Number> solution) {
        final Collection<CSPOMConstraint> failed = new ArrayList<CSPOMConstraint>();
        for (CSPOMConstraint c : constraints) {
            final Number[] tuple = new Number[c.getArity()];
            for (int i = c.getArity(); --i >= 0; ) {
                tuple[i] = solution.get(c.getVariable(i).getName());
            }
            if (!c.evaluate(tuple)) {
                failed.add(c);
            }
        }
        return failed;
    }

    public Collection<CSPOMConstraint> controlInt(final Map<String, Integer> solution) {
        final Collection<CSPOMConstraint> failed = new ArrayList<CSPOMConstraint>();
        for (CSPOMConstraint c : constraints) {
            final Number[] tuple = new Number[c.getArity()];
            for (int i = c.getArity(); --i >= 0; ) {
                tuple[i] = solution.get(c.getVariable(i).getName());
            }
            if (!c.evaluate(tuple)) {
                failed.add(c);
            }
        }
        return failed;
    }
}
