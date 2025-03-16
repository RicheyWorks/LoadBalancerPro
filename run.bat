@echo off
setlocal

:: Navigate to the script's directory
cd /d "%~dp0"

:: Run JavaFX application with relative paths
java --module-path "lib\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics ^
     -cp "bin;lib\*" gui.LoadBalancerGUI

pause



