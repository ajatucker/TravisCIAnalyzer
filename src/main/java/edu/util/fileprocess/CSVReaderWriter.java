package edu.util.fileprocess;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.build.commitanalyzer.MLCommitDiffInfo;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.CsvToBeanFilter;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.travisdiff.FailFixMapping;
import com.travisdiff.TravisCommitInfo;
import com.unity.entity.CommandType;
import com.unity.entity.PerfFixData;

public class CSVReaderWriter {

	public void writeListBean(List<PerfFixData> fixdata, String csvfilepath)
			throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

		try {

			// Creating writer class to generate
			// csv file
			FileWriter writer = new FileWriter(csvfilepath);

			// // Create Mapping Strategy to arrange the
			// // column name in order
			// ColumnPositionMappingStrategy mappingStrategy=
			// new ColumnPositionMappingStrategy();
			// mappingStrategy.setType(PerfFixData.class);
			//
			// // Arrange column name as provided in below array.
			// String[] columns = new String[]
			// { "projName","projGitUrl","fixCommitID", "fixCommitMsg",
			// "patchPath","srcFileChangeCount","assetChangeCount" };
			// mappingStrategy.setColumnMapping(columns);
			//
			//
			// // Createing StatefulBeanToCsv object
			// StatefulBeanToCsvBuilder<PerfFixData> builder=
			// new StatefulBeanToCsvBuilder(writer);
			// StatefulBeanToCsv beanWriter =
			// builder.withMappingStrategy(mappingStrategy).build();
			//
			// // Write list to StatefulBeanToCsv object
			// beanWriter.write(fixdata);

			// // closing the writer object
			// writer.close();

			StatefulBeanToCsvBuilder<PerfFixData> builder = new StatefulBeanToCsvBuilder<PerfFixData>(writer);
			StatefulBeanToCsv<PerfFixData> beanWriter = builder.build();

			beanWriter.write(fixdata);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void writeFailFixMappingListBean(List<FailFixMapping> listdata, String csvfilepath)
			throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

		try {

			// Creating writer class to generate
			// csv file
			FileWriter writer = new FileWriter(csvfilepath);

			// // Create Mapping Strategy to arrange the
			// // column name in order
			// ColumnPositionMappingStrategy mappingStrategy=
			// new ColumnPositionMappingStrategy();
			// mappingStrategy.setType(PerfFixData.class);
			//
			// // Arrange column name as provided in below array.
			// String[] columns = new String[]
			// { "projName","projGitUrl","fixCommitID", "fixCommitMsg",
			// "patchPath","srcFileChangeCount","assetChangeCount" };
			// mappingStrategy.setColumnMapping(columns);
			//
			//
			// // Createing StatefulBeanToCsv object
			// StatefulBeanToCsvBuilder<PerfFixData> builder=
			// new StatefulBeanToCsvBuilder(writer);
			// StatefulBeanToCsv beanWriter =
			// builder.withMappingStrategy(mappingStrategy).build();
			//
			// // Write list to StatefulBeanToCsv object
			// beanWriter.write(fixdata);

			// // closing the writer object
			// writer.close();

			StatefulBeanToCsvBuilder<FailFixMapping> builder = new StatefulBeanToCsvBuilder<FailFixMapping>(writer);
			StatefulBeanToCsv<FailFixMapping> beanWriter = builder.build();

			beanWriter.write(listdata);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public <T> void writeListBean(List<T> fixdata, String csvfilepath,Class neededClass)
			throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

		try {

			// Creating writer class to generate
			// csv file
			FileWriter writer = new FileWriter(csvfilepath);

			// // Create Mapping Strategy to arrange the
			// // column name in order
			// ColumnPositionMappingStrategy mappingStrategy=
			// new ColumnPositionMappingStrategy();
			// mappingStrategy.setType(PerfFixData.class);
			//
			// // Arrange column name as provided in below array.
			// String[] columns = new String[]
			// { "projName","projGitUrl","fixCommitID", "fixCommitMsg",
			// "patchPath","srcFileChangeCount","assetChangeCount" };
			// mappingStrategy.setColumnMapping(columns);
			//
			//
			// // Createing StatefulBeanToCsv object
			// StatefulBeanToCsvBuilder<PerfFixData> builder=
			// new StatefulBeanToCsvBuilder(writer);
			// StatefulBeanToCsv beanWriter =
			// builder.withMappingStrategy(mappingStrategy).build();
			//
			// // Write list to StatefulBeanToCsv object
			// beanWriter.write(fixdata);

			// // closing the writer object
			// writer.close();

			StatefulBeanToCsvBuilder<T> builder = new StatefulBeanToCsvBuilder<T>(writer);
			StatefulBeanToCsv<T> beanWriter = builder.build();

			beanWriter.write(fixdata);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	


	public List<PerfFixData> getListBeanFromCSV(String strpath) throws Exception {

		List<PerfFixData> data = null;

		Path path = Paths.get(strpath);

		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

			HeaderColumnNameMappingStrategy<PerfFixData> strategy = new HeaderColumnNameMappingStrategy<>();
			strategy.setType(PerfFixData.class);

			CsvToBean<PerfFixData> csvToBean = new CsvToBeanBuilder<PerfFixData>(br).withType(PerfFixData.class).withMappingStrategy(strategy)
					.withIgnoreLeadingWhiteSpace(true).build();

			data = csvToBean.parse();

		}

		return data;
	}
	
	public List<CommandType> getListCmdTypeFromCSV(String strpath) throws Exception {

		List<CommandType> data = null;

		Path path = Paths.get(strpath);

		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

			HeaderColumnNameMappingStrategy<CommandType> strategy = new HeaderColumnNameMappingStrategy<>();
			strategy.setType(CommandType.class);

			CsvToBean<CommandType> csvToBean = new CsvToBeanBuilder<CommandType>(br).withType(CommandType.class).withMappingStrategy(strategy)
					.withIgnoreLeadingWhiteSpace(true).build();

			data = csvToBean.parse();

		}

		return data;
	}
	
	public List<MLCommitDiffInfo> getMLCommitDiffInfoFromCSV(String strpath) throws Exception {

		List<MLCommitDiffInfo> data = null;

		Path path = Paths.get(strpath);

		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

			HeaderColumnNameMappingStrategy<MLCommitDiffInfo> strategy = new HeaderColumnNameMappingStrategy<>();

			strategy.setType(MLCommitDiffInfo.class);
			
			CsvToBean<MLCommitDiffInfo> csvToBean = new CsvToBeanBuilder<MLCommitDiffInfo>(br).withType(MLCommitDiffInfo.class)
					.withMappingStrategy(strategy).withIgnoreLeadingWhiteSpace(true).withFilter(new CsvToBeanFilter() {
						@Override
						public boolean allowLine(String[] line) {
							for (String one : line) {
				                if (one != null && one.length() > 0) {
				                    return true;
				                }
				            }
				            return false;
						}
						
					}).build();

			data = csvToBean.parse();

		}

		return data;
	}
	
	/**Input data is not actually processed here, only used to extract header for mapping strategy.<br><br>
	 * When reading the outputted data, the quote character escaped with another quote. This is a standard for CSV, though Excel seems to not support it.*/
	public void writeMLDiffBeanToFile(List<MLCommitDiffInfo> fixdata, String outputDataPath)
			throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

		try {
			// Creating writer class to generate
			// csv file
			FileWriter writer = new FileWriter(outputDataPath);

			
			SetOrderHeaderMappingStrategy<MLCommitDiffInfo> strategy = new SetOrderHeaderMappingStrategy<>(MLCommitDiffInfo.class);
			System.out.println(strategy.generateHeader());
			
			StatefulBeanToCsvBuilder<MLCommitDiffInfo> builder = new StatefulBeanToCsvBuilder<MLCommitDiffInfo>(writer).withEscapechar('"');
			StatefulBeanToCsv<MLCommitDiffInfo> beanWriter = builder.withMappingStrategy(strategy).build();

			beanWriter.write(fixdata);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public <T> void writeBeanToFile(List<T> fixdata, String csvfilepath)
			throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

		try {

			// Creating writer class to generate
			// csv file
			FileWriter writer = new FileWriter(csvfilepath);

			// // Create Mapping Strategy to arrange the
			// // column name in order
			// ColumnPositionMappingStrategy mappingStrategy=
			// new ColumnPositionMappingStrategy();
			// mappingStrategy.setType(PerfFixData.class);
			//
			// // Arrange column name as provided in below array.
			// String[] columns = new String[]
			// { "projName","projGitUrl","fixCommitID", "fixCommitMsg",
			// "patchPath","srcFileChangeCount","assetChangeCount" };
			// mappingStrategy.setColumnMapping(columns);
			//
			//
			// // Createing StatefulBeanToCsv object
			// StatefulBeanToCsvBuilder<PerfFixData> builder=
			// new StatefulBeanToCsvBuilder(writer);
			// StatefulBeanToCsv beanWriter =
			// builder.withMappingStrategy(mappingStrategy).build();
			//
			// // Write list to StatefulBeanToCsv object
			// beanWriter.write(fixdata);

			// // closing the writer object
			// writer.close();

			StatefulBeanToCsvBuilder<T> builder = new StatefulBeanToCsvBuilder<T>(writer);
			StatefulBeanToCsv<T> beanWriter = builder.build();

			beanWriter.write(fixdata);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	
	public <T> List<T> getListBeanFromCSV(String strpath,Class neededClass) throws Exception {

		List<T> data = null;

		Path path = Paths.get(strpath);

		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

			HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
			strategy.setType(neededClass);

			CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(br).withType(neededClass).withMappingStrategy(strategy)
					.withIgnoreLeadingWhiteSpace(true).build();

			data = csvToBean.parse();

		}

		return data;
	}
	
	
	
	/***********************For TraviCI Related Read Write****************************************/
	public List<TravisCommitInfo> getTravisCommitInfoBeanFromCSV(String strpath) throws Exception {

		List<TravisCommitInfo> data = null;

		Path path = Paths.get(strpath);

		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

			HeaderColumnNameMappingStrategy<TravisCommitInfo> strategy = new HeaderColumnNameMappingStrategy<>();
			strategy.setType(TravisCommitInfo.class);

			CsvToBean<TravisCommitInfo> csvToBean = new CsvToBeanBuilder<TravisCommitInfo>(br).withType(TravisCommitInfo.class).withMappingStrategy(strategy)
					.withIgnoreLeadingWhiteSpace(true).build();

			data = csvToBean.parse();

		}

		return data;
	}
	
	
	/********************************************************************************************/
	
	
}
