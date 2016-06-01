package it.egeos.geoserver.dbmanagers;

import java.io.IOException;
import java.io.Serializable;
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
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.geotools.util.Converters;

import it.egeos.geoserver.dbmanagers.abstracts.DBManager;
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

/**
 * 
 * @author Federico C. Guizzardi - cippinofg <at> gmail.com
 * 
 * DBManager implementation to control a Geoserver
 *
 */

public class GeoserverManager extends DBManager implements GeoserverManagerAPI{
    private Catalog cat; 
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
    }
    
    /** WORKSPACES **/
    
    @Override
    @SuppressWarnings("serial")
    public ArrayList<WorkspaceTuple> getWorkspaces(){
        return new ArrayList<WorkspaceTuple>(){{
            for(WorkspaceInfo w:cat.getWorkspaces())
                add(new WorkspaceTuple(w.getName()));
        }};
    }
    
    public boolean createWorkspace(String ws){               
        cat.add(newNamespace(ws,"http://"+ws));
        cat.add(newWorkspace(ws));        
        return true;
    }
    
    public boolean deleteWorkspace(String ws){
        WorkspaceInfo wi = cat.getWorkspaceByName(ws);
        getBuilder().removeWorkspace(wi, true);
        return true;
    }
    
    /** STORES **/

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

    @Override
    public WmsStoreTuple getWmsStore(final String workspace,String name){
        WMSStoreInfo st = cat.getStoreByName(workspace, name, WMSStoreInfo.class);
        return st!=null?new WmsStoreTuple(st.getName(), StoreTypes.WMS, new WorkspaceTuple(workspace),st.getCapabilitiesURL(),st.getUsername(),st.getPassword()):null;
    }
    
    public DataStoreInfo createPostgisStore(final WorkspaceTuple workspace,final String name,final String host,final Integer port,final String dbname,final String usr,final String pwd){
        DataStoreInfo ds = newDataStoreInfo(cat.getWorkspaceByName(workspace.name), name,"PostGIS");
        Map<String, Serializable> pars = ds.getConnectionParameters();
        pars.put("schema", workspace.name);
        pars.put("database",dbname);
        pars.put("host",host);
        pars.put("port",port);
        pars.put("passwd",pwd);
        pars.put("dbtype","postgis");
        pars.put("user",usr);
        cat.add(ds);
        return ds;
    }
    
    @Override    
    public String createWmsStore(final WorkspaceTuple workspace,final String name,final String url,final String usr, final String pwd){
        WMSStoreInfo st = newWmsStore(cat.getWorkspaceByName(workspace.name),name,url,usr,pwd);
        cat.add(st);
        return st.getId();
    }

    public void deleteStore(final WorkspaceTuple workspace,String name){       
        deleteStore(cat.getStoreByName(workspace.name, name, StoreInfo.class));
    }
    
    public void deleteStore(StoreInfo st){
        CatalogBuilder b = getBuilder(st);
        b.removeStore(st, true);
    }
    
    /** LAYERGROUPS **/
    

    public LayerGroupInfo createLayerGroup(final WorkspaceTuple workspace,final String layer,final List<LayerTuple> subs){
        LayerGroupInfo lg = newLayerGroup(cat.getWorkspaceByName(workspace.name),layer);        
        if(lg!=null){
            for(LayerTuple s:subs){
                PublishedInfo l = s.isLayerGroup()?cat.getLayerGroupByName(lg.getWorkspace(),s.name):cat.getLayerByName(s.name);                
                lg.getLayers().add(l);            
                lg.getStyles().add(null);           
            }
            try {
                getBuilder().calculateLayerGroupBounds(lg);
            }
            catch (Exception e) {
                log.error("Can't calculateLayerGroupBounds of a layergroup in '"+lg.getWorkspace().getName()+"' with name '"+lg.getName()+"': "+e.getMessage(), e);            
            }
        }
        cat.add(lg);        
        return lg;        
    }
    
    /*
     * Sets 'publishables' and 'styles' using subs, a <name,style> list 
     */  
    public void setSubLayers(String workspace,String glayer,final LinkedHashMap<LayerTuple, String> subs){
        LayerGroupInfo lg = cat.getLayerGroupByName(workspace, glayer);
        System.out.println("Trovato "+lg);
        
        if(lg!=null){
            lg.getLayers().clear();
            lg.getStyles().clear();
            for(LayerTuple s:subs.keySet()){
                PublishedInfo l = s.isLayerGroup()?cat.getLayerGroupByName(lg.getWorkspace(),s.name):cat.getLayerByName(s.name);
                System.out.println("L: "+s.name+" "+l);
                String style=subs.get(s);                
                lg.getLayers().add(l);            
                lg.getStyles().add(style!=null?cat.getStyleByName(style):null);           
            }
            try {
                getBuilder().calculateLayerGroupBounds(lg);
            }
            catch (Exception e) {
                log.error("Can't calculateLayerGroupBounds of a layergroup in '"+lg.getWorkspace().getName()+"' with name '"+lg.getName()+"': "+e.getMessage(), e);            
            }
            cat.save(lg);
        }        
    }
    
    
    
    @Override    
    public Boolean deleteLayerGroup(String workspace,String layer){
        deleteLayerGroup(cat.getLayerGroupByName(workspace, layer));            
        return true;
    }
    
    public void deleteLayerGroup(LayerGroupInfo lg) {    
        cat.remove(lg);        
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
    
    /** Layers **/
    @SuppressWarnings("serial")
    @Override    
    public ArrayList<LayerTuple> getFeatureLayersList(final String workspace){
        return new ArrayList<LayerTuple>(){{                    
            for(DataStoreInfo st:cat.getDataStoresByWorkspace(cat.getWorkspaceByName(workspace)))                
                for(FeatureTypeInfo ft:cat.getFeatureTypesByDataStore(st)){                
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
    public ArrayList<LayerTuple> getCoverageLayersList(final String workspace){
        return new ArrayList<LayerTuple>(){{
            for(CoverageStoreInfo st:cat.getCoverageStoresByWorkspace(cat.getWorkspaceByName(workspace)))                
                for(CoverageInfo ft:cat.getCoveragesByCoverageStore(st)){                
                    LayerTuple lt = new LayerTuple(ft.getName(), ft.getTitle(), new StoreTuple(ft.getStore().getName(),StoreTypes.COVERAGE, new WorkspaceTuple(workspace)));
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
    public ArrayList<LayerTuple> getWmsLayersList(final String workspace){
        return new ArrayList<LayerTuple>(){{
            for(WMSStoreInfo st:cat.getStoresByWorkspace(workspace, WMSStoreInfo.class))
                for(WMSLayerInfo r:cat.getResourcesByStore(st, WMSLayerInfo.class)){
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

    public FeatureTypeInfo createFeatureType(final LayerTuple lt,final String nativeCRS) throws Exception{
        StoreTuple st = lt.store;        
        return newFeatureTypeInfo(st.workspace.name, st.name, lt.name,lt.title,nativeCRS);
    }

    public FeatureTypeInfo createFeatureType(final SqlLayerTuple slt) throws Exception{
        return newSqlFeatureTypeInfo(
            slt.store.workspace.name,
            slt.store.name,
            slt.name,
            slt.sql,
            slt.geomEncList,
            slt.paramEncList
        );
    }
    
    public void deleteFeatureType(String workspace, String name){        
        FeatureTypeInfo ft = cat.getFeatureTypeByName(workspace, name);        
        for(LayerInfo l:cat.getLayers(ft))
            cat.remove(l);
        getBuilder().removeResource(ft,true);
    }
    
    public WMSLayerInfo createWmsLayer(String workspace,String store,final String layer,final String name,final String title, String srs, ProjectionPolicy policy) throws Exception{
        return newWmsLayerInfo(workspace,store,layer,name,title,srs,policy);
    }
      
    public void deleteWmsLayer(String workspace,String store,String layerName){  
        WMSLayerInfo res = cat.getResourceByStore(cat.getStoreByName(workspace, store, WMSStoreInfo.class), layerName, WMSLayerInfo.class);        
        for(LayerInfo l:cat.getLayers(res))
            cat.remove(l);
        getBuilder().removeResource(res, true);                
    }
        
    public void deleteCoverageLayer(String workspace,String name){
        CoverageInfo ci = cat.getCoverageByName(workspace, name);        
        for(LayerInfo l:cat.getLayers(ci))
            cat.remove(l);
        getBuilder().removeResource(ci,true);    
    }
        
    @Override    
    public boolean updateSQLLayer(final SqlLayerTuple slt){
        FeatureTypeInfo ft=cat.getFeatureTypeByDataStore(cat.getDataStoreByName(slt.store.workspace.name, slt.store.name), slt.name);
        ft.setTitle(slt.title);
        ft.getMetadata().put(
            FeatureTypeInfo.JDBC_VIRTUAL_TABLE, 
            newVirtualTable(
                slt.name,
                slt.sql,
                slt.geomEncList,
                slt.paramEncList
            )
        );
        
        cat.save(ft);
        return false;
    }
    
    @SuppressWarnings("serial")
    public List<StyleTuple> getLayerStyles(final String workspace,final String layer){
        return new ArrayList<StyleTuple>(){{
            FeatureTypeInfo ft = cat.getFeatureTypeByName(workspace, layer);
            LayerInfo ly = cat.getLayerByName(ft.getQualifiedNativeName());
            for(StyleInfo s:ly.getStyles()){
                WorkspaceInfo ws = s.getWorkspace();
                WorkspaceTuple wst= ws!=null?new WorkspaceTuple(ws.getName()):null;
                add(new StyleTuple(s.getName(), s.getFormat(), s.getFilename(),wst));
            }
        }};
    }
    
    @Override    
    public String assignOptStyle(String workspace,String layer,String style){     
        FeatureTypeInfo ft=cat.getFeatureTypeByName(workspace,layer);        
        LayerInfo l =  cat.getLayerByName(ft.getQualifiedNativeName()); 
        l.getStyles().add(cat.getStyleByName(style));
        cat.save(l);
        return null;
    }

    public String getDefaultStyle(String workspace,String layer){
        FeatureTypeInfo ft=cat.getFeatureTypeByName(workspace,layer);        
        LayerInfo l =  cat.getLayerByName(ft.getQualifiedNativeName());        
        return l.getDefaultStyle().getName();
    }
     
    public void setDefaultStyle(String workspace,String layer,String style){        
        FeatureTypeInfo ft=cat.getFeatureTypeByName(workspace,layer);        
        LayerInfo l =  cat.getLayerByName(ft.getQualifiedNativeName());
        
        StyleInfo st = cat.getStyleByName(workspace,style);
        if (st==null)
            st = cat.getStyleByName(style);
        l.setDefaultStyle(st);
        cat.save(l);        
    }
    
    public void removeStyle(String workspace, String layer,String style){
        FeatureTypeInfo ft=cat.getFeatureTypeByName(workspace,layer);        
        LayerInfo l =  cat.getLayerByName(ft.getQualifiedNativeName());
        
        StyleInfo st = cat.getStyleByName(workspace,style);
        if (st==null)
            st = cat.getStyleByName(style);
        
        l.getStyles().remove(st);
        cat.save(l);
    }
    
    /** Styles **/
    
    @SuppressWarnings("serial")
    @Override    
    public ArrayList<StyleTuple> getAllStyles(){        
        return new ArrayList<StyleTuple>(){{
            for(StyleInfo s:cat.getStyles()){
                WorkspaceInfo ws = s.getWorkspace();                                
                add(new StyleTuple(s.getName(), s.getFormat(), s.getFilename(),ws!=null?new WorkspaceTuple(ws.getName()):null));
            }
        }};
    }
    
    public StyleInfo createStyleInfo(String workspace,String name,String sld){
        //TODO: gestire upload file
        return newStyleInfo(cat.getWorkspaceByName(workspace),name,sld);
    }
    
    
    

    
    /* Deprecated */ 

    /**
     * @deprecated replaced by {@link #createWorkspace(String ws)} 
     */
    @Deprecated
    @Override    
    public boolean addWorkspace(String ws){               
        cat.add(newNamespace(ws,"http://"+ws));
        cat.add(newWorkspace(ws));        
        return true;
    }

    /**
     * @deprecated replaced by {@link #deleteWorkspace(String ws)} 
     */
    @Override
    @Deprecated
    public boolean delWorkspace(String ws){
        return deleteWorkspace(ws);
    }
    
    /**
     * @deprecated replaced by {@link #createPostgisStore(final WorkspaceTuple workspace,final String name,final String host,final Integer port,final String dbname,final String usr,final String pwd)} 
     */
    @Deprecated
    @Override    
    public boolean linkPGSchema(final String workspace,final String name,final String host,final Integer port,final String dbname,final String usr,final String pwd){       
        createPostgisStore(new WorkspaceTuple(workspace), name,host,port,dbname,usr,pwd);        
        return true;
    }

    /**
     * @deprecated replaced by {@link #deleteStore(final WorkspaceTuple workspace,String name)}
     */
    @Deprecated
    @Override        
    public String deleteWMSStore(final WorkspaceTuple workspace,String name){       
        deleteStore(workspace, name);
        return null;
    }

    /**
     * @deprecated replaced by {@link #createLayerGroup(final WorkspaceTuple workspace,final String layer,final List<LayerTuple> subs)}
     */
    @Override    
    @Deprecated
    public String addLayerGroup(final String workspace,final String layer,final List<LayerTuple> subs){        
        return createLayerGroup(new WorkspaceTuple(workspace), layer, subs).getId();
    }
    
    /**
     * @deprecated replaced by {@link #getWmsLayersList(final String workspace)}
     */
    @Deprecated
    @SuppressWarnings("serial")
    @Override    
    public ArrayList<LayerTuple> getCoverageLayersList(final String workspace,final String store){
        return new ArrayList<LayerTuple>(){{
            for(CoverageInfo c:cat.getCoveragesByStore(cat.getCoverageStoreByName(cat.getWorkspaceByName(workspace), store))){
                LayerTuple lt = new LayerTuple(c.getName(), c.getTitle(), new StoreTuple(c.getStore().getName(),StoreTypes.COVERAGE, new WorkspaceTuple(workspace)));
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
    
    /**
     * @deprecated replaced by {@link #getCoverageLayersList(final String workspace)}
     */
    @Deprecated
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

    /**
     * deprecated replaced by {@link #addWmsLayer(String workspace,String store,final String layer,final String name,final String title, String srs, ProjectionPolicy policy) }
     */
    @Override    
    @Deprecated
    public String addWmsLayer(String workspace,String store,final String layer,final String name,final String title,final Map<String,String> opts){
        ProjectionPolicy pp=ProjectionPolicy.NONE;
        String policy = opts.get("projectionPolicy");
        if ("REPROJECT_TO_DECLARED".equals(policy))
            pp=ProjectionPolicy.REPROJECT_TO_DECLARED;
        else if ("FORCE_DECLARED".equals(policy))
            pp=ProjectionPolicy.FORCE_DECLARED;
        
        try {
            return createWmsLayer(workspace,store,layer,name,title,opts.get("srs"),pp).toString();
        }
        catch (Exception e) {
            log.error("Can't create a wms layer "+workspace+"."+store+"."+layer+": "+e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * @deprecated replaced by {@link #createFeatureType(final SqlLayerTuple slt)} 
     */
    @Deprecated
    @Override    
    public boolean addSQLLayer(final SqlLayerTuple slt) {
        try {
            return createFeatureType(slt)!=null;
        }
        catch (Exception e) {
            log.error("Can't create sql layer: "+e.getMessage(), e);
        }
        return false;
    }

    /**
     * @deprecated replaced by {@link #createFeatureType(final LayerTuple lt,final String nativeCRS)}
     */    
    @Deprecated
    @Override    
    public boolean linkPGTable(final LayerTuple lt,final String nativeCRS){
        try {
            return createFeatureType(lt,nativeCRS)!=null;
        }
        catch (Exception e) {
            log.error("Can't link pg table: "+e.getMessage(), e);
        }
        return false;
    }

    /**
     * @deprecated replaced by {@link #getLayerStyles(final String workspace,final String layer)} 
     */
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
   
    /**
     * @deprecated replaced by {@link #getDefaultStyle(final String workspace,String layer)} 
     */
    @Deprecated
    @Override        
    public String getDefaultStyle(String layer){
        LayerInfo li=cat.getLayerByName(layer);
        return li.getDefaultStyle().getName();
    }
   
    /**
     * @deprecated replaced by {@link #setDefaultStyle(final String workspace,final String layer,final String style)} 
     */    
    @Deprecated
    @Override    
    public String assignStyle(final String workspace,final String layer,final String style){
        setDefaultStyle(workspace, layer, style);
        return style;
    }
       
    /**
     * @deprecated replaced by {@link #removeStyle(String workspace, String layer,String style)} 
     */
    @Deprecated
    @Override    
    public String removeStyle(String layer,String style){
        LayerInfo l=cat.getLayerByName(layer);
        l.getStyles().remove(cat.getStyleByName(style));
        cat.save(l);
        return null;
    }
    
    /**
     * @deprecated replaced by {@link #deleteWmsLayer(String workspace,String store,String layerName)} 
     */
    @Deprecated
    @Override    
    public boolean removeWmsLayer(String workspace,String store,String layerName){  
        deleteWmsLayer(workspace, store, layerName);        
        return true;
    }
    
    /**
     * @deprecated replaced by {@link #deleteFeatureType(String workspace, String name)} 
     */
    @Deprecated
    @Override    
    public boolean removeShapefile(String workspace, String name){        
        deleteFeatureType(workspace, name);
        return false;
    }

    /**
     * @deprecated replaced by {@link #deleteFeatureType(String workspace, String name)} 
     */
    @Deprecated
    @Override    
    public boolean removeFeatureType(String workspace, String storename, String layerName){
        deleteFeatureType(workspace, layerName);
        return true;
    }

    /**
     * @deprecated replaced by {@link #deleteCoverageLayer(String workspace,String name)} 
     */
    @Deprecated
    @Override    
    public boolean removeCoverageLayer(String workspace, String storename, String name){
        deleteCoverageLayer(workspace, name);
        return true;
    }

    /**
     * @deprecated replaced by {@link #setSubLayers(String workspace,String glayer,final LinkedHashMap<LayerTuple, String> subs)} 
     */
    @Deprecated
    @Override    
    public Boolean assignSubLayers(String workspace,String glayer,LinkedHashMap<LayerTuple, String> children){
        setSubLayers(workspace, glayer, children);
        return true;
    }
    
    /**
     * @deprecated replaced by {@link #createStyleInfo(String workspace,String name,String sld)} 
     */
    @Deprecated
    @Override    
    public boolean upload(String name,String sld,String workspace){
        return createStyleInfo(workspace,name,sld)!=null;
    }

    /**
     * @deprecated replaced by {@link #createStyleInfo(String workspace,String name,String sld)} 
     */
    @Deprecated
    @Override    
    public boolean upload(String name,String sld){
        return createStyleInfo(null,name,sld)!=null;
    }
    
    //TODO: verify from here


    
    
    
        
 


    

    


  
            

        

        



    







    

    

               
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
    
    
    /** Private methods **/

    private CatalogBuilder getBuilder(){
        return new CatalogBuilder(cat);
    }
    
    private CatalogBuilder getBuilder(final WorkspaceInfo workspace){
        return new CatalogBuilder(cat){{
            setWorkspace(workspace);
        }};        
    }    

    private CatalogBuilder getBuilder(final StoreInfo store){
        return new CatalogBuilder(cat){{
            setStore(store);
        }};        
    }
    
    private WorkspaceInfo newWorkspace(String name){
        WorkspaceInfo ws = cat.getFactory().createWorkspace();
        ws.setName(name);          
        return ws;
    }

    private NamespaceInfo newNamespace(String prefix,String uri) {
        NamespaceInfo ns = cat.getFactory().createNamespace();
        ns.setPrefix(prefix);
        ns.setURI(uri);           
        return ns;
    }

    private WMSStoreInfo newWmsStore(WorkspaceInfo ws,final String name,final String url,final String usr, final String pwd) {       
        WMSStoreInfo st = cat.getFactory().createWebMapServer();
        st.setName(name);
        st.setCapabilitiesURL(url);
        st.setWorkspace(ws);
        st.setEnabled(true);
        st.setUsername(usr);
        st.setPassword(pwd);
        st.setType("WMS");
        st.setEnabled(true);
        return st;
    }
    
    private DataStoreInfo newDataStoreInfo(WorkspaceInfo ws,final String name, String type){        
        CatalogBuilder b = getBuilder(ws);
        return b.buildDataStore(name);
    }
    
    private LayerGroupInfo newLayerGroup(WorkspaceInfo ws, String name){
        LayerGroupInfo lg=cat.getFactory().createLayerGroup();
        lg.setWorkspace(ws);
        lg.setName(name);
        return lg;
    }

    private WMSLayerInfo newWmsLayerInfo(String workspace,String store,final String layer,final String name,final String title,String srs, ProjectionPolicy policy) throws IOException{
        WMSStoreInfo st = cat.getStoreByName(workspace, store, WMSStoreInfo.class);
        CatalogBuilder b = getBuilder(st);        
        WMSLayerInfo l = b.buildWMSLayer(layer);
        l.setTitle(title);
        l.setName(name);
        l.setNativeName(layer);
        l.setSRS(srs);
        l.setProjectionPolicy(policy);
        l.setEnabled(true);                
        cat.add(l);
        cat.add(b.buildLayer(l));
        return l;
    }
    
    private FeatureTypeInfo newSqlFeatureTypeInfo(String workspace,String store,String name,String sql, List<VTGeometryTuple> geomEncList, List<VTParameterTuple> paramEncList) throws Exception{
        DataStoreInfo st = cat.getStoreByName(workspace, store, DataStoreInfo.class);
        JDBCDataStore ds = (JDBCDataStore)st.getDataStore(null);
        CatalogBuilder b = getBuilder(st);     
        VirtualTable vt = newVirtualTable(name,sql,geomEncList,paramEncList);
        ds.createVirtualTable(vt);
        FeatureTypeInfo ft = b.buildFeatureType(ds.getFeatureSource(vt.getName()));
        ft.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
        
        //This is a trick to generate NativeBoundingBox from db data
        ft.setNativeBoundingBox(new ReferencedEnvelope());
        ReferencedEnvelope box = b.getNativeBounds(ft);       
        ft.setNativeBoundingBox(box);
        ft.setLatLonBoundingBox(b.getLatLonBounds(box, box.getCoordinateReferenceSystem()));
        
        cat.add(ft);
        cat.add(b.buildLayer(ft));        
        return ft;
    }

    private VirtualTable newVirtualTable(String name, String sql,List<VTGeometryTuple> geoms,List<VTParameterTuple> pars){
        VirtualTable vt=new VirtualTable(name, sql);
        for(VTGeometryTuple g:geoms)
            vt.addGeometryMetadatata(g.getName(),Geometries.getForName(g.getGeometryType()).getBinding(),Converters.convert(g.getSrid(), Integer.class));    
        for(VTParameterTuple p:pars)
            vt.addParameter(new VirtualTableParameter(p.getName(), p.getDefaultValue(),new RegexpValidator(p.getRegexpValidator())));
        return vt;
    }

    private FeatureTypeInfo newFeatureTypeInfo(String workspace,String store,String name,String title, String srs) throws Exception{
        DataStoreInfo st = cat.getStoreByName(workspace, store, DataStoreInfo.class);
        JDBCDataStore ds = (JDBCDataStore)st.getDataStore(null);
        CatalogBuilder b = getBuilder(st);        
                
        FeatureTypeInfo ft = b.buildFeatureType(ds.getFeatureSource(name));
        
        ft.setStore(cat.getStoreByName(workspace, store, StoreInfo.class));
        ft.setName(name);
        ft.setNativeName(name);
        ft.setTitle(title);        
        ReferencedEnvelope box = b.getNativeBounds(ft);       
        ft.setNativeBoundingBox(box);
        ft.setLatLonBoundingBox(b.getLatLonBounds(box, box.getCoordinateReferenceSystem()));
        cat.add(ft);
        cat.add(b.buildLayer(ft)); 
        return ft;        
    }    

    private StyleInfo newStyleInfo(WorkspaceInfo ws,String name,String filename){
        StyleInfoImpl style=new StyleInfoImpl(cat);
        style.setName(name);        
        style.setWorkspace(ws);
        style.setFilename(filename);
        
        if(cat.validate(style, true).isValid())
            cat.add(style);
        else
            style=null;
        
        return style;
    }
}
