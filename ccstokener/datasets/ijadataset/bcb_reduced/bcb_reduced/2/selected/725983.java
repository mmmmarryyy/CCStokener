package org.matsim.gbl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Random;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.config.ConfigReaderMatsimV1;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.world.World;
import org.xml.sax.SAXException;

public abstract class Gbl {

    private static final long DEFAULT_RANDOM_SEED = 4711;

    private static Config config = null;

    private static World world = null;

    public static final Random random = new Random(DEFAULT_RANDOM_SEED);

    private static final Logger log = Logger.getLogger(Gbl.class);

    public static final Config createConfig(final String[] args) {
        if (Gbl.config != null) {
            Gbl.errorMsg("config exists already! Cannot create a 2nd global config.");
        }
        Gbl.config = new Config();
        Gbl.config.addCoreModules();
        if ((args != null) && (args.length == 1)) {
            log.info("Input config file: " + args[0]);
            ConfigReaderMatsimV1 reader = new ConfigReaderMatsimV1(Gbl.config);
            try {
                reader.parse(args[0]);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if ((args != null) && (args.length >= 1)) {
            log.info("Input config file: " + args[0]);
            log.info("Input local config dtd: " + args[1]);
            ConfigReaderMatsimV1 reader = new ConfigReaderMatsimV1(Gbl.config);
            reader.readFile(args[0], args[1]);
        }
        Gbl.random.setSeed(Gbl.config.global().getRandomSeed());
        return Gbl.config;
    }

    public static final Config getConfig() {
        return Gbl.config;
    }

    public static final void setConfig(final Config config) {
        Gbl.config = config;
    }

    public static final World createWorld() {
        if (Gbl.world != null) {
            Gbl.errorMsg("world exists already! Cannot create a 2nd global world.");
        }
        Gbl.world = new World();
        return Gbl.world;
    }

    public static final World getWorld() {
        if (Gbl.world == null) {
            Gbl.createWorld();
        }
        return Gbl.world;
    }

    public static final void reset() {
        log.info("Gbl.reset() -- reset config, world");
        Gbl.config = null;
        Gbl.world = null;
        Gbl.random.setSeed(DEFAULT_RANDOM_SEED);
        CharyparNagelScoringFunction.initialized = false;
    }

    public static final void printMemoryUsage() {
        long totalMem = Runtime.getRuntime().totalMemory();
        long freeMem = Runtime.getRuntime().freeMemory();
        long usedMem = totalMem - freeMem;
        log.info("used RAM: " + usedMem + "B = " + (usedMem / 1024) + "kB = " + (usedMem / 1024 / 1024) + "MB" + "  free: " + freeMem + "B = " + (freeMem / 1024 / 1024) + "MB  total: " + totalMem + "B = " + (totalMem / 1024 / 1024) + "MB");
    }

    public static final void printSystemInfo() {
        log.info("JVM: " + System.getProperty("java.version") + "; " + System.getProperty("java.vm.vendor") + "; " + System.getProperty("java.vm.info") + "; " + System.getProperty("sun.arch.data.model") + "-bit");
        log.info("OS: " + System.getProperty("os.name") + "; " + System.getProperty("os.version") + "; " + System.getProperty("os.arch"));
        log.info("CPU cores: " + Runtime.getRuntime().availableProcessors());
        log.info("max. Memory: " + Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 + "MB (" + Runtime.getRuntime().maxMemory() + "B)");
    }

    /** Prints some information about the current build/revision of this code.
	 * Currently, this will only work with the Nightly-Build-Jars.
	 */
    public static final void printBuildInfo() {
        String revision = null;
        String date = null;
        URL url = Gbl.class.getResource("/revision.txt");
        if (url != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                revision = reader.readLine();
                date = reader.readLine();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (revision == null) {
                    log.info("MATSim-Build: unknown");
                } else {
                    log.info("MATSim-Build: " + revision + " (" + date + ")");
                }
            }
        } else {
            log.info("MATSim-Build: unknown");
        }
    }

    public static final void errorMsg(final Exception e) {
        e.printStackTrace();
        System.exit(-1);
    }

    public static final void errorMsg(final String msg) {
        new Exception(msg).printStackTrace();
        System.exit(-1);
    }

    private static long measurementStartTime = Long.MAX_VALUE;

    private static final String printTime() {
        if (Gbl.measurementStartTime == Long.MAX_VALUE) {
            log.error("Did not start measurements.");
            return "";
        }
        return printTimeDiff(System.currentTimeMillis(), Gbl.measurementStartTime);
    }

    public static final String printTimeDiff(final long later, final long earlier) {
        long elapsedTimeMillis = later - earlier;
        float elapsedTimeSec = elapsedTimeMillis / 1000F;
        float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);
        float elapsedTimeHour = elapsedTimeMillis / (60 * 60 * 1000F);
        float elapsedTimeDay = elapsedTimeMillis / (24 * 60 * 60 * 1000F);
        return elapsedTimeMillis + " msecs; " + elapsedTimeSec + " secs; " + elapsedTimeMin + " mins; " + elapsedTimeHour + " hours; " + elapsedTimeDay + " days ###";
    }

    public static final void startMeasurement() {
        Gbl.measurementStartTime = System.currentTimeMillis();
    }

    public static final void printElapsedTime() {
        log.info("### elapsed time: " + Gbl.printTime());
    }

    public static final void printRoundTime() {
        log.info("### round time: " + Gbl.printTime());
        Gbl.startMeasurement();
    }
}
