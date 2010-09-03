package org.hupo.psi.mi.psiscore.ws;

/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.hupo.psi.mi.psiscore.*;
import org.hupo.psi.mi.psiscore.model.PsiscoreInput;
import org.hupo.psi.mi.psiscore.util.PsiTools;
import org.hupo.psi.mi.psiscore.config.Constants;
import org.hupo.psi.mi.psiscore.ws.config.PsiscoreConfig;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * default scoring service implementation. 
 * provides basic threading functionality to host multiple scoring services.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author hagen (mpi-inf,mpg)
 */
@Controller
public class DefaultPsiscoreService implements PsiscoreService {

	// in this array you specify all the calculators the server will later manage.
	// for this, an instance of each class will be created to handle the associated
	// requests
	// TODO move into a nicer place such as a config file
    private String[] calculatorClasses = {
    		"org.hupo.psi.mi.psiscore.ws.ExampleScoreCalculator",
    		"org.hupo.psi.mi.psiscore.ws.YetAnotherExampleScoreCalculator"
    		};
	
    private static final int THREAD_POOL_SIZE = 1; // how many scoring threads can run in parallel
	private static final int NO_POLLING_NEEDED = 1; // 1 second = come back instantly
	private static final int POLLING_INTERVAL = 5; // suggested polling interval for server, seconds
	
	
	
    @Autowired
    private PsiscoreConfig config;
    
    private Set<AbstractScoreCalculator> calculators = null;
    private HashMap<String, Report> allReports = null;
    private HashMap<String, PsiscoreInput> allInputData = null;
    private ExecutorService threadPool = null;
    private Map<String, Set<Future>> threadFutures = null;
    private static Set<String> uniqueIds = null;;
    private int activeThreads = 0;

    /**
     *  Init all global variables
     * @throws PsiscoreException 
     */
    public DefaultPsiscoreService() throws PsiscoreException{
    	super();
    	
    	calculators = new HashSet<AbstractScoreCalculator>();
    	
    	for (int i = 0; i < calculatorClasses.length; i++){
    		try {
    			System.out.println("\tInitializing score calculator: " + calculatorClasses[i]);
    			AbstractScoreCalculator calc = (AbstractScoreCalculator)( Class.forName( calculatorClasses[i] ).newInstance() );
    			calculators.add(calc);
    		} catch (InstantiationException e) {
    			throw new PsiscoreException("Cannot instantiate scoring calculator " + calculatorClasses[i], new PsiscoreFault(), e);
    		} catch (IllegalAccessException e) {
    			throw new PsiscoreException("Cannot instantiate scoring calculator " + calculatorClasses[i], new PsiscoreFault(), e);
    		} catch (ClassNotFoundException e) {
    			throw new PsiscoreException("Cannot instantiate scoring calculator " + calculatorClasses[i], new PsiscoreFault(), e);
    		}
    	}
    	initDataStorage(); // where to kept the incoming data
    	initThreadPool(); // how to manage scoring
   	
    }
    
    

    /**
     * Request the server to score the interactions specified in the data section of the request with the algorithms provided 
     */
    public JobResponse submitJob( List<AlgorithmDescriptor> algorithmDescriptions, ResultSet inputData, String returnFormat) throws PsiscoreException, InvalidArgumentException{
    	//prepare the user response that will contain the job id and the polling interval
		JobResponse response = new JobResponse();
		String jobId = getUniqueId();
		response.setJobId(jobId);
		
    	// check which calculators we actually need to fulfill all requests	 within the algorithm descriptions	
		Map<Class, List<AlgorithmDescriptor>> requiredCalculators =  getRequiredCalculators(algorithmDescriptions);
		// get the input
		PsiscoreInput input = PsiTools.getPsiscoreInput(inputData);
		input.setPsiscoreId(jobId);
		// and store it to later compare with the scoring results
		storeInputData(jobId, input);
		storeReport(jobId, new Report());
		
		// start a scoring thread for each calculator
		for (Iterator<Class> it = requiredCalculators.keySet().iterator(); it.hasNext();){
			Class calculatorClass = it.next();
			
			ScoringParameters parameters = new ScoringParameters();
			parameters.setAlgorithmDescriptions(requiredCalculators.get(calculatorClass));
			parameters.setInputData(input);
			parameters.setReturnFormat(returnFormat);
			parameters.setJobID(jobId); 
			// instanciate a new calculator object
			AbstractScoreCalculator calculator = null;
			try {
				calculator = (AbstractScoreCalculator)calculatorClass.newInstance();
			} catch (InstantiationException e) {
				throw new PsiscoreException("Cannot instantiate scoring calculator ", new PsiscoreFault(), e);
			} catch (IllegalAccessException e) {
				throw new PsiscoreException("Cannot instantiate scoring calculator ", new PsiscoreFault(), e);
			}
			// at the listners and submit the job to the pool , where it will be started
			ScoringListener listener = new PsiscoreServiceListener();
			calculator.addScoringListener(listener);
			calculator.setName(jobId);
			calculator.setScoringParameters(parameters);
			submitThreadToPool(calculator);
		}
		// to tough part is estimating the correct polling interval for the server 
		response.setPollingInterval(estimatePollingInterval(input, algorithmDescriptions));
		
		return response;
	}
    
    /**
	 * Simple method for getting a standard polling interval.
	 * should be overriden by user scoring servers
	 * @param entrySet
	 * @return polling interval in seconds
	 */
	private int estimatePollingInterval(PsiscoreInput input, List<AlgorithmDescriptor> algorithmDescriptions){
		//todo replace with more realistic estimation, e.g. based on entrySet legnth and number of requested algorithms 
		if (this.activeThreads >= THREAD_POOL_SIZE){
			return POLLING_INTERVAL;
		}else{
			return NO_POLLING_NEEDED;
		}
	}
    
	
    /**
     * Initialize the data storage. If data should be kept in a database, this is
     * where the conenction could be made or if data is kept in memory, this is 
     * where the objects could be created.
     */
    private void initDataStorage(){
    	this.allReports = new HashMap<String, Report>();
    	this.allInputData = new HashMap<String, PsiscoreInput>();
    }
    

    /**
     * Store the input data in a database, in local memory, etc ...
     * @param jobId
     * @param input
     */
    private void storeInputData(String jobId, PsiscoreInput input){
    	allInputData.put(jobId, input);
    }
    
    
    private void storeReport(String jobId, Report report){
    	allReports.put(jobId, report);
    	
    }
    
    

    /**
     * Get the input data from the storage (DB, memory etc).
     * @param jobId
     * @return
     * @throws InvalidArgumentException
     */
    private PsiscoreInput getInteractionData(String jobId) throws InvalidArgumentException{
    	if (!this.allInputData.containsKey(jobId)){
			throw new InvalidArgumentException("There is no job (input) with this id", new PsiscoreFault());
		}
		return this.allInputData.get(jobId);
    }
    
    private Report getReport(String jobId)  throws InvalidArgumentException{
    	if (!this.allReports.containsKey(jobId)){
    		throw new InvalidArgumentException("There are no reports associated with this id", new PsiscoreFault());
    	}
    	return this.allReports.get(jobId);
    }
    
    
	/**
     * Get version of the scoring service
     */
    public String getVersion() throws PsiscoreException {
        return config.getVersion();
    }
    
    
    /**
     * Return all scoring algorithms available on the server 
     */
    public List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException {
    	List<AlgorithmDescriptor> descriptorList = new ArrayList<AlgorithmDescriptor>();
    	for (Iterator<AbstractScoreCalculator> it = calculators.iterator(); it.hasNext();){
    		AbstractScoreCalculator calc = it.next();
   			descriptorList.addAll(calc.getSupportedScoringMethods());
    	}
    	return descriptorList;
    }
    
    
    
    /**
     * Return all data types the server supports
     */
    public List<String> getSupportedDataTypes() throws PsiscoreException {
    	List<String> supportedReturnTypes = new ArrayList<String>();
    	
    	supportedReturnTypes.add(Constants.RETURN_TYPE_MITAB25);
    	supportedReturnTypes.add(Constants.RETURN_TYPE_XML25);
    	return supportedReturnTypes;
    }

    
    /**
     * Return the scoring calculators that are needed to fulfll the requested algorithm descriptions
     * @param requestedAlgorithms
     * @return
     * @throws PsiscoreException
     */
    private Map<Class, List<AlgorithmDescriptor>> getRequiredCalculators(List<AlgorithmDescriptor> requestedAlgorithms) throws PsiscoreException{
    	Map<Class, List<AlgorithmDescriptor>> requiredCalculators = new HashMap<Class, List<AlgorithmDescriptor>>();
    	
    	for (Iterator<AlgorithmDescriptor> algIt = requestedAlgorithms.iterator(); algIt.hasNext();){
    		AlgorithmDescriptor requestedAlgorithm = algIt.next();
    		for (Iterator<AbstractScoreCalculator> calcIt = calculators.iterator(); calcIt.hasNext();){
	    		AbstractScoreCalculator calculator = calcIt.next();
	    		List<AlgorithmDescriptor> calculatorAlgorithms = calculator.getSupportedScoringMethods();
	    		for (Iterator<AlgorithmDescriptor> algIt2 = calculatorAlgorithms.iterator(); algIt2.hasNext();){
	    			AlgorithmDescriptor calculatorAlgorithm = algIt2.next();
	    			if (calculatorAlgorithm.getId() == null || requestedAlgorithm.getId() == null){
	    				throw new PsiscoreException("There is a problem with the algo descriptions", new PsiscoreFault());
	    			}
	    			if (calculatorAlgorithm.getId().equalsIgnoreCase(requestedAlgorithm.getId())){
	    				List<AlgorithmDescriptor> requestedAlgorithmsCalculator = null;
	    				if (requiredCalculators.get(calculator.getClass()) == null){
	    					requestedAlgorithmsCalculator= new ArrayList<AlgorithmDescriptor>();
	    				}else{
	    					requestedAlgorithmsCalculator = requiredCalculators.get(calculator.getClass());
	    				}
	    				requestedAlgorithmsCalculator.add(requestedAlgorithm);
	    				requiredCalculators.put(calculator.getClass(), requestedAlgorithmsCalculator);
	    			}
	    		}
    		}
    	}
    	return requiredCalculators;
    }
    
    
    /**
     * get the job associated with the jobId from the server. Will throw exceptions if the job
     * does not exist or if the scoring is still in progress.
     */
	public QueryResponse getJob(String jobId) throws PsiscoreException, InvalidArgumentException, JobStillRunningException {
		QueryResponse response = new QueryResponse();
		
		if (getJobStatus(jobId).equalsIgnoreCase(Constants.MESSAGE_JOB_FINISHED)){
			PsiscoreInput input = getInteractionData(jobId);
			response.setResultSet(PsiTools.getResultSet(input));
			response.setReport(getReport(jobId));
		
		}else{
			throw new JobStillRunningException("The job has not yet finished please try again later", new PsiscoreFault());
		}
		
		return response;
	}
	
	/**
	 * Get the status of the scoring job. Will thrown an exception if the jobId does not exist.
	 */
	public String getJobStatus(String jobID) throws PsiscoreException, InvalidArgumentException {
		String jobStatus = null;
		Set<Future> futures = this.threadFutures.get(jobID);
		
		if (futures == null){
			throw new InvalidArgumentException("The server does not have any active scoring jobs", new PsiscoreFault());
		}

		// check all futures to see if the individual scoring job have finished
		boolean allDone = true;
		boolean errorOccured = false;
		for (Iterator<Future> it = futures.iterator(); it.hasNext();){
			Future future = it.next();
			if (!future.isDone()){
				allDone = false;
			}
			if (future.isCancelled()){
				errorOccured = true;
			}
		}
		if (allDone & !errorOccured){
			jobStatus = Constants.MESSAGE_JOB_FINISHED;
		}else if (allDone & errorOccured){
			jobStatus = Constants.MESSAGE_JOB_ERROR;
		}
		else{
			jobStatus = Constants.MESSAGE_JOB_RUNNING;
		}
		
		return jobStatus;
	}

	
    
	/**
	 * Generate and store a random unique identifiers
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

	
	 /**
     * Clean up the mess
     */
    public void finalize(){
    	this.threadPool.shutdownNow();
    	
    	if (calculators != null){
    		for (Iterator<AbstractScoreCalculator> it = calculators.iterator(); it.hasNext();){
        		AbstractScoreCalculator calculator = it.next();
        		calculator = null;
    		}
    	}
    	calculators = null;
    	try {
			super.finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	/**
	 * Start the thread pool that will manage scoring server access. 
	 */
	private void initThreadPool(){
		if (this.threadPool != null){
			this.threadPool.shutdownNow();
		}
		this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		//this.threadPool = Executors.newSingleThreadExecutor();
		this.threadFutures = new HashMap<String, Set<Future>>();
	}
    
	
	/**
	 * Add a scoring thread to the pool and thereby start it
	 * @param thread
	 */
	private synchronized void submitThreadToPool(Thread thread){
		Future future = future = threadPool.submit(thread);
		Set<Future> futures = null;
		if (!this.threadFutures.containsKey(thread.getName())){
			futures = new HashSet<Future>();
		}else{
			futures = this.threadFutures.get(thread.getName());
		}
		futures.add(future);
		this.threadFutures.put(thread.getName(), futures);
		this.activeThreads++;
	}
	

	/**
	 * Listener class handling the outcome of scoring jobs
	 * @author hagen
	 *
	 */
	private class PsiscoreServiceListener implements ScoringListener{

		/**
		 * not yet supported
		 */
		public synchronized void comeBackLater() {
			activeThreads--;
		}

		/**
		 * do something meaningful with the error that happened
		 */
		public synchronized void errorOccured(ScoringParameters parameters){
			activeThreads--;
			Report report = null; 
			try {
				report = getReport(parameters.getJobID());
				report.getResults().addAll(parameters.getScoringReport().getResults());
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}

		}

		/**
		 * no scores have been found
		 */
		public synchronized void noScoresAdded(ScoringParameters parameters) throws PsiscoreException{
			activeThreads--;
			Report report = null; 
			try {
				report = getReport(parameters.getJobID());
			} catch (InvalidArgumentException e) {
				throw new PsiscoreException("The job with this ID cannot be found on the server anymore.", new PsiscoreFault());
			}
			report.getResults().addAll(parameters.getScoringReport().getResults());
		}

		
		/**
		 * new scores added, add the to complete results.
		 * @throws PsiscoreException 
		 * @throws InvalidArgumentException 
		 */
		public synchronized void scoresAdded(ScoringParameters parameters) throws PsiscoreException{
			activeThreads--;
			PsiscoreInput inputData = null;
			Report report = null; 
			try {
				inputData = getInteractionData(parameters.getJobID());
				report = getReport(parameters.getJobID());
			} catch (InvalidArgumentException e) {
				throw new PsiscoreException("The job with this ID cannot be found on the server anymore.", new PsiscoreFault());
				
			}
			report.getResults().addAll(parameters.getScoringReport().getResults());
			PsiTools.addConfidencesToPsiscoreInput(inputData, parameters.getInputData());
			storeInputData(parameters.getJobID(), inputData);

		}
	}
}

