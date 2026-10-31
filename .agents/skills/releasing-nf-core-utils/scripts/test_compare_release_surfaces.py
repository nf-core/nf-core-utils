"""Read-only comparison contract; run with python3 -m unittest discover -s <scripts>."""

import contextlib
import io
import json
import unittest
from unittest.mock import patch

import compare_release_surfaces


class ReleaseComparisonTest(unittest.TestCase):
    def test_published_versions_and_asymmetric_differences(self):
        registry = {
            "plugin": {
                "releases": [
                    {"version": "0.6.0", "status": "PUBLISHED"},
                    {"version": "0.7.0", "status": "PENDING"},
                    {"version": "0.4.0", "status": "PUBLISHED"},
                ]
            }
        }
        output = io.StringIO()
        with (
            patch("subprocess.check_output", return_value="0.6.0\n0.7.0\n0.1.0\n"),
            patch(
                "urllib.request.urlopen", return_value=io.StringIO(json.dumps(registry))
            ),
            contextlib.redirect_stdout(output),
        ):
            self.assertEqual(compare_release_surfaces.main(), 0)
        self.assertEqual(
            output.getvalue(),
            (
                "GitHub: 0.1.0, 0.6.0, 0.7.0\n"
                "Registry (PUBLISHED): 0.4.0, 0.6.0\n"
                "Registry (not PUBLISHED): 0.7.0\n"
                "Missing from registry: 0.1.0, 0.7.0\n"
                "Missing from GitHub: 0.4.0\n"
            ),
        )

    def test_failure_never_reports_an_empty_registry_as_success(self):
        for response in (
            "not json",
            "{}",
            '{"plugin":{"releases":[{"version":"0.7.0"}]}}',
        ):
            with (
                self.subTest(response=response),
                patch("subprocess.check_output", return_value="0.6.0\n"),
                patch("urllib.request.urlopen", return_value=io.StringIO(response)),
                contextlib.redirect_stdout(io.StringIO()) as output,
                contextlib.redirect_stderr(io.StringIO()) as errors,
            ):
                self.assertEqual(compare_release_surfaces.main(), 1)
                self.assertEqual(output.getvalue(), "")
                self.assertIn("Release comparison failed", errors.getvalue())

        with (
            patch("subprocess.check_output", side_effect=FileNotFoundError),
            contextlib.redirect_stderr(io.StringIO()),
        ):
            self.assertEqual(compare_release_surfaces.main(), 1)


if __name__ == "__main__":
    unittest.main()
