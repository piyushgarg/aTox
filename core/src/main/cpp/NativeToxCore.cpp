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
static jmethodID mid_onConferenceTitle;

// Указатели на методы-слушатели Kotlin для событий NGC-групп
static jmethodID mid_onGroupInvite;
static jmethodID mid_onGroupMessage;
static jmethodID mid_onGroupPeerJoin;
static jmethodID mid_onGroupPeerExit;
static jmethodID mid_onGroupTopic;
static jmethodID mid_onGroupPeerName;
static jmethodID mid_onGroupPassword;
static jmethodID mid_onGroupPeerStatus;
static jmethodID mid_onGroupPrivacyState;
static jmethodID mid_onGroupVoiceState;
static jmethodID mid_onGroupTopicLock;
static jmethodID mid_onGroupPeerLimit;
static jmethodID mid_onGroupPrivateMessage;
static jmethodID mid_onGroupSelfJoin;
static jmethodID mid_onGroupJoinFail;
static jmethodID mid_onGroupModeration;

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

// Обратный вызов при изменении названия (заголовка) конференции
void cb_conference_title(Tox *tox, uint32_t conference_number, uint32_t peer_number, const uint8_t *title, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onConferenceTitle) {
        jbyteArray t = env->NewByteArray(length);
        env->SetByteArrayRegion(t, 0, length, (const jbyte*)title);
        env->CallVoidMethod(listener, mid_onConferenceTitle, (jint)conference_number, (jint)peer_number, t);
        env->DeleteLocalRef(t);
    }
}

// Обратный вызов при получении приглашения в NGC-группу
void cb_group_invite(Tox *tox, uint32_t friend_number, const uint8_t *invite_data, size_t length, const uint8_t *group_name, size_t group_name_length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupInvite) {
        jbyteArray invite = env->NewByteArray(length);
        env->SetByteArrayRegion(invite, 0, length, (const jbyte*)invite_data);
        jbyteArray name = env->NewByteArray(group_name_length);
        env->SetByteArrayRegion(name, 0, group_name_length, (const jbyte*)group_name);
        env->CallVoidMethod(listener, mid_onGroupInvite, (jint)friend_number, invite, name);
        env->DeleteLocalRef(invite);
        env->DeleteLocalRef(name);
    }
}

// Обратный вызов при получении нового сообщения в NGC-группе
void cb_group_message(Tox *tox, uint32_t group_number, uint32_t peer_id, TOX_MESSAGE_TYPE message_type, const uint8_t *message, size_t length, uint32_t message_id, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupMessage) {
        jbyteArray msg = env->NewByteArray(length);
        env->SetByteArrayRegion(msg, 0, length, (const jbyte*)message);
        env->CallVoidMethod(listener, mid_onGroupMessage, (jint)group_number, (jint)peer_id, (jint)message_type, msg, (jint)message_id);
        env->DeleteLocalRef(msg);
    }
}

// Обратный вызов при входе участника в NGC-группу
void cb_group_peer_join(Tox *tox, uint32_t group_number, uint32_t peer_id, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupPeerJoin) {
        env->CallVoidMethod(listener, mid_onGroupPeerJoin, (jint)group_number, (jint)peer_id);
    }
}

// Обратный вызов при выходе участника из NGC-группы
void cb_group_peer_exit(Tox *tox, uint32_t group_number, uint32_t peer_id, Tox_Group_Exit_Type exit_type,
                         const uint8_t name[], size_t name_length,
                         const uint8_t part_message[], size_t part_message_length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupPeerExit) {
        env->CallVoidMethod(listener, mid_onGroupPeerExit, (jint)group_number, (jint)peer_id, (jint)exit_type);
    }
}

// Обратный вызов при смене темы NGC-группы
void cb_group_topic(Tox *tox, uint32_t group_number, uint32_t peer_id, const uint8_t *topic, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupTopic) {
        jbyteArray top = env->NewByteArray(length);
        env->SetByteArrayRegion(top, 0, length, (const jbyte*)topic);
        env->CallVoidMethod(listener, mid_onGroupTopic, (jint)group_number, (jint)peer_id, top);
        env->DeleteLocalRef(top);
    }
}

// Обратный вызов при смене имени участника NGC-группы
void cb_group_peer_name(Tox *tox, uint32_t group_number, uint32_t peer_id, const uint8_t *name, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupPeerName) {
        jbyteArray n = env->NewByteArray(length);
        env->SetByteArrayRegion(n, 0, length, (const jbyte*)name);
        env->CallVoidMethod(listener, mid_onGroupPeerName, (jint)group_number, (jint)peer_id, n);
        env->DeleteLocalRef(n);
    }
}

// Обратный вызов при смене пароля NGC-группы
void cb_group_password(Tox *tox, uint32_t group_number, const uint8_t *password, size_t length, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupPassword) {
        jbyteArray pass = env->NewByteArray(length);
        env->SetByteArrayRegion(pass, 0, length, (const jbyte*)password);
        env->CallVoidMethod(listener, mid_onGroupPassword, (jint)group_number, pass);
        env->DeleteLocalRef(pass);
    }
}

// Обратный вызов при смене сетевого статуса участника NGC-группы
void cb_group_peer_status(Tox *tox, uint32_t group_number, uint32_t peer_id, Tox_User_Status status, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupPeerStatus) {
        env->CallVoidMethod(listener, mid_onGroupPeerStatus, (jint)group_number, (jint)peer_id, (jint)status);
    }
}

// Обратный вызов при изменении режима приватности NGC-группы
void cb_group_privacy_state(Tox *tox, uint32_t group_number, Tox_Group_Privacy_State privacy_state, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupPrivacyState) {
        env->CallVoidMethod(listener, mid_onGroupPrivacyState, (jint)group_number, (jint)privacy_state);
    }
}

// Обратный вызов при изменении голосового статуса NGC-группы
void cb_group_voice_state(Tox *tox, uint32_t group_number, Tox_Group_Voice_State voice_state, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupVoiceState) {
        env->CallVoidMethod(listener, mid_onGroupVoiceState, (jint)group_number, (jint)voice_state);
    }
}

// Обратный вызов при смене блокировки изменения темы NGC-группы
void cb_group_topic_lock(Tox *tox, uint32_t group_number, Tox_Group_Topic_Lock topic_lock, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupTopicLock) {
        env->CallVoidMethod(listener, mid_onGroupTopicLock, (jint)group_number, (jint)topic_lock);
    }
}

// Обратный вызов при смене лимита количества участников NGC-группы
void cb_group_peer_limit(Tox *tox, uint32_t group_number, uint32_t peer_limit, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupPeerLimit) {
        env->CallVoidMethod(listener, mid_onGroupPeerLimit, (jint)group_number, (jint)peer_limit);
    }
}

// Обратный вызов при получении приватного сообщения внутри NGC-группы
void cb_group_private_message(Tox *tox, uint32_t group_number, uint32_t peer_id, Tox_Message_Type type, const uint8_t *message, size_t length, uint32_t message_id, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupPrivateMessage) {
        jbyteArray msg = env->NewByteArray(length);
        env->SetByteArrayRegion(msg, 0, length, (const jbyte*)message);
        env->CallVoidMethod(listener, mid_onGroupPrivateMessage, (jint)group_number, (jint)peer_id, (jint)type, msg, (jint)message_id);
        env->DeleteLocalRef(msg);
    }
}

// Обратный вызов при успешном собственном подключении к NGC-группе
void cb_group_self_join(Tox *tox, uint32_t group_number, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupSelfJoin) {
        env->CallVoidMethod(listener, mid_onGroupSelfJoin, (jint)group_number);
    }
}

// Обратный вызов при ошибке подключения к NGC-группе
void cb_group_join_fail(Tox *tox, uint32_t group_number, Tox_Group_Join_Fail fail_type, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupJoinFail) {
        env->CallVoidMethod(listener, mid_onGroupJoinFail, (jint)group_number, (jint)fail_type);
    }
}

// Обратный вызов при административных (модерационных) событиях в NGC-группе
void cb_group_moderation(Tox *tox, uint32_t group_number, uint32_t source_peer_id, uint32_t target_peer_id, Tox_Group_Mod_Event mod_type, void *user_data) {
    JNIEnv* env = get_env();
    jobject listener = tox_listeners[tox];
    if (listener && mid_onGroupModeration) {
        env->CallVoidMethod(listener, mid_onGroupModeration, (jint)group_number, (jint)source_peer_id, (jint)target_peer_id, (jint)mod_type);
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

// ===================================================================================
// Управление инстансом Tox (Инициализация, завершение работы, цикл toxIterate)
// ===================================================================================

// Создание нового инстанса Tox с настройками по умолчанию
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

// Создание нового инстанса Tox с расширенными настройками сети и прокси
JNIEXPORT jlong JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxNewWithOptions(JNIEnv *env, jobject thiz, jbyteArray savedata,
                                                       jboolean ipv6Enabled, jboolean udpEnabled, jboolean localDiscoveryEnabled,
                                                       jint proxyType, jstring proxyHost, jint proxyPort) {
    struct Tox_Options *options = tox_options_new(nullptr);
    
    std::vector<uint8_t> sd = jba2vec(env, savedata);
    if (!sd.empty()) {
        tox_options_set_savedata_type(options, TOX_SAVEDATA_TYPE_TOX_SAVE);
        tox_options_set_savedata_data(options, sd.data(), sd.size());
    }
    
    tox_options_set_ipv6_enabled(options, ipv6Enabled);
    tox_options_set_udp_enabled(options, udpEnabled);
    tox_options_set_local_discovery_enabled(options, localDiscoveryEnabled);
    
    if (proxyType > 0 && proxyHost) {
        const char *host_str = env->GetStringUTFChars(proxyHost, nullptr);
        if (host_str) {
            tox_options_set_proxy_type(options, (TOX_PROXY_TYPE)proxyType);
            tox_options_set_proxy_host(options, host_str);
            tox_options_set_proxy_port(options, (uint16_t)proxyPort);
            env->ReleaseStringUTFChars(proxyHost, host_str);
        }
    }
    
    TOX_ERR_NEW err;
    Tox *tox = tox_new(options, &err);
    tox_options_free(options);
    if (err != TOX_ERR_NEW_OK) LOGE("tox_new_with_options failed: %d", err);
    return reinterpret_cast<jlong>(tox);
}

// Уничтожение инстанса Tox и освобождение ресурсов
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

// Подключение к публичному узлу (Bootstrap)
JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxBootstrap(JNIEnv *env, jobject thiz, jlong toxPtr, jstring address, jint port, jbyteArray publicKey) {
    const char *addr = env->GetStringUTFChars(address, nullptr);
    std::vector<uint8_t> pk = jba2vec(env, publicKey);
    tox_bootstrap(reinterpret_cast<Tox*>(toxPtr), addr, port, pk.data(), nullptr);
    env->ReleaseStringUTFChars(address, addr);
}

// Добавление TCP-релея для стабильного соединения
JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxAddTcpRelay(JNIEnv *env, jobject thiz, jlong toxPtr, jstring address, jint port, jbyteArray publicKey) {
    const char *addr = env->GetStringUTFChars(address, nullptr);
    std::vector<uint8_t> pk = jba2vec(env, publicKey);
    tox_add_tcp_relay(reinterpret_cast<Tox*>(toxPtr), addr, port, pk.data(), nullptr);
    env->ReleaseStringUTFChars(address, addr);
}

// Проведение одной сетевой итерации ядра Tox и диспетчеризация callback-событий в Kotlin
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
        tox_callback_conference_title(tox, cb_conference_title);

        // Регистрация нативных C++ коллбеков для событий NGC-групп
        tox_callback_group_invite(tox, cb_group_invite);
        tox_callback_group_message(tox, cb_group_message);
        tox_callback_group_peer_join(tox, cb_group_peer_join);
        tox_callback_group_peer_exit(tox, cb_group_peer_exit);
        tox_callback_group_topic(tox, cb_group_topic);
        tox_callback_group_peer_name(tox, cb_group_peer_name);
        tox_callback_group_password(tox, cb_group_password);
        tox_callback_group_peer_status(tox, cb_group_peer_status);
        tox_callback_group_privacy_state(tox, cb_group_privacy_state);
        tox_callback_group_voice_state(tox, cb_group_voice_state);
        tox_callback_group_topic_lock(tox, cb_group_topic_lock);
        tox_callback_group_peer_limit(tox, cb_group_peer_limit);
        tox_callback_group_private_message(tox, cb_group_private_message);
        tox_callback_group_self_join(tox, cb_group_self_join);
        tox_callback_group_join_fail(tox, cb_group_join_fail);
        tox_callback_group_moderation(tox, cb_group_moderation);
    }
    tox_iterate(tox, nullptr);
}

// Получение рекомендованного интервала ожидания (в миллисекундах) перед следующей итерацией
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxIterationInterval(JNIEnv *env, jobject thiz, jlong toxPtr) {
    return tox_iteration_interval(reinterpret_cast<Tox*>(toxPtr));
}

// ===================================================================================
// Профиль пользователя (Имя, статус-сообщение, адреса, ключи, порты)
// ===================================================================================

// Получение собственного имени пользователя
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetName(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t len = tox_self_get_name_size(tox);
    std::vector<uint8_t> name(len);
    tox_self_get_name(tox, name.data());
    return vec2jba(env, name.data(), len);
}

// Установка собственного имени пользователя
JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSetName(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray name) {
    std::vector<uint8_t> n = jba2vec(env, name);
    tox_self_set_name(reinterpret_cast<Tox*>(toxPtr), n.data(), n.size(), nullptr);
}

// Получение собственного статус-сообщения
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetStatusMessage(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t len = tox_self_get_status_message_size(tox);
    std::vector<uint8_t> msg(len);
    tox_self_get_status_message(tox, msg.data());
    return vec2jba(env, msg.data(), len);
}

// Установка собственного статус-сообщения
JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSetStatusMessage(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray msg) {
    std::vector<uint8_t> m = jba2vec(env, msg);
    tox_self_set_status_message(reinterpret_cast<Tox*>(toxPtr), m.data(), m.size(), nullptr);
}

// Получение собственного полного Tox-адреса (Tox ID) для обмена контактами
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetAddress(JNIEnv *env, jobject thiz, jlong toxPtr) {
    uint8_t addr[TOX_ADDRESS_SIZE];
    tox_self_get_address(reinterpret_cast<Tox*>(toxPtr), addr);
    return vec2jba(env, addr, TOX_ADDRESS_SIZE);
}

// Получение собственного публичного ключа (Public Key)
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetPublicKey(JNIEnv *env, jobject thiz, jlong toxPtr) {
    uint8_t pk[TOX_PUBLIC_KEY_SIZE];
    tox_self_get_public_key(reinterpret_cast<Tox*>(toxPtr), pk);
    return vec2jba(env, pk, TOX_PUBLIC_KEY_SIZE);
}

// Получение секретного ключа для резервной копии профиля
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSelfGetSecretKey(JNIEnv *env, jobject thiz, jlong toxPtr) {
    uint8_t sk[TOX_SECRET_KEY_SIZE];
    tox_self_get_secret_key(reinterpret_cast<Tox*>(toxPtr), sk);
    return vec2jba(env, sk, TOX_SECRET_KEY_SIZE);
}

// Получение активного UDP-порта, занятого инстансом Tox
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSelfGetUdpPort(JNIEnv *env, jobject thiz, jlong toxPtr) {
    TOX_ERR_GET_PORT err;
    uint16_t port = tox_self_get_udp_port(reinterpret_cast<Tox*>(toxPtr), &err);
    if (err != TOX_ERR_GET_PORT_OK) return 0;
    return port;
}

// Получение активного TCP-порта, занятого инстансом Tox
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSelfGetTcpPort(JNIEnv *env, jobject thiz, jlong toxPtr) {
    TOX_ERR_GET_PORT err;
    uint16_t port = tox_self_get_tcp_port(reinterpret_cast<Tox*>(toxPtr), &err);
    if (err != TOX_ERR_GET_PORT_OK) return 0;
    return port;
}

// Получение временного DHT-идентификатора узла
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSelfGetDhtId(JNIEnv *env, jobject thiz, jlong toxPtr) {
    uint8_t dht_id[TOX_PUBLIC_KEY_SIZE];
    tox_self_get_dht_id(reinterpret_cast<Tox*>(toxPtr), dht_id);
    return vec2jba(env, dht_id, TOX_PUBLIC_KEY_SIZE);
}

// ===================================================================================
// Настройки профиля (Nospam, сохранение сессии)
// ===================================================================================

// Получение текущего значения антиспам-кода (Nospam)
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetNospam(JNIEnv *env, jobject thiz, jlong toxPtr) {
    return tox_self_get_nospam(reinterpret_cast<Tox*>(toxPtr));
}

// Установка нового значения антиспам-кода (Nospam)
JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxSetNospam(JNIEnv *env, jobject thiz, jlong toxPtr, jint nospam) {
    tox_self_set_nospam(reinterpret_cast<Tox*>(toxPtr), nospam);
}

// Получение полной бинарной сессии Tox (Savedata) для сохранения профиля на диск
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGetSavedata(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t len = tox_get_savedata_size(tox);
    std::vector<uint8_t> sd(len);
    tox_get_savedata(tox, sd.data());
    return vec2jba(env, sd.data(), len);
}
// ===================================================================================
// Взаимодействие с друзьями (Добавление, удаление, отправка сообщений, запросы статуса)
// ===================================================================================

// Добавление друга (отправка запроса на добавление)
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxAddFriend(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray pubKey, jbyteArray message) {
    std::vector<uint8_t> pk = jba2vec(env, pubKey);
    std::vector<uint8_t> msg = jba2vec(env, message);
    return tox_friend_add(reinterpret_cast<Tox*>(toxPtr), pk.data(), msg.data(), msg.size(), nullptr);
}

// Добавление друга без отправки запроса (для подтверждения входящего запроса)
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxAddFriendNorequest(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray pubKey) {
    std::vector<uint8_t> pk = jba2vec(env, pubKey);
    return tox_friend_add_norequest(reinterpret_cast<Tox*>(toxPtr), pk.data(), nullptr);
}

// Удаление друга из списка контактов
JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxDeleteFriend(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    tox_friend_delete(reinterpret_cast<Tox*>(toxPtr), friendNumber, nullptr);
}

// Получение списка идентификаторов всех друзей
JNIEXPORT jintArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxGetFriendList(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    size_t count = tox_self_get_friend_list_size(tox);
    std::vector<uint32_t> friends(count);
    tox_self_get_friend_list(tox, friends.data());
    jintArray res = env->NewIntArray(count);
    env->SetIntArrayRegion(res, 0, count, reinterpret_cast<const jint*>(friends.data()));
    return res;
}

// Получение публичного ключа друга по его номеру
JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxGetFriendPublicKey(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    uint8_t pk[TOX_PUBLIC_KEY_SIZE];
    if (tox_friend_get_public_key(reinterpret_cast<Tox*>(toxPtr), friendNumber, pk, nullptr)) {
        return vec2jba(env, pk, TOX_PUBLIC_KEY_SIZE);
    }
    return nullptr;
}

// Поиск номера друга по его публичному ключу
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendByPublicKey(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray pubKey) {
    std::vector<uint8_t> pk = jba2vec(env, pubKey);
    return tox_friend_by_public_key(reinterpret_cast<Tox*>(toxPtr), pk.data(), nullptr);
}

// Проверка существования друга в списке контактов по его номеру
JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendExists(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    return tox_friend_exists(reinterpret_cast<Tox*>(toxPtr), friendNumber);
}

// Получение имени друга по его номеру
JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendGetName(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_FRIEND_QUERY err;
    size_t len = tox_friend_get_name_size(tox, friendNumber, &err);
    if (err != TOX_ERR_FRIEND_QUERY_OK || len == 0) return nullptr;
    std::vector<uint8_t> name(len);
    if (tox_friend_get_name(tox, friendNumber, name.data(), &err)) {
        return vec2jba(env, name.data(), len);
    }
    return nullptr;
}

// Получение статус-сообщения друга по его номеру
JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendGetStatusMessage(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_FRIEND_QUERY err;
    size_t len = tox_friend_get_status_message_size(tox, friendNumber, &err);
    if (err != TOX_ERR_FRIEND_QUERY_OK || len == 0) return nullptr;
    std::vector<uint8_t> msg(len);
    if (tox_friend_get_status_message(tox, friendNumber, msg.data(), &err)) {
        return vec2jba(env, msg.data(), len);
    }
    return nullptr;
}

// Получение пользовательского статуса друга (Online, Away, Busy)
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendGetStatus(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    TOX_ERR_FRIEND_QUERY err;
    TOX_USER_STATUS status = tox_friend_get_status(reinterpret_cast<Tox*>(toxPtr), friendNumber, &err);
    if (err != TOX_ERR_FRIEND_QUERY_OK) return 0;
    return (jint)status;
}

// Получение типа сетевого соединения с другом (Offline, TCP, UDP)
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendGetConnectionStatus(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    TOX_ERR_FRIEND_QUERY err;
    TOX_CONNECTION conn = tox_friend_get_connection_status(reinterpret_cast<Tox*>(toxPtr), friendNumber, &err);
    if (err != TOX_ERR_FRIEND_QUERY_OK) return 0;
    return (jint)conn;
}

// Проверка, набирает ли друг сообщение в данный момент
JNIEXPORT jboolean JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendGetTyping(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    TOX_ERR_FRIEND_QUERY err;
    return tox_friend_get_typing(reinterpret_cast<Tox*>(toxPtr), friendNumber, &err);
}

// Получение времени последнего нахождения друга в сети (Unix Timestamp)
JNIEXPORT jlong JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendGetLastOnline(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber) {
    TOX_ERR_FRIEND_GET_LAST_ONLINE err;
    uint64_t last_online = tox_friend_get_last_online(reinterpret_cast<Tox*>(toxPtr), friendNumber, &err);
    if (err != TOX_ERR_FRIEND_GET_LAST_ONLINE_OK) return 0;
    return (jlong)last_online;
}

// Отправка текстового сообщения другу
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendSendMessage(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint type, jbyteArray message) {
    std::vector<uint8_t> msg = jba2vec(env, message);
    return tox_friend_send_message(reinterpret_cast<Tox*>(toxPtr), friendNumber, (TOX_MESSAGE_TYPE)type, msg.data(), msg.size(), nullptr);
}

// Установка собственного статуса набора текста для конкретного друга
JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxSetTyping(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jboolean typing) {
    tox_self_set_typing(reinterpret_cast<Tox*>(toxPtr), friendNumber, typing, nullptr);
}

// Получение собственного пользовательского статуса (Online, Away, Busy)
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxGetSelfUserStatus(JNIEnv *env, jobject thiz, jlong toxPtr) {
    return tox_self_get_status(reinterpret_cast<Tox*>(toxPtr));
}

// Установка собственного пользовательского статуса (Online, Away, Busy)
JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxSetSelfUserStatus(JNIEnv *env, jobject thiz, jlong toxPtr, jint status) {
    tox_self_set_status(reinterpret_cast<Tox*>(toxPtr), (TOX_USER_STATUS)status);
}

// ===================================================================================
// Передача файлов и кастомные пакеты данных
// ===================================================================================

// Отправка управляющей команды для передачи файла (пауза, возобновление, отмена)
JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFileControl(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint fileNumber, jint control) {
    tox_file_control(reinterpret_cast<Tox*>(toxPtr), friendNumber, fileNumber, (TOX_FILE_CONTROL)control, nullptr);
}

// Инициирование новой передачи файла другу
JNIEXPORT jint JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFileSend(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint kind, jlong fileSize, jbyteArray fileId, jbyteArray filename) {
    std::vector<uint8_t> id = jba2vec(env, fileId);
    std::vector<uint8_t> name = jba2vec(env, filename);
    return tox_file_send(reinterpret_cast<Tox*>(toxPtr), friendNumber, kind, fileSize, id.data(), name.data(), name.size(), nullptr);
}

// Отправка фрагмента (чанка) данных файла
JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFileSendChunk(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint fileNumber, jlong position, jbyteArray data) {
    std::vector<uint8_t> d = jba2vec(env, data);
    tox_file_send_chunk(reinterpret_cast<Tox*>(toxPtr), friendNumber, fileNumber, position, d.data(), d.size(), nullptr);
}

// Получение уникального идентификатора файла (File ID) для передачи
JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFileGetFileId(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jint fileNumber) {
    uint8_t file_id[TOX_FILE_ID_LENGTH];
    TOX_ERR_FILE_GET err;
    if (tox_file_get_file_id(reinterpret_cast<Tox*>(toxPtr), friendNumber, fileNumber, file_id, &err)) {
        return vec2jba(env, file_id, TOX_FILE_ID_LENGTH);
    }
    return nullptr;
}

// Отправка надежного (Lossless) пользовательского пакета другу
JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendSendLosslessPacket(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jbyteArray data) {
    std::vector<uint8_t> d = jba2vec(env, data);
    tox_friend_send_lossless_packet(reinterpret_cast<Tox*>(toxPtr), friendNumber, d.data(), d.size(), nullptr);
}

// Отправка ненадежного (Lossy) пользовательского пакета другу
JNIEXPORT void JNICALL Java_ltd_evilcorp_core_tox_NativeTox_toxFriendSendLossyPacket(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jbyteArray data) {
    std::vector<uint8_t> d = jba2vec(env, data);
    tox_friend_send_lossy_packet(reinterpret_cast<Tox*>(toxPtr), friendNumber, d.data(), d.size(), nullptr);
}

// ===================================================================================
// Криптография и шифрование профиля
// ===================================================================================

// Извлечение соли (Salt) из зашифрованных данных профиля
JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_getSalt(JNIEnv *env, jobject thiz, jbyteArray data) {
    std::vector<uint8_t> d = jba2vec(env, data);
    uint32_t salt_len = tox_pass_salt_length();
    std::vector<uint8_t> salt(salt_len);
    if (tox_get_salt(d.data(), salt.data(), nullptr)) {
        return vec2jba(env, salt.data(), salt_len);
    }
    return nullptr;
}

// Получение ключа шифрования из пароля с использованием соли
JNIEXPORT jbyteArray JNICALL Java_ltd_evilcorp_core_tox_NativeTox_passKeyDeriveWithSalt(JNIEnv *env, jobject thiz, jbyteArray passphrase, jbyteArray salt) {
    // In the simple API approach, we just return the passphrase as the "key".
    // The actual key derivation happens inside tox_pass_encrypt/decrypt.
    return (jbyteArray)env->NewLocalRef(passphrase);
}

// Расшифрование данных профиля с использованием пароля/ключа
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

// Зашифрование данных профиля с использованием пароля/ключа
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

// ===================================================================================
// Управление конференциями (Групповые чаты)
// ===================================================================================

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

// Установка нового названия (заголовка) конференции
JNIEXPORT void JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceSetTitle(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber, jbyteArray title) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint8_t> t = jba2vec(env, title);
    TOX_ERR_CONFERENCE_TITLE err;
    tox_conference_set_title(tox, conferenceNumber, t.data(), t.size(), &err);
    if (err != TOX_ERR_CONFERENCE_TITLE_OK) {
        LOGE("tox_conference_set_title failed: %d", err);
    }
}

// Получение текущего названия (заголовка) конференции
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferenceGetTitle(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_TITLE err;
    size_t len = tox_conference_get_title_size(tox, conferenceNumber, &err);
    if (err != TOX_ERR_CONFERENCE_TITLE_OK || len == 0) return nullptr;
    std::vector<uint8_t> title(len);
    if (tox_conference_get_title(tox, conferenceNumber, title.data(), &err)) {
        return vec2jba(env, title.data(), len);
    }
    return nullptr;
}

// Проверка, является ли указанный участник конференции текущим пользователем
JNIEXPORT jboolean JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxConferencePeerNumberIsOurself(JNIEnv *env, jobject thiz, jlong toxPtr, jint conferenceNumber, jint peerNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    TOX_ERR_CONFERENCE_PEER_QUERY err;
    bool res = tox_conference_peer_number_is_ours(tox, conferenceNumber, peerNumber, &err);
    if (err != TOX_ERR_CONFERENCE_PEER_QUERY_OK) {
        return false;
    }
    return res;
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

// ===================================================================================
// Групповые конференции нового поколения NGC (Next Generation Conferences / Tox Groups)
// ===================================================================================

// Создание нового NGC группового чата
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupNew(JNIEnv *env, jobject thiz, jlong toxPtr, jint privacyState, jbyteArray groupName, jbyteArray selfName) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint8_t> name = jba2vec(env, groupName);
    std::vector<uint8_t> self = jba2vec(env, selfName);
    Tox_Err_Group_New err;
    uint32_t group_num = tox_group_new(tox, (Tox_Group_Privacy_State)privacyState, name.data(), name.size(), self.data(), self.size(), &err);
    if (err != TOX_ERR_GROUP_NEW_OK) {
        LOGE("tox_group_new failed: %d", err);
        return -1;
    }
    return group_num;
}

// Принятие приглашения и вход в NGC групповой чат
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupJoin(JNIEnv *env, jobject thiz, jlong toxPtr, jint friendNumber, jbyteArray inviteData, jbyteArray selfName, jbyteArray password) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint8_t> invite = jba2vec(env, inviteData);
    std::vector<uint8_t> self = jba2vec(env, selfName);
    std::vector<uint8_t> pass = jba2vec(env, password);
    Tox_Err_Group_Invite_Accept err;
    uint32_t group_num = tox_group_invite_accept(tox, friendNumber, invite.data(), invite.size(), self.data(), self.size(), pass.empty() ? nullptr : pass.data(), pass.size(), &err);
    if (err != TOX_ERR_GROUP_INVITE_ACCEPT_OK) {
        LOGE("tox_group_invite_accept failed: %d", err);
        return -1;
    }
    return group_num;
}

// Выход из NGC группового чата
JNIEXPORT jboolean JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupLeave(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_Leave err;
    bool res = tox_group_leave(tox, groupNumber, nullptr, 0, &err);
    if (err != TOX_ERR_GROUP_LEAVE_OK) {
        LOGE("tox_group_leave failed: %d", err);
        return false;
    }
    return res;
}

// Отправка текстового сообщения в NGC групповой чат
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupSendMessage(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber, jint type, jbyteArray message) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint8_t> msg = jba2vec(env, message);
    Tox_Err_Group_Send_Message err;
    uint32_t message_id = tox_group_send_message(tox, groupNumber, (TOX_MESSAGE_TYPE)type, msg.data(), msg.size(), &err);
    if (err != TOX_ERR_GROUP_SEND_MESSAGE_OK) {
        LOGE("tox_group_send_message failed: %d", err);
        return -1;
    }
    return message_id;
}

// Установка темы NGC группового чата
JNIEXPORT jboolean JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupSetTopic(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber, jbyteArray topic) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint8_t> top = jba2vec(env, topic);
    Tox_Err_Group_Topic_Set err;
    bool res = tox_group_set_topic(tox, groupNumber, top.data(), top.size(), &err);
    if (err != TOX_ERR_GROUP_TOPIC_SET_OK) {
        LOGE("tox_group_set_topic failed: %d", err);
        return false;
    }
    return res;
}

// Получение темы NGC группового чата
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupGetTopic(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_State_Query err;
    size_t size = tox_group_get_topic_size(tox, groupNumber, &err);
    if (err != TOX_ERR_GROUP_STATE_QUERY_OK || size == 0) return nullptr;
    std::vector<uint8_t> topic(size);
    if (tox_group_get_topic(tox, groupNumber, topic.data(), &err)) {
        return vec2jba(env, topic.data(), size);
    }
    return nullptr;
}

// Получение названия NGC группового чата
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupGetName(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_State_Query err;
    size_t size = tox_group_get_name_size(tox, groupNumber, &err);
    if (err != TOX_ERR_GROUP_STATE_QUERY_OK || size == 0) return nullptr;
    std::vector<uint8_t> name(size);
    if (tox_group_get_name(tox, groupNumber, name.data(), &err)) {
        return vec2jba(env, name.data(), size);
    }
    return nullptr;
}

// Получение уникального постоянного 32-байтового идентификатора NGC группового чата
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupGetChatId(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_State_Query err;
    uint8_t chat_id[TOX_GROUP_CHAT_ID_SIZE];
    if (tox_group_get_chat_id(tox, groupNumber, chat_id, &err)) {
        return vec2jba(env, chat_id, TOX_GROUP_CHAT_ID_SIZE);
    }
    LOGE("tox_group_get_chat_id failed: %d", err);
    return nullptr;
}

// Установка или удаление пароля NGC группового чата
JNIEXPORT jboolean JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupSetPassword(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber, jbyteArray password) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_Set_Password err;
    if (password == nullptr) {
        bool res = tox_group_set_password(tox, groupNumber, nullptr, 0, &err);
        return res && (err == TOX_ERR_GROUP_SET_PASSWORD_OK);
    }
    std::vector<uint8_t> pass = jba2vec(env, password);
    bool res = tox_group_set_password(tox, groupNumber, pass.data(), pass.size(), &err);
    if (err != TOX_ERR_GROUP_SET_PASSWORD_OK) {
        LOGE("tox_group_set_password failed: %d", err);
        return false;
    }
    return res;
}

// Получение текущего установленного пароля NGC группового чата
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupGetPassword(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_State_Query err;
    size_t size = tox_group_get_password_size(tox, groupNumber, &err);
    if (err != TOX_ERR_GROUP_STATE_QUERY_OK || size == 0) return nullptr;
    std::vector<uint8_t> pass(size);
    if (tox_group_get_password(tox, groupNumber, pass.data(), &err)) {
        return vec2jba(env, pass.data(), size);
    }
    return nullptr;
}

// Получение имени участника NGC группового чата по его ID
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupPeerGetName(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber, jint peerId) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_Peer_Query err;
    size_t size = tox_group_peer_get_name_size(tox, groupNumber, peerId, &err);
    if (err != TOX_ERR_GROUP_PEER_QUERY_OK || size == 0) return nullptr;
    std::vector<uint8_t> name(size);
    if (tox_group_peer_get_name(tox, groupNumber, peerId, name.data(), &err)) {
        return vec2jba(env, name.data(), size);
    }
    return nullptr;
}

// Получение публичного ключа участника NGC группового чата по его ID
JNIEXPORT jbyteArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupPeerGetPublicKey(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber, jint peerId) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_Peer_Query err;
    uint8_t pk[TOX_PUBLIC_KEY_SIZE];
    if (tox_group_peer_get_public_key(tox, groupNumber, peerId, pk, &err)) {
        return vec2jba(env, pk, TOX_PUBLIC_KEY_SIZE);
    }
    return nullptr;
}

// Получение нашего Peer ID в NGC групповом чате
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupSelfGetPeerId(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_Self_Query err;
    uint32_t peer_id = tox_group_self_get_peer_id(tox, groupNumber, &err);
    if (err != TOX_ERR_GROUP_SELF_QUERY_OK) {
        LOGE("tox_group_self_get_peer_id failed: %d", err);
        return -1;
    }
    return peer_id;
}

// Получение нашей роли в NGC групповом чате
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupSelfGetRole(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_Self_Query err;
    Tox_Group_Role role = tox_group_self_get_role(tox, groupNumber, &err);
    if (err != TOX_ERR_GROUP_SELF_QUERY_OK) {
        LOGE("tox_group_self_get_role failed: %d", err);
        return -1;
    }
    return (jint)role;
}

// Отправка приглашения в NGC группу конкретному другу
JNIEXPORT jboolean JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupInviteSend(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber, jint friendNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_Invite_Friend err;
    bool res = tox_group_invite_friend(tox, groupNumber, friendNumber, &err);
    if (err != TOX_ERR_GROUP_INVITE_FRIEND_OK) {
        LOGE("tox_group_invite_friend failed: %d", err);
        return false;
    }
    return res;
}

// Присоединение к NGC группе напрямую по Chat ID (без инвайта от друга)
JNIEXPORT jint JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupJoinDirect(JNIEnv *env, jobject thiz, jlong toxPtr, jbyteArray chatId, jbyteArray selfName, jbyteArray password) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint8_t> cid = jba2vec(env, chatId);
    std::vector<uint8_t> self = jba2vec(env, selfName);
    std::vector<uint8_t> pass = jba2vec(env, password);
    Tox_Err_Group_Join err;
    uint32_t group_num = tox_group_join(tox, cid.data(), self.data(), self.size(), pass.empty() ? nullptr : pass.data(), pass.size(), &err);
    if (err != TOX_ERR_GROUP_JOIN_OK) {
        LOGE("tox_group_join failed: %d", err);
        return -1;
    }
    return group_num;
}

// Переподключение к ранее сохранённой NGC-группе после загрузки профиля
JNIEXPORT jboolean JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupReconnect(JNIEnv *env, jobject thiz, jlong toxPtr, jint groupNumber) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    Tox_Err_Group_Reconnect err;
    bool res = tox_group_reconnect(tox, groupNumber, &err);
    if (err != TOX_ERR_GROUP_RECONNECT_OK) {
        LOGE("tox_group_reconnect failed: %d", err);
        return false;
    }
    return res;
}

// Перечисление всех активных NGC групп в текущей сессии Tox.
// Возвращает массив groupNumber всех групп, которые toxcore знает на данный момент.
JNIEXPORT jintArray JNICALL
Java_ltd_evilcorp_core_tox_NativeTox_toxGroupGetChatlist(JNIEnv *env, jobject thiz, jlong toxPtr) {
    Tox *tox = reinterpret_cast<Tox*>(toxPtr);
    std::vector<uint32_t> groups;
    // Сканируем все возможные groupNumber (от 0 до 65535).
    // tox_group_get_chat_id вернёт false для невалидных номеров.
    for (uint32_t i = 0; i <= 65535; i++) {
        uint8_t chat_id[TOX_GROUP_CHAT_ID_SIZE];
        Tox_Err_Group_State_Query err;
        if (tox_group_get_chat_id(tox, i, chat_id, &err)) {
            groups.push_back(i);
        }
    }
    jintArray res = env->NewIntArray(groups.size());
    env->SetIntArrayRegion(res, 0, groups.size(), reinterpret_cast<const jint*>(groups.data()));
    return res;
}

// Инициализация JNI-библиотеки при ее загрузке в JVM (кэширование Method ID событий)
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
    mid_onConferenceTitle = env->GetMethodID(cls, "onConferenceTitle", "(II[B)V");

    // Поиск Method ID обработчиков событий NGC-групп в классе-слушателе
    mid_onGroupInvite = env->GetMethodID(cls, "onGroupInvite", "(I[B[B)V");
    mid_onGroupMessage = env->GetMethodID(cls, "onGroupMessage", "(III[BI)V");
    mid_onGroupPeerJoin = env->GetMethodID(cls, "onGroupPeerJoin", "(II)V");
    mid_onGroupPeerExit = env->GetMethodID(cls, "onGroupPeerExit", "(III)V");
    mid_onGroupTopic = env->GetMethodID(cls, "onGroupTopic", "(II[B)V");
    mid_onGroupPeerName = env->GetMethodID(cls, "onGroupPeerName", "(II[B)V");
    mid_onGroupPassword = env->GetMethodID(cls, "onGroupPassword", "(I[B)V");
    mid_onGroupPeerStatus = env->GetMethodID(cls, "onGroupPeerStatus", "(III)V");
    mid_onGroupPrivacyState = env->GetMethodID(cls, "onGroupPrivacyState", "(II)V");
    mid_onGroupVoiceState = env->GetMethodID(cls, "onGroupVoiceState", "(II)V");
    mid_onGroupTopicLock = env->GetMethodID(cls, "onGroupTopicLock", "(II)V");
    mid_onGroupPeerLimit = env->GetMethodID(cls, "onGroupPeerLimit", "(II)V");
    mid_onGroupPrivateMessage = env->GetMethodID(cls, "onGroupPrivateMessage", "(III[BI)V");
    mid_onGroupSelfJoin = env->GetMethodID(cls, "onGroupSelfJoin", "(I)V");
    mid_onGroupJoinFail = env->GetMethodID(cls, "onGroupJoinFail", "(II)V");
    mid_onGroupModeration = env->GetMethodID(cls, "onGroupModeration", "(IIII)V");

    if (!register_toxav_methods(env)) {
        LOGE("Failed to register ToxAV methods");
    }

    return JNI_VERSION_1_6;
}

}

