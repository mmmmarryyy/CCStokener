package org.sat4j;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import org.sat4j.core.ASolverFactory;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public abstract class AbstractLauncher implements Serializable {

    protected AbstractLauncher() {
        exitCode = ExitCode.UNKNOWN;
        out = new PrintWriter(System.out, true);
        shutdownHook = new Thread() {

            public void run() {
                displayResult();
            }
        };
        silent = false;
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    protected void displayResult() {
        if (solver != null) {
            System.out.flush();
            out.flush();
            double d = (double) (System.currentTimeMillis() - beginTime) / 1000D;
            solver.printStat(out, "c ");
            solver.printInfos(out, "c ");
            out.println("s " + exitCode);
            if (exitCode == ExitCode.SATISFIABLE) {
                int ai[] = solver.model();
                out.print("v ");
                reader.decode(ai, out);
                out.println();
            }
            log("Total wall clock time (in seconds) : " + d);
        }
    }

    public abstract void usage();

    /**
	 * @throws IOException
	 */
    protected final void displayHeader() {
        displayLicense();
        URL url = AbstractLauncher.class.getResource("/sat4j.version");
        if (url == null) {
            log("no version file found!!!");
        } else {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(url.openStream()));
                log("version " + in.readLine());
            } catch (IOException e) {
                log("c ERROR: " + e.getMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        log("c ERROR: " + e.getMessage());
                    }
                }
            }
        }
        Properties prop = System.getProperties();
        String[] infoskeys = { "java.runtime.name", "java.vm.name", "java.vm.version", "java.vm.vendor", "sun.arch.data.model", "java.version", "os.name", "os.version", "os.arch" };
        for (int i = 0; i < infoskeys.length; i++) {
            String key = infoskeys[i];
            log(key + ((key.length() < 14) ? "\t\t" : "\t") + prop.getProperty(key));
        }
        Runtime runtime = Runtime.getRuntime();
        log("Free memory \t\t" + runtime.freeMemory());
        log("Max memory \t\t" + runtime.maxMemory());
        log("Total memory \t\t" + runtime.totalMemory());
        log("Number of processors \t" + runtime.availableProcessors());
    }

    public void displayLicense() {
        log("SAT4J: a SATisfiability library for Java (c) 2004-2010 Daniel Le Berre");
        log("This is free software under the dual EPL/GNU LGPL licenses.");
        log("See www.sat4j.org for details.");
    }

    protected IProblem readProblem(String s) throws FileNotFoundException, ParseFormatException, IOException, ContradictionException {
        log("solving " + s);
        log("reading problem ... ");
        reader = createReader(solver, s);
        IProblem iproblem = reader.parseInstance(s);
        log("... done. Wall clock time " + (double) (System.currentTimeMillis() - beginTime) / 1000D + "s.");
        log("#vars     " + iproblem.nVars());
        log("#constraints  " + iproblem.nConstraints());
        iproblem.printInfos(out, "c ");
        return iproblem;
    }

    protected abstract Reader createReader(ISolver isolver, String s);

    public void run(String as[]) {
        displayHeader();
        solver = configureSolver(as);
        if (solver == null) {
            usage();
            return;
        }
        String s;
        if (!silent) solver.setVerbose(true);
        s = getInstanceName(as);
        if (s == null) {
            usage();
            return;
        }
        try {
            beginTime = System.currentTimeMillis();
            IProblem iproblem = readProblem(s);
            try {
                solve(iproblem);
            } catch (TimeoutException _ex) {
                log("timeout");
            }
        } catch (FileNotFoundException filenotfoundexception) {
            System.err.println("FATAL " + filenotfoundexception.getLocalizedMessage());
        } catch (IOException ioexception) {
            System.err.println("FATAL " + ioexception.getLocalizedMessage());
        } catch (ContradictionException _ex) {
            exitCode = ExitCode.UNSATISFIABLE;
            log("(trivial inconsistency)");
        } catch (ParseFormatException parseformatexception) {
            System.err.println("FATAL " + parseformatexception.getLocalizedMessage());
        }
        return;
    }

    protected abstract String getInstanceName(String as[]);

    protected abstract ISolver configureSolver(String as[]);

    public void log(String s) {
        if (!silent) out.println("c " + s);
    }

    protected void solve(IProblem iproblem) throws TimeoutException {
        exitCode = iproblem.isSatisfiable() ? ExitCode.SATISFIABLE : ExitCode.UNSATISFIABLE;
    }

    public final void setExitCode(ExitCode exitcode) {
        exitCode = exitcode;
    }

    public final ExitCode getExitCode() {
        return exitCode;
    }

    public final long getBeginTime() {
        return beginTime;
    }

    public final Reader getReader() {
        return reader;
    }

    public void setLogWriter(PrintWriter printwriter) {
        out = printwriter;
    }

    public PrintWriter getLogWriter() {
        return out;
    }

    protected void setSilent(boolean flag) {
        silent = flag;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        out = new PrintWriter(System.out, true);
        shutdownHook = new Thread() {

            @Override
            public void run() {
                displayResult();
            }
        };
    }

    protected <T extends ISolver> void showAvailableSolvers(ASolverFactory<T> asolverfactory) {
        if (asolverfactory != null) {
            log("Available solvers: ");
            String as[] = asolverfactory.solverNames();
            for (int i = 0; i < as.length; i++) log(as[i]);
        }
    }

    private static final long serialVersionUID = 1L;

    public static final String SOLUTION_PREFIX = "v ";

    public static final String ANSWER_PREFIX = "s ";

    public static final String COMMENT_PREFIX = "c ";

    protected long beginTime;

    protected ExitCode exitCode;

    protected Reader reader;

    protected transient PrintWriter out;

    protected transient Thread shutdownHook;

    protected ISolver solver;

    private boolean silent;
}
