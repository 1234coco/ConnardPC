CREATE TRIGGER auto_add_device
AFTER INSERT ON USERS
FOR EACH ROW
BEGIN
    INSERT INTO DEVICES (ID, CONFIG)
    VALUES (NEW.ID, '[]');
END;