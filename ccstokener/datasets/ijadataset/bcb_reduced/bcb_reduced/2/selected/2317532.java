package ws.prova;

import org.mandarax.kernel.*;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Model;
import ws.prova.parser.AtomicConsult;
import ws.prova.parser.ParsingException;
import ws.prova.parser.RulebaseParser;
import ws.prova.reference.ExceptionHandler;
import ws.prova.reference.ConstantTermImpl;
import ws.prova.util.DefaultLogicFactorySupport;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Built-in predicates and parser class.
 * NOTE: For all built-in predicates, if all arguments are bound,
 * it is a responsibility of
 * a built-in predicate to ensure that they are consistent.
 * <p>Title: Prova </p>
 * <p>Description: Built-in predicates</p>
 * <p>Copyright: Copyright (C) 2002-2007</p>
 * <p>Company: City University, London, United Kingdom</p>
 *
 * @author <A HREF="mailto:a.kozlenkov@city.ac.uk">Alex Kozlenkov</A>
 * @version 2.0 <25 March 2007>
 */
public final class Calc {

    private final reagent prova;

    private final Map map_tid = new HashMap();

    private final SwingAdaptor adaptor;

    private int unique_id = 0;

    private Messenger messenger;

    private Updater updater;

    public Calc(reagent prova) {
        this.prova = prova;
        this.messenger = prova.getMessenger();
        this.updater = prova.getUpdater();
        adaptor = new SwingAdaptor(prova);
    }

    /**
	 * A helper method attempting to retrieve a Class given its fully qualified name
	 *
	 * @param className String
	 * @return Class
	 * @throws ClassNotFoundException
	 */
    public static Class forName(String className) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    /**
	 * A default procedure for finding classes given their simple or
	 * qualified names in Prova scripts.
	 * Multiple attempts are made to discover the class.
	 *
	 * @param var String the name of the class or a fully qualified name.
	 * @return Class
	 */
    public static Class findClass(String var) {
        Class cl = null;
        try {
            try {
                try {
                    cl = forName("ws.prova." + var);
                } catch (Exception exc) {
                    cl = forName("java.lang." + var);
                }
            } catch (Exception ex) {
                cl = forName(var);
            }
        } catch (ClassNotFoundException ex) {
        }
        return cl;
    }

    /**
	 * Invoke a Java method that does not return any objects in addition to the Boolean success flag
	 *
	 * @param list: the target object, method name, followed by the method parameters
	 * @return success flag returned by the method
	 */
    public Boolean invoke(RList list) {
        Vector args = list.Objects();
        int iarg = args.size();
        Object target = args.elementAt(--iarg);
        Class cl = target.getClass();
        String pred = (String) args.elementAt(--iarg);
        Object[] objs = new Object[iarg];
        Class[] par = new Class[iarg];
        int i = 0;
        while (iarg-- > 0) {
            objs[i++] = args.elementAt(iarg);
            par[iarg] = Object.class;
        }
        list.clear();
        try {
            Boolean rc = (Boolean) cl.getMethod(pred, par).invoke(target, objs);
            return rc;
        } catch (Exception x) {
            x.printStackTrace(System.err);
        }
        return Boolean.FALSE;
    }

    public String toString() {
        return "Calc()";
    }

    /**
	 * Checks whether a string corresponds to a declared poly-predicate
	 *
	 * @param arg1 - a string to be checked
	 * @return Boolean.TRUE if such poly-predicate exists
	 */
    public Boolean poly(Object arg1, Object arg2) {
        Object obj = prova.getPoly(((String) arg1) + '+' + arg2);
        return Boolean.valueOf(obj != null);
    }

    public Boolean inc(Object arg1, Object arg2) {
        int in = ((Integer) arg1).intValue();
        int out = ((Integer) arg2).intValue();
        return Boolean.valueOf(out == in + 1);
    }

    public static Boolean ne(Object arg1, Object arg2) {
        if (!(arg1 instanceof Number) || !(arg2 instanceof Number)) {
            return Boolean.valueOf(!arg1.toString().equals(arg2.toString()));
        }
        Number narg1 = (Number) arg1;
        Number narg2 = (Number) arg2;
        if (narg1 instanceof Float || narg1 instanceof Double || narg2 instanceof Float || narg2 instanceof Double) {
            return Boolean.valueOf(narg1.doubleValue() != narg2.doubleValue());
        }
        return Boolean.valueOf(narg1.intValue() != narg2.intValue());
    }

    public static Boolean lt(Number arg1, Number arg2) {
        if (arg1 instanceof Float || arg1 instanceof Double || arg2 instanceof Float || arg2 instanceof Double) {
            return Boolean.valueOf(arg1.doubleValue() < arg2.doubleValue());
        }
        return Boolean.valueOf(arg1.intValue() < arg2.intValue());
    }

    public static Boolean gt(Number arg1, Number arg2) {
        if (arg1 instanceof Float || arg1 instanceof Double || arg2 instanceof Float || arg2 instanceof Double) {
            return Boolean.valueOf(arg1.doubleValue() > arg2.doubleValue());
        }
        return Boolean.valueOf(arg1.intValue() > arg2.intValue());
    }

    public static Boolean le(Number arg1, Number arg2) {
        if (arg1 instanceof Float || arg1 instanceof Double || arg2 instanceof Float || arg2 instanceof Double) {
            return Boolean.valueOf(arg1.doubleValue() <= arg2.doubleValue());
        }
        return Boolean.valueOf(arg1.intValue() <= arg2.intValue());
    }

    public static Boolean ge(Number arg1, Number arg2) {
        if (arg1 instanceof Float || arg1 instanceof Double || arg2 instanceof Float || arg2 instanceof Double) {
            return Boolean.valueOf(arg1.doubleValue() >= arg2.doubleValue());
        }
        return Boolean.valueOf(arg1.intValue() >= arg2.intValue());
    }

    public Boolean tokenize(Object arg1, List ret) {
        String in = (String) arg1;
        String[] res = in.split(" ");
        for (int i = 0; i < res.length; i++) {
            ret.add(res[i]);
        }
        return Boolean.TRUE;
    }

    /**
	 * Return a new object of the class of the VariableTerm passed as input
	 *
	 * @param r
	 * @param bindings
	 * @return TRUE if the object creation was successful
	 */
    public Boolean create(final Object[] r, final List bindings) {
        Object arg = r[0];
        if (arg instanceof VariableTerm) {
            VariableTerm vt = (VariableTerm) arg;
            try {
                Object[] retobj = new Object[1];
                retobj[0] = vt.getType().newInstance();
                bindings.add(retobj);
                return Boolean.TRUE;
            } catch (Exception ex) {
            }
        }
        return Boolean.FALSE;
    }

    /**
	 * Construct a Java object from Prova.
	 *
	 * @param r		  Object[]
	 *                 <br> Parameters:
	 *                 <br> [in] fully qualified constructor name;
	 *                 <br> [out]|[in] constructed object (may supply an object on input to immediately match it
	 *                 against the constructed object via unification);
	 *                 <br> [in] a Prova list containing the parameters passed to the constructor.
	 * @param bindings
	 * @return currently, it is not important as the <code>bindings</code> determine whether the object was constructed.
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
    public Boolean construct(final Object[] r, final List bindings) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        String constructor = (String) r[0];
        Class cl = forName(constructor);
        List list = new ArrayList();
        Object[] retobj = new Object[3];
        retobj[0] = r[0];
        if (r[2] instanceof ComplexTerm) {
            ComplexTerm ct = (ComplexTerm) r[2];
            RListUtils.rlist2List(ct, list, 0);
            retobj[2] = r[2];
        } else if (r[2] instanceof RList) {
            retobj[2] = RListUtils.emptyRListTerm;
        }
        int arity = list.size();
        Object[] data = new Object[arity];
        Class[] par = new Class[arity];
        int i = 0;
        for (ListIterator li = list.listIterator(); li.hasNext(); i++) {
            Object o = li.next();
            data[i] = o;
            par[i] = o.getClass();
            if (par[i] == Double.class) {
                par[i] = double.class;
            } else if (par[i] == Boolean.class) {
                par[i] = boolean.class;
            } else if (par[i] == Integer.class) {
                par[i] = int.class;
            } else if (par[i] == ArrayList.class) {
                par[i] = List.class;
            }
        }
        try {
            retobj[1] = cl.getConstructor(par).newInstance(data);
            bindings.add(retobj);
        } catch (Exception ex) {
            java.lang.reflect.Constructor[] cs = cl.getConstructors();
            for (int k = 0; k < cs.length; k++) {
                Class[] par2 = cs[k].getParameterTypes();
                if (par2.length != arity) {
                    continue;
                }
                boolean matching = true;
                for (int m = 0; m < arity; m++) {
                    if (data[m] instanceof Number && par2[m].isPrimitive()) {
                        data[m] = ws.prova.calc.CalcClauseSet.adaptNumber(par2[m], data[m]);
                        if (data[m] != null) continue;
                        matching = false;
                        break;
                    } else if (java.util.Collection.class.isAssignableFrom(par2[m]) && data[m] instanceof ComplexTerm) {
                        List l = new ArrayList();
                        RListUtils.rlist2List((ComplexTerm) data[m], l, 0);
                        data[m] = l;
                    } else if (data[m] instanceof Number && par2[m].isPrimitive()) {
                        if (par2[m] == int.class) {
                            data[m] = new Integer(((Number) data[m]).intValue());
                            continue;
                        }
                        if (par2[m] == long.class) {
                            data[m] = new Long(((Number) data[m]).longValue());
                            continue;
                        }
                        if (par2[m] == double.class) {
                            data[m] = new Double(((Number) data[m]).doubleValue());
                            continue;
                        }
                        if (par2[m] == short.class) {
                            data[m] = new Short(((Number) data[m]).shortValue());
                            continue;
                        } else if (par2[m] == float.class) {
                            data[m] = new Float(((Number) data[m]).floatValue());
                            continue;
                        }
                        if (par2[m] == byte.class) {
                            data[m] = new Byte(((Number) data[m]).byteValue());
                            continue;
                        }
                        matching = false;
                        break;
                    } else if (!par2[m].isAssignableFrom(data[m].getClass()) && !par2[m].isAssignableFrom(par[m])) {
                        matching = false;
                        break;
                    }
                }
                if (matching) {
                    Object o = null;
                    o = cs[k].newInstance(data);
                    retobj[1] = o;
                    bindings.add(retobj);
                    return Boolean.FALSE;
                }
            }
            if (ex instanceof NoSuchMethodException) throw (NoSuchMethodException) ex;
        }
        return Boolean.FALSE;
    }

    public Boolean inc(final Object[] r, final List bindings) {
        Object arg0 = r[0];
        Object arg1 = r[1];
        Object[] retobj = new Object[2];
        if (arg0 instanceof VariableTerm) {
            if (arg1 instanceof VariableTerm) {
                return Boolean.FALSE;
            }
            int i1 = ((Integer) arg1).intValue();
            int i0 = i1 - 1;
            retobj[0] = new Integer(i0);
            retobj[1] = arg1;
        } else {
            int i0 = ((Integer) arg0).intValue();
            if (arg1 instanceof VariableTerm) {
                int i1 = i0 + 1;
                retobj[0] = arg0;
                retobj[1] = new Integer(i1);
            } else {
                int i1 = ((Integer) arg1).intValue();
                if (i1 != i0 + 1) {
                    return Boolean.FALSE;
                }
                retobj[0] = arg0;
                retobj[1] = arg1;
            }
        }
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Return all elements in a collection if the supplied element is free.
	 * Return all matching instances, if the supplied element is bound.
	 * !!! Note that if all arguments are bound, it is a responsibility of
	 * a built-in predicate to ensure that they are consistent.
	 *
	 * @param r		  Object[]
	 *                 <br> Parameters:
	 *                 <br> optional: [out] index of the element in the collection;
	 *                 <br> [out]|[in] an element in the collection;
	 *                 <br> [in] a Java Collection or a Java Iterator or a Prova list.
	 * @param bindings List
	 * @return Boolean.TRUE
	 */
    public Boolean element(final Object[] r, final List bindings) {
        Collection list;
        Iterator it = null;
        int where_list = 1;
        if (r.length == 3) {
            where_list = 2;
        }
        if (r[where_list] instanceof Collection) {
            list = (Collection) r[where_list];
            it = list.iterator();
        } else if (r[where_list] instanceof Iterator) {
            it = (Iterator) r[where_list];
        } else if (r[where_list] instanceof ComplexTerm) {
            List list2 = new ArrayList();
            RListUtils.rlist2List((ComplexTerm) r[where_list], list2, 0);
            list = list2;
            it = list.iterator();
        } else if (r[where_list] instanceof ws.prova.RList) {
            list = ((ws.prova.RList) r[where_list]).Objects();
            it = list.iterator();
        }
        int index = 0;
        while (it.hasNext()) {
            Object o = it.next();
            if (!(r[0] instanceof VariableTerm) && !(r[0] instanceof ComplexTerm) && ne(o, r[0]).booleanValue()) continue;
            if (r.length == 3) {
                Object[] retobj = new Object[3];
                retobj[0] = new Integer(index++);
                retobj[1] = o;
                retobj[2] = r[2];
                bindings.add(retobj);
                continue;
            }
            Object[] retobj = new Object[2];
            retobj[0] = o;
            retobj[1] = r[1];
            bindings.add(retobj);
        }
        return Boolean.TRUE;
    }

    /**
	 * This code requires Jena 2.4 to be on compilation path.
	 * Jena JAR's are not required at run-time if Jena functionality is not used.
	 * <br>
	 * Non-deterministic SPARQL wrapper for Prova. The SELECT and ASK functionality are supported.
	 * Each binding translates to one instantiation of variables in name-value pairs.
	 *
	 * @param r		  Object[]
	 *                 <br> Parameters:
	 *                 <br> optional: [in] Jena model with the RDF data;
	 *                 <br> [in] SPARQL query as a String
	 *                 <br> [out] the first name(Value) pair where 'name' is expected to be a constant term representing the name of the variable to be returned
	 *                 <br> [out] ...
	 *                 <br> [out] the last name(Value) pair where 'name' is expected to be a constant term representing the name of the variable to be returned
	 *                 <p/>
	 *                 The list of name/value pairs can also have a list tail (rest) as a variable, in which case
	 *                 all binding name/value pairs that are not explicitly listed, will be bound to that list tail
	 *                 <p/>
	 *                 For example,
	 *                 <br>  sparql_select(QueryString,url(URL),type(Type)|NVPairs)
	 *                 <p/>
	 *                 The number of supplied name/value pairs may be a subset of those effectively returned by the query.
	 *                 If any supplied names are not part of the returned bindings, the query fails.
	 *                 If there are no supplied name/value pairs and there is no rest, the predicate will simply test whether the query
	 *                 returns anything matching, in which case it will succeed.
	 * @param bindings The resulting bindings
	 * @return Boolean.TRUE
	 */
    public Boolean sparql_select(final Object[] r, final List bindings) {
        int index = 0;
        Model model = null;
        if ((r[index] instanceof Model)) {
            index++;
            model = (Model) r[0];
        }
        String queryString = (String) r[index++];
        com.hp.hpl.jena.query.Query query = com.hp.hpl.jena.query.QueryFactory.create(queryString);
        com.hp.hpl.jena.query.QueryExecution queryExecution = model == null ? com.hp.hpl.jena.query.QueryExecutionFactory.create(query) : com.hp.hpl.jena.query.QueryExecutionFactory.create(query, model);
        com.hp.hpl.jena.query.ResultSet results = queryExecution.execSelect();
        int numVars = results.getResultVars().size();
        List names = new ArrayList();
        boolean restAvailable = false;
        for (int i = index; i < r.length; i++) {
            if (i == r.length - 1 && r[i] instanceof VariableTerm && ((VariableTerm) r[i]).getType() == ws.prova.RList.class) {
                restAvailable = true;
                continue;
            }
            ComplexTerm nv = (ComplexTerm) r[i];
            names.add(((ConstantTerm) nv.getTerms()[1]).getObject());
        }
        while (results.hasNext()) {
            QuerySolution solution = (QuerySolution) results.next();
            Object[] retobj = restAvailable ? new Object[index + numVars] : new Object[index + names.size()];
            int n = 0;
            for (; n < index; n++) retobj[n] = r[n];
            for (Iterator it = names.iterator(); it.hasNext(); n++) {
                Term[] newTerms = new Term[2];
                newTerms[1] = new ConstantTermImpl(names.get(n - index));
                Term[] newTerms2 = new Term[2];
                String name = (String) it.next();
                newTerms2[1] = new ConstantTermImpl(solution.get(name));
                newTerms2[0] = RListUtils.emptyRListTerm;
                ComplexTerm nt2 = DefaultLogicFactorySupport.cplx(reagent.flist, newTerms2);
                newTerms[0] = nt2;
                ComplexTerm nt = DefaultLogicFactorySupport.cplx(reagent.flist, newTerms);
                retobj[n] = nt;
            }
            if (restAvailable) {
                for (Iterator varIter = results.getResultVars().iterator(); varIter.hasNext(); n++) {
                    String name = (String) varIter.next();
                    if (names.contains(name)) continue;
                    Term[] newTerms = new Term[2];
                    newTerms[1] = new ConstantTermImpl(name);
                    Term[] newTerms2 = new Term[2];
                    newTerms2[1] = new ConstantTermImpl(solution.get(name));
                    newTerms2[0] = RListUtils.emptyRListTerm;
                    ComplexTerm nt2 = DefaultLogicFactorySupport.cplx(reagent.flist, newTerms2);
                    newTerms[0] = nt2;
                    ComplexTerm nt = DefaultLogicFactorySupport.cplx(reagent.flist, newTerms);
                    retobj[n] = nt;
                }
            }
            bindings.add(retobj);
        }
        return Boolean.TRUE;
    }

    /**
	 * Satisfied if the argument is a variable.
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean free(final Object[] r, final List bindings) {
        if (r[0] instanceof VariableTerm) {
            Object[] retobj = new Object[1];
            retobj[0] = r[0];
            bindings.add(retobj);
        }
        return Boolean.TRUE;
    }

    /**
	 * Satisfied if it the argument is a constant or a complex term without any variables.
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean
	 */
    public Boolean bound(final Object[] r, final List bindings) {
        if (r[0] instanceof VariableTerm) {
            return Boolean.FALSE;
        }
        if (r[0] instanceof ComplexTerm) {
            ComplexTerm ct = (ComplexTerm) r[0];
            if (ct.containsVariables()) {
                return Boolean.FALSE;
            }
        }
        Object[] retobj = new Object[1];
        retobj[0] = r[0];
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in type predicate that returns in r[1] the type of the object in r[0].
	 * Since Prova 1.90, it also returns the type of variable terms
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean
	 */
    public Boolean type(final Object[] r, final List bindings) {
        Object[] retobj = new Object[2];
        retobj[0] = r[0];
        if (r[0] instanceof VariableTerm) {
            VariableTerm vt = (VariableTerm) r[0];
            retobj[1] = vt.getType();
        } else if (r[0] instanceof ComplexTerm) {
            ComplexTerm ct = (ComplexTerm) r[0];
            if (ct.containsVariables()) {
                return Boolean.FALSE;
            }
            retobj[1] = "ComplexTerm";
        } else {
            retobj[1] = r[0].getClass().getName();
        }
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in type predicate that holds for any r[0] that is a subtype of r[1].
	 * Since Prova 1.90, it also returns the type of variable terms
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean
	 */
    public Boolean subtype(final Object[] r, final List bindings) {
        Object[] retobj = new Object[2];
        Class[] type = new Class[2];
        for (int i = 0; i < 2; i++) {
            if (r[i] instanceof VariableTerm) {
                VariableTerm vt = (VariableTerm) r[0];
                type[i] = vt.getType();
            } else if (r[i] instanceof ComplexTerm) {
                type[i] = r[i].getClass();
            } else if (r[i] instanceof ConstantTerm) {
                ConstantTerm kt = (ConstantTerm) r[i];
                type[i] = kt.getType();
            } else if (r[i] instanceof Class) {
                type[i] = (Class) r[i];
            } else {
                type[i] = r[i].getClass();
            }
        }
        if (!type[1].isAssignableFrom(type[0])) return Boolean.FALSE;
        retobj[0] = r[0];
        retobj[1] = r[1];
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in predicate tokenize for tokenizing a String in r[0] given a separator in r[1].
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean
	 */
    public Boolean tokenize(final Object[] r, final List bindings) {
        Object arg0 = r[0];
        Object arg1 = r[1];
        Object arg2 = r[2];
        Object[] retobj = new Object[3];
        if (arg0 instanceof String) {
            String s0 = (String) arg0;
            String[] res = s0.split(" ");
            if (arg1 instanceof String) {
                if (!((String) arg1).equals(res[0])) {
                    return Boolean.FALSE;
                }
            }
            if (arg2 instanceof String) {
                if (!((String) arg2).equals(res[1])) {
                    return Boolean.FALSE;
                }
            }
            retobj[0] = arg0;
            retobj[1] = res[0];
            retobj[2] = res[1];
            bindings.add(retobj);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
	 * A built-in fopen predicate.
	 * Open a file and return a <code>BufferedReader</code> given its name in <code>r[0]</code>.
	 * Since version 2.0, it allows reading from a resource on the classpath.
	 *
	 * @param r		  <br> Parameters:
	 *                 <br> [in] the file name
	 *                 <br> [out] a <code>BufferedReader</code> corresponding to an open file
	 * @param bindings a list reproducing the instantiations for the predicate
	 *                 arguments.
	 *                 <br> bindings is empty if any problem occurs.
	 *                 <br> <code>r[1]</code> is set to the <code>BufferedReader</code> if opening the file succeeds.
	 * @return Boolean.TRUE
	 * @throws IOException
	 */
    public Boolean fopen(final Object[] r, final List bindings) throws IOException {
        String filename = (String) r[0];
        BufferedReader in = null;
        File file = new File(filename);
        if (!file.exists() || !file.canRead()) {
            try {
                in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
            } catch (Exception e) {
                throw new IOException();
            }
        } else {
            FileReader fr = new FileReader(file);
            in = new BufferedReader(fr);
        }
        Object[] retobj = new Object[2];
        retobj[0] = filename;
        retobj[1] = in;
        if (!returnObjectMatched(r[1], in)) return Boolean.FALSE;
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Consult from a <code>BufferedReader</code> in <code>r[0]</code>.
	 *
	 * @param r
	 * @param bindings
	 * @return
	 * @throws Exception
	 */
    public Boolean consult(final Object[] r, final List bindings) throws Exception {
        String src;
        BufferedReader in;
        String XID = "";
        Object[] retobj = new Object[r.length];
        Object[] values = null;
        if (r.length == 2) {
            if (r[0] instanceof String) {
                retobj[0] = XID = (String) r[0];
            } else {
                retobj[0] = XID = prova.newXID();
            }
            src = XID;
            retobj[1] = r[1];
            StringReader sr = new StringReader(r[1].toString());
            in = new BufferedReader(sr);
        } else {
            retobj[0] = r[0];
            if (r[0] instanceof ComplexTerm) {
                String input = "";
                src = "";
                ComplexTerm cterm = (ComplexTerm) r[0];
                Term[] content = cterm.getTerms();
                if (content.length == 2) {
                    src = ((ConstantTerm) content[1]).getObject().toString();
                    Term[] adds = ((ComplexTerm) content[0]).getTerms();
                    String s = adds[1].toString();
                    input = s.substring(1, s.length() - 1);
                    Term[] binds = ((ComplexTerm) adds[0]).getTerms();
                    if (binds[1] instanceof ComplexTerm) {
                        List boundObjects = new ArrayList();
                        RListUtils.rlist2List((ComplexTerm) binds[1], boundObjects, 0);
                        for (int i = 0; i < boundObjects.size(); i++) {
                            Object bo = boundObjects.get(i);
                            if (bo instanceof ComplexTerm) input = input.replaceAll("_" + i, bo.toString());
                        }
                        values = boundObjects.toArray();
                    }
                } else throw new ParsingException("consult parameters are invalid");
                StringReader sr = new StringReader(input);
                in = new BufferedReader(sr);
            } else if (r[0] instanceof String) {
                src = (String) r[0];
                File file = new File(src);
                if (!file.exists() || !file.canRead()) {
                    try {
                        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(src);
                        in = new BufferedReader(new InputStreamReader(is));
                    } catch (Exception e) {
                        try {
                            URL url = new URL(src);
                            in = new BufferedReader(new InputStreamReader(url.openStream()));
                        } catch (Exception ex) {
                            throw new IOException();
                        }
                    }
                } else {
                    FileReader fr = new FileReader(file);
                    in = new BufferedReader(fr);
                }
            } else if (r[0] instanceof BufferedReader) {
                src = "BufferedReader";
                in = (BufferedReader) r[0];
            } else if (r[0] instanceof StringBuffer) {
                src = "StringBuffer";
                StringReader sr = new StringReader(((StringBuffer) r[0]).toString());
                in = new BufferedReader(sr);
            } else {
                throw new ParsingException("consult parameters are invalid");
            }
        }
        RulebaseParser parser = new RulebaseParser(this, src, values);
        AtomicConsult temp_kb = new AtomicConsult();
        parser.parse(temp_kb, in);
        bindings.add(retobj);
        prova.commit_consult(XID, src, temp_kb);
        return Boolean.TRUE;
    }

    /**
	 * Read the the whole file and put all lines into a list for non-deterministic iteration
	 *
	 * @param r
	 * @param bindings
	 * @return
	 * @throws IOException
	 */
    public Boolean read_enum(final Object[] r, final List bindings) throws IOException {
        BufferedReader in = (BufferedReader) r[0];
        String line = "";
        while ((line = in.readLine()) != null) {
            Object[] retobj = new Object[2];
            retobj[0] = in;
            retobj[1] = line;
            if (!returnObjectMatched(r[1], line)) return Boolean.FALSE;
            bindings.add(retobj);
        }
        return Boolean.TRUE;
    }

    /**
	 * @deprecated
	 */
    public Boolean readline(final Object[] r, final List bindings) {
        BufferedReader in = (BufferedReader) r[0];
        Object[] retobj = new Object[2];
        try {
            String text = in.readLine();
            retobj[0] = in;
            retobj[1] = text;
            bindings.add(retobj);
        } catch (IOException e) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
	 * Tokenize a string returning tokens as a list
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean tokenize_list(final Object[] r, final List bindings) {
        String in = (String) r[0];
        String sep = (String) r[1];
        Object[] retobj = new Object[3];
        String[] res = in.split(sep);
        retobj[0] = in;
        retobj[1] = sep;
        retobj[2] = res;
        if (!returnObjectMatched(r[2], res)) return Boolean.FALSE;
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Tokenize a string returning non-deterministically enumerable tokens
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean tokenize_enum(final Object[] r, final List bindings) {
        String in = (String) r[0];
        String sep = (String) r[1];
        String[] res = in.split(sep);
        for (int i = 0; i < res.length; i++) {
            Object[] retobj = new Object[3];
            retobj[0] = in;
            retobj[1] = sep;
            retobj[2] = res[i];
            if (!returnObjectMatched(r[2], retobj[2])) return Boolean.FALSE;
            bindings.add(retobj);
        }
        return Boolean.TRUE;
    }

    /**
	 * Capture string tokens returning them non-deterministically
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean capture_enum(final Object[] r, final List bindings) {
        String in = (String) r[0];
        String regexp = (String) r[1];
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(in);
        while (m.find()) {
            Object[] retobj = new Object[3];
            retobj[0] = in;
            retobj[1] = regexp;
            List out = new ArrayList();
            for (int i = 1; i <= m.groupCount(); i++) {
                out.add(m.group(i));
            }
            retobj[2] = out.toArray();
            if (!returnObjectMatched(r[2], retobj[2])) return Boolean.FALSE;
            bindings.add(retobj);
        }
        return Boolean.TRUE;
    }

    /**
	 * Capture string tokens returning them as a Prova list
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean capture_list(final Object[] r, final List bindings) {
        String in = (String) r[0];
        String regexp = (String) r[1];
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(in);
        Object[] retobj = new Object[3];
        retobj[0] = in;
        retobj[1] = regexp;
        List list = new ArrayList();
        while (m.find()) {
            List out = new ArrayList();
            for (int i = 1; i <= m.groupCount(); i++) {
                out.add(m.group(i));
            }
            list.add(RListUtils.list2RList(out));
        }
        retobj[2] = list.toArray();
        if (!returnObjectMatched(r[2], retobj[2])) return Boolean.FALSE;
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Parse string to list
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean parse_list(final Object[] r, final List bindings) {
        String in = (String) r[0];
        String pattern = (String) r[1];
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(in);
        if (m.find()) {
            Object[] retobj = new Object[3];
            retobj[0] = in;
            retobj[1] = pattern;
            Object[] tokens = new Object[m.groupCount()];
            for (int i = 1; i <= tokens.length; i++) {
                tokens[i - 1] = m.group(i);
                if (tokens[i - 1] == null) {
                    tokens[i - 1] = "";
                }
            }
            retobj[2] = tokens;
            if (!returnObjectMatched(r[2], retobj[2])) return Boolean.FALSE;
            bindings.add(retobj);
        }
        return Boolean.TRUE;
    }

    /**
	 * Parse string to name-value pairs
	 *
	 * @param r
	 * @param bindings Name and Value lists
	 * @return Boolean.TRUE
	 */
    public Boolean parse_nv(final Object[] r, final List bindings) {
        String in = (String) r[0];
        String pattern = (String) r[1];
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(in);
        Object[] retobj = new Object[4];
        retobj[0] = in;
        retobj[1] = pattern;
        List list_n = new ArrayList();
        List list_v = new ArrayList();
        while (m.find()) {
            list_n.add(m.group(1));
            list_v.add(m.group(2));
        }
        retobj[2] = list_n.toArray();
        retobj[3] = list_v.toArray();
        if (!returnObjectMatched(r[2], retobj[2])) return Boolean.FALSE;
        if (!returnObjectMatched(r[3], retobj[3])) return Boolean.FALSE;
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    private StringBuffer print(Object r, String sep) {
        Collection list = null;
        if (r instanceof Collection) {
            list = (Collection) r;
        } else if (r instanceof ComplexTerm) {
            list = new ArrayList();
            RListUtils.rlist2List((ComplexTerm) r, list, 0);
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator li = list.iterator(); li.hasNext(); ) {
            Object o = li.next();
            if (o instanceof Term) {
                Term t = (Term) o;
                if (t.isVariable()) {
                    sb.append(t);
                    if (li.hasNext()) sb.append(sep);
                    continue;
                }
                if (t.isCompound()) {
                    sb.append(RListUtils.term_as_string(t));
                    if (li.hasNext()) sb.append(sep);
                    continue;
                }
            }
            sb.append(o);
            if (li.hasNext()) sb.append(sep);
        }
        return sb;
    }

    /**
	 * Introduce <code>r[0]</code> between the array of strings <code>r[1]</code>.
	 * <br>
	 * If the result r[2] is already constant,
	 * fail if it does not matche the result computed here.
	 *
	 * @param r
	 * @param bindings
	 * @return
	 */
    public Boolean implode(final Object[] r, final List bindings) {
        StringBuffer sb = print(r[1], (String) r[0]);
        Object[] retobj = new Object[3];
        retobj[0] = r[0];
        retobj[1] = r[1];
        retobj[2] = sb.toString();
        if (!returnObjectMatched(r[2], retobj[2])) return Boolean.FALSE;
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Translate escaped Java-standard string to a string with special characters
	 * embedded (for example, "\n" -> '\n').
	 * Unrecognised escapes are left as they are.
	 * Escape character is '\'.
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean unescape(final Object[] r, final List bindings) {
        String in = (String) r[0];
        Object[] retobj = new Object[2];
        retobj[0] = r[0];
        retobj[1] = unescape(in);
        if (!returnObjectMatched(r[1], retobj[1])) return Boolean.FALSE;
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Translate escaped Java-standard string to a string with special characters
	 * embedded (for example, "\n" -> '\n').
	 * Unrecognised escapes are left as they are.
	 * Escape character is '\'.
	 *
	 * @param in escaped string
	 * @return unescaped string
	 */
    public String unescape(String in) {
        final String metachars = "\"btrn\'\\";
        final String chars = "\"\b\t\r\n\'\\";
        final char escape = '\\';
        StringBuffer out = new StringBuffer();
        int p = 0;
        int i;
        int len = in.length();
        while ((i = in.indexOf(escape, p)) != -1) {
            out.append(in.substring(p, i));
            if (i + 1 == len) {
                break;
            }
            char meta = in.charAt(i + 1);
            int k = metachars.indexOf(meta);
            if (k == -1) {
                out.append(escape);
                out.append(meta);
            } else {
                out.append(chars.charAt(k));
            }
            p = i + 2;
        }
        if (p < len) {
            out.append(in.substring(p));
        }
        return out.toString();
    }

    /**
	 * Execute a command in the shell given a list of strings to be concatenated.
	 * Includes waiting for standard output and error
	 * as well as for the end of process.
	 *
	 * @param r
	 * @param bindings
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
    public Boolean exec(final Object[] r, final List bindings) throws IOException, InterruptedException {
        String cmd = "";
        Object[] retobj = new Object[1];
        retobj[0] = r[0];
        List list = new ArrayList();
        RListUtils.rlist2List((ComplexTerm) r[0], list, 0);
        for (ListIterator li = list.listIterator(); li.hasNext(); ) {
            Object o = li.next();
            if (o instanceof Term) {
                Term t = (Term) o;
                if (t.isVariable() || t.isCompound()) {
                    return Boolean.FALSE;
                }
            }
            cmd += o;
        }
        Process proc = java.lang.Runtime.getRuntime().exec(cmd);
        InputStream stdin = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdin);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) ;
        InputStream stderr = proc.getErrorStream();
        isr = new InputStreamReader(stderr);
        br = new BufferedReader(isr);
        line = null;
        while ((line = br.readLine()) != null) ;
        int exitVal = proc.waitFor();
        if (exitVal == 0) bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Match the <code>expected</code> (supplied) result with the result computed by a built-in.
	 * Usually such built-in (e.g., concat) will accept some input and produce a Java object as a result
	 * inanother argument.
	 * <br>
	 * If the <code>expected</code> is a <code>Term</code>, no checks are made.
	 * This situation is caught in CalcClause and full unification is used to make sure the result matches
	 * the supplied data.
	 *
	 * @param expected
	 * @param toBeReturned
	 * @return
	 */
    private boolean returnObjectMatched(Object expected, Object toBeReturned) {
        return expected instanceof Term || expected.equals(toBeReturned);
    }

    /**
	 * Concatenate a list of strings in <code>r[0]</code>
	 * If the result r[2] is already constant,
	 * fail if it does not matche the result computed here.
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean
	 */
    public Boolean concat(final Object[] r, final List bindings) {
        String cmd = "";
        Object[] retobj = new Object[2];
        retobj[0] = r[0];
        List list = new ArrayList();
        RListUtils.rlist2List((ComplexTerm) r[0], list, 0);
        for (ListIterator li = list.listIterator(); li.hasNext(); ) {
            Object o = li.next();
            if (o instanceof Term) {
                Term t = (Term) o;
                if (t.isVariable() || t.isCompound()) {
                    return Boolean.FALSE;
                }
            }
            cmd += o;
        }
        if (!returnObjectMatched(r[1], cmd)) return Boolean.FALSE;
        retobj[1] = cmd;
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    public Boolean fail(Object[] r, List ret) {
        return Boolean.TRUE;
    }

    private static final int sChunk = 8192;

    /**
	 * Copy from a <code>Reader</code> to a <code>Writer</code>
	 *
	 * @param r Parameters:
	 *          <br> [in] a <code>Reader</code>
	 *          <br> [in] a <code>Writer</code>
	 * @return Always <code>true</code>
	 * @throws IOException
	 */
    public boolean copy(final Object[] r) throws IOException {
        Reader is = (Reader) r[0];
        Writer os = (Writer) r[1];
        char[] buffer = new char[sChunk];
        int length;
        while ((length = is.read(buffer, 0, sChunk)) != -1) os.write(buffer, 0, length);
        is.close();
        os.close();
        return true;
    }

    /**
	 * A built-in predicate copy_stream implementing the copying of Stream's.
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 * @throws IOException
	 */
    public Boolean copy_stream(final Object[] r, final List bindings) throws IOException {
        InputStream is = (InputStream) r[0];
        OutputStream os = (OutputStream) r[1];
        byte[] buffer = new byte[sChunk];
        int length;
        while ((length = is.read(buffer, 0, sChunk)) != -1) os.write(buffer, 0, length);
        is.close();
        os.close();
        Object[] retobj = new Object[2];
        retobj[0] = r[0];
        retobj[1] = r[1];
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Place one bound variable on each position in the list.
	 * All of the variables are assumed to be be assigned different values by subsequent predicates.
	 *
	 * @param r		  - a linear list
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean permute_bound(final Object[] r, final List bindings) {
        List in = new ArrayList();
        RListUtils.rlist2List((ComplexTerm) r[0], in, 0);
        Object bound = null;
        int i_bound = 0;
        int i = 0;
        for (ListIterator li = in.listIterator(); li.hasNext(); i++) {
            Object o = li.next();
            if (!(o instanceof VariableTerm)) {
                if (bound != null) {
                    bound = null;
                    break;
                }
                bound = o;
                i_bound = i;
            }
        }
        if (bound == null) {
            Object[] retobj = new Object[2];
            retobj[0] = r[0];
            retobj[1] = r[0];
            bindings.add(retobj);
            return Boolean.TRUE;
        }
        List out = new ArrayList();
        i = 0;
        for (ListIterator li = in.listIterator(); li.hasNext(); i++) {
            out.clear();
            out.addAll(in);
            Object o = li.next();
            if (o != bound) {
                out.set(i, bound);
                out.set(i_bound, o);
            }
            Object[] retobj = new Object[2];
            retobj[0] = in.toArray();
            retobj[1] = out.toArray();
            bindings.add(retobj);
        }
        return Boolean.TRUE;
    }

    public static InputStream urlstream(String address) throws IOException {
        InputStream IS = null;
        java.net.URL Url = new java.net.URL(address);
        IS = Url.openStream();
        return IS;
    }

    public static FileInputStream finstream(String s) throws FileNotFoundException {
        FileInputStream is = null;
        is = new FileInputStream(s);
        return is;
    }

    public static FileOutputStream foutstream(String s) throws IOException {
        FileOutputStream os = null;
        os = new FileOutputStream(s);
        return os;
    }

    public static GZIPInputStream gzipstream(InputStream is) throws IOException {
        GZIPInputStream gzs = null;
        gzs = new GZIPInputStream(is);
        return gzs;
    }

    public static BufferedReader urlopen(String address) throws IOException {
        InputStream IS = null;
        java.net.URL Url = new java.net.URL(address);
        IS = Url.openStream();
        InputStreamReader ISR = new java.io.InputStreamReader(IS);
        BufferedReader buf = new BufferedReader(ISR);
        return buf;
    }

    /**
	 * A built-in predicate sql_insert for inserting tuples into a relational database.
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean.TRUE
	 * @throws SQLException
	 */
    public Boolean sql_insert(final Object[] r, final List bindings) throws SQLException {
        String sTable = (String) r[1];
        String sFields = RListUtils.term_as_string(r[2]);
        sFields = sFields.substring(1, sFields.indexOf("]")).replaceAll("\"", "");
        String sValues = RListUtils.term_as_string(r[3]);
        sValues = sValues.substring(1, sValues.lastIndexOf("]"));
        Statement stmt = null;
        stmt = ((Connection) r[0]).createStatement();
        String query = "insert into " + sTable + '(' + sFields + ") values(" + sValues + ')';
        query = query.replaceAll("\"", "\'");
        stmt.executeUpdate(query);
        Object[] retobj = new Object[3];
        retobj[0] = r[0];
        retobj[1] = r[1];
        retobj[2] = r[2];
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in predicate sql_update for updating tuples in a relational database.
	 *
	 * @param r
	 * @param bindings
	 * @return
	 * @throws SQLException
	 */
    public Boolean sql_update_execute(final Object[] r, final List bindings) throws SQLException {
        String sTable = (String) r[1];
        String sCommand = ((StringBuffer) r[2]).toString();
        Statement stmt = null;
        stmt = ((Connection) r[0]).createStatement();
        String query = "update " + sTable + sCommand;
        stmt.executeUpdate(query);
        Object[] retobj = new Object[3];
        retobj[0] = r[0];
        retobj[1] = r[1];
        retobj[2] = r[2];
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in predicate raise for raising Throwable.
	 *
	 * @param r		  Object[]
	 *                 <br> Parameters [in] Throwable
	 * @param bindings
	 * @return
	 * @throws Throwable
	 */
    public Boolean raise(final Object[] r, final List bindings) throws Throwable {
        Throwable throwable = (Throwable) r[0];
        throw throwable;
    }

    /**
	 * Store the message conversation-id in the current thread
	 * (normally called at the start of a message processing)
	 *
	 * @param r
	 * @return Always <code>true</code>
	 */
    public boolean store_tid(final Object[] r) {
        String tid = (String) r[0];
        String thread = Thread.currentThread().getName();
        map_tid.put(thread, tid);
        return true;
    }

    /**
	 * Store the message conversation-id in the current thread
	 * (normally called at the start of a message processing)
	 *
	 * @param tid
	 * @return Always <code>true</code>
	 */
    public boolean store_tid(String tid) {
        String thread = Thread.currentThread().getName();
        map_tid.put(thread, tid);
        return true;
    }

    /**
	 * Get the (current) conversation-id corresponding to the current thread
	 *
	 * @return current conversation id
	 */
    public String get_tid() {
        String thread = Thread.currentThread().getName();
        return (String) map_tid.get(thread);
    }

    /**
	 * Generate a unique id
	 *
	 * @param r
	 * @param bindings
	 * @return
	 */
    public Boolean unique_id(final Object[] r, final List bindings) {
        Object[] retobj = new Object[1];
        retobj[0] = new Integer(++unique_id).toString();
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    public Boolean spawn(final Object[] r, final List bindings) {
        return prova.spawn(r, bindings);
    }

    /**
	 * @param r
	 * @param bindings
	 * @param rest
	 * @param goal
	 * @return Boolean
	 */
    public Boolean rcvMsg(Object[] r, List bindings, Term rest, Clause goal) throws ClauseSetException {
        return messenger.rcvMsg(r, bindings, rest, goal);
    }

    /**
	 * @param r
	 * @param bindings
	 * @param rest
	 * @param goal
	 * @return Boolean
	 */
    public Boolean rcvMult(Object[] r, List bindings, Term rest, Clause goal) {
        return messenger.rcvMult(r, bindings, rest, goal);
    }

    /**
	 * @param r
	 * @param bindings
	 * @param rest
	 * @param goal
	 * @return Boolean
	 */
    public Boolean rcvMsgP(Object[] r, List bindings, Term rest, Clause goal) throws ClauseSetException {
        return messenger.rcvMsgP(r, bindings, rest, goal);
    }

    /**
	 * Delegate sendMsg to the main reagent
	 *
	 * @param r
	 * @param bindings
	 * @return Boolean
	 */
    public Boolean sendMsg(final Object[] r, final List bindings) {
        return messenger.sendMsg(r, bindings);
    }

    public static Object[] variables(Object o) {
        List vars = new ArrayList();
        if (o instanceof String) {
            return vars.toArray();
        }
        if (!(o instanceof ComplexTerm)) {
            return vars.toArray();
        }
        ComplexTerm ct = (ComplexTerm) o;
        ArrayList sub = new ArrayList();
        ct.getAllSubtermsA(sub);
        for (int i = 0; i < sub.size(); i++) {
            Term t = (Term) sub.get(i);
            if (t.isVariable()) {
                vars.add(t);
            }
        }
        return vars.toArray();
    }

    /**
	 * A built-in predicate byte_stream.
	 * <br> It either creates a byte stream for reading from a string (option A)
	 * <br> or fills a string from an existing byte string (option B)
	 * <br> This works from Prova rules like this (see test038.prova):
	 * <pre>
	 *    byte_stream("toto","UTF-8",BAIS),
	 *    FO=java.io.FileOutputStream("toto.gz"),
	 *    ZFO=java.util.zip.GZIPOutputStream(FO),
	 *    copy_stream(BAIS,ZFO), ...
	 * </pre>
	 * or this
	 * <pre>
	 * 	  FI=java.io.FileInputStream("toto.gz"),
	 *    ZFI=java.util.zip.GZIPInputStream(FI),
	 *    BAOS=java.io.ByteArrayOutputStream(),
	 *    copy_stream(ZFI,BAOS),
	 *    byte_stream(Result,"UTF-8",BAOS), ...
	 * </pre>
	 *
	 * @param r		  Object[]
	 *                 <br> Parameters
	 *                 <br> Option A:
	 *                 <br>    [in] string; [in] encoding; [out] ByteArrayInputStream.
	 *                 <br> Option B:
	 *                 <br>    [out] string; [in] encoding; [in] ByteArrayOutputStream.
	 * @param bindings List reproduce the inputs with instantiations
	 *                 for output parameters
	 * @return always Boolean.TRUE
	 */
    public Boolean byte_stream(final Object[] r, final List bindings) throws UnsupportedEncodingException {
        String enc = (String) r[1];
        Object[] retobj = new Object[3];
        retobj[1] = r[1];
        if (r[0] instanceof String) {
            String s = (String) r[0];
            retobj[0] = r[0];
            byte[] input = s.getBytes(enc);
            retobj[2] = new ByteArrayInputStream(input);
        } else {
            ByteArrayOutputStream bs = (ByteArrayOutputStream) r[2];
            retobj[2] = r[2];
            retobj[0] = bs.toString(enc);
        }
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in add predicate for two Number's.
	 *
	 * @param r		  Object[]
	 * @param bindings List
	 * @return Boolean
	 */
    public Boolean add(final Object[] r, final List bindings) {
        Object[] retobj = new Object[3];
        if (r[1] instanceof Float || r[1] instanceof Double || r[2] instanceof Float || r[2] instanceof Double) {
            double sum = ((Number) r[1]).doubleValue() + ((Number) r[2]).doubleValue();
            retobj[0] = new Double(sum);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && sum != ((Number) r[0]).doubleValue()) return Boolean.TRUE;
        } else {
            int sum = ((Number) r[1]).intValue() + ((Number) r[2]).intValue();
            retobj[0] = new Integer(sum);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && sum != ((Number) r[0]).intValue()) return Boolean.TRUE;
        }
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in subtract predicate for two Number's
	 *
	 * @param r		  Object[]
	 * @param bindings List
	 * @return Boolean
	 */
    public Boolean subtract(final Object[] r, final List bindings) {
        Object[] retobj = new Object[3];
        if (r[1] instanceof Float || r[1] instanceof Double || r[2] instanceof Float || r[2] instanceof Double) {
            double diff = ((Number) r[1]).doubleValue() - ((Number) r[2]).doubleValue();
            retobj[0] = new Double(diff);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && diff != ((Number) r[0]).doubleValue()) return Boolean.TRUE;
        } else {
            int diff = ((Number) r[1]).intValue() - ((Number) r[2]).intValue();
            retobj[0] = new Integer(diff);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && diff != ((Number) r[0]).intValue()) return Boolean.TRUE;
        }
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in multiply predicate for two Number's
	 *
	 * @param r		  Object[]
	 * @param bindings List
	 * @return Boolean
	 */
    public Boolean multiply(final Object[] r, final List bindings) {
        Object[] retobj = new Object[3];
        if (r[1] instanceof Float || r[1] instanceof Double || r[2] instanceof Float || r[2] instanceof Double) {
            double product = ((Number) r[1]).doubleValue() * ((Number) r[2]).doubleValue();
            retobj[0] = new Double(product);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && product != ((Number) r[0]).doubleValue()) return Boolean.TRUE;
        } else {
            int product = ((Number) r[1]).intValue() * ((Number) r[2]).intValue();
            retobj[0] = new Integer(product);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && product != ((Number) r[0]).intValue()) return Boolean.TRUE;
        }
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in divide predicate for two Number's
	 *
	 * @param r		  Object[]
	 * @param bindings List
	 * @return Boolean
	 */
    public Boolean divide(final Object[] r, final List bindings) {
        Object[] retobj = new Object[3];
        if (r[1] instanceof Float || r[1] instanceof Double || r[2] instanceof Float || r[2] instanceof Double) {
            double ratio = ((Number) r[1]).doubleValue() / ((Number) r[2]).doubleValue();
            retobj[0] = new Double(ratio);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && ratio != ((Number) r[0]).doubleValue()) return Boolean.TRUE;
        } else {
            int ratio = ((Number) r[1]).intValue() / ((Number) r[2]).intValue();
            retobj[0] = new Integer(ratio);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && ratio != ((Number) r[0]).intValue()) return Boolean.TRUE;
        }
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in remainder predicate for two Number's
	 *
	 * @param r		  Object[]
	 * @param bindings List
	 * @return Boolean
	 */
    public Boolean remainder(final Object[] r, final List bindings) {
        Object[] retobj = new Object[3];
        if (r[1] instanceof Float || r[1] instanceof Double || r[2] instanceof Float || r[2] instanceof Double) {
            double mod = ((Number) r[1]).doubleValue() % ((Number) r[2]).doubleValue();
            retobj[0] = new Double(mod);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && mod != ((Number) r[0]).doubleValue()) return Boolean.TRUE;
        } else {
            int mod = ((Number) r[1]).intValue() % ((Number) r[2]).intValue();
            retobj[0] = new Integer(mod);
            retobj[1] = r[1];
            retobj[2] = r[2];
            if (!(r[0] instanceof VariableTerm) && mod != ((Number) r[0]).intValue()) return Boolean.TRUE;
        }
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Non Prolog-like in-place replacement of a Prova list with a sorted list given
	 * the second argument as a Java Comparator. All complex and variable terms are put at the
	 * end of the sorted list in no particular order.
	 *
	 * @param r
	 * @return
	 */
    public boolean sort_list(Object[] r) {
        if (!(r[0] instanceof ComplexTerm)) {
            return false;
        }
        Term t = (Term) r[0];
        Comparator comparator = (Comparator) r[1];
        List list = new ArrayList();
        List listOther = new ArrayList();
        while (t instanceof ComplexTerm) {
            Object o = ((ComplexTerm) t).getTerms()[1];
            if (o instanceof VariableTerm || o == RListUtils.emptyRListTerm) listOther.add(o); else list.add(o);
            t = ((ComplexTerm) t).getTerms()[0];
        }
        ;
        Collections.sort(list, comparator);
        list.addAll(listOther);
        t = (Term) r[0];
        while (t instanceof ComplexTerm) {
            ((ComplexTerm) t).setTerm((Term) list.remove(0), 1);
            t = ((ComplexTerm) t).getTerms()[0];
        }
        ;
        return true;
    }

    /**
	 * A built-in predicate for assigning SwingAdaptor as an ActionListener
	 * for events from the Swing element specified in Arg1.
	 *
	 * @param r		  Object[]
	 * @param bindings List
	 * @return Boolean
	 */
    public Boolean listen(final Object[] r, final List bindings) {
        Object[] retobj = new Object[2];
        if (((String) r[0]).equals("action")) ((javax.swing.AbstractButton) r[1]).addActionListener(adaptor); else if (((String) r[0]).equals("change")) ((javax.swing.AbstractButton) r[1]).addChangeListener(adaptor); else if (((String) r[0]).equals("mouse")) ((java.awt.Component) r[1]).addMouseListener(adaptor);
        retobj[0] = r[0];
        retobj[1] = r[1];
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A built-in predicate for removing SwingAdaptor as an ActionListener
	 * for events from the Swing element specified in Arg1.
	 *
	 * @param r		  Object[]
	 * @param bindings List
	 * @return Boolean
	 */
    public Boolean unlisten(final Object[] r, final List bindings) {
        Object[] retobj = new Object[2];
        if (((String) r[0]).equals("action")) ((javax.swing.AbstractButton) r[1]).removeActionListener(adaptor); else if (((String) r[0]).equals("change")) ((javax.swing.AbstractButton) r[1]).removeChangeListener(adaptor);
        retobj[0] = r[0];
        retobj[1] = r[1];
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * A catch equivalent for Prova.
	 *
	 * @param r		  Parameters:
	 *                 <br> [in] Exception class;
	 *                 <br> [in] Atom to call as a Prova list;
	 *                 <br> [in] Exception handler to register the predicate handler in.
	 * @param bindings
	 * @return Boolean.TRUE
	 */
    public Boolean on_exception(final Object[] r, final List bindings, final ExceptionHandler exhandler) {
        Class exclass = (Class) r[0];
        if (r[1] instanceof String) {
            if (((String) r[1]).equals("clear")) {
                exhandler.unregister(exclass);
            }
        } else {
            ComplexTerm ct = (ComplexTerm) r[1];
            exhandler.register(exclass, ct);
        }
        Object[] retobj = new Object[r.length];
        retobj[0] = r[0];
        retobj[1] = r[1];
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Return the exception that has last occurred.
	 *
	 * @param r			Parameters:
	 *                  <br> [out] Exception (returned via <code>bindings</code>);
	 * @param bindings
	 * @param exhandler the current exception handler allowing the exception to be retrieved
	 * @return Boolean.TRUE
	 */
    public Boolean exception(final Object[] r, final List bindings, final ExceptionHandler exhandler) {
        Object[] retobj = new Object[1];
        retobj[0] = exhandler.getException();
        bindings.add(retobj);
        return Boolean.TRUE;
    }

    /**
	 * Generic optimised redirection of built-in constant deterministic predicates
	 *
	 * @param calc	The instance of <code>Calc</code> to be operated on
	 * @param method The code of the method (see <code>bcalcCode</code> in <code>ws.prova.RulebaseParser</code>
	 * @param ct
	 */
    public static boolean callBPred(final Calc calc, final int method, ComplexTerm ct) throws Exception {
        final List list = new ArrayList();
        RListUtils.rlist2List(ct, list, 0);
        final Object[] r = list.toArray();
        switch(method) {
            case 0:
                return calc.print(r, "\n");
            case 1:
                return calc.updater.retract(r);
            case 2:
                calc.updater.retractall(r[0]);
                return true;
            case 3:
                return calc.print(r, "");
            case 4:
                return calc.copy(r);
            case 5:
                return calc.store_tid(r);
            case 6:
                return calc.updater.prova_assert(r, calc.prova);
            case 7:
                return calc.updater.prova_asserta(r, calc.prova);
            case 8:
                return calc.updater.prova_insert(r);
            case 9:
                return calc.updater.prova_update_fact(r, calc.prova);
            case 10:
                return calc.sort_list(r);
        }
        return false;
    }

    private PrintWriter output = new PrintWriter(System.out);

    /**
	 * Optimised version of print/println
	 *
	 * @param r	Parameters:
	 *            <br> [in] <code>ComplexTerm</code> to be printed
	 *            <br> ?[in] Separator
	 * @param end <code>String</code> terminating the output (normally "\n" or "")
	 * @return Always <code>true</code>
	 */
    public boolean print(final Object[] r, final String end) {
        String sep = (r.length == 2) ? (String) r[1] : "";
        StringBuffer sb = print(r[0], sep);
        if (!end.equals("\n")) {
            sb.append(end);
            print(sb.toString());
        } else {
            println(sb.toString());
        }
        return true;
    }

    /**
	 * Used for printing error messages from <code>ws.prova.parser.RulebaseParser</code>.
	 *
	 * @param s
	 */
    public void println(String s) {
        output.println(s);
        output.flush();
    }

    /**
	 * Used for printing error messages from <code>ws.prova.parser.RulebaseParser</code>.
	 *
	 * @param s
	 */
    public void print(String s) {
        output.print(s);
    }

    public void setPrintWriter(PrintWriter output) {
        if (output == null) {
            throw new IllegalArgumentException("No specified PrintWriter");
        }
        this.output = output;
    }

    public PrintWriter getPrintWriter() {
        return output;
    }
}
