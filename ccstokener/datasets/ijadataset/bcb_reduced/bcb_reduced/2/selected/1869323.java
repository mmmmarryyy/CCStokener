package com.foobnix.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import com.foobnix.engine.FoobnixApplication;
import com.foobnix.exception.VKSongNotFoundException;
import com.foobnix.exception.VkErrorException;
import com.foobnix.model.FModel;
import com.foobnix.model.FModel.DOWNLOAD_STATUS;
import com.foobnix.model.FModelBuilder;
import com.foobnix.model.VkAudio;
import com.foobnix.util.pref.Pref;
import com.foobnix.util.pref.Prefs;

public class DownloadManager {

    public static String getBaseDownloadFolder(Context context) {
        File dir = new File(VersionHelper.getDownloadTo(context));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getPath();
    }

    public static void downloadFModel(FoobnixApplication app, FModel item) throws VKSongNotFoundException, VkErrorException {
        item.setStatus(FModel.DOWNLOAD_STATUS.ACTIVE);
        VkAudio mostRelevantSong = app.getIntegrationsQueryManager().getVkAdapter().getMostRelevantSong(item.getText());
        if (mostRelevantSong != null) {
            item.setPath(mostRelevantSong.getUrl());
        } else {
            item.setStatus(DOWNLOAD_STATUS.FAIL);
            return;
        }
        download(app, item);
    }

    public static void download(Context context, FModel item) {
        try {
            item.setStatus(FModel.DOWNLOAD_STATUS.ACTIVE);
            downloadProccess(context, item);
        } catch (IOException e) {
            item.setStatus(DOWNLOAD_STATUS.FAIL);
        }
    }

    public static String getFModelDownloadFolder(Context context, FModel item) {
        if (Pref.getInt(context, Prefs.DOWNLOAD_MODE) == Prefs.DOWNLOAD_MODE_SIMPLE) {
            return getBaseDownloadFolder(context);
        } else {
            String path = getBaseDownloadFolder(context) + "/";
            if (StringUtils.isNotEmpty(item.getTag())) {
                path += item.getTag() + "/";
            }
            if (StringUtils.isNotEmpty(item.getArtist())) {
                path += item.getArtist() + "/";
            }
            if (StringUtils.isNotEmpty(item.getAlbum())) {
                path += item.getAlbum() + "/";
            }
            File folder = new File(path);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            return path;
        }
    }

    public static String getFMoldelDownloadFile(Context context, FModel item) {
        String forlder = getFModelDownloadFolder(context, item);
        String text = FilenameUtils.normalizeNoEndSeparator(item.getText());
        String name = String.format("%s - %s.mp3", ((FModelBuilder) item).getNomilizedTrackNum(), text);
        return new File(forlder, name).getPath();
    }

    public static void downloadProccess(Context context, FModel item) throws IOException {
        int remote = SongUtil.getRemoteSize(item.getPath());
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double available = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
        if (remote > available) {
            item.setStatus(FModel.DOWNLOAD_STATUS.FAIL);
            return;
        }
        item.setStatus(FModel.DOWNLOAD_STATUS.ACTIVE);
        item.setDownloadTo(getFMoldelDownloadFile(context, item));
        LOG.d("begin download ", item.getDownloadTo(), item.getText(), item.getPath());
        if (new File(item.getDownloadTo()).exists()) {
            LOG.d("FModel exist", item.getDownloadTo());
            item.setStatus(FModel.DOWNLOAD_STATUS.EXIST);
            return;
        }
        URL url = new URL(item.getPath());
        HttpURLConnection connect = (HttpURLConnection) url.openConnection();
        connect.setRequestMethod("GET");
        connect.setDoOutput(true);
        connect.connect();
        FileOutputStream toStream = new FileOutputStream(new File(item.getDownloadTo()));
        InputStream fromStream = connect.getInputStream();
        if (fromStream == null) {
            LOG.d("Null from stream");
            return;
        }
        byte[] buffer = new byte[1024];
        int lenght = 0;
        int size = connect.getContentLength();
        int current = 0;
        while ((lenght = fromStream.read(buffer)) > 0) {
            toStream.write(buffer, 0, lenght);
            current += lenght;
            item.setPercent(current * 100 / size);
        }
        item.setStatus(FModel.DOWNLOAD_STATUS.DONE);
        item.setPercent(100);
        toStream.close();
        fromStream.close();
        LOG.d("end download ", item.getText(), item.getPath(), item.getDownloadTo());
        return;
    }
}
