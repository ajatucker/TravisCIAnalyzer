package com.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import com.TravisCIClient.TravisCIFileDownloader;
import com.build.statement.StatementPatchXmlReader;
import com.build.statement.StatementSimilarityMngr;
import com.commit.analysis.CommitFileTypeAnalysisMngr;
import com.config.Config;
import com.csharp.changesize.ChangeSizeAnalyzer;
import com.csharp.diff.CSharpDiffGenMngr;
import com.evaluation.CalculateEvaluation;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import com.travis.parser.BashCmdAnalysis;
import com.travis.parser.CmdClustering;
import com.travis.parser.CommandFrequency;
import com.travis.parser.ProjectCommand;
import com.travis.parser.TrvaisYamlFileParser;
import com.travis.task.TaskAnalyzer;
import com.travis.task.ToolAdoption;
import com.travisdiff.DecorateJSonTree;
import com.travisdiff.TravisCIDiffGenMngr;
import com.travisdiff.TravisCITree;
import com.unity.callgraph.CallGraphBasedDistinctFuncAnalyzer;
import com.unity.callgraph.CallGraphBasedFuncAnalyzer;
import com.unity.callgraph.CallGraphBasedFuncFixCommit;
import com.unity.callgraph.UserDefinedCallAnalysis;
import com.unity.commitanalyzer.CommitAnalysisMngr;
import com.unity.entity.CommandType;
import com.unity.entity.EvaluationProject;
import com.unity.entity.PerfFixData;
import com.unity.repodownloader.ProjectLoader;
import com.utility.TravisReportGenerator;

import edu.util.fileprocess.CSVReaderWriter;
import edu.util.fileprocess.CVSReader;

public class MainClass {

	public static void main(String[] args) {

		System.out.println("Enter your action:");

		System.out.println("1->Download Projects"				
				+ "\n2->Generate and Cluster Travis.yaml file Using AST Analysis"				
				+ "\n3->Project Task Analysis"
				+ "\n4->Evaluation Data Preparation"
				+ "\n5->All project task stat"
				+ "\n6->Generate TravisTree"	
				+ "\n8->Generate Travis Fail Fix Mapping"
				+ "\n7->Download TravisCI Config Files"
				+ "\n9->Fix Pattern Analysis"
				+ "\n10->RQ2->Build Block Analysis"
				+ "\n11->RQ2->Build Pattern Analysis"
				+ "\n12->-----"
				+ "\n13->Generate Report of Commits");

		Scanner cin = new Scanner(System.in);

		System.out.println("Enter an integer: ");
		int inputid = cin.nextInt();

		if (inputid == 1) {
			ProjectLoader projloader = new ProjectLoader();
			projloader.LoadDownloadProjects();
			System.out.println("Download Projects->Completed");

		} 
		else if (inputid == 2) {
			TrvaisYamlFileParser parser = new TrvaisYamlFileParser();
			CmdClustering cmdcluster = new CmdClustering();

			try {				
				List<ProjectCommand> projcmds = parser.getAllProjectCommands();
				List<CommandFrequency> cmdfrqs = cmdcluster.generateCmdFrequency(projcmds);
				CSVReaderWriter writer = new CSVReaderWriter();
				writer.writeBeanToFile(cmdfrqs, Config.csvFreqFile);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}		
		else if (inputid == 3) {
			TaskAnalyzer analyzer = new TaskAnalyzer();
			List<ToolAdoption> tooladoplist = analyzer.getTravisToolAdoption(true);

			System.out.println("\n\n*****Result*****\n\n");
			List<Boolean> buildpredicted = new ArrayList<>();
			List<Boolean> testpredicted = new ArrayList<>();
			List<Boolean> deploypredicted = new ArrayList<>();
			List<Boolean> analysispredicted = new ArrayList<>();

			for (ToolAdoption tooladop : tooladoplist) {
				String projname = tooladop.getProjName();
				boolean build = tooladop.isBuild();
				boolean test = tooladop.isTest();
				boolean anal = tooladop.isAnalysis();
				boolean depl = tooladop.isDeployment();

				buildpredicted.add(build);
				testpredicted.add(test);
				deploypredicted.add(depl);
				analysispredicted.add(anal);

				System.out.println(projname + "====>Build->" + build + "====>Test->" + test + "====>Analyzer->" + anal
						+ "====>Deployment->" + depl);
			}

			List<Boolean> buildtruth = new ArrayList<>();
			List<Boolean> testtruth = new ArrayList<>();
			List<Boolean> deploytruth = new ArrayList<>();
			List<Boolean> analysistruth = new ArrayList<>();

			CVSReader csvreader = new CVSReader();
			List<EvaluationProject> evalprojs = null;
			try {
				String file=Config.rootDir+"ground_truth_analysis.csv";
				evalprojs = csvreader.loadEvaluationProjects(file);
				System.out.println("Loading Evaluation File");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (EvaluationProject evalproj : evalprojs) {
				buildtruth.add(evalproj.isIsbuild());
				testtruth.add(evalproj.isTest());
				deploytruth.add(evalproj.isDeploy());
				analysistruth.add(evalproj.isCodeAnalysis());
			}

			CalculateEvaluation eval = new CalculateEvaluation();

			double buildf1 = eval.getF1Score(buildtruth, buildpredicted,"Build");
			double testf1 = eval.getF1Score(testtruth, testpredicted,"Test");
			double analysisf1 = eval.getF1Score(analysistruth, analysispredicted,"Code Analysis");
			double deplf1 = eval.getF1Score(deploytruth, deploypredicted,"Deployment");

			System.out.println("Build F1-Score:" + buildf1 + " Test F1:" + testf1 + " Analysis F1:" + analysisf1
					+ " Deployment F1:" + deplf1);

			int index = 0;
			for (EvaluationProject evalproj : evalprojs) {
				if (deploytruth.get(index) != deploypredicted.get(index)) {
					System.out.println(evalproj.getProjName() + "--->" + deploypredicted.get(index));
				}
				index++;
			}
		}

		else if (inputid == 4) {
			CVSReader csvreader = new CVSReader();

			List<EvaluationProject> evalprojs = null;
			try {
				String file=Config.rootDir+"ground_truth_analysis.csv";
				evalprojs = csvreader.loadEvaluationProjects(file);
				System.out.println("Loading Evaluation File");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (EvaluationProject evalproj : evalprojs) {
				String projname = evalproj.getProjName();
				String filepath = Config.rootDir+"travisPyFiles\\"
						+ projname.replace('/', '_') + "_.travis.yml";
				String copydir = Config.rootDir+"EvalRepos\\"
						+ projname.replace('/', '@');
				String copyfile = Config.rootDir+"EvalRepos\\"
						+ projname.replace('/', '@') + "\\" + ".travis.yml";

				File theDir = new File(copydir);
				if (!theDir.exists()) {
					theDir.mkdirs();
				}

				try {
					copyFile(filepath, copyfile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String gitname = "https://github.com/" + projname + ".git";
				System.out.println(gitname);

			}

		}

		else if (inputid == 5) {
			TaskAnalyzer analyzer = new TaskAnalyzer();
			List<ToolAdoption> tooladoplist = analyzer.getTravisToolAdoption(false);

			System.out.println("\n\n*****Result*****\n\n");
			List<Boolean> buildpredicted = new ArrayList<>();
			List<Boolean> testpredicted = new ArrayList<>();
			List<Boolean> deploypredicted = new ArrayList<>();
			List<Boolean> analysispredicted = new ArrayList<>();
			int totalproj = 0;
			int buildcount = 0;
			int testcount = 0;
			int depcount = 0;
			int anacount = 0;
			int cicount = 0;

			for (ToolAdoption tooladop : tooladoplist) {
				String projname = tooladop.getProjName();
				boolean build = tooladop.isBuild();
				boolean test = tooladop.isTest();
				boolean anal = tooladop.isAnalysis();
				boolean depl = tooladop.isDeployment();

				buildpredicted.add(build);
				testpredicted.add(test);
				deploypredicted.add(depl);
				analysispredicted.add(anal);

				if (build)
					buildcount++;
				else
					System.out.println(projname);

				if (test)
					testcount++;

				if (anal)
					anacount++;

				if (depl)
					depcount++;

				if (tooladop.isCi()) {
					cicount++;

				} else {
					System.out.println(projname);
				}

				totalproj++;
				// System.out.println(projname+"====>Build->"+build+"====>Test->"+test+"====>Analyzer->"+anal+"====>Deployment->"+depl);
			}

			System.out.println("Total:" + totalproj + " CI Count:" + cicount + " Build:" + buildcount + " Test:"
					+ testcount + " Analysis:" + anacount + " Deployment:" + depcount);

		} else if (inputid == 6) {
			TravisCITree travistree = new TravisCITree();
			ITree prevtree = travistree
					.getTravisCITree(Config.rootDir+".travis_robot-surgery1.yml");
			ITree curtree = travistree
					.getTravisCITree(Config.rootDir+".travis_robot-surgery2.yml");
			
			//keep a map, count of lifecycle of travis then generate a report of all data

//			 Matcher m = Matchers.getInstance().getMatcher();
//			 MappingStore mappings = m.match(prevtree, curtree);
//
//			 EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
//			 EditScript actions = editScriptGenerator.computeActions(mappings);
//			 
//			 System.out.println("test");

			Matcher defaultMatcher = Matchers.getInstance().getMatcher(); // retrieves the default matcher
			MappingStore mappings = defaultMatcher.match(prevtree, curtree); // computes the mappings between the trees
			EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator(); // instantiates the
																								// simplified Chawathe
																								// script generator
			EditScript actions = editScriptGenerator.computeActions(mappings); // computes the edit script

			System.out.println("Pre-print test");

			DecorateJSonTree decojson = new DecorateJSonTree();

			for (Action action : actions) {
				String strfield = decojson.getJsonField(action);
				strfield = strfield.replaceAll("\"", "");
				System.out.println(strfield);
				action.getNode().setMetadata("json_parent", strfield);
			}

			System.out.println("Post-print new test");

		}  else if (inputid == 7) {
			TravisCIFileDownloader dwnloader = new TravisCIFileDownloader();
			dwnloader.downloadTraviCIConfigFiles();
		} else if (inputid == 8) {
			System.out.println("TravisCI Fix Analysis");
			TravisCIDiffGenMngr diffmngr = new TravisCIDiffGenMngr();
			diffmngr.generateTravisCIFailFixChangeData();
		} else if (inputid == 9) {
			System.out.println("TravisCI Fix Analysis Statistics");
			TravisCIDiffGenMngr diffmngr = new TravisCIDiffGenMngr();
			diffmngr.generateTravisCIFailFixChangeDataStat();
		} else if (inputid == 10) {
			TrvaisYamlFileParser fileparser = new TrvaisYamlFileParser();

			LinkedHashMap<String, Integer> blocksstat = fileparser.getProjectBlockStatus();

			for (String block : blocksstat.keySet()) {
				System.out.println(block + "======>" + blocksstat.get(block));
			}

		}
		else if(inputid==11)
		{
			TaskAnalyzer analyzer = new TaskAnalyzer();
			List<ToolAdoption> tooladoplist = analyzer.getTravisToolAdoptionWithType();
		} else if (inputid == 12) {
			TrvaisYamlFileParser parser = new TrvaisYamlFileParser();
			CmdClustering cmdcluster = new CmdClustering();

			try {				
				List<ProjectCommand> projcmdsml = parser.getAllProjectCommands(Config.gitProjList);
				List<ProjectCommand> projcmdsnonml = parser.getAllProjectCommands(Config.gitProjListNonML);
				
				
				List<CommandFrequency> cmdfrqsml = cmdcluster.generateCmdFrequency(projcmdsml);
				List<CommandFrequency> cmdfrqsnonml = cmdcluster.generateCmdFrequency(projcmdsnonml);
				List<CommandFrequency> cmdunique=new ArrayList<>();
				
				for(CommandFrequency cmdnonml:cmdfrqsnonml)
				{
					boolean flag=false;
					for(CommandFrequency cmdml:cmdfrqsml)
					{
						if(cmdml.getCmdName().toString().equals(cmdnonml.getCmdName().toString()))
						{
							flag=true;
							break;
						}
					}
					
					if(flag==false)
					{
						cmdunique.add(cmdnonml);
					}
				}
				
				
				CSVReaderWriter writer = new CSVReaderWriter();
				writer.writeBeanToFile(cmdunique, Config.csvFreqFile);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (inputid == 13) {
			//dump travis files to temp folder pre/post commit
			
			TravisCIFileDownloader dwnloader = new TravisCIFileDownloader();
			dwnloader.downloadBeforeAndAfterCommitFiles();
	   
			TravisReportGenerator generate = new TravisReportGenerator();
			generate.compareFiles("b3ae91c7e89644a73b9e9e44391d034bf746342f.yml", "prevb3ae91c7e89644a73b9e9e44391d034bf746342f.yml");
			
		}

	}

	public static void copyFile(String from, String to) throws IOException {
		Path sourceFile = Paths.get(from);
		Path targetFile = Paths.get(to);

		try {

			Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

		} catch (IOException ex) {
			System.err.format("I/O Error when copying file");
		}
	}
	
	
	
}