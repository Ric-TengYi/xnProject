from acceptance_aggregate_helper import REPORT_DATE, run_aggregate

REPORT_STEM = f"phase84_fleet_operations_acceptance_regression_{REPORT_DATE}"

PHASE_SCRIPTS = [
    "phase68_vehicle_fleet_unified_regression.py",
    "phase74_fleet_core_regression.py",
]


if __name__ == "__main__":
    run_aggregate(REPORT_STEM, PHASE_SCRIPTS)
