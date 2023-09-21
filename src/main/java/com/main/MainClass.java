package com.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

import org.assimbly.docconverter.DocConverter;

import com.TravisCIClient.TravisCIFileDownloader;
import com.build.commitanalyzer.CommitAnalyzer;
import com.build.commitanalyzer.MLCommitDiffInfo;
import com.config.Config;
import com.evaluation.CalculateEvaluation;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.python.parser.PythonFileParser;
import com.travis.parser.CmdClustering;
import com.travis.parser.CommandFrequency;
import com.travis.parser.ProjectCommand;
import com.travis.parser.TravisYamlFileParser;
import com.travis.task.TaskAnalyzer;
import com.travis.task.ToolAdoption;
import com.travisdiff.DecorateJSonTree;
import com.travisdiff.TravisCIDiffGenMngr;
import com.travisdiff.TravisCITree;
import com.unity.entity.EvaluationProject;
import com.unity.repodownloader.ProjectLoader;

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
				+ "\n13->Generate Report of Commits"
				+ "\n14->Compute Python+Travis ASTs and Travis line-of-code changes");

		Scanner cin = new Scanner(System.in);

		System.out.println("Enter an integer: ");
		int inputid = cin.nextInt();
		if (inputid == 1) {
			ProjectLoader projloader = new ProjectLoader();
			projloader.LoadDownloadProjects();
			System.out.println("Download Projects->Completed");

		} 
		else if (inputid == 2) {
			TravisYamlFileParser parser = new TravisYamlFileParser();
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
			TravisYamlFileParser fileparser = new TravisYamlFileParser();

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
			TravisYamlFileParser parser = new TravisYamlFileParser();
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
				//FileOutputStream of = new FileOutputStream(Config.rootDir+".old_travis.yml");
			/*
	          Repository repository = git.getRepository();
	          RevWalk walk = new RevWalk(repository);
	          ObjectId commitId = ObjectId.fromString("b3ae91c7e89644a73b9e9e44391d034bf746342f");
	          RevCommit commit = walk.parseCommit(commitId);
	          //System.out.println(commit);
	          TreeWalk treeWalk = new TreeWalk(repository); //TreeWalk.forPath(git.getRepository(), path, commit.getTree()));//
	          //treeWalk.addTree(commit.getTree());
	          treeWalk.setFilter(PathFilter.create(".travis.yml"));
	          ObjectId blobId = commit.getTree().getId();
	          ObjectReader objectReader = repository.newObjectReader();
	          ObjectLoader objectLoader = objectReader.open(blobId);
	          byte[] bytes = objectLoader.getBytes();
	          String content = new String(bytes, "utf-8");
	          System.out.println(content);
	          */
	          //objectLoader.copyTo(of);
	          
	          //of.close();
	          //byte[] bytes = objectLoader.getBytes();
	          //String contents = new String(bytes, StandardCharsets.UTF_8);
	          //System.out.println(contents);
	          //System.out.println(commit);
	          /*RevTree tree = commit.getTree();
	          
	           * TreeWalk treewalk = new TreeWalk(repository);
	          treewalk.addTree(tree);
	          treewalk.setRecursive(true);
	          treewalk.setFilter(PathFilter.create(".travis.yml"));
	          ObjectId objectId = treewalk.getObjectId(0);
	          ObjectLoader loader = repository.open(objectId);
	          String content = new String(loader.getBytes().toString());
	          System.out.println("This is working so far");
	          System.out.println(content);
	          */
	          //System.out.println("No issues");
	   
			
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
		}
		else if(inputid == 14) {
			int pythonFileLimit = Config.maxPythonFiles; //Any commit changing more python files than this will be immediately be skipped
			//To not remove large commits, simply set to Integer.MAX_VALUE
			File outputFolder = new File(Config.rootDir + "Output");

			String csvPath = Config.rootDir + "ML-SampledCommitsFrom-PythonProjects.csv";
			String outputPath = outputFolder.getAbsolutePath() + File.separator + "ML-SampledCommitsFrom-PythonProjects_Output.csv";
			
			//PythonParser must be installed for this
			if(!System.getenv("PATH").contains("pythonparser")) {
				if(System.getProperty("gt.pp.path") == null) {
					System.err.println("Python parser not included in -Dgp.pp.path= argument or PATH system environment variable! Cannot analyze python ML projects.\n"
							+ "Install Python if needed, and GumTreeDiff\\pythonparser, from GitHub and pass here");
					return;
				}
				String s = System.getProperty("gt.pp.path");
				File f = new File(s.startsWith("\"") ? s.substring(1, s.length()-1) : s); //trim quotes or this is workingDir\"pythonParserAbsolutePath"
				if(!f.exists()) {
					System.err.println("PythonParser not on PATH, and not installed at the specified location!");
					System.err.println(f.getAbsolutePath());
					return;
				}
			}
			
			if(!outputFolder.exists())
				outputFolder.mkdirs();
			
			//Clean up the temp files from possible interrupted runs
			//try {
			//	Files.deleteIfExists(Path.of(Config.rootDir, "new_python.py"));
			//	Files.deleteIfExists(Path.of(Config.rootDir, "old_python.py"));
			//	Files.deleteIfExists(Path.of(Config.rootDir, "new_travis.yml"));
			//	Files.deleteIfExists(Path.of(Config.rootDir, "new_travis.yml"));
			//} catch (IOException e) {
			//	// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
			
		
			
			CSVReaderWriter readWrite = new CSVReaderWriter();
			try {
				List<MLCommitDiffInfo> diffInfos = readWrite.getMLCommitDiffInfoFromCSV(csvPath);
				PythonFileParser pyParser = new PythonFileParser();
				//Clear log for python AST
				String pyLogPath = Config.rootDir + "python_errors.log";
				File pyLogFile = new File(pyLogPath);
				if(pyLogFile.exists())
					pyLogFile.delete();
				pyLogFile.createNewFile();
				//Iterate across CSV lines
				for(MLCommitDiffInfo diffInfo : diffInfos) {
					String commitOutputFolder = outputFolder.getAbsolutePath() + File.separator + diffInfo.getProjName().replace("/", File.separator) + File.separator + diffInfo.getCommitID();
					new File(commitOutputFolder).mkdirs();
					boolean travisModified = false;
					loop: for(String name : diffInfo.getModifiedFiles()) { //check for travis change
						if(name.contains(".travis.yml")) {
							travisModified = true;
							break loop;
						}
					}
					int pythonFilesModified = 0;
					for(String fileName : diffInfo.getModifiedFiles()) {
						if(fileName.endsWith(".py"))
							pythonFilesModified++;
						if(pythonFilesModified > pythonFileLimit)
							break;
					}
					if(pythonFilesModified >= pythonFileLimit)
						System.out.println(diffInfo.getProjName() + ":" + diffInfo.getCommitID() + "; Too many python files: " + pythonFilesModified);
					if(pythonFilesModified == 0)
						System.out.println(diffInfo.getProjName() + ":" + diffInfo.getCommitID() + "; No python files modified");
					if(!travisModified)
						System.out.println(diffInfo.getProjName() + ":" + diffInfo.getCommitID() + "; travis.yml not modified");
					if(travisModified && pythonFilesModified > 0 && pythonFilesModified < pythonFileLimit) {
						String projName = diffInfo.getProjName();
						String projUrl = "https://github.com/" + projName + ".git";
						String projFolderName = projName.substring(projName.indexOf("/") + 1);
						String projAuthorName = projName.substring(0, projName.indexOf("/"));
						CommitAnalyzer analyzer = new CommitAnalyzer(projAuthorName, projFolderName, projUrl);
						File file = CommitAnalyzer.getCloneLocation(projFolderName);
						if(!file.exists()) {
							System.out.println("Starting clone to " + file.getAbsolutePath());
							analyzer.cloneRepository(projFolderName);
							System.out.println("Clone complete");
						}else{
							System.out.println(file.getAbsolutePath() + " already exists!");
							System.out.println("Already cloned, skipping cloning");
						}
						//analyzer.checkOutAtSpecificCommitID(diffInfo.getCommitID());
						//System.out.println("Checkout complete");
						int[] result = analyzer.getLoCChange(diffInfo.getCommitID());
						if(result != null) {
							diffInfo.setLinesAdded(result[0]);
							diffInfo.setLinesRemoved(result[1]);
							diffInfo.setLinesModified(result[2]);
							System.out.println(projName + " " + Arrays.toString(result));
						}
						if(result != null && result[2] != 0) {
							TravisYamlFileParser.EditResults results = analyzer.getYamlFileChangeAST(diffInfo.getCommitID(), ".travis.yml");
							if(results != null) {
								String astStr = TravisYamlFileParser.getYamlDiffStr(results);
								Gson gson = new GsonBuilder().setPrettyPrinting().create();
								String miniJson = gson.toJson(gson.fromJson(astStr, JsonElement.class)); //minified json string
								String travisOutputPath = commitOutputFolder + File.separator + "travis_diff.json";
								diffInfo.setTravisAstDiffStr(travisOutputPath);
								DocConverter.convertStringToFile(travisOutputPath, miniJson);
							}
						}
						//Python AST
						PythonFileParser.EditResults[] pyResults = analyzer.getPythonDiffAST(diffInfo.getCommitID());
						if(pyResults != null) {
							diffInfo.setPythonFilesChanged(pyResults.length);
							//Put all the file results into one object
							GsonBuilder gsonB = new GsonBuilder();
							Gson gson = gsonB.setPrettyPrinting().create();
							JsonObject jsonObj = new JsonObject(); //use to take in the json for each tree, put them together, and write to a string
							for(int i = 0; i < pyResults.length; i++) {
								String jsonStr = PythonFileParser.getPythonDiffString(pyResults[i]);
								jsonObj.add(pyResults[i].fileName, gson.fromJson(jsonStr, JsonElement.class));
							}
							//System.out.println("Json:");
							//System.out.println(jsonObj.toString());
							String miniJson = gson.toJson(jsonObj);
							String pythonOutputPath = commitOutputFolder + File.separator + "python_diff.json";
							diffInfo.setPythonAstDiffStr(pythonOutputPath);
							DocConverter.convertStringToFile(pythonOutputPath, miniJson);
							
						}else {
							System.out.println("Results were null");
						}
					}
				}
				File outputFile = new File(outputPath);
				if(!outputFile.exists())
					outputFile.createNewFile();
				readWrite.writeMLDiffBeanToFile(diffInfos, outputPath);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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