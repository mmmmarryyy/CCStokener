package net.sourceforge.jgrib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A class containing static methods which deliver descriptions and names of
 * parameters, levels and units for byte codes from GRIB records.
 */
public class GribPDSParamTable {

    /**
	 * Define logger
	 */
    private static Logger logger = Logger.getLogger(GribPDSParamTable.class.getName());

    /**
	 * System property variable to search, or set, when using 
	 * user supplied gribtab directories, and or a stand alone
	 * gribtab file
	 * 
	 * !! Important: Remember to format this as an URL !!
	 */
    public static final String PROPERTY_GRIBTABURL = "GribTabURL";

    /**
    * Name of directory to search in, when reading
    * buildin parameter tables, ie stored in jgrib.jar
    */
    private static final String TABLE_DIRECTORY = "tables";

    /**
    * Name of file to read, when searching for parameter tables
    * stored in a directory
    */
    private static final String TABLE_LIST = "tablelookup.lst";

    /**
    * There is 256 entries in a parameter table due to the
    * nature of a byte
    */
    private static final int NPARAMETERS = 256;

    /**
    * Identification of center e.g. 88 for Oslo
    */
    protected int center_id;

    /**
    * Identification of center defined sub-center - not fully implemented yet
    *
    */
    protected int subcenter_id;

    /**
    * Identification of parameter table version number
    *
    */
    protected int table_number;

    /**
    * Stores the name of the file containing this table - not opened unless
    * required for lookup.
    */
    protected String filename = null;

    /**
    * URL store corresponding url of filename containint this table.
    * Opened if required for lookup.
    */
    protected URL url = null;

    /**
    * Parameters - stores array of GribPDSParameter classes
    */
    protected GribPDSParameter[] parameters = null;

    /**
    * List of parameter tables
    */
    protected static ArrayList tables = null;

    /**
    *  Added by Richard D. Gonzalez
    *  static Array with parameter tables used by the GRIB file
    * (should only be one, but not actually limited to that - this allows
    *  GRIB files to be read that have more than one center's information in it)
    *
    */
    private static GribPDSParamTable[] paramTables = null;

    /**
    * Used to store names of files
    */
    private static Map fileTabMap = new HashMap();

    /**
    * Default constructor
    */
    private GribPDSParamTable() {
    }

    /**
    * Constructor used to add extra parameter tables
    * to the default, so the volume to search can be
    * increased.
    * @param name 
 	* @param cen 
 	* @param sub 
 	* @param tab 
 	* @param par 
    */
    private GribPDSParamTable(String name, int cen, int sub, int tab, GribPDSParameter par[]) {
        filename = name;
        center_id = cen;
        subcenter_id = sub;
        table_number = tab;
        url = null;
        parameters = par;
    }

    /**
    * This method is called as the very first
    * nethod in the JGRIB library. It's primary
    * function is to turn off the warnings from
    * the Log4J library, which is printed to 
    * STDERR, if logging is not enablet.
    * 
    * If log4j is allready configured this function
    * leaves the configuration untouched.
    * 
    * - frv_peg - 2006-07-25
    */
    public static void turnOffJGRIBLogging() {
        Logger root = Logger.getRootLogger();
        boolean isOK = root.getAllAppenders().hasMoreElements();
        if (!isOK) {
            root.setLevel(Level.OFF);
        }
    }

    /**
    * - peg - As of 2005-12-09 
    * Reimplementet static method to allow for user supplied
    * directory structures as known from initFromJar and also to
    * make it possible to read a single gribtab file. IE without
    * having to create a tablelookup.lst file
    */
    static {
        try {
            GribPDSParamTable.turnOffJGRIBLogging();
            GribPDSParamTable.tables = new ArrayList();
            initDefaultTableEntries(tables);
            initFromJAR(tables);
            String gribtab = System.getProperty(PROPERTY_GRIBTABURL);
            logger.debug("JGRIB: static: gribtab = " + gribtab);
            if (true || tables.size() == 0) {
                if (gribtab != null) {
                    URL url = new URL(gribtab);
                    File gribTabFile = new File(url.getFile());
                    if (gribTabFile.isFile()) {
                        try {
                            readTableEntry(gribTabFile.toURL(), tables);
                            logger.debug("Using user supplied gribtab table!");
                        } catch (IOException e) {
                            logger.error("IOException: " + e.getMessage());
                        } catch (NotSupportedException e) {
                            logger.error("NotSupportedException: " + e.getMessage());
                        }
                    } else {
                        try {
                            readTableEntries(gribtab, tables);
                            logger.debug("Using user supplied gribtab table directory!");
                        } catch (IOException e) {
                            logger.error("IOException: " + e.getMessage());
                        }
                    }
                }
            } else {
                logger.debug("Using gribtab table from class path (jar)");
            }
            paramTables = (GribPDSParamTable[]) tables.toArray(new GribPDSParamTable[tables.size()]);
        } catch (IOException e) {
            logger.error("IOException: " + e.getMessage());
        }
    }

    /**
     * Load default tables from jar file (class path)
     *
     * Reads in the list of tables available and stores them.  Does not actually
     *    open the parameter tables files, nor store the list of parameters, but
     *    just stores the file names of the parameter tables.
     * Parameters for a table are read in when the table is requested (in the
     *    getParameterTable method).
     * Currently hardcoded the file name to "tablelookup".  May change to command
     *    line later, but would rather minimize command line inputs.
     *
     * Added by Tor C.Bekkvik
     * todo add method for appending more GRIBtables later
     * todo comments
     * todo repeated gribtables in tablelookup : load only 1 copy !
     * todo keep mapping info; keep destination table center,table etc
     * @param aTables 
     * @throws IOException 
     */
    private static void initFromJAR(ArrayList aTables) throws IOException {
        ClassLoader cl = GribPDSParamTable.class.getClassLoader();
        URL baseUrl = cl.getResource(TABLE_DIRECTORY);
        if (baseUrl == null) {
            return;
        }
        logger.debug("JGRIB: Buildin gribtab url = " + baseUrl.toExternalForm());
        readTableEntries(baseUrl.toExternalForm(), aTables);
    }

    /**
    * Initiate default tables
    * added by Tor C.Bekkvik
    * @param aTables 
    */
    private static void initDefaultTableEntries(ArrayList aTables) {
        String[][] defaulttable_ncep_reanal2 = { { "var0", "undefined", "undefined" }, { "pres", "Pressure", "Pa" }, { "prmsl", "Pressure reduced to MSL", "Pa" }, { "ptend", "Pressure tendency", "Pa/s" }, { "var4", "undefined", "undefined" }, { "var5", "undefined", "undefined" }, { "gp", "Geopotential", "m^2/s^2" }, { "hgt", "Geopotential height", "gpm" }, { "dist", "Geometric height", "m" }, { "hstdv", "Std dev of height", "m" }, { "hvar", "Varianance of height", "m^2" }, { "tmp", "Temperature", "K" }, { "vtmp", "Virtual temperature", "K" }, { "pot", "Potential temperature", "K" }, { "epot", "Pseudo-adiabatic pot. temperature", "K" }, { "tmax", "Max. temperature", "K" }, { "tmin", "Min. temperature", "K" }, { "dpt", "Dew point temperature", "K" }, { "depr", "Dew point depression", "K" }, { "lapr", "Lapse rate", "K/m" }, { "visib", "Visibility", "m" }, { "rdsp1", "Radar spectra (1)", "" }, { "rdsp2", "Radar spectra (2)", "" }, { "rdsp3", "Radar spectra (3)", "" }, { "var24", "undefined", "undefined" }, { "tmpa", "Temperature anomaly", "K" }, { "presa", "Pressure anomaly", "Pa" }, { "gpa", "Geopotential height anomaly", "gpm" }, { "wvsp1", "Wave spectra (1)", "" }, { "wvsp2", "Wave spectra (2)", "" }, { "wvsp3", "Wave spectra (3)", "" }, { "wdir", "Wind direction", "deg" }, { "wind", "Wind speed", "m/s" }, { "ugrd", "u wind", "m/s" }, { "vgrd", "v wind", "m/s" }, { "strm", "Stream function", "m^2/s" }, { "vpot", "Velocity potential", "m^2/s" }, { "mntsf", "Montgomery stream function", "m^2/s^2" }, { "sgcvv", "Sigma coord. vertical velocity", "/s" }, { "vvel", "Pressure vertical velocity", "Pa/s" }, { "dzdt", "Geometric vertical velocity", "m/s" }, { "absv", "Absolute vorticity", "/s" }, { "absd", "Absolute divergence", "/s" }, { "relv", "Relative vorticity", "/s" }, { "reld", "Relative divergence", "/s" }, { "vucsh", "Vertical u shear", "/s" }, { "vvcsh", "Vertical v shear", "/s" }, { "dirc", "Direction of current", "deg" }, { "spc", "Speed of current", "m/s" }, { "uogrd", "u of current", "m/s" }, { "vogrd", "v of current", "m/s" }, { "spfh", "Specific humidity", "kg/kg" }, { "rh", "Relative humidity", "%" }, { "mixr", "Humidity mixing ratio", "kg/kg" }, { "pwat", "Precipitable water", "kg/m^2" }, { "vapp", "Vapor pressure", "Pa" }, { "satd", "Saturation deficit", "Pa" }, { "evp", "Evaporation", "kg/m^2" }, { "cice", "Cloud Ice", "kg/m^2" }, { "prate", "Precipitation rate", "kg/m^2/s" }, { "tstm", "Thunderstorm probability", "%" }, { "apcp", "Total precipitation", "kg/m^2" }, { "ncpcp", "Large scale precipitation", "kg/m^2" }, { "acpcp", "Convective precipitation", "kg/m^2" }, { "srweq", "Snowfall rate water equiv.", "kg/m^2/s" }, { "weasd", "Accum. snow", "kg/m^2" }, { "snod", "Snow depth", "m" }, { "mixht", "Mixed layer depth", "m" }, { "tthdp", "Transient thermocline depth", "m" }, { "mthd", "Main thermocline depth", "m" }, { "mtha", "Main thermocline anomaly", "m" }, { "tcdc", "Total cloud cover", "%" }, { "cdcon", "Convective cloud cover", "%" }, { "lcdc", "Low level cloud cover", "%" }, { "mcdc", "Mid level cloud cover", "%" }, { "hcdc", "High level cloud cover", "%" }, { "cwat", "Cloud water", "kg/m^2" }, { "var77", "undefined", "undefined" }, { "snoc", "Convective snow", "kg/m^2" }, { "snol", "Large scale snow", "kg/m^2" }, { "wtmp", "Water temperature", "K" }, { "land", "Land cover (land=1;sea=0)", "fraction" }, { "dslm", "Deviation of sea level from mean", "m" }, { "sfcr", "Surface roughness", "m" }, { "albdo", "Albedo", "%" }, { "tsoil", "Soil temperature", "K" }, { "soilm", "Soil moisture content", "kg/m^2" }, { "veg", "Vegetation", "%" }, { "salty", "Salinity", "kg/kg" }, { "den", "Density", "kg/m^3" }, { "runof", "Runoff", "kg/m^2" }, { "icec", "Ice concentration (ice=1;no ice=0)", "fraction" }, { "icetk", "Ice thickness", "m" }, { "diced", "Direction of ice drift", "deg" }, { "siced", "Speed of ice drift", "m/s" }, { "uice", "u of ice drift", "m/s" }, { "vice", "v of ice drift", "m/s" }, { "iceg", "Ice growth rate", "m/s" }, { "iced", "Ice divergence", "/s" }, { "snom", "Snow melt", "kg/m^2" }, { "htsgw", "Sig height of wind waves and swell", "m" }, { "wvdir", "Direction of wind waves", "deg" }, { "wvhgt", "Sig height of wind waves", "m" }, { "wvper", "Mean period of wind waves", "s" }, { "swdir", "Direction of swell waves", "deg" }, { "swell", "Sig height of swell waves", "m" }, { "swper", "Mean period of swell waves", "s" }, { "dirpw", "Primary wave direction", "deg" }, { "perpw", "Primary wave mean period", "s" }, { "dirsw", "Secondary wave direction", "deg" }, { "persw", "Secondary wave mean period", "s" }, { "nswrs", "Net short wave (surface)", "W/m^2" }, { "nlwrs", "Net long wave (surface)", "W/m^2" }, { "nswrt", "Net short wave (top)", "W/m^2" }, { "nlwrt", "Net long wave (top)", "W/m^2" }, { "lwavr", "Long wave", "W/m^2" }, { "swavr", "Short wave", "W/m^2" }, { "grad", "Global radiation", "W/m^2" }, { "var118", "undefined", "undefined" }, { "var119", "undefined", "undefined" }, { "var120", "undefined", "undefined" }, { "lhtfl", "Latent heat flux", "W/m^2" }, { "shtfl", "Sensible heat flux", "W/m^2" }, { "blydp", "Boundary layer dissipation", "W/m^2" }, { "uflx", "Zonal momentum flux", "N/m^2" }, { "vflx", "Meridional momentum flux", "N/m^2" }, { "wmixe", "Wind mixing energy", "J" }, { "imgd", "Image data", "" }, { "mslsa", "Mean sea level pressure (Std Atm)", "Pa" }, { "mslma", "Mean sea level pressure (MAPS)", "Pa" }, { "mslet", "Mean sea level pressure (ETA model)", "Pa" }, { "lftx", "Surface lifted index", "K" }, { "4lftx", "Best (4-layer) lifted index", "K" }, { "kx", "K index", "K" }, { "sx", "Sweat index", "K" }, { "mconv", "Horizontal moisture divergence", "kg/kg/s" }, { "vssh", "Vertical speed shear", "1/s" }, { "tslsa", "3-hr pressure tendency (Std Atmos Red)", "Pa/s" }, { "bvf2", "Brunt-Vaisala frequency^2", "1/s^2" }, { "pvmw", "Potential vorticity (mass-weighted)", "1/s/m" }, { "crain", "Categorical rain", "yes=1;no=0" }, { "cfrzr", "Categorical freezing rain", "yes=1;no=0" }, { "cicep", "Categorical ice pellets", "yes=1;no=0" }, { "csnow", "Categorical snow", "yes=1;no=0" }, { "soilw", "Volumetric soil moisture", "fraction" }, { "pevpr", "Potential evaporation rate", "W/m^2" }, { "cwork", "Cloud work function", "J/kg" }, { "u-gwd", "Zonal gravity wave stress", "N/m^2" }, { "v-gwd", "Meridional gravity wave stress", "N/m^2" }, { "pvort", "Potential vorticity", "m^2/s/kg" }, { "var150", "undefined", "undefined" }, { "var151", "undefined", "undefined" }, { "var152", "undefined", "undefined" }, { "mfxdv", "Moisture flux divergence", "gr/gr*m/s/m" }, { "vqr154", "undefined", "undefined" }, { "gflux", "Ground heat flux", "W/m^2" }, { "cin", "Convective inhibition", "J/kg" }, { "cape", "Convective Avail. Pot. Energy", "J/kg" }, { "tke", "Turbulent kinetic energy", "J/kg" }, { "condp", "Lifted parcel condensation pressure", "Pa" }, { "csusf", "Clear sky upward solar flux", "W/m^2" }, { "csdsf", "Clear sky downward solar flux", "W/m^2" }, { "csulf", "Clear sky upward long wave flux", "W/m^2" }, { "csdlf", "Clear sky downward long wave flux", "W/m^2" }, { "cfnsf", "Cloud forcing net solar flux", "W/m^2" }, { "cfnlf", "Cloud forcing net long wave flux", "W/m^2" }, { "vbdsf", "Visible beam downward solar flux", "W/m^2" }, { "vddsf", "Visible diffuse downward solar flux", "W/m^2" }, { "nbdsf", "Near IR beam downward solar flux", "W/m^2" }, { "nddsf", "Near IR diffuse downward solar flux", "W/m^2" }, { "ustr", "U wind stress", "N/m^2" }, { "vstr", "V wind stress", "N/m^2" }, { "mflx", "Momentum flux", "N/m^2" }, { "lmh", "Mass point model surface", "" }, { "lmv", "Velocity point model surface", "" }, { "sglyr", "Neraby model level", "" }, { "nlat", "Latitude", "deg" }, { "nlon", "Longitude", "deg" }, { "umas", "Mass weighted u", "gm/m*K*s" }, { "vmas", "Mass weigtted v", "gm/m*K*s" }, { "gust", "Wind gust", "m/s" }, { "lpsx", "x-gradient of log pressure", "1/m" }, { "lpsy", "y-gradient of log pressure", "1/m" }, { "hgtx", "x-gradient of height", "m/m" }, { "hgty", "y-gradient of height", "m/m" }, { "stdz", "Standard deviation of Geop. hgt.", "m" }, { "stdu", "Standard deviation of zonal wind", "m/s" }, { "stdv", "Standard deviation of meridional wind", "m/s" }, { "stdq", "Standard deviation of spec. hum.", "gm/gm" }, { "stdt", "Standard deviation of temperature", "K" }, { "cbuw", "Covariance between u and omega", "m/s*Pa/s" }, { "cbvw", "Covariance between v and omega", "m/s*Pa/s" }, { "cbuq", "Covariance between u and specific hum", "m/s*gm/gm" }, { "cbvq", "Covariance between v and specific hum", "m/s*gm/gm" }, { "cbtw", "Covariance between T and omega", "K*Pa/s" }, { "cbqw", "Covariance between spec. hum and omeg", "gm/gm*Pa/s" }, { "cbmzw", "Covariance between v and u", "m^2/si^2" }, { "cbtzw", "Covariance between u and T", "K*m/s" }, { "cbtmw", "Covariance between v and T", "K*m/s" }, { "stdrh", "Standard deviation of Rel. Hum.", "%" }, { "sdtz", "Std dev of time tend of geop. hgt", "m" }, { "icwat", "Ice-free water surface", "%" }, { "sdtu", "Std dev of time tend of zonal wind", "m/s" }, { "sdtv", "Std dev of time tend of merid wind", "m/s" }, { "dswrf", "Downward solar radiation flux", "W/m^2" }, { "dlwrf", "Downward long wave radiation flux", "W/m^2" }, { "sdtq", "Std dev of time tend of spec. hum", "gm/gm" }, { "mstav", "Moisture availability", "%" }, { "sfexc", "Exchange coefficient", "(kg/m^3)(m/s)" }, { "mixly", "No. of mixed layers next to surface", "integer" }, { "sdtt", "Std dev of time tend of temperature", "K" }, { "uswrf", "Upward short wave flux", "W/m^2" }, { "ulwrf", "Upward long wave flux", "W/m^2" }, { "cdlyr", "Non-convective cloud", "%" }, { "cprat", "Convective precip. rate", "kg/m^2/s" }, { "ttdia", "Temperature tendency by all physics", "K/s" }, { "ttrad", "Temperature tendency by all radiation", "K/s" }, { "ttphy", "Temperature tendency by non-radiation physics", "K/s" }, { "preix", "Precip index (0.0-1.00)", "fraction" }, { "tsd1d", "Std. dev. of IR T over 1x1 deg area", "K" }, { "nlgsp", "Natural log of surface pressure", "ln(kPa)" }, { "sdtrh", "Std dev of time tend of rel humt", "%" }, { "5wavh", "5-wave geopotential height", "gpm" }, { "cwat", "Plant canopy surface water", "kg/m^2" }, { "pltrs", "Maximum stomato plant resistance", "s/m" }, { "rhcld", "RH-type cloud cover", "%" }, { "bmixl", "Blackadar's mixing length scale", "m" }, { "amixl", "Asymptotic mixing length scale", "m" }, { "pevap", "Potential evaporation", "kg^2" }, { "snohf", "Snow melt heat flux", "W/m^2" }, { "snoev", "Snow sublimation heat flux", "W/m^2" }, { "mflux", "Convective cloud mass flux", "Pa/s" }, { "dtrf", "Downward total radiation flux", "W/m^2" }, { "utrf", "Upward total radiation flux", "W/m^2" }, { "bgrun", "Baseflow-groundwater runoff", "kg/m^2" }, { "ssrun", "Storm surface runoff", "kg/m^2" }, { "var236", "undefined", "undefined" }, { "ozone", "Total column ozone concentration", "Dobson" }, { "snoc", "Snow cover", "%" }, { "snot", "Snow temperature", "K" }, { "glcr", "Permanent snow points", "mask" }, { "lrghr", "Large scale condensation heating rate", "K/s" }, { "cnvhr", "Deep convective heating rate", "K/s" }, { "cnvmr", "Deep convective moistening rate", "kg/kg/s" }, { "shahr", "Shallow convective heating rate", "K/s" }, { "shamr", "Shallow convective moistening rate", "kg/kg/s" }, { "vdfhr", "Vertical diffusion heating rate", "K/s" }, { "vdfua", "Vertical diffusion zonal accel", "m/s/s" }, { "vdfva", "Vertical diffusion meridional accel", "m/s/s" }, { "vdfmr", "Vertical diffusion moistening rate", "kg/kg/s" }, { "swhr", "Solar radiative heating rate", "K/s" }, { "lwhr", "Longwave radiative heating rate", "K/s" }, { "cd", "Drag coefficient", "" }, { "fricv", "Friction velocity", "m/s" }, { "ri", "Richardson number", "" }, { "var255", "undefined", "undefined" } };
        int npar = defaulttable_ncep_reanal2.length;
        GribPDSParameter[] parameters = new GribPDSParameter[npar];
        for (int n = 0; n < npar; ++n) {
            String pname = defaulttable_ncep_reanal2[n][0];
            String pdesc = defaulttable_ncep_reanal2[n][1];
            String punit = defaulttable_ncep_reanal2[n][2];
            parameters[n] = new GribPDSParameter(n, pname, pdesc, punit);
        }
        aTables.add(new GribPDSParamTable("ncep_reanal2.1", 7, -1, 1, parameters));
        aTables.add(new GribPDSParamTable("ncep_reanal2.2", 7, -1, 2, parameters));
        aTables.add(new GribPDSParamTable("ncep_reanal2.3", 7, -1, 3, parameters));
        aTables.add(new GribPDSParamTable("ncep_reanal2.4", 81, -1, 3, parameters));
        aTables.add(new GribPDSParamTable("ncep_reanal2.5", 88, -1, 2, parameters));
        aTables.add(new GribPDSParamTable("ncep_reanal2.6", 88, -1, 128, parameters));
    }

    /**
    * @param aFileUrl
    * @param aTables
    * @throws IOException
    * @throws NotSupportedException
    */
    protected static void readTableEntry(URL aFileUrl, ArrayList aTables) throws IOException, NotSupportedException {
        logger.debug("JGRIB: readTableEntry: aFileUrl = " + aFileUrl.toString());
        InputStreamReader isr = new InputStreamReader(aFileUrl.openStream());
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();
        if (line.length() == 0 || line.startsWith("//")) {
            throw new NotSupportedException("Gribtab files cannot start with blanks " + "or comments, - Please follow standard (-1:center:subcenter:tablenumber)");
        }
        GribPDSParamTable table = new GribPDSParamTable();
        String[] tableDefArr = SmartStringArray.split(":", line);
        table.center_id = Integer.parseInt(tableDefArr[1].trim());
        table.subcenter_id = Integer.parseInt(tableDefArr[2].trim());
        table.table_number = Integer.parseInt(tableDefArr[3].trim());
        table.filename = aFileUrl.toExternalForm();
        table.url = aFileUrl;
        aTables.add(table);
        br.close();
        isr.close();
    }

    /**
    * @param aBaseUrl
    * @param aTables
    * @throws IOException
    */
    private static void readTableEntries(String aBaseUrl, ArrayList aTables) throws IOException {
        logger.debug("JGRIB: readTableEntries: aBaseUrl =" + aBaseUrl);
        InputStream is = new URL(aBaseUrl + "/" + TABLE_LIST).openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.length() == 0 || line.startsWith("//")) continue;
            GribPDSParamTable table = new GribPDSParamTable();
            int cix = line.indexOf("//");
            if (cix > 0) {
                line = line.substring(0, cix);
            }
            String[] tableDefArr = SmartStringArray.split(":", line);
            if (tableDefArr == null || tableDefArr.length < 4) continue;
            table.center_id = Integer.parseInt(tableDefArr[0].trim());
            table.subcenter_id = Integer.parseInt(tableDefArr[1].trim());
            table.table_number = Integer.parseInt(tableDefArr[2].trim());
            table.filename = tableDefArr[3].trim();
            table.url = new URL(aBaseUrl + "/" + table.filename);
            aTables.add(table);
        }
        is.close();
    }

    /**
    * Looks for the parameter table which matches the center, subcenter
    *    and table version from the tables array.
    * If this is the first time asking for this table, then the parameters for
    *    this table have not been read in yet, so this is done as well.
    *
    * @param center - integer from PDS octet 5, representing Center.
    * @param subcenter - integer from PDS octet 26, representing Subcenter
    * @param number - integer from PDS octet 4, representing Parameter Table Version
    * @return GribPDSParamTable matching center, subcenter, and number
    * @throws NotSupportedException 
    */
    public static GribPDSParamTable getParameterTable(int center, int subcenter, int number) throws NotSupportedException {
        for (int i = paramTables.length - 1; i >= 0; i--) {
            GribPDSParamTable table = paramTables[i];
            if (table.center_id == -1) continue;
            if (center == table.center_id) {
                if (table.subcenter_id == -1 || subcenter == table.subcenter_id) {
                    if (number == table.table_number) {
                        table.readParameterTable();
                        return table;
                    }
                }
            }
        }
        for (int i = paramTables.length - 1; i >= 0; i--) {
            GribPDSParamTable table = paramTables[i];
            if (table.center_id == -1 && number == table.table_number) {
                table.readParameterTable();
                return table;
            }
        }
        throw new NotSupportedException("Grib table not supported; cent " + center + ",sub " + subcenter + ",table " + number);
    }

    /**
    * Get the parameter with id <tt>id</tt>.
    * @param id 
    * @return description of the unit for the parameter
    */
    public GribPDSParameter getParameter(int id) {
        if (id < 0 || id >= NPARAMETERS) throw new IllegalArgumentException("Bad id: " + id);
        if (parameters[id] == null) return new GribPDSParameter(id, "undef_" + id, "undef", "undef");
        return parameters[id];
    }

    /**
    * Get the tag/name of the parameter with id <tt>id</tt>.
    *
    * @param id 
    * @return tag/name of the parameter
    */
    public String getParameterTag(int id) {
        return getParameter(id).getName();
    }

    /**
    * Get a description for the parameter with id <tt>id</tt>.
    *
    * @param id 
    * @return description for the parameter
    */
    public String getParameterDescription(int id) {
        return getParameter(id).getDescription();
    }

    /**
    * Get a description for the unit with id <tt>id</tt>.
    *
    * @param id 
    * @return description of the unit for the parameter
    */
    public String getParameterUnit(int id) {
        return getParameter(id).getUnit();
    }

    /**
    * @param pdsPar
    * @return true/false
    */
    private boolean setParameter(GribPDSParameter pdsPar) {
        if (pdsPar == null) return false;
        int id = pdsPar.getNumber();
        if (id < 0 || id >= NPARAMETERS) return false;
        parameters[id] = pdsPar;
        return true;
    }

    /**
    * 
    * @return center_id
    */
    public int getCenter_id() {
        return center_id;
    }

    /**
    * 
    * @return subcenter_id
    */
    public int getSubcenter_id() {
        return subcenter_id;
    }

    /**
    * 
    * @return table number
    */
    public int getTable_number() {
        return table_number;
    }

    /**
    * 
    * @return filename
    */
    public String getFilename() {
        return filename;
    }

    /**
    * 
    * @return url to file
    */
    public URL getUrl() {
        return url;
    }

    private void readParameterTable() {
        if (this.parameters != null) return;
        parameters = new GribPDSParameter[NPARAMETERS];
        int center;
        int subcenter;
        int number;
        try {
            BufferedReader br;
            if (filename != null && filename.length() > 0) {
                GribPDSParamTable tab = (GribPDSParamTable) fileTabMap.get(filename);
                if (tab != null) {
                    this.parameters = tab.parameters;
                    return;
                }
            }
            if (url != null) {
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
            } else {
                br = new BufferedReader(new FileReader("tables\\" + filename));
            }
            String line = br.readLine();
            String[] tableDefArr = SmartStringArray.split(":", line);
            center = Integer.parseInt(tableDefArr[1].trim());
            subcenter = Integer.parseInt(tableDefArr[2].trim());
            number = Integer.parseInt(tableDefArr[3].trim());
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("//")) continue;
                GribPDSParameter parameter = new GribPDSParameter();
                tableDefArr = SmartStringArray.split(":", line);
                parameter.number = Integer.parseInt(tableDefArr[0].trim());
                parameter.name = tableDefArr[1].trim();
                if (tableDefArr[2].indexOf('[') == -1) {
                    parameter.description = parameter.unit = tableDefArr[2].trim();
                } else {
                    String[] arr2 = SmartStringArray.split("[", tableDefArr[2]);
                    parameter.description = arr2[0].trim();
                    parameter.unit = arr2[1].substring(0, arr2[1].lastIndexOf(']')).trim();
                }
                if (!this.setParameter(parameter)) {
                    System.err.println("Warning, bad parameter ignored (" + filename + "): " + parameter.toString());
                }
            }
            if (filename != null && filename.length() > 0) {
                GribPDSParamTable loadedTable = new GribPDSParamTable(filename, center, subcenter, number, this.parameters);
                fileTabMap.put(filename, loadedTable);
            }
        } catch (IOException ioError) {
            System.err.println("An error occurred in GribPDSParamTable while " + "trying to open the parameter table " + filename + " : " + ioError);
        }
    }

    /**
    * Overrides Object.toString()
    * 
    * @see java.lang.Object#toString()
    * @return String representation of the object
    */
    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("-1:" + center_id + ":" + subcenter_id + ":" + table_number + "\n");
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] == null) continue;
                str.append(parameters[i].toString() + "\n");
            }
        }
        return str.toString();
    }
}
