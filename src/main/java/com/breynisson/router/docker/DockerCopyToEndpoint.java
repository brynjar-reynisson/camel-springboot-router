package com.breynisson.router.docker;

import com.breynisson.router.RouterException;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.support.DefaultEndpoint;

import java.util.Objects;

public class DockerCopyToEndpoint extends DefaultEndpoint {

    private final String container;
    private final String destDir;
    private final String tmpDir;

    public DockerCopyToEndpoint(String container, String destDir, String tmpDir) {
        this.container = container;
        this.destDir = destDir;
        this.tmpDir = tmpDir;
    }

    @Override
    protected String createEndpointUri() {
        return "docker:copyto?container=" + container + "&amp;destDir=" + destDir + "&amp;tmpDir=" + tmpDir;
    }

    public String getContainer() {
        return container;
    }

    public String getDestDir() {
        return destDir;
    }

    public String getTmpDir() {
        return tmpDir;
    }

    @Override
    public Producer createProducer() {
        return new DockerCopyToProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new RouterException("Not supported");
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DockerCopyToEndpoint) {
            DockerCopyToEndpoint other = (DockerCopyToEndpoint) object;
            return Objects.equals(container, other.container) &&
                    Objects.equals(destDir, other.destDir) &&
                    Objects.equals(tmpDir, other.tmpDir);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getClass().getSimpleName() + ":" + container + ":" + destDir + ":" + tmpDir).hashCode();
    }
}
