package com.python.parser;

import com.github.gumtreediff.gen.python.PythonTreeGenerator;

public class PatchedPythonTreeGenerator extends PythonTreeGenerator {

	private static final String PYTHONPARSER_CMD = System.getProperty("gt.pp.path", "pythonparser");
	
	@Override
	public String[] getCommandLine(String file) {
		return new String[] {"python", PYTHONPARSER_CMD,  "\"" + file + "\""};
	}
}
