package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.EnvVars;
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
    private String[] scmBranches = {"master", "feature","hotfix"};
    private String defaultFallBackBranch = "master";
    private String testFallbackBranch = "fallback";
    private String projectName = "RemoteJenkinsFileProject";
    private String pipelineScript = "pipeline { agent any; stages { stage('ReadFile') { steps {echo readFile('file')} } } }";
    private String pipelineScriptWithSlave = "pipeline { agent { label '%s' } ; stages { stage('ReadFile') { steps {echo readFile('file')} } } }";
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
        this.createProjectAndTest(false,this.defaultFallBackBranch);
    }

    @Test
    public void testRemoteJenkinsFileMatchBranchesTrueWithDifferentFallbackBranch() throws Exception {
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScriptWithFallbackBranch();
        // Create And Test
        this.createProjectAndTest(true,this.testFallbackBranch);
    }

    @Test
    public void testRemoteJenkinsFileMatchBranchesTrueWithDumpSlaveWithDifferentFallbackBranch() throws Exception {
        DumbSlave dumbSlave = this.jenkins.createOnlineSlave(Label.parseExpression("slave"));
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScriptWithSlaveWithFallbackBranch(dumbSlave);
        // Create And Test
        this.createProjectAndTest(true,this.testFallbackBranch);
    }

    @Test
    public void testRemoteJenkinsFileOnSlave() throws Exception {
        DumbSlave dumbSlave = this.jenkins.createOnlineSlave(Label.parseExpression("slave"));
        // Init Remote Jenkins file with Slave
        this.initRemoteJenkinsFileRepoWithPipelineScriptWithSlave(dumbSlave);
        // Create And Test
        this.createProjectAndTest(false,this.defaultFallBackBranch);
    }

    @Test
    public void testRemoteJenkinsFileMatchBranchesTrue() throws Exception {
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScript();
        // Create And Test
        this.createProjectAndTest(true,this.defaultFallBackBranch);
    }

    @Test
    public void testRemoteJenkinsFileWithLocalFile() throws Exception {
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScript();
        // Create And Test
        this.createProjectAndTest(false, true,this.defaultFallBackBranch);
    }

    @Test
    public void testRemoteJenkinsFileWithLocalDirectory() throws Exception {
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScript();
        // Create And Test
        this.createProjectAndTest(false, true,this.defaultFallBackBranch);
    }

    @Test
    public void testRemoteJenkinsFileMatchBranchesTrueOnSlave() throws Exception {
        DumbSlave dumbSlave = this.jenkins.createOnlineSlave(Label.parseExpression("slave"));
        // Init Remote Jenkins file with Slave
        this.initRemoteJenkinsFileRepoWithPipelineScriptWithSlave(dumbSlave);
        // Create And Test
        this.createProjectAndTest(true, this.defaultFallBackBranch);
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

    private void initRemoteJenkinsFileRepo(DumbSlave dumbSlave, Boolean createFallbackBranchForJenkinsFile) throws Exception {
        this.remoteJenkinsFileRepo.init();
        if (dumbSlave == null)
            this.remoteJenkinsFileRepo.write(this.jenkinsFile, this.pipelineScript);
        else {
            this.pipelineScriptWithSlave = String.format(this.pipelineScriptWithSlave, dumbSlave.getLabelString());
            this.remoteJenkinsFileRepo.write(this.jenkinsFile, this.pipelineScriptWithSlave);
        }
        this.remoteJenkinsFileRepo.git("add", this.jenkinsFile);
        this.remoteJenkinsFileRepo.git("commit", "--all", "--message=RemoteJenkinsFileRepoTest");
        if( createFallbackBranchForJenkinsFile) {
            this.remoteJenkinsFileRepo.git("checkout", "-b", this.testFallbackBranch,"master");
            this.remoteJenkinsFileRepo.git("branch", "-D", "master");
        }
        this.remoteJenkinsFileRepoSCM = new GitSCM(remoteJenkinsFileRepo.toString());
    }

    private void initRemoteJenkinsFileRepoWithPipelineScript() throws Exception {
        this.initRemoteJenkinsFileRepo(null,false);
    }

    private void initRemoteJenkinsFileRepoWithPipelineScriptWithSlave(DumbSlave dumbSlave) throws Exception {
        this.initRemoteJenkinsFileRepo(dumbSlave,false);
    }

    private void initRemoteJenkinsFileRepoWithPipelineScriptWithSlaveWithFallbackBranch(DumbSlave dumbSlave) throws Exception {
        this.initRemoteJenkinsFileRepo(dumbSlave,true);
    }

    private void initRemoteJenkinsFileRepoWithPipelineScriptWithFallbackBranch() throws Exception {
        this.initRemoteJenkinsFileRepo(null,true);
    }


    private WorkflowMultiBranchProject createProjectWithRemoteJenkinsFile(boolean matchBranches, String localFile, String fallBackBranch) throws IOException {
        WorkflowMultiBranchProject workflowMultiBranchProject = this.jenkins.createProject(WorkflowMultiBranchProject.class, this.projectName);
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(this.sourceCodeRepoSCMSource));
        RemoteJenkinsFileWorkflowBranchProjectFactory remoteJenkinsFileWorkflowBranchProjectFactory = new RemoteJenkinsFileWorkflowBranchProjectFactory(this.jenkinsFile, localFile, this.remoteJenkinsFileRepoSCM, matchBranches, fallBackBranch);
        workflowMultiBranchProject.setProjectFactory(remoteJenkinsFileWorkflowBranchProjectFactory);
        return workflowMultiBranchProject;
    }

    private void indexMultiBranchPipeline(WorkflowMultiBranchProject workflowMultiBranchProject, int expectedBranches) throws Exception {
        workflowMultiBranchProject.scheduleBuild2(0);
        this.jenkins.waitUntilNoActivity();
        assertEquals(expectedBranches, workflowMultiBranchProject.getItems().size());
    }

    private void checkBranchJobsAndLogs(WorkflowMultiBranchProject workflowMultiBranchProject, boolean checkForMatchBranch, boolean isLocalFileDefined, String fallBackBranch) throws Exception {
        // Check build num and logs for created Branch Jobs
        for (String branchName : this.scmBranches) {
            String branchNameToCheck = branchName;
            WorkflowJob branchJob = workflowMultiBranchProject.getJob(branchName);
            if ("master".equals(branchName) || !isLocalFileDefined) {
                WorkflowRun lastBuild = branchJob.getLastBuild();
                lastBuild.writeWholeLogTo(System.out);
                assertEquals(1, lastBuild.getNumber());
                jenkins.assertLogContains(this.testFileInitalContent + branchName, lastBuild);
                if (checkForMatchBranch && branchName != fallBackBranch) {
                    jenkins.assertLogContains("Failed to checkout", lastBuild);
                    jenkins.assertLogContains("Try to checkout " + fallBackBranch, lastBuild);
                    branchNameToCheck = fallBackBranch;
                }
                //Check Environment Variables
                EnvVars environment = lastBuild.getEnvironment();
                assertTrue(environment.containsKey(RemoteJenkinsFileItemListener.RJPP_SCM_ENV_NAME));
                assertTrue(environment.containsKey(RemoteJenkinsFileItemListener.RJPP_JFILE_ENV_NAME));
                assertTrue(environment.containsKey(RemoteJenkinsFileItemListener.RJPP_BRANCH_ENV_NAME));
                assertEquals(this.jenkinsFile, environment.get(RemoteJenkinsFileItemListener.RJPP_JFILE_ENV_NAME));
                assertEquals(this.remoteJenkinsFileRepoSCM.getRepositories().get(0).getURIs().get(0).toString(), environment.get(RemoteJenkinsFileItemListener.RJPP_SCM_ENV_NAME));
                assertEquals(branchNameToCheck, environment.get(RemoteJenkinsFileItemListener.RJPP_BRANCH_ENV_NAME));
            } else {
                assertNull(branchJob);
            }
        }
    }

    private void createProjectAndTest(boolean matchBranches, boolean isLocalFileDefined, String fallbackBranch) throws Exception {
        int expectedBranches;
        String localFile;
        if(isLocalFileDefined) {
            // Only master branch will contain the local file
            expectedBranches = 1;
            localFile = this.localFile;
        } else {
            expectedBranches = this.scmBranches.length;
            localFile = null;
        }

        // Create project with Remote Jenkins File Plugin
        WorkflowMultiBranchProject workflowMultiBranchProject = this.createProjectWithRemoteJenkinsFile(matchBranches, localFile, fallbackBranch);
        // Index MultiBranchProject
        this.indexMultiBranchPipeline(workflowMultiBranchProject, expectedBranches);
        // Run and check Branch Jobs
        this.checkBranchJobsAndLogs(workflowMultiBranchProject, matchBranches, isLocalFileDefined, fallbackBranch);
    }

    private void createProjectAndTest(boolean matchBranches, String fallbackBranch) throws Exception {
        this.createProjectAndTest(matchBranches, false, fallbackBranch);
    }


}
