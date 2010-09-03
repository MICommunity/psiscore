package org.hupo.psi.mi.psiscore.ws;

import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.PsiscoreException;

/**
 * Listener for the outcome of a scoring job
 * @author hagen (mpi-inf,mpg)
 *
 */
public interface ScoringListener{
   
	public void noScoresAdded(ScoringParameters params) throws PsiscoreException;
   
	public void scoresAdded(ScoringParameters params) throws PsiscoreException;
	
	public void errorOccured(ScoringParameters params);
	
	public void comeBackLater();
   
   
}
