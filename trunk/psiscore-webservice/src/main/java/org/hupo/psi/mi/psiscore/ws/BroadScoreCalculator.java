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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import psidev.psi.mi.tab.model.BinaryInteraction;

import org.hupo.psi.mi.psiscore.*;
import org.hupo.psi.mi.psiscore.util.PsiTools;

/**
 * the BroadScoreCalculator  will not convert 
 * MITAB input into an EntrySet representation. This requires scoring providers to
 * implemetnt two methods for adding confidence socres (one for MITAB, one for XML),
 * with the benefit that potentially time consuming EntrySet conversion can be 
 * avoided.
 * @author hagen (mpi-inf,mpg) 
 * @version $Id$
 */
public abstract class BroadScoreCalculator extends SimpleScoreCalculator{

	
	public BroadScoreCalculator(){
		super();
	}
	
	
	public BroadScoreCalculator(ScoringParameters params){
		super(params);
	}

	/**
	 * Actual calculation routine that will be called
	 * once the scoring thread started. calculates
	 * the scores and notifies a listener once it finishes
	 */
    public void run() {
    	try {
    		if (getScoringParameters().getInputFormat().equalsIgnoreCase(PsiTools.RETURN_TYPE_MITAB25)){
    			List<String> report = calculateScores(getScoringParameters().getBinaryInteraction());
        		getScoringParameters().getScoringReport().getResults().addAll(report);
    			
    		}else{
    			List<String> report = calculateScores(getScoringParameters().getEntrySet());
        		getScoringParameters().getScoringReport().getResults().addAll(report);
    		}
			if (getScoringParameters().scoresAdded()){
				triggerScoresAdded();
			}else{
				triggerNoScoresAdded();
			}
    	} catch (PsiscoreException e) {
			e.printStackTrace();
			triggerErrorOccured();
		}
    }
    
   
    /**
     * Not yet supported
     * @param interactions
     * @return
     * @throws PsiscoreException
     */
    public List<String> calculateScores(Collection<psidev.psi.mi.tab.model.BinaryInteraction> interactions) throws PsiscoreException{
    	 // not yet supported
    	 throw new PsiscoreException("BroadScoring is not yet supported");
    }
    
   /**
    * Calculate confidence scores for individual interaction
    * @param interaction
    * @return
    * @throws PsiscoreException
    */
    protected abstract Collection<psidev.psi.mi.xml.model.Confidence> getInteractionScores (psidev.psi.mi.xml.model.Interaction interaction) throws PsiscoreException;
    
    /**
     * Calculate confidence scores for individual interaction
     * @param interaction
     * @return
     * @throws PsiscoreException
     */
    protected abstract List<psidev.psi.mi.tab.model.Confidence> getInteractionScores (psidev.psi.mi.tab.model.BinaryInteractionImpl interaction) throws PsiscoreException;
    

    
   
    
  
    
   
}
