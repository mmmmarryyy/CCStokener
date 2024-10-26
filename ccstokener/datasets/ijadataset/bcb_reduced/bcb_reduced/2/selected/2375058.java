package com.otatop.dvdLibrary.business;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.otatop.dvdLibrary.data.Category;
import com.otatop.dvdLibrary.data.Media;

/**
 * The database load is a download of an online database of dvd's which creates seed
 * data. This data is then used during user data entry to auto populate fields 
 * based on the title or bar code.
 * 
 * @author otatop
 *
 */
@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
public class DvdDataLoader {

    private static final String dvdCsvFileName = "dvd_csv.txt";

    private static final String dvdCsvZipFileName = "dvd_csv.zip";

    private static final String dvdCsvFileUrl = "http://hometheaterinfo.com/download/dvd_csv.zip";

    public int added = 0;

    private DvdDataManager dataManager;

    public void setDataManager(DvdDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void loadDefaultData() throws DataLoadingException {
        try {
            Date startTime = new Date();
            System.out.println("Loading..." + startTime.toLocaleString());
            File dvdCsvFile = getDvdDataFileFromWeb();
            loadDataIntoDb(dvdCsvFile);
            dvdCsvFile.delete();
            Date endTime = new Date();
            Long elapsed = endTime.getTime() - startTime.getTime();
            double minutes = Math.floor(elapsed / 60000);
            double seconds = Math.round((elapsed % 60000) / 1000);
            System.out.println("done in " + minutes + " minutes " + seconds + " seconds");
        } catch (Exception e) {
            System.err.println("Load Filed:" + e.getMessage());
            throw new DataLoadingException(e.getMessage(), e);
        }
    }

    private void loadDataIntoDb(File dvdCsvFile) throws IOException {
        FileReader input = new FileReader(dvdCsvFile);
        BufferedReader bufRead = new BufferedReader(input);
        String line;
        line = bufRead.readLine();
        HashMap<String, Integer> columns = new HashMap<String, Integer>();
        int index = 0;
        for (String column : readCsvLine(line)) {
            columns.put(column, index);
            index += 1;
        }
        HashMap<String, Category> loadedCategories = new HashMap<String, Category>();
        HashMap<String, Integer> loadedMedia = new HashMap<String, Integer>();
        while ((line = bufRead.readLine()) != null) {
            String[] data = readCsvLine(line);
            String genere = data[columns.get("Genre")];
            Category category;
            if (loadedCategories.containsKey(genere)) {
                category = loadedCategories.get(genere);
            } else {
                category = new Category();
                category.setTitle(genere);
                dataManager.insertCategory(category);
                loadedCategories.put(genere, category);
                System.out.println("Created " + category);
            }
            Media media;
            String barCode = data[columns.get("UPC")];
            if (loadedMedia.containsKey(barCode)) {
                System.out.println("Duplicate found " + barCode);
                continue;
            } else {
                media = new Media();
                media.setBarcode(barCode);
                loadedMedia.put(barCode, 1);
            }
            media.setCategory(category);
            media.setRating(data[columns.get("Rating")]);
            media.setTitle(data[columns.get("DVD_Title")]);
            dataManager.insertMedia(media);
            added++;
            if (added % 20 == 0) {
                System.out.println("added:" + added + " Created:" + media);
                dataManager.batchFlushClear();
            }
        }
        bufRead.close();
    }

    private File getDvdDataFileFromWeb() throws IOException {
        System.out.println("Downloading " + dvdCsvFileUrl);
        URL url = new URL(dvdCsvFileUrl);
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        OutputStream out = new FileOutputStream(dvdCsvZipFileName);
        writeFromTo(in, out);
        System.out.println("Extracting " + dvdCsvFileName + " from " + dvdCsvZipFileName);
        File dvdZipFile = new File(dvdCsvZipFileName);
        File dvdCsvFile = new File(dvdCsvFileName);
        ZipFile zipFile = new ZipFile(dvdZipFile);
        ZipEntry zipEntry = zipFile.getEntry(dvdCsvFileName);
        FileOutputStream os = new FileOutputStream(dvdCsvFile);
        InputStream is = zipFile.getInputStream(zipEntry);
        writeFromTo(is, os);
        System.out.println("Deleting zip file");
        dvdZipFile.delete();
        System.out.println("Dvd csv file download complete");
        return dvdCsvFile;
    }

    private String[] readCsvLine(String line) {
        String currentEntry = "";
        boolean inQuotes = false;
        ArrayList<String> data = new ArrayList<String>();
        for (char ch : line.toCharArray()) {
            if (!inQuotes && ch == '"') {
                inQuotes = true;
                continue;
            }
            if (ch == '"') {
                inQuotes = false;
                continue;
            }
            if (!inQuotes && ch == ',') {
                data.add(currentEntry);
                currentEntry = "";
                continue;
            }
            currentEntry += ch;
        }
        return data.toArray(new String[data.size()]);
    }

    /**
	 * Copies from a stream to a stream
	 *
	 * @param in
	 *            the stream to copy from
	 * @param out
	 *            the stream to copy to
	 * @throws IOException
	 */
    private void writeFromTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, numRead);
        }
        in.close();
        out.close();
    }
}
