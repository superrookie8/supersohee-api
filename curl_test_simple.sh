#!/bin/bash

# 사용법: ./curl_test_simple.sh [API_URL]
# 예시: ./curl_test_simple.sh http://localhost:8080
# 예시: ./curl_test_simple.sh https://your-api-domain.com

API_URL="${1:-http://localhost:8080}"

echo "=== 스케줄 API 테스트 (간단 버전) ==="
echo "API URL: $API_URL"
echo ""

# 1. 전체 스케줄 조회
echo "1. 전체 스케줄 조회:"
RESPONSE=$(curl -s "$API_URL/api/schedules")
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""
echo "---"
echo ""

# 2. 첫 번째 스케줄의 날짜 필드만 확인
echo "2. 첫 번째 스케줄의 날짜 필드:"
echo "$RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data and len(data) > 0:
        first = data[0]
        print(f\"ID: {first.get('id', 'N/A')}\")
        print(f\"Title: {first.get('title', 'N/A')}\")
        print(f\"startDateTime: {first.get('startDateTime', 'N/A')}\")
        print(f\"endDateTime: {first.get('endDateTime', 'N/A')}\")
    else:
        print('스케줄이 없습니다.')
except Exception as e:
    print(f'오류: {e}')
    print('전체 응답:')
    print(sys.stdin.read())
" 2>/dev/null || echo "Python을 사용할 수 없습니다. 위의 전체 응답을 확인하세요."

