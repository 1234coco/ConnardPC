import random
import re
import sqlite3
import string
import jwt
import hashlib
import uuid
import base64
import encrypt
import logging
import time
import json
logging.basicConfig(level=logging.DEBUG, 
                            format="%(asctime)s - %(levelname)s - %(message)s",
                            handlers=[
                            logging.FileHandler("Database.log", encoding="utf-8"),  # Ghi log vào file với UTF-8
                            logging.StreamHandler()  # Hiển thị log trên console
                            ])
class mangeDB:
    def __init__(self):
        try:
            self.defaultConfig = open("default_config.json","r",encoding="utf-8").read()
            self.Database = sqlite3.connect("user.db",check_same_thread=False)
            self.cursor = self.Database.cursor()
            self.Database.execute("PRAGMA foreign_keys = ON;")
            self.secret = json.loads(open("config.json",mode="r",encoding="utf-8").read())["secret"]
            self.legth_HID = 40
        except:
            raise Exception("Không truy cập được Database")
    def init(self):
        self.cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='USERS'")
        check = self.cursor.fetchone()
        if check == None:
            self.cursor.execute("""CREATE TABLE USERS(
                ID INTEGER,
                USERNAME TEXT,
                PASSWORD TEXT,
                ROLES INTEGER,
                CONFIG TEXT)""")
        self.cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='ROLES'")
        check = self.cursor.fetchone()
        if check == None:
            self.cursor.execute("""CREATE TABLE ROLES(
                ID INTEGER,
                NAME TEXT,
                DESCRIPTION TEXT)""")
        self.cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='GAMES'")
        check = self.cursor.fetchone()
        if check == None:
            self.cursor.execute("""CREATE TABLE GAMES(
                ID INTEGER PRIMARY KEY AUTOINCREMENT,
                NAME TEXT,
                FILE TEXT,
                PATH TEXT,
                DESCRIPTION TEXT)""")
            logging.info("Đã tạo setup mới")
    def get_all_games(self):
        self.cursor.execute("SELECT ID, NAME, FILE, PATH, DESCRIPTION FROM GAMES")
        rows = self.cursor.fetchall()

        if not rows:
            print("Không có game nào trong cơ sở dữ liệu.")
            return []

        games = []
        for row in rows:
            ids = row[0]
            name = row[1]
            files = re.findall(r'"\s*([^"]*?)\s*"',row[2].replace('\'', '"').replace(r"\\","/"))
            paths = re.findall(r'"\s*([^"]*?)\s*"',row[3].replace('\'', '"').replace(r"\\","/"))
            description = row[4]
            
            game = {
                "id":ids,
                "name": name,
                "file": files,
                "path": paths,
                "description": description
            }
            games.append(game)

        print(games)
        return games
    def createJWT(self,userid):
        self.cursor.execute("SELECT ROLES FROM USERS WHERE ID = ?",(userid,))
        role = self.cursor.fetchone()
        print(type(role))
        if isinstance(role,tuple):
            payload = {
                "userid" : userid,
                "role" : role[0],
                "iat" : int(time.time()),
                "exp" : int(time.time())+604800
            }
            token = jwt.encode(payload, self.secret, algorithm="HS256")
            return token
        else:
            return False
    def verifyJWT(self,JWT:str):
        try:
        # Giải mã JWT và kiểm tra tính hợp lệ
            payload = jwt.decode(JWT, self.secret, algorithms=["HS256"])
            return payload
        except jwt.ExpiredSignatureError:
            # Nếu token đã hết hạn
            return "EXP"
        except jwt.InvalidTokenError:
            # Nếu token không hợp lệ
            return "INV"
    def createUser(self,name:str,password:str):
        try:
            ## Kiểm tra coi tk loz này tạo acc chưa
            self.cursor.execute("SELECT EXISTS(SELECT 1 FROM USERS WHERE USERNAME=?)",(name,))
            check = self.cursor.fetchone()
            if check[0] == 0:
                salt = base64.urlsafe_b64encode(encrypt.encoding_reversed((encrypt.encoding_base64(name))).encode("utf-8"))
                hash = hashlib.sha512()
                hash.update(password.encode("utf-8")+salt)
                hashed_password = base64.urlsafe_b64encode(hash.digest())
                hardwareId = ''.join(random.SystemRandom().choice(string.ascii_lowercase+string.ascii_uppercase + string.digits) for _ in range(self.legth_HID))
                id_user = int(time.time())
                self.cursor.execute("INSERT INTO USERS VALUES (?,?,?,?,?)",(id_user,name,hashed_password,0,hardwareId))
                self.Database.commit()
                logging.info(f"Đã tạo user {name}")
                return id_user
            else:
                logging.info(f"Tên {name} đã tồn tại trên hệ thống")
                return False
        except Exception as e:
            logging.error("Bị lỗi:"+str(e))
    def changePass(self,name,password):
        salt = base64.urlsafe_b64encode(encrypt.encoding_reversed((encrypt.encoding_base64(name))).encode("utf-8"))
        hash = hashlib.sha512()
        hash.update(password.encode("utf-8")+salt)
        hashed_password = base64.urlsafe_b64encode(hash.digest())
        print(hashed_password)
    def loginUser(self,name:str,password):
        try:
            self.cursor.execute("SELECT ID,PASSWORD FROM USERS WHERE USERNAME=?",(name,))
            check = self.cursor.fetchone()
            if check[1]!= None:
                salt = base64.urlsafe_b64encode(encrypt.encoding_reversed((encrypt.encoding_base64(name))).encode("utf-8"))
                hash = hashlib.sha512()
                hash.update(password.encode("utf-8")+salt)
                hashed_password = base64.urlsafe_b64encode(hash.digest())
                if check[1] == hashed_password:
                    return check[0]
                
        except Exception as e:
            logging.error(f"Địt m bị lỗi r:{e}")
    def nameUser(self,userid: int):
        self.cursor.execute("SELECT USERNAME FROM USERS WHERE ID=?",(userid,))
        check = self.cursor.fetchone()
        return check[0]
    def addDevices(self,Token:str):
        self.cursor.execute("SELECT EXISTS(SELECT TOKEN FROM TEMP_DEVICES WHERE TOKEN=?)",(Token,))
        check = self.cursor.fetchone()
        if check[0] == 0:
            self.cursor.execute("INSERT INTO TEMP_DEVICES VALUES (?,?)",(Token,None,))
            self.Database.commit()
            return
        else:
            return "EXT"
    def addDevicesAccount(self,userid:int,code:str):
        self.cursor.execute("SELECT EXISTS(SELECT TOKEN FROM TEMP_DEVICES WHERE TOKEN=?)",(code,))
        check = self.cursor.fetchone()
        if check[0] == 1:
            self.cursor.execute("SELECT EXISTS(SELECT ID FROM USERS WHERE ID=?)",(userid,))
            check = self.cursor.fetchone()
            if check[0] == 1:
                self.cursor.execute("SELECT HardwareID FROM USERS WHERE ID=?",(userid,))
                hardwareID = self.cursor.fetchone()
                self.cursor.execute("UPDATE TEMP_DEVICES SET HardwareID=? WHERE TOKEN=?",(hardwareID[0],code,))
                self.Database.commit()
                return
            return "USER_EXT"
        return "DEV_EXT"
    def getHID(self,code:str):
        self.cursor.execute("SELECT HardwareID FROM Temp_Devices WHERE TOKEN=?",(code,))
        HID = self.cursor.fetchone()
        return HID[0]
    def getAllDevices(self,userid:int):
        self.cursor.execute("SELECT HardwareID FROM USERS WHERE ID=?",(userid,))
        HID = self.cursor.fetchone()[0]
        self.cursor.execute("SELECT TOKEN FROM Temp_Devices WHERE HardwareID=?",(HID,))
        devices = self.cursor.fetchall()
        ls_devices = []
        for device in devices:
            ls_devices+=device
        return ls_devices
    def getConfig(self, userid: int, code=""):
        try:
            if userid != 0:
                self.cursor.execute("SELECT Config FROM DEVICES WHERE ID=?", (userid,))
            else:
                self.cursor.execute("SELECT CONFIG FROM DEVICES WHERE HardwareID=?", (code,))
            row = self.cursor.fetchone()
            if row:
                return json.loads(row[0])
            return False
        except Exception as e:
            logging.error(f"Lỗi getConfig: {e}")
            return False

    def addConfig(self, userid: int, device: str, id_game: int, types: int, limit: int = 0, end: int = 0):
        try:
            self.cursor.execute("SELECT Config FROM DEVICES WHERE ID=?", (userid,))
            row = self.cursor.fetchone()
            if not row:
                return False
            config = json.loads(row[0])
            if device not in config:
                config[device] = []
            game = {
                "id": id_game,
                "mode": "allow" if types == 0 else "pause" if types == 1 else "allow_limit",
                "running": False
            }
            if types == 1:
                game["timeEnd"] = end
            elif types == 2:
                game["limit"] = limit
                game["played"] = 0
            config[device].append(game)
            return self.changeConfig(userid, config)
        except Exception as e:
            logging.error(f"Lỗi addConfig: {e}")
            return False

    def changeConfig(self, userid: int, jsonConfig: dict, code: str = ""):
        try:
            if userid != 0:
                self.cursor.execute("SELECT EXISTS(SELECT 1 FROM DEVICES WHERE ID=?)", (userid,))
                if self.cursor.fetchone()[0] == 1:
                    self.cursor.execute("UPDATE DEVICES SET CONFIG=? WHERE ID=?", (json.dumps(jsonConfig,ensure_ascii=False), userid))
                    self.Database.commit()
                    logging.info("Đã cập nhật config thành công!")
                    return True
            else:
                self.cursor.execute("SELECT EXISTS(SELECT 1 FROM DEVICES WHERE HardwareID=?)", (code,))
                if self.cursor.fetchone()[0] == 1:
                    self.cursor.execute("UPDATE DEVICES SET CONFIG=? WHERE HardwareID=?", (json.dumps(jsonConfig,ensure_ascii=False), code))
                    self.Database.commit()
                    logging.info("Đã cập nhật config thành công!")
                    return True
            return "CNE"
        except Exception as e:
            logging.error(f"Lỗi changeConfig: {e}")
            return False
    def addGames(self,Name: str,Path: list,File: list, Description=""):
        try:
            self.cursor.execute("SELECT EXISTS(SELECT 1 FROM GAMES WHERE NAME=?)", (Name,))
            check = self.cursor.fetchone()
            if check[0] == 1:
                logging.info(f"Game {Name} đã tồn tại trong hệ thống.")
                return False
            self.cursor.execute("INSERT INTO GAMES (NAME,FILE,PATH,DESCRIPTION) VALUES (?,?,?,?)",(Name,str(Path),str(File),Description,))
            self.Database.commit()
            logging.info(f"Đã thêm thành công game:{Name},{str(Path)},{str(File)},{Description}")
        except Exception as e:
            logging.error(f"Đã bị lỗi khi thêm game:{str(e)}")
    def addPC(self,userid:int,code:str):
        self.cursor.execute("SELECT UpdateConfig FROM USERS WHERE ID=?", (userid,))
        check = self.cursor.fetchone()
        if check[0] == None:
            self.cursor.execute("UPDATE USERS SET UpdateConfig=? WHERE ID=?",(code,str(userid)))
            self.Database.commit()
        else:
            return "EXT"
        


if __name__ == "__main__":
    run = mangeDB()
    run.createUser("NgoLong","2008Long@")
    run.addGames("Minecraft",["TL.exe"],["%AppData%\\.tlauncher","%AppData%\\.minecraft"],"Game khối vuông")