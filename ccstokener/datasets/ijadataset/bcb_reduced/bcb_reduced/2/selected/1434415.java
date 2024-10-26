package leeon.mobile.server.bbscache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import leeon.mobile.BBSBrowser.models.BlockObject;
import leeon.mobile.BBSBrowser.models.BoardObject;

public class Utils {

    public static BoardObject findBoard(String boardName) {
        return findBoard(boardName, null);
    }

    public static BoardObject findBoard(Long bid) {
        return findBoard(null, bid);
    }

    public static BoardObject findBoard(String boardName, Long bid) {
        return findBoard(boardName, bid, CacheService.getOrCreateAll(false));
    }

    public static BoardObject findBoard(String boardName, Long bid, List<BlockObject> all) {
        for (BlockObject b : all) {
            BoardObject bd = findBoard(b.getAllBoardList(), boardName, bid);
            if (bd != null) return bd;
        }
        return null;
    }

    public static BlockObject findBoardInBlock(String boardName, Long bid) {
        for (BlockObject b : CacheService.getOrCreateAll(false)) {
            BoardObject bd = findBoard(b.getAllBoardList(), boardName, bid);
            if (bd != null) return b;
        }
        return null;
    }

    private static BoardObject findBoard(List<BoardObject> list, String boardName, Long bid) {
        for (BoardObject b : list) {
            if (b.isDir()) {
                BoardObject bd = findBoard(b.getChildBoardList(), boardName, bid);
                if (bd != null) return bd;
            } else {
                if (boardName != null && boardName.equals(b.getName())) return b; else if (bid != null && bid.toString().equals(b.getId())) return b;
            }
        }
        return null;
    }

    public static String dateToString(Date date) {
        return dateToString(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static String dateToString(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.SIMPLIFIED_CHINESE);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return sdf.format(date);
    }

    public static Date stringToDate(String date) {
        return stringToDate("yyyy-MM-dd HH:mm:ss", date, 0);
    }

    public static Date stringToDate(String format, String date) {
        return stringToDate(format, date, 0);
    }

    public static Date stringToDate(String format, String date, long def) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.SIMPLIFIED_CHINESE);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            return new Date(def);
        }
    }

    public static boolean isNight() {
        int h = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).get(Calendar.HOUR_OF_DAY);
        return (h >= 0 && h < 9);
    }

    public static final int MAX_RETRY = 3;

    public static InputStream readUrl(String url) throws IOException {
        int i = 0;
        while (true) {
            try {
                return new URL(url).openStream();
            } catch (IOException e) {
                if (i++ >= MAX_RETRY) throw e;
            }
        }
    }

    public static InputStream readUrl(String url, HttpServletResponse resp) throws IOException {
        int i = 0;
        while (true) {
            try {
                URL u = new URL(url);
                URLConnection conn = u.openConnection();
                resp.setContentType(conn.getContentType());
                resp.setCharacterEncoding(conn.getContentEncoding());
                return conn.getInputStream();
            } catch (IOException e) {
                if (i++ >= MAX_RETRY) throw e;
            }
        }
    }
}
