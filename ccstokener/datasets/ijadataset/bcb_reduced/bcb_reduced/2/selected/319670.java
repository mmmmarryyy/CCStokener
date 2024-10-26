package omschaub.aztrackerfind.main;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;

/**
 * @author omschaub
 *
 * 
 */
public class Downloader {

    public static void generic_getter(final String inet_address, final String url, final String dir_to_save, final String filename, final PluginInterface pluginInterface, final Display display) {
        try {
            final PluginConfig config_getter = pluginInterface.getPluginconfig();
            ResourceDownloaderFactory rdf;
            URL url_get = new URL(url);
            rdf = pluginInterface.getUtilities().getResourceDownloaderFactory();
            ResourceDownloader rd_t = rdf.create(url_get, config_getter.getPluginStringParameter("AzTrackerFind_url_user"), config_getter.getPluginStringParameter("AzTrackerFind_url_password"));
            rd_t = rdf.getRetryDownloader(rd_t, 3);
            rd_t = rdf.getTimeoutDownloader(rd_t, (config_getter.getPluginIntParameter("timeout_value") * 1000));
            rd_t = rdf.getSuffixBasedDownloader(rd_t);
            rd_t.addListener(new ResourceDownloaderListener() {

                public boolean completed(final ResourceDownloader downloader, InputStream data) {
                    boolean isAz = false;
                    int seeds = 0;
                    try {
                        FileOutputStream file = new FileOutputStream(dir_to_save + filename);
                        BufferedOutputStream out = new BufferedOutputStream(file);
                        int j;
                        j = 0;
                        while ((j = data.read()) != -1) {
                            out.write(j);
                        }
                        out.flush();
                        out.close();
                        File file_to_read = new File(dir_to_save + filename);
                        BufferedReader bir = new BufferedReader(new FileReader(file_to_read));
                        String temp_line;
                        while ((temp_line = bir.readLine()) != null) {
                            if (temp_line.startsWith("<title>Azureus")) {
                                isAz = true;
                            }
                            if (temp_line.startsWith("                <td> <a href=") || temp_line.startsWith("                  <td> <a href=")) {
                                String[] parsed = Parser.getTorrentUrl(temp_line);
                                if (!parsed[1].equals("none")) {
                                    seeds++;
                                    View.addTableElementDouble(View.bookMarkedIPs, parsed[2], url + parsed[1], 0);
                                    if (config_getter.getPluginBooleanParameter("AutoDownload")) {
                                        TorrentUtils.torrent_save(url + parsed[1], parsed[2], pluginInterface);
                                    }
                                }
                            }
                        }
                        bir.close();
                        file_to_read.delete();
                        data.close();
                        if (isAz) {
                            InetAddress inet = InetAddress.getByName(inet_address);
                            String dnsName = inet.getHostName();
                            View.addTableElementDouble(View.webPositiveIPs, (inet_address + ":" + 6969), dnsName, seeds);
                            rss_tester("http://" + inet_address + ":6969/rss_feed.xml", pluginInterface);
                            PluginConfig config_getter = pluginInterface.getPluginconfig();
                            if (seeds > 0 && config_getter.getPluginBooleanParameter("AzTrackerFind_autoinsert")) {
                                Bookmarks.addBookmark(pluginInterface, display, inet_address + ":" + 6969, dnsName, new String("" + seeds), "N", Time.getCurrentTime(config_getter.getBooleanParameter("MilitaryTime")));
                            }
                        }
                    } catch (Exception e) {
                    }
                    return (true);
                }

                public void reportPercentComplete(ResourceDownloader downloader, final int percentage) {
                }

                public void reportActivity(ResourceDownloader downloader, String activity) {
                }

                public void failed(ResourceDownloader downloader, ResourceDownloaderException e) {
                }

                public void reportAmountComplete(ResourceDownloader arg0, long arg1) {
                }
            });
            rd_t.asyncDownload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void torrent_getter(final String url, final PluginInterface pluginInterface) {
        try {
            ResourceDownloaderFactory rdf;
            URL url_get = new URL(url);
            final URL url_final;
            rdf = pluginInterface.getUtilities().getResourceDownloaderFactory();
            System.out.println("Downloader: " + url_get.getHost());
            System.out.println("Downloader: " + url_get.getFile());
            if (url_get.getFile().startsWith("/announce")) {
                url_final = new URL("http", url_get.getHost(), url_get.getPort(), "/" + url_get.getFile().substring(9, url_get.getFile().length()));
                System.out.println(url_final.getFile());
            } else {
                url_final = new URL(url);
            }
            ResourceDownloader rd_t = rdf.create(url_final);
            rd_t = rdf.getTimeoutDownloader(rd_t, 1200000);
            rd_t = rdf.getMetaRefreshDownloader(rd_t);
            rd_t.addListener(new ResourceDownloaderListener() {

                public void reportPercentComplete(ResourceDownloader downloader, int percentage) {
                    System.out.println("completed " + percentage + "%");
                }

                public void reportActivity(ResourceDownloader downloader, String activity) {
                    System.out.println(activity);
                }

                public boolean completed(ResourceDownloader downloader, InputStream data) {
                    try {
                        TorrentManager torrentManager = pluginInterface.getTorrentManager();
                        try {
                            Torrent new_torrent = torrentManager.createFromBEncodedInputStream(data);
                            DownloadManager dm = pluginInterface.getDownloadManager();
                            dm.addDownload(new_torrent);
                        } catch (TorrentException e1) {
                            e1.printStackTrace();
                        } catch (DownloadException e2) {
                            e2.printStackTrace();
                        }
                        data.close();
                    } catch (IOException e) {
                    }
                    return false;
                }

                public void failed(ResourceDownloader downloader, ResourceDownloaderException e) {
                    System.out.println(e);
                }

                public void reportAmountComplete(ResourceDownloader arg0, long arg1) {
                }
            });
            rd_t.asyncDownload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void rss_tester(final String url, final PluginInterface pluginInterface) {
        Thread t2 = new Thread() {

            public void run() {
                try {
                    ResourceDownloaderFactory rdf;
                    URL url_get = new URL(url);
                    URLConnection urlcon = url_get.openConnection();
                    String type = urlcon.getContentType();
                    if (type != null && type.startsWith("text/xml")) {
                        RSSReader.parseRSS(url, pluginInterface);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t2.start();
    }
}
