//
//

#ifndef AMP_RACK_AUDIOBUFFER_H
#define AMP_RACK_AUDIOBUFFER_H

typedef struct audio_buffer {
    int overruns;
    int pos;
//    float data[];
    float *data;
    float * raw;
    int size ;
} AudioBuffer;

#endif //AMP_RACK_AUDIOBUFFER_H
