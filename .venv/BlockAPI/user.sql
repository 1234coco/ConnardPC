--
-- File generated with SQLiteStudio v3.4.17 on Sun Apr 20 09:49:04 2025
--
-- Text encoding used: System
--
PRAGMA foreign_keys = off;
BEGIN TRANSACTION;

-- Table: DEVICES
CREATE TABLE IF NOT EXISTS DEVICES (ID INTEGER REFERENCES USERS (ID), HardwareID TEXT REFERENCES USERS (UpdateConfig), Config);

-- Table: GAMES
CREATE TABLE IF NOT EXISTS GAMES(
                ID INTEGER PRIMARY KEY AUTOINCREMENT,
                NAME TEXT,
                FILE TEXT,
                PATH TEXT,
                DESCRIPTION TEXT);
INSERT INTO GAMES (ID, NAME, FILE, PATH, DESCRIPTION) VALUES (1, 'Minecraft', '[''TL.exe'']', '[''%AppData%\\.tlauncher'', ''%AppData%\\.minecraft'']', 'Game kh?i vuông');
INSERT INTO GAMES (ID, NAME, FILE, PATH, DESCRIPTION) VALUES (2, 'Osu!', '[''osu!.exe'']', '[''%LOCALAPPDATA%\\osu!'',''''%LOCALAPPDATA%\\osulazer'']', 'K? hu? di?t bàn phím');
INSERT INTO GAMES (ID, NAME, FILE, PATH, DESCRIPTION) VALUES (3, 'Riot', '[''RiotClientServices.exe'']', '[''C:\\Riot Games\\Riot Client'']', 'Ð?nh cao c?a s? cay cú');
INSERT INTO GAMES (ID, NAME, FILE, PATH, DESCRIPTION) VALUES (4, 'Steam', '[''steam.exe'',''steamwebhelper.exe'']', '[''C:\\Program Files (x86)\\Steam'']', 'K? bán game m?nh nh?t l?ch s?');
INSERT INTO GAMES (ID, NAME, FILE, PATH, DESCRIPTION) VALUES (5, 'Miside', '[''MiSideFull.exe'']', '[C:\\Program Files\\Miside]', 'Gojo 2.0');
INSERT INTO GAMES (ID, NAME, FILE, PATH, DESCRIPTION) VALUES (6, 'Roblox', '[''RobloxStudioInstaller.exe'',''RobloxPlayerLauncher.exe'',''RobloxPlayerInstaller.exe'',''RobloxPlayerBeta.exe'']', '[''C:\\Program Files (x86)\\Roblox'']', 'Noi th? hi?n trình hack');

-- Table: ROLES
CREATE TABLE IF NOT EXISTS ROLES(
                ID INTEGER,
                NAME TEXT,
                DESCRIPTION TEXT);

-- Table: USERS
CREATE TABLE IF NOT EXISTS USERS (ID INTEGER UNIQUE, USERNAME TEXT, PASSWORD TEXT, ROLES INTEGER, HardwareID TEXT UNIQUE);
INSERT INTO USERS (ID, USERNAME, PASSWORD, ROLES, HardwareID) VALUES (1744509525, 'NgoLong', X'696B4E7567656F454A5F78426F4A762D32484553684D6A357A3036395A6E304B746E744F5A2D7253505A706F42486C6935556B38557A4B4B484649565A424B4B5F4167493361475339395A4E776F4B52496E666566773D3D', 0, 'JKHJFjkrehilhHILREHrRLeilRLRuELr569867656');

-- Trigger: autoAddDevices
CREATE TRIGGER IF NOT EXISTS autoAddDevices AFTER INSERT ON USERS FOR EACH ROW BEGIN INSERT INTO DEVICES (ID, HardwareID, CONFIG) VALUES (NEW.ID, NEW.HardwareID, '[]'); END;

-- Trigger: autoDeleteDevices
CREATE TRIGGER IF NOT EXISTS autoDeleteDevices AFTER DELETE ON USERS BEGIN DELETE FROM DEVICES WHERE ID = OLD.ID; END;

COMMIT TRANSACTION;
PRAGMA foreign_keys = on;
