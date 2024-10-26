package org.jdna.metadataupdater;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.jdna.cmdline.CommandLine;
import org.jdna.cmdline.CommandLineArg;
import org.jdna.cmdline.CommandLineProcess;
import sagex.SageAPI;
import sagex.api.Global;
import sagex.phoenix.Phoenix;
import sagex.phoenix.metadata.AutomaticMetadataVisitor;
import sagex.phoenix.metadata.IMetadataProvider;
import sagex.phoenix.progress.BasicProgressMonitor;
import sagex.phoenix.progress.ProgressTracker;
import sagex.phoenix.progress.TrackedItem;
import sagex.phoenix.tools.support.Troubleshooter;
import sagex.phoenix.vfs.IMediaFile;
import sagex.phoenix.vfs.IMediaFolder;
import sagex.phoenix.vfs.IMediaResource;
import sagex.phoenix.vfs.VirtualMediaFolder;
import sagex.phoenix.vfs.impl.FileResourceFactory;
import sagex.phoenix.vfs.util.PathUtils;
import sagex.util.Log4jConfigurator;

/**
 * This is an engine that will process one of more directories and files and
 * attempt to find metadata for each entry. The engine can operate in several
 * modes including automatic, stacked, etc.
 * 
 * @author seans
 * 
 */
@CommandLineProcess(acceptExtraArgs = true, description = "Import/Update TV/Movie information from an external source such as imdb or thetvdb.")
public class MetadataUpdater {

    private final Logger log = Logger.getLogger(MetadataUpdater.class);

    private BMTSageAPIProvider proxyAPI = null;

    private String[] files;

    private boolean listMedia = false;

    private boolean listProvders = false;

    private boolean offline = false;

    private boolean prompt = true;

    private boolean automaticUpdate = true;

    private int displaySize = 10;

    private boolean notifySageTV = false;

    private boolean support = false;

    private String notifyUrl = null;

    private String notifyUser = null;

    private String notifyPassword = null;

    /**
     * This method only needs to be called from the command line. All other
     * processes should use the MetadataUpdater directly and NOT call the main()
     * method.
     * 
     * @param args
     *            command line args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        new MetadataUpdater().run(args);
    }

    public void run(String args[]) throws Exception {
        Log4jConfigurator.configure("bmt", this.getClass().getClassLoader());
        proxyAPI = new BMTSageAPIProvider();
        SageAPI.setProvider(proxyAPI);
        try {
            String title = "Batch MetaData Tools (" + Version.VERSION + ")";
            System.out.println(title);
            logMetadataEnvironment();
            CommandLine cl = new CommandLine(title, "java MetadataTool", args);
            cl.process();
            try {
                cl.applyToAnnotated(this);
                if (cl.hasArg("help") || args == null || args.length == 0) {
                    cl.help(this);
                    return;
                }
            } catch (Exception e) {
                cl.help(this, e);
                return;
            }
            this.process();
        } catch (Exception e) {
            log.error("Failed to process Video MetaData!", e);
            System.out.println("Processing Failed, see log for details.");
        }
    }

    public void logMetadataEnvironment() {
        log.debug("========= BEGIN BATCH METADATA TOOLS ENVIRONMENT ==============");
        log.debug("   BMT Version:  " + bmt.api.GetVersion());
        log.debug(" Sagex Version:  " + sagex.api.Version.GetVersion());
        log.debug("  Java Version:  " + System.getProperty("java.version"));
        log.debug("Java Classpath:  " + System.getProperty("java.class.path"));
        log.debug("Fanart Enabled:  " + phoenix.fanart.IsFanartEnabled());
        log.debug("========= END BATCH METADATA TOOLS ENVIRONMENT ==============");
    }

    /**
     * This is the entry into the tool. This will process() all files/dirs that
     * are passed.
     * 
     * @throws Exception
     *             if processing fails for some unknown reason.
     */
    public void process() throws Exception {
        if (!automaticUpdate) {
            System.out.println("** Automatic Updating Disabled ***");
        }
        if (support) {
            List<File> locations = new ArrayList<File>();
            for (String file : files) {
                File dir = new File(file);
                if (dir.exists() && dir.isDirectory()) {
                    locations.add(dir);
                } else {
                    System.out.println("Skipping Location: " + dir.getAbsolutePath());
                }
            }
            System.out.println("Creating Support Zip File...");
            File out = Troubleshooter.createSupportZip("No Description: BMT Commandline", true, true, true);
            System.out.println("ZipFile Created: " + out.getPath());
            return;
        }
        if (listProvders) {
            renderProviders(Phoenix.getInstance().getMetadataManager().getProviders());
            return;
        }
        IMediaFolder parentFolder = null;
        List<IMediaResource> resources = new ArrayList<IMediaResource>();
        for (String f : files) {
            File file = new File(f);
            if (!file.exists() && offline == false) {
                System.out.println("File Not Found: " + file.getAbsolutePath() + "\nConsider adding --offline=true if you want to create an offline video.");
            } else {
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        log.warn("Failed to create file: " + file);
                    }
                }
                IMediaResource r = FileResourceFactory.createResource(null, file);
                resources.add(r);
            }
        }
        if (resources.size() == 1 && (resources.get(0) instanceof IMediaFolder)) {
            parentFolder = (IMediaFolder) resources.get(0);
        } else {
            parentFolder = new VirtualMediaFolder(null, "BMT Videos");
            for (IMediaResource r : resources) {
                ((VirtualMediaFolder) parentFolder).addMediaResource(r);
            }
        }
        if (parentFolder.getChildren().size() == 0) {
            System.out.println("No Files to process.");
        } else {
            if (listMedia) {
                parentFolder.accept(new ListMovieVisitor(false), new BasicProgressMonitor(), IMediaResource.DEEP_UNLIMITED);
                return;
            }
            ProgressTracker<IMediaFile> progress = new ProgressTracker<IMediaFile>(new ConsoleProgressMonitor());
            if (automaticUpdate) {
                parentFolder.accept(new AutomaticMetadataVisitor(null), progress, IMediaResource.DEEP_UNLIMITED);
            } else {
                parentFolder.accept(new ConsoleInteractiveMetadataVisitor(displaySize, null), progress, IMediaResource.DEEP_UNLIMITED);
            }
            System.out.printf("         Updated: %s items\n", progress.getSuccessfulItems().size());
            System.out.printf("Failed to Update: %s items\n", progress.getFailedItems().size());
            if (automaticUpdate && progress.getFailedItems().size() > 0 && prompt) {
                while (!progress.isCancelled() && progress.getFailedItems().peek() != null) {
                    TrackedItem<IMediaFile> item = progress.getFailedItems().pop();
                    System.out.println("  File: " + PathUtils.getLocation(item.getItem()));
                    System.out.println("Reason: " + item.getMessage());
                    System.out.println("---------------------------------");
                    item.getItem().accept(new ConsoleInteractiveMetadataVisitor(displaySize, null), new ConsoleProgressMonitor(), IMediaResource.DEEP_UNLIMITED);
                    System.out.println("---------------------------------");
                }
            } else {
                for (TrackedItem<IMediaFile> item : progress.getFailedItems()) {
                    System.out.println("  File: " + PathUtils.getLocation(item.getItem()));
                    System.out.println("Reason: " + item.getMessage());
                    System.out.println("---------------------------------");
                }
            }
        }
        if (notifySageTV) {
            try {
                if (notifyUrl != null) {
                    System.out.println("Notifying Sage to Refresh Imported Media via " + notifyUrl);
                    URL url = new URL(notifyUrl);
                    URLConnection conn = url.openConnection();
                    HTTPUtils.configueURLConnection(conn, notifyUser, notifyPassword);
                    conn.getContent();
                } else {
                    System.out.println("Notifying Sage to Refresh Imported Media using RMI");
                    proxyAPI.enableRemoteAPI(true);
                    Global.RunLibraryImportScan(false);
                }
            } catch (Throwable t) {
                System.out.println("Failed to notify SageTV: " + t.getMessage());
                log.error("Failed to notify sagetv for update", t);
            }
        }
    }

    public void renderProviders(List<IMetadataProvider> providers) {
        System.out.println("\n\nInstalled Metadata Providers (*=default");
        for (IMetadataProvider p : providers) {
            System.out.printf("%1s %-20s\n", "", p.getInfo().getId());
            System.out.println("   - " + p.getInfo().getName());
            System.out.println("   - " + p.getInfo().getDescription() + "\n");
        }
    }

    /**
     * set the files/dirs to process.
     * 
     * @param files
     */
    @CommandLineArg(name = "EXTRAARGS", description = "Internal Arg that collects files passed on the command line. do not set.")
    public void setFiles(String files[]) {
        this.files = files;
    }

    /**
     * used by the renderResult() to limit the # of results to show.
     * 
     * @param size
     */
    @CommandLineArg(name = "displaySize", description = "# of search results to display on the screen. (default 10)")
    public void setDisplaySize(String size) {
        displaySize = (Integer.parseInt(size));
    }

    /**
     * Just list the movies that it would find given the setFiles(...)
     * 
     * @param b
     */
    @CommandLineArg(name = "listMedia", description = "Just list the media it would find.")
    public void setListMovies(boolean b) {
        this.listMedia = b;
    }

    /**
     * List the know metadata provides that are installed.
     * 
     * @param b
     */
    @CommandLineArg(name = "listProviders", description = "Just list the installed metadata providers.")
    public void setListProviders(boolean b) {
        this.listProvders = b;
    }

    /**
     * enable/disable offline video support. Offline videos are empty video
     * files with metadata and thumbnails. A user would create these if the
     * video did exist in his/her collection but they are currently offline.
     * 
     * @param b
     */
    @CommandLineArg(name = "offline", description = "Create an offline file for the mediafile passed, and fetch its metadata.")
    public void setOffline(boolean b) {
        this.offline = b;
    }

    /**
     * prompt for selection
     * 
     * @param s
     */
    @CommandLineArg(name = "prompt", description = "When a media file cannot be updated, then prompt to enter a title to search on. (default true)")
    public void setPrompt(boolean b) {
        this.prompt = b;
    }

    /**
     * Force each item to be manually selected and updated.
     * 
     * @param s
     */
    @CommandLineArg(name = "auto", description = "Automatically choose best search result. [if false, it will force you to choose for each item] (default true)")
    public void setAutomaticUpdate(boolean b) {
        automaticUpdate = b;
    }

    @CommandLineArg(name = "support", description = "build a zip file with information for support")
    public void setSupport(boolean b) {
        support = b;
    }

    /**
     * Notify SageTV to refresh it's media
     * 
     * @param s
     */
    @CommandLineArg(name = "notifySageTV", description = "Notify SageTV to refresh it's Media Library. (default false, require RemoteAPIs to be installed)")
    public void setNotifySageTV(boolean b) {
        notifySageTV = b;
    }

    /**
     * Notify SageTV to refresh it's media, using the following url
     * 
     * @param s
     */
    @CommandLineArg(name = "notifyURL", description = "Notify SageTV to refresh it's Media Library using the provided URL")
    public void setNotifySageTVURL(String url) {
        notifyUrl = url;
        setNotifySageTV(true);
    }

    /**
     * Username for Notify Url
     * 
     * @param s
     */
    @CommandLineArg(name = "notifyUsername", description = "Username that is required for the notify url")
    public void setNotifyUsername(String user) {
        this.notifyUser = user;
    }

    /**
     * Username for Notify Url
     * 
     * @param s
     */
    @CommandLineArg(name = "notifyPassword", description = "Password that is required for the notify url")
    public void setNotifyPassword(String pass) {
        this.notifyPassword = pass;
    }

    /**
     * Enable SageTV remote configuration
     * 
     * @param s
     */
    @CommandLineArg(name = "remote", description = "Enable sagex Remote APIs for configuration.")
    public void setPerformTests(boolean remote) {
        proxyAPI.enableRemoteAPI(remote);
    }

    public String[] getFiles() {
        return files;
    }

    public void exit(String string) {
        System.out.println(string);
        System.exit(1);
    }
}
