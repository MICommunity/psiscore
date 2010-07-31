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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import psidev.psi.mi.xml.model.*;


import org.hupo.psi.mi.psiscore.*;
import org.hupo.psi.mi.psiscore.ws.*;

import org.springframework.stereotype.Controller;

/**
 * TODO write description of the class.
 *
 * @author Hagen Blankenburg 
 * @version $Id$
 */
@Controller
public abstract class AbstractScoreCalculator extends ScoreCalculator{
	
	public AbstractScoreCalculator(){
		super();
	}
	
	public AbstractScoreCalculator(ScoringParameters params){
		super(params);
	}

	/**
	 * Actual calculation routine that will be called
	 * once the scoring thread started. calculates
	 * the scores and notifies a listener once it finishes
	 */
    public void run() {
    	try {
			calculateScores(entrySet);
		} catch (PsiscoreException e) {
			e.printStackTrace();
			triggerErrorOccured();
		}
		
		if (this.scoringParameters.isScoresAdded()){
			triggerScoresAdded();
		}else{
			triggerNoScoresAdded();
		}
    }
    
    /**
     * Calculate the scores for each enty in the EntrySet. 
     */
    public EntrySet calculateScores(EntrySet entrySet) throws PsiscoreException{
    	if (entrySet.getEntries() == null){
    		 throw new PsiscoreException("Entry Set does not contain any entries", new PsiscoreFault());
    	}
    	// go over all entries
		Iterator<Entry> it = entrySet.getEntries().iterator();
		while (it.hasNext()){
			Entry entry = it.next();
			// and go over all interactions in that entry
			Collection<Interaction> interactions = entry.getInteractions();
			for (Iterator<Interaction> interactionIt = interactions.iterator(); interactionIt.hasNext();){
				Interaction interaction = interactionIt.next();
				// and request the scores for that interaction
				interaction = getScores(interaction);
			} 
		}
		
        return entrySet;
    }
    
    
    /**
     * Calculate scores for an indivudial single interaction. 
     *  
     * @param interaction
     * @return the same interactions plus added scores
     * @throws PsiscoreException
     */
    protected abstract Interaction getScores (Interaction interaction) throws PsiscoreException;
    
    
    /**
     * add the scoring methods (description, range of the score) and 
     * their protential requirements 
     * @return
     * @throws PsiscoreException
     */
    protected abstract List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException;
    
    
    
    private void triggerScoresAdded() {
        Iterator<ScoringListener> iter = scoringListeners.iterator();
        while (iter.hasNext()){
            ScoringListener li = iter.next();
            li.scoresAdded(this.scoringParameters);
        }
    }
    
    private void triggerNoScoresAdded() {
        Iterator<ScoringListener> iter = scoringListeners.iterator();
        while (iter.hasNext()){
            ScoringListener li = iter.next();
            li.noScoresAdded(this.scoringParameters);
        }
    }

    private void triggerErrorOccured() {
        Iterator<ScoringListener> iter = scoringListeners.iterator();
        while (iter.hasNext()){
            ScoringListener li = iter.next();
            li.errorOccured(this.scoringParameters);
        }
    }
    
  
    
   
}
