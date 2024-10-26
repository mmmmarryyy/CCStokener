package org.dbunit.util.fileloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.XmlDataSet;

/**
 * @author Jeff Jensen jeffjensen AT users.sourceforge.net
 * @author Last changed by: $Author$
 * @version $Revision$ $Date$
 * @since 2.4.8
 */
public class FullXmlDataFileLoader extends AbstractDataFileLoader {

    /** Create new instance. */
    public FullXmlDataFileLoader() {
    }

    /**
     * Create new instance with replacement objects.
     * 
     * @param replacementObjects
     *            The replacement objects for use with
     *            {@link org.dbunit.dataset.ReplacementDataSet}.
     */
    public FullXmlDataFileLoader(Map ro) {
        super(ro);
    }

    /**
     * Create new instance with replacement objects and replacement substrings.
     * 
     * @param ro
     *            The replacement objects for use with
     *            {@link org.dbunit.dataset.ReplacementDataSet}.
     * @param rs
     *            The replacement substrings for use with
     *            {@link org.dbunit.dataset.ReplacementDataSet}.
     */
    public FullXmlDataFileLoader(Map ro, Map rs) {
        super(ro, rs);
    }

    /**
     * {@inheritDoc}
     */
    protected IDataSet loadDataSet(URL url) throws DataSetException, IOException {
        InputStream in = url.openStream();
        IDataSet ds = new XmlDataSet(in);
        return ds;
    }
}
