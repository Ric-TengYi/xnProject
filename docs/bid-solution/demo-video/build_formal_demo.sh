#!/bin/zsh
set -euo pipefail

ROOT_DIR="/Users/tengyi/work/openclaw/workspace/xnProject"
WORK_DIR="$ROOT_DIR/docs/bid-solution/demo-video"
OUTPUT_DIR="$WORK_DIR/output"
AUDIO_TXT="$WORK_DIR/02_迎春配音文本.txt"
AUDIO_AIFF="$OUTPUT_DIR/迎春正式演示配音.wav"
AUDIO_M4A="$OUTPUT_DIR/迎春正式演示配音.m4a"
RAW_VIDEO="$OUTPUT_DIR/formal-demo-raw.mp4"
FINAL_VIDEO="$OUTPUT_DIR/迎春-渣土平台正式演示.mp4"
LOGIN_JSON="$OUTPUT_DIR/pc-login.json"
FRAMES_DIR="$WORK_DIR/frames"
SLIDESHOW_TXT="$OUTPUT_DIR/slideshow.txt"
VOICE_PARTS_DIR="$OUTPUT_DIR/voice-parts"
VOICE_LIST_TXT="$OUTPUT_DIR/voice-parts.txt"
VOICE_PARAGRAPHS_TXT="$OUTPUT_DIR/voice-paragraphs.txt"
SILENCE_SHORT_WAV="$OUTPUT_DIR/silence-short.wav"
SILENCE_LONG_WAV="$OUTPUT_DIR/silence-long.wav"

VOICE_NAME="Flo (中文（中国大陆）)"
VOICE_RATES=(106 108 110 108 112 110 108 110 108 108 110 108 106 106 108 110 110 112 110 106 102)
DEFAULT_TOTAL_PARAGRAPHS=21

mkdir -p "$OUTPUT_DIR"
mkdir -p "$VOICE_PARTS_DIR"

cd "$ROOT_DIR"

echo "[1/4] 生成迎春配音"

setopt local_options null_glob
rm -f "$VOICE_PARTS_DIR"/* "$VOICE_LIST_TXT" "$VOICE_PARAGRAPHS_TXT" "$SILENCE_SHORT_WAV" "$SILENCE_LONG_WAV"

awk 'BEGIN{RS=""; ORS="\n__PARA__\n"} {gsub(/\n+/, " "); gsub(/[[:space:]]+/, " "); print}' "$AUDIO_TXT" > "$VOICE_PARAGRAPHS_TXT"

ffmpeg -hide_banner -y -f lavfi -t 0.34 -i anullsrc=r=44100:cl=mono "$SILENCE_SHORT_WAV" > /dev/null 2>&1
ffmpeg -hide_banner -y -f lavfi -t 0.72 -i anullsrc=r=44100:cl=mono "$SILENCE_LONG_WAV" > /dev/null 2>&1

if [[ -n "${DASHSCOPE_API_KEY:-}" ]]; then
  node "$WORK_DIR/generate_bailian_narration.mjs" "$AUDIO_TXT" "$VOICE_PARTS_DIR"
else
  paragraph_index=1
  current_paragraph=""
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" == "__PARA__" ]]; then
      [[ -z "$current_paragraph" ]] && continue
      rate=${VOICE_RATES[$paragraph_index]}
      raw_part="$VOICE_PARTS_DIR/part-${paragraph_index}.aiff"
      clean_part="$VOICE_PARTS_DIR/part-${paragraph_index}.wav"
      say -v "$VOICE_NAME" -r "$rate" "$current_paragraph" -o "$raw_part"
      ffmpeg -hide_banner -y \
        -i "$raw_part" \
        -af "highpass=f=70,lowpass=f=6100,equalizer=f=170:t=q:w=1.0:g=1.0,equalizer=f=2400:t=q:w=1.0:g=-1.4,equalizer=f=3600:t=q:w=1.0:g=-2.1,equalizer=f=5200:t=q:w=1.0:g=-1.4,acompressor=threshold=0.18:ratio=1.45:attack=24:release=240:makeup=1.1" \
        -ar 44100 \
        -ac 1 \
        "$clean_part" > /dev/null 2>&1
      current_paragraph=""
      paragraph_index=$((paragraph_index + 1))
    else
      current_paragraph="$line"
    fi
  done < "$VOICE_PARAGRAPHS_TXT"
fi

: > "$VOICE_LIST_TXT"
TOTAL_PARAGRAPHS=$(find "$VOICE_PARTS_DIR" -maxdepth 1 -name 'part-*.wav' | wc -l | tr -d ' ')
if [[ "$TOTAL_PARAGRAPHS" == "0" ]]; then
  TOTAL_PARAGRAPHS=$DEFAULT_TOTAL_PARAGRAPHS
fi

for ((paragraph_index=1; paragraph_index<=TOTAL_PARAGRAPHS; paragraph_index++)); do
  clean_part="$VOICE_PARTS_DIR/part-${paragraph_index}.wav"
  printf "file '%s'\n" "$clean_part" >> "$VOICE_LIST_TXT"
  if [[ $paragraph_index -lt $TOTAL_PARAGRAPHS ]]; then
    case "$paragraph_index" in
      1|5|8|12|13|19|20)
        printf "file '%s'\n" "$SILENCE_LONG_WAV" >> "$VOICE_LIST_TXT"
        ;;
      *)
        printf "file '%s'\n" "$SILENCE_SHORT_WAV" >> "$VOICE_LIST_TXT"
        ;;
    esac
  fi
done

ffmpeg -hide_banner -y \
  -f concat -safe 0 -i "$VOICE_LIST_TXT" \
  -ar 44100 \
  -ac 1 \
  "$AUDIO_AIFF"

echo "[2/4] 转换配音格式"
ffmpeg -hide_banner -y \
  -i "$AUDIO_AIFF" \
  -af "loudnorm=I=-18:TP=-2.5:LRA=10" \
  -ar 44100 \
  -ac 1 \
  -c:a aac \
  "$AUDIO_M4A"

if [[ "${SKIP_CAPTURE:-0}" != "1" ]]; then
  echo "[3/4] 录制浏览器演示"
  curl -sS -o "$LOGIN_JSON" http://127.0.0.1:8090/api/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"admin","loginType":"ACCOUNT","tenantId":"1"}'

  DEMO_TOKEN=$(jq -r '.data.token' "$LOGIN_JSON")

  curl -sS http://127.0.0.1:8090/api/platform-integrations/gov/sync \
    -H "Authorization: Bearer $DEMO_TOKEN" \
    -H 'Content-Type: application/json' \
    -d '{"syncMode":"MANUAL","includeTransportPermits":true,"projectId":1,"contractId":51,"siteId":1,"remark":"正式演示视频录制前同步"}' > /dev/null

  node "$WORK_DIR/capture_demo_frames.mjs"

  cat > "$SLIDESHOW_TXT" <<EOF
file '$FRAMES_DIR/01-dashboard.png'
duration 40
file '$FRAMES_DIR/02-projects.png'
duration 30
file '$FRAMES_DIR/03-project-detail.png'
duration 34
file '$FRAMES_DIR/04-platform-integrations.png'
duration 46
file '$FRAMES_DIR/05-project-permits.png'
duration 34
file '$FRAMES_DIR/06-permits-management.png'
duration 34
file '$FRAMES_DIR/07-contract-detail.png'
duration 36
file '$FRAMES_DIR/08-settlements.png'
duration 30
file '$FRAMES_DIR/09-vehicle-tracking.png'
duration 36
file '$FRAMES_DIR/10-alerts.png'
duration 30
file '$FRAMES_DIR/11-events.png'
duration 32
file '$FRAMES_DIR/12-mobile-login.png'
duration 22
file '$FRAMES_DIR/13-mobile-home.png'
duration 32
file '$FRAMES_DIR/14-mobile-punch-in.png'
duration 32
file '$FRAMES_DIR/15-mobile-disposal.png'
duration 32
file '$FRAMES_DIR/16-mobile-event-report.png'
duration 32
file '$FRAMES_DIR/17-mobile-vehicle.png'
duration 28
file '$FRAMES_DIR/17-mobile-vehicle.png'
EOF

  ffmpeg -hide_banner -y \
    -f concat -safe 0 -i "$SLIDESHOW_TXT" \
    -vf "scale=1600:900:force_original_aspect_ratio=decrease,pad=1600:900:(ow-iw)/2:(oh-ih)/2,format=yuv420p" \
    -r 25 \
    "$RAW_VIDEO"
else
  echo "[3/4] 复用已有演示画面"
fi

echo "[4/4] 合成最终视频"
ffmpeg -hide_banner -y \
  -i "$RAW_VIDEO" \
  -i "$AUDIO_M4A" \
  -c:v libx264 \
  -pix_fmt yuv420p \
  -c:a aac \
  -shortest \
  "$FINAL_VIDEO"

echo "$FINAL_VIDEO"
