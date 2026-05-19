#include <jni.h>
#include <tox/toxav.h>
#include <android/log.h>

#include <map>

#define LOG_TAG "NativeToxAv"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM* g_av_vm;
static jmethodID mid_onCall;
static jmethodID mid_onCallState;
static jmethodID mid_onAudioReceiveFrame;
static jmethodID mid_onVideoReceiveFrame;
static jmethodID mid_onAudioBitRate;
static jmethodID mid_onVideoBitRate;
static jmethodID mid_onGroupAudio;

static std::map<ToxAV*, jobject> av_listeners;

JNIEnv* get_av_env() {
    JNIEnv* env;
    if (g_av_vm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) return env;
    g_av_vm->AttachCurrentThread(&env, nullptr);
    return env;
}

void cb_call(ToxAV *av, uint32_t friend_number, bool audio_enabled, bool video_enabled, void *user_data) {
    JNIEnv* env = get_av_env();
    jobject listener = av_listeners[av];
    if (listener && mid_onCall) {
        env->CallVoidMethod(listener, mid_onCall, (jint)friend_number, audio_enabled, video_enabled);
    }
}

void cb_call_state(ToxAV *av, uint32_t friend_number, uint32_t state, void *user_data) {
    JNIEnv* env = get_av_env();
    jobject listener = av_listeners[av];
    if (listener && mid_onCallState) {
        env->CallVoidMethod(listener, mid_onCallState, (jint)friend_number, (jint)state);
    }
}

void cb_audio_receive_frame(ToxAV *av, uint32_t friend_number, const int16_t *pcm, size_t sample_count, uint8_t channels, uint32_t sampling_rate, void *user_data) {
    JNIEnv* env = get_av_env();
    jobject listener = av_listeners[av];
    if (listener && mid_onAudioReceiveFrame) {
        jshortArray arr = env->NewShortArray(sample_count * channels);
        env->SetShortArrayRegion(arr, 0, sample_count * channels, (const jshort*)pcm);
        env->CallVoidMethod(listener, mid_onAudioReceiveFrame, (jint)friend_number, arr, (jint)sample_count, (jint)channels, (jint)sampling_rate);
        env->DeleteLocalRef(arr);
    }
}

void cb_video_receive_frame(ToxAV *av, uint32_t friend_number, uint16_t width, uint16_t height, const uint8_t *y, const uint8_t *u, const uint8_t *v, int32_t y_stride, int32_t u_stride, int32_t v_stride, void *user_data) {
    JNIEnv* env = get_av_env();
    jobject listener = av_listeners[av];
    if (listener && mid_onVideoReceiveFrame) {
        jbyteArray ya = env->NewByteArray(width * height);
        env->SetByteArrayRegion(ya, 0, width * height, (const jbyte*)y);
        jbyteArray ua = env->NewByteArray((width / 2) * (height / 2));
        env->SetByteArrayRegion(ua, 0, (width / 2) * (height / 2), (const jbyte*)u);
        jbyteArray va = env->NewByteArray((width / 2) * (height / 2));
        env->SetByteArrayRegion(va, 0, (width / 2) * (height / 2), (const jbyte*)v);
        
        env->CallVoidMethod(listener, mid_onVideoReceiveFrame, (jint)friend_number, (jint)width, (jint)height, ya, ua, va, (jint)y_stride, (jint)u_stride, (jint)v_stride);
        
        env->DeleteLocalRef(ya);
        env->DeleteLocalRef(ua);
        env->DeleteLocalRef(va);
    }
}

void cb_audio_bit_rate(ToxAV *av, uint32_t friend_number, uint32_t bit_rate, void *user_data) {
    JNIEnv* env = get_av_env();
    jobject listener = av_listeners[av];
    if (listener && mid_onAudioBitRate) {
        env->CallVoidMethod(listener, mid_onAudioBitRate, (jint)friend_number, (jint)bit_rate);
    }
}

void cb_video_bit_rate(ToxAV *av, uint32_t friend_number, uint32_t bit_rate, void *user_data) {
    JNIEnv* env = get_av_env();
    jobject listener = av_listeners[av];
    if (listener && mid_onVideoBitRate) {
        env->CallVoidMethod(listener, mid_onVideoBitRate, (jint)friend_number, (jint)bit_rate);
    }
}

// Обратный вызов при получении входящего звука из группового AV-чата
void cb_group_audio(void *tox, uint32_t groupnumber, uint32_t peernumber, const int16_t pcm[],
                    uint32_t samples, uint8_t channels, uint32_t sample_rate, void *userdata) {
    JNIEnv* env = get_av_env();
    jobject listener = nullptr;
    for (auto const& [av, lis] : av_listeners) {
        if (toxav_get_tox(av) == reinterpret_cast<Tox*>(tox)) {
            listener = lis;
            break;
        }
    }
    if (listener && mid_onGroupAudio) {
        jshortArray arr = env->NewShortArray(samples * channels);
        env->SetShortArrayRegion(arr, 0, samples * channels, (const jshort*)pcm);
        env->CallVoidMethod(listener, mid_onGroupAudio, (jint)groupnumber, (jint)peernumber, arr, (jint)samples, (jint)channels, (jint)sample_rate);
        env->DeleteLocalRef(arr);
    }
}

extern "C" {

JNIEXPORT jlong JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavNew(JNIEnv *env, jobject thiz, jlong toxPtr) {
    if (!toxPtr) return 0;
    TOXAV_ERR_NEW err;
    ToxAV *av = toxav_new(reinterpret_cast<Tox*>(toxPtr), &err);
    if (err != TOXAV_ERR_NEW_OK) LOGE("toxav_new failed: %d", err);
    return reinterpret_cast<jlong>(av);
}

JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavKill(JNIEnv *env, jobject thiz, jlong avPtr) {
    ToxAV *av = reinterpret_cast<ToxAV*>(avPtr);
    if (av) {
        if (av_listeners.count(av)) {
            env->DeleteGlobalRef(av_listeners[av]);
            av_listeners.erase(av);
        }
        toxav_kill(av);
    }
}

JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavIterate(JNIEnv *env, jobject thiz, jlong avPtr, jobject listener) {
    ToxAV *av = reinterpret_cast<ToxAV*>(avPtr);
    if (av) {
        if (av_listeners.find(av) == av_listeners.end()) {
            av_listeners[av] = env->NewGlobalRef(listener);
            toxav_callback_call(av, cb_call, nullptr);
            toxav_callback_call_state(av, cb_call_state, nullptr);
            toxav_callback_audio_receive_frame(av, cb_audio_receive_frame, nullptr);
            toxav_callback_video_receive_frame(av, cb_video_receive_frame, nullptr);
            toxav_callback_audio_bit_rate(av, cb_audio_bit_rate, nullptr);
            toxav_callback_video_bit_rate(av, cb_video_bit_rate, nullptr);
        }
        toxav_iterate(av);
    }
}

JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavIterationInterval(JNIEnv *env, jobject thiz, jlong avPtr) {
    return avPtr ? toxav_iteration_interval(reinterpret_cast<ToxAV*>(avPtr)) : 200;
}

JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavCall(JNIEnv *env, jobject thiz, jlong avPtr, jint friendNumber, jint audioBitrate, jint videoBitrate) {
    return toxav_call(reinterpret_cast<ToxAV*>(avPtr), friendNumber, audioBitrate, videoBitrate, nullptr);
}

JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavAnswer(JNIEnv *env, jobject thiz, jlong avPtr, jint friendNumber, jint audioBitrate, jint videoBitrate) {
    return toxav_answer(reinterpret_cast<ToxAV*>(avPtr), friendNumber, audioBitrate, videoBitrate, nullptr);
}

JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavCallControl(JNIEnv *env, jobject thiz, jlong avPtr, jint friendNumber, jint control) {
    return toxav_call_control(reinterpret_cast<ToxAV*>(avPtr), friendNumber, (TOXAV_CALL_CONTROL)control, nullptr);
}

JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavAudioSendFrame(JNIEnv *env, jobject thiz, jlong avPtr, jint friendNumber, jshortArray pcm, jint sampleCount, jint channels, jint samplingRate) {
    jshort *data = env->GetShortArrayElements(pcm, nullptr);
    bool res = toxav_audio_send_frame(reinterpret_cast<ToxAV*>(avPtr), friendNumber, reinterpret_cast<const int16_t*>(data), sampleCount, channels, samplingRate, nullptr);
    env->ReleaseShortArrayElements(pcm, data, JNI_ABORT);
    return res;
}

// Отправка кадра собственного видеопотока YUV420P собеседнику в видеозвонке
JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavVideoSendFrame(JNIEnv *env, jobject thiz, jlong avPtr, jint friendNumber, jint width, jint height, jbyteArray yPlane, jbyteArray uPlane, jbyteArray vPlane) {
    ToxAV *av = reinterpret_cast<ToxAV*>(avPtr);
    jbyte *yData = env->GetByteArrayElements(yPlane, nullptr);
    jbyte *uData = env->GetByteArrayElements(uPlane, nullptr);
    jbyte *vData = env->GetByteArrayElements(vPlane, nullptr);
    
    TOXAV_ERR_SEND_FRAME err;
    bool res = toxav_video_send_frame(av, friendNumber, width, height,
                                      reinterpret_cast<const uint8_t*>(yData),
                                      reinterpret_cast<const uint8_t*>(uData),
                                      reinterpret_cast<const uint8_t*>(vData),
                                      &err);
    if (err != TOXAV_ERR_SEND_FRAME_OK) {
        LOGE("toxav_video_send_frame failed: %d", err);
    }
    
    env->ReleaseByteArrayElements(yPlane, yData, JNI_ABORT);
    env->ReleaseByteArrayElements(uPlane, uData, JNI_ABORT);
    env->ReleaseByteArrayElements(vPlane, vData, JNI_ABORT);
    return res;
}

// Изменение аудио-битрейта в текущем соединении «на лету»
JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavAudioSetBitRate(JNIEnv *env, jobject thiz, jlong avPtr, jint friendNumber, jint bitrate) {
    ToxAV *av = reinterpret_cast<ToxAV*>(avPtr);
    TOXAV_ERR_BIT_RATE_SET err;
    bool res = toxav_audio_set_bit_rate(av, friendNumber, bitrate, &err);
    if (err != TOXAV_ERR_BIT_RATE_SET_OK) {
        LOGE("toxav_audio_set_bit_rate failed: %d", err);
    }
    return res;
}

// Изменение видео-битрейта в текущем соединении «на лету»
JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavVideoSetBitRate(JNIEnv *env, jobject thiz, jlong avPtr, jint friendNumber, jint bitrate) {
    ToxAV *av = reinterpret_cast<ToxAV*>(avPtr);
    TOXAV_ERR_BIT_RATE_SET err;
    bool res = toxav_video_set_bit_rate(av, friendNumber, bitrate, &err);
    if (err != TOXAV_ERR_BIT_RATE_SET_OK) {
        LOGE("toxav_video_set_bit_rate failed: %d", err);
    }
    return res;
}

// Создание нового группового AV-чата на основе инстанса Tox
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavAddAvGroupchat(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    return toxav_add_av_groupchat(tox, cb_group_audio, nullptr);
}

// Присоединение к групповому AV-чату (через включение/активацию A/V на группе)
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavJoinAvGroupchat(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    return toxav_groupchat_enable_av(tox, groupNumber, cb_group_audio, nullptr);
}

// Отправка собственного аудио-кадра PCM в групповой чат
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavGroupSendAudio(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber, jshortArray pcm, jint sampleCount, jint channels, jint samplingRate) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    jshort *data = env->GetShortArrayElements(pcm, nullptr);
    int32_t res = toxav_group_send_audio(tox, groupNumber, reinterpret_cast<const int16_t*>(data), sampleCount, channels, samplingRate);
    env->ReleaseShortArrayElements(pcm, data, JNI_ABORT);
    return res;
}

// Включение/активация AV-функций для указанной группы
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavGroupchatEnableAv(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    return toxav_groupchat_enable_av(tox, groupNumber, cb_group_audio, nullptr);
}

// Отключение AV-функций для указанной группы
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavGroupchatDisableAv(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    return toxav_groupchat_disable_av(tox, groupNumber);
}

// Проверка активности AV-функций для указанной группы
JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeToxAv_toxavGroupchatAvEnabled(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    return toxav_groupchat_av_enabled(tox, groupNumber);
}

bool register_toxav_methods(JNIEnv* env) {
    jclass cls = env->FindClass("ltd/evilcorp/core/tox/listener/ToxAvEventListener");
    if (!cls) return false;
    mid_onCall = env->GetMethodID(cls, "onCall", "(IZZ)V");
    mid_onCallState = env->GetMethodID(cls, "onCallState", "(II)V");
    mid_onAudioReceiveFrame = env->GetMethodID(cls, "onAudioReceiveFrame", "(I[SIII)V");
    mid_onVideoReceiveFrame = env->GetMethodID(cls, "onVideoReceiveFrame", "(III[B[B[BIII)V");
    mid_onAudioBitRate = env->GetMethodID(cls, "onAudioBitRate", "(II)V");
    mid_onVideoBitRate = env->GetMethodID(cls, "onVideoBitRate", "(II)V");
    mid_onGroupAudio = env->GetMethodID(cls, "onGroupAudio", "(II[SIII)V");
    return true;
}

void init_toxav_vm(JavaVM* vm) {
    g_av_vm = vm;
}

}
