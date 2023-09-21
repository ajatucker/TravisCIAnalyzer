package com.python.parser;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.config.Config;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class PythonFileParser {

	/**Used to be able to return both the editscript and the mappings from a function*/
	public class EditResults{
		public final EditScript edits;
		public final MappingStore mappings;
		public final String fileName;
		public final TreeContext ctx;
		
		public EditResults(String fileName, EditScript edits, MappingStore mappings, TreeContext beforeContext) {
			this.fileName = fileName;
			this.edits = edits;
			this.mappings = mappings;
			this.ctx = beforeContext;
		}
	}
	
	public PythonFileParser() {
		Run.initGenerators();
	}
	
	static final Path logPath = Path.of(Config.rootDir + "python_errors.log");
	
	public EditResults getPythonDiff(String beforeContent, String afterContent, String originalFileName) {
		Run.initGenerators();
		EditScript edits = null;
		MappingStore mappings = null;
		TreeContext ctx1 = null;
		try {
			PatchedPythonTreeGenerator gen1, gen2;
			gen1 = new PatchedPythonTreeGenerator();
			gen2 = new PatchedPythonTreeGenerator();
			
			ctx1 = gen1.generate(new StringReader(beforeContent));
			ITree tree1 = gen1.generateFrom().string(beforeContent).getRoot();
			ITree tree2 = gen2.generateFrom().string(afterContent).getRoot();
			Matcher matcher = Matchers.getInstance().getMatcher();
			mappings = matcher.match(tree1, tree2);
			EditScriptGenerator scriptGen = new SimplifiedChawatheScriptGenerator();
			edits = scriptGen.computeActions(mappings);
			return new EditResults(originalFileName, edits, mappings, ctx1);
		} catch (IOException e) {
			try {
				Files.writeString(logPath, originalFileName + System.lineSeparator(), StandardOpenOption.APPEND);
			} catch (IOException e1) {
				System.out.println("Failed to write about error");
				e1.printStackTrace();
			}
			e.printStackTrace();
		}catch (SyntaxException e) {
			System.err.println("Syntax Error");
			try {
				Files.writeString(logPath, originalFileName + System.lineSeparator(), StandardOpenOption.APPEND);
			} catch (IOException e1) {
				System.out.println("Failed to write about error");
				e1.printStackTrace();
			}
			e.printStackTrace();
		}catch (RuntimeException e) {
			e.printStackTrace();
			try {
				Files.writeString(logPath, originalFileName + System.lineSeparator(), StandardOpenOption.APPEND);
			} catch (IOException e1) {
				System.out.println("Failed to write about error");
				e1.printStackTrace();
			}
		}
		return new EditResults(originalFileName, edits, mappings, ctx1);
	}
	
	/**Formats the string into Gumtree's JSON format*/
	public static String getPythonDiffString(EditResults results) {
		try {
			StringWriter sw = new StringWriter();
			ActionsIoUtils.toJson(results.ctx, results.edits, results.mappings).writeTo(sw);
			return sw.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
