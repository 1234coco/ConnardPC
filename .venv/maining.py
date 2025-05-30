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
        self.host = "http://localhost:8000/"
        self.running = False  # Dùng biến chia sẻ giữa các tiến trình
        self.request = requests.sessions.Session()
        self.pause_game = list()
        self.allow_game =  list()
        self.allow_limit_game =  list()
        self.stop_game =  list()
        self.can_run_game = list()
        self.iduser = 1743771578
        self.jwtTest="JKHJFjkrehilhHILREHrRLeilRLRuELr569867656"
        # Cấu hình log
        logging.basicConfig(level=logging.INFO, 
                            format="%(asctime)s - %(levelname)s - %(message)s",
                            handlers=[
                            logging.FileHandler("GameBlockerService.log", encoding="utf-8"),  # Ghi log vào file với UTF-8
                            logging.StreamHandler()  # Hiển thị log trên console
                            ])
        logging.getLogger("requests").setLevel(logging.WARNING)
        self.jwts = self.request.get(self.host + "login",params={"user":"Mẹ Mày Béo","password":"2008Long@"}, timeout=5).text
        self.listGame = self.load_game_list()
        threading.Thread(target=self.stop_process, daemon=True).start()
        self.monitor_server()
        
        # multiprocessing.Process(target=self.process_pause,args=(self.pause_game,), daemon=True).start()
        # 
    def load_game_list(self):
        """Tải danh sách game từ API hoặc từ file nếu API không phản hồi."""
        try:
            self.config = self.request.get(self.host + "config",headers={"jwt":self.jwtTest}, timeout=5).json()
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
                config = self.request.get(self.host + "config",headers={"jwt":self.jwtTest}, timeout=5)
                self.config =config.json()
                if config.status_code == 200:
                    self.running = bool(self.config["status"])
                    self.split_game()
                        
            except requests.RequestException as e:
                logging.warning(f"Không thể kết nối API để cập nhật trạng thái: {e}")
            
            time.sleep(2)
    def stop_process(self):
        # Vẫn giữ nguyên code hiện tại của bạn để quét các tiến trình
        # và tắt những game có trong self.stop_game.
        stop_game = self.stop_game[:]  # copy ban đầu
        while True:
            if stop_game != self.stop_game[:]:
                stop_game = self.stop_game[:]  # cập nhật nếu list thay đổi
            logging.debug(f"Trạng thái running: {self.running}")
            if self.running:
                for process in psutil.process_iter(attrs=['pid', 'name', 'cmdline']):
                    cmdline = self.split_path(" ".join(process.info['cmdline']) if process.info['cmdline'] else "")
                    off = False
                    for game in stop_game:
                        breaked = False
                        for name in game.get("file", []):
                            if name == process.info["name"].lower() or name in cmdline:
                                logging.warning(f"Phát hiện game: {process.info['name']} (PID: {process.info['pid']}) - Đang tắt (stop)")
                                psutil.Process(process.info['pid']).terminate()
                                off = True
                                breaked = True
                                break
                        if breaked:
                            break
                        for path in game.get("path", []):
                            if self.split_path(os.path.expandvars(path)) in cmdline and path != "":
                                logging.warning(f"Phát hiện game: {process.info['name']} (PID: {process.info['pid']}) - Đang tắt (stop)")
                                psutil.Process(process.info['pid']).terminate()
                                off = True
                                break
                        if off:
                            break
            time.sleep(5)


    def allow_process(self):
        """
        Xử lý cho các game thuộc mode: 
          - pause: nếu thời gian hiện tại >= timeEnd thì xoá game khỏi config.
          - allow: chỉ cập nhật trạng thái chạy.
          - allow_limit: tăng played mỗi phút; nếu played>=limit thì chuyển game sang stop (xoá khỏi config).
        """
        # Dùng để lưu thời gian lần cập nhật cuối cho từng game (dành cho allow_limit)
        last_update = {}
        while True:
            if self.running:
                now = int(time.time())
                # Duyệt qua danh sách các game cho phép chạy (can_run_game)
                for game in self.can_run_game:
                    game_id = game.get("id")
                    # Lấy config của game (để cập nhật played, running, etc.)
                    config_game = next((g for g in self.config["games"] if g["id"] == game_id), None)
                    if not config_game:
                        continue  # Nếu không có trong config, bỏ qua

                    mode = config_game.get("mode", "")
                    # Nếu là pause, kiểm tra thời gian hết hạn
                    if mode == "pause":
                        if now >= int(config_game.get("timeEnd", 0)):
                            logging.info(f"Game {game_id} (pause) đã hết hạn (timeEnd <= now). Xoá khỏi config.")
                            try:
                                self.config["games"].remove(config_game)
                                self.update_config()
                            except Exception as e:
                                logging.error(f"Lỗi khi xoá game {game_id} khỏi config: {e}")
                            continue

                    # Kiểm tra xem game có đang chạy hay không
                    found_running = False
                    for process in psutil.process_iter(attrs=['pid', 'name', 'cmdline']):
                        try:
                            cmdline = self.split_path(" ".join(process.info['cmdline']) if process.info['cmdline'] else "")
                        except Exception:
                            continue
                        # So sánh bằng file name
                        for name in game.get("file", []):
                            if name == process.info["name"].lower() or name in cmdline:
                                found_running = True
                                break
                        if not found_running:
                            for path in game.get("path", []):
                                if self.split_path(os.path.expandvars(path)) in cmdline and path != "":
                                    found_running = True
                                    break
                        if found_running:
                            break

                    # Cập nhật trạng thái running trong config cho game
                    self.update_state(game_id, found_running)

                    if not found_running:
                        # Nếu không chạy, không cần xử lý played (và có thể reset last_update)
                        last_update.pop(game_id, None)
                        continue

                    # Nếu game đang chạy và thuộc loại allow_limit, tăng played mỗi phút
                    if mode == "allow_limit":
                        previous = last_update.get(game_id, now)
                        # Nếu đã chạy ít nhất 60 giây từ lần cập nhật cuối
                        if now - previous >= 60:
                            config_game["played"] = config_game.get("played", 0) + 1
                            last_update[game_id] = now
                            logging.info(f"Game {game_id} (allow_limit): tăng played lên {config_game['played']}")
                            # Kiểm tra nếu played đã đạt limit, chuyển game sang stop
                            if config_game["played"] >= int(config_game.get("limit", 0)):
                                logging.info(f"Game {game_id} đã đạt giới hạn (played >= limit). Chuyển sang stop.")
                                try:
                                    self.config["games"].remove(config_game)
                                    self.update_config()
                                except Exception as e:
                                    logging.error(f"Lỗi khi chuyển game {game_id} sang stop: {e}")
            time.sleep(5)
    def split_game(self):
        self.allow_game.clear()
        self.allow_limit_game.clear()
        self.stop_game.clear()
        # The line `self.pause_game.valueclear()` seems to have a typo. It should be
        # `self.pause_game.clear()` instead of `self.pause_game.valueclear()`.
        # Thank!
        config = copy.deepcopy(self.config)
        self.pause_game.clear()
        change = False
        for game in self.listGame:
            game_id = game.get("id")
            
            config_game = next((g for g in config["games"] if g["id"] == game_id), None)

            if config_game:
                mode = config_game["mode"]
                
                if mode == "pause":
                    if config_game["timeEnd"]<=int(time.time()):
                        print(int(time.time()))
                        config["games"].remove(config_game)
                        change = True
                    else:
                        self.pause_game.append(game)
                elif mode == "allow":
                    self.allow_game.append(game)
                elif mode == "allow_limit":
                    if int(config_game["limit"])> int(config_game["played"]):
                        self.allow_limit_game.append(game)
            
            else:
                self.stop_game.append(game)
        if change:
            self.config = config
            self.update_config()
        
        self.can_run_game = self.allow_game + self.allow_limit_game + self.pause_game
        
    
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
    def update_state(self, gameid: int, state: bool):
        for listC, game in enumerate(self.config["games"]):  # Dùng enumerate để lấy cả chỉ số và phần tử
            if game["id"] == gameid:
                if game["running"] != state:
                    game["running"] = state
                    print(self.config)
                    self.update_config()
                    return
                else:
                    pass  # Trường hợp trạng thái đã giống nhau, không làm gì cả
    def update_config(self):
        payload = {
                "config": json.dumps(self.config)
            }
        print(self.config)
        self.request.patch(self.host + "setConfig", headers={"Token": self.jwtTest}, params=payload)
if __name__ == "__main__":
    app = BlockGame()