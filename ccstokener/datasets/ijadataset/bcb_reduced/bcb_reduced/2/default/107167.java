import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

public class Methods {

    /**
	 * Verifies that user's system has java 'version' or higher. Prints error
	 * message and exits program if not.
	 * 
	 * @param str
	 *            minimum version
	 */
    protected static boolean enoughJavaInside(double minVersion) {
        String java_version = System.getProperty("java.version");
        String localVersion = java_version.trim().substring(0, 3);
        return Double.valueOf(localVersion) >= minVersion;
    }

    protected static boolean isLatestVersion(double myVersion, String referenceAddress) {
        Scanner scanner = null;
        try {
            URL url = new URL(referenceAddress);
            InputStream iS = url.openStream();
            scanner = new Scanner(iS);
            String firstLine = scanner.nextLine();
            double latestVersion = Double.valueOf(firstLine.trim());
            return myVersion >= latestVersion;
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host!!!");
            return false;
        } catch (Exception e) {
            System.out.println("Can't decide latest version");
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * Prints the input string appended to a generalized error message: "ERROR! - <<input
	 * string>>"
	 * 
	 * @param message
	 *            Error message
	 */
    public static void errorMessage(String message) {
        System.out.println("ERROR! - " + message);
    }

    /**
	 * Returns true if any of the Strings in the array contain the passed token
	 * 
	 * @param args
	 *            Array of Strings to check for passed token
	 * @param token
	 *            String that is searched for in args
	 * @return true if token is contained in any of the args
	 */
    public static boolean strArrayContainsToken(ArrayList<String> args, String token) {
        Iterator<String> iter = args.iterator();
        while (iter.hasNext()) {
            if (iter.next().equalsIgnoreCase(token)) return true;
        }
        return false;
    }

    /**
	 * Returns a string representing a bearing, meaning it will have a length of
	 * three if no decimal point is present and a length of five otherwise. Ex.
	 * 45.3 => 045.3
	 * 
	 * @param brgOrCrs
	 *            The bearing or course to be formatted for proper format
	 * @return String representing a bearing in proper readable format (ex.
	 *         010.4)
	 */
    public static String readableBrgOrCrs(String brgOrCrs) {
        while (brgOrCrs.length() < (brgOrCrs.contains(".") ? 5 : 3)) brgOrCrs = "0" + brgOrCrs;
        return brgOrCrs.trim();
    }

    /**
	 * Returns a string representing a bearing, meaning it will have a length of
	 * three if no decimal point is present and a length of five otherwise. Ex.
	 * 45.3 => 045.3
	 * 
	 * @param brgOrCrs
	 *            The bearing or course to be formatted for proper format
	 * @return String representing a bearing in proper readable format (ex.
	 *         010.4)
	 */
    public static String readableBrgOrCrs(double brgOrCrs) {
        return readableBrgOrCrs(String.valueOf(brgOrCrs));
    }

    /**
	 * Returns a string representing a range or distance in yards. It will apply
	 * any unit characters appended to the string. Ex. '3.4nm' => '6800' Returns
	 * null in the event of an incompatible input string.
	 * 
	 * @param range
	 *            range or distance in yards, k yards, or nm
	 * @return String representing a range in yards
	 */
    public static String applyYorKorNM(String range) {
        range = range.toLowerCase().replace(" ", "").trim();
        if (range.endsWith("y") || range.endsWith("yds") || range.endsWith("yards")) {
            return range.substring(0, range.indexOf("y"));
        }
        if (range.endsWith("k")) {
            range = range.replace("k", "").trim();
            if (isNumber(range)) {
                double temp = Double.valueOf(range);
                return "" + temp * 1000;
            } else {
                return null;
            }
        } else if (range.endsWith("nm")) {
            range = range.replace("nm", "").trim();
            if (isNumber(range)) {
                double temp = Double.valueOf(range);
                return "" + temp * 2000;
            } else {
                return null;
            }
        } else if (range.endsWith("m")) {
            range = range.replace("m", "").trim();
            if (isNumber(range)) {
                double temp = Double.valueOf(range);
                return "" + temp * 2000;
            } else {
                return null;
            }
        } else {
            return range;
        }
    }

    /**
	 * Returns an angle, converted from a bearing
	 * 
	 * @param bearing
	 *            bearing to be converted to decimal angle
	 * @return an angle representative of the input bearing
	 */
    public static double convertFromBearingToDegrees(double bearing) {
        bearing = posifyAngle(bearing);
        if (bearing == 0 || bearing == 360) return 90; else if (bearing <= 90) return 90 - bearing; else if (bearing <= 180) return 90 + 360 - bearing; else if (bearing <= 270) return 270 - (bearing - 180); else return 270 + (180 - bearing);
    }

    /**
	 * Returns a bearing, converted from an angle
	 * 
	 * @param decimalDegree
	 *            decimal degree to be converted into a bearing
	 * @return the equivalent bearing of the input decimal degree
	 */
    public static double convertFromDegreesToBearing(double decimalDegree) {
        decimalDegree = posifyAngle(decimalDegree);
        if (decimalDegree == 90) return 0; else if (decimalDegree <= 90) return 90 - decimalDegree; else if (decimalDegree <= 180) return 360 - (decimalDegree - 90); else if (decimalDegree <= 270) return 180 + (270 - decimalDegree); else return 180 - (decimalDegree - 270);
    }

    /**
	 * Returns the reciprocal bearing of the input bearing
	 * 
	 * @param bearing
	 *            original bearing
	 * @return reciprocal bearing of input bearing
	 */
    public static double reciprocalBearing(double bearing) {
        if (bearing < 180.0) return bearing + 180.0; else return bearing - 180.0;
    }

    /**
	 * Returns the distance (in yards) traveled in time 'time' at a speed of
	 * 'speed' knots. Time is expressed in minutes.
	 * 
	 * @param speed
	 *            speed of object
	 * @param time
	 *            interval of time in which the object is traveling
	 * @return Distance traveled in time 'time' at speed 'speed'
	 */
    public static double distanceTraveled(int speed, int time) {
        double distance = speed * time / 60.0 * 2000.0;
        return distance;
    }

    /**
	 * Returns the Location of an object which has traveled on x course over y
	 * distance
	 * 
	 * @param location
	 *            current location of object
	 * @param course
	 *            objects course
	 * @param distanceOrRange
	 *            distance or range traveled by object
	 * @return location in which object will finally reside
	 */
    public static Location getDestinationLocation(Location location, double course, double distanceOrRange) {
        double d = distanceOrRange;
        double dx = Math.round(Math.cos(Math.toRadians(convertFromBearingToDegrees(course))) * d);
        double dy = Math.round(Math.sin(Math.toRadians(convertFromBearingToDegrees(course))) * d);
        double x = (location.getX() + dx);
        double y = (location.getY() + dy);
        return new Location(x, y);
    }

    /**
	 * Centers the 'view' by zeroing out ownship and adjusting all contacts accordingly
	 * 
	 * @param ownShip
	 *            Own Ship
	 * @param contacts
	 *            ArrayList of Contacts held
	 */
    public static void centerWindow(Combatant ownShip, ArrayList<Contact> contacts) {
        double dy = ownShip.location.getY();
        double dx = ownShip.location.getX();
        Iterator<Contact> iter = contacts.iterator();
        while (iter.hasNext()) {
            Combatant comb = iter.next();
            Location curLoc = comb.getLocation();
            comb.getLocation().setX(curLoc.getX() - dx);
            comb.getLocation().setY(curLoc.getY() - dy);
        }
        ownShip.location = new Location(0, 0);
    }

    /**
	 * Returns the Location of an object which has traveled x time at y speed on
	 * z course
	 * 
	 * @param location
	 *            original location of object
	 * @param course
	 *            objects course
	 * @param speed
	 *            objects speed
	 * @param time
	 *            interval of time in which object is to travel
	 * @return location in which object will finally reside
	 */
    public static Location getDestinationLocation(Location location, double course, int speed, int time) {
        double d = distanceTraveled(speed, time);
        return getDestinationLocation(location, course, d);
    }

    /**
	 * Returns distance in yards between two Locations
	 * 
	 * @param combatant1
	 *            1st Combatant
	 * @param combatant2
	 *            2nd Combatant
	 * @return Distance/Range between the two input Combatants
	 */
    public static double getRange(Combatant combatant1, Combatant combatant2) {
        Location loc1 = combatant1.getLocation();
        Location loc2 = combatant2.getLocation();
        double x1 = loc1.getX();
        double x2 = loc2.getX();
        double y1 = loc1.getY();
        double y2 = loc2.getY();
        double dy = y1 - y2;
        double dx = x1 - x2;
        return Math.hypot(dx, dy);
    }

    /**
	 * Returns bearing to second Combatant from first Combatant
	 * 
	 * @param combatant1
	 *            1st Combatant
	 * @param combatant2
	 *            2nd Combatant
	 * @return Bearing to second Combatant from first Combatant
	 */
    public static double getBearing(Combatant combatant1, Combatant combatant2) {
        Location location1 = combatant1.getLocation();
        Location location2 = combatant2.getLocation();
        double dx = location2.getX() - location1.getX();
        double dy = location2.getY() - location1.getY();
        double angle = 0;
        if (dx == 0 && dy == 0) return 0; else if (dx == 0) {
            if (dy > 0) return 0; else return 180;
        } else if (dy == 0) {
            if (dx > 0) return 90; else return 270;
        } else {
            double refAngle = Math.abs(Math.toDegrees(Math.atan(dy / dx)));
            if (dy < 0 && dx < 0) {
                angle = refAngle + 180;
            } else if (dx < 0) {
                angle = (90 - refAngle) + 90;
            } else if (dy < 0) {
                angle = (90 - refAngle) + 270;
            } else {
                angle = refAngle;
            }
        }
        return Methods.convertFromDegreesToBearing(angle);
    }

    /**
	 * Returns true if the given string represents a number. It will return
	 * false if any character is not a digit or decimal point. Returns false if
	 * there is more than one decimal point.
	 * 
	 * @param str
	 *            String to test for 'numberness'
	 * @return true if string represents a number
	 */
    public static boolean isNumber(String str) {
        int len = str.length();
        int chs = 0;
        if (len < 1) return false;
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (ch == '.') {
                chs++;
            } else if (!Character.isDigit(ch)) {
                return false;
            }
        }
        if (chs > 1) return false; else return true;
    }

    /**
	 * Returns true if the given string represents a number. It will return
	 * false if any character is not a digit.
	 * 
	 * @param str
	 *            String to test for 'integerness'
	 * @return true if string represents a number
	 */
    public static boolean isInteger(String str) {
        int len = str.length();
        if (len < 1) return false;
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (!Character.isDigit(ch)) return false;
        }
        return true;
    }

    /**
	 * Returns a string without any leading zeros. Note that it is a dumb
	 * methods as it does not test for 'numberness', it only strips leading
	 * zeros regardless of input.
	 * 
	 * @param str
	 *            String representing a number
	 * @return String without any leading zeros
	 */
    public static String stripLeadingZeros(String str) {
        if (str.length() == 1) return str;
        while (str.startsWith("0") && str.length() > 1) str = str.substring(1).trim();
        return str.trim();
    }

    /**
	 * Returns a positive double representing an angle (0-360)
	 * 
	 * @param angle
	 *            original angle (possibly negative)
	 * @return an equivalent; will be positive
	 */
    public static double posifyAngle(double angle) {
        while (angle < 0) angle += 360;
        while (angle > 360) angle -= 360;
        return angle;
    }

    /**
	 * Returns a String with all passed args (Strings) replaced by passed
	 * replacement in given string
	 * 
	 * @param str
	 *            original String
	 * @param args
	 *            array of Strings to be replaced in original String
	 * @param replacement
	 *            String replacing instances of args in str
	 * @return String with all replacements made
	 */
    public static String replaceAllArgsWith(String str, String[] args, String replacement) {
        int cnt = args.length;
        if (str.length() < 1 || cnt < 1) return str;
        for (int i = 0; i < cnt; i++) {
            str = str.replace(args[i], replacement);
        }
        return str;
    }

    /**
	 * Returns true if String str equals any of the passed args (Strings)
	 * 
	 * @param str
	 *            original String
	 * @param args
	 *            Strings to compare to original String
	 * @return String void of passed token Strings
	 */
    public static boolean equalsAnyOfTheseArgs(String str, String[] args) {
        int len = str.length();
        int toks = args.length;
        if (len < 1 || toks < 1) return false;
        for (int i = 0; i < toks; i++) {
            if (str.equals(args[i])) return true;
        }
        return false;
    }

    /**
	 * Returns a String with all passed args (Strings) removed from given string
	 * 
	 * @param str
	 *            original String
	 * @param args
	 *            Array of Strings to be removed from original String
	 * @return String void of passed token Strings
	 */
    public static String deleteargs(String str, String[] args) {
        int stopper = args.length;
        if (str.length() < 1 || args.length < 1) return str;
        for (int i = 0; i < stopper; i++) {
            str = str.replace(args[i], "").trim();
        }
        return str;
    }

    /**
	 * Returns true if given String contains ALL args
	 * 
	 * @param str
	 *            original String
	 * @param args
	 *            Array of Strings to be checked for in original String
	 * @return True if given String contains ALL args
	 */
    public static boolean containsAllArgs(String str, String[] args) {
        int stopper = args.length;
        if (str.length() < 1 || args.length < 1) return false;
        for (int i = 0; i < stopper; i++) {
            if (!str.contains(args[i])) return false;
        }
        return true;
    }

    /**
	 * Returns true if given String contains ANY args
	 * 
	 * @param str
	 *            Original String
	 * @param args
	 *            Array of Strings to be checked for in original String
	 * @return True if given String contains ANY args
	 */
    public static boolean containsAnyArgs(String str, String[] args) {
        int stopper = args.length;
        if (str.length() < 1 || args.length < 1) return false;
        for (int i = 0; i < stopper; i++) {
            if (str.contains(args[i])) return true;
        }
        return false;
    }

    /**
	 * Returns true if given String begins with ANY of the passed args
	 * 
	 * @param str
	 *            Original String
	 * @param args
	 *            Array of Strings to be checked for in original String
	 * @return True if given String begins with ANY of the passed args
	 */
    public static boolean startsWithArgs(String str, String[] args) {
        int stopper = args.length;
        if (str.length() < 1 || args.length < 1) return false;
        for (int i = 0; i < stopper; i++) {
            if (!str.startsWith(args[i])) return false;
        }
        return true;
    }

    /**
	 * Returns an array of Objects with the answers to passed questions
	 * 
	 * @param parameters
	 *            Strings to print as questions
	 * @return Array of Objects that are the answers to the passed parameter
	 *         questions
	 */
    public static Object[] parseCommandParameters(ArrayList<String> parameters) {
        Object[] inputs = new Object[parameters.size()];
        Scanner scanner = new Scanner(System.in);
        for (int i = 0; i < parameters.size(); i++) {
            System.out.print(" > " + parameters.get(i) + ": ");
            inputs[i] = scanner.nextLine();
        }
        return inputs;
    }

    public static void runTests() {
        System.out.println(" ^^^ TEST: Degree and Bearing Conversion");
        boolean worked = true;
        for (double i = 0; i < 360; i++) {
            worked &= (Methods.convertFromDegreesToBearing(Methods.convertFromBearingToDegrees(i)) == i) && (Methods.convertFromBearingToDegrees(Methods.convertFromDegreesToBearing(i)) == i);
        }
        System.out.println("Degree and Bearing conversion " + (worked ? "works!" : "does NOT work!"));
        System.out.println(" ^^^ TEST: stripLeadingZeros(str)");
        System.out.println("005 -> " + Methods.stripLeadingZeros("005"));
        System.out.println("050 -> " + Methods.stripLeadingZeros("050"));
        System.out.println("500 -> " + Methods.stripLeadingZeros("500"));
        System.out.println(" ^^^ TEST: readableBngOrCrs(str)");
        System.out.println("5 -> " + Methods.readableBrgOrCrs("5"));
        System.out.println("50 -> " + Methods.readableBrgOrCrs("50"));
        System.out.println("500 -> " + Methods.readableBrgOrCrs("500"));
        System.out.println(" ^^^ TEST: applyYorKorNM(str)");
        System.out.println("2000 ->" + Methods.applyYorKorNM("2000"));
        System.out.println("2k ->" + Methods.applyYorKorNM("2k"));
        System.out.println("1nm ->" + Methods.applyYorKorNM("1nm"));
        System.out.println("2000y ->" + Methods.applyYorKorNM("2000y"));
        System.out.println("2000yds ->" + Methods.applyYorKorNM("2000yds"));
        System.out.println("2000yards ->" + Methods.applyYorKorNM("2000yards"));
        System.out.println("5.5m ->" + Methods.applyYorKorNM("5.5m"));
        System.out.println("5.5 m ->" + Methods.applyYorKorNM("5.5 m"));
    }
}
