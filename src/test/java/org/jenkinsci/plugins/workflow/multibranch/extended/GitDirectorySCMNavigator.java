package org.jenkinsci.plugins.workflow.multibranch.extended;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMSourceObserver;
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.assertFalse;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Sample provider which scans a directory for Git checkouts.
 */
public class GitDirectorySCMNavigator extends SCMNavigator {

    private final String directory;

    @DataBoundConstructor public GitDirectorySCMNavigator(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }

    @NonNull
    @Override
    protected String id() {
        return directory;
    }

    @Override public void visitSources(SCMSourceObserver observer) throws IOException, InterruptedException {
        TaskListener listener = observer.getListener();
        File[] kids = new File(directory).listFiles();
        if (kids == null) {
            listener.error(directory + " does not exist");
            return;
        }
        for (File kid : kids) {
            if (!observer.isObserving()) {
                return;
            }
            if (!new File(kid, ".git").isDirectory()) {
                listener.getLogger().format("Ignoring %s since it does not look like a Git checkout%n", kid);
                continue;
            }
            listener.getLogger().format("Considering: %s%n", kid);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Proc proc = new Launcher.LocalLauncher(listener).launch().pwd(kid).cmds("git", "remote", "-v").stdout(baos).start();
            if (proc.join() != 0) {
                listener.error("git-remote failed");
                continue;
            }
            String origin = kid.getAbsolutePath(); // fallback
            for (String line : IOUtils.readLines(new ByteArrayInputStream(baos.toByteArray()))) {
                Matcher m = ORIGIN.matcher(line);
                if (m.matches()) {
                    origin = m.group(1);
                    listener.getLogger().format("Found origin URL: %s%n", origin);
                    assertFalse(origin.startsWith("git@"));
                    break;
                }
            }
            SCMSourceObserver.ProjectObserver projectObserver = observer.observe(kid.getName());
            projectObserver.addSource(new GitSCMSource(getId() + "::" + kid.getName(), origin, "", "*", "", false));
            projectObserver.complete();
        }
    }
    private static final Pattern ORIGIN = Pattern.compile("origin\t(.+) [(]fetch[)]");

    @Extension @Symbol("gitDirectory")
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

        @Override public String getDisplayName() {
            return "Directory of Git checkouts";
        }

        @Override public SCMNavigator newInstance(String name) {
            return null;
        }

    }

}