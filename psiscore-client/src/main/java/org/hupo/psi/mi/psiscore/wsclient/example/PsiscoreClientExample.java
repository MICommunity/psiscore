package org.hupo.psi.mi.psiscore.wsclient.example;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hupo.psi.mi.psiscore.AlgorithmDescriptor;
import org.hupo.psi.mi.psiscore.JobResponse;
import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.JobStillRunningException;
import org.hupo.psi.mi.psiscore.PsiscoreException;
import org.hupo.psi.mi.psiscore.QueryResponse;
import org.hupo.psi.mi.psiscore.ResultSet;


import org.hupo.psi.mi.psiscore.util.PsiTools;
import org.hupo.psi.mi.psiscore.wsclient.PsiscoreMetaClient;
import org.hupo.psi.mi.psiscore.wsclient.PsiscoreClientException;




/**
 * Example on how to use PSISCORE servers. Each scoring server (unique URL) will
 * be handled by a client server instance. 
 * 
 * @author hagen (mpi-inf,mpg)
 */
public class PsiscoreClientExample {
	public static PsiscoreMetaClient client = null;
	public static Map<String, List<AlgorithmDescriptor>> algorithms = null; 
	public static Map<String,JobResponse> lastServerResponses = null;
	
	public PsiscoreClientExample(List<String> serverUrls){
		
    	client = new PsiscoreMetaClient(serverUrls);
    	
	}
	
	public void getAvailableScoringMethods(){
		// get all scoring algorithms from the server. this list will later also 
    	// be used to select the algorithms you want
    	System.out.println("getting a list of scoring algorithms:");
    	Map<String, List<AlgorithmDescriptor>> algorithms = null;
		try {
			algorithms = client.getSupportedScoringMethodsMap();
		} catch (PsiscoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
    	for (String server:algorithms.keySet()){
    		System.out.println("Server: "+server);
    		for (AlgorithmDescriptor algorithm:algorithms.get(server)){
        		System.out.println("\t"+algorithm.getId());
        		//List<String> types = algorithm.getAlgorithmTypes();
        		//for (String type: types){
        		//	System.out.println("\t\t"+type);
        		//}
        	}
    	}
    	this.algorithms = algorithms;
	}
	
	public  void scoreFilesInDirectory(String pathToDir, int numberOfFiles, int numberOfRuns){
			
    	Map<String,JobResponse> serverResponses = null;
    	if (algorithms == null){
    		getAvailableScoringMethods(); 
    	}
		File dir = new File(pathToDir);
		String[] children = dir.list(); 

		int total = 0;
    	if (children != null) {
    		for (int x = 0; x < numberOfRuns; x++){
				for (int i = 0; ( i < children.length && i < numberOfFiles); i++) {
				
					// Get filename of file or directory 
					String filename = children[i];
					System.out.print(total++ +" ("+i+"): "+filename);
					
					String mitabString = null;
					try {
						File tempFile = new File(dir+"/"+filename);
						String urlString = tempFile.toURL().toString();
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						mitabString = PsiTools.getInstance().readFromUrl(urlString);
						filename = null;
						tempFile = null;
						urlString = null;
						ResultSet inputData = new ResultSet();
		    	    	inputData.setMitab(mitabString);
		    	    	
		    	    	// alternatively,
		    	    	//psidev.psi.mi.xml254.jaxb.EntrySet set = PsiTools.readXmlEntrySetFromFile("D:/17220478.xml");
		    	    	//inputData.setEntrySet(set);
		    	    	
		    	    	// make sure its a valid input
		    	    	client.validateInput(inputData);
		    	    	serverResponses = client.submitJobMap(inputData, algorithms, PsiTools.RETURN_TYPE_MITAB25);
		    	    	inputData = null;
		    	    	mitabString = null;
		    	    	
		    	    	Iterator<Map.Entry<String, JobResponse>> jobResponseIt = serverResponses.entrySet().iterator();
		    	    	// go through all responses
		    	    	while (jobResponseIt.hasNext()){
		    	    		Map.Entry<String, JobResponse> pair = jobResponseIt.next();
		    	    		System.out.println(" jobid: " + pair.getValue().getJobId());
		    	    		pair = null;
		    	    	}
		    	    	jobResponseIt = null;
		    	    	//serverResponses = null;
		    	    	
						//System.out.println(mitabString);
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.exit(1);
					} catch (PsiscoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.exit(1);
					} catch (InvalidArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.exit(1);
					} catch (PsiscoreClientException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.exit(1);
					}

				}
    		} 
    	} 
    	this.lastServerResponses = serverResponses;
	}
	
	public  void scoreFile(String pathToFile, String type){
		
    	Map<String,JobResponse> serverResponses = null;
    	if (algorithms == null){
    		getAvailableScoringMethods(); 
    	}
					
		String mitabString = null;
		ResultSet inputData = new ResultSet();
		try {
			if (type.equalsIgnoreCase(PsiTools.RETURN_TYPE_MITAB25)){
				File tempFile = new File(pathToFile);
				String urlString = tempFile.toURL().toString();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mitabString = PsiTools.getInstance().readFromUrl(urlString);
				tempFile = null;
				urlString = null;
				inputData.setMitab(mitabString);
			}else if (type.equalsIgnoreCase(PsiTools.RETURN_TYPE_XML25)){
				psidev.psi.mi.xml254.jaxb.EntrySet set = PsiTools.getInstance().readXmlEntrySetFromFile(pathToFile);
				inputData.setEntrySet(set);
			}
	    	
	    	// make sure its a valid input
	    	client.validateInput(inputData);
	    	serverResponses = client.submitJobMap(inputData, algorithms, type);
	    	inputData = null;
	    	mitabString = null;
	    	
	    	Iterator<Map.Entry<String, JobResponse>> jobResponseIt = serverResponses.entrySet().iterator();
	    	// go through all responses
	    	while (jobResponseIt.hasNext()){
	    		Map.Entry<String, JobResponse> pair = jobResponseIt.next();
	    		System.out.println(" jobid: " + pair.getValue().getJobId());
	    		pair = null;
	    	}
	    	jobResponseIt = null;
	    	//serverResponses = null;
	    	
			//System.out.println(mitabString);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (PsiscoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (PsiscoreClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

    	this.lastServerResponses = serverResponses;
	}
	
	
	public void retrieveScoringJob(long sleepBefore){
		try {
			Thread.sleep(sleepBefore);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		QueryResponse response;
		try {
			response = client.getJobs(lastServerResponses);
			if (response != null){
	    		
	    		if (response.getResultSet().getEntrySet() == null){
	    			System.out.println("FINAL MERGED MITAB:");
	    			System.out.println(response.getResultSet().getMitab());
				}else{
					System.out.println("FINAL MERGED XML:");
	    			PsiTools.getInstance().writeXmlEntrySetToFile(response.getResultSet().getEntrySet(), "D:/scoredXml.xml");
				}
			}else{
				System.out.println("Scoring did not finish. ");
			} 
		} catch (PsiscoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (JobStillRunningException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (PsiscoreClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
    	
	}

	/**
	 * Test the whole bunch
	 * @param args
	 * @throws PsiscoreException
	 * @throws PsiscoreClientException
	 * @throws InvalidArgumentException
	 * @throws JobStillRunningException 
	 */
    public static void main(String[] args) throws PsiscoreException, PsiscoreClientException, InvalidArgumentException, JobStillRunningException{
    	List<String> serverUrls = new ArrayList<String>();
    	serverUrls.add("http://mpiat3502.ag3.mpi-sb.mpg.de/psiscorews/webservices/psiscore");
    	serverUrls.add("http://localhost:8080/cgi-bin/webservices/psiscore/");
    	//serverUrls.add("http://localhost:8080/psiscore-ws-0.9.7-mint/webservices/psiscore/");
    	//serverUrls.add("http://localhost:8080/psiscore-ws-0.9.6/webservices/psiscore/");
    	//serverUrls.add("http://localhost:8080/client/webservices/psiscore/");
    	//serverUrls.add("http://biotin.uio.no:8081/psiscore-ws/webservices/psiscore");
    	//serverUrls.add("http://psiscore.bioinf.mpi-inf.mpg.de/psiscorews/webservices/psiscore/");
    	serverUrls.add("http://mint.bio.uniroma2.it/psiscore-ws-0.9.7-SNAPSHOT/webservices/psiscore/");
    	PsiscoreClientExample example = new PsiscoreClientExample(serverUrls);
    	
    	example.getAvailableScoringMethods();
    	//example.scoreFilesInDirectory("D:/2008/", 40, 1);
    	//example.scoreFile("D:/2008/10026281.txt");
    	example.scoreFile("D:/mint_test.txt", PsiTools.RETURN_TYPE_MITAB25);
    	//example.scoreFile("D:/15557335.psi25.xml", PsiTools.RETURN_TYPE_XML25);
    	
    	example.retrieveScoringJob(5000);

    	
    	
    }
  
}



