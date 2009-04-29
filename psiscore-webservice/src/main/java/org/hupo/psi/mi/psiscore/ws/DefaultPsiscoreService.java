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

import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.model.BinaryInteraction;

import java.util.Collection;
import java.io.IOException;

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

    @Autowired
    private ScoreCalculator calculator;

    public String getVersion() {
        return config.getVersion();
    }

    public QueryResponse getByQuery( ResultSet inputData)
            throws PsiscoreServiceException, NotSupportedTypeException, NotSupportedMethodException {

        EntrySet entrySet = null;

        // if the xml is used
        try {
            entrySet = new EntrySetConverter().fromJaxb(inputData.getEntrySet());
        } catch (ConverterException e) {
            throw new PsiscoreServiceException("Problem converting from the psi mi 2.5.4 model to the generic one", e);
        }

        // if the mitab is used
        PsimiTabReader reader = new PsimiTabReader(false);
        try {
            Collection<BinaryInteraction> binaryInteractions = reader.read(inputData.getMitab());

            Tab2Xml tab2xml = new Tab2Xml();
            entrySet = tab2xml.convert(binaryInteractions);
        } catch (Exception e) {
            throw new PsiscoreServiceException("Problem converting from PSI MITAB to EntrySet", e);
        }

        // TODO handle assynchronous calling to this, and assign a tracking code.
        calculator.calculateScores(entrySet);

        // TODO return tracking code and whatever
        return null;
    }
}
