/*********************************************************************
* Software License Agreement (BSD License)
*
*  Copyright (C) 2010-2012 Ken Tossell
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions
*  are met:
*
*   * Redistributions of source code must retain the above copyright
*     notice, this list of conditions and the following disclaimer.
*   * Redistributions in binary form must reproduce the above
*     copyright notice, this list of conditions and the following
*     disclaimer in the documentation and/or other materials provided
*     with the distribution.
*   * Neither the name of the author nor other contributors may be
*     used to endorse or promote products derived from this software
*     without specific prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
*  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
*  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
*  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
*  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
*  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
*  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
*  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
*  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
*  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
*  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*********************************************************************/
/**
 * @defgroup frame Frame processing
 * @brief Tools for managing frame buffers and converting between image formats
 */
#include "libuvc/libuvc.h"
#include "libuvc/libuvc_internal.h"

/** @internal */
uvc_error_t uvc_ensure_frame_size(uvc_frame_t *frame, size_t need_bytes) {
    if (frame->library_owns_data) {
        frame->data_bytes = need_bytes;
        if (!frame->data || frame->capacity_bytes < need_bytes) {
            frame->capacity_bytes = need_bytes;
            frame->data = realloc(frame->data, frame->capacity_bytes);
        }
        if (!frame->data)
            return UVC_ERROR_NO_MEM;
        return UVC_SUCCESS;
    } else {
        if (!frame->data || frame->capacity_bytes < need_bytes)
            return UVC_ERROR_NO_MEM;
        return UVC_SUCCESS;
    }
}

/** @brief Allocate a frame structure
 * @ingroup frame
 *
 * @param data_bytes Number of bytes to allocate, or zero
 * @return New frame, or NULL on error
 */
uvc_frame_t *uvc_allocate_frame(size_t data_bytes) {
    uvc_frame_t *frame = malloc(sizeof(*frame));

    if (!frame)
        return NULL;

    memset(frame, 0, sizeof(*frame));

    frame->library_owns_data = 1;

    if (data_bytes > 0) {
        frame->capacity_bytes = data_bytes;
        frame->data_bytes = data_bytes;
        frame->data = malloc(data_bytes);

        if (!frame->data) {
            free(frame);
            return NULL;
        }
    }

    return frame;
}

/** @brief Free a frame structure
 * @ingroup frame
 *
 * @param frame Frame to destroy
 */
void uvc_free_frame(uvc_frame_t *frame) {
    if (frame->library_owns_data) {
        if (frame->capacity_bytes > 0 || frame->data_bytes > 0)
            free(frame->data);
        if (frame->metadata_bytes > 0)
            free(frame->metadata);
    }

    free(frame);
}

static inline unsigned char sat(int i) {
    return (unsigned char) (i >= 255 ? 255 : (i < 0 ? 0 : i));
}

/** @brief Duplicate a frame, preserving color format
 * @ingroup frame
 *
 * @param in Original frame
 * @param out Duplicate frame
 */
uvc_error_t uvc_duplicate_frame(uvc_frame_t *in, uvc_frame_t *out) {
    if (uvc_ensure_frame_size(out, in->data_bytes) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = in->frame_format;
    out->step = in->step;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->capture_time_finished = in->capture_time_finished;
    out->source = in->source;

    memcpy(out->data, in->data, in->data_bytes);

    if (in->metadata && in->metadata_bytes > 0) {
        if (out->metadata_bytes < in->metadata_bytes) {
            out->metadata = realloc(out->metadata, in->metadata_bytes);
        }
        out->metadata_bytes = in->metadata_bytes;
        memcpy(out->metadata, in->metadata, in->metadata_bytes);
    }

    return UVC_SUCCESS;
}

#define PIXEL_RGB565        2
#define PIXEL_UYVY            2
#define PIXEL_YUYV            2
#define PIXEL_RGB            3
#define PIXEL_BGR            3
#define PIXEL_RGBX            4

#define PIXEL2_RGB565        PIXEL_RGB565 * 2
#define PIXEL2_UYVY            PIXEL_UYVY * 2
#define PIXEL2_YUYV            PIXEL_YUYV * 2
#define PIXEL2_RGB            PIXEL_RGB * 2
#define PIXEL2_BGR            PIXEL_BGR * 2
#define PIXEL2_RGBX            PIXEL_RGBX * 2

#define PIXEL4_RGB565        PIXEL_RGB565 * 4
#define PIXEL4_UYVY            PIXEL_UYVY * 4
#define PIXEL4_YUYV            PIXEL_YUYV * 4
#define PIXEL4_RGB            PIXEL_RGB * 4
#define PIXEL4_BGR            PIXEL_BGR * 4
#define PIXEL4_RGBX            PIXEL_RGBX * 4

#define PIXEL8_RGB565        PIXEL_RGB565 * 8
#define PIXEL8_UYVY            PIXEL_UYVY * 8
#define PIXEL8_YUYV            PIXEL_YUYV * 8
#define PIXEL8_RGB            PIXEL_RGB * 8
#define PIXEL8_BGR            PIXEL_BGR * 8
#define PIXEL8_RGBX            PIXEL_RGBX * 8

#define PIXEL16_RGB565        PIXEL_RGB565 * 16
#define PIXEL16_UYVY        PIXEL_UYVY * 16
#define PIXEL16_YUYV        PIXEL_YUYV * 16
#define PIXEL16_RGB            PIXEL_RGB * 16
#define PIXEL16_BGR            PIXEL_BGR * 16
#define PIXEL16_RGBX        PIXEL_RGBX * 16

#define RGB2RGBX_2(prgb, prgbx, ax, bx) { \
        (prgbx)[bx+0] = (prgb)[ax+0]; \
        (prgbx)[bx+1] = (prgb)[ax+1]; \
        (prgbx)[bx+2] = (prgb)[ax+2]; \
        (prgbx)[bx+3] = 0xff; \
        (prgbx)[bx+4] = (prgb)[ax+3]; \
        (prgbx)[bx+5] = (prgb)[ax+4]; \
        (prgbx)[bx+6] = (prgb)[ax+5]; \
        (prgbx)[bx+7] = 0xff; \
    }
#define RGB2RGBX_16(prgb, prgbx, ax, bx) \
    RGB2RGBX_8(prgb, prgbx, ax, bx) \
    RGB2RGBX_8(prgb, prgbx, ax + PIXEL8_RGB, bx +PIXEL8_RGBX);
#define RGB2RGBX_8(prgb, prgbx, ax, bx) \
    RGB2RGBX_4(prgb, prgbx, ax, bx) \
    RGB2RGBX_4(prgb, prgbx, ax + PIXEL4_RGB, bx + PIXEL4_RGBX);
#define RGB2RGBX_4(prgb, prgbx, ax, bx) \
    RGB2RGBX_2(prgb, prgbx, ax, bx) \
    RGB2RGBX_2(prgb, prgbx, ax + PIXEL2_RGB, bx + PIXEL2_RGBX);

/** @brief Convert a frame from RGB888 to RGBX8888
 * @ingroup frame
 * @param ini RGB888 frame
 * @param out RGBX8888 frame
 */
uvc_error_t uvc_rgb2rgbx(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_RGB)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGBX) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_RGBX;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    uint8_t *prgb = in->data;
    const uint8_t *prgb_end = prgb + in->data_bytes - PIXEL8_RGB;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // RGB888 to RGBX8888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            prgb = in->data + in->step * h;
            prgbx = out->data + out->step * h;
            for (; (prgbx <= prgbx_end) && (prgb <= prgb_end) && (w < ww) ;) {
                RGB2RGBX_8(prgb, prgbx, 0, 0);

                prgb += PIXEL8_RGB;
                prgbx += PIXEL8_RGBX;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgbx <= prgbx_end) && (prgb <= prgb_end) ;) {
            RGB2RGBX_8(prgb, prgbx, 0, 0);

            prgb += PIXEL8_RGB;
            prgbx += PIXEL8_RGBX;
        }
    }
#else
    for (; (prgbx <= prgbx_end) && (prgb <= prgb_end);) {
        RGB2RGBX_8(prgb, prgbx, 0, 0);

        prgb += PIXEL8_RGB;
        prgbx += PIXEL8_RGBX;
    }
#endif
    return UVC_SUCCESS;
}

#define RGB2RGB565_2(prgb, prgb565, ax, bx) { \
        (prgb565)[bx+0] = (((prgb)[ax+1] << 3) & 0b11100000) | (((prgb)[ax+2] >> 3) & 0b00011111); \
        (prgb565)[bx+1] = (((prgb)[ax+0] & 0b11111000) | (((prgb)[ax+1] >> 5) & 0b00000111)); \
        (prgb565)[bx+2] = (((prgb)[ax+4] << 3) & 0b11100000) | (((prgb)[ax+5] >> 3) & 0b00011111); \
        (prgb565)[bx+3] = (((prgb)[ax+3] & 0b11111000) | (((prgb)[ax+4] >> 5) & 0b00000111)); \
    }
#define RGB2RGB565_16(prgb, prgb565, ax, bx) \
    RGB2RGB565_8(prgb, prgb565, ax, bx) \
    RGB2RGB565_8(prgb, prgb565, ax + PIXEL8_RGB, bx + PIXEL8_RGB565);
#define RGB2RGB565_8(prgb, prgb565, ax, bx) \
    RGB2RGB565_4(prgb, prgb565, ax, bx) \
    RGB2RGB565_4(prgb, prgb565, ax + PIXEL4_RGB, bx + PIXEL4_RGB565);
#define RGB2RGB565_4(prgb, prgb565, ax, bx) \
    RGB2RGB565_2(prgb, prgb565, ax, bx) \
    RGB2RGB565_2(prgb, prgb565, ax + PIXEL2_RGB, bx + PIXEL2_RGB565);

/** @brief Convert a frame from RGB888 to RGB565
 * @ingroup frame
 * @param ini RGB888 frame
 * @param out RGB565 frame
 */
uvc_error_t uvc_rgb2rgb565(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_RGB)
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

    uint8_t *prgb = in->data;
    const uint8_t *prgb_end = prgb + in->data_bytes - PIXEL8_RGB;
    uint8_t *prgb565 = out->data;
    const uint8_t *prgb565_end = prgb565 + out->data_bytes - PIXEL8_RGB565;

    // RGB888 to RGB565
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            prgb = in->data + in->step * h;
            prgb565 = out->data + out->step * h;
            for (; (prgb565 <= prgb565_end) && (prgb <= prgb_end) && (w < ww) ;) {
                RGB2RGB565_8(prgb, prgb565, 0, 0);

                prgb += PIXEL8_RGB;
                prgb565 += PIXEL8_RGB565;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgb565 <= prgb565_end) && (prgb <= prgb_end) ;) {
            RGB2RGB565_8(prgb, prgb565, 0, 0);

            prgb += PIXEL8_RGB;
            prgb565 += PIXEL8_RGB565;
        }
    }
#else
    for (; (prgb565 <= prgb565_end) && (prgb <= prgb_end);) {
        RGB2RGB565_8(prgb, prgb565, 0, 0);

        prgb += PIXEL8_RGB;
        prgb565 += PIXEL8_RGB565;
    }
#endif
    return UVC_SUCCESS;
}

//#define YUYV2RGB_2(pyuv, prgb) { \
//    float r = 1.402f * ((pyuv)[3]-128); \
//    float g = -0.34414f * ((pyuv)[1]-128) - 0.71414f * ((pyuv)[3]-128); \
//    float b = 1.772f * ((pyuv)[1]-128); \
//    (prgb)[0] = sat(pyuv[0] + r); \
//    (prgb)[1] = sat(pyuv[0] + g); \
//    (prgb)[2] = sat(pyuv[0] + b); \
//    (prgb)[3] = sat(pyuv[2] + r); \
//    (prgb)[4] = sat(pyuv[2] + g); \
//    (prgb)[5] = sat(pyuv[2] + b); \
//    }
#define IYUYV2RGB_2(pyuv, prgb, ax, bx) { \
        const int d1 = (pyuv)[ax+1]; \
        const int d3 = (pyuv)[ax+3]; \
        const int r = (22987 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
        const int g = (-5636 * (d1/*(pyuv)[ax+1]*/ - 128) - 11698 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
        const int b = (29049 * (d1/*(pyuv)[ax+1]*/ - 128)) >> 14; \
        const int y0 = (pyuv)[ax+0]; \
        (prgb)[bx+0] = sat(y0 + r); \
        (prgb)[bx+1] = sat(y0 + g); \
        (prgb)[bx+2] = sat(y0 + b); \
        const int y2 = (pyuv)[ax+2]; \
        (prgb)[bx+3] = sat(y2 + r); \
        (prgb)[bx+4] = sat(y2 + g); \
        (prgb)[bx+5] = sat(y2 + b); \
    }
#define IYUYV2RGB_16(pyuv, prgb, ax, bx) \
    IYUYV2RGB_8(pyuv, prgb, ax, bx) \
    IYUYV2RGB_8(pyuv, prgb, ax + PIXEL8_YUYV, bx + PIXEL8_RGB)
#define IYUYV2RGB_8(pyuv, prgb, ax, bx) \
    IYUYV2RGB_4(pyuv, prgb, ax, bx) \
    IYUYV2RGB_4(pyuv, prgb, ax + PIXEL4_YUYV, bx + PIXEL4_RGB)
#define IYUYV2RGB_4(pyuv, prgb, ax, bx) \
    IYUYV2RGB_2(pyuv, prgb, ax, bx) \
    IYUYV2RGB_2(pyuv, prgb, ax + PIXEL2_YUYV, bx + PIXEL2_RGB)

/** @brief Convert a frame from YUYV to RGB888
 * @ingroup frame
 *
 * @param in YUYV frame
 * @param out RGB888 frame
 */
uvc_error_t uvc_yuyv2rgb(uvc_frame_t *in, uvc_frame_t *out) {
    if (UNLIKELY(in->frame_format != UVC_FRAME_FORMAT_YUYV))
        return UVC_ERROR_INVALID_PARAM;

    if (UNLIKELY(uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGB) < 0))
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGB;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_RGB;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_YUYV;
    uint8_t *prgb = out->data;
    const uint8_t *prgb_end = prgb + out->data_bytes - PIXEL8_RGB;

#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            prgb = out->data + out->step * h;
            for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
                IYUYV2RGB_8(pyuv, prgb, 0, 0);

                prgb += PIXEL8_RGB;
                pyuv += PIXEL8_YUYV;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) ;) {
            IYUYV2RGB_8(pyuv, prgb, 0, 0);

            prgb += PIXEL8_RGB;
            pyuv += PIXEL8_YUYV;
        }
    }
#else
    // YUYV => RGB888
    for (; (prgb <= prgb_end) && (pyuv <= pyuv_end);) {
        IYUYV2RGB_8(pyuv, prgb, 0, 0);

        prgb += PIXEL8_RGB;
        pyuv += PIXEL8_YUYV;
    }
#endif
    return UVC_SUCCESS;
}

/** @brief Convert a frame from YUYV to RGB565
 * @ingroup frame
 * @param ini YUYV frame
 * @param out RGB565 frame
 */
uvc_error_t uvc_yuyv2rgb565(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_YUYV)
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

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_YUYV;
    uint8_t *prgb565 = out->data;
    const uint8_t *prgb565_end = prgb565 + out->data_bytes - PIXEL8_RGB565;

    uint8_t tmp[PIXEL8_RGB];    // for temporary rgb888 data(8pixel)

#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            prgb565 = out->data + out->step * h;
            for (; (prgb565 <= prgb565_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
                IYUYV2RGB_8(pyuv, tmp, 0, 0);
                RGB2RGB565_8(tmp, prgb565, 0, 0);

                prgb565 += PIXEL8_YUYV;
                pyuv += PIXEL8_RGB565;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgb565 <= prgb565_end) && (pyuv <= pyuv_end) ;) {
            IYUYV2RGB_8(pyuv, tmp, 0, 0);
            RGB2RGB565_8(tmp, prgb565, 0, 0);

            prgb565 += PIXEL8_YUYV;
            pyuv += PIXEL8_RGB565;
        }
    }
#else
    // YUYV => RGB565
    for (; (prgb565 <= prgb565_end) && (pyuv <= pyuv_end);) {
        IYUYV2RGB_8(pyuv, tmp, 0, 0);
        RGB2RGB565_8(tmp, prgb565, 0, 0);

        prgb565 += PIXEL8_YUYV;
        pyuv += PIXEL8_RGB565;
    }
#endif
    return UVC_SUCCESS;
}

#define IYUYV2RGBX_2(pyuv, prgbx, ax, bx) { \
        const int d1 = (pyuv)[ax+1]; \
        const int d3 = (pyuv)[ax+3]; \
        const int r = (22987 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
        const int g = (-5636 * (d1/*(pyuv)[ax+1]*/ - 128) - 11698 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
        const int b = (29049 * (d1/*(pyuv)[ax+1]*/ - 128)) >> 14; \
        const int y0 = (pyuv)[ax+0]; \
        (prgbx)[bx+0] = sat(y0 + r); \
        (prgbx)[bx+1] = sat(y0 + g); \
        (prgbx)[bx+2] = sat(y0 + b); \
        (prgbx)[bx+3] = 0xff; \
        const int y2 = (pyuv)[ax+2]; \
        (prgbx)[bx+4] = sat(y2 + r); \
        (prgbx)[bx+5] = sat(y2 + g); \
        (prgbx)[bx+6] = sat(y2 + b); \
        (prgbx)[bx+7] = 0xff; \
    }
#define IYUYV2RGBX_16(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_8(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_8(pyuv, prgbx, ax + PIXEL8_YUYV, bx + PIXEL8_RGBX);
#define IYUYV2RGBX_8(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_4(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_4(pyuv, prgbx, ax + PIXEL4_YUYV, bx + PIXEL4_RGBX);
#define IYUYV2RGBX_4(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_2(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_2(pyuv, prgbx, ax + PIXEL2_YUYV, bx + PIXEL2_RGBX);

/** @brief Convert a frame from YUYV to RGBX8888
* @ingroup frame
* @param ini YUYV frame
* @param out RGBX8888 frame
*/
uvc_error_t uvc_yuyv2rgbx(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_YUYV)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGBX) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_RGBX;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_YUYV;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // YUYV => RGBX8888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            prgbx = out->data + out->step * h;
            for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
                IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

                prgbx += PIXEL8_RGBX;
                pyuv += PIXEL8_YUYV;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) ;) {
            IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

            prgbx += PIXEL8_RGBX;
            pyuv += PIXEL8_YUYV;
        }
    }
#else
    for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end);) {
        IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

        prgbx += PIXEL8_RGBX;
        pyuv += PIXEL8_YUYV;
    }
#endif
    return UVC_SUCCESS;
}

#define IYUYV2BGR_2(pyuv, pbgr, ax, bx) { \
        const int d1 = (pyuv)[1]; \
        const int d3 = (pyuv)[3]; \
        const int r = (22987 * (d3/*(pyuv)[3]*/ - 128)) >> 14; \
        const int g = (-5636 * (d1/*(pyuv)[1]*/ - 128) - 11698 * (d3/*(pyuv)[3]*/ - 128)) >> 14; \
        const int b = (29049 * (d1/*(pyuv)[1]*/ - 128)) >> 14; \
        const int y0 = (pyuv)[ax+0]; \
        (pbgr)[bx+0] = sat(y0 + b); \
        (pbgr)[bx+1] = sat(y0 + g); \
        (pbgr)[bx+2] = sat(y0 + r); \
        const int y2 = (pyuv)[ax+2]; \
        (pbgr)[bx+3] = sat(y2 + b); \
        (pbgr)[bx+4] = sat(y2 + g); \
        (pbgr)[bx+5] = sat(y2 + r); \
    }
#define IYUYV2BGR_16(pyuv, pbgr, ax, bx) \
    IYUYV2BGR_8(pyuv, pbgr, ax, bx) \
    IYUYV2BGR_8(pyuv, pbgr, ax + PIXEL8_YUYV, bx + PIXEL8_BGR)
#define IYUYV2BGR_8(pyuv, pbgr, ax, bx) \
    IYUYV2BGR_4(pyuv, pbgr, ax, bx) \
    IYUYV2BGR_4(pyuv, pbgr, ax + PIXEL4_YUYV, bx + PIXEL4_BGR)
#define IYUYV2BGR_4(pyuv, pbgr, ax, bx) \
    IYUYV2BGR_2(pyuv, pbgr, ax, bx) \
    IYUYV2BGR_2(pyuv, pbgr, ax + PIXEL2_YUYV, bx + PIXEL2_BGR)

/** @brief Convert a frame from YUYV to BGR888
 * @ingroup frame
 *
 * @param in YUYV frame
 * @param out BGR888 frame
 */
uvc_error_t uvc_yuyv2bgr(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_YUYV)
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

    uint8_t *pyuv = in->data;
    uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_YUYV;
    uint8_t *pbgr = out->data;
    uint8_t *pbgr_end = pbgr + out->data_bytes - PIXEL8_BGR;

    // YUYV => BGR888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            pbgr = out->data + out->step * h;
            for (; (pbgr <= pbgr_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
                IYUYV2BGR_8(pyuv, pbgr, 0, 0);

                pbgr += PIXEL8_BGR;
                pyuv += PIXEL8_YUYV;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (pbgr <= pbgr_end) && (pyuv <= pyuv_end) ;) {
            IYUYV2BGR_8(pyuv, pbgr, 0, 0);

            pbgr += PIXEL8_BGR;
            pyuv += PIXEL8_YUYV;
        }
    }
#else
    for (; (pbgr <= pbgr_end) && (pyuv <= pyuv_end);) {
        IYUYV2BGR_8(pyuv, pbgr, 0, 0);

        pbgr += PIXEL8_BGR;
        pyuv += PIXEL8_YUYV;
    }
#endif
    return UVC_SUCCESS;
}

#define IYUYV2Y(pyuv, py) { \
    (py)[0] = (pyuv[0]); \
    }

/** @brief Convert a frame from YUYV to Y (GRAY8)
 * @ingroup frame
 *
 * @param in YUYV frame
 * @param out GRAY8 frame
 */
uvc_error_t uvc_yuyv2y(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_YUYV)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_GRAY8;
    out->step = in->width;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->capture_time_finished = in->capture_time_finished;
    out->source = in->source;

    uint8_t *pyuv = in->data;
    uint8_t *py = out->data;
    uint8_t *py_end = py + out->data_bytes;

    while (py < py_end) {
        IYUYV2Y(pyuv, py);

        py += 1;
        pyuv += 2;
    }

    return UVC_SUCCESS;
}

#define IYUYV2UV(pyuv, puv) { \
    (puv)[0] = (pyuv[1]); \
    }

/** @brief Convert a frame from YUYV to UV (GRAY8)
 * @ingroup frame
 *
 * @param in YUYV frame
 * @param out GRAY8 frame
 */
uvc_error_t uvc_yuyv2uv(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_YUYV)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_GRAY8;
    out->step = in->width;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->capture_time_finished = in->capture_time_finished;
    out->source = in->source;

    uint8_t *pyuv = in->data;
    uint8_t *puv = out->data;
    uint8_t *puv_end = puv + out->data_bytes;

    while (puv < puv_end) {
        IYUYV2UV(pyuv, puv);

        puv += 1;
        pyuv += 2;
    }

    return UVC_SUCCESS;
}

#define IUYVY2RGB_2(pyuv, prgb, ax, bx) { \
        const int d0 = (pyuv)[ax+0]; \
        const int d2 = (pyuv)[ax+2]; \
        const int r = (22987 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
        const int g = (-5636 * (d0/*(pyuv)[ax+0]*/ - 128) - 11698 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
        const int b = (29049 * (d0/*(pyuv)[ax+0]*/ - 128)) >> 14; \
        const int y1 = (pyuv)[ax+1]; \
        (prgb)[bx+0] = sat(y1 + r); \
        (prgb)[bx+1] = sat(y1 + g); \
        (prgb)[bx+2] = sat(y1 + b); \
        const int y3 = (pyuv)[ax+3]; \
        (prgb)[bx+3] = sat(y3 + r); \
        (prgb)[bx+4] = sat(y3 + g); \
        (prgb)[bx+5] = sat(y3 + b); \
    }
#define IUYVY2RGB_16(pyuv, prgb, ax, bx) \
    IUYVY2RGB_8(pyuv, prgb, ax, bx) \
    IUYVY2RGB_8(pyuv, prgb, ax + PIXEL8_UYVY, bx + PIXEL8_RGB)
#define IUYVY2RGB_8(pyuv, prgb, ax, bx) \
    IUYVY2RGB_4(pyuv, prgb, ax, bx) \
    IUYVY2RGB_4(pyuv, prgb, ax + PIXEL4_UYVY, bx + PIXEL4_RGB)
#define IUYVY2RGB_4(pyuv, prgb, ax, bx) \
    IUYVY2RGB_2(pyuv, prgb, ax, bx) \
    IUYVY2RGB_2(pyuv, prgb, ax + PIXEL2_UYVY, bx + PIXEL2_RGB)

/** @brief Convert a frame from UYVY to RGB888
 * @ingroup frame
 * @param ini UYVY frame
 * @param out RGB888 frame
 */
uvc_error_t uvc_uyvy2rgb(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_UYVY)
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

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_UYVY;
    uint8_t *prgb = out->data;
    const uint8_t *prgb_end = prgb + out->data_bytes - PIXEL8_RGB;

    // UYVY => RGB888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            prgb = out->data + out->step * h;
            for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
                IUYVY2RGB_8(pyuv, prgb, 0, 0);

                prgb += PIXEL8_RGB;
                pyuv += PIXEL8_UYVY;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) ;) {
            IUYVY2RGB_8(pyuv, prgb, 0, 0);

            prgb += PIXEL8_RGB;
            pyuv += PIXEL8_UYVY;
        }
    }
#else
    for (; (prgb <= prgb_end) && (pyuv <= pyuv_end);) {
        IUYVY2RGB_8(pyuv, prgb, 0, 0);

        prgb += PIXEL8_RGB;
        pyuv += PIXEL8_UYVY;
    }
#endif
    return UVC_SUCCESS;
}

/** @brief Convert a frame from UYVY to RGB565
 * @ingroup frame
 * @param ini UYVY frame
 * @param out RGB565 frame
 */
uvc_error_t uvc_uyvy2rgb565(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_UYVY)
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

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_UYVY;
    uint8_t *prgb565 = out->data;
    const uint8_t *prgb565_end = prgb565 + out->data_bytes - PIXEL8_RGB565;

    uint8_t tmp[PIXEL8_RGB];        // for temporary rgb888 data(8pixel)

    // UYVY => RGB565
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            prgb565 = out->data + out->step * h;
            for (; (prgb565 <= prgb565_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
                IUYVY2RGB_8(pyuv, tmp, 0, 0);
                RGB2RGB565_8(tmp, prgb565, 0, 0);

                prgb565 += PIXEL8_RGB565;
                pyuv += PIXEL8_UYVY;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgb565 <= prgb565_end) && (pyuv <= pyuv_end) ;) {
            IUYVY2RGB_8(pyuv, tmp, 0, 0);
            RGB2RGB565_8(tmp, prgb565, 0, 0);

            prgb565 += PIXEL8_RGB565;
            pyuv += PIXEL8_UYVY;
        }
    }
#else
    for (; (prgb565 <= prgb565_end) && (pyuv <= pyuv_end);) {
        IUYVY2RGB_8(pyuv, tmp, 0, 0);
        RGB2RGB565_8(tmp, prgb565, 0, 0);

        prgb565 += PIXEL8_RGB565;
        pyuv += PIXEL8_UYVY;
    }
#endif
    return UVC_SUCCESS;
}

#define IUYVY2RGBX_2(pyuv, prgbx, ax, bx) { \
        const int d0 = (pyuv)[ax+0]; \
        const int d2 = (pyuv)[ax+2]; \
        const int r = (22987 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
        const int g = (-5636 * (d0/*(pyuv)[ax+0]*/ - 128) - 11698 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
        const int b = (29049 * (d0/*(pyuv)[ax+0]*/ - 128)) >> 14; \
        const int y1 = (pyuv)[ax+1]; \
        (prgbx)[bx+0] = sat(y1 + r); \
        (prgbx)[bx+1] = sat(y1 + g); \
        (prgbx)[bx+2] = sat(y1 + b); \
        (prgbx)[bx+3] = 0xff; \
        const int y3 = (pyuv)[ax+3]; \
        (prgbx)[bx+4] = sat(y3 + r); \
        (prgbx)[bx+5] = sat(y3 + g); \
        (prgbx)[bx+6] = sat(y3 + b); \
        (prgbx)[bx+7] = 0xff; \
    }
#define IUYVY2RGBX_16(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_8(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_8(pyuv, prgbx, ax + PIXEL8_UYVY, bx + PIXEL8_RGBX)
#define IUYVY2RGBX_8(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_4(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_4(pyuv, prgbx, ax + PIXEL4_UYVY, bx + PIXEL4_RGBX)
#define IUYVY2RGBX_4(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_2(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_2(pyuv, prgbx, ax + PIXEL2_UYVY, bx + PIXEL2_RGBX)

/** @brief Convert a frame from UYVY to RGBX8888
 * @ingroup frame
 * @param ini UYVY frame
 * @param out RGBX8888 frame
 */
uvc_error_t uvc_uyvy2rgbx(uvc_frame_t *in, uvc_frame_t *out) {
    if (in->frame_format != UVC_FRAME_FORMAT_UYVY)
        return UVC_ERROR_INVALID_PARAM;

    if (uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGBX) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_RGBX;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_UYVY;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // UYVY => RGBX8888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            prgbx = out->data + out->step * h;
            for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
                IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

                prgbx += PIXEL8_RGBX;
                pyuv += PIXEL8_UYVY;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) ;) {
            IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

            prgbx += PIXEL8_RGBX;
            pyuv += PIXEL8_UYVY;
        }
    }
#else
    for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end);) {
        IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

        prgbx += PIXEL8_RGBX;
        pyuv += PIXEL8_UYVY;
    }
#endif
    return UVC_SUCCESS;
}

#define IUYVY2BGR_2(pyuv, pbgr, ax, bx) { \
        const int d0 = (pyuv)[ax+0]; \
        const int d2 = (pyuv)[ax+2]; \
        const int r = (22987 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
        const int g = (-5636 * (d0/*(pyuv)[ax+0]*/ - 128) - 11698 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
        const int b = (29049 * (d0/*(pyuv)[ax+0]*/ - 128)) >> 14; \
        const int y1 = (pyuv)[ax+1]; \
        (pbgr)[bx+0] = sat(y1 + b); \
        (pbgr)[bx+1] = sat(y1 + g); \
        (pbgr)[bx+2] = sat(y1 + r); \
        const int y3 = (pyuv)[ax+3]; \
        (pbgr)[bx+3] = sat(y3 + b); \
        (pbgr)[bx+4] = sat(y3 + g); \
        (pbgr)[bx+5] = sat(y3 + r); \
    }
#define IUYVY2BGR_16(pyuv, pbgr, ax, bx) \
    IUYVY2BGR_8(pyuv, pbgr, ax, bx) \
    IUYVY2BGR_8(pyuv, pbgr, ax + PIXEL8_UYVY, bx + PIXEL8_BGR)
#define IUYVY2BGR_8(pyuv, pbgr, ax, bx) \
    IUYVY2BGR_4(pyuv, pbgr, ax, bx) \
    IUYVY2BGR_4(pyuv, pbgr, ax + PIXEL4_UYVY, bx + PIXEL4_BGR)
#define IUYVY2BGR_4(pyuv, pbgr, ax, bx) \
    IUYVY2BGR_2(pyuv, pbgr, ax, bx) \
    IUYVY2BGR_2(pyuv, pbgr, ax + PIXEL2_UYVY, bx + PIXEL2_BGR)

/** @brief Convert a frame from UYVY to BGR888
 * @ingroup frame
 * @param ini UYVY frame
 * @param out BGR888 frame
 */
uvc_error_t uvc_uyvy2bgr(uvc_frame_t *in, uvc_frame_t *out) {
    if (UNLIKELY(in->frame_format != UVC_FRAME_FORMAT_UYVY))
        return UVC_ERROR_INVALID_PARAM;

    if (UNLIKELY(uvc_ensure_frame_size(out, in->width * in->height * PIXEL_BGR) < 0))
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_BGR;
    if (out->library_owns_data)
        out->step = in->width * PIXEL_BGR;
    out->sequence = in->sequence;
    out->capture_time = in->capture_time;
    out->source = in->source;

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_UYVY;
    uint8_t *pbgr = out->data;
    const uint8_t *pbgr_end = pbgr + out->data_bytes - PIXEL8_BGR;

    // UYVY => BGR888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            pbgr = out->data + out->step * h;
            for (; (pbgr <= pbgr_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
                IUYVY2BGR_8(pyuv, pbgr, 0, 0);

                pbgr += PIXEL8_BGR;
                pyuv += PIXEL8_UYVY;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (pbgr <= pbgr_end) && (pyuv <= pyuv_end) ;) {
            IUYVY2BGR_8(pyuv, pbgr, 0, 0);

            pbgr += PIXEL8_BGR;
            pyuv += PIXEL8_UYVY;
        }
    }
#else
    for (; (pbgr <= pbgr_end) && (pyuv <= pyuv_end);) {
        IUYVY2BGR_8(pyuv, pbgr, 0, 0);

        pbgr += PIXEL8_BGR;
        pyuv += PIXEL8_UYVY;
    }
#endif
    return UVC_SUCCESS;
}

/** @brief Convert a frame to RGB565
 * @ingroup frame
 *
 * @param in non-RGB565 frame
 * @param out RGB565 frame
 */
uvc_error_t uvc_any2rgb565(uvc_frame_t *in, uvc_frame_t *out) {

    switch (in->frame_format) {
#ifdef LIBUVC_HAS_JPEG
        case UVC_FRAME_FORMAT_MJPEG:
            return uvc_mjpeg2rgb565(in, out);
#endif
        case UVC_FRAME_FORMAT_YUYV:
            return uvc_yuyv2rgb565(in, out);
        case UVC_FRAME_FORMAT_UYVY:
            return uvc_uyvy2rgb565(in, out);
        case UVC_FRAME_FORMAT_RGB565:
            return uvc_duplicate_frame(in, out);
        case UVC_FRAME_FORMAT_RGB:
            return uvc_rgb2rgb565(in, out);
        default:
            return UVC_ERROR_NOT_SUPPORTED;
    }
}

/** @brief Convert a frame to RGB888
 * @ingroup frame
 *
 * @param in non-RGB888 frame
 * @param out RGB888 frame
 */
uvc_error_t uvc_any2rgb(uvc_frame_t *in, uvc_frame_t *out) {

    switch (in->frame_format) {
#ifdef LIBUVC_HAS_JPEG
        case UVC_FRAME_FORMAT_MJPEG:
            return uvc_mjpeg2rgb(in, out);
#endif
        case UVC_FRAME_FORMAT_YUYV:
            return uvc_yuyv2rgb(in, out);
        case UVC_FRAME_FORMAT_UYVY:
            return uvc_uyvy2rgb(in, out);
        case UVC_FRAME_FORMAT_RGB:
            return uvc_duplicate_frame(in, out);
        default:
            return UVC_ERROR_NOT_SUPPORTED;
    }
}

/** @brief Convert a frame to BGR888
 * @ingroup frame
 *
 * @param in non-BGR888 frame
 * @param out BGR888 frame
 */
uvc_error_t uvc_any2bgr(uvc_frame_t *in, uvc_frame_t *out) {

    switch (in->frame_format) {
#ifdef LIBUVC_HAS_JPEG
        case UVC_FRAME_FORMAT_MJPEG:
            return uvc_mjpeg2bgr(in, out);
#endif
        case UVC_FRAME_FORMAT_YUYV:
            return uvc_yuyv2bgr(in, out);
        case UVC_FRAME_FORMAT_UYVY:
            return uvc_uyvy2bgr(in, out);
        case UVC_FRAME_FORMAT_BGR:
            return uvc_duplicate_frame(in, out);
        default:
            return UVC_ERROR_NOT_SUPPORTED;
    }
}

/** @brief Convert a frame to RGBX8888
 * @ingroup frame
 *
 * @param in non-rgbx frame
 * @param out rgbx frame
 */
uvc_error_t uvc_any2rgbx(uvc_frame_t *in, uvc_frame_t *out) {

    switch (in->frame_format) {
#ifdef LIBUVC_HAS_JPEG
        case UVC_FRAME_FORMAT_MJPEG:
            return uvc_mjpeg2rgbx(in, out);
#endif
        case UVC_FRAME_FORMAT_YUYV:
            return uvc_yuyv2rgbx(in, out);
        case UVC_FRAME_FORMAT_UYVY:
            return uvc_uyvy2rgbx(in, out);
        case UVC_FRAME_FORMAT_RGBX:
            return uvc_duplicate_frame(in, out);
        case UVC_FRAME_FORMAT_RGB:
            return uvc_rgb2rgbx(in, out);
        default:
            return UVC_ERROR_NOT_SUPPORTED;
    }
}

/** @brief Convert a frame to yuyv
 * @ingroup frame
 *
 * @param in non-yuyv frame
 * @param out yuyv frame
 */
uvc_error_t uvc_any2yuyv(uvc_frame_t *in, uvc_frame_t *out) {

    switch (in->frame_format) {
#ifdef LIBUVC_HAS_JPEG
        case UVC_FRAME_FORMAT_MJPEG:
            return uvc_mjpeg2yuyv(in, out);
#endif
        case UVC_FRAME_FORMAT_YUYV:
            return uvc_duplicate_frame(in, out);
        default:
            return UVC_ERROR_NOT_SUPPORTED;
    }
}

/** @brief Convert a frame to nv12(yuv420SP)
 * 8-bit Y plane followed by an interleaved U/V plane with 2x2 subsampling
 * @ingroup frame
 *
 * @param in yuyv frame (Y0 U0 Y1 V0 Y2 U2 Y3 V2)
 * @param out nv12 frame
 */
uvc_error_t uvc_yuyv2nv12(uvc_frame_t *in, uvc_frame_t *out) {
    ENTER();

    if (UNLIKELY(in->frame_format != UVC_FRAME_FORMAT_YUYV)) RETURN(UVC_ERROR_INVALID_PARAM,
                                                                    uvc_error_t);

    if (UNLIKELY(uvc_ensure_frame_size(out, (in->width * in->height * 3) / 2) < 0)) RETURN(
            UVC_ERROR_NO_MEM, uvc_error_t);

    const uint8_t *src = in->data;
    uint8_t *dest = out->data;
    const int32_t width = in->width;
    const int32_t height = in->height;
    const int32_t src_step = in->step;
    const int32_t src_height = in->height;
    const int32_t dest_width = out->width = in->width;
    const int32_t dest_height = out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_NV12;
    out->step = in->width;
    const int8_t macro_pixel_size = PIXEL_YUYV * 2;

    const uint32_t hh = src_height < dest_height ? src_height : dest_height;
    uint8_t *uv = dest + dest_width * dest_height;
    int h, w;
    for (h = 0; h < hh - 1; h += 2) {
        uint8_t *y0 = dest + width * h;
        uint8_t *y1 = dest + width * (h + 1);
        const uint8_t *yuv = src + src_step * h;
        for (w = 0; w < src_step; w += macro_pixel_size) {
            *(y0++) = yuv[0];    // y
            *(y0++) = yuv[2];    // y'
            *(y1++) = yuv[src_step + 0];    // y on next row
            *(y1++) = yuv[src_step + 2];    // y' on next row
            *(uv++) = yuv[1];    // u
            *(uv++) = yuv[3];    // v
            yuv += macro_pixel_size;    // (1pixel=2bytes)x2pixels=4bytes (Y0 U0 Y1 V0)
        }
    }

    RETURN(UVC_SUCCESS, uvc_error_t);
}

/** @brief Convert a frame to nv12(yuv420SP)
 * 8-bit Y plane followed by an interleaved U/V plane with 2x2 subsampling
 * @ingroup frame
 *
 * @param in non-nv12 frame
 * @param out nv12 frame
 */
uvc_error_t uvc_any2nv12(uvc_frame_t *in, uvc_frame_t *out) {
    uvc_error_t result = UVC_ERROR_NO_MEM;
    uvc_frame_t *yuv = uvc_allocate_frame((in->width * in->height * 3) / 2);
    if (yuv) {
        result = uvc_any2yuyv(in, yuv);
        if (LIKELY(!result)) {
            result = uvc_yuyv2nv12(yuv, out);
        }
        uvc_free_frame(yuv);
    }
    return result;
}

/** @brief Convert a frame to nv21(yuv420SP)
 * As NV12 with U and V reversed in the interleaved plane
 * @ingroup frame
 *
 * @param in yuyv frame (Y0 U0 Y1 V0 Y2 U2 Y3 V2)
 * @param out nv21 frame
 */
uvc_error_t uvc_yuyv2nv21(uvc_frame_t *in, uvc_frame_t *out) {
    ENTER();

    if (UNLIKELY(in->frame_format != UVC_FRAME_FORMAT_YUYV)) RETURN(UVC_ERROR_INVALID_PARAM,
                                                                    uvc_error_t);

    if (UNLIKELY(uvc_ensure_frame_size(out, (in->width * in->height * 3) / 2) < 0)) RETURN(
            UVC_ERROR_NO_MEM, uvc_error_t);

    const uint8_t *src = in->data;
    uint8_t *dest = out->data;
    const int32_t width = in->width;
    const int32_t height = in->height;
    const int32_t src_step = in->step;
    const int32_t src_height = in->height;
    const int32_t dest_width = out->width = in->width;
    const int32_t dest_height = out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_NV21;
    out->step = in->width;
    const int8_t macro_pixel_size = PIXEL_YUYV * 2;

    const uint32_t hh = src_height < dest_height ? src_height : dest_height;
    uint8_t *uv = dest + dest_width * dest_height;
    int h, w;
    for (h = 0; h < hh - 1; h += 2) {
        uint8_t *y0 = dest + width * h;
        uint8_t *y1 = dest + width * (h + 1);
        const uint8_t *yuv = src + src_step * h;
        for (w = 0; w < src_step; w += macro_pixel_size) {
            *(y0++) = yuv[0];    // y
            *(y0++) = yuv[2];    // y'
            *(y1++) = yuv[src_step + 0];    // y on next row
            *(y1++) = yuv[src_step + 2];    // y' on next row
            *(uv++) = yuv[3];    // v
            *(uv++) = yuv[1];    // u
            yuv += macro_pixel_size;    // (1pixel=2bytes)x2pixels=4bytes (Y0 U0 Y1 V0)
        }
    }

    RETURN(UVC_SUCCESS, uvc_error_t);
}

/** @brief Convert a frame to nv21(yuv420SP)
 * As NV12 with U and V reversed in the interleaved plane
 * @ingroup frame
 *
 * @param in non-nv21 frame
 * @param out nv21 frame
 */
uvc_error_t uvc_any2nv21(uvc_frame_t *in, uvc_frame_t *out) {
    uvc_error_t result = UVC_ERROR_NO_MEM;
    uvc_frame_t *yuv = uvc_allocate_frame((in->width * in->height * 3) / 2);
    if (yuv) {
        result = uvc_any2yuyv(in, yuv);
        if (LIKELY(!result)) {
            result = uvc_yuyv2nv21(yuv, out);
        }
        uvc_free_frame(yuv);
    }
    return result;
}