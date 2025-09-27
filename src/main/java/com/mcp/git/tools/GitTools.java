package com.mcp.git.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GitTools {

    private static final Logger logger = LoggerFactory.getLogger(GitTools.class);

    /**
     * Shows the working tree status
     * @param repoPath Path to Git repository
     * @return Current status of working directory as text output
     */
    @Tool(name = "git_status", description = "Shows the working tree status. Input: repo_path (string): Path to Git repository. Returns: Current status of working directory as text output.")
    public String gitStatus(String repoPath) {
        logger.info("Fetching git status for repository at path: {}", repoPath);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                Status status = git.status().call();
                StringBuilder sb = new StringBuilder();
                if (!status.getAdded().isEmpty()) sb.append("Added: ").append(status.getAdded()).append("\n");
                if (!status.getChanged().isEmpty()) sb.append("Changed: ").append(status.getChanged()).append("\n");
                if (!status.getModified().isEmpty()) sb.append("Modified: ").append(status.getModified()).append("\n");
                if (!status.getMissing().isEmpty()) sb.append("Missing: ").append(status.getMissing()).append("\n");
                if (!status.getRemoved().isEmpty()) sb.append("Removed: ").append(status.getRemoved()).append("\n");
                if (!status.getUntracked().isEmpty()) sb.append("Untracked: ").append(status.getUntracked()).append("\n");
                if (sb.isEmpty()) sb.append("Working directory clean\n");
                String result = sb.toString();
                logger.info("request (gitStatus : repoPath={}) ==> (response ( {} ))", repoPath, result);
                return result;
            }
        } catch (RepositoryNotFoundException e) {
            String result = "Repository not found at: " + repoPath;
            logger.info("request (gitStatus : repoPath={}) ==> (response ( {} ))", repoPath, result);
            return result;
        } catch (Exception e) {
            String result = "Error reading git status: " + e.getMessage();
            logger.info("request (gitStatus : repoPath={}) ==> (response ( {} ))", repoPath, result);
            return result;
        }
    }

    /**
     * Shows changes in working directory not yet staged
     * @param repoPath Path to Git repository
     * @param contextLines Number of context lines to show (default: 3)
     * @return Diff output of unstaged changes
     */
    @Tool(name = "git_diff_unstaged", description = "Shows changes in working directory not yet staged. Inputs: repo_path (string), context_lines (number, optional). Returns: Diff output of unstaged changes.")
    public String gitDiffUnstaged(String repoPath, Integer contextLines) {
        logger.info("Fetching unstaged diff for repository at path==> {} ", repoPath);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var repo = git.getRepository();
                var reader = repo.newObjectReader();
                var oldTreeIter = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                var headId = repo.resolve("HEAD^{tree}");
                oldTreeIter.reset(reader, headId);
                var newTreeIter = new org.eclipse.jgit.treewalk.FileTreeIterator(repo);
                var diffs = git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();
                StringBuilder sb = new StringBuilder();
                for (var diff : diffs) {
                    sb.append(diff.getChangeType()).append(": ").append(diff.getNewPath()).append("\n");
                }
                if (sb.isEmpty()) sb.append("No unstaged changes\n");
                String result = sb.toString();
                logger.info("request (gitDiffUnstaged : repoPath={}, contextLines={}) ==> (response ( {} ))", repoPath, contextLines, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error reading unstaged diff: " + e.getMessage();
            logger.info("request (gitDiffUnstaged : repoPath={}, contextLines={}) ==> (response ( {} ))", repoPath, contextLines, result);
            return result;
        }
    }

    /**
     * Shows changes that are staged for commit
     * @param repoPath Path to Git repository
     * @param contextLines Number of context lines to show (default: 3)
     * @return Diff output of staged changes
     */
    @Tool(name = "git_diff_staged", description = "Shows changes that are staged for commit. Inputs: repo_path (string), context_lines (number, optional). Returns: Diff output of staged changes.")
    public String gitDiffStaged(String repoPath, Integer contextLines) {
        logger.info("Fetching staged diff for repository at path: {}", repoPath);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var repo = git.getRepository();
                var reader = repo.newObjectReader();
                var headId = repo.resolve("HEAD^{tree}");
                var indexId = repo.resolve("INDEX^{tree}");
                var indexTreeIter = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                if (indexId != null) {
                    indexTreeIter.reset(reader, indexId);
                } else {
                    indexTreeIter.reset();
                }
                var oldTreeIter = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                oldTreeIter.reset(reader, headId);
                var diffs = git.diff().setOldTree(oldTreeIter).setNewTree(indexTreeIter).call();
                StringBuilder sb = new StringBuilder();
                for (var diff : diffs) {
                    sb.append(diff.getChangeType()).append(": ").append(diff.getNewPath()).append("\n");
                }
                if (sb.isEmpty()) sb.append("No staged changes\n");
                String result = sb.toString();
                logger.info("request (gitDiffStaged : repoPath={}, contextLines={}) ==> (response ( {} ))", repoPath, contextLines, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error reading staged diff: " + e.getMessage();
            logger.info("request (gitDiffStaged : repoPath={}, contextLines={}) ==> (response ( {} ))", repoPath, contextLines, result);
            return result;
        }
    }

    /**
     * Shows differences between branches or commits
     * @param repoPath Path to Git repository
     * @param target Target branch or commit to compare with
     * @param contextLines Number of context lines to show (default: 3)
     * @return Diff output comparing current state with target
     */
    @Tool(name = "git_diff", description = "Shows differences between branches or commits. Inputs: repo_path (string), target (string), context_lines (number, optional). Returns: Diff output comparing current state with target.")
    public String gitDiff(String repoPath, String target, Integer contextLines) {
        logger.info("Fetching diff for repository at path: {} with target: {}", repoPath, target);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var repo = git.getRepository();
                var reader = repo.newObjectReader();
                var oldTreeIter = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                var targetId = repo.resolve(target + "^{tree}");
                oldTreeIter.reset(reader, targetId);
                var newTreeIter = new org.eclipse.jgit.treewalk.FileTreeIterator(repo);
                var diffs = git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();
                StringBuilder sb = new StringBuilder();
                for (var diff : diffs) {
                    sb.append(diff.getChangeType()).append(": ").append(diff.getNewPath()).append("\n");
                }
                if (sb.isEmpty()) sb.append("No diff with target\n");
                String result = sb.toString();
                logger.info("request (gitDiff : repoPath={}, target={}, contextLines={}) ==> (response ( {} ))", repoPath, target, contextLines, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error reading diff: " + e.getMessage();
            logger.info("request (gitDiff : repoPath={}, target={}, contextLines={}) ==> (response ( {} ))", repoPath, target, contextLines, result);
            return result;
        }
    }

    /**
     * Records changes to the repository
     * @param repoPath Path to Git repository
     * @param message Commit message
     * @return Confirmation with new commit hash
     */
    @Tool(name = "git_commit", description = "Records changes to the repository. Inputs: repo_path (string), message (string). Returns: Confirmation with new commit hash.")
    public String gitCommit(String repoPath, String message) {
        logger.info("Committing changes for repository at path: {} with message: {}", repoPath, message);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var commit = git.commit().setMessage(message).call();
                String result = "Committed: " + commit.getName();
                logger.info("request (gitCommit : repoPath={}, message={}) ==> (response ( {} ))", repoPath, message, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error committing: " + e.getMessage();
            logger.info("request (gitCommit : repoPath={}, message={}) ==> (response ( {} ))", repoPath, message, result);
            return result;
        }
    }

    /**
     * Adds file contents to the staging area
     * @param repoPath Path to Git repository
     * @param files Array of file paths to stage
     * @return Confirmation of staged files
     */
    @Tool(name = "git_add", description = "Adds file contents to the staging area. Inputs: repo_path (string), files (string[]). Returns: Confirmation of staged files.")
    public String gitAdd(String repoPath, List<String> files) {
        logger.info("Staging files for repository at path: {} with files: {}", repoPath, files);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                for (String file : files) {
                    git.add().addFilepattern(file).call();
                }
                String result = "Staged files: " + files;
                logger.info("request (gitAdd : repoPath={}, files={}) ==> (response ( {} ))", repoPath, files, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error staging files: " + e.getMessage();
            logger.info("request (gitAdd : repoPath={}, files={}) ==> (response ( {} ))", repoPath, files, result);
            return result;
        }
    }

    /**
     * Unstages all staged changes
     * @param repoPath Path to Git repository
     * @return Confirmation of reset operation
     */
    @Tool(name = "git_reset", description = "Unstages all staged changes. Input: repo_path (string). Returns: Confirmation of reset operation.")
    public String gitReset(String repoPath) {
        logger.info("Resetting staged changes for repository at path: {}", repoPath);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.MIXED).call();
                String result = "Unstaged all changes.";
                logger.info("request (gitReset : repoPath={}) ==> (response ( {} ))", repoPath, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error resetting: " + e.getMessage();
            logger.info("request (gitReset : repoPath={}) ==> (response ( {} ))", repoPath, result);
            return result;
        }
    }

    /**
     * Shows the commit logs with optional date filtering
     * @param repoPath Path to Git repository
     * @param maxCount Maximum number of commits to show (default: 10)
     * @param startTimestamp Start timestamp for filtering commits
     * @param endTimestamp End timestamp for filtering commits
     * @return Array of commit entries with hash, author, date, and message
     */
    @Tool(name = "git_log", description = "Shows the commit logs with optional date filtering. Inputs: repo_path (string), max_count (number, optional), start_timestamp (string, optional), end_timestamp (string, optional). Returns: Array of commit entries with hash, author, date, and message.")
    public List<Map<String, String>> gitLog(String repoPath, Integer maxCount, String startTimestamp, String endTimestamp) {
        logger.info("Fetching commit logs for repository at path: {} with maxCount: {}, startTimestamp: {}, endTimestamp: {}", repoPath, maxCount, startTimestamp, endTimestamp);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var logCmd = git.log();
                if (maxCount != null) logCmd.setMaxCount(maxCount);
                List<Map<String, String>> result = new java.util.ArrayList<>();
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ISO_DATE_TIME;
                java.time.Instant start = null, end = null;
                if (startTimestamp != null && !startTimestamp.isEmpty()) {
                    try { start = java.time.Instant.parse(startTimestamp); } catch (Exception ignore) {}
                }
                if (endTimestamp != null && !endTimestamp.isEmpty()) {
                    try { end = java.time.Instant.parse(endTimestamp); } catch (Exception ignore) {}
                }
                for (var commit : logCmd.call()) {
                    var commitDate = commit.getAuthorIdent().getWhen().toInstant();
                    boolean afterStart = (start == null) || !commitDate.isBefore(start);
                    boolean beforeEnd = (end == null) || !commitDate.isAfter(end);
                    if (afterStart && beforeEnd) {
                        result.add(Map.of(
                                "hash", commit.getName(),
                                "author", commit.getAuthorIdent().getName(),
                                "date", commit.getAuthorIdent().getWhen().toString(),
                                "message", commit.getFullMessage()
                        ));
                    }
                }
                logger.info("request (gitLog : repoPath={}, maxCount={}, startTimestamp={}, endTimestamp={}) ==> (response ( {} ))", repoPath, maxCount, startTimestamp, endTimestamp, result);
                return result;
            }
        } catch (Exception e) {
            List<Map<String, String>> result = List.of(Map.of("error", "Error reading log: " + e.getMessage()));
            logger.info("request (gitLog : repoPath={}, maxCount={}, startTimestamp={}, endTimestamp={}) ==> (response ( {} ))", repoPath, maxCount, startTimestamp, endTimestamp, result);
            return result;
        }
    }

    /**
     * Creates a new branch
     * @param repoPath Path to Git repository
     * @param branchName Name of the new branch
     * @param startPoint Starting point for the new branch (optional)
     * @return Confirmation of branch creation
     */
    @Tool(name = "git_create_branch", description = "Creates a new branch. Inputs: repo_path (string), branch_name (string), start_point (string, optional). Returns: Confirmation of branch creation.")
    public String gitCreateBranch(String repoPath, String branchName, String startPoint) {
        logger.info("Creating new branch for repository at path: {} with branchName: {} and startPoint: {}", repoPath, branchName, startPoint);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                if (startPoint != null && !startPoint.isEmpty()) {
                    git.branchCreate().setName(branchName).setStartPoint(startPoint).call();
                } else {
                    git.branchCreate().setName(branchName).call();
                }
                String result = "Created branch: " + branchName;
                logger.info("request (gitCreateBranch : repoPath={}, branchName={}, startPoint={}) ==> (response ( {} ))", repoPath, branchName, startPoint, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error creating branch: " + e.getMessage();
            logger.info("request (gitCreateBranch : repoPath={}, branchName={}, startPoint={}) ==> (response ( {} ))", repoPath, branchName, startPoint, result);
            return result;
        }
    }

    /**
     * Switches branches
     * @param repoPath Path to Git repository
     * @param branchName Name of branch to checkout
     * @return Confirmation of branch switch
     */
    @Tool(name = "git_checkout", description = "Switches branches. Inputs: repo_path (string), branch_name (string). Returns: Confirmation of branch switch.")
    public String gitCheckout(String repoPath, String branchName) {
        logger.info("Switching branches for repository at path: {} to branchName: {}", repoPath, branchName);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                git.checkout().setName(branchName).call();
                String result = "Checked out branch: " + branchName;
                logger.info("request (gitCheckout : repoPath={}, branchName={}) ==> (response ( {} ))", repoPath, branchName, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error checking out branch: " + e.getMessage();
            logger.info("request (gitCheckout : repoPath={}, branchName={}) ==> (response ( {} ))", repoPath, branchName, result);
            return result;
        }
    }

    /**
     * Shows the contents of a commit
     * @param repoPath Path to Git repository
     * @param revision The revision (commit hash, branch name, tag) to show
     * @return Contents of the specified commit
     */
    @Tool(name = "git_show", description = "Shows the contents of a commit. Inputs: repo_path (string), revision (string). Returns: Contents of the specified commit.")
    public String gitShow(String repoPath, String revision) {
        logger.info("Showing commit contents for repository at path: {} with revision: {}", repoPath, revision);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var repo = git.getRepository();
                var commit = repo.resolve(revision);
                if (commit == null) {
                    String result = "Revision not found: " + revision;
                    logger.info("request (gitShow : repoPath={}, revision={}) ==> (response ( {} ))", repoPath, revision, result);
                    return result;
                }
                var walk = new org.eclipse.jgit.revwalk.RevWalk(repo);
                var revCommit = walk.parseCommit(commit);
                StringBuilder sb = new StringBuilder();
                sb.append("Commit: ").append(revCommit.getName()).append("\n");
                sb.append("Author: ").append(revCommit.getAuthorIdent().getName()).append(" <").append(revCommit.getAuthorIdent().getEmailAddress()).append(">").append("\n");
                sb.append("Date: ").append(revCommit.getAuthorIdent().getWhen()).append("\n");
                sb.append("\n").append(revCommit.getFullMessage()).append("\n");
                walk.dispose();
                String result = sb.toString();
                logger.info("request (gitShow : repoPath={}, revision={}) ==> (response ( {} ))", repoPath, revision, result);
                return result;
            }
        } catch (Exception e) {
            String result = "Error showing commit: " + e.getMessage();
            logger.info("request (gitShow : repoPath={}, revision={}) ==> (response ( {} ))", repoPath, revision, result);
            return result;
        }
    }

    /**
     * List Git branches
     * @param repoPath Path to the Git repository
     * @param branchType Whether to list local, remote, or all branches
     * @param contains The commit sha that branch should contain (optional)
     * @param notContains The commit sha that branch should NOT contain (optional)
     * @return List of branches
     */
    @Tool(name = "git_branch", description = "List Git branches. Inputs: repo_path (string), branch_type (string), contains (string, optional), not_contains (string, optional). Returns: List of branches.")
    public List<String> gitBranch(String repoPath, String branchType, String contains, String notContains) {
        logger.info("Listing branches for repository at path: {} with branchType: {}, contains: {}, notContains: {}", repoPath, branchType, contains, notContains);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                List<String> branches = new java.util.ArrayList<>();
                if ("remote".equalsIgnoreCase(branchType)) {
                    git.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call().forEach(ref -> branches.add(ref.getName()));
                } else if ("all".equalsIgnoreCase(branchType)) {
                    git.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL).call().forEach(ref -> branches.add(ref.getName()));
                } else {
                    git.branchList().call().forEach(ref -> branches.add(ref.getName()));
                }
                logger.info("request (gitBranch : repoPath={}, branchType={}, contains={}, notContains={}) ==> (response ( {} ))", repoPath, branchType, contains, notContains, branches);
                return branches;
            }
        } catch (Exception e) {
            List<String> result = List.of("Error listing branches: " + e.getMessage());
            logger.info("request (gitBranch : repoPath={}, branchType={}, contains={}, notContains={}) ==> (response ( {} ))", repoPath, branchType, contains, notContains, result);
            return result;
        }
    }

    /**
     * Clones a repository to a local path
     * @param repoUrl URL of the remote repository
     * @param localPath Path to clone repo on local
     * @return Status of clone operation
     */
    @Tool(name = "git_clone", description = "Clone repo from git. Input: repo_path (string): Path to Git repository localPath (string): path to clone repo on local. Returns: Current status of clone repo.")
    public String cloneRepository(String repoUrl, String localPath) {
        logger.info("Cloning repository from URL: {} to local path: {}", repoUrl, localPath);
        UsernamePasswordCredentialsProvider creds = getCredentialsProvider();
        if (creds == null) {
            String result = "Token missing or expired: ";
            logger.info("request (cloneRepository : repoUrl={}, localPath={}) ==> (response ( {} ))", repoUrl, localPath, result);
            return result;
        }
        try {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(creds)
                    .call();
            String result = "Cloned successfully";
            logger.info("request (cloneRepository : repoUrl={}, localPath={}) ==> (response ( {} ))", repoUrl, localPath, result);
            return result;
        } catch (Exception e) {
            String result;
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("auth")) {
                result = "Token missing or expired : ";
            } else {
                result = "Error cloning: " + e.getMessage();
            }
            logger.info("request (cloneRepository : repoUrl={}, localPath={}) ==> (response ( {} ))", repoUrl, localPath, result);
            return result;
        }
    }

    /**
     * Pushes local commits to the remote repository
     * @param repoPath Path to local Git repository
     * @param remote Remote name (default: origin)
     * @param branch Branch name (optional)
     * @return Status of push operation
     */
    @Tool(name = "git_push", description = "Pushes local commits to the remote repository. Inputs: repo_path (string), remote (string, optional), branch (string, optional). Returns: Status of push operation.")
    public String gitPush(String repoPath, String remote, String branch) {
        logger.info("Pushing commits for repository at path: {} to remote: {} branch: {}", repoPath, remote, branch);
        UsernamePasswordCredentialsProvider creds = getCredentialsProvider();
        if (creds == null) {
            String result = "Token missing or expired:";
            logger.info("request (gitPush : repoPath={}, remote={}, branch={}) ==> (response ( {} ))", repoPath, remote, branch, result);
            return result;
        }
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var pushCmd = git.push().setCredentialsProvider(creds);
                if (remote != null && !remote.isEmpty()) pushCmd.setRemote(remote);
                if (branch != null && !branch.isEmpty()) pushCmd.add(branch);
                var resultObj = pushCmd.call();
                StringBuilder sb = new StringBuilder();
                for (var res : resultObj) {
                    sb.append(res.getMessages()).append("\n");
                }
                String result = sb.length() > 0 ? sb.toString() : "Push completed.";
                logger.info("request (gitPush : repoPath={}, remote={}, branch={}) ==> (response ( {} ))", repoPath, remote, branch, result);
                return result;
            }
        } catch (Exception e) {
            String result;
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("auth")) {
                result = "Token missing or expired: ";
            } else {
                result = "Error pushing: " + e.getMessage();
            }
            logger.info("request (gitPush : repoPath={}, remote={}, branch={}) ==> (response ( {} ))", repoPath, remote, branch, result);
            return result;
        }
    }

    /**
     * Pulls changes from the remote repository
     * @param repoPath Path to local Git repository
     * @param remote Remote name (default: origin)
     * @param branch Branch name (optional)
     * @return Status of pull operation
     */
    @Tool(name = "git_pull", description = "Pulls changes from the remote repository. Inputs: repo_path (string), remote (string, optional), branch (string, optional). Returns: Status of pull operation.")
    public String gitPull(String repoPath, String remote, String branch) {
        logger.info("Pulling changes for repository at path: {} from remote: {} branch: {}", repoPath, remote, branch);
        UsernamePasswordCredentialsProvider creds = getCredentialsProvider();
        if (creds == null) {
            String result = "Token missing or expired: ";
            logger.info("request (gitPull : repoPath={}, remote={}, branch={}) ==> (response ( {} ))", repoPath, remote, branch, result);
            return result;
        }
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var pullCmd = git.pull().setCredentialsProvider(creds);
                if (remote != null && !remote.isEmpty()) pullCmd.setRemote(remote);
                if (branch != null && !branch.isEmpty()) pullCmd.setRemoteBranchName(branch);
                var resultObj = pullCmd.call();
                String result;
                if (resultObj.isSuccessful()) {
                    result = "Pull completed.";
                } else {
                    result = "Pull completed with warnings: " + resultObj.getMergeResult().toString();
                }
                logger.info("request (gitPull : repoPath={}, remote={}, branch={}) ==> (response ( {} ))", repoPath, remote, branch, result);
                return result;
            }
        } catch (Exception e) {
            String result;
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("auth")) {
                result = "Token missing or expired: ";
            } else {
                result = "Error pulling: " + e.getMessage();
            }
            logger.info("request (gitPull : repoPath={}, remote={}, branch={}) ==> (response ( {} ))", repoPath, remote, branch, result);
            return result;
        }
    }

    /**
     * Fetches changes from the remote repository
     * @param repoPath Path to local Git repository
     * @param remote Remote name (default: origin)
     * @return Status of fetch operation
     */
    @Tool(name = "git_fetch", description = "Fetches changes from the remote repository. Inputs: repo_path (string), remote (string, optional). Returns: Status of fetch operation.")
    public String gitFetch(String repoPath, String remote) {
        logger.info("Fetching changes for repository at path: {} from remote: {}", repoPath, remote);
        UsernamePasswordCredentialsProvider creds = getCredentialsProvider();
        if (creds == null) {
            return "Token missing or expired: ";
        }
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var fetchCmd = git.fetch().setCredentialsProvider(creds);
                if (remote != null && !remote.isEmpty()) fetchCmd.setRemote(remote);
                var result = fetchCmd.call();
                logger.info("Fetch completed successfully for repository at path: {}", repoPath);
                return "Fetch completed: " + result.getMessages();
            }
        } catch (Exception e) {
            logger.info("Error fetching changes for repository at path: {}", repoPath, e);
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("auth")) {
                return "Token missing or expired: ";
            }
            return "Error fetching: " + e.getMessage();
        }
    }

    private String resolveGitPat() {
        // 1. Check environment variable
        String token = System.getenv("ACCESS_TOKEN");
        if (token != null && !token.isEmpty()) return token;
        // 2. Check system property
        token = System.getProperty("ACCESS_TOKEN");
        if (token != null && !token.isEmpty()) return token;
        // 3. Check application.properties
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                token = prop.getProperty("git.pat");
                if (token != null && !token.isEmpty()) return token;
            }
        } catch (IOException ignored) {}
        return null;
    }

    private UsernamePasswordCredentialsProvider getCredentialsProvider() {
        String pat = resolveGitPat();
        if (pat == null || pat.isEmpty()) {
            return null;
        }
        return new UsernamePasswordCredentialsProvider(pat, "");
    }

    private String resolveApiBaseUrl() {
        String url = System.getenv("GITHUB_API_BASE_URL");
        if (url != null && !url.isEmpty()) return url;
        url = System.getProperty("GITHUB_API_BASE_URL");
        if (url != null && !url.isEmpty()) return url;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                url = prop.getProperty("github.api.base.url");
                if (url != null && !url.isEmpty()) return url;
            }
        } catch (IOException ignored) {}
        return "https://api.github.com";
    }

    /**
     * Reviews open pull requests for a repository and returns a summary for each.
     * @param owner GitHub repository owner
     * @param repo GitHub repository name
     * @return Summary of open PRs (title, author, description, etc.)
     */
    @Tool(name = "review_open_pull_requests", description = "Reviews open pull requests for a repository and returns a summary for each. Inputs: owner (string), repo (string). Returns: List of PR summaries.")
    public String reviewOpenPullRequests(String owner, String repo) {
        String token = resolveGitPat();
        if (token == null || token.isEmpty()) {
            return "Token missing or expired : ";
        }
        String apiBaseUrl = resolveApiBaseUrl();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/repos/" + owner + "/" + repo + "/pulls?state=open")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error fetching PRs: " + response.code() + " " + response.message();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body().string());
            StringBuilder sb = new StringBuilder();
            if (root.isArray()) {
                for (JsonNode pr : root) {
                    sb.append("PR #").append(pr.get("number").asInt())
                            .append(": ").append(pr.get("title").asText())
                            .append(" by ").append(pr.get("user").get("login").asText()).append("\n");
                    sb.append("Description: ").append(pr.get("body") != null ? pr.get("body").asText("") : "").append("\n");
                    sb.append("URL: ").append(pr.get("html_url").asText()).append("\n\n");
                }
                if (sb.length() == 0) sb.append("No open PRs found.\n");
            } else {
                sb.append("Unexpected response format from GitHub API.");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error reviewing PRs: " + e.getMessage();
        }
    }

    /**
     * Approves a pull request on GitHub
     * @param owner GitHub repository owner
     * @param repo GitHub repository name
     * @param prNumber Pull request number
     * @return Status of approval
     */
    @Tool(name = "approve_pull_request", description = "Approves a pull request on GitHub. Inputs: owner (string), repo (string), pr_number (int). Returns: Status of approval.")
    public String approvePullRequest(String owner, String repo, int prNumber) {
        String token = resolveGitPat();
        if (token == null || token.isEmpty()) {
            return "Token missing or expired: " ;
        }
        String apiBaseUrl = resolveApiBaseUrl();
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        String url = apiBaseUrl + "/repos/" + owner + "/" + repo + "/pulls/" + prNumber + "/reviews";
        String jsonBody = "{\"event\":\"APPROVE\"}";
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return "PR approved successfully.";
            } else {
                JsonNode error = mapper.readTree(response.body().string());
                String msg = error.has("message") ? error.get("message").asText() : response.message();
                return "Error approving PR: " + response.code() + " " + msg;
            }
        } catch (Exception e) {
            return "Error approving PR: " + e.getMessage();
        }
    }

    /**
     * Utility to resolve GitHub owner and repo from env, system property, or application.properties
     */
    private String[] resolveOwnerRepo() {
        String owner = System.getenv("GITHUB_OWNER");
        if (owner != null && !owner.isEmpty()) {
            String repo = System.getenv("GITHUB_REPO");
            if (repo != null && !repo.isEmpty()) return new String[]{owner, repo};
        }
        owner = System.getProperty("GITHUB_OWNER");
        if (owner != null && !owner.isEmpty()) {
            String repo = System.getProperty("GITHUB_REPO");
            if (repo != null && !repo.isEmpty()) return new String[]{owner, repo};
        }
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                owner = prop.getProperty("github.owner");
                String repo = prop.getProperty("github.repo");
                if (owner != null && !owner.isEmpty() && repo != null && !repo.isEmpty()) {
                    return new String[]{owner, repo};
                }
            }
        } catch (IOException ignored) {}
        // Try to extract from .git/config
        try {
            File configFile = new File(".git/config");
            if (configFile.exists()) {
                List<String> lines = java.nio.file.Files.readAllLines(configFile.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("url = ")) {
                        String url = line.substring(6).replace(".git", "");
                        // Support both HTTPS and SSH URLs
                        String repoPath = url;
                        if (url.startsWith("https://github.com/")) {
                            repoPath = url.substring("https://github.com/".length());
                        } else if (url.startsWith("git@github.com:")) {
                            repoPath = url.substring("git@github.com:".length());
                        }
                        String[] parts = repoPath.split("/");
                        if (parts.length == 2) {
                            return new String[]{parts[0], parts[1]};
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Shows all open PRs for the current repo configured via env, system property, or application.properties
     * @return Summary of open PRs
     */
    @Tool(name = "show_current_repo_prs", description = "Shows all open PRs for the current repo configured via env, system property, or application.properties. Returns: List of PR summaries.")
    public String showCurrentRepoPRs() {
        String[] ownerRepo = resolveOwnerRepo();
        if (ownerRepo == null) {
            return "Owner or repo not configured in environment, system property, or application.properties.";
        }
        return reviewOpenPullRequests(ownerRepo[0], ownerRepo[1]);
    }
}
