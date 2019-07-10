Remote Jenkins File Plugin
==========================

This plugin enables definition of external Jenkins file from another repository for MultiBranch Pipelines.

# Description
Jenkins MultiBranch Pipeline feature is great to manage multi branched repositories which need to have
dynamically created Pipeline Jobs on-demand. To use this feature you need to define 
<a target="_blank" href="https://github.com/jenkinsci/workflow-plugin/blob/master/TUTORIAL.md#understanding-flow-scripts">Jenkins Pipeline Script</a>
in the repository alongside with the source code. 

But when it comes to adding new definitions/stages/steps or maintaining
the Jenkins Pipeline Script for other reasons, you need to make all the changes within all branches or make changes in master branch 
and wait for other branches getting the update. 
For eliminating this you can used <a href="https://jenkins.io/doc/book/pipeline/shared-libraries/">Jenkins Shared Library</a>.
However when you are in an large-scaled/enterprise environments with hundreds/thousands of developers with lots of repositories,
you need to somehow protect the content/stability of the Jenkins Pipeline Scripts to avoid failures. 

In this point Remote Jenkins File Plugin comes in. With this plugin you can define/set Jenkins files from another
repository while still able to use MultiBranch Pipeline Project features. This way you will be able to centralize all Jenkins files 
in another repository where you can review or restrict changes and use MultiBranch Pipeline for multi branched repositories.

In summary, Using MultiBranch Pipeline Jobs with Remote Jenkins File Plugin you can;

- Centralize Jenkins Files in another repository
- Easily maintain
- Review or restrict changes
- Apply changes to all Pipelines in seconds

You can see details on [Remote File Plugin Wiki Pages](https://wiki.jenkins.io/display/JENKINS/Remote+File+Plugin)
