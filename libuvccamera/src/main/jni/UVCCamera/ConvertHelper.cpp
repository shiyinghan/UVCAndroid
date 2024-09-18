#include "ConvertHelper.h"

#define THROW(action, message) { \
  LOGI("ERROR in line %d while %s:\n%s\n", __LINE__, action, message); \
  retval = -1;  goto fail; \
}

#define THROW_TJ(action)  THROW(action, tjGetErrorStr2(tjInstance))

#define THROW_UNIX(action)  THROW(action, strerror(errno))

#define WARN_TJ(action)  { \
  LOGW("ERROR in line %d while %s:\n%s\n", __LINE__, action, tjGetErrorStr2(tjInstance)); \
}

#define DEBUG_TJ(action)  { \
  LOGD("ERROR in line %d while %s:\n%s\n", __LINE__, action, tjGetErrorStr2(tjInstance)); \
}

const char *subsampName[TJ_NUMSAMP] = {
        "4:4:4", "4:2:2", "4:2:0", "Grayscale", "4:4:0", "4:1:1"
};

const char *colorspaceName[TJ_NUMCS] = {
        "RGB", "YCbCr", "GRAY", "CMYK", "YCCK"
};

int convert_mjpeg_to_rgbx_tj(void *in, int inSize, void *out, int width, int height) {
    int retval = UVC_SUCCESS;
    int flags = 0;
    int pixelFormat = TJPF_RGBX;
    tjhandle tjInstance = NULL;
    int inSubsamp, inColorspace;
    int _width, _height;
    const unsigned char *jpegBuf = (const unsigned char *) (in);
    unsigned char *dstBuf = (unsigned char *) (out);

    if ((tjInstance = tjInitDecompress()) == NULL) {
        THROW_TJ("initializing decompressor");
    }

    if (tjDecompressHeader3(tjInstance, jpegBuf, inSize, &_width, &_height,
                            &inSubsamp, &inColorspace) < 0) {
        DEBUG_TJ("reading JPEG header failed");
        retval = UVC_ERROR_INVALID_PARAM;
        goto fail;
    }

    if (_width != width || _height != height) {
        DEBUG_TJ("reading JPEG header error size");
        retval = UVC_ERROR_INVALID_PARAM;
        goto fail;
    }

//    LOGI("Image:  %d x %d pixels, %s subsampling, %s colorspace\n",
//         out->width, out->height,
//           subsampName[inSubsamp], colorspaceName[inColorspace]);

    // will cause memory overflow
//    if ((out->data = (unsigned char *) tjAlloc(tjBufSizeYUV2(width, padding, height, outSubsamp))) ==NULL)
//        THROW_UNIX("allocating uncompressed image buffer");

    if (tjDecompress2(tjInstance, jpegBuf, inSize, dstBuf, width, 0,
                      height, pixelFormat, flags) < 0) {
        DEBUG_TJ("decompressing JPEG image");
        retval = UVC_ERROR_INVALID_PARAM;
        goto fail;
    }
    tjDestroy(tjInstance);
    tjInstance = NULL;
    return retval;

    fail:
    tjDestroy(tjInstance);
    tjInstance = NULL;
    return retval;
}

/** @brief Convert an MJPEG frame to RGBX
 * @ingroup frame
 *
 * @param in MJPEG frame
 * @param out RGBX frame
 */
int uvc_mjpeg2rgbx_tj(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_MJPEG)
        return UVC_ERROR_INVALID_PARAM;

    const int pixel_byte = 4;

    if (uvc_ensure_frame_size(out, in->width * in->height * pixel_byte) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    out->step = in->width * pixel_byte;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->capture_time_finished = in->capture_time_finished;
    out->source = in->source;

    return convert_mjpeg_to_rgbx_tj(in->data, in->data_bytes, out->data, out->width, out->height);
}

/** @brief Convert an MJPEG frame to RGBX
 * @ingroup frame
 *
 * @param in MJPEG frame
 * @param out RGBX frame
 */
int uvc_mjpeg2rgbx_new(uvc_frame_t *in, uvc_frame_t *out) {
    uint8_t *in_data = (uint8_t *) in->data;
    uint8_t *out_data = (uint8_t *) out->data;

    if (in->frame_format != UVC_FRAME_FORMAT_MJPEG)
        return UVC_ERROR_INVALID_PARAM;

    const int pixel_byte = 4;

    if (uvc_ensure_frame_size(out, in->width * in->height * pixel_byte) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    out->step = in->width * pixel_byte;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->capture_time_finished = in->capture_time_finished;
    out->source = in->source;

    int width = in->width;
    int height = in->height;
    unsigned char *srcNV21 = (unsigned char *) malloc(width * height * PIXEL_NV21);
    unsigned char *src_nv21_y_data = (unsigned char *) in;
    unsigned char *src_nv21_uv_data = (unsigned char *) in + width * height;

    int result = libyuv::MJPGToNV21(in_data, in->data_bytes,
                                    src_nv21_y_data, width, src_nv21_uv_data, width,
                                    width, height, width, height);
    if (result == 0) {
        result = libyuv::NV21ToRGB24(src_nv21_y_data, width, src_nv21_uv_data, width,
                                     out_data, width * PIXEL_RGB, width, height);
    }

    free(srcNV21);

    return result;
}


/** @brief Convert a frame from RGBX8888 to YUYV
* @ingroup frame
* @param ini RGBX8888 frame
* @param out YUYV frame
*/
int uvc_rgbx_to_yuyv(uvc_frame_t *in, uvc_frame_t *out) {
    uint8_t *in_data = (uint8_t *) in->data;
    uint8_t *out_data = (uint8_t *) out->data;

    if (in->frame_format != UVC_FRAME_FORMAT_RGBX)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height * PIXEL_YUYV) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_YUYV;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_YUYV;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    int ret = UVC_SUCCESS;

    ret = libyuv::ABGRToYUY2(in_data, in->step,
                             out_data, out->step, out->width, out->height);

    return  ret;
}

/** @brief Convert a frame from RGBX8888 to NV12
* @ingroup frame
* @param ini RGBX8888 frame
* @param out NV12 frame
*/
 int uvc_rgbx_to_nv12(uvc_frame_t *in, uvc_frame_t *out) {
    uint8_t *in_data = (uint8_t *) in->data;
    uint8_t *out_data = (uint8_t *) out->data;

    if (in->frame_format != UVC_FRAME_FORMAT_RGBX)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, (in->width * in->height * 3) / 2) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_NV12;
    if (out->library_owns_data)
        out->step = in->width;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    int ret = UVC_SUCCESS;

    ret = libyuv::ABGRToNV12(in_data, in->step, out_data, out->step,
                             out_data + out->width * out->height,
                             out->step, out->width, out->height);

    return  ret;
}

/** @brief Convert a frame from RGBX8888 to NV21
* @ingroup frame
* @param ini RGBX8888 frame
* @param out NV21 frame
*/
 int uvc_rgbx_to_nv21(uvc_frame_t *in, uvc_frame_t *out) {
    uint8_t *in_data = (uint8_t *) in->data;
    uint8_t *out_data = (uint8_t *) out->data;

    if (in->frame_format != UVC_FRAME_FORMAT_RGBX)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, (in->width * in->height * 3) / 2) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_NV21;
    if (out->library_owns_data)
        out->step = in->width;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    int ret = UVC_SUCCESS;

    ret = libyuv::ABGRToNV21(in_data, in->step, out_data, out->step,
                             out_data + out->width * out->height,
                             out->step, out->width, out->height);

    return  ret;
}

/** @brief Convert a frame from RGBX8888 to RGB
* @ingroup frame
* @param ini RGBX8888 frame
* @param out RGB frame
*/
 int uvc_rgbx_to_rgb(uvc_frame_t *in, uvc_frame_t *out) {
    uint8_t *in_data = (uint8_t *) in->data;
    uint8_t *out_data = (uint8_t *) out->data;

    if (in->frame_format != UVC_FRAME_FORMAT_RGBX)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGB) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGB;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_RGB;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    int ret = UVC_SUCCESS;

    ret = libyuv::ABGRToRAW(in_data, in->step, out_data, out->step,
                            out->width, out->height);

    return  ret;
}

/** @brief Convert a frame from RGBX8888 to RGB565
* @ingroup frame
* @param ini RGBX8888 frame
* @param out RGB565 frame
*/
 int uvc_rgbx_to_rgb565(uvc_frame_t *in, uvc_frame_t *out) {
    uint8_t *in_data = (uint8_t *) in->data;
    uint8_t *out_data = (uint8_t *) out->data;

    if (in->frame_format != UVC_FRAME_FORMAT_RGBX)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGB565) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGB565;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_RGB565;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    int ret = UVC_SUCCESS;

    ret = libyuv::ABGRToRGB565(in_data, in->step, out_data, out->step,
                               out->width, out->height);

    return  ret;
}

/** @brief Convert a frame from RGBX8888 to BGR
* @ingroup frame
* @param ini RGBX8888 frame
* @param out BGR frame
*/
 int uvc_rgbx_to_bgr(uvc_frame_t *in, uvc_frame_t *out) {
    uint8_t *in_data = (uint8_t *) in->data;
    uint8_t *out_data = (uint8_t *) out->data;

    if (in->frame_format != UVC_FRAME_FORMAT_RGBX)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height * PIXEL_BGR) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_BGR;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_BGR;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    int ret = UVC_SUCCESS;

    ret = libyuv::ABGRToRGB24(in_data, in->step, out_data, out->step,
                              out->width, out->height);

    return  ret;
}