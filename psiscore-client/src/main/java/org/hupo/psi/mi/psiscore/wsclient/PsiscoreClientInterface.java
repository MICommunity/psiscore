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
package org.hupo.psi.mi.psiscore.wsclient;


import java.util.List;

import org.hupo.psi.mi.psiscore.AlgorithmDescriptor;
import org.hupo.psi.mi.psiscore.JobResponse;
import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.JobStillRunningException;
import org.hupo.psi.mi.psiscore.PsiscoreException;
import org.hupo.psi.mi.psiscore.QueryResponse;

/**
 * Defines the basic functions of a PSISCORE client
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author hagen (mpi-inf,mpg)
 */
public interface PsiscoreClientInterface {

	/**
	 * get the version of the client
	 * @return
	 * @throws PsiscoreClientException
	 */
	String getVersion() throws PsiscoreException;
	
	/**
	 * get a certain scoring job from the server
	 * @param jobId
	 * @return
	 * @throws PsiscoreClientException
	 * @throws InvalidArgumentException
	 * @throws JobStillRunningException
	 */
	QueryResponse getJob(String jobId) throws PsiscoreException, InvalidArgumentException, JobStillRunningException;
	
	/**
	 * get the supported data formats of the server
	 * @return
	 * @throws PsiscoreClientException
	 */
	List<String> getSupportedDataTypes() throws PsiscoreException;
	
	/**
	 * submit a scoring job.
	 * @param algorithmDescriptor
	 * @param inputData
	 * @param returnFormat
	 * @return
	 * @throws PsiscoreClientException
	 * @throws InvalidArgumentException
	 * @throws PsiscoreException
	 */
	JobResponse submitJob(java.util.List<org.hupo.psi.mi.psiscore.AlgorithmDescriptor> algorithmDescriptor, org.hupo.psi.mi.psiscore.ResultSet inputData, String returnFormat) throws PsiscoreClientException, InvalidArgumentException, PsiscoreException;
	
	/**
	 * the a description of all scoring algortihms the server provides
	 * @return
	 * @throws PsiscoreClientException
	 */
	List<AlgorithmDescriptor> getSupportedScoringMethods() throws PsiscoreException;
	
	/**
	 * get the status of a scoring job
	 * @param jobId
	 * @return
	 * @throws PsiscoreClientException
	 */
	String getJobStatus(String jobId) throws PsiscoreException, InvalidArgumentException;
	
}
