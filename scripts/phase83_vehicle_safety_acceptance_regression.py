from acceptance_aggregate_helper import REPORT_DATE, run_aggregate

REPORT_STEM = f"phase83_vehicle_safety_acceptance_regression_{REPORT_DATE}"

PHASE_SCRIPTS = [
    "phase62_vehicle_master_enhancement_regression.py",
    "phase63_vehicle_ops_cleanup_regression.py",
    "phase64_security_ledger_detail_regression.py",
    "phase65_vehicle_maintenance_detail_regression.py",
    "phase66_vehicle_repairs_detail_regression.py",
    "phase67_security_related_profile_regression.py",
    "phase68_vehicle_fleet_unified_regression.py",
]


if __name__ == "__main__":
    run_aggregate(REPORT_STEM, PHASE_SCRIPTS)
