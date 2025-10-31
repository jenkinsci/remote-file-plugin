package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;

import java.io.File;

@Extension
public class CustomScmListener extends SCMListener {

    @Override
    public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState pollingBaseline) throws Exception {
        super.onCheckout(build, scm, workspace, listener, changelogFile, pollingBaseline);
    }

    @Override
    public void onChangeLogParsed(Run<?, ?> build, SCM scm, TaskListener listener, ChangeLogSet<?> changelog) throws Exception {
        super.onChangeLogParsed(build, scm, listener, changelog);
    }

}
