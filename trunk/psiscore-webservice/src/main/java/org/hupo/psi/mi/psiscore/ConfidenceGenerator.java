package org.hupo.psi.mi.psiscore;

import org.hupo.psi.mi.psiscore.ws.config.PsiscoreServerProperties;

import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.Names;
import psidev.psi.mi.xml.model.Unit;

public class ConfidenceGenerator {
	
	
	 /**
     * Create a new confidence object to be added to the EntrySet representation  
     * @param unitShortLabel
     * @param unitFullName
     * @param value
     * @return
     */
    public static Confidence getNewEntrySetConfidence(String unitShortLabel, String unitFullName, String value){
		//System.out.println("New confidence:" + unitShortLabel + unitFullName + value);
		Confidence confidence = new Confidence();
		Unit unit = new Unit();
		Names names = new Names();
		names.setShortLabel(unitShortLabel);
		names.setFullName(unitFullName);
		unit.setNames(names);
		confidence.setUnit(unit);
		confidence.setValue(value);
		return confidence;
    	
    }
	
    /**
     * If no fullName of the confidence unit is provided, use the shortLabel as substitution
     * @param unitShortLabel
     * @param value
     * @return
     */
	public static Confidence getNewEntrySetConfidence(String unitShortLabel, String value){
		return getNewEntrySetConfidence(unitShortLabel, unitShortLabel, value);
	}
	
	/**
     * Get a new tab confidence with the values of the xml confidence provided
     * @param xmlConfidence
     * @return
     */
    public static psidev.psi.mi.tab.model.Confidence convertConfidence(Confidence xmlConfidence){
    	psidev.psi.mi.tab.model.Confidence tabConfidence = new psidev.psi.mi.tab.model.ConfidenceImpl();
		tabConfidence.setText(xmlConfidence.getUnit().getNames().getFullName());
		tabConfidence.setType(xmlConfidence.getUnit().getNames().getShortLabel());
		tabConfidence.setValue(xmlConfidence.getValue());
		return tabConfidence;
    }
    
    /**
     * Get a new xml confidence with the values provided in the tab confidcne
     * @param tabConfidence
     * @return
     */
    public static Confidence convertConfidence(psidev.psi.mi.tab.model.Confidence tabConfidence){
    	Confidence xmlConfidence = new Confidence();
		Unit unit = new Unit();
		Names names = new Names();
		names.setShortLabel(tabConfidence.getType());
		names.setFullName(tabConfidence.getText());
		unit.setNames(names);
		xmlConfidence.setUnit(unit);
		xmlConfidence.setValue(tabConfidence.getValue());
		return xmlConfidence;

    }
}
