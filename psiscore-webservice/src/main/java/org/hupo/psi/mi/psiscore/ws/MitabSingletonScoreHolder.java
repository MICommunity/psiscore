package org.hupo.psi.mi.psiscore.ws;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hupo.psi.mi.psiscore.PsiscoreException;
import org.hupo.psi.mi.psiscore.PsiscoreFault;
import org.hupo.psi.mi.psiscore.ws.config.PsiscoreServerProperties;

import psidev.psi.mi.xml.converter.ConverterException;

public class MitabSingletonScoreHolder {
	private Properties properties = PsiscoreServerProperties.getInstance().getProperties();
	private String path = properties.getProperty("org.hupo.psi.mi.psiscore.ws.MitabScoreCalculator.mitab");
	private boolean zippedInput = Boolean.parseBoolean(properties.getProperty("org.hupo.psi.mi.psiscore.ws.MitabScoreCalculator.zippedInput"));
	
    private static MitabSingletonScoreHolder instance = new MitabSingletonScoreHolder();
    private Map<String, String> scores = null;
    
    private MitabSingletonScoreHolder(){
    	System.out.println("loading " + path);
    	String tempPath = null;
    	String url = path;
    	if (zippedInput){
    		System.out.println("Zipped input, need to extract first");
    		tempPath = downloadAndExtractFile(path);
    		System.out.println("written to " + tempPath);
    		// convert the path to an url representation
			try {
				url = new File(tempPath).toURL().toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
		try {
			scores = readMitabScoringFile(url);
			// delete the temp file after the scores have been loaded
			if(zippedInput){
				new File(tempPath).delete();
			}
		} catch (PsiscoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		tempPath = null;
    }

    public static MitabSingletonScoreHolder getInstance() {
        return instance;
    }
    
    public Map<String, String> getScores(){
    	return scores;
    }
    
    /**

	 * @return
	 * @throws PsiscoreException 
     * @throws ConverterException 
	 */
	 private Map<String, String> readMitabScoringFile(String path) throws PsiscoreException {
        Map<String, String> scores = new HashMap<String, String>();
        int i = 0;
        try {
            URL url = new URL(path);
            InputStream in = url.openStream();
            BufferedReader dis = new BufferedReader(new InputStreamReader(in));
            String line = dis.readLine();
            
            while ((line = dis.readLine()) != null) {
            	
            	if (++i%10000 == 0){
            		System.out.println("mitab line " + i);
            	}
            	/*if (i > 1000){
            		break;
            	}*/
            	String[] temp = line.split("\t");
                String[] idsA = temp[0].split("\\|");
                String[] idsB = temp[1].split("\\|");
                
                for (String idPairA:idsA){
                	String[] idA = idPairA.split("\\:");
                	
                	for (String idPairB:idsB){
                    	String[] idB = idPairB.split("\\:");
                    	if (idA[0].equals("uniprotkb") && idB[0].equals("uniprotkb")){
                    		if (idA[1].compareTo(idB[1]) < 0){
                				scores.put(new String(idA[1] + idB[1]), new String(temp[14]));
                				//System.out.println(idA[1] + idB[1] + temp[14]);
                			}else{
                				scores.put(new String(idB[1] + idA[1]), new String(temp[14]));
                				//System.out.println(idB[1] + idA[1] + temp[14]);
                			}

                    	}
                    	idB = null;
                    }
                	idA = null;
                }
                temp = null;
                idsA = null;
                idsB = null;

                
            }
            in.close();
        } catch (IOException e) {
            throw new PsiscoreException("File not found", new PsiscoreFault(), e);
        } 

        return scores;
    }
	 
	 public String downloadAndExtractFile(String pathAsUrl){
	    	String dir = properties.getProperty("tempDir");
	    	String outputFile = null;
	    	try {
				URL url = new URL(pathAsUrl);
				ZipInputStream in = new ZipInputStream(url.openStream());
				BufferedOutputStream out = null;
			    ZipEntry entry;
			    while((entry = in.getNextEntry()) != null){
			    	int count;
			    	byte data[] = new byte[1024];
			    	out = new BufferedOutputStream(new FileOutputStream(dir + entry.getName()), 1024);
			    	outputFile = dir + entry.getName();
			    	while ((count = in.read(data,0,1024)) != -1){
		                 out.write(data,0,count);
		            }
			    	
		            out.flush();
		            out.close();
				     
				    in.closeEntry();
			    }

			    in.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// convert the path to an url representation
			try {
				outputFile = new File(outputFile).toURL().toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return outputFile;
	    }
}
