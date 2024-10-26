import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Utility {

    public static Date getDateOfLastSolve(String name) {
        try {
            BufferedReader in = getUserSolveStream(name);
            String line = "";
            for (int i = 0; i < 10; ++i) {
                line = in.readLine();
            }
            String[] info = line.split(" *\\| *");
            while (info.length >= 5 && !info[4].equalsIgnoreCase("AC")) {
                line = in.readLine();
                info = line.split(" *\\| *");
            }
            if (info.length < 5) {
                return (new Date(0));
            }
            in.close();
            return toDate(info[2]);
        } catch (IOException e) {
            return (new Date(0));
        }
    }

    public static Date toDate(String date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = new GregorianCalendar();
        Date ret = new Date(0);
        try {
            c.setTime(df.parse(date));
            c.add(Calendar.HOUR_OF_DAY, -6);
            ret = c.getTime();
        } catch (ParseException e) {
        }
        return ret;
    }

    public static boolean checkIfSameDay(Date d1, Date d2) {
        Calendar calc = new GregorianCalendar();
        calc.setTime(d1);
        int d1Day = calc.get(Calendar.DAY_OF_YEAR);
        int d1Year = calc.get(Calendar.YEAR);
        calc.setTime(d2);
        int d2Day = calc.get(Calendar.DAY_OF_YEAR);
        int d2Year = calc.get(Calendar.YEAR);
        return (d1Day == d2Day && d1Year == d2Year);
    }

    public static boolean checkIfSameWeek(Date d1, Date d2) {
        Calendar calc = new GregorianCalendar();
        calc.setTime(d1);
        int d1Week = calc.get(Calendar.WEEK_OF_YEAR);
        int d1Year = calc.get(Calendar.YEAR);
        calc.setTime(d2);
        int d2Week = calc.get(Calendar.WEEK_OF_YEAR);
        int d2Year = calc.get(Calendar.YEAR);
        return (d1Week == d2Week && d1Year == d2Year);
    }

    public static boolean checkIfSameHour(Date d1, Date d2) {
        Calendar calc = new GregorianCalendar();
        calc.setTime(d1);
        int d1Hour = calc.get(Calendar.HOUR_OF_DAY);
        int d1Day = calc.get(Calendar.DAY_OF_YEAR);
        int d1Year = calc.get(Calendar.YEAR);
        calc.setTime(d2);
        int d2Hour = calc.get(Calendar.HOUR_OF_DAY);
        int d2Day = calc.get(Calendar.DAY_OF_YEAR);
        int d2Year = calc.get(Calendar.YEAR);
        return (d1Day == d2Day && d1Year == d2Year && d2Hour == d1Hour);
    }

    public static BufferedReader getUserSolveStream(String name) throws IOException {
        BufferedReader in;
        try {
            URL url = new URL("http://www.spoj.pl/status/" + name.toLowerCase() + "/signedlist/");
            in = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (MalformedURLException e) {
            in = null;
            throw e;
        }
        return in;
    }

    public static BufferedReader getUserInfoStream(String name) throws IOException {
        BufferedReader in;
        try {
            URL url = new URL("http://www.spoj.pl/users/" + name.toLowerCase() + "/");
            in = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (MalformedURLException e) {
            in = null;
            throw e;
        }
        return in;
    }

    public static String getLastSpojSolve(String user) {
        try {
            BufferedReader in = getUserSolveStream(user);
            String line = "";
            for (int i = 0; i < 10; ++i) {
                line = in.readLine();
            }
            String[] info = line.split(" *\\| *");
            while (info.length >= 5 && !info[4].equalsIgnoreCase("AC")) {
                line = in.readLine();
                info = line.split(" *\\| *");
            }
            if (info.length < 5) {
                return ("User " + user + " has no correct submissions.");
            }
            String retCode = info[4];
            String prob = info[3];
            String strDate = toDate(info[2]).toString();
            in.close();
            return (user + "'s last solved classical problem was " + prob + " on " + strDate + ".");
        } catch (IOException e) {
            return ("User " + user + " was not found on SPOJ.");
        }
    }

    public static Date getSolveDate(String userName, String prob) throws IOException {
        Date solveDate = null;
        try {
            BufferedReader in = Utility.getUserSolveStream(userName);
            String line = "";
            for (int i = 0; i < 10; ++i) {
                line = in.readLine();
            }
            String[] info = line.split(" *\\| *");
            while (info.length >= 5) {
                if (info[3].equals(prob) && info[4].equalsIgnoreCase("AC")) {
                    solveDate = toDate(info[2]);
                }
                line = in.readLine();
                info = line.split(" *\\| *");
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return solveDate;
    }

    public static String getTimeTillMidnight() {
        Calendar now = new GregorianCalendar();
        now.setTime(new Date());
        int hr = 23 - now.get(Calendar.HOUR_OF_DAY);
        int min = 59 - now.get(Calendar.MINUTE);
        int sec = 60 - now.get(Calendar.SECOND);
        String ret = "";
        System.out.println("It is " + hr + ":" + min + ":" + sec);
        if (sec >= 60) {
            sec -= 60;
            ++min;
        }
        if (min >= 60) {
            min -= 60;
            ++hr;
        }
        if (hr >= 2) {
            ret += hr + " hours";
        } else if (hr == 1) {
            ret += hr + " hour";
        }
        if (min >= 2) {
            ret += " " + min + " minutes";
        } else if (min == 1) {
            ret += " " + min + " minute";
        }
        if (sec >= 2) {
            ret += " " + sec + " seconds";
        } else if (sec == 1) {
            ret += " " + sec + " second";
        }
        return ret;
    }

    public static String getDaysTillSunday() {
        Calendar now = new GregorianCalendar();
        now.setTime(new Date());
        int day = now.get(Calendar.DAY_OF_WEEK);
        System.out.println(day);
        String ret = (7 - day) + " days";
        return ret;
    }

    public static String getTimeTillHour() {
        Calendar now = new GregorianCalendar();
        now.setTime(new Date());
        int min = 59 - now.get(Calendar.MINUTE);
        int sec = 60 - now.get(Calendar.SECOND);
        String ret = "";
        System.out.println("It is " + ":" + min + ":" + sec);
        if (sec >= 60) {
            sec -= 60;
            ++min;
        }
        if (min >= 2) {
            ret += min + " minutes";
        } else if (min == 1) {
            ret += min + " minute";
        }
        if (sec >= 2) {
            ret += " " + sec + " seconds";
        } else if (sec == 1) {
            ret += " " + sec + " second";
        }
        return ret;
    }

    public static String toGrammaticallyCorrectString(String[] users, String singularPredicate, String pluralPredicate, String... noOneSpecialCase) {
        if (users.length == 0) {
            if (noOneSpecialCase.length > 0) return noOneSpecialCase[0];
            return "No one " + singularPredicate;
        } else if (users.length == 1) {
            return users[0] + " " + singularPredicate;
        } else if (users.length == 2) {
            return users[0] + " and " + users[1] + " " + pluralPredicate;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < users.length - 1; ++i) {
            result.append(users[i]);
            result.append(", ");
        }
        result.append("and ");
        result.append(users[users.length - 1]);
        result.append(' ');
        result.append(pluralPredicate);
        return result.toString();
    }

    public static boolean isValidProblemID(String problemID) {
        return !(problemID.length() < 3 || problemID.length() > 8 || problemID.matches(".*[^A-Z0-9_].*"));
    }

    public static String toValidProblemID(String problemID) {
        return problemID.toUpperCase().replaceAll("[^A-Z0-9_]*\\z", "");
    }
}
