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

@Service
public class GitTools {

    @Value("${git.pat}")
    private String gitPat;

    /**
     * Shows the working tree status
     * @param repoPath Path to Git repository
     * @return Current status of working directory as text output
     */
    @Tool(name = "git_status", description = "Shows the working tree status. Input: repo_path (string): Path to Git repository. Returns: Current status of working directory as text output.")
    public String gitStatus(String repoPath) {
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
                return sb.toString();
            }
        } catch (RepositoryNotFoundException e) {
            return "Repository not found at: " + repoPath;
        } catch (Exception e) {
            return "Error reading git status: " + e.getMessage();
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
        // JGit does not provide a direct diff output as text, so we use DiffCommand and format manually
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
                return sb.toString();
            }
        } catch (Exception e) {
            return "Error reading unstaged diff: " + e.getMessage();
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
        // JGit does not provide a direct staged diff output as text, so we use DiffCommand and format manually
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var repo = git.getRepository();
                var reader = repo.newObjectReader();
                var headId = repo.resolve("HEAD^{tree}");
                var indexId = repo.resolve("INDEX^{tree}"); // Use the index tree
                var indexTreeIter = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                if (indexId != null) {
                    indexTreeIter.reset(reader, indexId);
                } else {
                    // fallback: empty tree
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
                return sb.toString();
            }
        } catch (Exception e) {
            return "Error reading staged diff: " + e.getMessage();
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
                return sb.toString();
            }
        } catch (Exception e) {
            return "Error reading diff: " + e.getMessage();
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
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var commit = git.commit().setMessage(message).call();
                return "Committed: " + commit.getName();
            }
        } catch (Exception e) {
            return "Error committing: " + e.getMessage();
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
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                for (String file : files) {
                    git.add().addFilepattern(file).call();
                }
                return "Staged files: " + files;
            }
        } catch (Exception e) {
            return "Error staging files: " + e.getMessage();
        }
    }

    /**
     * Unstages all staged changes
     * @param repoPath Path to Git repository
     * @return Confirmation of reset operation
     */
    @Tool(name = "git_reset", description = "Unstages all staged changes. Input: repo_path (string). Returns: Confirmation of reset operation.")
    public String gitReset(String repoPath) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.MIXED).call();
                return "Unstaged all changes.";
            }
        } catch (Exception e) {
            return "Error resetting: " + e.getMessage();
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
                return result;
            }
        } catch (Exception e) {
            return List.of(Map.of("error", "Error reading log: " + e.getMessage()));
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
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                if (startPoint != null && !startPoint.isEmpty()) {
                    git.branchCreate().setName(branchName).setStartPoint(startPoint).call();
                } else {
                    git.branchCreate().setName(branchName).call();
                }
                return "Created branch: " + branchName;
            }
        } catch (Exception e) {
            return "Error creating branch: " + e.getMessage();
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
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                git.checkout().setName(branchName).call();
                return "Checked out branch: " + branchName;
            }
        } catch (Exception e) {
            return "Error checking out branch: " + e.getMessage();
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
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Git git = new Git(builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build())) {
                var repo = git.getRepository();
                var commit = repo.resolve(revision);
                if (commit == null) return "Revision not found: " + revision;
                var walk = new org.eclipse.jgit.revwalk.RevWalk(repo);
                var revCommit = walk.parseCommit(commit);
                StringBuilder sb = new StringBuilder();
                sb.append("Commit: ").append(revCommit.getName()).append("\n");
                sb.append("Author: ").append(revCommit.getAuthorIdent().getName()).append(" <").append(revCommit.getAuthorIdent().getEmailAddress()).append(">\n");
                sb.append("Date: ").append(revCommit.getAuthorIdent().getWhen()).append("\n");
                sb.append("\n").append(revCommit.getFullMessage()).append("\n");
                walk.dispose();
                return sb.toString();
            }
        } catch (Exception e) {
            return "Error showing commit: " + e.getMessage();
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
                return branches;
            }
        } catch (Exception e) {
            return List.of("Error listing branches: " + e.getMessage());
        }
    }
}
