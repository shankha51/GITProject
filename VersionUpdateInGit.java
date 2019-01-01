package com.travelport.impl;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.travelport.util.GitUtil;

public class VersionUpdateInGit {

	static final Map<String, String> appPropertyMap = GitUtil.readAppPropertyFile();
	static final Map<String, String> configPropertyMap = GitUtil.readConfigPropertyFile(); 
	static final String userId = configPropertyMap.get("userId");
	static final String password = configPropertyMap.get("password");
	static final String branch = configPropertyMap.get("branch");
	static final String oldVersion = configPropertyMap.get("oldVersion");
	static final String newVersion = configPropertyMap.get("newVersion");
	static final String repodir = configPropertyMap.get("repodir");
	
	public static void main(String[] args) {
		String projectName = null;
		String gitUrl = null;
		for(Entry<String, String> entry : appPropertyMap.entrySet()) {
			projectName = entry.getKey();
			gitUrl = entry.getValue();
		}
		
		// Cloning GIT project
		Git git = null;
		try {
			git = GitUtil.cloneRepository(userId, password, branch, gitUrl, repodir);
			System.out.println(projectName+": cloning done..");
		} catch(GitAPIException ex) {
			System.out.println("Exception occurred in cloning git project");
			ex.printStackTrace();
		}
		
		try {
			GitUtil.changeFileContent(repodir, oldVersion, newVersion, projectName);
		} catch(IOException io) {
			io.printStackTrace();
		}
		
		try {
			GitUtil.gitComit(git);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Close the GIT object
		if(null != git) {
			git.close();
		}
			
		// clean up here to not keep using more and more disk-space for these samples
        try {
			FileUtils.deleteDirectory(new File(repodir));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
