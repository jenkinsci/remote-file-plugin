package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import hudson.Extension;
import hudson.model.*;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.GitSCMExtensionDescriptor;
import hudson.plugins.git.util.BuildData;
import hudson.util.DescribableList;
import jenkins.branch.OrganizationFolder;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * This class extends GitSCMExtension for excluding plugin SCM from Job Poll
 */
public class ExcludeFromPoll extends GitSCMExtension {

    @DataBoundConstructor
    public ExcludeFromPoll(){}

    @Extension
    public static class DescriptorImpl extends GitSCMExtensionDescriptor {
        @Override
        public String getDisplayName() {
            return "Exclude From Poll";
        }
    }

    @Extension
    public static class HideMe extends DescriptorVisibilityFilter {
        @Override
        public boolean filter(Object context, @Nonnull Descriptor descriptor) {
            if( descriptor instanceof ExcludeFromPoll.DescriptorImpl) {
                if( context instanceof WorkflowMultiBranchProject ) {
                    return true;
                }
                else if ( context instanceof OrganizationFolder) {
                    return true;
                }
                return false;
            }
            return true;
        }
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return true;
    }

    @Override
    public Boolean isRevExcluded(GitSCM scm, org.jenkinsci.plugins.gitclient.GitClient git, GitChangeSet commit, TaskListener listener, BuildData buildData) throws IOException, InterruptedException, GitException {
        DescribableList<GitSCMExtension, GitSCMExtensionDescriptor> extensions = scm.getExtensions();
        for(GitSCMExtension gitSCMExtension : extensions) {
            if( gitSCMExtension instanceof ExcludeFromPoll) {
                return true;
            }
        }
        return false;
    }
}
