package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.Extension;
import hudson.scm.SCM;
import jenkins.branch.MultiBranchProjectFactory;
import jenkins.branch.MultiBranchProjectFactoryDescriptor;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.multibranch.AbstractWorkflowMultiBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.multibranch.extended.scm.LocalFileSCMSourceCriteria;
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
    private boolean matchBranches;
    private String scmSourceBranchName;
    private String fallbackBranch;

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
    public RemoteJenkinsFileWorkflowMultiBranchProjectFactory(String localFile, String remoteJenkinsFile, SCM remoteJenkinsFileSCM, boolean matchBranches) {
        this.localFile = localFile;
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
        this.matchBranches = matchBranches;
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
            // Don't match if remote SCM of remoteFileName is not configured
            if (this.remoteJenkinsFileSCM == null || StringUtils.isEmpty(this.remoteJenkinsFile)) {
                return false;
            }
            this.setScmSourceBranchName(probe.name());
            return LocalFileSCMSourceCriteria.matches(this.localFile, probe, taskListener);
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
        RemoteJenkinsFileWorkflowBranchProjectFactory projectFactory = new RemoteJenkinsFileWorkflowBranchProjectFactory(this.remoteJenkinsFile, this.localFile, this.remoteJenkinsFileSCM, this.getMatchBranches(), this.fallbackBranch);
        project.setProjectFactory(projectFactory);
    }

    /**
     *Jenkins @{@link DataBoundSetter}
     * @param matchBranches True to enable match branches feature
     */
    @DataBoundSetter
    public void setMatchBranches(boolean matchBranches) {
        this.matchBranches = matchBranches;
    }

    /**
     * Default getter method
     * @return @this.matchBranches
     */
    public boolean getMatchBranches() {
        return matchBranches;
    }

    /**
     * Set  @this.scmSourceBranchName to be used in new scm definition with new branch nameLocal
     * @param scmSourceBranchName Current branch name which MultiBranch pipeline working on.
     */
    public void setScmSourceBranchName(String scmSourceBranchName) {
        this.scmSourceBranchName = scmSourceBranchName;
    }

    /**
     * Default getter method
     * @return @this.scmSourceBranchName
     */
    public String getScmSourceBranchName() {
        return scmSourceBranchName;
    }

    public String getFallbackBranch() {
        return fallbackBranch;
    }

    @DataBoundSetter
    public void setFallbackBranch(String fallbackBranch) {
        this.fallbackBranch = fallbackBranch;
    }
}
