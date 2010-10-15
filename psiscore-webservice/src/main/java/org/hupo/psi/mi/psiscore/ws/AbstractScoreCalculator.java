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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Names;
import psidev.psi.mi.xml.model.Unit;

import org.hupo.psi.mi.psiscore.AlgorithmDescriptor;
import org.hupo.psi.mi.psiscore.PsiscoreException;



/**
 * Basic scoring interface, not defining whether the scoring will happen
 * on the XML or MITAB level. 
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author hagen (mpi-inf,mpg)
 * @version $Id$
 */
public abstract class AbstractScoreCalculator extends Thread{

	 protected ScoringParameters scoringParameters = null;
	 Set<ScoringListener> scoringListeners = null;
	
	/**
	 * Default constructor
	 */
    public AbstractScoreCalculator(){
    	super();
    	scoringListeners = new HashSet<ScoringListener>();
    }
    
    /**
     * Constructor 
     * @param params
     */
    public AbstractScoreCalculator(ScoringParameters params){
    	super();
    	
    	this.scoringParameters = params;
    	this.scoringListeners = new HashSet<ScoringListener>();
    }
    
    
    
    /**
     * Return the list of scoring algorithm descriptions a scoring calculator can calculate
     * @return
     * @throws PsiscoreException
     */
    abstract List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException;
    
    
   
    
    /**
     * Add a listener
     * @param listener
     */
    public void addScoringListener (ScoringListener listener){
    	
    	if (this.scoringListeners == null){
    		this.scoringListeners = new HashSet<ScoringListener>();
    	}
    	this.scoringListeners.add(listener);
    }
    
    /**
     * Set the scoring parameters
     * @param parameters
     */
    public void setScoringParameters (ScoringParameters parameters){
       	this.scoringParameters = parameters;
    }

	/**
	 * @return the scoringParameters
	 */
	public ScoringParameters getScoringParameters() {
		return scoringParameters;
	}

	/**
	 * @return the scoringListeners
	 */
	public Set<ScoringListener> getScoringListeners() {
		return scoringListeners;
	}

	/**
	 * @param scoringListeners the scoringListeners to set
	 */
	public void setScoringListeners(Set<ScoringListener> scoringListeners) {
		this.scoringListeners = scoringListeners;
	}
    
	
	
	/**
	 * Trigger the event that the scoring has finished successfully to all listeners
	 * @throws PsiscoreException
	 */
	protected void triggerScoresAdded() throws PsiscoreException {
	        Iterator<ScoringListener> iter = scoringListeners.iterator();
	        while (iter.hasNext()){
	            ScoringListener li = iter.next();
	            li.scoresAdded(scoringParameters);
	        }
	    }
	    
	    
	    
	/**
	 * Trigger the event that the scoring finished without the addition of
	 * new scores to all listeners
	 */
	protected void triggerNoScoresAdded()  throws PsiscoreException{
	        Iterator<ScoringListener> iter = scoringListeners.iterator();
	        while (iter.hasNext()){
	            ScoringListener li = iter.next();
	            li.noScoresAdded(scoringParameters);
	        }
	    }
	    
	    

	/**
	 * Trigger the event that the scoring finished with errors to all listeners
	 */
	protected void triggerErrorOccured(){
	        Iterator<ScoringListener> iter = scoringListeners.iterator();
	        while (iter.hasNext()){
	            ScoringListener li = iter.next();
	            li.errorOccured(scoringParameters);
	        }
	    }

}
