package org.openexi.scomp;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.xml.sax.*;
import org.openexi.schema.EXISchema;

public class EXISchemaFactoryTestUtil {

    public static EXISchema getEXISchema(Class<?> cls, EXISchemaFactoryErrorHandler compilerErrorHandler) throws IOException, ClassNotFoundException, EXISchemaFactoryException {
        return getEXISchema(null, cls, compilerErrorHandler);
    }

    /**
   * Loads schema then compiles it into SchemaCorpus.
   * Schema file is resolved relative to the specified class.
   */
    static EXISchema getEXISchema(String fileName, Class<?> cls) throws IOException, ClassNotFoundException, EXISchemaFactoryException {
        return getEXISchema(fileName, cls, new EXISchemaFactoryErrorMonitor(true));
    }

    public static EXISchema getEXISchema(String fileName, Class<?> cls, EXISchemaFactoryErrorHandler compilerErrorHandler) throws IOException, ClassNotFoundException, EXISchemaFactoryException {
        EXISchemaFactory schemaCompiler = new EXISchemaFactory();
        schemaCompiler.setCompilerErrorHandler(compilerErrorHandler);
        InputSource inputSource = null;
        if (fileName != null) {
            URL url;
            if ((url = cls.getResource(fileName)) != null) {
                inputSource = new InputSource(url.openStream());
                inputSource.setSystemId(url.toString());
            } else throw new RuntimeException("File '" + fileName + "' not found.");
        }
        EXISchema compiled = schemaCompiler.compile(inputSource);
        InputStream serialized = serializeSchema(compiled);
        return loadSchema(serialized);
    }

    private static EXISchema loadSchema(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = null;
        EXISchema schema = null;
        try {
            ois = new ObjectInputStream(is);
            schema = (EXISchema) ois.readObject();
        } finally {
            if (ois != null) ois.close();
        }
        return schema;
    }

    private static InputStream serializeSchema(EXISchema schema) throws IOException {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        InputStream is = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(schema);
            oos.flush();
            bos.flush();
            byte[] data = bos.toByteArray();
            is = new ByteArrayInputStream(data);
        } finally {
            if (oos != null) oos.close();
            if (bos != null) bos.close();
        }
        return is;
    }

    /**
   * Serialize bytes to a file.
   * @param bts
   * @param baseFileName a file that exists in the destination
   * @param fileName name of the file the bytes are output to
   * @param cls
   */
    public static void serializeBytes(byte[] bts, String baseFileName, String fileName, Class<?> cls) throws IOException, URISyntaxException {
        URI uri = cls.getResource(baseFileName).toURI();
        uri = uri.resolve(fileName);
        FileOutputStream fos;
        fos = new FileOutputStream(uri.toURL().getFile());
        fos.write(bts);
        fos.close();
    }
}
