#!/bin/bash 
adb forward tcp:4444 localabstract:/adb-hub
adb connect localhost:4444
#Finds .csv files. tr -d removes the DOS carriage return
FILE=$(adb -s localhost:4444 shell ls /sdcard/\*.csv | tr -d '\r')
adb -s localhost:4444 pull $FILE .
adb -s localhost:4444 shell rm $FILE
