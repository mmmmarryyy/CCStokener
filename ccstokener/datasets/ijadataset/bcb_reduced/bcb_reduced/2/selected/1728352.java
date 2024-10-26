package net.sf.ajio.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.sf.ajio.util.ClassPath;
import org.apache.log4j.Logger;

/**
 * AJIO
 * 
 * @author Olivier CHABROL  olivierchabrol@users.sourceforge.net
 * @copyright (C)2004 Olivier CHABROL
 */
public class ClassParser {

    public static final int UTF8 = 1;

    public static final int INTEGER = 3;

    public static final int FLOAT = 4;

    public static final int LONG = 5;

    public static final int DOUBLE = 6;

    public static final int CLASS = 7;

    public static final int STRING = 8;

    public static final int FIELDREF = 9;

    public static final int METHODREF = 10;

    public static final int INTERFACEREF = 11;

    public static final int NAMETYPE = 12;

    private ArrayList _liste = new ArrayList();

    private ArrayList _classList = new ArrayList();

    private HashMap _mapIdent = new HashMap();

    private DataInputStream _dataInputStream = null;

    private static Logger _log = Logger.getLogger(ClassParser.class.getName());

    /**
     * Parse un fichier
     * @param file fichier a parser 
     */
    private void parse(File file) {
        try {
            if (file.getName().endsWith(".class")) {
                try {
                    parse(new FileInputStream(file), (int) file.length());
                } catch (FileNotFoundException eFNFE) {
                    _log.error("File  " + file.toString() + " not found");
                }
            } else {
                _log.error(file.toString() + " does not have .class extension ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Renvoie la liste des d�pendences d'une class, cette  m�thode fait appel
     * au ClassPath pour d�terminer dans quelle jar ou r�pertoire est issue la 
     * classe 
     * @param className nom de la classe
     * @return liste des  d�pendances
     */
    public static String[] getImportedClass(String className) {
        ClassParser cp = new ClassParser();
        String path = (String) ClassPath.getInstance().getMap().get(className);
        if (path == null) {
            _log.error("Impossible de d�terminer le chemin de la classe " + className);
            return null;
        }
        if (path.endsWith(".jar")) cp.parseJar(path, className); else cp.parse(new File(path + File.separatorChar + className + ".class"));
        return cp.getImportedClasses();
    }

    /**
     * Parse un .class dans un jar
     * @param jarPath URL du jar
     * @param filePath nom de la class 
     */
    private void parseJar(String jarPath, String filePath) {
        String filePathBuf = null;
        if (filePath.endsWith(".class")) filePathBuf = filePath; else filePathBuf = filePath + ".class";
        if (jarPath == null) {
            _log.error("Impossible de d�terminer le path de " + filePath);
            return;
        }
        try {
            filePathBuf = changeToJarSeparator(filePathBuf);
            URL url = new URL("jar:file:/" + jarPath + "!/" + filePathBuf);
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            JarFile jarfile = conn.getJarFile();
            JarEntry jarEntry = jarfile.getJarEntry(filePathBuf);
            parse(jarfile.getInputStream(jarEntry), (int) jarEntry.getSize());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Change tout les '\'  en '/' dans une chaine de caract�re
     * @param value chaine d'entr�e
     * @return chaine de sortie
     */
    public static String changeToJarSeparator(String value) {
        if (value == null) return null;
        if (value.indexOf('\\') != -1) value = value.replace('\\', '/');
        return value;
    }

    /**
     * Parse une classe sous forme d'un inputStream
     * @param is classe sous forme d'un inputStream
     * @param length taille
     * @throws IOException Erreur de lecture du flux
     */
    private void parse(InputStream is, int length) throws IOException {
        ByteArrayInputStream bais = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
        byte[] buf = new byte[length];
        int nread;
        while ((nread = is.read(buf, 0, buf.length)) > 0) {
            baos.write(buf, 0, nread);
        }
        bais = new ByteArrayInputStream(baos.toByteArray());
        parse(bais);
    }

    /**
     * Parse une classe
     * @param is Flux d'entr�e correspondant � une classe
     * @throws IOException Erreur de lecture du flux
     */
    private void parse(ByteArrayInputStream is) throws IOException {
        _dataInputStream = new DataInputStream(is);
        _dataInputStream.readInt();
        ;
        _dataInputStream.readShort();
        _dataInputStream.readShort();
        readConstantPool();
        _dataInputStream.readShort();
        _dataInputStream.readShort();
        _dataInputStream.readShort();
        readInterfaces();
    }

    private void readInterfaces() throws IOException {
        short count = _dataInputStream.readShort();
        for (int i = 0; i < count; i++) {
            _dataInputStream.readShort();
        }
    }

    /**
     * Lecture de la partie de d�finition des constantes d�clar�es dans la 
     * class
     * @throws IOException Erreur de lecture
     */
    private void readConstantPool() throws IOException {
        int numCpEntry = _dataInputStream.readShort();
        for (int i = 1; i < numCpEntry; i++) {
            byte tagByte = _dataInputStream.readByte();
            switch(tagByte) {
                case UTF8:
                    String utfString = _dataInputStream.readUTF();
                    _mapIdent.put(new Integer(i), utfString);
                    _liste.add(utfString);
                    break;
                case INTEGER:
                    _dataInputStream.readInt();
                    break;
                case FLOAT:
                    _dataInputStream.readFloat();
                    break;
                case LONG:
                    _dataInputStream.readLong();
                    i++;
                    break;
                case DOUBLE:
                    _dataInputStream.readDouble();
                    i++;
                    break;
                case CLASS:
                    short classe = _dataInputStream.readShort();
                    _classList.add(new Integer(classe));
                    break;
                case FIELDREF:
                    readTagFieldRef(tagByte);
                    break;
                case METHODREF:
                    _dataInputStream.readShort();
                    _dataInputStream.readShort();
                    break;
                case INTERFACEREF:
                    _dataInputStream.readShort();
                    _dataInputStream.readShort();
                    break;
                case NAMETYPE:
                    _dataInputStream.readShort();
                    short shorte = _dataInputStream.readShort();
                    break;
                default:
                    _dataInputStream.readShort();
                    break;
            }
        }
    }

    /**
     * Renvoie la liste des d�pendences d'une  classe
     * @return Tableau de String correspondant � la liste des d�pendences 
     * d'une  classe
     * @todo voir le pb des tableau
     */
    private String[] getImportedClasses() {
        ArrayList buf = new ArrayList();
        String[] liste = null;
        int taille = _classList.size();
        for (int i = 0; i < taille; i++) {
            String str = (String) _mapIdent.get(_classList.get(i));
            if (str.indexOf("[") == -1) buf.add(str);
        }
        liste = (String[]) buf.toArray(new String[] {});
        Arrays.sort(liste, String.CASE_INSENSITIVE_ORDER);
        return liste;
    }

    /**
     * Affiche dans le logger la liste des class de d�pendances
     *
     */
    public void affiche() {
        String[] liste = new String[_classList.size()];
        for (int i = 0; i < liste.length; i++) {
            liste[i] = (String) _mapIdent.get(_classList.get(i));
        }
        Arrays.sort(liste, String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < liste.length; i++) _log.debug(i + " -> " + liste[i]);
    }

    /**
     * Lit le tag d'une classe � une position donn�e
     * @param aIndex position du tag
     * @throws IOException Erreur de lecture
     */
    private void readTagFieldRef(int aIndex) throws IOException {
        int classIndex = _dataInputStream.readShort();
        int nameType = _dataInputStream.readShort();
    }
}
