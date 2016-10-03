# Brute PR
Forked from PR Harmony: https://github.com/monitorjbl/pr-harmony

![Logo](/src/main/resources/images/pluginIcon.png?raw=true)

Most teams have processes around how they merge PRs, but there aren't mechanisms in Bitbucket to ensure that no one accidentally merges something they shouldn't. This plugin attempts to address some of these shortcomings.

# Branch Protection

There are branch permissions in Bitbucket, but they interfere with merge permissions. If you want to lock down a branch to prevent direct commits, you won't be able to merge unless you grant write access to said branch (which defeats the purpose of locking it down in the first place).

This plugin gives you some extra options to make your life a little easier:

* **Block Direct Commits**: Prevent any `git push` to branches.
* **Excluded Users**: Allows particular users the ability to push to blocked branches. Handy if you have integration jobs.
* **Block Pull Requests**: Prevents any pull request from being merged to branches. Handy if you have branches that should only be committed to by integration jobs.

# Pull Request Options

These options allow more Gerrit-style voting workflows within Bitbucket.

* **Default reviewers**: Reviewers that will be added to every PR that is opened.
* **Required reviewers**: Reviewers that must *approve* every PR that is opened.
* **# of reviews**: Number of required reviewers that must approve a PR for it to be merged.
* **Automerge Pull Requests**: Automatically merge pull requests when all required approvals are submitted.
* **AutoUnapprove**: Automatically unapprove pull request on any change of scope
* **Block request**: Block 
# Screenshots

# Building

Requires Oracle Java 8+ and Maven 3.2+

```
git clone https://github.com/brute-force-pl/brute-pr.git
cd brute-pr
mvn clean package -s settings.xml
```

# Debugging

The easiest way to debug this plugin is to use [Atlassian's SDK](https://developer.atlassian.com/docs/getting-started) to run it in a local instance of Bitbucket. Once you have it set up, simply go into the project directory and run the following:

```
atlas-run --product bitbucket --http-port 7990 --jvmargs "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

This will take some time to run at first, as it has to download the Bitbucket runtime. Once the app comes up, go to http://localhost:7990/bitbucket in your browser. The plugin will already be installed, all you have to do is configure it for the test project. The server will also be started with Java remote debugging enabled. In your IDE, you can connect to the remote debugging port on localhost at port 5005.

The SDK allows you to make live changes by detecting changes to class files and resources in the plugin. However, if for some reason it doesn't redeploy, you can use the `atlas-cli` command to quickly redeploy the plugin instead of restarting the server. Just run `atlas-cli` from the project directory and when the command prompt comes up type `pi`:
