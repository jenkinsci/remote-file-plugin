package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.DescriptorVisibilityFilter;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.GitSCMExtensionDescriptor;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import hudson.util.DescribableList;
import jenkins.branch.OrganizationFolder;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * This class extends GitSCMExtension for excluding git change log from plugin SCM
 */
public class ExcludeFromChangeSet extends GitSCMExtension {

    @DataBoundConstructor
    public ExcludeFromChangeSet(){}

    @Extension
    public static class DescriptorImpl extends GitSCMExtensionDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Exclude From ChangeSet";
        }
    }

    @Extension
    public static class ScmListener extends SCMListener {

        @Override
        public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState pollingBaseline) throws Exception {
            if (!(build instanceof WorkflowRun)) {
                return;
            }
            WorkflowRun workflowRun = (WorkflowRun) build;
            WorkflowJob workflowJob = ((WorkflowRun) build).getParent();
            FlowDefinition flowDefinition = workflowJob.getDefinition();
            if (!(flowDefinition instanceof ExtendedSCMBinder)) {
                return;
            }
            ExtendedSCMBinder extendedSCMBinder = (ExtendedSCMBinder) flowDefinition;
            if ( extendedSCMBinder.getRemoteJenkinsFileSCM() instanceof GitSCM) {
                GitSCM remoteJenkinsFileSCM = (GitSCM) extendedSCMBinder.getRemoteJenkinsFileSCM();
                if (remoteJenkinsFileSCM.getKey().equals(scm.getKey())) {
                    DescribableList<GitSCMExtension, GitSCMExtensionDescriptor> extensions = remoteJenkinsFileSCM.getExtensions();
                    for (GitSCMExtension gitSCMExtension : extensions) {
                        if (gitSCMExtension instanceof ExcludeFromChangeSet) {
                            if (changelogFile != null) {
                                // Empty changelog file to persist
                                FileUtils.write(changelogFile, "", StandardCharsets.UTF_8);
                                // Clear change set
                                workflowRun.getChangeSets().clear();
                            }
                        }
                    }
                }
            }
        }
    }

    @Extension
    public static class HideMe extends DescriptorVisibilityFilter {

        @Override
        public boolean filter(Object context, @NonNull Descriptor descriptor) {
            if (descriptor instanceof ExcludeFromChangeSet.DescriptorImpl) {
                if (context instanceof WorkflowMultiBranchProject) {
                    return true;
                } else {
                    return context instanceof OrganizationFolder;
                }
            }
            return true;
        }

    }

}
