from acceptance_aggregate_helper import REPORT_DATE, run_aggregate

REPORT_STEM = f"phase80_governance_logs_acceptance_regression_{REPORT_DATE}"

PHASE_SCRIPTS = [
    "phase70_governance_unified_regression.py",
    "phase76_message_linkage_regression.py",
    "phase77_contract_message_jump_regression.py",
]


if __name__ == "__main__":
    run_aggregate(REPORT_STEM, PHASE_SCRIPTS)
