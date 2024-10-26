package utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.json.JSONException;
import org.json.JSONStringer;

public class tools implements Constants {

    /**
   * Create an Internet shortcut (will overwrite existing internet shortcut)
   * @param where    location of the shortcut
   * @param URL      URL
   * @throws IOException
   */
    public static boolean createStrmFile(File where, String URL) {
        if (!valid(URL)) {
            Config.log(ERROR, "Cannot create a strm file because invalid parameters are specified, file=" + where + ", URL=" + URL);
            return false;
        }
        if (Config.IP_CHANGE_ENABLED) {
            for (IPChange change : Config.IP_CHANGES) {
                URL = URL.replace(change.getFrom(), change.getTo());
                Config.log(DEBUG, "After changing IP from \"" + change.getFrom() + "\" to \"" + change.getTo() + "\", URL=" + URL);
            }
        }
        String currentURL = null;
        if (where.isFile()) {
            currentURL = "";
            for (Iterator<String> it = tools.readFile(where).iterator(); it.hasNext(); ) {
                currentURL += it.next();
                if (it.hasNext()) currentURL += LINE_BRK;
            }
        }
        if (currentURL != null && currentURL.equals(URL)) {
            Config.log(DEBUG, "Not overwriting because file contents have not changed for: " + where);
            return true;
        } else {
            try {
                FileWriter fw = new FileWriter(where);
                fw.write(URL);
                fw.close();
                return true;
            } catch (Exception ex) {
                Config.log(WARNING, "Creating shortcut failed: " + ex.getMessage(), ex);
                return false;
            }
        }
    }

    public static String jsonKeyValue(String key, Object value) {
        try {
            String json = new JSONStringer().object().key(key).value(value).endObject().toString();
            return json.substring(1, json.length() - 1);
        } catch (JSONException ex) {
            Config.log(ERROR, "Cannot create JSON key value pair from key:" + key + ", value:" + value, ex);
            return "";
        }
    }

    public static boolean valid(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static String spacesToDots(String s) {
        if (s == null) return ""; else s = s.replaceAll(" ", ".").replaceAll(",", "");
        return s;
    }

    public static String safeFileName(String s) {
        if (!valid(s)) return "";
        String normal = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/') c = ' '; else if (Config.ILLEGAL_FILENAME_CHARS.get((int) c) == null) normal += c;
        }
        return stripInvalidXMLChars(normal.trim());
    }

    public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception x) {
            return false;
        }
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String normal = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/') c = ' '; else if (Config.UNCOMMON_CHARS.get((int) c) == null) normal += c;
        }
        return tools.stripInvalidXMLChars(normal).trim();
    }

    public static Document getXMLFromURL(java.net.URL url) {
        return getXMLFromURL(url, true);
    }

    public static Document getXMLFromURL(java.net.URL url, boolean useCaching) {
        try {
            SAXBuilder builder = new SAXBuilder();
            if (useCaching) {
                File cachedXMLFile = getXMLFromCache(url);
                if (cachedXMLFile != null) {
                    try {
                        Document xml = builder.build(cachedXMLFile);
                        return xml;
                    } catch (Exception x) {
                        Config.log(INFO, "Cached XML could not be parsed, reading from online source...", x);
                    }
                }
            }
            File tempXMLFile = new File(Config.BASE_PROGRAM_DIR + "\\res\\temp.xml");
            if (tempXMLFile.exists()) tempXMLFile.delete();
            tempXMLFile.createNewFile();
            FileWriter fstream = null;
            BufferedWriter tempXML = null;
            fstream = new FileWriter(tempXMLFile);
            tempXML = new BufferedWriter(fstream);
            URLConnection urlConn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                tempXML.write(stripInvalidXMLChars(line) + LINE_BRK);
                tempXML.flush();
            }
            in.close();
            tempXML.close();
            if (useCaching) {
                cacheXML(url, tempXMLFile);
            }
            Document xml = builder.build(tempXMLFile);
            return xml;
        } catch (Exception x) {
            Config.log(ERROR, "Could not get valid XML data from URL: " + url, x);
            String stack = getStacktraceAsString(x).toLowerCase();
            if (stack.contains("server returned http response code: 999") && stack.contains("us.music.yahooapis.com")) {
                Config.SCRAPE_MUSIC_VIDEOS = false;
                Config.log(WARNING, "Disabling future Yahoo Music Video scraping because requests are over-limit (Response code 999).");
            }
            return null;
        }
    }

    public static String getCachedXMLFileName(URL url) {
        String cacheFileName = safeFileName(url.toString().replace(Config.TVDB_API_KEY, Config.TVDB_API_KEY_OBSCURED).replace("http://", "").replace(SEP, "-").replace("/", "-"));
        String parent = Config.BASE_PROGRAM_DIR + SEP + "XMLCache" + SEP;
        String suffix = ".xml";
        int maxLength = MAX_FILENAME_LENGTH - (parent.length() + suffix.length());
        if (cacheFileName.length() > maxLength) cacheFileName = cacheFileName.substring(0, maxLength);
        return parent + cacheFileName + suffix;
    }

    private static void cacheXML(URL url, File XMLFromOnline) {
        File cachedFileLocation = new File(getCachedXMLFileName(url));
        if (!cachedFileLocation.getParentFile().exists()) cachedFileLocation.getParentFile().mkdir();
        if (cachedFileLocation.exists()) cachedFileLocation.delete();
        Config.log(DEBUG, "Caching XML from " + url + " to " + cachedFileLocation);
        try {
            cachedFileLocation.createNewFile();
            FileUtils.copyFile(XMLFromOnline, cachedFileLocation);
        } catch (Exception x) {
            Config.log(INFO, "Failed to copy file " + XMLFromOnline + " to " + cachedFileLocation, x);
        }
    }

    private static File getXMLFromCache(URL url) {
        File cachedFile = new File(getCachedXMLFileName(url));
        if (cachedFile.exists()) {
            Config.log(DEBUG, "Using XML from cached file: \"" + cachedFile + "\" for URL: " + url);
            return cachedFile;
        } else {
            Config.log(DEBUG, "No XML cache exists (" + cachedFile + ") for URL, will read from online source");
            return null;
        }
    }

    public static boolean writeToFile(File f, Collection lines, boolean overWrite) {
        if (overWrite) {
            try {
                if (f.exists()) f.delete();
                f.createNewFile();
            } catch (Exception x) {
                Config.log(WARNING, "Failed to overwrite file at: " + f, x);
                return false;
            }
        }
        for (Object o : lines) {
            String s = o.toString();
            if (!s.endsWith(LINE_BRK)) s += LINE_BRK;
            if (!writeToFile(f, s, true)) return false;
        }
        return true;
    }

    public static boolean writeToFile(File targetFile, String s, boolean append) {
        try {
            FileWriter writer = new FileWriter(targetFile, append);
            writer.write(s);
            writer.close();
            return true;
        } catch (Exception x) {
            Config.log(ERROR, "Cannot write to file: " + targetFile);
            return false;
        }
    }

    public static List<String> readFile(File f) {
        try {
            List<String> lines = new ArrayList<String>();
            Scanner scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
            scanner.close();
            return lines;
        } catch (Exception x) {
            Config.log(ERROR, "Cannot read file contents: " + x.getMessage(), x);
            return null;
        }
    }

    public static Set<File> getFilesArchivedBySource(String sourceName) {
        Set<File> files = new LinkedHashSet<File>();
        try {
            String sql = "SELECT dropbox_location FROM ArchivedFiles WHERE source_name = ?";
            List<String> dropboxLocations = Config.archivedFilesDB.getStringList(sql, params(sourceName));
            for (String dropboxLocation : dropboxLocations) {
                if (valid(dropboxLocation)) files.add(new File(dropboxLocation));
            }
            Config.log(INFO, "Found " + files.size() + " videos that are already archived in dropbox from source \"" + sourceName + "\"");
        } catch (Exception x) {
            Config.log(ERROR, "Cannot get source's archived files from SQLLite: " + x, x);
        }
        return files;
    }

    public static boolean addMetaDataChangeToDatabase(XBMCFile video, String typeOfMetaData, String newValue) {
        String dropboxLocation = video.getFinalLocation();
        String videoType = video.getType();
        String checkSQL = "SELECT id FROM QueuedChanges WHERE dropbox_location = ? AND meta_data_type = ?";
        int id = Config.queuedChangesDB.getSingleInt(checkSQL, params(dropboxLocation, typeOfMetaData));
        if (id == SQL_ERROR) {
            Config.log(ERROR, "Failed to check if this video already has a meta-data-change queues for type \"" + typeOfMetaData + "\": " + dropboxLocation);
            return false;
        }
        String sql;
        List<Param> params;
        final String insertSQL = "INSERT INTO QueuedChanges(dropbox_location, video_type, meta_data_type, value, status) " + "VALUES(?, ?, ?, ?, ?)";
        final List<Param> insertParams = tools.params(dropboxLocation, videoType, typeOfMetaData, newValue, QUEUED);
        final String updateAsQueuedSQL = "UPDATE QueuedChanges SET " + "video_type = ?, value = ?, status = ? WHERE id = ?";
        final List<Param> updateParams = tools.params(videoType, newValue, QUEUED, id);
        if (id > -1) {
            String currrentVal = Config.queuedChangesDB.getSingleString("SELECT value FROM QueuedChanges WHERE id = ?", params(id));
            boolean valueChanged = currrentVal == null || !currrentVal.equals(newValue);
            if (valueChanged) {
                String status = Config.queuedChangesDB.getSingleString("SELECT status FROM QueuedChanges WHERE id = ?", params(id));
                if (QUEUED.equalsIgnoreCase(status)) {
                    Config.log(INFO, "Changing queued meta-data " + typeOfMetaData + " from \"" + currrentVal + "\"  to \"" + newValue + "\" for " + videoType + " at: " + dropboxLocation);
                    sql = updateAsQueuedSQL;
                    params = updateParams;
                } else if (COMPLETED.equalsIgnoreCase(status)) {
                    Config.log(INFO, "Meta-data has changed. Will remove old meta-data and queue new meta-data for: " + dropboxLocation);
                    XBMCInterface xbmc = new XBMCInterface(Config.DATABASE_TYPE, (Config.DATABASE_TYPE.equals(MYSQL) ? Config.XBMC_MYSQL_VIDEO_SCHEMA : Config.sqlLiteVideoDBPath));
                    File archivedVideo = new File(dropboxLocation);
                    String xbmcPath = XBMCInterface.getFullXBMCPath(archivedVideo.getParentFile());
                    String fileIdSQL = "SELECT idFile " + "FROM files " + "WHERE idPath IN(SELECT idPath FROM path WHERE lower(strPath) = ?) " + "AND lower(strFileName) = ?";
                    int file_id = xbmc.getDB().getSingleInt(fileIdSQL, params(xbmcPath.toLowerCase(), archivedVideo.getName().toLowerCase()));
                    if (file_id < 0) {
                        if (file_id == SQL_ERROR) Config.log(WARNING, "Cannot update meta-data. Failed to determine idFile using: " + fileIdSQL); else Config.log(INFO, "Will not update meta-data. No file exists in XBMC's database");
                        return true;
                    }
                    int video_id = xbmc.getVideoIdInLibrary(videoType, file_id);
                    if (video_id < 0) {
                        if (file_id == SQL_ERROR) Config.log(WARNING, "Cannot update meta-data. Failed to determine video id"); else Config.log(INFO, "Will not update meta-data. No video exists in XBMC's database with idFile = " + file_id);
                        return true;
                    }
                    if (MOVIE_SET.equalsIgnoreCase(typeOfMetaData)) {
                        if (valid(currrentVal)) {
                            String removeMetadataSql = "DELETE FROM setlinkmovie " + "WHERE idSet = (SELECT idSet FROM sets WHERE strSet = ?) " + "AND idMovie = ?";
                            int rowsUpdated = xbmc.getDB().executeMultipleUpdate(removeMetadataSql, params(currrentVal, video_id));
                            if (rowsUpdated != SQL_ERROR) {
                                Config.log(INFO, "Successfully removed movie from old set \"" + currrentVal + "\" in preperation for adding to new set named \"" + newValue + "\"");
                            } else {
                                Config.log(ERROR, "Cannot update meta-data. Failed to remove movie from old set named \"" + currrentVal + "\" using " + removeMetadataSql);
                                return false;
                            }
                        }
                    } else if (PREFIX.equalsIgnoreCase(typeOfMetaData) || SUFFIX.equalsIgnoreCase(typeOfMetaData)) {
                        String xFixToRemove = currrentVal;
                        if (valid(xFixToRemove)) {
                            String idField, field, table;
                            if (videoType.equals(MOVIE)) {
                                field = "c00";
                                table = "movie";
                                idField = "idMovie";
                            } else if (videoType.equals(TV_SHOW)) {
                                field = "c00";
                                table = "episode";
                                idField = "idEpisode";
                            } else if (videoType.equals(MUSIC_VIDEO)) {
                                field = "c00";
                                table = "musicvideo";
                                idField = "idMVideo";
                            } else {
                                Config.log(WARNING, "Unknown video type: \"" + videoType + "\", will not update meta data");
                                return false;
                            }
                            String getCurrentValue = "SELECT " + field + " FROM " + table + " WHERE " + idField + " = ?";
                            String dbValue = xbmc.getDB().getSingleString(getCurrentValue, params(video_id));
                            String newDBValue = dbValue;
                            if (PREFIX.equalsIgnoreCase(typeOfMetaData)) {
                                if (dbValue.startsWith(xFixToRemove)) {
                                    newDBValue = dbValue.substring(xFixToRemove.length(), dbValue.length());
                                    Config.log(INFO, "Removing old prefix of \"" + xFixToRemove + "\" from \"" + dbValue + "\" for new value of \"" + newDBValue + "\"");
                                }
                            } else if (SUFFIX.equalsIgnoreCase(typeOfMetaData)) {
                                if (dbValue.endsWith(xFixToRemove)) {
                                    newDBValue = dbValue.substring(0, dbValue.indexOf(xFixToRemove));
                                    Config.log(INFO, "Removing old suffix of \"" + xFixToRemove + "\" from \"" + dbValue + "\" for new value of \"" + newDBValue + "\"");
                                }
                            }
                            if (newDBValue.equals(dbValue)) Config.log(WARNING, "The old suffix/prefix was not removed because it was not found. \"" + xFixToRemove + "\" not found in \"" + dbValue + "\""); else {
                                String removeXFixSQL = "UPDATE " + table + " SET " + field + " = ? WHERE " + idField + " = ?";
                                boolean updated = xbmc.getDB().executeSingleUpdate(removeXFixSQL, params(newDBValue, video_id));
                                if (!updated) Config.log(ERROR, "Failed to remove old prefix/suffix. Will not update meta-data. Sql = " + removeXFixSQL);
                            }
                        }
                    }
                    xbmc.close();
                    sql = updateAsQueuedSQL;
                    params = updateParams;
                } else {
                    Config.log(WARNING, "Unknown status in QueuedChanged table: \"" + status + "\". Will not update meta-data for: " + dropboxLocation);
                    return false;
                }
            } else {
                Config.log(DEBUG, "Meta-data has not changed for this video. Not updating. type=" + typeOfMetaData + ", value=" + newValue + ", file=" + dropboxLocation);
                return true;
            }
        } else {
            Config.log(valid(newValue) ? INFO : DEBUG, "Queueing new meta-data change: type=" + typeOfMetaData + ", value=" + newValue + ", file=" + dropboxLocation);
            sql = insertSQL;
            params = insertParams;
        }
        return Config.queuedChangesDB.executeSingleUpdate(sql, params);
    }

    public static String fileNameNoExt(File f) {
        String path = f.getPath();
        if (!f.getName().contains(".")) {
            Config.log(ERROR, "This files does not have an extension: " + path);
            return null;
        }
        int dotIndx = path.lastIndexOf(".");
        return path.substring(0, dotIndx);
    }

    public static boolean trackArchivedFile(String sourceName, String dropboxLocation, XBMCFile video) {
        if (!dropboxLocation.endsWith(".strm")) Config.log(ERROR, "File being archived is not a .strm: " + dropboxLocation);
        ArchivedFile currentlyArchivedFile = Config.archivedFilesDB.getArchivedFileByLocation(dropboxLocation);
        boolean updating = currentlyArchivedFile != null;
        String sql;
        if (updating) {
            boolean changed = !sourceName.equals(currentlyArchivedFile.sourceName) || !video.getFullPath().equals(currentlyArchivedFile.originalPath) || currentlyArchivedFile.missingSince != null || currentlyArchivedFile.missingCount != 0;
            if (!changed) {
                changed = !video.getType().equals(currentlyArchivedFile.videoType) || !video.getTitle().equals(currentlyArchivedFile.title) || video.hasBeenLookedUpOnTVDB() != currentlyArchivedFile.isTvDbLookup;
                if (!changed) {
                    if (video.isTvShow()) changed = !video.getSeries().equals(currentlyArchivedFile.series) || video.getSeasonNumber() != currentlyArchivedFile.seasonNumber || video.getEpisodeNumber() != currentlyArchivedFile.episodeNumber; else if (video.isMovie()) changed = video.getYear() != currentlyArchivedFile.year; else if (video.isMusicVideo()) changed = !video.getArtist().equals(currentlyArchivedFile.artist);
                }
            }
            if (!changed) {
                Config.log(DEBUG, "Nothing has changed for this video, no need to update tracker database for: " + dropboxLocation);
                return true;
            } else {
                Config.log(DEBUG, "Changes occurred for this video. updating database: " + dropboxLocation);
                sql = "UPDATE ArchivedFiles SET source_name = ?, dropbox_location = ?, " + "original_path = ?, missing_since = ?, missing_count = ?, date_archived = ?," + "video_type = ?, title = ?, series = ?, artist = ?, episode_number = ?, season_number = ?, year = ?, is_tvdb_lookup = ? " + "WHERE id = ?";
            }
        } else {
            sql = "INSERT INTO ArchivedFiles(source_name, dropbox_location, original_path, missing_since, missing_count, date_archived, " + "video_type, title, series, artist, episode_number, season_number, year, is_tvdb_lookup) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        PreparedStatement prep = null;
        try {
            prep = Config.archivedFilesDB.getStatement(sql);
            prep.setString(1, sourceName);
            prep.setString(2, dropboxLocation);
            prep.setString(3, video.getFullPath());
            prep.setTimestamp(4, null);
            prep.setInt(5, 0);
            prep.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            prep.setString(7, video.getType());
            prep.setString(8, video.getTitle());
            prep.setString(9, video.getSeries());
            prep.setString(10, video.getArtist());
            prep.setInt(11, video.getEpisodeNumber());
            prep.setInt(12, video.getSeasonNumber());
            prep.setInt(13, video.getYear());
            prep.setInt(14, video.hasBeenLookedUpOnTVDB() ? 1 : 0);
            if (currentlyArchivedFile != null) prep.setInt(15, currentlyArchivedFile.id);
            int updateCount = prep.executeUpdate();
            if (updateCount == 1) {
                Config.log(DEBUG, "Successfully " + (updating ? "updated" : "added") + " file in ArchivedFiles tracking table from source " + sourceName + ": " + Config.escapePath(video.getFullPath()) + ": " + dropboxLocation);
                return true;
            } else throw new Exception(updateCount + " rows were updated (expected 1).");
        } catch (Exception x) {
            Config.log(ERROR, "Failed to add archived file \"" + dropboxLocation + "\" to SQLite Database: " + x, x);
            return false;
        } finally {
            Config.archivedFilesDB.closeStatement();
        }
    }

    public static boolean markVideoAsMissing(String path) {
        ArchivedFile archivedFile = Config.archivedFilesDB.getArchivedFileByLocation(path);
        if (archivedFile == null) {
            Config.log(WARNING, "This file was not found in the ArchivedFiles database, cannot mark it as missing. Will set this file to be deleted.");
            return true;
        }
        Long missingSince = archivedFile.missingSince;
        int missingCount = archivedFile.missingCount;
        if (missingCount < 0) missingCount = 0;
        missingCount++;
        long now = System.currentTimeMillis();
        if (missingSince == null) missingSince = now;
        String updateSQL = "UPDATE ArchivedFiles SET missing_count = ?, missing_since = ? WHERE id = ?";
        try {
            PreparedStatement prep = Config.archivedFilesDB.getStatement(updateSQL);
            prep.setInt(1, missingCount);
            prep.setTimestamp(2, new java.sql.Timestamp(missingSince.longValue()));
            prep.setInt(3, archivedFile.id);
            prep.execute();
        } catch (Exception x) {
            Config.log(WARNING, "Failed to update file as missing using: " + updateSQL, x);
        } finally {
            Config.archivedFilesDB.closeStatement();
        }
        long missingForSeconds = (now - missingSince.longValue()) / 1000;
        double missingForHours = missingForSeconds / 60.0 / 60.0;
        if (missingForHours >= Config.MISSING_HOURS_DELETE_THRESHOLD && missingCount >= Config.MISSING_COUNT_DELETE_THRESHOLD) {
            Config.log(INFO, "This video should be deleted because it has been missing for " + toTwoDecimals(missingForHours) + " hours (threshold is " + Config.MISSING_HOURS_DELETE_THRESHOLD + "), " + "and has been missing the past " + missingCount + " times this program has checked for it (threshold is " + Config.MISSING_COUNT_DELETE_THRESHOLD + ")");
            return true;
        } else {
            String reason = "This file will not yet be deleted because: ";
            if (missingForHours < Config.MISSING_HOURS_DELETE_THRESHOLD) reason += "It has only been missing for " + toTwoDecimals(missingForHours) + " hours (less than threshold of " + Config.MISSING_HOURS_DELETE_THRESHOLD + "). ";
            if (missingCount < Config.MISSING_COUNT_DELETE_THRESHOLD) reason += "It has been missing the past " + missingCount + " times this program has checked, it must be missing for at least " + Config.MISSING_COUNT_DELETE_THRESHOLD + " times before it is deleted.";
            Config.log(INFO, reason);
            return false;
        }
    }

    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public static String stripInvalidXMLChars(String in) {
        StringBuilder validChars = new StringBuilder();
        StringBuilder inValidChars = new StringBuilder();
        if (!valid(in)) return "";
        for (int i = 0; i < in.length(); i++) {
            char current = in.charAt(i);
            int c = (int) current;
            if ((c >= 32 && c <= 126) || c == 10 || c == 13) validChars.append(current); else inValidChars.append(current);
        }
        return validChars.toString().trim();
    }

    public static String toTVDBAiredDate(Date dt) {
        try {
            return Config.tvdbFirstAiredSDF.format(dt);
        } catch (Exception x) {
            Config.log(ERROR, "Cannot format date as TVDB style using " + Config.tvdbFirstAiredSDF.toPattern() + ": " + dt);
            return null;
        }
    }

    public static boolean isNetworkShare(File f) {
        return isNetworkShare(f.getPath());
    }

    public static boolean isNetworkShare(String fullSharePath) {
        return Config.valid(fullSharePath) && fullSharePath.startsWith("\\\\");
    }

    public static boolean isShareAvailable(String fullSharePath) {
        if (!isNetworkShare(fullSharePath)) {
            Config.log(Config.WARNING, "Checking if share is available, but the file \"" + fullSharePath + "\" is not a network share");
            return new File(fullSharePath).getParentFile().exists();
        }
        int slashCount = 0;
        for (char c : fullSharePath.toCharArray()) if (c == '\\') slashCount++;
        if (slashCount >= 3) {
            int firstSeperator = fullSharePath.indexOf("\\", 2);
            int secondSeperator = fullSharePath.indexOf("\\", firstSeperator + 1);
            if (secondSeperator == -1) secondSeperator = fullSharePath.length();
            String baseShare = fullSharePath.substring(0, secondSeperator);
            File f = new File(baseShare);
            Config.log(DEBUG, "Checking if share at \"" + baseShare + "\" is available = " + f.exists());
            return f.exists();
        } else {
            Config.log(Config.WARNING, "Cannot check if the network share is available because the path is too short: " + fullSharePath);
            return false;
        }
    }

    public static String getStacktraceAsString(Exception x) {
        if (x == null) return null;
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        x.printStackTrace(printWriter);
        return writer.toString();
    }

    public static String cleanCommonWords(String s) {
        if (!valid(s)) return s;
        String[] articles = new String[] { "the", "a", "an", "The", "A", "An", "part", "Part" };
        for (String article : articles) {
            s = s.replace(" " + article + " ", " ");
            s = s.replace(" " + article, " ");
            s = s.replace(article + " ", " ");
            s = s.replace("  ", " ");
        }
        return s;
    }

    public static String cleanParenthesis(String s) {
        Pattern p = Pattern.compile("[\\(].*[\\)]");
        Matcher m = p.matcher(s);
        while (m.find()) {
            s = s.replace(m.group(), "");
        }
        return s;
    }

    public static String stripExtraLabels(String source) {
        if (source.toUpperCase().contains(" (HD)")) source = source.replace(" (HD)", "").replace(" (hd)", "");
        if (source.toUpperCase().contains(" [HD]")) source = source.replace(" [HD]", "").replace(" [hd]", "");
        return source;
    }

    public static boolean fuzzyTitleMatch(String source, String test, int percentDiscrepencyAllowed) {
        source = cleanCommonWords(cleanParenthesis(source.toLowerCase()));
        test = cleanCommonWords(cleanParenthesis(test.toLowerCase()));
        if (percentDiscrepencyAllowed > 100) percentDiscrepencyAllowed = 100;
        int fuzzyMatchMaxDifferent = source.length() / (100 / percentDiscrepencyAllowed);
        if (fuzzyMatchMaxDifferent <= 0) fuzzyMatchMaxDifferent = 1;
        int difference = getLevenshteinDistance(source, test);
        return difference <= fuzzyMatchMaxDifferent;
    }

    public static int getLevenshteinDistance(String s, String t) {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }
        int n = s.length();
        int m = t.length();
        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }
        if (n > m) {
            String tmp = s;
            s = t;
            t = tmp;
            n = m;
            m = t.length();
        }
        int p[] = new int[n + 1];
        int d[] = new int[n + 1];
        int _d[];
        int i;
        int j;
        char t_j;
        int cost;
        for (i = 0; i <= n; i++) {
            p[i] = i;
        }
        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;
            for (i = 1; i <= n; i++) {
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }
            _d = p;
            p = d;
            d = _d;
        }
        return p[n];
    }

    public static String tfl(String s, int fixedLength) {
        if (s == null) return s;
        if (s.length() >= fixedLength) return s.substring(0, fixedLength);
        int pads = fixedLength - s.length();
        for (int i = 0; i < pads; i++) s += " ";
        return s;
    }

    public static boolean verifyIpRange(String range) {
        try {
            String[] ips = range.split("-");
            for (String ipAddress : ips) {
                String[] octets = ipAddress.split("\\.");
                if (octets.length != 4) {
                    return false;
                }
                for (String s : octets) {
                    int i = Integer.parseInt(s);
                    if ((i < 0) || (i > 255)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception x) {
            Config.log(WARNING, "IP Range \"" + range + "\" is not valid: " + x);
            return false;
        }
    }

    public static boolean regexMatch(String regex, String test) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(test);
        return m.find();
    }

    public static String getRegexMatch(String regex, String test) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(test);
        if (m.find()) return m.group(); else return null;
    }

    public static String toTwoDecimals(double d) {
        DecimalFormat TWO_DECIMALS = new DecimalFormat("0.00");
        if ((d + "").equals("NaN")) return "0.00"; else return TWO_DECIMALS.format(d);
    }

    public static <T extends Document> void printXML(T t) {
        try {
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(t, System.out);
        } catch (IOException iox) {
            System.out.println("Failed to print xml: " + iox);
        }
    }

    public static XBMCFile getVideoFromOriginalLocation(String originalLocationUnescaped) {
        String sql = "SELECT original_path, dropbox_location, video_type, title, series, artist, episode_number, season_number, year, is_tvdb_lookup " + "FROM ArchivedFiles " + "WHERE original_path = ?";
        return Config.archivedFilesDB.getVideoWithMetaDataFromDB(sql, params(originalLocationUnescaped));
    }

    public static XBMCFile getVideoFromDropboxLocation(File f) {
        String sql = "SELECT original_path, dropbox_location, video_type, title, series, artist, episode_number, season_number, year, is_tvdb_lookup " + "FROM ArchivedFiles " + "WHERE dropbox_location = ?";
        return Config.archivedFilesDB.getVideoWithMetaDataFromDB(sql, params(f.getPath()));
    }

    public static String cleanJSONLabel(String jsonString) {
        if (valid(jsonString)) {
            jsonString = jsonString.replace("&#39;", "'");
            jsonString = jsonString.replace("/", "");
        }
        return jsonString;
    }

    public static String post(String strUrl, String data) throws Exception {
        URL url = new URL(strUrl);
        final String method = "POST";
        final String host = url.getHost();
        final String contentType = "application/x-www-form-urlencoded";
        final int contentLength = getContentLength(data);
        final String encoding = "UTF-8";
        final String connection = "Close";
        Config.log(DEBUG, "Sending data to: " + url + " (host=" + host + ", encoding=" + encoding + ", method=" + method + ", Content-Type=" + contentType + ", Content-Length=" + contentLength + ", Connection=" + connection + "):" + "\r\n" + data);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(method);
        conn.setRequestProperty("host", host);
        conn.setRequestProperty("content-type", contentType);
        conn.setRequestProperty("Content-Encoding", encoding);
        conn.setRequestProperty("content-length", contentLength + "");
        conn.setRequestProperty("connection", connection);
        if (tools.valid(Config.JSON_RPC_WEBSERVER_USERNAME) && tools.valid(Config.JSON_RPC_WEBSERVER_PASSWORD)) {
            String authString = Config.JSON_RPC_WEBSERVER_USERNAME + ":" + Config.JSON_RPC_WEBSERVER_PASSWORD;
            String authStringEnc = new sun.misc.BASE64Encoder().encode(authString.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
        }
        conn.setReadTimeout((int) (Config.JSON_RPC_TIMEOUT_SECONDS * 1000));
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(data);
        writer.flush();
        writer.close();
        int responseCode = 400;
        try {
            responseCode = conn.getResponseCode();
        } catch (Exception x) {
            Config.log(ERROR, "Failed to get response code from HTTP Server. Check your URL and username/password.", x);
        }
        String response = readStream(responseCode == 200 ? conn.getInputStream() : conn.getErrorStream());
        if (response == null) {
            return null;
        }
        Config.log(DEBUG, "Raw response from POST. Response Code = " + conn.getResponseCode() + " (" + conn.getResponseMessage() + "):\r\n" + response);
        return response.toString();
    }

    public static int getContentLength(String data) throws UnsupportedEncodingException {
        ByteArrayOutputStream sizeArray = new ByteArrayOutputStream();
        PrintWriter sizeGetter = new PrintWriter(new OutputStreamWriter(sizeArray, "UTF-8"));
        sizeGetter.write(data);
        sizeGetter.flush();
        sizeGetter.close();
        return sizeArray.size();
    }

    public static String readStream(InputStream is) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuilder data = new StringBuilder();
            while (true) {
                int i = in.read();
                if (i == -1) break;
                data.append((char) i);
            }
            return data.toString();
        } catch (IOException x) {
            Config.log(ERROR, "Failed to read stream: " + x, x);
            return null;
        }
    }

    public static boolean deleteStrmAndMetaFiles(File strmFile) {
        if (strmFile.exists()) {
            if (strmFile.getPath().toLowerCase().endsWith(".strm")) {
                String nameNoExt = tools.fileNameNoExt(strmFile);
                boolean deleted = strmFile.delete();
                if (!deleted) Config.log(WARNING, "Failed to delete .strm file: " + strmFile); else {
                    String[] metaExts = new String[] { ".nfo", ".tbn", "-fanart.jpg" };
                    for (String ext : metaExts) {
                        String path = nameNoExt + ext;
                        try {
                            File metaFile = new File(path);
                            if (metaFile.isFile()) {
                                if (metaFile.delete()) Config.log(DEBUG, "Deleted " + metaFile.getName());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                return deleted;
            } else {
                Config.log(WARNING, "Not deleting file because it does not have a .strm extension: " + strmFile.getPath());
                return false;
            }
        } else {
            Config.log(INFO, "Not deleting file because it does not exist on disk: " + strmFile);
            return false;
        }
    }

    public static List<Param> params(Object... params) {
        try {
            List<Param> paramList = new ArrayList<Param>();
            for (Object o : params) {
                if (o == null) Config.log(WARNING, "Null parameter found. Cannot auto-determine type!");
                Param param = null;
                if (o instanceof String) param = new Param(ParamType.STRING, (String) o); else if (o instanceof Integer) param = new Param(ParamType.INT, (Integer) o); else if (o instanceof Double) param = new Param(ParamType.DOUBLE, (Double) o); else if (o instanceof java.sql.Timestamp) param = new Param(ParamType.TIMESTAMP, (java.sql.Timestamp) o); else {
                    Config.log(WARNING, "Unknown param: " + o.getClass() + ": " + o);
                    param = new Param(ParamType.OBJECT, o);
                }
                paramList.add(param);
            }
            return paramList;
        } catch (Exception x) {
            Config.log(ERROR, "Failed to build parameter list from: " + Arrays.toString(params), x);
            return null;
        }
    }
}
