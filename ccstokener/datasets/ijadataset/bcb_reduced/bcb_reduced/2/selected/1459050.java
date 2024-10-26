package com.moviejukebox.gui;

import com.moviejukebox.gui.tools.OfdbPlugin;
import com.moviejukebox.gui.tools.Zip;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.AppleTrailersPlugin;
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.plugin.DefaultBackgroundPlugin;
import com.moviejukebox.plugin.DefaultPosterPlugin;
import com.moviejukebox.plugin.DefaultThumbnailPlugin;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.plugin.MovieListingPlugin;
import com.moviejukebox.plugin.MovieListingPluginBase;
import com.moviejukebox.plugin.OpenSubtitlesPlugin;
import com.moviejukebox.plugin.SetThumbnailPlugin;
import com.moviejukebox.scanner.FanartScanner;
import com.moviejukebox.scanner.MediaInfoScanner;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.scanner.PosterScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.writer.MovieJukeboxHTMLWriter;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;
import java.security.PrivilegedAction;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

public class MovieJukeBoxEditor {

    private static Logger logger = Logger.getLogger("moviejukebox");

    private Collection<MediaLibraryPath> movieLibraryPaths = new ArrayList<MediaLibraryPath>();

    private String movieLibraryRoot;

    private String tempFolder;

    private String tempSaveFolder;

    private String tempJukeboxDetailsRoot;

    private String skinHome;

    private String detailsDirName;

    private boolean forceThumbnailOverwrite;

    private boolean forcePosterOverwrite;

    private boolean fanartDownload;

    private OpenSubtitlesPlugin subtitlePlugin;

    private int backgroundWidth;

    private int backgroundHeight;

    String fanartToken;

    private AppleTrailersPlugin trailerPlugin;

    private boolean videoImagesDownload;

    private boolean moviejukeboxListing;

    MovieListingPlugin listingPlugin;

    private List<Movie> moviesList;

    private Library library;

    public List<Integer> movieFilter;

    public void init() {
        Formatter mjbFormatter = new Formatter() {

            public synchronized String format(LogRecord record) {
                return record.getMessage() + (String) java.security.AccessController.doPrivileged(new PrivilegedAction<Object>() {

                    public Object run() {
                        return System.getProperty("line.separator");
                    }
                });
            }
        };
        FileHandler fh;
        try {
            fh = new FileHandler("moviejukebox.log");
            fh.setFormatter(mjbFormatter);
            fh.setLevel(Level.ALL);
            logger.addHandler(fh);
        } catch (IOException ex) {
            Logger.getLogger(MovieJukeBoxEditor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(MovieJukeBoxEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(mjbFormatter);
        ch.setLevel(Level.FINE);
        logger.setUseParentHandlers(false);
        logger.addHandler(ch);
        logger.setLevel(Level.ALL);
        if (!PropertiesUtil.setPropertiesStreamName("properties/moviejukebox.properties", "properties")) {
            return;
        }
        System.out.println(PropertiesUtil.getProperty("genres.max"));
        library = new Library();
        moviesList = new ArrayList<Movie>();
        this.movieLibraryRoot = "libraries.xml";
        this.detailsDirName = PropertiesUtil.getProperty("mjb.detailsDirName", "Jukebox");
        this.forceThumbnailOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceThumbnailsOverwrite", "false"));
        this.forcePosterOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forcePostersOverwrite", "false"));
        this.skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        this.fanartDownload = Boolean.parseBoolean(PropertiesUtil.getProperty("moviedb.fanart.download", "false"));
        this.tempFolder = "./temp";
        this.tempSaveFolder = tempFolder + File.separator + "save";
        this.tempJukeboxDetailsRoot = tempFolder + File.separator + detailsDirName;
        backgroundWidth = Integer.parseInt(PropertiesUtil.getProperty("background.width", "1280"));
        backgroundHeight = Integer.parseInt(PropertiesUtil.getProperty("background.height", "720"));
        fanartToken = PropertiesUtil.getProperty("fanart.scanner.fanartToken", ".fanart");
        listingPlugin = this.getListingPlugin(PropertiesUtil.getProperty("mjb.listing.plugin", "com.moviejukebox.plugin.MovieListingPluginBase"));
        this.moviejukeboxListing = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.listing.generate", "false"));
        videoImagesDownload = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));
        subtitlePlugin = new OpenSubtitlesPlugin();
        trailerPlugin = new AppleTrailersPlugin();
        File tempJukeboxDetailsRootFile = new File(tempSaveFolder);
        tempJukeboxDetailsRootFile.mkdirs();
        cleanSaveTemp();
        cleanTempJukeboxDetailsRoot();
    }

    /**
     * Setzt eine MediaLibraryPath Collection. Wenn danach eine Scanvorgang
     * gestartet wird, werden die Ordner in dieser Library duchsucht
     * @param mlp
     */
    public void setMovieLibraryPath(Collection<MediaLibraryPath> mlp) {
        this.movieLibraryPaths = mlp;
    }

    /**
     * Gibt die momentan gespeicherte MediaLibraryPath Collection zurück
     * @return
     */
    public Collection<MediaLibraryPath> getMovieLibraryPath() {
        return this.movieLibraryPaths;
    }

    /**
     * Speichert die momentane Library Collection in libraries.xml ab
     */
    public void saveMovieLibraryPathToXML(File f) {
        writeMovieLibraryRootFile(movieLibraryPaths, f);
    }

    /**
     * 
     */
    public void loadMovieLibraryPathFromXML(File f) {
        movieLibraryPaths.clear();
        movieLibraryPaths = this.parseMovieLibraryRootFile(f);
    }

    public void updateImdb(int index) {
        Movie m = this.getMovie(index);
        m.setPosterURL(Movie.UNKNOWN);
        m.setFanartURL(Movie.UNKNOWN);
        ImdbPlugin imdbPlugin = new ImdbPlugin();
        imdbPlugin.scan(m);
        updateMoviePoster(index);
        updateFanArt(index);
    }

    public void updateOfdb(int index) {
    }

    public void downloadOfdbInfos(int index, boolean plot, boolean title, boolean genres, boolean override) {
        Movie m = getMovie(index);
        if ((m.getId("ofdb").equalsIgnoreCase("UNKNOWN")) || override) {
            String tempplot = m.getPlot();
            Collection<String> tempgernes = new ArrayList<String>();
            for (String genre : m.getGenres()) {
                tempgernes.add(genre);
            }
            String temptitle = m.getTitle();
            String sorttitle = m.getTitle();
            (new OfdbPlugin()).scan(m);
            if (!genres) {
                m.setGenres(tempgernes);
            }
            if (!title) {
                m.setTitle(temptitle);
                m.setTitleSort(sorttitle);
            }
            if (!plot) {
                m.setPlot(tempplot);
            }
        } else {
            logger.fine("Found ofdb entry for movie " + m.getBaseName() + "! No translation needed");
        }
    }

    public void downloadOfdbInfosForAllMovies(boolean plot, boolean title, boolean genres, final javax.swing.JProgressBar mybar) {
        mybar.setMaximum(moviesList.size());
        mybar.setString("0 / " + moviesList.size());
        for (int i = 0; i < moviesList.size(); i++) {
            downloadOfdbInfos(i, plot, title, genres, false);
            mybar.setValue(i + 1);
            mybar.setString((i + 1) + " / " + moviesList.size());
        }
    }

    public void importJukebox(String dir) {
        saveXmlFiles();
        MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        File fdir = new File(dir);
        File[] files = fdir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String filename = files[i].getName();
                String smallfilename = filename.toLowerCase();
                if (smallfilename.endsWith("xml") && !smallfilename.startsWith("genres_") && !smallfilename.startsWith("categories.xml") && !smallfilename.startsWith("other_") && !smallfilename.startsWith("rating_") && !smallfilename.startsWith("title_")) {
                    Movie m = new Movie();
                    m.setBaseName(filename.substring(0, files[i].getName().length() - 4));
                    xmlWriter.parseMovieXML(files[i], m);
                    m.setFile(new File("ressources" + File.separator + "HCTest.jpg"));
                    System.out.println("Movie gefunden: " + m.getBaseName());
                    System.out.println("copy poster file to temp directory...");
                    File target = new File(tempSaveFolder + File.separator + m.getPosterFilename());
                    File source = new File(dir + File.separator + m.getPosterFilename());
                    FileTools.copyFile(source, target);
                    target = new File(tempSaveFolder + File.separator + m.getFanartFilename());
                    source = new File(dir + File.separator + m.getFanartFilename());
                    if (source.exists()) {
                        FileTools.copyFile(source, target);
                    }
                    target = new File(tempSaveFolder + File.separator + filename);
                    source = files[i];
                    FileTools.copyFile(source, target);
                    System.out.println("add movie to movie-list\n");
                }
            }
            reloadJukeboxFromTemp();
        }
    }

    private void reloadJukeboxFromTemp() {
        MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        library.clear();
        moviesList.clear();
        File fdir = new File(tempSaveFolder);
        File[] files = fdir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String filename = files[i].getName();
                String smallfilename = filename.toLowerCase();
                if (smallfilename.endsWith("xml") && !smallfilename.startsWith("genres_") && !smallfilename.startsWith("categories.xml") && !smallfilename.startsWith("other_") && !smallfilename.startsWith("rating_") && !smallfilename.startsWith("title_")) {
                    Movie m = new Movie();
                    m.setBaseName(filename.substring(0, files[i].getName().length() - 4));
                    xmlWriter.parseMovieXML(files[i], m);
                    m.setFile(new File("ressources" + File.separator + "HCTest.jpg"));
                    moviesList.add(m);
                }
            }
            System.out.println("load complete " + moviesList.size() + " movies added to movie-list");
        }
    }

    public void loadJukebox(String path) {
        cleanSaveTemp();
        library = new Library();
        moviesList.clear();
        MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        Zip.unzip(tempSaveFolder, path);
        File fdir = new File(tempSaveFolder);
        fdir.mkdirs();
        File[] files = fdir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String filename = files[i].getName();
                String smallfilename = filename.toLowerCase();
                if (smallfilename.endsWith("xml")) {
                    Movie m = new Movie();
                    m.setBaseName(filename.substring(0, files[i].getName().length() - 4));
                    xmlWriter.parseMovieXML(files[i], m);
                    m.setFile(new File("ressources" + File.separator + "HCTest.jpg"));
                    System.out.println("Movie gefunden: " + m.getBaseName());
                    System.out.println("add movie to movie-list\n");
                    moviesList.add(m);
                }
            }
            System.out.println("load complete " + moviesList.size() + " movies added to movie-list");
        }
    }

    public void saveJukebox(String FilePath) {
        saveXmlFiles();
        Zip.zip(FilePath, tempSaveFolder);
    }

    private void saveXmlFiles() {
        try {
            MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
            library.clear();
            for (Movie movie : moviesList) {
                library.addMovie(movie);
            }
            library.buildIndex();
            for (Movie movie : library.values()) {
                logger.finest("Writing index data to movie: " + movie.getBaseName());
                xmlWriter.writeMovieXML(tempSaveFolder, tempSaveFolder, movie, library);
            }
        } catch (Exception ex) {
            logger.fine("Couldn't save Jukebox");
        }
    }

    public void createJukebox(String jukeboxRoot, String skin, final javax.swing.JProgressBar mybar) {
        try {
            skinHome = "./skins/" + skin;
            PropertiesUtil.setPropertiesStreamName("properties/moviejukebox.properties", skinHome);
            String jukeboxDetailsRoot = jukeboxRoot + File.separator + detailsDirName;
            FileTools.copyDir(tempSaveFolder, tempJukeboxDetailsRoot);
            MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
            MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter();
            MovieImagePlugin thumbnailPlugin = this.getThumbnailPlugin(PropertiesUtil.getProperty("mjb.thumbnail.plugin", "com.moviejukebox.plugin.DefaultThumbnailPlugin"));
            MovieImagePlugin posterPlugin = this.getPosterPlugin(PropertiesUtil.getProperty("mjb.poster.plugin", "com.moviejukebox.plugin.DefaultPosterPlugin"));
            int max = moviesList.size() * 2 + 4;
            int counter = 0;
            mybar.setMaximum(max);
            mybar.setString(null);
            library.clear();
            for (Movie movie : moviesList) {
                library.addMovie(movie);
                counter++;
                mybar.setValue(counter);
            }
            library.buildIndex();
            for (Movie movie : library.values()) {
                logger.finest("Writing index data to movie: " + movie.getBaseName());
                xmlWriter.writeMovieXML(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie, library);
                logger.finest("Creating thumbnails for movie: " + movie.getBaseName());
                createThumbnail(thumbnailPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, true);
                logger.finest("Creating detail poster for movie: " + movie.getBaseName());
                createPoster(posterPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, true);
                htmlWriter.generateMovieDetailsHTML(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                counter++;
                mybar.setValue(counter);
            }
            logger.fine("Generating Indexes...");
            xmlWriter.writeIndexXML(tempJukeboxDetailsRoot, detailsDirName, library);
            xmlWriter.writeCategoryXML(tempFolder, detailsDirName, library);
            htmlWriter.generateMoviesIndexHTML(tempFolder, detailsDirName, library);
            htmlWriter.generateMoviesCategoryHTML(tempFolder, detailsDirName, library);
            counter++;
            mybar.setValue(counter);
            logger.fine("Copying new files to Jukebox directory...");
            FileTools.copyDir(tempJukeboxDetailsRoot, jukeboxDetailsRoot);
            FileTools.copyFile(new File(tempFolder + File.separator + "index.htm"), new File(jukeboxRoot + File.separator + "index.htm"));
            counter++;
            mybar.setValue(counter);
            logger.fine("Copying resources to Jukebox directory...");
            FileTools.copyDir(skinHome + File.separator + "html", jukeboxDetailsRoot);
            counter++;
            mybar.setValue(counter);
            logger.fine("Clean up temporary files");
            cleanTempJukeboxDetailsRoot();
            File rootIndex = new File(tempFolder + File.separator + "index.htm");
            rootIndex.delete();
            counter++;
            mybar.setValue(counter);
            try {
                if (moviejukeboxListing) {
                    logger.fine("Generating listing output...");
                    listingPlugin.generate(tempSaveFolder, jukeboxDetailsRoot, library);
                }
            } catch (Exception ex) {
                logger.fine("Couldn't generate Listing! " + ex.toString());
            }
            logger.fine("Process terminated.");
        } catch (Exception e) {
            logger.fine("Couldn't create Jukebox");
        }
        PropertiesUtil.setPropertiesStreamName("properties/moviejukebox.properties", "properties");
    }

    private void cleanTempJukeboxDetailsRoot() {
        File tempJukeboxDetailsRootFile = new File(tempJukeboxDetailsRoot);
        if (tempJukeboxDetailsRootFile.exists()) {
            File[] isoList = tempJukeboxDetailsRootFile.listFiles();
            for (int nbFiles = 0; nbFiles < isoList.length; nbFiles++) {
                isoList[nbFiles].delete();
            }
        }
    }

    private void cleanSaveTemp() {
        File tempJukeboxDetailsRootFile = new File(tempSaveFolder);
        if (tempJukeboxDetailsRootFile.exists()) {
            File[] isoList = tempJukeboxDetailsRootFile.listFiles();
            for (int nbFiles = 0; nbFiles < isoList.length; nbFiles++) {
                isoList[nbFiles].delete();
            }
        }
    }

    public int getSize() {
        return moviesList.size();
    }

    public int getMovieIndexFromBase(String movie) {
        for (Movie m : moviesList) {
            if (m.getBaseName().equalsIgnoreCase(movie)) {
                return moviesList.indexOf(m);
            }
        }
        return -1;
    }

    public Movie getMovie(int i) {
        Movie m;
        if (i < 0) {
            m = moviesList.get(0);
        } else {
            if (i >= moviesList.size()) {
                m = moviesList.get(moviesList.size() - 1);
            } else {
                m = moviesList.get(i);
            }
        }
        return m;
    }

    public void delMovie(int index) {
        Movie m = moviesList.get(index);
        File toDel = new File(tempSaveFolder + File.separator + m.getPosterFilename());
        toDel.delete();
        toDel = new File(tempSaveFolder + File.separator + m.getFanartFilename());
        toDel.delete();
        toDel = new File(tempSaveFolder + File.separator + m.getBaseName() + ".xml");
        toDel.delete();
        for (MovieFile part : m.getFiles()) {
            toDel = new File(tempSaveFolder + File.separator + part.getVideoImageFile(part.getFirstPart()));
            toDel.delete();
        }
        moviesList.remove(index);
    }

    public void updateMoviePoster(int index) {
        Movie movie = moviesList.get(index);
        String tmpDestFileName = tempSaveFolder + File.separator + movie.getPosterFilename();
        File tmpDestFile = new File(tmpDestFileName);
        try {
            logger.finest("Downloading poster for " + movie.getBaseName() + " to " + tmpDestFileName + " [calling plugin]");
            downloadImage(tmpDestFile, movie.getPosterURL());
        } catch (Exception e) {
            logger.finer("Failed downloading movie poster : " + movie.getPosterURL());
            FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(tempSaveFolder + File.separator + movie.getPosterFilename()));
        }
    }

    public void updateFanArt(int index) {
        Movie movie = moviesList.get(index);
        try {
            String extension = movie.getFanartURL().substring(movie.getFanartURL().length() - 4);
            movie.setFanartFilename(movie.getBaseName() + fanartToken + extension);
            String tmpDestFileName = tempSaveFolder + File.separator + movie.getFanartFilename();
            logger.finest("Downloading fanart for " + movie.getBaseName() + " to " + tmpDestFileName + " [calling plugin]");
            BufferedImage fanartImage = GraphicTools.loadJPEGImage(movie.getFanartURL());
            if (fanartImage != null) {
                fanartImage = GraphicTools.scaleToSizeNormalized(backgroundWidth, backgroundHeight, fanartImage);
                GraphicTools.saveImageToDisk(fanartImage, tmpDestFileName);
            } else {
                movie.setFanartFilename(Movie.UNKNOWN);
                movie.setFanartURL(Movie.UNKNOWN);
            }
        } catch (Exception e) {
            logger.finer("Failed downloading fanart poster : " + movie.getFanartURL());
            FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(tempSaveFolder + File.separator + movie.getFanartFilename()));
        }
    }

    public void writeMovieLibraryRootFile(Collection<MediaLibraryPath> libs, File f) {
        {
            if (libs.size() > 0) {
                try {
                    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
                    XMLStreamWriter writer = outputFactory.createXMLStreamWriter(new FileOutputStream(f), "UTF-8");
                    writer.writeStartDocument("UTF-8", "1.0");
                    writer.writeStartElement("libraries");
                    for (MediaLibraryPath mlp : libs) {
                        writer.writeStartElement("library");
                        String path = mlp.getPath();
                        String nmtpath = mlp.getNmtRootPath();
                        String excludes = "";
                        for (String excl : mlp.getExcludes()) {
                            excludes += excl + ",";
                        }
                        if (excludes.length() > 0) {
                            excludes = excludes.substring(0, excludes.length() - 1);
                        }
                        writer.writeStartElement("path");
                        writer.writeCharacters(path);
                        writer.writeEndElement();
                        writer.writeStartElement("nmtpath");
                        writer.writeCharacters(nmtpath);
                        writer.writeEndElement();
                        writer.writeStartElement("exclude");
                        writer.writeAttribute("name", excludes);
                        writer.writeEndElement();
                        writer.writeStartElement("description");
                        writer.writeCharacters(mlp.getDescription());
                        writer.writeEndElement();
                        writer.writeEndElement();
                    }
                    writer.writeEndElement();
                    writer.writeEndDocument();
                    writer.close();
                } catch (XMLStreamException ex) {
                    Logger.getLogger(MovieJukeBoxEditor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(MovieJukeBoxEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void scanFolders(final javax.swing.JProgressBar mybar) throws FileNotFoundException, XMLStreamException, ClassNotFoundException {
        saveXmlFiles();
        MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        MovieImagePlugin backgroundPlugin = this.getBackgroundPlugin(PropertiesUtil.getProperty("mjb.background.plugin", "com.moviejukebox.plugin.DefaultBackgroundPlugin"));
        MovieDirectoryScanner mds = new MovieDirectoryScanner();
        MediaInfoScanner miScanner = new MediaInfoScanner();
        logger.fine("Initializing...");
        File tempJukeboxDetailsRootFile = new File(tempSaveFolder);
        tempJukeboxDetailsRootFile.mkdirs();
        library.clear();
        for (MediaLibraryPath mediaLibraryPath : movieLibraryPaths) {
            logger.fine("Scanning media library " + mediaLibraryPath.getPath());
            library = mds.scan(mediaLibraryPath, library);
        }
        logger.fine("Found " + library.size() + " movies in your media library");
        mybar.setMaximum(library.size());
        mybar.setString("0 / " + library.size());
        logger.fine("Searching for movies information...");
        int i = 0;
        for (Movie movie : library.values()) {
            if (movie.isTVShow()) {
                logger.fine("Updating data for: " + movie.getTitle() + " [Season " + movie.getSeason() + "]");
            } else if (movie.isTrailer()) {
                logger.fine("Updating data for: " + movie.getTitle() + " [Trailer]");
            } else {
                logger.fine("Updating data for: " + movie.getTitle());
            }
            updateMovieData(xmlWriter, miScanner, backgroundPlugin, tempSaveFolder, tempSaveFolder, movie);
            if (movie.isTVShow()) {
                if (videoImagesDownload) {
                    updateVideoImages(tempSaveFolder, tempSaveFolder, movie);
                }
            }
            subtitlePlugin.generate(movie);
            trailerPlugin.generate(movie);
            logger.finer("Updating poster for: " + movie.getTitle() + "...");
            updateMoviePoster(tempSaveFolder, tempSaveFolder, movie);
            if (fanartDownload) {
                FanartScanner.scan(backgroundPlugin, tempSaveFolder, tempSaveFolder, movie);
            }
            logger.finest("Writing index data to movie: " + movie.getBaseName());
            xmlWriter.writeMovieXML(tempSaveFolder, tempSaveFolder, movie, library);
            i++;
            mybar.setValue(i);
            mybar.setString(i + " / " + library.size());
        }
        reloadJukeboxFromTemp();
    }

    /**
     * Generates a movie XML file which contains data in the <tt>Movie</tt> bean.
     *
     * When an XML file exists for the specified movie file, it is loaded into the
     * specified <tt>Movie</tt> object.
     *
     * When no XML file exist, scanners are called in turn, in order to add information
     * to the specified <tt>movie</tt> object. Once scanned, the <tt>movie</tt> object
     * is persisted.
     */
    private void updateMovieData(MovieJukeboxXMLWriter xmlWriter, MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) throws FileNotFoundException, XMLStreamException {
        boolean forceXMLOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceXMLOverwrite", "false"));
        boolean checkNewer = Boolean.parseBoolean(PropertiesUtil.getProperty("filename.nfo.checknewer", "true"));
        File xmlFile = new File(jukeboxDetailsRoot + File.separator + movie.getBaseName() + ".xml");
        List<File> nfoFiles = MovieNFOScanner.locateNFOs(movie);
        for (File nfoFile : nfoFiles) {
            if (FileTools.isNewer(nfoFile, xmlFile) && checkNewer && xmlFile.exists()) {
                logger.fine("NFO for " + movie.getTitle() + " has changed, will rescan file.");
                movie.setDirtyNFO(true);
                movie.setDirtyPoster(true);
                movie.setDirtyFanart(true);
                forceXMLOverwrite = true;
                break;
            }
        }
        if (xmlFile.exists() && !forceXMLOverwrite) {
            logger.finer("XML file found for " + movie.getBaseName());
            xmlWriter.parseMovieXML(xmlFile, movie);
            DatabasePluginController.scanTVShowTitles(movie);
            String thumbnailExtension = PropertiesUtil.getProperty("thumbnails.format", "png");
            movie.setThumbnailFilename(movie.getBaseName() + "_small." + thumbnailExtension);
            String posterExtension = PropertiesUtil.getProperty("posters.format", "png");
            movie.setDetailPosterFilename(movie.getBaseName() + "_large." + posterExtension);
            PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
        } else {
            if (forceXMLOverwrite) {
                logger.finer("Rescanning internet for information on " + movie.getBaseName());
            } else {
                logger.finer("XML file not found. Scanning internet for information on " + movie.getBaseName());
            }
            MovieNFOScanner.scan(movie, nfoFiles);
            if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN) || movie.isDirtyPoster()) {
                PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
            }
            miScanner.scan(movie);
            DatabasePluginController.scan(movie);
        }
    }

    /**
     * Update the movie poster for the specified movie.
     *
     * When an existing thumbnail is found for the movie, it is not overwriten,
     * unless the mjb.forceThumbnailOverwrite is set to true in the property file.
     *
     * When the specified movie does not contain a valid URL for the poster, a dummy image is used instead.
     *
     * @param tempJukeboxDetailsRoot
     */
    private void updateMoviePoster(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String posterFilename = jukeboxDetailsRoot + File.separator + movie.getPosterFilename();
        String tmpDestFileName = tempJukeboxDetailsRoot + File.separator + movie.getPosterFilename();
        File posterFile = new File(posterFilename);
        File tmpDestFile = new File(tmpDestFileName);
        if ((!tmpDestFile.exists() && !posterFile.exists()) || (movie.isDirtyPoster())) {
            posterFile.getParentFile().mkdirs();
            if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase("Unknown")) {
                logger.finest("Dummy image used for " + movie.getBaseName());
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(tempJukeboxDetailsRoot + File.separator + movie.getPosterFilename()));
            } else {
                try {
                    logger.finest("Downloading poster for " + movie.getBaseName() + " to " + tmpDestFileName + " [calling plugin]");
                    downloadImage(tmpDestFile, movie.getPosterURL());
                } catch (Exception e) {
                    logger.finer("Failed downloading movie poster : " + movie.getPosterURL());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(tempJukeboxDetailsRoot + File.separator + movie.getPosterFilename()));
                }
            }
        }
    }

    private void updateVideoImages(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String videoImageFilename;
        File videoImageFile;
        File tmpDestFile;
        for (MovieFile moviefile : movie.getMovieFiles()) {
            for (int part = moviefile.getFirstPart(); part <= moviefile.getLastPart(); ++part) {
                videoImageFilename = FileTools.makeSafeFilename(movie.getBaseName() + "_VideoImage_" + part + ".jpg");
                videoImageFile = new File(jukeboxDetailsRoot + File.separator + videoImageFilename);
                tmpDestFile = new File(tempJukeboxDetailsRoot + File.separator + videoImageFilename);
                if ((!tmpDestFile.exists() && !videoImageFile.exists())) {
                    videoImageFile.getParentFile().mkdirs();
                    if (moviefile.getVideoImageURL(part) == null || moviefile.getVideoImageURL(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                        logger.finest("Dummy video image used for " + movie.getBaseName() + " - part " + part);
                        try {
                            FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);
                        } catch (Exception e) {
                            logger.finer("Failed copying dummy video image file: dummy_videoimage.jpg");
                        }
                    } else {
                        try {
                            logger.finest("Downloading video image for " + movie.getBaseName() + " part " + part + " to " + tmpDestFile.getName() + " [calling plugin]");
                            downloadImage(tmpDestFile, moviefile.getVideoImageURL(part));
                            moviefile.setVideoImageFile(part, FileTools.makeSafeFilename(videoImageFilename));
                        } catch (Exception e) {
                            logger.finer("Failed downloading video image : " + moviefile.getVideoImageURL(part));
                            FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);
                        }
                    }
                }
            }
        }
        return;
    }

    @SuppressWarnings("unchecked")
    private Collection<MediaLibraryPath> parseMovieLibraryRootFile(File f) {
        Collection<MediaLibraryPath> mlp = new ArrayList<MediaLibraryPath>();
        if (!f.exists() || f.isDirectory()) {
            logger.severe("The moviejukebox library input file you specified is invalid: " + f.getName());
            return mlp;
        }
        try {
            XMLConfiguration c = new XMLConfiguration(f);
            List<HierarchicalConfiguration> fields = c.configurationsAt("library");
            for (Iterator<HierarchicalConfiguration> it = fields.iterator(); it.hasNext(); ) {
                HierarchicalConfiguration sub = it.next();
                String path = sub.getString("path");
                String nmtpath = sub.getString("nmtpath");
                String description = sub.getString("description");
                long prebuf = -1;
                String prebufString = sub.getString("prebuf");
                if (prebufString != null && !prebufString.isEmpty()) {
                    try {
                        prebuf = Long.parseLong(prebufString);
                    } catch (Exception ignore) {
                    }
                }
                if (!(nmtpath.endsWith("/") || nmtpath.endsWith("\\"))) {
                    nmtpath = nmtpath + "/";
                }
                List<String> excludes = sub.getList("exclude[@name]");
                if (new File(path).exists()) {
                    MediaLibraryPath medlib = new MediaLibraryPath();
                    medlib.setPath(path);
                    medlib.setNmtRootPath(nmtpath);
                    medlib.setExcludes(excludes);
                    medlib.setDescription(description);
                    medlib.setPrebuf(prebuf);
                    mlp.add(medlib);
                    logger.fine("Found media library: " + medlib);
                } else {
                    logger.fine("Skipped invalid media library: " + path);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed parsing moviejukebox library input file: " + f.getName());
            e.printStackTrace();
        }
        return mlp;
    }

    public MovieImagePlugin getThumbnailPlugin(String className) {
        MovieImagePlugin thumbnailPlugin;
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends DefaultThumbnailPlugin> pluginClass = cl.loadClass(className).asSubclass(DefaultThumbnailPlugin.class);
            thumbnailPlugin = pluginClass.newInstance();
        } catch (Exception e) {
            thumbnailPlugin = new DefaultThumbnailPlugin();
            logger.severe("Failed instanciating ThumbnailPlugin: " + className);
            logger.severe("Default thumbnail plugin will be used instead.");
            e.printStackTrace();
        }
        return thumbnailPlugin;
    }

    public MovieImagePlugin getPosterPlugin(String className) {
        MovieImagePlugin posterPlugin;
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            posterPlugin = pluginClass.newInstance();
        } catch (Exception e) {
            posterPlugin = new DefaultPosterPlugin();
            logger.severe("Failed instanciating PosterPlugin: " + className);
            logger.severe("Default poster plugin will be used instead.");
            e.printStackTrace();
        }
        return posterPlugin;
    }

    public MovieImagePlugin getBackgroundPlugin(String className) {
        MovieImagePlugin backgroundPlugin;
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            backgroundPlugin = pluginClass.newInstance();
        } catch (Exception e) {
            backgroundPlugin = new DefaultBackgroundPlugin();
            logger.severe("Failed instanciating BackgroundPlugin: " + className);
            logger.severe("Default background plugin will be used instead.");
            e.printStackTrace();
        }
        return backgroundPlugin;
    }

    public MovieListingPlugin getListingPlugin(String className) {
        MovieListingPlugin listingPlugin;
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieListingPlugin> pluginClass = cl.loadClass(className).asSubclass(MovieListingPlugin.class);
            listingPlugin = pluginClass.newInstance();
        } catch (Exception e) {
            listingPlugin = new MovieListingPluginBase();
            logger.severe("Failed instantiating ListingPlugin: " + className);
            logger.severe("NULL listing plugin will be used instead.");
            e.printStackTrace();
        }
        return listingPlugin;
    }

    /**
     * Download the image for the specified url into the specified file.
     *
     * @throws IOException
     */
    public static void downloadImage(File imageFile, String imageURL) throws IOException {
        URL url = new URL(imageURL);
        URLConnection cnx = url.openConnection();
        cnx.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-GB; rv:1.8.1.5) Gecko/20070719 Iceweasel/2.0.0.5 (Debian-2.0.0.5-0etch1)");
        FileTools.copy(cnx.getInputStream(), new FileOutputStream(imageFile));
    }

    public static void createThumbnail(MovieImagePlugin thumbnailManager, String rootPath, String tempRootPath, String skinHome, Movie movie, boolean forceThumbnailOverwrite) {
        try {
            String src = tempRootPath + File.separator + movie.getPosterFilename();
            String oldsrc = rootPath + File.separator + movie.getPosterFilename();
            String dst = tempRootPath + File.separator + movie.getThumbnailFilename();
            String olddst = rootPath + File.separator + movie.getThumbnailFilename();
            FileInputStream fis;
            if (!(new File(olddst).exists()) || forceThumbnailOverwrite || (new File(src).exists())) {
                if (new File(src).exists()) {
                    logger.finest("New file exists");
                    fis = new FileInputStream(src);
                } else {
                    logger.finest("Use old file");
                    fis = new FileInputStream(oldsrc);
                }
                BufferedImage bi = GraphicTools.loadJPEGImage(fis);
                if (bi == null) {
                    logger.info("Using dummy thumbnail image for " + movie.getTitle());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(rootPath + File.separator + movie.getPosterFilename()));
                    fis = new FileInputStream(src);
                    bi = GraphicTools.loadJPEGImage(fis);
                }
                String perspectiveDirection = PropertiesUtil.getProperty("thumbnails.perspectiveDirection", "right");
                if (perspectiveDirection.equalsIgnoreCase("both")) {
                    String dstMirror = dst.substring(0, dst.lastIndexOf(".")) + "_mirror" + dst.substring(dst.lastIndexOf("."));
                    logger.finest("Generating mirror thumbnail from " + src + " to " + dstMirror);
                    BufferedImage biMirror = bi;
                    biMirror = thumbnailManager.generate(movie, bi, "left");
                    GraphicTools.saveImageToDisk(biMirror, dstMirror);
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                    bi = thumbnailManager.generate(movie, bi, "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                }
                if (perspectiveDirection.equalsIgnoreCase("right")) {
                    bi = thumbnailManager.generate(movie, bi, "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                }
                if (perspectiveDirection.equalsIgnoreCase("left")) {
                    bi = thumbnailManager.generate(movie, bi, "left");
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating left thumbnail from " + src + " to " + dst);
                }
                logger.finest("Generating thumbnail from " + src + " to " + dst);
            }
        } catch (Exception e) {
            logger.severe("Failed creating thumbnail for " + movie.getTitle());
            e.printStackTrace();
        }
    }

    public static void createPoster(MovieImagePlugin posterManager, String rootPath, String tempRootPath, String skinHome, Movie movie, boolean forcePosterOverwrite) {
        try {
            String src = tempRootPath + File.separator + movie.getPosterFilename();
            String oldsrc = rootPath + File.separator + movie.getPosterFilename();
            String dst = tempRootPath + File.separator + movie.getDetailPosterFilename();
            String olddst = rootPath + File.separator + movie.getDetailPosterFilename();
            FileInputStream fis;
            if (!(new File(olddst).exists()) || forcePosterOverwrite || (new File(src).exists())) {
                if (new File(src).exists()) {
                    logger.finest("New file exists");
                    fis = new FileInputStream(src);
                } else {
                    logger.finest("Use old file");
                    fis = new FileInputStream(oldsrc);
                }
                BufferedImage bi = GraphicTools.loadJPEGImage(fis);
                if (bi == null) {
                    logger.info("Using dummy poster image for " + movie.getTitle());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(rootPath + File.separator + movie.getPosterFilename()));
                    fis = new FileInputStream(src);
                    bi = GraphicTools.loadJPEGImage(fis);
                }
                logger.finest("Generating poster from " + src + " to " + dst);
                String perspectiveDirection = PropertiesUtil.getProperty("posters.perspectiveDirection", "right");
                if (perspectiveDirection.equalsIgnoreCase("both")) {
                    String dstMirror = dst.substring(0, dst.lastIndexOf(".")) + "_mirror" + dst.substring(dst.lastIndexOf("."));
                    logger.finest("Generating mirror thumbnail from " + src + " to " + dstMirror);
                    BufferedImage biMirror = bi;
                    biMirror = posterManager.generate(movie, bi, "left");
                    GraphicTools.saveImageToDisk(biMirror, dstMirror);
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                    bi = posterManager.generate(movie, bi, "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                }
                if (perspectiveDirection.equalsIgnoreCase("right")) {
                    bi = posterManager.generate(movie, bi, "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                }
                if (perspectiveDirection.equalsIgnoreCase("left")) {
                    bi = posterManager.generate(movie, bi, "left");
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating left thumbnail from " + src + " to " + dst);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed creating poster for " + movie.getTitle());
            e.printStackTrace();
        }
    }
}
