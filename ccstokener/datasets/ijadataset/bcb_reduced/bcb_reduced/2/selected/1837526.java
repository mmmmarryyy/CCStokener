package org.openexi.fujitsu.scomp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import junit.framework.Assert;
import org.openexi.fujitsu.schema.EXISchema;
import org.xml.sax.InputSource;

public final class CompileSchemas {

    private static final String OPTIONS_SHEMA_INSTANCE = "HeaderOptions.xsd";

    private static final String OPTIONS_SCHEMA_COMPILED = "HeaderOptions.xsc";

    private static final String EMPTY_SCHEMA_COMPILED = "EmptySchema.xsc";

    public static void main(String args[]) throws IOException {
        new CompileSchemas().compileOptionsSchema();
        new CompileSchemas().compileEmptySchema();
    }

    private void compileOptionsSchema() throws IOException {
        EXISchema corpus = null;
        try {
            EXISchemaFactoryErrorMonitor compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
            EXISchemaFactory schemaFactory = new EXISchemaFactory();
            schemaFactory.setCompilerErrorHandler(compilerErrorHandler);
            InputSource inputSource;
            URL url = CompileSchemas.class.getResource(OPTIONS_SHEMA_INSTANCE);
            inputSource = new InputSource(url.openStream());
            inputSource.setSystemId(url.toString());
            corpus = schemaFactory.compile(inputSource);
            Assert.assertEquals(0, compilerErrorHandler.getTotalCount());
            Assert.assertNotNull(corpus);
        } catch (Exception exc) {
            Assert.fail("Failed to compile EXI Header Options schema.");
        }
        URL xbrlSchemaURI = CompileSchemas.class.getResource(OPTIONS_SHEMA_INSTANCE);
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        URL url = new URL(xbrlSchemaURI, OPTIONS_SCHEMA_COMPILED);
        try {
            fos = new FileOutputStream(url.getFile());
            oos = new ObjectOutputStream(fos);
            oos.writeObject(corpus);
            oos.flush();
            fos.flush();
        } finally {
            if (oos != null) oos.close();
            if (fos != null) fos.close();
        }
    }

    private void compileEmptySchema() throws IOException {
        EXISchema corpus = null;
        try {
            EXISchemaFactoryErrorMonitor compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
            EXISchemaFactory schemaFactory = new EXISchemaFactory();
            schemaFactory.setCompilerErrorHandler(compilerErrorHandler);
            corpus = schemaFactory.compile();
            Assert.assertEquals(0, compilerErrorHandler.getTotalCount());
            Assert.assertNotNull(corpus);
        } catch (Exception exc) {
            Assert.fail("Failed to compile the empty schema.");
        }
        URL xbrlSchemaURI = CompileSchemas.class.getResource(OPTIONS_SHEMA_INSTANCE);
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        URL url = new URL(xbrlSchemaURI, EMPTY_SCHEMA_COMPILED);
        try {
            fos = new FileOutputStream(url.getFile());
            oos = new ObjectOutputStream(fos);
            oos.writeObject(corpus);
            oos.flush();
            fos.flush();
        } finally {
            if (oos != null) oos.close();
            if (fos != null) fos.close();
        }
    }
}
