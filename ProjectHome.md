# Motivation - Why do we need to assess the quality of molecular interactions? #
Molecular interactions are of fundamental importance for understanding the function of the cell. Consequently, more and more research groups are working in this domain, producing an ever-increasing amount of data on the interaction of molecules. Frameworks like the [HUPO-PSI](http://www.psidev.info/) initiated [PSICQUIC](http://code.google.com/p/psicquic/) are supporting researchers by making these distributed sets of interaction data easily accessible. However, they do not provide sufficient means for assessing the **reliability of the data**. As the experimental techniques with which molecular interactions can be determined largely differ, so does their quality and reliability. Independent means to **assess individual interactions** are required, regardless of whether they have been determined by a high-throughput technolgy like Y2H or TAP/MS, small-scale single-protein studies, or computations predictions. This is where PSISCORE comes into play.

# Behind the scenes - Distributed web service architecture #
There are many different approaches for confidence scoring individual molecular interactions. They can, for instance, be based on structural information, network topology, experimental conditions, similarity of the functional annotations of the interactors, or evolutionary conservation. It is therefore unfeasible that a single, central scoring schema could combine all these methods.
The obvious solution to this is **decentralization**. An illustration of this approach can be seen in this schematic drawing:

![http://psiscore.bioinf.mpi-inf.mpg.de/psiscore_architecture.png](http://psiscore.bioinf.mpi-inf.mpg.de/psiscore_architecture.png)

In the decentralized PSISCORE setting, individual research groups can focus on specific scoring approaches. A group with a focus on protein structures can provide confidence scores that assess the mutual interaction interfaces, while a group with an expertise on semantic similarity can host a server that will compute the functional similarities of interacting proteins.

The communication between individual PSISCORE servers and clients is defined by a [Web Service definition](PSISCORE_definition.md).

# The details - How does PSISCORE work? #
The input to a PSISCORE use case is a set of molecular interactions in a [HUPO-PSI](http://www.psidev.info/)-defined file format (i.e. [MITAB](http://code.google.com/p/psimi/wiki/PsimiTabFormat) or [PSI-MI XML 2.5](http://www.psidev.info/index.php?q=node/60)), for instance, as retrieved through a [PSICQUIC](http://code.google.com/p/psicquic/) query.
A PSISCORE client like [PSISCOREweb](http://psiscore.bioinf.mpi-inf.mpg.de/) then sends this file to multiple scoring servers, waits until the scoring has finished, and retrieves the individual scoring results. All confidence scores are then added to the input file, which the user can then download again. PSISCORE will not automatically combine individiual confidence according to certain unification scheme, but will provide the user with all the raw scores that hove been provided by the scoring servers.

# How can I use PSISCORE? #
PSISCORE being (I) a **distributed system** and (II) **open-source** it is very easy to participated, either as a user or as a service provider. You can easily add new scoring servers or clients at your institution and if you register new scoring servers in the PSISCORE registry, they are available to all PSISCORE clients and users. You can find reference implementations of PSISCORE servers and clients in the [download section](http://code.google.com/p/psiscore/downloads/list) and extensive documentation, how to set up or implement a new scoring server, in the [wiki](http://code.google.com/p/psiscore/w/list).

If you have any questions, just [ask them](http://groups.google.de/group/psiscore), or if you encounter any bugs or errors, please [report them](http://code.google.com/p/psiscore/issues/list).