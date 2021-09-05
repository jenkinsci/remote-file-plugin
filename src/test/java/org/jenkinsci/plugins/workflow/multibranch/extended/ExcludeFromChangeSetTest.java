package org.jenkinsci.plugins.workflow.multibranch.extended;

import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.GitSCMExtension;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.multibranch.extended.scm.ExcludeFromChangeSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;


public class ExcludeFromChangeSetTest {

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
    private final String pipelineScript = "pipeline { agent any; stages { stage('ReadFile') { steps {echo readFile('file')} } } }";
    private final String localFile = "pom.xml";
    private final String changeNotVisible = "ChangeNotVisible";

    @Before
    public void setup() throws Exception {
        // Init Source Code Repo with test files and branches
        this.initSourceCodeRepo();
    }

    @Test
    public void testRemoteJenkinsFile() throws Exception {
        // Init Remote Jenkins File Repo with test Jenkinsfile
        this.initRemoteJenkinsFileRepoWithPipelineScript(new ExcludeFromChangeSet());
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
        RemoteJenkinsFileWorkflowBranchProjectFactory remoteJenkinsFileWorkflowBranchProjectFactory = new RemoteJenkinsFileWorkflowBranchProjectFactory(this.jenkinsFile, "", this.remoteJenkinsFileRepoSCM, false, "", false);
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
            branchJob.scheduleBuild2(0);
            this.jenkins.waitUntilNoActivity();
            lastBuild = branchJob.getLastBuild();
            assertEquals(2, lastBuild.getNumber());
            lastBuild.getChangeSets().forEach(changeLogSet -> Arrays.asList(changeLogSet.getItems()).forEach(o -> {
                GitChangeSet gitChangeSet = (GitChangeSet) o;
                if (gitChangeSet.getMsg().contains(changeNotVisible)) {
                    Assert.fail("Change Commit is visible");
                }
            }));
        }
    }

    private void addDummyCommit() throws Exception {
        this.remoteJenkinsFileRepo.write("file", this.jenkinsFile + "\n//Test");
        this.remoteJenkinsFileRepo.git("commit", "--all", "--message=ChangeNotVisible");

        this.sourceCodeRepo.write("file", this.testFileInitalContent + "\n#Test");
        this.sourceCodeRepo.git("commit", "--all", "--message=ChangeVisible");
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
