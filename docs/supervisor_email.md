# Supervisor email — draft

Single email to Marek Rychly, covering the three approval items the implementation plan calls out. Send once; wait for the reply before writing the DDL that depends on it. Edit the subject/opening to match whatever thread you already have with him; the body is the part that matters.

---

**Subject:** CampusAtlas (PDBe 26/27Z, team Sinkolongo/Musukwa) — three questions before SQL schema submission

Dear Mr. Rychly,

We are the CampusAtlas team (Izukanji Sinkolongo 287734, Namukwanga Musukwa 287733). Before we submit the SQL schema on 2026-11-01 we would like to confirm three points so that what we build matches what the course expects.

**1. Multimedia approach.** In our submitted project plan (Section 5) we proposed computing color histograms in Java and storing them in Oracle as numeric feature rows. After verifying that the campus database runs Oracle 18c EE (where Oracle Multimedia is still available), we would prefer to use the Oracle-side `ORDSYS.ORDImage` + `SI_StillImage` / `SI_ScoreByFtrList` approach instead — it matches the course template, keeps the feature extraction inside the database, and is simpler. Do you have any objection to this change?

**2. Oracle Spatial Java API.** To read and write `SDO_GEOMETRY` values from Java we plan to use `oracle.spatial.geometry.JGeometry`. Is this already part of the Oracle library bundle the assignment allows, or do we need explicit approval to add it as a Gradle dependency? If approval is required, please let us know what to attach to the request.

**3. Campus schema account.** Could you confirm the username/schema we should use on `gort.fit.vutbr.cz` for uploading and testing our schema script? We would like to run it there at least once before the 2026-11-01 deadline so we are not surprised by any schema-level differences between our local Oracle 18c XE environment and the campus server.

Thank you,
Izukanji Sinkolongo (for the CampusAtlas team)

---

## After the reply arrives

Update [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) Section 7 ("Open items") to record the decisions, delete the ones that are answered, and let Namukwanga know if the multimedia approach changed from Approach A.
