package jp.go.aist.six.oval.core.repository.mongodb;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import jp.go.aist.six.oval.OvalException;
import jp.go.aist.six.oval.core.OvalContext;
import jp.go.aist.six.oval.model.definitions.DefinitionType;
import jp.go.aist.six.oval.model.definitions.DefinitionsElementAssoc;
import jp.go.aist.six.oval.model.definitions.OvalDefinitions;
import jp.go.aist.six.oval.model.definitions.StateType;
import jp.go.aist.six.oval.model.definitions.SystemObjectType;
import jp.go.aist.six.oval.model.definitions.TestType;
import jp.go.aist.six.oval.model.definitions.VariableType;
import jp.go.aist.six.oval.model.results.OvalResults;
import jp.go.aist.six.oval.model.sc.OvalSystemCharacteristics;
import jp.go.aist.six.util.xml.XmlException;
import jp.go.aist.six.util.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author	Akihito Nakamura, AIST
 * @version $Id: MongoOvalDatastoreTool.java 2328 2012-04-18 08:47:07Z nakamura5akihito@gmail.com $
 */
public class MongoOvalDatastoreTool {

    /**
     * Logger.
     */
    private static final Logger _LOG_ = LoggerFactory.getLogger(MongoOvalDatastoreTool.class);

    public static void main(final String[] args) {
        _LOG_.debug("args=" + Arrays.toString(args));
        if (args.length == 0) {
            System.exit(0);
        }
        final String cmd = args[0];
        _LOG_.debug("cmd=" + cmd);
        if (cmd.equals("-install")) {
            if (args.length == 2) {
                String location = args[1];
                _LOG_.debug("location=" + location);
                installOvalDefinitions(location);
            }
        } else if (cmd.equals("-delete-all")) {
            deleteAllData();
        }
    }

    private static MongoOvalDatastore _DATASTORE_;

    private static XmlMapper _XML_MAPPER_ = null;

    /**
     * Constructor.
     */
    public MongoOvalDatastoreTool() {
    }

    /**
     */
    private static MongoOvalDatastore _getDatastore() {
        if (_DATASTORE_ == null) {
            _DATASTORE_ = OvalContext.INSTANCE.getBean(MongoOvalDatastore.class);
        }
        return _DATASTORE_;
    }

    private static XmlMapper _getXmlMapper() {
        if (_XML_MAPPER_ == null) {
            _XML_MAPPER_ = OvalContext.INSTANCE.getBean(XmlMapper.class);
        }
        return _XML_MAPPER_;
    }

    /**
     */
    private static <T> T _unmarshalObject(final Class<T> type, final InputStream in_stream) throws XmlException {
        Object obj = _getXmlMapper().unmarshal(in_stream);
        T object = type.cast(obj);
        return object;
    }

    /**
     */
    private static URL _toURL(final String value) {
        if (value == null) {
            return null;
        }
        URL url = null;
        try {
            url = new URL(value);
        } catch (MalformedURLException ex) {
            url = null;
        }
        return url;
    }

    /**
     */
    public static String installOvalDefinitions(final String xml_location) {
        InputStream in_stream = null;
        try {
            URL url = _toURL(xml_location);
            if (url == null) {
                in_stream = new FileInputStream(xml_location);
            } else {
                in_stream = url.openStream();
            }
        } catch (IOException ex) {
            throw new OvalException(ex);
        }
        Class<OvalDefinitions> type = OvalDefinitions.class;
        OvalDefinitions object = _unmarshalObject(type, in_stream);
        String pid = _getDatastore().save(type, object);
        return pid;
    }

    /**
     */
    public static void deleteAllData() {
        MongoOvalDatastore ds = _getDatastore();
        _LOG_.debug("delete DefinitonsElementAssoc...");
        ds.delete(DefinitionsElementAssoc.class);
        _LOG_.debug("delete OvalDefinitons...");
        ds.delete(OvalDefinitions.class);
        _LOG_.debug("delete Definiton...");
        ds.delete(DefinitionType.class);
        _LOG_.debug("delete Test...");
        ds.delete(TestType.class);
        _LOG_.debug("delete SystemObject...");
        ds.delete(SystemObjectType.class);
        _LOG_.debug("delete State...");
        ds.delete(StateType.class);
        _LOG_.debug("delete Variable...");
        ds.delete(VariableType.class);
        _LOG_.debug("delete OvalSysChar...");
        ds.delete(OvalSystemCharacteristics.class);
        _LOG_.debug("delete OvalResults...");
        ds.delete(OvalResults.class);
    }
}
