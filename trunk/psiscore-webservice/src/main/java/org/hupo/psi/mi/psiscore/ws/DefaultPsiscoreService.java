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
package org.hupo.psi.mi.psiscore.ws;

import org.apache.cxf.transport.http.gzip.GZIPFeature;
import org.hupo.psi.mi.psiscore.*;
import org.hupo.psi.mi.psiscore.ws.config.PsiscoreConfig;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebParam;

import psidev.psi.mi.xml.model.Entry;

import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Parameter;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml.converter.impl254.EntryConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;

//import psidev.psi.mi.tab.converter.txt2tab.MitabLineParser;
import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.tab.converter.xml2tab.Xml2Tab;
import psidev.psi.mi.tab.converter.xml2tab.TabConversionException;
import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.PsimiTabWriter;

import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.builder.MitabDocumentDefinition;

import java.io.IOException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * default scoring service
 * provides basic threading functionality to host multiple scoring services.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author Hagen Blankenburg
 * @version $Id$
 */
@Controller
public class DefaultPsiscoreService implements PsiscoreService {

	// in this array you specify all the calculators the server will later manage.
	// for this, an instance of each class will be created to handle the associated
	// requests
	// TODO move this into a config file
    private String[] calculatorClasses = {"org.hupo.psi.mi.psiscore.ws.ExampleScoreCalculator",
    		"org.hupo.psi.mi.psiscore.ws.YetAnotherExampleScoreCalculator"};
	
    private static final int THREAD_POOL_SIZE = 2;
	private static final int STANDARD_POLLING_INTERVAL = 5; // seconds
	private static final int NO_POLLING_INTERVAL = 1; // seconds
    
	public static final String MESSAGE_JOB_FINISHED = "finished";
	public static final String MESSAGE_JOB_RUNNING = "running";
	public static final String RETURN_TYPE_XML25 = "psi-mi/xml25";
	public static final String RETURN_TYPE_MITAB25 = "psi-mi/tab25";
	private static final String NEW_LINE = System.getProperty("line.separator");
		
    private int activeThreads = 0;
    
   @Autowired
    private PsiscoreConfig config;
    
   	private PsiTools psiTools = null; 
    private Set<ScoreCalculator> calculators = null;
    private HashMap<String, ScoringParameters> allJobs = null;
    private ExecutorService threadPool = null;
    private Map<String, Set<Future>> threadFutures = null;
    private static Set<String> uniqueIds;

    /**
     *  Init all global variables
     * @throws PsiscoreException 
     */
    public DefaultPsiscoreService() throws PsiscoreException{
    	allJobs = new HashMap<String, ScoringParameters>();
    	psiTools = new PsiTools();
    	
    	calculators = new HashSet<ScoreCalculator>();
    	
    	for (int i = 0; i < calculatorClasses.length; i++){
    		try {
    			System.out.println("\tInitializing score calculator: " + calculatorClasses[i]);
    			ScoreCalculator calc = (ScoreCalculator)( Class.forName( calculatorClasses[i] ).newInstance() );
    			calculators.add(calc);
    		} catch (InstantiationException e) {
    			throw new PsiscoreException("Cannot instantiate scoring calculator " + calculatorClasses[i], new PsiscoreFault(), e);
    		} catch (IllegalAccessException e) {
    			throw new PsiscoreException("Cannot instantiate scoring calculator " + calculatorClasses[i], new PsiscoreFault(), e);
    		} catch (ClassNotFoundException e) {
    			throw new PsiscoreException("Cannot instantiate scoring calculator " + calculatorClasses[i], new PsiscoreFault(), e);
    		}
    	}

    	initThreadPool();
    }
    
   
    

    /**
     * Request the server to score the interactions specified in the data section of the request with the algorithms listed. 
     */
    public JobResponse submitJob( List<AlgorithmDescriptor> algorithmDescriptions, ResultSet inputData, String returnFormat) throws PsiscoreException, InvalidArgumentException{
    	//prepare the user respose 
		JobResponse response = new JobResponse();
		ScoringParameters parameters = new ScoringParameters();
		
		String jobID = getUniqueId();
		response.setJobId(jobID);

		// get the input data
		psidev.psi.mi.xml.model.EntrySet entrySet = psiTools.getEntrySet(inputData);
		String inputFormat = psiTools.getInputFormat(inputData);
		// estimate the poling time, should be overriden by scoring instances
		response.setPollingInterval(getPollingInterval(entrySet));
		
		// TODO only submit algoDesc for current scoring class
		parameters.setAlgorithmDescriptions(algorithmDescriptions);
		parameters.setEntrySet(entrySet);
		parameters.setInputFormat(inputFormat);
		parameters.setReturnFormat(returnFormat);
		parameters.setJobID(jobID); // jobID for user job, can have multiple subjobs for each algorithm
		// store the job and its parameters for later
		allJobs.put(jobID, parameters);
		
		// check which calculators we actually need to fulfill all requests		
		Set<Class> requiredCalculators =  getRequiredCalculators(algorithmDescriptions);
		// and start the scoring jobs for each thread
		for (Iterator<Class> it = requiredCalculators.iterator(); it.hasNext();){
			ScoreCalculator calculator = null;
			try {
				calculator = (ScoreCalculator)it.next().newInstance();
			} catch (InstantiationException e) {
				throw new PsiscoreException("Cannot instantiate scoring calculator ", new PsiscoreFault(), e);
			} catch (IllegalAccessException e) {
				throw new PsiscoreException("Cannot instantiate scoring calculator ", new PsiscoreFault(), e);
			}
			
			ScoringListener listener = new MyListener();
			calculator.addScoringListener(listener);
			calculator.setName(jobID);
			calculator.setScoringParameters(parameters);
			submitThreadToPool(calculator);
		}
		
		return response;
	}
    
    
    
	/**
     * Get version of the scoring service
     */
    public String getVersion() throws PsiscoreException {
        return config.getVersion();
    }
    
    
    /**
     * Return all available scoring algorithms. 
     */
    public List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException {
    	List<AlgorithmDescriptor> descriptorList = new ArrayList<AlgorithmDescriptor>();

    	for (Iterator<ScoreCalculator> it = calculators.iterator(); it.hasNext();){
    		ScoreCalculator calc = it.next();
   			descriptorList.addAll(calc.getSupportedScoringMethods());
    	}
    	
    	return descriptorList;
    }
    
    
    /**
     * Return all available return types
     */
    public List<String> getSupportedReturnTypes() throws PsiscoreException {
    	List<String> supportedReturnTypes = new ArrayList<String>();
    	
    	supportedReturnTypes.add(RETURN_TYPE_MITAB25);
    	//supportedReturnTypes.add(RETURN_TYPE_XML25);
    	return supportedReturnTypes;
    }

    
    /**
     * Retun the scoring calculators that are neede to fulfll the requested algorithm descriptions
     * @param algorithms
     * @return
     * @throws PsiscoreException
     */
    private Set<Class> getRequiredCalculators(List<AlgorithmDescriptor> algorithms) throws PsiscoreException{
    	Set<Class> requiredCalculators = new HashSet<Class>();
    	
    	for (Iterator<AlgorithmDescriptor> algIt = algorithms.iterator(); algIt.hasNext();){
    		AlgorithmDescriptor algorithm = algIt.next();
    		for (Iterator<ScoreCalculator> calcIt = calculators.iterator(); calcIt.hasNext();){
	    		ScoreCalculator calc = calcIt.next();
	    		List<AlgorithmDescriptor> calculatorAlgorithms = calc.getSupportedScoringMethods();
	    		for (Iterator<AlgorithmDescriptor> algIt2 = calculatorAlgorithms.iterator(); algIt2.hasNext();){
	    			AlgorithmDescriptor calculatorAlgorithm = algIt2.next();
	    			if (calculatorAlgorithm.getId() == null || algorithm.getId() == null){
	    				throw new PsiscoreException("There is a problem with the algo descriptions", new PsiscoreFault());
	    			}
	    			if (calculatorAlgorithm.getId().equalsIgnoreCase(algorithm.getId())){
	    				//System.out.println("LOADER CLASS" + calc.getClass());
	    				requiredCalculators.add(calc.getClass());
	    			}
	    		}
    		}
    	}
    	return requiredCalculators;
    }
    
    

    
    /**
     * 
     */
	public QueryResponse getJob(String jobID) throws PsiscoreException, InvalidArgumentException, JobStillRunningException {
		QueryResponse response = null;
		ResultSet rs = null;
		EntrySet entrySet = null;
		Set<Future> futures = this.threadFutures.get(jobID);
		
		if (!allJobs.containsKey(jobID)){
			throw new InvalidArgumentException("There is no job with this id1", new PsiscoreFault());
		}
		if (futures == null){
			throw new InvalidArgumentException("The server does not have any active scoring jobs", new PsiscoreFault());
		}
		entrySet = allJobs.get(jobID).getEntrySet();
		response = new QueryResponse();
			
		boolean allDone = true;
		for (Iterator<Future> it = futures.iterator(); it.hasNext();){
			if (!it.next().isDone()){
				allDone = false;
			}
		}
		if (allDone){
			response.setResultSet(getResultSet(allJobs.get(jobID)));
		}else{
			throw new JobStillRunningException("The job has not yet finished please try again later", new PsiscoreFault());
		}
		
		return response;
	}
	
	public String getJobStatus(String jobID) throws PsiscoreException, InvalidArgumentException {
		String jobStatus = null;
		Set<Future> futures = this.threadFutures.get(jobID);
		
		if (!allJobs.containsKey(jobID)){
			throw new InvalidArgumentException("There is no job with this id", new PsiscoreFault());
		}
		if (futures == null){
			throw new InvalidArgumentException("The server does not have any active scoring jobs", new PsiscoreFault());
		}
			
		// check all futures to see if the individual scoring job have finished
		boolean allDone = true;
		for (Iterator<Future> it = futures.iterator(); it.hasNext();){
			if (!it.next().isDone()){
				allDone = false;
			}
		}
		if (allDone){
			jobStatus = MESSAGE_JOB_FINISHED;
		}else{
			jobStatus = MESSAGE_JOB_RUNNING;
		}
		
		return jobStatus;
	}


	private ResultSet getResultSet(ScoringParameters parameters) throws PsiscoreException{
		psidev.psi.mi.xml.model.EntrySet entrySet = parameters.getEntrySet();
		
		ResultSet rs = new ResultSet();
		if (parameters.getInputFormat().equals(RETURN_TYPE_XML25)){
			//System.out.println("Will return XML");
			try {
			    rs.setEntrySet(new EntrySetConverter().toJaxb(parameters.getEntrySet()));
			} catch (ConverterException e) {
				e.printStackTrace();
			    throw new PsiscoreException("Problem when converting the EntrySet to XML ", new PsiscoreFault());
			}
		}
		else if (parameters.getInputFormat().equals(RETURN_TYPE_MITAB25)){
			//System.out.println("Will return TAB");
			try {
				Xml2Tab xml2tab = new Xml2Tab();
				Collection<BinaryInteraction> binaryInteractions = xml2tab.convert(parameters.getEntrySet());

				String mitab = createMitabResults((List<BinaryInteraction>) binaryInteractions);
		    	rs.setMitab(mitab);
			
			} catch (Exception e) {
				e.printStackTrace();
			    throw new PsiscoreException("Problem converting EntrySet to Mitab ", new PsiscoreFault());
			}
		}
		return rs;
	}
	
    
	/**
	 * 
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
	 * 
	 * @param binaryInteractions
	 * @return
	 */
	private String createMitabResults(List<BinaryInteraction> binaryInteractions) {
		MitabDocumentDefinition docDef = new MitabDocumentDefinition();
		StringBuilder sb = new StringBuilder(binaryInteractions.size() * 512);
	
		for (BinaryInteraction binaryInteraction : binaryInteractions) {
			String binaryInteractionString = docDef.interactionToString(binaryInteraction);
			sb.append(binaryInteractionString);
			sb.append(NEW_LINE);
		}
		return sb.toString();
	}


	/**
	 * 
	 * @param binaryInteractions
	 * @return
	 * @throws PsiscoreException
	 */
	private psidev.psi.mi.xml254.jaxb.EntrySet createEntrySet(List<BinaryInteraction> binaryInteractions) throws PsiscoreException {
		if (binaryInteractions.isEmpty()) {
			return new psidev.psi.mi.xml254.jaxb.EntrySet();
		}
	
		Tab2Xml tab2Xml = new Tab2Xml();
		try {
			psidev.psi.mi.xml.model.EntrySet mEntrySet = tab2Xml.convert(binaryInteractions);
	
			EntrySetConverter converter = new EntrySetConverter();
			converter.setDAOFactory(new InMemoryDAOFactory());
	
			return converter.toJaxb(mEntrySet);

			
		} catch (Exception e) {
			throw new PsiscoreException("Problem converting results to PSI-MI XML", new PsiscoreFault());
		}
	}
	
	 /**
     * Clean up the mess
     */
    public void finalize(){
    	this.threadPool.shutdownNow();
    	//allInputData = null;
    	allJobs = null;
    	if (calculators != null){
    		for (Iterator<ScoreCalculator> it = calculators.iterator(); it.hasNext();){
        		ScoreCalculator calculator = it.next();
        		calculator = null;
    		}
    	}
    	calculators = null;
    }
	
	/**
	 * Start the thread pool and initialize the futures
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
		Future future = null;
		future = threadPool.submit(thread);
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
	 * Stup method for gettin a standard polling interval.
	 * should be overriden by user scoring servers
	 * @param entrySet
	 * @return
	 */
	private int getPollingInterval(EntrySet entrySet){
		if (this.activeThreads >= THREAD_POOL_SIZE){
			return STANDARD_POLLING_INTERVAL;
		}else{
			return NO_POLLING_INTERVAL;
		}
	}
	
	
	
	

	/**
	 * Listener class handling the outcome of scoring jobs
	 * @author hagen
	 *
	 */
	private class MyListener implements ScoringListener{

		/**
		 * not yet supported
		 */
		public synchronized void comeBackLater() {
			activeThreads--;
			// not supported yet 
		}

		/**
		 * todo something meaningful with respect to error handling
		 */
		public synchronized void errorOccured(ScoringParameters params) {
			activeThreads--;
		}

		/**
		 * no scores have been found
		 */
		public synchronized void noScoresAdded(ScoringParameters params){
			activeThreads--;
		}
		
		/*public synchronized void scoresAdded(ScoringParameters parameters) throws PsiscoreException {
			allJobs.put(parameters.getJobID(), parameters);
		}*/

		
		/**
		 * new scores added, add the to complete results.
		 */
		public synchronized void scoresAdded(ScoringParameters parameters){
			activeThreads--;
			//job id is only unique for a specific user reuqest, there can be multiple individual 
			// scoring jobs per id

			ScoringParameters oldParameters = allJobs.get(parameters.getJobID());
			
			psidev.psi.mi.xml.model.EntrySet oldEntrySet = null;
			oldEntrySet = oldParameters.getEntrySet();
			psidev.psi.mi.xml.model.EntrySet newEntrySet = null;
			newEntrySet = parameters.getEntrySet();
			
			Iterator<Entry> oldIt = oldEntrySet.getEntries().iterator();
			Iterator<Entry> newIt = newEntrySet.getEntries().iterator();
			while (newIt.hasNext()){
				Entry newEntry = newIt.next();
				Entry oldEntry = oldIt.next();
				Collection<Interaction> newInteractions = newEntry.getInteractions();
				Collection<Interaction> oldInteractions = oldEntry.getInteractions();
				
				Iterator<Interaction> newIntIt = newInteractions.iterator();
				Iterator<Interaction> oldIntIt = oldInteractions.iterator();
				while (newIntIt.hasNext()){
					
					Interaction newInteraction = newIntIt.next();
					Interaction oldInteraction = oldIntIt.next();
					if (!newInteraction.equals(oldInteraction)){
						//throw new PsiscoreException("Trying to match distinct interactions", new PsiscoreFault());
					}
					if (newInteraction.getId() != oldInteraction.getId()){
						//throw new PsiscoreException("Trying to match distinct interactions", new PsiscoreFault());
					}
					//System.out.println(newInteraction.getId() + "-" +oldInteraction.getId());
					
					Collection<Confidence> oldConfidences = oldInteraction.getConfidences();
					Collection<Confidence> newConfidences = newInteraction.getConfidences();
					//oldConfidences.addAll(newConfidences);
					
					Iterator<Confidence> oldConfIt = oldConfidences.iterator();
					while(oldConfIt.hasNext()){
						
			    		Confidence oldConfidence = oldConfIt.next();
			    		//System.out.println("HAVE value: " + oldConfidence.getValue() + "- : " + oldConfidence.getUnit().getNames().getShortLabel());
			    		
			    		Iterator<Confidence> newConfIt = newConfidences.iterator();
			    		while(newConfIt.hasNext()){
			    			if (oldConfidence.equals(newConfIt.next())){
			    				//newConfIt.remove();
			    				//System.out.println("NOT, as there: value: " + oldConfidence.getValue() + " - " + oldConfidence.getUnit().getNames().getShortLabel());
			    			}
			    		}
			    		
			    		
			    		//System.out.println("Value: " + oldConfidence.getValue());
			    		//System.out.println("Unit: " + oldConfidence.getUnit().getNames().getShortLabel());
			    		
					}
					
					Iterator<Confidence> newConfIt = newConfidences.iterator();
					while(newConfIt.hasNext()){
						Confidence newConfidence = newConfIt.next();
						//System.out.println("WILL ADD value: " + newConfidence.getValue() + "- : " + newConfidence.getUnit().getNames().getShortLabel());
					}
					
				}
			}
			//System.out.println("Added confidences, good");
			allJobs.put(oldParameters.getJobID(), oldParameters);
			
		
		}
	}
}

