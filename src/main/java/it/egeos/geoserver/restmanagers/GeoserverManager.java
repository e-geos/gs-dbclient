package it.egeos.geoserver.restmanagers;

import it.egeos.geoserver.dbmanagers.CatalogLoader;
import it.egeos.geoserver.dbmanagers.Factory;
import it.egeos.geoserver.restmanagers.Abstracts.DBManager;
import it.egeos.geoserver.restmanagers.interfaces.GeoserverManagerAPI;
import it.egeos.geoserver.restmanagers.tuples.LayerGroupTuple;
import it.egeos.geoserver.restmanagers.tuples.LayerTuple;
import it.egeos.geoserver.restmanagers.tuples.SqlLayerTuple;
import it.egeos.geoserver.restmanagers.tuples.StoreTuple;
import it.egeos.geoserver.restmanagers.tuples.StyleTuple;
import it.egeos.geoserver.restmanagers.tuples.VTGeometryTuple;
import it.egeos.geoserver.restmanagers.tuples.VTParameterTuple;
import it.egeos.geoserver.restmanagers.tuples.WmsStoreTuple;
import it.egeos.geoserver.restmanagers.tuples.WorkspaceTuple;
import it.egeos.geoserver.restmanagers.types.StoreTypes;
import it.geosolutions.geoserver.rest.decoder.RESTWmsStore;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;

/**
 * 
 * @author Federico C. Guizzardi - cippinofg <at> gmail.com
 * 
 * RestManager implementation to control a Geoserver
 *
 */

public class GeoserverManager extends DBManager implements GeoserverManagerAPI{
    private Catalog cat;
    private Factory fact;    
    private String driver="org.postgresql.Driver";
    
    //Available access mode
    public final static char ADMIN='a'; 
    public final static char READ='r';
    public final static char WRITE='w';
    
    public GeoserverManager(String login, String password, String url) {
        super(login,password,url);
        try {
            cat = CatalogLoader.getCatalog(driver, url, login, password);
        } 
        catch (IOException e) {
            log.error("Can't open catalog", e);
        }        
        try {
            fact=new Factory(cat);
        } 
        catch (NullPointerException e) {
            log.error("Can't generate Factory", e);
        }
    }
    
    @Override
    @SuppressWarnings("serial")
    public ArrayList<WorkspaceTuple> getWorkspaces(){
        return new ArrayList<WorkspaceTuple>(){{
            for(WorkspaceInfo w:cat.getWorkspaces())
                add(new WorkspaceTuple(w.getName()));
        }};
    }
    
    @Override    
    public boolean addWorkspace(String ws){        
        cat.add(fact.newNamespace(ws,"http://"+ws));
        cat.add(fact.newWorkspace(ws));        
        return true;
    }

    @Override    
    public boolean delWorkspace(String ws){
        NamespaceInfo ni= cat.getNamespace(ws);
        WorkspaceInfo wi = cat.getWorkspaceByName(ws);
        cat.remove(wi);
        cat.remove(ni);
        return true;
    }
            
    @SuppressWarnings("serial")
    @Override    
    public List<StoreTuple> getDataStores(final WorkspaceTuple workspace){
        return new ArrayList<StoreTuple>(){{
            for(DataStoreInfo ds:cat.getDataStoresByWorkspace(cat.getWorkspaceByName(workspace.name)))
                add(new StoreTuple(ds.getName(), StoreTypes.DATA, workspace));
        }};
    }
    
    @SuppressWarnings("serial")
    @Override    
    public List<StoreTuple> getPostgisStores(final WorkspaceTuple workspace){
        return new ArrayList<StoreTuple>(){{
            for(DataStoreInfo ds:cat.getDataStoresByWorkspace(cat.getWorkspaceByName(workspace.name)))
                if ("PostGIS".equals(ds.getType()))
                    add(new StoreTuple(ds.getName(), StoreTypes.DATA, workspace));
        }};
    }
    
    @SuppressWarnings("serial")
    @Override    
    public ArrayList<StoreTuple> getCoverageStores(final WorkspaceTuple workspace){
        return new ArrayList<StoreTuple>(){{
            for(CoverageStoreInfo cs:cat.getCoverageStoresByWorkspace(cat.getWorkspaceByName(workspace.name)))
                add(new StoreTuple(cs.getName(), StoreTypes.COVERAGE, workspace));
        }};
    }
    
    @SuppressWarnings("serial")
    @Override    
    public ArrayList<StoreTuple> getWmsStores(final WorkspaceTuple workspace){
        return new ArrayList<StoreTuple>(){{
            for(WMSStoreInfo ws:cat.getStoresByWorkspace(workspace.name, WMSStoreInfo.class))
                add(new StoreTuple(ws.getName(), StoreTypes.WMS, workspace));
        }};
    }

    @Override
    public WmsStoreTuple getWmsStore(final String workspace,String name){
        WMSStoreInfo st = cat.getStoreByName(workspace, name, WMSStoreInfo.class);
        return new WmsStoreTuple(st.getName(), StoreTypes.WMS, new WorkspaceTuple(workspace),st.getCapabilitiesURL(),st.getUsername(),st.getPassword());
    }
    
    @Override    
    public String createWmsStore(final WorkspaceTuple workspace,final String name,final String url,final String usr, final String pwd){
        WMSStoreInfo st = fact.newWmsStore(cat.getWorkspaceByName(workspace.name),name,url,usr,pwd);
        cat.add(st);
        return st.getId();
    }

    @Override    
    public String deleteWMSStore(final WorkspaceTuple workspace,String name){       
        WMSStoreInfo st = cat.getStoreByName(workspace.name, name, WMSStoreInfo.class);
        cat.remove(st);
        return st.getId();
    }
            
    @Override    
    public WorkspaceTuple getWorkspace(final LayerTuple layer){
        WorkspaceTuple res=null;
        LayerInfo l = cat.getLayerByName(layer.name);
        if(l!=null){
            FeatureTypeInfo ft = cat.getFeatureType(l.getId());
            if (ft!=null)
                res=new WorkspaceTuple(ft.getNamespace().getName());
        }
        return res;
    }

    @Override    
    public StoreTuple getStore(WorkspaceTuple workspace, LayerTuple layer){     
        StoreTuple res=null;
        FeatureTypeInfo ft = cat.getFeatureTypeByName(workspace.name, layer.name);
        if (ft!=null){
            DataStoreInfo st = ft.getStore();
            res=new StoreTuple(st.getName(), StoreTypes.DATA, workspace);
        }
        return res;
    }
        
    @Override    
    @SuppressWarnings("serial")
    public List<StyleTuple> getLayerStyles(final String layer){
        return new ArrayList<StyleTuple>(){{
            LayerInfo ly = cat.getLayerByName(layer);
            for(StyleInfo s:ly.getStyles()){
                WorkspaceInfo ws = s.getWorkspace();
                WorkspaceTuple wst= ws!=null?new WorkspaceTuple(ws.getName()):null;
                add(new StyleTuple(s.getName(), s.getFormat(), s.getFilename(),wst));
            }
        }};
     }
        
    @Override    
    @SuppressWarnings("serial")
    public List<LayerGroupTuple> getLayerGroups(final String workspace){     
        return new ArrayList<LayerGroupTuple>(){{
            List<LayerGroupInfo> lgs = cat.getLayerGroupsByWorkspace(workspace);
            if(lgs!=null)
                for(LayerGroupInfo lg:lgs){
                    ReferencedEnvelope b = lg.getBounds();
                    add(new LayerGroupTuple(
                        lg.getName(),
                        lg.getTitle(),
                        b.getMaxX(),
                        b.getMaxY(),
                        b.getMinX(),
                        b.getMinY(),
                        b.getCoordinateReferenceSystem().toString()
                    ));
                }
        }};
    }

    /*
        * returns a list of <name,style> merging 'publishables' and 'styles' properties 
        */  
    @Override    
    @SuppressWarnings("serial")
    public LinkedHashMap<LayerTuple,StyleTuple> getSubLayers(final String workspace,final String layergroup){
        return new LinkedHashMap<LayerTuple,StyleTuple>(){{
            LayerGroupInfo lg = cat.getLayerGroupByName(workspace, layergroup);            
            List<PublishedInfo> lys = lg.getLayers();
            List<StyleInfo> sts = lg.getStyles();
            for(int i=0;i<lys.size();i++){
                PublishedInfo l = lys.get(i);
                StyleInfo si = sts.get(i);                
                LayerTuple lt = new LayerTuple(l.getName(), l.getTitle(), new StoreTuple());
                StyleTuple st = si!=null?new StyleTuple(si.getName(), si.getFormat(), si.getFilename(), new WorkspaceTuple(si.getWorkspace().getName())):null;
                put(lt,st);
            }            
        }};
    }
    
    @Override    
    public String prefixName(String name){
        String res=null;
        try {
            String[] parts = name.split(":");
            res=parts.length>1? parts[0]:null;         
        } 
        catch (ArrayIndexOutOfBoundsException|NullPointerException e) {
            //name is null so no split is needed or no contains :
        }       
        return res;
    }
    
    @Override    
    public String trimName(String name){
        String res=name;
        try {
            res=name.split(":")[1];         
        } 
        catch (ArrayIndexOutOfBoundsException|NullPointerException e) {
            //name is null so no split is needed or no contains :
        }       
        return res;
    }
    
    /*
     * Sets 'publishables' and 'styles' using subs, a <name,style> list 
     */  
    @Override    
    @SuppressWarnings("serial")
    public Boolean assignSubLayers(String workspace,String glayer,final LinkedHashMap<LayerTuple, String> subs){
        LayerGroupInfo lg = cat.getLayerGroupByName(workspace, glayer);
        if(lg!=null)
            assignSubLayers(lg, new LinkedHashMap<PublishedInfo, String>(){{
                for(LayerTuple s:subs.keySet())
                    put(s.isLayerGroup()?cat.getLayerGroupByName(lg.getWorkspace(),s.name):cat.getLayerByName(s.name),subs.get(s));
            }});
        cat.save(lg);
        return true;
     }
    
    @Override    
    @SuppressWarnings("serial")
    public String addLayerGroup(final String workspace,final String layer,final List<LayerTuple> subs){
        LayerGroupInfo lg = fact.newLayerGroup(workspace,layer);        
        assignSubLayers(lg, new LinkedHashMap<PublishedInfo, String>(){{
            for(LayerTuple s:subs)
                put(s.isLayerGroup()?cat.getLayerGroupByName(lg.getWorkspace(),s.name):cat.getLayerByName(s.name),null);
        }});        
        cat.add(lg);
        return null;
    }

    private void assignSubLayers(LayerGroupInfo lg,LinkedHashMap<PublishedInfo, String> subs){        
        for(PublishedInfo l:subs.keySet()){
            String style=subs.get(l);
            lg.getLayers().add(l);            
            lg.getStyles().add(style!=null?cat.getStyleByName(style):null);           
        }
        
        CatalogBuilder builder = new CatalogBuilder(cat);
        try {
            builder.calculateLayerGroupBounds(lg);
        }
        catch (Exception e) {
            log.error("Can't create a layergroup in '"+lg.getWorkspace().getName()+"' with name '"+lg.getName()+"': "+e.getMessage(), e);            
        }
    }
    
    @Override    
    public Boolean deleteLayerGroup(String workspace,String layer){
        LayerGroupInfo lg = cat.getLayerGroupByName(workspace, layer);
        if(lg!=null)
            cat.remove(lg);
        return true;
    }
        
    @SuppressWarnings("serial")
    @Override    
    public ArrayList<LayerTuple> getFeatureLayersList(final String workspace){
        return new ArrayList<LayerTuple>(){{            
            for(FeatureTypeInfo ft:cat.getFeatureTypesByNamespace(cat.getNamespace(workspace))){                
                LayerTuple lt = new LayerTuple(ft.getName(), ft.getTitle(), new StoreTuple(ft.getStore().getName(),StoreTypes.DATA, new WorkspaceTuple(workspace)));
                ReferencedEnvelope bb = ft.getNativeBoundingBox();
                lt.minX=bb.getMinX();
                lt.minY=bb.getMinY();
                lt.maxX=bb.getMaxX();
                lt.maxY=bb.getMaxY();
                lt.crs=ft.getSRS();
                lt.nativeName=ft.getNativeName();
                add(lt);
            }
        }};
    }
    
    @SuppressWarnings("serial")
    @Override    
    public ArrayList<LayerTuple> getCoverageLayersList(final String workspace,final String store){
        return new ArrayList<LayerTuple>(){{
            for(CoverageInfo c:cat.getCoveragesByStore(cat.getCoverageStoreByName(cat.getWorkspaceByName(workspace), store))){
                LayerTuple lt = new LayerTuple(c.getName(), c.getTitle(), new StoreTuple(c.getStore().getName(),StoreTypes.DATA, new WorkspaceTuple(workspace)));
                ReferencedEnvelope bb = c.getNativeBoundingBox();
                lt.minX=bb.getMinX();
                lt.minY=bb.getMinY();
                lt.maxX=bb.getMaxX();
                lt.maxY=bb.getMaxY();
                lt.crs=c.getSRS();                         
                lt.nativeName=c.getNativeName();
                add(lt);
            }
        }};
    }

    @SuppressWarnings("serial")
    @Override    
    public ArrayList<LayerTuple> getWmsLayersList(final String workspace,final String store){
        return new ArrayList<LayerTuple>(){{            
            for(WMSLayerInfo r:cat.getResourcesByStore(cat.getStoreByName(workspace, store, WMSStoreInfo.class),WMSLayerInfo.class)){
                LayerTuple lt = new LayerTuple(r.getName(), r.getTitle(), new StoreTuple(r.getStore().getName(),StoreTypes.DATA, new WorkspaceTuple(workspace)));
                ReferencedEnvelope bb = r.getNativeBoundingBox();
                lt.minX=bb.getMinX();
                lt.minY=bb.getMinY();
                lt.maxX=bb.getMaxX();
                lt.maxY=bb.getMaxY();
                lt.crs=r.getSRS();
                lt.nativeName=r.getNativeName();
                add(lt);
            }
        }};
     }
            
    @Override    
    public boolean addSQLLayer(final SqlLayerTuple slt){
        FeatureTypeInfo ft= fact.newFeatureTypeInfo(
            slt.store.workspace.name,
            slt.store.name,
            slt.name
        );
        ft.getMetadata().put(
            FeatureTypeInfo.JDBC_VIRTUAL_TABLE, 
            fact.newVirtualTable(
                slt.name,
                slt.sql,
                slt.geomEncList,
                slt.paramEncList
            )
        );
        cat.add(ft);
        return false;
    }
        
    @SuppressWarnings("serial")
    @Override    
    public SqlLayerTuple getSQLLayer(String workspace,String store,final String feature_name){     
        FeatureTypeInfo ft=cat.getFeatureTypeByDataStore(cat.getDataStoreByName(workspace, store), feature_name);
        VirtualTable vt = ft.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE,VirtualTable.class);
        return new SqlLayerTuple(
            ft.getName(),
            ft.getTitle(), 
            new StoreTuple(store, StoreTypes.DATA, new WorkspaceTuple(workspace)), 
            vt.getSql(), 
            new ArrayList<VTGeometryTuple>(){{
                for(String g:vt.getGeometries())
                    add(new VTGeometryTuple(g, vt.getGeometryType(g).getTypeName(), vt.getNativeSrid(g)+""));
            }}, 
            new ArrayList<VTParameterTuple>(){{
                for(String p:vt.getParameterNames()){
                    VirtualTableParameter par=vt.getParameter(p);
                    RegexpValidator val = (RegexpValidator)par.getValidator();
                    add(new VTParameterTuple(par.getName(),par.getDefaultValue(),val.getPattern().pattern()));
                }
            }}
        );
    }
        
    @Override    
    public boolean updateSQLLayer(final SqlLayerTuple slt){
        FeatureTypeInfo ft=cat.getFeatureTypeByDataStore(cat.getDataStoreByName(slt.store.workspace.name, slt.store.name), slt.name);
        ft.getMetadata().put(
            FeatureTypeInfo.JDBC_VIRTUAL_TABLE, 
            fact.newVirtualTable(
                slt.name,
                slt.sql,
                slt.geomEncList,
                slt.paramEncList
            )
        );
        
        cat.save(ft);
        return false;
    }

    @Override        
    public String getDefaultStyle(String layer){
        LayerInfo li=cat.getLayerByName(layer);
        return li.getDefaultStyle().getName();
    }
    
    @Override    
    public String assignStyle(final String workspace,final String layer,final String style){        
        FeatureTypeInfo ft=cat.getFeatureTypeByName(workspace,layer);        
        LayerInfo l = cat.getLayer(ft.getId());
        l.setDefaultStyle(cat.getStyleByName(style));
        cat.save(l);
        return null;
    }

    @Override    
    public String assignOptStyle(final String workspace,final String layer,final String style){     
        FeatureTypeInfo ft=cat.getFeatureTypeByName(workspace,layer);        
        LayerInfo l = cat.getLayer(ft.getId());
        l.getStyles().add(cat.getStyleByName(style));
        cat.save(l);
        return null;
    }

    @Override    
    public String removeStyle(final String layer,final String style){
        LayerInfo l=cat.getLayerByName(layer);
        l.getStyles().remove(cat.getStyleByName(style));
        cat.save(l);
        return null;
    }

    @Override    
    public String addWmsLayer(String workspace,String store,final String layer,final String name,final String title,final Map<String,String> opts){
        cat.add(fact.newWMSLayerInfo(workspace,store,layer,name,title,opts));
        return null;
    }
    
    @Override    
    public boolean removeWmsLayer(String workspace,String store,String layerName){
        cat.remove(cat.getResourceByStore(cat.getStoreByName(workspace, store, WMSStoreInfo.class), layerName, WMSLayerInfo.class));
        return false;
    }
    
    @Override    
    public boolean removeShapefile(String workspace, String layerName){
        LayerInfo l = cat.getLayer(layerName);
        cat.remove(l);
        //TODO: workspace is for compatibility
        return false;
    }
               
    @Override    
    public ArrayList<String> getRoles(){
        //TODO
        
        return null;
    }

    @Override    
    public HashMap<String,ArrayList<String>> getUserRoleRef(){
        //TODO
        return null;
    }

    @Override    
    public HashMap<String,ArrayList<String>> getGroupRoleRef(){
        //TODO
        return null;
    }
    
    @Override    
    public String addRole(final String name){
        //TODO
        return null;
    }

    @Override    
    public String addRole(final String name,final String parent){
        //TODO
        return null;
    }
    
    @Override    
    public String addUserRoleRef(String name,String role){
        //TODO
        return null;
    }

    @Override    
    public String addGroupRoleRef(String name,String role){
        //TODO
        return null;
    }
    
    @Override    
    public String addRoleToUser(final String role,final String user){
        //TODO
        return null;
    }

    @Override    
    public String addRoleToGroup(final String role,final String group){
        //TODO
        return null;
    }
        
    @Override    
    public String delRole(final String name){
        //TODO
        return null;
    }
    
    @Override    
    public String delRoleRefFromUser(final String role,final String user){
        //TODO
        return null;
    }
    
    @Override    
    public String delRoleRefFromGroup(final String role,final String group){
        //TODO
        return null;
    }

    @Override    
    public HashMap<String, ArrayList<String>> getRules(){
        //TODO
        return null;
    }

    @Override    
    public HashMap<String, ArrayList<String>> getRules(final String workspace){
        //TODO
        return null;
    }
    
    @Override    
    public HashMap<String, ArrayList<String>> getRules(final String workspace,final String layer){
        //TODO
        return null;
    }
    
    @Override    
    public HashMap<String, ArrayList<String>> getReaders(final String workspace,final String layer,final Boolean strict){
        //TODO
        return null;
    }

    @Override    
    public HashMap<String, ArrayList<String>> getReaders(final String workspace,final Boolean strict){
        //TODO
        return null;
    }
    
    @Override    
    public HashMap<String, ArrayList<String>> getWrites(final String workspace,final String layer,final Boolean strict){
        //TODO
        return null;
    }
    
    @Override    
    public HashMap<String, ArrayList<String>> getWrites(final String workspace,final Boolean strict){
        //TODO
        return null;
    }

    @Override    
    public HashMap<String, ArrayList<String>> getAdmins(final String workspace,final String layer,final Boolean strict){
        //TODO
        return null;
    }

    @Override    
    public HashMap<String, ArrayList<String>> getAdmins(final String workspace,final Boolean strict){
        //TODO
        return null;
    }
    
    @Override    
    public String createRule(String workspace,String layer,char access_mode, List<String> roles){
        //TODO
        return null;
    }

    @Override    
    public String deleteRule(String rule){
        //TODO
        return null;
    }
    
    @Override    
    public String deleteRule(final String workspace,final String layer,final char access_mode){
        //TODO
        return null;
    }
            
    @Override    
    public String addToRule(String rule, List<String> roles){
        //TODO
        return null;
    }

    @Override    
    public String addOrCreateRule(String rule, List<String> roles,Boolean add_only){
        //TODO
        return null;
    }
    
    @Override    
    public String addToRule(String workspace,String layer,char access_mode, List<String> roles){
        //TODO
        return null;
    }
    
    @Override    
    public String addOrCreateRule(final String workspace,final String layer,final char access_mode, final List<String> roles,final Boolean add_only){
        //TODO
        return null;
    }
    
    @Override    
    public String delToRule(String rule, final List<String> roles,final boolean ignore_missing){
        //TODO
        return null;
    }
    
    @Override    
    public String delToRule(final String workspace,final String layer,final char access_mode, final List<String> roles,final boolean ignore_missing){
        //TODO
        return null;
    }

    @Override    
    public ArrayList<StyleTuple> getAllStyles(){        
        //TODO
        return null;
    }
    
    @Override    
    public ArrayList<StyleTuple> getStyles(){
        return getStyles(null);
    }
    
    @Override    
    public ArrayList<StyleTuple> getStyles(final String workspace){
        //TODO
        return null;
    }

    @Override    
    public boolean upload(String name,String sld,String workspace){
        //TODO
        return false;
    }

    @Override    
    public boolean upload(String name,String sld){
        //TODO
        return false;
    }

    @Override    
    public ArrayList<String> getUsers(){    
        //TODO
        return null;
    }

    @Override    
    public ArrayList<String> getGroups(){
        //TODO
        return null;
    }
    
    @Override    
    public HashMap<String,ArrayList<String>> getMembers(){
        //TODO
        return null;
    }
    
    @Override    
    public String addUser(final String name,final String passwd){
        //TODO
        return null;
    }

    @Override    
    public String addGroup(final String name){
        //TODO
        return null;
    }

    @Override    
    public String addMember(final String name,final String group,final Boolean unique){
        //TODO
        return null;
    }
    
    @Override    
    public String[] addUserGroup(final String name,final String passwd,String group){
        //TODO
        return null;
    }
    
    @Override    
    public String delUser(final String name){
        //TODO
        return null;
    }

    @Override    
    public String delGroup(final String name){
        //TODO
        return null;
    }

    @Override    
    public String delMember(final String name,final String group){
        //TODO
        return null;
    }
    
    @Override    
    public String getSrsLayer(String layerName){        
        //TODO
        return null;
    }
    
    @Override    
    public boolean linkPGSchema(String workspace,GSPostGISDatastoreEncoder store){
        //TODO
        return false;
    }

    @Override    
    public boolean linkPGTable(final LayerTuple lt,final String nativeCRS){
        //TODO
        return false;
    }
    
    @Override    
    public boolean removeFeatureType(String workspace, String storename, String layerName){
        //TODO
        return false;
    }
    
    @Override    
    public boolean removeCoverageLayer(String workspace, String storename, String layerName){
        //TODO
        return false;
    }
}
