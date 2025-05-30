import time
import requests
import json
import psutil
import logging
import threading
import os
import pathlib
import shlex
import copy


class BlockGame:
    def __init__(self):
        self.running = False  # Dùng biến chia sẻ giữa các tiến trình
        self.request = requests.sessions.Session()
        self.pause_game = list()
        self.allow_game =  list()
        self.allow_limit_game =  list()
        self.stop_game =  list()
        self.can_run_game = list()
        with open("config.json","r",encoding="utf-8") as f:
            config = json.loads(f.read())
            self.host = config["Host"]
            self.jwtTest = config["Token"]
            
        # Cấu hình log
        logging.basicConfig(level=logging.INFO, 
                            format="%(asctime)s - %(levelname)s - %(message)s",
                            handlers=[
                            logging.FileHandler("GameBlockerService.log", encoding="utf-8"),  # Ghi log vào file với UTF-8
                            logging.StreamHandler()  # Hiển thị log trên console
                            ])
        logging.getLogger("requests").setLevel(logging.WARNING)
        
        
        # multiprocessing.Process(target=self.process_pause,args=(self.pause_game,), daemon=True).start()
        # 
    def start(self):
        self.listGame = self.load_game_list()
        threading.Thread(target=self.stop_process, daemon=True).start()
        self.monitor_server()
    def load_game_list(self):
        """Tải danh sách game từ API hoặc từ file nếu API không phản hồi."""
        try:
            self.config = self.request.get(self.host + "appConfig",params={"code":self.jwtTest}, timeout=5).json()
            response = self.request.get(self.host + "listGame", timeout=5)
            if response.status_code == 200:
                listGame = response.json()
                with open("Game.json", "w", encoding="utf-8") as file:
                    json.dump(listGame, file, indent=4)
                return listGame
        except requests.RequestException as e:
            logging.error(f"Lỗi kết nối API: {e}")

        # Nếu API lỗi, thử tải từ file
        if os.path.exists(".venv/asset/Game.json"):
            try:
                with open(".venv/asset/Game.json", "r", encoding="utf-8") as file:
                    return json.load(file)
            except json.JSONDecodeError:
                logging.error("File Game.json bị lỗi hoặc trống.")
        return []
    def monitor_server(self):
        """Theo dõi API để cập nhật trạng thái chạy."""
        while True:
            try:
                config = self.request.get(self.host + "appConfig",params={"code":self.jwtTest}, timeout=5)
                self.config =config.json()[self.jwtTest]
                print(self.config)
                if config.status_code == 200:
                    self.running = bool(self.request.get(self.host + "run",headers={"jwt":self.jwtTest}, timeout=5).text)
                        
            except requests.RequestException as e:
                logging.warning(f"Không thể kết nối API để cập nhật trạng thái: {e}")
            except:
                logging.warning("Chưa thêm thiết bị vì thế nên lỗi lòi mắt")
                self.running = False
            
            time.sleep(2)
    def control_access_file(self):
        pass
    def get_all_file_list(self,path):
        for root, dirs, files in os.walk(path):
            for name in dirs:
                print("Thư mục:", os.path.join(root, name))
            for name in files:
                print("Tập tin:", os.path.join(root, name))
    def stop_process(self):
        active = set()
        played_tracker = {}
        while True:
            if self.running:
                found = set()
                now = int(time.time())
                print(now)
                for proc in psutil.process_iter(attrs=['pid', 'name', 'cmdline']):
                    name = proc.info['name'] or ''
                    cmd  = ' '.join(proc.info.get('cmdline') or [])
                    for g in self.listGame:
                        if self.is_game_process(g, name.lower(), cmd):
                            gid = g['id']
                            for x in self.config:
                                if int(x["id"])==int(gid):
                                    print(x["id"])
                                    
                            cfg = next((x for x in self.config if int(x['id']) == int(gid)), None)
                            # Chưa có trong config → hỏi, rồi terminate
                            
                            if not cfg:
                                proc.terminate()
                            else:
                                mode = cfg['mode']
                                found.add((gid, cfg["mode"]))
                                
                                # ALLOW: bật running nếu chưa bật
                                print(type(cfg["running"]))
                                if mode == 'allow':
                                    if not cfg['running']:
                                    
                                        self.update_config(gid, 'allow', update=True, running=True)
    
                                # PAUSE: nếu timeEnd chưa đến thì terminate, ngược lại bật running
                                elif mode == 'pause':
                                    if cfg['timeEnd'] < now:
                                        self.update_config(gid, 'delete')
                                        proc.terminate()
                                    else:
                                        if cfg['running'] == False:
                                            self.update_config(gid, 'pause', update=True, running=True)
    
                                # ALLOW_LIMIT: kiểm tra played vs limit
                                elif mode == 'allow_limit':
                                    if cfg['played'] >= cfg['limit']:
                                        proc.terminate()
                                        self.update_config(gid, 'allow_limit', update=True, running=False)
                                    else:
                                        if not cfg['running']:
                                            self.update_config(gid, 'allow_limit', update=True, running=True)
    
                                        # In ra thông tin để debug
                                        print(f"Played_tracker:{str(played_tracker)}")
                                        print(f"Now:{now}")
    
                                        # Kiểm tra thời gian chơi
                                        last_played_time = played_tracker.get(str(gid), 0)
                                        if last_played_time == 0:
                                            played_tracker[str(gid)] = now  # Lưu thời gian lần đầu
                                        else:
                                            # Kiểm tra xem đã đủ 60s chưa
                                            if (now - last_played_time) >= 10:
                                                logging.info(f"Đã đủ 60s, bắt đầu cập nhật played của {str(gid)}")
                                                played_tracker[str(gid)] = now  # Cập nhật thời gian chơi
                                                self.update_config(gid, 'allow_limit', update=True, played=cfg['played'] + 1, running=True)
    
                                else:
                                    proc.terminate()
                            break
                        
                # Xóa game đã dừng
                stopped = active - found
                for gid, modes in stopped:
                    if str(gid) in played_tracker:
                        del played_tracker[str(gid)]  # Xóa khỏi tracker khi game dừng
                    self.update_config(gid, mode=modes, update=True, running=False)
    
                active = found
                time.sleep(5)

    # def split_game(self):
    #     self.allow_game.clear()
    #     self.allow_limit_game.clear()
    #     self.stop_game.clear()
    #     # The line `self.pause_game.valueclear()` seems to have a typo. It should be
    #     # `self.pause_game.clear()` instead of `self.pause_game.valueclear()`.
    #     # Thank!
    #     config = copy.deepcopy(self.config)
    #     self.pause_game.clear()
    #     change = False
    #     for game in self.listGame:
    #         game_id = game.get("id")
            
    #         config_game = next((g for g in config["games"] if g["id"] == game_id), None)

    #         if config_game:
    #             mode = config_game["mode"]
                
    #             if mode == "pause":
    #                 if config_game["timeEnd"]<=int(time.time()):
    #                     print(int(time.time()))
    #                     config["games"].remove(config_game)
    #                     change = True
    #                 else:
    #                     self.pause_game.append(game)
    #             elif mode == "allow":
    #                 self.allow_game.append(game)
    #             elif mode == "allow_limit":
    #                 if int(config_game["limit"])> int(config_game["played"]):
    #                     self.allow_limit_game.append(game)
            
    #         else:
    #             self.stop_game.append(game)
    #     if change:
    #         self.config = config
    #         self.update_config()
        
    #     self.can_run_game = self.allow_game + self.allow_limit_game + self.pause_game
        
    
    def split_path(self,path:str):
        try:
            split_path = shlex.split(path,posix=False)
            spn = []
            for i in split_path:
                try:
                    normall_path = pathlib.Path(i).as_posix()
                    spn.append(normall_path)
                except:
                    spn.append(i)
            normall_path = ""
            for i in spn:
                normall_path += i + " "
                
            return normall_path[:-1]
        except:
            raise Exception("Path dell hợp lệ")
    def is_game_process(self, game, process_name, cmdline):
        """Kiểm tra xem tiến trình có phải là game không."""
        for name in game.get("file", []):
            if name == process_name or name in cmdline:
                return True
        for path in game.get("path", []):
            if self.split_path(os.path.expandvars(path)) in cmdline and path != "":
                return True
        return False

    def add_game_to_config(self, game_id, mode, running):
        """Thêm game vào config qua endpoint /appConfig."""
        jsons = {"id": game_id, "mode": mode}
        payload = {
            "jsons": json.dumps(jsons),
            "device":self.jwtTest,
            "update": False
        }
        try:
            self.request.patch(self.host + "changeConfigApp", headers={"jwt": self.jwtTest}, params=payload)
            logging.info(f"Đã thêm game {game_id} vào config với mode {mode} và running {running}")
        except requests.RequestException as e:
            logging.error(f"Lỗi khi thêm game {game_id} vào config: {e}")

    def remove_game_from_config(self, game_id):
        """Xóa game khỏi config qua endpoint /appConfig."""
        payload = {
            "jsons": json.dumps({"id":game_id, "mode": "delete"}),
            "device":self.jwtTest,
            "update": False
        }
        try:
            self.request.patch(self.host + "changeConfigApp", params=payload)
            logging.info(f"Đã xóa game {game_id} khỏi config")
        except requests.RequestException as e:
            logging.error(f"Lỗi khi xóa game {game_id} khỏi config: {e}")

    def split_path(self, path: str):
        """Chia nhỏ đường dẫn và chuẩn hóa."""
        try:
            split_path = shlex.split(path, posix=False)
            spn = []
            for i in split_path:
                try:
                    normall_path = pathlib.Path(i).as_posix()
                    spn.append(normall_path)
                except:
                    spn.append(i)
            normall_path = ""
            for i in spn:
                normall_path += i + " "
            return normall_path[:-1]
        except:
            raise Exception("Path không hợp lệ")

    def delete_game(self, gameid: int):
        """Xóa game khỏi config (hàm cũ giữ lại để tương thích)."""
        payload = {
            "jsons": {"id": gameid, "mode": "delete"},
            "device":self.jwtTest,
            "update": False
        }
        print(self.config)
        self.request.patch(self.host + "changeConfigApp", params=payload)

    def update_config(self, gameid,mode,update=True,timeEnd=None,limit=None,played=None,running=None):
        """Cập nhật config với mode 'ask' (hàm cũ giữ lại để tương thích)."""
        json_data = {"id": gameid, "mode": mode}
        if timeEnd is not None:
            json_data["timeEnd"] = timeEnd
        if limit is not None:
            json_data["limit"] = limit
        if played is not None:
            json_data["played"] = played
        if running is not None:
            json_data["running"] = running
        payload = {
            "jsons": json.dumps(json_data),
            "device":self.jwtTest,
            "update": update,
           
        }
        print(self.config)
        self.request.patch(self.host + "changeConfigApp", params=payload)
if __name__ == "__main__":
    app = BlockGame()
    app.start()