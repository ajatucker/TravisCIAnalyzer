package com.utility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.config.Config;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.travisdiff.DecorateJSonTree;
import com.travisdiff.TravisCITree;

public class TravisReportGenerator {
	//private int count;
	@SuppressWarnings("unused")
	private Map<String, Integer> commandMap;
	
	public void getCommandMap()
	{
		System.out.println("This is the map:");
		for(Map.Entry<String, Integer> entry : commandMap.entrySet())
		{
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
	}
	public TravisReportGenerator()
	{
		commandMap = new HashMap<String, Integer>();
		//setCount(0);
		//System.out.println("?");
	}
	public void compareFiles(String curr, String prev)
	{
		TravisCITree travistree = new TravisCITree();
		ITree prevtree = travistree
				.getTravisCITree(Config.rootDir+"temp/"+prev);
		ITree curtree = travistree
				.getTravisCITree(Config.rootDir+"temp/"+curr);
		
		//keep a map, count of lifecycle of travis then generate a report of all data

		Matcher defaultMatcher = Matchers.getInstance().getMatcher(); // retrieves the default matcher
		MappingStore mappings = defaultMatcher.match(prevtree, curtree); // computes the mappings between the trees
		EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator(); // instantiates the
																							// simplified Chawathe
																							// script generator
		EditScript actions = editScriptGenerator.computeActions(mappings); // computes the edit script

		System.out.println("Pre-print test");

		DecorateJSonTree decojson = new DecorateJSonTree();
		
		Iterator<Action> iter = actions.iterator();
		for (Action action : actions) {
			
			String strfield = decojson.getJsonField(action);
			strfield = strfield.replaceAll("\"", "");
			System.out.println(strfield);
			action.getNode().setMetadata("json_parent", strfield);
			if(iter.hasNext() == true)
			{
				Action nxtaction = iter.next(); 
				String nextfield = decojson.getJsonField(nxtaction);
				if(commandMap.containsKey(strfield+"->"+nextfield) == true)
				{
					commandMap.put(strfield+"->"+nextfield, commandMap.get(strfield+"->"+nextfield) + 1);
				}
				else
				{					
					commandMap.put(strfield+"->"+nextfield, 1);
				}
			}
		}

		System.out.println("Post-print new test");
	}
	/*
	 * public int getCount() { return count; } public void setCount(int count) {
	 * this.count = count; }
	 */
}
