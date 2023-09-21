package com.build.commitanalyzer;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class ListConverter extends AbstractBeanField<String[]>{
	
	public ListConverter() {
		super();
	}
	
	@Override
	protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
		value.trim(); //just in case
		value = value.substring(2, value.length()-2); //remove ['   '] from ends. Now we have our strings separated by ', '
		return value.split("', ?'");
	}
	
	@Override
	public String convertToWrite(Object obj) {
		//assuming we're receiving a String[]
		String[] strings = (String[]) obj;
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int i = 0; i < strings.length; i++) {
			if(i != 0)
				sb.append(",");
			sb.append("'").append(strings[i]).append("'");
		}
		return sb.append("]").toString();
		//return sb.toString();
	}
	
}