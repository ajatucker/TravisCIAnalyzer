package com.travis.parser;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assimbly.docconverter.DocConverter;

import com.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.travis.task.DeploymentTask;
import com.travis.task.ProjectCommands;
import com.unity.entity.PerfFixData;
import com.utility.MapSorter;
import com.utility.ProjectPropertyAnalyzer;

import edu.util.fileprocess.TextFileReaderWriter;

public class TravisYamlFileParser {

	public TravisYamlFileParser() {
		Run.initGenerators();
	}
	
	/**Used to be able to return both the editscript and the mappings from a function*/
	public class EditResults{
		public final EditScript edits;
		public final MappingStore mappings;
		public final TreeContext ctx;
		
		public EditResults(EditScript edits, MappingStore mappings, TreeContext ctx) {
			this.edits = edits;
			this.mappings = mappings;
			this.ctx = ctx;
		}
	}
	
	public EditResults getYamlDiff(String before, String after) {
		EditScript edits = null;
		MappingStore mappings = null;
		TreeContext ctx = null;
		try {
			ctx = new YamlTreeGenerator().generate(new StringReader(before));
			ITree tree1 = new YamlTreeGenerator().generateFrom().string(before).getRoot();
			ITree tree2 = new YamlTreeGenerator().generateFrom().string(after).getRoot();
			Matcher matcher = Matchers.getInstance().getMatcher();
			mappings = matcher.match(tree1, tree2);
			EditScriptGenerator scriptGen = new SimplifiedChawatheScriptGenerator();
			edits = scriptGen.computeActions(mappings);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new EditResults(edits, mappings, ctx);
	}
	
	/**Serializes a yaml diff, as returned by getYamlDiff. Format is the Gumtree JSON format*/
	public static String getYamlDiffStr(EditResults results) {
		try {
			StringWriter sw = new StringWriter();
			ActionsIoUtils.toJson(results.ctx, results.edits, results.mappings).writeTo(sw);
			return sw.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		/*
		int i = 0;
		Pattern newlineDetect = Pattern.compile("\r?\n\t* *"); //detect yaml newlines, which can be followed by whitespace for indentation
		StringBuilder sb = new StringBuilder("['");
		for(Action a : results.edits) {
			if(i++ != 0) {
				sb.append("','");
			}
			sb.append(a.getName().trim());
			sb.append(":{");
			String treeStr = a.getNode().toTreeString().trim();
			treeStr = newlineDetect.matcher(treeStr).replaceAll(", ");
			if(results.mappings.isSrcMapped(a.getNode()) || a.getName().contains("delet")) //deletions or updates only
				sb.append(treeStr);
			sb.append("}->{");
			ITree mapped = results.mappings.getDstForSrc(a.getNode()); //only non-null for updates and moves
			String treeStr2 = ""; //should be left as this for deletions
			if(a.getName().contains("insert")) //addition
				treeStr2 = treeStr; //addition should be {},{newStuff}
			if(mapped != null) { //update or move of some sort
				treeStr2 = mapped.toTreeString().trim();
				treeStr2 = newlineDetect.matcher(treeStr2).replaceAll(", ");
			}
			sb.append(treeStr2);
			sb.append("}");
			
			//System.out.println("Edit number: " + i);
			//System.out.println("Name: " + a.getName());
			//System.out.println("Tree:\n" + a.getNode().toTreeString());
			//System.out.println("\nMapped tree:\n" + (mapped == null ? "Tree empty!" : mapped.toTreeString()));
			//System.out.println("\n\n");
			
		}
		sb.append("']");
		System.out.println(sb.toString());
		return sb.toString();
		*/
	}
	
	public String getJsonDataFromYamlFile(String filepath) throws Exception {
		String yaml = DocConverter.convertFileToString(filepath);

		String json = DocConverter.convertYamlToJson(yaml);

		ObjectMapper objectMapper = new ObjectMapper();

		try {

			JsonNode jsonNode = objectMapper.readTree(json);
			System.out.println(jsonNode);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return json;
	}

	public JsonNode getJsonObjectFromYamlFile(String filepath) throws Exception {
		String yaml = DocConverter.convertFileToString(filepath);

		String json = DocConverter.convertYamlToJson(yaml);

		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode jsonNode = null;

		try {

			jsonNode = objectMapper.readTree(json);
			// System.out.println(jsonNode);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return jsonNode;
	}

	public List<ProjectCommand> getAllProjectCommands() {
		List<ProjectCommand> projcmdlist = new ArrayList<>();

		String filepath = Config.gitProjList;

		List<String> projlist = TextFileReaderWriter.GetFileContentByLine(filepath);
		List<PerfFixData> fixdata = new ArrayList<>();

		for (String proj : projlist) {

			List<String> allcmds = new ArrayList<String>();
			String projname = ProjectPropertyAnalyzer.getProjName(proj);
			String projdir = Config.repoDir + projname;
			String travisfile = projdir + "\\" + ".travis.yml";

			File ftrvais = new File(travisfile);
			Map<String, String> envmap = getEnvVariable(proj);

			if (ftrvais.exists()) {
				JsonNode jsonnode = null;
				try {
					jsonnode = getJsonObjectFromYamlFile(travisfile);
					List<String> install = new ArrayList<>();
					searchForEntity(install, jsonnode, "install");

					List<String> script = new ArrayList<>();
					searchForEntity(script, jsonnode, "script");

					List<String> before_install = new ArrayList<>();
					searchForEntity(before_install, jsonnode, "before_install");

					List<String> before_script = new ArrayList<>();
					searchForEntity(before_script, jsonnode, "before_script");

					List<String> after_script = new ArrayList<>();
					searchForEntity(after_script, jsonnode, "after_script");

					List<String> after_success = new ArrayList<>();
					traverseForDeployment(after_success, jsonnode, "after_success", false);

					List<String> after_failure = new ArrayList<>();
					searchForEntity(after_failure, jsonnode, "after_failure");

					allcmds.addAll(install);
					allcmds.addAll(script);
					allcmds.addAll(before_install);
					allcmds.addAll(before_script);
					allcmds.addAll(after_script);
					allcmds.addAll(after_success);
					allcmds.addAll(after_failure);

					for (String cmd : allcmds) {
						projcmdlist.add(new ProjectCommand(projname, cmd, envmap));
					}

					// System.out.println(jsonnode);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		BashCmdAnalysis bashcmdanalysis = new BashCmdAnalysis();

		for (int index = 0; index < projcmdlist.size(); index++) {
			List<String> basecmds = bashcmdanalysis.getBashCommandTree(projcmdlist.get(index).getCmdName(),
					projcmdlist.get(index).getEnvs());

			if (basecmds != null && basecmds.size() > 0) {
				projcmdlist.get(index).setBaseCmd(basecmds.get(0));
			}
		}

		return projcmdlist;
	}
	
	
	public List<ProjectCommand> getAllProjectCommands(String projectlist) {
		List<ProjectCommand> projcmdlist = new ArrayList<>();

		String filepath = projectlist;

		List<String> projlist = TextFileReaderWriter.GetFileContentByLine(filepath);
		List<PerfFixData> fixdata = new ArrayList<>();

		for (String proj : projlist) {

			List<String> allcmds = new ArrayList<String>();
			String projname = ProjectPropertyAnalyzer.getProjName(proj);
			String projdir = Config.repoDir + projname;
			String travisfile = projdir + "\\" + ".travis.yml";

			File ftrvais = new File(travisfile);
			Map<String, String> envmap = getEnvVariable(proj);

			if (ftrvais.exists()) {
				JsonNode jsonnode = null;
				try {
					jsonnode = getJsonObjectFromYamlFile(travisfile);
					List<String> install = new ArrayList<>();
					searchForEntity(install, jsonnode, "install");

					List<String> script = new ArrayList<>();
					searchForEntity(script, jsonnode, "script");

					List<String> before_install = new ArrayList<>();
					searchForEntity(before_install, jsonnode, "before_install");

					List<String> before_script = new ArrayList<>();
					searchForEntity(before_script, jsonnode, "before_script");

					List<String> after_script = new ArrayList<>();
					searchForEntity(after_script, jsonnode, "after_script");

					List<String> after_success = new ArrayList<>();
					traverseForDeployment(after_success, jsonnode, "after_success", false);

					List<String> after_failure = new ArrayList<>();
					searchForEntity(after_failure, jsonnode, "after_failure");

					allcmds.addAll(install);
					allcmds.addAll(script);
					allcmds.addAll(before_install);
					allcmds.addAll(before_script);
					allcmds.addAll(after_script);
					allcmds.addAll(after_success);
					allcmds.addAll(after_failure);

					for (String cmd : allcmds) {
						projcmdlist.add(new ProjectCommand(projname, cmd, envmap));
					}

					// System.out.println(jsonnode);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		BashCmdAnalysis bashcmdanalysis = new BashCmdAnalysis();

		for (int index = 0; index < projcmdlist.size(); index++) {
			List<String> basecmds = bashcmdanalysis.getBashCommandTree(projcmdlist.get(index).getCmdName(),
					projcmdlist.get(index).getEnvs());

			if (basecmds != null && basecmds.size() > 0) {
				projcmdlist.get(index).setBaseCmd(basecmds.get(0));
			}
		}

		return projcmdlist;
	}

	public List<ProjectCommands> getAllGitProjectCommands(boolean isevalproj) {

		String filepath=null;
		
		if(isevalproj)
			filepath= Config.gitProjListEval;
		else
			filepath= Config.gitProjList;

		List<String> projlist = TextFileReaderWriter.GetFileContentByLine(filepath);
		List<ProjectCommands> allprojcmdlist = new ArrayList<>();

		for (String proj : projlist) {
			List<ProjectCommand> projcmdlist = new ArrayList<>();
			List<String> strallcmds = new ArrayList<String>();

			String projname = ProjectPropertyAnalyzer.getProjName(proj);
			String projdir = Config.repoDir + projname;
			String travisfile = projdir + "\\" + ".travis.yml";

			File ftrvais = new File(travisfile);
			Map<String, String> envmap = getEnvVariable(proj);
			ProjectCommands projcmds = new ProjectCommands(projname);

			if (ftrvais.exists()) {
				JsonNode jsonnode = null;
				try {
					jsonnode = getJsonObjectFromYamlFile(travisfile);
					List<String> install = new ArrayList<>();
					searchForEntity(install, jsonnode, "install");

					List<String> script = new ArrayList<>();
					searchForEntity(script, jsonnode, "script");

					List<String> before_install = new ArrayList<>();
					searchForEntity(before_install, jsonnode, "before_install");

					List<String> before_script = new ArrayList<>();
					searchForEntity(before_script, jsonnode, "before_script");

					List<String> after_script = new ArrayList<>();
					searchForEntity(after_script, jsonnode, "after_script");

					List<String> after_success = new ArrayList<>();
					searchForEntity(after_success, jsonnode, "after_success");

					List<String> after_failure = new ArrayList<>();
					searchForEntity(after_failure, jsonnode, "after_failure");

					strallcmds.addAll(install);
					strallcmds.addAll(script);
					strallcmds.addAll(before_install);
					strallcmds.addAll(before_script);
					strallcmds.addAll(after_script);
					strallcmds.addAll(after_success);
					strallcmds.addAll(after_failure);

					for (String cmd : strallcmds) {
						projcmdlist.add(new ProjectCommand(projname, cmd, envmap));
					}

					BashCmdAnalysis bashcmdanalysis = new BashCmdAnalysis();

					for (int index = 0; index < projcmdlist.size(); index++) {

						List<String> basecmds = bashcmdanalysis.getBashCommandTree(projcmdlist.get(index).getCmdName(),
								projcmdlist.get(index).getEnvs());

						if (basecmds != null && basecmds.size() > 0) {
							projcmdlist.get(index).setBaseCmd(basecmds.get(0));
						}
					}

					projcmds.addProjCmds(projcmdlist);
					// System.out.println(jsonnode);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			allprojcmdlist.add(projcmds);

		}

		return allprojcmdlist;
	}

	public Map<String, String> getEnvVariable(String proj) {

		List<String> allenvs = new ArrayList<String>();
		String projname = ProjectPropertyAnalyzer.getProjName(proj);
		String projdir = Config.repoDir + projname;
		String travisfile = projdir + "\\" + ".travis.yml";

		File ftrvais = new File(travisfile);
		Map<String, String> envs = new HashMap<String, String>();
		if (ftrvais.exists()) {
			JsonNode jsonnode = null;
			try {
				jsonnode = getJsonObjectFromYamlFile(travisfile);
				List<String> envlist = new ArrayList<>();
				traverseForEnv(envlist, jsonnode, false);
				System.out.println(envlist);
				allenvs.addAll(envlist);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		BashCmdAnalysis bashcmdanalysis = new BashCmdAnalysis();

		for (int index = 0; index < allenvs.size(); index++) {
			Map<String, String> env = bashcmdanalysis.getBashEnvVariable(allenvs.get(index));

			if (env != null && env.size() > 0) {
				System.out.println(envs);
				for (String key : env.keySet()) {
					if (!envs.containsKey(key)) {
						envs.put(key, env.get(key));
					}
				}
			}
		}

		return envs;

	}

	public boolean getDeployment(String proj) {

		List<String> allenvs = new ArrayList<String>();
		String projname = ProjectPropertyAnalyzer.getProjName(proj);
		String projdir = Config.repoDir + projname;
		String travisfile = projdir + "\\" + ".travis.yml";
		boolean flag = false;
		File ftrvais = new File(travisfile);
		Map<String, String> envs = new HashMap<String, String>();
		if (ftrvais.exists()) {
			JsonNode jsonnode = null;
			try {
				jsonnode = getJsonObjectFromYamlFile(travisfile);
				List<String> envlist = new ArrayList<>();
				traverseForDeployment(envlist, jsonnode, false);
				System.out.println(envlist);
				allenvs.addAll(envlist);

				if (allenvs.size() > 0) {
					flag = true;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return flag;

	}
	
	public boolean getLanguage(String proj) {

		List<String> allenvs = new ArrayList<String>();
		String projname = ProjectPropertyAnalyzer.getProjName(proj);
		String projdir = Config.repoDir + projname;
		String travisfile = projdir + "\\" + ".travis.yml";
		boolean flag = false;
		File ftrvais = new File(travisfile);
		Map<String, String> envs = new HashMap<String, String>();
		if (ftrvais.exists()) {
			JsonNode jsonnode = null;
			try {
				jsonnode = getJsonObjectFromYamlFile(travisfile);
				List<String> envlist = new ArrayList<>();
				traverseForLanguage(envlist, jsonnode, false);
				System.out.println(envlist);
				allenvs.addAll(envlist);

				if (allenvs.size() > 0) {
					flag = true;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return flag;

	}

	public String getDeploymentStat(String proj) {

		List<String> allenvs = new ArrayList<String>();
		//String projname = ProjectPropertyAnalyzer.getProjName(proj);
		String projdir = Config.repoDir + proj;
		String travisfile = projdir + "\\" + ".travis.yml";
		String provider = null;
		boolean flag = false;
		File ftrvais = new File(travisfile);
		Map<String, String> envs = new HashMap<String, String>();
		if (ftrvais.exists()) {
			JsonNode jsonnode = null;
			try {
				jsonnode = getJsonObjectFromYamlFile(travisfile);
				List<String> envlist = new ArrayList<>();
				traverseForDeploymentStat(envlist, jsonnode, false);
				System.out.println(envlist);
				allenvs.addAll(envlist);

				if (allenvs.size() > 0) {
					provider = allenvs.get(0);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return provider;

	}

	public List<DeploymentTask> getProjectDeploymentStatus() {
		List<DeploymentTask> projstatus = new ArrayList<>();
		String filepath = Config.gitProjList;

		List<String> projlist = TextFileReaderWriter.GetFileContentByLine(filepath);

		for (String proj : projlist) {
			boolean status = getDeployment(proj);

			DeploymentTask deptask = new DeploymentTask(proj, status);
			projstatus.add(deptask);
		}

		return projstatus;
	}
	
	public List<DeploymentTask> getProjectLanguageStatus() {
		List<DeploymentTask> projstatus = new ArrayList<>();
		String filepath = Config.gitProjList;

		List<String> projlist = TextFileReaderWriter.GetFileContentByLine(filepath);

		for (String proj : projlist) {
			boolean status = getLanguage(proj);

			DeploymentTask deptask = new DeploymentTask(proj, status);
			projstatus.add(deptask);
		}

		return projstatus;
	}

	public String getProjectDeploymentServiceName(String projname) {

		String service = getDeploymentStat(projname);

		return service;
	}

	public static void traverseForEnv(List<String> envlist, JsonNode root, boolean flag) {

		if (root.isObject()) {
			Iterator<String> fieldNames = root.fieldNames();

			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				if (fieldName.equals("env")) {
					flag = true;
				} else if (fieldName.equals("global") && root.findPath("env") != null) {
					flag = true;
				} else if (fieldName.equals("jobs") && root.findPath("env") != null) {
					flag = true;
				} else {
					flag = false;
				}
				JsonNode fieldValue = root.get(fieldName);
				traverseForEnv(envlist, fieldValue, flag);
			}
		} else if (root.isArray()) {
			ArrayNode arrayNode = (ArrayNode) root;
			for (int i = 0; i < arrayNode.size(); i++) {
				JsonNode arrayElement = arrayNode.get(i);
				traverseForEnv(envlist, arrayElement, flag);
			}
		} else {
			if (root != null) {
				JsonNode parent = root.findParent("env");
				if (flag == true) {
					envlist.add(root.asText());
					// System.out.println(root.asText());
				}
			}

		}
	}

	public static void traverseForDeployment(List<String> envlist, JsonNode root, boolean flag) {

		if (root.isObject()) {
			Iterator<String> fieldNames = root.fieldNames();

			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				if (fieldName.equals("deploy")) {
					flag = true;
				} else if (fieldName.equals("provider") && root.findPath("deploy") != null) {
					flag = true;
				} else {
					flag = false;
				}
				JsonNode fieldValue = root.get(fieldName);
				traverseForDeployment(envlist, fieldValue, flag);
			}
		} else if (root.isArray()) {
			ArrayNode arrayNode = (ArrayNode) root;
			for (int i = 0; i < arrayNode.size(); i++) {
				JsonNode arrayElement = arrayNode.get(i);
				traverseForDeployment(envlist, arrayElement, flag);
			}
		} else {
			if (root != null) {
				JsonNode parent = root.findParent("deploy");
				if (flag == true) {
					envlist.add(root.asText());
					// System.out.println(root.asText());
				}
			}

		}
	}
	
	public static void traverseForLanguage(List<String> envlist, JsonNode root, boolean flag) {

		if (root.isObject()) {
			Iterator<String> fieldNames = root.fieldNames();

			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				if (fieldName.equals("language")) {
					flag = true;
				} else if (fieldName.equals("language") && root.findPath("language") != null) {
					flag = true;
				} else {
					flag = false;
				}
				JsonNode fieldValue = root.get(fieldName);
				traverseForLanguage(envlist, fieldValue, flag);
			}
		} else if (root.isArray()) {
			ArrayNode arrayNode = (ArrayNode) root;
			for (int i = 0; i < arrayNode.size(); i++) {
				JsonNode arrayElement = arrayNode.get(i);
				traverseForLanguage(envlist, arrayElement, flag);
			}
		} else {
			if (root != null) {
				JsonNode parent = root.findParent("language");
				if (flag == true) {
					envlist.add(root.asText());
					// System.out.println(root.asText());
				}
			}

		}
	}
	
	
	

	public static void traverseForDeploymentStat(List<String> envlist, JsonNode root, boolean flag) {

		if (root.isObject()) {
			Iterator<String> fieldNames = root.fieldNames();

			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				if (fieldName.equals("deploy")) {
					flag = true;
				} else if (fieldName.equals("provider") && root.findPath("deploy") != null) {
					flag = true;
					envlist.add(root.get(fieldName).asText());
				} else {
					flag = false;
				}
				JsonNode fieldValue = root.get(fieldName);
				traverseForDeployment(envlist, fieldValue, flag);
			}
		} else if (root.isArray()) {
			ArrayNode arrayNode = (ArrayNode) root;
			for (int i = 0; i < arrayNode.size(); i++) {
				JsonNode arrayElement = arrayNode.get(i);
				traverseForDeployment(envlist, arrayElement, flag);
			}
		} else {
			if (root != null) {
				JsonNode parent = root.findParent("deploy");
				if (flag == true) {
					// envlist.add(root.asText());
					// System.out.println(root.asText());
				}
			}

		}
	}

	public static void traverseForDeployment(List<String> envlist, JsonNode root, String entityName, boolean flag) {

		if (root.isObject()) {
			Iterator<String> fieldNames = root.fieldNames();

			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				if (fieldName.equals(entityName)) {
					flag = true;
				} else {
					flag = false;
				}
				JsonNode fieldValue = root.get(fieldName);
				traverseForDeployment(envlist, fieldValue, entityName, flag);
			}
		} else if (root.isArray()) {
			ArrayNode arrayNode = (ArrayNode) root;
			for (int i = 0; i < arrayNode.size(); i++) {
				JsonNode arrayElement = arrayNode.get(i);
				traverseForDeployment(envlist, arrayElement, entityName, flag);
			}
		} else {
			if (root != null) {
				JsonNode parent = root.findParent(entityName);
				if (flag == true) {
					envlist.add(root.asText());
					// System.out.println(root.asText());
				}
			}

		}
	}

	public List<String> getJsonValues(JsonNode jsonnode, String key) {
		List<String> values = new ArrayList<String>();

		JsonNode nodes = jsonnode.get(key);

		if (nodes != null && nodes.isArray()) {
			for (JsonNode node : nodes) {
				values.add(node.asText());
			}
		} else if (nodes != null && !nodes.isArray()) {
			values.add(nodes.asText());
		}

		return values;
	}

	private JsonNode searchForEntity(List<String> values, JsonNode node, String entityName) {
		// A naive depth-first search implementation using recursion. Useful
		// **only** for small object graphs. This will be inefficient
		// (stack overflow) for finding deeply-nested needles or needles
		// toward the end of a forest with deeply-nested branches.
		if (node == null) {
			return null;
		}
		if (node.has(entityName)) {
			List<String> vals = getJsonValues(node, entityName);

			for (String val : vals) {

//	    		if(!values.contains(val))
//	    		{
				values.add(val);
//	    		}
			}

			return node.get(entityName);
		}
		if (!node.isContainerNode()) {
			return null;
		}

		JsonNode childResult = null;
		boolean found = false;
		for (JsonNode child : node) {
			if (child.isContainerNode()) {
				childResult = searchForEntity(values, child, entityName);

//	            if (childResult != null && !childResult.isMissingNode()) {
//	              found=true;  
//	            }
			}
		}

		// not found fall through
		return null;
	}

	private JsonNode searchForEntityV2(List<String> values, JsonNode node, String entityName) {
		// A naive depth-first search implementation using recursion. Useful
		// **only** for small object graphs. This will be inefficient
		// (stack overflow) for finding deeply-nested needles or needles
		// toward the end of a forest with deeply-nested branches.

		if (node == null) {
			return null;
		}
		if (node.has(entityName)) {
			List<String> vals = getJsonValues(node, entityName);

			for (String val : vals) {

//	    		if(!values.contains(val))
//	    		{
				values.add(val);
//	    		}
			}

			return node.get(entityName);
		}
		if (!node.isContainerNode()) {
			return null;
		}

		JsonNode childResult = null;
		boolean found = false;
		for (JsonNode child : node) {
			if (child.isContainerNode()) {
				childResult = searchForEntity(values, child, entityName);

//	            if (childResult != null && !childResult.isMissingNode()) {
//	              found=true;  
//	            }
			}
		}

		// not found fall through
		return null;
	}

	public LinkedHashMap<String, Integer> getProjectBlockStatus() {
		List<String> projblocks = new ArrayList<>();
		Map<String, Integer> blockmap = new HashMap<>();

		String filepath = Config.gitProjList;

		List<String> projlist = TextFileReaderWriter.GetFileContentByLine(filepath);

		for (String proj : projlist) {
			projblocks = getBlockNames(proj);
			if (!projblocks.contains("language"))
				System.out.println(proj);

			for (String block : projblocks) {
				if (blockmap.containsKey(block)) {
					int val = blockmap.get(block);
					blockmap.put(block, val + 1);
				} else {
					blockmap.put(block, 1);
				}
			}

		}
		LinkedHashMap<String, Integer> sortedblocks = MapSorter.getSortedMap(blockmap);

		return sortedblocks;
	}

	public List<String> getBlockNames(String proj) {

		List<String> allenvs = new ArrayList<String>();
		String projname = ProjectPropertyAnalyzer.getProjName(proj);
		String projdir = Config.repoDir + projname;
		String travisfile = projdir + "\\" + ".travis.yml";
		boolean flag = false;
		File ftrvais = new File(travisfile);
		Map<String, String> envs = new HashMap<String, String>();
		List<String> blocknames = new ArrayList<>();
		if (ftrvais.exists()) {
			JsonNode jsonnode = null;
			try {
				jsonnode = getJsonObjectFromYamlFile(travisfile);
				List<String> envlist = new ArrayList<>();

				traverseForBlocks(envlist, blocknames, jsonnode, false);
				// System.out.println(envlist);
				allenvs.addAll(envlist);

				if (allenvs.size() > 0) {
					flag = true;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return blocknames;

	}

	public static void traverseForBlocks(List<String> envlist, List<String> blocknames, JsonNode root, boolean flag) {

		if (root.isObject()) {
			Iterator<String> fieldNames = root.fieldNames();

			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();

				if (!blocknames.contains(fieldName))
					blocknames.add(fieldName);

				if (fieldName.equals("deploy")) {
					flag = true;
				} else if (fieldName.equals("provider") && root.findPath("deploy") != null) {
					flag = true;
				} else {
					flag = false;
				}
				JsonNode fieldValue = root.get(fieldName);
				traverseForBlocks(envlist, blocknames, fieldValue, flag);
			}
		} else if (root.isArray()) {
			ArrayNode arrayNode = (ArrayNode) root;
			for (int i = 0; i < arrayNode.size(); i++) {
				JsonNode arrayElement = arrayNode.get(i);
				traverseForBlocks(envlist, blocknames, arrayElement, flag);
			}
		} else {
			if (root != null) {
				JsonNode parent = root.findParent("deploy");
				if (flag == true) {
					envlist.add(root.asText());
					// System.out.println(root.asText());
				}
			}

		}
	}

}
