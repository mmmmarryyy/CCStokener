package be.kuleuven.peno3.mobiletoledo.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.IOUtils;
import be.kuleuven.peno3.mobiletoledo.model.CourseActivity;
import com.google.gson.Gson;

public class ScheduleClientDAO {

    public static void main(String args[]) {
        System.out.println("test");
        Calendar begin = new GregorianCalendar();
        begin.set(2010, 11, 30, 16, 00);
        Calendar end = new GregorianCalendar();
        end.set(2010, 11, 30, 18, 00);
        CourseActivity[] courseActivities = getCourseActivities(begin, end, "all", "none", "1");
        for (CourseActivity courseActivity : courseActivities) {
            System.out.println(courseActivity.getTeacher());
        }
    }

    public static CourseActivity[] getCourseActivities(Calendar begin, Calendar end, String course_id, String group, String study_programme) {
        try {
            String beginDate = toSQLString(begin);
            String endDate = toSQLString(end);
            String url = "beginDate=" + beginDate + "&endDate=" + endDate + "&group=" + group + "&course=" + course_id + "&study_programme=" + study_programme;
            url = "http://localhost:9876/ScheduleHandler/getCourseActivities?" + url;
            String json = stringOfUrl(url);
            System.out.println("json in client:" + json);
            CourseActivity[] courseActivities = new Gson().fromJson(json.toString(), CourseActivity[].class);
            return courseActivities;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        } catch (HttpException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String stringOfUrl(String addr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.out.println("test");
        URL url = new URL(addr);
        System.out.println("test2");
        IOUtils.copy(url.openStream(), output);
        return output.toString();
    }

    private static String toSQLString(Calendar cal) {
        String year = "" + cal.get(Calendar.YEAR);
        String month = "" + cal.get(Calendar.MONTH);
        String day = "" + cal.get(Calendar.DAY_OF_MONTH);
        String hour = "" + cal.get(Calendar.HOUR_OF_DAY);
        String minute = "" + cal.get(Calendar.MINUTE);
        return year + "-" + month + "-" + day + "%20" + hour + ":" + minute + ":00";
    }
}
