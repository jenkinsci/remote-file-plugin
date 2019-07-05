package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.plugins.git.GitSCM;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

@SuppressWarnings("ALL")
public class RemoteJenkinsFileWorkflowBranchProjectFactoryTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    @Rule
    public GitSampleRepoRule sourceCodeRepo = new GitSampleRepoRule();
    @Rule
    public GitSampleRepoRule remoteJenkinsFileRepo = new GitSampleRepoRule();

    private GitSCMSource sourceCodeRepoSCMSource;
    private GitSCM remoteJenkinsFileRepoSCM;
    private String testFileInitalContent = "Initial Content of Test File";
    private String jenkinsFile = "Jenkinsfile";
    private String[] scmBranches = {"master", "feature"};

    @Before
    public void setup() throws Exception {
        // Init Source Code Repo with test files and branches
        sourceCodeRepo.init();
        for (String branchName : scmBranches) {
            if (!branchName.equals("master"))
                sourceCodeRepo.git("checkout", "-b", branchName);
            sourceCodeRepo.write("file", this.testFileInitalContent + branchName);
            sourceCodeRepo.git("commit", "--all", "--message=InitRepoWithFile");
        }
        sourceCodeRepoSCMSource = new GitSCMSource(null, sourceCodeRepo.toString(), "", "*", "", false);

        // Init Remote Jenkins File Repo with test Jenkinsfile
        remoteJenkinsFileRepo.init();
        remoteJenkinsFileRepo.write(this.jenkinsFile, "pipeline { agent any; stages { stage('ReadFile') { steps {echo readFile('file')} } } }");
        remoteJenkinsFileRepo.git("add", this.jenkinsFile);
        remoteJenkinsFileRepo.git("commit", "--all", "--message=RemoteJenkinsFileRepoTest");
        remoteJenkinsFileRepoSCM = new GitSCM(remoteJenkinsFileRepo.toString());
    }

    @Test
    public void testRemoteJenkinsFile() throws Exception {
        // Create project with Remote Jenkins File Plugin
        WorkflowMultiBranchProject workflowMultiBranchProject = jenkins.createProject(WorkflowMultiBranchProject.class, "RemoteJenkinsFileTestProject");
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(sourceCodeRepoSCMSource));
        RemoteJenkinsFileWorkflowBranchProjectFactory remoteJenkinsFileWorkflowBranchProjectFactory = new RemoteJenkinsFileWorkflowBranchProjectFactory(this.jenkinsFile, remoteJenkinsFileRepoSCM);
        workflowMultiBranchProject.setProjectFactory(remoteJenkinsFileWorkflowBranchProjectFactory);

        // Index MultiBranchProject
        this.indexMultiBranchPipeline(workflowMultiBranchProject);
        this.checkBranchJobsAndLogs(workflowMultiBranchProject);
    }

    private void indexMultiBranchPipeline(WorkflowMultiBranchProject workflowMultiBranchProject) throws Exception {
        workflowMultiBranchProject.scheduleBuild2(0);
        jenkins.waitUntilNoActivity();
        assertEquals(this.scmBranches.length, workflowMultiBranchProject.getItems().size());
    }

    private void checkBranchJobsAndLogs(WorkflowMultiBranchProject workflowMultiBranchProjectb) throws Exception {
        // Check build num and logs for created Branch Jobs
        for (String branchName : this.scmBranches) {
            WorkflowJob branchJob = workflowMultiBranchProjectb.getJob(branchName);
            WorkflowRun lastBuild = branchJob.getLastBuild();
            assertEquals(1, lastBuild.getNumber());
            jenkins.assertLogContains(this.testFileInitalContent + branchName, lastBuild);
        }
    }
}
