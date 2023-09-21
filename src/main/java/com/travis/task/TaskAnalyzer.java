package com.travis.task;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.travis.parser.BashCmdAnalysis;
import com.travis.parser.CmdClustering;
import com.travis.parser.CommandFrequency;
import com.travis.parser.ProjectCommand;
import com.travis.parser.TravisYamlFileParser;
import com.unity.entity.CommandType;
import com.unity.entity.FrequencyEntity;
import com.unity.entity.PerfFixData;
import com.utility.ProjectPropertyAnalyzer;

import edu.util.fileprocess.CSVReaderWriter;
import edu.util.fileprocess.CVSReader;
import edu.util.fileprocess.TextFileReaderWriter;

public class TaskAnalyzer {

	public List<ToolAdoption> getTravisToolAdoption(boolean isevalproj) {
		TravisYamlFileParser parser = new TravisYamlFileParser();
		CVSReader csvrw = new CVSReader();
		List<ToolAdoption> tooladoplist = null;
		try {
			// String
			// json=parser.getJsonDataFromYamlFile("G:\\Research\\ML_CI\\Project_Repo\\GitRepo\\cjekel@tindetheus\\.travis.yml");
			List<ProjectCommands> projcmds = parser.getAllGitProjectCommands(isevalproj);
			List<DeploymentTask> projdeps = parser.getProjectDeploymentStatus();
			List<DeploymentTask> language = parser.getProjectLanguageStatus();
			Map<String, CommandType> cmdlist = csvrw.loadCmdType(Config.csvCmdTypeFile);

			tooladoplist = analyzeAdoption(projcmds, projdeps,language, cmdlist);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return tooladoplist;
	}

	private List<ToolAdoption> analyzeAdoption(List<ProjectCommands> projcmds, List<DeploymentTask> projdeps,
			List<DeploymentTask> languages, Map<String, CommandType> cmdlist) {
		String filepath = Config.gitProjList;

		List<String> projlist = TextFileReaderWriter.GetFileContentByLine(filepath);

		int index = 0;
		List<ToolAdoption> tooladoptionlist = new ArrayList<>();
		for (String proj : projlist) {

			String projname = ProjectPropertyAnalyzer.getProjName(proj);

			String projdir = Config.repoDir + projname;
			String travisfile = projdir + "\\" + ".travis.yml";

			ToolAdoption tooladop = new ToolAdoption(projname);

			File ftrvais = new File(travisfile);
			if (ftrvais.exists()) {
				tooladop.setCi(true);
			} else {
				tooladop.setCi(false);
			}

			int buildcount = getBuildCmdCount(projcmds.get(index), cmdlist);
			int testcount = getTestCmdCount(projcmds.get(index), cmdlist);
			int analysiscount = getAnalyzerCmdCount(projcmds.get(index), cmdlist);
			int depcount = getDeploymentCmdCount(projcmds.get(index), cmdlist);
			boolean depstatus = projdeps.get(index).getDeploymentStatus();
			boolean langstatus = languages.get(index).getDeploymentStatus();

			tooladop.setBuildCount(buildcount);
			tooladop.setTestCount(testcount);
			tooladop.setAnalysisCount(analysiscount);

			if (depstatus)
				tooladop.setDeploymentCount(depcount + 1);
			else
				tooladop.setDeploymentCount(depcount);
			
			if(langstatus)
				tooladop.setBuild(true);
			if (buildcount > 0)
				tooladop.setBuild(true);
			if (testcount > 0)
				tooladop.setTest(true);
			if (analysiscount > 0)
				tooladop.setAnalysis(true);

			tooladoptionlist.add(tooladop);
			index++;
		}

		return tooladoptionlist;
	}

	private int getBuildCmdCount(ProjectCommands projcmds, Map<String, CommandType> cmdlist) {
		List<ProjectCommand> projcmdlist = projcmds.getProjCmds();
		int count = 0;
		for (ProjectCommand projcmd : projcmdlist) {

			if (projcmd.getBaseCmd() != null && cmdlist.containsKey(projcmd.getBaseCmd())) {
				CommandType cmdtype = cmdlist.get(projcmd.getBaseCmd());

				if (cmdtype.getCategoryName().toLowerCase().equals("build")) {
					count++;
				}
			} else if (projcmd.getBaseCmd() != null) {
				String basecmd = projcmd.getBaseCmd();
				System.out.print(basecmd);
				basecmd = basecmd.toLowerCase();

				if (basecmd.endsWith(".sh") || basecmd.endsWith(".py") || basecmd.startsWith("./")) {
					if (basecmd.contains("setup") || basecmd.contains("build") || basecmd.contains("import")) {
						count++;
					}
				}

			}

		}

		return count;
	}

	private Map<String, Integer> getBuildCmdCountStat(ProjectCommands projcmds, Map<String, CommandType> cmdlist) {
		List<ProjectCommand> projcmdlist = projcmds.getProjCmds();
		Map<String, Integer> cmdfreqmap = new HashMap<>();
		int count = 0;
		for (ProjectCommand projcmd : projcmdlist) {

			if (projcmd.getBaseCmd() != null && cmdlist.containsKey(projcmd.getBaseCmd())) {
				CommandType cmdtype = cmdlist.get(projcmd.getBaseCmd());

				if (cmdtype.getCategoryName().toLowerCase().equals("build") || cmdtype.getCategoryName().toLowerCase().equals("general")) {
					count++;

					if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
						int val = cmdfreqmap.get(projcmd.getBaseCmd());
						cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
					} else {
						cmdfreqmap.put(projcmd.getBaseCmd(), 1);
					}
				}
			} else if (projcmd.getBaseCmd() != null) {
				String basecmd = projcmd.getBaseCmd();
				// System.out.print(basecmd);
				basecmd = basecmd.toLowerCase();

				if (basecmd.endsWith(".sh") || basecmd.endsWith(".py") || basecmd.startsWith("./")) {
					if (basecmd.contains("setup") || basecmd.contains("build") || basecmd.contains("import")) {
						count++;

						if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
							int val = cmdfreqmap.get(projcmd.getBaseCmd());
							cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
						} else {
							cmdfreqmap.put(projcmd.getBaseCmd(), 1);
						}
					}
				}

			}

		}

		return cmdfreqmap;
	}

	private int getTestCmdCount(ProjectCommands projcmds, Map<String, CommandType> cmdlist) {
		List<ProjectCommand> projcmdlist = projcmds.getProjCmds();
		int count = 0;
		for (ProjectCommand projcmd : projcmdlist) {
			if (projcmd.getBaseCmd() != null && cmdlist.containsKey(projcmd.getBaseCmd())) {
				CommandType cmdtype = cmdlist.get(projcmd.getBaseCmd());

				if (cmdtype.getCategoryName().toLowerCase().equals("test")) {
					count++;
				} else if (projcmd.getBaseCmd().toLowerCase().contains("coverage")
						&& projcmd.getCmdName().toLowerCase().contains("unittest")) {
					count++;
				} else if (projcmd.getBaseCmd().toLowerCase().contains("coverage")
						&& projcmd.getCmdName().toLowerCase().contains("run")
						&& projcmd.getCmdName().toLowerCase().contains("test")) {
					count++;
				}

			} else if (projcmd.getBaseCmd() != null) {
				String basecmd = projcmd.getBaseCmd();
				basecmd = basecmd.toLowerCase();

				if (basecmd.endsWith(".sh") || basecmd.endsWith(".py") || basecmd.startsWith("./")) {
					if (basecmd.contains("test") || basecmd.contains("train") || basecmd.contains("eval")
							|| basecmd.contains("model")) {
						count++;
					} else if (basecmd.toLowerCase().contains("setup")
							&& projcmd.getCmdName().toLowerCase().contains(" test")) {
						count++;
					}

				} else if (projcmd.getCmdName().toLowerCase().contains(" test")) {
					count++;
				}

			}

		}

		return count;
	}

	private Map<String, Integer> getTestCmdCountStat(ProjectCommands projcmds, Map<String, CommandType> cmdlist) {
		List<ProjectCommand> projcmdlist = projcmds.getProjCmds();
		Map<String, Integer> cmdfreqmap = new HashMap<>();
		int count = 0;
		for (ProjectCommand projcmd : projcmdlist) {
			if (projcmd.getBaseCmd() != null && cmdlist.containsKey(projcmd.getBaseCmd())) {
				CommandType cmdtype = cmdlist.get(projcmd.getBaseCmd());

				if (cmdtype.getCategoryName().toLowerCase().equals("test")) {
					count++;
					if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
						int val = cmdfreqmap.get(projcmd.getBaseCmd());
						cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
					} else {
						cmdfreqmap.put(projcmd.getBaseCmd(), 1);
					}
				} else if (projcmd.getBaseCmd().toLowerCase().contains("coverage")
						&& projcmd.getCmdName().toLowerCase().contains("unittest")) {
					count++;
					if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
						int val = cmdfreqmap.get(projcmd.getBaseCmd());
						cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
					} else {
						cmdfreqmap.put(projcmd.getBaseCmd(), 1);
					}
				} else if (projcmd.getBaseCmd().toLowerCase().contains("coverage")
						&& projcmd.getCmdName().toLowerCase().contains("run")
						&& projcmd.getCmdName().toLowerCase().contains("test")) {
					count++;
					if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
						int val = cmdfreqmap.get(projcmd.getBaseCmd());
						cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
					} else {
						cmdfreqmap.put(projcmd.getBaseCmd(), 1);
					}
				}

			} else if (projcmd.getBaseCmd() != null) {
				String basecmd = projcmd.getBaseCmd();
				basecmd = basecmd.toLowerCase();

				if (basecmd.endsWith(".sh") || basecmd.endsWith(".py") || basecmd.startsWith("./")) {
					if (basecmd.contains("test") || basecmd.contains("train") || basecmd.contains("eval")
							|| basecmd.contains("model")) {
						count++;
						if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
							int val = cmdfreqmap.get(projcmd.getBaseCmd());
							cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
						} else {
							cmdfreqmap.put(projcmd.getBaseCmd(), 1);
						}
					} else if (basecmd.toLowerCase().contains("setup")
							&& projcmd.getCmdName().toLowerCase().contains(" test")) {
						count++;
						if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
							int val = cmdfreqmap.get(projcmd.getBaseCmd());
							cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
						} else {
							cmdfreqmap.put(projcmd.getBaseCmd(), 1);
						}
					}

				} else if (projcmd.getCmdName().toLowerCase().contains(" test")) {
					count++;
					if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
						int val = cmdfreqmap.get(projcmd.getBaseCmd());
						cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
					} else {
						cmdfreqmap.put(projcmd.getBaseCmd(), 1);
					}
				}

			}

		}

		return cmdfreqmap;
	}

	private int getAnalyzerCmdCount(ProjectCommands projcmds, Map<String, CommandType> cmdlist) {
		List<ProjectCommand> projcmdlist = projcmds.getProjCmds();
		int count = 0;
		for (ProjectCommand projcmd : projcmdlist) {
			if(projcmd.getBaseCmd() != null && projcmd.getBaseCmd().toString().equals("luacheck"))
				System.out.print("test");
				
				
			if (projcmd.getBaseCmd() != null && cmdlist.containsKey(projcmd.getBaseCmd())) {
				CommandType cmdtype = cmdlist.get(projcmd.getBaseCmd());

				if (cmdtype.getCategoryName().toLowerCase().equals("analyzer")) {
					count++;
				}
			} else if (projcmd.getBaseCmd() != null) {
				String basecmd = projcmd.getBaseCmd();
				basecmd = basecmd.toLowerCase();

				if (basecmd.endsWith(".sh") || basecmd.endsWith(".py") || basecmd.startsWith("./")) {
					if (basecmd.contains("cove")) {
						count++;
					}
				}

			}

		}

		return count;
	}

	private Map<String, Integer> getAnalyzerCmdCountStat(ProjectCommands projcmds, Map<String, CommandType> cmdlist) {
		List<ProjectCommand> projcmdlist = projcmds.getProjCmds();
		Map<String, Integer> cmdfreqmap = new HashMap<>();
		int count = 0;
		for (ProjectCommand projcmd : projcmdlist) {
			if (projcmd.getBaseCmd() != null && cmdlist.containsKey(projcmd.getBaseCmd())) {
				CommandType cmdtype = cmdlist.get(projcmd.getBaseCmd());

				if (cmdtype.getCategoryName().toLowerCase().equals("analyzer")) {
					count++;
					if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
						int val = cmdfreqmap.get(projcmd.getBaseCmd());
						cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
					} else {
						cmdfreqmap.put(projcmd.getBaseCmd(), 1);
					}
				}
			} else if (projcmd.getBaseCmd() != null) {
				String basecmd = projcmd.getBaseCmd();
				basecmd = basecmd.toLowerCase();

				if (basecmd.endsWith(".sh") || basecmd.endsWith(".py") || basecmd.startsWith("./")) {
					if (basecmd.contains("cove")) {
						count++;
						if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
							int val = cmdfreqmap.get(projcmd.getBaseCmd());
							cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
						} else {
							cmdfreqmap.put(projcmd.getBaseCmd(), 1);
						}
					}
				}

			}

		}

		return cmdfreqmap;
	}

	private int getDeploymentCmdCount(ProjectCommands projcmds, Map<String, CommandType> cmdlist) {
		List<ProjectCommand> projcmdlist = projcmds.getProjCmds();
		int count = 0;
		for (ProjectCommand projcmd : projcmdlist) {
			if (projcmd.getBaseCmd() != null && cmdlist.containsKey(projcmd.getBaseCmd())) {
				CommandType cmdtype = cmdlist.get(projcmd.getBaseCmd());

				if (cmdtype.getCategoryName().toLowerCase().equals("deployment")) {
					count++;
				}
			} else if (projcmd.getBaseCmd() != null) {
				String basecmd = projcmd.getBaseCmd();
				basecmd = basecmd.toLowerCase();

				if (basecmd.endsWith(".sh") || basecmd.endsWith(".py") || basecmd.startsWith("./")) {
					if (basecmd.contains("deploy")) {
						count++;
					}
				}

			}

		}

		return count;
	}

	private Map<String, Integer> getDeploymentCmdCountStat(ProjectCommands projcmds, Map<String, CommandType> cmdlist) {
		List<ProjectCommand> projcmdlist = projcmds.getProjCmds();
		Map<String, Integer> cmdfreqmap = new HashMap<>();
		int count = 0;
		for (ProjectCommand projcmd : projcmdlist) {
			if (projcmd.getBaseCmd() != null && cmdlist.containsKey(projcmd.getBaseCmd())) {
				CommandType cmdtype = cmdlist.get(projcmd.getBaseCmd());

				if (cmdtype.getCategoryName().toLowerCase().equals("deployment")) {
					count++;
					if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
						int val = cmdfreqmap.get(projcmd.getBaseCmd());
						cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
					} else {
						cmdfreqmap.put(projcmd.getBaseCmd(), 1);
					}
				}
			} else if (projcmd.getBaseCmd() != null) {
				String basecmd = projcmd.getBaseCmd();
				basecmd = basecmd.toLowerCase();

				if (basecmd.endsWith(".sh") || basecmd.endsWith(".py") || basecmd.startsWith("./")) {
					if (basecmd.contains("deploy")) {
						count++;
						if (cmdfreqmap.containsKey(projcmd.getBaseCmd())) {
							int val = cmdfreqmap.get(projcmd.getBaseCmd());
							cmdfreqmap.put(projcmd.getBaseCmd(), val + 1);
						} else {
							cmdfreqmap.put(projcmd.getBaseCmd(), 1);
						}
					}
				}

			}

		}

		return cmdfreqmap;
	}

	public List<ToolAdoption> getTravisToolAdoptionWithType() {
		TravisYamlFileParser parser = new TravisYamlFileParser();
		CVSReader csvrw = new CVSReader();
		List<ToolAdoption> tooladoplist = null;
		try {
			// String
			// json=parser.getJsonDataFromYamlFile("G:\\Research\\ML_CI\\Project_Repo\\GitRepo\\cjekel@tindetheus\\.travis.yml");
			List<ProjectCommands> projcmds = parser.getAllGitProjectCommands(false);
			List<DeploymentTask> projdeps = parser.getProjectDeploymentStatus();
			Map<String, CommandType> cmdlist = csvrw.loadCmdType(Config.csvCmdTypeFile);

			tooladoplist = analyzeAdoptionWithCmdStat(projcmds, projdeps, cmdlist);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return tooladoplist;
	}

	private List<ToolAdoption> analyzeAdoptionWithCmdStat(List<ProjectCommands> projcmds, List<DeploymentTask> projdeps,
			Map<String, CommandType> cmdlist) {
		String filepath = Config.gitProjList;

		List<String> projlist = TextFileReaderWriter.GetFileContentByLine(filepath);

		int index = 0;
		List<ToolAdoption> tooladoptionlist = new ArrayList<>();
		Map<String, FrequencyEntity> globalbuildmap = new HashMap<>();
		Map<String, FrequencyEntity> globaltestmap = new HashMap<>();
		Map<String, FrequencyEntity> globalanalmap = new HashMap<>();
		Map<String, FrequencyEntity> globaldepldmap = new HashMap<>();
		Map<String, Integer> globaldeplservicemap = new HashMap<>();

		for (String proj : projlist) {

			String projname = ProjectPropertyAnalyzer.getProjName(proj);

			String projdir = Config.repoDir + projname;
			String travisfile = projdir + "\\" + ".travis.yml";

			ToolAdoption tooladop = new ToolAdoption(projname);

			File ftrvais = new File(travisfile);
			if (ftrvais.exists()) {
				tooladop.setCi(true);
			} else {
				tooladop.setCi(false);
			}

			Map<String, Integer> buildmap = getBuildCmdCountStat(projcmds.get(index), cmdlist);
			Map<String, Integer> testmap = getTestCmdCountStat(projcmds.get(index), cmdlist);
			Map<String, Integer> anayzermap = getAnalyzerCmdCountStat(projcmds.get(index), cmdlist);
			Map<String, Integer> deplmap = getDeploymentCmdCountStat(projcmds.get(index), cmdlist);
			boolean depstatus = projdeps.get(index).getDeploymentStatus();

			if (depstatus) {
				TravisYamlFileParser parser = new TravisYamlFileParser();
				String service = parser.getDeploymentStat(projname);

				if(service.equals("releases"))
				{
					System.out.println("releases");
				}
				if (service != null) {
					if (globaldeplservicemap.containsKey(service)) {
						int count = globaldeplservicemap.get(service);
						globaldeplservicemap.put(service, count + 1);
					} else {
						globaldeplservicemap.put(service, 1);
					}
				}
			}

			////////////////
			for (String key : buildmap.keySet()) {
				if (globalbuildmap.containsKey(key)) {
					FrequencyEntity freq = globalbuildmap.get(key);
					freq.setProjFreq(freq.getProjFreq() + 1);
					freq.setInstanceFreq(freq.getInstanceFreq() + buildmap.get(key));
					globalbuildmap.put(key, freq);
				} else {
					FrequencyEntity freq = new FrequencyEntity();
					freq.setProjFreq(1);
					freq.setInstanceFreq(buildmap.get(key));
					globalbuildmap.put(key, freq);
				}
			}
			////////////////

			////////////////
			for (String key : testmap.keySet()) {
				if (globaltestmap.containsKey(key)) {
					FrequencyEntity freq = globaltestmap.get(key);
					freq.setProjFreq(freq.getProjFreq() + 1);
					freq.setInstanceFreq(freq.getInstanceFreq() + testmap.get(key));
					globaltestmap.put(key, freq);
				} else {
					FrequencyEntity freq = new FrequencyEntity();
					freq.setProjFreq(1);
					freq.setInstanceFreq(testmap.get(key));
					globaltestmap.put(key, freq);
				}
			}
			////////////////

			////////////////
			for (String key : anayzermap.keySet()) {
				if (globalanalmap.containsKey(key)) {
					FrequencyEntity freq = globalanalmap.get(key);
					freq.setProjFreq(freq.getProjFreq() + 1);
					freq.setInstanceFreq(freq.getInstanceFreq() + anayzermap.get(key));
					globalanalmap.put(key, freq);
				} else {
					FrequencyEntity freq = new FrequencyEntity();
					freq.setProjFreq(1);
					freq.setInstanceFreq(anayzermap.get(key));
					globalanalmap.put(key, freq);
				}
			}
			////////////////

			////////////////
			for (String key : deplmap.keySet()) {
				if (globaldepldmap.containsKey(key)) {
					FrequencyEntity freq = globaldepldmap.get(key);
					freq.setProjFreq(freq.getProjFreq() + 1);
					freq.setInstanceFreq(freq.getInstanceFreq() + deplmap.get(key));
					globaldepldmap.put(key, freq);
				} else {
					FrequencyEntity freq = new FrequencyEntity();
					freq.setProjFreq(1);
					freq.setInstanceFreq(deplmap.get(key));
					globaldepldmap.put(key, freq);
				}
			}
			////////////////

			index++;
		}

		System.out.println("***********Build Cmd***********");
		for (String key : globalbuildmap.keySet()) {
			FrequencyEntity freq = globalbuildmap.get(key);

			System.out.println(key + "," + freq.getProjFreq() + "," + freq.getInstanceFreq());
		}

		System.out.println("***********Test Cmd***********");
		for (String key : globaltestmap.keySet()) {
			FrequencyEntity freq = globaltestmap.get(key);

			System.out.println(key + "," + freq.getProjFreq() + "," + freq.getInstanceFreq());
		}

		System.out.println("***********Analyzer Cmd***********");
		for (String key : globalanalmap.keySet()) {
			FrequencyEntity freq = globalanalmap.get(key);

			System.out.println(key + "," + freq.getProjFreq() + "," + freq.getInstanceFreq());
		}

		System.out.println("***********Deployment Cmd***********");
		for (String key : globaldepldmap.keySet()) {
			FrequencyEntity freq = globaldepldmap.get(key);

			System.out.println(key + "," + freq.getProjFreq() + "," + freq.getInstanceFreq());
		}

		//////////////////

		System.out.println("***********Deployment Services***********");
		for (String key : globaldeplservicemap.keySet()) {
			int count = globaldeplservicemap.get(key);
			System.out.println(key + "," + count);
		}

		///////////////

		return tooladoptionlist;
	}

}
