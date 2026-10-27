# Local database setup

How to get a working local Oracle instance so you can develop CampusAtlas without going through the campus network. Follow this once per development machine. The final submission still has to run against the campus server, but working locally is faster for day-to-day work.

Target: **Oracle Database 18c Express Edition** on Windows. Linux steps are similar — package names change, listener behaviour is the same.

---

## 1. Which Oracle do you need

The project uses Oracle Multimedia (ORDIM) — `ORDSYS.ORDImage`, `SI_StillImage`, `SI_ScoreByFtrList`. ORDIM was removed starting in Oracle 19c, so you **must** install **18c XE** (or older). The campus server is 18c Enterprise Edition, so matching 18c XE locally is both sufficient and consistent.

Download: https://www.oracle.com/database/technologies/xe18c-downloads.html (Windows x64, ~2.4 GB, requires a free Oracle account).

Confirm you are **not** installing 21c XE — that version has no multimedia and the project will not run against it.

## 2. Installing

Run the InstallShield installer. Set the SYS/SYSTEM/PDBADMIN password when prompted (one password covers all three accounts). Keep it; the application will use it indirectly.

When done, verify the Windows services:

```powershell
Get-Service | Where-Object { $_.Name -like 'Oracle*' } | Format-Table Name, Status
```

Expect `OracleServiceXE` and `OracleOraDB18Home1TNSListener` both `Running`.

## 3. Fix the listener so `localhost` works

On a fresh install the 18c listener often binds only to the active network interface (for example a Tailscale or domain IP), not to `127.0.0.1`. JDBC connections to `localhost` will be refused in that case.

Edit `C:\app\<YOUR_USER>\product\18.0.0\dbhomeXE\network\admin\listener.ora` and change the TCP ADDRESS line to:

```
(ADDRESS = (PROTOCOL = TCP)(HOST = 0.0.0.0)(PORT = 1521))
```

Restart the listener service:

```powershell
Restart-Service OracleOraDB18Home1TNSListener -Force
```

Confirm `netstat -ano | findstr :1521` shows `0.0.0.0:1521 LISTENING`.

## 4. Create the project schema user

The application connects as a dedicated user (not SYS/SYSTEM). Create it inside the pluggable DB `XEPDB1`.

Save the following as `scratch/setup_user.sql` (any path works):

```sql
ALTER SESSION SET CONTAINER = XEPDB1;

CREATE USER cardb IDENTIFIED BY "<PASSWORD>"
  DEFAULT TABLESPACE USERS
  QUOTA UNLIMITED ON USERS;

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO cardb;

EXIT;
```

Replace `<PASSWORD>` with the password you want the app to use (same string you pass as `-Dpassword=...` later). Run it as SYSTEM:

```powershell
& 'C:\app\<YOUR_USER>\product\18.0.0\dbhomeXE\bin\sqlplus.exe' `
   -L 'system/<SYS_PASSWORD>@//localhost:1521/XEPDB1' `
   '@scratch\setup_user.sql'
```

Expect `User created.` and `Grant succeeded.`

## 5. Verify Multimedia is available

Still as SYSTEM against `XEPDB1`:

```sql
SELECT comp_id, status, version FROM dba_registry WHERE comp_id = 'ORDIM';
```

You want one row showing `ORDIM / VALID / 18.0.0.0.0`. If the row is missing or status is `INVALID`, the project will not work.

## 6. Load the project schema

Once `schema/schema.sql` exists (Phase 1 deliverable), run it as `cardb`:

```powershell
& 'C:\app\<YOUR_USER>\product\18.0.0\dbhomeXE\bin\sqlplus.exe' `
   -L 'cardb/<PASSWORD>@//localhost:1521/XEPDB1' `
   '@schema\schema.sql'
```

Then optionally `schema/sample_data.sql` with the same command.

## 7. Running the application

From the project root:

```powershell
.\gradlew.bat "-Dlogin=cardb" "-Dpassword=<PASSWORD>" run
```

The build pipes `-D` system properties into the running JVM via the Gradle `application` plugin. Once the login dialog is implemented (Phase 2), you can omit `-Dlogin` and `-Dpassword` and enter them in the UI instead.

## 8. Connecting with SQL Developer

Open **New Connection** and use:

| Field | Application user | DBA |
|-------|------------------|-----|
| Username | `cardb` | `SYSTEM` |
| Password | `<PASSWORD>` | `<SYS_PASSWORD>` |
| Hostname | `localhost` | `localhost` |
| Port | `1521` | `1521` |
| Service name | `XEPDB1` | `XEPDB1` |
| Role | Default | Default |

Use the SYSTEM connection only for admin tasks (adding users, granting). Day-to-day queries go through `cardb`.

## 9. Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `ORA-12514` service not registered | Wrong service name | Use `XEPDB1`, not `XE` |
| `127.0.0.1:1521 connection refused` | Listener bound to a different interface | See step 3 |
| `ORA-00942: table or view does not exist` on `product` or `facility_image` | Schema not loaded for this user | Re-run `schema/schema.sql` as `cardb` |
| `ORA-00904: SI_SCOREBYFTRLIST` | ORDIM not installed or Oracle version is 19c+ | Confirm step 5; reinstall 18c XE if needed |
