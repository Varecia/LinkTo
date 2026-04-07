cmake_minimum_required(VERSION 3.22)
project("orb_slam3_android")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

set(LIB_ROOT ${CMAKE_SOURCE_DIR}/libs)

###### OpenCV ######
set(OpenCV_DIR ${LIB_ROOT}/OpenCV-android-sdk/sdk/native/jni)

###### Eigen3 ######
set(EIGEN3_ROOT ${LIB_ROOT}/eigen)
add_subdirectory(${EIGEN3_ROOT} ${CMAKE_CURRENT_BINARY_DIR}/eigen)
set(Eigen3_DIR ${CMAKE_CURRENT_BINARY_DIR}/eigen)
include_directories(${EIGEN3_ROOT})

###### Boost #######
set(BOOST_ROOT ${LIB_ROOT}/boost)
set(Boost_INCLUDE_DIR ${BOOST_ROOT}/${ANDROID_ABI}/include)
set(Boost_LIBRARY_DIR ${BOOST_ROOT}/${ANDROID_ABI}/lib)
set(Boost_USE_STATIC_LIBS ON)
set(Boost_USE_MULTITHREADED ON)
add_library(boost_system STATIC IMPORTED)
add_library(boost_serialization STATIC IMPORTED)
set_target_properties(boost_system PROPERTIES IMPORTED_LOCATION ${Boost_LIBRARY_DIR}/libboost_system.a)
set_target_properties(boost_serialization PROPERTIES IMPORTED_LOCATION ${Boost_LIBRARY_DIR}/libboost_serialization.a)
include_directories(${Boost_INCLUDE_DIR})

##### OpenSSL ######
set(OPENSSL_ROOT ${LIB_ROOT}/OpenSSL/openssl-1.1.1k-clang)
add_library(crypto SHARED IMPORTED)
add_library(ssl SHARED IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION ${OPENSSL_ROOT}/${ANDROID_ABI}/lib/libcrypto.so.1.1)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION ${OPENSSL_ROOT}/${ANDROID_ABI}/lib/libssl.so.1.1)
include_directories(${OPENSSL_ROOT}/include)

#### ORB_SLAM3 #####
set(ORB_SLAM3_ROOT ${LIB_ROOT}/ORB_SLAM3)

    ###### DBoW2 #######
    set(DBoW2_ROOT ${ORB_SLAM3_ROOT}/Thirdparty/DBoW2)
    add_subdirectory(${DBoW2_ROOT} ${CMAKE_CURRENT_BINARY_DIR}/ORB_SLAM3/Thirdparty/DBoW2)

    ####### g2o ########
    set(g2o_ROOT ${ORB_SLAM3_ROOT}/Thirdparty/g2o)

    ###### Sophus ######
    set(Sophus_ROOT ${ORB_SLAM3_ROOT}/Thirdparty/Sophus)
    add_subdirectory(${Sophus_ROOT} ${CMAKE_CURRENT_BINARY_DIR}/ORB_SLAM3/Thirdparty/Sophus)

add_subdirectory(${ORB_SLAM3_ROOT} ${CMAKE_CURRENT_BINARY_DIR}/ORB_SLAM3)
include_directories(${ORB_SLAM3_ROOT} ${ORB_SLAM3_ROOT}/CameraModels)

###### native ######
add_library(slam_native SHARED slam_native.cpp)
target_link_libraries(slam_native
        eigen
        ORB_SLAM3
        ${OpenCV_LIBS}
        crypto
        ssl
        log
        android
        dl
)
target_include_directories(slam_native PRIVATE
        ${EIGEN3_ROOT}
        ${ORB_SLAM3_ROOT}/include/CameraModels
        ${ORB_SLAM3_ROOT}/include
        ${DBoW2_ROOT}
        ${g2o_ROOT}
        ${Sophus_ROOT}
        ${OpenCV_DIR}/include
)

###### debug #######
message(STATUS "")
message(STATUS "================== Configuration ==================")
message(STATUS "Library root: ${LIB_ROOT}")
message(STATUS "OpenCV version: ${OpenCV_VERSION}")
message(STATUS "OpenCV libs: ${OpenCV_LIBS}")
message(STATUS "ORB-SLAM3 root: ${ORB_SLAM3_ROOT}")
message(STATUS "Eigen3 root: ${EIGEN3_ROOT}")
message(STATUS "Boost root: ${BOOST_ROOT}")
message(STATUS "OpenSSL root: ${OPENSSL_ROOT}")
message(STATUS "===================================================")
message(STATUS "")
