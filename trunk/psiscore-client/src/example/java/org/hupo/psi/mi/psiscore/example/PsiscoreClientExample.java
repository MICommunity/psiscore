package org.hupo.psi.mi.psiscore.example;

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

import psidev.psi.mi.tab.model.BinaryInteraction;

/**
 * Example on how to use PSISCORE servers. Each scoring server (unique URL) will
 * be handled by a client server instance. 
 * 
 * @author hagen (mpi-inf,mpg)
 */
public class PsiscoreClientExample {
	

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
    	//serverUrls.add("http://mpiat3502.ag3.mpi-sb.mpg.de/psiscorews/webservices/psiscore");
    	//serverUrls.add("http://localhost:8080/psiscore-ws/webservices/psiscore"); 
    	serverUrls.add("http://localhost:8080/test/webservices/psiscore");
    	PsiscoreMetaClient clientExample = new PsiscoreMetaClient(serverUrls);
    	
    	// get all scoring algorithms from the server. this list will later also 
    	// be used to select the algorithms you want
    	System.out.println("getting a list of scoring algorithms:");
    	Map<String, List<AlgorithmDescriptor>> algorithms = clientExample.getSupportedScoringMethodsMap();
    	List<AlgorithmDescriptor> algorithmsSublist = new ArrayList<AlgorithmDescriptor>();
    	for (String server:algorithms.keySet()){
    		System.out.println("Server: "+server);
    		for (AlgorithmDescriptor algorithm:algorithms.get(server)){
        		System.out.println("\t"+algorithm.getId());
        		//if (algorithm.getId().equalsIgnoreCase("BPScore") || algorithm.getId().equalsIgnoreCase("Domain support, inferred")){
        			algorithmsSublist.add(algorithm);
        		//}
        		List<String> types = algorithm.getAlgorithmTypes();
        		for (String type: types){
        			System.out.println("\t\t"+type);
        		}
        	}
    	}
    	
    	// read the mitab
    	String mitabString = PsiTools.readFromUrl("http://www.mpi-inf.mpg.de/~hagen/psiscore/20368287_part.txt");
    	ResultSet inputData = new ResultSet();
    	inputData.setMitab(mitabString);
    	
    	// alternatively,
    	//psidev.psi.mi.xml254.jaxb.EntrySet set = PsiTools.readXmlEntrySetFromFile("D:/17220478.xml");
    	//inputData.setEntrySet(set);
    	
    	// make sure its a valid input
    	clientExample.validateInput(inputData);
    	// let's score the mitab, the response contains the time we should wait
    	Map<String,JobResponse> serverResponses = clientExample.submitJob(inputData, algorithmsSublist);

    	/*Iterator<Map.Entry<String, JobResponse>> jobResponseIt = serverResponses.entrySet().iterator();
    	// go through all responses
    	while (jobResponseIt.hasNext()){
    		Map.Entry<String, JobResponse> pair = jobResponseIt.next();
    		System.out.println("JobId on server " + pair.getKey() + ": " + pair.getValue().getJobId());
    		try {
    			Thread.sleep(( pair.getValue().getPollingInterval())*1000);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
    		QueryResponse data = clientExample.getJob(pair.getKey(), pair.getValue().getJobId());
    		if (data != null){
    			System.out.println("Results: " + data.getReport().getResults());
    			if (data.getResultSet().getEntrySet() == null){
	    			System.out.println("FINAL MITAB:");
	    			System.out.println(data.getResultSet().getMitab());
    			}else{
    				System.out.println("FINAL XML:");
	    			PsiTools.writeXmlEntrySetToFile(data.getResultSet().getEntrySet(), "D:/scoredXml.xml");
    			}
    		}else{
    			System.out.println("Scoring did not finish. ");
    		}
    	}*/
    	
    	// wait some time before we try to get the job
    	try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	QueryResponse response = clientExample.getJobs(serverResponses);
    	if (response != null){
    		System.out.println(response.getReport().getResults());
    		if (response.getResultSet().getEntrySet() == null){
    			System.out.println("FINAL MERGED MITAB:");
    			System.out.println(response.getResultSet().getMitab());
			}else{
				System.out.println("FINAL MERGED XML:");
    			PsiTools.writeXmlEntrySetToFile(response.getResultSet().getEntrySet(), "D:/scoredXml.xml");
			}
		}else{
			System.out.println("Scoring did not finish. ");
		}   
    }
  
}



