package orinoco.layout;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import orinoco.PDFWriter;

/**
 * The font face. Maintains a hashtable of the font's character widths
 */
class Face {

    /**
	 * The character widths for a given font face
	 */
    private double[] characterWidths;

    /**
	 * The faces currently read in
	 */
    private static HashMap faces = new HashMap();

    /**
	 * Reads the faces from the face file
	 * 
	 * @exception IOException
	 * @param font
	 *            the font
	 */
    private Face(String font) throws IOException {
        characterWidths = new double[256];
        StringBuffer sb = new StringBuffer();
        sb.append('/');
        sb.append(Constants.FONTS_DIR);
        sb.append('/');
        sb.append(font);
        sb.append(Constants.CHAR_WIDTHS_SUFFIX);
        String path = sb.toString();
        URL url = getClass().getResource(path);
        InputStream is = url.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        int pos = 0;
        String width = br.readLine();
        while (width != null && pos < 256) {
            characterWidths[pos] = Double.parseDouble(width);
            pos++;
            width = br.readLine();
        }
    }

    /**
	 * Gets the width required for the specified character in this font face
	 * 
	 * @param c
	 *            the number of points
	 * @return the character
	 */
    public double getWidth(char c) {
        if (c >= characterWidths.length) {
            return 0.599791944;
        } else {
            return characterWidths[(int) c];
        }
    }

    /**
	 * Gets the font face from the hash table of faces already read in In the
	 * event of the face not being present, display the error and return the
	 * empty set of widths, but otherwise processing will continue
	 * 
	 * @param name
	 *            the name of the font face
	 * @return the face
	 */
    public static synchronized Face getFace(String name) {
        Face face = (Face) faces.get(name);
        if (face == null) {
            try {
                face = new Face(name);
                faces.put(name, face);
            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }
        return face;
    }
}
