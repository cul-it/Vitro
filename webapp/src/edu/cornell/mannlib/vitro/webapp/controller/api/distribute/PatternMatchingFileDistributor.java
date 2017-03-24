/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.controller.api.distribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.vitro.webapp.application.ApplicationUtils;
import edu.cornell.mannlib.vitro.webapp.utils.configuration.Property;

/**
 * Distributes the contents of a file, if the file exists. Otherwise,
 * distributes an empty data set.
 * 
 * Example:
 * 
 * <pre>
 * :pmfd
 *   a   <java:edu.cornell.mannlib.vitro.webapp.controller.api.distribute.DataDistributor> ,
 *       <java:edu.cornell.mannlib.vitro.webapp.controller.api.distribute.PatternMatchingFileDistributor> ;
 *   :actionName "collaboration_sunburst" ;
 *   :parameterName "department" ;
 *   :parameterPattern "[^/#]+$";
 *   :filepathPattern "crossunit-\\0.json" ;
 *   :contentType "application/json" .
 *   :emptyResponse "[]" ;
 * </pre>
 */
public class PatternMatchingFileDistributor extends DataDistributorBase {
    private static final Log log = LogFactory
            .getLog(PatternMatchingFileDistributor.class);

    /** The name of the request parameter that will select the file. */
    private String parameterName;

    /** The pattern to parse the value of the request parameter. */
    private Pattern parameterParser;

    /** The pattern to create the file path from the parsed values. */
    private String filepathPattern;

    /** The content type to attach to the file. */
    private String contentType;

    /** The response to provide if the file does not exist. */
    private String emptyResponse;

    private FileFinder fileFinder;

    @Property(uri = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationSetup#parameterName", minOccurs = 1, maxOccurs = 1)
    public void setParameterName(String name) {
        parameterName = name;
    }

    @Property(uri = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationSetup#parameterPattern", minOccurs = 1, maxOccurs = 1)
    public void setParameterPattern(String pattern) {
        parameterParser = Pattern.compile(pattern);
    }

    @Property(uri = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationSetup#filepathPattern", minOccurs = 1, maxOccurs = 1)
    public void setFilepathPattern(String pattern) {
        filepathPattern = pattern;
    }

    @Property(uri = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationSetup#contentType", minOccurs = 1, maxOccurs = 1)
    public void setContentType(String cType) {
        contentType = cType;
    }

    @Property(uri = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationSetup#emptyResponse", minOccurs = 1, maxOccurs = 1)
    public void setEmptyResponse(String response) {
        emptyResponse = response;
    }

    @Override
    public void init(DataDistributorContext context)
            throws DataDistributorException {
        super.init(context);
        fileFinder = new FileFinder(parameters, parameterName, parameterParser,
                filepathPattern,
                ApplicationUtils.instance().getHomeDirectory().getPath());
    }

    @Override
    public String getContentType() throws DataDistributorException {
        return contentType;
    }

    @Override
    public void writeOutput(OutputStream output)
            throws DataDistributorException {
        try {
            File file = fileFinder.find();
            if (file != null && file.isFile()) {
                IOUtils.copy(new FileInputStream(file), output);
                return;
            } else {
                IOUtils.write(emptyResponse, output);
            }
        } catch (IOException e) {
            throw new DataDistributorException(e);
        }
    }

    @Override
    public void close() throws DataDistributorException {
        // Nothing to close.
    }

    /**
     * Does the heavy lifting of locating the file.
     */
    protected static class FileFinder {
        private final Map<String, String[]> parameters;
        private final String parameterName;
        private final Pattern parameterParser;
        private final String filepathPattern;
        private final Path home;

        public FileFinder(Map<String, String[]> parameters,
                String parameterName, Pattern parameterParser,
                String filepathPattern, Path home) {
            this.parameters = parameters;
            this.parameterName = parameterName;
            this.parameterParser = parameterParser;
            this.filepathPattern = filepathPattern;
            this.home = home;
        }

        public File find() {
            String parameter = getParameterFromRequest();
            if (parameter == null) {
                return null;
            } else {
                return doPatternMatching(parameter);
            }
        }

        private String getParameterFromRequest() {
            String[] values = parameters.get(parameterName);
            if (values == null || values.length == 0) {
                log.warn("No value provided for request parameter '"
                        + parameterName + "'");
                return null;
            }
            return values[0];
        }

        private File doPatternMatching(String parameter) {
            Matcher m = parameterParser.matcher(parameter);
            if (m.find()) {
                return substituteIntoFilepath(m);
            } else {
                log.warn("Failed to parse the request parameter: '"
                        + parameterParser + "' doesn't match '" + parameter
                        + "'");
                return null;
            }

        }

        private File substituteIntoFilepath(Matcher m) {
            String path = filepathPattern;
            for (int i = 0; i <= m.groupCount(); i++) {
                path = path.replace("\\" + i, m.group(i));
            }
            return home.resolve(path).toFile();
        }

    }

}
