package it.egeos.geoserver;

import it.egeos.geoserver.dbmanagers.CatalogLoader;
import it.egeos.geoserver.dbmanagers.Factory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.jdbc.VirtualTable;


public class Main {

    public static void main(String[] args) throws Exception {

        String driver="org.postgresql.Driver";
        String connectionUrl="jdbc:postgresql://localhost:5432/gscatalog";
        String dbUser="gscatalog";
        String dbPasswd="gscatalog";
        
        Catalog cat = CatalogLoader.getCatalog(driver, connectionUrl, dbUser, dbPasswd);
        
        System.out.println("Namespaces");
        for(NamespaceInfo s:cat.getNamespaces()){
            System.out.println("++> "+s.getName()+" "+s.getPrefix()+" "+s.getURI());
            for(FeatureTypeInfo l:cat.getFeatureTypesByNamespace(s))
                System.out.println("\tL:"+l.getName()+" "+l.getStore().getType());
            
        }
        for(WorkspaceInfo s:cat.getWorkspaces()){
            System.out.println("--> "+s.getName()+" "+s.toString());
            for(LayerGroupInfo l:cat.getLayerGroupsByWorkspace(s.getName())){
                System.out.println("\tLG:"+l.getName());
                for(PublishedInfo sl:l.getLayers()){
                    String style="undef";
                    if (sl instanceof LayerInfo)
                        style=((LayerInfo)sl).getDefaultStyle().getName();
                    
                    System.out.println("\t\t"+sl.getName()+" "+style);
                }
                
                System.out.println("\tMetadata");
                for(StyleInfo id:l.getStyles())
                    try {
                        System.out.println("\t"+id.getName());
                    }
                    catch (Exception e) {
                        System.out.println("\tfail "+e.getMessage());
                    }
            }
        }
        
        System.out.println("=========================================================================");
        for(FeatureTypeInfo l:cat.getFeatureTypes()){
            System.out.println("--"+l.getName());
            MetadataMap m = l.getMetadata();
                        
            for(String k:m.keySet())
                System.out.println("\t\t"+k+" "+m.get(k));
            
           VirtualTable vt = m.get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, VirtualTable.class);
           System.out.println("\tvt: "+(vt!=null?vt.getSql():"no sql"));
        }
        Factory f=new Factory(cat);
/*        
        NamespaceInfo ni=f.newNamespace("Oceania2","http://Oceania2");
        WorkspaceInfo ws = f.newWorkspace("Oceania2");
        
        cat.add(ni);
        cat.add(ws);
        */
        
        
        System.out.println("Fine "+cat);
    }
    

}
