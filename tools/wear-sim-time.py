#!/usr/bin/env python

from __future__ import division
from __future__ import print_function

import subprocess
import argparse
import time


def adb(adb_args, command):
  cmd = "adb {} {}".format(adb_args, command)
  print("Executing adb command: " + cmd)
  subprocess.call(cmd, shell=True)

def main():
  adb("", "shell input keyevent KEYCODE_WAKEUP")
  time.sleep(1)
  i = 0
  while i < 60*24:
    hour = int(i / 60)
    minute = i % 60
    adb("", "shell /system/xbin/su 0 date {}:{}:00".format( hour, minute))
    adb("", "shell su 0 am broadcast -a android.intent.action.TIME_SET")
    if i % 240 == 0:
      adb("", "shell input keyevent 13")
    i = i + 4

if __name__ == "__main__":
  main()
