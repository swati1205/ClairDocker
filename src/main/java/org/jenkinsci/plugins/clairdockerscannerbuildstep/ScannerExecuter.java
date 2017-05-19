package org.jenkinsci.plugins.clairdockerscannerbuildstep;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Launcher.ProcStarter;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.ArgumentListBuilder;
//import org.apache.http.HttpResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

//import org.apache.http.HttpResponse;

import hudson.model.Computer;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.slaves.Channels;
import java.net.*;

/**
 * This class does the actual execution..
 * 
 */
public class ScannerExecuter {
	static int countHigh = 0;
	static int countMed = 0;
	static int countLow = 0;
	static int countNeg = 0;

	public static int execute(AbstractBuild build, Launcher launcher, BuildListener listener, String artifactName,
			String apiURL, String localImage, String localAnalyzerPath, String build_no, String jenkins_home)
					throws IOException, InterruptedException {
		int exitCode = 0;
		PrintStream print_stream = null;
		// try {
		// Form input might be in $VARIABLE or ${VARIABLE} form, expand.
		// expand() is a noop for strings not in the above form.
		final EnvVars env = build.getEnvironment(listener);
		localImage = env.expand(localImage);

		// Get the IP of the node using hudson.remoting.callable and channel
		Callable<String, IOException> task = new Callable<String, IOException>() {
			@Override
			public String call() throws IOException {
				// This code will run on the build slave
				return InetAddress.getLocalHost().getHostAddress();
			}

			@Override
			public void checkRoles(org.jenkinsci.remoting.RoleChecker checker) throws SecurityException {
				// nothing to do here
			}
		};
		String localIP = Computer.currentComputer().getChannel().call(task);
		System.out.println("IP Address of Build Slave is := " + localIP);

		ArgumentListBuilder args = new ArgumentListBuilder();
		// ./analyze-local-images -endpoint "http://10.242.138.116:6060"
		// -my-address 10.242.138.115 postgres
		args.add(localAnalyzerPath, "-endpoint", apiURL, "-my-address", localIP, localImage);
		File outFile = new File(build.getRootDir(), "out");
		File outFile1 = new File(build.getRootDir(), "out1");
		Launcher.ProcStarter ps = launcher.launch();
		ps.cmds(args);
		ps.stdin(null);
		ps.stderr(listener.getLogger());
		print_stream = new PrintStream(outFile, "UTF-8");
		ps.stdout(print_stream);

		exitCode = ps.join(); // RUN !

		// Copy local file to workspace FilePath object (which might be on
		// remote machine)
		FilePath workspace = build.getWorkspace();
		FilePath target = new FilePath(workspace, artifactName);
		FilePath outFilePath = new FilePath(outFile);

		PrintStream out = new PrintStream(new FileOutputStream(outFile1));
		FilePath outfilFilePath1 = new FilePath(outFile1);
		FilePath filename = Text_HTMLConverter.text_to_html(outFile, outfilFilePath1, out, build_no, target, build,
				jenkins_home);
		filename.copyTo(target);

		return exitCode;

	}

}
