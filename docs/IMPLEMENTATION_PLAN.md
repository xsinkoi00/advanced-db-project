# CampusAtlas — Implementation Plan

Smart Campus Facilities Management System
Course project, PDBe 26/27Z, Faculty of Information Technology, Brno University of Technology.

This document is the working implementation plan. It expands the submitted [project plan](./CampusAtlas_Project_Plan.pdf) into concrete, buildable phases. It is written to be understandable by the whole team, the supervisor, and anyone who later picks up the repository.

---

## 1. What we are building

A Java Swing desktop client that talks to an Oracle database. The user is a campus facilities administrator who:

- maintains a small 2D map of a fictional campus,
- creates/edits/deletes five kinds of spatial objects (buildings, parking areas, walkways, green spaces, ponds),
- attaches photographs to each facility,
- rotates, replaces and deletes those photographs,
- runs spatial queries (intersections, nearest neighbour, within-distance, area, length, distance),
- searches photographs by **visual content** (not filename),
- can set DB connection details at runtime and reinitialise sample data.

The application is a graded educational prototype, not a production system. The dataset is ~20 spatial objects and 12–20 photos.

### Target databases

| Environment | Server | Purpose |
|-------------|--------|---------|
| Development | Local Oracle 18c XE (`localhost:1521/XEPDB1`) | Fast iteration, no network dependency |
| Grading     | Campus Oracle 18c EE (`gort.fit.vutbr.cz:1521/orclpdb`) | Final submission and defence |

Both servers are 18c, so Oracle Spatial and Oracle Multimedia (ORDIM) are available in each. The schema must build cleanly on both.

### Multimedia approach — Approach A (Oracle Multimedia / ORDIM)

Images are stored in `ORDSYS.ORDImage` columns. Feature extraction (`SI_StillImage`, `SI_AverageColor`, `SI_ColorHistogram`, `SI_PositionalColor`, `SI_Texture`) is done **by Oracle**. Similarity search uses `SI_ScoreByFtrList` with weighted feature criteria. This matches the course template, uses Oracle libraries (allowed by the assignment), and works on 18c on both local and campus servers.

> This replaces the Java-side histogram approach described in Section 5 of the submitted project plan. The project plan explicitly left the method open for instructor confirmation; after confirming the campus server is 18c, Approach A is the simpler and safer choice.

---

## 2. Architecture

```
+-------------------------------------------------------+
|  Java Swing client (single process)                   |
|  package: dbs.oracle_project                          |
|                                                       |
|  +--------------+   +--------------+   +-----------+  |
|  | UI           |-->| Service      |-->| DAO       |  |
|  | (Swing/Java2D|   | (business    |   | (JDBC +   |  |
|  |  panels)     |   |  rules)      |   |  ORDIM)   |  |
|  +--------------+   +--------------+   +-----------+  |
|         ^                                   |         |
|         |                                   v         |
|  +--------------+                    +-----------+    |
|  | Connection   |------------------->| JDBC conn |    |
|  | dialog       |                    +-----------+    |
|  +--------------+                          |          |
+------------------------------------------- | ---------+
                                             |
                                             v
                              +-----------------------------+
                              | Oracle 18c                  |
                              | - relational tables         |
                              | - SDO_GEOMETRY + indexes    |
                              | - ORDImage + SI_* columns   |
                              +-----------------------------+
```

- **UI layer** — Swing frames and panels. The map is a `JPanel` that overrides `paintComponent` and draws `SDO_GEOMETRY` features with `Java2D`. No external map library.
- **Service layer** — thin, holds application logic (e.g., "update facility geometry and rebuild its spatial feature cache"), composes DAO calls.
- **DAO layer** — one DAO class per aggregate: `FacilityDAO`, `FacilityImageDAO`. Wraps JDBC + ORDIM calls. All SQL lives here.
- **Connection** — a single `OracleDataSource` built from values the user enters in the login dialog. No credentials in source.

### Build

Gradle (`application` plugin), Java 1.8 source/target to match the lab machines. The current `build.gradle.kts` and `settings.gradle.kts` stay; only the main class name in `gradle.properties` needs the fix we already applied.

### Package layout (planned)

```
src/main/java/dbs/oracle_project/
├── App.java                 (main; opens LoginDialog)
├── db/
│   ├── ConnectionManager.java
│   └── Schema.java          (reinit sample data)
├── model/
│   ├── Facility.java
│   ├── FacilityCategory.java    (enum)
│   ├── Geometry.java            (wrapper around JGeometry)
│   └── FacilityImage.java
├── dao/
│   ├── FacilityDAO.java
│   └── FacilityImageDAO.java
├── ui/
│   ├── MainFrame.java
│   ├── LoginDialog.java
│   ├── MapPanel.java            (Java2D canvas, pan/zoom, selection)
│   ├── FacilityEditor.java      (side panel: attributes + photos)
│   ├── SpatialQueryPanel.java
│   ├── ImageSearchPanel.java
│   └── HelpWindow.java
└── util/
    ├── GeometryTools.java       (SDO <-> Java2D conversions)
    └── ImageTools.java          (rotate via BufferedImage)
```

---

## 3. Data model

### Relational / spatial — one table per category

Each category keeps its own attributes and a geometry column. This is simpler than a single `facility` supertype and keeps the DDL readable for the supervisor.

```sql
building      (id, name, use_type, description, status, geom SDO_GEOMETRY)
parking_area  (id, name, capacity, geom SDO_GEOMETRY)
walkway       (id, name, path_type, geom SDO_GEOMETRY)
green_space   (id, name, managed, geom SDO_GEOMETRY)
water_area    (id, name, geom SDO_GEOMETRY)
```

Each table gets a row in `USER_SDO_GEOM_METADATA` (with the SRID left `NULL` — local Cartesian plane, acceptable for a fictional campus) and a spatial index of type `MDSYS.SPATIAL_INDEX_V2`.

### Multimedia

Images live in a separate table, foreign-keyed back to the facility by `(category, facility_id)`:

```sql
facility_image (
  id            NUMBER PK,
  category      VARCHAR2(16)  -- 'BUILDING', 'PARKING', ...
  facility_id   NUMBER,
  caption       VARCHAR2(500),
  uploaded_at   TIMESTAMP DEFAULT SYSTIMESTAMP,
  image         ORDSYS.ORDImage,
  image_si      ORDSYS.SI_StillImage,
  image_ac      ORDSYS.SI_AverageColor,
  image_ch      ORDSYS.SI_ColorHistogram,
  image_pc      ORDSYS.SI_PositionalColor,
  image_tx      ORDSYS.SI_Texture
);
```

Features are populated after each image upload via the same `SI_StillImage(ORDImage.getContent())` call that the course template uses. Similarity search uses `SI_ScoreByFtrList` ordered ascending.

### Why not a single `facility` supertype

The five categories have genuinely different attributes (capacity, path type, etc.) and sharing one table forces nullable columns. One table per category is clearer for a schema the supervisor will read.

---

## 4. Coverage matrix — assignment requirements → where they are implemented

| Requirement | Covered by |
|-------------|-----------|
| 5 types of spatial entities | Five tables with `SDO_GEOMETRY` |
| Insert/update/delete spatial data | DAO + Swing map editor |
| Map visualisation + control (move/resize) | `MapPanel` with drag/resize handles |
| 1 non-trivial spatial operator | `SDO_RELATE` — detect building/green-space conflicts |
| 2 more spatial operators | `SDO_WITHIN_DISTANCE`, `SDO_NN` |
| ≥3 analytic functions on spatial data | `SDO_GEOM.SDO_AREA`, `SDO_GEOM.SDO_LENGTH`, `SDO_GEOM.SDO_DISTANCE` — plus one SQL window function (`RANK() OVER (ORDER BY SDO_GEOM.SDO_AREA ...)`) for safety against a strict reading of "analytic function" |
| Insert/update/delete multimedia | `FacilityImageDAO` + Swing panel |
| Image rotation | `BufferedImage` rotate in Java, re-upload via `OrdImage.loadData` |
| Query multimedia by content | `SI_ScoreByFtrList` similarity query |
| User-settable credentials | `LoginDialog`, no hardcoded URL |
| (Re)initialise DB content | "Initialise sample data" button → `Schema.initSampleData()` |
| GUI with help | `HelpWindow` opened from main menu |
| Runs on Windows and Linux | Pure Java + Gradle wrapper |
| View+operate on both data types + relational | Facility editor side panel shows attributes, geometry and photos together |

---

## 5. Phases

Dates mirror the submitted project plan. Today is 2026-10-27.

### Phase 0 — Housekeeping (now, 1–2 days)

Goal: unblock everything else.

- [ ] Add supervisor `marek-rychly` to the Git repository with read-only access.
- [ ] Drop the leftover `product` table from the local DB.
- [ ] Delete `src/main/java/dbs/oracle_project/Product.java`.
- [ ] Replace the body of `App.java` with a `main` that opens a `LoginDialog` placeholder.
- [ ] Create folder structure `schema/` for DDL files and `docs/` for documents.
- [ ] Move the submitted project-plan PDF into `docs/`.
- [ ] First commit with a clear message ("scrub starter template, scaffold CampusAtlas layout").

### Phase 1 — Schema (deadline 2026-11-01)

Goal: a `schema/schema.sql` + `schema/sample_data.sql` that run clean on both local XE and campus EE.

- [ ] Write `schema/schema.sql`:
  - `DROP ... PURGE` statements for every object (allowed to fail on first run).
  - `CREATE TABLE` for the five category tables.
  - `INSERT INTO USER_SDO_GEOM_METADATA` rows (one per table, SRID `NULL`, bounds large enough for a fictional campus — e.g., `(-1000..1000, -1000..1000)`).
  - `CREATE INDEX ... INDEXTYPE IS MDSYS.SPATIAL_INDEX_V2` on each geometry column.
  - `CREATE TABLE facility_image` with ORDImage + SI_* columns.
  - `CREATE INDEX` on `(category, facility_id)` for image lookups.
- [ ] Write `schema/sample_data.sql`:
  - Fifteen to twenty spatial objects across the five categories.
  - A dozen placeholder images uploaded via `ORDImage` from local files.
- [ ] Run `schema.sql` on local XE as `cardb`; confirm no errors.
- [ ] Run `schema.sql` on campus (`gort.fit.vutbr.cz`) as the course-provided user; confirm no errors.
- [ ] Submit `schema.sql` to the course system by 2026-11-01.

### Phase 2 — Core plumbing + spatial CRUD + map (2026-11-02 → 2026-11-15)

Goal: the user can connect to a DB, see the map, and create/edit/delete facilities on it.

- [ ] `ConnectionManager` and `LoginDialog` — collects host/service/user/password, builds `OracleDataSource`, offers a "Test connection" button.
- [ ] `Schema` helper with `reinitSampleData(Connection)` that runs the DDL + sample-data scripts from the classpath (bundled via `src/main/resources/schema/`).
- [ ] `MainFrame` with menu bar: File (Connect, Reinit sample data, Exit), View (Spatial queries, Image search), Help (About, Help).
- [ ] `FacilityDAO.listAll()` returns facilities with their geometries, read via `JGeometry.loadJS` (from the Oracle Spatial Java API, part of the Oracle libraries you already depend on).
- [ ] `MapPanel`:
  - `paintComponent` draws each geometry with a per-category colour.
  - Mouse: click selects a feature, drag moves it, corner handles resize it.
  - Toolbar: "New Building", "New Walkway", ... each entering a draw mode.
  - Save and Cancel apply/revert edits.
- [ ] `FacilityDAO.save(Facility)` writes the geometry back via `JGeometry.storeJS`.
- [ ] Integration demo: create a building, move it, resize it, delete it, reload the DB, see it persisted.

### Phase 3 — Spatial queries, multimedia, image search (2026-11-16 → 2026-11-29)

Goal: all three required spatial operators, three analytics, and the full image flow.

- [ ] `SpatialQueryPanel`:
  - "Find conflicts" — `SDO_RELATE(a.geom, b.geom, 'mask=ANYINTERACT') = 'TRUE'` between a selected polygon and all green spaces/buildings.
  - "Within distance" — `SDO_WITHIN_DISTANCE(geom, :p, 'distance=:d')`.
  - "Nearest neighbour" — `SDO_NN(geom, :p, 'sdo_num_res=:n', 1) = 'TRUE'`.
  - "Area/length/distance" — three dedicated buttons returning values from `SDO_GEOM.SDO_AREA`, `SDO_GEOM.SDO_LENGTH`, `SDO_GEOM.SDO_DISTANCE`.
  - A "Ranked by area" view using `RANK() OVER (ORDER BY SDO_GEOM.SDO_AREA(geom, 0.05) DESC)` to satisfy a strict reading of "analytic function".
  - Results list + map highlight.
- [ ] `FacilityImageDAO`:
  - `upload(facilityKey, File)` → inserts a new row, loads bytes into `ORDImage`, calls `setProperties`, then issues the two `UPDATE` statements the template already shows (`SI_StillImage(image.getContent())`, then `SI_AverageColor / SI_ColorHistogram / SI_PositionalColor / SI_Texture`).
  - `replace(id, File)` — same as upload but updates existing row.
  - `rotate(id, degrees)` — download bytes, rotate via `BufferedImage` + `AffineTransform`, re-upload, re-run feature extraction.
  - `delete(id)` — plain row delete.
  - `findSimilar(id, weights)` — the `SI_ScoreByFtrList` query from the template.
- [ ] `FacilityEditor` side panel: thumbnails, buttons, rotate slider, replace file chooser.
- [ ] `ImageSearchPanel`: user picks a facility image or uploads a query file, slider for AC/CH/PC/TX weights, result grid ranked ascending by score.
- [ ] First end-to-end integration run: load sample data, draw a new polygon, upload a photo to it, rotate it, run similarity search, confirm it comes back.

### Phase 4 — Finalisation (2026-11-30 → 2026-12-06)

- [ ] Smoke-test on Linux (any distro with JDK 11+).
- [ ] Smoke-test on Windows.
- [ ] Write `README.md` with build/run instructions for both OSes.
- [ ] Fill `HelpWindow` content: short "how to" per tool with screenshots.
- [ ] Write `docs/developer.md` with the data model and how to extend it.
- [ ] Freeze the schema; make sure `schema/schema.sql` still runs clean on campus.
- [ ] Build a distribution ZIP of the repo content without `.git` and submit by 2026-12-06.

### Phase 5 — Defence (Week 13)

- [ ] Rehearse the demo flow: connect → reinit → add facility → move/resize → three spatial queries → upload photo → rotate → image search.
- [ ] Prepare short answers for: data model choice, why ORDIM over Java histograms, how `SDO_RELATE` differs from `SDO_WITHIN_DISTANCE`, how `SI_ScoreByFtrList` weighs features.

---

## 6. How to run (today, local)

```powershell
# 1. Make sure Oracle 18c XE service and listener are running.
# 2. From the project root (replace <PASSWORD> with the one you chose at install):
.\gradlew.bat "-Dlogin=cardb" "-Dpassword=<PASSWORD>" run
```

Setup steps for the local DB (user creation, listener fix, schema bootstrap) are in [db_setup.md](./db_setup.md).

The current `App.java` still runs the throwaway car demo. That changes in Phase 0.

Once the login dialog exists (Phase 2) users will instead launch the app with no `-Dlogin`/`-Dpassword` and enter credentials in the UI.

---

## 7. Risks and open items

| Risk | Mitigation |
|------|-----------|
| Campus 18c server might be unreachable from outside the faculty network | Dev target stays local XE; only test on campus once per phase |
| Oracle Spatial Java API (`oracle.spatial.geometry.JGeometry`) is not in the current Gradle dependencies | Add `com.oracle.database.jdbc:ojdbc-sdoapi` or the equivalent lab-provided JAR; **this needs supervisor approval if it isn't a listed Oracle lab library** |
| Rotation of large JPEGs can be slow with `BufferedImage` | Downscale on upload; keep original bytes but rotate thumbnails only |
| Grading machine may use a different JDK | Target Java 1.8 bytecode; the build is already set up this way |
| Repo commit activity is uneven across the team | Both members push their own commits; weekly reviews stored under `docs/reviews/` |

Open items to confirm with the supervisor (short email, one message):

1. We are using Approach A (ORDIM / `SI_ScoreByFtrList`) for content-based image search. Any objection?
2. Can we add the Oracle Spatial Java API JAR (`sdoapi`) if it is not already in the lab Oracle library bundle?
3. Confirm the username/schema we should use on `gort.fit.vutbr.cz` for schema upload testing.

---

## 8. Done means

- `schema/schema.sql` runs clean on local XE and campus EE.
- `./gradlew run` on Windows or `./gradlew run` on Linux opens the Swing client.
- A fresh user can connect, hit "Initialise sample data", and see a populated campus map.
- All eight functional requirements from the project plan work.
- Help window explains how to use each tool.
- Repository has an even distribution of commits from both team members.
- Final ZIP submitted by 2026-12-06; defence rehearsed.
