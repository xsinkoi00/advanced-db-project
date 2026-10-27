-- CampusAtlas — demonstration sample data
-- Phase 1 deliverable. Spatial objects only; photographs are uploaded by the
-- Java application in Phase 3 (ORDImage uploads require a JDBC-side bytes
-- transfer that is cleaner than wiring a DIRECTORY object inside a shared
-- schema script).
--
-- Local Cartesian plane, 0..1000 units in each axis. The layout is not meant
-- to resemble any real campus; it is dense enough to make spatial queries
-- interesting (one green space contains one water area; one walkway crosses
-- several buildings).

------------------------------------------------------------
-- 1. Clear anything already loaded
------------------------------------------------------------
DELETE FROM facility_image;
DELETE FROM building;
DELETE FROM parking_area;
DELETE FROM walkway;
DELETE FROM green_space;
DELETE FROM water_area;
COMMIT;

------------------------------------------------------------
-- 2. Buildings (5)
------------------------------------------------------------
INSERT INTO building (name, use_type, description, geom) VALUES (
  'Main Library','ACADEMIC','Three-floor reading and reference library',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(100,700, 300,700, 300,900, 100,900, 100,700)));

INSERT INTO building (name, use_type, description, geom) VALUES (
  'Science Hall','ACADEMIC','Physics and chemistry labs',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(400,700, 650,700, 650,900, 400,900, 400,700)));

INSERT INTO building (name, use_type, description, geom) VALUES (
  'Administration','ADMIN','Registrar and finance offices',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(700,750, 900,750, 900,900, 700,900, 700,750)));

INSERT INTO building (name, use_type, description, geom) VALUES (
  'Cafeteria','SERVICE','Main dining hall',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(150,400, 300,400, 300,550, 150,550, 150,400)));

INSERT INTO building (name, use_type, description, geom) VALUES (
  'East Dormitory','RESIDENTIAL','Student housing, 180 rooms',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(650,100, 900,100, 900,350, 650,350, 650,100)));

------------------------------------------------------------
-- 3. Parking areas (4)
------------------------------------------------------------
INSERT INTO parking_area (name, capacity, geom) VALUES (
  'North Parking Lot', 120,
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(50,920, 380,920, 380,990, 50,990, 50,920)));

INSERT INTO parking_area (name, capacity, geom) VALUES (
  'East Parking Lot', 80,
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(920,700, 990,700, 990,990, 920,990, 920,700)));

INSERT INTO parking_area (name, capacity, geom) VALUES (
  'South Parking Lot', 60,
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(650,50, 950,50, 950,100, 650,100, 650,50)));

INSERT INTO parking_area (name, capacity, geom) VALUES (
  'West Staff Parking', 20,
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(10,100, 60,100, 60,400, 10,400, 10,100)));

------------------------------------------------------------
-- 4. Walkways (4)
------------------------------------------------------------
INSERT INTO walkway (name, path_type, geom) VALUES (
  'Main Pedestrian Path','PAVED',
  SDO_GEOMETRY(2002, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,2,1),
    SDO_ORDINATE_ARRAY(200,900, 500,800, 800,850)));

INSERT INTO walkway (name, path_type, geom) VALUES (
  'Library Connector','PAVED',
  SDO_GEOMETRY(2002, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,2,1),
    SDO_ORDINATE_ARRAY(200,700, 200,550)));

INSERT INTO walkway (name, path_type, geom) VALUES (
  'North-South Spine','PAVED',
  SDO_GEOMETRY(2002, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,2,1),
    SDO_ORDINATE_ARRAY(500,100, 500,900)));

INSERT INTO walkway (name, path_type, geom) VALUES (
  'East-West Cross','GRAVEL',
  SDO_GEOMETRY(2002, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,2,1),
    SDO_ORDINATE_ARRAY(100,500, 900,500)));

------------------------------------------------------------
-- 5. Green spaces (3)
------------------------------------------------------------
INSERT INTO green_space (name, managed, geom) VALUES (
  'Central Lawn','Y',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(350,400, 600,400, 600,650, 350,650, 350,400)));

INSERT INTO green_space (name, managed, geom) VALUES (
  'North Garden','Y',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(400,920, 600,920, 600,990, 400,990, 400,920)));

INSERT INTO green_space (name, managed, geom) VALUES (
  'Memorial Park','N',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(100,100, 300,100, 300,350, 100,350, 100,100)));

------------------------------------------------------------
-- 6. Water areas (2)
------------------------------------------------------------
INSERT INTO water_area (name, geom) VALUES (
  'Main Pond',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(400,200, 550,200, 550,320, 400,320, 400,200)));

INSERT INTO water_area (name, geom) VALUES (
  'Central Fountain',
  SDO_GEOMETRY(2003, NULL, NULL,
    SDO_ELEM_INFO_ARRAY(1,1003,1),
    SDO_ORDINATE_ARRAY(480,500, 530,500, 530,540, 480,540, 480,500)));

COMMIT;

------------------------------------------------------------
-- 7. Row-count sanity check
------------------------------------------------------------
SELECT 'building'     AS tbl, COUNT(*) AS cnt FROM building
UNION ALL SELECT 'parking_area', COUNT(*) FROM parking_area
UNION ALL SELECT 'walkway',      COUNT(*) FROM walkway
UNION ALL SELECT 'green_space',  COUNT(*) FROM green_space
UNION ALL SELECT 'water_area',   COUNT(*) FROM water_area;

EXIT;
