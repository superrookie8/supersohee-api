# 아케이드 게임 점수 랭킹 API 문서

## 기본 정보

- **Base URL**: `https://your-api-domain.com/api/arcade` (개발 환경에 맞게 변경 필요)
- **Content-Type**: `application/json`
- **인증 방식**: JWT 토큰 (Authorization 헤더에 `Bearer {token}` 형식)

---

## 1. 점수 제출

게임 종료 후 점수를 제출합니다. 기존 최고 점수보다 높을 때만 업데이트됩니다.

### 엔드포인트

```
POST /api/arcade/score
```

### 인증

✅ **필수** (JWT 토큰 필요)

### 요청 헤더

```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### 요청 Body (JSON)

```json
{
  "score": 1234
}
```

### 요청 필드

| 필드  | 타입    | 필수 | 설명               |
| ----- | ------- | ---- | ------------------ |
| score | Integer | ✅   | 게임 점수 (0 이상) |

### 성공 응답 (200 OK)

```json
{
  "userId": "user123",
  "nickname": "플레이어1",
  "profileImageUrl": "https://example.com/profile.jpg",
  "bestScore": 1234,
  "rank": 5
}
```

### 응답 필드

| 필드            | 타입            | 설명                         |
| --------------- | --------------- | ---------------------------- |
| userId          | String          | 사용자 ID                    |
| nickname        | String          | 사용자 닉네임                |
| profileImageUrl | String          | 프로필 이미지 URL            |
| bestScore       | Integer         | 최고 점수                    |
| rank            | Integer \| null | 전체 랭킹 순위 (없으면 null) |

### 에러 응답

- **400 Bad Request**: 점수가 0 미만이거나 필수 필드 누락
- **401 Unauthorized**: 인증 토큰이 없거나 유효하지 않음

---

## 2. 내 최고 점수 조회

현재 로그인한 사용자의 최고 점수를 조회합니다.

### 엔드포인트

```
GET /api/arcade/my-score
```

### 인증

✅ **필수** (JWT 토큰 필요)

### 요청 헤더

```
Authorization: Bearer {JWT_TOKEN}
```

### 성공 응답 (200 OK)

```json
{
  "userId": "user123",
  "nickname": "플레이어1",
  "profileImageUrl": "https://example.com/profile.jpg",
  "bestScore": 1234,
  "rank": 5
}
```

### 응답 필드

| 필드            | 타입            | 설명                         |
| --------------- | --------------- | ---------------------------- |
| userId          | String          | 사용자 ID                    |
| nickname        | String          | 사용자 닉네임                |
| profileImageUrl | String          | 프로필 이미지 URL            |
| bestScore       | Integer \| null | 최고 점수 (없으면 null)      |
| rank            | Integer \| null | 전체 랭킹 순위 (없으면 null) |

### 에러 응답

- **401 Unauthorized**: 인증 토큰이 없거나 유효하지 않음

---

## 3. 랭킹 조회

전체 또는 상위 N명의 랭킹을 조회합니다.

### 엔드포인트

```
GET /api/arcade/ranking?limit=10
```

### 인증

❌ **선택** (인증 시 내 랭킹 정보 포함)

### 요청 헤더 (선택)

```
Authorization: Bearer {JWT_TOKEN}  // 내 랭킹 정보를 받으려면 필요
```

### 쿼리 파라미터

| 파라미터 | 타입    | 필수 | 설명                          |
| -------- | ------- | ---- | ----------------------------- |
| limit    | Integer | ❌   | 조회할 상위 N명 (없으면 전체) |

### 예시

```
GET /api/arcade/ranking          // 전체 랭킹
GET /api/arcade/ranking?limit=10 // 상위 10명
GET /api/arcade/ranking?limit=100 // 상위 100명
```

### 성공 응답 (200 OK)

```json
{
  "rankings": [
    {
      "rank": 1,
      "userId": "user456",
      "nickname": "최고수",
      "profileImageUrl": "https://example.com/profile2.jpg",
      "bestScore": 9999
    },
    {
      "rank": 2,
      "userId": "user789",
      "nickname": "2등",
      "profileImageUrl": "https://example.com/profile3.jpg",
      "bestScore": 8888
    }
  ],
  "totalCount": 150,
  "myRank": 5
}
```

### 응답 필드

| 필드                       | 타입            | 설명                          |
| -------------------------- | --------------- | ----------------------------- |
| rankings                   | Array           | 랭킹 목록 (점수 높은 순)      |
| rankings[].rank            | Integer         | 순위                          |
| rankings[].userId          | String          | 사용자 ID                     |
| rankings[].nickname        | String          | 사용자 닉네임                 |
| rankings[].profileImageUrl | String          | 프로필 이미지 URL             |
| rankings[].bestScore       | Integer         | 최고 점수                     |
| totalCount                 | Integer         | 전체 참여자 수                |
| myRank                     | Integer \| null | 내 랭킹 (인증 안 했으면 null) |

---

## Unity 연동 예시 코드

### C# 예시 (UnityWebRequest 사용)

```csharp
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;
using System;

[Serializable]
public class ScoreRequest
{
    public int score;
}

[Serializable]
public class ScoreResponse
{
    public string userId;
    public string nickname;
    public string profileImageUrl;
    public int? bestScore;
    public int? rank;
}

[Serializable]
public class RankingEntry
{
    public int rank;
    public string userId;
    public string nickname;
    public string profileImageUrl;
    public int bestScore;
}

[Serializable]
public class RankingResponse
{
    public RankingEntry[] rankings;
    public int totalCount;
    public int? myRank;
}

public class ArcadeAPIClient : MonoBehaviour
{
    private const string BASE_URL = "https://your-api-domain.com/api/arcade";
    private string authToken; // JWT 토큰 저장

    // 1. 점수 제출
    public IEnumerator SubmitScore(int score, Action<ScoreResponse> onSuccess, Action<string> onError)
    {
        string url = $"{BASE_URL}/score";

        ScoreRequest request = new ScoreRequest { score = score };
        string jsonData = JsonUtility.ToJson(request);

        using (UnityWebRequest www = UnityWebRequest.Post(url, jsonData, "application/json"))
        {
            www.SetRequestHeader("Authorization", $"Bearer {authToken}");

            yield return www.SendWebRequest();

            if (www.result == UnityWebRequest.Result.Success)
            {
                ScoreResponse response = JsonUtility.FromJson<ScoreResponse>(www.downloadHandler.text);
                onSuccess?.Invoke(response);
            }
            else
            {
                onError?.Invoke(www.error);
            }
        }
    }

    // 2. 내 최고 점수 조회
    public IEnumerator GetMyScore(Action<ScoreResponse> onSuccess, Action<string> onError)
    {
        string url = $"{BASE_URL}/my-score";

        using (UnityWebRequest www = UnityWebRequest.Get(url))
        {
            www.SetRequestHeader("Authorization", $"Bearer {authToken}");

            yield return www.SendWebRequest();

            if (www.result == UnityWebRequest.Result.Success)
            {
                ScoreResponse response = JsonUtility.FromJson<ScoreResponse>(www.downloadHandler.text);
                onSuccess?.Invoke(response);
            }
            else
            {
                onError?.Invoke(www.error);
            }
        }
    }

    // 3. 랭킹 조회
    public IEnumerator GetRanking(int? limit, Action<RankingResponse> onSuccess, Action<string> onError)
    {
        string url = $"{BASE_URL}/ranking";
        if (limit.HasValue)
        {
            url += $"?limit={limit.Value}";
        }

        using (UnityWebRequest www = UnityWebRequest.Get(url))
        {
            // 인증 토큰이 있으면 헤더에 추가 (선택)
            if (!string.IsNullOrEmpty(authToken))
            {
                www.SetRequestHeader("Authorization", $"Bearer {authToken}");
            }

            yield return www.SendWebRequest();

            if (www.result == UnityWebRequest.Result.Success)
            {
                RankingResponse response = JsonUtility.FromJson<RankingResponse>(www.downloadHandler.text);
                onSuccess?.Invoke(response);
            }
            else
            {
                onError?.Invoke(www.error);
            }
        }
    }
}
```

### 사용 예시

```csharp
// 점수 제출
StartCoroutine(arcadeAPI.SubmitScore(1234,
    (response) => {
        Debug.Log($"점수 제출 성공! 최고 점수: {response.bestScore}, 랭킹: {response.rank}위");
    },
    (error) => {
        Debug.LogError($"점수 제출 실패: {error}");
    }
));

// 랭킹 조회 (상위 10명)
StartCoroutine(arcadeAPI.GetRanking(10,
    (response) => {
        Debug.Log($"전체 참여자: {response.totalCount}명");
        foreach (var entry in response.rankings)
        {
            Debug.Log($"{entry.rank}위: {entry.nickname} - {entry.bestScore}점");
        }
        if (response.myRank.HasValue)
        {
            Debug.Log($"내 랭킹: {response.myRank.Value}위");
        }
    },
    (error) => {
        Debug.LogError($"랭킹 조회 실패: {error}");
    }
));
```

---

## 주의사항

1. **JWT 토큰 관리**: 로그인 후 받은 JWT 토큰을 안전하게 저장하고, 만료되면 갱신해야 합니다.

2. **null 처리**: C#에서 `int?` (nullable int)로 처리하거나, JSON 파싱 시 null 체크를 해야 합니다.

3. **에러 처리**: 네트워크 오류, 인증 오류 등을 적절히 처리해야 합니다.

4. **CORS**: Unity WebGL 빌드 시 CORS 설정이 필요할 수 있습니다. 백엔드에서 CORS 설정을 확인하세요.

5. **Base URL**: 실제 배포 환경의 API 주소로 변경해야 합니다.

---

## Unity 연동 가능 여부

✅ **완전히 가능합니다!**

Unity는 `UnityWebRequest`를 통해 HTTP 요청을 보낼 수 있으며, JSON 데이터를 주고받을 수 있습니다. 위의 예시 코드를 참고하여 구현하시면 됩니다.
