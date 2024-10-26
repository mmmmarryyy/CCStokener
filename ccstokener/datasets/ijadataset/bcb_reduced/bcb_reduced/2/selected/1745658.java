package net.sf.fit4oaw.fixture.oaw5;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import jlibdiff.Diff;
import jlibdiff.Hunk;
import jlibdiff.HunkAdd;
import jlibdiff.HunkChange;
import jlibdiff.HunkDel;
import jlibdiff.HunkVisitor;
import net.sf.fit4oaw.fixture.oaw5.internal.FixtureContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.xpand2.XpandFacade;
import org.eclipse.xtend.expression.ExpressionFacade;
import fit.ColumnFixture;

/**
 * Calls an Xpand template and compares the result to a file.
 * 
 * <h2>Properties</h2>
 * <table border="1">
 * <tr><th>Name</th><th>Type</th><th>Required</th><th>Description</th></tr>
 * <tr>
 * <td>template</td>
 * <td>String</td>
 * <td>yes</td>
 * <td>Qualified path to an Xpand definition.</td>
 * </tr>
 * <tr>
 * <td>target</td>
 * <td>String</td>
 * <td>yes</td>
 * <td>Expression string that evaluates to the object for which the template should be called.</td>
 * </tr>
 * <tr>
 * <td>compareTo</td>
 * <td>String</td>
 * <td>yes</td>
 * <td>URL to a file to which holds the expected output of the template invocation.</td>
 * </tr>
 * <tr>
 * <td>arg1 - arg5</td>
 * <td>String</td>
 * <td>no</td>
 * <td>Arguments for the template. The strings passed as an argumented will be evaluated as expressions
 * and the results are passed to the template definition.</td>
 * </tr>
 * <tr>
 * <td>success?</td>
 * <td>Boolean</td>
 * <td>yes</td>
 * <td>Executes the template invocation. Successful execution is indicated by <code>true</code>.</td>
 * </tr>
 * </table>
 * 
 * @author Karsten Thoms
 */
public class CallXpandTemplate extends ColumnFixture {

    private static final Log LOG = LogFactory.getLog(CallXpandTemplate.class);

    public String template;

    public String target;

    public String compareTo;

    public String arg1;

    public String arg2;

    public String arg3;

    public String arg4;

    public String arg5;

    private String diff;

    private class HunkPrintVisitor extends HunkVisitor {

        private StringWriter sw = new StringWriter();

        @Override
        public void visitHunkAdd(HunkAdd hunk) {
            sw.append(hunk.convert());
        }

        @Override
        public void visitHunkChange(HunkChange hunk) {
            sw.append(hunk.convert());
        }

        @Override
        public void visitHunkDel(HunkDel hunk) {
            sw.append(hunk.convert());
        }

        public StringBuffer getBuffer() {
            return sw.getBuffer();
        }
    }

    public Boolean success() throws IOException {
        FixtureContext ctx = FixtureContext.getInstance();
        XpandFacade xpand = XpandFacade.create(ctx.getXpandContext());
        ExpressionFacade expr = new ExpressionFacade(ctx.getExecutionContext());
        Object targetObject = expr.evaluate(target);
        List<Object> args = new ArrayList<Object>();
        if (arg1 != null) {
            args.add(expr.evaluate(arg1));
        }
        if (arg2 != null) {
            args.add(expr.evaluate(arg2));
        }
        if (arg3 != null) {
            args.add(expr.evaluate(arg3));
        }
        if (arg4 != null) {
            args.add(expr.evaluate(arg4));
        }
        if (arg5 != null) {
            args.add(expr.evaluate(arg5));
        }
        ctx.getOutput().clear();
        xpand.evaluate(template, targetObject, args.toArray());
        String templateOutput = ctx.getOutput().getOutput();
        String expectedContent = fetchCompareContent();
        diff = compare(expectedContent, templateOutput);
        if ("".equals(diff)) {
            return true;
        } else {
            LOG.info("--------------------------------------------------------------------------------");
            LOG.info(" Template  : " + template);
            LOG.info(" Target    : " + target);
            LOG.info(" Compare to: " + compareTo);
            if (arg1 != null) {
                LOG.info(" arg1      : " + arg1);
            }
            if (arg2 != null) {
                LOG.info(" arg2      : " + arg2);
            }
            if (arg3 != null) {
                LOG.info(" arg3      : " + arg3);
            }
            if (arg4 != null) {
                LOG.info(" arg4      : " + arg4);
            }
            if (arg5 != null) {
                LOG.info(" arg5      : " + arg5);
            }
            LOG.info("--------------------------------------------------------------------------------");
            LOG.info(diff);
            LOG.info("--------------------------------------------------------------------------------");
            return false;
        }
    }

    private String fetchCompareContent() throws IOException {
        URL url = new URL(compareTo);
        StringWriter sw = new StringWriter();
        IOUtils.copy(url.openStream(), sw);
        return sw.getBuffer().toString();
    }

    @SuppressWarnings("unchecked")
    private String compare(String expected, String actual) throws IOException {
        Diff diff = new Diff();
        diff.diffString(expected, actual);
        Vector hunks = diff.getHunks();
        HunkPrintVisitor visitor = new HunkPrintVisitor();
        for (int i = 0; i < hunks.size(); i++) {
            Hunk hunk = (Hunk) hunks.get(i);
            hunk.accept(visitor);
        }
        return visitor.getBuffer().toString();
    }

    @Override
    public void reset() throws Exception {
        super.reset();
        template = null;
        target = null;
        compareTo = null;
        arg1 = null;
        arg2 = null;
        arg3 = null;
        arg4 = null;
        arg5 = null;
    }
}
