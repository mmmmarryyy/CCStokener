package net.sf.crsx.generic;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.crsx.Builder;
import net.sf.crsx.CRS;
import net.sf.crsx.CRSException;
import net.sf.crsx.Constructor;
import net.sf.crsx.Contractum;
import net.sf.crsx.Copyable;
import net.sf.crsx.Factory;
import net.sf.crsx.Kind;
import net.sf.crsx.Match;
import net.sf.crsx.Pattern;
import net.sf.crsx.Primitive;
import net.sf.crsx.PropertiesHolder;
import net.sf.crsx.Sink;
import net.sf.crsx.Stub;
import net.sf.crsx.Substitute;
import net.sf.crsx.Term;
import net.sf.crsx.Unification;
import net.sf.crsx.Valuation;
import net.sf.crsx.Variable;
import net.sf.crsx.Visitor;
import net.sf.crsx.util.Buffer;
import net.sf.crsx.util.ExtensibleMap;
import net.sf.crsx.util.ExtensibleSet;
import net.sf.crsx.util.LinkedExtensibleMap;
import net.sf.crsx.util.Pair;
import net.sf.crsx.util.SequenceIterator;
import net.sf.crsx.util.StackedMap;
import net.sf.crsx.util.Util;
import net.sf.crsx.util.WriterAppender;

/**
 * Special construction with custom rules for matching and copying/contraction.
 * 
 * This evaluator uses the string representation of nullary constructors as arguments and returns
 * a nullary constructor with a string representation of the result.
 * <p>
 * Supported forms (a "constant" is a nullary constructor with the right representation) are listed in the
 * {@link Primitive} helper class.
 * <p>
 * Each form is active in three situations:
 * <ol>
 * <li>When <em>copying</em>, special forms are quietly replaced with their result when possible.
 * <li>When <em>matching</em>, special forms in patterns generally should match the same as their result 
 * 		(<b>note:</b> not fully implemented and not even fully possible...).
 *		Special forms in the matched term never match.
 * <li>When <em>contracting</em>, special forms <em>must</em> be replaceable by their result or it is an error!
 * </ol>
 * These rules ensure that evaluators used in rules do not accidentally creep into the rewritten term.
 * <p>
 * 
 * @author <a href="http://www.research.ibm.com/people/k/krisrose">Kristoffer Rose</a>.
 * @version $Id: GenericEvaluator.java,v 1.167 2012/03/14 23:20:09 krisrose Exp $
 */
class GenericEvaluator extends FixedGenericConstruction {

    /** Regular expression for integers. */
    static final java.util.regex.Pattern INTEGER_REGEX = java.util.regex.Pattern.compile("[-+]*[0-9]+");

    /** The string without outermost quotes. */
    public static String unquote(String s) {
        if (s.length() > 0) {
            switch(s.charAt(0)) {
                case '"':
                    s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
                    break;
                case '\'':
                    s = s.substring(1, s.length() - 1).replace("''", "'");
                    break;
            }
        }
        return s;
    }

    /** The string with quotes. */
    public static String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /**
	 * Return the computed version of the term.
     * @see Contractum
	 * @param term to evaluate (must be a GeneroicEvaluator)
	 * @param globals variables free in context
	 * @return computed result or null if it cannot be computed
	 */
    static Term compute(Term term, Set<Variable> globals) {
        return (term instanceof GenericEvaluator ? ((GenericEvaluator) term).compute(globals) : null);
    }

    /**
	 * Perform the computation implied by the term.
	 * @param globals variables free in context
     * @see Contractum
	 * @return computed result or null if it cannot be computed
	 */
    private Term compute(Set<Variable> globals) {
        final int arity = arity();
        Term t;
        if (arity == 0) return this;
        t = compute(sub(0), globals);
        if (t != null) replaceSub(0, binders(0), t);
        Primitive what = primitive();
        return compute(what, LinkedExtensibleMap.EMPTY_RENAMING, globals);
    }

    /** Evaluate arguments. */
    void computeArguments() {
        for (int i = 0; i < arity(); ++i) {
            Term t = compute(sub(i), null);
            if (t != null) replaceSub(i, binders(i), t);
        }
    }

    /** Evaluate argument. */
    void computeArgument(int i) {
        if (0 < i && i < arity()) {
            Term t = compute(sub(i), null);
            if (t != null) replaceSub(i, binders(i), t);
        }
    }

    /** Recalculate what evaluator with specific arguments. Only observes sub(0) and arity(). */
    public Primitive primitive() {
        if (arity() > 0 && sub(0).kind() == Kind.CONSTRUCTION) {
            String s = sub(0).constructor().symbol();
            for (Primitive w : Primitive.values()) {
                if (s.equals(w.symbol)) {
                    int arity = arity() - 1;
                    if (arity < w.minArgCount || arity > w.maxArgCount) throw new UnsupportedOperationException("Illformed evaluator pattern $[" + this + "]");
                    return w;
                }
            }
        }
        return Primitive.IGNORE;
    }

    /**
	 * Perform the computation implied by the term (internal version).
     * Used by implementations of {@link Contractum#contract(Sink, Valuation, ExtensibleMap)}.
	 * @param what operation
	 * @param renamings of variables in effect
	 * @param globals variables free in context
	 * @param term the $-term we're evaluating
	 * @param sub operation's arguments in order (with sub[0] the constant corresponding to what)
	 * @return computed result or null if it cannot be computed
	 */
    private Term compute(Primitive what, ExtensibleMap<Variable, Variable> renamings, Set<Variable> globals) {
        What: switch(what) {
            case PLUS:
                computeArguments();
                try {
                    long sum = 0;
                    for (int i = 1; i < arity(); ++i) {
                        Term a = sub(i);
                        if (!Util.isConstant(a)) return null;
                        sum += Long.decode(a.constructor().symbol());
                    }
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(sum), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case MINUS:
                computeArguments();
                try {
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) return null;
                    long left = Long.decode(sub(1).constructor().symbol());
                    long right = Long.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(left - right), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case TIMES:
                computeArguments();
                try {
                    long product = 1;
                    for (int i = 1; i < arity(); ++i) {
                        Term a = sub(i);
                        if (!Util.isConstant(a)) return null;
                        product *= Long.decode(a.toString());
                    }
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(product), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case DIVIDE:
                computeArguments();
                if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                try {
                    long left = Long.decode(sub(1).constructor().symbol());
                    long right = Long.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(left / right), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case MODULO:
                computeArguments();
                if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                try {
                    long left = Long.decode(sub(1).constructor().symbol());
                    long right = Long.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(left % right), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case HEX:
                computeArguments();
                if (!Util.isConstant(sub(1))) break;
                try {
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(Long.parseLong(sub(1).constructor().symbol(), 16)), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case BIT_AND:
                computeArguments();
                if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                try {
                    long left = Long.decode(sub(1).constructor().symbol());
                    long right = Long.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(left & right), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case BIT_OR:
                computeArguments();
                if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                try {
                    long left = Long.decode(sub(1).constructor().symbol());
                    long right = Long.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(left | right), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case BIT_XOR:
                computeArguments();
                if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                try {
                    long left = Long.decode(sub(1).constructor().symbol());
                    long right = Long.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(left ^ right), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case BIT_NOT:
                computeArguments();
                if (!Util.isConstant(sub(1))) break;
                try {
                    long bits = Long.decode(sub(1).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(~bits), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case BIT_MINUS:
                computeArguments();
                if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                try {
                    long left = Long.decode(sub(1).constructor().symbol());
                    long right = Long.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(left & ~right), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                } catch (NumberFormatException e) {
                }
                break;
            case BIT_SUB_SET_EQ:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    try {
                        long left = Long.decode(sub(1).constructor().symbol());
                        long right = Long.decode(sub(2).constructor().symbol());
                        return rewrapWithProperties(factory.newConstruction(((left & (~right)) == 0 ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                    } catch (NumberFormatException e) {
                    }
                    break;
                }
            case EQ:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    return rewrapWithProperties(factory.newConstruction((sub(1).equals(sub(2)) ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case NE:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    return rewrapWithProperties(factory.newConstruction((sub(1).equals(sub(2)) ? factory.nilConstructor : factory.trueConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case LE:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    int left = Integer.decode(sub(1).constructor().symbol());
                    int right = Integer.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction((left <= right ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case GE:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    int left = Integer.decode(sub(1).constructor().symbol());
                    int right = Integer.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction((left >= right ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case LT:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    int left = Integer.decode(sub(1).constructor().symbol());
                    int right = Integer.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction((left < right ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case GT:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    int left = Integer.decode(sub(1).constructor().symbol());
                    int right = Integer.decode(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction((left > right ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case CONCATENATION:
                {
                    computeArguments();
                    StringBuilder b = new StringBuilder();
                    for (int i = 1; i < arity(); ++i) {
                        Term a = sub(i);
                        if (!Util.isConstant(a)) return null;
                        String text;
                        switch(a.kind()) {
                            case CONSTRUCTION:
                                text = a.constructor().symbol();
                                break;
                            case META_APPLICATION:
                                text = a.metaVariable();
                                break;
                            case VARIABLE_USE:
                                text = a.variable().name();
                                break;
                            default:
                                break What;
                        }
                        b.append(text);
                    }
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(b.toString()), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case INDEX:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    int index = sub(1).constructor().symbol().indexOf(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(index), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case CONTAINS:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    boolean contains = sub(1).constructor().symbol().contains(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction((contains ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case STARTS_WITH:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    boolean startsWith = sub(1).constructor().symbol().startsWith(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction((startsWith ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case ENDS_WITH:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2))) break;
                    boolean endsWith = sub(1).constructor().symbol().endsWith(sub(2).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction((endsWith ? factory.trueConstructor : factory.nilConstructor), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case LENGTH:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1))) break;
                    int length = sub(1).constructor().symbol().length();
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(length), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case REPLACE:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2)) || !Util.isConstant(sub(3))) break;
                    String symbol = sub(1).constructor().symbol().replace(sub(2).constructor().symbol(), sub(3).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(symbol), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case SQUASH:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1))) break;
                    String symbol = Util.symbol(sub(1)).replaceAll("[\\n\\t ]+", " ").trim();
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(symbol), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case SUBSTRING:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2)) || (arity() == 4 && !Util.isConstant(sub(3)))) break;
                    String symbol = (arity() == 4 ? sub(1).constructor().symbol().substring(Integer.parseInt(sub(2).constructor().symbol()), Integer.parseInt(sub(3).constructor().symbol())) : sub(1).constructor().symbol().substring(Integer.parseInt(sub(2).constructor().symbol())));
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(symbol), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case ESCAPE:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1))) break;
                    String string = quote(sub(1).constructor().symbol());
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(string), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case MANGLE:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1))) break;
                    String string = Util.quoteJavaIdentifierPart(Util.symbol(sub(1)));
                    return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(string), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                }
            case SPLIT:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2)) || arity() != 3) break;
                    String string = sub(1).constructor().symbol();
                    String regex = sub(2).constructor().symbol();
                    String[] split = string.split(regex);
                    Term sequence = factory.nil();
                    final Variable[] nobinds = {};
                    for (int i = split.length - 1; i >= 0; --i) sequence = factory.cons(nobinds, factory.constant(split[i]), nobinds, sequence);
                    return rewrapWithProperties(sequence);
                }
            case UPTO_FIRST:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2)) || (arity() > 3 && !Util.isConstant(sub(3)))) break;
                    String string = sub(1).constructor().symbol();
                    String first = sub(2).constructor().symbol();
                    int index = string.indexOf(first);
                    return rewrapWithProperties(index < 0 ? (arity() > 3 ? sub(3) : sub(1)) : factory.constant(string.substring(0, index + first.length())));
                }
            case BEFORE_FIRST:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2)) || (arity() > 3 && !Util.isConstant(sub(3)))) break;
                    String string = sub(1).constructor().symbol();
                    String first = sub(2).constructor().symbol();
                    int index = string.indexOf(first);
                    return rewrapWithProperties(index < 0 ? (arity() > 3 ? sub(3) : sub(1)) : factory.constant(string.substring(0, index)));
                }
            case AFTER_FIRST:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1)) || !Util.isConstant(sub(2)) || (arity() > 3 && !Util.isConstant(sub(3)))) break;
                    String string = sub(1).constructor().symbol();
                    String first = sub(2).constructor().symbol();
                    int index = string.indexOf(first);
                    return rewrapWithProperties(index < 0 ? (arity() > 3 ? sub(3) : factory.constant("")) : factory.constant(string.substring(index + first.length())));
                }
            case EMPTY_SEQUENCE:
                {
                    computeArguments();
                    if (sub(1).kind() == Kind.CONSTRUCTION) {
                        return rewrapWithProperties(Util.isNull(sub(1)) ? factory.constant("True") : factory.nil());
                    }
                    break;
                }
            case CONSTRUCTION:
                {
                    computeArguments();
                    if (arity() == 1 || !Util.isConstant(sub(1))) break;
                    return rewrapWithProperties(unquoteConstructionAndArguments(factory, sub(1).constructor(), (arity() <= 2 ? factory.nil() : sub(2)), renamings));
                }
            case VARIABLE:
                {
                    computeArguments();
                    if (arity() == 1 || !Util.isConstant(sub(1))) break;
                    return rewrapWithProperties(factory.newVariableUse(factory.makeVariable(sub(1).constructor().symbol(), true)));
                }
            case META_APPLICATION:
                {
                    computeArguments();
                    if (arity() == 1 || !Util.isConstant(sub(1)) || arity() == 2 || !SequenceIterator.isSequencing(sub(2))) break;
                    return rewrapWithProperties(factory.newMetaApplication(sub(1).constructor().symbol(), (arity() <= 2 ? NO_TERMS : Util.unconsArray(sub(2), false, renamings))));
                }
            case PROPERTY_NAMED:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1))) break;
                    String name = sub(1).constructor().symbol();
                    Term value = sub(2);
                    Term owner = sub(3);
                    return rewrapWithProperties(GenericTerm.wrapWithProperty(name, value, owner, false));
                }
            case PROPERTY_NAMED_NOT:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1))) break;
                    String name = sub(1).constructor().symbol();
                    Term owner = sub(2);
                    return rewrapWithProperties(GenericTerm.wrapWithNotProperty(name, (GenericTerm) owner, false));
                }
            case PROPERTY_VARIABLE:
                {
                    computeArguments();
                    Variable property;
                    if (sub(1).kind() == Kind.VARIABLE_USE) property = sub(1).variable(); else if (Util.isConstant(sub(1))) property = factory.makeVariable(sub(1).constructor().symbol(), true); else break;
                    Term value = sub(2);
                    Term owner = sub(3);
                    return rewrapWithProperties(GenericTerm.wrapWithProperty(property, value, owner, false));
                }
            case PROPERTY_VARIABLE_NOT:
                {
                    computeArguments();
                    Variable property;
                    if (sub(1).kind() == Kind.VARIABLE_USE) property = sub(1).variable(); else if (Util.isConstant(sub(1))) property = factory.makeVariable(sub(1).constructor().symbol(), true); else break;
                    Term owner = sub(2);
                    try {
                        return rewrapWithProperties(GenericTerm.wrapWithNotProperty(property, (GenericTerm) owner, false));
                    } catch (CRSException e) {
                        break;
                    }
                }
            case PROPERTY_COLLECT:
                {
                    computeArguments();
                    String metavar;
                    if (sub(1).kind() == Kind.META_APPLICATION && sub(1).arity() == 0) metavar = sub(1).metaVariable(); else if (Util.isConstant(sub(1))) metavar = sub(1).constructor().symbol(); else break;
                    Term owner = sub(2);
                    return rewrapWithProperties(GenericTerm.wrapWithPropertiesRef(metavar, (GenericTerm) owner, false));
                }
            case PROPERTIES:
                {
                    computeArguments();
                    return rewrapWithProperties(unquoteProperties(factory, sub(1), sub(2)));
                }
            case MATCH:
                break;
            case PARSE:
            case PARSE_URL:
            case PARSE_TEXT:
            case PARSE_RESOURCE:
            case LOAD:
            case LOAD_TERM:
                {
                    computeArguments();
                    try {
                        for (int i = 1; i < arity(); ++i) if (!Util.isConstant(sub(i))) break What;
                        String category;
                        String resource;
                        Reader reader;
                        switch(what) {
                            case PARSE:
                            case PARSE_URL:
                                {
                                    URL base = new URL("file:.");
                                    Term parseContextURL = Util.materialize(factory.get(Factory.BASE_URL), false);
                                    if (parseContextURL != null) base = new URL(base, parseContextURL.toString());
                                    if (arity() == 2) {
                                        category = null;
                                        URL url = new URL(base, Util.symbol(sub(1)));
                                        resource = url.toExternalForm();
                                        reader = new InputStreamReader(url.openStream());
                                    } else {
                                        category = Util.isNull(sub(1)) ? null : sub(1).constructor().symbol();
                                        URL url = new URL(base, Util.symbol(sub(2)));
                                        resource = url.toExternalForm();
                                        reader = new InputStreamReader(url.openStream());
                                    }
                                    break;
                                }
                            case PARSE_TEXT:
                                {
                                    category = Util.symbol(sub(1));
                                    resource = null;
                                    reader = new StringReader(Util.symbol(sub(2)));
                                    break;
                                }
                            case PARSE_RESOURCE:
                                {
                                    category = Util.symbol(sub(1));
                                    resource = Util.symbol(sub(2));
                                    reader = Util.resourceReader(factory, resource);
                                    break;
                                }
                            case LOAD:
                                {
                                    resource = Util.symbol(sub(1));
                                    reader = Util.resourceReader(factory, resource);
                                    category = (arity() <= 2 || !Util.isTrue(sub(2)) ? null : Util.symbol(sub(2)));
                                    break;
                                }
                            case LOAD_TERM:
                                {
                                    resource = Util.quoteJavaIdentifierPart(Util.symbol(sub(1)));
                                    reader = Util.resourceReader(factory, resource);
                                    category = null;
                                    break;
                                }
                            default:
                                break What;
                        }
                        if (Util.getInteger(factory, Factory.VERBOSE_OPTION, 0) > 0) factory.message(toString(), false);
                        Buffer buffer = new Buffer(factory);
                        Sink bufferSink = buffer.sink();
                        Map<String, Variable> free = null;
                        if (globals != null) {
                            free = new HashMap<String, Variable>();
                            for (Variable v : globals) free.put(v.name(), v);
                        }
                        factory.parser(factory).parse(bufferSink, category, reader, resource, 1, 1, null, free);
                        Term result = buffer.term(true);
                        assert result != null : "Parser did not build complete term.";
                        return rewrapWithProperties(result);
                    } catch (CRSException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            case SAVE_TERM:
                computeArgument(1);
                if (Util.isConstant(sub(1))) {
                    String resource = Util.quoteJavaIdentifierPart(Util.symbol(sub(1)));
                    try {
                        Appendable w = new WriterAppender(new FileWriter(resource));
                        sub(2).appendTo(w, new HashMap<Variable, String>(), Integer.MAX_VALUE, true);
                    } catch (IOException e) {
                        throw new RuntimeException(toString() + " failed", e);
                    }
                    return sub(3);
                }
                break;
            case SCRIPT:
                computeArgument(2);
                if (arity() == 2 || Util.isConstant(sub(2))) {
                    try {
                        SequenceBuffer result = new SequenceBuffer(factory);
                        Builder builder = factory.builder();
                        builder.load(result.sink(), sub(1));
                        if (arity() > 2) {
                            factory.set(sub(2).constructor().symbol(), (GenericCRS) builder.toCRS(true));
                        }
                        return rewrapWithProperties(result.term(true));
                    } catch (CRSException e) {
                        throw new RuntimeException(toString() + " failed", e);
                    }
                }
                break;
            case NORMALIZE:
                computeArguments();
                if (Util.isConstant(sub(2))) {
                    Constructor c = factory.get(sub(2).constructor().symbol()).constructor();
                    if (c != null && c instanceof CRS) {
                        try {
                            return rewrapWithProperties(((CRS) c).normalize(sub(1)));
                        } catch (CRSException e) {
                        }
                    }
                }
                break;
            case URL:
                computeArguments();
                computeArguments();
                if (Util.isConstant(sub(1)) && (arity() == 2 || Util.isConstant(sub(2)))) {
                    String name = sub(1).constructor().symbol();
                    try {
                        URL base = new URL("file:.");
                        Term parseContextURL = Util.materialize(((GenericFactory) factory).get(Factory.BASE_URL), false);
                        if (parseContextURL != null) base = new URL(base, parseContextURL.toString());
                        if (arity() > 2) base = new URL(base, sub(2).constructor().symbol());
                        URL url = new URL(base, name);
                        return rewrapWithProperties(factory.newConstruction(factory.makeConstructor(url), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                    } catch (MalformedURLException e) {
                    }
                }
                break;
            case IF:
                {
                    Term test = compute(sub(1), null);
                    if (test == null) test = sub(1);
                    if (Util.isConstant(test)) {
                        Constructor c = test.constructor();
                        if (!Util.isNull(c)) {
                            Term t = compute(sub(2), null);
                            return rewrapWithProperties(t != null ? t : sub(2));
                        } else {
                            if (arity() == 3) return rewrapWithProperties(factory.nil()); else {
                                Term t = compute(sub(3), null);
                                return rewrapWithProperties(t != null ? t : sub(3));
                            }
                        }
                    }
                    break;
                }
            case PICK:
                {
                    Term pick = compute(sub(1), null);
                    if (pick == null) pick = sub(1);
                    if (Util.isConstant(pick)) {
                        try {
                            int index = Integer.decode(Util.symbol(pick));
                            Term list = sub(2);
                            if (index < 0) break What;
                            while (index-- > 0) {
                                if (!Util.isCons(list.constructor())) break What;
                                list = list.sub(1);
                            }
                            if (!Util.isCons(list.constructor())) break What;
                            return rewrapWithProperties(sub(0));
                        } catch (NumberFormatException e) {
                        }
                    }
                    break;
                }
            case IF_ZERO:
                {
                    Term test = compute(sub(1), null);
                    if (test == null) test = sub(1);
                    if (Util.isConstant(test)) {
                        Constructor c = test.constructor();
                        if ("0".equals(c.symbol())) {
                            Term t = compute(sub(2), null);
                            return rewrapWithProperties(t != null ? t : sub(2));
                        } else {
                            if (arity() == 3) return rewrapWithProperties(factory.nil()); else {
                                Term t = compute(sub(3), null);
                                return rewrapWithProperties(t != null ? t : sub(3));
                            }
                        }
                    }
                    break;
                }
            case IF_EMPTY:
                {
                    Term test = compute(sub(1), null);
                    if (test == null) test = sub(1);
                    if (Util.isConstant(test)) {
                        Constructor c = test.constructor();
                        if ("".equals(c.symbol()) || Util.isNull(c)) {
                            Term t = compute(sub(2), null);
                            return rewrapWithProperties(t != null ? t : sub(2));
                        } else {
                            if (arity() == 3) return rewrapWithProperties(factory.nil()); else {
                                Term t = compute(sub(3), null);
                                return rewrapWithProperties(t != null ? t : sub(3));
                            }
                        }
                    }
                    break;
                }
            case IF_DEF:
                {
                    Term key = compute(sub(1), null);
                    if (key == null) key = sub(1);
                    boolean defined = false;
                    if (sub(0) instanceof PropertiesConstraintsWrapper) {
                        PropertiesConstraintsWrapper propertiesWrapper = (PropertiesConstraintsWrapper) sub(0);
                        WhichKey: switch(key.kind()) {
                            case CONSTRUCTION:
                                {
                                    String symbol = key.constructor().symbol();
                                    Map<String, Term> properties = propertiesWrapper.getLocalProperties();
                                    if (properties.containsKey(symbol)) {
                                        defined = properties.get(symbol) != null;
                                    } else {
                                        if (propertiesWrapper.hasUnknownKeys()) break What;
                                    }
                                    break;
                                }
                            case VARIABLE_USE:
                                {
                                    Variable variable = key.variable();
                                    Map<Variable, Term> properties = propertiesWrapper.getLocalVariableProperties();
                                    if (properties.containsKey(variable)) {
                                        defined = properties.get(variable) != null;
                                    } else {
                                        if (propertiesWrapper.hasUnknownKeys()) break What;
                                    }
                                    break;
                                }
                            case META_APPLICATION:
                                {
                                    String metaVariable = key.metaVariable();
                                    for (Pair<String, Term> p : propertiesWrapper.getLocalMetaProperties()) {
                                        if (metaVariable.equals(p.getKey())) {
                                            defined = p.getValue() != null;
                                            break WhichKey;
                                        }
                                    }
                                }
                            default:
                                break What;
                        }
                    }
                    if (!defined) {
                        PropertiesHolder properties = (PropertiesHolder) sub(0).constructor();
                        switch(key.kind()) {
                            case CONSTRUCTION:
                                defined = properties.getProperty(key.constructor().symbol()) != null;
                                break;
                            case VARIABLE_USE:
                                defined = properties.getProperty(key.variable()) != null;
                                break;
                            default:
                                break What;
                        }
                    }
                    if (!defined && Util.isConstant(sub(1))) {
                        defined = ((GenericFactory) factory).defined(sub(1).constructor().symbol());
                    }
                    if (defined) {
                        Term t = compute(sub(2), null);
                        return rewrapWithProperties(t != null ? t : sub(2));
                    } else {
                        if (arity() == 3) return factory.nil(); else {
                            Term t = compute(sub(3), null);
                            return rewrapWithProperties(t != null ? t : sub(3));
                        }
                    }
                }
            case IF_LINEAR:
                {
                    Term use = compute(sub(1), null);
                    if (use == null) use = sub(1);
                    if (use.kind() != Kind.VARIABLE_USE) break What;
                    return (!use.variable().promiscuous() ? sub(2) : arity() == 3 ? factory.nil() : sub(3));
                }
            case GET:
                {
                    Term key = compute(sub(1), null);
                    if (key == null) key = sub(1);
                    if (key.kind() == Kind.VARIABLE_USE || Util.isConstant(key)) {
                        Term value = null;
                        PropertiesHolder properties = (sub(0) instanceof PropertiesConstraintsWrapper ? (PropertiesHolder) sub(0) : sub(0).constructor());
                        switch(key.kind()) {
                            case CONSTRUCTION:
                                value = properties.getProperty(key.constructor().symbol());
                                break;
                            case VARIABLE_USE:
                                value = properties.getProperty(key.variable());
                                break;
                        }
                        if (value == null && Util.isConstant(key)) {
                            Stub lookup = ((GenericFactory) factory).get(key.constructor().symbol());
                            if (lookup != null) value = lookup.copy(false, null);
                        }
                        if (value != null) return rewrapWithProperties(value);
                        if (arity() > 2) {
                            value = compute(sub(2), null);
                            return rewrapWithProperties(value != null ? value : sub(2));
                        }
                    }
                    break;
                }
            case SORT_OF:
                {
                    Term term = sub(1);
                    if (term.kind() == Kind.CONSTRUCTION) {
                        Set<String> sorts = factory.sortsOf(term.constructor());
                        Term t = factory.nil();
                        if (sorts != null) for (String sort : sorts) t = factory.cons(NO_BIND, factory.constant(sort), NO_BIND, t);
                        return t;
                    }
                    return factory.nil();
                }
            case CHECK_SORT_OF:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1))) break;
                    String sort = Util.symbol(sub(1));
                    Term term = sub(2);
                    if (term.kind() == Kind.CONSTRUCTION && factory.sortsOf(term.constructor()).contains(sort)) {
                        try {
                            factory.checkSorts("Sort error: ", term, new HashMap<String, Pair<String, Term[]>>(), new HashMap<Variable, String>(), true, false);
                            return factory.truth();
                        } catch (CRSException e) {
                            System.err.println("Sort error: " + e.getMessage());
                        }
                    }
                    return factory.nil();
                }
            case PRINT:
                computeArguments();
                try {
                    sub(1).appendTo(System.out, new HashMap<Variable, String>(), Integer.MAX_VALUE, true);
                    System.out.println();
                    System.out.flush();
                    return rewrapWithProperties(arity() == 2 ? sub(1) : sub(2));
                } catch (IOException e) {
                }
                break;
            case FORMAT_NUMBER:
                {
                    computeArguments();
                    if (!Util.isConstant(sub(1))) break;
                    if (arity() == 2) return rewrapWithProperties(factory.constant(Util.symbol(sub(1)))); else break;
                }
            case SHOW:
                return rewrapWithProperties(factory.constant(sub(1).toString()));
            case COMPUTE:
                return rewrapWithProperties(sub(1));
            case ECHO:
                computeArguments();
                System.out.print(sub(1).constructor().symbol());
                System.out.flush();
                return rewrapWithProperties(arity() == 2 ? factory.nil() : sub(2));
            case DUMP:
                try {
                    Map<Variable, String> used = new HashMap<Variable, String>();
                    sub(1).appendTo(System.out, used, Integer.MAX_VALUE, true);
                    System.out.print(" ");
                    sub(2).appendTo(System.out, used, Integer.MAX_VALUE, true);
                    System.out.println();
                    System.out.flush();
                    return rewrapWithProperties(sub(2));
                } catch (IOException e) {
                }
                break;
            case TRACE:
                try {
                    sub(1).appendTo(System.err, new HashMap<Variable, String>(), Integer.MAX_VALUE, true);
                    System.err.println();
                    System.err.flush();
                    return rewrapWithProperties(arity() == 2 ? sub(1) : sub(2));
                } catch (IOException e) {
                }
                break;
            case ERROR:
                computeArguments();
                for (int i = 0; i < arity(); ++i) if (!Util.isConstant(sub(i))) break What;
                error();
                break;
            case IGNORE:
                break;
        }
        return null;
    }

    /**
	 * Add properties on this term to a generated term.
	 * @param term to add the properties to
	 * @return term with added properties
	 */
    Term rewrapWithProperties(Term term) {
        try {
            return GenericTerm.wrapWithPropertiesOf((GenericTerm) term, this);
        } catch (CRSException e) {
            return null;
        }
    }

    /** Helper to trigger runtime error using {@link RuntimeException} to support $[Error,..]. */
    void error() {
        switch(arity()) {
            case 1:
                throw new RuntimeException();
            case 2:
                throw new RuntimeException(sub(1).toString());
            case 3:
                RuntimeException ex;
                try {
                    Class<?> clazz = Class.forName(sub(2).constructor().symbol());
                    java.lang.reflect.Constructor<?> c = clazz.getConstructor(String.class);
                    ex = (RuntimeException) c.newInstance(sub(1).toString());
                } catch (ClassCastException e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + (arity() == 3 ? "," + sub(2) : "") + "] construction", e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + "," + sub(2) + "] construction", e);
                } catch (SecurityException e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + "," + sub(2) + "] construction", e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + "," + sub(2) + "] construction", e);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + "," + sub(2) + "] construction", e);
                } catch (InstantiationException e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + "," + sub(2) + "] construction", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + "," + sub(2) + "] construction", e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + "," + sub(2) + "] construction", e);
                } catch (Throwable e) {
                    throw new RuntimeException("Error in $[Error," + sub(1) + "," + sub(2) + "] construction", e);
                }
                throw ex;
        }
    }

    /**
	 * Create evaluator instance.
	 * @param factory to use
	 * @param constructor to use (usually $).
	 * @param binds bindings for subterms
	 * @param sub subterms
	 */
    public GenericEvaluator(GenericFactory factory, Constructor constructor, Variable[][] binds, Term[] sub) {
        super(factory, constructor, binds, sub);
    }

    public void visit(Visitor visitor, ExtensibleSet<Variable> bound) throws CRSException {
        Primitive what = primitive();
        visitor.visitPrimitive(what, true);
        switch(what) {
            case CONSTRUCTION:
            case META_APPLICATION:
            case VARIABLE:
                super.visit(visitor, bound);
                break;
            case PROPERTY_COLLECT:
            case PROPERTY_NAMED:
            case PROPERTY_NAMED_NOT:
            case PROPERTY_VARIABLE:
            case PROPERTY_VARIABLE_NOT:
                visitor.visitConstruction(this, true, bound);
                visitor.visitConstructor(constructor(), true, bound);
                constructor().visit(visitor, bound);
                visitor.visitConstructor(constructor(), false, bound);
                visitor.visitConstructionSub(this, 0, true, bound);
                sub(0).visit(visitor, bound.extend(binders(0)));
                visitor.visitConstructionSub(this, 0, false, bound);
                visitor.visitConstructionSub(this, 1, true, bound);
                sub(1).visit(visitor, bound.extend(binders(1)));
                visitor.visitConstructionSub(this, 1, false, bound);
                visitor.visitPrimitiveNormal(what, true);
                for (int i = 2; i < arity(); ++i) {
                    visitor.visitConstructionSub(this, i, true, bound);
                    sub(i).visit(visitor, bound.extend(binders(i)));
                    visitor.visitConstructionSub(this, i, false, bound);
                }
                visitor.visitConstruction(this, false, bound);
                visitor.visitPrimitiveNormal(what, false);
                break;
            default:
                visitor.visitConstruction(this, true, bound);
                visitor.visitConstructor(constructor(), true, bound);
                constructor().visit(visitor, bound);
                visitor.visitConstructor(constructor(), false, bound);
                if (arity() > 0) {
                    visitor.visitConstructionSub(this, 0, true, bound);
                    sub(0).visit(visitor, bound.extend(binders(0)));
                    visitor.visitConstructionSub(this, 0, false, bound);
                    visitor.visitPrimitiveNormal(what, true);
                    for (int i = 1; i < arity(); ++i) {
                        visitor.visitConstructionSub(this, i, true, bound);
                        sub(i).visit(visitor, bound.extend(binders(i)));
                        visitor.visitConstructionSub(this, i, false, bound);
                    }
                }
                visitor.visitConstruction(this, false, bound);
                visitor.visitPrimitiveNormal(what, false);
                break;
        }
        visitor.visitPrimitive(what, false);
    }

    @Override
    public boolean match(Match match, Term term, ExtensibleSet<Variable> bound, Map<String, Integer> contractionCount, boolean promiscuous, Collection<Variable> once, Collection<Variable> onceSeen) {
        Primitive what = primitive();
        switch(what) {
            case CONSTRUCTION:
                {
                    if (term.kind() != Kind.CONSTRUCTION) return false;
                    Constructor c = term.constructor();
                    if (GenericTerm.BIND.equals(c.symbol()) || GenericTerm.BIND_LINEAR.equals(c.symbol())) return false;
                    if (sub(0).arity() == 1) {
                        if (!factory.isData(c)) return false;
                        Pattern sortPattern = (Pattern) sub(0).sub(0);
                        Set<String> sorts = factory.sortsOf(c);
                        boolean ok = false;
                        for (String sort : sorts) if (sortPattern.match(match, factory.constant(sort), bound, contractionCount, promiscuous, once, onceSeen)) {
                            ok = true;
                            break;
                        }
                        if (!ok) return false;
                    }
                    if (arity() == 1) return true;
                    if (!matchConstructor(factory, match, (Pattern) sub(1), c, bound, contractionCount, promiscuous, once, onceSeen)) return false;
                    if (arity() == 2) return true; else {
                        Term quotedArguments = quoteArguments(factory, term);
                        return ((Pattern) sub(2)).match(match, quotedArguments, bound, contractionCount, promiscuous, once, onceSeen);
                    }
                }
            case VARIABLE:
                {
                    if (term.kind() != Kind.VARIABLE_USE) return false;
                    if (arity() == 1) return true;
                    Buffer buffer = new Buffer(factory);
                    buffer.sink().start(factory.makeConstructor(term.variable().name())).end();
                    return ((Pattern) sub(1)).match(match, buffer.term(true), bound, contractionCount, promiscuous, once, onceSeen);
                }
            case META_APPLICATION:
                {
                    if (term.kind() != Kind.META_APPLICATION) return false;
                    if (arity() == 1) return true;
                    if (!((Pattern) sub(1)).match(match, factory.constant(term.metaVariable()), bound, contractionCount, promiscuous, once, onceSeen)) return false;
                    if (arity() == 2) return true; else {
                        Term quotedArguments = quoteArguments(factory, term);
                        return ((Pattern) sub(2)).match(match, quotedArguments, bound, contractionCount, promiscuous, once, onceSeen);
                    }
                }
            case PROPERTIES:
                {
                    if (!((Pattern) sub(2)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen)) return false;
                    Term termProperties = quoteProperties(term);
                    return ((Pattern) sub(1)).match(match, termProperties, bound, contractionCount, promiscuous, once, onceSeen);
                }
            case PROPERTY_NAMED:
            case PROPERTY_NAMED_NOT:
                {
                    Term nameTerm = sub(1);
                    if (nameTerm.kind() == Kind.META_APPLICATION) {
                        Substitute s = match.getSubstitute(nameTerm.metaVariable());
                        if (s == null || s.getBindings().length > 0) throw new RuntimeException("Match error: NamedProperty with non-constant pattern.");
                        nameTerm = s.getBody();
                    }
                    if (!Util.isConstant(nameTerm)) throw new RuntimeException("Match error: NamedProperty with non-constant name.");
                    String name = Util.symbol(nameTerm);
                    PropertiesHolder properties = (term instanceof PropertiesHolder ? (PropertiesHolder) term : term.constructor());
                    Pattern restPattern;
                    if (what == Primitive.PROPERTY_NAMED) {
                        if (properties == null) return false;
                        Term value = properties.getProperty(name);
                        if (value == null || !((Pattern) sub(2)).match(match, value, bound, contractionCount, promiscuous, once, onceSeen)) return false;
                        restPattern = (Pattern) sub(3);
                    } else {
                        if (properties != null) {
                            Term value = properties.getProperty(name);
                            if (value != null) return false;
                        }
                        restPattern = (Pattern) sub(2);
                    }
                    return restPattern.match(match, term, bound, contractionCount, promiscuous, once, onceSeen);
                }
            case PROPERTY_VARIABLE:
            case PROPERTY_VARIABLE_NOT:
                {
                    Term variableTerm = sub(1);
                    if (variableTerm.kind() == Kind.META_APPLICATION) {
                        Substitute s = match.getSubstitute(variableTerm.metaVariable());
                        if (s == null || s.getBindings().length > 0) throw new RuntimeException("Match error: VariableProperty with non-variable pattern.");
                        variableTerm = s.getBody();
                    }
                    if (variableTerm.kind() == Kind.VARIABLE_USE) throw new RuntimeException("Match error: VariableProperty with non-variable pattern.");
                    Variable variable = variableTerm.variable();
                    if (match.renamings().containsKey(variable)) variable = match.renamings().get(variable);
                    PropertiesHolder properties = (term instanceof PropertiesHolder ? (PropertiesHolder) term : term.constructor());
                    Pattern restPattern;
                    if (what == Primitive.PROPERTY_VARIABLE) {
                        if (properties == null) return false;
                        Term value = properties.getProperty(variable);
                        if (value == null || !((Pattern) sub(2)).match(match, value, bound, contractionCount, promiscuous, once, onceSeen)) return false;
                        restPattern = (Pattern) sub(3);
                    } else {
                        if (properties != null) {
                            Term value = properties.getProperty(variable);
                            if (value != null) return false;
                        }
                        restPattern = (Pattern) sub(2);
                    }
                    return restPattern.match(match, term, bound, contractionCount, promiscuous, once, onceSeen);
                }
            case PROPERTY_COLLECT:
                {
                    if (!Util.isConstant(sub(1))) return false;
                    String metavar = sub(1).constructor().symbol();
                    Term owner = sub(2);
                    return ((Pattern) GenericTerm.wrapWithPropertiesRef(metavar, ((GenericTerm) owner), false)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen);
                }
            case MATCH:
                {
                    assert arity() == 3 : "Match pattern needs two patterns?";
                    return ((Pattern) sub(1)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen) && ((Pattern) sub(2)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen);
                }
            case NOT_MATCH:
                {
                    if (((Pattern) sub(1)).match(match.clone(), term, bound, contractionCount, promiscuous, once, onceSeen)) return false;
                    return arity() == 2 || ((Pattern) sub(2)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen);
                }
            case MATCH_REGEX:
                {
                    String r = unquote(sub(1).constructor().symbol());
                    java.util.regex.Pattern regex = java.util.regex.Pattern.compile(r);
                    return Util.isConstant(term) && regex.matcher(term.constructor().symbol()).matches() && (arity() == 2 || ((Pattern) sub(2)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen));
                }
            case IS_INTEGER:
                {
                    return Util.isConstant(term) && INTEGER_REGEX.matcher(Util.symbol(term)).matches() && (arity() == 1 || ((Pattern) sub(1)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen));
                }
            case PLUS:
                {
                    if (!Util.isConstant(sub(1)) && !Util.isConstant(sub(2))) return false;
                    int constantArg = (Util.isConstant(sub(1)) ? 1 : 2);
                    if (!INTEGER_REGEX.matcher(Util.symbol(sub(constantArg))).matches()) return false;
                    final int n = Integer.parseInt(Util.symbol(sub(constantArg)));
                    int metaArg = 3 - constantArg;
                    if (!Util.isMetaApplication(sub(metaArg))) return false;
                    if (!((Pattern) sub(metaArg)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen)) return false;
                    String arg = sub(metaArg).metaVariable();
                    final Substitute argSubstitute = match.getSubstitute(arg);
                    if (!Util.isConstant(argSubstitute.getBody())) return false;
                    Substitute minusSubstitute = new Substitute() {

                        public boolean rematch(Match match, Variable[] binders, Term term) {
                            return argSubstitute.rematch(match, binders, term);
                        }

                        public Copyable substitute(Valuation valuation, Term[] replacement) {
                            return argSubstitute.substitute(valuation, replacement);
                        }

                        public Variable[] getBindings() {
                            return argSubstitute.getBindings();
                        }

                        public Term getBody() {
                            int arg = Integer.parseInt(Util.symbol(argSubstitute.getBody()));
                            return factory.constant(arg - n);
                        }
                    };
                    match.putSubstitute(arg, minusSubstitute);
                    return true;
                }
            case IF:
                return Util.isConstant(sub(1)) && ((Pattern) sub(sub(1).constructor().equals(factory.nilConstructor) ? 3 : 2)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen);
            case GET:
                if (Util.isConstant(sub(1))) {
                    Term value = Util.materialize(((GenericFactory) factory).get(sub(1).constructor().symbol()), false);
                    return ((Pattern) (value != null || arity() == 2 ? value : sub(2))).match(match, term, bound, contractionCount, promiscuous, once, onceSeen);
                }
                return false;
            case PRINT:
                try {
                    sub(1).appendTo(System.out, new HashMap<Variable, String>(), Integer.MAX_VALUE, true);
                    System.out.println();
                    System.out.flush();
                } catch (IOException e) {
                    throw new UnsupportedOperationException(e);
                }
                return (arity() == 2 ? false : ((Pattern) sub(2)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen));
            case DUMP:
                try {
                    if (sub(2).kind() != Kind.META_APPLICATION || binders(2).length != 0) throw new UnsupportedOperationException(toString());
                    Map<Variable, String> used = new HashMap<Variable, String>();
                    sub(1).appendTo(System.out, used, Integer.MAX_VALUE, true);
                    System.out.print(" ");
                    if (((Pattern) sub(2)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen)) {
                        match.getSubstitute(sub(2).metaVariable()).getBody().appendTo(System.out, used, Integer.MAX_VALUE, true);
                        sub(2).appendTo(System.out, used, Integer.MAX_VALUE, true);
                        System.out.println();
                        System.out.flush();
                        return true;
                    } else {
                        System.out.println("#FAIL");
                        System.out.flush();
                        return false;
                    }
                } catch (IOException e) {
                    throw new UnsupportedOperationException(e);
                }
            case TRACE:
                try {
                    sub(1).appendTo(System.err, new HashMap<Variable, String>(), Integer.MAX_VALUE, true);
                    System.err.println();
                    System.err.flush();
                } catch (IOException e) {
                    throw new UnsupportedOperationException(e);
                }
                return (arity() == 2 ? false : ((Pattern) sub(2)).match(match, term, bound, contractionCount, promiscuous, once, onceSeen));
            case ERROR:
                error();
                break;
        }
        return super.match(match, term, bound, contractionCount, promiscuous, once, onceSeen);
    }

    private static boolean matchConstructor(GenericFactory factory, Match match, Pattern pattern, Constructor constructor, ExtensibleSet<Variable> bound, Map<String, Integer> contractionCount, boolean promiscuous, Collection<Variable> once, Collection<Variable> onceSeen) {
        Buffer buffer = new Buffer(factory);
        buffer.sink().start(constructor).end();
        return pattern.match(match, buffer.term(true), bound, contractionCount, promiscuous, once, onceSeen);
    }

    /**
	 * Interprets list of arguments in quoted form: ( $B x1 y1 z1 . sub1; ....; $BL xn . subn; ).
	 * Note that the variables and subterms are shared with the original!
	 * @param factory for term creation
	 * @param parent term to quote...subterms and variables are shared between this and the returned form
	 */
    private static Term unquoteConstructionAndArguments(GenericFactory factory, Constructor constructor, Term quotedArguments, ExtensibleMap<Variable, Variable> renamings) {
        List<Term> subs = new ArrayList<Term>();
        List<Variable[]> binders = new ArrayList<Variable[]>();
        Term rest = quotedArguments;
        while (Util.isCons(rest.constructor())) {
            List<Variable> bs = new ArrayList<Variable>();
            for (int j = 0; j < rest.binders(0).length; ++j) bs.add(rest.binders(0)[j]);
            Term a = rest.sub(0);
            while (a.kind() == Kind.CONSTRUCTION && (GenericTerm.BIND.equals(a.constructor().symbol()) || GenericTerm.BIND_LINEAR.equals(a.constructor().symbol()))) {
                assert a.arity() == 1 && a.binders(0).length == 1;
                Variable b = a.binders(0)[0];
                bs.add(renamings.containsKey(b) ? renamings.get(b) : b);
                a = a.sub(0);
            }
            binders.add(bs.toArray(new Variable[0]));
            subs.add(a.copy(true, renamings));
            rest = rest.sub(1);
        }
        return factory.newConstruction(constructor.copy(renamings), binders.toArray(new Variable[0][]), subs.toArray(new Term[0]));
    }

    /**
	 * Return list of the subterms of parent in quoted form: ( $B x1 y1 z1 . sub1; ....; $B xn . subn; ).
	 * Note that the variables and subterms are shared with the original!
	 * @param factory for term creation
	 * @param parent term to quote...subterms and variables are shared between this and the returned form
	 */
    private static Term quoteArguments(GenericFactory factory, Term parent) {
        Term quoted = factory.nil();
        for (int i = parent.arity() - 1; i >= 0; --i) {
            Term sub = parent.sub(i);
            Variable[] binds = parent.binders(i);
            for (int j = binds.length - 1; j >= 0; --j) {
                Variable[][] bs = new Variable[1][];
                bs[0] = new Variable[1];
                bs[0][0] = binds[j];
                Term[] ts = new Term[1];
                ts[0] = sub;
                sub = factory.newConstruction(factory.makeConstructor(binds[j].promiscuous() ? GenericTerm.BIND : GenericTerm.BIND_LINEAR), bs, ts);
            }
            quoted = factory.cons(GenericTerm.NO_BIND, sub, GenericTerm.NO_BIND, quoted);
        }
        return quoted;
    }

    /**
	 * Construct properties from list in {@link Primitive#PROPERTIES} format.
	 * @param factory for construction?
	 * @param props list of property units
	 * @param term to wrap the properties around
	 * @return
	 */
    private static Term unquoteProperties(Factory<? extends Term> factory, Term props, Term term) {
        assert (false);
        return null;
    }

    /**
	 * Explicit properties in the format used by {@link Primitive#PROPERTIES}.
	 * @param term with properties
	 * @return list of just the quoted properties
	 */
    private static Term quoteProperties(Term term) {
        PropertiesHolder ps = (term instanceof PropertiesHolder ? (PropertiesHolder) term : term.constructor());
        Buffer buffer = new Buffer(term.maker());
        Sink sink = buffer.sink();
        int conses = 0;
        if (ps != null) {
            String ref = null;
            Map<String, Term> namedProps;
            Map<Variable, Term> variableProps;
            List<Pair<String, Term>> metaProps = new ArrayList<Pair<String, Term>>();
            if (ps instanceof PropertiesConstraintsWrapper) {
                PropertiesConstraintsWrapper pcw = (PropertiesConstraintsWrapper) ps;
                ref = pcw.propertiesRef;
                namedProps = pcw.namedPropertyConstraints;
                variableProps = pcw.variablePropertyConstraints;
                metaProps = pcw.metaPropertyConstraints;
            } else {
                namedProps = new HashMap<String, Term>();
                for (String name : ps.propertyNames()) namedProps.put(name, ps.getProperty(name));
                variableProps = new HashMap<Variable, Term>();
                for (Variable variable : ps.propertyVariables()) variableProps.put(variable, ps.getProperty(variable));
            }
            if (ref != null) {
                sink = sink.start(sink.makeConstructor(CRS.CONS_SYMBOL));
                ++conses;
                sink = sink.start(sink.makeConstructor("$RP")).startMetaApplication(ref).endMetaApplication().end();
            }
            for (Map.Entry<String, Term> e : namedProps.entrySet()) {
                sink = sink.start(sink.makeConstructor(CRS.CONS_SYMBOL));
                ++conses;
                sink = sink.start(sink.makeConstructor("$NP")).start(sink.makeConstructor(e.getKey())).end().copy(e.getValue(), false).end();
            }
            for (Map.Entry<Variable, Term> e : variableProps.entrySet()) {
                sink = sink.start(sink.makeConstructor(CRS.CONS_SYMBOL));
                ++conses;
                sink = sink.start(sink.makeConstructor("$VP")).use(e.getKey()).copy(e.getValue(), false).end();
            }
            for (Pair<String, Term> p : metaProps) {
                sink = sink.start(sink.makeConstructor(CRS.CONS_SYMBOL));
                ++conses;
                sink = sink.start(sink.makeConstructor("$MP")).startMetaApplication(p.getKey()).endMetaApplication().copy(p.getValue(), false).end();
            }
        }
        sink = sink.start(sink.makeConstructor(CRS.NIL_SYMBOL)).end();
        while (conses-- > 0) sink = sink.end();
        return buffer.term(true);
    }

    @Override
    public Sink contract(Sink sink, Valuation valuation, ExtensibleMap<Variable, Variable> renamings) {
        final int arity = arity();
        GenericEvaluator contracted;
        {
            Variable[][] cbinders = new Variable[arity][];
            Term[] csub = new Term[arity];
            for (int i = 0; i < arity; ++i) {
                cbinders[i] = binders(i);
                csub[i] = Buffer.contraction(((Contractum) sub(i)), valuation, renamings);
            }
            contracted = new GenericEvaluator(factory, constructor(), cbinders, csub);
        }
        Primitive cwhat = contracted.primitive();
        Term computed = contracted.compute(cwhat, renamings, null);
        if (computed == null) return sink.copy(contracted, true);
        if (!(computed instanceof Contractum)) throw new RuntimeException("CRS rewrite error: could not contract evaluator " + this + " (not a Contractum instance)");
        return cwhat == Primitive.LOAD_TERM ? computed.copy(sink, false, renamings) : ((Contractum) computed).contract(sink, valuation, renamings);
    }

    @Override
    public GenericTerm makeUnificationCopy(StackedMap<Variable> varrenamings, Map<String, String> mvarrenamings) {
        ExtensibleMap<Variable, Variable> renamings = new LinkedExtensibleMap<Variable, Variable>();
        final Constructor c = constructor().copy(renamings.extend(varrenamings));
        final int a = arity();
        final Variable[][] bss = new Variable[a][];
        final Term[] ss = new Term[a];
        for (int i = 0; i < a; ++i) {
            final Variable[] bs = new Variable[binders(i).length];
            varrenamings.pushscope();
            for (int j = 0; j < bs.length; ++j) {
                Variable v = binders(i)[j];
                bs[j] = v;
                varrenamings.put(v, v);
            }
            bss[i] = bs;
            ss[i] = ((GenericTerm) sub(i)).makeUnificationCopy(varrenamings, mvarrenamings);
            varrenamings.popscope();
        }
        return new GenericEvaluator(factory, c, bss, ss);
    }

    @Override
    protected Unification unifyThese(Unification unification, Pattern that, StackedMap<Variable> rho, StackedMap<Variable> rhoprime, Set<String> existingMVars) throws CRSException {
        Primitive what = primitive();
        switch(what) {
            case CONSTRUCTION:
                {
                    assert this.arity() == 3 : "$[C...] pattern of arity != 3.";
                    assert this.sub[1] instanceof GenericMetaApplication && this.sub[2] instanceof GenericMetaApplication : "$[C...] pattern with non-metavariable child";
                    switch(that.kind()) {
                        case CONSTRUCTION:
                            {
                                if (that instanceof GenericEvaluator) {
                                    Primitive thatwhat = ((GenericEvaluator) that).primitive();
                                    switch(thatwhat) {
                                        case CONSTRUCTION:
                                            {
                                                assert false : "$[C...] pattern being unified with $[C...] pattern - duplicate rule?";
                                            }
                                        case NOT_MATCH:
                                            {
                                                return null;
                                            }
                                        default:
                                            {
                                                assert false : "Unknown evaluator pattern type in unification";
                                            }
                                    }
                                }
                                unification.putSubstitute(((GenericMetaApplication) this.sub(1)).metaVariable, new Variable[0], factory.newConstruction(that.constructor(), GenericTerm.NO_BINDS, GenericTerm.NO_TERMS));
                                Pattern tail = (Pattern) factory.nil();
                                final Variable[] nobind = {};
                                for (int i = that.arity() - 1; i >= 0; --i) {
                                    tail = (Pattern) factory.cons(that.binders(i), that.sub(i), nobind, tail);
                                }
                                unification.putSubstitute(((GenericMetaApplication) this.sub(2)).metaVariable, new Variable[0], tail);
                                return unification;
                            }
                        case VARIABLE_USE:
                        case META_APPLICATION:
                            {
                                return null;
                            }
                        default:
                            {
                                assert false : "Unknown pattern type being unified with $ rule";
                                return null;
                            }
                    }
                }
            case NOT_MATCH:
                {
                    assert this.arity() == 3 : "$[NotMatch...] pattern of arity != 3.";
                    if (that instanceof GenericEvaluator) {
                        Primitive thatwhat = ((GenericEvaluator) that).primitive();
                        switch(thatwhat) {
                            case CONSTRUCTION:
                                {
                                    return null;
                                }
                            case NOT_MATCH:
                                {
                                    return null;
                                }
                            default:
                                {
                                    throw new CRSException("Attempt to unify $[NotMatch, ...] pattern against unknown $ pattern");
                                }
                        }
                    } else {
                        if (this.sub(1).equals(that)) {
                            return null;
                        }
                        return ((GenericTerm) this.sub(2)).unifyThese(unification, that, rho, rhoprime, existingMVars);
                    }
                }
            default:
                {
                    throw new CRSException("Unknown $ pattern encountered in unification: " + this);
                }
        }
    }

    @Override
    public GenericTerm applySubstitutes(Unification unification) {
        return super.applySubstitutes(unification);
    }
}
