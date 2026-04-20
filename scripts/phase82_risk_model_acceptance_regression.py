from acceptance_aggregate_helper import REPORT_DATE, run_aggregate

REPORT_STEM = f"phase82_risk_model_acceptance_regression_{REPORT_DATE}"

PHASE_SCRIPTS = [
    "phase69_alerts_events_unified_regression.py",
]


if __name__ == "__main__":
    run_aggregate(REPORT_STEM, PHASE_SCRIPTS)
