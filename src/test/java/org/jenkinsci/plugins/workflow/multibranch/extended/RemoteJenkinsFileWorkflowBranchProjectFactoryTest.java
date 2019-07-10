package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.model.Label;
import hudson.plugins.git.GitSCM;
import hudson.slaves.DumbSlave;
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

import java.io.IOException;

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
    private String projectName = "RemoteJenkinsFileProject";
    private String pipelineScript = "pipeline { agent any; stages { stage('ReadFile') { steps {echo readFile('file')} } } }";
    private String pipelineScriptWithSlave = "pipeline { agent { label '%s' } ; stages { stage('ReadFile') { steps {echo readFile('file')} } } }";

    @Before
    public void setup() throws Exception {
        // Init Source Code Repo with test files and branches
        this.initSourceCodeRepo();
    }

    private void initSourceCodeRepo() throws Exception {
        this.sourceCodeRepo.init();
        for (String branchName : scmBranches) {
            if (!branchName.equals("master"))
                this.sourceCodeRepo.git("checkout", "-b", branchName);
            this.sourceCodeRepo.write("file", this.testFileInitalContent + branchName);
            this.sourceCodeRepo.git("commit", "--all", "--message=InitRepoWithFile");
        }
        this.sourceCodeRepoSCMSource = new GitSCMSource(null, this.sourceCodeRepo.toString(), "", "*", "", false);
    }

    private void initRemoteJenkinsFileRepo(DumbSlave dumbSlave) throws Exception {
        this.remoteJenkinsFileRepo.init();
        if ( dumbSlave == null)
            this.remoteJenkinsFileRepo.write(this.jenkinsFile, this.pipelineScript);
        else
        {
            this.pipelineScriptWithSlave = String.format(this.pipelineScriptWithSlave,dumbSlave.getLabelString());
            this.remoteJenkinsFileRepo.write(this.jenkinsFile, this.pipelineScriptWithSlave);
        }
        this.remoteJenkinsFileRepo.git("add", this.jenkinsFile);
        this.remoteJenkinsFileRepo.git("commit", "--all", "--message=RemoteJenkinsFileRepoTest");
        this.remoteJenkinsFileRepoSCM = new GitSCM(remoteJenkinsFileRepo.toString());
    }

    private void initRemoteJenkinsFileRepoWithPipelineScript() throws Exception {
        this.initRemoteJenkinsFileRepo(null);
    }

    private void initRemoteJenkinsFileRepoWithPipelineScriptWithSlave(DumbSlave dumbSlave) throws Exception {
        this.initRemoteJenkinsFileRepo(dumbSlave);
    }

    private WorkflowMultiBranchProject createProjectWithRemoteJenkinsFile() throws IOException {
        WorkflowMultiBranchProject workflowMultiBranchProject = this.jenkins.createProject(WorkflowMultiBranchProject.class, this.projectName);
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(this.sourceCodeRepoSCMSource));
        RemoteJenkinsFileWorkflowBranchProjectFactory remoteJenkinsFileWorkflowBranchProjectFactory = new RemoteJenkinsFileWorkflowBranchProjectFactory(this.jenkinsFile, this.remoteJenkinsFileRepoSCM);
        workflowMultiBranchProject.setProjectFactory(remoteJenkinsFileWorkflowBranchProjectFactory);
        return workflowMultiBranchProject;
    }

    private void indexMultiBranchPipeline(WorkflowMultiBranchProject workflowMultiBranchProject) throws Exception {
        workflowMultiBranchProject.scheduleBuild2(0);
        this.jenkins.waitUntilNoActivity();
        assertEquals(this.scmBranches.length, workflowMultiBranchProject.getItems().size());
    }

    private void checkBranchJobsAndLogs(WorkflowMultiBranchProject workflowMultiBranchProjectb) throws Exception {
        // Check build num and logs for created Branch Jobs
        for (String branchName : this.scmBranches) {
            WorkflowJob branchJob = workflowMultiBranchProjectb.getJob(branchName);
            WorkflowRun lastBuild = branchJob.getLastBuild();
            lastBuild.writeWholeLogTo(System.out);
            assertEquals(1, lastBuild.getNumber());
            jenkins.assertLogContains(this.testFileInitalContent + branchName, lastBuild);
        }
    }

    private void createProjectAndTest() throws Exception {
        // Create project with Remote Jenkins File Plugin
        WorkflowMultiBranchProject workflowMultiBranchProject = this.createProjectWithRemoteJenkinsFile();
        // Index MultiBranchProject
        this.indexMultiBranchPipeline(workflowMultiBranchProject);
        // Run and check Branch Jobs
        this.checkBranchJobsAndLogs(workflowMultiBranchProject);
    }

    @Test
    public void testRemoteJenkinsFile() throws Exception {
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScript();
        // Create And Test
        this.createProjectAndTest();
    }

    @Test
    public void testRemoteJenkinsFileOnSlave() throws Exception {
        DumbSlave dumbSlave = this.jenkins.createOnlineSlave(Label.parseExpression("slave"));
        // Init Remote Jenkins file with Slave
        this.initRemoteJenkinsFileRepoWithPipelineScriptWithSlave(dumbSlave);
        // Create And Test
        this.createProjectAndTest();
    }


}
