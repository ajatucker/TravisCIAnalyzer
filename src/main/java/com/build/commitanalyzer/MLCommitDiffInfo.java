package com.build.commitanalyzer;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

public class MLCommitDiffInfo {
	
	@CsvBindByName(column = "GitAuthor", required = true)
	private String gitAuthor;
	@CsvBindByName(column = "ProjectName", required = true)
	private String projName;
	@CsvBindByName(column = "CommitID", required = true)
	private String commitID;
	@CsvBindByName(column = "CommitMessage", required = true)
	private String commitMessage;
	@CsvCustomBindByName(column = "Lsof ModifiedFiles", required = true, converter = ListConverter.class)
	private String[] modifiedFiles;
	@CsvBindByName(column = "LinesAdded", required = false)
	private int linesAdded;
	@CsvBindByName(column = "LinesRemoved", required = false)
	private int linesRemoved;
	@CsvBindByName(column = "LinesModified", required = false)
	private int linesModified;
	
	public String getGitAuthor() {
		return gitAuthor;
	}
	
	public void setGitAuthor(String gitAuthor) {
		this.gitAuthor = gitAuthor;
	}

	public String getProjName() {
		return projName;
	}

	public void setProjName(String projName) {
		this.projName = projName;
	}

	public String getCommitID() {
		return commitID;
	}

	public void setCommitID(String commitID) {
		this.commitID = commitID;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public String[] getModifiedFiles() {
		return modifiedFiles;
	}

	public void setModifiedFiles(String[] modifiedFiles) {
		this.modifiedFiles = modifiedFiles;
	}

	public int getLinesAdded() {
		return linesAdded;
	}

	public void setLinesAdded(int linesAdded) {
		this.linesAdded = linesAdded;
	}

	public int getLinesRemoved() {
		return linesRemoved;
	}

	public void setLinesRemoved(int linesRemoved) {
		this.linesRemoved = linesRemoved;
	}

	public int getLinesModified() {
		return linesModified;
	}

	public void setLinesModified(int linesModified) {
		this.linesModified = linesModified;
	}
	
}
