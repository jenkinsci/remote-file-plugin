package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.triggers.SCMTrigger;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.multibranch.extended.scm.ExcludeFromPoll;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;


public class ExcludeFromPollTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    @Rule
    public GitSampleRepoRule sourceCodeRepo = new GitSampleRepoRule();
    @Rule
    public GitSampleRepoRule remoteJenkinsFileRepo = new GitSampleRepoRule();

    private GitSCMSource sourceCodeRepoSCMSource;
    private GitSCM remoteJenkinsFileRepoSCM;
    private final String testFileInitalContent = "Initial Content of Test File";
    private final String jenkinsFile = "Jenkinsfile";
    private final String[] scmBranches = {"master"};
    private final String projectName = "RemoteJenkinsFileProject";
    private final String pipelineScript = "pipeline { triggers { pollSCM '* * * * *'}; agent any; stages { stage('ReadFile') { steps {echo readFile('file')} } } }";
    private final String localFile = "pom.xml";

    @Before
    public void setup() throws Exception {
        // Init Source Code Repo with test files and branches
        this.initSourceCodeRepo();
    }

    @After
    public void dispose() throws Exception {
        // On Windows, test is not executed, because of below error:
        // ExcludeFromPollTest.testRemoteJenkinsFile » FileSystem ...\remote-file-plugin\target\tmp\j h14274673471981878038\workspace\RemoteJenkinsFileProject_master@script\a0f6ab0a0a5fefdeec6a896ffe3ae660a88d73d9fe90d600368f502366cf31c7: The process cannot access the file because it is being used by another process
        // This error occurs when invoking org.jvnet.hudson.test.TestEnvironment.dispose(TestEnvironment.java:84)
        // Basically it is trying to delete temporary directory
        // After adding a small timeout, it is working
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    public void testRemoteJenkinsFile() throws Exception {
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScript(new ExcludeFromPoll());
        // Create And TestsourceCodeRepo
        this.createProjectAndTest();
    }

    private void initSourceCodeRepo() throws Exception {
        this.sourceCodeRepo.init();
        for (String branchName : scmBranches) {
            if (!branchName.equals("master")) {
                this.sourceCodeRepo.git("checkout", "-b", branchName,"master");
                this.sourceCodeRepo.git("rm", this.localFile);
            } else {
                this.sourceCodeRepo.write(this.localFile, this.testFileInitalContent + branchName);
                this.sourceCodeRepo.git("add", this.localFile);
            }
            this.sourceCodeRepo.write("file", this.testFileInitalContent + branchName);
            this.sourceCodeRepo.git("commit", "--all", "--message=InitRepoWithFile");
        }
        this.sourceCodeRepoSCMSource = new GitSCMSource(null, this.sourceCodeRepo.toString(), "", "*", "", false);
    }

    private void initRemoteJenkinsFileRepo(GitSCMExtension gitSCMExtension) throws Exception {
        this.remoteJenkinsFileRepo.init();
            this.remoteJenkinsFileRepo.write(this.jenkinsFile, this.pipelineScript);
        this.remoteJenkinsFileRepo.git("add", this.jenkinsFile);
        this.remoteJenkinsFileRepo.git("commit", "--all", "--message=RemoteJenkinsFileRepoTest");
        this.remoteJenkinsFileRepoSCM = new GitSCM(remoteJenkinsFileRepo.toString());
        if( gitSCMExtension != null)
            this.remoteJenkinsFileRepoSCM.getExtensions().add(gitSCMExtension);
    }

    private void initRemoteJenkinsFileRepoWithPipelineScript(GitSCMExtension gitSCMExtension) throws Exception {
        this.initRemoteJenkinsFileRepo(gitSCMExtension);
    }


    private WorkflowMultiBranchProject createProjectWithRemoteJenkinsFile() throws IOException {
        WorkflowMultiBranchProject workflowMultiBranchProject = this.jenkins.createProject(WorkflowMultiBranchProject.class, this.projectName);
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(this.sourceCodeRepoSCMSource));
        RemoteJenkinsFileWorkflowBranchProjectFactory remoteJenkinsFileWorkflowBranchProjectFactory = new RemoteJenkinsFileWorkflowBranchProjectFactory(this.jenkinsFile, "", this.remoteJenkinsFileRepoSCM, false, "",false);
        workflowMultiBranchProject.setProjectFactory(remoteJenkinsFileWorkflowBranchProjectFactory);
        return workflowMultiBranchProject;
    }

    private void indexMultiBranchPipeline(WorkflowMultiBranchProject workflowMultiBranchProject, int expectedBranches) throws Exception {
        workflowMultiBranchProject.scheduleBuild2(0);
        this.jenkins.waitUntilNoActivity();
        assertEquals(expectedBranches, workflowMultiBranchProject.getItems().size());
    }

    private void checkBranchJobsAndLogs(WorkflowMultiBranchProject workflowMultiBranchProject) throws Exception {
        // Check build num and logs for created Branch Jobs
        for (String branchName : this.scmBranches) {
            WorkflowJob branchJob = workflowMultiBranchProject.getJob(branchName);
            WorkflowRun lastBuild = branchJob.getLastBuild();
            lastBuild.writeWholeLogTo(System.out);
            assertEquals(1, lastBuild.getNumber());
            this.addDummyCommit();
            SCMTrigger scmTrigger = branchJob.getSCMTrigger();
            branchJob.getSCMTrigger().run();
            this.jenkins.waitUntilNoActivity();
            lastBuild = branchJob.getLastBuild();
            assertEquals(1, lastBuild.getNumber());
        }
    }

    private void addDummyCommit() throws Exception {
        this.remoteJenkinsFileRepo.write("file", this.jenkinsFile + "\n//Test");
        this.remoteJenkinsFileRepo.git("commit", "--all", "--message=NoPolling");
    }

    private void createProjectAndTest() throws Exception {
        // Create project with Remote Jenkins File Plugin
        WorkflowMultiBranchProject workflowMultiBranchProject = this.createProjectWithRemoteJenkinsFile();
        // Index MultiBranchProject
        this.indexMultiBranchPipeline(workflowMultiBranchProject, scmBranches.length);
        // Run and check Branch Jobs
        this.checkBranchJobsAndLogs(workflowMultiBranchProject);
    }


}
