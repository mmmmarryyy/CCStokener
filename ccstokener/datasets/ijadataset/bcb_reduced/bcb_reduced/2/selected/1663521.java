package cspom;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.script.ScriptException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import cspom.constraint.Constraint;
import cspom.constraint.GlobalConstraint;
import cspom.predicate.PredicateConstraint;
import cspom.variable.Domain;
import cspom.variable.Variable;

/**
 * 
 * Problem is the central class of the Constraint Satisfaction Problem Object
 * Model. You can create a problem from scratch by instantiating this class and
 * then using the addConstraint and addVariable classes. Most usually, the
 * Problem.load method is used in order to create a Problem object from a file.
 * <p>
 * The Problem class adheres to the following definition :
 * 
 * A CSP is defined as a pair (X, C), X being a finite set of variables, and C a
 * finite set of constraints.
 * 
 * A domain, that is a finite set of values is associated to each variable x in
 * X. Each constraint involves a finite set of variables (its scope) and defines
 * a set of allowed and forbidden instantiations of these variables.
 * 
 * 
 * 
 * @author Julien Vion
 * @see Constraint
 * @see Variable
 * 
 */
public class Problem {

    public static final String VERSION;

    static {
        Matcher matcher = Pattern.compile("Rev:\\ (\\d+)").matcher("$Rev: 319 $");
        matcher.find();
        VERSION = matcher.group(1);
    }

    private final String name;

    private final List<Variable> variables;

    private final Collection<Constraint> constraints;

    private final Collection<Scope> scopes;

    private final Collection<Domain> domains;

    private final RelationManager relationManager;

    private static final Logger logger = Logger.getLogger("cspfj.problem.XMLGenerator");

    /**
     * Creates an empty problem, without any initial variables nor constraints.
     * 
     * @param name
     *            : the name of the problem
     */
    public Problem(final String name) {
        this.name = name;
        variables = new ArrayList<Variable>();
        constraints = new ArrayList<Constraint>();
        scopes = new ArrayList<Scope>();
        relationManager = new RelationManager();
        domains = new ArrayList<Domain>();
    }

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

    public static Problem load(final String string) throws FileNotFoundException, ParseException, IOException {
        final URI uri;
        try {
            uri = new URI(string);
        } catch (URISyntaxException e) {
            throw new ParseException(e.toString(), 0);
        }
        if (uri.getScheme() == null) {
            return load(new URL("file://" + uri));
        }
        return load(uri.toURL());
    }

    public static Problem load(final URL url) throws ParseException, FileNotFoundException, IOException {
        final Problem problem = new Problem(url.getPath());
        final InputStream problemIS = problemInputStream(url);
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        final SAXParser saxParser;
        final XMLReader reader;
        try {
            saxParser = saxParserFactory.newSAXParser();
            reader = saxParser.getXMLReader();
        } catch (ParserConfigurationException e) {
            throw new ParseException(e.toString(), 0);
        } catch (SAXException e) {
            throw new ParseException(e.toString(), 0);
        }
        final ProblemHandler handler = new ProblemHandler(problem);
        reader.setContentHandler(handler);
        try {
            reader.parse(new InputSource(problemIS));
        } catch (SAXParseException e) {
            logger.throwing(Problem.class.getSimpleName(), "load", e);
            throw new ParseException("line " + e.getLineNumber() + ": " + e.toString(), e.getLineNumber());
        } catch (SAXException e) {
            throw new ParseException(e.toString(), 0);
        }
        return problem;
    }

    public void addVariable(final Variable variable) {
        variables.add(variable);
        domains.add(variable.getDomain());
    }

    public void addConstraint(final Constraint constraint) {
        constraints.add(constraint);
        final Scope scope = Scope.findScope(constraint.getScope(), scopes);
        if (scope == null) {
            scopes.add(new Scope(constraint));
        } else {
            scope.addConstraint(constraint);
        }
        if (!(constraint instanceof GlobalConstraint)) {
            relationManager.linkRelation(constraint.getRelation(), constraint);
        }
    }

    public void semiCompilePredicates() throws ScriptException {
        for (Constraint c : constraints) {
            if (!(c instanceof PredicateConstraint)) {
                continue;
            }
            ((PredicateConstraint) c).semiCompile(relationManager);
        }
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public Collection<Constraint> getConstraints() {
        return constraints;
    }

    public Collection<Scope> getScopes() {
        return scopes;
    }

    public RelationManager getRelationManager() {
        return relationManager;
    }

    public String getName() {
        return name;
    }

    public Collection<Constraint> checkSolution(final List<Number> solution) throws ScriptException {
        final Collection<Constraint> falsified = new ArrayList<Constraint>();
        for (Constraint c : constraints) {
            final Number[] values = new Number[c.getArity()];
            for (int i = c.getArity(); --i >= 0; ) {
                values[i] = solution.get(variables.indexOf(c.getScope()[i]));
            }
            if (!c.evaluate(values)) {
                falsified.add(c);
            }
        }
        logger.info("Checked " + solution + " : " + falsified.size());
        return falsified;
    }

    public void standardizeConstraints() {
        for (Scope s : getScopes()) {
            final Variable[] finalScope = s.getScope();
            for (Constraint c : new ArrayList<Constraint>(s.getConstraints())) {
                if (!finalScope.equals(c.getScope())) {
                    s.getConstraints().remove(c);
                    constraints.remove(c);
                    relationManager.unlinkRelation(c.getRelation(), c);
                    final Constraint standardized = c.standardize(finalScope);
                    addConstraint(standardized);
                }
            }
        }
    }

    public Collection<Domain> getDomains() {
        return domains;
    }
}
