package org.hupo.psi.mi.psiscore.ws;

import org.hupo.psi.mi.psiscore.PsiscoreException;

/**
 * Listener for the outcome of a scoring job
 * @author Hagen Blankenburg
 *
 */
public interface ScoringListener{
   
	public void noScoresAdded(ScoringParameters params);
   
	public void scoresAdded(ScoringParameters params);
	
	public void errorOccured(ScoringParameters params);
	
	public void comeBackLater();
   
   
}
