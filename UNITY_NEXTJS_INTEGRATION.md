# Unity + Next.js 통합 가이드

## 구조

```
Unity 게임 → Next.js (점수 전달) → 백엔드 API
```

Unity는 Next.js로 점수만 전달하고, Next.js가 백엔드 API를 호출합니다.

---

## 1. Unity → Next.js 통신 설정

### Unity C# 스크립트 (Unity 측)

```csharp
using UnityEngine;

public class ScoreManager : MonoBehaviour
{
    // 게임 종료 시 점수를 Next.js로 전달
    public void SendScoreToNextJS(int score)
    {
        // react-unity-webgl의 sendMessage 사용
        #if UNITY_WEBGL && !UNITY_EDITOR
            Application.ExternalCall("receiveScoreFromUnity", score);
        #endif
    }

    // 게임 시작 시 최고 점수 요청
    public void RequestMyBestScore()
    {
        #if UNITY_WEBGL && !UNITY_EDITOR
            Application.ExternalCall("requestMyBestScore");
        #endif
    }

    // 랭킹 요청
    public void RequestRanking(int limit = 10)
    {
        #if UNITY_WEBGL && !UNITY_EDITOR
            Application.ExternalCall("requestRanking", limit);
        #endif
    }

    // Next.js에서 점수를 받는 콜백 (Next.js → Unity)
    public void OnScoreReceived(string jsonData)
    {
        // JSON 파싱하여 UI 업데이트
        Debug.Log($"Received score data: {jsonData}");
        // 예: ScoreResponse response = JsonUtility.FromJson<ScoreResponse>(jsonData);
    }

    // Next.js에서 랭킹을 받는 콜백
    public void OnRankingReceived(string jsonData)
    {
        Debug.Log($"Received ranking data: {jsonData}");
        // 랭킹 UI 업데이트
    }
}
```

---

## 2. Next.js 컴포넌트 (Next.js 측)

### 업데이트된 UnityGame 컴포넌트

```typescript
import React, { useEffect, useRef } from "react";
import { Unity, useUnityContext } from "react-unity-webgl";

interface ScoreResponse {
  userId: string;
  nickname: string;
  profileImageUrl: string;
  bestScore: number | null;
  rank: number | null;
}

interface RankingEntry {
  rank: number;
  userId: string;
  nickname: string;
  profileImageUrl: string;
  bestScore: number;
}

interface RankingResponse {
  rankings: RankingEntry[];
  totalCount: number;
  myRank: number | null;
}

const UnityGame: React.FC = () => {
  const { unityProvider, loadingProgression, isLoaded, sendMessage, addEventListener, removeEventListener } = useUnityContext({
    loaderUrl: "/Build/sohee_run.loader.js",
    dataUrl: "/Build/sohee_run.data",
    frameworkUrl: "/Build/sohee_run.framework.js",
    codeUrl: "/Build/sohee_run.wasm",
    streamingAssetsUrl: "StreamingAssets",
    companyName: "MyCompany",
    productName: "MyProduct",
    productVersion: "1.0",
  });

  // API Base URL (환경 변수로 관리 권장)
  const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/arcade";

  // JWT 토큰 가져오기 (예: localStorage, cookie, 또는 인증 컨텍스트)
  const getAuthToken = (): string | null => {
    // 실제 구현에서는 인증 상태 관리 라이브러리 사용 (예: Context API, Zustand, Redux)
    return localStorage.getItem("authToken");
  };

  // 백엔드 API 호출: 점수 제출
  const submitScoreToBackend = async (score: number): Promise<ScoreResponse | null> => {
    const token = getAuthToken();
    if (!token) {
      console.error("인증 토큰이 없습니다.");
      return null;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/score`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ score }),
      });

      if (!response.ok) {
        throw new Error(`점수 제출 실패: ${response.status}`);
      }

      const data: ScoreResponse = await response.json();
      console.log("점수 제출 성공:", data);
      return data;
    } catch (error) {
      console.error("점수 제출 중 오류:", error);
      return null;
    }
  };

  // 백엔드 API 호출: 내 최고 점수 조회
  const getMyBestScore = async (): Promise<ScoreResponse | null> => {
    const token = getAuthToken();
    if (!token) {
      console.error("인증 토큰이 없습니다.");
      return null;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/my-score`, {
        method: "GET",
        headers: {
          "Authorization": `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`점수 조회 실패: ${response.status}`);
      }

      const data: ScoreResponse = await response.json();
      return data;
    } catch (error) {
      console.error("점수 조회 중 오류:", error);
      return null;
    }
  };

  // 백엔드 API 호출: 랭킹 조회
  const getRanking = async (limit?: number): Promise<RankingResponse | null> => {
    const token = getAuthToken();
    const url = limit
      ? `${API_BASE_URL}/ranking?limit=${limit}`
      : `${API_BASE_URL}/ranking`;

    try {
      const headers: HeadersInit = {
        "Content-Type": "application/json",
      };

      // 토큰이 있으면 추가 (랭킹 조회는 선택적 인증)
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const response = await fetch(url, {
        method: "GET",
        headers,
      });

      if (!response.ok) {
        throw new Error(`랭킹 조회 실패: ${response.status}`);
      }

      const data: RankingResponse = await response.json();
      return data;
    } catch (error) {
      console.error("랭킹 조회 중 오류:", error);
      return null;
    }
  };

  // Unity에서 호출할 함수들 (전역으로 등록)
  useEffect(() => {
    // Unity → Next.js: 점수 전달 받기
    (window as any).receiveScoreFromUnity = async (score: number) => {
      console.log(`Unity에서 점수 받음: ${score}`);
      const result = await submitScoreToBackend(score);

      if (result) {
        // Next.js → Unity: 결과 전달
        sendMessage("ScoreManager", "OnScoreReceived", JSON.stringify(result));
      }
    };

    // Unity → Next.js: 내 최고 점수 요청
    (window as any).requestMyBestScore = async () => {
      console.log("Unity에서 최고 점수 요청");
      const result = await getMyBestScore();

      if (result) {
        sendMessage("ScoreManager", "OnScoreReceived", JSON.stringify(result));
      }
    };

    // Unity → Next.js: 랭킹 요청
    (window as any).requestRanking = async (limit: number = 10) => {
      console.log(`Unity에서 랭킹 요청: 상위 ${limit}명`);
      const result = await getRanking(limit);

      if (result) {
        sendMessage("ScoreManager", "OnRankingReceived", JSON.stringify(result));
      }
    };

    // 정리 함수
    return () => {
      delete (window as any).receiveScoreFromUnity;
      delete (window as any).requestMyBestScore;
      delete (window as any).requestRanking;
    };
  }, [sendMessage]);

  return (
    <div>
      {!isLoaded && <p>Loading... {Math.round(loadingProgression * 100)}%</p>}

      <div
        id="unityContainer"
        style={{ width: "340px", height: "600px", background: "black" }}
      >
        <Unity
          unityProvider={unityProvider}
          style={{ width: "340px", height: "600px" }}
        />
      </div>
    </div>
  );
};

export default UnityGame;
```

---

## 3. 사용 흐름

### 게임 종료 시 점수 제출

1. **Unity**: 게임 종료 → `SendScoreToNextJS(score)` 호출
2. **Next.js**: `receiveScoreFromUnity` 함수가 점수 받음
3. **Next.js**: 백엔드 API 호출 (`POST /api/arcade/score`)
4. **Next.js**: 결과를 Unity로 전달 (`sendMessage`)
5. **Unity**: `OnScoreReceived` 콜백에서 결과 처리

### 랭킹 조회

1. **Unity**: 랭킹 버튼 클릭 → `RequestRanking(10)` 호출
2. **Next.js**: `requestRanking` 함수가 요청 받음
3. **Next.js**: 백엔드 API 호출 (`GET /api/arcade/ranking?limit=10`)
4. **Next.js**: 결과를 Unity로 전달 (`sendMessage`)
5. **Unity**: `OnRankingReceived` 콜백에서 랭킹 UI 업데이트

---

## 4. 환경 변수 설정

`.env.local` 파일에 API URL 추가:

```env
NEXT_PUBLIC_API_URL=https://your-api-domain.com/api/arcade
```

---

## 5. 인증 토큰 관리

실제 프로젝트에서는 인증 상태 관리 라이브러리를 사용하세요:

```typescript
// 예: Context API 사용
import { useAuth } from "@/contexts/AuthContext";

const UnityGame: React.FC = () => {
  const { token } = useAuth();

  const getAuthToken = (): string | null => {
    return token;
  };

  // ...
};
```

---

## 6. 에러 처리 개선

```typescript
// 에러 처리 예시
const submitScoreToBackend = async (score: number): Promise<ScoreResponse | null> => {
  const token = getAuthToken();
  if (!token) {
    // Unity에 에러 전달
    sendMessage("ScoreManager", "OnScoreError", "인증이 필요합니다.");
    return null;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/score`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ score }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      sendMessage("ScoreManager", "OnScoreError", errorData.message || "점수 제출 실패");
      return null;
    }

    const data: ScoreResponse = await response.json();
    sendMessage("ScoreManager", "OnScoreReceived", JSON.stringify(data));
    return data;
  } catch (error) {
    console.error("점수 제출 중 오류:", error);
    sendMessage("ScoreManager", "OnScoreError", "네트워크 오류가 발생했습니다.");
    return null;
  }
};
```

---

## 요약

✅ **Unity**: 점수만 Next.js로 전달 (`Application.ExternalCall`)
✅ **Next.js**: 백엔드 API 호출 (제가 작성한 API 문서 사용)
✅ **Next.js**: 결과를 Unity로 전달 (`sendMessage`)

이 구조로 Unity는 백엔드와 직접 통신하지 않고, Next.js가 중간에서 API 호출을 처리합니다.
