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

/**
 * This class extends @{@link WorkflowBranchProjectFactory} to inject defined Jenkins file and repository in
 * Remote Jenkins File Plugin
 * @author Aytunc BEKEN, aytuncbeken.ab@gmail.com
 */
public class RemoteJenkinsFileWorkflowBranchProjectFactory extends WorkflowBranchProjectFactory {


    private final String defaultJenkinsFile = "Jenkinsfile";
    private String remoteJenkinsFile;
    private SCM remoteJenkinsFileSCM;


    /**
     * Jenkins @{@link DataBoundSetter}
     *
     * @param remoteJenkinsFile path of the Jenkinsfile
     */
    @DataBoundSetter
    public void setRemoteJenkinsFile(String remoteJenkinsFile) {
        if (StringUtils.isEmpty(remoteJenkinsFile)) {
            this.remoteJenkinsFile = defaultJenkinsFile;
        } else {
            this.remoteJenkinsFile = remoteJenkinsFile;
        }
    }

    /**
     * Jenkins @{@link DataBoundSetter}
     *
     * @param remoteJenkinsFileSCM @{@link SCM} definition for the Jenkinsfile
     */
    @DataBoundSetter
    public void setRemoteJenkinsFileSCM(SCM remoteJenkinsFileSCM) {
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
    }


    /**
     * Jenkins @{@link DataBoundConstructor}
     *
     * @param remoteJenkinsFile    path of the Jenkinsfile
     * @param remoteJenkinsFileSCM @{@link SCM} definition for the Jenkinsfile
     */
    @DataBoundConstructor
    public RemoteJenkinsFileWorkflowBranchProjectFactory(String remoteJenkinsFile, SCM remoteJenkinsFileSCM) {
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
    }

    /**
     * Extends @{@link WorkflowBranchProjectFactory}
     *
     * @return @{@link FlowDefinition}
     */
    @Override
    protected FlowDefinition createDefinition() {
        return new ExtendedSCMBinder(this.remoteJenkinsFile, this.remoteJenkinsFileSCM);
    }

    /**
     * Extends @{@link WorkflowBranchProjectFactory}
     *
     * @param source @{@link SCMSource}
     * @return @{@link SCMSourceCriteria}
     */
    @Override
    protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return (SCMSourceCriteria) (probe, taskListener) -> {
            taskListener.getLogger().println("Ignoring Jenkins file checking in Source Code SCM, Jenkins file will be provided by Remote Jenkins File Plugin");
            return true;
        };
    }

    /**
     * Descriptor Implementation for @{@link org.jenkinsci.plugins.workflow.multibranch.AbstractWorkflowMultiBranchProjectFactory}
     */
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

    /**
     * Default getter method
     * @return @this.remoteJenkinsFile
     */
    public String getRemoteJenkinsFile() {
        return remoteJenkinsFile;
    }

    /**
     * Default getter method
     * @return @this.remoteJenkinsFile
     */
    public SCM getRemoteJenkinsFileSCM() {
        return remoteJenkinsFileSCM;
    }
}