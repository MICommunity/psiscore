package org.hupo.psi.mi.psiscore.ws.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.PsiscoreFault;
import org.hupo.psi.mi.psiscore.util.PsiTools;

public class PsiscoreServerProperties {
	
	private static PsiscoreServerProperties instance = new PsiscoreServerProperties();
	private Properties props = null;
	
	public static PsiscoreServerProperties getInstance(){
		return instance;
	}
	
	private PsiscoreServerProperties(){
		/*String path = getClass().getProtectionDomain().getCodeSource().
		   getLocation().toString().substring(6);*/
		props = loadProperties();

	}
	
	public Properties getProperties(){
		return props;
	}
	
	private Properties loadProperties(){
    	InputStream is = null;
    	// be careful if you move this method to another class as the relative path in the webapp might change
		is = PsiscoreServerProperties.class.getResourceAsStream("../../../../../../../psiscoreServer.properties");
		Properties properties = new Properties();
		try {
			properties.load(is);
		} catch (IOException e) {
			e.printStackTrace();
			//throw new PsiscoreException("Cannot load PSISCORE properties ", new PsiscoreFault(), e);
		}
		return properties;
    }
    
    
    public Properties getPropertiesFromURL(String urlString) throws InvalidArgumentException{
    	Properties properties = null;
		URL url = null;
		try {
			url = new URL(urlString);
			URLConnection con = url.openConnection();
			properties = new Properties();
			properties.load(con.getInputStream()) ;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InvalidArgumentException("Cannot load PSISCORE properties fils", new PsiscoreFault(), e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InvalidArgumentException("Cannot load PSISCORE properties fils", new PsiscoreFault(), e);
		} 
		return properties;
	}
    
    /*public static void main(String args[]){
    	System.out.println(PsiscoreServerProperties.getInstance().props);
    }*/
}
