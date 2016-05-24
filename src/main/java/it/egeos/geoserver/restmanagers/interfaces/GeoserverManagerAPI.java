package it.egeos.geoserver.restmanagers.interfaces;

import it.egeos.geoserver.restmanagers.tuples.LayerGroupTuple;
import it.egeos.geoserver.restmanagers.tuples.LayerTuple;
import it.egeos.geoserver.restmanagers.tuples.SqlLayerTuple;
import it.egeos.geoserver.restmanagers.tuples.StoreTuple;
import it.egeos.geoserver.restmanagers.tuples.StyleTuple;
import it.egeos.geoserver.restmanagers.tuples.WorkspaceTuple;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface GeoserverManagerAPI {

    ArrayList<WorkspaceTuple> getWorkspaces();

    boolean addWorkspace(String ws);

    boolean delWorkspace(String ws);

    List<StoreTuple> getDataStores(WorkspaceTuple workspace);

    List<StoreTuple> getPostgisStores(WorkspaceTuple workspace);

    ArrayList<StoreTuple> getCoverageStores(WorkspaceTuple workspace);

    ArrayList<StoreTuple> getWmsStores(WorkspaceTuple workspace);

    String createWmsStore(WorkspaceTuple workspace, String name, String url, String usr, String pwd);

    String deleteWMSStore(WorkspaceTuple workspace, String name);

    WorkspaceTuple getWorkspace(LayerTuple layer);

    StoreTuple getStore(WorkspaceTuple workspace, LayerTuple layer);

    List<StyleTuple> getLayerStyles(String layer);

    List<LayerGroupTuple> getLayerGroups(String workspace);

    /*
     * returns a list of <name,style> merging 'publishables' and 'styles'
     * properties
     */
    LinkedHashMap<LayerTuple, StyleTuple> getSubLayers(String workspace, String layergroup);

    String prefixName(String name);

    String trimName(String name);

    /*
     * Sets 'publishables' and 'styles' using subs, a <name,style> list
     */
    Boolean assignSubLayers(String workspace, String glayer, LinkedHashMap<LayerTuple, String> subs);

    String addLayerGroup(String workspace, String layer, List<LayerTuple> subL);

    Boolean deleteLayerGroup(String workspace, String layer);

    ArrayList<LayerTuple> getFeatureLayersList(String workspace);

    ArrayList<LayerTuple> getCoverageLayersList(String workspace, String store);

    ArrayList<LayerTuple> getWmsLayersList(String workspace, String store);

    boolean addSQLLayer(SqlLayerTuple slt);

    SqlLayerTuple getSQLLayer(String workspace, String store, String feature_name);

    boolean updateSQLLayer(SqlLayerTuple slt);

    String getDefaultStyle(String layer);

    String assignStyle(String workspace, String layer, String style);

    String assignOptStyle(String workspace, String layer, String style);

    String removeStyle(String layer, String style);

    String addWmsLayer(String workspace, String store, String layer, String name, String title,
            Map<String, String> opts);

    boolean removeWmsLayer(String workspace, String store, String layerName);

    boolean removeShapefile(String workspace, String layerName);

    ArrayList<String> getRoles();

    HashMap<String, ArrayList<String>> getUserRoleRef();

    HashMap<String, ArrayList<String>> getGroupRoleRef();

    String addRole(String name);

    String addRole(String name, String parent);

    String addUserRoleRef(String name, String role);

    String addGroupRoleRef(String name, String role);

    String addRoleToUser(String role, String user);

    String addRoleToGroup(String role, String group);

    String delRole(String name);

    String delRoleRefFromUser(String role, String user);

    String delRoleRefFromGroup(String role, String group);

    HashMap<String, ArrayList<String>> getRules();

    HashMap<String, ArrayList<String>> getRules(String workspace);

    HashMap<String, ArrayList<String>> getRules(String workspace, String layer);

    HashMap<String, ArrayList<String>> getReaders(String workspace, String layer, Boolean strict);

    HashMap<String, ArrayList<String>> getReaders(String workspace, Boolean strict);

    HashMap<String, ArrayList<String>> getWrites(String workspace, String layer, Boolean strict);

    HashMap<String, ArrayList<String>> getWrites(String workspace, Boolean strict);

    HashMap<String, ArrayList<String>> getAdmins(String workspace, String layer, Boolean strict);

    HashMap<String, ArrayList<String>> getAdmins(String workspace, Boolean strict);

    String createRule(String workspace, String layer, char access_mode, List<String> roles);

    String deleteRule(String rule);

    String deleteRule(String workspace, String layer, char access_mode);

    String addToRule(String rule, List<String> roles);

    String addOrCreateRule(String rule, List<String> roles, Boolean add_only);

    String addToRule(String workspace, String layer, char access_mode, List<String> roles);

    String addOrCreateRule(String workspace, String layer, char access_mode, List<String> roles, Boolean add_only);

    String delToRule(String rule, List<String> roles, boolean ignore_missing);

    String delToRule(String workspace, String layer, char access_mode, List<String> roles, boolean ignore_missing);

    ArrayList<StyleTuple> getAllStyles();

    ArrayList<StyleTuple> getStyles();

    ArrayList<StyleTuple> getStyles(String workspace);

    boolean upload(String name, String sld, String workspace);

    boolean upload(String name, String sld);

    ArrayList<String> getUsers();

    ArrayList<String> getGroups();

    HashMap<String, ArrayList<String>> getMembers();

    String addUser(String name, String passwd);

    String addGroup(String name);

    String addMember(String name, String group, Boolean unique);

    String[] addUserGroup(String name, String passwd, String group);

    String delUser(String name);

    String delGroup(String name);

    String delMember(String name, String group);

    String getSrsLayer(String layerName);

    boolean linkPGSchema(String workspace, GSPostGISDatastoreEncoder store);

    boolean linkPGTable(LayerTuple lt, String nativeCRS);

    boolean removeFeatureType(String workspace, String storename, String layerName);

    boolean removeCoverageLayer(String workspace, String storename, String layerName);

    StoreTuple getWmsStore(String workspace, String name);

}