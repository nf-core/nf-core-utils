const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const dispatchRelease = require("./release-automation.cjs");

const sha = "a".repeat(40);
function fixture() {
  const calls = [];
  const source = {
    repository: { full_name: "nf-core/nf-core-utils" },
    head_repository: { full_name: "nf-core/nf-core-utils" },
    event: "push",
    head_branch: "main",
    head_sha: sha,
    status: "completed",
    conclusion: "success",
    path: ".github/workflows/validation.yml",
  };
  const context = {
    eventName: "workflow_run",
    repo: { owner: "nf-core", repo: "nf-core-utils" },
    payload: { workflow_run: source },
  };
  const state = {
    heads: [sha, sha],
    build: "version = '1.2.3'\n",
    runs: Object.fromEntries(
      ["build.yml", "validation.yml"].map((name) => [
        name,
        [
          {
            id: 20,
            head_sha: sha,
            head_branch: "main",
            event: "push",
            status: "completed",
            conclusion: "success",
          },
        ],
      ]),
    ),
  };
  state.runs["release.yml"] = [];
  const github = {
    rest: {
      git: {
        getRef: async () => ({
          data: { object: { sha: state.heads.shift() } },
        }),
      },
      repos: {
        getContent: async (request) => {
          assert.deepEqual(request, {
            owner: "nf-core",
            repo: "nf-core-utils",
            path: "build.gradle",
            ref: sha,
          });
          return {
            data: {
              encoding: "base64",
              content: Buffer.from(state.build).toString("base64"),
            },
          };
        },
      },
      actions: {
        listWorkflowRuns: () => {},
        createWorkflowDispatch: async (request) => {
          calls.push(request);
        },
      },
    },
    paginate: async (_method, request) => {
      assert.equal(request.head_sha, sha);
      if (request.workflow_id !== "release.yml") {
        assert.equal(request.event, "push");
        assert.equal(request.branch, "main");
      }
      return state.runs[request.workflow_id];
    },
  };
  return { context, github, state, calls, source };
}

test("only dispatches guarded release on main, with exact version and SHA and no bypass inputs", async () => {
  const f = fixture();
  assert.deepEqual(await dispatchRelease(f), {
    version: "1.2.3",
    expected_sha: sha,
    status: "dispatch accepted; publication not verified",
  });
  assert.deepEqual(f.calls, [
    {
      owner: "nf-core",
      repo: "nf-core-utils",
      workflow_id: "release.yml",
      ref: "main",
      inputs: { version: "1.2.3", expected_sha: sha },
    },
  ]);
});

test("rejects untrusted events, forks, branches and unsuccessful validation without writes", async () => {
  for (const change of [
    (f) => {
      f.context.eventName = "push";
    },
    (f) => {
      f.context.repo.owner = "someone";
    },
    (f) => {
      f.source.repository.full_name = "someone/fork";
    },
    (f) => {
      f.source.head_repository.full_name = "someone/fork";
    },
    (f) => {
      f.source.event = "pull_request";
    },
    (f) => {
      f.source.head_branch = "other";
    },
    (f) => {
      f.source.status = "in_progress";
    },
    (f) => {
      f.source.conclusion = "failure";
    },
    (f) => {
      f.source.path = ".github/workflows/other.yml";
    },
    (f) => {
      f.source.head_sha = "main";
    },
  ]) {
    const f = fixture();
    change(f);
    await assert.rejects(dispatchRelease(f), /Expected successful/);
    assert.equal(f.calls.length, 0);
  }
});

test("requires newest successful push CI for both exact workflows at the candidate SHA", async () => {
  for (const workflow of ["build.yml", "validation.yml"]) {
    for (const mutation of [
      (runs) => [],
      (runs) => [
        { ...runs[0], id: 10 },
        { ...runs[0], conclusion: "failure" },
      ],
      (runs) => [{ ...runs[0], status: "in_progress", conclusion: null }],
      (runs) => [{ ...runs[0], head_sha: "b".repeat(40) }],
      (runs) => [{ ...runs[0], head_branch: "other" }],
      (runs) => [{ ...runs[0], event: "pull_request" }],
    ]) {
      const f = fixture();
      f.state.runs[workflow] = mutation(f.state.runs[workflow]);
      await assert.rejects(dispatchRelease(f), /Latest .* run must succeed/);
      assert.equal(f.calls.length, 0);
    }
  }
});

test("refuses stale main, including movement during checks", async () => {
  for (const heads of [["b".repeat(40)], [sha, "b".repeat(40)]]) {
    const f = fixture();
    f.state.heads = heads;
    await assert.rejects(dispatchRelease(f), /no longer main/);
    assert.equal(f.calls.length, 0);
  }
});

test("never retries an earlier release attempt, even one that failed", async () => {
  const f = fixture();
  f.state.runs["release.yml"] = [{ conclusion: "failure" }];
  await assert.rejects(dispatchRelease(f), /partial-release recovery/);
  assert.equal(f.calls.length, 0);
});

test("requires exactly one stable version from candidate content", async () => {
  for (const build of [
    "version = '1.2.3-edge'",
    "version = '01.2.3'",
    "",
    "version = '1.2.3'\nversion = '2.0.0'\n",
  ]) {
    const f = fixture();
    f.state.build = build;
    await assert.rejects(dispatchRelease(f), /one stable release version/);
    assert.equal(f.calls.length, 0);
  }
});

test("API errors fail closed; ambiguous dispatch failures are not retried", async () => {
  const f = fixture();
  f.github.paginate = async () => {
    throw new Error("403");
  };
  await assert.rejects(dispatchRelease(f), /403/);
  assert.equal(f.calls.length, 0);
  const dispatch = fixture();
  dispatch.github.rest.actions.createWorkflowDispatch = async (request) => {
    dispatch.calls.push(request);
    throw new Error("connection lost after request");
  };
  await assert.rejects(dispatchRelease(dispatch), /connection lost/);
  assert.equal(dispatch.calls.length, 1);
});

test("workflow proposal is unregistered, unconditionally disabled and approval-scoped", () => {
  const workflow = fs.readFileSync(
    "docs/release-automation/entrypoint.yml.disabled",
    "utf8",
  );
  assert.match(workflow, /if: \$\{\{ false \}\}/);
  assert.match(workflow, /environment: release-automation-approval/);
  assert.match(workflow, /secrets.RELEASE_DISPATCH_TOKEN/);
  assert.match(workflow, /retries: 0/);
  assert.equal(
    fs.existsSync(".github/workflows/release-automation.yml"),
    false,
  );
});
