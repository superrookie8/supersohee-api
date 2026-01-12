#!/bin/bash

echo "=== 로컬 서버 테스트 ==="
echo "로컬 서버가 실행 중이어야 합니다 (http://localhost:8080)"
echo ""

# 로컬 서버 테스트
curl -s "http://localhost:8080/api/schedules" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data and len(data) > 0:
        print('✅ API 응답 성공')
        print(f'총 {len(data)}개의 스케줄')
        print()
        print('첫 번째 스케줄:')
        first = data[0]
        print(f\"Title: {first['title']}\")
        print(f\"startDateTime: {first['startDateTime']}\")
        print(f\"endDateTime: {first['endDateTime']}\")
        print()
        
        # 타임존 정보 확인
        if '+09:00' in first.get('startDateTime', '') or '+09:00' in first.get('endDateTime', ''):
            print('✅ 타임존 정보가 포함되어 있습니다! (+09:00)')
        else:
            print('❌ 타임존 정보가 없습니다. 수정이 필요합니다.')
    else:
        print('스케줄이 없습니다.')
except Exception as e:
    print(f'오류: {e}')
    print('로컬 서버가 실행 중인지 확인하세요.')
"

