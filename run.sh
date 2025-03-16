#!/bin/bash

# Ensure script runs in its own directory
cd "$(dirname "$0")"

# Run JavaFX application with correct module-path and classpath
java --module-path "lib/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics" \
     -cp "bin:lib/*" gui.LoadBalancerGUI

