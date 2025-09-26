package com.mcp.git.tools;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class GitTools2 {


    private static String gitPat = System.getProperty("ACCESS_TOKEN");

    /**
     * Shows the working tree status
     * @param repoUrl Path to Git repository
     *                localPath Path to local repository
     * @return Current status of working directory as text output
     */

    @Tool(name = "git_clone", description = "Clone repo from git. Input: repo_path (string): Path to Git repository localPath (string): path to clone repo on local. Returns: Current status of clone repo.")
    public String cloneRepository(String repoUrl, String localPath) {

        try {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(getCredentialsProvider()) // Use credentials if available
                    .call();
            return "Cloned successfully";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private static UsernamePasswordCredentialsProvider getCredentialsProvider() {
        if (gitPat != null && !gitPat.isEmpty()) {
            return new UsernamePasswordCredentialsProvider(gitPat, "");
        }
        return null;
    }
}
