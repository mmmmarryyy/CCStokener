package subget;

import subget.exceptions.OsdbException;
import subget.osdb.Osdb;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import subget.bundles.Bundles;
import subget.exceptions.BadLoginException;
import subget.exceptions.BrokenAviHeaderException;
import subget.exceptions.LanguageNotSupportedException;
import subget.exceptions.MoveFileException;
import subget.exceptions.NotSupportedContainerException;
import subget.exceptions.SevenZipException;
import subget.exceptions.CharsetNotDetectedException;
import subget.exceptions.NoFPSException;
import subget.exceptions.TimeoutException;
import subget.exceptions.SubtitlesNotFoundException;

/**
 * @author povder
 *
 */
public class VideoFile implements Comparable {

    public static final String EXTENSIONS[] = { ".avi", ".mkv", ".mp4", ".ogm", ".mpg", ".wmv", ".vob", ".rmvb", ".rm", ".mpeg", ".divx", ".dv", ".ts", ".gmm", ".ivf", ".m1v", ".m2p", ".m2t", ".m4v", ".3gp", ".asx", ".avd", ".m4v", ".mgv", ".mmv", ".mod", ".mov", ".mpe", ".3g2", ".fbr", ".flv", ".movie", ".mp2v" };

    private File file;

    private String nameWithoutExtension;

    private String container;

    private String napiMd5sum;

    private String napiHash;

    private String osdbHash;

    private String outputSubsFileName;

    private File outputSubsDir;

    private String resolutionString = "";

    private String timeString = "";

    private String fpsString = "";

    private float fps = -1;

    private int time = 0;

    private long frames = 0;

    private ArrayList<SubtitleFile> foundSubs = new ArrayList<SubtitleFile>(5);

    private boolean hasInfo = false;

    private ArrayList<String> warnings = new ArrayList<String>(5);

    private boolean error = false;

    private int toDownloadCount = 1;

    private File infoFile = null;

    private boolean download = true;

    public boolean getDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public long getFrames() {
        return frames;
    }

    public String getOsdbHash() {
        return osdbHash;
    }

    public float getFps() {
        return fps;
    }

    public void increaseToDownloadCount() {
        ++toDownloadCount;
    }

    public void decreaseToDownloadCount() {
        --toDownloadCount;
    }

    public VideoFile(String fullPath) {
        file = new File(fullPath);
        napiMd5sum = "";
        napiHash = "";
        osdbHash = "";
        if (Global.getOneOutputFolder() == false) {
            outputSubsDir = new File(file.getParent());
        } else {
            outputSubsDir = new File(Global.getPathToOutputDir());
        }
        setContainer();
        setNameWithoutExtension();
        outputSubsFileName = nameWithoutExtension;
    }

    public VideoFile(File aFile) {
        file = aFile;
        napiMd5sum = "";
        napiHash = "";
        osdbHash = "";
        if (Global.getOneOutputFolder() == false) {
            outputSubsDir = new File(file.getParent());
        } else {
            outputSubsDir = new File(Global.getPathToOutputDir());
        }
        setContainer();
        setNameWithoutExtension();
        outputSubsFileName = nameWithoutExtension;
    }

    public boolean isError() {
        return error;
    }

    public int getTime() {
        return time;
    }

    public ArrayList<String> getWarnings() {
        return warnings;
    }

    public File getFile() {
        return file;
    }

    public ArrayList<SubtitleFile> getFoundSubtitles() {
        return foundSubs;
    }

    public void downloadSelectedSubtitles() throws InterruptedException {
        if (Global.getOneOutputFolder() == false) {
            infoFile = new File(outputSubsDir.getAbsolutePath() + "/SubGetInfo.txt");
        } else {
            infoFile = new File(outputSubsDir.getAbsolutePath() + "/[SubGetInfo]" + file.getName() + ".txt");
        }
        int errorCount = 0;
        Logging.logger.fine(String.format(Bundles.subgetBundle.getString("Downloading_subtitles_to_%s..."), file.getName()));
        int sameLangCount = 1;
        for (int i = 0; i < foundSubs.size(); ++i) {
            if (Thread.interrupted()) {
                throw (new InterruptedException());
            }
            if (foundSubs.get(i).getToDownload()) {
                try {
                    if (toDownloadCount != 1) {
                        downloadSubtitle(i, true, sameLangCount);
                    } else {
                        downloadSubtitle(i, false, 1);
                    }
                } catch (XmlRpcException ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("OSDb_error_while_downloading_%s_subtitles_to_%s_from_OSDb_database."), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                    warnings.add(String.format(Bundles.subgetBundle.getString("OSDb_error_while_downloading_%s_subtitles_to_%s_from_OSDb_database."), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                } catch (BadLoginException ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("OSDb_login_error_while_downloading_%s_subtitles_to_%s_from_OSDb_database."), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                    warnings.add(String.format(Bundles.subgetBundle.getString("OSDb_login_error_while_downloading_%s_subtitles_to_%s_from_OSDb_database."), foundSubs.get(i).getLanguage(), file.getName()));
                } catch (XmlRpcFault ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("OSDb_error_while_downloading_%s_subtitles_to_%s_from_OSDb_database."), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                    warnings.add(String.format(Bundles.subgetBundle.getString("OSDb_error_while_downloading_%s_subtitles_to_%s_from_OSDb_database."), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                } catch (OsdbException ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Error_while_downloading_%s_subtitles_to_%s_from_OSDb_database.\n") + ex.getDialogMessage(), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                    warnings.add(String.format(Bundles.subgetBundle.getString("Error_while_downloading_%s_subtitles_to_%s_from_OSDb_database.\n") + ex.getDialogMessage(), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                } catch (TimeoutException ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Timeout_while_downloading_%s_subtitles_to_%s_from_OSDb_database."), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                    warnings.add(String.format(Bundles.subgetBundle.getString("Timeout_while_downloading_%s_subtitles_to_%s_from_OSDb_database."), Language.XXToName(foundSubs.get(i).getLanguage()), file.getName()));
                } catch (UnknownHostException ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Unknown_host_-_%s._Aborting_download_of_%s_subtitles."), ex.getMessage(), Language.XXToName(foundSubs.get(i).getLanguage())));
                    warnings.add(String.format(Bundles.subgetBundle.getString("Unknown_host_-_%s."), ex.getMessage()));
                    ++errorCount;
                } catch (IOException ex) {
                    System.out.println(ex.getCause().toString());
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("I/O_error_occured_while_downloading_%s_subtitles."), Language.XXToName(foundSubs.get(i).getLanguage())));
                    warnings.add(String.format(Bundles.subgetBundle.getString("I/O_error_occured_while_downloading_%s_subtitles."), Language.XXToName(foundSubs.get(i).getLanguage())));
                    ++errorCount;
                } catch (MoveFileException ex) {
                    warnings.add(String.format(Bundles.subgetBundle.getString("Error_occured_while_moving_%s_subtitles_to_output_folder."), Language.XXToName(foundSubs.get(i).getLanguage())));
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Error_occured_while_moving_%s_subtitles_to_output_folder."), Language.XXToName(foundSubs.get(i).getLanguage())));
                    ++errorCount;
                }
            }
            if (i + 1 < foundSubs.size() && foundSubs.get(i).getLanguage().equals(foundSubs.get(i + 1).getLanguage())) {
                ++sameLangCount;
            } else {
                sameLangCount = 1;
            }
            if (foundSubs.get(i).getFile() != null && foundSubs.get(i).getFile().exists()) {
                foundSubs.get(i).getFile().delete();
            }
        }
        if (toDownloadCount == errorCount && toDownloadCount != 0) {
            error = true;
        }
        toDownloadCount = 0;
    }

    private void downloadSubtitle(int index, boolean addSuffix, int sameLangCount) throws MoveFileException, UnknownHostException, IOException, TimeoutException, XmlRpcException, BadLoginException, XmlRpcFault, OsdbException, InterruptedException {
        SubtitleFile sub = foundSubs.get(index);
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());
        if (sub.getWasDownloaded()) {
            Logging.logger.fine(String.format(Bundles.subgetBundle.getString("Omitting_earlier_downloaded_%s_subtitles."), sub.getLanguage()));
            return;
        }
        if (sub.getFromBase() == Global.SubDataBase.BASE_NAPI) {
            Logging.logger.fine(String.format(Bundles.subgetBundle.getString("Downloading_no._%d_%s_subtitles_from_NAPI_database..."), sameLangCount, Language.XXToName(sub.getLanguage())));
            processDownloadCharsetTranslation(sub, sameLangCount);
            processDownloadFormatConversion(sub, sameLangCount);
            sub.setExtension();
            if (addSuffix == true) {
                sub.setLanguageSuffix(sameLangCount);
            }
            sub.moveToFinalDestination();
            sub.setWasDownloaded(true);
            Logging.logger.finer(String.format(Bundles.subgetBundle.getString("Succesfully_downloaded_no._%d_%s_subtitles_to_%s."), sameLangCount, Language.XXToName(sub.getLanguage()), getFileName()));
            if (Global.getWriteInfo()) {
                FileWriter fw = new FileWriter(infoFile, true);
                fw.write(sub.getOutFileName().substring(1) + Bundles.subgetBundle.getString("\n\tLanguage:_") + Language.XXToName(sub.getLanguage()) + Bundles.subgetBundle.getString("\n\tDatabase:_") + " NAPI" + Bundles.subgetBundle.getString("\n\tFormat:_") + sub.getSubFormatString() + Bundles.subgetBundle.getString("\n\tUploader:_") + sub.getUploader() + Bundles.subgetBundle.getString("\n\tRating:_") + sub.getRating() + Bundles.subgetBundle.getString("\n\tDownload_date:_") + df.format(new Date()) + "\n\n");
                fw.close();
            }
            return;
        }
        if (sub.getFromBase() == Global.SubDataBase.BASE_OSDB) {
            Logging.logger.fine(String.format(Bundles.subgetBundle.getString("Downloading_and_unpacking_no._%d_%s_subtitles_from_OSDB_database..."), sameLangCount, Language.XXToName(sub.getLanguage())));
            sub.osdbDownloadAndUnpack();
            sub.detectFormat();
            processDownloadCharsetTranslation(sub, sameLangCount);
            processDownloadFormatConversion(sub, sameLangCount);
            sub.setExtension();
            if (addSuffix == true) {
                sub.setLanguageSuffix(sameLangCount);
            }
            sub.moveToFinalDestination();
            sub.setWasDownloaded(true);
            Logging.logger.finer(String.format(Bundles.subgetBundle.getString("Succesfully_downloaded_no._%d_%s_subtitles_to_%s."), sameLangCount, Language.XXToName(sub.getLanguage()), getFileName()));
            if (Global.getWriteInfo()) {
                FileWriter fw = new FileWriter(infoFile, true);
                fw.write(sub.getOutFileName().substring(1) + Bundles.subgetBundle.getString("\n\tLanguage:_") + Language.XXToName(sub.getLanguage()) + Bundles.subgetBundle.getString("\n\tDatabase:_") + " OSDb" + Bundles.subgetBundle.getString("\n\tFormat:_") + sub.getSubFormatString() + Bundles.subgetBundle.getString("\n\tUploader:_") + sub.getUploader() + Bundles.subgetBundle.getString("\n\tRating:_") + sub.getRating() + Bundles.subgetBundle.getString("\n\tDownload_date:_") + df.format(new Date()) + "\n\n");
                fw.close();
            }
        }
    }

    public void searchForSubtitles(String[] languages, boolean justOne) throws InterruptedException {
        int errorCount = 0;
        error = false;
        int databaseCount = 0;
        for (int a = 0; a < Global.getSubDataBases().size(); ++a) {
            if (Global.getSubDataBases().get(a) == Global.SubDataBase.BASE_NAPI && Global.getNapiDownload()) {
                ++databaseCount;
                try {
                    Logging.logger.fine(Bundles.subgetBundle.getString("Searching_in_NAPI_database..."));
                    for (int i = 0; i < languages.length; ++i) {
                        if (languages[i].equals("pl") == false && languages[i].equals("en") == false) {
                            continue;
                        }
                        if (Thread.interrupted()) {
                            throw (new InterruptedException("cancelled"));
                        }
                        try {
                            foundSubs.add(napiSearchForSubtitles(languages[i]));
                            Logging.logger.finer(String.format(Bundles.subgetBundle.getString("Found_1_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                            if (justOne) {
                                foundSubs.get(0).setToDownload(true);
                                return;
                            }
                        } catch (TimeoutException ex) {
                            Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Timeout_while_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                            warnings.add(String.format(Bundles.subgetBundle.getString("Timeout_while_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                        } catch (UnknownHostException ex) {
                            throw ex;
                        } catch (SevenZipException ex) {
                            throw ex;
                        } catch (MalformedURLException ex) {
                            Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Error_occured_while_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                            warnings.add(String.format(Bundles.subgetBundle.getString("Error_occured_while_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                        } catch (FileNotFoundException ex) {
                            Logging.logger.severe(String.format(Bundles.subgetBundle.getString("I/O_error_occured_while_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                            warnings.add(String.format(Bundles.subgetBundle.getString("I/O_error_occured_while_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                        } catch (IOException ex) {
                            Logging.logger.severe(String.format(Bundles.subgetBundle.getString("I/O_error_occured_while_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                            warnings.add(String.format(Bundles.subgetBundle.getString("I/O_error_occured_while_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                        } catch (NoSuchAlgorithmException ex) {
                            Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Error_occured_while_computing_MD5_sum_when_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                            warnings.add(String.format(Bundles.subgetBundle.getString("Error_occured_while_computing_MD5_sum_when_searching_for_%s_subtitles_to_%s_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                        } catch (LanguageNotSupportedException ex) {
                            Logging.logger.severe(String.format(Bundles.subgetBundle.getString("%s_language_is_not_supported."), Language.XXToName(languages[i])));
                            warnings.add(String.format(Bundles.subgetBundle.getString("%s_language_is_not_supported."), Language.XXToName(languages[i])));
                        } catch (SubtitlesNotFoundException ex) {
                            Logging.logger.warning(String.format(Bundles.subgetBundle.getString("No_%s_subtitles_to_%s_found_in_NAPI_database."), Language.XXToName(languages[i]), file.getName()));
                        }
                    }
                } catch (UnknownHostException ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Unknown_host_-_%s._Aborting_search."), ex.getMessage()));
                    ++errorCount;
                    warnings.add(String.format(Bundles.subgetBundle.getString("Unknown_host_-_%s."), ex.getMessage()));
                } catch (SevenZipException ex) {
                    Logging.logger.severe(Bundles.subgetBundle.getString("Can't_find_7zip_executable_or_you_don't_have_permission_to_execute_it._Aborting_search_in_NAPI_base."));
                    ++errorCount;
                    warnings.add(Bundles.subgetBundle.getString("Couldn't_find_7zip_executable_or_you_don't_have_permission_to_execute_it.\nSearch_in_NAPI_base_was_aborted."));
                }
            }
            if (Global.getSubDataBases().get(a) == Global.SubDataBase.BASE_OSDB && Global.getOsdbDownload()) {
                ++databaseCount;
                try {
                    Logging.logger.fine(Bundles.subgetBundle.getString("Searching_in_OSDb_database..."));
                    if (Thread.interrupted()) {
                        throw (new InterruptedException("cancelled"));
                    }
                    try {
                        ArrayList<SubtitleFile> found = osdbSearchForSubtitles(languages);
                        foundSubs.addAll(found);
                        Logging.logger.finer(String.format(Bundles.subgetBundle.getString("Found_%d_subtitles_to_%s_in_OSDb_database."), found.size(), file.getName()));
                    } catch (TimeoutException ex) {
                        Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Timeout_while_searching_for_subtitles_to_%s_in_OSdb_database."), file.getName()));
                        warnings.add(String.format(Bundles.subgetBundle.getString("Timeout_while_searching_for_subtitles_to_%s_in_OSdb_database."), file.getName()));
                    } catch (SubtitlesNotFoundException ex) {
                        Logging.logger.warning(String.format(Bundles.subgetBundle.getString("No_subtitles_to_%s_found_in_OSDb_database."), file.getName()));
                    } catch (IOException ex) {
                        Logging.logger.severe(String.format(Bundles.subgetBundle.getString("I/O_error_occured_while_searching_for_subtitles_to_%s_in_OSdb_database."), file.getName()));
                        warnings.add(String.format(Bundles.subgetBundle.getString("I/O_error_occured_while_searching_for_subtitles_to_%s_in_OSdb_database."), file.getName()));
                    } catch (XmlRpcException ex) {
                        if (ex.getCause() instanceof UnknownHostException) {
                            throw (UnknownHostException) ex.getCause();
                        } else {
                            if (ex.getCause() instanceof InterruptedException) {
                                throw (InterruptedException) ex.getCause();
                            }
                            Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Error_occured_while_searching_for_subtitles_to_%s_in_OSDb_database."), file.getName()));
                            warnings.add(String.format(Bundles.subgetBundle.getString("Error_occured_while_searching_for_subtitles_to_%s_in_OSDb_database."), file.getName()));
                        }
                    } catch (XmlRpcFault ex) {
                        Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Error_occured_while_searching_for_subtitles_to_%s_in_OSDb_database."), file.getName()));
                        warnings.add(String.format(Bundles.subgetBundle.getString("Error_occured_while_searching_for_subtitles_to_%s_in_OSDb_database."), file.getName()));
                    }
                } catch (UnknownHostException ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Unknown_host_-_%s._Aborting_search."), ex.getMessage()));
                    ++errorCount;
                    warnings.add(String.format(Bundles.subgetBundle.getString("Unknown_host_-_%s."), ex.getMessage()));
                } catch (BadLoginException ex) {
                    Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Could_not_login_to_OSDb._Aborting_search.")));
                    ++errorCount;
                    warnings.add(String.format(Bundles.subgetBundle.getString("Could_not_login_to_OSDb._Aborting_search.")));
                }
            }
        }
        if (foundSubs.size() != 0) {
            Logging.logger.finer(String.format(Bundles.subgetBundle.getString("Found_%d_subtitle_files_to_%s."), foundSubs.size(), file.getName()));
            Collections.sort(foundSubs, SubtitleFile.subtitleLanguageComparator);
            foundSubs.get(0).setToDownload(true);
        } else {
            Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Not_found_any_subtitle_files_to_%s."), file.getName()));
        }
        if (errorCount == databaseCount) {
            error = true;
        }
    }

    public SubtitleFile napiSearchForSubtitles(String language) throws UnknownHostException, MalformedURLException, FileNotFoundException, IOException, NoSuchAlgorithmException, LanguageNotSupportedException, SevenZipException, SubtitlesNotFoundException, InterruptedException, TimeoutException {
        if (language.equals("pl") == false && language.equals("en") == false) {
            throw new SubtitlesNotFoundException(String.format(Bundles.subgetBundle.getString("No_%s_subtitles_to_%s_found_Napiprojekt_base."), Language.XXToName(language), file.getName()));
        }
        if (Global.check7z() == false) {
            throw (new SevenZipException(Bundles.subgetBundle.getString("7zip_executable_not_found")));
        }
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        URL url = new URL(getNapiDownloadUrl(language));
        String subtitles7z = Global.getPathToTmpDir() + File.separator + napiMd5sum + ".7z";
        out = new BufferedOutputStream(new FileOutputStream(subtitles7z));
        conn = url.openConnection(Global.getProxy());
        in = Timeouts.getInputStream(conn);
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, numRead);
        }
        in.close();
        out.close();
        File subtitles7zFile = new File(subtitles7z);
        if (subtitles7zFile.length() <= 4) {
            subtitles7zFile.delete();
            SubtitlesNotFoundException e = new SubtitlesNotFoundException(String.format(Bundles.subgetBundle.getString("No_%s_subtitles_to_%s_found_Napiprojekt_base."), Language.XXToName(language), file.getName()));
            throw e;
        }
        SubtitleFile sub = new SubtitleFile(subtitles7z, outputSubsFileName, outputSubsDir.getAbsolutePath(), language);
        sub.napiUnpack();
        sub.detectFormat();
        sub.setFromBase(Global.SubDataBase.BASE_NAPI);
        sub.setDisplayName(outputSubsFileName);
        return sub;
    }

    public ArrayList<SubtitleFile> osdbSearchForSubtitles(String[] languages) throws UnknownHostException, IOException, XmlRpcException, XmlRpcFault, SubtitlesNotFoundException, InterruptedException, BadLoginException, TimeoutException {
        if (Global.getCancelOsdbSearch()) {
            throw (new BadLoginException(""));
        }
        ArrayList<SubtitleFile> subs;
        if (osdbHash.equals("")) {
            try {
                setOsdbHash();
            } catch (IOException e) {
                throw new IOException(Bundles.subgetBundle.getString("Error_while_computing_OSDB_hash."));
            }
        }
        subs = Osdb.searchSubtitles(osdbHash, file.length(), languages, outputSubsFileName, outputSubsDir);
        return subs;
    }

    public void setOsdbHash() throws IOException {
        osdbHash = Osdb.getOsdbHash(file);
    }

    private final long bytesToLong(byte[] b, int offset) {
        long l = 0;
        l |= b[3 + offset] & 0xFF;
        l <<= 8;
        l |= b[2 + offset] & 0xFF;
        l <<= 8;
        l |= b[1 + offset] & 0xFF;
        l <<= 8;
        l |= b[0 + offset] & 0xFF;
        return l;
    }

    public void getVideoInfo() throws IOException, BrokenAviHeaderException, NotSupportedContainerException {
        if (container.equalsIgnoreCase("AVI")) {
            FileInputStream fis = new FileInputStream(file);
            byte[] riff = new byte[4];
            fis.read(riff, 0, 4);
            if (riff[0] != 82 || riff[1] != 73 || riff[2] != 70 || riff[3] != 70) {
                throw new BrokenAviHeaderException(file.getName());
            }
            byte[] buffer = new byte[56];
            fis.skip(28);
            fis.read(buffer, 0, 56);
            int width = (int) bytesToLong(buffer, 32);
            int height = (int) bytesToLong(buffer, 36);
            long uSecPerFrame = bytesToLong(buffer, 0);
            frames = bytesToLong(buffer, 16);
            if (uSecPerFrame == 0) {
                throw new BrokenAviHeaderException(file.getName());
            }
            double FPS = (double) 1000000 / (double) uSecPerFrame;
            if (FPS == (double) 0) {
                throw new BrokenAviHeaderException(file.getName());
            }
            time = (int) Math.round((double) frames / FPS);
            resolutionString = String.valueOf(width) + "x" + String.valueOf(height);
            timeString = String.valueOf(time);
            if (String.valueOf(FPS).length() < 6) {
                fpsString = String.valueOf(FPS).substring(0, String.valueOf(FPS).length()).replace('.', ',');
            } else {
                fpsString = String.valueOf(FPS).substring(0, 6).replace('.', ',');
            }
            fps = (float) FPS;
            hasInfo = true;
            return;
        }
        throw new NotSupportedContainerException(container);
    }

    private String getNapiDownloadUrl(String lang) throws NoSuchAlgorithmException, IOException, LanguageNotSupportedException {
        if (napiHash.equals("")) {
            setNapiHash();
        }
        String url = "http://napiprojekt.pl/unit_napisy/dl.php?l=" + lang.toUpperCase() + "&f=" + napiMd5sum + "&t=" + napiHash + "&v=other&kolejka=false&nick=&pass=&napios=" + System.getProperty("os.name");
        return url;
    }

    private void setContainer() {
        int i = file.getName().lastIndexOf('.');
        if (i == -1) {
            container = "unknown";
        } else {
            container = file.getName().substring(i + 1).toUpperCase();
        }
    }

    private void setNameWithoutExtension() {
        int i = file.getName().lastIndexOf('.');
        if (i == -1) {
            nameWithoutExtension = file.getName();
        } else {
            nameWithoutExtension = file.getName().substring(0, i);
        }
    }

    public String getNapiHash() {
        return napiHash;
    }

    public String getNapiMd5sum() {
        return napiMd5sum;
    }

    public boolean getHasInfo() {
        return hasInfo;
    }

    public String getFpsString() {
        return fpsString;
    }

    public String getResolutionString() {
        return resolutionString;
    }

    public String getTimeString() {
        return timeString;
    }

    public void setNapiHash() throws NoSuchAlgorithmException, IOException {
        if (napiMd5sum.equals("")) {
            setNapiMd5sum();
        }
        napiHash = Napi.getNapiHash(napiMd5sum);
    }

    private void setNapiMd5sum() throws IOException, NoSuchAlgorithmException {
        napiMd5sum = Napi.getNapiMd5sum(file);
    }

    public String getFileName() {
        return file.getName();
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }

    @Override
    public int compareTo(Object o) {
        return getFileName().compareTo(VideoFile.class.cast(o).getFileName());
    }

    private void processDownloadCharsetTranslation(SubtitleFile sub, int sameLangCount) {
        if (Global.getEnableCharsetTranslation() && Global.getConvertDownloaded()) {
            try {
                sub.translateCharset();
                Logging.logger.finer(String.format(Bundles.subgetBundle.getString("Succesfully_translated_charset_of_no._%d_%s_subtitles_from_%s_to_%s."), sameLangCount, Language.XXToName(sub.getLanguage()), Global.getConvertFrom(), Global.getConvertTo()));
            } catch (LanguageNotSupportedException ex) {
                Logging.logger.severe(String.format(Bundles.subgetBundle.getString("%s_language_content_is_not_supported_for_charset_autodetection"), Language.XXToName(sub.getLanguage())));
                warnings.add(String.format(Bundles.subgetBundle.getString("%s_language_content_is_not_supported_for_charset_autodetection"), Language.XXToName(sub.getLanguage())));
            } catch (CharsetNotDetectedException ex) {
                Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Could_not_detect_charset_of_no._%d_%s_subtitles"), sameLangCount, Language.XXToName(sub.getLanguage())));
                warnings.add(String.format(Bundles.subgetBundle.getString("Could_not_detect_charset_of_no._%d_%s_subtitles"), sameLangCount, Language.XXToName(sub.getLanguage())));
            } catch (IOException ex) {
                Logging.logger.severe(String.format(Bundles.subgetBundle.getString("I/0_error_occured_while_translating_charset_of_no._%d_%s_subtitles"), sameLangCount, Language.XXToName(sub.getLanguage())));
                warnings.add(String.format(Bundles.subgetBundle.getString("I/0_error_occured_while_translating_charset_of_no._%d_%s_subtitles"), sameLangCount, Language.XXToName(sub.getLanguage())));
            }
        }
    }

    private void processDownloadFormatConversion(SubtitleFile sub, int sameLangCount) {
        if ((sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_MDVD && Global.getMdvdFormat().equals("") == false) || (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_MPL2 && Global.getMpl2Format().equals("") == false) || (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_SRT && Global.getSrtFormat().equals("") == false) || (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_SUBVIEWER && Global.getSubFormat().equals("") == false) || (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_TMP && Global.getTmpFormat().equals("") == false)) {
            String convertTo = "";
            try {
                if (Global.getEnableCharsetTranslation() == false || Global.getConvertDownloaded() == false || sub.getCharset().equals("")) {
                    boolean autodetect = false;
                    if (Global.getManualCharsetFormat()) {
                        sub.setCharset(Global.getCharsetFormat());
                    }
                    if (Global.getAskCharsetFormat()) {
                        String charset = Global.dialogs.showCharsetDialog(sub.getFile().getAbsolutePath(), true);
                        if (charset == null) {
                            throw (new CharsetNotDetectedException("cancelled"));
                        } else {
                            if (charset.equals("autodetect")) {
                                autodetect = true;
                            } else {
                                sub.setCharset(charset);
                            }
                        }
                    }
                    if (Global.getAutodetectCharsetFormat() || autodetect) {
                        Logging.logger.fine(Bundles.subgetBundle.getString("Autodetecting_charset..."));
                        try {
                            String[] autodetection = CharsetTranslation.detectCharset(sub.getFile(), sub.getLanguage());
                            Logging.logger.fine(String.format(Bundles.subgetBundle.getString("Detected_charset:_%s."), autodetection[0]));
                            if (autodetection[1].equals("unsure")) {
                                if (Global.dialogs.showWarningYesNoDialog(Bundles.subgetBundle.getString("Warning"), Bundles.subgetBundle.getString("There_is_only_50%_chance_that_detected_charset_(%s)_of_file_%s_is_correct.\n") + Bundles.subgetBundle.getString("Do_you_want_to_use_detected_charset?")) == false) {
                                    Logging.logger.fine(Bundles.subgetBundle.getString("Aborting_format_conversion."));
                                    throw (new CharsetNotDetectedException("aborted"));
                                } else {
                                }
                                String charset = Global.dialogs.showCharsetDialog(sub.getFile().getAbsolutePath(), false);
                                if (charset == null) {
                                    throw (new CharsetNotDetectedException("cancelled"));
                                } else {
                                    sub.setCharset(charset);
                                }
                            }
                            sub.setCharset(autodetection[0]);
                        } catch (LanguageNotSupportedException ex) {
                            throw (new CharsetNotDetectedException("language not supported"));
                        } catch (IOException ex) {
                            throw (new CharsetNotDetectedException("I/O error"));
                        }
                    }
                }
                if (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_TMP) {
                    convertTo = Global.getTmpFormat();
                }
                if (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_SRT) {
                    convertTo = Global.getSrtFormat();
                }
                if (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_MDVD) {
                    convertTo = Global.getMdvdFormat();
                    try {
                        if (hasInfo == false) {
                            getVideoInfo();
                        }
                    } catch (BrokenAviHeaderException ex) {
                        throw (new NoFPSException("broken header"));
                    } catch (NotSupportedContainerException ex) {
                        throw (new NoFPSException("unsupported container"));
                    }
                }
                if (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_SUBVIEWER) {
                    convertTo = Global.getSubFormat();
                }
                if (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_MPL2) {
                    convertTo = Global.getMpl2Format();
                }
                if (convertTo.equals("") == false) {
                    SubtitleFile.SubtitleFormat convertToFormat = null;
                    if (sub.getSubFormat() == SubtitleFile.SubtitleFormat.SUB_FORMAT_MDVD) {
                        try {
                            if (hasInfo == false) {
                                getVideoInfo();
                            }
                        } catch (BrokenAviHeaderException ex) {
                            throw (new NoFPSException("broken header"));
                        } catch (NotSupportedContainerException ex) {
                            throw (new NoFPSException("unsupported container"));
                        }
                    }
                    if (convertTo.equals("TMP")) {
                        convertToFormat = SubtitleFile.SubtitleFormat.SUB_FORMAT_TMP;
                    }
                    if (convertTo.equals("mDVD")) {
                        try {
                            if (hasInfo == false) {
                                getVideoInfo();
                            }
                        } catch (BrokenAviHeaderException ex) {
                            throw (new NoFPSException("broken header"));
                        } catch (NotSupportedContainerException ex) {
                            throw (new NoFPSException("unsupported container"));
                        }
                        convertToFormat = SubtitleFile.SubtitleFormat.SUB_FORMAT_MDVD;
                    }
                    if (convertTo.equals("SRT")) {
                        convertToFormat = SubtitleFile.SubtitleFormat.SUB_FORMAT_SRT;
                    }
                    if (convertTo.equals("SUB")) {
                        convertToFormat = SubtitleFile.SubtitleFormat.SUB_FORMAT_SUBVIEWER;
                    }
                    if (convertTo.equals("MPL2")) {
                        convertToFormat = SubtitleFile.SubtitleFormat.SUB_FORMAT_MPL2;
                    }
                    if (convertToFormat != null) {
                        Logging.logger.fine(Bundles.subgetBundle.getString("Converting_subtitles_format..."));
                        sub.convertFormat(convertToFormat, fps);
                        Logging.logger.fine(String.format(Bundles.subgetBundle.getString("Succesfully_converted_no_%d_%s_subtitle_format_to_%s."), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
                    }
                }
            } catch (NoFPSException ex) {
                Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Could_not_convert_no._%d_%s_subtitle_format_to_%s,_FPS_information_missing."), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
                warnings.add(String.format(Bundles.subgetBundle.getString("Could_not_convert_no._%d_%s_subtitle_format_to_%s,_FPS_information_missing."), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
            } catch (FileNotFoundException ex) {
                Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Error_occured_while_converting_no._%d_%s_subtitle_format_to_%s"), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
                warnings.add(String.format(Bundles.subgetBundle.getString("Error_occured_while_converting_no._%d_%s_subtitle_format_to_%s"), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
            } catch (IOException ex) {
                Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Error_occured_while_converting__no._%d_%s_subtitle_format_to_%s"), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
                warnings.add(String.format(Bundles.subgetBundle.getString("Error_occured_while_converting__no._%d_%s_subtitle_format_to_%s"), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
            } catch (CharsetNotDetectedException ex) {
                Logging.logger.severe(String.format(Bundles.subgetBundle.getString("Could_not_convert_no._%d_%s_subtitle_format_to_%s,_charset_information_missing."), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
                warnings.add(String.format(Bundles.subgetBundle.getString("Could_not_convert_no._%d_%s_subtitle_format_to_%s,_charset_information_missing."), sameLangCount, Language.XXToName(sub.getLanguage()), convertTo));
            }
        }
    }

    public void cleanUp() {
        for (int i = 0; i < foundSubs.size(); ++i) {
            if (foundSubs.get(i).getFile() != null && foundSubs.get(i).getFile().exists()) {
                foundSubs.get(i).getFile().delete();
            }
        }
    }
}

;
