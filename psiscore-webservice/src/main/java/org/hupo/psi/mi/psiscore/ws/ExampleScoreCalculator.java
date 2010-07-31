package org.hupo.psi.mi.psiscore.ws;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psidev.psi.mi.xml.model.*;
import org.hupo.psi.mi.psiscore.*;
import org.springframework.stereotype.Controller;

/**
 * A simple example scoring server that looks up confidence scores in a file
 *
 * @author Hagen Blankenburg 
 * @version $Id$
 */
@Controller
public class ExampleScoreCalculator extends AbstractScoreCalculator{
	Map<String, Double> scores = null;
	
	public ExampleScoreCalculator() throws PsiscoreException{
		// upon initialization, retrive the confidence scores from a file.
		// in a database environment, this would be the place to acquire
		// the database connection
		String path = System.getProperty("user.dir")+"/src/example/resources/exampleScores.txt";
		scores = readScoringFile(path);
	}
	
	
	   
    /**
     * Calculate scores for an individual single interaction. 
     *  
     * @param interaction
     * @return the same interactions plus added scores
     * @throws PsiscoreException
     */
    protected Interaction getScores (Interaction interaction) throws PsiscoreException{ 
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
			return interaction;
		}
		// try the first combination
		Double confScore = scores.get(queries.get(0)+"-"+queries.get(1));
		// if that wasnt successful, try the other one
		if (confScore == null){
			confScore = scores.get(queries.get(1)+"-"+queries.get(0));
		}
		// if it was stil lnot successful, we don't have any scores
		if (confScore == null){
			return interaction;
		}
		
		// now we only need to add a new confidence element
		String confidenceUnit = "example confidence";
		String value = confScore.toString();
		Confidence confidence = new Confidence();
		Unit unit = new Unit();
		Names names = new Names();
		names.setShortLabel(confidenceUnit);
		unit.setNames(names);
		confidence.setUnit(unit);
		confidence.setValue(value);

		// the confidence section if the interaction, the place to add new confidences
    	Collection<Confidence> confidences = interaction.getConfidences();
		confidences.add(confidence);
		// we note that we found some new scores for at least one interaction
		this.scoringParameters.setScoresAdded(true);
	    		            		
		return interaction;
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
    	descriptor.setId("example confidence");
    	List<String> algorithmTypes = descriptor.getAlgorithmType();
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
		FileInputStream fstream = null;
		DataInputStream in = null;
	    BufferedReader br = null;
    	try{
    		fstream = new FileInputStream(path);
			in = new DataInputStream(fstream);
    	    br = new BufferedReader(new InputStreamReader(in));
    	    String strLine;
    	    while ((strLine = br.readLine()) != null)   {
    	    	String[] temp = strLine.split("\\;");
    	    	// simply store a combined id from both interactors and the score
    	    	scores.put(temp[0].trim()+"-"+temp[1].trim(),Double.valueOf(temp[2].trim()));
			}
    	    fstream.close();
			br.close();
			in.close();
    	}catch (FileNotFoundException e1){
    		throw new PsiscoreException("The file you specified does not exists.");
    	}catch (IOException e) {
    		throw new PsiscoreException("The file you specified cannot be read.");
    	}
    	return scores;
	}
   
}
