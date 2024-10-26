package org.dmd.dmg.tools.dmggenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.Iterator;
import org.dmd.dmc.DmcValueException;
import org.dmd.dmc.DmcValueExceptionSet;
import org.dmd.dmg.generated.dmo.DmgConfigDMO;
import org.dmd.dmg.types.Generator;
import org.dmd.dms.SchemaManager;
import org.dmd.dms.util.DmoGenerator;
import org.dmd.dms.util.DmsSchemaParser;
import org.dmd.util.BooleanVar;
import org.dmd.util.FileUpdateManager;
import org.dmd.util.exceptions.ResultException;
import org.dmd.util.formatting.PrintfFormat;
import org.dmd.util.parsing.CommandLine;
import org.dmd.util.parsing.ConfigFinder;
import org.dmd.util.parsing.ConfigLocation;
import org.dmd.util.parsing.ConfigVersion;
import org.dmd.util.parsing.StringArrayList;

/**
 * The DmoGenerator is a commandline utility that lets you generate Dark Matter Objects (DMOs)
 * from Dark Matter Schema (DMS) definitions.
 */
public class DmgGenUtility {

    SchemaManager baseSchema;

    SchemaManager baseWithDMGSchema;

    SchemaManager readSchemas;

    ConfigFinder schemaFinder;

    ConfigFinder configFinder;

    DmsSchemaParser schemaParser;

    DmgParser parser;

    DmoGenerator codeGenerator;

    PrintfFormat format;

    String fileHeader;

    CommandLine cl;

    StringBuffer help;

    BooleanVar helpFlag = new BooleanVar();

    StringArrayList srcdir = new StringArrayList();

    StringBuffer workspace = new StringBuffer();

    BooleanVar autogen = new BooleanVar();

    StringBuffer cfg = new StringBuffer();

    BooleanVar debug = new BooleanVar();

    public DmgGenUtility(String[] args) throws ResultException, IOException, DmcValueException, DmcValueExceptionSet {
        initHelp();
        cl = new CommandLine();
        cl.addOption("-h", helpFlag, "Dumps the help message.");
        cl.addOption("-srcdir", srcdir, "The source directories to search.");
        cl.addOption("-workspace", workspace, "The workspace prefix");
        cl.addOption("-autogen", autogen, "Indicates that you want to generate from all configs automatically.");
        cl.addOption("-cfg", cfg, "The configuration file to load.");
        cl.addOption("-debug", debug, "Dump debug information.");
        cl.parseArgs(args);
        if (helpFlag.booleanValue()) {
            System.out.println(help.toString());
        }
        baseSchema = new SchemaManager();
        baseWithDMGSchema = new SchemaManager();
        readSchemas = null;
        StringArrayList searchdirs = new StringArrayList();
        if (srcdir.size() > 0) {
            searchdirs = new StringArrayList();
            for (String dir : srcdir) {
                searchdirs.add(workspace.toString() + "/" + dir);
            }
        } else {
            searchdirs = srcdir;
        }
        schemaFinder = new ConfigFinder(searchdirs.iterator());
        if (debug.booleanValue()) schemaFinder.debug(true);
        schemaFinder.addSuffix(".dms");
        schemaFinder.findConfigs();
        schemaParser = new DmsSchemaParser(baseSchema, schemaFinder);
        schemaParser.parseSchema(baseWithDMGSchema, "dmg", true);
        configFinder = new ConfigFinder(searchdirs.iterator());
        configFinder.addSuffix(".dmg");
        configFinder.findConfigs();
        if (debug.booleanValue()) configFinder.debug(true);
        parser = new DmgParser(baseWithDMGSchema, configFinder);
        codeGenerator = new DmoGenerator(System.out);
        String f = "%-" + configFinder.getLongestName() + "s";
        format = new PrintfFormat(f);
        fileHeader = "";
    }

    void initHelp() {
        String userHome = System.getProperty("user.home");
        help = new StringBuffer();
        help.append("dmwgen -h -cfg -workspace -srcdir -autogen\n\n");
        help.append("The dmwgen tool generates Dark Matter Wrappers based on a specified schema.\n");
        help.append("Schemas configurations (that end with a .dms extension) are recursivley discovered\n");
        help.append("in your development environment using information you provide in one of several ways.\n");
        help.append("\n");
        help.append("The default behaviour is to look for a .darkmatter folder in " + userHome + "\n");
        help.append("and to read the sourcedirs.txt file that resides there. The sourcedirs.txt file\n");
        help.append("specifies file paths to search, one path per line. The path must be fully qualified\n");
        help.append("i.e. C:/mydev/myproject/src\n");
        help.append("\n");
        help.append("The tool can also search .jar files that contain schemas defined by others. \n");
        help.append("Just specify a line with the jar file name (or the last part thereof). As long\n");
        help.append("the line ends with .jar, all jars that end with that suffix will be searched for\n");
        help.append("schema configurations.\n");
        help.append("\n");
        help.append("You can also specify code locations on the command line via the -srcdir option.\n");
        help.append("\n");
        help.append("If you specify the -workspace option, this prefix will be placed in front of all \n");
        help.append("arguments to the -srcdir option.\n");
        help.append("\n");
        help.append("Or you can specify a configuration file (formatted like sourcedirs.txt) to load.\n");
        help.append("via the -cfg option.\n");
        help.append("\n");
        help.append("-h dumps the help information.\n");
        help.append("\n");
        help.append("\n");
        help.append("\n");
        help.append("\n");
        help.append("example: dmwgen -workspace C:/eclipse/workspace -srcdir proj1/src proj2/src proj3/src\n");
        help.append("\n");
    }

    public void run() throws DmcValueExceptionSet {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String currLine = null;
        if (autogen.booleanValue()) {
            if (autogen.booleanValue()) {
                for (ConfigVersion version : configFinder.getVersions().values()) {
                    ConfigLocation loc = version.getLatestVersion();
                    if (!loc.isFromJAR()) {
                        generateFromConfig(version);
                    }
                }
                System.exit(0);
            }
            System.exit(0);
        }
        System.out.println("\ndmg generator - enter the name of the Dark Matter Generator config\n");
        System.out.println("Enter ? for a list of configs...\n\n");
        while (true) {
            try {
                String s = in.readLine();
                if (s == null) return;
                currLine = s.trim();
                if (currLine.length() == 0) continue;
                ConfigVersion currConfig = configFinder.getConfig(currLine);
                if (currLine.equals("?")) {
                    System.out.println("");
                    System.out.println(configFinder.getSearchInfo() + "\n");
                    Iterator<ConfigVersion> it = configFinder.getVersions().values().iterator();
                    while (it.hasNext()) {
                        ConfigVersion version = it.next();
                        ConfigLocation loc = version.getLatestVersion();
                        if (loc.getJarFilename() == null) {
                            System.out.println(format.sprintf(loc.getConfigName()) + "   version: " + loc.getVersion());
                            System.out.println(format.sprintf("") + " " + loc.getConfigParentDirectory() + "\n");
                        } else {
                            System.out.println(format.sprintf("JAR " + loc.getConfigName()) + " " + loc.getDirectory());
                            System.out.println(format.sprintf("") + " " + loc.getConfigParentDirectory() + "\n");
                        }
                    }
                    System.out.println("");
                } else if (currConfig == null) {
                    System.err.println("\n" + currLine + " is not a recoginized config name.\n\n");
                } else {
                    try {
                        parser.parseConfig(currConfig.getLatestVersion());
                        loadRequiredSchemas();
                        Iterator<Generator> generators = parser.getTheConfig().getGenerator();
                        readFileHeader(parser.getTheConfig(), currConfig.getLatestVersion());
                        if (generators != null) {
                            while (generators.hasNext()) {
                                Generator g = generators.next();
                                g.getGenerator().setProgressStream(System.out);
                                g.getGenerator().setFileHeader(fileHeader);
                                FileUpdateManager.instance().reportProgress(System.out);
                                FileUpdateManager.instance().reportErrors(System.err);
                                FileUpdateManager.instance().generationStarting();
                                g.getGenerator().generateCode(parser.getTheConfig(), currConfig.getLatestVersion(), configFinder, readSchemas);
                                FileUpdateManager.instance().generationComplete();
                            }
                        }
                    } catch (ResultException e) {
                        e.printStackTrace();
                    } catch (DmcValueException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException:\n" + e.getMessage());
                System.exit(1);
            }
        }
    }

    void loadRequiredSchemas() throws ResultException, DmcValueException, DmcValueExceptionSet {
        readSchemas = new SchemaManager();
        schemaParser.parseSchema(readSchemas, parser.getTheConfig().getSchemaToLoad(), true);
    }

    /**
	 * If the schema has a generatedFileHeader specified, we try to read the file.
	 * @param sd The schema definition.
	 * @param sl The schema location.
	 * @throws IOException
	 */
    void readFileHeader(DmgConfigDMO config, ConfigLocation sl) throws IOException {
        fileHeader = "";
        if (config.getGeneratedFileHeader() != null) {
            StringBuffer sb = new StringBuffer();
            if (sl.getJarFilename() != null) {
                URL url = new URL("jar:file:" + sl.getJarFilename() + "!/" + sl.getJarDirectory() + "/" + config.getGeneratedFileHeader());
                LineNumberReader in = new LineNumberReader(new InputStreamReader(url.openStream()));
                String str;
                while ((str = in.readLine()) != null) {
                    sb.append(str + "\n");
                }
                in.close();
            } else {
                LineNumberReader in = new LineNumberReader(new FileReader(sl.getDirectory() + File.separator + config.getGeneratedFileHeader()));
                String str;
                while ((str = in.readLine()) != null) {
                    sb.append(str + "\n");
                }
                in.close();
            }
            fileHeader = sb.toString();
        }
    }

    void generateFromConfig(ConfigVersion currConfig) {
        try {
            parser.parseConfig(currConfig.getLatestVersion());
            loadRequiredSchemas();
            Iterator<Generator> generators = parser.getTheConfig().getGenerator();
            readFileHeader(parser.getTheConfig(), currConfig.getLatestVersion());
            if (generators != null) {
                while (generators.hasNext()) {
                    Generator g = generators.next();
                    g.getGenerator().setProgressStream(System.out);
                    g.getGenerator().setFileHeader(fileHeader);
                    FileUpdateManager.instance().reportProgress(System.out);
                    FileUpdateManager.instance().reportErrors(System.err);
                    FileUpdateManager.instance().generationStarting();
                    g.getGenerator().generateCode(parser.getTheConfig(), currConfig.getLatestVersion(), configFinder, readSchemas);
                    FileUpdateManager.instance().generationComplete();
                }
            }
        } catch (ResultException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (DmcValueException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (DmcValueExceptionSet e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
