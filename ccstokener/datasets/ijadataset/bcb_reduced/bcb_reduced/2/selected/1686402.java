package org.proteored.miapeapi.xml.util.peaklistreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

/**
 * 
 * @author Alberto
 * 
 *         Class description:
 * 
 *         Esta clase servirá como interface para la lectura de un fichero de
 *         lista de picos que se pasa como input (String) y lo modelizará para
 *         que sea llamada desde la aplicación de MIAPE para la incorporación
 *         de los datos como un MIAPE MS
 */
public class TMsData {

    private String Inputfilename;

    private Vector PFFSpectra;

    TSpectrum MySpectrum;

    private final String BEGINIONS = "BEGIN IONS";

    private final String ENDIONS = "END IONS";

    private final String PEPMASS = "PEPMASS=";

    private final String CHARGE = "CHARGE=";

    private final String TITLE = "TITLE";

    private final String ARRAYS_SEP = " ";

    private final String ARRAYS_TAB = "\t";

    private final String LITTLE_EN = "little";

    private final String BIG_EN = "big";

    private final boolean Outputview = false;

    private final boolean Sorted = false;

    public TMsData(final String inputfile) {
        try {
            Inputfilename = inputfile;
            PFFSpectra = new Vector();
            ReadMSFile(inputfile, PFFSpectra);
            if (Sorted) quickSort(PFFSpectra);
            for (int i = 0; i < PFFSpectra.size(); i++) {
                setSpectrumReference(i, String.valueOf(i + 1));
            }
            if (Outputview) {
                TPeptideInformation _test = new TPeptideInformation();
                for (int i = 0; i < PFFSpectra.size(); i++) {
                    setSpectrumReference(i, String.valueOf(i + 1));
                    System.out.println("Nuevo orden: " + String.valueOf(i + 1));
                    System.out.println("Spectrum Reference: " + getSpectrumReference(i));
                    System.out.println(getQueryNumber(i));
                    System.out.println(getPepMoverZ(i));
                    System.out.println(getPepMass(i));
                    System.out.println(getPepCharge(i));
                    System.out.println(getMzArrayPrecision(i));
                    System.out.println(getMzArrayEndian(i));
                    System.out.println(getMzArrayLength(i));
                    System.out.println(getMzArrayPFFSpectrum(i));
                    System.out.println(getIntensityPFFSpectrum(i));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getFilename() {
        return Inputfilename;
    }

    public Vector getSpectra() {
        return PFFSpectra;
    }

    public Object getSpectrum(int _reference) {
        return getSpectra().elementAt(_reference);
    }

    public void ReadMSFile(String _inputfile, Vector _Spectra) {
        int i;
        int _spot_index;
        String _job_run_id;
        String _job_id;
        String _spot_id;
        String _job_item_id;
        String _precursor_mass;
        String _charge;
        String _peak_list_id;
        String _centroid;
        String _peak_height;
        Vector _aux_values;
        Vector _aux_mass;
        Vector _aux_intensity;
        int _line_count;
        int tabPos, nexttabPos;
        try {
            URL url = new URL(_inputfile);
            BufferedReader inStream = new BufferedReader(new InputStreamReader(url.openStream()));
            _aux_values = new Vector();
            _aux_mass = new Vector();
            _aux_intensity = new Vector();
            _line_count = 0;
            _spot_index = 0;
            _spot_id = Integer.toString(_spot_index);
            _job_item_id = "";
            _peak_list_id = "";
            _precursor_mass = "";
            _charge = "";
            do {
                String inLine;
                if ((inLine = inStream.readLine()) == null) break;
                inLine = inLine.trim();
                _line_count++;
                _aux_values.removeAllElements();
                if (!inLine.equals("")) {
                    if (inLine.compareTo(BEGINIONS) == 0) {
                        _precursor_mass = "";
                        _charge = "";
                        _aux_mass.clear();
                        _aux_intensity.clear();
                        _spot_id = Integer.toString(_spot_index);
                        _job_item_id = "";
                        _peak_list_id = "";
                        _spot_index++;
                    } else {
                        if (inLine.compareTo(ENDIONS) == 0) {
                            CreateSpectrum(_spot_id, _job_item_id, _peak_list_id, _precursor_mass, _charge);
                            MySpectrum.FillMassValues(_aux_mass, _aux_intensity);
                            _Spectra.addElement((TSpectrum) MySpectrum);
                        } else {
                            if (inLine.startsWith(PEPMASS)) _precursor_mass = ParsePrecursorMass(inLine); else if (inLine.startsWith(CHARGE)) _charge = ParseCharge(inLine); else if (inLine.startsWith(TITLE)) _peak_list_id = ParseTitle(inLine); else if ((tabPos = inLine.indexOf(ARRAYS_SEP)) > 0) {
                                _centroid = inLine.substring(0, tabPos);
                                if ((nexttabPos = inLine.indexOf(ARRAYS_SEP, tabPos + 1)) <= 0) nexttabPos = inLine.length();
                                _peak_height = inLine.substring(tabPos + 1, nexttabPos);
                                _aux_mass.addElement((String) _centroid);
                                _aux_intensity.addElement((String) _peak_height);
                            } else if ((tabPos = inLine.indexOf(ARRAYS_TAB)) > 0) {
                                _centroid = inLine.substring(0, tabPos);
                                if ((nexttabPos = inLine.indexOf(ARRAYS_TAB, tabPos + 1)) <= 0) nexttabPos = inLine.length();
                                _peak_height = inLine.substring(tabPos + 1, nexttabPos);
                                _aux_mass.addElement((String) _centroid);
                                _aux_intensity.addElement((String) _peak_height);
                            }
                        }
                    }
                }
            } while (true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String ParsePrecursorMass(String _line) {
        String _ret = "";
        int nexttabPos = _line.length();
        if (_line.indexOf(ARRAYS_SEP) > 0) nexttabPos = _line.indexOf(ARRAYS_SEP); else if (_line.indexOf(ARRAYS_TAB) > 0) nexttabPos = _line.indexOf(ARRAYS_TAB);
        _ret = _line.substring(_line.indexOf(PEPMASS) + PEPMASS.length(), nexttabPos);
        return (_ret);
    }

    private String ParseCharge(String _line) {
        String _ret = "";
        int index = -1;
        if ((index = _line.indexOf("+")) > 0) _ret = _line.substring(_line.indexOf(CHARGE) + CHARGE.length(), index); else if ((index = _line.indexOf("-")) > 0) _ret = _line.substring(_line.indexOf(CHARGE), index);
        return (_ret);
    }

    private String ParseTitle(String _line) {
        String _ret = "";
        _ret = _line.substring(_line.indexOf(TITLE) + TITLE.length(), _line.length());
        return (_ret);
    }

    private void CreateSpectrum(String _spot_id, String _job_item_id, String _peak_list_id, String _precursor_mass, String _charge) {
        double precursormass;
        int peptidecharge;
        try {
            precursormass = Double.parseDouble(_precursor_mass);
        } catch (Exception ex1) {
            precursormass = 0.0;
        }
        try {
            peptidecharge = Integer.parseInt(_charge);
        } catch (Exception ex1) {
            peptidecharge = -1;
        }
        try {
            MySpectrum = new TSpectrum(_spot_id, _job_item_id, _peak_list_id, precursormass);
            if (peptidecharge > 0) MySpectrum.SetPeptideCharge(peptidecharge);
        } catch (Exception ex2) {
            ex2.printStackTrace();
        }
    }

    private void FillMassValues(Vector _mass, Vector _intensities) {
        MySpectrum.FillMassValues(_mass, _intensities);
    }

    private String CodecPFFSpectrum(int _pos) {
        String _ret = "";
        _ret += getMzArrayPFFSpectrum(_pos);
        _ret += "\n";
        _ret += getIntensityPFFSpectrum(_pos);
        _ret += "\n\n";
        return (_ret);
    }

    public String getMzArrayPrecision(int _pos) {
        String _ret = "";
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().GetMzDataPrecision();
        return (_ret);
    }

    public String getMzArrayEndian(int _pos) {
        String _ret = "";
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().GetCodification();
        return (_ret);
    }

    public String getMzArrayLength(int _pos) {
        String _ret = "";
        _ret = String.valueOf(((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().GetMzArrayCount());
        return (_ret);
    }

    public String getMzArrayPFFSpectrum(int _pos) {
        String _ret = "";
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().GetMzData(TBase64PeakList.MZ);
        return (_ret);
    }

    public String getIntensityPFFSpectrum(int _pos) {
        String _ret = "";
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().GetMzData(TBase64PeakList.INTENSITY);
        return (_ret);
    }

    public byte[] getMzByteArrayPFFSpectrum(int _pos) {
        return (((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().GetByteMzData(TBase64PeakList.MZ));
    }

    public byte[] getIntensityByteArrayPFFSpectrum(int _pos) {
        return (((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().GetByteMzData(TBase64PeakList.INTENSITY));
    }

    public double getPepMass(int _pos) {
        double _ret = 0.0;
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeptideMass();
        return (_ret);
    }

    public double getPepMoverZ(int _pos) {
        double _ret = 0.0;
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeptideMoverZ();
        return (_ret);
    }

    public int getPepCharge(int _pos) {
        int _ret = -1;
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeptideCharge();
        return (_ret);
    }

    public String getQueryNumber(int _pos) {
        String _ret = "NA";
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetSpot_id();
        return (_ret);
    }

    public String getSpectrumReference(int _pos) {
        String _ret = "";
        _ret = ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().GetSpectrumReference();
        return (_ret);
    }

    public void setSpectrumReference(int _pos, String _value) {
        ((TSpectrum) PFFSpectra.elementAt(_pos)).GetPeakList().SetSpectrumReference(_value);
    }

    public void quickSort(Vector elements) {
        if (!elements.isEmpty()) quickSort(elements, 0, elements.size() - 1);
    }

    private void quickSort(Vector elements, int lowIndex, int highIndex) {
        int lowToHighIndex = lowIndex;
        int highToLowIndex = highIndex;
        int pivotIndex = (lowToHighIndex + highToLowIndex) / 2;
        double pivotValue = ((TSpectrum) elements.elementAt(pivotIndex)).GetPeptideMass();
        int newLowIndex = highIndex + 1;
        int newHighIndex = lowIndex - 1;
        do {
            if (newHighIndex + 1 >= newLowIndex) break;
            double lowToHighValue;
            for (lowToHighValue = ((TSpectrum) elements.elementAt(lowToHighIndex)).GetPeptideMass(); (lowToHighIndex < newLowIndex) & (lowToHighValue - pivotValue < 0); lowToHighValue = ((TSpectrum) elements.elementAt(lowToHighIndex)).GetPeptideMass()) {
                newHighIndex = lowToHighIndex;
                lowToHighIndex++;
            }
            double highToLowValue;
            for (highToLowValue = ((TSpectrum) elements.elementAt(highToLowIndex)).GetPeptideMass(); (newHighIndex <= highToLowIndex) & (highToLowValue - pivotValue > 0); highToLowValue = ((TSpectrum) elements.elementAt(highToLowIndex)).GetPeptideMass()) {
                newLowIndex = highToLowIndex;
                highToLowIndex--;
            }
            if (lowToHighIndex == highToLowIndex) newHighIndex = lowToHighIndex; else if (lowToHighIndex < highToLowIndex) {
                double compareResult = lowToHighValue - highToLowValue;
                if (compareResult >= 0) {
                    TSpectrum parking = (TSpectrum) elements.elementAt(lowToHighIndex);
                    elements.setElementAt(((TSpectrum) elements.elementAt(highToLowIndex)), lowToHighIndex);
                    elements.setElementAt(parking, highToLowIndex);
                    newLowIndex = highToLowIndex;
                    newHighIndex = lowToHighIndex;
                    lowToHighIndex++;
                    highToLowIndex--;
                }
            }
        } while (true);
        if (lowIndex < newHighIndex) quickSort(elements, lowIndex, newHighIndex);
        if (newLowIndex < highIndex) quickSort(elements, newLowIndex, highIndex);
    }
}
