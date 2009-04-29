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

import org.hupo.psi.mi.psiscore.*;
import org.hupo.psi.mi.psiscore.ws.config.PsiscoreConfig;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebParam;

/**
 * TODO write description of the class.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
public class DefaultPsiscoreService implements PsiscoreService{

    @Autowired
    private PsiscoreConfig config;

    public String getVersion() {
        return config.getVersion();
    }

    public QueryResponse getByQuery(@WebParam(name = "inputData",
            targetNamespace = "http://psi.hupo.org/mi/psiscore") ResultSet inputData)
            throws PsiscoreServiceException, NotSupportedTypeException, NotSupportedMethodException {
        return null;
    }
}
