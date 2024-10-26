package visualbiology;

import java.net.URL;
import java.net.URLConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Performs a Multipart HTTP post to the given URL. A post operation is started
 * with the creation of a MultipartPost object. Post parameters are sent with
 * writeParameter() and may be eithier strings or the contents of an XML file. A
 * post is finished by calling done() which returns an InputStream for reading
 * the servers response.
 * 
 * NOTE: This class is meant to communicate with the SBML.org online validator.
 * As such, it assumes uploaded files are XML and always sends a Content-Type:
 * text/xml.
 */
class MultipartPost {

    public MultipartPost(String url) throws IOException {
        Random random = new Random();
        connection = (new URL(url)).openConnection();
        boundary = "<<" + Long.toString(random.nextLong(), 30);
        String type = "multipart/form-data; boundary=" + boundary;
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", type);
        stream = connection.getOutputStream();
    }

    public InputStream done() throws IOException {
        writeln("--" + boundary + "--");
        writeln();
        stream.close();
        return connection.getInputStream();
    }

    public void writeParameter(String name, String value) throws IOException {
        writeln("--" + boundary);
        writeln("Content-Disposition: form-data; name=\"" + name + "\"");
        writeln();
        writeln(value);
    }

    public void writeParameter(String name, File file) throws IOException {
        String prefix = "Content-Disposition: form-data; name=\"file\"; filename=";
        writeln("--" + boundary);
        writeln(prefix + '"' + file.getName() + '"');
        writeln("Content-Type: text/xml");
        writeln();
        InputStream source = new FileInputStream(file);
        copy(source, stream);
        stream.flush();
        source.close();
    }

    void copy(InputStream source, OutputStream destination) throws IOException {
        byte[] buffer = new byte[8192];
        int nbytes = 0;
        while ((nbytes = source.read(buffer, 0, buffer.length)) >= 0) {
            destination.write(buffer, 0, nbytes);
        }
    }

    void writeln(String s) throws IOException {
        write(s);
        writeln();
    }

    void writeln() throws IOException {
        write('\r');
        write('\n');
    }

    void write(char c) throws IOException {
        stream.write(c);
    }

    void write(String s) throws IOException {
        stream.write(s.getBytes());
    }

    URLConnection connection;

    OutputStream stream;

    String boundary;
}

/**
 * SBMLValidator is simply a container for the static method
 * validateSBML(filename, parameters).
 */
public class SBMLValidator {

    public static String validatorURL = "http://sbml.org/validator/";

    @SuppressWarnings("rawtypes")
    public static InputStream validateSBML(String filename, Map parameters) throws IOException {
        return validateSBML(filename, parameters, validatorURL);
    }

    /**
	 * Validates the given SBML filename (or http:// URL) by calling the
	 * SBML.org online validator. The results are returned as an InputStream
	 * whose format may be controlled by setting parameters.put("output", ...)
	 * to one of: "xml", "xhtml", "json", "text" (default: xml).
	 * 
	 * @return an InputStream containing the validation results.
	 */
    @SuppressWarnings({ "rawtypes", "unchecked", "unused" })
    public static InputStream validateSBML(String filename, Map parameters, String validatorURL) throws IOException {
        if (parameters.get("output") == null) {
            parameters.put("output", "xml");
        }
        MultipartPost post = new MultipartPost(validatorURL);
        InputStream result = null;
        if (filename.startsWith("http://")) {
            post.writeParameter("url", filename);
        } else {
            post.writeParameter("file", new File(filename));
        }
        try {
            Iterator iter = parameters.keySet().iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                String value = (String) parameters.get(name);
                post.writeParameter(name, value);
            }
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        return post.done();
    }
}

/**
 * usage: java validateSBML [-h] [-o output-format] [-d opt1[,opt2,...]]
 * filename.xml usage: java validateSBML [-h] [-o output-format] [-d
 * opt1[,opt2,...]] http://...
 * 
 * Validates the SBML document given by filename.xml or located at the http://
 * URL. Output-format is optional and may be one of: xml, xhtml, json, text
 * (default: xml)
 */
class validateSBML {

    static void usage() {
        String usage = "usage: java validateSBML [-h] [-o output-format] [-d opt1[,opt2,...]] filename.xml\n" + "usage: java validateSBML [-h] [-o output-format] [-d opt1[,opt2,...]] http://..." + "\n\n" + "  Validates the SBML document given by filename.xml or located at\n" + "  the http:// URL." + "\n\n" + "Options:\n\n" + "  -o output-format\n" + "    Specify an output format.\n\n" + "      xml   : XML (Default)\n" + "      xhtml : XHTML\n" + "      text  : plain text\n" + "      json  : JavaScript Object Notation\n\n" + "  -d opt1[,opt2,...]\n" + "    Disable the given consistency check options.\n" + "    The options are given as comma-separated characters.\n" + "    Each character is one of the followings:\n\n" + "      u : disable the units consistency check\n" + "      g : disable the overall SBML consistency check\n" + "      i : disable the identifier consistency check\n" + "      m : disable the MathML consistency check\n" + "      s : disable the SBO consistency check\n" + "      o : disable the overdetermined model check\n" + "      p : disable the modeling practice check\n\n" + "  -h  : Print this usage and exit.\n";
        System.out.println(usage);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void main(String[] args) {
        String filename = null;
        String output = "xml";
        String offcheck = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h")) {
                usage();
                return;
            } else if (args[i].equals("-o")) {
                if ((i + 1) >= args.length) {
                    usage();
                    return;
                }
                Pattern p = Pattern.compile("(xml|xhtml|json|text)");
                Matcher m = p.matcher(args[i + 1]);
                if (!m.matches()) {
                    usage();
                    return;
                }
                output = args[++i];
            } else if (args[i].equals("-d")) {
                if ((i + 1) >= args.length) {
                    usage();
                    return;
                }
                Pattern p = Pattern.compile("[a-zA-Z](,[a-zA-Z])*");
                Matcher m = p.matcher(args[i + 1]);
                if (!m.matches()) {
                    usage();
                    return;
                }
                offcheck = args[++i];
            } else if (args[i].startsWith("-")) {
                usage();
                return;
            } else {
                if ((i + 1) < args.length) {
                    usage();
                    return;
                }
                filename = args[i];
            }
        }
        if (filename == null) {
            usage();
            return;
        }
        try {
            HashMap parameters = new HashMap();
            InputStream result = null;
            parameters.put("output", output);
            if (offcheck != null) {
                parameters.put("offcheck", offcheck);
            }
            result = SBMLValidator.validateSBML(filename, parameters);
            print(result, System.out);
            result.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void print(InputStream source, OutputStream destination) throws IOException {
        byte[] buffer = new byte[8192];
        int nbytes = 0;
        while ((nbytes = source.read(buffer, 0, buffer.length)) >= 0) {
            destination.write(buffer, 0, nbytes);
        }
        destination.flush();
    }
}
