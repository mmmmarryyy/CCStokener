package net.sourceforge.jmisc;

import java.net.*;
import java.io.*;

/** This reads in numbers from a stream.  It deals with fortran output that
    might have numbers run together.
    <p> Warning: getLineNumber might be confused if the next character is
    a 'new line'.

    @author <a href=mailto:"fredgc@users.sourceforge.net">Fred Gylys-Colwell</a>
    @version $Name:  $, $Revision: 1.1.1.1 $ 
*/
public class FortranReader extends LineNumberReader {

    /** The constructor from a filename (or URL). */
    public static FortranReader makeReader(String filename) throws IOException {
        FortranReader in;
        URL url;
        try {
            url = new URL(filename);
            InputStreamReader reader = new InputStreamReader(url.openStream());
            in = new FortranReader(reader);
        } catch (MalformedURLException e) {
            in = new FortranReader(new FileReader(filename));
        }
        return in;
    }

    /** The constructor from an already open stream. */
    public FortranReader(InputStream stream) throws IOException {
        super(new InputStreamReader(stream));
    }

    /** The constructor from an already open Reader. */
    public FortranReader(Reader r) {
        super(r);
    }

    int nextChar = -2;

    /** Peek at the next character in the stream.
	@return the character or -1 on end of file.
     */
    public int next() throws IOException {
        if (nextChar == -2) nextChar = super.read();
        return nextChar;
    }

    public int read() throws IOException {
        int z = next();
        if (z == -1) return z;
        nextChar = super.read();
        return z;
    }

    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        if (next() < 0) return next();
        cbuf[off] = (char) next();
        int z = super.read(cbuf, off + 1, len - 1);
        if (z < 0) {
            return z;
        }
        read();
        return z + 1;
    }

    int markNext;

    public void mark(int readAheadLimit) throws IOException {
        super.mark(readAheadLimit);
        markNext = next();
    }

    public void reset() throws IOException {
        super.reset();
        nextChar = markNext;
    }

    public long skip(int n) throws IOException {
        if (next() == -1) return 0;
        if (n == 1) {
            read();
            return 1;
        }
        long result = super.skip(n - 1);
        read();
        return result + 1;
    }

    /** Read a signed integer. */
    public int readInt() throws IOException {
        return readInt(true);
    }

    /** Read an integer.  If signed=false, then it must NOT be a signed
	integer.
	@throws EOFException on end of file.
    */
    public int readInt(boolean signed) throws IOException {
        if (next() == -1) throw new EOFException("- end of file -");
        while (Character.isWhitespace((char) next())) {
            read();
        }
        int sign = 1;
        if (signed) {
            if ((next() == '+')) {
                sign = 1;
                read();
            }
            if ((next() == '-')) {
                sign = -1;
                read();
            }
        }
        if (next() == -1) throw new EOFException("- end of file -");
        int val = 0;
        boolean flag = true;
        while (Character.isDigit((char) next())) {
            val = val * 10 + (next() - '0');
            read();
            flag = false;
        }
        if (flag) {
            throw new IOException("Unexpected character: '" + next() + "'");
        }
        return sign * val;
    }

    /** Read a floating point number.
     	@throws EOFException on end of file.
    */
    public double readDouble() throws IOException {
        if (next() == -1) throw new EOFException("- end of file -");
        while (Character.isWhitespace((char) next())) {
            read();
        }
        double mantissa = 0;
        int sign = 1;
        if (next() == '+') {
            read();
        }
        if (next() == '-') {
            sign = -1;
            read();
        }
        boolean mantissaFound = false;
        while (Character.isDigit((char) next())) {
            mantissa = mantissa * 10 + (next() - '0');
            read();
            mantissaFound = true;
        }
        if (next() == '.') {
            read();
            double scoot = 1;
            while (Character.isDigit((char) next())) {
                mantissa = mantissa * 10 + (next() - '0');
                scoot = scoot * 10;
                read();
                mantissaFound = true;
            }
            mantissa = mantissa / scoot;
        }
        if (!mantissaFound) {
            if (next() == -1) throw new EOFException("- end of file -");
            throw new IOException("Unexpected character: '" + (char) next() + "'");
        }
        if ((next() == 'E') || (next() == 'e') || (next() == 'D') || (next() == 'd')) {
            read();
            int esign = 1;
            if (next() == '+') {
                read();
            }
            if (next() == '-') {
                esign = -1;
                read();
            }
            int exp = 0;
            boolean flag = true;
            while (Character.isDigit((char) next())) {
                exp = exp * 10 + (next() - '0');
                read();
                flag = false;
            }
            if (flag) {
                if (next() == -1) throw new EOFException("- end of file -");
                throw new IOException("Unexpected character: '" + (char) next() + "'");
            }
            mantissa = mantissa * Math.pow(10, exp * esign);
        }
        return mantissa * sign;
    }

    /** Read until the end of the line.  The next character is the one after
	the '\n' */
    public void eol() throws IOException {
        try {
            while ((next() >= 0) && (next() != '\n')) {
                read();
            }
            read();
        } catch (EOFException e) {
        }
    }

    /** Read to the end of the line.   The line (without the \n) is returned. */
    public String readLine() throws IOException {
        String s = "";
        if (next() == -1) return null;
        if (next() == '\n') {
            s = "";
            read();
            return s;
        }
        String rest = super.readLine();
        if (rest == null) {
            nextChar = -1;
            return s;
        }
        read();
        return s + rest;
    }
}
