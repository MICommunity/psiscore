package org.hupo.psi.mi.psiscore.wsclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.hupo.psi.mi.psiscore.wsclient.config.PsiscoreClientProperties;


/**
 * Example on how to use PSISCORE servers. Each scoring server (unique URL) will
 * be handled by a client server instance. 
 * 
 * @author hagen (mpi-inf,mpg)
 */
public class PsiscoreMetaClient {
	protected Map<String, SimplePsiscoreClient> psiscoreServerClients = null;
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
	 * 
	 * @param registryUrl url to the command that retrieves the list of server 
	 * from the registry. must be the simple format, i.e. server=url
	 */
	public Set<String> addServersFromRegistry(String registryUrl) throws PsiscoreClientException{
		if (registryUrl == null || registryUrl.trim().length() == 0 || registryUrl.trim().equalsIgnoreCase("null")){
			registryUrl = PsiscoreClientProperties.getInstance().getProperties().getProperty("registryCommand");
		}
		//System.out.println("URL: " + registryUrl);
		URL registry = null;
		try {
			//System.out.println(registryUrl);
			registry = new URL(registryUrl);
			URLConnection yc = registry.openConnection();
	        BufferedReader in = new BufferedReader(
	                                new InputStreamReader(
	                                yc.getInputStream()));
	        
	        String inputLine;
	        String[] inputLineSplit = null;
	        while ((inputLine = in.readLine()) != null) {
	        	inputLineSplit = inputLine.split("=");
	        	if (inputLineSplit.length != 2){
	        		throw new PsiscoreClientException("There was a problem while contacting the registry ", new PsiscoreFault());
	        	}
	        	createAndAddPsiscoreClient(inputLineSplit[1]);
	        }
	        in.close();
	        
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new PsiscoreClientException("Unable to contact the PSISCORE registry", new PsiscoreFault(), e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new PsiscoreClientException("Unable to contact the PSISCORE registry", new PsiscoreFault(), e);
		} 
		return psiscoreServerClients.keySet();
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
		//System.out.println("GET JOBS");
		if (ids == null){
			throw new InvalidArgumentException("No jobs to retrieve", new PsiscoreFault());
		}
		ResultSet rs = null;
		QueryResponse response = null;
		Report report = null;
		PsiscoreInput input = null;
		Iterator<Map.Entry<String, JobResponse>> it = ids.entrySet().iterator();
		while (it.hasNext()){
			
			Map.Entry<String, JobResponse> pair = it.next();
			//System.out.println("JOB: " + pair.getKey() );
			QueryResponse serverResponse = null;
			try{
				serverResponse = getJob(pair.getKey(), pair.getValue().getJobId());
			}catch(InvalidArgumentException e){
				e.printStackTrace();
			}
			
			if (serverResponse == null){
				throw new JobStillRunningException("Server " + pair.getKey() + " has not finished scoring", new PsiscoreFault());
			}
			ResultSet newRs = serverResponse.getResultSet();
			PsiscoreInput scoredData =  PsiTools.getInstance().getPsiscoreInput(newRs);
			if (input == null){
				//System.out.println("new scoredData");
				input = scoredData;
				report = serverResponse.getReport();
			}else{
				
				//System.out.println("got scoredData");
				PsiTools.getInstance().addConfidencesToPsiscoreInput(input, scoredData);
				report.getResults().addAll(serverResponse.getReport().getResults());
			}
		}
		rs = PsiTools.getInstance().getResultSet(input);
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
	public Map<String, JobResponse> submitJobList(ResultSet inputData, List<AlgorithmDescriptor> descriptors, String returnFormat) throws PsiscoreClientException, PsiscoreException, InvalidArgumentException{
		Map<String, JobResponse> jobResponses = new HashMap<String, JobResponse>();
		Iterator<Map.Entry<String, SimplePsiscoreClient>> clientIterator = psiscoreServerClients.entrySet().iterator();
    	
	    while (clientIterator.hasNext()){
	    	
	    	Map.Entry<String, SimplePsiscoreClient> pair = clientIterator.next();
	    	SimplePsiscoreClient currentClient = pair.getValue();
	    	JobResponse response = currentClient.submitJob(descriptors, inputData, returnFormat);
	    	jobResponses.put(currentClient.getId(), response);
	    }
	    
		return jobResponses;
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
	public Map<String, JobResponse> submitJobMap(ResultSet inputData, Map<String, List<AlgorithmDescriptor>> descriptors, String returnFormat) throws PsiscoreClientException, PsiscoreException, InvalidArgumentException{
		Map<String, JobResponse> jobResponses = new HashMap<String, JobResponse>();
		Iterator<Map.Entry<String, SimplePsiscoreClient>> clientIterator = psiscoreServerClients.entrySet().iterator();
    			
	    while (clientIterator.hasNext()){
	    	Map.Entry<String, SimplePsiscoreClient> pair = clientIterator.next();
	    	SimplePsiscoreClient currentClient = pair.getValue();
	    	List<AlgorithmDescriptor> algorithms = descriptors.get(pair.getKey());
	    	// only submit the job to a server if there is an algorithm request from it
	    	if (algorithms != null){
	    		JobResponse response = currentClient.submitJob(algorithms, inputData, returnFormat);
	    		jobResponses.put(currentClient.getId(), response);
	    	}
	    	pair = null;
	    	currentClient = null;
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
    	entrySet = PsiTools.getInstance().getEntrySetFromInput(resultSet);
    	if (entrySet == null){
			throw new InvalidArgumentException("No valid input data (MITAB or PSIMI XML) detected", new PsiscoreFault());
		}
    	entrySet = null;
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



