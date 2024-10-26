package com.c5corp.c5dem;

import java.io.*;
import java.net.URL;

/**
* <p>Reader.java is a general purpose text file reader that has
* methods for reading different data types out of the same file.
* It was designed for use with USGS DEM files and the data types
* found in that particular file format. It extends java.io.BufferedInputStream.</p>
* @author  Brett Stalbaum
* @version 2.0
* @since 1.0
*/
public class Reader extends BufferedInputStream {

    private int input;

    private long readerposition;

    protected boolean eof = false;

    /** Constructor for a reader accepts a String specifying the path to 
	 * the DEM file.
	 * @param str String representation of a DEM file
	 * @throws IOException
	 */
    protected Reader(String str) throws IOException {
        super(new FileInputStream(str), 8192);
        readerposition = 0;
    }

    /** Constructor for a reader accepts a String specifying the path to the 
	* DEM file. Allows the specification of a buffer size for 
	* BufferedInputStream class.
	* @param str String representation of a DEM file 
	* @param buf the buffer size
	* @throws IOException 
	* @see java.io.BufferedInputStream
	*/
    protected Reader(String str, int buf) throws IOException {
        super(new FileInputStream(str), buf);
        readerposition = 0;
    }

    /** Constructor for a reader accepts a java.net.URL specifying the path to the dem file. Allows
	* the specification of a buffer size for BufferedInputStream class.
	* @param url the URL pointing to a DEM
	* @throws IOException 
	* @since 1.0.3
	*/
    protected Reader(URL url) throws IOException {
        super(url.openStream());
        readerposition = 0;
    }

    /** <code>public short[] ReadDataElementsShort(int number_of_elements);</code>
	* A reader method that reads in a stream of Data from a Dem file
	* and converts the space separated input into elements of an array...
	* has ability to stop at eof (also sets eof to true)
	* @param number_of_elements the number of elements to parse
	* @return an array containing the elements
	*/
    public short[] ReadDataElementsShort(int number_of_elements) {
        StringBuffer thebuf = new StringBuffer();
        short[] srt = new short[number_of_elements];
        int i = 0;
        boolean foundinput = false;
        while (i < number_of_elements) {
            try {
                input = read();
                readerposition++;
                if (input != -1) {
                    while ((char) input != ' ') {
                        if (input != -1) {
                            thebuf.append((char) input);
                            foundinput = true;
                        } else {
                            eof = true;
                            break;
                        }
                        input = read();
                        readerposition++;
                    }
                    if (foundinput) {
                        srt[i] = (short) Integer.parseInt(thebuf.toString());
                        foundinput = false;
                        thebuf = new StringBuffer();
                        i++;
                    }
                } else {
                    eof = true;
                    break;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return srt;
    }

    /** <code>public int[] ReadDataElementsInt(int number_of_elements);</code>
	* reader that reads in a stream of Data from a Dem file
	* and converts the space separated input into elements of an array...
	* has ability to stop at eof (also sets eof to true). Added in version 1.0.2
	* because dem files denominated in meters actually contain decimeters.
	* @param number_of_elements the number of elements to parse
	* @return an array containing the elements
	*/
    public int[] ReadDataElementsInt(int number_of_elements) {
        StringBuffer thebuf = new StringBuffer();
        int[] arr = new int[number_of_elements];
        int i = 0;
        boolean foundinput = false;
        while (i < number_of_elements) {
            try {
                input = read();
                readerposition++;
                if (input != -1) {
                    while ((char) input != ' ') {
                        if (input != -1) {
                            thebuf.append((char) input);
                            foundinput = true;
                        } else {
                            eof = true;
                            break;
                        }
                        input = read();
                        readerposition++;
                    }
                    if (foundinput) {
                        arr[i] = Integer.parseInt(thebuf.toString());
                        foundinput = false;
                        thebuf = new StringBuffer();
                        i++;
                    }
                } else {
                    eof = true;
                    break;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return arr;
    }

    /** <code>public double[] ReadDataElementsDouble(int number_of_elements);</code>
	* reader that reads in a stream of Data from a Dem file
	* and converts the space separated input into elements of an array...
	* has ability to stop at eof (also sets eof to true)
	* @param number_of_elements the number of elements to parse
	* @return an array containing the elements
	*/
    public double[] ReadDataElementsDouble(int number_of_elements) {
        StringBuffer thebuf = new StringBuffer();
        double[] dbl = new double[number_of_elements];
        int i = 0;
        boolean foundinput = false;
        Double dblobj;
        while (i < number_of_elements) {
            try {
                input = read();
                readerposition++;
                if (input != -1) {
                    while ((char) input != ' ') {
                        if (input != -1) {
                            if (((char) input == 'd') || ((char) input == 'D')) {
                                thebuf.append('E');
                            } else {
                                thebuf.append((char) input);
                            }
                            foundinput = true;
                        } else {
                            eof = true;
                            break;
                        }
                        input = read();
                        readerposition++;
                    }
                    if (foundinput) {
                        dblobj = new Double(thebuf.toString());
                        dbl[i] = dblobj.doubleValue();
                        foundinput = false;
                        thebuf = new StringBuffer();
                        i++;
                    }
                } else {
                    eof = true;
                    break;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return dbl;
    }

    /** <code>public String readStringRange(int from, int to);</code>
	* reader that takes the current count from in, grabs the chars in the
	* range public int from (typically the previous to) int to
	* if there is no input (just space), a single space is returned
	* @param from here
	* @param to here
	* @return the String representation of the chars
	*/
    public String readStringRange(int from, int to) {
        StringBuffer thebuf = new StringBuffer();
        for (; from <= to; from++) {
            try {
                input = read();
                readerposition++;
                if (input != -1) {
                    thebuf.append((char) input);
                } else {
                    eof = true;
                    break;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return thebuf.toString();
    }

    /** <code>public char readCharRange(InputStream in, int from, int to);</code>
	* reader that counts from (from) to in, grabing the chars in the
	* range public int from (which is the previous to) int to
	* if there is no input (just space), a single space is returned
	* @param from here
	* @param to here
	* @return the String representation of the chars
	*/
    public char readCharRange(int from, int to) {
        StringBuffer thebuf = new StringBuffer();
        StringBuffer temp = new StringBuffer();
        boolean found = false;
        for (; from <= to; from++) {
            try {
                thebuf.append((char) read());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        for (; from <= to; from++) {
            try {
                input = read();
                readerposition++;
                if (input != -1) {
                    thebuf.append((char) input);
                } else {
                    eof = true;
                    break;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        if (!found) {
            temp.append(' ');
        }
        return temp.charAt(0);
    }

    /** <code>public short readShortRange(InputStream in, int from, int to)</code>
	* reader that takes the current count from in, grabs the chars in that
	* range, and if there is no input (just space), -1 is returned
	* @param from here
	* @param to here
	* @return the String representation of the chars
	*/
    public short readShortRange(int from, int to) {
        StringBuffer thebuf = new StringBuffer();
        StringBuffer temp = new StringBuffer();
        boolean found = false;
        for (; from <= to; from++) {
            try {
                input = read();
                readerposition++;
                if (input != -1) {
                    thebuf.append((char) input);
                } else {
                    eof = true;
                    break;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        for (short i = 0; i < thebuf.length(); i++) {
            if (thebuf.charAt(i) != ' ') {
                temp.append(thebuf.charAt(i));
                found = true;
            }
        }
        if (!found) {
            return ((short) -1);
        }
        return (short) Integer.parseInt(temp.toString());
    }

    /** public double readDoubleRangeRange(int from, int to)
	* reader that takes the current count from in, grabs the chars in that
	* range, and if there is no input (just space), NaN is returned.
	* @param from here
	* @param to here
	* @return the String representation of the chars
	*/
    public double readDoubleRange(int from, int to) {
        StringBuffer thebuf = new StringBuffer();
        StringBuffer temp = new StringBuffer();
        double value;
        boolean found = false;
        for (; from <= to; from++) {
            try {
                input = read();
                readerposition++;
                if (input != -1) {
                    thebuf.append((char) input);
                } else {
                    eof = true;
                    break;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        for (short i = 0; i < thebuf.length(); i++) {
            if (thebuf.charAt(i) != ' ') {
                found = true;
                if (thebuf.charAt(i) == 'D' || thebuf.charAt(i) == 'd') {
                    temp.append('E');
                } else {
                    temp.append(thebuf.charAt(i));
                }
            }
        }
        if (!found) {
            return (Double.NaN);
        } else {
            Double dub = new Double(temp.toString());
            value = dub.doubleValue();
        }
        return value;
    }

    /** Returns the position of the internal reader
	 * @return the position of the internal reader
	 */
    public long getReaderPosition() {
        return readerposition;
    }

    /** overrides java.lang.Object.toString() */
    public String toString() {
        return this.getClass().getName() + "\n" + C5DemConstants.copy;
    }
}
