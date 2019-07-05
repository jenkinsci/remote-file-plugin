package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.Extension;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.extended.scm.ExtendedSCMBinder;
import org.jenkinsci.plugins.workflow.multibranch.extended.scm.SCMFilter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collection;

public class RemoteJenkinsFileWorkflowBranchProjectFactory extends WorkflowBranchProjectFactory {


    private final String defaultJenkinsFile = "Jenkinsfile";
    private String remoteJenkinsFile;
    private SCM remoteJenkinsFileSCM;


    @DataBoundSetter
    public void setRemoteJenkinsFile(String remoteJenkinsFile) {
        if (StringUtils.isEmpty(remoteJenkinsFile)) {
            this.remoteJenkinsFile = defaultJenkinsFile;
        } else {
            this.remoteJenkinsFile = remoteJenkinsFile;
        }
    }

    @DataBoundSetter
    public void setRemoteJenkinsFileSCM(SCM remoteJenkinsFileSCM) {
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
    }


    @DataBoundConstructor
    public RemoteJenkinsFileWorkflowBranchProjectFactory(String remoteJenkinsFile, SCM remoteJenkinsFileSCM) {
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
    }

    @Override
    protected FlowDefinition createDefinition() {
        return new ExtendedSCMBinder(this.remoteJenkinsFile, this.remoteJenkinsFileSCM);
    }

    @Override
    protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return (SCMSourceCriteria) (probe, taskListener) -> {
            taskListener.getLogger().println("Ignoring Jenkins file check in Source Code SCM");
            return true;
        };
    }

    @Extension
    public static class DescriptorImpl extends AbstractWorkflowBranchProjectFactoryDescriptor {
        @Override
        public String getDisplayName() {
            return "by " + org.jenkinsci.plugins.workflow.multibranch.extended.Messages.ProjectRecognizer_DisplayName();
        }

        public Collection<? extends SCMDescriptor<?>> getApplicableDescriptors() {
            return SCMFilter.filter();
        }
    }

    public String getRemoteJenkinsFile() {
        return remoteJenkinsFile;
    }

    public SCM getRemoteJenkinsFileSCM() {
        return remoteJenkinsFileSCM;
    }
}