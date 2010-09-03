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
package org.hupo.psi.mi.psiscore.util;

import org.hupo.psi.mi.psiscore.*;
import org.hupo.psi.mi.psiscore.model.PsiscoreInput;

import psidev.psi.mi.xml.PsimiXmlReader;
import psidev.psi.mi.xml.PsimiXmlReaderException;
import psidev.psi.mi.xml.PsimiXmlWriter;
import psidev.psi.mi.xml.PsimiXmlWriterException;
import psidev.psi.mi.xml.model.Entry;

import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;

import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;

import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.tab.converter.xml2tab.Xml2Tab;
import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.builder.MitabDocumentDefinition;

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
import java.util.Iterator;
import java.util.List;


/**
 * Utility class providing many converter functions from and to PSI MI tab and XML
 * 
 * @author hagen (mpi-inf,mpg)
 * @version $Id$
 */

public class PsiTools{
	public static final String RETURN_TYPE_XML25 = "psi-mi/xml25";
	public static final String RETURN_TYPE_MITAB25 = "psi-mi/tab25";
	private static final String NEW_LINE = System.getProperty("line.separator");

    
   /**
    * checks if a valid XML or MITAB has been submitted. if both or non are existing, an exception will be thrown
    * @param inputData
    * @return
    * @throws InvalidArgumentException
    * @throws PsiscoreException
    */
    public static final String getInputFormat(ResultSet inputData) throws InvalidArgumentException, PsiscoreException{
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
    
    
    
    public static final String getInputFormat(PsiscoreInput inputData) throws InvalidArgumentException, PsiscoreException{
    	String inputFormat = null;
    	if (inputData.xmlUsed() && inputData.mitabUsed()){
    		throw new InvalidArgumentException("Only use PSI-MI XML or PSI-MI TAB, not both ", new PsiscoreFault());
    	}
    	else if (inputData.xmlUsed()){
			inputFormat = RETURN_TYPE_XML25;
		}else if (inputData.mitabUsed()){
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
    public static final psidev.psi.mi.xml.model.EntrySet getEntrySetFromMitabString(String mitab) throws PsiscoreException, InvalidArgumentException{
    	
    	Collection<BinaryInteraction> binaryInteractions = getBinaryInteractionsFromMitab(mitab);
    	return getEntrySetFromBinaryInteractions(binaryInteractions);
    }
    
    
    public static final psidev.psi.mi.xml.model.EntrySet getEntrySetFromBinaryInteractions(Collection<BinaryInteraction> interactions) throws PsiscoreException{
    	
    	psidev.psi.mi.xml.model.EntrySet entrySet = null;
    	
		try {
			Tab2Xml tab2xml = new Tab2Xml();
		    entrySet = tab2xml.convert(interactions);
 
		} catch (IllegalAccessException e) {
			throw new PsiscoreException("There was a problem trying to parse the MITAB file.", new PsiscoreFault());
		} catch (XmlConversionException e) {
			throw new PsiscoreException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());
		} 
		
		return entrySet;
    }
    
    public static final Collection<BinaryInteraction> getBinaryInteractionsFromMitab(String mitab) throws PsiscoreException, InvalidArgumentException{
    	PsimiTabReader reader = new PsimiTabReader(false);
    	Collection<BinaryInteraction> binaryInteractions = null;
    	psidev.psi.mi.xml.model.EntrySet entrySet = null;
    	
		try {
			binaryInteractions = reader.read(mitab);
		} catch (ConverterException e) {
			reader = new PsimiTabReader(true);
			try {
				binaryInteractions = reader.read(mitab);
			} catch (IOException e1) {
				throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());
			} catch (ConverterException e1) {
				throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());
			}

		}catch (IOException e) {
			throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());		
		}
		
		
		Tab2Xml tab2xml = new Tab2Xml();
	    try {
			entrySet = tab2xml.convert(binaryInteractions);
		} catch (IllegalAccessException e) {
			throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());
		} catch (XmlConversionException e) {
			throw new InvalidArgumentException("The MITAB file appears to be invalid. Please make sure it is valid before trying to upload it again. The parser said: " + e.getMessage(), new PsiscoreFault());		}
		
		return binaryInteractions;
    }
    
    
    
    
    /**
     * Get the XML  representation of the input data. 
     * @param inputData
     * @return
     * @throws InvalidArgumentException 
     * @throws PsiscoreException 
     */
    public static final psidev.psi.mi.xml.model.EntrySet getEntrySetFromInput(ResultSet inputData) throws InvalidArgumentException, PsiscoreException{
    	psidev.psi.mi.xml.model.EntrySet entrySet = null;
    	//xml
    	if (inputData.getEntrySet() != null){
			try {
				psidev.psi.mi.xml254.jaxb.EntrySet jaxbEntrySet = inputData.getEntrySet();
				List<?> entries = jaxbEntrySet.getEntries();
				Iterator<?> it= entries.iterator();
				EntrySetConverter converter = new EntrySetConverter();
				converter.setDAOFactory(new InMemoryDAOFactory());
				entrySet = converter.fromJaxb(jaxbEntrySet);
			} catch (ConverterException e) {
				e.printStackTrace();
			    throw new InvalidArgumentException("Problem converting from the PSI MI 2.5.4 model to the generic one", new PsiscoreFault());
			}
    	}
    	//mitab
		if (entrySet == null && inputData.getMitab() != null){
			entrySet = getEntrySetFromMitabString(inputData.getMitab());
		}
		
		if (entrySet == null){
			throw new InvalidArgumentException("No valid input data (MITAB or PSIMI XML) detected", new PsiscoreFault());
		}
		
		return entrySet;
    }
    
    
    
    /**
     * Convert ResultSet input into either XML or MITAB representation
     * @param inputData
     * @return
     * @throws InvalidArgumentException
     * @throws PsiscoreException
     */
    public static final PsiscoreInput getPsiscoreInput(ResultSet inputData) throws InvalidArgumentException, PsiscoreException{
    	PsiscoreInput input = new PsiscoreInput();
    	
    	//xml
    	if (inputData.getEntrySet() != null){
			try {
				psidev.psi.mi.xml254.jaxb.EntrySet jaxbEntrySet = inputData.getEntrySet();
				List<?> entries = jaxbEntrySet.getEntries();
				Iterator<?> it= entries.iterator();
				EntrySetConverter converter = new EntrySetConverter();
				converter.setDAOFactory(new InMemoryDAOFactory());
				EntrySet entrySet = converter.fromJaxb(jaxbEntrySet);
				input.setXmlEntySet(entrySet);
				input.setPrimaryInput(RETURN_TYPE_XML25);
			} catch (ConverterException e) {
				e.printStackTrace();
			    throw new InvalidArgumentException("Problem converting from the PSI-MI 2.5.4 model to the generic one", new PsiscoreFault());
			}
    	}
    	//mitab
		if (!input.xmlUsed()&& inputData.getMitab() != null){
			Collection<BinaryInteraction> interactions = getBinaryInteractionsFromMitab(inputData.getMitab());
			input.setMitabInteractions(interactions);
			input.setPrimaryInput(RETURN_TYPE_MITAB25);
			
		}
		
		if (!input.xmlUsed() && !input.mitabUsed()){
			throw new InvalidArgumentException("No valid input data (MITAB or PSIMI XML) detected", new PsiscoreFault());
		}
		
		return input;
    }
    
	

	/**
	 * 
	 * @param binaryInteractions
	 * @return
	 */
    public static final String createMitabResults(Collection<BinaryInteraction> binaryInteractions) {
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
    public static final psidev.psi.mi.xml254.jaxb.EntrySet createXmlEntrySet(List<BinaryInteraction> binaryInteractions) throws PsiscoreException {
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
			e.printStackTrace();
			throw new PsiscoreException("Problem converting results to PSI-MI XML", e);
		}
	}
    
    
    public static final ResultSet getResultSet(psidev.psi.mi.xml.model.EntrySet entrySet, String returnFormat) throws PsiscoreException{
		ResultSet rs = new ResultSet();
		if (returnFormat.equals(RETURN_TYPE_XML25)){
			try {
				EntrySetConverter converter = new EntrySetConverter();
				converter.setDAOFactory(new InMemoryDAOFactory());
				rs.setEntrySet(converter.toJaxb(entrySet));
			} catch (ConverterException e) {
				e.printStackTrace();
			    throw new PsiscoreException("Problem when converting the EntrySet to XML ", new PsiscoreFault());
			}
		}
		else if (returnFormat.equals(RETURN_TYPE_MITAB25)){
			try {
				Xml2Tab xml2tab = new Xml2Tab();
				Collection<BinaryInteraction> binaryInteractions = xml2tab.convert(entrySet);

				String mitab = createMitabResults((List<BinaryInteraction>) binaryInteractions);
		    	rs.setMitab(mitab);
			
			} catch (Exception e) {
				e.printStackTrace();
			    throw new PsiscoreException("Problem converting EntrySet to Mitab ", new PsiscoreFault());
			}
		}
		return rs;
	}
    
    
    public static final ResultSet getResultSet(PsiscoreInput inputData) throws PsiscoreException{
		ResultSet rs = new ResultSet();
		if (inputData.xmlUsed() && !inputData.getPrimaryInput().equals(RETURN_TYPE_MITAB25)){
			try {
				EntrySetConverter converter = new EntrySetConverter();
				converter.setDAOFactory(new InMemoryDAOFactory());
				rs.setEntrySet(converter.toJaxb(inputData.getXmlEntySet()));

			} catch (ConverterException e) {
				//e.printStackTrace();
			    throw new PsiscoreException("Problem when converting the EntrySet to XML ", new PsiscoreFault());
			}
		}
		else if (inputData.mitabUsed()){
			rs.setMitab(createMitabResults( (List<BinaryInteraction>) inputData.getMitabInteractions()) );
		}else{
			throw new PsiscoreException("No valid data in stored input detected.", new PsiscoreFault());
		}
			
		return rs;
	}
    

    public static final void addConfidencesToPsiscoreInput(PsiscoreInput oldInputData, PsiscoreInput scoredInput) throws PsiscoreException{
    	if (oldInputData.getPrimaryInput().equals(RETURN_TYPE_XML25) && scoredInput.xmlUsed()){
    		//System.out.println("Will add XML confidences to XML model");
    		addConfidencesToEntrySet(oldInputData.getXmlEntySet(), scoredInput.getXmlEntySet());
    	}else if (oldInputData.getPrimaryInput().equals(RETURN_TYPE_MITAB25) && scoredInput.xmlUsed()){
    		//System.out.println("Will add XML confidences to MITAB model");
    		addConfidencesToBinaryInteractions(oldInputData.getMitabInteractions(), scoredInput.getXmlEntySet());
    	}else if (oldInputData.getPrimaryInput().equals(RETURN_TYPE_MITAB25) && scoredInput.mitabUsed()){
    		//System.out.println("Will add MITAB confidences to MITAB model");
    		addConfidencesToBinaryInteractions(oldInputData.getMitabInteractions(), scoredInput.getMitabInteractions());
    	}else if (oldInputData.getPrimaryInput().equals(RETURN_TYPE_XML25) && scoredInput.mitabUsed()){
    		//System.out.println("Will add MITAB confidences to XML model");
    		addConfidencesToEntrySet(oldInputData.getXmlEntySet(), scoredInput.getMitabInteractions());
    	}else{
    		throw new PsiscoreException("Server error while trying to add confidences", new PsiscoreFault());
    	}
    	 
    }

    
    public static final void addConfidencesToEntrySet(psidev.psi.mi.xml.model.EntrySet oldEntrySet, psidev.psi.mi.xml.model.EntrySet newEntrySet) throws PsiscoreException{
    	Iterator<Entry> oldIt = oldEntrySet.getEntries().iterator();
		Iterator<Entry> newIt = newEntrySet.getEntries().iterator();
		// go through all new interactions and see if we have to add any confidences
		while (newIt.hasNext()){
			if (!oldIt.hasNext()){
				throw new PsiscoreException("Trying to match different interactions", new PsiscoreFault());
			}
			Entry newEntry = newIt.next();
			Entry oldEntry = oldIt.next();
			Collection<Interaction> newInteractions = newEntry.getInteractions();
			Collection<Interaction> oldInteractions = oldEntry.getInteractions();
			
			Iterator<Interaction> newIntIt = newInteractions.iterator();
			Iterator<Interaction> oldIntIt = oldInteractions.iterator();
			while (newIntIt.hasNext()){
				Interaction newInteraction = newIntIt.next();
				Interaction oldInteraction = oldIntIt.next();
				if (!sameInteraction(newInteraction, oldInteraction)){
					throw new PsiscoreException("Trying to match different interactions", new PsiscoreFault());
				}
				addConfidenceCollectionToCollection(oldInteraction.getConfidences(), newInteraction.getConfidences());
			}
		}
    }
    
    public static final void addConfidencesToEntrySet(psidev.psi.mi.xml.model.EntrySet oldEntrySet, Collection<BinaryInteraction> newInteractions) throws PsiscoreException{
    	Iterator<Entry> oldIt = oldEntrySet.getEntries().iterator();

		while (oldIt.hasNext()){
			Entry oldEntry = oldIt.next();
			
			Collection<Interaction> oldInteractions = oldEntry.getInteractions();
			Iterator<Interaction> oldIntIt = oldInteractions.iterator();
			Iterator<BinaryInteraction> newIntIt = newInteractions.iterator();
			
			while (newIntIt.hasNext()){
				Interaction oldInteraction = oldIntIt.next();
				BinaryInteraction newInteraction = newIntIt.next();
				
				if (!sameInteraction(newInteraction, newInteraction)){
					throw new PsiscoreException("Trying to match different interactions", new PsiscoreFault());
				}
				addConfidenceListToCollection(oldInteraction.getConfidences(), newInteraction.getConfidenceValues());
			}
		}
    }
    
    public static final void addConfidencesToBinaryInteractions(Collection<BinaryInteraction> oldInteractions, Collection<BinaryInteraction> newInteractions) throws PsiscoreException{
		Iterator<BinaryInteraction> newIntIt = newInteractions.iterator();
		Iterator<BinaryInteraction> oldIntIt = oldInteractions.iterator();
		while (newIntIt.hasNext()){
			BinaryInteraction newInteraction = newIntIt.next();
			BinaryInteraction oldInteraction = oldIntIt.next();
			
			if (!sameInteraction(newInteraction, oldInteraction)){
				throw new PsiscoreException("Trying to match different binary interactions", new PsiscoreFault());
			}
			addConfidenceListToList(oldInteraction.getConfidenceValues(), newInteraction.getConfidenceValues());
		}
		
    }
    
    
    public static final void addConfidencesToBinaryInteractions(Collection<BinaryInteraction> oldInteractions, psidev.psi.mi.xml.model.EntrySet newEntrySet) throws PsiscoreException{
    	Iterator<Entry> newIt = newEntrySet.getEntries().iterator();
		// go through all new interactions and see if we have to add any confidences
		while (newIt.hasNext()){
			Entry newEntry = newIt.next();
			
			Collection<Interaction> newInteractions = newEntry.getInteractions();
			Iterator<Interaction> newIntIt = newInteractions.iterator();
			
			Iterator<BinaryInteraction> oldIntIt = oldInteractions.iterator();
			while (newIntIt.hasNext()){
				Interaction newInteraction = newIntIt.next();
				BinaryInteraction binaryInteraction = oldIntIt.next();
				
				if (!sameInteraction(newInteraction, binaryInteraction)){
					throw new PsiscoreException("Trying to match different interactions", new PsiscoreFault());
				}
				addConfidenceCollectionToList(binaryInteraction.getConfidenceValues(), newInteraction.getConfidences());
			}
		}

    }
    

    
    
    
    
    public static void addConfidenceCollectionToList(List<psidev.psi.mi.tab.model.Confidence> oldConfidences, Collection<Confidence> newConfidences){
    	Collection<psidev.psi.mi.tab.model.Confidence> trulyNewConfidences = new ArrayList<psidev.psi.mi.tab.model.Confidence>();
    	Iterator<Confidence> newConfIt = newConfidences.iterator();
		while(newConfIt.hasNext()){
			boolean haveConfidence = false;
			Confidence newConfidence = newConfIt.next();
			Iterator<psidev.psi.mi.tab.model.Confidence> oldConfIt = oldConfidences.iterator();
    		while(oldConfIt.hasNext()){
    			psidev.psi.mi.tab.model.Confidence oldConfidence = oldConfIt.next();
    			if (sameConfidence(oldConfidence, newConfidence )){
    				haveConfidence = true;
   			}
    		}
    		if (!haveConfidence){
    			trulyNewConfidences.add(ConfidenceGenerator.convertConfidence(newConfidence));
    		}

		}
		oldConfidences.addAll(trulyNewConfidences);
    }
    
    public static void addConfidenceListToCollection(Collection<Confidence> oldConfidences, List<psidev.psi.mi.tab.model.Confidence> newConfidences){
    	// first iterate through all old confidences and see if they are also part of the
    	// new confidences. if so, remove them from teh new confidences
    	Collection<Confidence> trulyNewConfidences = new ArrayList<Confidence>();
    	Iterator<psidev.psi.mi.tab.model.Confidence> newConfIt = newConfidences.iterator();
		while(newConfIt.hasNext()){
			boolean haveConfidence = false;
			psidev.psi.mi.tab.model.Confidence newConfidence = newConfIt.next();
    		Iterator<Confidence> oldConfIt = oldConfidences.iterator();
    		while(oldConfIt.hasNext()){
    			Confidence oldConfidence = oldConfIt.next(); 
    			if (!sameConfidence(newConfidence, oldConfidence)){
    				haveConfidence = true;
    			}
    		}
    		if (!haveConfidence){
    			trulyNewConfidences.add(ConfidenceGenerator.convertConfidence(newConfidence));
    		}
		}
		oldConfidences.addAll(trulyNewConfidences);

    }
    
    
    public static void addConfidenceCollectionToCollection(Collection<Confidence> oldConfidences, Collection<Confidence> newConfidences){
		// first iterate through all old confidences and see if they are also part of the
    	// new confidences. if so, remove them from teh new confidences
    	Collection<Confidence> trulyNewConfidences = new ArrayList<Confidence>();
    	Iterator<Confidence> newConfIt = newConfidences.iterator();
		while(newConfIt.hasNext()){
			boolean haveConfidence = false;
    		Confidence newConfidence = newConfIt.next();
    		Iterator<Confidence> oldConfIt = oldConfidences.iterator();
    		while(oldConfIt.hasNext()){
    			Confidence oldConfidence = oldConfIt.next(); 
    			if (!sameConfidence(oldConfidence, newConfidence)){
    				haveConfidence = true;
    			}
    		}
    		if (!haveConfidence){
    			trulyNewConfidences.add(newConfidence);
    		}
		}
		oldConfidences.addAll(trulyNewConfidences);
    }
    
    
    public static void addConfidenceListToList(List<psidev.psi.mi.tab.model.Confidence> oldConfidences, List<psidev.psi.mi.tab.model.Confidence> newConfidences){
		// first iterate through all old confidences and see if they are also part of the
    	// new confidences. if so, remove them from teh new confidences
    	Collection<psidev.psi.mi.tab.model.Confidence> trulyNewConfidences = new ArrayList<psidev.psi.mi.tab.model.Confidence>();
    	Iterator<psidev.psi.mi.tab.model.Confidence> newConfIt = newConfidences.iterator();
		while(newConfIt.hasNext()){
			boolean haveConfidence = false;
			psidev.psi.mi.tab.model.Confidence newConfidence = newConfIt.next();
			Iterator<psidev.psi.mi.tab.model.Confidence> oldConfIt = oldConfidences.iterator();
    		while(oldConfIt.hasNext()){
    			psidev.psi.mi.tab.model.Confidence oldConfidence = newConfIt.next();
    			if (!sameConfidence(oldConfidence, newConfidence )){
    				haveConfidence = true;
    			}
    		}
    		if (!haveConfidence){
    			trulyNewConfidences.add(newConfidence);
    		}
		}
		oldConfidences.addAll(trulyNewConfidences);
    }
    
    
      
    /**
     * Compare two interactions to see if they refer to the same entity. 
     * TODO make the comparison more sophisticated, not only check their names
     * @param xmlInteraction
     * @param binaryInteraction
     * @return
     */
    public static boolean sameInteraction(Interaction xmlInteraction, BinaryInteraction binaryInteraction){
    	// TODO fix me!!
    	return true;
    	
    }
    
    
    /**
     * Compare to interactions from the XML objcet model to see if they refer to the same entity
     * Simply comparing the complete interacton objects can fail, as their id can change.
     * TODO make this comparison more sophiostcated
     * @param xmlInteractionA
     * @param xmlInteractionB
     * @return
     */
    public static boolean sameInteraction(Interaction xmlInteractionA, Interaction xmlInteractionB){
    	if (xmlInteractionA.equals(xmlInteractionB)){
    		return true;
    	}else{
    		return false;
    	}
    		
    }
    
    public static boolean sameInteraction(BinaryInteraction binaryInteractionA, BinaryInteraction binaryInteractionB){
    	/*Collection<CrossReference> referencesA1 = binaryInteractionA.getInteractorA().getIdentifiers();
    	Collection<CrossReference> referencesA2 = binaryInteractionA.getInteractorB().getIdentifiers();
    	Collection<CrossReference> referencesB1 = binaryInteractionB.getInteractorA().getIdentifiers();
    	Collection<CrossReference> referencesB2 = binaryInteractionB.getInteractorB().getIdentifiers();
    	Iterator<CrossReference> a1It = referencesA1.iterator();
    	while(a1It.hasNext()){
    		if(a1It.next()..getDatabase())
    		*/
    	if (binaryInteractionA.equals(binaryInteractionB)){
    		return true;
    	}else{
    		return false;
    	}
    }
   
    
    /**
     *Compare two confidences to see if they refer to the same entity
     * @param xmlConfidenceA
     * @param xmlConfidenceB
     * @return
     */
    public static boolean sameConfidence(Confidence xmlConfidenceA, Confidence xmlConfidenceB){
    	if (xmlConfidenceA.equals(xmlConfidenceB)){
    		return true;
    	}else{
    		return false;
    	}
    }
    
    
    /**
     *Compare two confidences to see if they refer to the same entity
     * @param xmlConfidenceA
     * @param xmlConfidenceB
     * @return
     */
    public static boolean sameConfidence(psidev.psi.mi.tab.model.Confidence tabConfidence, Confidence xmlConfidence){
    	if (tabConfidence.getValue().equalsIgnoreCase(xmlConfidence.getValue()) &&
    		tabConfidence.getText().equalsIgnoreCase(xmlConfidence.getUnit().getNames().getFullName())){
    		return true;
    	}else{
    		return false;
    	}
    }
    
    /**
     * Compare two confidnces to see in the refer to the same entity
     * @param tabConfidenceA
     * @param tabConfidenceB
     * @return
     */
    public static boolean sameConfidence(psidev.psi.mi.tab.model.Confidence tabConfidenceA, psidev.psi.mi.tab.model.Confidence tabConfidenceB){
    	if (tabConfidenceA.getValue().equalsIgnoreCase(tabConfidenceB.getValue()) &&
    		tabConfidenceA.getText().equalsIgnoreCase(tabConfidenceB.getText()) &&
    		tabConfidenceA.getType().equalsIgnoreCase(tabConfidenceB.getType())){
    		return true;
    	}else{
    		return false;
    	}
    }
    
    

    public static String readFromUrl(String urlString) throws PsiscoreException, InvalidArgumentException{
    	StringBuffer fBuf = new StringBuffer(); 
    	try {
			URL url = new URL(urlString);
			InputStream in = url.openStream ();
			BufferedReader dis = new BufferedReader (new InputStreamReader (in));
			fBuf = new StringBuffer() ;
			String line;
			while (	( line = dis.readLine ()) != null) {
		        fBuf.append (line + NEW_LINE);
			}
			in.close();
    	}catch (IOException e) {
	       throw new InvalidArgumentException("File not found", new PsiscoreFault(), e);
	    }
	    return fBuf.toString();
    }
    
    
    public static String readMitab(String path) throws PsiscoreException{
    	StringBuffer mitab = new StringBuffer();
    	try{
    		FileInputStream fstream;
			fstream = new FileInputStream(path);
			
    	    DataInputStream in = new DataInputStream(fstream);
    	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    	    String strLine;
    	    while ((strLine = br.readLine()) != null)   {
				mitab.append(strLine);
				mitab.append("\n");
			}
			in.close();
			
    	}catch (FileNotFoundException e1){
    		throw new PsiscoreException("The file you specified does not exists.", new PsiscoreFault());
    	}catch (IOException e) {
    		throw new PsiscoreException("The file you specified cannot be read.", new PsiscoreFault());
    	}
    	
    return mitab.toString();
    }
    
    public static void writeEntrySetToFile(EntrySet entrySet, String path) throws PsiscoreException{
	    PsimiXmlWriter writer = new PsimiXmlWriter();
		try {
			writer.write(entrySet, new File(path));
		} catch (PsimiXmlWriterException e) {
			throw new PsiscoreException("Cannot write XML content to file ", new PsiscoreFault(), e);
			
		}
    }
    
    public static void writeXmlEntrySetToFile(psidev.psi.mi.xml254.jaxb.EntrySet jaxbEntrySet, String path) throws PsiscoreException{
    	EntrySetConverter converter = new EntrySetConverter();
		converter.setDAOFactory(new InMemoryDAOFactory());
		EntrySet entrySet;
		try {
			entrySet = converter.fromJaxb(jaxbEntrySet);
		} catch (ConverterException e1) {
			throw new PsiscoreException("Cannot convert XML model to PSI MI XML model ", new PsiscoreFault(), e1);
		}
		writeEntrySetToFile(entrySet, path);
    }
        
    
    public static psidev.psi.mi.xml254.jaxb.EntrySet readXmlEntrySetFromFile(String path) throws PsiscoreException{
    	psidev.psi.mi.xml.model.EntrySet entrySet = readEntrySetFromFile(path);
    	psidev.psi.mi.xml254.jaxb.EntrySet jaxbEntrySet = null;
    	EntrySetConverter converter = new EntrySetConverter();
		converter.setDAOFactory(new InMemoryDAOFactory());
		try {
			jaxbEntrySet = converter.toJaxb(entrySet);
		} catch (ConverterException e) {
			throw new PsiscoreException("Cannot convert XML model to PSI MI XML model ", new PsiscoreFault(), e);
		}
		
    	return jaxbEntrySet;

    }
    
    public static psidev.psi.mi.xml.model.EntrySet readEntrySetFromFile(String path) throws PsiscoreException{
    	psidev.psi.mi.xml.model.EntrySet entrySet = null;
    	InputStream inStream = PsiTools.class.getResourceAsStream(path);
    	PsimiXmlReader reader = new PsimiXmlReader();
    	
    	try {
    		entrySet = reader.read(new File(path) );
		} catch (PsimiXmlReaderException e2) {
			// TODO Auto-generated catch block
			throw new PsiscoreException("Cannot parse XML file", new PsiscoreFault(), e2);
		}

		return entrySet;
    }
    
    public static void main(String[] args) throws PsiscoreException, InvalidArgumentException{
    	//System.out.println(readFromUrl("http://tutorials.jenkov.com/java-networking/urls-local-files.html"));
    	System.out.println(readFromUrl("file:D:/mitab.txt"));
    	
	    
    }
   
}

