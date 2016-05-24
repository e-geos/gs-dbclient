package it.egeos.geoserver;

import java.util.ArrayList;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;

import it.egeos.geoserver.dbmanagers.GeoserverManager;
import it.egeos.geoserver.restmanagers.tuples.LayerGroupTuple;
import it.egeos.geoserver.restmanagers.tuples.LayerTuple;
import it.egeos.geoserver.restmanagers.tuples.StoreTuple;
import it.egeos.geoserver.restmanagers.tuples.WorkspaceTuple;


public class Main {

    @SuppressWarnings("serial")
    public static void main(String[] args) throws Exception {


        String connectionUrl="jdbc:postgresql://localhost:5432/geoviewer";
        String dbUser="geoviewer";
        String dbPasswd="miofratelloefigliounico";
        
        GeoserverManager gm=new GeoserverManager(dbUser, dbPasswd, connectionUrl);
        
        System.out.println("Prima");
        Catalog cat = gm.getCat();
        
        WorkspaceTuple ws= new WorkspaceTuple("c1p");
        
        gm.addWorkspace(ws.name);
        gm.createPostgisStore(ws, "bbbb", "localhost", 5432, "geoviewer", "geoviewer","miofratelloefigliounico");
        gm.createWmsStore(ws, "aaaa", "http://share.egeos-services.it/reflector/dev2/service", null, null);
        
        System.out.println("Cerco 'aaaa' "+(gm.getWmsStore(ws.name, "aaaa")!=null?"trovato":"mancante"));
        System.out.println("Cerco 'bbbb' come WMSStore "+(gm.getWmsStore(ws.name, "bbbb")!=null?"trovato":"mancante"));
        //add layer 1
        
        
        gm.createLayerGroup(ws, "lg_ws00",new ArrayList<LayerTuple>(){{
            //add layer 1 in lg
        }});
        
        
        gm.deleteStore(ws, "bbbb");
        gm.deleteStore(ws, "aaaa");
        gm.deleteLayerGroup(ws.name,"lg_ws00");
        gm.deleteWorkspace("c1p");
        
        
        for(WorkspaceInfo w:cat.getWorkspaces()){
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

            
        }
        
       

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
