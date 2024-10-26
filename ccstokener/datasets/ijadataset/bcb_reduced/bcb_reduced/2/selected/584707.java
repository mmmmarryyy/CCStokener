package mx.unam.fciencias.balpox.parser;

import mx.unam.fciencias.balpox.core.ThreadEvent;
import mx.unam.fciencias.balpox.core.ThreadEventManager;
import mx.unam.fciencias.balpox.util.PropertyManager;
import org.apache.log4j.Logger;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Esta clase se encarga de eliminar todos los dobles espacios que pueda tener
 * un archivo de texto o HTML; también, si el archivo tiene extensión HTML ó HTM, se encarga
 * de eliminar todos los tags para dejarlo como un archivo de texto simple.
 * <p/>
 * <br><br>Fecha: 19-Jun-2006&nbsp;&nbsp;&nbsp;&nbsp;Hora:13:22:17
 *
 * @author <a href="mailto:balpo@gmx.net?subject=trimmer.flex">Rodrigo Poblanno Balp</a>
 */
@SuppressWarnings({ "UnusedAssignment", "FieldCanBeLocal", "UnusedDeclaration", "JavaDoc", "SimplifiableIfStatement", "ForLoopReplaceableByForEach" })
public class Trimmer extends Thread {

    /**
	 * This character denotes the end of file
	 */
    public static final int YYEOF = -1;

    /**
	 * initial size of the lookahead buffer
	 */
    private static final int ZZ_BUFFERSIZE = 16384;

    /**
	 * lexical states
	 */
    public static final int YYINITIAL = 0;

    /**
	 * Translates characters to character classes
	 */
    private static final String ZZ_CMAP_PACKED = "\11\0\1\1\1\0\2\0\1\5\22\0\1\1\3\0\1\0\7\0" + "\1\4\1\0\1\4\14\0\1\4\1\2\1\0\1\3\2\0\32\0" + "\4\0\1\0\1\0\32\0\1\0\1\0\45\0\4\0\4\0\1\0" + "\12\0\1\0\4\0\1\0\5\0\27\0\1\0\37\0\1\0Ŀ\0" + "\31\0\162\0\4\0\14\0\16\0\5\0\11\0\1\0\213\0\1\0" + "\13\0\1\0\1\0\3\0\1\0\1\0\1\0\24\0\1\0\54\0" + "\1\0\46\0\1\0\5\0\4\0\202\0\10\0\105\0\1\0\46\0" + "\2\0\2\0\6\0\20\0\41\0\46\0\2\0\1\0\7\0\47\0" + "\110\0\33\0\5\0\3\0\56\0\32\0\5\0\13\0\43\0\2\0" + "\1\0\143\0\1\0\1\0\17\0\2\0\7\0\2\0\12\0\3\0" + "\2\0\1\0\20\0\1\0\1\0\36\0\35\0\3\0\60\0\46\0" + "\13\0\1\0Œ\0\66\0\3\0\1\0\22\0\1\0\7\0\12\0" + "\43\0\10\0\2\0\2\0\2\0\26\0\1\0\7\0\1\0\1\0" + "\3\0\4\0\3\0\1\0\36\0\2\0\1\0\3\0\16\0\4\0" + "\21\0\6\0\4\0\2\0\2\0\26\0\1\0\7\0\1\0\2\0" + "\1\0\2\0\1\0\2\0\37\0\4\0\1\0\1\0\23\0\3\0" + "\20\0\11\0\1\0\3\0\1\0\26\0\1\0\7\0\1\0\2\0" + "\1\0\5\0\3\0\1\0\22\0\1\0\17\0\2\0\17\0\1\0" + "\23\0\10\0\2\0\2\0\2\0\26\0\1\0\7\0\1\0\2\0" + "\1\0\5\0\3\0\1\0\36\0\2\0\1\0\3\0\17\0\1\0" + "\21\0\1\0\1\0\6\0\3\0\3\0\1\0\4\0\3\0\2\0" + "\1\0\1\0\1\0\2\0\3\0\2\0\3\0\3\0\3\0\10\0" + "\1\0\3\0\77\0\1\0\13\0\10\0\1\0\3\0\1\0\27\0" + "\1\0\12\0\1\0\5\0\46\0\2\0\43\0\10\0\1\0\3\0" + "\1\0\27\0\1\0\12\0\1\0\5\0\3\0\1\0\40\0\1\0" + "\1\0\2\0\43\0\10\0\1\0\3\0\1\0\27\0\1\0\20\0" + "\46\0\2\0\43\0\22\0\3\0\30\0\1\0\11\0\1\0\1\0" + "\2\0\7\0\72\0\60\0\1\0\2\0\13\0\10\0\72\0\2\0" + "\1\0\1\0\2\0\2\0\1\0\1\0\2\0\1\0\6\0\4\0" + "\1\0\7\0\1\0\3\0\1\0\1\0\1\0\1\0\2\0\2\0" + "\1\0\4\0\1\0\2\0\11\0\1\0\2\0\5\0\1\0\1\0" + "\25\0\2\0\42\0\1\0\77\0\10\0\1\0\42\0\35\0\4\0" + "\164\0\42\0\1\0\5\0\1\0\2\0\45\0\6\0\112\0\46\0" + "\12\0\51\0\7\0\132\0\5\0\104\0\5\0\122\0\6\0\7\0" + "\1\0\77\0\1\0\1\0\1\0\4\0\2\0\7\0\1\0\1\0" + "\1\0\4\0\2\0\47\0\1\0\1\0\1\0\4\0\2\0\37\0" + "\1\0\1\0\1\0\4\0\2\0\7\0\1\0\1\0\1\0\4\0" + "\2\0\7\0\1\0\7\0\1\0\27\0\1\0\37\0\1\0\1\0" + "\1\0\4\0\2\0\7\0\1\0\47\0\1\0\23\0\105\0\125\0" + "\14\0ɬ\0\2\0\10\0\12\0\32\0\5\0\113\0\3\0\3\0" + "\17\0\15\0\1\0\4\0\16\0\22\0\16\0\22\0\16\0\15\0" + "\1\0\3\0\17\0\64\0\43\0\1\0\3\0\2\0\103\0\130\0" + "\10\0\51\0\127\0\35\0\63\0\36\0\2\0\5\0΋\0\154\0" + "\224\0\234\0\4\0\132\0\6\0\26\0\2\0\6\0\2\0\46\0" + "\2\0\6\0\2\0\10\0\1\0\1\0\1\0\1\0\1\0\1\0" + "\1\0\37\0\2\0\65\0\1\0\7\0\1\0\1\0\3\0\3\0" + "\1\0\7\0\3\0\4\0\2\0\6\0\4\0\15\0\5\0\3\0" + "\1\0\7\0\102\0\2\0\23\0\1\0\34\0\1\0\15\0\1\0" + "\40\0\22\0\120\0\1\0\4\0\1\0\2\0\12\0\1\0\1\0" + "\3\0\5\0\6\0\1\0\1\0\1\0\1\0\1\0\1\0\4\0" + "\1\0\3\0\1\0\7\0\3\0\3\0\5\0\5\0\26\0\44\0" + "ກ\0\3\0\31\0\11\0\7\0\5\0\2\0\5\0\4\0\126\0" + "\6\0\3\0\1\0\137\0\5\0\50\0\4\0\136\0\21\0\30\0" + "\70\0\20\0Ȁ\0ᦶ\0\112\0冦\0\132\0ҍ\0ݳ\0⮤\0" + "⅜\0Į\0\2\0\73\0\225\0\7\0\14\0\5\0\5\0\1\0" + "\1\0\12\0\1\0\15\0\1\0\5\0\1\0\1\0\1\0\2\0" + "\1\0\2\0\1\0\154\0\41\0ū\0\22\0\100\0\2\0\66\0" + "\50\0\15\0\66\0\2\0\30\0\3\0\31\0\1\0\6\0\5\0" + "\1\0\207\0\7\0\1\0\34\0\32\0\4\0\1\0\1\0\32\0" + "\12\0\132\0\3\0\6\0\2\0\6\0\2\0\6\0\2\0\3\0" + "\3\0\2\0\3\0\2\0\31\0";

    /**
	 * Translates characters to character classes
	 */
    private static final char[] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

    /**
	 * Translates DFA states to action switch labels.
	 */
    private static final int[] ZZ_ACTION = zzUnpackAction();

    private static final String ZZ_ACTION_PACKED_0 = "\1\0\1\1\1\2\2\1\1\3\1\0\1\4";

    private static int[] zzUnpackAction() {
        int[] result = new int[8];
        int offset = 0;
        offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackAction(String packed, int offset, int[] result) {
        int i = 0;
        int j = offset;
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do {
                result[j++] = value;
            } while (--count > 0);
        }
        return j;
    }

    /**
	 * Translates a state to a row index in the transition table
	 */
    private static final int[] ZZ_ROWMAP = zzUnpackRowMap();

    private static final String ZZ_ROWMAP_PACKED_0 = "\0\0\0\6\0\14\0\22\0\30\0\6\0\22\0\6";

    private static int[] zzUnpackRowMap() {
        int[] result = new int[8];
        int offset = 0;
        offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackRowMap(String packed, int offset, int[] result) {
        int i = 0;
        int j = offset;
        int l = packed.length();
        while (i < l) {
            int high = packed.charAt(i++) << 16;
            result[j++] = high | packed.charAt(i++);
        }
        return j;
    }

    /**
	 * The transition table of the DFA
	 */
    private static final int[] ZZ_TRANS = zzUnpackTrans();

    private static final String ZZ_TRANS_PACKED_0 = "\1\2\1\3\1\4\1\2\1\5\1\6\7\0\1\3" + "\4\0\3\7\1\10\2\7\4\0\1\5\1\0";

    private static int[] zzUnpackTrans() {
        int[] result = new int[30];
        int offset = 0;
        offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackTrans(String packed, int offset, int[] result) {
        int i = 0;
        int j = offset;
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            value--;
            do {
                result[j++] = value;
            } while (--count > 0);
        }
        return j;
    }

    private static final int ZZ_UNKNOWN_ERROR = 0;

    private static final int ZZ_NO_MATCH = 1;

    private static final int ZZ_PUSHBACK_2BIG = 2;

    private static final String ZZ_ERROR_MSG[] = { "Unkown internal scanner error", "Error: could not match input", "Error: pushback value was too large" };

    /**
	 * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
	 */
    private static final int[] ZZ_ATTRIBUTE = zzUnpackAttribute();

    private static final String ZZ_ATTRIBUTE_PACKED_0 = "\1\0\1\11\3\1\1\11\1\0\1\11";

    private static int[] zzUnpackAttribute() {
        int[] result = new int[8];
        int offset = 0;
        offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackAttribute(String packed, int offset, int[] result) {
        int i = 0;
        int j = offset;
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do {
                result[j++] = value;
            } while (--count > 0);
        }
        return j;
    }

    /**
	 * the input device
	 */
    private java.io.Reader zzReader;

    /**
	 * the current state of the DFA
	 */
    private int zzState;

    /**
	 * the current lexical state
	 */
    private int zzLexicalState = YYINITIAL;

    /**
	 * this buffer contains the current text to be matched and is
	 * the source of the yytext() string
	 */
    private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

    /**
	 * the textposition at the last accepting state
	 */
    private int zzMarkedPos;

    /**
	 * the textposition at the last state to be included in yytext
	 */
    private int zzPushbackPos;

    /**
	 * the current text position in the buffer
	 */
    private int zzCurrentPos;

    /**
	 * startRead marks the beginning of the yytext() string in the buffer
	 */
    private int zzStartRead;

    /**
	 * endRead marks the last character in the buffer, that has been read
	 * from input
	 */
    private int zzEndRead;

    /**
	 * number of newlines encountered up to the start of the matched text
	 */
    private int yyline;

    /**
	 * the number of characters up to the start of the matched text
	 */
    private int yychar;

    /**
	 * the number of characters from the last newline up to the start of the
	 * matched text
	 */
    private int yycolumn;

    /**
	 * zzAtBOL == true <=> the scanner is currently at the beginning of a line
	 */
    private boolean zzAtBOL = true;

    /**
	 * zzAtEOF == true <=> the scanner is at the EOF
	 */
    private boolean zzAtEOF;

    /**
	 * denotes if the user-EOF-code has already been executed
	 */
    private boolean zzEOFDone;

    /**
	 * El nombre del thread para esta clase.
	 */
    public static final String name = Trimmer.class.toString();

    /**
	 * El logger para esta clase.
	 */
    Logger log = Logger.getLogger(Trimmer.class);

    /**
	 * En caso de utilizar este analizador léxico con el uso de cadenas exclusivamente, esta cadena contiene el resultado.
	 */
    protected StringWriter sw;

    /**
	 * Se encarga de escribir la salida.
	 */
    protected BufferedWriter out;

    /**
	 * Archivo de salida para <code>Trimmer</code>.
	 */
    private File outputFile;

    /**
	 * El archivo del cual se leerá.
	 */
    private String inputFile;

    /**
	 * Construye un nuevo analizador léxico <code>Trimmer</code> cuya entrada
	 * está definida por <code>in</code> y salida por <code>archivo</code>.
	 *
	 * @param in	  El lector de entrada para el analizador léxico.
	 * @param archivo El archivo de salida para el escáner.
	 *
	 * @deprecated Para cubrir todos los caracteres sin importar el encoding. Utilizar {@link mx.unam.fciencias.balpox.parser.Trimmer#Trimmer(String,String)}
	 */
    public Trimmer(Reader in, String archivo) {
        try {
            out = new BufferedWriter(new FileWriter(getOutput(archivo)));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        this.zzReader = in;
    }

    /**
	 * Crea un nuevo trimmer a partir de los respectivos archivos de entrada y de salida.
	 *
	 * @param fileIn  El archivo del cual se leerá.
	 * @param fileOut El archivo al que se escribirá.
	 *
	 * @throws FileNotFoundException Si <code>fileIn</code> no existe.
	 */
    public Trimmer(String fileIn, String fileOut) throws FileNotFoundException {
        try {
            File f = new File(fileIn);
            if (!f.exists()) {
                throw new FileNotFoundException("No existe el archivo: " + fileIn);
            }
            out = new BufferedWriter(new FileWriter(getOutput(fileOut)));
            this.zzReader = new FileReader(fileIn);
            inputFile = "file:///".concat(fileIn).replaceAll("\\\\", "/").replaceAll("\\s", "%20");
            log.info("inputFile=" + inputFile);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
	 * Constructor similar a {@link mx.unam.fciencias.balpox.parser.Trimmer#Trimmer(String)}, es decir,
	 * leerá, un archivo o el teclado, pero sacará el resultado a una cadena.
	 *
	 * @param in  El método de entrada.
	 * @param out El contenedor de salida.
	 */
    public Trimmer(Reader in, StringWriter out) {
        this.zzReader = in;
        this.out = new BufferedWriter(out);
        sw = out;
    }

    /**
	 * Constructor utilizado para manejar cadenas, y no archivos.
	 * <p><b>Para este constructor NO debe llamarse como Thread, ya que el método {@link #run()} realiza operaciones en archivo.</b><br>
	 * Debe utilizarse el método {@link #getTrimmedText()} que realiza dentro del mismo thread de ejecución el trabajo.</p>
	 *
	 * @param normalText La cadena a la que se le quitarán los espacios redundantes.
	 */
    public Trimmer(String normalText) {
        this.zzReader = new StringReader(normalText);
        sw = new StringWriter(2054);
        this.out = new BufferedWriter(sw);
    }

    /**
	 * Método que realiza el reconocimiento del texto y eliminación de espación tal como se ejecutaría concurrentemente en {@link #run()}.
	 *
	 * @return La <b>cadena</b> resultado de <code>trim</code>.
	 */
    public String getTrimmedText() {
        try {
            while (!zzAtEOF) {
                yylex();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        sw.flush();
        return sw.toString();
    }

    /**
	 * Preprocesa un texto HTML, dado que no simpre se tiene la disponibilidad de tener un
	 * transformador de HTML a texto. Este preprocesador sólo convierte HTML a texto.<br>
	 *
	 * @param uri El archivo, ya sea local o remoto, que se va a preprocesar.
	 *
	 * @return Una cadena que representa visualmente el HTML contenido en <code>uri</code>, sin tags.
	 */
    public String preProcessHTML(String uri) {
        final StringBuffer buf = new StringBuffer();
        try {
            HTMLDocument doc = new HTMLDocument() {

                public HTMLEditorKit.ParserCallback getReader(int pos) {
                    return new HTMLEditorKit.ParserCallback() {

                        public void handleText(char[] data, int pos) {
                            buf.append(data);
                            buf.append('\n');
                        }
                    };
                }
            };
            URL url = new URI(uri).toURL();
            URLConnection conn = url.openConnection();
            Reader rd = new InputStreamReader(conn.getInputStream());
            new ParserDelegator().parse(rd, doc.getReader(0), Boolean.TRUE);
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (URISyntaxException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return buf.toString();
    }

    /**
	 * Obtiene el archivo de salida para el escáner.
	 *
	 * @return El archivo al que este escáner escribirá.
	 */
    public File getOutputFile() {
        return outputFile;
    }

    /**
	 * Obtiene el archivo de entrada.
	 *
	 * @return El archivo del cual se está leyendo.
	 */
    public String getInputFile() {
        return inputFile;
    }

    /**
	 * Establece cuál es el archivo que debe leerse para su eliminación
	 * de espacios y tags HTML.<br>
	 * Se utiliza una simple expresión regular para la supresión de HTML:<br><br>
	 * <i><([^>]*></i><br><br>
	 * de este modo, todo lo encapsulado entre '<' y '>' (sin tomar en cuenta '>') será eliminado.
	 *
	 * @param file El archivo del cual se lee antes de eliminar tags HTML y dobles espacios.
	 *
	 * @return El nombre del archivo al que se escribe la salida.
	 */
    private File getOutput(String file) {
        outputFile = new File(file.substring(0, file.indexOf('.')).concat(PropertyManager.readProperty("file.preaet"))).getAbsoluteFile();
        log.info("Utilizando archivo de salida: " + outputFile.getAbsolutePath());
        return outputFile;
    }

    /**
	 * If this thread was constructed using a separate
	 * <code>Runnable</code> run object, then that
	 * <code>Runnable</code> object's <code>run</code> method is called;
	 * otherwise, this method does nothing and returns.
	 * <p/>
	 * Subclasses of <code>Thread</code> should override this method.
	 *
	 * @see Thread#start()
	 * @see Thread#stop()
	 * @see Thread#Thread(ThreadGroup,
	 *Runnable,String)
	 * @see Runnable#run()
	 * @see #preProcessHTML(String)
	 */
    @Override
    public void run() {
        ThreadEventManager.threadStarted(new ThreadEvent(this, Trimmer.name, System.currentTimeMillis()));
        if (getInputFile().indexOf("htm") != -1) {
            this.zzReader = new StringReader(preProcessHTML(getInputFile()));
        }
        try {
            while (!zzAtEOF) {
                yylex();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            ThreadEventManager.threadStopped(new ThreadEvent(this, Trimmer.name, System.currentTimeMillis()));
        }
    }

    /**
	 * Creates a new scanner
	 * There is also a java.io.InputStream version of this constructor.
	 *
	 * @param in the java.io.Reader to read input from.
	 */
    public Trimmer(java.io.Reader in) {
        try {
            out = new BufferedWriter(new FileWriter("salida.trimmer.txt"));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        this.zzReader = in;
    }

    /**
	 * Creates a new scanner.
	 * There is also java.io.Reader version of this constructor.
	 *
	 * @param in the java.io.Inputstream to read input from.
	 */
    public Trimmer(java.io.InputStream in) {
        this(new java.io.InputStreamReader(in));
    }

    /**
	 * Unpacks the compressed character translation table.
	 *
	 * @param packed the packed character translation table
	 *
	 * @return the unpacked character translation table
	 */
    private static char[] zzUnpackCMap(String packed) {
        char[] map = new char[0x10000];
        int i = 0;
        int j = 0;
        while (i < 1210) {
            int count = packed.charAt(i++);
            char value = packed.charAt(i++);
            do {
                map[j++] = value;
            } while (--count > 0);
        }
        return map;
    }

    /**
	 * Refills the input buffer.
	 *
	 * @return <code>false</code>, iff there was new input.
	 *
	 * @throws java.io.IOException if any I/O-Error occurs
	 */
    private boolean zzRefill() throws java.io.IOException {
        if (zzStartRead > 0) {
            System.arraycopy(zzBuffer, zzStartRead, zzBuffer, 0, zzEndRead - zzStartRead);
            zzEndRead -= zzStartRead;
            zzCurrentPos -= zzStartRead;
            zzMarkedPos -= zzStartRead;
            zzPushbackPos -= zzStartRead;
            zzStartRead = 0;
        }
        if (zzCurrentPos >= zzBuffer.length) {
            char newBuffer[] = new char[zzCurrentPos * 2];
            System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
            zzBuffer = newBuffer;
        }
        int numRead = zzReader.read(zzBuffer, zzEndRead, zzBuffer.length - zzEndRead);
        if (numRead < 0) {
            return true;
        } else {
            zzEndRead += numRead;
            return false;
        }
    }

    /**
	 * Closes the input stream.
	 */
    public final void yyclose() throws java.io.IOException {
        zzAtEOF = true;
        zzEndRead = zzStartRead;
        if (zzReader != null) {
            zzReader.close();
        }
    }

    /**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 * <p/>
	 * All internal variables are reset, the old input stream
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>ZZ_INITIAL</tt>.
	 *
	 * @param reader the new input stream
	 */
    public final void yyreset(java.io.Reader reader) {
        zzReader = reader;
        zzAtBOL = true;
        zzAtEOF = false;
        zzEndRead = zzStartRead = 0;
        zzCurrentPos = zzMarkedPos = zzPushbackPos = 0;
        yyline = yychar = yycolumn = 0;
        zzLexicalState = YYINITIAL;
    }

    /**
	 * Returns the current lexical state.
	 */
    public final int yystate() {
        return zzLexicalState;
    }

    /**
	 * Enters a new lexical state
	 *
	 * @param newState the new lexical state
	 */
    public final void yybegin(int newState) {
        zzLexicalState = newState;
    }

    /**
	 * Returns the text matched by the current regular expression.
	 */
    public final String yytext() {
        return new String(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
    }

    /**
	 * Returns the character at position <tt>pos</tt> from the
	 * matched text.
	 * <p/>
	 * It is equivalent to yytext().charAt(pos), but faster
	 *
	 * @param pos the position of the character to fetch.
	 *            A value from 0 to yylength()-1.
	 *
	 * @return the character at position pos
	 */
    public final char yycharat(int pos) {
        return zzBuffer[zzStartRead + pos];
    }

    /**
	 * Returns the length of the matched text region.
	 */
    public final int yylength() {
        return zzMarkedPos - zzStartRead;
    }

    /**
	 * Reports an error that occured while scanning.
	 * <p/>
	 * In a wellformed scanner (no or only correct usage of
	 * yypushback(int) and a match-all fallback rule) this method
	 * will only be called with things that "Can't Possibly Happen".
	 * If this method is called, something is seriously wrong
	 * (e.g. a JFlex bug producing a faulty scanner etc.).
	 * <p/>
	 * Usual syntax/scanner level error handling should be done
	 * in error fallback rules.
	 *
	 * @param errorCode the code of the errormessage to display
	 */
    private void zzScanError(int errorCode) {
        String message;
        try {
            message = ZZ_ERROR_MSG[errorCode];
        } catch (ArrayIndexOutOfBoundsException e) {
            message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
        }
        throw new Error(message);
    }

    /**
	 * Pushes the specified amount of characters back into the input stream.
	 * <p/>
	 * They will be read again by then next call of the scanning method
	 *
	 * @param number the number of characters to be read again.
	 *               This number must not be greater than yylength()!
	 */
    public void yypushback(int number) {
        if (number > yylength()) {
            zzScanError(ZZ_PUSHBACK_2BIG);
        }
        zzMarkedPos -= number;
    }

    /**
	 * Contains user EOF-code, which will be executed exactly once,
	 * when the end of file is reached
	 */
    private void zzDoEOF() {
        if (!zzEOFDone) {
            zzEOFDone = true;
            try {
                out.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
	 * Resumes scanning until the next regular expression is matched,
	 * the end of input is encountered or an I/O-Error occurs.
	 *
	 * @return the next token
	 *
	 * @throws java.io.IOException if any I/O-Error occurs
	 */
    public int yylex() throws java.io.IOException {
        int zzInput;
        int zzAction;
        int zzCurrentPosL;
        int zzMarkedPosL;
        int zzEndReadL = zzEndRead;
        char[] zzBufferL = zzBuffer;
        char[] zzCMapL = ZZ_CMAP;
        int[] zzTransL = ZZ_TRANS;
        int[] zzRowMapL = ZZ_ROWMAP;
        int[] zzAttrL = ZZ_ATTRIBUTE;
        while (true) {
            zzMarkedPosL = zzMarkedPos;
            boolean zzR = false;
            for (zzCurrentPosL = zzStartRead; zzCurrentPosL < zzMarkedPosL; zzCurrentPosL++) {
                switch(zzBufferL[zzCurrentPosL]) {
                    case '':
                    case '':
                    case '':
                    case ' ':
                    case ' ':
                        yyline++;
                        yycolumn = 0;
                        zzR = false;
                        break;
                    case '\r':
                        yyline++;
                        yycolumn = 0;
                        zzR = true;
                        break;
                    case '\n':
                        if (zzR) {
                            zzR = false;
                        } else {
                            yyline++;
                            yycolumn = 0;
                        }
                        break;
                    default:
                        zzR = false;
                        yycolumn++;
                }
            }
            if (zzR) {
                boolean zzPeek;
                if (zzMarkedPosL < zzEndReadL) {
                    zzPeek = zzBufferL[zzMarkedPosL] == '\n';
                } else if (zzAtEOF) {
                    zzPeek = false;
                } else {
                    boolean eof = zzRefill();
                    zzEndReadL = zzEndRead;
                    zzMarkedPosL = zzMarkedPos;
                    zzBufferL = zzBuffer;
                    if (eof) {
                        zzPeek = false;
                    } else {
                        zzPeek = zzBufferL[zzMarkedPosL] == '\n';
                    }
                }
                if (zzPeek) {
                    yyline--;
                }
            }
            zzAction = -1;
            zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
            zzState = zzLexicalState;
            zzForAction: {
                while (true) {
                    if (zzCurrentPosL < zzEndReadL) {
                        zzInput = zzBufferL[zzCurrentPosL++];
                    } else if (zzAtEOF) {
                        zzInput = YYEOF;
                        break zzForAction;
                    } else {
                        zzCurrentPos = zzCurrentPosL;
                        zzMarkedPos = zzMarkedPosL;
                        boolean eof = zzRefill();
                        zzCurrentPosL = zzCurrentPos;
                        zzMarkedPosL = zzMarkedPos;
                        zzBufferL = zzBuffer;
                        zzEndReadL = zzEndRead;
                        if (eof) {
                            zzInput = YYEOF;
                            break zzForAction;
                        } else {
                            zzInput = zzBufferL[zzCurrentPosL++];
                        }
                    }
                    int zzNext = zzTransL[zzRowMapL[zzState] + zzCMapL[zzInput]];
                    if (zzNext == -1) {
                        break zzForAction;
                    }
                    zzState = zzNext;
                    int zzAttributes = zzAttrL[zzState];
                    if ((zzAttributes & 1) == 1) {
                        zzAction = zzState;
                        zzMarkedPosL = zzCurrentPosL;
                        if ((zzAttributes & 8) == 8) {
                            break zzForAction;
                        }
                    }
                }
            }
            zzMarkedPos = zzMarkedPosL;
            switch(zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
                case 4:
                    {
                    }
                case 5:
                    break;
                case 1:
                    {
                        out.write(yytext());
                    }
                case 6:
                    break;
                case 2:
                    {
                        out.write(" ");
                    }
                case 7:
                    break;
                case 3:
                    {
                    }
                case 8:
                    break;
                default:
                    if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
                        zzAtEOF = true;
                        zzDoEOF();
                        return YYEOF;
                    } else {
                        zzScanError(ZZ_NO_MATCH);
                    }
            }
        }
    }

    /**
	 * Runs the scanner on input files.
	 * <p/>
	 * This is a standalone scanner, it will print any unmatched
	 * text to System.out unchanged.
	 *
	 * @param argv the command line, contains the filenames to run
	 *             the scanner on.
	 */
    public static void main(String argv[]) {
        if (argv.length == 0) {
            System.out.println("Usage : java Trimmer <inputfile>");
        } else {
            for (int i = 0; i < argv.length; i++) {
                Trimmer scanner = null;
                try {
                    scanner = new Trimmer(new java.io.FileReader(argv[i]));
                    while (!scanner.zzAtEOF) {
                        scanner.yylex();
                    }
                } catch (java.io.FileNotFoundException e) {
                    System.out.println("File not found : \"" + argv[i] + "\"");
                } catch (java.io.IOException e) {
                    System.out.println("IO error scanning file \"" + argv[i] + "\"");
                    System.out.println(e);
                } catch (Exception e) {
                    System.out.println("Unexpected exception:");
                    e.printStackTrace();
                }
            }
        }
    }
}
