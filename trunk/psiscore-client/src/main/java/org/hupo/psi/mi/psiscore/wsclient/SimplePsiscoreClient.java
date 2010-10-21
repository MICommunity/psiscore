/**
 * Copyright 2009 The European Bioinformatics Institute, and others.
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
package org.hupo.psi.mi.psiscore.wsclient;

import org.hupo.psi.mi.psiscore.AlgorithmDescriptor;
import org.hupo.psi.mi.psiscore.JobResponse;
import org.hupo.psi.mi.psiscore.JobStillRunningException;
import org.hupo.psi.mi.psiscore.PsiscoreFault;
import org.hupo.psi.mi.psiscore.QueryResponse;
import org.hupo.psi.mi.psiscore.PsiscoreException;
import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.util.PsiTools;

import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.tab.converter.xml2tab.TabConversionException;
import psidev.psi.mi.tab.converter.xml2tab.Xml2Tab;
import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;
import psidev.psi.mi.xml.model.EntrySet;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Generic PSISCORE client providing basic functionality to query a particular PSISCORE server.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author hagen (mpi-inf,mpg)
 */
public class SimplePsiscoreClient extends AbstractPsiscoreClient {
	private boolean active = true;
	private String id = null;
	


	/**
	 * Default constructor
	 * @param serviceAddress URL of the PSISCORE server that is to be queried
	 */
    public SimplePsiscoreClient(String serviceAddress) {
        super(serviceAddress);
        this.id = serviceAddress;
    }

    
    /**
     * 
     * @param serviceAddress  URL of the PSISCORE server that is to be queried
     * @param timeout HTTP timout in milliseconds
     */
    protected SimplePsiscoreClient(String serviceAddress, long timeout) {
        super(serviceAddress, timeout);
        this.id = serviceAddress;
    }


    /**
	 * get the version of the client
	 * @return
	 * @throws PsiscoreClientException
	 */
    public String getVersion() throws PsiscoreException {
    	try{
    		return getService().getVersion();
    	}catch (javax.xml.ws.soap.SOAPFaultException e){
    		throw new PsiscoreException("Cannot connect to server", new PsiscoreFault(), e);
    	}
    }
    

    /**
	 * get a certain scoring job from the server
	 * @param jobId
	 * @return
	 * @throws PsiscoreClientException
	 * @throws InvalidArgumentException
	 * @throws JobStillRunningException
	 */
    public QueryResponse getJob(String jobId) throws PsiscoreException, InvalidArgumentException, JobStillRunningException {
    	try{
    		return getService().getJob(jobId);
    	}catch (javax.xml.ws.soap.SOAPFaultException e){
    		throw new PsiscoreException("Cannot connect to server.", new PsiscoreFault(), e);
    	}
    }
    
    
    /**
	 * get the status of a scoring job
	 * @param jobId
	 * @return
	 * @throws PsiscoreClientException
	 */
    public String getJobStatus(String jobId) throws PsiscoreException, InvalidArgumentException {
    	try{
    		return getService().getJobStatus(jobId);
    	}catch (javax.xml.ws.soap.SOAPFaultException e){
    		throw new PsiscoreException("Cannot connect to server.", new PsiscoreFault(), e);
    	}
    }

    /**
	 * get the supported data formats of the server
	 * @return
	 * @throws PsiscoreClientException
	 */
    public List<String> getSupportedDataTypes() throws PsiscoreException {
    	try{
    		return getService().getSupportedDataTypes();
    	}catch (javax.xml.ws.soap.SOAPFaultException e){
    		throw new PsiscoreException("Cannot connect to server.", new PsiscoreFault(), e);
    	}
    }

    
    /**
	 * submit a scoring job.
	 * @param algorithmDescriptor
	 * @param inputData
	 * @param returnFormat
	 * @return
	 * @throws PsiscoreClientException
	 * @throws InvalidArgumentException
	 * @throws PsiscoreException
	 */
    public JobResponse submitJob(java.util.List<org.hupo.psi.mi.psiscore.AlgorithmDescriptor> algorithmDescriptors, org.hupo.psi.mi.psiscore.ResultSet inputData, String returnFormat) throws PsiscoreClientException, PsiscoreException, InvalidArgumentException{
    	//validateInput(algorithmDescriptors, inputData);
    	
    	try{
    		return getService().submitJob(algorithmDescriptors, inputData, returnFormat);
    	}catch (javax.xml.ws.soap.SOAPFaultException e){
    		throw new PsiscoreException("Cannot connect to server.", new PsiscoreFault(), e);
    	}
    }

    
    /**
	 * the a description of all scoring algortihms the server provides
	 * @return
	 * @throws PsiscoreClientException
	 */
    public List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException {
    	try{
    		return getService().getSupportedScoringMethods();
    	}catch (javax.xml.ws.soap.SOAPFaultException e){
    		throw new PsiscoreException("Cannot connect to server", new PsiscoreFault(), e);
    	}
    }
    
    /**
     * Check if the input is valid. returns true if a valid input was found, otherwise throws an exception
     * @param algorithmDescriptor
     * @param inputData
     * @param returnFormat
     * @throws PsiscoreClientException
     * @throws PsiscoreException
     * @throws InvalidArgumentException
     */
    private boolean validateInput(java.util.List<org.hupo.psi.mi.psiscore.AlgorithmDescriptor> algorithmDescriptor, org.hupo.psi.mi.psiscore.ResultSet inputData) throws PsiscoreClientException, PsiscoreException, InvalidArgumentException{
    	EntrySet rs = PsiTools.getInstance().getEntrySetFromInput(inputData);
    	rs = null;
    	return true;
    }

    /**
     * 
     * Parse Mitab from String to Collection. Will fail if there is an error in the file
     * @param mitab
     * @param skipHeader
     * @return
     * @throws PsiscoreException Exception that occurs while parsing
     * @throws InvalidArgumentException If a MITAB file has been transmitted that is not valid
     */
    private psidev.psi.mi.xml.model.EntrySet parseMitab(String mitab, boolean skipHeader) throws PsiscoreClientException, InvalidArgumentException{
    	PsimiTabReader reader = new PsimiTabReader(skipHeader);
    	Collection<BinaryInteraction> binaryInteractions = null;
    	psidev.psi.mi.xml.model.EntrySet entrySet = null;
    	
		try {
			binaryInteractions = reader.read(mitab);
			
			Tab2Xml tab2xml = new Tab2Xml();
		    entrySet = tab2xml.convert(binaryInteractions);
					    
		    Xml2Tab xml2tab = new Xml2Tab();
		    Collection<BinaryInteraction> binaryInteractionsConverted = xml2tab.convert(entrySet);
		    
			 
		} catch (IllegalAccessException e) {
			throw new PsiscoreClientException("There was a problem trying to parse the MITAB file.", new PsiscoreFault());
		} catch (XmlConversionException e) {
			throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());
		} catch (TabConversionException e) {
			throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());		
		}catch (ConverterException e) {
			throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());		
		}catch (IOException e) {
			throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());		
		}
		
		return entrySet;
    }
    
	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
    
}
