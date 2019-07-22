package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.Extension;
import hudson.scm.SCM;
import jenkins.branch.MultiBranchProjectFactory;
import jenkins.branch.MultiBranchProjectFactoryDescriptor;
import jenkins.scm.api.SCMProbeStat;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.multibranch.AbstractWorkflowMultiBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;

/**
 * This class extends @{@link AbstractWorkflowMultiBranchProjectFactory} to inject defined Jenkins file and repository in
 * Remote Jenkins File Plugin
 * @author Julien Roy, julien.vanroy@gmail.com
 */
public class RemoteJenkinsFileWorkflowMultiBranchProjectFactory extends AbstractWorkflowMultiBranchProjectFactory {

    private static final String DEFAULT_JENKINS_FILE = "Jenkinsfile";

    private String localFile;
    private String remoteJenkinsFile;
    private SCM remoteJenkinsFileSCM;

    private RemoteJenkinsFileWorkflowMultiBranchProjectFactory() {
    }

    /**
     * Jenkins @{@link DataBoundSetter}
     *
     * @param remoteJenkinsFile path of the Jenkinsfile
     */
    @DataBoundSetter
    public void setRemoteJenkinsFile(String remoteJenkinsFile) {
        if (StringUtils.isEmpty(remoteJenkinsFile)) {
            this.remoteJenkinsFile = RemoteJenkinsFileWorkflowMultiBranchProjectFactory.DEFAULT_JENKINS_FILE;
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
     * Jenkins @{@link DataBoundSetter}
     *
     * @param localFileForRecognize file to search in local repository to activate build ( can be null )
     */
    @DataBoundSetter
    public void setLocalFileForReconize(String localFileForRecognize) {
        this.localFile = localFileForRecognize;
    }

    /**
     * Jenkins @{@link DataBoundConstructor}
     *
     * @param remoteJenkinsFile    path of the Jenkinsfile
     * @param remoteJenkinsFileSCM @{@link SCM} definition for the Jenkinsfile
     */
    @DataBoundConstructor
    public RemoteJenkinsFileWorkflowMultiBranchProjectFactory(String localFile, String remoteJenkinsFile, SCM remoteJenkinsFileSCM) {
        this.localFile = localFile;
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
    }

    /**
     * Extends @{@link WorkflowBranchProjectFactory}
     *
     * @param source @{@link SCMSource}
     * @return @{@link SCMSourceCriteria}
     */
    @Override
    protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return (probe, taskListener) -> {

            // Not match if remote SCM of remoteFileName is not configured
            if (this.remoteJenkinsFileSCM == null || StringUtils.isEmpty(this.remoteJenkinsFile)) {
                return false;
            }

            // Match all if local file is not specified
            if (StringUtils.isEmpty(this.localFile)) {
                taskListener.getLogger().println("Not local file defined, skipping checking in Source Code SCM, Jenkins file will be provided by Remote Jenkins File Plugin");
                return true;
            }

            SCMProbeStat stat = probe.stat(this.localFile);
            switch (stat.getType()) {
                case NONEXISTENT:
                    if (stat.getAlternativePath() != null) {
                        taskListener.getLogger().format("      ‘%s’ not found (but found ‘%s’, search is case sensitive)%n", this.localFile, stat.getAlternativePath());
                    } else {
                        taskListener.getLogger().format("      ‘%s’ not found%n", this.localFile);
                    }
                    return false;
                case DIRECTORY:
                    taskListener.getLogger().format("      ‘%s’ found but is a directory not a file%n", this.localFile);
                    return false;
                default:
                    taskListener.getLogger().format("      ‘%s’ found%n", this.localFile);
                    return true;
            }
        };
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

    /**
     * Default getter method
     * @return @this.localFile
     */
    public String getLocalFile() {
        return localFile;
    }

    /**
     * Descriptor Implementation for @{@link org.jenkinsci.plugins.workflow.multibranch.extended.RemoteJenkinsFileWorkflowMultiBranchProjectFactory}
     */
    @Extension
    public static class DescriptorImpl extends MultiBranchProjectFactoryDescriptor {

        @Override
        public String getDisplayName() {
            return org.jenkinsci.plugins.workflow.multibranch.extended.Messages.ProjectRecognizer_DisplayName();
        }

        @CheckForNull
        @Override
        public MultiBranchProjectFactory newInstance() {
            return new RemoteJenkinsFileWorkflowMultiBranchProjectFactory();
        }
    }

    @Override
    protected void customize(WorkflowMultiBranchProject project) {
        RemoteJenkinsFileWorkflowBranchProjectFactory projectFactory = new RemoteJenkinsFileWorkflowBranchProjectFactory(this.remoteJenkinsFile, this.remoteJenkinsFileSCM);
        project.setProjectFactory(projectFactory);
    }

}
