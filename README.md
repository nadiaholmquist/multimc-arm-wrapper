# MultiMC Java wrapper for ARM devices

This repository contains a script that can be used to get Minecraft running on ARM devices through MultiMC.

## Installation
1. Install [MultiMC](https://multimc.org/), on Arch/Manjaro it is available as `multimc5` from the AUR.
2. Start up MultiMC, and when it asks you to pick a Java installation, click Browse and navigate to where you cloned this repository to, and select the `java` script.
3. Using MultiMC should work normally from here, add an account and create an instance
4. Have fun!

## Limitations
This script currently does not support Minecraft versions older than 1.13, as those use LWJGL 2 which does not have official ARM binaries and will have to be set up in a different way.
