"""Release version contract: python3 -m unittest discover -s scripts/release -v."""

import contextlib
import io
import unittest
from pathlib import Path

import release_version as rv


class ReleaseVersionTest(unittest.TestCase):
    def test_stable_versions_are_not_prereleases(self):
        for version in ["0.8.0", "1.0.0", "10.20.30"]:
            with self.subTest(version=version):
                self.assertFalse(rv.is_prerelease(version))

    def test_release_candidates_are_prereleases(self):
        for version in ["1.0.0-rc.1", "1.0.0-rc.12", "0.8.0-rc.3"]:
            with self.subTest(version=version):
                self.assertTrue(rv.is_prerelease(version))

    def test_everything_else_is_rejected(self):
        for version in [
            "",
            "1.0",
            "v1.0.0",
            "01.0.0",
            "1.0.0-rc.0",
            "1.0.0-rc.01",
            "1.0.0-rc",
            "1.0.0-rc.1.2",
            "1.0.0-RC.1",
            "1.0.0-beta.1",
            "1.0.0-dev",
            "0.7.0-dev",
            "1.0.0+build",
            "1.0.0-rc.1+build",
            "1.0.0\n",
            "1.0.0 ",
            "1.0.0-rc.1\n1.0.0",
        ]:
            with self.subTest(version=version), self.assertRaises(ValueError):
                rv.is_prerelease(version)

    def run_main(self, *args):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = rv.main(["release_version.py", *args])
        return code, out.getvalue(), err.getvalue()

    def test_main_prints_github_output_line(self):
        self.assertEqual(self.run_main("1.0.0-rc.2"), (0, "prerelease=true\n", ""))
        self.assertEqual(self.run_main("0.8.0"), (0, "prerelease=false\n", ""))

    def test_main_fails_without_output_for_bad_input(self):
        code, out, err = self.run_main("1.0.0-beta.1")
        self.assertEqual((code, out), (1, ""))
        self.assertIn("::error::", err)
        self.assertEqual(self.run_main()[0], 2)


class WorkflowWiringTest(unittest.TestCase):
    """The pre-release branching lives in YAML shell, so pin its wiring textually."""

    workflow = (
        Path(__file__).resolve().parents[2] / ".github/workflows/release.yml"
    ).read_text()

    def test_version_check_uses_the_tested_helper(self):
        self.assertIn(
            'python3 scripts/release/release_version.py "$VERSION" >> "$GITHUB_OUTPUT"',
            self.workflow,
        )

    def test_prerelease_is_not_latest_and_skips_docs(self):
        self.assertIn(
            "if [[ \"$PRERELEASE\" == true ]]; then channel=--prerelease; else channel=--latest; fi",
            self.workflow,
        )
        self.assertIn('"$channel"', self.workflow)
        docs = self.workflow.split("- name: Dispatch documentation update")[1]
        self.assertIn(
            "if: steps.prerequisites.outputs.prerelease != 'true'",
            docs.split("- name:")[0],
        )


if __name__ == "__main__":
    unittest.main()
