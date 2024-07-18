package com.breynisson.router.docker;

import com.breynisson.router.RouterException;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

@Component("docker")
public class DockerComponent extends DefaultComponent {

    private static final String CMD_COPY_TO = "copyto";

    private static final String PARAM_CONTAINER = "container";
    private static final String PARAM_DEST_DIR = "destDir";
    private static final String PARAM_TMP_DIR = "tmpDir";

    public DockerComponent(CamelContext camelContext) {
        super(camelContext);
        try {
            doBuild();
        } catch (Exception e) {
            throw new RouterException(e);
        }
    }

    @Override
    protected void setProperties(Endpoint endpoint, Map<String, Object> parameters) {
        //no-op
    }

    @Override
    protected void validateParameters(String uri, Map<String, Object> parameters, String optionPrefix) {
        //no-op
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) {
        if (CMD_COPY_TO.equals(remaining)) {
            return createCopyToEndpoint(parameters);
        }
        throw new RouterException("Unknown command " + remaining);
    }

    private Endpoint createCopyToEndpoint(Map<String, Object> parameters) {
        String container = get(parameters, PARAM_CONTAINER);
        String destDir = get(parameters, PARAM_DEST_DIR);
        String tmpDir = get(parameters, PARAM_TMP_DIR);
        return new DockerCopyToEndpoint(container, destDir, tmpDir);
    }

    private String get(Map<String, Object> parameters, String paramName) {
        return (String) parameters.get(paramName);
    }
}
