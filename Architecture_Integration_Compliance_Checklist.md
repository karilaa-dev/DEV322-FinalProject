Architecture Integration Compliance Checklist — Noise & Motion Anomaly Detector
Date: 2026-06-10
Branch base: `main` (local HEAD updated from origin/main)
Report author: Henry (automated assistant)

Purpose
This document answers the course checklist items using the current repository state (main branch) and references files added in the feature PR branch `Add-AnomalyEntity-DAO-Database-henry` where applicable. For each "Yes" I provide exact file paths and function names.

---

1) Data Layer Structure

- RemoteDataSource exists as a separate class.
  - Answer: No (not present on `main`).
  - Present on branch: N/A — there is no RemoteDataSource in the repository (neither on `main` nor in the feature branch).

- RemoteDataSource contains the Ktor client and suspend API calls.
  - Answer: No.

- DTO classes exist and represent API response structure.
  - Answer: No.

- Room Entity classes are separate from DTO classes.
  - Answer: Partial — Room entity/DAO/DB exist in feature branch (not on `main`).
  - Files (feature branch `Add-AnomalyEntity-DAO-Database-henry`):
    - `app/src/main/java/com/bananaginger/noisedetector/data/AnomalyEntity.kt` — Room entity `AnomalyEntity` (fields: `id`, `timestamp`, `date`, `day`, `type`, `magnitude`, `severity`, `description`, `metadata`).
    - `app/src/main/java/com/bananaginger/noisedetector/data/AnomalyDao.kt` — DAO interface.
    - `app/src/main/java/com/bananaginger/noisedetector/data/AppDatabase.kt` — `RoomDatabase`.

- DTO objects are mapped to Entity objects inside the Repository.
  - Answer: No (no Repository / mapping implemented).
  - Mapping occurs in function: N/A


2) Repository Discipline

- Repository exposes Flow from DAO, not from the network.
  - Answer: No (Repository is not implemented on `main`).
  - If implemented, expected function signature: `fun getAll(): Flow<List<AnomalyEntity>>` (should delegate to DAO `getAll()`).

- Repository contains a suspend `refresh()` (or equivalent) function that calls the RemoteDataSource.
  - Answer: No.

- Repository writes API results into Room.
  - Answer: No DAO invocation from a repository (no repository present). If present, the DAO function invoked would be: `insertAll(anomalies: List<AnomalyEntity>)` (see DAO in feature branch).

- Repository does not expose DTO objects to ViewModel or UI.
  - Confirm: N/A (no repository). Expected: Yes when repository is implemented.


3) Room Compliance

- DAO list queries return Flow.
  - Answer: Yes — in feature branch (not on `main`).
  - DAO function: `fun getAll(): Flow<List<AnomalyEntity>>` (file: `app/src/main/java/com/bananaginger/noisedetector/data/AnomalyDao.kt` on branch `Add-AnomalyEntity-DAO-Database-henry`).

- DAO write operations are suspend functions.
  - Answer: Yes (feature branch):
    - `suspend fun insert(anomaly: AnomalyEntity): Long`
    - `suspend fun insertAll(anomalies: List<AnomalyEntity>)`
    - `suspend fun update(anomaly: AnomalyEntity)`
    - `suspend fun delete(anomaly: AnomalyEntity)`
    - `suspend fun deleteOlderThan(cutoff: Long)`
  - File: `app/src/main/java/com/bananaginger/noisedetector/data/AnomalyDao.kt` (feature branch).

- Room is the single source of truth observed by the UI.
  - Answer: No (UI is not wired to Room yet). Current UI (`MainActivity`) is static and does not observe repository/DAO flows.


4) ViewModel Discipline

- ViewModel exposes StateFlow (or Flow converted to StateFlow).
  - Answer: No — no `ViewModel` classes exist on `main` or feature branch.

- Coroutines are launched only in `viewModelScope`.
  - Answer: N/A (no ViewModel present).

- ViewModel does not call Ktor or RemoteDataSource directly.
  - Answer: N/A (no ViewModel present).


5) UI Layer Compliance

- Composables collect StateFlow using `collectAsState()`.
  - Answer: No — current composables (`Greeting`) do not collect any flows.
  - Composable example on `main`: `Greeting` in `app/src/main/java/com/bananaginger/noisedetector/MainActivity.kt`.

- No suspend functions are called directly from Composables.
  - Confirm: Yes — e.g., `Greeting` does not call suspend functions. File: `app/src/main/java/com/bananaginger/noisedetector/MainActivity.kt`.

- No DAO or database logic exists in Activity or Composable layer.
  - Confirm: Yes — `MainActivity` is UI only, no DB calls.


6) Boundary Violations Check

- `GlobalScope` does not appear in the project.
  - Confirm: Yes — search returned no instances in `app/src`.

- `runBlocking` does not appear in the project.
  - Confirm: Yes — no instances found.

- Network code does not appear in Activity or Composable files.
  - Confirm: Yes — `MainActivity` contains UI only.

- DTO classes are not referenced in UI files.
  - Confirm: Yes — there are no DTO classes defined or referenced in UI files.


7) Reflection — boundary between RemoteDataSource and Repository

Where the boundary should be (recommended layout):

- RemoteDataSource (network-only):
  - Responsibility: perform HTTP requests and return DTOs.
  - Location: `app/src/main/java/com/bananaginger/noisedetector/data/remote/RemoteDataSource.kt` (example).
  - Signature example: `suspend fun fetchAnomalies(): List<AnomalyDto>`.

- DTOs:
  - Location: `app/src/main/java/com/bananaginger/noisedetector/data/dto/AnomalyDto.kt`.
  - DTOs represent raw API responses and are only used inside `RemoteDataSource` and `Repository`.

- Mappers (DTO -> Entity):
  - Location: `app/src/main/java/com/bananaginger/noisedetector/data/mappers/AnomalyMappers.kt`.
  - Example: `fun AnomalyDto.toEntity(): AnomalyEntity`.

- Repository (single source of truth coordinator):
  - Responsibility: expose Flows from Room DAO to the app, provide `suspend fun refresh()` to fetch from RemoteDataSource and persist.
  - Location: `app/src/main/java/com/bananaginger/noisedetector/data/repository/AnomalyRepository.kt`.
  - Example API:
    - `fun getAll(): Flow<List<AnomalyEntity>>` (delegates to `anomalyDao.getAll()`)
    - `suspend fun refresh()` { val dtos = remote.fetchAnomalies(); dao.insertAll(dtos.map { it.toEntity() }) }

Why the boundary matters:
- Separates network parsing concerns from caching/business rules.
- Keeps UI/ViewModel dependent on Entities and Flows (stable contract). UI remains testable and offline-friendly.
- Makes unit testing straightforward: RemoteDataSource can be mocked separately from Repository, and Repository can be tested using an in-memory Room or a fake DAO.


8) Summary & Action items (to get full "Yes" answers)

Current status: partial. Room entity/DAO/DB were implemented in the feature PR branch `Add-AnomalyEntity-DAO-Database-henry`. The `main` branch contains only UI skeleton and theme.

To complete the checklist and obtain full compliance, implement the following:
- Add `RemoteDataSource` (network client) and DTOs.
- Add `data/mappers/AnomalyMappers.kt` with DTO->Entity mapping.
- Implement `AnomalyRepository` as described (expose `getAll()` Flow and `suspend refresh()` that writes to DAO).
- Implement `AnomalyViewModel` that exposes `StateFlow` using `stateIn(viewModelScope...)` and calls repository.refresh() as needed.
- Update composables to `collectAsState()` from `ViewModel.anomalies` and display data.

Files that exist in feature branch (for reviewer/professor):
- `app/src/main/java/com/bananaginger/noisedetector/data/AnomalyEntity.kt` (Entity)
- `app/src/main/java/com/bananaginger/noisedetector/data/AnomalyDao.kt` (DAO — functions listed above)
- `app/src/main/java/com/bananaginger/noisedetector/data/AppDatabase.kt` (RoomDatabase — `anomalyDao()` and `getInstance(context)`)

Files on `main` (current HEAD):
- `app/src/main/java/com/bananaginger/noisedetector/MainActivity.kt` (UI greeting composable)
- theme files under `app/src/main/java/com/bananaginger/noisedetector/ui/theme`

---

If you want, I can now:
- (A) Create skeletons for `RemoteDataSource`, DTOs, `Repository`, `Mappers`, `ViewModel` and push them to a branch, or
- (B) Only generate and push this checklist file (done) and leave implementation to the team.


End of report.
