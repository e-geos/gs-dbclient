package it.egeos.geoserver.dbmanagers;

import java.io.IOException;

import org.apache.commons.dbcp.BasicDataSource;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding;

public class CatalogLoader  {
    public static Catalog getCatalog(String driver, String connectionUrl, String dbUser,String dbPasswd) throws IOException {
        CatalogImpl catalog = new CatalogImpl();        
        catalog.setFacade(new JDBCCatalogFacade(new ConfigDatabase(
                new BasicDataSource(){{        
                    setDriverClassName(driver);
                    setUrl(connectionUrl);
                    setUsername(dbUser);
                    setPassword(dbPasswd);
                    setMinIdle(3);
                    setMaxActive(10);
                }},
                new XStreamInfoSerialBinding(
                    new XStreamPersisterFactory()
                )
            ){{
                initDb(null);    
            }}));
        return catalog;
    }    
}
