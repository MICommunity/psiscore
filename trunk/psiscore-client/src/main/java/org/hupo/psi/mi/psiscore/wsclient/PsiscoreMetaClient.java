package org.hupo.psi.mi.psiscore.wsclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.hupo.psi.mi.psiscore.AlgorithmDescriptor;
import org.hupo.psi.mi.psiscore.JobResponse;
import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.JobStillRunningException;
import org.hupo.psi.mi.psiscore.PsiscoreException;
import org.hupo.psi.mi.psiscore.PsiscoreFault;
import org.hupo.psi.mi.psiscore.QueryResponse;
import org.hupo.psi.mi.psiscore.Report;
import org.hupo.psi.mi.psiscore.ResultSet;
import org.hupo.psi.mi.psiscore.model.PsiscoreInput;
import org.hupo.psi.mi.psiscore.util.PsiTools;
import org.hupo.psi.mi.psiscore.wsclient.SimplePsiscoreClient;
import org.hupo.psi.mi.psiscore.wsclient.PsiscoreClientException;

import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.tab.converter.xml2tab.TabConversionException;
import psidev.psi.mi.tab.converter.xml2tab.Xml2Tab;
import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.builder.MitabDocumentDefinition;
import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.xml.converter.ConverterException;

import psidev.psi.mi.xml.converter.impl254.EntryConverter;

import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;
import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.PsimiXmlReader;
import psidev.psi.mi.xml.PsimiXmlReaderException;


/**
 * Example on how to use PSISCORE servers. Each scoring server (unique URL) will
 * be handled by a client server instance. 
 * 
 * @author hagen (mpi-inf,mpg)
 */
public class PsiscoreMetaClient {
	private Map<String, SimplePsiscoreClient> psiscoreServerClients = null;
	private static Set<String> uniqueIds;
	
	
	/**
	 * Empty default constructor
	 */
	public PsiscoreMetaClient(){
		psiscoreServerClients = new HashMap<String,SimplePsiscoreClient>();
	}
	
	/**
	 * Constructor that add client instances for each server url 
	 * @param serverUrls
	 */
	public PsiscoreMetaClient(List<String> serverUrls){
		this();
		init(serverUrls);
	}
	
	/**
	 * Create and add an instance for each scoring server url
	 * @param clientUrls
	 */
	public void init(List<String> clientUrls){
		for (String url:clientUrls){
			createAndAddPsiscoreClient(url);
		}
	}
	
		
	/**
	 * Add a scoring server 
	 * @param client
	 * @return
	 */
	public String addPsiscoreClient(SimplePsiscoreClient client){
		System.out.println("\t\tAdding new PsiscoreClient: " + client.getId());
		this.psiscoreServerClients.put(client.getId(), client);
		return client.getId();
	}
	
	/**
	 * Create and add a client instance
	 * @param clientUrl
	 * @return
	 */
	public String createAndAddPsiscoreClient(String clientUrl){
		if (this.psiscoreServerClients.containsKey(clientUrl)){
			return clientUrl;
		}else{
			SimplePsiscoreClient client = new SimplePsiscoreClient(clientUrl);
			return addPsiscoreClient(client);
		}
	}
	
	/**
	 * Get all client instances that are managed
	 * @return
	 */
	public Map<String, SimplePsiscoreClient> getPsiscoreClients(){
		return this.psiscoreServerClients;
	}
	
		
	/**
	 * 
	 */
	public void clearPsiscoreClients(){
		psiscoreServerClients = new HashMap<String,SimplePsiscoreClient>();
	}


	/**
	 * Get the supported return types from all scoring servers 
	 * @return
	 * @throws PsiscoreClientException
	 */
	public List<String> getSupportedDataTypes() throws PsiscoreException{
	    List<String> types = new ArrayList<String>();
	    Iterator<Map.Entry<String, SimplePsiscoreClient>> it = psiscoreServerClients.entrySet().iterator();
	    while (it.hasNext()){
	    	Map.Entry<String, SimplePsiscoreClient> pair = it.next();
	    	types.addAll(pair.getValue().getSupportedDataTypes());
	    }
	    return types;
    }
	
    /**
     * get the scoring algorithms from each scoring servers
     * @return
     * @throws PsiscoreClientException
     */
	public Map<String, List<AlgorithmDescriptor>> getSupportedScoringMethodsMap() throws PsiscoreException{
		Map<String, List<AlgorithmDescriptor>> scoringMethods = new HashMap<String, List<AlgorithmDescriptor>>();
		Iterator<Map.Entry<String, SimplePsiscoreClient>> it = psiscoreServerClients.entrySet().iterator();
	    while (it.hasNext()){
	    	Map.Entry<String, SimplePsiscoreClient> pair = it.next();
	    	List<AlgorithmDescriptor> descriptions = new ArrayList<AlgorithmDescriptor>();
	    	descriptions.addAll(pair.getValue().getSupportedScoringMethods());
	    	scoringMethods.put(pair.getValue().getId(), descriptions);
	    }
        return scoringMethods;
	}

	
	  /**
     * get the scoring algorithms from each scoring servers
     * @return
     * @throws PsiscoreClientException
     */
	public List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException{
		List<AlgorithmDescriptor> descriptions = new ArrayList<AlgorithmDescriptor>();
		Iterator<Map.Entry<String, SimplePsiscoreClient>> it = psiscoreServerClients.entrySet().iterator();
	    while (it.hasNext()){
	    	Map.Entry<String, SimplePsiscoreClient> pair = it.next();
	    	descriptions.addAll(pair.getValue().getSupportedScoringMethods());
	    }
        return descriptions;
	}

	
	/**
	 * get the version form all scoring servers
	 * @param clientId
	 * @return
	 * @throws PsiscoreClientException
	 * @throws InvalidArgumentException
	 */
	public String getVersion(String clientId) throws PsiscoreException, InvalidArgumentException{
		if (!psiscoreServerClients.containsKey(clientId)){
			throw new InvalidArgumentException("There is no client with this ID", new PsiscoreFault());
		}
        String version = psiscoreServerClients.get(clientId).getVersion();
        //System.out.println("Version: " +  version);
        return version;
	}
	
	/**
	 * Get all jobs from fro the respective scoring servers
	 * @param ids
	 * @return
	 * @throws PsiscoreException
	 * @throws InvalidArgumentException
	 * @throws JobStillRunningException
	 * @throws PsiscoreClientException
	 */
	public QueryResponse getJobs(Map<String, JobResponse> ids) throws PsiscoreException, InvalidArgumentException, JobStillRunningException, PsiscoreClientException{
		ResultSet rs = null;
		QueryResponse response = null;
		Report report = null;
		PsiscoreInput input = null;
		Iterator<Map.Entry<String, JobResponse>> it = ids.entrySet().iterator();
		while (it.hasNext()){
			Map.Entry<String, JobResponse> pair = it.next();
			
			QueryResponse serverResponse = getJob(pair.getKey(), pair.getValue().getJobId());
			
			if (serverResponse == null){
				throw new JobStillRunningException("Server " + pair.getKey() + " has not finished scoring", new PsiscoreFault());
			}
			ResultSet newRs = serverResponse.getResultSet();
			if (input == null){
				input = PsiTools.getPsiscoreInput(newRs);
				report = serverResponse.getReport();
			}else{
				PsiTools.addConfidencesToPsiscoreInput(input, PsiTools.getPsiscoreInput(newRs));
				report.getResults().addAll(serverResponse.getReport().getResults());
			}
		}
		rs = PsiTools.getResultSet(input);
		response = new QueryResponse();
		response.setResultSet(rs);
		response.setReport(report);
		return response;
		
	}

	
	/**
	 * get the scoring job associated with that id. retry getting the job for a certain timespan
	 * @param id
	 * @param timeout how long should the client retry to get the results, seconds
	 * @param pollingInterval seconds between retrying
	 * @return
	 * @throws PsiscoreClientException
	 * @throws InvalidArgumentException
	 * @throws JobStillRunningException
	 */
	public QueryResponse getJob(String jobId, String serverId, int pollingInterval, long timeout) throws PsiscoreException, InvalidArgumentException, JobStillRunningException{
		
		QueryResponse queryResponse = null;
		
		if (timeout < 0 || timeout > 60){
			throw new InvalidArgumentException("You can only use timeouts between 0 and 60 seconds");
		}
		
		do{
			try {
				Iterator<Map.Entry<String, SimplePsiscoreClient>> it = psiscoreServerClients.entrySet().iterator();
			    while (it.hasNext()){
			    	Map.Entry<String, SimplePsiscoreClient> pair = it.next();
			    	SimplePsiscoreClient currentClient = pair.getValue();
			    	queryResponse = currentClient.getJob(jobId);
			    }
				// getjob didnt throw an error, thus we are done
				timeout = 0; 
			} catch (JobStillRunningException e) {
				try {
					Thread.sleep(pollingInterval * 1000);
				} catch (InterruptedException e1) {}
			}
			timeout = timeout - pollingInterval;
		} while (timeout > 0);
		
		return queryResponse;
	}
	
	/**
	 * Get a single job from a single server
	 * @param serverId
	 * @param jobId
	 * @return
	 * @throws PsiscoreException
	 * @throws InvalidArgumentException
	 * @throws JobStillRunningException
	 */
	public QueryResponse getJob(String serverId, String jobId) throws PsiscoreException, InvalidArgumentException, JobStillRunningException{
		
		QueryResponse queryResponse = null;
		
		if (!psiscoreServerClients.containsKey(serverId)){
			throw new InvalidArgumentException("There is no job with this id", new PsiscoreFault());
		}
		SimplePsiscoreClient client = psiscoreServerClients.get(serverId);
		queryResponse = client.getJob(jobId);
 
		return queryResponse;
	}
	
	
	
	
	/**
	 * Submit a scoring job to all servers
	 * @param inputData
	 * @param descriptors
	 * @return
	 * @throws PsiscoreClientException
	 * @throws PsiscoreException
	 * @throws InvalidArgumentException
	 */
	public Map<String, JobResponse> submitJob(ResultSet inputData, List<AlgorithmDescriptor> descriptors) throws PsiscoreClientException, PsiscoreException, InvalidArgumentException{
		Map<String, JobResponse> jobResponses = new HashMap<String, JobResponse>();
		Iterator<Map.Entry<String, SimplePsiscoreClient>> clientIterator = psiscoreServerClients.entrySet().iterator();
    	
	    while (clientIterator.hasNext()){
	    	
	    	Map.Entry<String, SimplePsiscoreClient> pair = clientIterator.next();
	    	SimplePsiscoreClient currentClient = pair.getValue();
	    	JobResponse response = currentClient.submitJob(descriptors, inputData, "psimi/tab25");
	    	jobResponses.put(currentClient.getId(), response);
	    }
	    
		return jobResponses;
	}
	
	
    
    /**
     * Check if the input is valid. returns true if a valid input was found, otherwise throws an exception
     * @param algorithmDescriptor
     * @param inputData
     * @param returnFormat
     * @throws PsiscoreClientException
     * @throws PsiscoreException
     * @throws InvalidArgumentException
     */
    public boolean validateInput(org.hupo.psi.mi.psiscore.ResultSet resultSet) throws PsiscoreClientException, PsiscoreException, InvalidArgumentException{
    	psidev.psi.mi.xml.model.EntrySet entrySet = null;
    	entrySet = PsiTools.getEntrySetFromInput(resultSet);
    	if (entrySet == null){
			throw new InvalidArgumentException("No valid input data (MITAB or PSIMI XML) detected", new PsiscoreFault());
		}
		return true;
    }
    
      
    /**
     * Generate and return an id unique for this server
     * @return
     */
    private String getUniqueId(){
		Random random = new Random();
    	String token = Long.toString(Math.abs(random.nextLong()), 36);
    	if (uniqueIds == null){
    		 uniqueIds= new HashSet<String>();
    	}
    	if (uniqueIds.contains(token)){
    		return getUniqueId();
    	}else{
    		uniqueIds.add(token);
    		return token;
    	}
	}
}



