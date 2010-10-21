package org.hupo.psi.mi.psiscore.wsclient.config;

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

public class PsiscoreClientProperties {
	
	private static PsiscoreClientProperties instance = new PsiscoreClientProperties();
	private Properties props = null;
	
	public static PsiscoreClientProperties getInstance(){
		return instance;
	}
	
	private PsiscoreClientProperties(){
		/*String path = getClass().getProtectionDomain().getCodeSource().
		   getLocation().toString().substring(6);*/
		props = loadProperties();
		boolean overwrite = Boolean.parseBoolean( (String) props.get("overwriteProperties") );
		if (props.containsKey("writeDir")){
			File file = new File(props.getProperty("writeDir"));
	    	if (!file.exists()){
	    		file.mkdir();
	    	}
	    	file = null;
		}
		if (props.containsKey("tempDir")){
			File file = new File(props.getProperty("tempDir"));
	    	if (!file.exists()){
	    		file.mkdir();
	    	}
	    	file = null;
		}
		
		System.out.println(props);
	}
	
	public Properties getProperties(){
		return props;
	}
	
	private Properties loadProperties(){
    	InputStream is = null;
    	// be careful if you move this method to another class as the relative path in the webapp might change
		is = PsiscoreClientProperties.class.getResourceAsStream("../../../../../../../psiscoreClient.properties");
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
    
    public static void main(String args[]){
    	System.out.println(PsiscoreClientProperties.getInstance().props);
    }
}
