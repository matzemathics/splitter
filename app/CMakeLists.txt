cmake_minimum_required(VERSION 3.4.1)

include_directories( src/main/cpp )

# one day we will be able to replace those lines with find_library(opus-lib opus)
add_library(opus SHARED IMPORTED)
string(REPLACE "/app/build/intermediates/" "/libopus/build/intermediates/" OPUS_TARGET "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libopus.so")
include_directories(../libopus/src/main/cpp/include)
set_target_properties(opus PROPERTIES IMPORTED_LOCATION ${OPUS_TARGET})

find_package (oboe REQUIRED CONFIG)

add_library( opusjni SHARED src/main/cpp/jni.cpp )

target_link_libraries( opusjni oboe::oboe log opus )
