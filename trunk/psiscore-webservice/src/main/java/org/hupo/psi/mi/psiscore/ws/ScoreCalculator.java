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
import java.util.List;
import java.util.Set;

import psidev.psi.mi.xml.model.EntrySet;

import org.hupo.psi.mi.psiscore.AlgorithmDescriptor;
import org.hupo.psi.mi.psiscore.PsiscoreException;



/**
 * Abstract class describing a scoring calculator. Actual
 * scoring calculators only have to overwrite the scoring method
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author Hagen Blankenburg
 * @version $Id$
 */
public abstract class ScoreCalculator extends Thread{
	EntrySet entrySet = null;
	ScoringParameters scoringParameters = null;
	Set<ScoringListener> scoringListeners = null;
	
	/**
	 * Default constructor
	 */
    public ScoreCalculator(){
    	scoringListeners = new HashSet<ScoringListener>();
    }
	
    /**
     * Constructor 
     * @param params
     */
    public ScoreCalculator(ScoringParameters params){
    	if (params != null){
    		this.entrySet = params.getEntrySet();
    		this.scoringParameters = params;
    	}
    	this.scoringListeners = new HashSet<ScoringListener>();
    }
    
    	
    /**
     * Calculate the scores for each entry in the entryset. 
     * @param entrySet the input
     * @return the same entry set plus scores
     * @throws PsiscoreException 
     */
    abstract EntrySet calculateScores(EntrySet entrySet) throws PsiscoreException;
    
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
       	if (this.entrySet == null){
       		this.entrySet = scoringParameters.getEntrySet();
      	}
    }

}
