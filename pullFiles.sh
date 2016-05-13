#!/bin/bash 
adb forward tcp:4444 localabstract:/adb-hub
adb connect localhost:4444
#Finds .csv files. tr -d removes the DOS carriage return
for i in $( adb -s localhost:4444 shell ls /sdcard/\*.csv | tr -d '\r' ); do
	adb -s localhost:4444 pull $i .
done
for i in $( adb -s localhost:4444 shell ls /sdcard/\*.csv | tr -d '\r' ); do
	adb -s localhost:4444 shell mv $i /sdcard/old/
done
