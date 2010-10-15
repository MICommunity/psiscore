package org.hupo.psi.mi.psiscore.model;

import java.io.Serializable;
import java.util.Collection;

import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.PsiscoreException;
import org.hupo.psi.mi.psiscore.util.PsiTools;

import psidev.psi.mi.tab.model.BinaryInteraction;

public class PsiscoreInput implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4766563336687702037L;
	private String psiscoreId = null;
	private psidev.psi.mi.xml.model.EntrySet xmlEntySet = null;
	//private psidev.psi.mi.xml254.jaxb.EntrySet xmlEntySet = null;
	private Collection<BinaryInteraction> mitabInteractions = null;
	private String primaryInput = null; 
	
	public PsiscoreInput(){
		
	}
	
	public PsiscoreInput(String psiscoreId){
		this.psiscoreId = psiscoreId;
	}
	
	public boolean mitabUsed(){
		if (this.mitabInteractions != null){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean xmlUsed(){
		if (this.xmlEntySet != null){
			return true;
		}else{
			return false;
		}
	}
	
	

	/**
	 * @return the psiscoreId
	 */
	public String getPsiscoreId() {
		
		return psiscoreId;
	}

	/**
	 * @param psiscoreId the psiscoreId to set
	 */
	public void setPsiscoreId(String psiscoreId) {
		this.psiscoreId = psiscoreId;
	}



	/**
	 * @param xmlEntySet the xmlEntySet to set
	 * @throws PsiscoreException 
	 */
	public void setXmlEntySet(psidev.psi.mi.xml.model.EntrySet xmlEntySet){
		this.xmlEntySet = xmlEntySet;
		/*try {
			this.xmlEntySet = PsiTools.getInstance().entrySetXmlModelToJaxb(xmlEntySet);
		} catch (PsiscoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}


	/**
	 * @return the mitabInteractions
	 */
	public Collection<BinaryInteraction> getMitabInteractions() {
		return mitabInteractions;
		
	}


	/**
	 * @param mitabInteractions the mitabInteractions to set
	 */
	public void setMitabInteractions(Collection<BinaryInteraction> mitabInteractions) {
		this.mitabInteractions = mitabInteractions;
	}

	/**
	 * @return the xmlEntySet
	 */
	public psidev.psi.mi.xml.model.EntrySet getXmlEntySet() {
		/*try {
			return PsiTools.getInstance().entrySetJaxbToXmlModel(xmlEntySet);
		} catch (PsiscoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;*/
		return this.xmlEntySet;
	}

	/**
	 * @return the primaryInput
	 */
	public String getPrimaryInput() {
		return primaryInput;
	}

	/**
	 * @param primaryInput the primaryInput to set
	 */
	public void setPrimaryInput(String primaryInput) {
		this.primaryInput = primaryInput;
	}
	
	
	

}
