#!/bin/bash

# API 엔드포인트 (로컬 또는 배포된 서버 주소로 변경)
API_URL="${1:-http://localhost:8080}"

echo "=== 스케줄 API 테스트 ==="
echo "API URL: $API_URL"
echo ""

# jq가 있는지 확인
if command -v jq &> /dev/null; then
    USE_JQ=true
else
    USE_JQ=false
    echo "⚠️  jq가 설치되어 있지 않습니다. JSON 포맷팅 없이 출력합니다."
    echo "   jq 설치: brew install jq (macOS)"
    echo ""
fi

# 1. 전체 스케줄 조회 (날짜 범위 없음)
echo "1. 전체 스케줄 조회:"
if [ "$USE_JQ" = true ]; then
    curl -s "$API_URL/api/schedules" | jq '.' | head -50
else
    curl -s "$API_URL/api/schedules" | python3 -m json.tool 2>/dev/null || curl -s "$API_URL/api/schedules"
fi
echo ""
echo "---"
echo ""

# 2. 날짜 범위로 스케줄 조회 (2026년 1월)
echo "2. 2026년 1월 스케줄 조회:"
if [ "$USE_JQ" = true ]; then
    curl -s "$API_URL/api/schedules?start=2026-01-01T00:00:00&end=2026-01-31T23:59:59" | jq '.' | head -100
else
    curl -s "$API_URL/api/schedules?start=2026-01-01T00:00:00&end=2026-01-31T23:59:59" | python3 -m json.tool 2>/dev/null || curl -s "$API_URL/api/schedules?start=2026-01-01T00:00:00&end=2026-01-31T23:59:59"
fi
echo ""
echo "---"
echo ""

# 3. 첫 번째 스케줄의 날짜 형식 확인
echo "3. 첫 번째 스케줄 상세 (날짜 형식 확인):"
if [ "$USE_JQ" = true ]; then
    FIRST_ID=$(curl -s "$API_URL/api/schedules" | jq -r '.[0].id // empty')
    if [ -n "$FIRST_ID" ]; then
        curl -s "$API_URL/api/schedules/$FIRST_ID" | jq '{id, title, startDateTime, endDateTime, createdAt, updatedAt}'
    else
        echo "스케줄이 없습니다."
    fi
else
    echo "첫 번째 스케줄의 날짜 필드만 확인:"
    curl -s "$API_URL/api/schedules" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data and len(data) > 0:
        first = data[0]
        print(f\"ID: {first.get('id', 'N/A')}\")
        print(f\"Title: {first.get('title', 'N/A')}\")
        print(f\"startDateTime: {first.get('startDateTime', 'N/A')}\")
        print(f\"endDateTime: {first.get('endDateTime', 'N/A')}\")
        print(f\"createdAt: {first.get('createdAt', 'N/A')}\")
        print(f\"updatedAt: {first.get('updatedAt', 'N/A')}\")
    else:
        print('스케줄이 없습니다.')
except:
    print('JSON 파싱 실패')
" 2>/dev/null || echo "Python을 사용할 수 없습니다. 전체 응답을 확인하세요."
fi

