from acceptance_aggregate_helper import REPORT_DATE, run_aggregate

REPORT_STEM = f"phase81_alerts_security_acceptance_regression_{REPORT_DATE}"

PHASE_SCRIPTS = [
    "phase64_security_ledger_detail_regression.py",
    "phase67_security_related_profile_regression.py",
    "phase69_alerts_events_unified_regression.py",
    "phase76_message_linkage_regression.py",
]


if __name__ == "__main__":
    run_aggregate(REPORT_STEM, PHASE_SCRIPTS)
