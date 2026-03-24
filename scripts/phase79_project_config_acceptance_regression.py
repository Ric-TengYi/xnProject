from acceptance_aggregate_helper import REPORT_DATE, run_aggregate

REPORT_STEM = f"phase79_project_config_acceptance_regression_{REPORT_DATE}"

PHASE_SCRIPTS = [
    "phase73_project_runtime_regression.py",
]


if __name__ == "__main__":
    run_aggregate(REPORT_STEM, PHASE_SCRIPTS)
