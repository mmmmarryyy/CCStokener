package net.jomper.cm.fetch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import net.jomper.cm.model.CMArtist;
import net.jomper.cm.model.CMTag;
import net.jomper.cm.model.CMusic;
import net.jomper.cm.model.CMusicTag;
import net.jomper.cm.util.CMConstants;
import net.jomper.cm.util.CMLog;
import net.jomper.cm.util.CMUtil;
import net.jomper.cm.util.FetchUtil;

public class GMusicHandler {

    @SuppressWarnings("unchecked")
    public void handle(Map<String, Object> data, String urlPath) {
        System.out.println("started:" + urlPath);
        try {
            URL url = new URL(urlPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
            String line = null;
            List<CMusic> musics = (List<CMusic>) data.get(CMConstants.MUSIC);
            List<CMArtist> artists = (List<CMArtist>) data.get(CMConstants.ARTIST);
            List<CMusicTag> musicTags = (List<CMusicTag>) data.get(CMConstants.MUSIC_TAG);
            CMTag currentTag = (CMTag) data.get(CMConstants.CURRENT_TAG);
            CMusic currentMusic = null;
            while ((line = reader.readLine()) != null) {
                if (currentTag.getMusicQuantity() == 0) {
                    int musicQuantity = FetchUtil.getTagMusicQuantity(line);
                    if (musicQuantity != -1) {
                        currentTag.setMusicQuantity(musicQuantity);
                    }
                }
                CMusic music = null;
                try {
                    music = FetchUtil.getCMusic(line);
                } catch (Exception e) {
                    CMLog.getLogger(this).severe("getCMusic error:" + line);
                }
                if (music != null) {
                    currentMusic = music;
                    musics.add(music);
                    CMusicTag musicTag = new CMusicTag();
                    musicTag.setMusicCode(music.getCode());
                    musicTag.setTagName(currentTag.getName());
                    musicTags.add(musicTag);
                }
                if (currentMusic != null) {
                    CMArtist artist = null;
                    try {
                        artist = FetchUtil.getCMArtist(line);
                    } catch (Exception e) {
                        CMLog.getLogger(this).severe("getCMArtist error:" + line);
                    }
                    if (artist != null) {
                        if (!CMUtil.artistIsExist(artists, artist.getCode())) {
                            artists.add(artist);
                        }
                        currentMusic.setArtistCode(artist.getCode());
                        currentMusic = null;
                    }
                }
            }
        } catch (MalformedURLException e) {
            CMLog.getLogger(this).severe("GMusicHandler malformed url error:" + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            CMLog.getLogger(this).severe("GMusicHandler io error:" + e.getMessage());
            e.printStackTrace();
        } finally {
        }
        try {
            synchronized (this) {
                wait(1000L);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("finished:" + urlPath);
    }
}
