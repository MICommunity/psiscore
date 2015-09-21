_By: Hagen Blankenburg, Jules Kerssemakers, Jens Hanssen, Javier De Las Rivas_

Version 1.0

# Method overview #
Short description of the methods provided by a PSISCORE service

## `getSupportedScoringMethods()` ##

Returns a servers available scoring algorithms and their respective requirements (parameters)

  * Takes:
    * Nothing
  * Returns:
    * List of AlgorithmDescriptors
      * identifier (String)
      * scoringTypeList (controlled vocabulary term)
> > > > Some examples/suggestions: (specific terms still open to change)
          * "structure based": if it takes into account structure data (e.g. PDB files)
          * "network based": if the overall layout is processed into the score
      * parameterSpecifierList: (type, ParameterLabel, Value)
      * score type / range (Controlled vocab. term)
        * "zero to one"
        * "zero to infinity"
  * Exceptions:
    * PsiScoreException


## `getSupportedDataTypes()` ##

Returns the input data types this server can handle (either PSI-MI XML, OR MITAB)

  * Takes
    * Nothing
  * Returns
    * SupportedDataTypeList (CV: MIME/type: “PSIMIXML25” / “PSIMITAB25”)
  * Exceptions:
    * PsiScoreException


## `submitJob()` ##
Request the server to score the interactions specified in the data section of the request with the algorithms listed.

  * Takes:
    * AlgorithmSpecifierList: List of:
      * What algorithm: Algorithm identifier → CV term
      * Lists of (parameterLabel,value pairs)
    * dataTAB (Empty if MIXML is used)
    * dataXML (Empty if MITAB is used)

  * Returns:
    * JobResponse
      * jobID: Identifier
      * pollingInterval (Guideline, X seconds, 1 indicates “come back 'now' ”, larger values: try again every X seconds)
  * Exceptions:
    * PsiScoreException
    * InvalidArgumentException
      * oversized input (large XML/TAB) gives InvalidArgument

## `getJobStatus()` ##
Request the status of the job, Is it retrievable yet?

  * Takes:
    * jobID
  * Returns:
    * jobStatus

> > > controlled vocabulary term, one of:
      * InQueue
      * Working
      * Finished
      * FinishedWithErrors
      * Expired
  * Exceptions:
    * PsiScoreException
    * InvalidArgumentException


## `getJob()` ##
Retrieves the finished job data.

  * Takes:
    * jobID
  * Returns:
    * data with added scores (XML or MITAB file)
    * Report: (list of strings)
  * Exceptions:
    * PsiScoreException
    * JobStillRunningException--> If the job specified is still in Queue, still running, or already expired from cache
    * InvalidArgumentException --> if the JobID specified is completely unknown, or otherwise malformed.

# General considerations #
  * Scores will be added, that is, existing scores will be preserved.
    * for MITAB, this means adding the new score to the confidence column
    * for MIXML, this means adding a new confidence element to the confidenceList within the interaction element.
  * Scores should be between 0.0 and 1.0 (Other types are allowed, but discouraged for reasons of normalization)

