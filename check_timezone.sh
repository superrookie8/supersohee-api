#!/bin/bash
echo "=== 배포된 서버 타임존 확인 ==="
curl -s "https://port-0-supersohee-api-mka53i531b9feda0.sel3.cloudtype.app/api/schedules" | python3 << 'PYTHON'
import sys, json
try:
    data = json.load(sys.stdin)
    if data and len(data) > 0:
        first = data[0]
        print(f"Title: {first['title']}")
        print(f"startDateTime: {first['startDateTime']}")
        print(f"endDateTime: {first['endDateTime']}")
        print()
        if '+09:00' in first.get('startDateTime', '') or '+09:00' in first.get('endDateTime', ''):
            print('✅ 타임존 정보가 포함되어 있습니다! (+09:00)')
        else:
            print('❌ 타임존 정보가 없습니다.')
            print('   빌드/배포가 제대로 되지 않았을 수 있습니다.')
    else:
        print('스케줄이 없습니다.')
except Exception as e:
    print(f'오류: {e}')
PYTHON
