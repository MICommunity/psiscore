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
import org.hupo.psi.mi.psiscore.ws.config.PsiscoreServerProperties;
import org.hupo.psi.mi.psiscore.ws.config.PsiscoreConfig;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.Confidence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

	
	private Properties properties = PsiscoreServerProperties.getInstance().getProperties();

	// stuff to be read from the properties file
	private String[] calculatorClasses = null;
    private int threadPoolSize ; 
	private int pollingIntervalNoWaiting = Constants.pollingIntervalNoWaiting; 
	private int pollingInterval ; 
	private int numberOfRunningJobsBeforeCleanup;
	private int serverBreakIfOverloaded;
	private int maxRunningJobs;
	
    public int count = 0;
    
    @Autowired
    private PsiscoreConfig config;
    
    private Set<AbstractScoreCalculator> calculators = null;
    
    private ExecutorService threadPool = null;
    private Map<String, Set<Future>> threadFutures = null;
    private Set<String> uniqueIds = null;
    private Set<String> finishedJobs = null;
    private Set<String> runningJobs = null;

    private long lastTimestamp = 0;

    /**
     *  Init all global variables
     * @throws PsiscoreException 
     */
    public DefaultPsiscoreService() throws PsiscoreException{
    	super();
    	try {
	    	threadPoolSize = Integer.parseInt(properties.getProperty("threadPoolSize"));
	    	pollingInterval = Integer.parseInt(properties.getProperty("pollingInterval"));
	    	numberOfRunningJobsBeforeCleanup = Integer.parseInt(properties.getProperty("numberOfRunningJobsBeforeCleanup"));
	    	String tempCalculatorClasses = properties.getProperty("scoreCalculatorClasses");
	    	maxRunningJobs = Integer.parseInt(properties.getProperty("maxRunningJobs"));
	    	serverBreakIfOverloaded = Integer.parseInt(properties.getProperty("serverBreakIfOverloaded"));
	    	
	    	calculatorClasses = tempCalculatorClasses.split(";");
    	}catch(Exception e){
    		throw new PsiscoreException("Error accessing crucial parameters from proteries file", new PsiscoreFault(), e);
    	}
    	
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
    	
    	initThreadPool(); // how to manage scoring
    	// request temp file such that potentially long running directory
    	// clearing can already be started
    	TempFileDataStorage.getInstance();
   	
    }
    
    
    

    /**
     * Request the server to score the interactions specified in the data section of the request with the algorithms provided 
     */
    public JobResponse submitJob( List<AlgorithmDescriptor> algorithmDescriptions, ResultSet inputData, String returnFormat) throws PsiscoreException, InvalidArgumentException{
    	// if we have too many active jobs check if some of them
    	// have already finished such that we can get rid of their
    	// futures objects
		cleanUpRunningJobs();
    	if (runningJobs.size() > numberOfRunningJobsBeforeCleanup){
    		System.out.println("have: " + runningJobs.size() + " jobs, total ids: " + uniqueIds.size() );
    		try {
    			Thread.sleep(serverBreakIfOverloaded);
    		} catch (InterruptedException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
    	
    	if (runningJobs.size() > maxRunningJobs){
    		throw new PsiscoreException("More than " + maxRunningJobs + " active jobs are on the server. Will not accept now jobs, please come back later.", new PsiscoreFault());
    	}
    	
		String jobId = getUniqueId();

		// check which calculators we actually need to fulfill all requests	 within the algorithm descriptions	
		Map<Class, List<AlgorithmDescriptor>> requiredCalculators =  getRequiredCalculators(algorithmDescriptions);
		// get the input
		PsiscoreInput input = PsiTools.getInstance().getPsiscoreInput(inputData);
		
		input.setPsiscoreId(jobId);
		// and store it to later compare with the scoring results
		TempFileDataStorage.getInstance().storeInputData(jobId, input);
		input = null;
		Report rep = new Report();
		TempFileDataStorage.getInstance().storeReport(jobId, rep);
		rep = null;
		// start a scoring thread for each calculator
		for (Iterator<Class> it = requiredCalculators.keySet().iterator(); it.hasNext();){
			Class calculatorClass = it.next();
			input = TempFileDataStorage.getInstance().getInputData(jobId);
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
			}finally{
				calculatorClass = null;
			}
			// add the listners and submit the job to the pool , where it will be started
			ScoringListener listener = new PsiscoreServiceListener();
			calculator.addScoringListener(listener);
			calculator.setName(jobId);
			calculator.setScoringParameters(parameters);
			submitThreadToPool(calculator);
		}
		// what we"ll tell the client about the job
		JobResponse response = new JobResponse();
		response.setJobId(jobId);

		response.setPollingInterval(estimatePollingInterval(input, algorithmDescriptions));
		
		input = null;
		requiredCalculators = null;
		return response;
	}
    
    /**
	 * Simple method for getting a standard polling interval.
	 * should be overriden by user scoring servers
	 * @param entrySet
	 * @return polling interval in seconds
	 */
	private synchronized int estimatePollingInterval(PsiscoreInput input, List<AlgorithmDescriptor> algorithmDescriptions){
		//todo replace with more realistic estimation, e.g. based on entrySet legnth and number of requested algorithms 
		if (runningJobs.size() >= threadPoolSize){
			return pollingInterval;
		}else{
			return pollingIntervalNoWaiting;
		}
	}
    
	public void cleanUpRunningJobs(){
		//System.out.print("Running before: " + runningJobs.size());
		try{
			Set<String> runningCopy = new HashSet<String>(runningJobs);
			for (String jobId : runningCopy){
				try {
					getJobStatus(jobId);
				} catch (PsiscoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				jobId = null;
			}
			runningCopy = null;
		}catch(ConcurrentModificationException e){
			System.out.println("Concurrent modification, better luck next time");
		}
		//System.out.println(", after: " + runningJobs.size() + ", total ids: " + uniqueIds.size() );
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
    		if (requestedAlgorithm == null){
    			throw new PsiscoreException("There is a problem with the algo descriptions " , new PsiscoreFault());
    		}
    		
    		for (Iterator<AbstractScoreCalculator> calcIt = calculators.iterator(); calcIt.hasNext();){
	    		AbstractScoreCalculator calculator = calcIt.next();
	    		List<AlgorithmDescriptor> calculatorAlgorithms = calculator.getSupportedScoringMethods();
	    		for (Iterator<AlgorithmDescriptor> algIt2 = calculatorAlgorithms.iterator(); algIt2.hasNext();){
	    			AlgorithmDescriptor calculatorAlgorithm = algIt2.next();
	    			
	    			if (calculatorAlgorithm == null){
	    				throw new PsiscoreException("There is a problem with the algo descriptions", new PsiscoreFault());
	    			}
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
	    			calculatorAlgorithm = null;
	    		}
	    		calculatorAlgorithms = null;
    		}
    		requestedAlgorithm = null;
    	}
    	requestedAlgorithms = null;
    	return requiredCalculators;
    }
    
    
    /**
     * get the job associated with the jobId from the server. Will throw exceptions if the job
     * does not exist or if the scoring is still in progress.
     */
	public QueryResponse getJob(String jobId) throws PsiscoreException, InvalidArgumentException, JobStillRunningException {
		QueryResponse response = new QueryResponse();
		
		if (getJobStatus(jobId).equalsIgnoreCase(Constants.MESSAGE_JOB_FINISHED)){
			PsiscoreInput input = TempFileDataStorage.getInstance().getInputData(jobId);
			response.setResultSet(PsiTools.getInstance().getResultSet(input));
			response.setReport(TempFileDataStorage.getInstance().getReport(jobId));
		
		}else{
			throw new JobStillRunningException("The job has not yet finished please try again later", new PsiscoreFault());
		}
		
		return response;
	}
	
	/**
	 * Get the status of the scoring job. Will thrown an exception if the jobId does not exist.
	 */
	public synchronized String getJobStatus(String jobId) throws PsiscoreException, InvalidArgumentException {
		if (finishedJobs.contains(jobId)){
			return Constants.MESSAGE_JOB_FINISHED;
		}
		String jobStatus = null;
		Set<Future> futures = this.threadFutures.get(jobId);
		
		if (futures == null){
			throw new InvalidArgumentException("The server does not have any active scoring jobs for this id.", new PsiscoreFault());
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
			// remove the futures of this job and add it to the set of finished jobs
			this.threadFutures.remove(jobId);
				
			removeRunningJob(jobId);
			finishedJobs.add(jobId);
			jobStatus = Constants.MESSAGE_JOB_FINISHED;
		}else if (errorOccured){
			jobStatus = Constants.MESSAGE_JOB_ERROR;
		}else{
			jobStatus = Constants.MESSAGE_JOB_RUNNING;
		}
		futures = null;
		return jobStatus;
	}
	
		private synchronized void removeRunningJob(String jobId){
			runningJobs.remove(jobId);
		}
	
	public void removeJob(String id){
		if (uniqueIds.contains(id)){
    		uniqueIds.remove(id);
    	}
		TempFileDataStorage.getInstance().deleteStoredInputData(id);
		
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
    	random = null;
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
    protected void finalize(){
    	this.threadPool.shutdownNow();
    	
    	if (calculators != null){
    		for (Iterator<AbstractScoreCalculator> it = calculators.iterator(); it.hasNext();){
        		AbstractScoreCalculator calculator = it.next();
        		calculator = null;
    		}
    	}
    	calculators = null;
    	
    	if (uniqueIds.size() > 0){
    		Iterator<String> it = uniqueIds.iterator();
    		while (it.hasNext()){
    			removeJob(it.next());
    		}
    	}
    	
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
		this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
		//this.threadPool = Executors.newSingleThreadExecutor();
		this.threadFutures = new HashMap<String, Set<Future>>();
		this.uniqueIds = new HashSet<String>();
		this.finishedJobs = new HashSet<String>();
		this.runningJobs = new HashSet<String>();
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
		this.runningJobs.add(thread.getName());
		this.threadFutures.put(thread.getName(), futures);
	}
	
	private void threadFinished(){
		cleanUpRunningJobs();
	}
	
	
	private synchronized void addScores(ScoringParameters parameters) throws PsiscoreException{
		PsiscoreInput inputData = null;
		Report report = null; 
		
		try {
			inputData = TempFileDataStorage.getInstance().getInputData(parameters.getJobID());
			report = TempFileDataStorage.getInstance().getReport(parameters.getJobID());
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
			Report rep = parameters.getScoringReport();
			rep.getResults().add("IMPORTANT: Parts of the scoring results have potentially been deleted from the server. Run the scoring job again!");
			TempFileDataStorage.getInstance().storeReport(parameters.getJobID(), rep);
			TempFileDataStorage.getInstance().storeInputData(parameters.getJobID(), parameters.getInputData());
			//throw new PsiscoreException("The job with this ID cannot be found on the server anymore.", new PsiscoreFault(), e);
			
		}

		// add and save the new report and data
		report.getResults().addAll(parameters.getScoringReport().getResults());
		TempFileDataStorage.getInstance().storeReport(parameters.getJobID(), report);

		PsiTools.getInstance().addConfidencesToPsiscoreInput(inputData, parameters.getInputData());

		TempFileDataStorage.getInstance().storeInputData(parameters.getJobID(), inputData);
		
		inputData = null;
		report = null;
		parameters = null;
		threadFinished();
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
			//activeThreads--;
			threadFinished();
		}

		/**
		 * do something meaningful with the error that happened
		 */
		public synchronized void errorOccured(ScoringParameters parameters){
			//activeThreads--;
			
			Report report = null; 
			try {
				report = TempFileDataStorage.getInstance().getReport(parameters.getJobID());
				
			} catch (InvalidArgumentException e) {
				Report rep = parameters.getScoringReport();
				rep.getResults().add("IMPORTANT: Parts of the scoring results have potentially been deleted from the server. Run the scoring job again!");
				TempFileDataStorage.getInstance().storeReport(parameters.getJobID(), rep);
				
			}
			report.getResults().addAll(parameters.getScoringReport().getResults());
			TempFileDataStorage.getInstance().storeReport(parameters.getJobID(), report);
			
			parameters = null;
			threadFinished();

		}

		/**
		 * no scores have been found
		 */
		public synchronized void noScoresAdded(ScoringParameters parameters) throws PsiscoreException{
			//activeThreads--;
			Report report = null; 
			
			try {
				report = TempFileDataStorage.getInstance().getReport(parameters.getJobID());
			} catch (InvalidArgumentException e) {
				Report rep = parameters.getScoringReport();
				rep.getResults().add("IMPORTANT: Parts of the scoring results have potentially been deleted from the server. Run the scoring job again!");
				TempFileDataStorage.getInstance().storeReport(parameters.getJobID(), rep);
				TempFileDataStorage.getInstance().storeInputData(parameters.getJobID(), parameters.getInputData());
				//throw new PsiscoreException("The job with this ID cannot be found on the server anymore.", new PsiscoreFault(), e);
			}
			// add and save the new report
			report.getResults().addAll(parameters.getScoringReport().getResults());
			TempFileDataStorage.getInstance().storeReport(parameters.getJobID(), report);
			
			parameters = null;
			threadFinished();
		}

		
		/**
		 * new scores added, add the to complete results.
		 * @throws PsiscoreException 
		 * @throws InvalidArgumentException 
		 */
		public synchronized void scoresAdded(ScoringParameters parameters) throws PsiscoreException{
			addScores(parameters);
			
		}
	}
}

