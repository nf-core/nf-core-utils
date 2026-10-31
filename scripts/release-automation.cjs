// Called only by the disabled workflow proposal; never executes on import.
module.exports = async function dispatchRelease({ github, context }) {
  const owner = "nf-core";
  const repo = "nf-core-utils";
  const source = context.payload.workflow_run;
  if (
    context.eventName !== "workflow_run" ||
    context.repo.owner !== owner ||
    context.repo.repo !== repo ||
    source?.repository?.full_name !== `${owner}/${repo}` ||
    source?.head_repository?.full_name !== `${owner}/${repo}` ||
    source?.event !== "push" ||
    source?.head_branch !== "main" ||
    source?.status !== "completed" ||
    source?.conclusion !== "success" ||
    source?.path !== ".github/workflows/validation.yml" ||
    !/^[a-f0-9]{40}$/.test(source?.head_sha)
  ) {
    throw new Error(
      "Expected successful main-push validation in nf-core/nf-core-utils",
    );
  }
  const sha = source.head_sha;
  async function requireCurrentMain() {
    const { data } = await github.rest.git.getRef({
      owner,
      repo,
      ref: "heads/main",
    });
    if (data.object.sha !== sha) throw new Error("Candidate is no longer main");
  }
  await requireCurrentMain();

  // Query each workflow separately: a passing workflow with a similar display
  // name, a pull request run, or an older successful attempt is not sufficient.
  for (const workflow_id of ["build.yml", "validation.yml"]) {
    const runs = await github.paginate(github.rest.actions.listWorkflowRuns, {
      owner,
      repo,
      workflow_id,
      head_sha: sha,
      branch: "main",
      event: "push",
      per_page: 100,
    });
    const latest = runs.sort((a, b) => b.id - a.id)[0];
    if (
      !latest ||
      latest.head_sha !== sha ||
      latest.head_branch !== "main" ||
      latest.event !== "push" ||
      latest.status !== "completed" ||
      latest.conclusion !== "success"
    ) {
      throw new Error(`Latest ${workflow_id} run must succeed at ${sha}`);
    }
  }

  const { data: build } = await github.rest.repos.getContent({
    owner,
    repo,
    path: "build.gradle",
    ref: sha,
  });
  if (build.encoding !== "base64" || typeof build.content !== "string") {
    throw new Error("Expected build.gradle content");
  }
  const versions = [
    ...Buffer.from(build.content, "base64")
      .toString("utf8")
      .matchAll(
        /^version = '((?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*))'$/gm,
      ),
  ];
  if (versions.length !== 1)
    throw new Error("Expected one stable release version in build.gradle");
  const version = versions[0][1];

  const previous = await github.paginate(github.rest.actions.listWorkflowRuns, {
    owner,
    repo,
    workflow_id: "release.yml",
    head_sha: sha,
    per_page: 100,
  });
  if (previous.length) {
    throw new Error(
      "Release already attempted at this commit; use README partial-release recovery",
    );
  }
  await requireCurrentMain();
  // The receiver MUST compare expected_sha to github.sha before any publishing
  // credentials. Checking main above cannot close the dispatch race by itself.
  await github.rest.actions.createWorkflowDispatch({
    owner,
    repo,
    workflow_id: "release.yml",
    ref: "main",
    inputs: { version, expected_sha: sha },
  });
  return {
    version,
    expected_sha: sha,
    status: "dispatch accepted; publication not verified",
  };
};
