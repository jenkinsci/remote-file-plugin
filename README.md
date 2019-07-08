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

# Requirements
You need Jenkins 2.182 version to use this plugin.

# How To
To use this Plugin first you need to install it from Jenkins Plugin Site, for details see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/)

After installing plugin for using the plugin you can follow the steps with examples below,

Assume that you have a project to build, named "MyCrazyProject", which is stored in repository
*https://github.com/aytuncbeken/MyCrazyProject.git*

* Create a new repository for storing Jenkins files for your projects. For example,  it is named "MyJenkinsLibrary" and stored in 
*https://github.com/aytuncbeken/MyJenkinsLibrary.git*
* Create a directory in this repository for your project. For example, create directory called "MyCrazyProject"
* Commit/Push your Jenkins File, named "Jenkinsfile" under this directory
* Open you Jenkins and create a new MultiBranch Project. For example, named "MyCrazyProject Job"
* Add Source for your project to build. For example, fill information for "MyCrazyProject"
* In the "Build Configuration", change "Mode" to "by Remote Jenkins File Plugin". 
You will see new SCM definition is occurred under.
* Define your script path. For example, it will be "MyCrazyProject/Jenkinsfile"
* Define your SCM for Jenkins file. For example, it will be the repository *https://github.com/aytuncbeken/MyJenkinsLibrary.git*
* Click Save.
* In the "Scan MultiBranch Pipeline Log", you will see logs "Ignoring Jenkins file checking in Source Code SCM" which is normal
as this plugin ignores Jenkins files in your repository and inject Jenkins File from other repository which you stored your Jenkins files.

You can see screenshot of example definition.

[MultiBranch Pipeline Definition 1](images/multibranch-pipeline-definition-1.png)

[MultiBranch Pipeline Definition 1](images/multibranch-pipeline-definition-2.png)

# Note
For filtering branches in the project, you can use "Filter by name" feature.
