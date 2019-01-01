package com.travelport.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

public class GitUtil {
	
	private GitUtil() {}

	public static Map<String, String> readAppPropertyFile() {
		Map<String, String> appPropertyMap = new HashMap<>();
		Properties prop = new Properties();
		try(InputStream input = GitUtil.class.getClassLoader().getResourceAsStream("config/application.properties")) {
			prop.load(input);
			prop.forEach((key, value) -> {
				appPropertyMap.put((String)key, (String)value);
			});
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return appPropertyMap;
	}
	
	public static Map<String, String> readConfigPropertyFile() {
		Map<String, String> configPropertyMap = new HashMap<>();
		Properties prop = new Properties();
		try(InputStream input = GitUtil.class.getClassLoader().getResourceAsStream("config/config.properties")) {
			prop.load(input);
			prop.forEach((key, value) -> {
				configPropertyMap.put((String)key, (String)value);
			});
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return configPropertyMap;
	}
	
	public static Git cloneRepository(String userId, String password, String branch, String gitURL, String checkoutDir) throws GitAPIException, TransportException, GitAPIException {
		return Git.cloneRepository()
					 .setURI(gitURL)
					 .setNoCheckout(false)
					 .setBranch(branch)
					 .setDirectory(new File(checkoutDir))
					 .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userId, password))
					 .call();		
	}
	
	public static void changeFileContent(String checkoutDir, String oldVersion, String newVersion, String projectName) throws IOException {
		FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
		Repository repository = repositoryBuilder.setGitDir(new File(checkoutDir+"/.git"))
		                .readEnvironment() // scan environment GIT_* variables
		                .findGitDir() // scan up the file system tree
		                .setMustExist(true)
		                .build();
		
		// find the HEAD
        ObjectId lastCommitId = repository.resolve(Constants.MASTER);
        
        // a RevWalk allows to walk over commits based on some filtering that is defined
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            // and using commit's tree find the path
            RevTree tree = commit.getTree();
            System.out.println("Having tree: " + tree);

            // now try to find a specific file
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create("pom.xml"));
                if (!treeWalk.next()) {
                    throw new IllegalStateException("Did not find expected file 'pom.xml'");
                }

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);

                // and then one can the loader to read the file
                String noteStr = new String(loader.getBytes()).trim();
                noteStr = noteStr.replace(oldVersion, newVersion);
                System.out.println(noteStr);
                
                writeDataToFile(noteStr, checkoutDir, projectName);
            }

            revWalk.dispose();
        }
		
	}
	
	private static void writeDataToFile(String content, String repodir, String projectName) throws IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(repodir+"/pom.xml"))){
			bufferedWriter.write(content);
		} catch(IOException io) {
			io.printStackTrace();
		}
		
	}
	
	public static void gitComit(Git git) throws Exception {
		git.add()
        	.addFilepattern("pom.xml")
        	.call();
		
		git.commit()
		   .setMessage("Pom file updated")
		   .call();
		
		PushCommand pushCommand = git.push().setPushAll().setForce(true);
        pushCommand.setRemote("https://github.com/shankha51/TestingGITRepo.git");
        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider("shankha51", "Shankha@51"));
        pushCommand.call();
		
	}
}
