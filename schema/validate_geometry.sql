-- CampusAtlas: Spatial Geometry Validation
-- Validates geometry in all five spatial tables.

SELECT 'Building' AS category, name,
       SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geom, 0.005) AS validity
FROM building

UNION ALL

SELECT 'Parking Area', name,
       SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geom, 0.005)
FROM parking_area

UNION ALL

SELECT 'Walkway', name,
       SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geom, 0.005)
FROM walkway

UNION ALL

SELECT 'Green Space', name,
       SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geom, 0.005)
FROM green_space

UNION ALL

SELECT 'Water Area', name,
       SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geom, 0.005)
FROM water_area;