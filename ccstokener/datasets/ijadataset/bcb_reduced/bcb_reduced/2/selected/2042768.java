package URLcrawler;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import mainpackage.FileStorage;
import mainpackage.Lyricscatcher;
import mainpackage.StringManager;
import settingsStorage.ConfigLoader;
import settingsStorage.ExternalDatabaseStorage;
import settingsStorage.NoSuchParameterException;
import settingsStorage.StatisticsStorage;
import filePackage.LyricsFile;
import filePackage.StrCompare;

public class DownloadThread extends SwingWorker<Void, String> {

    @Override
    protected Void doInBackground() {
        try {
            DownloadManager.running = true;
            DownloadManager.numberofrunningdownloadthreads++;
            while (DownloadManager.running) {
                try {
                    DownloadTask currenttask = null;
                    do {
                        currenttask = requestNextTask();
                        if (currenttask == null) {
                            DownloadManager.numberofrunningdownloadthreads--;
                            if (DownloadManager.numberofrunningdownloadthreads == 0) ExternalDatabaseStorage.Save();
                            return null;
                        }
                    } while (currenttask == null);
                    System.out.println("new task " + currenttask);
                    if (currenttask.task == DownloadTask.DOWNLOADCOVER) {
                        downloadCoverOf(currenttask);
                    }
                    if (currenttask.task == DownloadTask.DOWNLOADCOVERFROMLINK) {
                        downloadCoverFromLinkOf(currenttask);
                    } else if (currenttask.task == DownloadTask.DOWNLOADVIDEO) {
                        downloadVideoOf(currenttask);
                    } else if (currenttask.task == DownloadTask.DOWNLOADVIDEOFROMLINK) {
                        downloadVideoLinkOf(currenttask);
                    } else if (currenttask.task == DownloadTask.DOWNLOADLYRICS) {
                        downloadLyricsOf(currenttask);
                    } else if (currenttask.task == DownloadTask.DOWNLOADBACKGROUND) {
                        downloadBackgroundOf(currenttask);
                    } else if (currenttask.task == DownloadTask.DOWNLOADBACKGROUNDFROMLINK) {
                        downloadBackgroundFromLinkOf(currenttask);
                    } else if (currenttask.task == DownloadTask.DOWNLOADLYRICSCOVERVIDEOBACKGROUND) {
                        if (downloadLyricsOf(currenttask)) {
                            downloadCoverOf(currenttask);
                            downloadBackgroundOf(currenttask);
                            downloadVideoOf(currenttask);
                        }
                    } else if (currenttask.task == DownloadTask.DOWNLOADLYRICSCOVERBACKGROUND) {
                        if (downloadLyricsOf(currenttask)) {
                            downloadCoverOf(currenttask);
                            downloadBackgroundOf(currenttask);
                        }
                    } else if (currenttask.task == DownloadTask.DOWNLOADMP3) {
                        downloadMP3Of(currenttask);
                    } else if (currenttask.task == DownloadTask.DOWNLOADMP3LYRICSCOVERBACKGROUNDVIDEO) {
                        if (downloadMP3Of(currenttask)) {
                            if (downloadLyricsOf(currenttask)) {
                                downloadCoverOf(currenttask);
                                downloadBackgroundOf(currenttask);
                                downloadVideoOf(currenttask);
                            }
                        }
                    } else if (currenttask.task == DownloadTask.DOWNLOADMP3LYRICSCOVERBACKGROUND) {
                        if (downloadMP3Of(currenttask)) {
                            if (downloadLyricsOf(currenttask)) {
                                downloadCoverOf(currenttask);
                                downloadBackgroundOf(currenttask);
                            }
                        }
                    }
                    Piwik.log(currenttask.toPiwikString(), currenttask.source);
                } catch (Exception e) {
                    System.err.println("uncaught error in DownloadThread! But continuing work");
                    e.printStackTrace();
                }
            }
            DownloadManager.numberofrunningdownloadthreads--;
        } catch (Exception e) {
            System.err.println("uncaught error in DownloadThread!");
            e.printStackTrace();
        }
        if (DownloadManager.numberofrunningdownloadthreads == 0) ExternalDatabaseStorage.Save();
        return null;
    }

    @Override
    protected void process(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            DownloadManager.appendToLog(list.get(i));
        }
        DownloadManager.setSecondStatus(list.get(list.size() - 1));
    }

    @Override
    protected void done() {
        DownloadManager.running = false;
    }

    private DownloadTask requestNextTask() {
        final SaveLocation sl = new SaveLocation();
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    sl.setDownloadTask(DownloadManager.getNextTask());
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return sl.dt;
    }

    private boolean downloadMP3Of(DownloadTask currenttask) {
        String loc = retrieveMP3Location(currenttask);
        publish("found mp3 on:" + loc);
        if (loc == null) return false;
        try {
            return saveMP3(new URL(loc), currenttask);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean saveMP3(URL url, DownloadTask task) {
        if (url == null) {
            return false;
        }
        FileOutputStream out = null;
        InputStream stream = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File file = new File(task.source.getPreferredMP3Location());
        publish("Starting download to file:" + file.getAbsolutePath());
        try {
            out = new FileOutputStream(file);
            URLConnection uc = url.openConnection();
            String contentType = uc.getContentType();
            int length = uc.getContentLength();
            StatisticsStorage.numberofdownloadedbytes += length;
            publish("Content type: " + contentType + " with a length of " + length);
            stream = uc.getInputStream();
            bis = new BufferedInputStream(stream);
            bos = new BufferedOutputStream(out);
            byte[] bytes = new byte[1024];
            int i = 0;
            long time = System.currentTimeMillis();
            int nextbyte;
            while ((nextbyte = bis.read()) != -1) {
                bos.write(nextbyte);
                i++;
                if (System.currentTimeMillis() - time >= 1000) {
                    publish("downloaded " + (i++) + " bytes of " + length + " bytes.");
                    time = System.currentTimeMillis();
                    int progress = (i - 1) / length;
                    if (progress <= 100 && progress >= 0) setProgress(progress);
                }
            }
            bos.write(bytes);
        } catch (IOException e) {
            System.err.println(url);
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (out != null) out.close();
                if (stream != null) stream.close();
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                return false;
            }
        }
        publish("done downloading mp3");
        task.source.setPath(file.getAbsolutePath());
        FileStorage.addMP3FileInformation(task.source);
        if (task.source.hasLyrics()) {
            task.source.getLyrics().setTag("MP3", file.getName());
        }
        return true;
    }

    private String retrieveMP3Location(DownloadTask currenttask) {
        try {
            URL starturl = new URL(ConfigLoader.readString("null0") + currenttask.source.getArtist().replace(" ", "%20"));
            String artistpage = URLLoadClass.ReadURLString(starturl);
            publish("accessing URL or mp3:" + starturl);
            ArrayList<String> list = URLLoadClass.RetrieveStringsBetweenMarks(artistpage, ConfigLoader.readString("null1"), ConfigLoader.readString("null2"));
            publish("artists found:" + list.size());
            for (int i = 0; i < list.size(); i++) {
                System.out.println("artist link:" + ConfigLoader.readString("null3") + list.get(i));
                if (list.get(i).contains(".html")) {
                    try {
                        publish("artist link:" + ConfigLoader.readString("null3") + list.get(i));
                        String songlistpage = URLLoadClass.ReadURLString(new URL(ConfigLoader.readString("null3") + list.get(i)));
                        ArrayList<String> songlist = URLLoadClass.RetrieveStringsBetweenMarks(songlistpage, ConfigLoader.readString("null4"), ConfigLoader.readString("null5"));
                        publish("songs found:" + songlist.size());
                        for (int j = 0; j < songlist.size(); j++) {
                            if (songlist.get(j).contains(".html/") && !ArraylistAlreadyContains(list, songlist.get(j))) {
                                list.add(j + 1, songlist.get(j));
                            }
                            if (StrCompare.isSimilar(songlist.get(j), currenttask.source.getTitle())) {
                                try {
                                    publish("found the song! " + songlist.get(j));
                                    String number = StrCompare.retrieveNumberInString(URLLoadClass.RetrieveStringsBetweenMarks(songlist.get(j), "-", ".mp3").get(0));
                                    String player = ConfigLoader.readString("null10") + number;
                                    System.out.println("player-page can be found @ " + player);
                                    String playerpage = URLLoadClass.ReadURLString(new URL(player));
                                    String songinfo = ConfigLoader.readString("null6") + URLLoadClass.RetrieveStringsBetweenMarks(playerpage, ConfigLoader.readString("null11"), ConfigLoader.readString("null12")).get(0);
                                    System.out.println("reading information from " + songinfo);
                                    String xml = URLLoadClass.ReadURLString(new URL(songinfo));
                                    System.out.println("xml:" + xml);
                                    String location = URLLoadClass.RetrieveStringsBetweenMarks(xml, ConfigLoader.readString("null8"), ConfigLoader.readString("null9")).get(0);
                                    String result = ConfigLoader.readString("null3") + location;
                                    System.out.println("result:" + result);
                                    if (URLLoadClass.EstimateLengthOfPage(new URL(result)) > 1500) {
                                        return result;
                                    } else {
                                        publish("too small");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NoSuchParameterException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean ArraylistAlreadyContains(ArrayList<?> list, Object obj) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(obj)) return true;
        }
        return false;
    }

    private boolean downloadLyricsOf(DownloadTask currenttask) {
        try {
            System.out.println("Trying to download lyrics");
            String USDB = ConfigLoader.readString("USDB0");
            String link = USDB + ConfigLoader.readString("USDB1");
            link += currenttask.source.getArtist();
            link += ConfigLoader.readString("USDB2") + currenttask.source.getTitle();
            link += ConfigLoader.readString("USDB3") + DownloadManager.getUserName();
            link += ConfigLoader.readString("USDB4") + DownloadManager.getPassword();
            System.out.println("Trying to download lyrics from:");
            String page = URLLoadClass.ReadURLString(new URL(link.replaceAll(" ", "%20")));
            System.out.println("Downloaded possible lyrics from:");
            if (page.contains("no result")) {
                publish("found no results for " + currenttask);
                return false;
            } else if (page.contains("SongID")) {
                publish("found 1 result for " + currenttask);
                saveDownloadedPage(page, currenttask);
                return true;
            } else if (page.contains("result:")) {
                publish("found several results for " + currenttask);
                if (DownloadManager.downloadAllLyrics()) {
                    int numberofresults = Integer.parseInt(URLLoadClass.RetrieveStringsBetweenMarks(page, "result:", "\n").get(0));
                    for (int i = 1; i <= numberofresults; i++) {
                        downloadLyricsFrom(link + ConfigLoader.readString("USDB5") + i, currenttask);
                    }
                } else {
                    downloadLyricsFrom(link + ConfigLoader.readString("USDB5") + 1, currenttask);
                }
                return true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchParameterException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void downloadVideoOf(DownloadTask task) {
        URL url = retrieveVideoDownloadLocation(task);
        saveVideo(url, task);
    }

    private void saveDownloadedPage(String page, DownloadTask currenttask) {
        try {
            publish("Saving lyrics");
            String saveloc = currenttask.source.getPreferredLyricsLocation();
            String number = URLLoadClass.RetrieveStringsBetweenMarks(page.replaceAll("<br>", "\n"), ConfigLoader.readString("USDB6"), "\n").get(0);
            int seconds = Integer.parseInt(number);
            publish("Waiting for " + seconds + " seconds as asked by USDB-admins. \nAdd songs to usdb.kilu.de to reduce this time.");
            if (seconds != 0 && !Lyricscatcher.DEBUGGING) {
                synchronized (this) {
                    try {
                        wait(1000 * seconds);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            page = page.replaceAll("<br>#", "\n#");
            page = page.replaceAll("<br>", "");
            page = page.replaceAll("&nbsp;", " ");
            page = URLLoadClass.removeFirstLines(page, 3);
            publish("Saving lyrics to:" + saveloc);
            FileWriter fw = new FileWriter(saveloc);
            fw.write(page);
            fw.close();
            LyricsFile lf = new LyricsFile(saveloc, currenttask.source);
            FileStorage.addLyricsFile(lf);
            currenttask.source.setLyrics(lf);
            if (currenttask.source.hasLyrics()) {
                currenttask.source.getLyrics().setTag("MP3", currenttask.source.getName());
                if (currenttask.source.hasVideo()) {
                    File vid = new File(currenttask.source.getVideo());
                    currenttask.source.getLyrics().setTag("Video", vid.getName());
                }
                if (currenttask.source.hasCover()) {
                    File cov = new File(currenttask.source.getImage());
                    currenttask.source.getLyrics().setTag("Cover", cov.getName());
                }
                if (currenttask.source.hasBackground()) {
                    File cov = new File(currenttask.source.getBackground());
                    currenttask.source.getLyrics().setTag("Cover", cov.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadLyricsFrom(String link, DownloadTask currenttask) {
        try {
            String page = URLLoadClass.ReadURLString(new URL(link.replaceAll(" ", "%20")));
            saveDownloadedPage(page, currenttask);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveVideo(URL url, DownloadTask task) {
        if (url == null) {
            return;
        }
        FileOutputStream out = null;
        InputStream stream = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File file = new File(task.source.getPreferredVideoLocation("mp4"));
        publish("Starting download to file:" + file.getAbsolutePath());
        try {
            out = new FileOutputStream(file);
            URLConnection uc = url.openConnection();
            String contentType = uc.getContentType();
            int length = uc.getContentLength();
            StatisticsStorage.numberofdownloadedbytes += length;
            publish("Content type: " + contentType + " with a length of " + length);
            stream = uc.getInputStream();
            bis = new BufferedInputStream(stream);
            bos = new BufferedOutputStream(out);
            byte[] bytes = new byte[1024];
            int i = 0;
            long time = System.currentTimeMillis();
            int nextbyte;
            while ((nextbyte = bis.read()) != -1) {
                bos.write(nextbyte);
                i++;
                if (System.currentTimeMillis() - time >= 1000) {
                    publish("downloaded " + (i++) + " bytes of " + length + " bytes.");
                    time = System.currentTimeMillis();
                    int progress = (i - 1) / length;
                    if (progress <= 100 && progress >= 0) setProgress(progress);
                }
            }
            bos.write(bytes);
        } catch (IOException e) {
            System.err.println(url);
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
                if (stream != null) stream.close();
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("done downloading video");
        task.source.setVideo(file.getAbsolutePath());
        if (task.source.hasLyrics()) {
            task.source.getLyrics().setTag("Video", file.getName());
        }
    }

    private URL retrieveVideoDownloadLocation(DownloadTask task) {
        try {
            String l = "";
            if (task.source.hasLyrics()) l = task.source.getLyrics().getTag("youtube");
            if (l != null && l != "") {
                try {
                    if (l.indexOf("youtube.") == 0) l = "www." + l;
                    if (l.indexOf("www.") == 0) l += "http://" + l;
                    String videopage = URLLoadClass.ReadURLString(new URL(l));
                    publish("Link found in lyrics:" + l);
                    URL url = URLLoadClass.createDownloadURL(videopage);
                    publish("" + url);
                    if (URLLoadClass.EstimateLengthOfPage(url) / 1000000 <= DownloadManager.downloadMaximum()) if (url != null && URLLoadClass.EstimateLengthOfPage(url) <= DownloadManager.downloadMaximum() * 1000000 && URLLoadClass.getContentTypeOfPage(url).toLowerCase().indexOf("video") >= 0) {
                        return url;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String searchform = ConfigLoader.readString("Youtube");
            searchform = StringManager.fillInForm(task.source, searchform);
            searchform = StringManager.removeIllegalCharacters(searchform);
            URL yahoo = new URL(ConfigLoader.readString("yout0") + searchform.replace(" ", "+") + ConfigLoader.readString("yout1"));
            publish("Searching video for: " + task.source.getArtist());
            String searchpage = URLLoadClass.ReadURLString(yahoo);
            ArrayList<String> firsturls = URLLoadClass.RetrieveStringsBetweenMarks(searchpage, ConfigLoader.readString("yout2"), ConfigLoader.readString("yout3"));
            for (int i = 0; i < firsturls.size(); i++) {
                if (firsturls.get(i).indexOf("/watch") >= 0) {
                    publish("checking " + (i + 1) + "th videopages for video");
                    try {
                        String videopage = URLLoadClass.ReadURLString(new URL("http://www.youtube.com" + firsturls.get(i)));
                        publish("http://www.youtube.com" + firsturls.get(i));
                        URL url = URLLoadClass.createDownloadURL(videopage);
                        publish("" + url);
                        if (URLLoadClass.EstimateLengthOfPage(url) / 1000000 <= DownloadManager.downloadMaximum()) publish("video is too big:" + URLLoadClass.EstimateLengthOfPage(url) / 1000000 + "Mb which is bigger than " + DownloadManager.downloadMaximum() + "Mb (" + URLLoadClass.getContentTypeOfPage(url) + ")");
                        if (url != null && URLLoadClass.EstimateLengthOfPage(url) <= DownloadManager.downloadMaximum() * 1000000 && URLLoadClass.getContentTypeOfPage(url).toLowerCase().indexOf("video") >= 0) {
                            return url;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void downloadVideoLinkOf(DownloadTask task) {
        if (task.link.indexOf("youtube.") == 0) task.link = "www." + task.link;
        if (task.link.indexOf("www.") == 0) task.link += "http://" + task.link;
        URL url;
        try {
            url = URLLoadClass.createDownloadURL(URLLoadClass.ReadURLString(new URL(task.link)));
        } catch (MalformedURLException e) {
            System.err.println("Sorry, this is a bad link.");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            System.err.println("Could not connect.");
            e.printStackTrace();
            return;
        }
        if (URLLoadClass.EstimateLengthOfPage(url) == 0) {
            System.err.println("Sorry, this is an empty video.");
            return;
        }
        saveVideo(url, task);
    }

    private void downloadCoverOf(DownloadTask task) {
        String url = retrieveCoverURL(task);
        if (url == null) {
            publish("No cover found");
            return;
        }
        try {
            saveCover(task, new URL(url));
        } catch (MalformedURLException e) {
            System.err.println(url);
            e.printStackTrace();
        }
    }

    private void downloadCoverFromLinkOf(DownloadTask task) {
        String url = task.link;
        if (url == null) {
            publish("No cover found");
            return;
        }
        try {
            saveCover(task, new URL(url));
        } catch (MalformedURLException e) {
            System.err.println(url);
            e.printStackTrace();
        }
    }

    private boolean saveCover(DownloadTask task, URL url) {
        if (url == null) {
            return false;
        }
        File file = new File(task.source.getPreferredCoverLocation("jpg"));
        try {
            publish("Downloading image to JVM");
            BufferedImage bi = ImageIO.read(url);
            publish("Saving image");
            synchronized (this) {
                ImageIO.write(bi, "jpg", file);
            }
        } catch (Exception e) {
            System.err.println(url + "->" + file.getAbsolutePath());
            e.printStackTrace();
            publish("Error saving image from url: " + url);
            return false;
        }
        task.source.setImage(file.getAbsolutePath());
        if (task.source.hasLyrics()) {
            task.source.getLyrics().setTag("Cover", file.getName());
        }
        return true;
    }

    private String retrieveCoverURL(DownloadTask task) {
        try {
            URL yahoo = new URL(ConfigLoader.readString("grac0") + task.source.getArtist().toLowerCase().replaceAll(" ", "+") + ConfigLoader.readString("grac1"));
            publish("Searching album-art for: " + task.source.getArtist() + " @ " + yahoo);
            String searchpage = URLLoadClass.ReadURLString(yahoo);
            ArrayList<String> firsturls = URLLoadClass.RetrieveStringsBetweenMarks(searchpage, ConfigLoader.readString("grac2"), ConfigLoader.readString("grac3"));
            for (int i = 0; i < firsturls.size(); i++) {
                publish("checking " + (i + 1) + " albumpage for album-art");
                URL albumurl = new URL(ConfigLoader.readString("grac4") + firsturls.get(i));
                String albumpage = URLLoadClass.ReadURLString(albumurl);
                ArrayList<String> coverurls = URLLoadClass.RetrieveStringsBetweenMarks(albumpage, ConfigLoader.readString("grac5"), ConfigLoader.readString("grac6"));
                if (coverurls.size() >= 1) {
                    String coverurl = coverurls.get(0);
                    if (coverurl.indexOf(ConfigLoader.readString("grac7")) < 0 && coverurl.toString().indexOf(ConfigLoader.readString("grac8")) > 0) return ConfigLoader.readString("grac9") + coverurl;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean downloadBackgroundFromLinkOf(DownloadTask task) {
        String url = task.link;
        if (url == null) {
            publish("No cover found");
            return false;
        }
        try {
            return saveCover(task, new URL(url));
        } catch (MalformedURLException e) {
            System.err.println(url);
            e.printStackTrace();
        }
        return false;
    }

    private boolean downloadBackgroundOf(DownloadTask task) {
        String url = retrieveBackgroundURL(task);
        if (url == null) {
            publish("No cover found");
            return false;
        }
        try {
            return saveBackground(task, new URL(url));
        } catch (MalformedURLException e) {
            System.err.println(url);
            e.printStackTrace();
        }
        return false;
    }

    private String retrieveBackgroundURL(DownloadTask task) {
        try {
            String discogs = ConfigLoader.readString("disc0");
            discogs += StrCompare.removeNonLettersKeepSpaces(task.source.getArtist()).replaceAll(" ", "%20");
            discogs += ConfigLoader.readString("disc1");
            discogs += DownloadManager.getAPIKey();
            System.out.println("looking for background @ " + discogs);
            String discogsapi = URLLoadClass.ReadGZIPURLString(new URL(discogs));
            ArrayList<String> images = URLLoadClass.RetrieveStringsBetweenMarks(discogsapi, ConfigLoader.readString("disc2"), ConfigLoader.readString("disc3"));
            System.out.println("" + images.size() + " images found.");
            for (int i = 0; i < images.size(); i++) {
                if (images.get(i).contains(ConfigLoader.readString("disc4"))) {
                    return URLLoadClass.RetrieveStringsBetweenMarks(images.get(i), ConfigLoader.readString("disc5"), ConfigLoader.readString("disc6")).get(0);
                }
            }
            for (int i = 0; i < images.size(); i++) {
                try {
                    return URLLoadClass.RetrieveStringsBetweenMarks(images.get(i), ConfigLoader.readString("disc5"), ConfigLoader.readString("disc6")).get(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean saveBackground(DownloadTask task, URL url) {
        if (url == null) {
            return false;
        }
        File file = new File(task.source.getPreferredBackgroundLocation("jpg"));
        try {
            publish("Downloading Background to JVM");
            BufferedImage bi = ImageIO.read(url);
            publish("Saving Background");
            synchronized (this) {
                ImageIO.write(bi, "jpg", file);
            }
        } catch (Exception e) {
            System.err.println(url + "->" + file.getAbsolutePath());
            e.printStackTrace();
            publish("Error saving Background from url: " + url);
            return false;
        }
        task.source.setBackground(file.getAbsolutePath());
        if (task.source.hasLyrics()) {
            task.source.getLyrics().setTag("Background", file.getName());
        }
        return true;
    }
}

class SaveLocation {

    DownloadTask dt;

    void setDownloadTask(DownloadTask t) {
        dt = t;
    }
}
