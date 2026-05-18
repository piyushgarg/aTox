#include <jni.h>
#include <tox/tox.h>
#include <tox/toxencryptsave.h>
#include <android/log.h>
#include <vector>
#include <string>
#include <map>

#define LOG_TAG "NativeToxCore"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM* g_vm;
static jmethodID mid_onFriendMessage;
static jmethodID mid_onFriendRequest;
static jmethodID mid_onFriendConnectionStatus;
static jmethodID mid_onSelfConnectionStatus;
static jmethodID mid_onFriendName;
static jmethodID mid_onFriendStatusMessage;
static jmethodID mid_onFriendStatus;
static jmethodID mid_onFriendTyping;
static jmethodID mid_onFriendReadReceipt;
static jmethodID mid_onFriendLosslessPacket;
static jmethodID mid_onFriendLossyPacket;
static jmethodID mid_onFileRecv;
static jmethodID mid_onFileRecvControl;
static jmethodID mid_onFileRecvChunk;
static jmethodID mid_onFileChunkRequest;

// Указатели на методы-слушатели Kotlin для событий конференций
static jmethodID mid_onConferenceInvite;
static jmethodID mid_onConferenceMessage;
static jmethodID mid_onConferencePeerListChanged;
static jmethodID mid_onConferencePeerName;

static std::map<Tox*, jobject> tox_listeners;

JNIEnv* get_env() {
    JNIEnv* env;
    if (g_vm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) return env;
    g_vm->AttachCurrentThread(&env, nullptr);
    return env;
}

void cb_friend_message(Tox *tox, uint32_t friend_number, TOX_MESSAGE_TYPE type, const uint8_t *message, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendMessage) {
        jbyteArray msg = env->NewByteArray(length);
        env->SetByteArrayRegion(msg, 0, length, (const jbyte*)message);
        env->CallVoidMethod(listener, mid_onFriendMessage, (jint)friend_number, (jint)type, (jint)0, msg);
        env->DeleteLocalRef(msg);
    }
}

void cb_friend_request(Tox *tox, const uint8_t *public_key, const uint8_t *message, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendRequest) {
        jbyteArray pk = env->NewByteArray(TOX_PUBLIC_KEY_SIZE);
        env->SetByteArrayRegion(pk, 0, TOX_PUBLIC_KEY_SIZE, (const jbyte*)public_key);
        jbyteArray msg = env->NewByteArray(length);
        env->SetByteArrayRegion(msg, 0, length, (const jbyte*)message);
        env->CallVoidMethod(listener, mid_onFriendRequest, pk, (jint)0, msg);
        env->DeleteLocalRef(pk);
        env->DeleteLocalRef(msg);
    }
}

void cb_self_connection_status(Tox *tox, TOX_CONNECTION status, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onSelfConnectionStatus) {
        env->CallVoidMethod(listener, mid_onSelfConnectionStatus, (jint)status);
    }
}

void cb_friend_connection_status(Tox *tox, uint32_t friend_number, TOX_CONNECTION status, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendConnectionStatus) {
        env->CallVoidMethod(listener, mid_onFriendConnectionStatus, (jint)friend_number, (jint)status);
    }
}

void cb_friend_name(Tox *tox, uint32_t friend_number, const uint8_t *name, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendName) {
        jbyteArray n = env->NewByteArray(length);
        env->SetByteArrayRegion(n, 0, length, (const jbyte*)name);
        env->CallVoidMethod(listener, mid_onFriendName, (jint)friend_number, n);
        env->DeleteLocalRef(n);
    }
}

void cb_friend_status_message(Tox *tox, uint32_t friend_number, const uint8_t *message, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendStatusMessage) {
        jbyteArray msg = env->NewByteArray(length);
        env->SetByteArrayRegion(msg, 0, length, (const jbyte*)message);
        env->CallVoidMethod(listener, mid_onFriendStatusMessage, (jint)friend_number, msg);
        env->DeleteLocalRef(msg);
    }
}

void cb_friend_status(Tox *tox, uint32_t friend_number, TOX_USER_STATUS status, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendStatus) {
        env->CallVoidMethod(listener, mid_onFriendStatus, (jint)friend_number, (jint)status);
    }
}

void cb_friend_typing(Tox *tox, uint32_t friend_number, bool is_typing, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendTyping) {
        env->CallVoidMethod(listener, mid_onFriendTyping, (jint)friend_number, (jboolean)is_typing);
    }
}

void cb_friend_read_receipt(Tox *tox, uint32_t friend_number, uint32_t message_id, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendReadReceipt) {
        env->CallVoidMethod(listener, mid_onFriendReadReceipt, (jint)friend_number, (jint)message_id);
    }
}

void cb_file_recv(Tox *tox, uint32_t friend_number, uint32_t file_number, uint32_t kind, uint64_t file_size, const uint8_t *filename, size_t filename_length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFileRecv) {
        jbyteArray name = env->NewByteArray(filename_length);
        env->SetByteArrayRegion(name, 0, filename_length, (const jbyte*)filename);
        env->CallVoidMethod(listener, mid_onFileRecv, (jint)friend_number, (jint)file_number, (jint)kind, (jlong)file_size, name);
        env->DeleteLocalRef(name);
    }
}

void cb_file_recv_control(Tox *tox, uint32_t friend_number, uint32_t file_number, TOX_FILE_CONTROL control, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFileRecvControl) {
        env->CallVoidMethod(listener, mid_onFileRecvControl, (jint)friend_number, (jint)file_number, (jint)control);
    }
}

void cb_file_recv_chunk(Tox *tox, uint32_t friend_number, uint32_t file_number, uint64_t position, const uint8_t *data, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFileRecvChunk) {
        jbyteArray chunk = env->NewByteArray(length);
        env->SetByteArrayRegion(chunk, 0, length, (const jbyte*)data);
        env->CallVoidMethod(listener, mid_onFileRecvChunk, (jint)friend_number, (jint)file_number, (jlong)position, chunk);
        env->DeleteLocalRef(chunk);
    }
}

void cb_file_chunk_request(Tox *tox, uint32_t friend_number, uint32_t file_number, uint64_t position, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFileChunkRequest) {
        env->CallVoidMethod(listener, mid_onFileChunkRequest, (jint)friend_number, (jint)file_number, (jlong)position, (jint)length);
    }
}

void cb_friend_lossless_packet(Tox *tox, uint32_t friend_number, const uint8_t *data, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendLosslessPacket) {
        jbyteArray pkg = env->NewByteArray(length);
        env->SetByteArrayRegion(pkg, 0, length, (const jbyte*)data);
        env->CallVoidMethod(listener, mid_onFriendLosslessPacket, (jint)friend_number, pkg);
        env->DeleteLocalRef(pkg);
    }
}

void cb_friend_lossy_packet(Tox *tox, uint32_t friend_number, const uint8_t *data, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onFriendLossyPacket) {
        jbyteArray pkg = env->NewByteArray(length);
        env->SetByteArrayRegion(pkg, 0, length, (const jbyte*)data);
        env->CallVoidMethod(listener, mid_onFriendLossyPacket, (jint)friend_number, pkg);
        env->DeleteLocalRef(pkg);
    }
}

// Обратный вызов при получении приглашения в конференцию (групповой чат)
void cb_conference_invite(Tox *tox, uint32_t friend_number, TOX_CONFERENCE_TYPE type, const uint8_t *cookie, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onConferenceInvite) {
        jbyteArray cook = env->NewByteArray(length);
        env->SetByteArrayRegion(cook, 0, length, (const jbyte*)cookie);
        env->CallVoidMethod(listener, mid_onConferenceInvite, (jint)friend_number, (jint)type, cook);
        env->DeleteLocalRef(cook);
    }
}

// Обратный вызов при получении сообщения в групповом чате (конференции)
void cb_conference_message(Tox *tox, uint32_t conference_number, uint32_t peer_number, TOX_MESSAGE_TYPE type, const uint8_t *message, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onConferenceMessage) {
        jbyteArray msg = env->NewByteArray(length);
        env->SetByteArrayRegion(msg, 0, length, (const jbyte*)message);
        env->CallVoidMethod(listener, mid_onConferenceMessage, (jint)conference_number, (jint)peer_number, (jint)type, msg);
        env->DeleteLocalRef(msg);
    }
}

// Обратный вызов при изменении списка участников конференции (вход/выход)
void cb_conference_peer_list_changed(Tox *tox, uint32_t conference_number, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onConferencePeerListChanged) {
        env->CallVoidMethod(listener, mid_onConferencePeerListChanged, (jint)conference_number);
    }
}

// Обратный вызов при изменении имени одного из участников конференции
void cb_conference_peer_name(Tox *tox, uint32_t conference_number, uint32_t peer_number, const uint8_t *name, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onConferencePeerName) {
        jbyteArray n = env->NewByteArray(length);
        env->SetByteArrayRegion(n, 0, length, (const jbyte*)name);
        env->CallVoidMethod(listener, mid_onConferencePeerName, (jint)conference_number, (jint)peer_number, n);
        env->DeleteLocalRef(n);
    }
}

static std::vector<uint8_t> jba2vec(JNIEnv *env, jbyteArray array) {
    std::vector<uint8_t> res;
    if (array) {
        jsize len = env->GetArrayLength(array);
        res.resize(len);
        env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte*>(res.data()));
    }
    return res;
}

static jbyteArray vec2jba(JNIEnv *env, const uint8_t* data, size_t len) {
    if (!data || len == 0) return env->NewByteArray(0);
    jbyteArray res = env->NewByteArray(len);
    env->SetByteArrayRegion(res, 0, len, reinterpret_cast<const jbyte*>(data));
    return res;
}

extern "C" {
    bool register_toxav_methods(JNIEnv* env);
    void init_toxav_vm(JavaVM* vm);

JNIEXPORT jlong JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxNew(JNIEnv *env, jobject thiz, jbyteArray savedata) {
    struct Tox_Options *options = tox_options_new(nullptr);
    std::vector<uint8_t> sd = jba2vec(env, savedata);
    if (!sd.empty()) {
        tox_options_set_savedata_type(options, TOX_SAVEDATA_TYPE_TOX_SAVE);
        tox_options_set_savedata_data(options, sd.data(), sd.size());
    }
    tox_options_set_udp_enabled(options, true);
    
    TOX_ERR_NEW err;
    Tox *tox = tox_new(options, &err);
    tox_options_free(options);
    if (err != TOX_ERR_NEW_OK) LOGE("tox_new failed: %d", err);
    return reinterpret_cast<jlong>(tox);
}

JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxKill(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    if (tox) {
        if (tox_listeners.count(tox)) {
            env->DeleteGlobalRef(tox_listeners[tox]);
            tox_listeners.erase(tox);
        }
        tox_kill(tox);
    }
}

JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxBootstrap(JNIEnv *env, jobject thiz, jlong toxPtr, jstring address, jint port, jbyteArray publicKey) {
    const char *addr = env->GetStringUTFChars(address, nullptr);
    std::vector<uint8_t> pk = jba2vec(env, publicKey);
    tox_bootstrap(reinterpret_cast<Tox*>(toxPtr), addr, port, pk.data(), nullptr);
    env->ReleaseStringUTFChars(address, addr);
}

JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxAddTcpRelay(JNIEnv *env, jobject thiz, jlong toxPtr, jstring address, jint port, jbyteArray publicKey) {
    const char *addr = env->GetStringUTFChars(address, nullptr);
    std::vector<uint8_t> pk = jba2vec(env, publicKey);
    tox_add_tcp_relay(reinterpret_cast<Tox*>(toxPtr), addr, port, pk.data(), nullptr);
    env->ReleaseStringUTFChars(address, addr);
}

JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxIterate(JNIEnv *env, jobject thiz, jlong toxPtr, jobject listener) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    if (tox_listeners.find(tox) == tox_listeners.end()) {
        tox_listeners[tox] = env->NewGlobalRef(listener);
        tox_callback_friend_message(tox, cb_friend_message);
        tox_callback_friend_request(tox, cb_friend_request);
        tox_callback_self_connection_status(tox, cb_self_connection_status);
        tox_callback_friend_connection_status(tox, cb_friend_connection_status);
        tox_callback_friend_name(tox, cb_friend_name);
        tox_callback_friend_status_message(tox, cb_friend_status_message);
        tox_callback_friend_status(tox, cb_friend_status);
        tox_callback_friend_typing(tox, cb_friend_typing);
        tox_callback_friend_read_receipt(tox, cb_friend_read_receipt);
        tox_callback_file_recv(tox, cb_file_recv);
        tox_callback_file_recv_control(tox, cb_file_recv_control);
        tox_callback_file_recv_chunk(tox, cb_file_recv_chunk);
        tox_callback_file_chunk_request(tox, cb_file_chunk_request);
        tox_callback_friend_lossless_packet(tox, cb_friend_lossless_packet);
        tox_callback_friend_lossy_packet(tox, cb_friend_lossy_packet);

        // Регистрация нативных C++ коллбеков для групповых событий
        tox_callback_conference_invite(tox, cb_conference_invite);
        tox_callback_conference_message(tox, cb_conference_message);
        tox_callback_conference_peer_list_changed(tox, cb_conference_peer_list_changed);
        tox_callback_conference_peer_name(tox, cb_conference_peer_name);
    }
    tox_iterate(tox, nullptr);
}

JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxIterationInterval(JNIEnv *env, jobject thiz, jlong toxPtr) {
    return tox_iteration_interval(reinterpret_cast<Tox*>(toxPtr));
}

JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetName(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t len = tox_self_get_name_size(tox);
    std::vector<uint8_t> name(len);
    tox_self_get_name(tox, name.data());
    return vec2jba(env, name.data(), len);
}

JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSetName(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray name) {
    std::vector<uint8_t> n = jba2vec(env, name);
    tox_self_set_name(reinterpret_cast<Tox*>(toxPtr), n.data(), n.size(), nullptr);
}

JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetStatusMessage(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t len = tox_self_get_status_message_size(tox);
    std::vector<uint8_t> msg(len);
    tox_self_get_status_message(tox, msg.data());
    return vec2jba(env, msg.data(), len);
}

JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSetStatusMessage(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray msg) {
    std::vector<uint8_t> m = jba2vec(env, msg);
    tox_self_set_status_message(reinterpret_cast<Tox*>(toxPtr), m.data(), m.size(), nullptr);
}

JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetAddress(JNIEnv *env, jobject thiz, jlong toxPtr) {
    uint8_t addr[TOX_ADDRESS_SIZE];
    tox_self_get_address(reinterpret_cast<Tox*>(toxPtr), addr);
    return vec2jba(env, addr, TOX_ADDRESS_SIZE);
}

JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetPublicKey(JNIEnv *env, jobject thiz, jlong toxPtr) {
    uint8_t pk[TOX_PUBLIC_KEY_SIZE];
    tox_self_get_public_key(reinterpret_cast<Tox*>(toxPtr), pk);
    return vec2jba(env, pk, TOX_PUBLIC_KEY_SIZE);
}

JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetNospam(JNIEnv *env, jobject thiz, jlong toxPtr) {
    return tox_self_get_nospam(reinterpret_cast<Tox*>(toxPtr));
}

JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSetNospam(JNIEnv *env, jobject thiz, jlong toxPtr, jint nospam) {
    tox_self_set_nospam(reinterpret_cast<Tox*>(toxPtr), nospam);
}

JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetSavedata(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t len = tox_get_savedata_size(tox);
    std::vector<uint8_t> sd(len);
    tox_get_savedata(tox, sd.data());
    return vec2jba(env, sd.data(), len);
}

// ... I will skip the rest of the implementations for brevity and implement them incrementally if needed ...
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxAddFriend(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray pubKey, jbyteArray message) {
    std::vector<uint8_t> pk = jba2vec(env, pubKey);
    std::vector<uint8_t> msg = jba2vec(env, message);
    return tox_friend_add(reinterpret_cast<Tox*>(toxPtr), pk.data(), msg.data(), msg.size(), nullptr);
}

JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxAddFriendNorequest(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray pubKey) {
    std::vector<uint8_t> pk = jba2vec(env, pubKey);
    return tox_friend_add_norequest(reinterpret_cast<Tox*>(toxPtr), pk.data(), nullptr);
}

JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxDeleteFriend(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    tox_friend_delete(reinterpret_cast<Tox*>(toxPtr), friendNumber, nullptr);
}

JNIEXPORT jintArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxGetFriendList(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t count = tox_self_get_friend_list_size(tox);
    std::vector<uint32_t> friends(count);
    tox_self_get_friend_list(tox, friends.data());
    jintArray res = env->NewIntArray(count);
    env->SetIntArrayRegion(res, 0, count, reinterpret_cast<const jint*>(friends.data()));
    return res;
}

JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxGetFriendPublicKey(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    uint8_t pk[TOX_PUBLIC_KEY_SIZE];
    if (tox_friend_get_public_key(reinterpret_cast<Tox*>(toxPtr), friendNumber, pk, nullptr)) {
        return vec2jba(env, pk, TOX_PUBLIC_KEY_SIZE);
    }
    return nullptr;
}

JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendByPublicKey(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray pubKey) {
    std::vector<uint8_t> pk = jba2vec(env, pubKey);
    return tox_friend_by_public_key(reinterpret_cast<Tox*>(toxPtr), pk.data(), nullptr);
}

JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendSendMessage(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint type, jbyteArray message) {
    std::vector<uint8_t> msg = jba2vec(env, message);
    return tox_friend_send_message(reinterpret_cast<Tox*>(toxPtr), friendNumber, (TOX_MESSAGE_TYPE)type, msg.data(), msg.size(), nullptr);
}

JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxSetTyping(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jboolean typing) {
    tox_self_set_typing(reinterpret_cast<Tox*>(toxPtr), friendNumber, typing, nullptr);
}

JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxGetSelfUserStatus(JNIEnv *env, jobject thiz, jlong toxPtr) {
    return tox_self_get_status(reinterpret_cast<Tox*>(toxPtr));
}

JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxSetSelfUserStatus(JNIEnv *env, jobject thiz, jlong toxPtr, jint status) {
    tox_self_set_status(reinterpret_cast<Tox*>(toxPtr), (TOX_USER_STATUS)status);
}

JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFileControl(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint fileNumber, jint control) {
    tox_file_control(reinterpret_cast<Tox*>(toxPtr), friendNumber, fileNumber, (TOX_FILE_CONTROL)control, nullptr);
}

JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFileSend(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint kind, jlong fileSize, jbyteArray fileId, jbyteArray filename) {
    std::vector<uint8_t> id = jba2vec(env, fileId);
    std::vector<uint8_t> name = jba2vec(env, filename);
    return tox_file_send(reinterpret_cast<Tox*>(toxPtr), friendNumber, kind, fileSize, id.data(), name.data(), name.size(), nullptr);
}

JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFileSendChunk(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint fileNumber, jlong position, jbyteArray data) {
    std::vector<uint8_t> d = jba2vec(env, data);
    tox_file_send_chunk(reinterpret_cast<Tox*>(toxPtr), friendNumber, fileNumber, position, d.data(), d.size(), nullptr);
}

JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendSendLosslessPacket(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jbyteArray data) {
    std::vector<uint8_t> d = jba2vec(env, data);
    tox_friend_send_lossless_packet(reinterpret_cast<Tox*>(toxPtr), friendNumber, d.data(), d.size(), nullptr);
}

// Crypto
JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_getSalt(JNIEnv *env, jobject thiz, jbyteArray data) {
    std::vector<uint8_t> d = jba2vec(env, data);
    uint32_t salt_len = tox_pass_salt_length();
    std::vector<uint8_t> salt(salt_len);
    if (tox_get_salt(d.data(), salt.data(), nullptr)) {
        return vec2jba(env, salt.data(), salt_len);
    }
    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_passKeyDeriveWithSalt(JNIEnv *env, jobject thiz, jbyteArray passphrase, jbyteArray salt) {
    // In the simple API approach, we just return the passphrase as the "key".
    // The actual key derivation happens inside tox_pass_encrypt/decrypt.
    return (jbyteArray)env->NewLocalRef(passphrase);
}

JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_passDecrypt(JNIEnv *env, jobject thiz, jbyteArray data, jbyteArray passkey) {
    std::vector<uint8_t> d = jba2vec(env, data);
    std::vector<uint8_t> p = jba2vec(env, passkey);
    uint32_t extra_len = tox_pass_encryption_extra_length();
    if (d.size() <= extra_len) return nullptr;
    std::vector<uint8_t> out(d.size() - extra_len);
    TOX_ERR_DECRYPTION err;
    if (tox_pass_decrypt(d.data(), d.size(), p.data(), p.size(), out.data(), &err)) {
        return vec2jba(env, out.data(), out.size());
    }
    LOGE("tox_pass_decrypt failed: %d", err);
    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_passEncrypt(JNIEnv *env, jobject thiz, jbyteArray data, jbyteArray passkey) {
    std::vector<uint8_t> d = jba2vec(env, data);
    std::vector<uint8_t> p = jba2vec(env, passkey);
    uint32_t extra_len = tox_pass_encryption_extra_length();
    std::vector<uint8_t> out(d.size() + extra_len);
    TOX_ERR_ENCRYPTION err;
    if (tox_pass_encrypt(d.data(), d.size(), p.data(), p.size(), out.data(), &err)) {
        return vec2jba(env, out.data(), out.size());
    }
    LOGE("tox_pass_encrypt failed: %d", err);
    return nullptr;
}

// Создание новой текстовой конференции (группового чата)
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceNew(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_NEW err;
    uint32_t conf = tox_conference_new(tox, &err);
    if (err != TOX_ERR_CONFERENCE_NEW_OK) {
        LOGE("tox_conference_new failed: %d", err);
        return -1;
    }
    return conf;
}

// Удаление существующей конференции (выход из группы)
JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceDelete(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_DELETE err;
    tox_conference_delete(tox, conferenceNumber, &err);
    if (err != TOX_ERR_CONFERENCE_DELETE_OK) {
        LOGE("tox_conference_delete failed: %d", err);
    }
}

// Приглашение друга в конференцию
JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceInvite(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint conferenceNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_INVITE err;
    tox_conference_invite(tox, friendNumber, conferenceNumber, &err);
    if (err != TOX_ERR_CONFERENCE_INVITE_OK) {
        LOGE("tox_conference_invite failed: %d", err);
    }
}

// Присоединение к конференции по полученному приглашению (cookie)
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceJoin(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jbyteArray cookie) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint8_t> cook = jba2vec(env, cookie);
    TOX_ERR_CONFERENCE_JOIN err;
    uint32_t conf = tox_conference_join(tox, friendNumber, cook.data(), cook.size(), &err);
    if (err != TOX_ERR_CONFERENCE_JOIN_OK) {
        LOGE("tox_conference_join failed: %d", err);
        return -1;
    }
    return conf;
}

// Отправка текстового сообщения в конференцию
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceSendMessage(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber, jint type, jbyteArray message) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint8_t> msg = jba2vec(env, message);
    TOX_ERR_CONFERENCE_SEND_MESSAGE err;
    bool res = tox_conference_send_message(tox, conferenceNumber, (TOX_MESSAGE_TYPE)type, msg.data(), msg.size(), &err);
    if (err != TOX_ERR_CONFERENCE_SEND_MESSAGE_OK) {
        LOGE("tox_conference_send_message failed: %d", err);
        return 0;
    }
    return res ? 1 : 0;
}

// Получение общего количества участников в конференции
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceGetPeerCount(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_PEER_QUERY err;
    uint32_t count = tox_conference_peer_count(tox, conferenceNumber, &err);
    if (err != TOX_ERR_CONFERENCE_PEER_QUERY_OK) {
        LOGE("tox_conference_peer_count failed: %d", err);
        return -1;
    }
    return count;
}

// Получение имени участника по его порядковому номеру
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceGetPeerName(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber, jint peerNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_PEER_QUERY err;
    size_t size = tox_conference_peer_get_name_size(tox, conferenceNumber, peerNumber, &err);
    if (err != TOX_ERR_CONFERENCE_PEER_QUERY_OK) {
        LOGE("tox_conference_peer_get_name_size failed: %d", err);
        return nullptr;
    }
    std::vector<uint8_t> name(size);
    tox_conference_peer_get_name(tox, conferenceNumber, peerNumber, name.data(), &err);
    if (err != TOX_ERR_CONFERENCE_PEER_QUERY_OK) {
        LOGE("tox_conference_peer_get_name failed: %d", err);
        return nullptr;
    }
    return vec2jba(env, name.data(), size);
}

// Получение публичного ключа участника по его порядковому номеру
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceGetPeerPublicKey(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber, jint peerNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_PEER_QUERY err;
    uint8_t pk[TOX_PUBLIC_KEY_SIZE];
    tox_conference_peer_get_public_key(tox, conferenceNumber, peerNumber, pk, &err);
    if (err != TOX_ERR_CONFERENCE_PEER_QUERY_OK) {
        LOGE("tox_conference_peer_get_public_key failed: %d", err);
        return nullptr;
    }
    return vec2jba(env, pk, TOX_PUBLIC_KEY_SIZE);
}

// Получение списка идентификаторов всех активных конференций
JNIEXPORT jintArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceGetChatlist(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t count = tox_conference_get_chatlist_size(tox);
    std::vector<uint32_t> chatlist(count);
    tox_conference_get_chatlist(tox, chatlist.data());
    jintArray res = env->NewIntArray(count);
    env->SetIntArrayRegion(res, 0, count, reinterpret_cast<const jint*>(chatlist.data()));
    return res;
}

// Получение типа конференции (0 - текст, 1 - аудио/видео)
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceGetType(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_GET_TYPE err;
    TOX_CONFERENCE_TYPE type = tox_conference_get_type(tox, conferenceNumber, &err);
    if (err != TOX_ERR_CONFERENCE_GET_TYPE_OK) {
        LOGE("tox_conference_get_type failed: %d", err);
        return -1;
    }
    return (jint)type;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_vm = vm;
    init_toxav_vm(vm);
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;

    jclass cls = env->FindClass("ltd/evilcorp/core/tox/listener/ToxEventListener");
    mid_onFriendMessage = env->GetMethodID(cls, "onFriendMessage", "(III[B)V");
    mid_onFriendRequest = env->GetMethodID(cls, "onFriendRequest", "([BI[B)V");
    mid_onFriendConnectionStatus = env->GetMethodID(cls, "onFriendConnectionStatus", "(II)V");
    mid_onSelfConnectionStatus = env->GetMethodID(cls, "onSelfConnectionStatus", "(I)V");
    mid_onFriendName = env->GetMethodID(cls, "onFriendName", "(I[B)V");
    mid_onFriendStatusMessage = env->GetMethodID(cls, "onFriendStatusMessage", "(I[B)V");
    mid_onFriendStatus = env->GetMethodID(cls, "onFriendStatus", "(II)V");
    mid_onFriendTyping = env->GetMethodID(cls, "onFriendTyping", "(IZ)V");
    mid_onFriendReadReceipt = env->GetMethodID(cls, "onFriendReadReceipt", "(II)V");
    mid_onFileRecv = env->GetMethodID(cls, "onFileRecv", "(IIIJ[B)V");
    mid_onFileRecvControl = env->GetMethodID(cls, "onFileRecvControl", "(III)V");
    mid_onFileRecvChunk = env->GetMethodID(cls, "onFileRecvChunk", "(IIJ[B)V");
    mid_onFileChunkRequest = env->GetMethodID(cls, "onFileChunkRequest", "(IIJI)V");
    mid_onFriendLosslessPacket = env->GetMethodID(cls, "onFriendLosslessPacket", "(I[B)V");
    mid_onFriendLossyPacket = env->GetMethodID(cls, "onFriendLossyPacket", "(I[B)V");

    // Поиск Method ID обработчиков событий конференций в классе-слушателе
    mid_onConferenceInvite = env->GetMethodID(cls, "onConferenceInvite", "(II[B)V");
    mid_onConferenceMessage = env->GetMethodID(cls, "onConferenceMessage", "(III[B)V");
    mid_onConferencePeerListChanged = env->GetMethodID(cls, "onConferencePeerListChanged", "(I)V");
    mid_onConferencePeerName = env->GetMethodID(cls, "onConferencePeerName", "(II[B)V");

    if (!register_toxav_methods(env)) {
        LOGE("Failed to register ToxAV methods");
    }

    return JNI_VERSION_1_6;
}

}

