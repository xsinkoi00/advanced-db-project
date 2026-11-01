# CampusAtlas — Contribution Record and Team Review

**Project:** CampusAtlas — Smart Campus Facilities Management System  
**Course:** Advanced Databases  
**University:** Brno University of Technology, Faculty of Information Technology  
**Date:** 1 November 2026  
**Team members:** Namukwanga and Izukanji  
**Document status:** Draft — awaiting review and confirmation by both team members

---

## 1. Purpose

This document records individual technical contributions, testing activities, implementation progress, and outstanding tasks for the CampusAtlas semester project.

It supports team coordination, version-control accountability, and preparation for the final project defence.

This is an initial contribution record and proposed team review. It should not be treated as minutes of a completed joint review until both team members have verified its contents.

## 2. Contributions — Namukwanga Musukwa

### 2.1 Database Schema Validation

- Executed and verified the database schema on the university Oracle 18c server.
- Loaded and checked 18 sample spatial objects across five facility categories.
- Developed `schema/validate_geometry.sql`.
- Validated all 18 spatial geometries using `SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT`.
- Confirmed all geometries returned `TRUE`.
- Verified five spatial indexes and one multimedia lookup index.
- Tested database constraints, confirming that invalid parking capacities were rejected.

**GitHub:** [Pull Request #1](https://github.com/xsinkoi00/advanced-db-project/pull/1)  
**Commit:** `f274068` — `test: add Oracle spatial geometry validation script`

### 2.2 Spatial Analysis Testing

Developed `schema/test_spatial_analysis.sql` to test:

- `SDO_GEOM.SDO_AREA` — calculates building areas.
- `SDO_GEOM.SDO_LENGTH` — calculates walkway lengths.
- `SDO_GEOM.SDO_DISTANCE` — calculates distances between facilities.

All three queries executed successfully against the campus Oracle database.

**Results:** 5 building areas, 4 walkway lengths, and 4 parking-distance calculations.

**GitHub:** [Pull Request #2](https://github.com/xsinkoi00/advanced-db-project/pull/2)  
**Commit:** `737b490` — `test: add Oracle spatial analysis queries`

### 2.3 Spatial Operator Testing

Developed `schema/test_spatial_operators.sql` to test:

- `SDO_RELATE` — identified 7 building–walkway spatial relationships.
- `SDO_WITHIN_DISTANCE` — identified 2 walkways within 100 coordinate units of the Main Library.
- `SDO_NN` — identified the 3 nearest parking areas to the Main Library.

All three queries executed successfully against the campus Oracle database.

**GitHub:** [Pull Request #3](https://github.com/xsinkoi00/advanced-db-project/pull/3)  
**Commit:** `b8cd002` — `test: add Oracle spatial operator validation queries`

### 2.4 Multimedia Referential Integrity

Identified and investigated a database integrity weakness allowing images to reference nonexistent facilities.

Developed `schema/multimedia_integrity.sql` and integrated six Oracle triggers into `schema/schema.sql`:

1. `TRG_IMAGE_VALID_FACILITY` — validates image ownership.
2. `TRG_BUILDING_DELETE_IMAGES` — deletes associated building images.
3. `TRG_PARKING_DELETE_IMAGES` — deletes associated parking images.
4. `TRG_WALKWAY_DELETE_IMAGES` — deletes associated walkway images.
5. `TRG_GREEN_DELETE_IMAGES` — deletes associated green-space images.
6. `TRG_WATER_DELETE_IMAGES` — deletes associated water-area images.

**Functional tests completed:**

- Invalid image references correctly rejected.
- Valid image references accepted.
- Automatic image deletion verified for all five facility categories.
- All six triggers confirmed enabled.
- Test transactions rolled back to preserve sample data.

**GitHub:** [Pull Request #4](https://github.com/xsinkoi00/advanced-db-project/pull/4)  
**Commit:** `c7ad996` — `feat: enforce multimedia referential integrity with Oracle triggers`

### 2.5 Complete Schema Verification

- Successfully executed the updated `schema.sql` on campus Oracle 18c.
- Verified creation of six database tables.
- Verified spatial metadata and index creation.
- Verified compilation of all six multimedia integrity triggers.
- Reloaded 18 sample facilities successfully.
- Confirmed no unexpected errors in the completed installation test.

### 2.6 Java Application Verification

- Built the existing Java application successfully using Gradle on macOS/Windows.
- Launched the CampusAtlas graphical interface.
- Established a connection to the university Oracle database.
- Verified that spatial facilities could be retrieved and displayed on the campus map.

These activities verified existing application functionality; they do not represent authorship of the initial Java implementation.

## 3. Contributions — Izukanji

Based on the existing repository and collaboration history, Izukanji has led the initial implementation of:

- The CampusAtlas database schema and sample data.
- The Java application structure and Gradle project.
- Database connection and login functionality.
- Spatial data access and map visualization.
- Spatial query functionality.
- Multimedia image-management and similarity-search code.

## 4. GitHub Contribution Summary

| PR | Contribution | Author | Status |
|---|---|---|---|
| #1 | Spatial geometry validation | Namukwanga | Merged |
| #2 | Spatial analytical queries | Namukwanga | Merged |
| #3 | Spatial operator testing | Namukwanga | Merged |
| #4 | Multimedia integrity triggers | Namukwanga | Merged |

All four contributions have been incorporated into the project's `main` branch.

## 5. Outstanding Work

| Task | Responsibility | Status |
|---|---|---|
| Confirm November 1 schema submission | IzuKanji Sinkolongo | reviewed |
| Review and approve this contribution record | Both members | reviewed |
| Test real image upload and retrieval | Both members, ownership to agree | Pending verification |
| Test image rotation and content-based search | Both members, ownership to agree | Pending verification |
| Test facility creation, movement, resizing and deletion through the GUI | Both members, ownership to agree | Pending verification |
| Verify application operation on Linux and Windows | Both members, ownership to agree | Pending verification |
| Complete final documentation and defence preparation | Both members | Pending |

## 6. Risks and Follow-up

The updated database schema has passed installation and targeted functional tests on the campus Oracle server.

However, the complete Java application's multimedia operations have not yet been tested end to end. Successful compilation and map visualization do not establish that every application feature works correctly.

Both members should continue maintaining descriptive Git commits, reviewing each other's changes, and documenting project progress.

The final project submission is due **6 December 2026**, followed by the project defence in week 13.

## 7. Team Review Confirmation

**Prepared by:** Namukwanga Musukwa  
**Prepared on:** 1 November 2026

**Reviewed by:** Izukanji Sinkolongo    
**Agreed follow-up actions:** To be confirmed during team review

This record should be updated after the team has reviewed and agreed on the contributions and outstanding responsibilities.