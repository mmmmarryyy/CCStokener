package net.sf.mzmine.modules.peaklistmethods.identification.mascot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Vector;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import org.w3c.dom.Element;
import com.compomics.mslims.util.http.forms.HTTPForm;
import com.compomics.mslims.util.http.forms.inputs.InputInterface;
import com.compomics.mslims.util.http.forms.inputs.RadioInput;
import com.compomics.mslims.util.http.forms.inputs.SelectInput;
import com.compomics.mslims.util.http.forms.inputs.TextFieldInput;

public class MascotParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    ArrayList<Parameter> para = new ArrayList<Parameter>();

    HTTPForm iForm = null;

    private String serverName = "127.0.0.1";

    @SuppressWarnings("unchecked")
    public MascotParameters(String serverName) throws MalformedURLException, IOException {
        super(new Parameter[0]);
        para.add(peakLists);
        URL url = new URL(getSearchMaskUrlString());
        URLConnection lConn = url.openConnection();
        InputStream ins = lConn.getInputStream();
        iForm = HTTPForm.parseHTMLForm(ins);
        Vector<InputInterface> lvInputs = iForm.getInputs();
        for (int i = 0; i < lvInputs.size(); i++) {
            InputInterface lInput = (InputInterface) lvInputs.elementAt(i);
            if (lInput.getType() == InputInterface.SELECTINPUT) {
                if (lInput.getName().equals("FORMAT")) {
                    lInput.setValue("Mascot generic");
                } else if (lInput.getName().equals("REPORT")) {
                    lInput.setValue(((SelectInput) lInput).getElements()[0]);
                } else {
                    SelectInput input = (SelectInput) lInput;
                    String[] elements = ((SelectInput) lInput).getElements();
                    if (input.getMultiple()) {
                        UserParameter p = new MultiChoiceParameter<String>(lInput.getComment(), lInput.getName(), elements);
                        para.add(p);
                    } else {
                        UserParameter p = new ComboParameter<String>(lInput.getComment(), lInput.getName(), elements);
                        para.add(p);
                    }
                }
            }
            if (lInput.getType() == InputInterface.CHECKBOX) {
                if (lInput.getName().equals("OVERVIEW")) {
                    lInput.setValue("0");
                } else para.add(new BooleanParameter(lInput.getComment(), lInput.getName()));
            }
            if (lInput.getType() == InputInterface.RADIOINPUT) {
                RadioInput input = (RadioInput) lInput;
                String[] elements = input.getChoices();
                para.add(new ComboParameter<String>(lInput.getComment(), lInput.getName(), elements));
            }
            if (lInput.getType() == InputInterface.TEXTFIELDINPUT && lInput instanceof TextFieldInput) {
                TextFieldInput textFiled = (TextFieldInput) lInput;
                if (textFiled.isHidden()) {
                } else if (textFiled.getName().equals("FILE")) {
                    textFiled.setValue("");
                } else if (textFiled.getName().equals("PRECURSOR")) {
                    textFiled.setValue("");
                } else if (textFiled.getName().equals("USERNAME")) {
                    textFiled.setValue("");
                } else if (textFiled.getName().equals("USEREMAIL")) {
                    textFiled.setValue("");
                } else if (textFiled.getName().equals("COM")) {
                    textFiled.setValue("Mzmine " + MZmineCore.getMZmineVersion());
                } else if (textFiled.getName().equals("SEG")) {
                    textFiled.setValue("");
                } else {
                    para.add(new StringParameter(lInput.getComment(), lInput.getName()));
                }
            }
        }
    }

    protected String getSearchMaskUrlString() {
        return getMascotInstallUrlString() + "cgi/search_form.pl?SEARCH=MIS";
    }

    protected String getMascotSubmitUrlString() {
        return getMascotInstallUrlString() + "cgi/nph-mascot.exe?1";
    }

    protected String getMascotInstallUrlString() {
        return "http://" + serverName + "/mascot/";
    }

    public String getServerName() {
        return serverName;
    }

    public synchronized String getBoundery() {
        return iForm.getBoundary();
    }

    public synchronized String getSubmissionString(File file, int charge) {
        return null;
    }

    @Override
    public Parameter[] getParameters() {
        return para.toArray(new Parameter[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Parameter> T getParameter(T parameter) {
        for (Parameter p : para) {
            if (p.getName().equals(parameter.getName())) return (T) p;
        }
        return null;
    }

    @Override
    public void loadValuesFromXML(Element element) {
    }

    @Override
    public void saveValuesToXML(Element element) {
    }

    public ParameterSet cloneParameter() {
        return this;
    }
}
