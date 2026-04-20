import json
import subprocess
import sys
from pathlib import Path

REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase75_final_acceptance_regression_{REPORT_DATE}"
REPORT_DIR = Path("docs/test-reports")
JSON_PATH = REPORT_DIR / f"{REPORT_STEM}.json"
MD_PATH = REPORT_DIR / f"{REPORT_STEM}.md"

PHASE_SCRIPTS = [
    "phase61_plan5_pending_acceptance_regression.py",
    "phase62_vehicle_master_enhancement_regression.py",
    "phase63_vehicle_ops_cleanup_regression.py",
    "phase64_security_ledger_detail_regression.py",
    "phase65_vehicle_maintenance_detail_regression.py",
    "phase66_vehicle_repairs_detail_regression.py",
    "phase67_security_related_profile_regression.py",
    "phase68_vehicle_fleet_unified_regression.py",
    "phase69_alerts_events_unified_regression.py",
    "phase70_governance_unified_regression.py",
    "phase71_business_master_regression.py",
    "phase72_contract_settlement_regression.py",
    "phase73_project_runtime_regression.py",
    "phase74_fleet_core_regression.py",
]


def load_phase_summary(script_name: str):
    phase_name = script_name.removesuffix(".py")
    report_path = REPORT_DIR / f"{phase_name}_{REPORT_DATE}.json"
    if not report_path.exists():
        return None
    return json.loads(report_path.read_text(encoding="utf-8"))


def main():
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    aggregate = []
    total_passed = 0
    total_failed = 0

    for script_name in PHASE_SCRIPTS:
        script_path = Path("scripts") / script_name
        print(f"== Running {script_name} ==")
        completed = subprocess.run(
            [sys.executable, str(script_path)],
            capture_output=True,
            text=True,
            timeout=900,
        )
        phase_summary = load_phase_summary(script_name)
        if phase_summary is None:
            aggregate.append(
                {
                    "script": script_name,
                    "status": "FAIL",
                    "passed": 0,
                    "failed": 1,
                    "detail": "未生成阶段报告",
                    "stdout_tail": completed.stdout[-1200:],
                    "stderr_tail": completed.stderr[-1200:],
                }
            )
            total_failed += 1
            continue

        passed = int(phase_summary.get("passed", 0))
        failed = int(phase_summary.get("failed", 0))
        total_passed += passed
        total_failed += failed
        aggregate.append(
            {
                "script": script_name,
                "status": "PASS" if completed.returncode == 0 and failed == 0 else "FAIL",
                "passed": passed,
                "failed": failed,
                "detail": f"returncode={completed.returncode}",
                "stdout_tail": completed.stdout[-1200:],
                "stderr_tail": completed.stderr[-1200:],
            }
        )

    summary = {
        "report": REPORT_STEM,
        "date": REPORT_DATE,
        "phaseCount": len(PHASE_SCRIPTS),
        "passed": total_passed,
        "failed": total_failed,
        "results": aggregate,
    }
    JSON_PATH.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

    lines = [
        f"# {REPORT_STEM}",
        "",
        f"- 日期：{REPORT_DATE}",
        f"- 阶段数：{len(PHASE_SCRIPTS)}",
        f"- 用例通过：{total_passed}",
        f"- 用例失败：{total_failed}",
        "",
        "## 阶段结果",
        "",
        "| 阶段脚本 | 状态 | 通过 | 失败 | 说明 |",
        "|---|---|---|---|---|",
    ]
    for item in aggregate:
        lines.append(
            f"| {item['script']} | {item['status']} | {item['passed']} | {item['failed']} | {item['detail']} |"
        )
    MD_PATH.write_text("\n".join(lines), encoding="utf-8")

    print(json.dumps({"passed": total_passed, "failed": total_failed}, ensure_ascii=False))
    if total_failed > 0:
        sys.exit(1)


if __name__ == "__main__":
    main()
