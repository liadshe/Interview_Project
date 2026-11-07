def call() {
    echo "Starting prepareEnv: Extracting SCM variables..."

    // default values
    def committerName = "Unknown/Manual Trigger"
    def commitId = "N/A"
    def commitMsg = "N/A"

    // collect all entries from all change sets 
    def entries = []
    for (cs in currentBuild.changeSets) {
        for (item in cs.items) {
            entries << item
        }
    }

    if (entries) {
        // pick the most recent commit by timestamp in case of multiple commits in a single push
        def last = entries.max { it.timestamp }
        committerName = last.author.fullName
        commitId = last.commitId.take(7)
        commitMsg = last.msg
    }

    // trigger cause (push / manual / etc.)
    def cause = currentBuild.getBuildCauses()?.collect { it.shortDescription }?.join(', ') ?: 'unknown'

    echo "SCM Trigger detected. Committer: ${committerName}, Commit ID: ${commitId}, Cause: ${cause}"

    env.SCM_COMMITTER = committerName
    env.SCM_COMMIT_ID = commitId
    env.SCM_COMMIT_MSG = commitMsg
    env.BUILD_TRIGGER_CAUSE = cause
}