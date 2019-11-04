package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import hudson.model.TaskListener;
import jenkins.scm.api.SCMProbeStat;
import jenkins.scm.api.SCMSourceCriteria.Probe;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

public class LocalFileSCMSourceCriteria {

    /**
     * @param localFile    path of an arbitrary local file which must be present for the project to be recognised
     * @param probe        the Probe being used for the current SCM Criteria
     * @param taskListener the listener for the current scan task
     * @return
     * @throws IOException
     */
    public static boolean matches(String localFile, Probe probe, TaskListener taskListener) throws IOException {
        // Match all if local file is not specified
        if (StringUtils.isEmpty(localFile)) {
            taskListener.getLogger().println("No local file defined. Skipping Source Code SCM probe, since Jenkinsfile will be provided by Remote Jenkins File Plugin");
            return true;
        }

        SCMProbeStat stat = probe.stat(localFile);
        switch (stat.getType()) {
            case NONEXISTENT:
                if (stat.getAlternativePath() != null) {
                    taskListener.getLogger().format("      ‘%s’ not found (but found ‘%s’, search is case sensitive)%n", localFile, stat.getAlternativePath());
                } else {
                    taskListener.getLogger().format("      ‘%s’ not found%n", localFile);
                }
                return false;
            case DIRECTORY:
                taskListener.getLogger().format("      ‘%s’ found but is a directory not a file%n", localFile);
                return false;
            default:
                taskListener.getLogger().format("      ‘%s’ found%n", localFile);
                return true;
        }
    }
}
