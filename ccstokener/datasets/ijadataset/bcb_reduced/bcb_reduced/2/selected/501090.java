package org.objectwiz.plugin.uibuilder;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectwiz.core.facet.customization.Application;
import org.objectwiz.core.model.ObjectModel;
import org.objectwiz.core.model.Property;
import org.objectwiz.core.facet.customization.BinaryResource;
import org.objectwiz.plugin.uibuilder.model.Board;
import org.objectwiz.plugin.uibuilder.model.Role;
import org.objectwiz.plugin.uibuilder.model.UIBuilderGeneralSettings;
import org.objectwiz.plugin.uibuilder.model.action.Action;
import org.objectwiz.plugin.uibuilder.model.dataset.DatasetDescriptor;
import org.objectwiz.plugin.uibuilder.model.value.Value;
import org.objectwiz.plugin.uibuilder.model.widget.Widget;
import org.objectwiz.utils.FileUtils;

/**
 * A metadata source built upon in-memory POJOs.
 *
 * @author Benoit Del Basso <benoit.delbasso at helmet.fr>
 */
public class PojoUIMetadataSource implements UIMetadataSource {

    private Application applicationRef;

    private UIBuilderGeneralSettings settings;

    private Map<Integer, Board> boards = new HashMap();

    private Map<Integer, Widget> widgets = new HashMap();

    private Map<Integer, Action> actions = new HashMap();

    private Map<Integer, Value> values = new HashMap();

    private Map<Integer, DatasetDescriptor> dsDescriptors = new HashMap();

    private List<BinaryResource> binaryResources = new ArrayList();

    public PojoUIMetadataSource(Application applicationRef) {
        if (applicationRef == null) throw new NullPointerException("applicationRef");
        this.applicationRef = applicationRef;
    }

    protected Application getApplicationRef() {
        return applicationRef;
    }

    private <E> E require(Map<Integer, E> map, Integer id) throws UIBuilderMetadataException {
        if (map.containsKey(id)) {
            return map.get(id);
        } else {
            throw new UIBuilderMetadataException("ID not found: " + id);
        }
    }

    @Override
    public ObjectModel getMetadataModel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<Role, Board> getDefaultBoards() {
        if (settings == null) return null;
        return settings.getDefaultBoards();
    }

    @Override
    public Board getBoardById(int boardId) throws UIBuilderMetadataException {
        return require(boards, boardId);
    }

    @Override
    public Widget getWidgetById(int widgetId) throws UIBuilderMetadataException {
        return require(widgets, widgetId);
    }

    @Override
    public Action getActionById(int actionId) throws UIBuilderMetadataException {
        return require(actions, actionId);
    }

    @Override
    public Value getValueById(int valueId) throws UIBuilderMetadataException {
        return require(values, valueId);
    }

    @Override
    public DatasetDescriptor getDatasetDescriptorById(int descriptorId) throws UIBuilderMetadataException {
        return require(dsDescriptors, descriptorId);
    }

    @Override
    public int getBinaryResourcesCount() {
        return binaryResources.size();
    }

    @Override
    public List<BinaryResource> getBinaryResources(int firstOffset, int count) {
        List<BinaryResource> list = new ArrayList();
        int n = binaryResources.size();
        for (int i = firstOffset; i < firstOffset + count; i++) {
            if (i >= n) break;
            list.add(binaryResources.get(i));
        }
        return list;
    }

    @Override
    public BinaryResource getHeaderImage() {
        if (settings == null) return null;
        return settings.getHeaderImage();
    }

    @Override
    public BinaryResource getStylesheet() {
        if (settings == null) return null;
        return settings.getStylesheet();
    }

    @Override
    public Collection getSourceObjects(Property property, Object target) {
        return Collections.EMPTY_LIST;
    }

    protected BinaryResource createResourceFromUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            BinaryResource r = new BinaryResource();
            r.setId(binaryResources.size());
            r.setApplication(applicationRef);
            r.setName(url.getFile());
            r.setMimeType("media");
            r.setOriginalUrl(url.toExternalForm());
            r.setBytes(FileUtils.readInputStreamAsBytes(url.openStream()));
            binaryResources.add(r);
            return r;
        } catch (Exception e) {
            throw new RuntimeException("Could not create resource from URL: " + urlStr, e);
        }
    }

    /**
     * Helper for adding objects into the metadata. Dispatches the objects depending
     * of their type.
     * Supported objects: {@link UIBuilderSettings}, {@link Board}, etc.
     */
    protected void add(Object obj) {
        if (obj instanceof UIBuilderGeneralSettings) {
            if (settings != null) throw new IllegalStateException("Settings already defined");
            settings = (UIBuilderGeneralSettings) obj;
        } else if (obj instanceof Board) {
            Board board = (Board) obj;
            boards.put(board.getId(), board);
        }
    }

    @Override
    public Map<Integer, Integer> getBinaryResourcesVersions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BinaryResource getBinaryResourceById(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
