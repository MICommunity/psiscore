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
import psidev.psi.mi.xml.model.*;

import org.hupo.psi.mi.psiscore.*;

/**
 * Basic interface for a simple score calculator. A simple score calculator will
 * always convert the input data into a EntrySet representation, regardless if it
 * was MITAB or XML. This allows scoring providers to save implementation time 
 * as they only need to implement the getInteractionScores method for entry sets.
 * 
 * More advanced users might want to use the BroadScoreCalculator that willnot convert 
 * MITAB input into an EntrySet representation. This requires scoring providers to
 * implemetnt two methods for adding confidence socres (one for MITAB, one for XML),
 * with the benefit that potentially time consuming EntrySet conversion can be 
 * avoided. 
 *
 * @author hagen (mpi-inf,mpg) 
 * @version $Id$
 */
public abstract class SimpleScoreCalculator extends AbstractScoreCalculator{

	
	public SimpleScoreCalculator(){
		super();
	}
	
	
	public SimpleScoreCalculator(ScoringParameters params){
		super(params);
	}

	/**
	 * Actual calculation routine that will be called
	 * once the scoring thread started. calculates
	 * the scores and notifies a listener once it finishes
	 */
    public void run() {
    	try {
    		List<String> report = calculateScores(scoringParameters.getEntrySet());
    		
    		scoringParameters.getScoringReport().getResults().addAll(report);
			if (scoringParameters.scoresAdded()){
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
    * Calculate scores for each entry in the EntrySet. The scores are added to the EntrySet directly and the 
    * scoring report will be returned.
    * @param entrySet the entry set where the scores will be added to
    * @return scoring report
    * @throws PsiscoreException
    */
    public List<String> calculateScores(EntrySet entrySet) throws PsiscoreException{
    	if (entrySet.getEntries() == null){
    		 throw new PsiscoreException("EntrySet does not contain any entries", new PsiscoreFault());
    	}
    	List<String> report = new ArrayList<String>();
    	int scored = 0;
    	int unscored = 0;
    	int totalConfidences = 0;
    	// go over all entries
		Iterator<Entry> it = entrySet.getEntries().iterator();
		scoringParameters.setScoresAdded(false);
		while (it.hasNext()){
			Entry entry = it.next();
			// and go over all interactions in that entry
			Collection<Interaction> interactions = entry.getInteractions();
			for (Iterator<Interaction> interactionIt = interactions.iterator(); interactionIt.hasNext();){
				
				Interaction interaction = interactionIt.next();

				Collection<Confidence> confidences = null;
				// and request the scores for that interaction
				try{
					confidences = getInteractionScores(interaction);
				}catch (ScoringException e){
					report.add(e.getMessage());
				}
				//confidences = null;
				if(confidences == null || confidences.size() == 0){
					unscored++;
				}else if (confidences.size() > 0){
					scoringParameters.setScoresAdded(true);
					scored++;
					totalConfidences += confidences.size();
					interaction.getConfidences().addAll(confidences);
					
				}

			} 
			
		}
		report.add(totalConfidences + " confidences scores added for " + scored +  " interactions, nothing added for " + unscored + " interactions" );
        return report;
    }
    

    
    
   /**
    * Calculate confidence scores for individual interaction
    * @param interaction
    * @return
    * @throws PsiscoreException
    */
    protected abstract Collection<Confidence> getInteractionScores (Interaction interaction) throws PsiscoreException, ScoringException;
    

    
   
    
  
    
   
}
