package com.wordpress.tshah.jalbumgetter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import org.cmc.music.common.ID3ReadException;
import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;
import com.yahoo.search.ImageSearchRequest;
import com.yahoo.search.ImageSearchResults;
import com.yahoo.search.SearchClient;
import com.yahoo.search.SearchException;

/**
 *
 * @author tshah
 */
public class Main {

    private static void goThrough(File dir, ArrayList<File> files) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                goThrough(f, files);
            } else if (f.getAbsolutePath().endsWith("mp3")) {
                files.add(f);
            }
        }
    }

    public static void main(String[] args) {
        JFileChooser askDir = new JFileChooser();
        askDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        askDir.setMultiSelectionEnabled(false);
        int returnVal = askDir.showOpenDialog(null);
        if (returnVal == JFileChooser.CANCEL_OPTION) {
            System.exit(returnVal);
        }
        File startDir = askDir.getSelectedFile();
        ArrayList<File> files = new ArrayList<File>();
        goThrough(startDir, files);
        SearchClient client = new SearchClient("VZFo5W5i");
        MyID3 singleton = new MyID3();
        for (File song : files) {
            try {
                MusicMetadataSet set = singleton.read(song);
                IMusicMetadata meta = set.getSimplified();
                String qu = song.getName();
                if (meta.getAlbum() != null) {
                    qu = meta.getAlbum();
                } else if (meta.getArtist() != null) {
                    qu = meta.getArtist();
                }
                if (qu.length() > 2) {
                    ImageSearchRequest req = new ImageSearchRequest(qu);
                    ImageSearchResults res = client.imageSearch(req);
                    if (res.getTotalResultsAvailable().doubleValue() > 1) {
                        System.out.println("Downloading " + res.listResults()[0].getUrl());
                        URL url = new URL(res.listResults()[0].getUrl());
                        URLConnection con = url.openConnection();
                        con.setConnectTimeout(10000);
                        int realSize = con.getContentLength();
                        if (realSize > 0) {
                            String mime = con.getContentType();
                            InputStream stream = con.getInputStream();
                            byte[] realData = new byte[realSize];
                            for (int i = 0; i < realSize; i++) {
                                stream.read(realData, i, 1);
                            }
                            stream.close();
                            ImageData imgData = new ImageData(realData, mime, qu, 0);
                            meta.addPicture(imgData);
                            File temp = File.createTempFile("tempsong", "mp3");
                            singleton.write(song, temp, set, meta);
                            FileChannel inChannel = new FileInputStream(temp).getChannel();
                            FileChannel outChannel = new FileOutputStream(song).getChannel();
                            try {
                                inChannel.transferTo(0, inChannel.size(), outChannel);
                            } catch (IOException e) {
                                throw e;
                            } finally {
                                if (inChannel != null) inChannel.close();
                                if (outChannel != null) outChannel.close();
                            }
                            temp.delete();
                        }
                    }
                }
            } catch (ID3ReadException e) {
            } catch (MalformedURLException e) {
            } catch (UnsupportedEncodingException e) {
            } catch (ID3WriteException e) {
            } catch (IOException e) {
            } catch (SearchException e) {
            }
        }
    }
}
