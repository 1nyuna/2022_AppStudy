#include <jni.h>
#include <string>
#include <jni.h>
#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_myapplication_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jint

JNICALL
Java_com_example_myapplication_MainActivity_addition(JNIEnv *env, jobject thiz, jint a, jint b) {
    // TODO: implement addition()
    return a+b;
}