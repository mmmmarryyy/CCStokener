package fr.ird.animat.server.tuna;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.util.StringTokenizer;
import java.util.Properties;
import java.net.URL;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import fr.ird.animat.server.Clock;

/**
 * Configuration de la simulation.
 *
 * @author Martin Desruisseaux
 * @version $Id: Configuration.java,v 1.4 2004/11/10 08:41:48 desruisseaux Exp $
 */
final class Configuration {

    /**
     * Le fuseau horaire pour l'affichage de dates.
     */
    public final TimeZone timezone;

    /**
     * Le premier pas de temps de la simulation.
     */
    public final Clock firstTimeStep;

    /**
     * Pause � prendre entre deux pas de la simulation, en nombre de millisecondes.
     */
    public final long pause;

    /**
     * Indique si les images doivent �tre affich�es en tons de gris. Une image en
     * tons de gris fait mieux ressortir les autres indications color�es, comme la
     * position des p�ches ainsi que des bancs de thons virtuels. Si ce drapeau est
     * <code>false</code>, alors les images garderont leurs palette d'origine.
     */
    public final boolean grayscaleImages;

    /**
     * Indique si les positions de p�ches doivent �tre affich�es.
     */
    public final boolean fisheriesVisible;

    /**
     * Distance maximale (en miles nautiques) que peut parcourir un thon en une journ�e.
     */
    public final double dailyDistance;

    /**
     * Rayon de perception des thons, en miles nautiques. Les thons ne "sentiront" pas les
     * param�tres en dehors de ce rayon.
     */
    public final double perceptionRadius;

    /**
     * R�solution d�sir�e des images en degr�s d'angle de longitude et de latitude.
     */
    public final double resolution;

    /**
     * Liste des s�ries, op�rations et �valuateurs � utiliser.
     */
    public final Set<Parameter> parameters;

    /**
     * Esp�ces d�sign�es par leurs codes de la FAO.
     */
    public final Set<String> species;

    /**
     * Les param�tres {@link #parameters} sous forme de tableau.  Ce tableau peut contenir un
     * �l�ment supl�mentaire pour {@link fr.ird.animat.server.Parameter#HEADING}. Ce tableau est
     * utilis� pour construire les objets  {@link Species}  en partageant le m�me tableau pour
     * chaque esp�ce.
     */
    final Parameter[] parameterArray;

    /**
     * Construit une configuration � partir du fichier sp�cifi�.
     */
    public Configuration(final File file) throws IOException {
        this(new BufferedReader(new FileReader(file)));
    }

    /**
     * Construit une configuration � partir de l'URL sp�cifi�.
     */
    public Configuration(final URL url) throws IOException {
        this(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    /**
     * Construit une configuration � partir du flot sp�cifi�.
     * Le flot sera ferm� une fois la lecture termin�e.
     */
    private Configuration(final BufferedReader reader) throws IOException {
        final Properties properties;
        properties = loadProperties(reader);
        parameters = loadParameters(reader);
        reader.close();
        try {
            timezone = TimeZone.getTimeZone(getProperty(properties, "TIME_ZONE"));
            final long timeStep = Math.round((24.0 * 60 * 60 * 1000) * Double.parseDouble(getProperty(properties, "TIME_STEP")));
            final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
            dateFormat.setTimeZone(timezone);
            final Date startTime = dateFormat.parse(getProperty(properties, "START_TIME"));
            firstTimeStep = Clock.createClock(startTime, new Date(startTime.getTime() + timeStep), timezone);
            pause = Math.round(1000 * Double.parseDouble(getProperty(properties, "PAUSE")));
            grayscaleImages = Boolean.valueOf(getProperty(properties, "GRAYSCALE_IMAGES")).booleanValue();
            fisheriesVisible = Boolean.valueOf(getProperty(properties, "FISHERIES_VISIBLE")).booleanValue();
            resolution = Double.parseDouble(getProperty(properties, "RESOLUTION")) / 60;
            dailyDistance = Double.parseDouble(getProperty(properties, "DAILY_DISTANCE"));
            perceptionRadius = Double.parseDouble(getProperty(properties, "PERCEPTION_RADIUS"));
            final Set<String> species = new HashSet<String>();
            if (true) {
                final StringTokenizer tk = new StringTokenizer(getProperty(properties, "SPECIES"), ",");
                while (tk.hasMoreTokens()) {
                    species.add(tk.nextToken().trim());
                }
            }
            this.species = Collections.unmodifiableSet(species);
        } catch (ParseException exception) {
            final IOException e = new IOException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        } catch (NumberFormatException exception) {
            final IOException e = new IOException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        final int paramCount = parameters.size();
        if (false) {
            parameterArray = parameters.toArray(new Parameter[paramCount + 1]);
        } else {
            parameterArray = parameters.toArray(new Parameter[paramCount]);
        }
    }

    /**
     * Retourne la propri�t� sp�cifi�e.
     *
     * @throws NoSuchElementException si la propri�t� n'est pas d�finie.
     */
    private static String getProperty(final Properties properties, final String key) throws NoSuchElementException {
        final String property = properties.getProperty(key);
        if (property == null) {
            throw new NoSuchElementException("La propri�t� \"" + key + "\" n'est pas d�finie.");
        }
        return property;
    }

    /**
     * Charge les propri�t�s jusqu'� la ligne qui marque la fin des propri�t�s dans le fichier
     * de configuration. Ca sera la premi�re ligne qui ne contient que le caract�re '-' ou des
     * espaces. Le flot <code>reader</code> sera positionn�e apr�s cette ligne de d�marcation.
     */
    private static Properties loadProperties(final BufferedReader reader) throws IOException {
        final StringBuffer buffer = new StringBuffer();
        final String lineSeparator = System.getProperty("line.separator", "\n");
        String line;
        while ((line = reader.readLine()) != null) {
            boolean isSeparator = false;
            for (int i = line.length(); --i >= 0; ) {
                final char c = line.charAt(i);
                if (Character.isWhitespace(c)) {
                    continue;
                }
                if (c != '-') {
                    isSeparator = false;
                    break;
                }
                isSeparator = true;
            }
            if (isSeparator) {
                break;
            }
            buffer.append(line);
            buffer.append(lineSeparator);
        }
        final InputStream in = new ByteArrayInputStream(buffer.toString().getBytes());
        final Properties properties = new Properties();
        properties.load(in);
        in.close();
        return properties;
    }

    /**
     * Lit le tableau qui suit les propri�t�s et construit les objets {@link Parameter}
     * correspondants.
     */
    private static Set<Parameter> loadParameters(final BufferedReader reader) throws IOException {
        final Set<Parameter> parameters = new LinkedHashSet<Parameter>();
        final String[] args = new String[4];
        String line;
        while ((line = reader.readLine()) != null) {
            if ((line = line.trim()).length() == 0) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            final StringTokenizer tokens = new StringTokenizer(line, ";");
            Arrays.fill(args, null);
            for (int i = 0; i < args.length; i++) {
                if (!tokens.hasMoreTokens()) {
                    break;
                }
                final String token = tokens.nextToken().trim();
                if (token.length() != 0) {
                    args[i] = token;
                }
            }
            try {
                parameters.add(new Parameter(args[0], args[1], args[2], Float.parseFloat(args[3])));
            } catch (NumberFormatException exception) {
                final IOException e = new IOException(exception.getLocalizedMessage());
                e.initCause(exception);
                throw e;
            }
        }
        return Collections.unmodifiableSet(parameters);
    }

    /**
     * Retourne l'�cart de temps entre la date des param�tres � utiliser et la date
     * de la simulation.
     */
    final long getTimeLag() {
        long timelag = Long.MAX_VALUE;
        for (final Parameter param : parameters) {
            final long dt = param.timelag;
            if (dt < timelag) {
                timelag = dt;
            }
        }
        return timelag != Long.MAX_VALUE ? timelag : 0;
    }

    /**
     * Retourne une cha�ne de caract�re repr�sentant la configuration.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer("Configuration");
        final String lineSeparator = System.getProperty("line.separator", "\n");
        buffer.append(lineSeparator);
        for (final Parameter param : parameters) {
            buffer.append("    ");
            buffer.append(param);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
