package com.build.commitanalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.assimbly.docconverter.DocConverter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.AndRevFilter;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.MaxCountRevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import com.build.analyzer.entity.CommitChange;
import com.config.Config;
import com.csharp.diff.CSharpDiffGenerator;
import com.github.gumtreediff.actions.EditScript;

//import edu.utsa.data.DataResultsHolder;
//import edu.utsa.data.DataStatsHolder;
//import edu.utsa.main.MainClass;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.SyntaxException;
import com.python.parser.PythonFileParser;
import com.travis.parser.TravisYamlFileParser;
import com.travis.parser.TravisYamlFileParser.EditResults;
import com.travisdiff.TravisCIDiffGenerator;
import com.unity.callgraph.ClassFunction;
import com.unity.entity.PerfFixData;

import edu.util.fileprocess.TextFileReaderWriter;

/**
 * 
 * @author 
 *
 * 
 */

public class CommitAnalyzer {

	/** Various methods encapsulating methods to treats Git and commits datas */
	private CommitAnalyzingUtils commitAnalyzingUtils;

	/** All the statistical datas (number of faulty commit, actions, etc) */
	private DataStatsHolder statsHolder;

	/** File managing object for tables */
	private DataResultsHolder resultsHolder;

	/** Name of the project */
	private String project;

	/** Owner of the project (necessary for Markdown parsing) */
	private String projectOwner;

	/** Path to the directory */
	private String directoryPath;

	/** Repository object, representing the directory */
	private Repository repository;

	/** Git entity to treat with the Repository data */
	private Git git;

	/** Revision walker from JGit */
	private RevWalk rw;

	private CommitChange commitChangeTracker;

	private String gradleChanges;

	private String gitUrl;

	/** Classic constructor */
	public CommitAnalyzer(String projectOwner, String project) throws Exception {
		this.projectOwner = projectOwner;
		this.project = project;

		directoryPath = Config.repoDir + project + "\\.git";

		commitAnalyzingUtils = new CommitAnalyzingUtils();
		statsHolder = new DataStatsHolder();
		repository = commitAnalyzingUtils.setRepository(directoryPath);
		git = new Git(repository);
		rw = new RevWalk(repository);
		this.commitChangeTracker = new CommitChange();
		this.gradleChanges = "";
	}

	/** Classic constructor */
	public CommitAnalyzer(String projectOwner, String project, String giturl) throws Exception {
		this.projectOwner = projectOwner;
		this.project = project;

		directoryPath = Config.repoDir + project + "\\.git";
		System.out.println(directoryPath);
		commitAnalyzingUtils = new CommitAnalyzingUtils();
		statsHolder = new DataStatsHolder();
		repository = commitAnalyzingUtils.setRepository(directoryPath);
		git = new Git(repository);
		rw = new RevWalk(repository);
		this.commitChangeTracker = new CommitChange();
		this.gradleChanges = "";
		this.gitUrl = giturl;
	}
	
	/**Just for testing purposes*/
	public CommitAnalyzer(String projectOwner, String project, File projectLocation) throws Exception{
		this.projectOwner = projectOwner;
		this.project = project;
		directoryPath = projectLocation.getAbsolutePath();
		commitAnalyzingUtils = new CommitAnalyzingUtils();
		statsHolder = new DataStatsHolder();
		repository = commitAnalyzingUtils.setRepository(directoryPath);
		git = new Git(repository);
		rw = new RevWalk(repository);
		this.commitChangeTracker = new CommitChange();
		this.gradleChanges = "";
	}

	public CommitChange getCommitChangeTracker() {
		return commitChangeTracker;
	}

	public void commitSampleTry(String ID) {
		List<Action> totalactions = new ArrayList<Action>();
		List<Action> act = new ArrayList<Action>();
		List<String> debugging = new ArrayList<String>();
		String r = "";
		// File debug = new File("debug-" + ID + ".txt");

		try {
			ObjectId objectid = repository.resolve(ID);

			if (objectid == null)
				return;

			RevCommit commit = rw.parseCommit(objectid);

			// System.out.println(commit.getFullMessage());

			if (commit.getParentCount() > 0) {
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());

				DiffFormatter df = commitAnalyzingUtils.setDiffFormatter(repository, true);

				List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());

				for (DiffEntry diff : diffs) {
					if (diff.getNewPath().contains("build.gradle")) {

						commitChangeTracker.setBuildFileChange(commitChangeTracker.getBuildFileChange() + 1);

					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	public List<PerfFixData> getAllPerformanceCommits()
			throws MissingObjectException, IncorrectObjectTypeException, IOException {
		List<PerfFixData> perffixdata = new ArrayList<>();

		Collection<Ref> allRefs = repository.getAllRefs().values();

		// a RevWalk allows to walk over commits based on some filtering that is defined
		try (RevWalk revWalk = new RevWalk(repository)) {
			for (Ref ref : allRefs) {
				revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
			}
			// System.out.println("Walking all commits starting with " + allRefs.size() + "
			// refs: " + allRefs);
			int count = 0;
			for (RevCommit commit : revWalk) {
				System.out.println("Commit: " + commit);
				count++;

				String commitmsg = commit.getFullMessage().toLowerCase();
				commitmsg = commitmsg.replaceAll(",", " cma ");
				commitmsg = commitmsg.replaceAll("\"", " quote ");

				if (isPerformanceCommit(commitmsg) && !commitmsg.contains("merge")) {
					String commitid = commit.getName();
					PerfFixData fixdata = new PerfFixData(this.project, this.getGitUrl(), commitid);
					fixdata.setFixCommitMsg(commitmsg);
					fixdata.setPatchPath("");
					fixdata.setAssetChangeCount(0);
					fixdata.setSrcFileChangeCount(0);
					perffixdata.add(fixdata);
				}

			}
			// System.out.println("Had " + count + " commits");
			// System.out.println(this.project);
		}

		return perffixdata;
	}

	private boolean isPerformanceCommit(String commitmsg) {
		for (String token : Config.perfCommitToken) {
			if (commitmsg.contains(token)) {
				return true;
			}
		}

		return false;
	}

	public String getGitUrl() {
		return gitUrl;
	}

	public void setGitUrl(String gitUrl) {
		this.gitUrl = gitUrl;
	}

	public List<EditScript> extractCSharpFileChange(String ID) {
		// File debug = new File("debug-" + ID + ".txt");
		List<EditScript> actionslist = new ArrayList<>();

		try {
			ObjectId objectid = repository.resolve(ID);

			if (objectid == null)
				return null;

			RevCommit commit = rw.parseCommit(objectid);

			/// System.out.println(commit.getFullMessage());

			if (commit.getParentCount() > 0) {
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
				DiffFormatter df = commitAnalyzingUtils.setDiffFormatter(repository, true);

				List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());

				for (DiffEntry diff : diffs) {

					if (diff.getNewPath().endsWith(".cs")) {

						String currentContent = getFileContentAtCommit(ID, diff);
						String previousContent = getFileContentAtCommit(parent.getName(), diff);

						File f1 = commitAnalyzingUtils.writeContentInFile("g1.cs", currentContent);

						File f2 = commitAnalyzingUtils.writeContentInFile("g2.cs", previousContent);

						CSharpDiffGenerator diffgen = new CSharpDiffGenerator();
						EditScript actions = diffgen.generateDiff(f1, f2);
						if (actions != null) {
							actionslist.add(actions);
						}

						f1.delete();
						f2.delete();

					}
				}

			}
			// gradleChanges = gradlechgmgr.getXMLChange();

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return actionslist;
	}

	public PerfFixData extractFileChangeData(String ID, PerfFixData fixcommit) {
		// File debug = new File("debug-" + ID + ".txt");
		List<EditScript> actionslist = new ArrayList<>();
		int srcfilecount = 0;
		int otherfilecount = 0;

		try {
			ObjectId objectid = repository.resolve(ID);

			if (objectid == null)
				return null;

			RevCommit commit = rw.parseCommit(objectid);

			/// System.out.println(commit.getFullMessage());

			if (commit.getParentCount() > 0) {
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
				DiffFormatter df = commitAnalyzingUtils.setDiffFormatter(repository, true);

				List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());

				for (DiffEntry diff : diffs) {
					String changepath = diff.getNewPath();
					changepath = changepath.toLowerCase();
					if (changepath.endsWith(".cs")) {
						srcfilecount++;
					} else if (!changepath.endsWith(".md") && !changepath.contains("readme")) {
						otherfilecount++;
					}
				}

			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		fixcommit.setAssetChangeCount(otherfilecount);
		fixcommit.setSrcFileChangeCount(srcfilecount);

		return fixcommit;
	}

	public TravisYamlFileParser.EditResults getYamlFileChangeAST(String commitid, String fileName) {
		try (DiffFormatter df = new DiffFormatter(System.out)) {
			ObjectId objectid1 = repository.resolve(commitid);
			if (objectid1 == null) { //invalid id
				return null;
			}
			RevTree newTree = getTree(commitid); //the current file tree
			if(newTree == null) {
				return null;
			}
			RevCommit newCommit = rw.parseCommit(objectid1);
			RevTree oldTree = null;
			if(newCommit.getParentCount() > 0) { //don't try to get parent of initial commit
				RevCommit oldCommit = rw.parseCommit(newCommit.getParent(0).getId());
				if(oldCommit == null) //no parent to compare to
					return null;
				oldTree = oldCommit.getTree(); //the previous commit's file tree
			}
			String newTravisContent, oldTravisContent = "";
			try{
				newTravisContent = this.getStringFile(newTree, fileName);
				//System.out.println("New travis:\n" + newTravisContent + "\n");
				if(oldTree != null)
					oldTravisContent = this.getStringFile(oldTree, fileName);
				//System.out.println("Old travis:\n" + oldTravisContent + "\n");
			}catch(IOException | IllegalStateException e){
				System.out.println("YAML file not found");
				return null;
			}
			
			
			//TODO Fix up yaml to match the more strict format this parser uses
			/* This appears to be considered valid by travis, but not the code:
			 * foo:
			 *     - bar:
			 *     baz:
			 */

			TravisYamlFileParser parser = new TravisYamlFileParser();
			EditResults results;
			try{
				results = parser.getYamlDiff(oldTravisContent, newTravisContent);
			}catch(SyntaxException syn) {
				System.out.println("Yaml doesn't match format!");
				return null;
			}
			return results;
		} catch (RevisionSyntaxException e1) {
			e1.printStackTrace();
		} catch (AmbiguousObjectException e1) {
			e1.printStackTrace();
		} catch (IncorrectObjectTypeException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	
	static final Pattern unicodeFind = Pattern.compile("[^\\p{ASCII}]"); //valid python variable names must match this, and not start with a number. 
	
	static final Path logPath = Path.of(Config.rootDir + "python_errors.log");

	
	//TODO filter better. Currently uses all python files in the commit
	public PythonFileParser.EditResults[] getPythonDiffAST(String commitid) {
		//StringBuilder log = new StringBuilder("Erroring files in commit: ");
		try (DiffFormatter df = new DiffFormatter(System.out)) {
			RevCommit newCommit = null;
			RevTree newTree = null, oldTree = null;
			try {
				Files.writeString(logPath, System.lineSeparator() + projectOwner + "/" + project + ":" + commitid + System.lineSeparator(), StandardOpenOption.APPEND);
				ObjectId objectid1 = repository.resolve(commitid);
				if (objectid1 == null) { //invalid id
					return null;
				}
				newTree = getTree(commitid); //the current file tree
				if(newTree == null) {
					return null;
				}
				newCommit = rw.parseCommit(objectid1);
				oldTree = null;
				if(newCommit.getParentCount() > 0) { //don't try to get parent of initial commit
					RevCommit oldCommit = rw.parseCommit(newCommit.getParent(0).getId());
					if(oldCommit == null) //no parent to compare to
						return null;
					oldTree = oldCommit.getTree(); //the previous commit's file tree
				}
			}catch(IOException e) {
				e.printStackTrace();
				return null;
			}
			if(oldTree == null)
				return null; //Initial commit
			List<PythonFileParser.EditResults> resultList = new LinkedList<>();
			try(ObjectReader reader = git.getRepository().newObjectReader()) {
				AbstractTreeIterator oldTreeIter = null;
				try {
					oldTreeIter = newCommit.getParentCount() > 0 ? new CanonicalTreeParser(null, reader, oldTree.getId()) : new EmptyTreeIterator();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
				df.setReader(reader, new org.eclipse.jgit.lib.Config());
				List<DiffEntry> diffList = null;
				try {
					diffList = git.diff().setOldTree(oldTreeIter)
								.setNewTree(new CanonicalTreeParser(null, reader, newTree.getId())).call();
				} catch (GitAPIException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
				List<String> checkedFiles = new LinkedList<>();
				for(DiffEntry diff : diffList) {
					if(diff.getNewPath().endsWith(".py") || diff.getOldPath().endsWith(".py")) { //TODO filter better?
						String pathName = "";
						try {
							String newVersion = "", oldVersion = "";
							pathName = diff.getOldPath();
							if(checkedFiles.contains(pathName))
								continue; //do not check the same file twice
							checkedFiles.add(pathName);
							if(diff.getChangeType() != ChangeType.DELETE) { //if deleted, file no longer exists and empty string should be used
								try {
									newVersion = getStringFile(newTree, diff.getNewPath());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								pathName = diff.getNewPath();
							}
							System.out.println("Comparing versions of " + pathName);
							if(diff.getChangeType() != ChangeType.ADD) { //if file added, the file didn't exist before and empty string should be used
								try {
									oldVersion = getStringFile(oldTree, diff.getOldPath());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							System.out.println(diff.getChangeType() + ": " + pathName);
							//unicode errors pythonparser so it needs to be removed and replaced with something interpreted similar enough to not change the syntax
							newVersion = unicodeFind.matcher(newVersion).replaceAll("?"); //just replace them with ?
							oldVersion = unicodeFind.matcher(oldVersion).replaceAll("?"); //There are probably better ways but this works
							PythonFileParser parser = new PythonFileParser();
							PythonFileParser.EditResults results = parser.getPythonDiff(oldVersion, newVersion, pathName);
							resultList.add(results);
						}catch(RuntimeException e) {
							System.out.println(pathName);
							try {
								Files.writeString(logPath, pathName + System.lineSeparator(), StandardOpenOption.APPEND);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							if(e instanceof SyntaxException)
								System.out.println("Syntax error");
							//Logging for debug purposes
							e.printStackTrace();
							continue;
						}
					}
				}
				return resultList.toArray(new PythonFileParser.EditResults[resultList.size()]);
			}
		}
	}
	
	/**Returns an int[] containing the lines added, removed, and modified, in that order, between that commit and its first parent, or null if it failed to resolve a commit or errored*/
	public int[] getLoCChange(String commitid) {
		try (DiffFormatter df = new DiffFormatter(System.out)) {
			ObjectId objectid1 = repository.resolve(commitid);
			if (objectid1 == null) { //invalid id
				return null;
			}
			RevTree newTree = getTree(commitid); //the current file tree
			if(newTree == null) {
				return null;
			}
			RevCommit newCommit = rw.parseCommit(objectid1);
			RevTree oldTree = null;
			if(newCommit.getParentCount() > 0) { //don't try to get parent of initial commit
				RevCommit oldCommit = rw.parseCommit(newCommit.getParent(0).getId());
				if(oldCommit == null) //no parent to compare to
					return null;
				oldTree = oldCommit.getTree(); //the previous commit's file tree
			}
			int totalAdded = 0, totalRemoved = 0, totalModified = 0;
			
			try(ObjectReader reader = git.getRepository().newObjectReader()) {
				//newCommit.getParentCount() == 0 means this is the initial commit, so the old commit has no tree
				AbstractTreeIterator oldTreeIter = newCommit.getParentCount() > 0 ? new CanonicalTreeParser(null, reader, oldTree.getId()) : new EmptyTreeIterator();
				df.setReader(reader, new org.eclipse.jgit.lib.Config());
				List<DiffEntry> diffList = git.diff().setOldTree(oldTreeIter)
							.setNewTree(new CanonicalTreeParser(null, reader, newTree.getId())).call();
				for(DiffEntry diff : diffList) {
					if(diff.getNewPath().contains(".travis.yml")) {
						for (Edit edit : df.toFileHeader(diff).toEditList()) {
							System.out.println(diff.getNewPath());
							int removed = edit.getEndA() - edit.getBeginA();
							System.out.println("Removed: " + removed);
							totalModified += removed;
							totalRemoved += removed;
							int added = edit.getEndB() - edit.getBeginB();
							System.out.println("Added: " + added);
							totalModified += added;
							totalAdded += added;
						}
					}
				}
			}
			return new int[] {totalAdded, totalRemoved, totalModified};
		}catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	public String getFileContentAtCommit(String commitid, DiffEntry diff) {
		String content = "";
		try {
			ObjectId objectid1 = repository.resolve(commitid);

			if (objectid1 == null)
				return null;
			
			RevCommit parent = rw.parseCommit(objectid1);

			RevTree tree = getTree(commitid);

			content = getStringFile(tree, diff.getNewPath());

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return content;
	}
	
	public RevTree getTree(String cmtid) throws IOException {
		ObjectId lastCommitId = repository.resolve(cmtid);
		// a RevWalk allows to walk over commits based on some filtering
		try (RevWalk revWalk = new RevWalk(repository)) {
			List<Ref> refs = git.branchList().call();
			for(Ref ref : refs) {
				revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
			}
			RevCommit commit = null;
			try{
				commit = revWalk.parseCommit(lastCommitId);
			}catch(MissingObjectException e) {
				System.out.println("Assuming commit from " + project + " is missing");
				return null;
			}

			// System.out.println("Time of commit (seconds since epoch): " +
			// commit.getCommitTime());

			// and using commit's tree find the path
			RevTree tree = commit.getTree();
			// System.out.println("Having tree: " + tree);
			return tree;
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String getStringFile(RevTree tree, String filter) throws IOException {
		// now try to find a specific file
		try (TreeWalk treeWalk = new TreeWalk(repository)) {

			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);

			treeWalk.setFilter(PathFilter.create(filter));
			if (!treeWalk.next()) {
				throw new IllegalStateException("Did not find expected file:" + filter);
			}

			// FileMode specifies the type of file, FileMode.REGULAR_FILE for
			// normal file, FileMode.EXECUTABLE_FILE for executable bit
			// set
			FileMode fileMode = treeWalk.getFileMode(0);
			ObjectLoader loader = repository.open(treeWalk.getObjectId(0));

			// loader.copyTo(System.out);
			byte[] butestr = loader.getBytes();

			String str = new String(butestr);

			return str;

		}
	}

	public List<ClassFunction> getClassFunctionCall(String commitid) {

		List<ClassFunction> classfunclist = new ArrayList<>();

		try {
			ObjectId objectid = repository.resolve(commitid);
			RevCommit commit = rw.parseCommit(objectid);

			RevTree tree = commit.getTree();

			// TreeWalk treeWalk = new TreeWalk(repository);
			// treeWalk.addTree(tree);
			// treeWalk.setRecursive(false);
			// treeWalk.setPostOrderTraversal(false);

			TreeWalk treeWalk = new TreeWalk(repository);
			treeWalk.addTree(commit.getTree());
			treeWalk.setRecursive(false);

			// treeWalk.setRecursive(true);

			while (treeWalk.next()) {
				// System.out.println("found:" + treeWalk.getPathString());

				if (treeWalk.isSubtree()) {
					// System.out.println("dir: " + treeWalk.getPathString());
					treeWalk.enterSubtree();
				}

				else if (treeWalk.getPathString().endsWith(".cs")) {
					ObjectId objectId = treeWalk.getObjectId(0);
					ObjectLoader loader = repository.open(objectId);

					// and then one can the loader to read the file
					// loader.copyTo(System.out);

					byte[] butestr = loader.getBytes();
					String str = new String(butestr);
					File f1 = commitAnalyzingUtils.writeContentInFile("g1.cs", str);
					CSharpDiffGenerator diffgen = new CSharpDiffGenerator();
					ClassFunction clsfunc = diffgen.getClassFunction(f1);
					classfunclist.add(clsfunc);

					f1.delete();

				}

			}
			treeWalk.reset();

		} catch (Exception ex) {
			System.out.print(ex.getMessage());
		}

		return classfunclist;
	}

	/*********************************
	 * For Travis
	 ***********************************************************/

	public EditScript extractTravisFileChange(String fID, String pID) {
		// File debug = new File("debug-" + ID + ".txt");
		EditScript actions = null;

		try {
			// ObjectId failobjectid = repository.resolve(fID);
			ObjectId passobjectid = repository.resolve(pID);

//			if (failobjectid == null || passobjectid == null)
//				return null;

			if (passobjectid == null)
				return null;

			// RevCommit failcommit = rw.parseCommit(failobjectid);
			RevCommit failcommit = null;
			RevCommit passcommit = null;

			try {
				passcommit = rw.parseCommit(passobjectid);
				if (passcommit.getParentCount() > 0) {
					failcommit = rw.parseCommit(passcommit.getParent(0).getId());
					// DiffFormatter df = commitAnalyzingUtils.setDiffFormatter(repository, true);
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
				// return null;
			}

			if (passcommit == null) {
				try {
					ObjectId failobjectid = repository.resolve(fID);
					failcommit = rw.parseCommit(failobjectid);

					rw.markStart(failcommit);
					PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<>();
					plotCommitList.source(rw);
					plotCommitList.fillTo(Integer.MAX_VALUE);

					for (RevCommit com : rw) {
						passcommit = rw.parseCommit(com.getId());
						if (passcommit != null) {
							break;
						}
					}

				} catch (Exception e) {
					System.out.println(e.getMessage());
					return null;
				}
			}

			// RevCommit commit = rw.parseCommit(objectid);

			/// System.out.println(commit.getFullMessage());

			/// System.out.println(commit.getFullMessage());

			DiffFormatter df = commitAnalyzingUtils.setDiffFormatter(repository, true);

			List<DiffEntry> diffs = df.scan(failcommit.getTree(), passcommit.getTree());

			for (DiffEntry diff : diffs) {

				if (diff.getNewPath().endsWith("travis.yml")) {

					String currentContent = getFileContentAtCommit(pID, diff);
					// String previousContent = getFileContentAtCommit(fID, diff);
					String previousContent = getFileContentAtCommit(failcommit.getName(), diff);

					String tempcurr = Config.patchDir + "j1.json";
					String tempprev = Config.patchDir + "j2.json";

					File f1 = commitAnalyzingUtils.writeContentInFile(tempcurr, currentContent);
					File f2 = commitAnalyzingUtils.writeContentInFile(tempprev, previousContent);

					TravisCIDiffGenerator diffgen = new TravisCIDiffGenerator();
					actions = diffgen.extractTravisFileChange(f2, f1);

					f1.delete();
					f2.delete();

				}
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return actions;
	}

	/*********************************
	 * For Travis with local travis file
	 ***********************************************************/

	public EditScript extractTravisFileChangeV2(String fID, String pID, String projname) {
		// File debug = new File("debug-" + ID + ".txt");
		EditScript actions = null;

		String ymlrepo = Config.travisRepoDir + projname + "/";
		String failfile = ymlrepo + fID + ".yml";
		String passfile = ymlrepo + pID + ".yml";

		File ffailfile = new File(failfile);
		File fpassfile = new File(passfile);

		if (ffailfile.exists() && fpassfile.exists()) {
			try {
				String strfailcontent=TextFileReaderWriter.readFile(ffailfile);
				String strpasscontent=TextFileReaderWriter.readFile(fpassfile);
				String tempcurr = Config.patchDir + "j1.json";
				String tempprev = Config.patchDir + "j2.json";

				File f1 = commitAnalyzingUtils.writeContentInFile(tempcurr, strpasscontent);
				File f2 = commitAnalyzingUtils.writeContentInFile(tempprev, strfailcontent);

				TravisCIDiffGenerator diffgen = new TravisCIDiffGenerator();
				actions = diffgen.extractTravisFileChange(f2, f1);

				f1.delete();
				f2.delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			

		} else {

			try {
				// ObjectId failobjectid = repository.resolve(fID);
				ObjectId passobjectid = repository.resolve(pID);

//			if (failobjectid == null || passobjectid == null)
//				return null;

				if (passobjectid == null)
					return null;

				// RevCommit failcommit = rw.parseCommit(failobjectid);
				RevCommit failcommit = null;
				RevCommit passcommit = null;

				try {
					passcommit = rw.parseCommit(passobjectid);
					if (passcommit.getParentCount() > 0) {
						failcommit = rw.parseCommit(passcommit.getParent(0).getId());
						// DiffFormatter df = commitAnalyzingUtils.setDiffFormatter(repository, true);
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
					// return null;
				}

				if (passcommit == null) {
					try {
						ObjectId failobjectid = repository.resolve(fID);
						failcommit = rw.parseCommit(failobjectid);

						rw.markStart(failcommit);
						PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<>();
						plotCommitList.source(rw);
						plotCommitList.fillTo(Integer.MAX_VALUE);

						for (RevCommit com : rw) {
							passcommit = rw.parseCommit(com.getId());
							if (passcommit != null) {
								break;
							}
						}

					} catch (Exception e) {
						System.out.println(e.getMessage());
						return null;
					}
				}

				// RevCommit commit = rw.parseCommit(objectid);

				/// System.out.println(commit.getFullMessage());

				/// System.out.println(commit.getFullMessage());

				DiffFormatter df = commitAnalyzingUtils.setDiffFormatter(repository, true);

				List<DiffEntry> diffs = df.scan(failcommit.getTree(), passcommit.getTree());

				for (DiffEntry diff : diffs) {

					if (diff.getNewPath().endsWith("travis.yml")) {

						String currentContent = getFileContentAtCommit(pID, diff);
						// String previousContent = getFileContentAtCommit(fID, diff);
						String previousContent = getFileContentAtCommit(failcommit.getName(), diff);

						String tempcurr = Config.patchDir + "j1.json";
						String tempprev = Config.patchDir + "j2.json";

						File f1 = commitAnalyzingUtils.writeContentInFile(tempcurr, currentContent);
						File f2 = commitAnalyzingUtils.writeContentInFile(tempprev, previousContent);

						TravisCIDiffGenerator diffgen = new TravisCIDiffGenerator();
						actions = diffgen.extractTravisFileChange(f2, f1);

						f1.delete();
						f2.delete();

					}
				}

			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

		return actions;
	}

	private String getHashBefore(Date date) throws Exception {
		try (RevWalk revWalk = new RevWalk(repository)) {
			revWalk.markStart(revWalk.parseCommit(repository.resolve(Constants.HEAD)));
			revWalk.setRetainBody(false);
			revWalk.setRevFilter(AndRevFilter.create(CommitTimeRevFilter.before(date), MaxCountRevFilter.create(1)));
			RevCommit revCommit = revWalk.next();
			if (revCommit == null) {
				return null;
			}
			return revCommit.name();
		}
	}

	public void checkOutAtSpecificCommitID(String commitid) {
		try {
			git.fetch().call();
			git.checkout().setName(commitid).call();
		} catch (RefAlreadyExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RefNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRefNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CheckoutConflictException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void checkoutRepoBeforeDate(Date dt) {
		try {
			String commitid = getHashBefore(dt);
			checkOutAtSpecificCommitID(commitid);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static File getCloneLocation(String projectName) {
		return new File(Config.repoDir + projectName);
	}
	
	public void cloneRepository(String folderName) {
		try {
			File f = getCloneLocation(folderName);
			Git.cloneRepository().setURI(gitUrl).setBranch("main").setDirectory(f).call();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
