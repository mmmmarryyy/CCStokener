package org.systemsEngineering.core.moe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Utility class that aids loading and storing an {@link MeasureOfEffectivenessAnalysis}.
 * 
 * @author Mike Werner
 */
public class PersistenceManager {

    private PersistenceManager() {
    }

    /**
	 * Reads a {@link MeasureOfEffectivenessAnalysis} from an input stream.
	 * 
	 * @param in the input stream to read from.
	 * @return the analysis read.
	 * @throws IOException if reading from the stream fails.
	 * @throws ClassNotFoundException if a class necessary to create the analysis can not be found.
	 */
    public static MeasureOfEffectivenessAnalysis load(InputStream in) throws IOException, ClassNotFoundException {
        return (MeasureOfEffectivenessAnalysis) (new ObjectInputStream(in).readObject());
    }

    /**
	 * Reads a {@link MeasureOfEffectivenessAnalysis} from a file.
	 * 
	 * @param file the file to read from.
	 * @return the analysis read.
	 * @throws IOException if reading from the file fails.
	 * @throws ClassNotFoundException if a class necessary to create the analysis can not be found.
	 */
    public static MeasureOfEffectivenessAnalysis load(File file) throws IOException, ClassNotFoundException {
        return load(new FileInputStream(file));
    }

    /**
	 * Reads a {@link MeasureOfEffectivenessAnalysis} from a {@code URL}.
	 * 
	 * @param url the url to read from.
	 * @return the analysis read.
	 * @throws IOException if reading from the url fails.
	 * @throws ClassNotFoundException if a class necessary to create the analysis can not be found.
	 */
    public static MeasureOfEffectivenessAnalysis load(URL url) throws IOException, ClassNotFoundException {
        return load(url.openStream());
    }

    /**
	 * Writes a {@link MeasureOfEffectivenessAnalysis} to an output stream.
	 * 
	 * @param out the stream to write to.
	 * @param analysis the analysis to write.
	 * @throws IOException if writing to the stream fails.
	 */
    public static void write(OutputStream out, MeasureOfEffectivenessAnalysis analysis) throws IOException {
        new ObjectOutputStream(out).writeObject(analysis);
    }

    /**
	 * Writes a {@link MeasureOfEffectivenessAnalysis} to a file.
	 * 
	 * @param file the file to write to.
	 * @param analysis the analysis to write.
	 * @throws IOException if writing to the file fails.
	 */
    public static void write(File file, MeasureOfEffectivenessAnalysis analysis) throws IOException {
        new ObjectOutputStream(new FileOutputStream(file)).writeObject(analysis);
    }

    /**
	 * Writes a {@link MeasureOfEffectivenessAnalysis} to a {@code URL}.
	 * 
	 * @param url the url to write to.
	 * @param analysis the analysis to write.
	 * @throws IOException if writing to the url fails.
	 */
    public static void write(URL url, MeasureOfEffectivenessAnalysis analysis) throws IOException {
        new ObjectOutputStream(url.openConnection().getOutputStream()).writeObject(analysis);
    }
}
