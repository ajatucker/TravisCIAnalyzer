package com.travisdiff;

import com.opencsv.bean.CsvBindByName;


//This class is different from the TravisCommitInfo class as that is for a different data set
//The TravisCommits class is for generating the report of how Travis CI/CD pipelines are evolving
public class TravisCommits {
	@CsvBindByName(column = "CommitID", required = true)	
	private String commitid;
	
	@CsvBindByName(column = "CommitMessage", required = true)	
	private String commitmessage;
	
	@CsvBindByName(column = "GitAuthor", required = true)	
	private String author;
	
	@CsvBindByName(column = "Lsof ModifiedFiles", required = true)
	private String  modifiedfiles;	
	
	
	@CsvBindByName(column = "ProjectName", required = true)
	private String  repo;
	
	
	public TravisCommits(String repo, String id, String message, String name, String files)
	{
		this.repo=repo;
		this.commitid=id;
		this.commitmessage=message;
		this.author=name;
		this.modifiedfiles=files;
	}

	public String getRepo() {
		return repo;
	}

	public void setRepo(String repoUrl) {
		this.repo = repoUrl;
	}

	public String getCommit() {
		return commitid;
	}
	
	public String setCommit(String id) {
		return commitid=id;
	}
	
	public String getAuthor() {
		return author;
	}

	public void setAuthor(String name) {
		this.author = name;
	}
	
	public String getCommitMessage() {
		return commitmessage;
	}
	
	public String setCommitMessage(String m) {
		return commitmessage=m;
	}
	
	public String getModifiedFiles() {
		return modifiedfiles;
	}

	public void setModifiedFiles(String files) {
		this.modifiedfiles = files;
	}

	


}
