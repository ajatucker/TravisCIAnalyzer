package com.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

public class Config {
	// public static String rootDir="/media/AutoBuilder/UnityPerformance/";
	 //public static String
	 //rootDir=".\\Project_Data";
	 public static String
	 rootDir="C:/Users/ogime/Desktop/ML-DevOps-Research/Project_Repo/";
	 //
	 //text file that contains list of projects to analyze
	 public static String gitProjList=rootDir+"Project_Source_NonML.txt";
	 public static String gitProjListNonML=rootDir+"Project_Source_NonML.txt";
	 public static String gitProjListEval=rootDir+"eval_Project_Source.txt";
	
	 

	 //reporDir used for storing Unity Projects
	 public static String repoDirEval= rootDir+"EvalRepos/"; /*rootDir+"GitRepo/";*/	 
	 public static String repoDir = rootDir+"GitRepo/";
	 
	 public static String travisRepoDir = "D:/ClonedRepos/";
	
	 public static String csvFile=rootDir+"perf_commit_data_Updated.csv";
	 
	 public static String csvFreqFile=rootDir+"new_new_cmd_frequency_non_ml_unique.csv";
	 
	 public static String csvCmdTypeFile=rootDir+"command_type_new_ML_Non_Ml.csv";
	 
	 //public static String csvCITransitionFile=rootDir+"Tool_transition.csv";
	 public static String csvCITransitionFile="C:/Users/ogime/Desktop/ML-DevOps-Research/Project_Repo/ML.csv";
	 
	 public static String csvBlockCategory=Config.rootDir+"block_type.csv";
			 
	 public static String patchDir=rootDir+"PatchDir";

	public static String[] perfCommitToken = { ".." };

	public static int commitid = 1;
	public static int stmtUniqueId = 1;
	
	public static String gitHubUserName="ajatucker";
	public static String gitHubPwd="gitG00D$rub5";
	
	public static String[] compoundcmds= {"python","bash", "source", "python3", "sh", "eval", "xvfb-run", "/bin/bash",
			"doit","ruby","unbuffer","catchsegv"};
	
	public static String repoStrDate="08-10-2021";

}