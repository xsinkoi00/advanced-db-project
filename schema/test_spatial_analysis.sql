-- CampusAtlas: Spatial Analysis Tests
-- Tests area, length and distance calculations.

-- 1. Calculate building areas
SELECT name,
       ROUND(SDO_GEOM.SDO_AREA(geom, 0.005), 2) AS area
FROM building
ORDER BY area DESC;


-- 2. Calculate walkway lengths
SELECT name,
       ROUND(SDO_GEOM.SDO_LENGTH(geom, 0.005), 2) AS length
FROM walkway
ORDER BY length DESC;


-- 3. Distance from Main Library to parking areas
SELECT p.name AS parking_area,
       ROUND(
           SDO_GEOM.SDO_DISTANCE(b.geom, p.geom, 0.005),
           2
       ) AS distance
FROM building b
CROSS JOIN parking_area p
WHERE b.name = 'Main Library'
ORDER BY distance ASC;