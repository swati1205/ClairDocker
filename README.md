## Clair security plugin - Jenkins Plugin ##

*   This Jenkins plugin enables scanning of Docker images using Clair.

## Prerequisites for the plugin to be operational ##

*	Analyzer tool should be present on localhost, else create the Jenkins slave in machine where the Analyzer tool is installed.
*	In the latter condition, restrict the job build to the slave.

## Usage of plugin in Jenkins ##

*	In the global configuration page ("Manage Jenkins"/"Configure System") in the section for this plugin, enter values for Clair endpoint URL, address of system in which clair server is present. (the format should be :”http://ip_address:port”)
*	In the configuration page for your project, add "Clair Docker Vulnerability Scanner" step from the "Add build step" dropdown list. Enter the value for Image name that is to be scanned. Enter the Local Analyzer Path (where the analyzer tool is installed).
*	To set the quality gate, check the Set Quality Gates, set the values for High, Medium and Low in “Threshold to mark build failed field”.
*	When run successfully, an artifact named "scanout.html" will be created in the project's workspace. 
	If more than one "Clair Docker vulnerability scanner" step is added to a build, the additional artifact will be suffixed with consecutive numbers.
	
## Installing manually ##

*	Copy the *target/ clair-docker-scanner.hpi* file to *$JENKINS/plugins/* where *JENKINS* is the Jenkins root directory, by default it is */var/lib/jenkins/*.
