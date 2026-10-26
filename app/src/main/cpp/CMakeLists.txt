cmake_minimum_required(VERSION 3.22)
project(retrofold_gba)
add_library(retrofold_gba SHARED emulator_bridge.cpp)
find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(retrofold_gba ${log-lib} ${android-lib})
# Per il core reale: add_subdirectory(mgba) e link mgba-static.
# Istruzioni complete in README.md sezione "Collegare mGBA".
