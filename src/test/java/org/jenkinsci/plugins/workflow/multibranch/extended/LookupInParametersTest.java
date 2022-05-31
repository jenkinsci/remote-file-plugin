package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
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

import java.io.IOException;

import static org.junit.Assert.assertEquals;


public class LookupInParametersTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    @Rule
    public GitSampleRepoRule sourceCodeRepo = new GitSampleRepoRule();
    @Rule
    public GitSampleRepoRule remoteJenkinsFileRepo = new GitSampleRepoRule();

    private GitSCMSource sourceCodeRepoSCMSource;
    private GitSCM remoteJenkinsFileRepoSCM;
    private String testFileInitialContent = "Initial Content of Test File";
    private String jenkinsFileDefault = "jenkinsFile";
    private String jenkinsFile2 = "jenkinsFile2";
    private String jenkinsFileParameter = "${JenkinsFileParam}";
    private String[] scmBranches = {"master"};
    private String projectName = "RemoteJenkinsFileProject";
    private String pipelineScript1 = "pipeline{     agent any;     parameters {       string defaultValue: 'Jenkinsfile', description: '', name: 'JenkinsFileParam', trim: false};     stages{         stage('Test'){             steps{                 echo \"pipelineScriptDefault\"             }         }     } }";
    private String pipelineScript2 = "pipeline{     agent any;     parameters {       string defaultValue: 'Jenkinsfile', description: '', name: 'JenkinsFileParam', trim: false};     stages{         stage('Test'){             steps{                 echo \"pipelineScript2\"             }         }     } }";
    private String localFile = "pom.xml";

    @Before
    public void setup() throws Exception {
        // Init Source Code Repo with test files and branches
        this.initSourceCodeRepo();
    }

    @Test
    public void testRemoteJenkinsFile() throws Exception {
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScript();
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
                this.sourceCodeRepo.write(this.localFile, this.testFileInitialContent + branchName);
                this.sourceCodeRepo.git("add", this.localFile);
            }
            this.sourceCodeRepo.write("file", this.testFileInitialContent + branchName);
            this.sourceCodeRepo.git("commit", "--all", "--message=InitRepoWithFile");
        }
        this.sourceCodeRepoSCMSource = new GitSCMSource(null, this.sourceCodeRepo.toString(), "", "*", "", false);
    }

    private void initRemoteJenkinsFileRepo() throws Exception {
        this.remoteJenkinsFileRepo.init();
            this.remoteJenkinsFileRepo.write(this.jenkinsFileDefault, this.pipelineScript1);
            this.remoteJenkinsFileRepo.write(this.jenkinsFile2, this.pipelineScript2);
        this.remoteJenkinsFileRepo.git("add", this.jenkinsFileDefault);
        this.remoteJenkinsFileRepo.git("add", this.jenkinsFile2);
        this.remoteJenkinsFileRepo.git("commit", "--all", "--message=RemoteJenkinsFileRepoTest");
        this.remoteJenkinsFileRepoSCM = new GitSCM(remoteJenkinsFileRepo.toString());
    }

    private void initRemoteJenkinsFileRepoWithPipelineScript() throws Exception {
        this.initRemoteJenkinsFileRepo();
    }


    private WorkflowMultiBranchProject createProjectWithRemoteJenkinsFile() throws IOException {
        WorkflowMultiBranchProject workflowMultiBranchProject = this.jenkins.createProject(WorkflowMultiBranchProject.class, this.projectName);
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(this.sourceCodeRepoSCMSource));
        RemoteJenkinsFileWorkflowBranchProjectFactory remoteJenkinsFileWorkflowBranchProjectFactory = new RemoteJenkinsFileWorkflowBranchProjectFactory(this.jenkinsFileParameter, "", this.remoteJenkinsFileRepoSCM, false, "",true);
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
            jenkins.assertLogContains("pipelineScriptDefault", lastBuild);
            jenkins.waitUntilNoActivity();
            branchJob.scheduleBuild2(0, new ParametersAction(new StringParameterValue("JenkinsFileParam","jenkinsFile2")));
            jenkins.waitUntilNoActivity();
            lastBuild = branchJob.getLastBuild();
            lastBuild.writeWholeLogTo(System.out);
            jenkins.assertLogContains("pipelineScript2", lastBuild);
            jenkins.waitUntilNoActivity();
        }
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
