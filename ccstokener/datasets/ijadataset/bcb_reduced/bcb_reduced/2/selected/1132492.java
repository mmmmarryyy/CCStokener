package com.dfruits.forms.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.java.custos.annots.Post;
import net.java.custos.annots.Pre;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.w3c.dom.Node;
import com.dfruits.access.annots.CheckAccess;
import com.dfruits.aspects.ui.MeasureExecTime;
import com.dfruits.aspects.ui.RunInUI;
import com.dfruits.forms.XPathNodesListFactory;
import com.dfruits.forms.registrars.RestrictedComponentsRegistrar;
import com.dfruits.forms.service.FormDef;
import com.dfruits.forms.service.Formilat;
import com.dfruits.forms.service.FormsService;
import com.dfruits.queries.BindingContext;
import com.swtworkbench.community.xswt.XSWT;
import com.swtworkbench.community.xswt.patches.XSWTFactory;

public class FormsServiceImpl implements FormsService {

    private XSWT.Configuration config = new XSWT.Configuration();

    private Map<String, FormDef> formDefs = new HashMap<String, FormDef>();

    public Formilat load(String formID, Object parent, BindingContext ctx) {
        FormDef def = formDefs.get(formID);
        if (def == null) {
            return null;
        }
        Map guis = loadForm(formID, parent, ctx);
        Formilat ret = new Formilat(guis, def);
        return ret;
    }

    public boolean registerForm(FormDef def) {
        formDefs.put(def.id, def);
        return true;
    }

    public List<FormDef> listForms() {
        return new ArrayList<FormDef>(formDefs.values());
    }

    @MeasureExecTime("$log.info( ''form load time: '' + $time + '' msec ('' + id + '')'' ) ")
    @Pre(exec = "$log.info( ''loading form: id='' + id )", cond = "this.hasForm( id )", onFail = "$log.error( ''form not registered: id='' + id );" + "$return = Collections.EMPTY_MAP")
    @Post(onThrown = "$log.error( $throwable.getMessage() );")
    public Map loadForm(String id, Object parent, BindingContext ctx) {
        FormDef data = formDefs.get(id);
        Bundle bundle = Platform.getBundle(data.targetPluginID);
        URL url = bundle.getEntry(data.path);
        if (url == null) {
            url = bundle.getResource(data.path);
        }
        XSWT xswt = createXSWT(url, id, ctx);
        Map guis = createUI(xswt, parent);
        return guis;
    }

    public boolean hasForm(String id) {
        return formDefs.get(id) != null;
    }

    @Post(cond = "$return == true", onSuccess = "$log.trace(''added default package import: '' + pkg)")
    public boolean registerPackage(String pkg) {
        if (!config.packageImports.contains(pkg)) {
            return config.packageImports.add(pkg);
        }
        return false;
    }

    @RunInUI
    private Map createUI(XSWT xswt, Object parent) {
        Map guis = null;
        try {
            guis = xswt.parse(parent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return guis;
    }

    private XSWT createXSWT(URL inputURL, String formID, BindingContext ctx) {
        XSWT xswt = null;
        InputStream urlInput = null;
        InputStream newInput = null;
        try {
            urlInput = inputURL.openStream();
            newInput = checkInput(urlInput, formID, ctx);
            xswt = XSWTFactory.create(newInput, config);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlInput != null) {
                try {
                    urlInput.close();
                } catch (IOException e) {
                }
            }
            if (newInput != null) {
                try {
                    newInput.close();
                } catch (IOException e) {
                }
            }
        }
        return xswt;
    }

    private InputStream checkInput(InputStream in, String formID, BindingContext ctx) {
        XPathNodesListFactory fact = new XPathNodesListFactory(in, RestrictedComponentsRegistrar.RESTRICTED_EXPR);
        fact.addNameSpace(RestrictedComponentsRegistrar.ACCESS_CHECKER_NS_PRE, RestrictedComponentsRegistrar.ACCESS_CHECKER_NS);
        List<Node> list = fact.createList();
        for (Node node : list) {
            checkInput(node, formID, ctx);
        }
        String source = null;
        try {
            source = fact.transformBack();
        } catch (Exception e) {
        }
        ByteArrayInputStream input = new ByteArrayInputStream(source.getBytes());
        return input;
    }

    @CheckAccess("restrictedUIHandler")
    private void checkInput(Node node, String formID, BindingContext ctx) {
    }

    public FormDef getFormDef(String id) {
        return formDefs.get(id);
    }
}
