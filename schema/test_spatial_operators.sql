-- CampusAtlas: Spatial Operator Tests
-- Tests three Oracle Spatial operators.
-- Tested against the university Oracle 18c database.


-- TEST 1: SDO_RELATE
-- Find walkways that touch, cross or overlap buildings.

SELECT b.name AS building,
       w.name AS walkway
FROM building b
JOIN walkway w
ON SDO_RELATE(
    b.geom,
    w.geom,
    'mask=ANYINTERACT'
) = 'TRUE'
ORDER BY b.name;


-- TEST 2: SDO_WITHIN_DISTANCE
-- Find walkways within 100 units of the Main Library.

SELECT w.name AS walkway
FROM walkway w
CROSS JOIN building b
WHERE b.name = 'Main Library'
AND SDO_WITHIN_DISTANCE(
    w.geom,
    b.geom,
    'distance=100'
) = 'TRUE'
ORDER BY w.name;


-- TEST 3: SDO_NN
-- Find the three nearest parking areas to the Main Library.

SELECT p.name AS parking_area,
       ROUND(SDO_NN_DISTANCE(1), 2) AS distance
FROM parking_area p
CROSS JOIN building b
WHERE b.name = 'Main Library'
AND SDO_NN(
    p.geom,
    b.geom,
    'sdo_num_res=3',
    1
) = 'TRUE'
ORDER BY distance ASC;