package org.jenkinsci.plugins.clairdockerscannerbuildstep;

import hudson.model.Action;
import hudson.model.AbstractBuild;

public class ClairScannerAction implements Action {

	private String resultsUrl;
	private AbstractBuild<?, ?> build;
	private String artifactSuffix;
	private String localImage;

	public ClairScannerAction(AbstractBuild<?, ?> build, String artifactSuffix, String artifactName,
			String localImage) {
		this.build = build;
		this.artifactSuffix = artifactSuffix;
		this.resultsUrl = "../artifact/" + artifactName;
		this.localImage = localImage;
	}

	@Override
	public String getIconFileName() {
		// return the path to the icon file
		return "/plugin/clair-docker-scanner/images/clair.png";
	}

	@Override
	public String getDisplayName() {
		// return the label for your link
		// return "Clair Docker Scanner - " + localImage;
		return "Docker Security Analysis - " + localImage;
	}

	@Override
	public String getUrlName() {
		// defines the suburl, which is appended to ...jenkins/job/jobname
		if (artifactSuffix == null) {
			return "clair-results";
		} else {
			return "clair-results-" + artifactSuffix;
		}
	}

	public AbstractBuild<?, ?> getBuild() {
		return this.build;
	}

	public String getResultsUrl() {
		return this.resultsUrl;
	}
}
