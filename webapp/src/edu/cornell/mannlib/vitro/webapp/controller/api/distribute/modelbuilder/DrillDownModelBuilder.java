/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.controller.api.distribute.modelbuilder;

import static edu.cornell.mannlib.vitro.webapp.utils.sparqlrunner.SparqlQueryRunner.createSelectQueryContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.AuthorizationRequest;
import edu.cornell.mannlib.vitro.webapp.controller.api.distribute.DataDistributor.DataDistributorException;
import edu.cornell.mannlib.vitro.webapp.controller.api.distribute.DataDistributorContext;
import edu.cornell.mannlib.vitro.webapp.modelaccess.RequestModelAccess;
import edu.cornell.mannlib.vitro.webapp.utils.configuration.Property;

/**
 * Run the top level model builder(s) to "seed" the local model with top-level
 * information.
 * 
 * Execute a SELECT query against the local model to obtain a list of parameter
 * maps to use when drilling down.
 * 
 * Run the drill down model builder(s) to obtain more info for each item in the
 * list.
 */
public class DrillDownModelBuilder implements ResettableModelBuilder {

    private List<ModelBuilder> topLevelModelBuilders = new ArrayList<>();
    private String drillDownQuery;
    private List<ResettableModelBuilder> drillDownModelBuilders = new ArrayList<>();

    @Property(uri = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationSetup#topLevelModelBuilder", minOccurs = 1)
    public void addTopLevelModelBuilder(ModelBuilder builder) {
        topLevelModelBuilders.add(builder);
    }

    @Property(uri = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationSetup#drillDownQuery", minOccurs = 1, maxOccurs = 1)
    public void addDrillDownQuery(String query) {
        drillDownQuery = query;
    }

    @Property(uri = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationSetup#drillDownModelBuilder", minOccurs = 1)
    public void addDrillDownModelBuilder(ResettableModelBuilder builder) {
        drillDownModelBuilders.add(builder);
    }

    private DataDistributorContext ddContext;

    @Override
    public void init(DataDistributorContext ddc)
            throws DataDistributorException {
        this.ddContext = ddc;
    }

    @Override
    public Model buildModel() throws DataDistributorException {
        Model m = ModelFactory.createDefaultModel();
        for (ModelBuilder mb : topLevelModelBuilders) {
            m.add(runModelBuilder(mb, ddContext));
        }
        for (Map<String, String> drillDownParameters : getDrillDownParameterMaps(
                m)) {
            for (ResettableModelBuilder mb : drillDownModelBuilders) {
                m.add(runDrillDownModelBuilder(mb, drillDownParameters));
            }
        }
        return m;
    }

    private Model runModelBuilder(ModelBuilder mb,
            DataDistributorContext context) throws DataDistributorException {
        try {
            mb.init(context);
            return mb.buildModel();
        } finally {
            mb.close();
        }
    }

    private List<Map<String, String>> getDrillDownParameterMaps(
            Model localModel) {
        return createSelectQueryContext(localModel, drillDownQuery).execute()
                .toStringFields().getListOfMaps();
    }

    private Model runDrillDownModelBuilder(ResettableModelBuilder mb,
            Map<String, String> drillDownParameters)
            throws DataDistributorException {
        DataDistributorContext ddc = new DrillDownContext(ddContext,
                drillDownParameters);
        Model model = runModelBuilder(mb, ddc);
        mb.reset();
        return model;
    }

    @Override
    public void close() {
        // Nothing to close.
    }

    @Override
    public void reset() {
        // Everything can safely be run again.
    }

    /**
     * A DataDistributorContext that starts with a baseline and includes the
     * drill-down values as additional requestParameters -- note that drill-down
     * values may override existing parameters on the request.
     */
    private static class DrillDownContext implements DataDistributorContext {
        private final DataDistributorContext baselineContext;
        private final Map<String, String[]> requestParameters;

        public DrillDownContext(DataDistributorContext baselineContext,
                Map<String, String> drillDownParameters) {
            this.baselineContext = baselineContext;

            this.requestParameters = new HashMap<>(
                    baselineContext.getRequestParameters());
            for (Entry<String, String> entry : drillDownParameters.entrySet()) {
                this.requestParameters.put(entry.getKey(),
                        new String[] { entry.getValue() });
            }
        }

        @Override
        public Map<String, String[]> getRequestParameters() {
            return this.requestParameters;
        }

        @Override
        public RequestModelAccess getRequestModels() {
            return baselineContext.getRequestModels();
        }

        @Override
        public boolean isAuthorized(AuthorizationRequest ar) {
            return baselineContext.isAuthorized(ar);
        }

    }
}
