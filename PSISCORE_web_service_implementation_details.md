In this document you will find all the information required to implement your own score calculator with the help of the Java reference implementation. If you want more details on how to compile the reference implementation and how to deploy it, you might want to read [Creating\_a\_new\_PSISCORE\_server](Creating_a_new_PSISCORE_server.md).

By using Java inheritance, we try to hide as much implementation details from you as possible. In essence, all you need to do in order to get a working score calculator is to create a class that implements two methods, one that performs the scoring of an individual interaction object, and one that describes what sort of scoring methods your calculator offers.

If you just want to develop a PSISCORE server class without knowing all the background details, read the [Score\_Calculator\_quick\_start](Score_Calculator_quick_start.md). If you want to change other aspects of your server as well, you can find more implementation details in **All the details** below.


---


# All the details #

This section contains more information on the class structure of the PSISCORE server reference implementation. The information found here might be useful if you want to change the reference implementation, for instance, for using specific caching or threading techniques.

## `ScoreCalculator` ##
A `ScoreCalculator` is the abstract interface for all calculators, defining the basic methods every scoring calculator has to have. The PSISCORE reference implementation is based on Java threading, therefore, `ScoreCalculator` extends the `Thread` class. Each request will be handled by a `ScoreCalculator` thread and the outcome of this thread will be handled by listeners.
The two important methods defined by the `ScoreCalculator` are `calculateScores` and `getSupportedScoringMethods`. The former takes an `EntrySet` (the object representation of the PSI-MI input data) and returns the same `EntrySet` plus the potential confidence scores calculated. The latter returns a list of `AlgorithmDescriptor`s, constructs that describe each scoring algorithms offered by a server.
If you want to implement your own scoring class, you do not need to make any changes to the `ScoreCalculator`.

## `AbstractScoreCalculator` ##
The `AbstractScoreCalculator` extends the `ScoreCalculator` interface. The `AbstractScoreCalculator` implements the `run` method that is required for every Java `Thread`. This method calls the `calculateScores` method that is responsible for scoring.
In the code snippet below, `calculateScores` will iterate through all interactions in the user input data (i.e. the `EntrySet`) and send each interaction to the `getScores` method that does the actual calculation:
```
Iterator<Entry> it = entrySet.getEntries().iterator();
while (it.hasNext()){
	Entry entry = it.next();
	Collection<Interaction> interactions = entry.getInteractions();
		for (Iterator<Interaction> interactionIt = interactions.iterator(); interactionIt.hasNext();){
			Interaction interaction = interactionIt.next();
			interaction = getScores(interaction);
		} 
	}
```
The `getScores` method of the `AbstractScoreCalculator` is an abstract method, which means that it has to be defined by a class that implements the `AbstractScoreCalculator`.
If your scoring routine only requires data from the particular interaction that is to be scored (e.g. the UniProtKB identifiers of the participating interactors), you only need to implement the `getScores` method (for more details on this, see the text above). If, however, you also require data from other parts of the input file (e.g. the experimental parameters provided in an PSI-MI XML file), you will need to make changes to the `calculateScores` method by overriding it in your particular score calculator.

## YourScoreCalculator ##
The last element in the score calculator inheritance line is your particular score calculator that will need to extend the `AbstractScoreCalculator`. See the above **Quick start** section for details on the methods in this class.

## `DefaultPsiscoreService` ##
The `DefaultPsiscoreService` is the central class that manages all score calculators and handles user requests and responses.