package com.TravisCIClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
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
	
	private String getFileContents(Repository repo, String cmt, boolean isParent) throws MissingObjectException, IncorrectObjectTypeException, IOException
	{
		String cmtstr = null;
		RevWalk walk = new RevWalk(repo);
		RevCommit commit;
		ObjectId commitId = ObjectId.fromString(cmt);
		if(isParent == false)
		{
			commit = walk.parseCommit(commitId);
		}
		else
		{
			RevCommit precommit = walk.parseCommit(commitId);
			commit = walk.parseCommit(precommit.getParent(0));
		}
		//ObjectId commitId = ObjectId.fromString(cmt);
		//RevCommit commit = walk.parseCommit(commitId);
		//commit.
		//@SuppressWarnings("resource")
		TreeWalk treeWalk = new TreeWalk(repo);
		treeWalk.addTree(commit.getTree());
		treeWalk.setRecursive(false);
		while (treeWalk.next()) {
		    if (treeWalk.isSubtree()) {
		        //System.out.println("dir: " + treeWalk.getPathString());
		        treeWalk.enterSubtree();
		    } else {
		    	if(treeWalk.getPathString().contains("travis.yml"))
		    	{
		    		System.out.println("found!");
		    		ObjectReader read = repo.newObjectReader();
		    		ObjectLoader load = read.open(treeWalk.getObjectId(0));
		    		cmtstr = new String(load.getBytes(), StandardCharsets.UTF_8);
		    		break;
		    	}
		        //System.out.println("file: " + treeWalk.getPathString());
		    }
		}
		
		return cmtstr;
	}
	
	private void downloadPrevAndCurrTravisFiles(String repourl, String passcmt) {
		Git git;
		String prevcmt = "prev"+passcmt;
		String passcmtstr = null;
		String prevcmtstr = null;
		CommitAnalyzingUtils commitAnalyzingUtils = new CommitAnalyzingUtils();
		try {
			String gitPath = Config.travisRepoDir+repourl+"/.git";
			String localfolder = Config.rootDir + "temp";
			//this one gets repo name as input
			git = Git.open(new File(gitPath));
			Repository repo = git.getRepository();
			
			passcmtstr = getFileContents(repo, passcmt, false);
			
			ObjectId currCommitId = ObjectId.fromString(passcmt);
			RevWalk currWalk = new RevWalk(repo);
			RevCommit currCommit = currWalk.parseCommit(currCommitId);
			if(currCommit.getParent(0).getTree() == null)
			{
				prevcmtstr = new String("placeholder");
			}
			else
			{
				prevcmtstr = getFileContents(repo, passcmt, true);
			}
			
			/*
			 * RevWalk walk = new RevWalk(repo); ObjectId commitId =
			 * ObjectId.fromString(passcmt); RevCommit commit = walk.parseCommit(commitId);
			 * //commit. //@SuppressWarnings("resource") TreeWalk treeWalk = new
			 * TreeWalk(repo); treeWalk.addTree(commit.getTree());
			 * treeWalk.setRecursive(false); while (treeWalk.next()) { if
			 * (treeWalk.isSubtree()) { //System.out.println("dir: " +
			 * treeWalk.getPathString()); treeWalk.enterSubtree(); } else {
			 * if(treeWalk.getPathString().contains("travis.yml")) {
			 * System.out.println("found!"); ObjectReader read = repo.newObjectReader();
			 * ObjectLoader load = read.open(treeWalk.getObjectId(0)); passcmtstr = new
			 * String(load.getBytes(), StandardCharsets.UTF_8); }
			 * //System.out.println("file: " + treeWalk.getPathString()); } }
			 */
			//GHCommit passcommit = repo.getCommit(passcmt);
			
			
				/*
				 * List<GHCommit> prevcommit = passcommit.getParents(); //if(prevcommit != null)
				 * { GHTree prevtree = prevcommit.get(0).getTree(); List<GHTreeEntry>
				 * prevghentry =prevtree.getTree();
				 * 
				 * for (GHTreeEntry item : prevghentry) {
				 * if(item.getPath().contains("travis.yml")) { GHBlob pgblob = item.asBlob();
				 * InputStream prist = pgblob.read(); prevcmtstr = new
				 * String(prist.readAllBytes(),StandardCharsets.UTF_8); break; } }
				 */
			 			
			
			/*
			 * GHTree passtree = passcommit.getTree(); List<GHTreeEntry> passghentry =
			 * passtree.getTree(); //GHTreeEntry prevItem = null; for (GHTreeEntry item :
			 * passghentry) { if (item.getPath().contains("travis.yml")) { GHBlob gblob =
			 * item.asBlob(); InputStream ist = gblob.read(); passcmtstr = new
			 * String(ist.readAllBytes(), StandardCharsets.UTF_8); break; } }
			 */
			
			String strprevfile = localfolder + "/" + prevcmt + ".yml"; 
			String strpassfile = localfolder + "/" + passcmt + ".yml";
			
			File f1=new File(strprevfile);
			File f2=new File(strpassfile);
			
			
			if(!f1.exists()) 
			{				 
				f1 = commitAnalyzingUtils.writeContentInFile(strprevfile, prevcmtstr);
			}			
			if(!f2.exists())
			{				
				f2 = commitAnalyzingUtils.writeContentInFile(strpassfile, passcmtstr);
			}		
			if (f1!=null && f2!=null && f1.exists() && f2.exists()) 
			{
			  System.out.println(repourl + "==>" + prevcmt + "==>" + passcmt + "==>" + "Done"); 
			}
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

				String tempfolder = Config.rootDir + "temp";//item.getRepo();

				File f = new File(tempfolder);

				if (!f.exists()) {
					f.mkdirs();
				}

				String strprevfile = tempfolder + "/" + "prev"+item.getCommit() + ".yml";
				//System.out.println("Created "+strprevfile);
				String strpassfile = tempfolder + "/" + item.getCommit() + ".yml";
				//System.out.println("Created "+strpassfile);

				File ffail = new File(strprevfile);
				File fpass = new File(strpassfile);

				if (!ffail.exists() && !fpass.exists()) {
					downloadPrevAndCurrTravisFiles(item.getRepo(), item.getCommit());
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
