-- CampusAtlas: Multimedia Referential Integrity
-- Prevents images from referencing nonexistent facilities.

CREATE OR REPLACE TRIGGER trg_image_valid_facility
BEFORE INSERT OR UPDATE OF category, facility_id
ON facility_image
FOR EACH ROW

DECLARE
    v_id NUMBER;

BEGIN
    CASE :NEW.category

        WHEN 'BUILDING' THEN
            SELECT id INTO v_id
            FROM building
            WHERE id = :NEW.facility_id
            FOR UPDATE;

        WHEN 'PARKING_AREA' THEN
            SELECT id INTO v_id
            FROM parking_area
            WHERE id = :NEW.facility_id
            FOR UPDATE;

        WHEN 'WALKWAY' THEN
            SELECT id INTO v_id
            FROM walkway
            WHERE id = :NEW.facility_id
            FOR UPDATE;

        WHEN 'GREEN_SPACE' THEN
            SELECT id INTO v_id
            FROM green_space
            WHERE id = :NEW.facility_id
            FOR UPDATE;

        WHEN 'WATER_AREA' THEN
            SELECT id INTO v_id
            FROM water_area
            WHERE id = :NEW.facility_id
            FOR UPDATE;

        ELSE
            RAISE_APPLICATION_ERROR(
                -20002,
                'Invalid facility category'
            );

    END CASE;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(
            -20001,
            'Cannot attach image: facility does not exist'
        );
END;
/
-- Automatically delete a building's images
-- when the building is deleted.

CREATE OR REPLACE TRIGGER trg_building_delete_images
AFTER DELETE ON building
FOR EACH ROW

BEGIN
    DELETE FROM facility_image
    WHERE category = 'BUILDING'
      AND facility_id = :OLD.id;
END;
/
-- Automatically delete parking area images
-- when the parking area is deleted.

CREATE OR REPLACE TRIGGER trg_parking_delete_images
AFTER DELETE ON parking_area
FOR EACH ROW
BEGIN
    DELETE FROM facility_image
    WHERE category = 'PARKING_AREA'
      AND facility_id = :OLD.id;
END;
/
-- Automatically delete walkway images
CREATE OR REPLACE TRIGGER trg_walkway_delete_images
AFTER DELETE ON walkway
FOR EACH ROW
BEGIN
    DELETE FROM facility_image
    WHERE category = 'WALKWAY'
      AND facility_id = :OLD.id;
END;
/

-- Automatically delete green space images
CREATE OR REPLACE TRIGGER trg_green_delete_images
AFTER DELETE ON green_space
FOR EACH ROW
BEGIN
    DELETE FROM facility_image
    WHERE category = 'GREEN_SPACE'
      AND facility_id = :OLD.id;
END;
/

-- Automatically delete water area images
CREATE OR REPLACE TRIGGER trg_water_delete_images
AFTER DELETE ON water_area
FOR EACH ROW
BEGIN
    DELETE FROM facility_image
    WHERE category = 'WATER_AREA'
      AND facility_id = :OLD.id;
END;
/