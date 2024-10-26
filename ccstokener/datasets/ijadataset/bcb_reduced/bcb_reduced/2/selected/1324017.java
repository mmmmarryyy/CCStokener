package arme.core.video.impl.ipvideo;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import arme.core.video.ImageSource;
import arme.core.video.ImageSourceException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;

/**
 * Esta clase es una fuente de im&aacute;genes concreta, la cual toma el flujo de
 * imagenes de un broadcast que se encuentre en una direcci&oacute;n IP. Soporta
 * Streaming MJPG, y captura de imagenes independientes JPG.
 *
 * @author Jonny Alexander V&eacute;lez
 * @author Juli&aacute;n Alejandro Lamprea
 *
 * @version 1.0.0
 */
public class IPVideo implements ImageSource {

    private static final String CONFIG_LOAD_ERROR = "Error cargando la configuracion de video";

    private static final String CONNECT_ERROR = "Error conectando con el URL de video";

    private static final String DISCONNECT_ERROR = "Problema al desconectarse del flujo de video";

    private static final String ERROR_FLUJO_DATOS = "Error leyendo el flujo de datos";

    private static final String ERROR_DECODIFICANDO_IMAGEN = "Error decodificando la imagen";

    /**
	 * URL de conexi&oacute;n a la fuente de video
	 */
    private String url;

    /**
	 * Variable de estado que determina si el flujo de video es de tipo MJPG
	 */
    private boolean mjpgMode;

    /**
	 * Variable de estado que determina si la entrada de video debe ser
	 * reescalada
	 */
    private boolean scaleInput;

    /**
	 * Ancho de las im&aacute;genes del flujo de video IP
	 */
    private int width;

    /**
	 * Altura de las im&aacute;genes del flujo de video IP
	 */
    private int height;

    /**
	 * Atributo de la clase, referencia a la &uacute;ltima imagen obtenida
	 */
    private BufferedImage image;

    /**
	 * Variable de estado, para indicar si hay conexi&oacute;n con una fuente de video
	 */
    private boolean connected;

    /**
	 * Flujo de lectura de datos.
	 */
    private DataInputStream dataInputStream;

    /**
	 * Esta funci&oacute;n cierra la fuente de im&aacute;genes.
	 *
	 * @throws ImageSourceException
	 *             Error generado por la fuente de im&aacute;genes.
	 */
    public void close() throws ImageSourceException {
        disconnect();
    }

    /**
	 * Obtiene la altura de las im&aacute;genes devueltas por esta fuente.
	 *
	 * @return Altura de la imagen
	 * @throws ImageSourceException
	 *             Error generado por la fuente de imagenes.
	 */
    public int getHeight() throws ImageSourceException {
        return height;
    }

    /**
	 * Esta funcion obtiene la siguiente imagen del flujo de la fuente.
	 *
	 * @param buffer
	 *            Flujo de bytes de la imagen.
	 * @throws ImageSourceException
	 *             Error generado por la fuente de im&aacute;genes.
	 */
    public void getImage(int[] buffer) throws ImageSourceException {
        readStream();
        if (scaleInput) image.getGraphics().drawImage(image, 0, 0, width, height, null);
        image.getData().getDataElements(0, 0, width, height, buffer);
    }

    /**
	 * Obtiene el ancho de las imagenes devueltas por esta fuente.
	 *
	 * @return Ancho de la imagen
	 * @throws ImageSourceException
	 *             Error generado por la fuente de im&aacute;genes.
	 */
    public int getWidth() throws ImageSourceException {
        return width;
    }

    /**
	 * Esta funci&oacute;n abre la fuente de imagenes.
	 *
	 * @param config
	 *            Archivo o cadena de configuraci&oacute;n si este lo soporta.
	 * @throws ImageSourceException
	 *             Error generado por la fuente de im&aacute;genes.
	 */
    public void open(String config) throws ImageSourceException {
        loadConfiguration(config);
        connect();
        if (scaleInput == false) {
            readStream();
            width = image.getWidth(null);
            height = image.getHeight(null);
        }
    }

    /**
	 * Esta funci&oacute;n realiza la conexi&oacute;n con el URL fuente de video
	 *
	 * @throws ImageSourceException
	 *             Si ocurre un error de conexi&oacute;n
	 */
    private void connect() throws ImageSourceException {
        try {
            URL url = new URL(this.url);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            dataInputStream = new DataInputStream(new BufferedInputStream(huc.getInputStream()));
            connected = true;
        } catch (IOException e) {
            throw new ImageSourceException(CONNECT_ERROR, e);
        }
    }

    /**
	 * Esta funci&oacute;n se desconecta del URL fuente de video
	 *
	 * @throws ImageSourceException
	 *             Si ocurre un error de conexi&oacute;n
	 */
    private void disconnect() throws ImageSourceException {
        try {
            if (connected) {
                dataInputStream.close();
                connected = false;
            }
        } catch (Exception e) {
            throw new ImageSourceException(DISCONNECT_ERROR, e);
        }
    }

    /**
	 * Funci&oacute;n gen&eacute;rica de lectura, llama a la funci&oacute;n espec&iacute;fica de lectura
	 * seg&uacute;n el modo de Stream, MJPG o JPG.
	 *
	 * @throws ImageSourceException
	 *             Si se genera un error en la lectura de la imagen
	 */
    private void readStream() throws ImageSourceException {
        if (mjpgMode) {
            readMJPG();
        } else {
            if (connected == false) try {
                connect();
            } catch (ImageSourceException e) {
            }
            readJPG();
            try {
                disconnect();
            } catch (ImageSourceException e) {
            }
        }
    }

    /**
	 * Esta funci&oacute;n lee el siguiente frame del stream de imagenes. Preprocesa el
	 * MJPG, para retirar el encabezado de encapsulamiento y lee la imagen.
	 *
	 * @throws ImageSourceException
	 *             Si ocurre un error leyendo del stream de datos
	 */
    private void readMJPG() throws ImageSourceException {
        readLine(4, dataInputStream);
        readJPG();
        readLine(1, dataInputStream);
    }

    /**
	 * Esta funci&oacute;n lee la siguiente imagen de la fuente.
	 *
	 * @throws ImageSourceException
	 *             Si ocurre un error en la decodificaci&oacute;n del flujo de datos
	 *             como una imagen
	 */
    private void readJPG() throws ImageSourceException {
        try {
            JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(dataInputStream);
            image = decoder.decodeAsBufferedImage();
        } catch (Exception e) {
            try {
                disconnect();
            } catch (ImageSourceException e1) {
            }
            throw new ImageSourceException(ERROR_DECODIFICANDO_IMAGEN, e);
        }
    }

    /**
	 * Funci&oacute;n para leer una cantidad de l&iacute;neas del flujo de bytes de la imagen.
	 * Se usa para el modo MJPG.
	 *
	 * @param n
	 * @param dis
	 * @throws ImageSourceException
	 */
    private void readLine(int n, DataInputStream dis) throws ImageSourceException {
        for (int i = 0; i < n; i++) {
            readLine(dis);
        }
    }

    /**
	 * Lee una &uacute;nica linea del flujo de datos de entrada.
	 *
	 * @param dis
	 *            Flujo de datos con la fuente de video
	 * @throws ImageSourceException
	 *             Si se presenta un error leyendo del flujo de datos
	 */
    private void readLine(DataInputStream dis) throws ImageSourceException {
        try {
            boolean end = false;
            String lineEnd = "\n";
            byte[] lineEndBytes = lineEnd.getBytes();
            byte[] byteBuf = new byte[lineEndBytes.length];
            while (!end) {
                String t = "";
                if (byteBuf != null) {
                    dis.read(byteBuf, 0, lineEndBytes.length);
                    t = new String(byteBuf);
                }
                if (t.equals(lineEnd)) end = true;
            }
        } catch (Exception e) {
            throw new ImageSourceException(ERROR_FLUJO_DATOS, e);
        }
    }

    /**
	 * Carga los parametros de configuraci&oacute;n del Video IP, a partir de un
	 * archivo o configuraci&oacute;n en formato XML.
	 *
	 * @param config
	 *            Archivo o cadena XML de configuraci&oacute;n
	 * @throws ImageSourceException
	 *             Si ocurre un error en la lectura o interpretaci&oacute;n del XML de
	 *             configuraci&oacute;n
	 */
    private void loadConfiguration(String config) throws ImageSourceException {
        XMLConfigParser xmlConfig = new XMLDefaultConfigParser();
        try {
            xmlConfig.parseConfigFile(config);
        } catch (XMLConfigParserException e) {
            throw new ImageSourceException(CONFIG_LOAD_ERROR, e);
        }
        scaleInput = false;
        url = xmlConfig.getURLValue();
        mjpgMode = xmlConfig.isMJPGMode();
        if (xmlConfig.hasInputResolution()) {
            scaleInput = true;
            width = xmlConfig.getInputWidth();
            height = xmlConfig.getInputHeight();
        }
    }
}
