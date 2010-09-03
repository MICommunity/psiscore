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

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.http.gzip.GZIPOutInterceptor;
import org.apache.cxf.endpoint.Client;
import org.hupo.psi.mi.psiscore.DbRef;
import org.hupo.psi.mi.psiscore.PsiscoreService;
import org.hupo.psi.mi.psiscore.RequestInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract superclass for the PSISCORE clients.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author Hagen
 * @version $Id: AbstractPsiscoreClient.java 167 2009-08-12 09:40:05Z brunoaranda $
 */
public abstract class AbstractPsiscoreClient implements PsiscoreClientInterface {

    private PsiscoreService service;

    public AbstractPsiscoreClient(String serviceAddress) {
        this(serviceAddress, 5000L);
    }

    public AbstractPsiscoreClient(String serviceAddress, long timeout) {
    	
        if (serviceAddress == null) return;
        System.out.println("New Service : " + serviceAddress);
        ClientProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(PsiscoreService.class);
        factory.setAddress(serviceAddress);
        

        this.service = (PsiscoreService) factory.create();

        final Client client = ClientProxy.getClient(service);

        final HTTPConduit http = (HTTPConduit) client.getConduit();
        //client.getInInterceptors().add(new GZIPInInterceptor());
        //client.getOutInterceptors().add(new GZIPOutInterceptor());

        final HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        httpClientPolicy.setReceiveTimeout(timeout);
        httpClientPolicy.setAllowChunking(false);
        httpClientPolicy.setConnectionTimeout(1000L);
        httpClientPolicy.setAcceptEncoding("UTF-8");

        http.setClient(httpClientPolicy);

    }

    public PsiscoreService getService() {
        return service;
    }




}
