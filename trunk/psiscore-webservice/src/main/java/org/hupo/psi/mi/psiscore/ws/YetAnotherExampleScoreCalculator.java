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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psidev.psi.mi.xml.model.*;

import org.hupo.psi.mi.psiscore.*;
import org.springframework.stereotype.Controller;

/**
 * Yet another simple example scoring server that looks up confidence scores in a file
 *
 * @author hagen (mpi-inf,mpg) 
 * @version $Id$
 */
public class YetAnotherExampleScoreCalculator extends SimpleScoreCalculator{
	Map<String, Double[]> scores = null;
	private String[] algorithmIds = {"myConfidence1", "myConfidence 2"};
	
	
	public YetAnotherExampleScoreCalculator() throws PsiscoreException{
		super();
		// upon initialization, retrieve the confidence scores from a file
		// in a database environment, this would be the place to acquire
		// the database connection
		String path = "http://psiscore.bioinf.mpi-inf.mpg.de/yetMoreExampleScores.txt";
		scores = readScoringFile(path);
	}
	
	
	   
    /**
     * Calculate scores for an individual single interaction. 
     *  
     * @param interaction
     * @return the same interactions plus added scores
     * @throws PsiscoreException
     */
    protected Collection<Confidence> getInteractionScores (Interaction interaction) throws PsiscoreException{ 

    	Collection<Confidence> confidences = new HashSet<Confidence>();
    	
    	// this scoring method only requires the ids of the interactors
    	List<String> queries = new ArrayList<String>();

		Collection<Participant> participants = interaction.getParticipants();
		for (Iterator<Participant> participantIt = participants.iterator(); participantIt.hasNext(); ){
			Participant participant = participantIt.next();
			Interactor interactor = participant.getInteractor();
			DbReference primaryRef = interactor.getXref().getPrimaryRef();
			queries.add(primaryRef.getId());
		}
		// it can only work with two interactions, if there is a complex we cannot score it
		if (queries.size() != 2){
			return confidences;
		}
		
		// try the first combination
		Double[] confScores = scores.get(queries.get(0)+"-"+queries.get(1));
		// if that wasnt successful, try the other one
		if (confScores == null){
			confScores = scores.get(queries.get(1)+"-"+queries.get(0));
		}
		// if it was stil lnot successful, we don't have any scores
		if (confScores == null){
			return confidences;
		}
		
		// now we only need to add a new confidence element
		for (int i = 0; i < 2; i++){
			// if the current algorithm wasn requested, don't add it
			if (!getScoringParameters().getAlgorithmIds().contains(algorithmIds[i])){
				continue;
			}
			
			String confidenceUnit = algorithmIds[i];
			String value = confScores[i].toString();
			confidences.add(ConfidenceGenerator.getNewEntrySetConfidence(confidenceUnit, value));
		}
	    		            		
		return confidences;
    }
    
    
    /**
     * add the scoring methods (description, range of the score) and 
     * their protential requirements 
     * @return
     * @throws PsiscoreException
     */
    protected List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException {
    	List<AlgorithmDescriptor> descriptorList = new ArrayList<AlgorithmDescriptor>();
    	// for each of these confidence units, an entry in the PSI MI controlled vocabulary should be done
    	AlgorithmDescriptor descriptor = new AlgorithmDescriptor();
    	descriptor.setId(algorithmIds[0]);
    	descriptor.getAlgorithmTypes().add("fool curation");
    	descriptor.setRange("0-100");
    	descriptorList.add(descriptor);
    	
    	descriptor = new AlgorithmDescriptor();
    	descriptor.setId(algorithmIds[1]);
    	descriptor.getAlgorithmTypes().add("expert curation");
    	descriptor.setRange("0-100");
    	descriptorList.add(descriptor);
    	
    	return descriptorList;
    }
    
    
    /**
	 * Read confidence scores from a very simple file
	 * file format: identifier1,identifier2,score1,score2
	 * 
	 * @return
	 * @throws PsiscoreException 
	 */
	private Map<String, Double[]> readScoringFile(String path) throws PsiscoreException{
		Map<String, Double[]> scores = new HashMap<String, Double[]>();
		try {
			URL url = new URL(path);
			InputStream in = url.openStream ();
			BufferedReader dis = new BufferedReader (new InputStreamReader (in));
			String line = dis.readLine ();
			while (	( line = dis.readLine ()) != null) {
				String[] temp = line.trim().split("\\,");
				Double[] doubles = new Double[2];
    	    	doubles[0] = Double.valueOf(temp[2].trim());
    	    	doubles[1] = Double.valueOf(temp[3].trim());
    	    	scores.put(temp[0].trim()+"-"+temp[1].trim(),doubles);
			}
			in.close();
    	}catch (IOException e) {
	       throw new PsiscoreException("File not found", new PsiscoreFault(), e);
	    }
    	return scores;
	}
	
}
