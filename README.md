# scholarsVitro

This fork of Vitro, owned by CUL-IT, provides a place to store modifications to Vitro that are useful at Cornell.
These branches are of interest:

## Branch `develop`

The only change is to this README file.

## Branch `scholars/maint-rel-1.8`

The version of Vitro used by __Scholars@Cornell__. It contains:

* Performance improvements to `edu.cornell.mannlib.vitro.webapp.reasoner`, back-ported from Vitro 1.9 by Brian Lowe.
* Transaction support on the RDF service, mostly in `edu.cornell.mannlib.vitro.webapp.rdfservice`
    * Don't know where this comes from, probably from Brian, along with the reasoner code.
* Improvements to the `edu.cornell.mannlib.vitro.webapp.utils.sparqlrunner` package, 
  back-ported from Vitro 1.9 by Jim Blake.
    * Replaces `edu.cornell.mannlib.vitro.webapp.utils.sparql`
* Cardinality options in `edu.cornell.mannlib.vitro.webapp.utils.configuration`, 
  back-ported from Vitro 1.9 by Jim Blake.

Other tweaks?

## Branch `feature/ontology-editor`

The GUI work done by Amit Mizrahi in 2016, to replace the existing ontology editor pages in Vitro.

The goal was to commit this to Vitro core, but no pull request was prepared. Is additional work necessary?

## Branch `feature/data-api-1.8`

An early implementation of the Data Distribution API. 
This has been superseded by [the published Maven artifact](https://github.com/cul-it/vivo-data-distribution-api), 
and could probably be deleted without any loss.

