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


import java.util.List;

import org.hupo.psi.mi.psiscore.AlgorithmDescriptor;

import psidev.psi.mi.xml.model.EntrySet;


/**
 * Parameters of the scoring job. Contains jobId, desired 
 * scoring algorithms, return format etc.
 *
 * @author Hagen Blankenburg 
 * @version $Id$
 */
public class ScoringParameters{
	
	private EntrySet entrySet = null;
	private String inputFormat = null;
	private String returnFormat = null;
	private String jobID = null;
	private boolean scoresAdded = false;
	private List<AlgorithmDescriptor> algorithmDescriptions = null;
	private Exception errorOccured = null;
	
	
	/**
	 * Empty Constructor 
	 */
	public ScoringParameters(){
		super();
	}


	/**
	 * @return the entrySet
	 */
	public EntrySet getEntrySet() {
		return entrySet;
	}


	/**
	 * @param entrySet the entrySet to set
	 */
	public void setEntrySet(EntrySet entrySet) {
		this.entrySet = entrySet;
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


	public boolean isScoresAdded() {
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
	
	
}
