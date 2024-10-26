package maze.commons.ee.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import maze.commons.adv_shared.generic_operation.impl.NopGenericSameTypeTransformer;
import maze.commons.generic.EmptyCheckableIterable;
import maze.commons.generic.GenericClosure;
import maze.commons.generic.Getter;
import maze.commons.generic.operation.GenericSameTypeTransformer;
import maze.commons.shared.constants.SharedConstants;
import maze.commons.shared.util.FormatUtils;
import org.apache.commons.collections.KeyValue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Normunds Mazurs
 * 
 */
public final class EeSharedUtil {

    @Deprecated
    public static final long SECOND = SharedConstants.SECOND;

    @Deprecated
    public static final long MINUTE = SharedConstants.MINUTE;

    @Deprecated
    public static final long HOUR = SharedConstants.HOUR;

    @Deprecated
    public static final long DAY = SharedConstants.DAY;

    private EeSharedUtil() {
    }

    public static RuntimeException logException(final String whileDoing, final Throwable e, final Logger logger) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        logger.log(Level.SEVERE, "While " + whileDoing + " exception", e);
        return new RuntimeException(e);
    }

    public static String randomCode() {
        return UUID.randomUUID().toString();
    }

    public static String readAndClose(final Reader reader) throws IOException {
        try {
            return IOUtils.toString(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static String readAndClose(final InputStream in) throws IOException {
        return readAndClose(new InputStreamReader(in, "UTF-8"));
    }

    public static String readResource(final Class<?> cls, final String fullName, final String defFullName) throws IOException {
        if (cls == null) {
            throw new IllegalArgumentException("Argument cls is null !");
        }
        if (StringUtils.isBlank(fullName)) {
            throw new IllegalArgumentException("Argument fullName is blank !");
        }
        final InputStream rresourceAsStream = cls.getResourceAsStream(fullName);
        final InputStream resourceAsStream;
        if (rresourceAsStream == null) {
            if (defFullName != null) {
                resourceAsStream = cls.getResourceAsStream(defFullName);
            } else {
                resourceAsStream = rresourceAsStream;
            }
            if (resourceAsStream == null) {
                throw new IOException(fullName + " cannot be loaded!");
            }
        } else {
            resourceAsStream = rresourceAsStream;
        }
        return readAndClose(resourceAsStream);
    }

    public static String readResource(final Class<?> cls, final String fullName) throws IOException {
        return readResource(cls, fullName, null);
    }

    protected static String readLineAndClose(final Reader reader) throws IOException {
        try {
            return new BufferedReader(reader).readLine();
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    protected static String readLineAndClose(final File file) throws FileNotFoundException, IOException {
        final Reader reader = new FileReader(file);
        return readLineAndClose(reader);
    }

    public static String fileReadLineAndClose(final String filePath) throws FileNotFoundException, IOException {
        final File file = new File(filePath);
        return readLineAndClose(file);
    }

    public static String readUrl(final URL urlToReadFrom) throws IOException {
        assert urlToReadFrom != null;
        final InputStream in = urlToReadFrom.openStream();
        return readAndClose(in);
    }

    public static String readUrl(final String urlToReadFrom) throws IOException {
        assert StringUtils.isNotBlank(urlToReadFrom);
        return readUrl(new URL(urlToReadFrom));
    }

    protected static String httpPost(final String strToPost, final HttpURLConnection conn) throws ProtocolException, IOException {
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        final OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        try {
            wr.write(strToPost);
            wr.flush();
            return readAndClose(conn.getInputStream());
        } finally {
            IOUtils.closeQuietly(wr);
        }
    }

    public static String createHttpPostString(final String encoding, final KeyValue... dataToPost) throws UnsupportedEncodingException {
        assert dataToPost != null;
        assert encoding != null;
        final StringBuilder strToPostBuilder = new StringBuilder();
        boolean notFirst = false;
        for (final KeyValue keyVal : dataToPost) {
            final Object key = keyVal.getKey();
            assert key != null;
            final Object value = keyVal.getValue();
            assert value != null;
            if (notFirst) {
                strToPostBuilder.append("&");
            } else {
                notFirst = true;
            }
            strToPostBuilder.append(URLEncoder.encode(key.toString(), encoding));
            strToPostBuilder.append("=");
            strToPostBuilder.append(URLEncoder.encode(value.toString(), encoding));
        }
        final String strToPost = strToPostBuilder.toString();
        return strToPost;
    }

    public static String httpPost(final URL url, final String encoding, final KeyValue... dataToPost) throws IOException {
        assert url != null;
        final String strToPost = createHttpPostString(encoding, dataToPost);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        return httpPost(strToPost, conn);
    }

    public static String httpPost(final URL url, final KeyValue... dataToPost) throws IOException {
        return httpPost(url, "UTF-8", dataToPost);
    }

    public static String httpPost(final String strUrl, final KeyValue... dataToPost) throws IOException {
        assert StringUtils.isNotBlank(strUrl);
        return httpPost(new URL(strUrl), dataToPost);
    }

    @Deprecated
    public static String formatCurrency(final int howMany, final char decimalSep) {
        return FormatUtils.formatCurrency(howMany, decimalSep);
    }

    @Deprecated
    public static String formatCurrency(final int howMany) {
        return FormatUtils.formatCurrency(howMany);
    }

    public static String minCharCountOnPre(final String str, final String forPrefix, final int minCharCount) {
        if (str == null) {
            return null;
        }
        return StringUtils.repeat(forPrefix, minCharCount - str.length()) + str;
    }

    public static int findChar(final String inStr, final char toFindChar, final int number, final int startFrom) {
        if (startFrom < 0 || number <= 0) {
            return startFrom;
        }
        final int i = inStr.indexOf(toFindChar, startFrom);
        if (i == -1) {
            return -1;
        }
        if (number <= 1) {
            return i;
        }
        return findChar(inStr, toFindChar, number - 1, i + 1);
    }

    public static int findChar(final String inStr, final char toFindChar, final int number) {
        return findChar(inStr, toFindChar, number, 0);
    }

    public static <K, V> Map<K, V> createMapWithKeySet(final Map<K, V> map, final Set<K> keySet, final V defaultValue) {
        if (keySet == null || keySet.isEmpty()) {
            return map;
        }
        if (map == null) {
            throw new IllegalArgumentException("Argument map is null !");
        }
        for (final K k : keySet) {
            map.put(k, defaultValue);
        }
        return map;
    }

    public static <K, V> Map<K, V> createHashMapWithKeySet(final Set<K> keySet, final V defaultValue) {
        return createMapWithKeySet(new HashMap<K, V>(3), keySet, defaultValue);
    }

    public static <K, V> Map<K, V> createHashMapWithKeySet(final Set<K> keySet) {
        return createHashMapWithKeySet(keySet, null);
    }

    public static String generateRandomCode(final int len, final GenericSameTypeTransformer<String> codeTransformer) {
        final String rawCode = minCharCountOnPre(StringUtils.left(Integer.toHexString(new SecureRandom().nextInt()), len).toUpperCase(), "0", len);
        return codeTransformer.transform(rawCode);
    }

    public static String[] generateRandomCodes(final int count, final int len, final GenericSameTypeTransformer<String> codeTransformer) {
        final String[] a = new String[count];
        for (int q = 0; q < count; q++) {
            a[q] = generateRandomCode(len, codeTransformer);
        }
        return a;
    }

    public static String generateRandomCode(final int len) {
        return generateRandomCode(len, NopGenericSameTypeTransformer.<String>create());
    }

    public static String[] generateRandomCodes(final int count, final int len) {
        return generateRandomCodes(count, len, NopGenericSameTypeTransformer.<String>create());
    }

    /**
	 * Copied from com.google.gwt.user.datepicker.client.CalendarUtil
	 */
    @SuppressWarnings("deprecation")
    static boolean hasTime(Date start) {
        return start.getHours() != 0 || start.getMinutes() != 0 || start.getSeconds() != 0;
    }

    /**
	 * Copied from com.google.gwt.user.datepicker.client.CalendarUtil
	 */
    @SuppressWarnings("deprecation")
    private static void resetTime(Date date) {
        long msec = date.getTime();
        msec = (msec / 1000) * 1000;
        date.setTime(msec);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
    }

    /**
	 * Copied from com.google.gwt.user.datepicker.client.CalendarUtil
	 */
    public static Date copyDate(Date date) {
        if (date == null) {
            return null;
        }
        Date newDate = new Date();
        newDate.setTime(date.getTime());
        return newDate;
    }

    /**
	 * Copied from com.google.gwt.user.datepicker.client.CalendarUtil
	 */
    public static int getDaysBetween(Date start, Date finish) {
        if (hasTime(start)) {
            start = copyDate(start);
            resetTime(start);
        }
        if (hasTime(finish)) {
            finish = copyDate(finish);
            resetTime(finish);
        }
        long aTime = start.getTime();
        long bTime = finish.getTime();
        long adjust = 60 * 60 * 1000;
        adjust = (bTime > aTime) ? adjust : -adjust;
        return (int) ((bTime - aTime + adjust) / (24 * 60 * 60 * 1000));
    }

    @SuppressWarnings("deprecation")
    public static int getDaysInMonth(final int yearMinus1900, final int zeroBasedMonthIndex) {
        return getDaysBetween(new Date(yearMinus1900, zeroBasedMonthIndex, 1), new Date(yearMinus1900, zeroBasedMonthIndex + 1, 1));
    }

    @SuppressWarnings("deprecation")
    public static long subscribeMonths(final long startTime, final long fromTime, final int monthCount) {
        assert startTime > 0;
        assert fromTime > 0;
        assert monthCount > 0;
        final Date startDate = new Date(startTime);
        final int startDay = startDate.getDate();
        final Date fromDate = new Date(fromTime);
        final int fromDay = fromDate.getDate();
        final int tillDaysInMonth = getDaysInMonth(fromDate.getYear(), fromDate.getMonth() + monthCount);
        if (startDay < fromDay) {
            throw new IllegalArgumentException("Impossible situation: 'start' day is before 'from' day !");
        }
        if (startDay - fromDay > 3) {
            throw new IllegalArgumentException("Impossible situation: 'from' day is more than 3 days before 'start' day !");
        }
        if (Math.abs(startDate.getHours() - fromDate.getHours()) > 1) {
            throw new IllegalArgumentException("Impossible situation: between 'start' hour of day and 'from' hour of day is more than one hour !");
        }
        final int resultingDay;
        if (startDay > tillDaysInMonth) {
            resultingDay = tillDaysInMonth;
        } else {
            resultingDay = startDay;
        }
        return new Date(fromDate.getYear(), fromDate.getMonth() + monthCount, resultingDay, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds()).getTime();
    }

    public static boolean isValidImei(final String imei) {
        if (StringUtils.isBlank(imei) || imei.trim().length() != 15) {
            return false;
        }
        final int[][] sumTable = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 } };
        int sum = 0, flip = 0;
        for (int i = imei.length() - 1; i >= 0; i--) {
            sum += sumTable[flip++ & 0x1][Character.digit(imei.charAt(i), 10)];
        }
        return sum % 10 == 0;
    }

    public static boolean isValidSimNumber(final String simNumber) {
        if (StringUtils.isBlank(simNumber) || simNumber.length() < 8) {
            return false;
        }
        final int numberStartsIndex = simNumber.startsWith("+") ? 1 : 0;
        return StringUtils.isNumeric(simNumber.substring(numberStartsIndex));
    }

    public static InputStream tryToGetResourceAsStream(final String resourceName, final Class<?> cls) {
        if (StringUtils.isBlank(resourceName)) {
            throw new IllegalArgumentException("Argument resourceName is blank!");
        }
        if (cls == null) {
            throw new IllegalArgumentException("Argument class (cls) is null!");
        }
        return cls.getResourceAsStream(resourceName);
    }

    public static InputStream findResourceAsStream(final EmptyCheckableIterable<Getter<String>> resourceNameVariantsIterable, final Class<?> cls, final GenericClosure<Getter<String>> propNameCallback) throws IOException {
        if (propNameCallback == null) {
            throw new IllegalArgumentException("Argument propNameCallback is null!");
        }
        if (resourceNameVariantsIterable == null) {
            throw new IllegalArgumentException("Argument propertiesNameVariantsIterable is null!");
        }
        if (cls == null) {
            throw new IllegalArgumentException("Argument class (cls) is null!");
        }
        if (resourceNameVariantsIterable.isEmpty()) {
            throw new IllegalArgumentException("Argument propertiesNameVariantsIterable is empty!");
        }
        for (final Getter<String> resourceNameGetter : resourceNameVariantsIterable) {
            final String resourceName = resourceNameGetter.get();
            final InputStream resourceAsStream = tryToGetResourceAsStream(resourceName, cls);
            if (resourceAsStream != null) {
                propNameCallback.execute(resourceNameGetter);
                return resourceAsStream;
            }
        }
        throw new IOException("Cannot find resource as Stream, starting with " + resourceNameVariantsIterable.iterator().next().get());
    }

    public static Properties findProperties(final EmptyCheckableIterable<Getter<String>> propertiesNameVariantsIterable, final Class<?> cls, final GenericClosure<Getter<String>> propNameCallback) throws IOException {
        if (propNameCallback == null) {
            throw new IllegalArgumentException("Argument propNameCallback is null!");
        }
        if (propertiesNameVariantsIterable == null) {
            throw new IllegalArgumentException("Argument propertiesNameVariantsIterable is null!");
        }
        if (cls == null) {
            throw new IllegalArgumentException("Argument class (cls) is null!");
        }
        final InputStream propertiesAsStream = findResourceAsStream(propertiesNameVariantsIterable, cls, propNameCallback);
        assert propertiesAsStream != null;
        try {
            final Properties properties = new Properties();
            properties.load(propertiesAsStream);
            return properties;
        } finally {
            propertiesAsStream.close();
        }
    }

    public static String replaceStringAt(final String str, final String toReplace, final String replacement, final int indexAt) {
        return StringUtils.left(str, indexAt) + replacement + StringUtils.right(str, str.length() - toReplace.length() - indexAt);
    }

    public static String getPropertyAsIs(final Properties properties, final String propName) {
        if (StringUtils.isBlank(propName)) {
            throw new IllegalArgumentException("Argument propName is blank!");
        }
        if (properties == null) {
            throw new IllegalArgumentException("Argument properties is null!");
        }
        return properties.getProperty(propName);
    }

    public static String getProperty(final Properties properties, final String propName) {
        if (StringUtils.isBlank(propName)) {
            throw new IllegalArgumentException("Argument propName is blank!");
        }
        if (properties == null) {
            throw new IllegalArgumentException("Argument properties is null!");
        }
        final String property = properties.getProperty(propName);
        return StringUtils.isBlank(property) ? property : StringUtils.trim(property);
    }
}
