cmake_minimum_required(VERSION 3.24)
project(smw_thumbnail_cache_tests LANGUAGES C CXX)

find_package(SDL2 REQUIRED CONFIG)
find_package(SDL2_image QUIET CONFIG)
if(NOT TARGET SDL2_image::SDL2_image)
    set(SMW_SDL_IMAGE_SOURCE "" CACHE PATH "Local SDL_image 2.x source tree")
    if(NOT EXISTS "${SMW_SDL_IMAGE_SOURCE}/CMakeLists.txt")
        message(FATAL_ERROR "Install SDL2_image or set SMW_SDL_IMAGE_SOURCE to its local source tree")
    endif()
    set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
    set(SDL2IMAGE_SAMPLES OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_TESTS OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_INSTALL OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_VENDORED OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_BACKEND_STB ON CACHE BOOL "" FORCE)
    foreach(format AVIF JPG JXL TIF WEBP)
        set(SDL2IMAGE_${format} OFF CACHE BOOL "" FORCE)
    endforeach()
    add_subdirectory("${SMW_SDL_IMAGE_SOURCE}" sdl-image)
endif()

add_executable(thumbnail-cache-test
    test_thumbnail_cache.cpp
    ../../src/common/gfx/ThumbnailCache.cpp
    ../../src/common/util/SdlHelpers.cpp
)
target_compile_features(thumbnail-cache-test PRIVATE cxx_std_17)
target_include_directories(thumbnail-cache-test PRIVATE ../../src/common)
target_link_libraries(thumbnail-cache-test PRIVATE SDL2::SDL2 SDL2_image::SDL2_image)
enable_testing()
add_test(NAME thumbnail-cache COMMAND thumbnail-cache-test)
