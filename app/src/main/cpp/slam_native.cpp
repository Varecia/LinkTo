#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include "System.h"
#include "ImuTypes.h"
#include "sophus/se3.hpp"

#define LOG_TAG "ORB_SLAM3"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

ORB_SLAM3::System *g_slam_system = nullptr;
ORB_SLAM3::System::eSensor g_sensor_type = ORB_SLAM3::System::MONOCULAR;

Sophus::SE3f g_latest_pose;
bool g_has_pose = false;

struct IMUBuffer {
  std::vector<ORB_SLAM3::IMU::Point> measurements;
  double lastTimestamp = -1;

  void clear() {
    measurements.clear();
  }

  bool empty() const {
    return measurements.empty();
  }

  size_t size() const {
    return measurements.size();
  }
} g_imuBuffer;

pthread_mutex_t g_pose_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t g_imu_mutex = PTHREAD_MUTEX_INITIALIZER;

extern "C" {
JNIEXPORT void JNICALL
Java_com_tos_linkto_utils_slam_SlamManager_nativeInit(
  JNIEnv *env,
  jobject /* this */,
  jstring vocabPath,
  jstring configPath,
  jboolean useIMU) {
  const char *vocab = env->GetStringUTFChars(vocabPath, nullptr);
  const char *config = env->GetStringUTFChars(configPath, nullptr);

  if (useIMU) {
    g_sensor_type = ORB_SLAM3::System::IMU_MONOCULAR;
  } else {
    g_sensor_type = ORB_SLAM3::System::MONOCULAR;
  }

  g_slam_system = new ORB_SLAM3::System(vocab, config, g_sensor_type, false);

  env->ReleaseStringUTFChars(vocabPath, vocab);
  env->ReleaseStringUTFChars(configPath, config);

  g_has_pose = false;
  g_imuBuffer.clear();

  LOGI("SLAM System initialized, useIMU=%d", useIMU);
}

JNIEXPORT void JNICALL
Java_com_tos_linkto_utils_slam_SlamManager_nativeProcessIMU(
  JNIEnv *env,
  jobject /* this */,
  jfloat accX, jfloat accY, jfloat accZ,
  jfloat gyroX, jfloat gyroY, jfloat gyroZ,
  jlong timestamp) {
  if (!g_slam_system || g_sensor_type != ORB_SLAM3::System::IMU_MONOCULAR) {
    return;
  }

  double timestamp_sec = timestamp / 1e9;

  ORB_SLAM3::IMU::Point imuPoint(
    accX, accY, accZ,
    gyroX, gyroY, gyroZ,
    timestamp_sec
  );

  pthread_mutex_lock(&g_imu_mutex);
  g_imuBuffer.measurements.push_back(imuPoint);
  pthread_mutex_unlock(&g_imu_mutex);

  LOGD("IMU buffered, count=%lu, ts=%.6f", g_imuBuffer.size(), timestamp_sec);
}

JNIEXPORT jfloatArray JNICALL
Java_com_tos_linkto_utils_slam_SlamManager_nativeProcessFrame(
  JNIEnv *env,
  jobject /* this */,
  jbyteArray data,
  jint width,
  jint height,
  jlong timestamp) {
  if (!g_slam_system) {
    LOGE("SLAM system not initialized");
    return nullptr;
  }

  jbyte *jpegData = env->GetByteArrayElements(data, nullptr);
  std::vector<uchar> imgData(jpegData, jpegData + env->GetArrayLength(data));
  cv::Mat img = cv::imdecode(imgData, cv::IMREAD_COLOR);
  env->ReleaseByteArrayElements(data, jpegData, JNI_ABORT);

  if (img.empty()) {
    LOGE("Failed to decode image");
    return nullptr;
  }

  cv::Mat gray;
  cv::cvtColor(img, gray, cv::COLOR_BGR2GRAY);

  double timestamp_sec = timestamp / 1e9;

  Sophus::SE3f pose;

  if (g_sensor_type == ORB_SLAM3::System::IMU_MONOCULAR) {
    pthread_mutex_lock(&g_imu_mutex);
    std::vector<ORB_SLAM3::IMU::Point> imuMeas = g_imuBuffer.measurements;
    g_imuBuffer.clear();
    pthread_mutex_unlock(&g_imu_mutex);

    if (!imuMeas.empty()) {
      LOGD("Processing frame with %lu IMU measurements", imuMeas.size());
      pose = g_slam_system->TrackMonocular(gray, timestamp_sec, imuMeas);
    } else {
      pose = g_slam_system->TrackMonocular(gray, timestamp_sec);
    }
  } else {
    pose = g_slam_system->TrackMonocular(gray, timestamp_sec);
  }

  pthread_mutex_lock(&g_pose_mutex);
  g_latest_pose = pose;
  g_has_pose = true;
  pthread_mutex_unlock(&g_pose_mutex);

  jfloatArray result = nullptr;

  Eigen::Matrix4f eigMat = pose.matrix();
  bool isZero = true;
  for (int i = 0; i < 16; i++) {
    if (eigMat.data()[i] != 0.0f) {
      isZero = false;
      break;
    }
  }

  if (!isZero) {
    result = env->NewFloatArray(16);
    jfloat poseArray[16];
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        poseArray[i * 4 + j] = eigMat(i, j);
      }
    }
    env->SetFloatArrayRegion(result, 0, 16, poseArray);
  }

  return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_tos_linkto_utils_slam_SlamManager_nativeGetCameraPose(
  JNIEnv *env,
  jobject /* this */) {
  pthread_mutex_lock(&g_pose_mutex);

  if (!g_slam_system || !g_has_pose) {
    pthread_mutex_unlock(&g_pose_mutex);
    return nullptr;
  }

  Eigen::Matrix4f eigMat = g_latest_pose.matrix();
  pthread_mutex_unlock(&g_pose_mutex);

  jfloatArray result = env->NewFloatArray(16);
  jfloat poseArray[16];
  for (int i = 0; i < 4; i++) {
    for (int j = 0; j < 4; j++) {
      poseArray[i * 4 + j] = eigMat(i, j);
    }
  }
  env->SetFloatArrayRegion(result, 0, 16, poseArray);

  return result;
}

JNIEXPORT jint JNICALL
Java_com_tos_linkto_utils_slam_SlamManager_nativeGetTrackingState(
  JNIEnv *env,
  jobject /* this */) {
  if (!g_slam_system) {
    return -1;
  }

  pthread_mutex_lock(&g_pose_mutex);
  bool hasPose = g_has_pose;
  pthread_mutex_unlock(&g_pose_mutex);

  return hasPose ? 1 : 0;
}

JNIEXPORT void JNICALL
Java_com_tos_linkto_utils_slam_SlamManager_nativeReset(
  JNIEnv *env,
  jobject /* this */) {
  if (g_slam_system) {
    g_slam_system->Reset();

    pthread_mutex_lock(&g_pose_mutex);
    g_has_pose = false;
    pthread_mutex_unlock(&g_pose_mutex);

    pthread_mutex_lock(&g_imu_mutex);
    g_imuBuffer.clear();
    pthread_mutex_unlock(&g_imu_mutex);

    LOGI("SLAM System reset");
  }
}

JNIEXPORT void JNICALL
Java_com_tos_linkto_utils_slam_SlamManager_nativeShutdown(
  JNIEnv *env,
  jobject /* this */) {
  if (g_slam_system) {
    g_slam_system->Shutdown();

    while (!g_slam_system->isShutDown()) {
      usleep(1000);
    }

    delete g_slam_system;
    g_slam_system = nullptr;

    LOGI("SLAM System shutdown");
  }

  pthread_mutex_lock(&g_pose_mutex);
  g_has_pose = false;
  pthread_mutex_unlock(&g_pose_mutex);

  pthread_mutex_lock(&g_imu_mutex);
  g_imuBuffer.clear();
  pthread_mutex_unlock(&g_imu_mutex);
}
} // extern "C"
