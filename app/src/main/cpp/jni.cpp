#include <climits>
//
// Created by matze on 06.11.20.
//

#include <jni.h>

#include <android/log.h>

#include <oboe/Oboe.h>

#include <opus.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#include <cstring>
#include <cerrno>
#include <unistd.h>

#define MAX_LOG_MESSAGE_LENGTH 256
#define MAX_FRAME_LENGTH 1000

#define SAMPLE_RATE 48000
#define FRAME_SIZE 1920
// 20ms < 48000 / 1920 = 25 ms
#define AUDIO_TIMEOUT 20000

using namespace oboe;

// throw a exception
void ThrowException(JNIEnv* env, const char* className, const char* message)
{
    jclass clazz = env->FindClass(className);
    if (nullptr != clazz) {
        env->ThrowNew(clazz, message);
        env->DeleteLocalRef(clazz);
    }
}


// throw errno exception
void ThrowErrnoException(JNIEnv* env, const char* className, int errnum)
{
    char buffer[MAX_LOG_MESSAGE_LENGTH];
    strerror_r(errnum, buffer, MAX_LOG_MESSAGE_LENGTH);
    ThrowException(env, className, buffer);
}

void LogErrno(const char *tag)
{
    __android_log_print(ANDROID_LOG_ERROR,
            tag, "%s",
            strerror(errno));
}

bool checkRunning(JNIEnv *env, jobject thiz, const char *member)
{
    jclass audioThread = env->GetObjectClass(thiz);
    jmethodID getter = env->GetMethodID(audioThread, member,"()Z");
    return env->CallBooleanMethod(thiz, getter);
}

/* ---------------------
 *
 * AudioRecorder
 *
 * ---------------------
 */

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_splitter_OpusEncoder_00024Companion_init_1encoder(
    JNIEnv *env,
    jobject thiz,
    jint sample_rate,
    jint i)
{
    return (jlong) opus_encoder_create(sample_rate, i, OPUS_APPLICATION_AUDIO, nullptr);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_splitter_OpusEncoder_00024Companion_native_1encode(
    JNIEnv *env,
    jobject thiz,
    jlong enc,
    jint frame_size,
    jshortArray input,
    jbyteArray output,
    jint len)
{
    jshort *audioSignal = env->GetShortArrayElements(input, 0);

    auto *data = new unsigned char[len];
    int numEncoded = opus_encode((OpusEncoder *) enc, audioSignal, frame_size, data, len);

    env->SetByteArrayRegion(output, 0, numEncoded, (const jbyte *) data);
    return numEncoded;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_splitter_OpusEncoder_00024Companion_native_1close(
    JNIEnv *env,
    jobject thiz,
    jlong enc)
{
    opus_encoder_destroy((OpusEncoder*) enc);
}

/* ---------------------
 *
 * AudioReceiver
 *
 * ----------------------
 */

extern "C"
JNIEXPORT void JNICALL
Java_com_example_splitter_ReceiveService_nativeRecvLoop(
    JNIEnv *env,
    jobject thiz,
    jint sock_fd)
{
    int err;
    OpusDecoder *dec;

    dec = opus_decoder_create(SAMPLE_RATE, 2, &err);
    if(err != 0)
    {
        __android_log_print(ANDROID_LOG_ERROR,
                "AudioEngine",
                "Error creating decoder: %s",
                opus_strerror(err));
        return;
    }

    AudioStream *audioStream;
    unsigned int frame_length;
    unsigned char frame[MAX_FRAME_LENGTH];
    int nread, ndecoded;
    opus_int16 decoded_frame [FRAME_SIZE * 2];

    Result res;
    AudioStreamBuilder builder;
    builder.setDirection(Direction::Output);
    builder.setSampleRate(SAMPLE_RATE);
    builder.setChannelCount(2);
    builder.setFormat(AudioFormat::I16);
    builder.setUsage(Usage::Media);
    builder.setContentType(ContentType::Music);

    res = builder.openStream(&audioStream);
    if (res != Result::OK)
    {
        __android_log_print(ANDROID_LOG_ERROR,
                            "AudioEngine",
                            "Error opening stream %s",
                            convertToText(res));
    }

    memset(&frame, 0, sizeof(frame));

    while (checkRunning(env, thiz, "getStarted"))
    {
        if(recv(sock_fd, &frame_length, sizeof(frame_length), 0) != 4)
        {
            //we should probably handle this
            close(sock_fd);
            ThrowException(env,
                    "java/lang/Exception",
                    "bad frame size");
        }
        frame_length = be32toh(frame_length);

        if (frame_length == 0) break;

        for (nread = 0; nread < frame_length;)
        {
            nread += recv(sock_fd, &frame[nread], frame_length - nread, 0);
        }

        ndecoded = opus_decode(
                dec, frame, frame_length, decoded_frame, FRAME_SIZE, 0);
        if (ndecoded < 0)
        {
            close(sock_fd);
            __android_log_print(ANDROID_LOG_ERROR,
                    "AudioEngine", "opus_decode: %s",
                    opus_strerror(ndecoded));
        }

        if(audioStream->getState() != StreamState::Started)
        {
            audioStream->start(AUDIO_TIMEOUT);
        }

        res = audioStream->write(decoded_frame, FRAME_SIZE, AUDIO_TIMEOUT);
        if (res != Result::OK)
        {
            __android_log_print(ANDROID_LOG_ERROR,
                    "AudioEngine", "stream->write(): %s",
                    convertToText(res));
        }
    }

    audioStream->close();

    if(close(sock_fd) < 0)
    {
        ThrowErrnoException(env, "java/io/IOException", errno);
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_splitter_ReceiveService_connect(
    JNIEnv *env,
    jobject  __unused _thiz,
    jint port,
    jstring host)
{
    int sock_fd;
    struct sockaddr_in server;
    unsigned long addr;

    const char* cstrIpAddr = env->GetStringUTFChars(host, nullptr);
    if (nullptr == cstrIpAddr)
    {
        ThrowException(env,
                       "java/lang/Exception",
                       "invalid ip string");
    }

    memset(&server, 0, sizeof(server));
    if (0 == inet_aton(cstrIpAddr, (in_addr *) &addr))
    {
        ThrowException(env,
                       "java/lang/Exception",
                       "invalid ip string");
    }

    memcpy( (char *)&server.sin_addr, &addr, sizeof(addr));
    server.sin_port = htons(port);
    server.sin_family = AF_INET;

    sock_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (sock_fd < 0)
    {
        LogErrno("AudioReceiver");
        return -1;
    }

    if (connect(sock_fd, (const struct sockaddr*)&server, sizeof(server)) < 0)
    {
        close(sock_fd);
        LogErrno("AudioReceiver");
        return -1;
    }

    return sock_fd;
}