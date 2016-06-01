package it.egeos.geoserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.geotools.geometry.jts.Geometries;
import org.geotools.referencing.CRS;

import it.egeos.geoserver.dbmanagers.GeoserverManager;
import it.egeos.geoserver.restmanagers.tuples.LayerGroupTuple;
import it.egeos.geoserver.restmanagers.tuples.LayerTuple;
import it.egeos.geoserver.restmanagers.tuples.SqlLayerTuple;
import it.egeos.geoserver.restmanagers.tuples.StoreTuple;
import it.egeos.geoserver.restmanagers.tuples.StyleTuple;
import it.egeos.geoserver.restmanagers.tuples.VTGeometryTuple;
import it.egeos.geoserver.restmanagers.tuples.VTParameterTuple;
import it.egeos.geoserver.restmanagers.tuples.WorkspaceTuple;
import it.egeos.geoserver.restmanagers.types.StoreTypes;

public class Main {

    @SuppressWarnings({ "deprecation", "serial" })
    public static void main(String[] args) throws Exception {


        String connectionUrl="jdbc:postgresql://localhost:5432/geoviewer";
        String dbUser="geoviewer";
        String dbPasswd="miofratelloefigliounico";
        
        GeoserverManager gm=new GeoserverManager(dbUser, dbPasswd, connectionUrl);
        
        System.out.println("Prima");

        
        WorkspaceTuple ws= new WorkspaceTuple("c1p");
        
        gm.addWorkspace(ws.name);

        //DataStore
        gm.createPostgisStore(ws, "postgis", "localhost", 5432,dbUser,dbUser,dbPasswd);
        
        //SQLView
        gm.addSQLLayer(
            new SqlLayerTuple(
                "sqlview", 
                "sqlview",
                new StoreTuple("postgis", null, new WorkspaceTuple(ws.name)), 
                "select * from \"c1p\".\"66006_Zone_Protezione_Speciale\"", 
                new ArrayList<VTGeometryTuple>(){{
                    add(new VTGeometryTuple("the_geom",Geometries.GEOMETRY.getName(), "32633"));
                }}, 
                new ArrayList<VTParameterTuple>(){{
                    
                }}
            )
        );
        
        //PostgisLayer
        gm.linkPGTable(new LayerTuple("66006_Parchi_e_Riserve", "postgis_layer", new StoreTuple("postgis", "", new WorkspaceTuple(ws.name))), "EPSG:3857");
        gm.assignStyle(ws.name, "66006_Parchi_e_Riserve", "green");
        gm.assignOptStyle(ws.name, "66006_Parchi_e_Riserve", "line");
        gm.assignOptStyle(ws.name, "66006_Parchi_e_Riserve", "geo:pippo2");
        gm.assignOptStyle(ws.name, "66006_Parchi_e_Riserve", "burg");        
        gm.removeStyle(ws.name, "66006_Parchi_e_Riserve", "burg");
        
        gm.linkPGTable(new LayerTuple("66006_Zone_Protezione_Speciale", "postgis_layer", new StoreTuple("postgis", "", new WorkspaceTuple(ws.name))), "EPSG:3857");
        
        gm.removeShapefile(ws.name, "66006_Zone_Protezione_Speciale");
        
        //WMSStore
        gm.createWmsStore(ws, "aaaa", "http://share.egeos-services.it/reflector/dev2/service", null, null);
        
        //WMSLayer
        gm.addWmsLayer(ws.name, "aaaa", "rv2", "rv2_name", "rv2_title", new HashMap<String, String>(){{
            put("srs","EPSG:3857");
            put("projectionPolicy","REPROJECT_TO_DECLARED");
        }});

        gm.addWmsLayer(ws.name, "aaaa", "rv2", "rv2_todelete", "rv2_todelete", new HashMap<String, String>(){{
            put("srs","EPSG:3857");
            put("projectionPolicy","REPROJECT_TO_DECLARED");
        }});

        
        gm.addWmsLayer(ws.name, "aaaa", "rv1", "rv1", "rv1", new HashMap<String, String>(){{
            put("srs","EPSG:3857");
            put("projectionPolicy","REPROJECT_TO_DECLARED");
        }});
        
        gm.updateSQLLayer(
            new SqlLayerTuple(
                "sqlview", 
                "rimpiazzo",
                new StoreTuple("postgis", null, new WorkspaceTuple(ws.name)), 
                "select * from \"c1p\".\"66006_Zone_Protezione_Speciale\" where 1=1", 
                new ArrayList<VTGeometryTuple>(){{
                    add(new VTGeometryTuple("the_geom",Geometries.GEOMETRY.getName(), "32633"));
                }}, 
                new ArrayList<VTParameterTuple>(){{
                    
                }}));
        
        gm.removeWmsLayer(ws.name, "aaaa", "rv2_todelete");
        
        //Layergroup
        gm.createLayerGroup(ws, "lg_ws00",new ArrayList<LayerTuple>(){{
            add(new LayerTuple("sqlview", "rimpiazzo",new StoreTuple("postgis","", new WorkspaceTuple(ws.name))));
        }});
        
        System.out.println("Cerco 'aaaa' "+(gm.getWmsStore(ws.name, "aaaa")!=null?"trovato":"mancante"));
        System.out.println("Cerco 'bbbb' come WMSStore "+(gm.getWmsStore(ws.name, "bbbb")!=null?"trovato":"mancante"));
        
        gm.assignSubLayers(ws.name, "lg_ws00", new LinkedHashMap<LayerTuple, String>(){{
            put(new LayerTuple("rv2_name", null, new StoreTuple("aaaa", StoreTypes.WMS, new WorkspaceTuple(ws.name))),"green");
            put(new LayerTuple("rv2_name", null, new StoreTuple("aaaa", StoreTypes.WMS, new WorkspaceTuple(ws.name))),null);
            put(new LayerTuple("rv2_name", null, new StoreTuple("aaaa", StoreTypes.WMS, new WorkspaceTuple(ws.name))),"grass");
        }});
        
        
        System.out.println("Features in "+ws.name);
        ArrayList<LayerTuple> fls = gm.getFeatureLayersList(ws.name);
        for(LayerTuple l:fls){
            System.out.println("\t"+l);
            for(StyleTuple s:gm.getLayerStyles(ws.name,l.name))
               System.out.println("\t\tS:"+s.workspace+"."+s.name);
            System.out.println("\t\tDef: "+gm.getDefaultStyle(ws.name,l.name));
            
        }
        System.out.println("Coverages in "+ws.name);
        ArrayList<LayerTuple> fls1 = gm.getCoverageLayersList(ws.name);
        for(LayerTuple l:fls1) 
            System.out.println("\t"+l);

        System.out.println("WMSs in "+ws.name);
        ArrayList<LayerTuple> fls2 = gm.getWmsLayersList(ws.name);
        for(LayerTuple l:fls2){ 
            System.out.println("\t"+l);            
        }
        
        System.out.println("LayerGroups in "+ws.name);
        for(LayerGroupTuple lg:gm.getLayerGroups(ws.name)){
            System.out.println("\t"+lg.getName()+" ("+lg.getTitle()+")");
            LinkedHashMap<LayerTuple, StyleTuple> subs = gm.getSubLayers(ws.name, lg.getName());
            for(LayerTuple sl:subs.keySet()){
                System.out.println("\t\t"+sl.name+" "+subs.get(sl));
            }
        }
        

        System.out.println("Styles for "+gm.getDefaultStyle("geo","66006_Parchi_e_Riserve"));
        for(StyleTuple s:gm.getLayerStyles("geo","66006_Parchi_e_Riserve"))
            System.out.println("\t\tS:"+s.workspace+"."+s.name);
        
/*        */
        
        
        //gm.deleteStore(ws, "bbbb");
        //gm.deleteStore(ws, "aaaa");
        //gm.deleteLayerGroup(ws.name,"lg_ws00");
        //gm.deleteWorkspace("c1p");
        
        
/*        for(WorkspaceInfo w:cat.getWorkspaces()){
            System.out.println("w: "+w.getName());
            
            System.out.println("\tdatastores:");            
            for(StoreTuple s:gm.getDataStores(new WorkspaceTuple(w.getName()))){
                System.out.println("\t\td: "+s.name);
            }
            
            System.out.println("\tcoverstores:");
            for(StoreTuple s:gm.getCoverageStores(new WorkspaceTuple(w.getName()))){
                System.out.println("\t\tc: "+s.name);            
            }

            System.out.println("\twmsstores:");
            for(StoreTuple s:gm.getWmsStores(new WorkspaceTuple(w.getName()))){
                System.out.println("\t\tw: "+s.name);            
            }

            System.out.println("\tlayergroups:");
            for(LayerGroupTuple s:gm.getLayerGroups(w.getName())){
                System.out.println("\t\tw: "+s.getName());            
            }

            
        }*/
        
       

        /* 
        * System.out.println("Add...");

        
        
        
        System.out.println("Dopo");
        for(WorkspaceTuple w:gm.getWorkspaces())
            System.out.println("w:"+w);
        
        System.out.println("Del...");
        

        System.out.println("Final");
        for(WorkspaceTuple w:gm.getWorkspaces())
            System.out.println("w:"+w);
        */
        
        
        

/*        Catalog cat = CatalogLoader.getCatalog(driver, connectionUrl, dbUser, dbPasswd);
        Factory f=new Factory(cat);
        
        
        for(WorkspaceInfo s:cat.getWorkspaces()){
            System.out.println("--> "+s.getName()+" "+s.toString());
        }*/
        
        
        
/*        
        NamespaceInfo ni=f.newNamespace("Oceania2","http://Oceania2");
        WorkspaceInfo ws = f.newWorkspace("Oceania2");
        
        cat.add(ni);
        cat.add(ws);
        */
        
        
        System.out.println("Fine ");
    }
    

}
