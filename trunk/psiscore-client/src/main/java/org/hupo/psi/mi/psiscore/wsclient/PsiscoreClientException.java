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

/**
 * Generic exception for the client implementations.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author hagen (mpi-inf,mpg)
 */
public class PsiscoreClientException extends Exception {
	public static final long serialVersionUID = 20100723122326L;
    private org.hupo.psi.mi.psiscore.PsiscoreFault psiscoreClientException;
        
    public PsiscoreClientException() {
        super();
    }

    public PsiscoreClientException(String message) {
        super(message);
    }

    
    public PsiscoreClientException(String message, org.hupo.psi.mi.psiscore.PsiscoreFault psiscoreException) {
        super(message);
        this.psiscoreClientException = psiscoreException;
    }
    
    public PsiscoreClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public PsiscoreClientException(Throwable cause) {
        super(cause);
    }
    
    public PsiscoreClientException(String message, org.hupo.psi.mi.psiscore.PsiscoreFault psiscoreException, Throwable cause) {
        super(message, cause);
        this.psiscoreClientException = psiscoreException;
    }

    public org.hupo.psi.mi.psiscore.PsiscoreFault getFaultInfo() {
        return this.psiscoreClientException;
    }
}

