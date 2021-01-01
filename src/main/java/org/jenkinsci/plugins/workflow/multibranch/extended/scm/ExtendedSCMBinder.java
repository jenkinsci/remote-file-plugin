package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * This class consist of necessary methods for binding another Jenkins file from another repository to Multibranch pipeline.
 *
 * @author Aytunc BEKEN, aytuncbeken.ab@gmail.com
 */
public class ExtendedSCMBinder extends FlowDefinition {

    private String remoteJenkinsFile;
    private SCM remoteJenkinsFileSCM;
    private String scmSourceBranchName;
    private boolean matchBranches;
    private String fallbackBranch;
    private String matchBranchFailMessage;
    private String matchBranchFallbackMessage;


    /**
     * Constructor for the class.
     *
     * @param remoteJenkinsFile    Path of the remote jenkins file from Remote Jenkins File Plugin descriptor
     * @param remoteJenkinsFileSCM SCM definition from Remote Jenkins File Plugin descriptor
     */
    public ExtendedSCMBinder(String remoteJenkinsFile, SCM remoteJenkinsFileSCM, String scmSourceBranchName, boolean matchBranches, String fallbackBranch) {
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
        this.matchBranches = matchBranches;
        this.scmSourceBranchName = scmSourceBranchName;
        this.fallbackBranch = fallbackBranch;
        this.matchBranchFailMessage = "Failed to checkout for " + this.scmSourceBranchName + " branch for Jenkins File.";
        this.matchBranchFallbackMessage = "Try to checkout " + this.fallbackBranch + " branch for Jenkins File.  ";
    }

    /**
     * Overwrites create method of FlowDefiniton class. This methods sets the defined Jenkins file and defined SCM on
     * Remote Jenkins Plugin to Pipeline job which will be created by MultiBranch Pipeline.
     *
     * @param handle   {@link FlowExecutionOwner}
     * @param listener {@link TaskListener}
     * @param actions  List of {@link Action}
     * @return {@link FlowExecution}
     * @throws Exception
     */
    @Override
    public FlowExecution create(FlowExecutionOwner handle, TaskListener listener, List<? extends Action> actions) throws Exception {
        if (this.matchBranches && this.remoteJenkinsFileSCM instanceof GitSCM) {
            try {
                return new CpsScmFlowDefinition(this.generateSCMWithNewBranch(this.scmSourceBranchName), this.remoteJenkinsFile).create(handle, listener, actions);
            } catch (Exception ex) {
                if (ex instanceof AbortException) {
                    // This can be reason of there is no branch named in the Remote Jenkinsfile Provider Repository
                    // Fallback to master branch
                    listener.getLogger().println(this.matchBranchFailMessage);
                    listener.getLogger().println(this.matchBranchFallbackMessage);
                    return new CpsScmFlowDefinition(this.generateSCMWithNewBranch(fallbackBranch), this.remoteJenkinsFile).create(handle, listener, actions);
                }
            }
        }
        // If matchBranches not checked or SCM is not GitSCM, return with Remote Jenkinsfile Provider SCM as defined in Jenkins
        return new CpsScmFlowDefinition(this.remoteJenkinsFileSCM, this.remoteJenkinsFile).create(handle, listener, actions);
    }

    /**
     * Descriptor Implementation for {@link FlowDefinitionDescriptor}.
     */
    @Extension
    public static class DescriptorImpl extends FlowDefinitionDescriptor {

        @Override
        public String getDisplayName() {
            return "Pipeline from Remote Jenkins File Plugin";
        }
    }

    /**
     * This method hides Remote Jenkins Plugin from other Pipeline Jobs and make visible to only MultiBranch Pipeline
     *
     * @see DescriptorVisibilityFilter
     */
    @Extension
    public static class HideMe extends DescriptorVisibilityFilter {

        @Override
        public boolean filter(Object context, @Nonnull Descriptor descriptor) {
            if (descriptor instanceof DescriptorImpl) {
                return context instanceof WorkflowJob && ((WorkflowJob) context).getParent() instanceof WorkflowMultiBranchProject;
            }
            return true;
        }

    }

    /**
     * Genereate new {@link SCM} object with a given branch name from defined Remote Jenkins File SCM definition
     * @param branchName Branch name to use in new {@link SCM}
     * @return new {@link SCM} defined with new branch
     */
    private GitSCM generateSCMWithNewBranch(String branchName) {
        GitSCM configuredGitSCM = (GitSCM) this.remoteJenkinsFileSCM;
        return new GitSCM(configuredGitSCM.getUserRemoteConfigs(), Collections.singletonList(new BranchSpec(branchName)), configuredGitSCM.isDoGenerateSubmoduleConfigurations(), configuredGitSCM.getSubmoduleCfg(), configuredGitSCM.getBrowser(), configuredGitSCM.getGitTool(), configuredGitSCM.getExtensions());
    }

    public SCM getRemoteJenkinsFileSCM() {
        return remoteJenkinsFileSCM;
    }

    public String getRemoteJenkinsFile() {
        return remoteJenkinsFile;
    }
}
