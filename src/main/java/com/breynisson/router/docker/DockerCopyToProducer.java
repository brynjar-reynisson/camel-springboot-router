package com.breynisson.router.docker;

import com.breynisson.router.RouterException;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.support.DefaultProducer;

public class DockerCopyToProducer extends DefaultProducer {

    public DockerCopyToProducer(DockerCopyToEndpoint dockerCopyToEndpoint) {
        super(dockerCopyToEndpoint);
    }

    @Override
    public void process(Exchange exchange) {

        DockerCopyToEndpoint dockerCopyToEndpoint = (DockerCopyToEndpoint) getEndpoint();
        Object body = exchange.getMessage().getBody();
        if (body instanceof GenericFile) {
            GenericFile file = (GenericFile) body;
            String path = file.getAbsoluteFilePath();
            path = path.replace("\\.\\", "\\");
            path = path.replace("/./", "/");
            String[] cpCmd = { "docker", "cp", path, dockerCopyToEndpoint.getContainer() + ":" + dockerCopyToEndpoint.getTmpDir() + "/" + file.getFileName() };
            String[] mvCmd = { "docker", "exec", "-d", dockerCopyToEndpoint.getContainer(), "mv", dockerCopyToEndpoint.getTmpDir() + "/" + file.getFileName(), dockerCopyToEndpoint.getDestDir()};

            try {
                Process cpProcess = new ProcessBuilder()
                        .inheritIO()
                        .command(cpCmd)
                        .start();
                int exitStatus = cpProcess.waitFor();
                if (exitStatus != 0) {
                    throw new IllegalStateException("Exit status for docker cp was " + exitStatus);
                }

                Process mvProcess = new ProcessBuilder()
                        .inheritIO()
                        .command(mvCmd)
                        .start();
                exitStatus = mvProcess.waitFor();
                if (exitStatus != 0) {
                    throw new IllegalStateException("Exit status for docker mv was " + exitStatus);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                throw new RouterException(e);
            }
        } else {
            throw new RouterException("Unsupported body type: " + body.getClass().getCanonicalName());
        }
    }
}
