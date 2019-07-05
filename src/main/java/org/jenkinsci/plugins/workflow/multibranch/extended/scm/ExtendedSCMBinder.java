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

public class ExtendedSCMBinder extends FlowDefinition {

    private String remoteJenkinsFile;
    private SCM remoteJenkinsFileSCM;

    public ExtendedSCMBinder(String remoteJenkinsFile, SCM remoteJenkinsFileSCM) {
        this.remoteJenkinsFile = remoteJenkinsFile;
        this.remoteJenkinsFileSCM = remoteJenkinsFileSCM;
    }

    @Override
    public FlowExecution create(FlowExecutionOwner handle, TaskListener listener, List<? extends Action> actions) throws Exception {
        return new CpsScmFlowDefinition(this.remoteJenkinsFileSCM, this.remoteJenkinsFile).create(handle, listener, actions);
    }

    @Extension
    public static class DescriptorImpl extends FlowDefinitionDescriptor {

        @Override
        public String getDisplayName() {
            return "Pipeline from Remote Jenkins File Plugin";
        }
    }

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
