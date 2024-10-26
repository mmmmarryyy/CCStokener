package com.project8.main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.http.util.ByteArrayBuffer;
import com.project8.book.Book;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class BookDownloader extends AsyncTask<Void, Void, Void> {

    private ProgressDialog progress;

    private String bookFolder;

    private String addBook;

    private Context context;

    private static final String internalStorage = Environment.getExternalStorageDirectory() + "/Textbooks/";

    public BookDownloader(ProgressDialog progress, String Book, Context context) {
        this.progress = progress;
        this.addBook = Book;
        this.bookFolder = "ITextbook_" + Book;
        this.context = context;
    }

    public void onPreExecute() {
        progress.show();
    }

    public Void doInBackground(Void... unused) {
        try {
            URL url = new URL("http://www.cis.umassd.edu/~bcollins/books/" + bookFolder + "/" + bookFolder + ".zip");
            File bookZip = new File(Environment.getExternalStorageDirectory() + "/" + bookFolder + ".zip");
            long startTime = System.currentTimeMillis();
            Log.d("Book Downloader", "Download Starting");
            Log.d("Book Downloader", "Downloading: " + url);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(bookZip);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("BookDownloader", "Downloaded");
            unzip(Environment.getExternalStorageDirectory() + "/" + bookFolder + ".zip", internalStorage);
            bookZip.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onPostExecute(Void unused) {
        progress.dismiss();
        Global.books.add(new Book(addBook));
        ((Activity) context).finish();
        Toast toast = Toast.makeText(context, "Download Complete", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void unzip(String zipFile, String outputLocation) throws ZipException, IOException {
        int BUFFER = 2048;
        File file = new File(zipFile);
        ZipFile zip = new ZipFile(file);
        String newPath = outputLocation;
        new File(newPath).mkdir();
        Enumeration zipFileEntries = zip.entries();
        while (zipFileEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
            String currentEntry = entry.getName();
            File destFile = new File(newPath, currentEntry);
            if (!entry.isDirectory()) {
                System.out.println("Unzipping: " + destFile.getName());
                destFile.getParentFile().mkdirs();
                BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry), 8000);
                int currentByte;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(destFile);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, currentByte);
                }
                dest.flush();
                dest.close();
                is.close();
            } else {
                System.out.println("Creating Directory: " + destFile.getName());
                destFile.mkdirs();
            }
        }
    }
}
