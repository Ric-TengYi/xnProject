from acceptance_aggregate_helper import REPORT_DATE, run_aggregate

REPORT_STEM = f"phase86_pc_remaining_full_acceptance_regression_{REPORT_DATE}"

PHASE_SCRIPTS = [
    "phase78_business_core_acceptance_regression.py",
    "phase79_project_config_acceptance_regression.py",
    "phase80_governance_logs_acceptance_regression.py",
    "phase81_alerts_security_acceptance_regression.py",
    "phase82_risk_model_acceptance_regression.py",
    "phase83_vehicle_safety_acceptance_regression.py",
    "phase84_fleet_operations_acceptance_regression.py",
    "phase85_fleet_finance_reports_acceptance_regression.py",
]


if __name__ == "__main__":
    run_aggregate(REPORT_STEM, PHASE_SCRIPTS)
