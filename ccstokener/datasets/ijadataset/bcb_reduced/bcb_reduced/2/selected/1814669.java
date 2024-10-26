package net.rptools.chartool.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.rptools.chartool.model.Character;
import net.rptools.chartool.model.RPTokFile;
import net.rptools.chartool.model.property.PropertyList;
import net.rptools.chartool.model.property.PropertyMap;
import net.rptools.chartool.model.property.PropertySettings;
import net.rptools.chartool.model.property.PropertySettingsFile;
import net.rptools.chartool.model.property.SlotPropertyValue;
import net.rptools.chartool.model.xml.ConverterSupport;
import net.rptools.chartool.ui.component.RPIcon;
import net.rptools.chartool.ui.component.Utilities;
import net.rptools.chartool.ui.component.RPIconFactory.MapToolAsset;
import net.rptools.lib.FileUtil;
import net.rptools.lib.MD5Key;
import net.rptools.lib.io.PackedFile;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;

/**
 * Support for loading and saving files
 * 
 * @author jgorrell
 * @version $Revision$ $Date$ $Author$
 */
public class CharToolPersistenceSupport {

    /**
   * Resource name for the blank token packed file
   */
    public static final String BLANK_RPTOK_FILE = "net/rptools/chartool/resources/images/blank.rptok";

    /**
   * One an only instance of the persistence support class.
   */
    private static final CharToolPersistenceSupport singletonInstance = new CharToolPersistenceSupport();

    /**
   * Logger instance for this class.
   */
    private static final Logger LOGGER = Logger.getLogger(CharToolPersistenceSupport.class.getName());

    /**
   * Save a character to a file.
   * 
   * @param character Save this character.
   * @param file Save to this file.
   * @param settings Property settings used to update the token properties
   */
    public void saveCharacter(Character character, File file, PropertySettings settings) {
        RPTokFile rptok = null;
        Writer writer = null;
        boolean cleanupOnError = false;
        try {
            if (!file.exists()) FileUtil.saveResource(BLANK_RPTOK_FILE, file.getParentFile(), file.getName());
            rptok = new RPTokFile(file);
            XStream xstream = ConverterSupport.getXStream(Character.class, PropertyMap.class, PropertyList.class, SlotPropertyValue.class, RPIcon.class);
            writer = new BufferedWriter(new OutputStreamWriter(rptok.getOutputStream(RPTokFile.CHARACTER_PATH)));
            String gameName = settings.getGameName();
            if (gameName != null && (gameName = gameName.trim()).length() > 0) character.setGame(settings.getGameName());
            character.setSources(settings.getSourceNames());
            xstream.toXML(character, writer);
            writer.close();
            rptok.updateContents(character, settings);
            rptok.setProperty(PropertySettingsFile.GAME_NAME_PROP_NAME, settings.getGameName());
            rptok.save();
            rptok.close();
        } catch (FileNotFoundException e1) {
            LOGGER.log(Level.WARNING, "Could not open file: " + file.getAbsolutePath(), e1);
            cleanupOnError = true;
            throw new IllegalArgumentException("Could not open the file '" + file.getAbsolutePath() + "'.", e1);
        } catch (IOException e1) {
            LOGGER.log(Level.WARNING, "Could not write file: " + file.getAbsolutePath(), e1);
            cleanupOnError = true;
            throw new IllegalArgumentException("Could not write the file '" + file.getAbsolutePath() + "'.", e1);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unexpected error in file : " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Unexpected error in file  '" + file.getAbsolutePath(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    LOGGER.log(Level.INFO, "Ignoring close exception on '" + file.getAbsolutePath() + "'.", e1);
                }
            }
            if (rptok != null) rptok.close();
            if (cleanupOnError) file.delete();
        }
    }

    /**
   * Load a character from a file.
   * 
   * @param file The file to be loaded.
   * @param settings The settings that describe the property values.
   * @return The character contained in the file
   */
    public Character loadCharacter(File file, PropertySettings settings) {
        RPTokFile rptok = new RPTokFile(file);
        Reader reader = null;
        Character character = null;
        boolean wrongGame = false;
        try {
            String charGameName = null;
            String charSourceNames = null;
            try {
                charGameName = (String) new PackedFile(file).getProperty(PropertySettingsFile.GAME_NAME_PROP_NAME);
                charSourceNames = (String) new PackedFile(file).getProperty(PropertySettingsFile.SOURCE_PROP_NAME);
            } catch (IOException e1) {
                throw new IllegalArgumentException("Invalid token file: " + file.getAbsolutePath());
            }
            Set<String> sources = charSourceNames == null ? null : ConverterSupport.readSet(null, charSourceNames);
            String message = "The character in file '" + file.getName() + " needs the";
            wrongGame = charGameName != null && !settings.getGameName().equals(charGameName);
            if (wrongGame) message += " '" + charGameName + "' game settings loaded";
            if (sources != null && !settings.getSourceNames().containsAll(sources)) {
                if (wrongGame) message += " and the";
                wrongGame = true;
                sources.removeAll(settings.getSourceNames());
                if (sources.size() == 1) {
                    message += " '" + sources.iterator().next() + "' game source loaded";
                } else {
                    String[] sa = sources.toArray(new String[sources.size()]);
                    for (int i = 0; i < sa.length; i++) {
                        if (i > 0 && i + 1 < sa.length) message += ",";
                        message += " '" + sa[i] + "'";
                        if (i + 2 == sa.length) message += " &";
                    }
                    message += " game sources loaded";
                }
            }
            message += ".";
            if (wrongGame) throw new IllegalArgumentException(message);
            if (rptok.hasFile(RPTokFile.CHARACTER_PATH)) {
                XStream xstream = ConverterSupport.getXStream(Character.class, PropertyMap.class, PropertyList.class, SlotPropertyValue.class, RPIcon.class);
                reader = new BufferedReader(new InputStreamReader(rptok.getFile(RPTokFile.CHARACTER_PATH)));
                character = (Character) xstream.fromXML(reader);
            } else {
                character = new Character();
            }
            character.getCustomProperties().setPropertyDescriptors(settings.getCustomPropertySet().getProperties());
            character.setGame(settings.getGameName());
            character.setSources(settings.getSourceNames());
            rptok.updateCharacter(character, settings);
            return character;
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Couldn't open property descriptor set file: " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Could not open the file '" + file.getAbsolutePath() + "'.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't open property token file: " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Could not open the file '" + file.getAbsolutePath() + "'.");
        } catch (ConversionException e) {
            LOGGER.log(Level.WARNING, "Invalid character file: " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("The character set file '" + file.getAbsolutePath() + "' is invalid.", e);
        } catch (RuntimeException e) {
            if (wrongGame) throw e;
            LOGGER.log(Level.WARNING, "Unexpected error in file : " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Unexpected error in file  '" + file.getAbsolutePath(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    LOGGER.log(Level.INFO, "Ignoring close exception on '" + file.getAbsolutePath() + "'.", e1);
                }
            }
        }
    }

    /**
   * Load an asset from a token file.
   * 
   * @param url load the asset from here
   * @return A structure with all of the asset data in it.
   */
    public MapToolAsset loadAsset(URL url) {
        Reader reader = null;
        try {
            XStream xstream = ConverterSupport.getXStream(MapToolAsset.class);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            MapToolAsset asset = (MapToolAsset) xstream.fromXML(reader);
            return asset;
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Couldn't open property descriptor set url: " + url.toExternalForm(), e);
            throw new IllegalArgumentException("Could not open the file '" + url.toExternalForm() + "'.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't open property token url: " + url.toExternalForm(), e);
            throw new IllegalArgumentException("Could not open the file '" + url.toExternalForm() + "'.");
        } catch (ConversionException e) {
            LOGGER.log(Level.WARNING, "Invalid Map Tool Asset XML url: " + url.toExternalForm(), e);
            throw new IllegalArgumentException("The Map Tool Asset XML '" + url.toExternalForm() + "' is invalid.", e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unexpected error in file : " + url.toExternalForm(), e);
            throw new IllegalArgumentException("Unexpected error in file  '" + url.toExternalForm(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    LOGGER.log(Level.INFO, "Ignoring close exception on '" + url.toExternalForm() + "'.", e1);
                }
            }
        }
    }

    /**
   * Load an asset from a token file.
   * 
   * @param asset Save this asset
   * @param file Save it here
   */
    public void saveAsset(MapToolAsset asset, RPTokFile file) {
        Writer writer = null;
        try {
            XStream xstream = ConverterSupport.getXStream(MapToolAsset.class);
            writer = new BufferedWriter(new OutputStreamWriter(file.getOutputStream(RPTokFile.ASSET_DIR + asset.getId())));
            xstream.toXML(asset, writer);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Couldn't open property descriptor set file: " + file.getPackedFile().getAbsolutePath() + "/" + asset.getId(), e);
            throw new IllegalArgumentException("Could not open the file '" + file.getPackedFile().getAbsolutePath() + "/" + asset.getId() + "'.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't open property token file: " + file.getPackedFile().getAbsolutePath() + "/" + asset.getId(), e);
            throw new IllegalArgumentException("Could not open the file '" + file.getPackedFile().getAbsolutePath() + "/" + asset.getId() + "'.");
        } catch (ConversionException e) {
            LOGGER.log(Level.WARNING, "Invalid Map Tool Asset XML file: " + file.getPackedFile().getAbsolutePath() + "/" + asset.getId(), e);
            throw new IllegalArgumentException("The Map Tool Asset XML '" + file.getPackedFile().getAbsolutePath() + "/" + asset.getId() + "' is invalid.", e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unexpected error in file : " + file.getPackedFile().getAbsolutePath() + "/" + asset.getId(), e);
            throw new IllegalArgumentException("Unexpected error in file  '" + file.getPackedFile().getAbsolutePath() + "/" + asset.getId(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    LOGGER.log(Level.INFO, "Ignoring close exception on '" + file.getPackedFile().getAbsolutePath() + "/" + asset.getId() + "'.", e1);
                }
            }
        }
    }

    /**
   * Load the globals for the passed settings file.
   * 
   * @param settingsFile Find the globals for this settings file.
   * @return The global data.
   */
    protected Map<Object, Object> loadGlobals(File settingsFile) {
        File file = getGlobalsFile(settingsFile);
        if (file == null || !file.exists()) return null;
        Reader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return (Map<Object, Object>) new XStream().fromXML(reader);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Couldn't open globals file: " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Could not open the file '" + file.getAbsolutePath() + "'.");
        } catch (ConversionException e) {
            LOGGER.log(Level.WARNING, "Invalid globals XML url: " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("The Global Property XML '" + file.getAbsolutePath() + "' is invalid.", e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unexpected error in file : " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Unexpected error in file  '" + file.getAbsolutePath(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    LOGGER.log(Level.INFO, "Ignoring close exception on '" + file.getAbsolutePath() + "'.", e1);
                }
            }
        }
    }

    /**
   * Save the global properties for a game settings file.
   * 
   * @param globals The globals being saved
   * @param settingsFile Save the globals for this settings file.
   */
    protected void saveGlobals(Map<Object, Object> globals, File settingsFile) {
        File file = getGlobalsFile(settingsFile);
        if (file == null) return;
        if (globals == null || globals.isEmpty()) {
            file.delete();
            return;
        }
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            new XStream().toXML(globals, writer);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Couldn't open globals file: " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Could open the globals file '" + file.getAbsolutePath() + "'.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't write the globals file: " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Could not write the globals file '" + file.getAbsolutePath() + "'.");
        } catch (ConversionException e) {
            LOGGER.log(Level.WARNING, "Invalid globals XML file: " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("The globals XML file '" + file.getAbsolutePath() + "' is invalid.", e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unexpected error in file : " + file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Unexpected error in file  '" + file.getAbsolutePath(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    LOGGER.log(Level.INFO, "Ignoring close exception on '" + file.getAbsolutePath() + "'.", e1);
                }
            }
        }
    }

    /**
   * Get the global variables file for a settings file.
   * 
   * @param settingsFile Find the globals file for this settings file.
   * @return The file containing the global data.
   */
    private File getGlobalsFile(File settingsFile) {
        File file = null;
        if (settingsFile != null) {
            MD5Key id = new MD5Key(settingsFile.getAbsolutePath().getBytes());
            String name = settingsFile.getName();
            name = name.substring(0, name.lastIndexOf('.'));
            name = id.toString() + "-" + name + "-game.xml";
            file = new File(Utilities.GAME_SETTINGS_FILE_DIR, name);
        }
        return file;
    }

    /**
   * Get an instance of the persistence support class.
   * 
   * @return The one and only instance of the persistence support.
   */
    public static CharToolPersistenceSupport getInstance() {
        return singletonInstance;
    }
}
