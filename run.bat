@echo off
set PROJECT_DIR=C:\Users\730ri\OneDrive\Desktop\LoadBalancerPro
java --module-path "%PROJECT_DIR%\lib\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics -cp "%PROJECT_DIR%\bin;%PROJECT_DIR%\lib\*" gui.LoadBalancerGUI
pause


