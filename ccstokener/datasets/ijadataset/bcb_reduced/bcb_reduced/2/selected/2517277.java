package org.ofsm.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ofsm.IFiniteStateMachineInitiable;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.ofsm.IEvent;
import org.ofsm.IState;

public class FileFiniteStateMachine extends FiniteStateMachine implements IFiniteStateMachineInitiable {

    private final String machineName = "FileFiniteStateMachine";

    private final String processCriteriaPattern = ".ofsm.xml";

    private URI initByResource;

    private InputStream initByStream;

    private FileFiniteStateMachineModel model = new FileFiniteStateMachineModel();

    public String getName() {
        return machineName;
    }

    public boolean mayProcessed(URI params) {
        if (params.getPath().contains(processCriteriaPattern)) return true; else return false;
    }

    public boolean mayProcessed(String streamName) {
        if (streamName.contains(processCriteriaPattern)) return true; else return false;
    }

    public void init(InputStream stream) throws Exception {
        initByStream = stream;
        unmarshal(initByStream);
    }

    public synchronized void init(URI params) throws Exception {
        this.clearTransitions();
        initByResource = params;
        InputStream stream = null;
        if (params.getScheme().equalsIgnoreCase("file")) {
            stream = new FileInputStream(new File(params));
        } else {
            URL url = initByResource.toURL();
            stream = url.openStream();
        }
        try {
            unmarshal(stream);
        } finally {
            stream.close();
        }
    }

    public void reset() throws Exception {
        if (initByResource != null) init(initByResource); else super.reset();
    }

    private void unmarshal(InputStream stream) throws Exception {
        model.unmarshal(stream);
        setInitialState(model.getInitialState());
        for (Map.Entry<IState, List<StateTransition>> lstItem : model.getTransitionInfos().entrySet()) {
            for (StateTransition item : lstItem.getValue()) {
                addTransition(item);
            }
        }
    }
}
