package org.jenkinsci.plugins.workflow.multibranch.extended;

import com.cloudbees.hudson.plugins.folder.computed.FolderComputation;
import hudson.plugins.git.GitSCM;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import jenkins.branch.MultiBranchProject;
import jenkins.branch.OrganizationFolder;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProjectFactory;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RemoteJenkinsFileWorkflowMultiBranchProjectFactoryTest {

    private static final String SCRIPT = "Jenkinsfile";

    @ClassRule
    public static final BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRule r = new JenkinsRule();
    @Rule
    public GitSampleRepoRule sampleRepo1 = new GitSampleRepoRule();
    @Rule
    public GitSampleRepoRule sampleRepo2 = new GitSampleRepoRule();
    @Rule
    public GitSampleRepoRule sampleRepo3 = new GitSampleRepoRule();
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testWithoutRemoteJenkinsFile() throws Exception {

        File clones = tmp.newFolder();

        // Add script repository
        sampleRepo1.init();
        sampleRepo1.write(SCRIPT, "echo 'ran one'");
        sampleRepo1.git("add", SCRIPT);
        sampleRepo1.git("commit", "--all", "--message=first_commit");
        sampleRepo1.git("clone", ".", new File(clones, "source-one").getAbsolutePath());

        // Add source code repository
        sampleRepo2.init();
        sampleRepo2.write("source.txt", "source code");
        sampleRepo2.git("add", "source.txt");
        sampleRepo2.git("commit", "--all", "--message=first_commit");
        sampleRepo2.git("clone", ".", new File(clones, "source-two").getAbsolutePath());

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        OrganizationFolder top = r.jenkins.createProject(OrganizationFolder.class, "top");
        top.getNavigators().add(new GitDirectorySCMNavigator(clones.getAbsolutePath()));
        top.getProjectFactories().clear();
        top.getProjectFactories().add(new WorkflowMultiBranchProjectFactory());

        // Make sure we created one multi-branch projects:
        top.scheduleBuild2(0).getFuture().get();
        top.getComputation().writeWholeLogTo(System.out);
        assertEquals(1, top.getItems().size());
        MultiBranchProject<?, ?> one = top.getItem("source-one");
        assertThat(one, is(instanceOf(WorkflowMultiBranchProject.class)));
        // Check that it has Git configured:
        List<SCMSource> sources = one.getSCMSources();
        assertEquals(1, sources.size());
        assertEquals("GitSCMSource", sources.get(0).getClass().getSimpleName());

        // Check that the master branch project works:
        r.waitUntilNoActivity();
        WorkflowJob p = findBranchProject((WorkflowMultiBranchProject) one, "master");
        WorkflowRun b1 = p.getLastBuild();
        assertEquals(1, b1.getNumber());
        r.assertLogContains("ran one", b1);
    }

    @Test
    public void testRemoteJenkinsFileWithoutLocalFileFiltering() throws Exception {

        File clones = tmp.newFolder();
        File remoteClones = tmp.newFolder();

        // Add script repository
        sampleRepo1.init();
        sampleRepo1.write(SCRIPT, "echo 'ran one'");
        sampleRepo1.git("add", SCRIPT);
        sampleRepo1.git("commit", "--all", "--message=first_commit");
        sampleRepo1.git("clone", ".", new File(remoteClones, "script-one").getAbsolutePath());

        // Add source code repository
        sampleRepo2.init();
        sampleRepo2.write("source.txt", "source code");
        sampleRepo2.git("add", "source.txt");
        sampleRepo2.git("commit", "--all", "--message=first_commit");
        sampleRepo2.git("clone", ".", new File(clones, "source-one").getAbsolutePath());

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        OrganizationFolder top = r.jenkins.createProject(OrganizationFolder.class, "top");
        top.getNavigators().add(new GitDirectorySCMNavigator(clones.getAbsolutePath()));
        top.getProjectFactories().clear();
        top.getProjectFactories().add(new RemoteJenkinsFileWorkflowMultiBranchProjectFactory(null, SCRIPT,  new GitSCM(sampleRepo1.toString()),false,false));

        // Make sure we created one multi-branch projects:
        top.scheduleBuild2(0).getFuture().get();
        top.getComputation().writeWholeLogTo(System.out);
        assertEquals(1, top.getItems().size());
        MultiBranchProject<?, ?> one = top.getItem("source-one");
        assertThat(one, is(instanceOf(WorkflowMultiBranchProject.class)));
        // Check that it has Git configured:
        List<SCMSource> sources = one.getSCMSources();
        assertEquals(1, sources.size());
        assertEquals("GitSCMSource", sources.get(0).getClass().getSimpleName());

        // Check that the master branch project works:
        r.waitUntilNoActivity();
        WorkflowJob p = findBranchProject((WorkflowMultiBranchProject) one, "master");
        WorkflowRun b1 = p.getLastBuild();
        assertEquals(1, b1.getNumber());
        r.assertLogContains("ran one", b1);
    }

    @Test
    public void testRemoteJenkinsFileWithLocalFileFiltering() throws Exception {

        File clones = tmp.newFolder();
        File remoteClones = tmp.newFolder();

        // Add script repository
        sampleRepo1.init();
        sampleRepo1.write(SCRIPT, "echo 'ran one'");
        sampleRepo1.git("add", SCRIPT);
        sampleRepo1.git("commit", "--all", "--message=first_commit");
        sampleRepo1.git("clone", ".", new File(remoteClones, "script-one").getAbsolutePath());

        // Add source code repository
        sampleRepo2.init();
        sampleRepo2.write("source.txt", "source code");
        sampleRepo2.git("add", "source.txt");
        sampleRepo2.git("commit", "--all", "--message=first_commit");
        sampleRepo2.git("clone", ".", new File(clones, "source-one").getAbsolutePath());

        // Add second source code repository
        sampleRepo3.init();
        sampleRepo3.write("pom.xml", "<pom></pom>");
        sampleRepo3.git("add", "pom.xml");
        sampleRepo3.git("commit", "--all", "--message=first_commit");
        sampleRepo3.git("clone", ".", new File(clones, "source-two").getAbsolutePath());

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        OrganizationFolder top = r.jenkins.createProject(OrganizationFolder.class, "top");
        top.getNavigators().add(new GitDirectorySCMNavigator(clones.getAbsolutePath()));
        top.getProjectFactories().clear();
        top.getProjectFactories().add(new RemoteJenkinsFileWorkflowMultiBranchProjectFactory("pom.xml", SCRIPT,  new GitSCM(sampleRepo1.toString()), false,false));

        // Make sure we created one multi-branch projects:
        top.scheduleBuild2(0).getFuture().get();
        top.getComputation().writeWholeLogTo(System.out);
        assertEquals(1, top.getItems().size());
        MultiBranchProject<?, ?> one = top.getItem("source-two");
        assertThat(one, is(instanceOf(WorkflowMultiBranchProject.class)));
        // Check that it has Git configured:
        List<SCMSource> sources = one.getSCMSources();
        assertEquals(1, sources.size());
        assertEquals("GitSCMSource", sources.get(0).getClass().getSimpleName());

        // Check that the master branch project works:
        r.waitUntilNoActivity();
        WorkflowJob p = findBranchProject((WorkflowMultiBranchProject) one, "master");
        WorkflowRun b1 = p.getLastBuild();
        assertEquals(1, b1.getNumber());
        r.assertLogContains("ran one", b1);
    }

    private static WorkflowJob findBranchProject(WorkflowMultiBranchProject mp, String name) throws IOException, InterruptedException {
        WorkflowJob p = mp.getItem(name);
        showIndexing(mp);
        assertNotNull(name + " project not found", p);
        return p;
    }

    private static void showIndexing(WorkflowMultiBranchProject mp) throws IOException, InterruptedException {
        FolderComputation<?> indexing = mp.getIndexing();
        System.out.println("---%<--- " + indexing.getUrl());
        indexing.writeWholeLogTo(System.out);
        System.out.println("---%<--- ");
    }

    @Test
    public void testRemoteJenkinsFileWithLocalDirectoryFiltering() throws Exception {

        File clones = tmp.newFolder();
        File remoteClones = tmp.newFolder();

        // Add script repository
        sampleRepo1.init();
        sampleRepo1.write(SCRIPT, "echo 'ran one'");
        sampleRepo1.git("add", SCRIPT);
        sampleRepo1.git("commit", "--all", "--message=first_commit");
        sampleRepo1.git("clone", ".", new File(remoteClones, "script-one").getAbsolutePath());

        // Add source code repository
        sampleRepo2.init();
        sampleRepo2.write("source.txt", "source code");
        sampleRepo2.git("add", "source.txt");
        sampleRepo2.git("commit", "--all", "--message=first_commit");
        sampleRepo2.git("clone", ".", new File(clones, "source-one").getAbsolutePath());

        // Add second source code repository
        sampleRepo3.init();
        sampleRepo3.write("pomdir/pom.xml", "<pom></pom>");
        sampleRepo3.git("add", "pomdir/pom.xml");
        sampleRepo3.git("commit", "--all", "--message=first_commit");
        sampleRepo3.git("clone", ".", new File(clones, "source-two").getAbsolutePath());

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        OrganizationFolder top = r.jenkins.createProject(OrganizationFolder.class, "top");
        top.getNavigators().add(new GitDirectorySCMNavigator(clones.getAbsolutePath()));
        top.getProjectFactories().clear();
        top.getProjectFactories().add(new RemoteJenkinsFileWorkflowMultiBranchProjectFactory("pomdir", SCRIPT,  new GitSCM(sampleRepo1.toString()), false,false));

        // Make sure we created one multi-branch projects:
        top.scheduleBuild2(0).getFuture().get();
        top.getComputation().writeWholeLogTo(System.out);
        assertEquals(1, top.getItems().size());
        MultiBranchProject<?, ?> one = top.getItem("source-two");
        assertThat(one, is(instanceOf(WorkflowMultiBranchProject.class)));
        // Check that it has Git configured:
        List<SCMSource> sources = one.getSCMSources();
        assertEquals(1, sources.size());
        assertEquals("GitSCMSource", sources.get(0).getClass().getSimpleName());

        // Check that the master branch project works:
        r.waitUntilNoActivity();
        WorkflowJob p = findBranchProject((WorkflowMultiBranchProject) one, "master");
        WorkflowRun b1 = p.getLastBuild();
        assertEquals(1, b1.getNumber());
        r.assertLogContains("ran one", b1);
    }
}
