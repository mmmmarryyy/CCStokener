package openadmin.util.territoryload;

import openadmin.model.territory.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/** Clase ServiciosCallejero
 * Contiene las funciones estaticas relacionadas con los servicios de callejero y datos catastrales no protegidos
 * @author Francisco Martinez
 * @version 1.0
 */
public class ServiciosCallejero {

    private static String baseURL[] = { "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejeroCodigos.asmx/ConsultaProvincia?", "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejeroCodigos.asmx/ConsultaMunicipioCodigos?CodigoProvincia=" + "<prov>" + "&CodigoMunicipio=&CodigoMunicipioIne=", "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejeroCodigos.asmx/ConsultaViaCodigos?CodigoProvincia=" + "<prov>" + "&CodigoMunicipio=" + "<codMEH>" + "&CodigoMunicipioINE=" + "<codINE>" + "&CodigoVia=", "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejeroCodigos.asmx/ConsultaNumeroCodigos?CodigoProvincia=" + "<prov>" + "&CodigoMunicipio=" + "<codMEH>" + "&CodigoMunicipioINE=" + "<codINE>" + "&CodigoVia=" + "<via>" + "&Numero=" + "<num>", "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejeroCodigos.asmx/Consulta_DNPLOC_Codigos?CodigoProvincia=" + "<prov>" + "&CodigoMunicipio=" + "<codMEH>" + "&CodigoMunicipioINE=" + "<codINE>" + "&CodigoVia=" + "<via>" + "&Numero=" + "<num>" + "&Bloque=&Escalera=&Planta=&Puerta=", "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejeroCodigos.asmx/Consulta_DNPRC_Codigos?CodigoProvincia=" + "<prov>" + "&CodigoMunicipio=" + "<codMEH>" + "&CodigoMunicipioINE=" + "<codINE>" + "&RC=" + "<rc>", "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejeroCodigos.asmx/Consulta_DNPPP_Codigos?CodigoProvincia=" + "<prov>" + "&CodigoMunicipio=" + "<codMEH>" + "&CodigoMunicipioINE=" + "<codINE>" + "&Poligono=" + "<pol>" + "&Parcela=" + "<par>" };

    /** Metodo getProvincias
	  * Sirve para obtener el listado de provincias espa�olas en las que tiene competencia el Catastro,
	  * se conecta a la direccion generica del Catastro,
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @return Set<Province> Conjunto con las provincias obtenidas
	  */
    public static Set<Province> getProvincias() {
        return getProvincias(baseURL[0]);
    }

    /** Metodo getProvincias
	  * Sirve para obtener el listado de provincias espa�olas en las que tiene competencia el Catastro, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param String pURL url del servicio al que se accede
	  * @return Set<Province> Conjunto con las provincias obtenidas
	  */
    public static Set<Province> getProvincias(String pURL) {
        Set<Province> result = new HashSet<Province>();
        String iniProv = "<prov>";
        String finProv = "</prov>";
        String iniNomProv = "<np>";
        String finNomProv = "</np>";
        String iniCodigo = "<cpine>";
        String finCodigo = "</cpine>";
        int ini, fin;
        try {
            URL url = new URL(pURL);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            Province provincia;
            while ((str = br.readLine()) != null) {
                if (str.contains(iniProv)) {
                    provincia = new Province();
                    while ((str = br.readLine()) != null && !str.contains(finProv)) {
                        if (str.contains(iniNomProv)) {
                            ini = str.indexOf(iniNomProv) + iniNomProv.length();
                            fin = str.indexOf(finNomProv);
                            provincia.setDescription(str.substring(ini, fin));
                        }
                        if (str.contains(iniCodigo)) {
                            ini = str.indexOf(iniCodigo) + iniCodigo.length();
                            fin = str.indexOf(finCodigo);
                            provincia.setCodeProvince(Integer.parseInt(str.substring(ini, fin)));
                        }
                    }
                    result.add(provincia);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println(e);
        }
        return result;
    }

    /** Metodo getMunicipios
	  * Sirve para obtener el listado de municipios a partir de una determinada provincia,
	  * se conecta a la direccion generica del Catastro,
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param int prov codigo de la provincia de la que se quiere obtener el listado
	  * @return Set<Municipality> conjunto con los municipios obtenidas
	  */
    public static Set<Municipality> getMunicipios(int prov) {
        return getMunicipios(baseURL[1].replace("<prov>", Integer.toString(prov)));
    }

    /** Metodo getMunicipios
	  * Sirve para obtener el listado de municipios a partir de una determinada provincia, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param String pURL url del servicio al que se accede
	  * @return Set<Municipality> conjunto con los municipios obtenidas
	  */
    public static Set<Municipality> getMunicipios(String pURL) {
        Set<Municipality> result = new HashSet<Municipality>();
        String iniCuerr = "<cuerr>";
        String finCuerr = "</cuerr>";
        String iniDesErr = "<des>";
        String finDesErr = "</des>";
        String iniMun = "<muni>";
        String finMun = "</muni>";
        String iniNomMun = "<nm>";
        String finNomMun = "</nm>";
        String iniCarto = "<carto>";
        String iniCodDelMEH = "<cd>";
        String finCodDelMEH = "</cd>";
        String iniCodMunMEH = "<cmc>";
        String finCodMunMEH = "</cmc>";
        String iniCodProvINE = "<cp>";
        String finCodProvINE = "</cp>";
        String iniCodMunINE = "<cm>";
        String finCodMunINE = "</cm>";
        boolean error = false;
        int ini, fin;
        try {
            URL url = new URL(pURL);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            Municipality municipio;
            while ((str = br.readLine()) != null) {
                if (str.contains(iniCuerr)) {
                    ini = str.indexOf(iniCuerr) + iniCuerr.length();
                    fin = str.indexOf(finCuerr);
                    if (Integer.parseInt(str.substring(ini, fin)) > 0) error = true;
                }
                if (error) {
                    if (str.contains(iniDesErr)) {
                        ini = str.indexOf(iniDesErr) + iniDesErr.length();
                        fin = str.indexOf(finDesErr);
                        throw (new Exception(str.substring(ini, fin)));
                    }
                } else {
                    if (str.contains(iniMun)) {
                        municipio = new Municipality();
                        municipio.setCodemunicipalityine(0);
                        municipio.setCodemunicipalitydgc(0);
                        while ((str = br.readLine()) != null && !str.contains(finMun)) {
                            if (str.contains(iniNomMun)) {
                                ini = str.indexOf(iniNomMun) + iniNomMun.length();
                                fin = str.indexOf(finNomMun);
                                municipio.setMuniName(str.substring(ini, fin).trim());
                            }
                            if (str.contains(iniCarto)) {
                                if (str.contains("URBANA")) municipio.setIsurban(true);
                                if (str.contains("RUSTICA")) municipio.setIsrustic(true);
                            }
                            if (str.contains(iniCodDelMEH)) {
                                ini = str.indexOf(iniCodDelMEH) + iniCodDelMEH.length();
                                fin = str.indexOf(finCodDelMEH);
                                municipio.setCodemunicipalitydgc(municipio.getCodemunicipalitydgc() + Integer.parseInt(str.substring(ini, fin)) * 1000);
                            }
                            if (str.contains(iniCodMunMEH)) {
                                ini = str.indexOf(iniCodMunMEH) + iniCodMunMEH.length();
                                fin = str.indexOf(finCodMunMEH);
                                municipio.setCodemunicipalitydgc(municipio.getCodemunicipalitydgc() + Integer.parseInt(str.substring(ini, fin)));
                            }
                            if (str.contains(iniCodProvINE)) {
                                ini = str.indexOf(iniCodProvINE) + iniCodProvINE.length();
                                fin = str.indexOf(finCodProvINE);
                                municipio.setCodemunicipalityine(municipio.getCodemunicipalityine() + Integer.parseInt(str.substring(ini, fin)) * 1000);
                            }
                            if (str.contains(iniCodMunINE)) {
                                ini = str.indexOf(iniCodMunINE) + iniCodMunINE.length();
                                fin = str.indexOf(finCodMunINE);
                                municipio.setCodemunicipalityine(municipio.getCodemunicipalityine() + Integer.parseInt(str.substring(ini, fin)));
                            }
                            municipio.setDescription();
                        }
                        result.add(municipio);
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println(e);
        }
        return result;
    }

    /** Metodo getVias
	  * Sirve para obtener el listado de vias de un determinado municipio a partir de su codigo segun MEH,
	  * se conecta a la direccion generica del Catastro,
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param int prov codigo de la provincia a la que pertenece el municipio
	  * @param int codigo codigo del municipio segun MEH del que se quiere obtener el listado
	  * @return Set<Street> conjunto con las vias obtenidas
	  */
    public static Set<Street> getVias(int prov, int codigo) {
        return getVias(baseURL[2].replace("<prov>", Integer.toString(prov)).replace("<codMEH>", Integer.toString(codigo - codigo / 1000 * 1000)).replace("<codINE>", ""));
    }

    /** Metodo getViasINE
	  * Sirve para obtener el listado de vias de un determinado municipio a partir de su codigo segun INE,
	  * se conecta a la direccion generica del Catastro,
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param int prov codigo de la provincia a la que pertenece el municipio
	  * @param int codigo codigo del municipio segun INE del que se quiere obtener el listado
	  * @return Set<Street> conjunto con las vias obtenidas
	  */
    public static Set<Street> getViasINE(int prov, int codigo) {
        return getVias(baseURL[2].replace("<prov>", Integer.toString(prov)).replace("<codINE>", Integer.toString(codigo - codigo / 1000 * 1000)).replace("<codMEH>", ""));
    }

    /** Metodo getVias
	  * Sirve para obtener el listado de vias de un determinado municipio, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param String pURL url del servicio al que se accede
	  * @return Set<Street> conjunto con los vias obtenidas
	  */
    public static Set<Street> getVias(String pURL) {
        Set<Street> result = new HashSet<Street>();
        String iniCuerr = "<cuerr>";
        String finCuerr = "</cuerr>";
        String iniDesErr = "<des>";
        String finDesErr = "</des>";
        String iniVia = "<calle>";
        String finVia = "</calle>";
        String iniCodVia = "<cv>";
        String finCodVia = "</cv>";
        String iniTipoVia = "<tv>";
        String finTipoVia = "</tv>";
        String iniNomVia = "<nv>";
        String finNomVia = "</nv>";
        boolean error = false;
        int ini, fin;
        try {
            URL url = new URL(pURL);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            Street via;
            while ((str = br.readLine()) != null) {
                if (str.contains(iniCuerr)) {
                    ini = str.indexOf(iniCuerr) + iniCuerr.length();
                    fin = str.indexOf(finCuerr);
                    if (Integer.parseInt(str.substring(ini, fin)) > 0) error = true;
                }
                if (error) {
                    if (str.contains(iniDesErr)) {
                        ini = str.indexOf(iniDesErr) + iniDesErr.length();
                        fin = str.indexOf(finDesErr);
                        throw (new Exception(str.substring(ini, fin)));
                    }
                } else {
                    if (str.contains(iniVia)) {
                        via = new Street();
                        while ((str = br.readLine()) != null && !str.contains(finVia)) {
                            if (str.contains(iniCodVia)) {
                                ini = str.indexOf(iniCodVia) + iniCodVia.length();
                                fin = str.indexOf(finCodVia);
                                via.setCodeStreet(Integer.parseInt(str.substring(ini, fin)));
                            }
                            if (str.contains(iniTipoVia)) {
                                TypeStreet tipo = new TypeStreet();
                                if (!str.contains(finTipoVia)) tipo.setCodetpStreet(""); else {
                                    ini = str.indexOf(iniTipoVia) + iniTipoVia.length();
                                    fin = str.indexOf(finTipoVia);
                                    tipo.setCodetpStreet(str.substring(ini, fin));
                                }
                                tipo.setDescription(getDescripcionTipoVia(tipo.getCodetpStreet()));
                                via.setTypeStreet(tipo);
                            }
                            if (str.contains(iniNomVia)) {
                                ini = str.indexOf(iniNomVia) + iniNomVia.length();
                                fin = str.indexOf(finNomVia);
                                via.setStreetName(str.substring(ini, fin).trim());
                            }
                        }
                        result.add(via);
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println(e);
        }
        return result;
    }

    /** Metodo getRefNumerosVia
	  * Sirve para obtener la referencia catastral del domicilio situado en ese lugar,
	  * se conecta a la direccion generica del Catastro,
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param int prov codigo de la provincia a la que pertenece el municipio
	  * @param int cod codigo del municipio segun MEH del que se quiere obtener la info
	  * @param int via codigo de la via segun el catastro
	  * @param int num numero del domicilio a buscar
	  * @return String referencia catastral obtenida
	  */
    public static String getRefNumeroVia(int prov, int cod, int via, int num) {
        return getRefNumeroVia(baseURL[3].replace("<prov>", Integer.toString(prov)).replace("<codMEH>", Integer.toString(cod - cod / 1000 * 1000)).replace("<codINE>", "").replace("<via>", Integer.toString(via)).replace("<num>", Integer.toString(num)));
    }

    /** Metodo getRefNumeroViaINE
	  * Sirve para obtener la referencia catastral del domicilio situado en ese lugar,
	  * se conecta a la direccion generica del Catastro,
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param int prov codigo de la provincia a la que pertenece el municipio
	  * @param int cod codigo del municipio segun INE del que se quiere obtener la info
	  * @param int via codigo de la via segun el catastro
	  * @param int num numero del domicilio a buscar
	  * @return String referencia catastral obtenida
	  */
    public static String getRefNumeroViaINE(int prov, int cod, int via, int num) {
        return getRefNumeroVia(baseURL[3].replace("<prov>", Integer.toString(prov)).replace("<codINE>", Integer.toString(cod - cod / 1000 * 1000)).replace("<codMEH>", "").replace("<via>", Integer.toString(via)).replace("<num>", Integer.toString(num)));
    }

    /** Metodo getRefNumeroVia
	  * Sirve para obtener la referencia catastral del domicilio situado en ese lugar, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param String pURL url del servicio al que se accede
	  * @return String referencia catastral obtenida
	  */
    public static String getRefNumeroVia(String pURL) {
        return ConversorCoordenadas.getRefCatastral(pURL);
    }

    public static Set<Address> getDatosCatastralesLocalizacion(int prov, int mun, int via, int num) {
        Set<Address> result = getDatosCatastrales(baseURL[4].replace("<prov>", Integer.toString(prov)).replace("<codMEH>", Integer.toString(mun - mun / 1000 * 1000)).replace("<codINE>", "").replace("<via>", Integer.toString(via)).replace("<num>", Integer.toString(num)));
        if (result.size() > 1) {
            Object ad[] = result.toArray();
            Coordinate coord = ((Address) getDatosCatastralesRefCatastral(prov, mun, ((Address) ad[0]).getDescription()).toArray()[0]).getCoodinate();
            for (Address inm : result) inm.setCoodinate(coord);
        }
        return result;
    }

    public static Set<Address> getDatosCatastralesLocalizacionINE(int prov, int mun, int via, int num) {
        Set<Address> result = getDatosCatastrales(baseURL[4].replace("<prov>", Integer.toString(prov)).replace("<codINE>", Integer.toString(mun - mun / 1000 * 1000)).replace("<codMEH>", "").replace("<via>", Integer.toString(via)).replace("<num>", Integer.toString(num)));
        if (result.size() > 1) {
            Object ad[] = result.toArray();
            Coordinate coord = ((Address) getDatosCatastralesRefCatastral(prov, mun, ((Address) ad[0]).getDescription()).toArray()[0]).getCoodinate();
            for (Address inm : result) inm.setCoodinate(coord);
        }
        return result;
    }

    public static Set<Address> getDatosCatastralesRefCatastral(int prov, int mun, String rc) {
        Set<Address> result = getDatosCatastrales(baseURL[5].replace("<prov>", Integer.toString(prov)).replace("<codMEH>", Integer.toString(mun - mun / 1000 * 1000)).replace("<codINE>", "").replace("<rc>", rc));
        if (result.size() > 1) {
            Object ad[] = result.toArray();
            Coordinate coord = ((Address) getDatosCatastralesRefCatastral(prov, mun, ((Address) ad[0]).getDescription()).toArray()[0]).getCoodinate();
            for (Address inm : result) inm.setCoodinate(coord);
        }
        return result;
    }

    public static Set<Address> getDatosCatastralesRefCatastralINE(int prov, int mun, String rc) {
        Set<Address> result = getDatosCatastrales(baseURL[5].replace("<prov>", Integer.toString(prov)).replace("<codINE>", Integer.toString(mun - mun / 1000 * 1000)).replace("<codMEH>", "").replace("<rc>", rc));
        if (result.size() > 1) {
            Object ad[] = result.toArray();
            Coordinate coord = ((Address) getDatosCatastralesRefCatastral(prov, mun, ((Address) ad[0]).getDescription()).toArray()[0]).getCoodinate();
            for (Address inm : result) inm.setCoodinate(coord);
        }
        return result;
    }

    public static Set<Address> getDatosCatastralesPoligonoParcela(int prov, int mun, int pol, int par) {
        Set<Address> result = getDatosCatastrales(baseURL[6].replace("<prov>", Integer.toString(prov)).replace("<codMEH>", Integer.toString(mun - mun / 1000 * 1000)).replace("<codINE>", "").replace("<pol>", Integer.toString(pol)).replace("<par>", Integer.toString(par)));
        if (result.size() > 1) {
            Object ad[] = result.toArray();
            Coordinate coord = ((Address) getDatosCatastralesRefCatastral(prov, mun, ((Address) ad[0]).getDescription()).toArray()[0]).getCoodinate();
            for (Address inm : result) inm.setCoodinate(coord);
        }
        return result;
    }

    public static Set<Address> getDatosCatastralesPoligonoParcelaINE(int prov, int mun, int pol, int par) {
        Set<Address> result = getDatosCatastrales(baseURL[6].replace("<prov>", Integer.toString(prov)).replace("<codINE>", Integer.toString(mun - mun / 1000 * 1000)).replace("<codMEH>", "").replace("<pol>", Integer.toString(pol)).replace("<par>", Integer.toString(par)));
        if (result.size() > 1) {
            Object ad[] = result.toArray();
            Coordinate coord = ((Address) getDatosCatastralesRefCatastral(prov, mun, ((Address) ad[0]).getDescription()).toArray()[0]).getCoodinate();
            for (Address inm : result) inm.setCoodinate(coord);
        }
        return result;
    }

    public static Set<Address> getDatosCatastrales(String pURL) {
        Set<Address> result = new HashSet<Address>();
        String iniCuerr = "<cuerr>";
        String finCuerr = "</cuerr>";
        String iniDesErr = "<des>";
        String finDesErr = "</des>";
        String iniInm1 = "<rcdnp>";
        String finInm1 = "</rcdnp>";
        String iniInm2 = "<bi>";
        String finInm2 = "</bi>";
        String iniPC1 = "<pc1>";
        String iniPC2 = "<pc2>";
        String finPC1 = "</pc1>";
        String finPC2 = "</pc2>";
        String iniCar = "<car>";
        String finCar = "</car>";
        String iniCC1 = "<cc1>";
        String finCC1 = "</cc1>";
        String iniCC2 = "<cc2>";
        String finCC2 = "</cc2>";
        String iniLDT = "<ldt>";
        String iniBq = "<bq>";
        String finBq = "</bq>";
        String iniEs = "<es>";
        String finEs = "</es>";
        String iniPt = "<pt>";
        String finPt = "</pt>";
        String iniPu = "<pu>";
        String finPu = "</pu>";
        boolean error = false;
        int ini, fin;
        int postal = 0;
        try {
            URL url = new URL(pURL);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = br.readLine()) != null) {
                if (str.contains(iniCuerr)) {
                    ini = str.indexOf(iniCuerr) + iniCuerr.length();
                    fin = str.indexOf(finCuerr);
                    if (Integer.parseInt(str.substring(ini, fin)) > 0) error = true;
                }
                if (error) {
                    if (str.contains(iniDesErr)) {
                        ini = str.indexOf(iniDesErr) + iniDesErr.length();
                        fin = str.indexOf(finDesErr);
                        throw (new Exception(str.substring(ini, fin)));
                    }
                } else {
                    if (str.contains(iniInm1) || str.contains(iniInm2)) {
                        Address inmueble = new Address();
                        while ((str = br.readLine()) != null && !str.contains(finInm1) && !str.contains(finInm2)) {
                            if (str.contains(iniPC1) && str.contains(finPC1)) {
                                ini = str.indexOf(iniPC1) + iniPC1.length();
                                fin = str.indexOf(finPC1);
                                inmueble.setDescription(str.substring(ini, fin));
                            }
                            if (str.contains(iniPC2) && str.contains(finPC2)) {
                                ini = str.indexOf(iniPC2) + iniPC2.length();
                                fin = str.indexOf(finPC2);
                                inmueble.setDescription(inmueble.getDescription().concat(str.substring(ini, fin)));
                            }
                            if (str.contains(iniLDT) && str.contains("-")) {
                                postal = Integer.parseInt(str.substring(str.lastIndexOf("-") - 5, str.lastIndexOf("-")));
                            }
                            if (str.contains(iniCar) && str.contains(finCar)) {
                                ini = str.indexOf(iniCar) + iniCar.length();
                                fin = str.indexOf(finCar);
                                inmueble.setDescription(inmueble.getDescription().concat(str.substring(ini, fin)));
                            }
                            if (str.contains(iniCC1) && str.contains(finCC1)) {
                                ini = str.indexOf(iniCC1) + iniCC1.length();
                                fin = str.indexOf(finCC1);
                                inmueble.setDescription(inmueble.getDescription().concat(str.substring(ini, fin)));
                            }
                            if (str.contains(iniCC2) && str.contains(finCC2)) {
                                ini = str.indexOf(iniCC2) + iniCC2.length();
                                fin = str.indexOf(finCC2);
                                inmueble.setDescription(inmueble.getDescription().concat(str.substring(ini, fin)));
                            }
                            if (str.contains(iniBq) && str.contains(finBq)) {
                                ini = str.indexOf(iniBq) + iniBq.length();
                                fin = str.indexOf(finBq);
                                inmueble.setBlock(str.substring(ini, fin));
                            }
                            if (str.contains(iniEs) && str.contains(finEs)) {
                                ini = str.indexOf(iniEs) + iniEs.length();
                                fin = str.indexOf(finEs);
                                inmueble.setStairs(str.substring(ini, fin));
                            }
                            if (str.contains(iniPt) && str.contains(finPt)) {
                                ini = str.indexOf(iniPt) + iniPt.length();
                                fin = str.indexOf(finPt);
                                inmueble.setFloor(str.substring(ini, fin));
                            }
                            if (str.contains(iniPu) && str.contains(finPu)) {
                                ini = str.indexOf(iniPu) + iniPu.length();
                                fin = str.indexOf(finPu);
                                inmueble.setDoor(str.substring(ini, fin));
                            }
                        }
                        result.add(inmueble);
                    }
                }
            }
            br.close();
            if (result.size() == 1) {
                Object ad[] = result.toArray();
                Coordinate coord = ConversorCoordenadas.getCoordenadas(((Address) ad[0]).getDescription());
                coord.setPostcode(postal);
                for (Address inm : result) inm.setCoodinate(coord);
            }
        } catch (Exception e) {
            System.err.println(e);
        }
        return result;
    }

    /** Metodo getDescripcionTipoVia
	  * Sirve para obtener la descripcion del tipo de via a partir de su codigo, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param String tipo codigo del tipo de calle
	  * @return String descripcion del tipo de calle
	  */
    public static String getDescripcionTipoVia(String tipo) {
        if (tipo.trim().equalsIgnoreCase("AG")) return "AGREGADO";
        if (tipo.trim().equalsIgnoreCase("AL")) return "ALDEA, ALAMEDA";
        if (tipo.trim().equalsIgnoreCase("AR")) return "AREA, ARRABAL";
        if (tipo.trim().equalsIgnoreCase("AU")) return "AUTOPISTA";
        if (tipo.trim().equalsIgnoreCase("AV")) return "AVENIDA";
        if (tipo.trim().equalsIgnoreCase("AY")) return "ARROYO";
        if (tipo.trim().equalsIgnoreCase("BJ")) return "BAJADA";
        if (tipo.trim().equalsIgnoreCase("BO")) return "BARRIO";
        if (tipo.trim().equalsIgnoreCase("BR")) return "BARRANCO";
        if (tipo.trim().equalsIgnoreCase("CA")) return "CA�ADA";
        if (tipo.trim().equalsIgnoreCase("CG")) return "COLEGIO, CIGARRAL";
        if (tipo.trim().equalsIgnoreCase("CH")) return "CHALET";
        if (tipo.trim().equalsIgnoreCase("CI")) return "CINTURON";
        if (tipo.trim().equalsIgnoreCase("CJ")) return "CALLEJA, CALLEJON";
        if (tipo.trim().equalsIgnoreCase("CL")) return "CALLE";
        if (tipo.trim().equalsIgnoreCase("CM")) return "CAMINO, CARMEN";
        if (tipo.trim().equalsIgnoreCase("CN")) return "COLONIA";
        if (tipo.trim().equalsIgnoreCase("CO")) return "CONCEJO, COLEGIO";
        if (tipo.trim().equalsIgnoreCase("CP")) return "CAMPA, CAMPO";
        if (tipo.trim().equalsIgnoreCase("CR")) return "CARRETERA, CARRERA";
        if (tipo.trim().equalsIgnoreCase("CS")) return "CASERIO";
        if (tipo.trim().equalsIgnoreCase("CT")) return "CUESTA, COSTANILLA";
        if (tipo.trim().equalsIgnoreCase("CU")) return "CONJUNTO";
        if (tipo.trim().equalsIgnoreCase("DE")) return "DETR�S";
        if (tipo.trim().equalsIgnoreCase("DP")) return "DIPUTACION";
        if (tipo.trim().equalsIgnoreCase("DS")) return "DISEMINADOS";
        if (tipo.trim().equalsIgnoreCase("ED")) return "EDIFICIOS";
        if (tipo.trim().equalsIgnoreCase("EM")) return "EXTRAMUROS";
        if (tipo.trim().equalsIgnoreCase("EN")) return "ENTRADA, ENSANCHE";
        if (tipo.trim().equalsIgnoreCase("ER")) return "EXTRARRADIO";
        if (tipo.trim().equalsIgnoreCase("ES")) return "ESCALINATA";
        if (tipo.trim().equalsIgnoreCase("EX")) return "EXPLANADA";
        if (tipo.trim().equalsIgnoreCase("FC")) return "FERROCARRIL";
        if (tipo.trim().equalsIgnoreCase("FN")) return "FINCA";
        if (tipo.trim().equalsIgnoreCase("GL")) return "GLORIETA";
        if (tipo.trim().equalsIgnoreCase("GR")) return "GRUPO";
        if (tipo.trim().equalsIgnoreCase("GV")) return "GRAN VIA";
        if (tipo.trim().equalsIgnoreCase("HT")) return "HUERTA, HUERTO";
        if (tipo.trim().equalsIgnoreCase("JR")) return "JARDINES";
        if (tipo.trim().equalsIgnoreCase("LD")) return "LADO, LADERA";
        if (tipo.trim().equalsIgnoreCase("LG")) return "LUGAR";
        if (tipo.trim().equalsIgnoreCase("MC")) return "MERCADO";
        if (tipo.trim().equalsIgnoreCase("ML")) return "MUELLE";
        if (tipo.trim().equalsIgnoreCase("MN")) return "MUNICIPIO";
        if (tipo.trim().equalsIgnoreCase("MS")) return "MASIAS";
        if (tipo.trim().equalsIgnoreCase("MT")) return "MONTE";
        if (tipo.trim().equalsIgnoreCase("MZ")) return "MANZANA";
        if (tipo.trim().equalsIgnoreCase("PB")) return "POBLADO";
        if (tipo.trim().equalsIgnoreCase("PD")) return "PARTIDA";
        if (tipo.trim().equalsIgnoreCase("PJ")) return "PASAJE, PASADIZO";
        if (tipo.trim().equalsIgnoreCase("PL")) return "POLIGONO";
        if (tipo.trim().equalsIgnoreCase("PM")) return "PARAMO";
        if (tipo.trim().equalsIgnoreCase("PQ")) return "PARROQUIA, PARQUE";
        if (tipo.trim().equalsIgnoreCase("PR")) return "PROLONGACION, CONTINUAC.";
        if (tipo.trim().equalsIgnoreCase("PS")) return "PASEO";
        if (tipo.trim().equalsIgnoreCase("PT")) return "PUENTE";
        if (tipo.trim().equalsIgnoreCase("PZ")) return "PLAZA";
        ;
        if (tipo.trim().equalsIgnoreCase("QT")) return "QUINTA";
        if (tipo.trim().equalsIgnoreCase("RB")) return "RAMBLA";
        if (tipo.trim().equalsIgnoreCase("RC")) return "RINCON, RINCONA";
        if (tipo.trim().equalsIgnoreCase("RD")) return "RONDA";
        if (tipo.trim().equalsIgnoreCase("RM")) return "RAMAL";
        if (tipo.trim().equalsIgnoreCase("RP")) return "RAMPA";
        if (tipo.trim().equalsIgnoreCase("RR")) return "RIERA";
        if (tipo.trim().equalsIgnoreCase("RU")) return "RUA";
        if (tipo.trim().equalsIgnoreCase("SA")) return "SALIDA";
        if (tipo.trim().equalsIgnoreCase("SD")) return "SENDA";
        if (tipo.trim().equalsIgnoreCase("SL")) return "SOLAR";
        if (tipo.trim().equalsIgnoreCase("SN")) return "SALON";
        if (tipo.trim().equalsIgnoreCase("SU")) return "SUBIDA";
        if (tipo.trim().equalsIgnoreCase("TN")) return "TERRENOS";
        if (tipo.trim().equalsIgnoreCase("TO")) return "TORRENTE";
        if (tipo.trim().equalsIgnoreCase("TR")) return "TRAVESIA";
        if (tipo.trim().equalsIgnoreCase("UR")) return "URBANIZACION";
        if (tipo.trim().equalsIgnoreCase("VR")) return "VEREDA";
        if (tipo.trim().equalsIgnoreCase("CY")) return "CALEYA";
        return "OTRO";
    }
}
