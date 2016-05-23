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

    public abstract ArrayList<WorkspaceTuple> getWorkspaces();

    public abstract boolean addWorkspace(String ws);

    public abstract boolean delWorkspace(String ws);

    public abstract List<StoreTuple> getDataStores(WorkspaceTuple workspace);

    public abstract List<StoreTuple> getPostgisStores(WorkspaceTuple workspace);

    public abstract ArrayList<StoreTuple> getCoverageStores(WorkspaceTuple workspace);

    public abstract ArrayList<StoreTuple> getWmsStores(WorkspaceTuple workspace);

    public abstract String createWmsStore(WorkspaceTuple workspace, String name, String url, String usr, String pwd);

    public abstract String deleteWMSStore(WorkspaceTuple workspace, String name);

    public abstract WorkspaceTuple getWorkspace(LayerTuple layer);

    public abstract StoreTuple getStore(WorkspaceTuple workspace, LayerTuple layer);

    public abstract List<StyleTuple> getLayerStyles(String layer);

    public abstract List<LayerGroupTuple> getLayerGroups(String workspace);

    /*
     * returns a list of <name,style> merging 'publishables' and 'styles' properties 
     */
    public abstract LinkedHashMap<LayerTuple, StyleTuple> getSubLayers(String workspace, String layergroup);

    public abstract String prefixName(String name);

    public abstract String trimName(String name);

    /*
     * Sets 'publishables' and 'styles' using subs, a <name,style> list 
     */
    public abstract Boolean assignSubLayers(String workspace, String glayer, LinkedHashMap<LayerTuple, String> subs);

    public abstract String addLayerGroup(String workspace, String layer, List<LayerTuple> subL);

    public abstract Boolean deleteLayerGroup(String workspace, String layer);

    public abstract ArrayList<LayerTuple> getFeatureLayersList(String workspace);

    public abstract ArrayList<LayerTuple> getCoverageLayersList(String workspace, String store);

    public abstract ArrayList<LayerTuple> getWmsLayersList(String workspace, String store);

    public abstract boolean addSQLLayer(SqlLayerTuple slt);

    public abstract SqlLayerTuple getSQLLayer(String workspace, String store, String feature_name);

    public abstract boolean updateSQLLayer(SqlLayerTuple slt);

    public abstract String getDefaultStyle(String layer);

    public abstract String assignStyle(String workspace, String layer, String style);

    public abstract String assignOptStyle(String workspace, String layer, String style);

    public abstract String removeStyle(String layer, String style);

    public abstract String addWmsLayer(String workspace, String store, String layer, String name, String title,
            Map<String, String> opts);

    public abstract boolean removeWmsLayer(String workspace, String store, String layerName);

    public abstract boolean removeShapefile(String workspace, String layerName);

    public abstract ArrayList<String> getRoles();

    public abstract HashMap<String, ArrayList<String>> getUserRoleRef();

    public abstract HashMap<String, ArrayList<String>> getGroupRoleRef();

    public abstract String addRole(String name);

    public abstract String addRole(String name, String parent);

    public abstract String addUserRoleRef(String name, String role);

    public abstract String addGroupRoleRef(String name, String role);

    public abstract String addRoleToUser(String role, String user);

    public abstract String addRoleToGroup(String role, String group);

    public abstract String delRole(String name);

    public abstract String delRoleRefFromUser(String role, String user);

    public abstract String delRoleRefFromGroup(String role, String group);

    public abstract HashMap<String, ArrayList<String>> getRules();

    public abstract HashMap<String, ArrayList<String>> getRules(String workspace);

    public abstract HashMap<String, ArrayList<String>> getRules(String workspace, String layer);

    public abstract HashMap<String, ArrayList<String>> getReaders(String workspace, String layer, Boolean strict);

    public abstract HashMap<String, ArrayList<String>> getReaders(String workspace, Boolean strict);

    public abstract HashMap<String, ArrayList<String>> getWrites(String workspace, String layer, Boolean strict);

    public abstract HashMap<String, ArrayList<String>> getWrites(String workspace, Boolean strict);

    public abstract HashMap<String, ArrayList<String>> getAdmins(String workspace, String layer, Boolean strict);

    public abstract HashMap<String, ArrayList<String>> getAdmins(String workspace, Boolean strict);

    public abstract String createRule(String workspace, String layer, char access_mode, List<String> roles);

    public abstract String deleteRule(String rule);

    public abstract String deleteRule(String workspace, String layer, char access_mode);

    public abstract String addToRule(String rule, List<String> roles);

    public abstract String addOrCreateRule(String rule, List<String> roles, Boolean add_only);

    public abstract String addToRule(String workspace, String layer, char access_mode, List<String> roles);

    public abstract String addOrCreateRule(String workspace, String layer, char access_mode, List<String> roles,
            Boolean add_only);

    public abstract String delToRule(String rule, List<String> roles, boolean ignore_missing);

    public abstract String delToRule(String workspace, String layer, char access_mode, List<String> roles,
            boolean ignore_missing);

    public abstract ArrayList<StyleTuple> getAllStyles();

    public abstract ArrayList<StyleTuple> getStyles();

    public abstract ArrayList<StyleTuple> getStyles(String workspace);

    public abstract boolean upload(String name, String sld, String workspace);

    public abstract boolean upload(String name, String sld);

    public abstract ArrayList<String> getUsers();

    public abstract ArrayList<String> getGroups();

    public abstract HashMap<String, ArrayList<String>> getMembers();

    public abstract String addUser(String name, String passwd);

    public abstract String addGroup(String name);

    public abstract String addMember(String name, String group, Boolean unique);

    public abstract String[] addUserGroup(String name, String passwd, String group);

    public abstract String delUser(String name);

    public abstract String delGroup(String name);

    public abstract String delMember(String name, String group);

    public abstract String getSrsLayer(String layerName);

    public abstract boolean linkPGSchema(String workspace, GSPostGISDatastoreEncoder store);

    public abstract boolean linkPGTable(LayerTuple lt, String nativeCRS);

    public abstract boolean removeFeatureType(String workspace, String storename, String layerName);

    public abstract boolean removeCoverageLayer(String workspace, String storename, String layerName);

}