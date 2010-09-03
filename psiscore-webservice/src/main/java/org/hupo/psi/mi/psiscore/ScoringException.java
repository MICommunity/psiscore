
package org.hupo.psi.mi.psiscore;


public class ScoringException extends Exception {
    public static final long serialVersionUID = 20100821170844L;
    
    private org.hupo.psi.mi.psiscore.PsiscoreFault scoringException;

    public ScoringException() {
        super();
    }
    
    public ScoringException(String message) {
        super(message);
    }
    
    public ScoringException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScoringException(String message, org.hupo.psi.mi.psiscore.PsiscoreFault psiscoreException) {
        super(message);
        this.scoringException = psiscoreException;
    }

    public ScoringException(String message, org.hupo.psi.mi.psiscore.PsiscoreFault psiscoreException, Throwable cause) {
        super(message, cause);
        this.scoringException = psiscoreException;
    }

    public org.hupo.psi.mi.psiscore.PsiscoreFault getFaultInfo() {
        return this.scoringException;
    }
}
