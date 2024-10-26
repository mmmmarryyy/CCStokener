package kanakata.testcase;

import junit.framework.TestFailure;
import junit.framework.TestResult;
import kanakata.runtime.DefaultTimeSource;
import kanakata.runtime.FixedTimeSource;
import kanakata.runtime.SystemTime;
import kanakata.runtime.TimeSource;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.easymock.IExpectationSetters;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;
import java.util.logging.Logger;

public class BaseTestCase extends Assert {

    protected final Logger log = Logger.getLogger(getClass().getName());

    private final Stack<CleanupAction> cleanupActions = new Stack<CleanupAction>();

    private final List mocks = new ArrayList();

    public void registerCleanupAction(CleanupAction cleanupAction) {
        cleanupActions.push(cleanupAction);
    }

    @Before
    public void beforeTest() throws Exception {
        SystemTime.set(createTimeSource());
    }

    @After
    public void performCleanupActions() {
        TestResult result = new TestResult();
        performCleanupActions(cleanupActions, result);
        if (result.errorCount() > 0) {
            fail(createFailureMessage(result));
        }
    }

    @After
    public void afterTest() throws Exception {
        SystemTime.restoreDefault();
        cleanupActions.clear();
        mocks.clear();
        verify();
    }

    public void setUpDefaultTimeZone(TimeZone timeZone) {
        final TimeZone defaultTimeZone = TimeZone.getDefault();
        CleanupAction cleanupAction = new CleanupAction() {

            public void perform() throws Exception {
                TimeZone.setDefault(defaultTimeZone);
            }
        };
        registerCleanupAction(cleanupAction);
        TimeZone.setDefault(timeZone);
    }

    public void setUpDefaultLocale(Locale locale) {
        final Locale defaultLocale = Locale.getDefault();
        CleanupAction cleanupAction = new CleanupAction() {

            public void perform() throws Exception {
                Locale.setDefault(defaultLocale);
            }
        };
        registerCleanupAction(cleanupAction);
        Locale.setDefault(locale);
    }

    public void setUpSystemProperty(final String propertyName, String propertyValue) {
        final String originalPropertyValue = System.getProperty(propertyName);
        CleanupAction cleanupAction = new CleanupAction() {

            public void perform() throws Exception {
                if (originalPropertyValue != null) {
                    System.setProperty(propertyName, originalPropertyValue);
                } else {
                    System.getProperties().remove(propertyName);
                }
            }
        };
        registerCleanupAction(cleanupAction);
        if (propertyValue == null) {
            System.getProperties().remove(propertyName);
        } else {
            System.setProperty(propertyName, propertyValue);
        }
    }

    public void setUpEmptySystemProperty(final String propertyName) {
        setUpSystemProperty(propertyName, null);
    }

    public void setUpSystemTime(TimeSource source) {
        final TimeSource systemTimeSource = SystemTime.getSource();
        SystemTime.set(source);
        registerCleanupAction(new CleanupAction() {

            public void perform() throws Exception {
                SystemTime.set(systemTimeSource);
            }
        });
    }

    public void setUpSystemTime(long systemTime) {
        setUpSystemTime(new FixedTimeSource(systemTime));
    }

    public void setUpSystemTime(Calendar calendar) {
        setUpSystemTime(calendar.getTimeInMillis());
    }

    public void setUpSystemTime(Date date) {
        setUpSystemTime(date.getTime());
    }

    public Calendar newCalendar() {
        return SystemTime.asCalendar(Locale.getDefault(), TimeZone.getDefault());
    }

    public Calendar newCalendarWithoutMillisecondValue() {
        Calendar calendar = newCalendar();
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public Date newDate() {
        return SystemTime.asDate();
    }

    public Date newDateWithoutMillisecondValue() {
        return newCalendarWithoutMillisecondValue().getTime();
    }

    public URL locationOf(String resourceName, boolean classResource) {
        URL resourceLocation = classResource ? getClass().getResource(resourceName) : getClass().getClassLoader().getResource(resourceName);
        assertNotNull(resourceLocation);
        return resourceLocation;
    }

    public InputStream openResource(String resourceName, boolean classResource) throws Exception {
        URL resourceLocation = locationOf(resourceName, classResource);
        return resourceLocation.openStream();
    }

    public byte[] readResource(String resource, boolean classResource) throws Exception {
        URL url = locationOf(resource, classResource);
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        byte[] contents = new byte[connection.getContentLength()];
        in.read(contents);
        return contents;
    }

    public Document openDocument(String resourceName, boolean classResource) throws Exception {
        return openDocument(openResource(resourceName, classResource));
    }

    public Document openDocument(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(inputStream);
    }

    public Document newDocument(String namespaceUri, String rootElementName) throws Exception {
        Document document = newDocument();
        Element rootElement = document.createElementNS(namespaceUri, rootElementName);
        document.appendChild(rootElement);
        return document;
    }

    public Document newDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    public String serializeDocument(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    public ByteArrayOutputStream installStandardOutInterceptor() {
        PrintStream previousOut = System.out;
        registerCleanupAction(new RestoreStandardOutAction(previousOut));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        return outputStream;
    }

    public ByteArrayOutputStream installStandardErrInterceptor() {
        PrintStream previousErr = System.err;
        registerCleanupAction(new RestoreStandardErrAction(previousErr));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outputStream));
        return outputStream;
    }

    public <T> T createStrictMock(Class<T> toMock) {
        return registerMock(EasyMock.createStrictMock(toMock));
    }

    public <T> T createStrictMock(String name, Class<T> toMock) {
        return registerMock(EasyMock.createStrictMock(name, toMock));
    }

    public <T> T createMock(Class<T> toMock) {
        return registerMock(EasyMock.createMock(toMock));
    }

    public <T> T createMock(String name, Class<T> toMock) {
        return registerMock(EasyMock.createMock(name, toMock));
    }

    public <T> T createNiceMock(Class<T> toMock) {
        return registerMock(EasyMock.createNiceMock(toMock));
    }

    public <T> T createNiceMock(String name, Class<T> toMock) {
        return registerMock(EasyMock.createNiceMock(name, toMock));
    }

    public <T> IExpectationSetters<T> expect(T value) {
        return EasyMock.expect(value);
    }

    public <T> IExpectationSetters<T> expectLastCall() {
        return EasyMock.expectLastCall();
    }

    public IArgumentMatcher reportMatcher(IArgumentMatcher matcher) {
        EasyMock.reportMatcher(matcher);
        return matcher;
    }

    public void replay() {
        for (Object mock : mocks) {
            EasyMock.replay(mock);
        }
    }

    public void verify() {
        for (Object mock : mocks) {
            EasyMock.verify(mock);
        }
    }

    public void reset() {
        for (Object mock : mocks) {
            EasyMock.reset(mock);
        }
    }

    protected TimeSource createTimeSource() {
        return new DefaultTimeSource();
    }

    private String createFailureMessage(TestResult result) {
        StringBuffer failureMessage = new StringBuffer();
        for (Enumeration errors = result.errors(); errors.hasMoreElements(); ) {
            TestFailure error = (TestFailure) errors.nextElement();
            failureMessage.append(error.trace());
        }
        return failureMessage.toString();
    }

    private void performCleanupActions(Stack<CleanupAction> cleanupActions, TestResult testResult) {
        while (!cleanupActions.isEmpty()) {
            try {
                cleanupActions.pop().perform();
            } catch (Exception e) {
                testResult.addError(null, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T registerMock(T mock) {
        mocks.add(mock);
        return mock;
    }
}
