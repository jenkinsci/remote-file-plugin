package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.DescriptorVisibilityFilter;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.*;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * This class consist of necessary methods for binding another Jenkins file from another repository to Multibranch pipeline.
 * @author Aytunc BEKEN, aytuncbeken.ab@gmail.com
 */
public class ExtendedSCMBinder extends FlowDefinition {

    private String remoteJenkinsFile;
    private SCM remoteJenkinsFileSCM;

    /**
     * Constructor for the class.
     *
     * @param remoteJenkinsFile    Path of the remote jenkins file from Remote Jenkins File Plugin descriptor
     * @param remoteJenkinsFileSCM SCM definition from Remote Jenkins File Plugin descriptor
     */
    public ExtendedSCMBinder(String remoteJenkinsFile, SCM remoteJenkinsFileSCM) {
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
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
}
