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

import org.apache.cxf.transport.http.gzip.GZIPFeature;
import org.hupo.psi.mi.psiscore.*;
import org.hupo.psi.mi.psiscore.ws.config.PsiscoreConfig;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebParam;

import psidev.psi.mi.xml.model.Entry;

import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Parameter;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml.converter.impl254.EntryConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;

//import psidev.psi.mi.tab.converter.txt2tab.MitabLineParser;
import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.tab.converter.xml2tab.Xml2Tab;
import psidev.psi.mi.tab.converter.xml2tab.TabConversionException;
import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.PsimiTabWriter;

import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.builder.MitabDocumentDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * abstract default scoring service
 * provides basic threading functionality to host multiple
 * scoring services. 
 *
 * 
 * @author Hagen Blankenburg
 * @version $Id$
 */

public class PsiTools{
	public static final String RETURN_TYPE_XML25 = "psi-mi/xml25";
	public static final String RETURN_TYPE_MITAB25 = "psi-mi/tab25";
	private static final String NEW_LINE = System.getProperty("line.separator");
		

    /**
     * 
     */
    public PsiTools(){
    }
    
   /**
    * checks if a valid XML or MITAB has been submitted. if both or non are existing, an exception will be thrown
    * @param inputData
    * @return
    * @throws InvalidArgumentException
    * @throws PsiscoreException
    */
    protected String getInputFormat(ResultSet inputData) throws InvalidArgumentException, PsiscoreException{
    	String inputFormat = null;
    	if (inputData.getEntrySet() != null && inputData.getMitab() != null){
    		throw new InvalidArgumentException("Only use PSI-MI XML or PSI-MI TAB, not both ", new PsiscoreFault());
    	}
    	else if (inputData.getEntrySet() != null){
			inputFormat = RETURN_TYPE_XML25;
		}else if (inputData.getMitab() != null){
			inputFormat = RETURN_TYPE_MITAB25;
		}
		else{
			throw new InvalidArgumentException("No valid input data (MITAB or PSIMI XML) detected", new PsiscoreFault());
		}
    	return inputFormat;
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
    protected psidev.psi.mi.xml.model.EntrySet parseMitab(String mitab, boolean skipHeader) throws PsiscoreException, InvalidArgumentException{
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
			throw new PsiscoreException("There was a problem trying to parse the MITAB file.", new PsiscoreFault());
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
     * Get the XML  representation of the input data. 
     * @param inputData
     * @return
     * @throws InvalidArgumentException 
     * @throws PsiscoreException 
     */
    protected psidev.psi.mi.xml.model.EntrySet getEntrySet(ResultSet inputData) throws InvalidArgumentException, PsiscoreException{
    	psidev.psi.mi.xml.model.EntrySet entrySet = null;
    	//xml
    	if (inputData.getEntrySet() != null){
			try {
				psidev.psi.mi.xml254.jaxb.EntrySet jaxbEntrySet = inputData.getEntrySet();
				List<?> entries = jaxbEntrySet.getEntry();
				Iterator<?> it= entries.iterator();
				EntrySetConverter converter = new EntrySetConverter();
				entrySet = converter.fromJaxb(jaxbEntrySet);
			} catch (ConverterException e) {
				e.printStackTrace();
			    throw new InvalidArgumentException("Problem converting from the psi mi 2.5.4 model to the generic one", new PsiscoreFault());
			}
    	}
    	//mitab
		if (entrySet == null && inputData.getMitab() != null){
			Collection<BinaryInteraction> binaryInteractions = null;
			try{
				entrySet = parseMitab(inputData.getMitab(), false);
			}catch(InvalidArgumentException e){
				entrySet = parseMitab(inputData.getMitab(), true);
			}
		}
		
		if (entrySet == null){
			throw new InvalidArgumentException("No valid input data (MITAB or PSIMI XML) detected", new PsiscoreFault());
		}
		return entrySet;
    }
    

	

	/**
	 * 
	 * @param binaryInteractions
	 * @return
	 */
    protected String createMitabResults(List<BinaryInteraction> binaryInteractions) {
		MitabDocumentDefinition docDef = new MitabDocumentDefinition();
		StringBuilder sb = new StringBuilder(binaryInteractions.size() * 512);
	
		for (BinaryInteraction binaryInteraction : binaryInteractions) {
			String binaryInteractionString = docDef.interactionToString(binaryInteraction);
			sb.append(binaryInteractionString);
			sb.append(NEW_LINE);
		}
		return sb.toString();
	}


	/**
	 * 
	 * @param binaryInteractions
	 * @return
	 * @throws PsiscoreException
	 */
	protected psidev.psi.mi.xml254.jaxb.EntrySet createEntrySet(List<BinaryInteraction> binaryInteractions) throws PsiscoreException {
		if (binaryInteractions.isEmpty()) {
			return new psidev.psi.mi.xml254.jaxb.EntrySet();
		}
	
		Tab2Xml tab2Xml = new Tab2Xml();
		try {
			psidev.psi.mi.xml.model.EntrySet mEntrySet = tab2Xml.convert(binaryInteractions);
	
			EntrySetConverter converter = new EntrySetConverter();
			converter.setDAOFactory(new InMemoryDAOFactory());
	
			return converter.toJaxb(mEntrySet);

			
		} catch (Exception e) {
			throw new PsiscoreException("Problem converting results to PSI-MI XML", new PsiscoreFault());
		}
	}
	

}

