/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SAMPLES_FULLDUPLEXPASS_H
#define SAMPLES_FULLDUPLEXPASS_H

#include <dlfcn.h>
#include <mutex>
#include <cmath>
#include <algorithm>
#include <cstring>
#include "ladspa.h"
#include "FullDuplexStream.h"
#include "FileWriter.h"
#include "Meter.h"
#include "LockFreeQueue.h"

class FullDuplexPass : public FullDuplexStream {
public:
    #define MAX_PLUGINS 10
    float inputVolume = 1.0f ;
    float outputVolume = 1.0f ;
    float masterGain = 1.8f ;
    bool meterEnabled = true ;
    bool pause = false ;
    bool noiseGateEnabled = true ;
    float gateThresholdDb = -55.0f ;
    bool humFilterEnabled = true ;
    int sampleRate = 48000 ;

    // Lock Free Queue Manager
    void (queue)(float * data, int frames) ;
    LockFreeQueueManager * lockFreeQueueManager = nullptr;

    void (*connect_port [MAX_PLUGINS])(LADSPA_Handle Instance,
                         unsigned long Port,
                         LADSPA_Data * DataLocation);
    void (*run [MAX_PLUGINS])(LADSPA_Handle Instance,
                unsigned long SampleCount);
    void (*run_adding [MAX_PLUGINS])(LADSPA_Handle Instance,
                unsigned long SampleCount);
    void (*set_run_adding_gain [MAX_PLUGINS])(LADSPA_Handle Instance,
                                LADSPA_Data   Gain);

    void * handle [MAX_PLUGINS] ;
    LADSPA_Data run_adding_gain [MAX_PLUGINS] ;
    const LADSPA_Descriptor * descriptor [MAX_PLUGINS] ;
    int inputPorts [MAX_PLUGINS] ;
    int inputPorts2 [MAX_PLUGINS] ;
    int outputPorts [MAX_PLUGINS] ;
    int outputPorts2 [MAX_PLUGINS] ;
    bool pushToLockFreeBeforeOutputVolume = false ;
    int activePlugins = 0 ;
    std::mutex chainMutex ;
    float * raw = nullptr ;
    bool recordingActive = false ;
    bool triggerRecord = false ;
    bool bypass = false ;
    float * inSamples = nullptr ;
    float * left = nullptr ;
    float * right = nullptr ;

    void setSampleRate (int rate) {
        if (rate > 0) {
            sampleRate = rate ;
            configureFilters () ;
        }
    }

    virtual oboe::DataCallbackResult
    onBothStreamsReady(
            std::shared_ptr<oboe::AudioStream> inputStream,
            const void *inputData,
            int numInputFrames,
            std::shared_ptr<oboe::AudioStream> outputStream,
            void *outputData,
            int numOutputFrames) {
        const float *inputFloats = static_cast<const float *>(inputData);
        float *outputFloats = static_cast<float *>(outputData);
        int32_t inputChannels = std::max(1, inputStream->getChannelCount());
        int32_t outputChannels = std::max(1, outputStream->getChannelCount());
        int32_t frames = std::min(numInputFrames, numOutputFrames);

        if (left == nullptr || right == nullptr || numOutputFrames <= 0) {
            memset(outputData, 0, numOutputFrames * outputChannels * sizeof(float));
            return oboe::DataCallbackResult::Continue;
        }

        for (int32_t i = 0; i < numOutputFrames; i++) {
            left[i] = 0.0f;
            right[i] = 0.0f;
        }

        for (int32_t i = 0; i < frames; i++) {
            float l = inputFloats[i * inputChannels] * inputVolume;
            float r = inputChannels > 1
                    ? inputFloats[i * inputChannels + 1] * inputVolume
                    : l;
            left[i] = l;
            right[i] = r;
        }

        if (noiseGateEnabled) {
            applyNoiseGate(left, right, numOutputFrames);
        }
        if (humFilterEnabled) {
            applyHumFilters(left, right, numOutputFrames);
        }

        if (!bypass) {
            process(left, right, numOutputFrames);
        }

        float gain = outputVolume * masterGain;
        for (int32_t i = 0; i < numOutputFrames; i++) {
            float l = softClip(left[i] * gain);
            float r = softClip(right[i] * gain);
            for (int32_t c = 0; c < outputChannels; c++) {
                outputFloats[i * outputChannels + c] = (c == 0) ? l : r;
            }
        }

        if (lockFreeQueueManager != nullptr && raw != nullptr && inSamples != nullptr && !pause && numOutputFrames > 0) {
            for (int32_t i = 0; i < numOutputFrames; i++) {
                raw[i] = left[i];
                inSamples[i] = right[i];
            }
            lockFreeQueueManager->process(raw, inSamples, numOutputFrames);
        }

        return oboe::DataCallbackResult::Continue;
    }

    void process (float * l, float * r, int frames) {
        std::unique_lock<std::mutex> lock(chainMutex, std::try_to_lock);
        if (!lock.owns_lock()) {
            return;
        }
        for (int i = 0; i < activePlugins; i++) {
            if (inputPorts[i] != -1)
                connect_port[i](handle[i], inputPorts[i], (LADSPA_Data *) l);
            if (inputPorts2[i] != -1)
                connect_port[i](handle[i], inputPorts2[i], (LADSPA_Data *) r);
            if (outputPorts[i] != -1)
                connect_port[i](handle[i], outputPorts[i], (LADSPA_Data *) l);
            if (outputPorts2[i] != -1)
                connect_port[i](handle[i], outputPorts2[i], (LADSPA_Data *) r);

            if (run[i] == nullptr) {
                continue;
            }
            run[i](handle[i], frames);

            // A mono pedal only writes the left channel. Mirror it so the
            // pedal does not collapse the output to one side.
            if (outputPorts2[i] == -1) {
                for (int f = 0; f < frames; f++) {
                    r[f] = l[f];
                }
            }
        }
    }

    void configureFilters () {
        float sr = (float) std::max(1, sampleRate);
        hpL.setHighPass(sr, 25.0f, 0.707f);
        hpR.setHighPass(sr, 25.0f, 0.707f);
        notch50L.setNotch(sr, 50.0f, 12.0f);
        notch50R.setNotch(sr, 50.0f, 12.0f);
        notch60L.setNotch(sr, 60.0f, 12.0f);
        notch60R.setNotch(sr, 60.0f, 12.0f);
        gateEnv = 0.0f;
        gateGain = 1.0f;
    }

    void applyHumFilters (float * l, float * r, int frames) {
        for (int i = 0; i < frames; i++) {
            float xl = hpL.process(notch50L.process(notch60L.process(l[i])));
            float xr = hpR.process(notch50R.process(notch60R.process(r[i])));
            l[i] = xl;
            r[i] = xr;
        }
    }

    void applyNoiseGate (float * l, float * r, int frames) {
        float threshold = std::pow(10.0f, gateThresholdDb / 20.0f);
        float attack = std::exp(-1.0f / (0.002f * (float) sampleRate));
        float release = std::exp(-1.0f / (0.150f * (float) sampleRate));
        for (int i = 0; i < frames; i++) {
            float level = std::max(std::fabs(l[i]), std::fabs(r[i]));
            if (level > gateEnv) {
                gateEnv = attack * gateEnv + (1.0f - attack) * level;
            } else {
                gateEnv = release * gateEnv + (1.0f - release) * level;
            }
            float target = gateEnv > threshold ? 1.0f : 0.0f;
            float speed = target > gateGain ? 0.08f : 0.002f;
            gateGain += (target - gateGain) * speed;
            l[i] *= gateGain;
            r[i] *= gateGain;
        }
    }

    static float softClip (float x) {
        float ax = std::fabs(x);
        if (ax <= 0.8f) {
            return x;
        }
        float y = 0.8f + 0.2f * std::tanh((ax - 0.8f) / 0.2f);
        return x < 0.0f ? -y : y;
    }

    static constexpr float kPi = 3.14159265358979323846f;

    struct Biquad {
        float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f, a1 = 0.0f, a2 = 0.0f;
        float x1 = 0.0f, x2 = 0.0f, y1 = 0.0f, y2 = 0.0f;

        void setHighPass (float sr, float freq, float q) {
            float w0 = 2.0f * kPi * freq / sr;
            float alpha = std::sin(w0) / (2.0f * q);
            float cosw = std::cos(w0);
            float a0 = 1.0f + alpha;
            b0 = ((1.0f + cosw) / 2.0f) / a0;
            b1 = -(1.0f + cosw) / a0;
            b2 = b0;
            a1 = (-2.0f * cosw) / a0;
            a2 = (1.0f - alpha) / a0;
            reset();
        }

        void setNotch (float sr, float freq, float q) {
            float w0 = 2.0f * kPi * freq / sr;
            float alpha = std::sin(w0) / (2.0f * q);
            float cosw = std::cos(w0);
            float a0 = 1.0f + alpha;
            b0 = 1.0f / a0;
            b1 = (-2.0f * cosw) / a0;
            b2 = 1.0f / a0;
            a1 = (-2.0f * cosw) / a0;
            a2 = (1.0f - alpha) / a0;
            reset();
        }

        void reset () {
            x1 = x2 = y1 = y2 = 0.0f;
        }

        inline float process (float x) {
            float y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    };

    Biquad hpL, hpR, notch50L, notch50R, notch60L, notch60R;
    float gateEnv = 0.0f;
    float gateGain = 1.0f;
};
#endif //SAMPLES_FULLDUPLEXPASS_H
