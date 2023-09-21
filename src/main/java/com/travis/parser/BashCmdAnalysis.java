package com.travis.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.crashub.bash.script.Script;
import org.gentoo.libbash.java_libbashLexer;
import org.gentoo.libbash.java_libbashParser;

import com.config.Config;

public class BashCmdAnalysis {
	
	
	public List<String> getBashCommandTree(String s,Map<String,String> envmap)
	{
//		  java_libbashLexer lexer = new java_libbashLexer(new ANTLRStringStream(s));
//		  java_libbashParser libparser= new java_libbashParser(new CommonTokenStream(lexer));
//		  try {
//			  Object tree=libparser.start().getTree();
//			  
//			  System.out.println(tree);
//		} catch (RecognitionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		s=s.replace("sudo", "");
		s=s.replace("travis_retry", "");
		s=s.replace("travis-sphinx","");
		s=s.replace("before_install","");
		s=s.replace("_before_install","");
		
		if(s.contains("travis_wait"))
		{
			Pattern pattern = Pattern.compile("(travis_wait)(\\s+)(\\d+)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(s); 
		    s = matcher.replaceAll("");		
		}
		
		s=s.replace("travis_wait","");
		s=s.replace("travis wait","");
		
		s=s.trim();
		
		if(s.length()<=0)
			return null;
		
		List<String> commands=null;
		try {
			Script script=new Script(s);
			Tree tree=script.getTree();
			
			script.prettyPrint();
			
			//script.getCommandList(tree);
			script.getCommandListWithCompundCmd(tree,envmap);
			
			commands=script.getCommands();
			
			if(isCompoundCmd(commands))
			{
				
			}
			
			
			System.out.print(commands);
			
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return commands;
		
	}
	
	
	public List<String> getBashCommandTreeFromChange(String s,Map<String,String> envmap)
	{
//		  java_libbashLexer lexer = new java_libbashLexer(new ANTLRStringStream(s));
//		  java_libbashParser libparser= new java_libbashParser(new CommonTokenStream(lexer));
//		  try {
//			  Object tree=libparser.start().getTree();
//			  
//			  System.out.println(tree);
//		} catch (RecognitionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		s=s.replace("sudo", "");
		s=s.replace("travis_retry", "");
		s=s.replace("travis-sphinx","");
		s=s.replace("before_install","");
		s=s.replace("_before_install","");		
		
		if(s.contains("travis_wait"))
		{
			Pattern pattern = Pattern.compile("(travis_wait)(\\s+)(\\d+)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(s); 
		    s = matcher.replaceAll("");		
		}
		
		s=s.replace("travis_wait","");
		s=s.replace("travis wait","");
		
		s=s.trim();
		
		if(s.length()<=0)
			return null;
		
		List<String> commands=null;
		try {
			Script script=new Script(s);
			Tree tree=script.getTree();
			
			script.prettyPrint();
			
			//script.getCommandList(tree);
			script.getCommandListWithCompundCmd(tree,envmap);
			
			commands=script.getCommands();
			
			if(isCompoundCmd(commands))
			{
				
			}
			
			
			//System.out.print(commands);
			
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return commands;
		
	}
	
	
	public Map<String, String> getBashEnvVariable(String s)
	{
//		  java_libbashLexer lexer = new java_libbashLexer(new ANTLRStringStream(s));
//		  java_libbashParser libparser= new java_libbashParser(new CommonTokenStream(lexer));
//		  try {
//			  Object tree=libparser.start().getTree();
//			  
//			  System.out.println(tree);
//		} catch (RecognitionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		s=s.replace("sudo", "");
		s=s.replace("travis_retry", "");
		s=s.replace("travis-sphinx","");
		
		if(s.contains("travis_wait"))
		{
			Pattern pattern = Pattern.compile("(travis_wait)(\\s+)(\\d+)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(s); 
		    s = matcher.replaceAll("");		
		}
		
		s=s.replace("travis_wait","");
		s=s.trim();
		
		//Map<String,String> allenvs=new HashMap<String,String>();
		Map<String, String> envmap=new HashMap<String,String>();
		try {
			Script script=new Script(s);
			Tree tree=script.getTree();
			
			script.prettyPrint();
			
			//script.getCommandList(tree);
			script.getEnvDefination(tree,envmap);
			
			System.out.println("test");
			
			

		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return envmap;
		
	}
	
	
	private boolean isCompoundCmd(List<String>  cmds)
	{
		boolean flag=false;
		String basecmd = null;
		if(cmds!=null & cmds.size()>0)
		{
			basecmd=cmds.get(0);
		}
		
		for(String cmd:Config.compoundcmds)
		{
			if(cmd.equals(basecmd))
			{
				flag=true;
				break;
			}
		}		
		
		return flag;
	}

}
