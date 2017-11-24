package org.jenkinsci.plugins.clairdockerscannerbuildstep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

public class Values_File{
	public static File valuesFile(int high,int med,int low,int neg,String build_no,AbstractBuild build, String jenkins_home, BuildListener listener) throws IOException, InterruptedException
		 {
		int b_no=Integer.parseInt(build_no);
		EnvVars env = build.getEnvironment(listener);
		String JobName=env.get("JOB_NAME");
		System.out.println("Jenkins home variable"+jenkins_home);
		//System.out.println("values "+high+" "+med+" "+low+" "+neg+" "+build_no);
		int Total_count=high+med+low+neg;
		String Filename=JobName+"_Severity_Count.properties";
		File writer = new File(build.getRootDir(),"print_stream");
			 FilePath workspace = build.getWorkspace();
			FilePath target = new FilePath(workspace,Filename );
			FilePath outFile=new FilePath(writer);
		
 String FILENAME =jenkins_home+"/"+Filename;
 
 File file = new File(FILENAME);
		System.out.println("Filename of properties file"+FILENAME );
			BufferedWriter bw = null;
			FileWriter fw = null;

			try {

				//String data = ("\n"+"Build no :"+b_no+"\t"+"Total count:"+Total_count+"\t"+"values: "+high+"\t"+med+"\t"+low+"\t"+neg);
				String data=(b_no+" = "+high+", "+med+", "+low+", "+neg+","+Total_count+"\n"); 
				

				// if file doesnt exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}

				// true = append file
				fw = new FileWriter(file.getAbsoluteFile(), true);
				bw = new BufferedWriter(fw);

				bw.write(data);

				System.out.println("Done");

			} catch (IOException e) {

				e.printStackTrace();

			} finally {

				try {

					if (bw != null)
						bw.close();

					if (fw != null)
						fw.close();

				} catch (IOException ex) {

					ex.printStackTrace();

				}
			}

			FilePath Filepath1=new FilePath(file);
			Filepath1.copyTo(target);
		System.out.println("value of target "+target);
		System.out.println("value of workspoce "+workspace);
	
			return file;
		
		 
		 }

	public static File layervalues(List<String> finalcommand1, List<String> size_layer_file, String build_no,
			AbstractBuild build, String jenkins_home, BuildListener listener) throws IOException, InterruptedException {
		
		// TODO Auto-generated method stub
		
		int b_no=Integer.parseInt(build_no);
		System.out.println("Jenkins home variable"+jenkins_home);
		EnvVars env = build.getEnvironment(listener);
		String JobName=env.get("JOB_NAME");
		String Filename=JobName+"_Layer_Data.Properties";
		File writer = new File(build.getRootDir(),"print_stream");
			 FilePath workspace = build.getWorkspace();
			FilePath target = new FilePath(workspace,Filename );
			FilePath outFile=new FilePath(writer);
		
 String FILENAME =jenkins_home+"/"+Filename;
 
 File file1 = new File(FILENAME);
		System.out.println("Filename of properties file"+FILENAME );
			BufferedWriter bw = null;
			FileWriter fw = null;
StringBuilder sb1=new StringBuilder();
fw = new FileWriter(file1.getAbsoluteFile(), true);
	bw = new BufferedWriter(fw);
			try {
                int i=0;
           //    final int buildno=b_no;
               if (!file1.exists()) {
					file1.createNewFile();
				}
               
				String data=(b_no+ " = " +finalcommand1.toString()+"}, " +size_layer_file.toString()+"\n"); 
				// if file doesnt exists, then create it
				bw.write(data);
               
				
						

				System.out.println("Done");

			} catch (IOException e) {

				e.printStackTrace();

			} finally {

				try {

					if (bw != null)
						bw.close();

					if (fw != null)
						fw.close();

				} catch (IOException ex) {

					ex.printStackTrace();

				}
			}

			FilePath Filepath1=new FilePath(file1);
			Filepath1.copyTo(target);
		System.out.println("value of target for layer data "+target);
		System.out.println("value of workspoce for layer data"+workspace);
	
			
		
		return file1 ;
	
	}
}