package com.travisdiff;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.assimbly.docconverter.DocConverter;

import com.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.gen.antlr3.json.AntlrJsonTreeGenerator;
import com.github.gumtreediff.tree.*;
import edu.util.fileprocess.FileWriterUtil;

public class TravisCITree {

	public ITree getTravisCITree(String yamlfile) {
		FileWriterUtil fwriter = new FileWriterUtil();

		String yaml;
		String json = null;
		try {
			yaml = DocConverter.convertFileToString(yamlfile);
			
			if(yaml.length()<1)
				json="{\"placeholder\":\"John\"}";
			else
				json = DocConverter.convertYamlToJson(yaml);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String filename=Config.patchDir+"/"+"testj1.json";
		
		fwriter.writetoFile(filename, json);

		/*
		 * ObjectMapper objectMapper = new ObjectMapper();
		 * 
		 * JsonNode jsonNode = null;
		 * 
		 * try {
		 * 
		 * jsonNode = objectMapper.readTree(json); System.out.println(jsonNode);
		 * 
		 * } catch (IOException e) { e.printStackTrace(); }
		 */

		TreeContext tc = null;
		Reader targetReader = new StringReader(filename);
		try {
//			tc = new AntlrJsonTreeGenerator().generateFrom().charset("UTF-8")
//					.stream(getClass().getResourceAsStream(filename));
			
			//tc = new AntlrJsonTreeGenerator().generate(targetReader);
			Run.initGenerators(); // registers the available parsers
			String file = filename;
			tc = TreeGenerators.getInstance().getTree(file); // retrieves and applies the default parser for the file 
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File ftemp=new File(filename);
		
		if(ftemp.exists())
			ftemp.delete();
		
		

		ITree tree = tc.getRoot();

		return tree;
	}

}
