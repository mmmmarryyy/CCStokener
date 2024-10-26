package Common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Utils {

    /**
	 * This method trims a Double to have only <code>numOfDigits</code> digits after the
	 * decimal point.
	 * 
	 * @param num
	 * @param numOfDigits - Number of digits to leave after the decimal point.
	 * @param round - If true we round the trimmed numbers.
	 * @return Double with <code>numOfDigits</code> digits after the decimal point.
	 */
    public static double trim(double num, int numOfDigits, boolean round) {
        int accuracy = (int) Math.pow(10, numOfDigits);
        num = num * accuracy;
        num = round ? Math.round(num) : Math.floor(num);
        num = num / accuracy;
        return num;
    }

    /**
	 * This method checks if the two doubles are equals with accuracy of 0.00001.
	 * 
	 * @param a - first double
	 * @param b - second double
	 * @return True if the two doubles are equals with accuracy of 0.00001.
	 */
    public static boolean doubleEquals(double a, double b) {
        return (a > b - 0.00001) && (a < b + 0.00001);
    }

    /**
	 * This method calculates the trimmed mean of the list's elements.
	 * 
	 * @param list - The elements
	 * @param alpha - The percent of elements to trim from the both ends of the list
	 * @return The trimmed mean
	 */
    public static double calcTrimmedMean(List<Integer> list, double alpha) {
        if (list.size() == 0) {
            return 0;
        } else if (list.size() == 1) {
            return list.get(0);
        }
        Collections.sort(list);
        int elementsToCut = (int) Math.floor(list.size() * alpha);
        for (int i = 0; i < elementsToCut; i++) {
            list.remove(0);
            list.remove(list.size() - 1);
        }
        int sum = 0;
        for (int element : list) {
            sum += element;
        }
        return (double) sum / (double) list.size();
    }

    /**
	 * This method calculates the trimmed mean of a copy of the list's elements.
	 * 
	 * @param list - The elements
	 * @param alpha - The percent of elements to trim from the both ends of the list
	 * @return The trimmed mean
	 */
    public static double calcTrimmedMeanOnCopy(List<Integer> list, double alpha) {
        List<Integer> listCopy = new ArrayList<Integer>(list.size());
        for (Integer value : list) {
            listCopy.add(value);
        }
        return calcTrimmedMean(listCopy, alpha);
    }

    /**
	 * This method calculates the trimmed mean of the list's double elements.
	 * 
	 * @param list - The double elements
	 * @param alpha - The percent of elements to trim from the both ends of the list
	 * @return The trimmed mean
	 */
    public static double calcTrimmedDoubleMean(List<Double> list, double alpha) {
        if (list.size() == 0) {
            return 0;
        } else if (list.size() == 1) {
            return list.get(0);
        }
        Collections.sort(list);
        int elementsToCut = (int) Math.floor(list.size() * alpha);
        for (int i = 0; i < elementsToCut; i++) {
            list.remove(0);
            list.remove(list.size() - 1);
        }
        double sum = 0;
        for (Double element : list) {
            sum += element;
        }
        return (double) sum / (double) list.size();
    }

    /**
	 * This method reads properties files.
	 * 
	 * @param filePath - The file path holding the properties file.
	 * @return The properties 
	 */
    public static Properties readPropertiesFile(String filePath) throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        FileInputStream inStream = new FileInputStream(filePath);
        properties.load(inStream);
        inStream.close();
        return properties;
    }

    /**
	 * This method writes properties to a file.
	 * 
	 * @param properties - The properties to save.
	 * @param filePath - The location you want to save.
	 * @param comments - Comments about the propery file.
	 */
    public static void writePropertiesFile(Properties properties, String filePath, String comments) throws FileNotFoundException, IOException {
        FileOutputStream outStream = new FileOutputStream(filePath);
        properties.store(outStream, comments);
        outStream.close();
    }

    /**
	 * This method creates a dots separated string from <code>strs</code>.
	 * 
	 * @param strs - The strings to append with dots between them.
	 * @return The string: <code>strs[0].strs[1].strs[2]....</code>
	 */
    public static String createDotSeparatedString(String... strs) {
        boolean firstIter = true;
        String result = null;
        for (String str : strs) {
            if (firstIter) {
                result = str;
                firstIter = false;
            } else {
                result += Constants.DOT + str;
            }
        }
        return result;
    }

    /**
	 * This method check whether a property from <code>properties</code> exist.
	 * 
	 * @param properties - the properties class
	 * @param key - the key of the property 
	 * @return True if property exist
	 */
    public static boolean isPropertyExist(Properties properties, String key) {
        return properties.getProperty(key) != null;
    }

    /**
	 * This method returns a property from <code>properties</code> as integer.
	 * 
	 * @param properties - The properties class.
	 * @param key - The key of the property.
	 * @return The value as integer.
	 */
    public static int getPropertyAsInteger(Properties properties, String key) {
        String value = properties.getProperty(key);
        return Integer.parseInt(value);
    }

    /**
	 * This method returns a property from <code>properties</code> as double.
	 * 
	 * @param properties - The properties class.
	 * @param key - The key of the property.
	 * @return The value as double.
	 */
    public static double getPropertyAsDouble(Properties properties, String key) {
        String value = properties.getProperty(key);
        return Double.parseDouble(value);
    }

    /**
	 * This method extracts the extension of the file from a given filePath.
	 * 
	 * @param filePath - path of a file.
	 * @return - The extension of the file.
	 */
    public static String extractFileExtension(String filePath) {
        int beginIndex = filePath.lastIndexOf('.');
        return filePath.substring(beginIndex + 1);
    }

    /**
	 * This method returns the current date in formatted form.
	 * For example: 01.03.11_15-32-27
	 * 
	 * @return String with the current date in formatted form.
	 */
    public static String getCurrentTimeString() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy_HH-mm-ss");
        return formatter.format(date);
    }

    /**
	 * This method extrects the date (formated as <code>getCurrentTimeString()</code> output) from the pattern
	 * inside str. </br>For example: For the input "log_print_07.03.11_20-24-24.txt" we get Date instant
	 * with the date 07.03.11 with the time 20:24:24 
	 * 
	 * @param str - The string holding the pattern
	 * @return The date represanted by the pattern OR null if pattern doesn't match.
	 * @see Utils.getCurrentTimeString()
	 */
    public static Date extrectDateFromPattern(String str) {
        Pattern p = Pattern.compile("\\d\\d\\.\\d\\d\\.\\d\\d_\\d\\d-\\d\\d-\\d\\d");
        Matcher m = p.matcher(str);
        if (!m.find()) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy_HH-mm-ss");
        Date date = null;
        try {
            date = formatter.parse(m.group());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
	 * This method serialize a serializable object.
	 * 
	 * @param filePath - The output file path.
	 * @param object - The serializable object.
	 */
    public static void serializeObject(String filePath, Serializable object) {
        FileOutputStream fileOut = null;
        ObjectOutputStream out = null;
        try {
            fileOut = new FileOutputStream(filePath);
            out = new ObjectOutputStream(fileOut);
            out.writeObject(object);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                if (fileOut != null && out != null) {
                    out.close();
                    fileOut.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
	 * This method deserialize a file holding a serialized object.
	 * 
	 * @param filePath - The file path of the serialized object.
	 * @return The serializable object.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public static Object deserializeObject(String filePath) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(filePath);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        Object object = in.readObject();
        in.close();
        fileIn.close();
        return object;
    }

    /**
	 * This method returns the file path of the file with that latest date signature (formated as
	 * <code>getCurrentTimeString()</code> output).
	 * 
	 * @param path - The files library path.
	 * @return The latest file path OR null if none exist.
	 * @see Utils.getCurrentTimeString()
	 */
    public static String getLatestFile(String path) {
        File latestFile = null;
        Date latestDate = null;
        for (File file : new File(path).listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            if (latestDate == null) {
                latestDate = extrectDateFromPattern(file.getPath());
                latestFile = file;
            } else {
                Date tempDate = extrectDateFromPattern(file.getPath());
                if (tempDate.getTime() - latestDate.getTime() > 0) {
                    latestFile = file;
                    latestDate = tempDate;
                }
            }
        }
        return latestFile == null ? null : latestFile.getAbsolutePath();
    }

    /**
	 * This method downloads a file from a given URL.
	 * 
	 * @param url - The URL of the wanted file.
	 * @param outputFilePath - The path of the downloaded file.
	 * @throws IOException
	 */
    public static void getFileFromUrl(String url, String outputFilePath) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
        byte data[] = new byte[1024];
        int count = in.read(data, 0, 1024);
        while (count > 0) {
            bout.write(data, 0, count);
            count = in.read(data, 0, 1024);
        }
        bout.close();
        in.close();
    }

    /**
	 * This method extract GZip files.
	 *  
	 * @param infile - The GZip file.
	 * @param deleteGzipfileOnSuccess - If true we delete the GZip after the extraction.
	 * @return The extracted file from the GZip.
	 * @throws IOException
	 */
    public static File unGzip(File infile, boolean deleteGzipfileOnSuccess) throws IOException {
        GZIPInputStream gin = new GZIPInputStream(new FileInputStream(infile));
        File outFile = new File(infile.getParent(), infile.getName().replaceAll("\\.gz$", ""));
        FileOutputStream fos = new FileOutputStream(outFile);
        byte[] buf = new byte[100000];
        int len;
        while ((len = gin.read(buf)) > 0) fos.write(buf, 0, len);
        gin.close();
        fos.close();
        if (deleteGzipfileOnSuccess) infile.delete();
        return outFile;
    }
}
