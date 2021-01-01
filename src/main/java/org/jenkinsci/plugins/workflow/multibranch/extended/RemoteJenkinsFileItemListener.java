package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.git.GitSCM;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.extended.scm.ExtendedSCMBinder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.StringJoiner;

@Extension
public class RemoteJenkinsFileItemListener extends EnvironmentContributor {

    public static final String RJPP_SCM_ENV_NAME = "RJPP_SCM_URL";
    public static final String RJPP_JFILE_ENV_NAME = "RJPP_JENKINSFILE";

    @Override
    public void buildEnvironmentFor(@NotNull Run r, @NotNull EnvVars envs, @NotNull TaskListener listener) throws IOException, InterruptedException {
        if (r instanceof WorkflowRun) {
            WorkflowRun workflowRun = (WorkflowRun) r;
            WorkflowJob workflowJob = workflowRun.getParent();
            FlowDefinition flowDefinition = workflowJob.getDefinition();
            if (flowDefinition instanceof ExtendedSCMBinder) {
                ExtendedSCMBinder extendedSCMBinder = (ExtendedSCMBinder) flowDefinition;
                if (extendedSCMBinder.getRemoteJenkinsFileSCM() instanceof GitSCM) {
                    GitSCM gitSCM = (GitSCM) extendedSCMBinder.getRemoteJenkinsFileSCM();
                    StringJoiner scmUrls = new StringJoiner(",");
                    for (RemoteConfig remoteConfig : gitSCM.getRepositories()) {
                        for (URIish urIish : remoteConfig.getURIs()) {
                            scmUrls.add(urIish.toString());
                        }
                    }
                    String jenkinsFile = extendedSCMBinder.getRemoteJenkinsFile();
                    envs.put(this.RJPP_SCM_ENV_NAME, scmUrls.toString());
                    envs.put(this.RJPP_JFILE_ENV_NAME, jenkinsFile);
                }
            }
        }
        super.buildEnvironmentFor(r, envs, listener);
    }
}
