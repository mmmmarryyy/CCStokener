package org.apache.myfaces.trinidad.model;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.faces.context.FacesContext;
import org.apache.myfaces.trinidad.logging.TrinidadLogger;
import org.apache.myfaces.trinidad.util.ClassLoaderUtils;
import org.apache.myfaces.trinidad.util.ContainerUtils;

public class XMLMenuModel extends BaseMenuModel implements Serializable {

    public XMLMenuModel() {
        super();
        _modelId = Integer.valueOf(System.identityHashCode(this)).toString();
    }

    /**
   * setSource - specifies the XML metadata and creates
   * the XML Menu Model.
   *
   * @param menuMetadataUri - String URI to the XML metadata.
   */
    public void setSource(String menuMetadataUri) {
        if (menuMetadataUri == null || "".equals(menuMetadataUri)) return;
        _mdSource = menuMetadataUri;
        _createModel();
    }

    /**
   * Makes the TreeModel part of the menu model.  Also creates the
   * _viewIdFocusPathMap, _nodeFocusPathMap, and idNodeMaps.
   *
   * @param data The Tree Model instance
   */
    @Override
    public void setWrappedData(Object data) {
        super.setWrappedData(data);
        if (this == _getRootModel()) {
            _viewIdFocusPathMap = _contentHandler.getViewIdFocusPathMap(_mdSource);
            _nodeFocusPathMap = _contentHandler.getNodeFocusPathMap(_mdSource);
            _idNodeMap = _contentHandler.getIdNodeMap(_mdSource);
        }
    }

    /**
   * Returns the rowKey to the current viewId, or in the case of where the
   * model has nodes with duplicate viewId's and one is encountered, we
   * return the rowKey of the currently selected node.
   * <p>
   *
   * The getFocusRowKey method
   * <ul>
   * <li>gets the current viewId by calling
   * FacesContext.getCurrentInstance().getViewRoot().getViewId()
   * <li>compares the current viewId with the viewId's in the viewIdFocusPathMap
   * that was built by traversing the tree when the model was created.
   * <li>returns the focus path to the node with the current viewId or null if
   * the current viewId can't be found.
   * <li>in the case where a viewId has multiple focus paths, the currently
   * selected node is used as a key into the nodeFocusPathMap to return the
   * correct focus path.
   * </ul>
   *
   * @return  the rowKey to the node with the current viewId or null if the
   * current viewId can't be found.
   */
    @SuppressWarnings("unchecked")
    @Override
    public Object getFocusRowKey() {
        Object focusPath = null;
        String currentViewId = _getCurrentViewId();
        FacesContext context = FacesContext.getCurrentInstance();
        if ((_prevViewId != null) && _prevViewId.equals(currentViewId)) return _prevFocusPath;
        _prevViewId = currentViewId;
        if (_getRequestMethod() != _METHOD_POST) {
            Map<String, String> paramMap = context.getExternalContext().getRequestParameterMap();
            String UrlNodeId = paramMap.get(_NODE_ID_PROPERTY);
            if (UrlNodeId != null) {
                _setCurrentlySelectedNode(_getNodeFromURLParams(UrlNodeId));
                _setRequestMethod(_METHOD_GET);
            }
        }
        if (_getRequestMethod() == _METHOD_NONE) {
            _setCurrentlySelectedNode(null);
        }
        ArrayList<Object> fpArrayList = (ArrayList<Object>) _viewIdFocusPathMap.get(currentViewId);
        if (fpArrayList != null) {
            if (_prevRequestNode != null) {
                focusPath = _nodeFocusPathMap.get(_prevRequestNode);
                _prevRequestNode = null;
            } else {
                Object currentNode = _getCurrentlySelectedNode();
                if (fpArrayList.size() == 1 || currentNode == null) {
                    focusPath = fpArrayList.get(0);
                    _prevRequestNode = null;
                } else {
                    focusPath = _nodeFocusPathMap.get(currentNode);
                    _prevRequestNode = currentNode;
                }
            }
        }
        _prevFocusPath = focusPath;
        _setRequestMethod(_METHOD_NONE);
        return focusPath;
    }

    /**
   * Gets the URI to the XML menu metadata.
   *
   * @return String URI to the XML menu metadata.
   */
    public String getSource() {
        return _mdSource;
    }

    /**
   * Sets the boolean value that determines whether or not to create
   * nodes whose rendered attribute value is false.  The default
   * value is false.
   *
   * This is set through a managed property of the XMLMenuModel
   * managed bean -- typically in the faces-config.xml file for
   * a faces application.
   */
    public void setCreateHiddenNodes(boolean createHiddenNodes) {
        _createHiddenNodes = createHiddenNodes;
    }

    /**
   * Gets the boolean value that determines whether or not to create
   * nodes whose rendered attribute value is false.  The default
   * value is false.
   *
   * This is called by the contentHandler when parsing the XML metadata
   * for each node.
   *
   * @return the boolean value that determines whether or not to create
   * nodes whose rendered attribute value is false.
   */
    public boolean getCreateHiddenNodes() {
        return _createHiddenNodes;
    }

    /**
   * Maps the focusPath returned when the viewId is newViewId
   * to the focusPath returned when the viewId is aliasedViewId.
   * This allows view id's not in the treeModel to be mapped
   * to a focusPath.
   *
   * @param newViewId the view id to add a focus path for.
   * @param aliasedViewId the view id to use to get the focusPath to use
   *        for newViewId.
   */
    @SuppressWarnings("unchecked")
    public void addViewId(String newViewId, String aliasedViewId) {
        List<Object> focusPath = _viewIdFocusPathMap.get(aliasedViewId);
        if (focusPath != null) {
            _viewIdFocusPathMap.put(newViewId, focusPath);
        }
    }

    /**
   * Sets the currently selected node and the request method.
   * This is called by a selected node's doAction method.  This
   * menu node must have had its "action" attribute set, thus the
   * method is POST.
   *
   * @param currentNode  The currently selected node in the menu
   */
    public void setCurrentlyPostedNode(Object currentNode) {
        _setCurrentlySelectedNode(currentNode);
        _setRequestMethod(_METHOD_POST);
        _prevViewId = null;
    }

    /**
   * Get a the MenuNode corresponding to the key "id" from the
   * node id hashmap.
   *
   * @param id - String node id key for the hashmap entry.
   * @return The Node Object that corresponds to id.
   */
    public Object getNode(String id) {
        XMLMenuModel rootModel = _getRootModel();
        Map<String, Object> idNodeMap = rootModel._getIdNodeMap();
        if (idNodeMap == null) return null;
        return idNodeMap.get(id);
    }

    /**
   * Gets the list of custom properties from the node
   * and returns the value of propName.  Node must be an itemNode.
   * If it is not an itemNode, the node will not have any custom
   * properties and null will be returned.
   *
   * @param node Object used to get its list of custom properties
   * @param propName String name of the property whose value is desired
   *
   * @return Object value of propName for Object node.
   */
    @SuppressWarnings("unchecked")
    public Object getCustomProperty(Object node, String propName) {
        if (node == null) return null;
        FacesContext context = FacesContext.getCurrentInstance();
        ELContext elContext = context.getELContext();
        ELResolver resolver = elContext.getELResolver();
        String value = null;
        try {
            Map<String, String> propMap = (Map<String, String>) resolver.getValue(elContext, node, _CUSTOM_ATTR_LIST);
            if (propMap == null) return null;
            value = propMap.get(propName);
        } catch (PropertyNotFoundException ex) {
            return null;
        }
        if (value != null && ContainerUtils.isValueReference(value)) {
            Object elValue = null;
            try {
                elValue = context.getApplication().evaluateExpressionGet(context, value, Object.class);
            } catch (Exception ex) {
                _LOG.warning("INVALID_EL_EXPRESSION", value);
                _LOG.warning(ex);
                return null;
            }
            return elValue;
        }
        return value;
    }

    /**
   * getStream - Opens an InputStream to the provided URI.
   *
   * @param uri - String uri to a data source.
   * @return InputStream to the data source.
   */
    public InputStream getStream(String uri) {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            URL url = context.getExternalContext().getResource(uri);
            return url.openStream();
        } catch (Exception ex) {
            _LOG.severe("OPEN_URI_EXCEPTION", uri);
            _LOG.severe(ex);
            return null;
        }
    }

    /**
   * Get the Model's viewIdFocusPathMap
   *
   * @return the Model's viewIdFocusPathMap
   */
    public Map<String, List<Object>> getViewIdFocusPathMap() {
        if (this != _getRootModel() || _contentHandler == null) return null;
        if (_viewIdFocusPathMap == null) _viewIdFocusPathMap = _contentHandler.getViewIdFocusPathMap(_mdSource);
        return _viewIdFocusPathMap;
    }

    private Map<String, Object> _getIdNodeMap() {
        return (this == _getRootModel()) ? _idNodeMap : null;
    }

    /**
   * Get a the MenuNode corresponding to the key "id" from the
   * node id hashmap.
   *
   * @param id - String node id key for the hashmap entry.
   * @return The MenuNode that corresponds to id.
   */
    private Object _getNodeFromURLParams(String urlNodeId) {
        return _idNodeMap.get(urlNodeId);
    }

    /**
    * Creates a menu model based on the menu metadata Uri.
    * This is accomplished by:
    * <ol>
    * <li> Get the MenuContentHandlerImpl through the Services API.
    * <li> Set the root model and current model on the content handler, which,
    * in turn, sets the models on each of the nodes.
    * <li> Parse the metadata.  This calls into the MenuContentHandler's
    * startElement and endElement methods, where a List of nodes and a TreeModel
    * are created, along with the 3 hashMaps needed by the Model.</li>
    * <li> Use the TreeModel to create the XMLMenuModel.</li>
    * </ol>
    */
    private void _createModel() {
        try {
            if (_contentHandler == null) {
                List<MenuContentHandler> services = ClassLoaderUtils.getServices(_MENUCONTENTHANDLER_SERVICE);
                if (services.isEmpty()) {
                    throw new IllegalStateException(_LOG.getMessage("NO_MENUCONTENTHANDLER_REGISTERED"));
                }
                _contentHandler = services.get(0);
                if (_contentHandler == null) {
                    throw new NullPointerException();
                }
            }
            _setRootModelKey(_contentHandler);
            _setModelId(_contentHandler);
            TreeModel treeModel = _contentHandler.getTreeModel(_mdSource);
            setWrappedData(treeModel);
        } catch (Exception ex) {
            _LOG.severe("ERR_CREATE_MENU_MODEL", _mdSource);
            _LOG.severe(ex);
            return;
        }
    }

    /**
   * _setRootModelKey - sets the top-level, menu model's Key on the
   * menu content handler. This is so nodes will only operate
   * on the top-level, root model.
   *
   */
    @SuppressWarnings("unchecked")
    private void _setRootModelKey(MenuContentHandler contentHandler) {
        if (_getRootModel() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
            requestMap.put(_ROOT_MODEL_KEY, this);
            contentHandler.setRootModelKey(_ROOT_MODEL_KEY);
        }
    }

    /**
   * Returns the root menu model.
   *
   * @return XMLMenuModel the root menu model.
   */
    @SuppressWarnings("unchecked")
    private XMLMenuModel _getRootModel() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        return (XMLMenuModel) requestMap.get(_ROOT_MODEL_KEY);
    }

    /**
   * _getModelId - gets the local, menu model's Sys Id.
   *
   * @return String the model's System Id.
   */
    private String _getModelId() {
        return _modelId;
    }

    /**
   * _setModelId - sets the local, menu model's id on the
   * menu content handler.
   */
    @SuppressWarnings("unchecked")
    private void _setModelId(MenuContentHandler contentHandler) {
        String modelId = _getModelId();
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(modelId, this);
        contentHandler.setModelId(modelId);
    }

    /**
   * Returns the current viewId.
   *
   * @return  the current viewId or null if the current viewId can't be found
   */
    private String _getCurrentViewId() {
        String currentViewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        return currentViewId;
    }

    /**
   * Gets the currently selected node in the menu
   */
    private Object _getCurrentlySelectedNode() {
        return _currentNode;
    }

    /**
   * Sets the currently selected node.
   *
   * @param currentNode.  The currently selected node in the menu.
   */
    private void _setCurrentlySelectedNode(Object currentNode) {
        _currentNode = currentNode;
    }

    /**
   * Sets the request method
   *
   * @param method
   */
    private void _setRequestMethod(String method) {
        _requestMethod = method;
    }

    /**
   * Get the request method
   */
    private String _getRequestMethod() {
        return _requestMethod;
    }

    public interface MenuContentHandler {

        /**
      * Get the TreeModel built while parsing metadata.
      *
      * @param uri String mapkey to a (possibly) treeModel cached on
      *        the MenuContentHandlerImpl.
      * @return TreeModel.
      */
        public TreeModel getTreeModel(String uri);

        /**
      * Sets the root model's request map key on the ContentHandler so
      * that the nodes can get back to their root model
      * through the request map.
      */
        public void setRootModelKey(String key);

        /**
      * Sets the local, sharedNode model's Model id on the ContentHandler so that
      * the local model can be gotten to, if necessary.
      */
        public void setModelId(String modelId);

        /**
     * Get the Model's idNodeMap
     *
     * @return the Model's idNodeMap
     */
        public Map<String, Object> getIdNodeMap(Object modelKey);

        /**
     * Get the Model's nodeFocusPathMap
     *
     * @return the Model's nodeFocusPathMap
     */
        public Map<Object, List<Object>> getNodeFocusPathMap(Object modelKey);

        /**
     * Get the Model's viewIdFocusPathMap
     *
     * @return the Model's viewIdFocusPathMap
     */
        public Map<String, List<Object>> getViewIdFocusPathMap(Object modelKey);
    }

    private Object _currentNode = null;

    private Object _prevFocusPath = null;

    private String _prevViewId = null;

    private String _requestMethod = _METHOD_NONE;

    private String _mdSource = null;

    private boolean _createHiddenNodes = false;

    private String _modelId = null;

    private Map<String, List<Object>> _viewIdFocusPathMap;

    private Map<Object, List<Object>> _nodeFocusPathMap;

    private Map<String, Object> _idNodeMap;

    private static MenuContentHandler _contentHandler = null;

    private static Object _prevRequestNode = null;

    private static final String _ROOT_MODEL_KEY = "org.apache.myfaces.trinidad.model.XMLMenuModel.__root_menu__";

    private static final String _NODE_ID_PROPERTY = "nodeId";

    private static final String _METHOD_GET = "get";

    private static final String _METHOD_POST = "post";

    private static final String _METHOD_NONE = "none";

    private static final String _CUSTOM_ATTR_LIST = "customPropList";

    private static final String _MENUCONTENTHANDLER_SERVICE = "org.apache.myfaces.trinidad.model.XMLMenuModel$MenuContentHandler";

    private static final TrinidadLogger _LOG = TrinidadLogger.createTrinidadLogger(XMLMenuModel.class);

    private static final long serialVersionUID = 1L;
}
