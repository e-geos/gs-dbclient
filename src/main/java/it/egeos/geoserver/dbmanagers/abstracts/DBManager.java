package it.egeos.geoserver.dbmanagers.abstracts;

import org.apache.log4j.Logger;

/**
 * 
 * @author Federico C. Guizzardi - cippinofg <at> gmail.com
 * 
 * DbManager is a (abstract) class to comunicate with Geoserver by db
 * 
 */

public abstract class DBManager {
	protected Logger log = Logger.getLogger(this.getClass());
		
	public DBManager(String login,String password,String url) {
	}
	
	public void shutdown(){	    
	}
	
	@Override
	protected void finalize() throws Throwable {
	    super.finalize();
	}	
}

