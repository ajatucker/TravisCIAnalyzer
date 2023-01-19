package com.TravisCIClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.kohsuke.github.GHBlob;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GitHub;

import com.build.commitanalyzer.CommitAnalyzingUtils;
import com.config.Config;
import com.travisdiff.TravisCIChangeBlocks;
import com.travisdiff.TravisCommitInfo;
import com.travisdiff.TravisCommits;
import com.utility.ProjectPropertyAnalyzer;

import edu.util.fileprocess.CVSReader;

public class TravisCIFileDownloader {
	private GitHub github;

	public TravisCIFileDownloader() {
		try {
			github = GitHub.connectUsingPassword(Config.gitHubUserName, Config.gitHubPwd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void downloadTravisFiles(String repourl, String failcmt, String passcmt) {
		GHRepository repo;
		String reponame = ProjectPropertyAnalyzer.getProjRepoName(repourl);
		String localfolder = ProjectPropertyAnalyzer.getProjName(repourl);
		String failcmtstr = null;
		String passcmtstr = null;
		CommitAnalyzingUtils commitAnalyzingUtils = new CommitAnalyzingUtils();
		try {
			repo = github.getRepository(reponame);

			GHCommit failcommit = repo.getCommit(failcmt);
			GHTree failtree = failcommit.getTree();
			List<GHTreeEntry> failghentry = failtree.getTree();

			for (GHTreeEntry item : failghentry) {
				if (item.getPath().contains("travis.yml")) {
					GHBlob gblob = item.asBlob();
					InputStream ist = gblob.read();
					failcmtstr = new String(ist.readAllBytes(), StandardCharsets.UTF_8);
					break;
				}
			}

			GHCommit passcommit = repo.getCommit(passcmt);
			GHTree passtree = passcommit.getTree();
			List<GHTreeEntry> passghentry = passtree.getTree();

			for (GHTreeEntry item : passghentry) {
				if (item.getPath().contains("travis.yml")) {
					GHBlob gblob = item.asBlob();
					InputStream ist = gblob.read();
					passcmtstr = new String(ist.readAllBytes(), StandardCharsets.UTF_8);
					break;
				}
			}

			String localrepo = Config.travisRepoDir + localfolder;
			String strfailfile = localrepo + "/" + failcmt + ".yml";
			String strpassfile = localrepo + "/" + passcmt + ".yml";
			
			File f1=new File(strfailfile);
			File f2=new File(strpassfile);
			
		

			if(!f1.exists())
				f1 = commitAnalyzingUtils.writeContentInFile(strfailfile, failcmtstr);
			
			if(!f2.exists())
				f2 = commitAnalyzingUtils.writeContentInFile(strpassfile, passcmtstr);

			if (f1!=null && f2!=null && f1.exists() && f2.exists()) {
				System.out.println(repourl + "==>" + failcmt + "==>" + passcmt + "==>" + "Done");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void downloadPrevAndCurrTravisFiles(String repourl,String prevcmt, String passcmt) {
		GHRepository repo;
		//String prevcmt = "prev_travis_"+passcmt;
		//String reponame = ProjectPropertyAnalyzer.getProjRepoName(repourl);
		//String localfolder = ProjectPropertyAnalyzer.getProjName(repourl);
		String passcmtstr = null;
		String prevcmtstr = null;
		CommitAnalyzingUtils commitAnalyzingUtils = new CommitAnalyzingUtils();
		try {
			String localrepo = Config.rootDir + repourl; 
			String localfolder = Config.travisRepoDir + repourl;
			//localfolder = localfolder.replace('/', '\\');
			//this one gets repo name as input
			repo = github.getRepository(repourl); //getting lost here??

			GHCommit passcommit = repo.getCommit(passcmt);
			

			//String localrepo = Config.rootDir + repourl;
			
			  String strprevfile = localrepo + "/" + prevcmt + ".yml"; 
			  List<GHCommit> prevcommit = passcommit.getParents();
			  //if(prevcommit != null) { 
			  GHTree prevtree = prevcommit.get(0).getTree();
			  List<GHTreeEntry> prevghentry =prevtree.getTree();
			  
			  for (GHTreeEntry item : prevghentry) {
				  if(item.getPath().contains("travis.yml")) 
				  { 
					  GHBlob pgblob = item.asBlob();
					  InputStream prist = pgblob.read(); 
					  prevcmtstr = new String(prist.readAllBytes(),StandardCharsets.UTF_8); 
					  break; 
					  } 
			} 
			 			
			
			GHTree passtree = passcommit.getTree();
			List<GHTreeEntry> passghentry = passtree.getTree();
			//GHTreeEntry prevItem = null;
			for (GHTreeEntry item : passghentry) 
			{
				if (item.getPath().contains("travis.yml")) 
				{
					GHBlob gblob = item.asBlob();
					InputStream ist = gblob.read();
					passcmtstr = new String(ist.readAllBytes(), StandardCharsets.UTF_8);
					break;
				}
			}
			
			
			
		
			
			
			 

			//String localrepo = Config.rootDir + repourl;
			//String strprevfile = localrepo + "/" + prevcmt + ".yml";
			String strpassfile = localrepo + "/" + passcmt + ".yml";
			
			File f1=new File(strprevfile);
			File f2=new File(strpassfile);
			
			
			  if(!f1.exists()) 
				  f1 =commitAnalyzingUtils.writeContentInFile(strprevfile,prevcmtstr);
			 
			 
			
			if(!f2.exists())
				f2 = commitAnalyzingUtils.writeContentInFile(strpassfile, passcmtstr);

			
			
			
			  if (f1!=null && f2!=null && f1.exists() && f2.exists()) {
			  System.out.println(repourl + "==>" + prevcmt + "==>" + passcmt + "==>" +
			  "Done"); }
			 
			/*
			 * if ( f2!=null && f2.exists()) { System.out.println(repourl + "==>" + prevcmt
			 * + "==>" + passcmt + "==>" + "Done"); }
			 */
			else
			{
				System.out.println("Uh oh");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void downloadBeforeAndAfterCommitFiles() {
		List<TravisCommits> cmtlist = null;
		CVSReader csvreader = new CVSReader();
		try {
			cmtlist = csvreader.loadTravisCommits(Config.csvCITransitionFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int index = 1;
		for (TravisCommits item : cmtlist) {
			//String status = item.getMainProblem();

			//if (!status.toLowerCase().equals("unknow")) {

				//String repourl = item.getRepoUrl();
				//String localfolder = repourl;//ProjectPropertyAnalyzer.getProjName(repourl);
				//String reponame=ProjectPropertyAnalyzer.getProjRepoName(repourl);

				String localrepo = Config.rootDir + item.getRepo();

				File f = new File(localrepo);

				if (!f.exists()) {
					f.mkdirs();
				}

				//String strprevfile = localrepo + "/" + "prev_travis_"+item.getCommit() + ".yml";
				String strpassfile = localrepo + "/" + item.getCommit() + ".yml";

				//File ffail = new File(strprevfile);
				File fpass = new File(strpassfile);

				if (!fpass.exists()) {
					downloadPrevAndCurrTravisFiles(item.getRepo(),"prev_travis_"+item.getCommit(), item.getCommit());
				}
			//}

			System.out.println("Index:" + index++);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void downloadTraviCIConfigFiles() {
		List<TravisCommitInfo> cmtlist = null;
		CVSReader csvreader = new CVSReader();
		try {
			cmtlist = csvreader.loadTravisCommitInfo(Config.csvCITransitionFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int index = 1;
		for (TravisCommitInfo item : cmtlist) {
			String status = item.getMainProblem();

			if (!status.toLowerCase().equals("unknow")) {

				String repourl = item.getRepoUrl();
				//build localFolder from repoUrl
				String localfolder = ProjectPropertyAnalyzer.getProjName(repourl);
				// String reponame=ProjectPropertyAnalyzer.getProjRepoName(repourl);

				String localrepo = Config.travisRepoDir + localfolder;

				File f = new File(localrepo);

				if (!f.exists()) {
					f.mkdirs();
				}

				String strfailfile = localrepo + "/" + item.getFailCommit() + ".yml";
				String strpassfile = localrepo + "/" + item.getPassCommit() + ".yml";

				File ffail = new File(strfailfile);
				File fpass = new File(strpassfile);

				if (!ffail.exists() || !fpass.exists()) {
					downloadTravisFiles(item.getRepoUrl(), item.getFailCommit(), item.getPassCommit());
				}
			}

			System.out.println("Index:" + index++);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
