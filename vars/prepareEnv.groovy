def call() {
    echo "Starting prepareEnv: Extracting SCM variables..."
    
    // default details in case of manual running
    def committerName = "Unknown/Manual Trigger"
    def commitId = "N/A"
    
    // get details from currentBuild 
    if (currentBuild.changeSets.size() > 0) {
        def changeSet = currentBuild.changeSets[0]
        if (!changeSet.isEmpty()) {
            def lastCommit = changeSet.getItems()[0]
            committerName = lastCommit.getAuthor().getFullName()
            commitId = lastCommit.getCommitId().take(7) // 7 תווים ראשונים של ה-Hash
        }
    }     
    echo "SCM Trigger detected. Committer: ${committerName}, Commit ID: ${commitId}"
    
    // define vars into the pipeline environment 
    env.SCM_COMMITTER = committerName
    env.SCM_COMMIT_ID = commitId
}