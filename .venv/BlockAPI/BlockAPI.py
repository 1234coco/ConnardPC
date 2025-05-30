from fastapi import FastAPI,Request,HTTPException,Header
import json
import jwt
import sqlite3
import ManageDB
db = ManageDB.mangeDB()
app = FastAPI()
token = open("token.csv","r",encoding="utf-8").readlines()
@app.get("/")
def check():
    return True
@app.get("/listGame")
def listGame():
    return db.get_all_games()
@app.get("/run")
def running(jwt:str=Header()):
    res = db.verifyJWT(jwt)
    print(res)
    if res == "INV":
        config = db.getConfig(0,jwt)
        print(config)
        if config != False:
            return config["status"]
        return HTTPException(404,"Token không hợp lệ")
    elif res == "EXP":
        return HTTPException(405,"Token hết hạn, vui lòng tạo lại")
    else:
        userid = res["userid"] 
        config = db.getConfig(userid)
        print(config)
        if config != False:
            return config["status"]
@app.get("/config")
async def getConfig(jwt:str= Header()):
    res = db.verifyJWT(jwt)
    print(res)
    if res == "INV":
        config = db.getConfig(0,jwt)
        print(config)
        if config != False:
            return config
        return HTTPException(404,"Token không hợp lệ")
    elif res == "EXP":
        return HTTPException(405,"Token hết hạn, vui lòng tạo lại")
    else:
        userid = res["userid"]
        config = db.getConfig(userid)
        print(config)
        if config != False:
            return config
    return HTTPException(409,"Tài khoản không tồn tại!")
@app.get("/appConfig")
async def appConfig(code:str):
    if db.addDevices(code) == "EXT":
        res = db.getHID(code)
        if res == None:
            raise HTTPException(409,"Thiết bị không tồn tại!")
        else:
            return {code:db.getConfig(0,res)[code]}
            
@app.get("/about")
async def about(jwt:str=Header()):
    res = db.verifyJWT(jwt)
    if res == "INV":
        return HTTPException(404,"Token không hợp lệ")
    elif res == "EXP":
        return HTTPException(405,"Token hết hạn, vui lòng tạo lại")
    else:
        userid = res["userid"]
        return db.nameUser(userid)
@app.post("/addPC")
async def addPC(Code:str):
    if type(db.addDevices(Code)) is None:
        raise HTTPException(407,"PC đã tồn tại trong server")
    return
@app.post("/addPCAccount")
async def addPCAccount(code:str,Token:str = Header()):
    if code == "status":
        return HTTPException(404,"Tên không hợp lệ")
    res = db.verifyJWT(Token)
    if res == "INV":
        return HTTPException(404,"Token không hợp lệ")
    elif res == "EXP":
        return HTTPException(405,"Token hết hạn, vui lòng tạo lại")
    else:
        userid = res["userid"]
        response = db.addDevicesAccount(userid,code)
        if response == None:
            return
        if response == "USER_EXT":
            return "Tài khoản không tồn tại"
        else:
            return "Máy tính không tồn tại"
@app.get("/verify")
async def verify(code:str):
    try:
        HID = db.getHID(code)
        if HID != None:
            return
        raise HTTPException(404,"Token thiết bị không hợp lệ")
    except:
        raise HTTPException(400,"Lỗi Không xác định")
@app.patch("/setStatus")    
async def setStatu(status:int,Token:str = Header()):
    res = db.verifyJWT(Token)
    if res == "INV":
        return HTTPException(404,"Token không hợp lệ")
    elif res == "EXP":
        return HTTPException(405,"Token hết hạn, vui lòng tạo lại")
    else:
        userid = res["userid"]
        config = db.getConfig(userid)
        config["status"]=status
        db.changeConfig(userid,config)
@app.post("/signup")
async def signup(request:Request):
    payload = await request.json()
    try:
        user = payload.get("user")
        password = payload.get("password")
        if isinstance(password,str) and isinstance(user,str):
            create = db.createUser(user,password)
            print(type(create))
            if isinstance(create,int):
                jwts = db.createJWT(create)
                print(type(jwts))
                return {"access_token":jwts}
            if create == False:
                return HTTPException(status_code=407,detail="Tên tài khoản đã tồn tại, vui lòng chọn tên khác!")
        else:
            return HTTPException(status_code=408,detail="Không đủ thông tin đăng kí")
    except Exception as e:
        raise HTTPException(status_code=400,detail=f"Lỗi không xác định:{e}")
@app.get("/login")
async def login(user: str, password: str):
    user_id = db.loginUser(user, password)  # Gọi phương thức loginUser từ db
    if user_id:  # Nếu login thành công và user_id không phải là False
        return {"access_token": db.createJWT(user_id)}  # Trả về JWT token
    else:
        raise HTTPException(status_code=401, detail="Tài khoản hoặc mật khẩu không đúng")
@app.patch("/setConfig")
async def setConfig(config:str,Token: str=Header()):
    try:
        res = db.verifyJWT(Token)
        if res == "INV":
            cfg = json.loads(config)
            if db.changeConfig(0,cfg,Token) != "CNE":
                return True
            return HTTPException(404,"Token không hợp lệ")
        elif res == "EXP":
            return HTTPException(405,"Token hết hạn, vui lòng tạo lại")
        else:
            cfg = json.loads(config)
            db.changeConfig(res["userid"],cfg)
            return True
    except:
        return HTTPException(400,"Lỗi không xác định")
@app.get("/getAllDevices")
async def get_all_devices(jwt:str=Header()):
    try:
        res = db.verifyJWT(jwt)
        if res == "EXP":
            raise HTTPException(405, "Token hết hạn, vui lòng tạo lại")
        if res == "INV":
            raise HTTPException(404, "Token không hợp lệ")
        userid = res["userid"]
        return db.getAllDevices(userid)
        
    except Exception as e:
        raise HTTPException(400, f"Lỗi không xác định: {e}")
@app.patch("/changeConfig")
async def change_config(jsons: str,device: str, update: bool = False):
    try:
        HID = db.getHID(device)
        if HID[0]==None:
            raise HTTPException(404,"Thiết bị không tồn tại trên hệ thống")
        config = db.getConfig(0,HID)
        cfg = json.loads(jsons)

        if not config or device not in config:
            raise HTTPException(404, "Không tìm thấy config hoặc thiết bị")
 
        games = config[device]

        if cfg["mode"] == "delete":
            new_games = [game for game in games if int(game.get("id")) != int(cfg["id"])]
            if len(new_games) == len(games):
                raise HTTPException(404, "Game không tồn tại trong config")
            config[device] = new_games

        else:
            game_found = False
            updated = False
            for idx, game in enumerate(games):
                if int(game.get("id")) == int(cfg["id"]):
                    game_found = True
                    if update:
                        if game.get("mode") != cfg["mode"]:
                            raise HTTPException(413, "Vui lòng chọn chính xác chế độ!")
                        if cfg["mode"] == "allow":
                            for key in cfg:
                                if key not in ("id", "mode", "update"):
                                    game[key] = cfg[key]
                            updated = True
                        elif cfg["mode"] == "pause":
                            allowed_keys = {"timeEnd", "running"}
                            for key in cfg:
                                if key in allowed_keys:
                                    game[key] = cfg[key]
                            updated = True
                        elif cfg["mode"] == "allow_limit":
                            allowed_keys = {"limit", "played", "running"}
                            for key in cfg:
                                if key in allowed_keys:
                                    game[key] = cfg[key]
                            updated = True
                        elif cfg["mode"] == "ask":
                            allowed_keys = {"allow", "running"}
                            for key in cfg:
                                if key in allowed_keys:
                                    game[key] = cfg[key]
                            updated = True
                        else:
                            raise HTTPException(414, "Mode không tồn tại")
                    else:
                        if cfg["mode"] == "allow":
                            games[idx] = {"id": cfg["id"], "mode": "allow", "running": False}
                            updated = True
                        elif cfg["mode"] == "pause":
                            required = {"id", "mode", "timeEnd"}
                            if not required.issubset(cfg.keys()):
                                raise HTTPException(412, "Payload reset cho pause thiếu trường cần thiết")
                            games[idx] = {"id": cfg["id"], "mode": "pause", "timeEnd": cfg["timeEnd"], "running": False}
                            updated = True
                        elif cfg["mode"] == "allow_limit":
                            required = {"id", "mode", "limit"}
                            if not required.issubset(cfg.keys()):
                                raise HTTPException(412, "Payload reset cho allow_limit thiếu trường cần thiết")
                            games[idx] = {"id": cfg["id"], "mode": "allow_limit", "limit": cfg["limit"], "played": cfg.get("played", 0), "running": False}
                            updated = True
                        elif cfg["mode"] == "ask":
                            required = {"id"}
                            if not required.issubset(cfg.keys()):
                                raise HTTPException(412, "Payload reset cho ask thiếu trường cần thiết")
                            games[idx] = {"id": cfg["id"], "mode": "ask", "allow": False, "running": False}
                            updated = True
                        else:
                            raise HTTPException(414, "Mode không tồn tại")
                    break

            if not game_found and not update:
                if cfg["mode"] == "allow":
                    new_game = {"id": cfg["id"], "mode": "allow", "running": False}
                elif cfg["mode"] == "pause":
                    required = {"id", "mode", "timeEnd"}
                    if not required.issubset(cfg.keys()):
                        raise HTTPException(412, "Payload reset cho pause thiếu trường cần thiết")
                    new_game = {"id": cfg["id"], "mode": "pause", "timeEnd": cfg["timeEnd"], "running": False}
                elif cfg["mode"] == "allow_limit":
                    required = {"id", "mode", "limit"}
                    if not required.issubset(cfg.keys()):
                        raise HTTPException(412, "Payload reset cho allow_limit thiếu trường cần thiết")
                    new_game = {"id": cfg["id"], "mode": "allow_limit", "limit": cfg["limit"], "played": cfg.get("played", 0), "running": False}
                elif cfg["mode"] == "ask":
                    required = {"id"}
                    if not required.issubset(cfg.keys()):
                        raise HTTPException(412, "Payload reset cho ask thiếu trường cần thiết")
                    new_game = {"id": cfg["id"], "mode": "ask", "allow": False, "running": False}
                else:
                    raise HTTPException(414, "Mode không tồn tại")
                games.append(new_game)
                updated = True
            elif not game_found and update:
                raise HTTPException(404, "Game không tồn tại trong config")


        ret = db.changeConfig(0, config, HID)
        

        if ret == "CNE":
            raise HTTPException(409, "Cập nhật config không thành công")
        return {"success": True}

    except HTTPException as he:
        raise he
    except Exception as e:
        raise HTTPException(400, f"Lỗi không xác định: {e}")
# async def change_config(jsons: str,device: str, update: bool = False, jwt: str = Header()):
#     try:
#         res = db.verifyJWT(jwt)
#         if res == "EXP":
#             raise HTTPException(405, "Token hết hạn, vui lòng tạo lại")

#         cfg = json.loads(jsons)

#         if "id" not in cfg or "mode" not in cfg:
#             raise HTTPException(412, "json không hợp lệ, thiếu id hoặc mode")

#         if res == "INV":
#             config = db.getConfig(0, jwt)
#         else:
#             config = db.getConfig(res["userid"])

#         if not config or device not in config:
#             raise HTTPException(404, "Không tìm thấy config hoặc thiết bị")

#         games = config[device]

#         if cfg["mode"] == "delete":
#             new_games = [game for game in games if game.get("id") != cfg["id"]]
#             if len(new_games) == len(games):
#                 raise HTTPException(404, "Game không tồn tại trong config")
#             config[device] = new_games

#         else:
#             game_found = False
#             updated = False
#             for idx, game in enumerate(games):
#                 if int(game.get("id")) == int(cfg["id"]):
#                     game_found = True
#                     if update:
#                         if game.get("mode") != cfg["mode"]:
#                             raise HTTPException(413, "Vui lòng chọn chính xác chế độ!")
#                         if cfg["mode"] == "allow":
#                             for key in cfg:
#                                 if key not in ("id", "mode", "update"):
#                                     game[key] = cfg[key]
#                             updated = True
#                         elif cfg["mode"] == "pause":
#                             allowed_keys = {"timeEnd", "running"}
#                             for key in cfg:
#                                 if key in allowed_keys:
#                                     game[key] = cfg[key]
#                             updated = True
#                         elif cfg["mode"] == "allow_limit":
#                             allowed_keys = {"limit", "played", "running"}
#                             for key in cfg:
#                                 if key in allowed_keys:
#                                     game[key] = cfg[key]
#                             updated = True
#                         elif cfg["mode"] == "ask":
#                             allowed_keys = {"allow", "running"}
#                             for key in cfg:
#                                 if key in allowed_keys:
#                                     game[key] = cfg[key]
#                             updated = True
#                         else:
#                             raise HTTPException(414, "Mode không tồn tại")
#                     else:
#                         if cfg["mode"] == "allow":
#                             games[idx] = {"id": cfg["id"], "mode": "allow", "running": False}
#                             updated = True
#                         elif cfg["mode"] == "pause":
#                             required = {"id", "mode", "timeEnd"}
#                             if not required.issubset(cfg.keys()):
#                                 raise HTTPException(412, "Payload reset cho pause thiếu trường cần thiết")
#                             games[idx] = {"id": cfg["id"], "mode": "pause", "timeEnd": cfg["timeEnd"], "running": False}
#                             updated = True
#                         elif cfg["mode"] == "allow_limit":
#                             required = {"id", "mode", "limit"}
#                             if not required.issubset(cfg.keys()):
#                                 raise HTTPException(412, "Payload reset cho allow_limit thiếu trường cần thiết")
#                             games[idx] = {"id": cfg["id"], "mode": "allow_limit", "limit": cfg["limit"], "played": cfg.get("played", 0), "running": False}
#                             updated = True
#                         elif cfg["mode"] == "ask":
#                             required = {"id"}
#                             if not required.issubset(cfg.keys()):
#                                 raise HTTPException(412, "Payload reset cho ask thiếu trường cần thiết")
#                             games[idx] = {"id": cfg["id"], "mode": "ask", "allow": False, "running": False}
#                             updated = True
#                         else:
#                             raise HTTPException(414, "Mode không tồn tại")
#                     break

#             if not game_found and not update:
#                 if cfg["mode"] == "allow":
#                     new_game = {"id": cfg["id"], "mode": "allow", "running": False}
#                 elif cfg["mode"] == "pause":
#                     required = {"id", "mode", "timeEnd"}
#                     if not required.issubset(cfg.keys()):
#                         raise HTTPException(412, "Payload reset cho pause thiếu trường cần thiết")
#                     new_game = {"id": cfg["id"], "mode": "pause", "timeEnd": cfg["timeEnd"], "running": False}
#                 elif cfg["mode"] == "allow_limit":
#                     required = {"id", "mode", "limit"}
#                     if not required.issubset(cfg.keys()):
#                         raise HTTPException(412, "Payload reset cho allow_limit thiếu trường cần thiết")
#                     new_game = {"id": cfg["id"], "mode": "allow_limit", "limit": cfg["limit"], "played": cfg.get("played", 0), "running": False}
#                 elif cfg["mode"] == "ask":
#                     required = {"id"}
#                     if not required.issubset(cfg.keys()):
#                         raise HTTPException(412, "Payload reset cho ask thiếu trường cần thiết")
#                     new_game = {"id": cfg["id"], "mode": "ask", "allow": False, "running": False}
#                 else:
#                     raise HTTPException(414, "Mode không tồn tại")
#                 games.append(new_game)
#                 updated = True
#             elif not game_found and update:
#                 raise HTTPException(404, "Game không tồn tại trong config")


#         if res == "INV":
#             ret = db.changeConfig(0, config, jwt)
#         else:
#             ret = db.changeConfig(res["userid"], config)

#         if ret == "CNE":
#             raise HTTPException(409, "Cập nhật config không thành công")
#         return {"success": True}

#     except HTTPException as he:
#         raise he
#     except Exception as e:
#         raise HTTPException(400, f"Lỗi không xác định: {e}")
@app.patch("/changeConfigApp")
async def change_config_app(jsons: str,device: str, update: bool = False):
    try:
        HID = db.getHID(device)
        if HID[0]==None:
            raise HTTPException(404,"Thiết bị không tồn tại trên hệ thống")
        config = db.getConfig(0,HID)
        cfg = json.loads(jsons)

        if not config or device not in config:
            raise HTTPException(404, "Không tìm thấy config hoặc thiết bị")

        games = config[device]

        if cfg["mode"] == "delete":
            new_games = [game for game in games if int(game.get("id")) != int(cfg["id"])]
            if len(new_games) == len(games):
                raise HTTPException(404, "Game không tồn tại trong config")
            config[device] = new_games

        else:
            game_found = False
            updated = False
            for idx, game in enumerate(games):
                if int(game.get("id")) == int(cfg["id"]):
                    game_found = True
                    if update:
                        if game.get("mode") != cfg["mode"]:
                            raise HTTPException(413, "Vui lòng chọn chính xác chế độ!")
                        if cfg["mode"] == "allow":
                            for key in cfg:
                                if key not in ("id", "mode", "update"):
                                    game[key] = cfg[key]
                            updated = True
                        elif cfg["mode"] == "pause":
                            allowed_keys = {"timeEnd", "running"}
                            for key in cfg:
                                if key in allowed_keys:
                                    game[key] = cfg[key]
                            updated = True
                        elif cfg["mode"] == "allow_limit":
                            allowed_keys = {"limit", "played", "running"}
                            for key in cfg:
                                if key in allowed_keys:
                                    game[key] = cfg[key]
                            updated = True
                        elif cfg["mode"] == "ask":
                            allowed_keys = {"allow", "running"}
                            for key in cfg:
                                if key in allowed_keys:
                                    game[key] = cfg[key]
                            updated = True
                        else:
                            raise HTTPException(414, "Mode không tồn tại")
                    else:
                        if cfg["mode"] == "allow":
                            games[idx] = {"id": cfg["id"], "mode": "allow", "running": False}
                            updated = True
                        elif cfg["mode"] == "pause":
                            required = {"id", "mode", "timeEnd"}
                            if not required.issubset(cfg.keys()):
                                raise HTTPException(412, "Payload reset cho pause thiếu trường cần thiết")
                            games[idx] = {"id": cfg["id"], "mode": "pause", "timeEnd": cfg["timeEnd"], "running": False}
                            updated = True
                        elif cfg["mode"] == "allow_limit":
                            required = {"id", "mode", "limit"}
                            if not required.issubset(cfg.keys()):
                                raise HTTPException(412, "Payload reset cho allow_limit thiếu trường cần thiết")
                            games[idx] = {"id": cfg["id"], "mode": "allow_limit", "limit": cfg["limit"], "played": cfg.get("played", 0), "running": False}
                            updated = True
                        elif cfg["mode"] == "ask":
                            required = {"id"}
                            if not required.issubset(cfg.keys()):
                                raise HTTPException(412, "Payload reset cho ask thiếu trường cần thiết")
                            games[idx] = {"id": cfg["id"], "mode": "ask", "allow": False, "running": False}
                            updated = True
                        else:
                            raise HTTPException(414, "Mode không tồn tại")
                    break

            if not game_found and not update:
                if cfg["mode"] == "allow":
                    new_game = {"id": cfg["id"], "mode": "allow", "running": False}
                elif cfg["mode"] == "pause":
                    required = {"id", "mode", "timeEnd"}
                    if not required.issubset(cfg.keys()):
                        raise HTTPException(412, "Payload reset cho pause thiếu trường cần thiết")
                    new_game = {"id": cfg["id"], "mode": "pause", "timeEnd": cfg["timeEnd"], "running": False}
                elif cfg["mode"] == "allow_limit":
                    required = {"id", "mode", "limit"}
                    if not required.issubset(cfg.keys()):
                        raise HTTPException(412, "Payload reset cho allow_limit thiếu trường cần thiết")
                    new_game = {"id": cfg["id"], "mode": "allow_limit", "limit": cfg["limit"], "played": cfg.get("played", 0), "running": False}
                elif cfg["mode"] == "ask":
                    required = {"id"}
                    if not required.issubset(cfg.keys()):
                        raise HTTPException(412, "Payload reset cho ask thiếu trường cần thiết")
                    new_game = {"id": cfg["id"], "mode": "ask", "allow": False, "running": False}
                else:
                    raise HTTPException(414, "Mode không tồn tại")
                games.append(new_game)
                updated = True
            elif not game_found and update:
                raise HTTPException(404, "Game không tồn tại trong config")


        ret = db.changeConfig(0, config, HID)
        

        if ret == "CNE":
            raise HTTPException(409, "Cập nhật config không thành công")
        return {"success": True}

    except HTTPException as he:
        raise he
    except Exception as e:
        raise HTTPException(400, f"Lỗi không xác định: {e}")

# def changeConfig(jsons: str,update=False,jwt: str=Header()):
#     try:
#         res = db.verifyJWT(jwt)
#         if res == "INV":
#             cfg = json.loads(jsons)
#             config = db.getConfig(0,jwt)
#             if config == False:
#                 raise HTTPException(404,"Token không hợp lệ")
#             elif not "id" in cfg or not "mode" in cfg:
#                 raise HTTPException(412,"json không hợp lệ")
#             for i in config:
#                 if cfg["id"] == i["id"]:
#                     if cfg["mode"]==i["mode"]:
#                         if cfg["mode"] == "allow":
#                             return True
#                         if cfg["mode"] == "pause":
#                             try:
#                                 if cfg["timeEnd"] != i["timeEnd"]:
#                                     i["timeEnd"] = cfg["timeEnd"]
#                             except:
#                                 raise HTTPException(412,"json không hợp lệ")
#                         if cfg["mode"]=="allow_limit":
#                             try:
#                                 if cfg["limit"]!=i["limit"]:
#                                     i["limit"]=cfg["limit"]
#                             except:
#                                 raise HTTPException(412,"json không hợp lệ")
#             return HTTPException(404,"Token không hợp lệ")
#         elif res == "EXP":
#             return HTTPException(405,"Token hết hạn, vui lòng tạo lại")
#         else:
#             cfg = json.loads(jsons)
#             config = db.getConfig(res["userid"])
#             tempC = {"status":config["status"],
#                      "games":[]}
#             if config == False:
#                 raise HTTPException(404,"Token không hợp lệ")
#             elif not "id" in cfg or not "mode" in cfg:
#                 raise HTTPException(412,"json không hợp lệ")
#             for i in config["games"]:
#                 if cfg["id"] == i["id"]:
#                     if update:
#                         if cfg["mode"]==i["mode"]:
#                             match cfg["mode"]:
#                                 case "allow":
#                                     pass
#                                 case "pause":
#                                     pause_key = ["timeEnd","running"]
#                                     for y in cfg.keys():
#                                         if y in pause_key:
#                                             i[y] = cfg[y]
#                                 case "allow_limit":
#                                     allow_limit_key = ["limit","played","running"]
#                                     for y in cfg.keys():
#                                         if y in allow_limit_key:
#                                             i[y] = cfg[y]
#                                 case _:
#                                     raise HTTPException(414,"Mode ảo quá! không tồn tại")
#                         else:
#                             raise HTTPException(413,"Vui lòng chọn chính xác chế độ!")
#                     else:
#                         if cfg["mode"] == "allow":
#                             i = {}
#                             i = {"id":cfg["id"],
#                                  "mode":"allow",
#                                  "running":False}
#                         if cfg["mode"] == "pause":
#                             pause_key = ["timeEnd"]
#                             try:
#                                 if all(key in cfg for key in pause_key):
#                                     i = {}
#                                     i = {"id":cfg["id"],
#                                      "mode":"pause",
#                                      "timeEnd":cfg["timeEnd"],
#                                      "running":False}
#                                 else:
#                                     raise HTTPException(412,"json không hợp lệ")
#                             except:
#                                 raise HTTPException(412,"json không hợp lệ")
#                         if cfg["mode"]=="allow_limit":
#                             allow_limit_key = ["limit"]
#                             try:
#                                 if all(key in cfg for key in pause_key):
#                                     i = {}
#                                     i = {"id":cfg["id"],
#                                      "mode": "allow_limit",
#                                      "limit": cfg["limit"],
#                                      "played": 0,
#                                      "running": False}
#                                 else:
#                                     raise HTTPException(412,"json không hợp lệ")
#                             except:
#                                 raise HTTPException(412,"json không hợp lệ")
#                 tempC["games"].append(i)
#             if db.changeConfig(0,tempC,jwt) != "CNE":
#                 return True
        
    