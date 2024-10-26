package gnu.kinsight;

import java.io.IOException;
import java.net.*;
import java.util.*;
import javax.swing.*;
import gnu.kinsight.timeline.Event;

/**
 * The data representation of a person.
 *
 * @author <a href="mailto:gann@pobox.com">Gann Bierner</a>
 * @version $Revision: 1.9 $
 * @see Comparable
 */
public class Person implements Comparable {

    private static ImageIcon genericIcon;

    private static HashSet suffixes;

    public static final int MALE = 0;

    public static final int FEMALE = 1;

    private String fname, mname, lname, maiden, suffix, aka;

    private int sex;

    private Date bDate, dDate;

    private String bPlace;

    private boolean dead;

    private String info, pic;

    private byte[] imageData;

    private ImageIcon icon;

    private HashSet marriages;

    private List events = new ArrayList();

    static {
        genericIcon = new ImageIcon(ClassLoader.getSystemResource("gnu/kinsight/pix/default.gif"));
        suffixes = new HashSet();
        suffixes.add("Jr");
        suffixes.add("Sr");
        suffixes.add("I");
        suffixes.add("II");
        suffixes.add("III");
        suffixes.add("IV");
    }

    /**
     * Creates a new <code>Person</code> instance.  Default is male.
     *
     */
    public Person() {
        sex = MALE;
        fname = mname = lname = maiden = "";
    }

    /**
     * Creates a new <code>Person</code> instance.
     *
     * @param f first name
     * @param m middle name
     * @param l last name
     * @param mdn maiden name
     * @param s sex
     */
    public Person(String f, String m, String l, String mdn, String s) {
        fname = f;
        mname = m;
        lname = l;
        maiden = mdn;
        sex = s.equals("M") ? MALE : FEMALE;
    }

    public Person(String name, int s) {
        sex = s;
        String[] names = name.split(" ");
        fname = names[0];
        int last = names.length - 1;
        if (names[last].endsWith(".") || suffixes.contains(names[last])) {
            suffix = names[last];
            last = last - 1;
        }
        lname = names[last];
        mname = "";
        for (int i = 1; i < last; i++) {
            String n = names[i].trim();
            if (n.startsWith("(") && n.endsWith(")")) {
                n = n.substring(1, n.length() - 1);
                if (sex == FEMALE) {
                    maiden = n;
                } else {
                    aka = n;
                }
            } else {
                mname += n + " ";
            }
        }
        mname = mname.trim();
    }

    /**
     * Does a lexicographic comparison on the names.
     *
     * @param o an <code>Object</code>
     * @return an <code>int</code>
     */
    public int compareTo(Object o) {
        Person p = (Person) o;
        if (p == null) {
            return -1;
        }
        return (lname + fname + mname).compareTo(p.getLastName() + p.getFirstName() + p.getMiddleName());
    }

    /**
     * Add a marriage for which this person is a member.
     *
     * @param m a <code>Marriage</code>
     */
    protected void addMarriage(Marriage m) {
        if (marriages == null) marriages = new HashSet();
        marriages.add(m);
    }

    /**
     * The marriages in which this person participates
     *
     * @return a <code>Collection</code> of <code>Marriages</code>
     */
    public Collection getMarriages() {
        return marriages;
    }

    /**
     * Remove a marriage.
     *
     * @param m a <code>Marriage</code>
     */
    protected void removeMarriage(Marriage m) {
        if (marriages != null) marriages.remove(m);
    }

    public List getEvents() {
        return events;
    }

    public void addEvent(Event e) {
        events.add(e);
    }

    /**
     * the first name
     *
     * @return a <code>String</code>, null if not set
     */
    public String getFirstName() {
        return fname;
    }

    /**
     * the middle name
     *
     * @return a <code>String</code>, null if not set
     */
    public String getMiddleName() {
        return mname;
    }

    /**
     * the last name
     *
     * @return a <code>String</code>, null if not set
     */
    public String getLastName() {
        return lname;
    }

    /**
     * the maiden name. 
     *
     * @return a <code>String</code>, null if not set
     */
    public String getMaidenName() {
        return maiden;
    }

    /**
     * Trailing information such as Jr., Sr.,  etc.
     *
     * @return a <code>String</code>, null if not set
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * a nickname
     *
     * @return a <code>String</code>, null if not set
     */
    public String getAKA() {
        return aka;
    }

    /**
     * the sex
     *
     * @return an <code>int</code>.  Either Person.MALE or Person.Female
     */
    public int getSex() {
        return sex;
    }

    /**
     * Set the first name
     *
     * @param n a <code>String</code>
     */
    public void setFirstName(String n) {
        fname = n;
    }

    /**
     * set the middle name
     *
     * @param n a <code>String</code>
     */
    public void setMiddleName(String n) {
        mname = n;
    }

    /**
     * set the last name
     *
     * @param n a <code>String</code>
     */
    public void setLastName(String n) {
        lname = n;
    }

    /**
     * set the maiden name
     *
     * @param n a <code>String</code>
     */
    public void setMaidenName(String n) {
        maiden = n;
    }

    /**
     * Set the suffix (such as Jr. or Sr.)
     *
     * @param n a <code>String</code>
     */
    public void setSuffix(String n) {
        suffix = n;
    }

    /**
     * set the nickname
     *
     * @param n a <code>String</code>
     */
    public void setAKA(String n) {
        aka = n;
    }

    /**
     * set the sex
     *
     * @param i an <code>int</code>, either Person.MALE or Person.FEMALE
     */
    public void setSex(int i) {
        sex = i;
    }

    /**
     * return the date of birth
     *
     * @return a <code>Date</code>
     */
    public Date getBirthDate() {
        return bDate;
    }

    /**
     * set the date of birth
     *
     * @param v a <code>Date</code>
     */
    public void setBirthDate(Date v) {
        this.bDate = v;
    }

    /**
     * get the date of death
     *
     * @return a <code>Date</code>
     */
    public Date getDeathDate() {
        return dDate;
    }

    /**
     * set the date of death
     *
     * @param v a <code>Date</code>
     */
    public void setDeathDate(Date v) {
        this.dDate = v;
    }

    /**
     * return the place in which this person was born
     *
     * @return a <code>String</code>
     */
    public String getBirthPlace() {
        return bPlace;
    }

    /**
     * set the place in which this person was born
     *
     * @param v a <code>String</code>
     */
    public void setBirthPlace(String v) {
        this.bPlace = v;
    }

    /**
     * any miscellaneous information about this person
     *
     * @return a <code>String</code>
     */
    public String getInfo() {
        return info;
    }

    /**
     * add any miscellaneous information about this person
     *
     * @param v a <code>String</code>
     */
    public void setInfo(String v) {
        this.info = v;
    }

    /**
     * whether or not this person is dead
     *
     * @param v a <code>String</code>
     */
    public boolean isDead() {
        return dead;
    }

    /**
     * set whether or not this person is dead
     *
     * @param v a <code>boolean</code>
     */
    public void isDead(boolean v) {
        this.dead = v;
    }

    /**
     * set the name of a picture for this person as stored in the family
     * file.  
     *
     * @param v a <code>String</code>
     */
    public void setPic(String v) {
        pic = v;
    }

    public void setPic(URL url) throws IOException {
        imageData = new byte[url.openConnection().getContentLength()];
        url.openStream().read(imageData, 0, imageData.length);
        icon = null;
    }

    public void setImageData(byte[] data) {
        imageData = data;
    }

    /**
     * whether or not this person has a picture stored
     *
     * @return a <code>boolean</code>
     */
    public boolean hasPic() {
        return imageData != null;
    }

    /**
     * return a picture for this person from a family file
     *
     * @param file a <code>FamilyFile</code>
     * @return an <code>ImageIcon</code>
     */
    public ImageIcon getPic() {
        if (imageData == null) {
            return genericIcon;
        }
        if (icon == null) {
            icon = new ImageIcon(imageData);
        }
        return icon;
    }

    public byte[] getImageData() {
        return imageData;
    }

    /**
     * return the name of the picture as stored in the family file
     *
     * @return a <code>String</code>
     */
    public String getPicName() {
        return pic;
    }

    public boolean equals(Object o) {
        return this == o;
    }

    public String toString() {
        String name = aka != null && !aka.equals("") ? aka : fname;
        if (!mname.equals("")) name += " " + mname;
        name += " " + lname;
        return name;
    }

    public static void main(String[] args) {
        Person p = new Person("Jane Anne (Smith) Jones", FEMALE);
        System.out.println(p);
        System.out.println("'" + p.fname + "'");
        System.out.println("'" + p.mname + "'");
        System.out.println("'" + p.lname + "'");
        System.out.println("'" + p.maiden + "'");
    }
}
