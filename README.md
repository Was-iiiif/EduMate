# EduMate - AI Student Learning Partner

EduMate is a native Android application built in **Java + XML with View Binding** where learners teach academic topics to an adaptive AI student, answer its progressive follow-up questions, and receive rubric-based scoring with evidence citations.

---

## 🏗 Architecture & Stack Overview

```
+---------------------------------------------------------------------------------+
|                               Activities & UI                                   |
|   MainActivity · TopicSelectionActivity · SessionActivity · FeedbackActivity   |
|                      HistoryActivity · SettingsActivity                         |
+---------------------------------------------------------------------------------+
                                      │
                                      ▼
+---------------------------------------------------------------------------------+
|                               ViewModels & LiveData                             |
|  HomeViewModel · SessionViewModel · FeedbackViewModel · HistoryViewModel        |
|  * Survives rotation; single shared executor pattern                            |
+---------------------------------------------------------------------------------+
            │                                  │                        │
            ▼                                  ▼                        ▼
+-----------------------+          +-----------------------+  +-------------------+
|     AIRepository      |          |   SessionRepository   |  |   SyncRepository  |
|  * Student Prompts    |          |  * Local SQLite CRUD  |  |  * Upload/Restore |
|  * Schema Validation  |          |  * Atomic Transaction |  |  * Soft Delete    |
+-----------------------+          +-----------------------+  +-------------------+
            │                                  │                        │
            ▼                                  ▼                        ▼
+-----------------------+          +-----------------------+  +-------------------+
|  Gemini AI REST /     |          |  EduMateDbHelper/DAO  |  | Firebase Realtime |
|  Local Rubric Engine  |          |  (sessions, messages, |  | Database          |
|                       |          |   evaluations tables) |  |                   |
+-----------------------+          +-----------------------+  +-------------------+
```

### Sidecar Components
- **VoiceController**: Turn-based `SpeechRecognizer` input (with runtime `RECORD_AUDIO` permission check and editing box preview) + `TextToSpeech` output with replay and stop controls.
- **SyncWorker**: Background retry mechanism using Android Jetpack `WorkManager` with a `NetworkType.CONNECTED` constraint.
- **EduMateNetworkCallback**: Real-time network listener (`ConnectivityManager.NetworkCallback`) triggering background sync automatically upon reconnection.
- **EduMateSyncService**: Minimal started service demonstrating background sync via explicit Intent.
- **EduMateProvider**: Read-only `ContentProvider` (`content://com.edumate.app.provider/sessions`) exposing session topics and scores.

---

## 📊 Rubric Scoring & Evaluation Design

The rubric evaluation scores **only** the learner's explanations against a topic checklist (5–8 key concepts):

| Criterion | Weight | Measures |
|---|---|---|
| **Accuracy** | 40% | Factual correctness and lack of errors |
| **Completeness** | 25% | Coverage of the topic checklist |
| **Clarity** | 20% | Structural organization and clear analogies |
| **Reasoning** | 15% | Explaining *why* and *how* (causal relationships) |

**Overall Score Formula:**
$$\text{Overall} = 0.40 \times \text{Accuracy} + 0.25 \times \text{Completeness} + 0.20 \times \text{Clarity} + 0.15 \times \text{Reasoning}$$

Computed strictly in app code (clamped between 0 and 100), with each score citing quotes and listing detected misconceptions.

---

## 💾 Local SQLite Schema

1. **`sessions`**:
   - `id TEXT PRIMARY KEY` (Stable UUID)
   - `topic TEXT NOT NULL`
   - `persona TEXT NOT NULL`
   - `started_at INTEGER NOT NULL`
   - `ended_at INTEGER`
   - `status TEXT NOT NULL` (`IN_PROGRESS` or `COMPLETED`)
   - `overall_score REAL`
   - `sync_status TEXT NOT NULL` (`PENDING` or `SYNCED`)
   - `deleted_at INTEGER` (Nullable timestamp for soft deletes)

2. **`messages`**:
   - `id INTEGER PRIMARY KEY AUTOINCREMENT`
   - `session_id TEXT NOT NULL` (Foreign key $\to$ `sessions.id` with `ON DELETE CASCADE`)
   - `sender TEXT NOT NULL` (`LEARNER` or `STUDENT`)
   - `message_text TEXT NOT NULL`
   - `created_at INTEGER NOT NULL`

3. **`evaluations`**:
   - `id INTEGER PRIMARY KEY AUTOINCREMENT`
   - `session_id TEXT NOT NULL UNIQUE` (Foreign key $\to$ `sessions.id`)
   - `accuracy REAL NOT NULL`
   - `completeness REAL NOT NULL`
   - `clarity REAL NOT NULL`
   - `reasoning REAL NOT NULL`
   - `overall_score REAL NOT NULL`
   - `feedback TEXT`
   - `strengths TEXT` (JSON array)
   - `improvements TEXT` (JSON array)
   - `misconceptions TEXT` (JSON array of `{statement, messageId, citation}`)
   - `created_at INTEGER NOT NULL`

Session completion and evaluation insertion are wrapped in an atomic SQLite transaction.

---

## 🧪 Testing & Verification

Unit tests located in `app/src/test/java/com/edumate/app`:
1. `EvaluationScoringTest.java`: Validates rubric weighting, edge cases (0, 100), decimal rounding, and out-of-bounds clamping.
2. `EvaluationJsonValidationTest.java`: Tests structured JSON schema parsing, missing keys exception, and bounds enforcement.
3. `SessionModelTest.java`: Tests UUID stability, defaults, soft delete lifecycle, and sync status transitions.
4. `TopicChecklistTest.java`: Tests 5–8 concept checklist coverage for preset and custom topics.

### Running Tests
```bash
./gradlew test
```

### Building Debug APK
```bash
./gradlew assembleDebug
```
The APK is output to: `app/build/outputs/apk/debug/app-debug.apk`.
