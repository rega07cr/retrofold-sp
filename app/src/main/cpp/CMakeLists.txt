cmake_minimum_required(VERSION 3.22)

# mGBA viene scaricato dal workflow CI (Linux) o da tools/fetch-mgba.sh
# in app/src/main/cpp/mgba. Non committare il sorgente (vedi .gitignore).
set(MGBA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/mgba")
if(NOT EXISTS "${MGBA_DIR}/CMakeLists.txt")
    message(FATAL_ERROR
        "mGBA sorgenti mancanti in ${MGBA_DIR}. "
        "Esegui tools/fetch-mgba.sh oppure lascia fare al workflow CI.")
endif()

project(retrofold_gba LANGUAGES C CXX)

# Nome distinto per non scontrarsi col target "mgba" upstream.
set(BINARY_NAME mgba_core)
# Niente LTO: più robusto cross-ABI e più veloce in CI.
set(BUILD_LTO OFF CACHE BOOL "" FORCE)

add_subdirectory("${MGBA_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/mgba" EXCLUDE_FROM_ALL)

add_library(retrofold_gba SHARED emulator_bridge.cpp)

# Gli include di mGBA arrivano via PUBLIC dal target mgba_core.
target_link_libraries(retrofold_gba PRIVATE
    mgba_core
    android
    log
    aaudio
)
