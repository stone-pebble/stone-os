# Source Control Workflow

**Source:** https://source.android.com/docs/setup/create/coding-tasks
**Scrape Date:** 2025-10-23

---

Working with Android code requires using Git (an open-source version control system) and Repo (a Google-built repository management tool that runs on top of Git). See Source Control Tools for an explanation of the relationship between Repo and Git and links to supporting documentation for each tool.


## FLOW

Android development involves the following basic workflow:

1.  Start a new topic branch using `repo start`.
2.  Edit the files.
3.  Stage changes using `git add`.
4.  Commit changes using `git commit`.
5.  Upload changes to the review server using `repo upload`.


## TASKS

Working with Git and Repo in the Android code repositories involves performing the following common tasks.

| Command | Description |
| :--- | :--- |
| `repo init` | Initializes a new client. |
| `repo sync` | Syncs the client to the repositories. |
| `repo start` | Starts a new branch. |
| `repo status` | Shows the status of the current branch. |
| `repo upload` | Uploads changes to the review server. |
| `git add` | Stages the files. |
| `git commit` | Commits the staged files. |
| `git branch` | Shows the current branches. |
| `git branch [branch]` | Creates a new topic branch. |
| `git checkout [branch]` | Switches HEAD to the specified branch. |
| `git merge [branch]` | Merges [branch] into current branch. |
| `git diff` | Shows diff of the unstaged changes. |
| `git diff --cached` | Shows diff of the staged changes. |
| `git log` | Shows the history of the current branch. |
| `git log m/[codeline]..` | Shows the commits that aren't pushed. |

For information about using Repo to download source, see Downloading the Source and the Repo Command Reference.


## SYNCHRONIZING CLIENTS

To synchronize the files for all available projects:

```bash
repo sync
```

To synchronize the files for selected projects:

```bash
repo sync PROJECT0 PROJECT1 ... PROJECTN
```


## CREATING TOPIC BRANCHES

Start a topic branch in your local work environment whenever you begin a change, such as when you begin work on a bug or new feature. A topic branch isn't a copy of the original files; it's a pointer to a particular commit, which makes creating local branches and switching among them a lightweight operation. By using branches, you can isolate one aspect of your work from the others. For an interesting article about using topic branches, refer to Separating topic branches.

To start a topic branch using Repo, navigate to the project and run:

```bash
repo start BRANCH_NAME .
```

The trailing period ( .) represents the project in the current working directory.

To verify that the new branch was created:

```bash
repo status .
```


## USING TOPIC BRANCHES

To assign the branch to a specific project:

```bash
repo start BRANCH_NAME PROJECT_NAME
```

For a list of all projects, refer to android.googlesource.com. If you've already navigated to the project directory, just use a period to represent the current project.

To switch to another branch in your local work environment:

```bash
git checkout BRANCH_NAME
```

To view a list of existing branches:

```bash
git branch
```

or

```bash
repo branches
```

Both commands return the list of existing branches with the name of the current branch preceded by an asterisk (*).

**Note:** A bug might cause `repo sync` to reset the local topic branch. If `git branch` shows `* (no branch)` after you run `repo sync`, run `git checkout` again.


## STAGING FILES

By default, Git notices but doesn't track the changes that you make in a project. To tell Git to preserve your changes, you must mark or stage those changes for inclusion in a commit.

To stage changes:

```bash
git add
```

This command accepts arguments for files or directories within the project directory. Despite the name, `git add` doesn't just add files to the Git repository; it can also be used to stage file modifications and deletions.


## VIEWING CLIENT STATUS

To list the state of files:

```bash
repo status
```

To view uncommitted edits (local edits that are not marked for commit):

```bash
repo diff
```

To view committed edits (located edits that are marked for commit), ensure that you're in the project directory then run `git diff` with the `cached` argument:

```bash
cd ~/WORKING_DIRECTORY/PROJECT
git diff --cached
```


## COMMITTING CHANGES

A commit is the basic unit of revision control in Git and consists of a snapshot of the directory structure and file contents for the entire project. Use this command to create a commit in Git:

```bash
git commit
```

When prompted for a commit message, provide a short (but helpful) message for changes submitted to AOSP. If you don't add a commit message, the commit fails.

Clear, concise, detailed commit messages are important and required. For guidance on creating them, see Making your change


## UPLOADING CHANGES TO GERRIT

Update to the latest revision, then upload the change:

```bash
repo sync
repo upload
```

These commands return a list of changes you have committed and prompt you to select the branches to upload to the review server. If there's only one branch, you see a simple `y/n` prompt.


## RESOLVING SYNC CONFLICTS

If the `repo sync` command returns sync conflicts:

1.  View the files that are unmerged (status code = `U`).
2.  Edit the conflict regions as necessary.
3.  Change to the relevant project directory. Add and commit the affected files, then rebase the changes:
    
    ```bash
    git add .
    git commit
    git rebase --continue
    ```

4.  After the rebase completes, start the entire sync again:
    
    ```bash
    repo sync PROJECT0 PROJECT1 ... PROJECTN
    ```


## CLEANING UP CLIENTS

After merging changes to Gerrit, update your local working directory, then use `repo prune` to safely remove stale topic branches:

```bash
repo sync
repo prune
```


## DELETING CLIENTS

Because all state information is stored in your client, you only need to delete the directory from your file system:

```bash
rm -rf WORKING_DIRECTORY
```

Deleting a client permanently deletes any changes you haven't uploaded for review.
