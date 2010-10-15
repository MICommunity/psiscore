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
import java.util.Set;

import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml.model.*;

import org.hupo.psi.mi.psiscore.*;
import org.springframework.stereotype.Controller;

/**
 * Yet another simple example scoring server that looks up confidence scores in a file
 *
 * @author hagen (mpi-inf,mpg) 
 * @version $Id$
 */
public class MitabScoreCalculator extends SimpleScoreCalculator{
	Map<String, String> scores = null;
	Map<String, AlgorithmDescriptor> algorithms = null;
		
	public MitabScoreCalculator() throws PsiscoreException{
		super();
		// the very time intense parsing of the score file is outsourced into
		// a singleton that will only be instantiated once
		scores = MitabSingletonScoreHolder.getInstance().getScores();
		
		algorithms = new HashMap<String, AlgorithmDescriptor>();
		// and add the descriptions of all the scoring methods that are offered
		AlgorithmDescriptor descriptor = new AlgorithmDescriptor();
        descriptor.setId("MINT-score");
        List<String> algorithmTypes = descriptor.getAlgorithmTypes();
        algorithmTypes.add("literature based");
        descriptor.setRange("0-1");
        algorithms.put(descriptor.getId(), descriptor);
        
        descriptor = new AlgorithmDescriptor();
        descriptor.setId("HomoMINT-score");
        algorithmTypes = descriptor.getAlgorithmTypes();
        algorithmTypes.add("literature based");
        descriptor.setRange("0-1");
        algorithms.put(descriptor.getId(), descriptor);
        
	}
	
	
	   
    /**
     * Calculate scores for an individual single interaction. 
     *  
     * @param interaction
     * @return the same interactions plus added scores
     * @throws PsiscoreException
     * @throws ScoringException 
     */
     protected Collection<Confidence> getInteractionScores(Interaction interaction) throws PsiscoreException, ScoringException {
    	 // this server only supports scoring of binary interactions. 
    	 // if more than two particiapnts are present, it will throw an exception, that will be caught and result
    	 // in an error report

    	 if (interaction.getParticipants().size() != 2){
    		throw new ScoringException("Cannot score protein complexes, only binary interactions with two interactors.", new PsiscoreFault());
		}
    	Collection<Confidence> confidences = new ArrayList<Confidence>();
    	
        List<Collection<DbReference>> refs = new ArrayList<Collection<DbReference>>();
        Iterator<Participant> partIt = interaction.getParticipants().iterator();
        while (partIt.hasNext()){
        	Participant part = partIt.next();
        	refs.add(part.getInteractor().getXref().getAllDbReferences());
        }
       
    	  
    	Iterator<DbReference> itA = refs.get(0).iterator();
    	while (itA.hasNext()){
    		DbReference refA = itA.next();
    		if (!refA.getDb().equals("uniprotkb")){
    			continue;
    		}
			Iterator<DbReference> itB = refs.get(1).iterator();
        	while (itB.hasNext()){
        		DbReference refB = itB.next();
        		if (!refB.getDb().equals("uniprotkb")){
        			refB = null;
        			continue;
        		}
    			String idString  = null;
    			String scoreString = null;
    			if (refA.getId().compareTo(refB.getId()) < 0){
    				idString = refA.getId()+refB.getId();
    			}else{
    				idString = refB.getId()+refA.getId();
    			}
    			//System.out.println(idString );
    			if (scores.containsKey(idString)) {
    				scoreString = scores.get(idString);
    				//System.out.println(scoreString);
    				String[] confidencesSplit = scoreString.split("\\|");
        	        // now we only need to add a new confidence element for each score
    				for (String confTemp : confidencesSplit){
    					String[] conf = confTemp.split("\\:");
    					String confidenceUnit = conf[0];
    					String value = conf[1];
    					
    					Set<String> requestedAlgos = scoringParameters.getAlgorithmIds();
    					for (Iterator<String> it = requestedAlgos.iterator(); it.hasNext();){
    						String algo = it.next();
    						if (algo.equalsIgnoreCase(confidenceUnit)){
    							confidences.add(ConfidenceGenerator.getNewEntrySetConfidence(confidenceUnit, value));
    							conf = null;
    						}
    					}
    				}
    				confidencesSplit = null;
    			}
    			refB = null;
    			idString = null;
    			scoreString = null;
    		}
        	refA = null;
    	}
    	refs = null;

        return confidences;
    }

     
     
    
    /**
     * add the scoring methods (description, range of the score) and 
     * their protential requirements 
     * @return
     * @throws PsiscoreException
     */
   protected List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException {
        List<AlgorithmDescriptor> descriptions = new ArrayList<AlgorithmDescriptor>();
        Iterator<AlgorithmDescriptor> it = algorithms.values().iterator();
        while (it.hasNext()){
        	descriptions.add(it.next());
        }
        return descriptions;
    }
   
   
	   protected void finalize() throws Throwable {
		    try {
		        scores = null;
		        algorithms = null;
		    } finally {
		        super.finalize();
		    }
		}

    
    
    
	
	 public static void main(String[] args) throws PsiscoreException{
		 MitabScoreCalculator calc = new MitabScoreCalculator();
		 //System.out.println(calc.getSupportedScoringMethods());
		 
		 Interaction interaction = new Interaction();
		 
		 Participant part = new Participant();
		 Interactor inter = new Interactor();
		 Xref xref = new Xref();
		 DbReference dbRef = new DbReference();
		 dbRef.setDb("uniprotkb");
		 dbRef.setId("Q9NZD8");
		 xref.setPrimaryRef(dbRef);
		 inter.setXref(xref);
		 part.setInteractor(inter);
		 interaction.getParticipants().add(part);
		 
		 
		 part = new Participant();
		 inter = new Interactor();
		 xref = new Xref();
		 dbRef = new DbReference();
		 dbRef.setDb("uniprotkb");
		 dbRef.setId("O14978");
		 xref.setPrimaryRef(dbRef);
		 inter.setXref(xref);
		 part.setInteractor(inter);
		 interaction.getParticipants().add(part);
		 Collection<Confidence> confidences = null;
		 try {
			 confidences = calc.getInteractionScores(interaction);
		} catch (ScoringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!confidences.isEmpty()){
			Iterator<Confidence> it = confidences.iterator();
			while (it.hasNext()){
				Confidence conf = it.next();
				System.out.println(conf.getValue() + conf.getUnit());
			}
		}else{
			System.out.println("nothing");
		}
	 }
}
