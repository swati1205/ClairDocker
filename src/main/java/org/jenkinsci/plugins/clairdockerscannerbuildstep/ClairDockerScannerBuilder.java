package org.jenkinsci.plugins.clairdockerscannerbuildstep;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Util;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.model.BuildListener;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * This is the builder class.
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. *
 * 
 */
public class ClairDockerScannerBuilder extends Builder {

	public static final int OK_CODE = 0;
	public static final int DISALLOWED_CODE = 4;
	private final String localImage;
	private int high = 0;
	private int low = 0;
	private int medium = 0;
	private boolean Severity = false;
	String localimage1;
	private static int count;
	private static int buildId = 0;
	private final String localAnalyzerPath;

	public synchronized static void setCount(int count) {
		ClairDockerScannerBuilder.count = count;
	}

	public synchronized static void setBuildId(int buildId) {
		ClairDockerScannerBuilder.buildId = buildId;
	}

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public ClairDockerScannerBuilder(String localImage, int high, int low, int medium, Boolean Severity,
			String localAnalyzerPath) {

		this.localImage = localImage;
		this.localAnalyzerPath = localAnalyzerPath;
		this.Severity = (Severity != null) && Severity;
		if (Severity != false) {
			this.low = low;
			this.high = high;
			this.medium = medium;

		}

	}

	/**
	 * Public access required by config.jelly to display current values in
	 * configuration screen.
	 */
	public String getLocalImage() {
		return localImage;
	}

	public String getlocalAnalyzerPath() {
		return localAnalyzerPath;
	}

	public int getHigh() {
		return high;
	}

	public int getLow() {
		return low;
	}

	public int getMedium() {
		return medium;
	}

	public boolean isSeverity() {
		return Severity;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
			throws java.lang.InterruptedException, IOException {
		EnvVars env = build.getEnvironment(listener);

		System.out.println("list of env" + env);
		System.out.println("Build_Number" + env.get("BUILD_NUMBER"));
		String build_no = env.get("BUILD_NUMBER");
		String jenkins_home = env.get("JENKINS_HOME");
		localimage1 = env.expand(localImage);

		System.out.println("local imgae " + localimage1);
		// String localAnalyzerPath = getDescriptor().getlocalAnalyzerPath();
		String apiURL = getDescriptor().getApiURL();
		if (apiURL == null || apiURL.trim().equals("")) {
			listener.getLogger().println(
					"\nFATAL: Configuration not found. Please set the global configuration parameters in the \"Clair Docker Vulnerability Scanner\" section under  \"Manage Jenkins/Configure System\" and continue.\n");
			return false;
		}

		// Allow API urls without the protocol part, add the "https://" in this
		// case
		if (apiURL.indexOf("://") == -1) {
			apiURL = "http://" + apiURL;
		}

		// Support unique names for artifacts when there are multiple steps in
		// the same build
		String artifactSuffix, artifactName;
		if (build.hashCode() != buildId) {
			// New build
			setBuildId(build.hashCode());
			setCount(1);
			artifactSuffix = null; // When ther is only one step, there should
									// be no suffix at all
			artifactName = "scanout.html";
		} else {
			setCount(count + 1);
			artifactSuffix = Integer.toString(count);
			artifactName = "scanout-" + artifactSuffix + ".html";
		}
		// artifactName = "scanout-" + localimage1 + ".html";

		int exitCode = ScannerExecuter.execute(build, launcher, listener, artifactName, apiURL, localimage1,
				localAnalyzerPath, build_no, jenkins_home);
		build.addAction(new ClairScannerAction(build, artifactSuffix, artifactName, localimage1));

		archiveArtifacts(build, launcher, listener);

		boolean result = Text_HTMLConverter.checkQualityGate(high, low, medium, Severity, build);
		if (result == true) {
			throw new AbortException("Quality Gate failed.");
		}

		switch (exitCode) {
		case OK_CODE:
			return true;
		case DISALLOWED_CODE:
			return false;
		default:
			// This exception causes the message to appear in the Jenkins
			// console
			throw new AbortException("Scanning failed.");
		}
	}

	// Archive all artifacts
	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
	// No idea why this is needed
	private void archiveArtifacts(AbstractBuild build, Launcher launcher, BuildListener listener)
			throws java.lang.InterruptedException {
		ArtifactArchiver artifactArchiver = new ArtifactArchiver("*");
		artifactArchiver.perform(build, build.getWorkspace(), launcher, listener);
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();

	}

	/**
	 * Descriptor for {@link ClairDockerScannerBuilder}. Used as a singleton.
	 * The class is marked as public so that it can be accessed from views.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		/**
		 * To persist global configuration information, simply store it in a
		 * field and call save().
		 */
		private String localAnalyzerPath;
		private String apiURL;

		/**
		 * In order to load the persisted global configuration, you have to call
		 * load() in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		// On-the-fly validation of the job level configuration of fields

		public FormValidation doCheckLocalImage(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please enter name of the image");
			return FormValidation.ok();
		}

		public FormValidation doCheckLocalAnalyzerPath(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please enter Analyzer path");
			return FormValidation.ok();
		}

		// On-the-fly validation of the global level configuration of apiURL
		// field

		public FormValidation doCheckApiURL(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please enter the Clair endpoint URL");
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Docker Security Scanner";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
			// localAnalyzerPath = formData.getString("localAnalyzerPath");
			apiURL = formData.getString("apiURL");
			save();
			return super.configure(req, formData);
		}

		/*
		 * public String getlocalAnalyzerPath() { return localAnalyzerPath; }
		 */

		public String getApiURL() {
			return apiURL;
		}
	}
}
