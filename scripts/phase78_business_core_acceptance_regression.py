from acceptance_aggregate_helper import REPORT_DATE, run_aggregate

REPORT_STEM = f"phase78_business_core_acceptance_regression_{REPORT_DATE}"

PHASE_SCRIPTS = [
    "phase71_business_master_regression.py",
    "phase72_contract_settlement_regression.py",
]


if __name__ == "__main__":
    run_aggregate(REPORT_STEM, PHASE_SCRIPTS)
