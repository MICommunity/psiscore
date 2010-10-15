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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hupo.psi.mi.psiscore.AlgorithmDescriptor;
import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.PsiscoreException;
import org.hupo.psi.mi.psiscore.PsiscoreFault;
import org.hupo.psi.mi.psiscore.Report;
import org.hupo.psi.mi.psiscore.model.PsiscoreInput;
import org.hupo.psi.mi.psiscore.util.PsiTools;

import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.xml.model.EntrySet;


/**
 * Parameters of the scoring job. Contains jobId, desired 
 * scoring algorithms, return format etc.
 *
 * @author hagen (mpi-inf,mpg) 
 * @version $Id$
 */
public class ScoringParameters implements Serializable{
	
	private static final long serialVersionUID = 2864816240356379576L;
	private PsiscoreInput inputData = null;
	private Report scoringReport = new Report();
	
	private String inputFormat = null;
	private String returnFormat = null;
	private String jobID = null;
	
	private List<AlgorithmDescriptor> algorithmDescriptions = null;
	private Set<String> algorithmIds = null;

	private Exception errorOccured = null;
	private boolean scoresAdded = false;
	private boolean convertedMitab = false;
	
	
	/**
	 * Empty Constructor 
	 */
	public ScoringParameters(){
		super();
	}


	/**
	 * Return the EntrySet representation of the input data. if the input is present in the xml
	 * format already, no conversion needs to be perfomred. if the input is present as a mitab,
	 * it will be converted to xml and then stored for later access
	 * @return the entrySet
	 * @throws InvalidArgumentException 
	 * @throws PsiscoreException 
	 */
	public EntrySet getEntrySet() throws PsiscoreException {
		if (inputData.xmlUsed()){
			return inputData.getXmlEntySet();
		}else if (inputData.mitabUsed() ){
			convertedMitab = true;
			this.inputData.setXmlEntySet(PsiTools.getInstance().getEntrySetFromBinaryInteractions(inputData.getMitabInteractions()));
			return inputData.getXmlEntySet();
		}else{
			throw new PsiscoreException("No valid input detected.", new PsiscoreFault());
		}
	}
	

	public Collection<BinaryInteraction> getBinaryInteraction() throws PsiscoreException{
		if (inputData.mitabUsed()){
			return inputData.getMitabInteractions();
		}else{
			throw new PsiscoreException("No valid input detected.", new PsiscoreFault()); 
		}
	}
	

	/**
	 * @param entrySet the entrySet to set
	 */
	public void setEntrySet(EntrySet entrySet) {
		this.inputData.setXmlEntySet(entrySet);
	}


	/**
	 * @return the inputFormat
	 */
	public String getInputFormat() {
		return inputFormat;
	}


	/**
	 * @param inputFormat the inputFormat to set
	 */
	public void setInputFormat(String inputFormat) {
		this.inputFormat = inputFormat;
	}


	/**
	 * @return the jobID
	 */
	public String getJobID() {
		return jobID;
	}


	/**
	 * @param jobID the jobID to set
	 */
	public void setJobID(String jobID) {
		this.jobID = jobID;
	}


	/**
	 * @return the returnFormat
	 */
	public String getReturnFormat() {
		return returnFormat;
	}


	/**
	 * @param returnFormat the returnFormat to set
	 */
	public void setReturnFormat(String returnFormat) {
		this.returnFormat = returnFormat;
	}


	public void setScoresAdded(boolean scoresAdded) {
		this.scoresAdded = scoresAdded;
	}


	public boolean scoresAdded() {
		return scoresAdded;
	}


	/**
	 * @return the algorithmDescriptions
	 */
	public List<AlgorithmDescriptor> getAlgorithmDescriptions() {
		return algorithmDescriptions;
	}


	/**
	 * @param algorithmDescriptions the algorithmDescriptions to set
	 */
	public void setAlgorithmDescriptions(
			List<AlgorithmDescriptor> algorithmDescriptions) {
		this.algorithmDescriptions = algorithmDescriptions;
		this.algorithmIds = extractAlgorithmIds();
	}


	/**
	 * @return the errorOccured
	 */
	public Exception getErrorOccured() {
		return errorOccured;
	}


	/**
	 * @param errorOccured the errorOccured to set
	 */
	public void setErrorOccured(Exception errorOccured) {
		this.errorOccured = errorOccured;
	}
	
	/**
     * Get a set of all ids of requested scoring methods
     * @return 
     * @throws PsiscoreException
     */
    private Set<String> extractAlgorithmIds(){
    	Set<String> requestedAlgorithmIds = new HashSet<String>();
		
		Iterator<AlgorithmDescriptor> it = algorithmDescriptions.iterator();
		while (it.hasNext()){
			AlgorithmDescriptor descriptor = it.next();
			requestedAlgorithmIds.add(descriptor.getId());
			
		}
		return requestedAlgorithmIds;
    }


	/**
	 * @param algorithmIds the algorithmIds to set
	 */
	public void setAlgorithmIds(Set<String> algorithmIds) {
		this.algorithmIds = algorithmIds;
	}


	/**
	 * @return the algorithmIds
	 */
	public Set<String> getAlgorithmIds() {
		return algorithmIds;
	}


	/**
	 * @return the inputData
	 */
	public PsiscoreInput getInputData() {
		return inputData;
	}


	/**
	 * @param inputData the inputData to set
	 */
	public void setInputData(PsiscoreInput inputData) {
		this.inputData = inputData;

		if (inputData.xmlUsed()){
			inputFormat = PsiTools.RETURN_TYPE_XML25;
		}else if(inputData.mitabUsed()){
			inputFormat = PsiTools.RETURN_TYPE_MITAB25;
		}
	}


	/**
	 * @return the convertedMitab
	 */
	public boolean isConvertedMitab() {
		return convertedMitab;
	}


	/**
	 * @param convertedMitab the convertedMitab to set
	 */
	public void setConvertedMitab(boolean convertedMitab) {
		this.convertedMitab = convertedMitab;
	}


	/**
	 * @return the scoringReport
	 */
	public Report getScoringReport() {
		return scoringReport;
	}


	/**
	 * @param scoringReport the scoringReport to set
	 */
	public void setScoringReport(Report scoringReport) {
		this.scoringReport = scoringReport;
	}





	
	
	
}
