#!/usr/bin/env python

from __future__ import division
from __future__ import print_function

import subprocess
import argparse


def adb(adb_args, command):
  cmd = "adb {} {}".format(adb_args, command)
  print("Executing adb command: " + cmd)
  subprocess.call(cmd, shell=True)

def main():

  i = 0
  while i < 60*24:
    hour = int(i / 60)
    minute = i % 60
    adb("", "shell /system/xbin/su 0 date {}:{}:00".format( hour, minute))
    adb("", "shell su 0 am broadcast -a android.intent.action.TIME_SET")
    if minute % 60 == 0:
      adb("", "shell input keyboard keyevent 13")
    i = i + 2

if __name__ == "__main__":
  main()
