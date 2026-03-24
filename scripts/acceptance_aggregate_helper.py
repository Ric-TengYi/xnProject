import json
import subprocess
import sys
from pathlib import Path

REPORT_DATE = "2026-03-23"
REPORT_DIR = Path("docs/test-reports")


def load_phase_summary(script_name: str):
    phase_name = script_name.removesuffix(".py")
    report_path = REPORT_DIR / f"{phase_name}_{REPORT_DATE}.json"
    if not report_path.exists():
        return None
    return json.loads(report_path.read_text(encoding="utf-8"))


def run_aggregate(report_stem: str, phase_scripts: list[str]):
    report_dir = REPORT_DIR
    json_path = report_dir / f"{report_stem}.json"
    md_path = report_dir / f"{report_stem}.md"
    report_dir.mkdir(parents=True, exist_ok=True)

    aggregate = []
    total_passed = 0
    total_failed = 0

    for script_name in phase_scripts:
      script_path = Path("scripts") / script_name
      print(f"== Running {script_name} ==")
      completed = subprocess.run(
          [sys.executable, str(script_path)],
          capture_output=True,
          text=True,
          timeout=1800,
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
        "report": report_stem,
        "date": REPORT_DATE,
        "phaseCount": len(phase_scripts),
        "passed": total_passed,
        "failed": total_failed,
        "results": aggregate,
    }
    json_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

    lines = [
        f"# {report_stem}",
        "",
        f"- 日期：{REPORT_DATE}",
        f"- 阶段数：{len(phase_scripts)}",
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
    md_path.write_text("\n".join(lines), encoding="utf-8")

    print(json.dumps({"passed": total_passed, "failed": total_failed}, ensure_ascii=False))
    if total_failed > 0:
        sys.exit(1)
