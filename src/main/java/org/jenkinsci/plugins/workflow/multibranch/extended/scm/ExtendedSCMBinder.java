package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.DescriptorVisibilityFilter;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
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

import java.util.Collections;
import java.util.List;

/**
 * This class consist of necessary methods for binding another Jenkins file from another repository to Multibranch pipeline.
 *
 * @author Aytunc BEKEN, aytuncbeken.ab@gmail.com
 */
public class ExtendedSCMBinder extends FlowDefinition {

    private String remoteJenkinsFile = "";
    private String remoteJenkinsFileBranch = "";
    private String originJenkinsFileDefinition = "";
    private String localMarker = "";
    private Boolean lookupInParameters = false;
    private Boolean matchBranches = false;
    private final SCM remoteJenkinsFileSCM;
    private String scmSourceBranchName = "master";
    private String fallbackBranch = "master";
    private final String matchBranchFailMessage;
    private final String matchBranchFallbackMessage;


    /**
     * Constructor for the class.
     *
     * @param remoteJenkinsFile    Path of the remote jenkins file from Remote Jenkins File Plugin descriptor
     * @param remoteJenkinsFileSCM SCM definition from Remote Jenkins File Plugin descriptor
     */
    public ExtendedSCMBinder(String remoteJenkinsFile, SCM remoteJenkinsFileSCM, String scmSourceBranchName, boolean matchBranches, String fallbackBranch, String originJenkinsFileDefinition, Boolean lookupInParameters, String localMarker) {
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
        this.matchBranches = matchBranches;
        this.scmSourceBranchName = scmSourceBranchName;
        this.remoteJenkinsFileBranch = scmSourceBranchName;
        this.fallbackBranch = fallbackBranch;
        this.matchBranchFailMessage = "Failed to checkout for " + this.scmSourceBranchName + " branch for Jenkins File.";
        this.matchBranchFallbackMessage = "Try to checkout " + this.fallbackBranch + " branch for Jenkins File.  ";
        this.originJenkinsFileDefinition = originJenkinsFileDefinition;
        this.lookupInParameters = lookupInParameters;
        this.localMarker = localMarker;
    }

    /**
     * Overwrites create method of FlowDefinition class. This methods sets the defined Jenkins file and defined SCM on
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
        // Be sure that old versions of this plugin is working
        if( this.lookupInParameters != null && this.originJenkinsFileDefinition != null) {
            if (this.lookupInParameters && this.originJenkinsFileDefinition.startsWith("$")) {
                //Clean parameter name for later use
                String jenkinsfileParameterName = this.originJenkinsFileDefinition.replace("$", "").replace("{", "").replace("}", "");
                //Setting default Jenkinsfile if jenkinsfileParameterName not found in parameters
                String newJenkinsFile = "Jenkinsfile";
                //Search for ParametersAction in Actions
                for (Action action : actions) {
                    if (action instanceof ParametersAction) {
                        //This can be the parameter that we are looking for
                        ParametersAction parametersAction = (ParametersAction) action;
                        //Check if the parameter name matches with JenkinsfileParameter
                        ParameterValue parameterValue = parametersAction.getParameter(jenkinsfileParameterName);
                        if( parameterValue != null) {
                            // If the parameters is there, set the value as new Jenkinsfile
                            newJenkinsFile = String.valueOf(parameterValue.getValue());
                        }
                    }
                }

                this.remoteJenkinsFile = newJenkinsFile;
            }
        }


        if (this.matchBranches && this.remoteJenkinsFileSCM instanceof GitSCM) {
            try {
                this.remoteJenkinsFileBranch = this.scmSourceBranchName;
                return new CpsScmFlowDefinition(this.generateSCMWithNewBranch(this.scmSourceBranchName), this.remoteJenkinsFile).create(handle, listener, actions);
            } catch (Exception ex) {
                if (ex instanceof AbortException) {
                    // This can be reason of there is no branch named in the Remote Jenkinsfile Provider Repository
                    // Fallback to master branch
                    listener.getLogger().println(this.matchBranchFailMessage);
                    listener.getLogger().println(this.matchBranchFallbackMessage);
                    this.remoteJenkinsFileBranch = this.fallbackBranch;
                    return new CpsScmFlowDefinition(this.generateSCMWithNewBranch(this.fallbackBranch), this.remoteJenkinsFile).create(handle, listener, actions);
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

        @NonNull
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
        public boolean filter(Object context, @NonNull Descriptor descriptor) {
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

    public String getRemoteJenkinsFileBranch() {
        // This null check is required for the jobs which were using version 1.12 ( or older).
        // This value was not implemented on that version. Therefore, It becomes as null
        if( remoteJenkinsFileBranch == null)
            return "N/A";
        return remoteJenkinsFileBranch;
    }

    public boolean isMatchBranches() {
        return matchBranches;
    }

    public String getLocalMarker() {
        return localMarker;
    }
}
