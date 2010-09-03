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

import psidev.psi.mi.xml.model.*;

import org.hupo.psi.mi.psiscore.*;
import org.springframework.stereotype.Controller;

/**
 * A simple example scoring server that looks up confidence scores in a file
 *
 * @author hagen (mpi-inf,mpg) 
 * @version $Id$
 */
public class ExampleScoreCalculator extends SimpleScoreCalculator{
	private Map<String, Double> scores = null;
	private String algorithmId = "example confidence";
	
	public ExampleScoreCalculator() throws PsiscoreException{
		super();
		// upon initialization, retrive the confidence scores from a file.
		// in a database environment, this would be the place to acquire
		// the database connection
		String path = "http://psiscore.bioinf.mpi-inf.mpg.de/exampleScores.txt";
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
		Double confScore = scores.get(queries.get(0)+"-"+queries.get(1));
		// if that wasnt successful, try the other one
		if (confScore == null){
			confScore = scores.get(queries.get(1)+"-"+queries.get(0));
		}
		// if it was stil lnot successful, we don't have any scores
		if (confScore == null){
			return confidences;
		}
		
		
		
		// now we only need to add a new confidence element
		String confidenceUnit = algorithmId;
		String value = confScore.toString();
		confidences.add(ConfidenceGenerator.getNewEntrySetConfidence(confidenceUnit, value));
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
    	
    	AlgorithmDescriptor descriptor = new AlgorithmDescriptor();
    	descriptor.setId(algorithmId);
    	List<String> algorithmTypes = descriptor.getAlgorithmTypes();
    	algorithmTypes.add("predicted");
    	descriptor.setRange("0-1");
    	descriptorList.add(descriptor);
    	
    	return descriptorList;
    }
    
    
    /**
	 * Read confidence scores from a very simple file
	 * file format: identifier1;identifier2;score
	 * 
	 * @return
	 * @throws PsiscoreException 
	 */
	private Map<String, Double> readScoringFile(String path) throws PsiscoreException{
		Map<String, Double> scores = new HashMap<String, Double>();
		try {
			URL url = new URL(path);
			InputStream in = url.openStream ();
			BufferedReader dis = new BufferedReader (new InputStreamReader (in));
			String line = dis.readLine ();
			while (	( line = dis.readLine ()) != null) {
				String[] temp = line.split("\\;");
    	    	// simply store a combined id from both interactors and the score
    	    	scores.put(temp[0].trim()+"-"+temp[1].trim(),Double.valueOf(temp[2].trim()));
			}
			in.close();
    	}catch (IOException e) {
	       throw new PsiscoreException("File not found", new PsiscoreFault(), e);
	    }
    	return scores;
	}
   
}
