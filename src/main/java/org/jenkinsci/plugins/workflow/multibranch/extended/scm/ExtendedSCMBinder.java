package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.plugins.git.Branch;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.BuildChooserContext;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.*;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * This class consist of necessary methods for binding another Jenkins file from another repository to Multibranch pipeline.
 * @author Aytunc BEKEN, aytuncbeken.ab@gmail.com
 */
public class ExtendedSCMBinder extends FlowDefinition {

    private String remoteJenkinsFile;
    private SCM remoteJenkinsFileSCM;
    private String scmSourceBranchName;
    private Boolean matchBranches;
    private String fallbackBranch = "master";

    /**
     * Constructor for the class.
     *
     * @param remoteJenkinsFile    Path of the remote jenkins file from Remote Jenkins File Plugin descriptor
     * @param remoteJenkinsFileSCM SCM definition from Remote Jenkins File Plugin descriptor
     */
    public ExtendedSCMBinder(String remoteJenkinsFile, SCM remoteJenkinsFileSCM, String scmSourceBranchName, Boolean matchBranches) {
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
        this.matchBranches = matchBranches;
        this.scmSourceBranchName = scmSourceBranchName;
    }

    /**
     * Overwrites create method of FlowDefiniton class. This methods sets the defined Jenkins file and defined SCM on
     * Remote Jenkins Plugin to Pipeline job which will be created by MultiBranch Pipeline.
     *
     * @param handle @{@link FlowExecutionOwner}
     * @param listener @{@link TaskListener}
     * @param actions List of @{@link Action}
     * @return @{@link FlowExecution}
     * @throws Exception
     */
    @Override
    public FlowExecution create(FlowExecutionOwner handle, TaskListener listener, List<? extends Action> actions) throws Exception {
        SCMDescriptor scmDescriptor = this.remoteJenkinsFileSCM.getDescriptor();
        if (this.matchBranches && this.remoteJenkinsFileSCM instanceof GitSCM)
        {
            try {
                //Try to create pipeline from branch name from which discovered as Pipeline in MultiBranch
                /*GitSCM scm = this.generateNewSCM("muz");
                scm.checkout();
                Run run = (Run) handle.getExecutable();
                EnvVars envVars = run.getEnvironment(listener);
                FilePath workspace = new FilePath(handle.getRootDir());
                GitClient gitClient = scm.createClient(listener,envVars, (Run<?, ?>) handle.getExecutable(),workspace);
                gitClient.init();
                Set<Branch> remoteBranches = gitClient.getRemoteBranches();
                scm.getBuildChooser()*/


                return new CpsScmFlowDefinition(this.generateNewSCM(this.scmSourceBranchName), this.remoteJenkinsFile).create(handle, listener, actions);
            }
            catch (Exception ex) {
                if( ex instanceof AbortException) {
                    // This can be reason of there is no branch named in the Remote File Repository
                    // Fallback to master
                    return new CpsScmFlowDefinition(this.generateNewSCM(fallbackBranch), this.remoteJenkinsFile).create(handle, listener, actions);
                }
            }
        }
        // It matchBranches not checked or SCM is not GitSCM, return with Remote File SCM as defined in Jenkins
        return new CpsScmFlowDefinition(this.remoteJenkinsFileSCM, this.remoteJenkinsFile).create(handle, listener, actions);
    }

    /**
     * Descriptor Implementation for @{@link FlowDefinitionDescriptor}.
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

    private GitSCM generateNewSCM(String branchName) {
        GitSCM configuredGitSCM = (GitSCM) this.remoteJenkinsFileSCM;
        List<BranchSpec> branchSpecs = Arrays.asList(new BranchSpec(branchName));
        return new GitSCM(configuredGitSCM.getUserRemoteConfigs(),branchSpecs,configuredGitSCM.isDoGenerateSubmoduleConfigurations(),configuredGitSCM.getSubmoduleCfg(),configuredGitSCM.getBrowser(),configuredGitSCM.getGitTool(),configuredGitSCM.getExtensions());
    }
}
