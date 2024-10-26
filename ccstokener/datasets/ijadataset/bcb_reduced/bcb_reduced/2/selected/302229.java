package src.projects.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import src.lib.objects.Triplet;
import src.lib.objects.Tuple;

/**
 * utility functions.
 * @author alirezak
 * @version 1.0
 * 
 */
public class Utils {

    private Utils() {
    }

    public static final Comparator<Tuple<Integer, ArrayList<String>>> ORDER_TUPLE_ARRAY_STRING_INTEGER = new Comparator<Tuple<Integer, ArrayList<String>>>() {

        public int compare(Tuple<Integer, ArrayList<String>> e1, Tuple<Integer, ArrayList<String>> e2) {
            return e2.get_first() - e1.get_first();
        }
    };

    public static final Comparator<Tuple<Integer, ArrayList<Tuple<String, String>>>> ORDER_TUPLE_ARRAY_TUPLE_STRING_INTEGER = new Comparator<Tuple<Integer, ArrayList<Tuple<String, String>>>>() {

        public int compare(Tuple<Integer, ArrayList<Tuple<String, String>>> e1, Tuple<Integer, ArrayList<Tuple<String, String>>> e2) {
            return e2.get_first() - e1.get_first();
        }
    };

    public static final Comparator<Tuple<Integer, String>> ORDER_TUPLE_INTEGER_STRING = new Comparator<Tuple<Integer, String>>() {

        public int compare(Tuple<Integer, String> e1, Tuple<Integer, String> e2) {
            return e2.get_first() - e1.get_first();
        }
    };

    public static final Comparator<Tuple<Double, String>> ORDER_TUPLE_DOUBLE_STRING = new Comparator<Tuple<Double, String>>() {

        public int compare(Tuple<Double, String> e1, Tuple<Double, String> e2) {
            if (e2.get_first() < e1.get_first()) {
                return 1;
            } else if (e2.get_first() > e1.get_first()) {
                return -1;
            }
            return 0;
        }
    };

    public static final Comparator<Tuple<String, Integer>> ORDER_TUPLE_STRING_INTEGER = new Comparator<Tuple<String, Integer>>() {

        public int compare(Tuple<String, Integer> e1, Tuple<String, Integer> e2) {
            return e2.get_second() - e1.get_second();
        }
    };

    public static final Comparator<Tuple<Double, String>> ORDER_TUPLE_DOUBLE_STRING_DESCENDING = new Comparator<Tuple<Double, String>>() {

        public int compare(Tuple<Double, String> e1, Tuple<Double, String> e2) {
            if (e1.get_first() < e2.get_first()) {
                return 1;
            } else if (e1.get_first() > e2.get_first()) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    public static final Comparator<Tuple<Double, String>> ORDER_TUPLE_DOUBLE_STRING_ASCENDING = new Comparator<Tuple<Double, String>>() {

        public int compare(Tuple<Double, String> e1, Tuple<Double, String> e2) {
            if (e1.get_first() < e2.get_first()) {
                return -1;
            } else if (e1.get_first() > e2.get_first()) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    public static final Comparator<Tuple<Integer, Integer>> ORDER_TUPLE_INTEGER_INTEGER = new Comparator<Tuple<Integer, Integer>>() {

        public int compare(Tuple<Integer, Integer> e1, Tuple<Integer, Integer> e2) {
            return e1.get_first() - e2.get_first();
        }
    };

    public static final Comparator<Tuple<Integer, Integer>> ORDER_TUPLE_INTEGER_INTEGER_DEC = new Comparator<Tuple<Integer, Integer>>() {

        public int compare(Tuple<Integer, Integer> e1, Tuple<Integer, Integer> e2) {
            return e2.get_second() - e1.get_second();
        }
    };

    public static final Comparator<Triplet<String, String, Integer>> ORDER_TRIPLET_STRING_STRING_INTEGER = new Comparator<Triplet<String, String, Integer>>() {

        public int compare(Triplet<String, String, Integer> e1, Triplet<String, String, Integer> e2) {
            return e2.get_second().compareTo(e1.get_second());
        }
    };

    public static final Comparator<Tuple<String, Float>> ORDER_TUPLE_STRING_FLOAT = new Comparator<Tuple<String, Float>>() {

        public int compare(Tuple<String, Float> e2, Tuple<String, Float> e1) {
            return e1.get_second().compareTo(e2.get_second());
        }
    };

    public static int max2(int a, int b) {
        return a < b ? b : a;
    }

    public static float max2(float a, float b) {
        return a < b ? b : a;
    }

    public static double max2(double a, double b) {
        return a < b ? b : a;
    }

    public static short max2(short a, short b) {
        return a < b ? b : a;
    }

    public static int max3(int a, int b, int c) {
        return max2(max2(a, b), c);
    }

    public static double max3(double a, double b, double c) {
        return max2(max2(a, b), c);
    }

    public static long min2(long a, long b) {
        return a > b ? b : a;
    }

    public static int min2(int a, int b) {
        return a > b ? b : a;
    }

    public static double min2(double a, double b) {
        return a > b ? b : a;
    }

    public static boolean fileExists(String fname) {
        if (fname == null) {
            return false;
        }
        return new File(fname).exists();
    }

    public static boolean creatDirectory(String dirPath) {
        if (dirPath == null) {
            return false;
        }
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return (dir.mkdirs());
        }
        return false;
    }

    public static boolean isDirectory(String dirPath) {
        if (dirPath == null) {
            return false;
        }
        File dir = new File(dirPath);
        return (dir.isDirectory());
    }

    public static String[] getFilesInDir(String path) {
        if (path == null) {
            return null;
        }
        if (new File(path) == null) {
            return null;
        }
        String[] r = new File(path).list();
        int c = 0;
        while (r == null && c++ < 5) {
            r = new File(path).list();
        }
        if (r == null) {
            return new String[0];
        }
        return (r);
    }

    public static boolean forceDelete(String input) {
        if (fileExists(input)) {
            if (!isDirectory(input)) {
                return new File(input).delete();
            } else {
                for (String d : getFilesInDir(input)) {
                    forceDelete(input + "/" + d);
                }
                return new File(input).delete();
            }
        }
        return false;
    }

    public static boolean deleteFile(String input) {
        if (fileExists(input)) {
            return new File(input).delete();
        }
        return false;
    }

    public static boolean renameFile(String from, String to) {
        if (fileExists(from) && !fileExists(to)) {
            return new File(from).renameTo(new File(to));
        } else {
            return false;
        }
    }

    public static String repeatString(String s, int rep) {
        String ret = "";
        for (int i = 0; i < rep; i++) {
            ret += s;
        }
        return ret;
    }

    public static String intToFixedWidthString(Integer n, int width) {
        String s = String.valueOf(n);
        return repeatString(" ", width - s.length()) + s;
    }

    public static String stringArrayToString(String[] array, String sep) {
        String ret = "";
        for (String s : array) {
            if (ret.length() == 0) {
                ret = s;
            } else {
                ret += sep + s;
            }
        }
        return ret;
    }

    public static String stringArrayToString(ArrayList<String> array, String sep) {
        String ret = "";
        if (array == null) {
            return ret;
        }
        for (String s : array) {
            if (ret.length() == 0) {
                ret = s;
            } else {
                ret += sep + s;
            }
        }
        return ret;
    }

    public static String stringArrayToString(HashSet<String> array, String sep) {
        String ret = "";
        if (array == null) {
            return ret;
        }
        for (String s : array) {
            if (ret.length() == 0) {
                ret = s;
            } else {
                ret += sep + s;
            }
        }
        return ret;
    }

    /**
	 * returns s1 - s2
	 * @param s1
	 * @param s2
	 * @return
	 */
    public static HashSet<String> diffHashSetString(HashSet<String> s1, HashSet<String> s2) {
        HashSet<String> res = new HashSet<String>();
        for (String s : s1) {
            if (!s2.contains(s)) {
                res.add(s);
            }
        }
        return res;
    }

    public static float maxInArray(float[] a) {
        float m = a[0];
        for (float s : a) {
            m = max2(m, s);
        }
        return m;
    }

    public static float maxInArray(ArrayList<Float> a) {
        float m = a.get(0);
        for (float s : a) {
            m = max2(m, s);
        }
        return m;
    }

    public static short maxInArray(short[] a) {
        short m = a[0];
        for (short s : a) {
            m = max2(m, s);
        }
        return m;
    }

    public static Float median(ArrayList<Float> list) {
        Collections.sort(list);
        if (list.size() % 2 != 0) {
            return list.get(list.size() / 2);
        } else {
            return (list.get(list.size() / 2) + list.get(list.size() / 2 - 1)) / 2;
        }
    }

    public static float average(ArrayList<Float> list) {
        float sum = (float) 0;
        if (list.size() == 0) {
            return sum;
        }
        for (float f : list) {
            sum += f;
        }
        return sum / list.size();
    }

    public static Float median(HashSet<Float> list) {
        ArrayList<Float> aList = new ArrayList<Float>(list);
        Collections.sort(aList);
        if (list.size() % 2 != 0) {
            return aList.get(aList.size() / 2);
        } else {
            return (aList.get(aList.size() / 2) + aList.get(aList.size() / 2 - 1)) / 2;
        }
    }

    public static float average(HashSet<Float> list) {
        float sum = (float) 0;
        if (list.size() == 0) {
            return sum;
        }
        for (float f : list) {
            sum += f;
        }
        return sum / list.size();
    }

    public static String getFilePath(String file) {
        int i = file.lastIndexOf('/');
        if (i < 0) {
            return file;
        }
        return file.substring(0, i);
    }

    public static String getFileName(String file) {
        return file.substring(file.lastIndexOf('/') + 1);
    }

    /**
	 * returns choose(n, k): copied from http://stackoverflow.com/questions/1678690/what-is-a-good-way-to-implement-choose-notation-in-java
	 * @param x
	 * @param y
	 * @return
	 */
    public static double choose(int x, int y) {
        if (y < 0 || y > x) {
            return 0;
        }
        if (y > x / 2) {
            y = x - y;
        }
        double answer = 1.0;
        for (int i = 1; i <= y; i++) {
            answer *= (x + 1 - i);
            answer /= i;
        }
        return answer;
    }

    /**
	 * for reading a webpage
	 * @param url
	 * @return
	 * @throws Exception
	 */
    public static BufferedReader readURL(String url) throws Exception {
        return new BufferedReader(new InputStreamReader(new URL(url).openStream()));
    }
}
