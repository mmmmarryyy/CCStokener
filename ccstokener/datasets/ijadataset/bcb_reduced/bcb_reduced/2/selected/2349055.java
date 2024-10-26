package com.google.api.adwords.lib.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A utility class for retrieving image datum.
 *
 * @author api.arogal@gmail.com (Adam Rogal)
 */
public final class ImageUtils {

    /**
   * {@code ImageUtils} is meant to be used statically.
   */
    private ImageUtils() {
    }

    /**
   * Gets the image data {@code byte[]} located in {@code fileName}.
   *
   * @param fileName the image file to load
   * @return the image data {@code byte[]} located in {@code fileName}
   * @throws IOException if the image could not be read
   */
    public static byte[] getImageDataFromFile(String fileName) throws IOException {
        return getByteArrayFromStream(new BufferedInputStream(new FileInputStream(new File(fileName))));
    }

    /**
   * Gets the image data {@code byte[]} located at {@code url} or
   * {@code null} if the image could not be loaded.
   *
   * @param url the image URL to load
   * @return the image data {@code byte[]} located at {@code url}
   * @throws IOException if the image could not be read from the URL
   */
    public static byte[] getImageDataFromUrl(String url) throws IOException {
        return getByteArrayFromStream(new BufferedInputStream(new URL(url).openStream()));
    }

    /**
   * Gets the byte array from the input stream containing the full data from
   * that stream.
   *
   * @param inputStream the {@code InputStream} to get the byte array from
   * @return a byte array containing all data from the input stream
   * @throws IOException if the stream cannot be read
   */
    private static byte[] getByteArrayFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            int i = 0;
            while ((i = inputStream.read()) != -1) {
                outputStream.write(i);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw e;
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }
        return outputStream.toByteArray();
    }
}
