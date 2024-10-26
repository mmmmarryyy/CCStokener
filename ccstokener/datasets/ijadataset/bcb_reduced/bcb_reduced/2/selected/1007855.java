package com.ssg.tools.jsonxml.json.schema;

import com.ssg.tools.jsonxml.Comparer;
import com.ssg.tools.jsonxml.Comparer.COMPARE_STATUS;
import com.ssg.tools.jsonxml.Comparer.ComparatorContext;
import com.ssg.tools.jsonxml.Comparer.ComparatorPair;
import com.ssg.tools.jsonxml.Formatter;
import com.ssg.tools.jsonxml.common.Utilities;
import com.ssg.tools.jsonxml.common.Utilities.SizeInfo;
import com.ssg.tools.jsonxml.common.ValidationError;
import com.ssg.tools.jsonxml.common.tools.BufferingReader;
import com.ssg.tools.jsonxml.common.tools.P;
import com.ssg.tools.jsonxml.json.JSONParserLite;
import com.ssg.tools.jsonxml.common.tools.URLResolver.CacheingURLResolver;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;

/**
 *
 * @author ssg
 */
public class JSONSchemaTest extends TestCase {

    public JSONSchemaTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of load method, of class JSONSchema.
     */
    public void testLoad() throws Exception {
        System.out.println("load");
        Reader r = new StringReader("{\"type\": \"object\"}");
        JSONSchema instance = new JSONSchema();
        JSchema expResult = new JSchema();
        expResult.setType(new JSONValueType[] { JSONValueType.OBJECT });
        JSchema result = instance.load(new StreamSource(r));
        assertEquals(expResult, result);
        r = new StringReader("{\"type\": \"object\", \"name\": \"test\"}");
        instance = new JSONSchema();
        expResult = new JSchema();
        expResult.setName("test");
        expResult.setType(new JSONValueType[] { JSONValueType.OBJECT });
        result = instance.load(new StreamSource(r));
        assertEquals(expResult, result);
        r = new StringReader("{\"type\": \"object\", \"name\": \"test\", \"undefinedElement\": 22343}");
        instance = new JSONSchema();
        expResult = new JSchema();
        expResult.setName("test");
        expResult.setType(new JSONValueType[] { JSONValueType.OBJECT });
        result = instance.load(new StreamSource(r));
        assertEquals(expResult, result);
        try {
            r = new StringReader("{\"type\": \"object\", \"name\": \"test\", \"undefinedElement\": }");
            result = instance.load(new StreamSource(r));
            fail("Wrong schema (no value after property name). MUST throw exception.");
        } catch (IOException ioex) {
        }
        try {
            r = new StringReader("{\"name\": \"test\", \"undefinedElement\": 222}");
            result = instance.load(new StreamSource(r));
            fail("Wrong schema (schema properties, but something is set). MUST throw exception.");
        } catch (IOException ioex) {
        }
        try {
            r = new StringReader("[]");
            result = instance.load(new StreamSource(r));
            fail("Wrong schema (schema MUST be object, but got list). MUST throw exception.");
        } catch (IOException ioex) {
        }
    }

    /**
     * Test of loadRefs method, of class JSONSchema.
     */
    public void testLoadRefs() throws Exception {
        System.out.println("loadRefs");
        JSONSchema instance = new JSONSchema();
        instance.loadRefs(null);
    }

    /**
     * Test of save method, of class JSONSchema.
     */
    public void testSave_Writer() throws Exception {
        System.out.println("save");
        String schema = "{\"name\":\"test\",\"type\":\"object\"}";
        JSONSchema instance = new JSONSchema();
        JSchema result = instance.load(new StreamSource(new StringReader(schema)));
        instance.setSchema(result);
        StringWriter sw = new StringWriter();
        instance.save(sw);
        assertEquals(schema, sw.toString());
    }

    /**
     * Test of save method, of class JSONSchema.
     */
    public void testSave_Writer_boolean() throws Exception {
        System.out.println("save");
        String schema = "{\"name\":\"test\",\"type\":\"object\"}";
        JSONSchema instance = new JSONSchema();
        JSchema result = instance.load(new StreamSource(new StringReader(schema)));
        instance.setSchema(result);
        StringWriter sw = new StringWriter();
        instance.save(sw, false);
        assertEquals(schema, sw.toString());
        sw = new StringWriter();
        instance.save(sw, true);
        assertTrue(schema.length() < sw.toString().length());
    }

    /**
     * Test of validate method, of class JSONSchema.
     */
    public void testValidate_Object() throws Exception {
        System.out.println("validate");
        String schema = "{\"name\":\"test\",\"type\":\"object\",\"properties\":{\"aaa\":{\"type\":\"string\"}}}";
        JSONSchema instance = new JSONSchema();
        JSchema result = instance.load(new StreamSource(new StringReader(schema)));
        instance.setSchema(result);
        Map data = new LinkedHashMap();
        assertTrue(instance.validate(data));
        data.put("aaa", "aa");
        assertTrue(instance.validate(data));
        data.put("aaa", 1);
        assertFalse(instance.validate(data));
    }

    /**
     * Test of validate method, of class JSONSchema.
     */
    public void testValidate_JSONValidationContext_Object() throws Exception {
        System.out.println("validate");
        JSONValidationContext ctx = new JSONValidationContext();
        String schema = "{\"name\":\"test\",\"type\":\"object\",\"properties\":{\"aaa\":{\"type\":\"string\"}}}";
        JSONSchema instance = new JSONSchema();
        JSchema result = instance.load(new StreamSource(new StringReader(schema)));
        instance.setSchema(result);
        Map data = new LinkedHashMap();
        assertTrue(instance.validate(ctx, data));
        assertEquals(0, ctx.getValidationErrors().size());
        data.put("aaa", "aa");
        assertTrue(instance.validate(ctx, data));
        assertEquals(0, ctx.getValidationErrors().size());
        data.put("aaa", 1);
        assertFalse(instance.validate(ctx, data));
        assertEquals(1, ctx.getValidationErrors().size());
        instance.setSchema(null);
        assertFalse(instance.validate(ctx, data));
    }

    /**
     * Test of validate method, of class JSONSchema.
     */
    public void testValidate_4args() throws Exception {
        System.out.println("validate");
        JSONValidationContext ctx = new JSONValidationContext();
        String schema = "{\"name\":\"test\",\"type\":\"object\",\"properties\":{\"aaa\":{\"type\":\"string\"}}}";
        JSONSchema instance = new JSONSchema();
        JSchema result = instance.load(new StreamSource(new StringReader(schema)));
        instance.setSchema(result);
        Map data = new LinkedHashMap();
        assertTrue(instance.validate(ctx, instance.getSchema(), data, true));
        assertEquals(0, ctx.getValidationErrors().size());
        data.put("aaa", "aa");
        assertTrue(instance.validate(ctx, instance.getSchema(), data, true));
        assertEquals(0, ctx.getValidationErrors().size());
        data.put("aaa", 1);
        assertFalse(instance.validate(ctx, instance.getSchema(), data, true));
        assertEquals(1, ctx.getValidationErrors().size());
    }

    /**
     * Test of getSchema method, of class JSONSchema.
     */
    public void testGetSchema() throws Exception {
        System.out.println("getSchema");
        String schema = "{\"name\":\"test\",\"type\":\"object\",\"properties\":{\"aaa\":{\"type\":\"string\"}}}";
        JSONSchema instance = new JSONSchema(new StringReader(schema));
        JSchema expResult = instance.load(new StreamSource(new StringReader(schema)));
        JSchema result = instance.getSchema();
        assertEquals(expResult, result);
    }

    /**
     * Test of setSchema method, of class JSONSchema.
     */
    public void testSetSchema() throws Exception {
        System.out.println("setSchema");
        String schema = "{\"name\":\"test\",\"type\":\"object\",\"properties\":{\"aaa\":{\"type\":\"string\"}}}";
        JSONSchema instance = new JSONSchema();
        JSchema expResult = instance.load(new StreamSource(new StringReader(schema)));
        instance.setSchema(expResult);
        JSchema result = instance.getSchema();
        assertEquals(expResult, result);
    }

    /**
     * Test various construction methods...
     * @throws Exception 
     */
    public void testConstructors() throws Exception {
        System.out.println("test constructors");
        File f = new File("./src/test/java/SampleSchema.json");
        File f2 = new File("./src/test/java/SampleSchema.json.none");
        String schema = "{\"name\":\"test\",\"type\":\"object\",\"properties\":{\"aaa\":{\"type\":\"string\"}}}";
        String schema2 = "{\"name\":\"test\",\"type\":\"object\",\"properties\":{\"aaa\":{\"type\":\"string\"}}";
        JSONSchema file = new JSONSchema(f);
        JSONSchema fileI = new JSONSchema(f, "ISO8859_1");
        JSONSchema uri = new JSONSchema(f.toURI());
        JSONSchema uriI = new JSONSchema(f.toURI(), "ISO8859_1");
        JSONSchema url = new JSONSchema(f.toURI().toURL());
        JSONSchema urlI = new JSONSchema(f.toURI().toURL(), "ISO8859_1");
        JSONSchema stream = new JSONSchema(f.toURI().toURL().openStream());
        JSONSchema streamI = new JSONSchema(f.toURI().toURL().openStream(), "ISO8859_1");
        assertEquals(file.getSchema(), fileI.getSchema());
        assertEquals(uri.getSchema(), uriI.getSchema());
        assertEquals(url.getSchema(), urlI.getSchema());
        assertEquals(stream.getSchema(), streamI.getSchema());
        assertEquals(file.getSchema(), uri.getSchema());
        assertEquals(file.getSchema(), url.getSchema());
        assertEquals(file.getSchema(), stream.getSchema());
        JSONSchema string = new JSONSchema(schema);
        Map<URI, JSchema> deps = new LinkedHashMap<URI, JSchema>();
        JSONSchema readerDeps = new JSONSchema(new StringReader(schema), deps);
        JSONSchema readerDepsN = new JSONSchema(new StringReader(schema), (Map<URI, JSchema>) null);
        JSONSchema stringS = new JSONSchema(string.getSchema());
        assertEquals(string.getSchema(), readerDeps.getSchema());
        assertEquals(string.getSchema(), readerDepsN.getSchema());
        assertEquals(string.getSchema(), stringS.getSchema());
        try {
            JSONSchema tmp = new JSONSchema(f2.toURI());
            fail("No source: must throw exception");
        } catch (IOException ioex) {
        }
        try {
            JSONSchema tmp = new JSONSchema(new URI("http://serygterhwe:rtr"));
            fail("No source: must throw exception");
        } catch (IOException ioex) {
        }
        try {
            JSONSchema tmp = new JSONSchema(new URL("http://localhost:1111"));
            fail("No source: must throw exception");
        } catch (IOException ioex) {
        }
        try {
            JSONSchema tmp = new JSONSchema(new ByteArrayInputStream(schema2.getBytes()));
            fail("Invalid JSON: must throw exception");
        } catch (IOException ioex) {
        }
        try {
            JSONSchema tmp = new JSONSchema(new ByteArrayInputStream(schema2.getBytes()), "UTF-8");
            fail("Invalid JSON: must throw exception");
        } catch (IOException ioex) {
        }
    }

    /**
     * Test of main method, of class JSONSchema.
     */
    public void testMain() throws Exception {
        System.out.println("main");
        final boolean LOG = false;
        String fileName = "src/test/java/big.json";
        String[] urls = new String[] { "http://json-schema.org/schema", "http://json-schema.org/hyper-schema", "http://json-schema.org/json-ref", "http://json-schema.org/interfaces", "http://json-schema.org/geo", "http://json-schema.org/card", "http://json-schema.org/calendar", "http://json-schema.org/address", fileName };
        CacheingURLResolver uriResolver = new CacheingURLResolver();
        boolean canConnect = true;
        try {
            InputStream is = new URL(urls[0]).openStream();
            is.close();
        } catch (ConnectException cex) {
            for (int i = 2; i < (urls.length - 1); i++) {
                String url = urls[i];
                uriResolver.register(new URL(url), new File("./src/test/java/" + url.replace(":", "_").replace("/", "_") + ".schema.json"));
            }
            canConnect = true;
        } catch (Exception ex) {
            canConnect = false;
        }
        boolean scanSchemas = canConnect;
        boolean testValidation = true;
        boolean testLoadSave = canConnect;
        boolean testSelfValidation = canConnect;
        if (scanSchemas) {
            String url = urls[5];
            JSONSchema schema = new JSONSchema(new BufferingReader(uriResolver.resolveURL((new URL(url)), "UTF-8").getReader()) {

                @Override
                public int read() throws IOException {
                    int c = super.read();
                    if (LOG) {
                        System.out.print((char) c);
                        System.out.flush();
                    }
                    return c;
                }
            });
            StringWriter wr = new StringWriter();
            schema.save(wr, true);
            if (LOG) {
                System.out.println(wr.toString());
            }
        }
        if (testValidation) {
            Object sample = JSONParserLite.parse(new BufferingReader(uriResolver.resolveURL((new File(fileName)).toURI().toURL(), "UTF-8").getReader()));
            {
                long start = System.nanoTime();
                SizeInfo si = Utilities.estimateObjectSize(sample, null);
                long end = System.nanoTime();
                System.out.println(MessageFormat.format("  Building schema for {1} (evaluated in {0,number,#####.## ms})" + ((LOG) ? ":\n{2}" : ""), ((end - start) / 1000000.0), fileName, (LOG) ? si : null));
            }
            JSONSchema schema = new JSONSchema();
            JSONSchemaBuilder jsb = null;
            int maxTrials = 100;
            long start = System.nanoTime();
            for (int i = 0; i < maxTrials; i++) {
                jsb = new JSONSchemaBuilder(schema, "big", sample, null);
            }
            double avg = (System.nanoTime() - start) / 1000000.0 / maxTrials;
            StringWriter wr = new StringWriter();
            start = System.nanoTime();
            schema.save(wr, true);
            double avg2 = (System.nanoTime() - start) / 1000000.0;
            start = System.nanoTime();
            P.PP P = new P.PP();
            P.set(schema.getSchema(), "properties", "offers", "items", "properties", "___externalId", "minimum", 0);
            P.set(schema.getSchema(), "properties", "offers", "items", "properties", "___externalId", "maximum", 1639);
            P.set(schema.getSchema(), "properties", "offers", "items", "properties", "___externalId", "exclusiveMaximum", true);
            P.set(schema.getSchema(), "properties", "offers", "items", "minItems", 1000);
            P.set(schema.getSchema(), "properties", "offers", "items", "maxItems", 5);
            P.set(schema.getSchema(), "properties", "offers", "items", "uniqueItems", true);
            P.set(schema.getSchema(), "properties", "offers", "items", "properties", "description", "minLength", 500);
            P.set(schema.getSchema(), "properties", "offers", "items", "properties", "description", "maxLength", 1600);
            P.set(schema.getSchema(), "properties", "offers", "items", "properties", "description", "disallow", new JSONValueType(JSONValueType.__NUMBER));
            P.set(schema.getSchema(), "properties", "places", "items", "properties", "___externalId", "minimum", 0);
            P.set(schema.getSchema(), "properties", "places", "items", "properties", "___externalId", "exclusiveMinimum", true);
            P.set(schema.getSchema(), "properties", "places", "items", "properties", "___externalId", "divisibleBy", 2.0);
            P.set(schema.getSchema(), "properties", "places", "items", "properties", "ZZZ", new JSchema());
            P.set(schema.getSchema(), "properties", "places", "items", "properties", "ZZZ", "default", 1999);
            P.set(schema.getSchema(), "properties", "places", "items", "uniqueItems", false);
            P.set(schema.getSchema(), "properties", "associations", "items", "properties", "___toResolvedVia", "enum", new String[] { "id", "aa" });
            P.set(schema.getSchema(), "properties", "associations", "items", "properties", "___modelRef", "enum", new int[] { 18, 19, 20, 35, 22, 32, 33, 21, 27, 17, 25, 30 });
            wr = new StringWriter();
            schema.save(wr, true);
            System.out.println(MessageFormat.format("  Generated subSchema for {0} in {1,number,###.##} (average out of {2} trials), printed in {4,number,###.##} ms" + ((LOG) ? ":\n {3}" : ""), fileName, avg, maxTrials, (LOG) ? wr.toString() : null, avg2));
            for (int i = 0; i < 20; i++) {
            }
            P.delete(sample, "offers", 0, "___externalId");
            P.set(sample, "offers", 1, "pluginId", new ArrayList());
            P.set(sample, "offers", 1, "___externalId", -3);
            P.set(sample, "offers", 2, "description", 456);
            P.set(sample, "places", 1, "___externalId", 0);
            List l = P.getList(sample, "offers");
            Object o = l.get(3);
            l.add(o);
            l.add(o);
            l.add(o);
            l.add(o);
            JSONValidationContext vctx = new JSONValidationContext();
            vctx.setAssignMissingDefaults(true);
            boolean hasErrors = schema.validate(vctx, schema.getSchema(), sample, true);
            List<ValidationError> errors = vctx.getValidationErrors();
            double avg3 = (System.nanoTime() - start) / 1000000.0;
            System.out.println(MessageFormat.format("  Validated in {1,number,###.##} ms, result = {0}, errors={2}.", hasErrors, avg3, errors.size()));
            System.out.println(vctx.getSummary());
            assertEquals(15, errors.size());
        }
        if (testLoadSave) {
            for (int i = 0; i < urls.length; i++) {
                Object o1 = JSONParserLite.parse(new BufferingReader(uriResolver.resolveURL(Utilities.asURL(urls[i]), "UTF-8").getReader()));
                if (LOG) {
                    System.out.println(Formatter.toJSONString(o1, false));
                }
                JSONSchema schema = null;
                try {
                    schema = new JSONSchema(uriResolver.resolveURL(Utilities.asURL(urls[i]), "UTF-8").getReader());
                } catch (JSONSchemaException jsex) {
                    continue;
                }
                StringWriter wr = new StringWriter();
                schema.save(wr);
                if (LOG) {
                    System.out.println(wr.toString());
                }
                Object o2 = JSONParserLite.parse(new BufferingReader(new StringReader(wr.toString())));
                Comparer cmp = new Comparer();
                ComparatorPair cp = cmp.compare(new ComparatorContext(), o1, o2, 100);
                if (cp.getStatus().equals(COMPARE_STATUS.match)) {
                    if (LOG) {
                        System.out.println("Loaded " + urls[i] + " and interpreted JSON schemas match.");
                    }
                } else {
                    System.out.println("\nDelta between loaded (" + urls[i] + ") and interpreted schema:\n" + cmp.dumpComparatorPair(cp, "", true));
                    System.out.println("ORIGINA/LOADEDL:\n" + Formatter.toJSONString(o1, true) + "\n" + Formatter.toJSONString(o2, true));
                    fail("Incomplete schema loading.");
                }
            }
        }
        if (testSelfValidation) {
            JSONSchema schema = new JSONSchema(new BufferingReader(uriResolver.resolveURL(Utilities.asURL(urls[0]), "UTF-8").getReader()));
            for (int i = 0; i < 8; i++) {
                Object obj = JSONParserLite.parse(new BufferingReader(uriResolver.resolveURL(Utilities.asURL(urls[i]), "UTF-8").getReader()));
                JSONValidationContext ctx = new JSONValidationContext();
                if (!schema.validate(obj)) {
                    System.out.println("  self-validation FAILED for " + urls[i]);
                    for (ValidationError err : ctx.getValidationErrors()) {
                        System.out.println("  " + err.getMessage());
                    }
                    fail("JSON schema self-validation failed for reference schema " + urls[i]);
                } else {
                    System.out.println("  self-validation PASSED for " + urls[i]);
                }
            }
        }
    }
}
