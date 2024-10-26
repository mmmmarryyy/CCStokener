package net.rptools.chartool.model.db;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.rptools.chartool.model.property.PropertyDescriptor;
import net.rptools.chartool.model.property.PropertyDescriptorMap;
import net.rptools.chartool.model.property.PropertyDescriptorSet;
import net.rptools.chartool.model.property.PropertyMap;
import net.rptools.chartool.model.xml.ConverterSupport;
import net.rptools.lib.io.PackedFile;
import com.thoughtworks.xstream.XStream;

/**
 * Create & update the contents of property tables with the data in XML files. Save
 * data in property tables into XML.
 * 
 * @author jgorrell
 * @version $Revision$ $Date$ $Author$
 */
public class PropertyTableXML {

    /**
   * The one and only instance of this class.
   */
    private static PropertyTableXML singletonInstance;

    /**
   * Logger instance for this class.
   */
    private static final Logger logger = Logger.getLogger(PropertyTableXML.class.getName());

    /**
   * Singleton constructor.
   */
    private PropertyTableXML() {
    }

    /**
   * Read a property table descriptor from XML.
   * 
   * @param file Read the descriptor from this file.
   * @param pack The packed file containing the data file.
   * @return A table descriptor 
   * @throws IllegalArgumentException Problems accessing the data from the reader or the data is not
   * an rptools file.
   * @throws ClassCastException The first object from the reader was not a property table descriptor.
   */
    public PropertyDescriptorSet readDescriptor(PackedFile pack, String file) {
        PropertyDescriptorSet pds = null;
        Reader reader = null;
        try {
            reader = new InputStreamReader(pack.getFile(file));
            pds = readDescriptor(reader);
            if (pds.getType().matches("\\s")) throw new IllegalStateException("Invalid table name: " + pds.getType());
            return pds;
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Database file '" + file + "' does not exist.", e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problems reading database file: '" + file + "'", e);
            throw new IllegalStateException("Problems reading database file '" + file + "'.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
   * Read a property table descriptor from XML.
   * 
   * @param reader A read for the XML
   * @return A table descriptor 
   * @throws IllegalArgumentException Problems accessing the data from the reader or the data is not
   * an rptools file.
   * @throws ClassCastException The first object from the reader was not a property table descriptor.
   */
    public PropertyDescriptorSet readDescriptor(Reader reader) {
        try {
            XStream xstream = getXStream();
            ObjectInputStream is = xstream.createObjectInputStream(reader);
            return (PropertyDescriptorSet) is.readObject();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problems reading the descriptor", e);
            throw new IllegalArgumentException("Problems reading data from the passed reader.", e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Problems reading the descriptor", e);
            throw new IllegalArgumentException("Data in the reader is not an rptools file.", e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Unexpected problems reading the descriptor", e);
            throw new IllegalArgumentException("Unexpected problems reading the descriptor", e);
        }
    }

    /**
   * Create an XStream instance with the proper aliases and converters.
   * 
   * @return The configured <code>XStream</code> instance.
   */
    private XStream getXStream() {
        XStream xstream = ConverterSupport.getXStream(PropertyDescriptor.class, PropertyMap.class, PropertyDescriptorSet.class, PropertyDescriptorMap.class, PropertyTable.class, PropertyTable.class);
        return xstream;
    }

    /**
   * Make sure the passed file path is a valid data file.
   * 
   * @param data Stream containing the data file.
   * @return The value <code>true</code> if the Xml file is a valid rpgame file
   * @throws IllegalArgumentException Problem validating the file.
   */
    public boolean validateXmlFile(URL data) {
        if (data == null) return false;
        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(data.openStream()));
            XStream xstream = getXStream();
            ObjectInputStream is = xstream.createObjectInputStream(reader);
            PropertyDescriptorSet ptd = (PropertyDescriptorSet) is.readObject();
            xstream.alias(ptd.getType(), PropertyMap.class);
            int i = 0;
            while (is.readObject() != null && i < 10) i++;
        } catch (EOFException e) {
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problems reading data from the URL: " + data.toExternalForm(), e);
            throw new IllegalArgumentException("Problems reading data from the URL: " + data.toExternalForm(), e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Data in the URL is not an rptools file: " + data.toExternalForm(), e);
            throw new IllegalArgumentException("Data in the URL is not an rptools file: " + data.toExternalForm(), e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Unexpected problems reading the URL: " + data.toExternalForm(), e);
            throw new IllegalArgumentException("Unexpected problems reading the URL: " + data.toExternalForm(), e);
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return true;
    }

    /**
   * Create a property table from the passed data file and then load in all of the data.
   * 
   * @param data The path to the data file. It is validated to make sure it exists and can be read.
   * @param dbName The name of the database getting the new table.
   * @param tableName The name of the new table. This table can not already exist in the database.
   * @param packed The packed file containing the actual data file.
   * @return The property table that was created. 
   */
    public PropertyTable createAndLoadPropertyTable(String data, String dbName, String tableName, PackedFile packed) {
        if (data == null || (data = data.trim()).length() < 0) return null;
        try {
            return createAndLoadPropertyTable(dbName, tableName, packed.getFile(data));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problem opening the packed file: " + data, e);
            throw new IllegalStateException("Problem opening the packed file: " + data, e);
        }
    }

    /**
   * Create a property table from the passed data file and then load in all of the data.
   * 
   * @param dbName The name of the database getting the new table.
   * @param tableName The name of the new table. This table can not already exist in the database.
   * @param url The URL to the actual data.
   * @return The property table that was created. 
   */
    public PropertyTable createAndLoadPropertyTable(String dbName, String tableName, URL url) {
        if (url == null) return null;
        try {
            return createAndLoadPropertyTable(dbName, tableName, url.openStream());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problem opening the URL: " + url.toExternalForm(), e);
            throw new IllegalStateException("Problem opening the packed file: " + url.toExternalForm(), e);
        }
    }

    /**
   * Create a property table from the passed data file and then load in all of the data.
   * 
   * @param dbName The name of the database getting the new table.
   * @param tableName The name of the new table. This table can not already exist in the database.
   * @param stream Input stream containing the actual data.
   * @return The property table that was created. 
   */
    public PropertyTable createAndLoadPropertyTable(String dbName, String tableName, InputStream stream) {
        return createOrUpdatePropertyTable(dbName, tableName, stream, false);
    }

    /**
   * Update a property table with all of the data from the passed data file.
   * 
   * @param data The path to the data file. It is validated to make sure it exists and can be read.
   * @param dbName The name of the database getting the new table.
   * @param tableName The name of the new table. This table must already exist in the database.
   * @param packed The packed file containing the actual data file.
   * @return The property table that was created. 
   */
    public PropertyTable updatePropertyTable(String data, String dbName, String tableName, PackedFile packed) {
        if (data == null || (data = data.trim()).length() < 0) return null;
        try {
            return createOrUpdatePropertyTable(dbName, tableName, packed.getFile(data), true);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problem opening the packed file: " + data, e);
            throw new IllegalStateException("Problem opening the packed file: " + data, e);
        }
    }

    /**
   * Create a property table from the passed data file and then load in all of the data.
   * 
   * @param dbName The name of the database getting the new table.
   * @param tableName The name of the new table. This table can not already exist in the database.
   * @param stream Input stream containing the actual data.
   * @param update Flag indicating that the table should have records added to it.
   * @return The property table that was created. 
   */
    private PropertyTable createOrUpdatePropertyTable(String dbName, String tableName, InputStream stream, boolean update) {
        if (stream == null) return null;
        Reader reader = null;
        PropertyTable table = null;
        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            XStream xstream = getXStream();
            ObjectInputStream is = xstream.createObjectInputStream(reader);
            PropertyDescriptorSet ptd = (PropertyDescriptorSet) is.readObject();
            xstream.alias(ptd.getType(), PropertyMap.class);
            if (!update) {
                table = new PropertyTable(dbName, tableName, ptd);
            } else {
                table = PropertyTable.getPropertyTable(dbName, tableName);
            }
            PropertyMap map = null;
            while ((map = (PropertyMap) is.readObject()) != null) table.insert(map, null);
        } catch (EOFException e) {
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problems reading data from the passed reader.", e);
            throw new IllegalArgumentException("Problems reading data from the passed reader.", e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Data in the reader is not an rptools file.", e);
            throw new IllegalArgumentException("Data in the reader is not an rptools file.", e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Unexpected problems reading the data", e);
            throw new IllegalArgumentException("Unexpected problems reading the data", e);
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return table;
    }

    /**
   * Load the database. The current database will be unloaded. 
   * 
   * @param dbName The name of the database that will be modified.
   * @param file The file to be loaded.
   * @param packed The packed file containing the actual data file.
   * @return The loaded property table,
   * @throws IllegalArgumentException Problem loading the file. The message is suitable for display to the user.
   */
    public PropertyTable loadDatabaseFile(String dbName, String file, PackedFile packed) {
        PropertyDescriptorSet pds = null;
        Reader reader = null;
        try {
            reader = new InputStreamReader(packed.getFile(file));
            pds = readDescriptor(reader);
            if (pds.getType().matches("\\s")) throw new IllegalStateException("Invalid table name: " + pds.getType());
        } catch (FileNotFoundException e1) {
            throw new IllegalArgumentException("Unable to read the new file: '" + file + "'");
        } catch (IOException e1) {
            logger.log(Level.WARNING, "Problems reading data from the packed file: '" + file + "'", e1);
            throw new IllegalArgumentException("Problems reading data from the file: '" + file + "'", e1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        PropertyTable.deletePropertyTable(dbName, pds.getType());
        return createAndLoadPropertyTable(file, dbName, pds.getType(), packed);
    }

    /**
   * Write a property table as XML
   * 
   * @param table Table being written.
   * @param writer Write the XML here.
   */
    public void writeDatabaseFile(PropertyTable table, Writer writer) {
        ObjectOutputStream oos = null;
        try {
            XStream xstream = getXStream();
            xstream.alias(table.getType(), PropertyMap.class);
            oos = xstream.createObjectOutputStream(writer, "rptools-data");
            oos.writeObject(table);
            List<Long> keys = table.keyset(null, null);
            for (Long key : keys) {
                oos.writeObject(table.select(key));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Problem writing table: " + table.getDatabase() + "." + table.getTable(), e);
            throw new IllegalStateException("Problem writing table: " + table.getDatabase() + "." + table.getTable());
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Ignoring exception on close.", e);
            }
        }
    }

    /**
   * Get a property table XML instance.
   * 
   * @return A property table xml instance
   */
    public static PropertyTableXML getInstance() {
        if (singletonInstance == null) singletonInstance = new PropertyTableXML();
        return singletonInstance;
    }
}
