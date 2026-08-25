cmake_minimum_required(VERSION 3.24)

project(skycast_core VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(skycast_core
    src/assessment.cpp
)

target_include_directories(skycast_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

if(MSVC)
    target_compile_options(skycast_core PRIVATE /W4 /permissive-)
else()
    target_compile_options(skycast_core PRIVATE -Wall -Wextra -Wpedantic -Wconversion)
endif()

enable_testing()

add_executable(skycast_core_tests
    tests/assessment_test.cpp
)

target_link_libraries(skycast_core_tests PRIVATE skycast_core)

add_test(NAME skycast_core_tests COMMAND skycast_core_tests)
