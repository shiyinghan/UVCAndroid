#ifndef UVC_CAMERA_CONVERTHELPER_H
#define UVC_CAMERA_CONVERTHELPER_H

#include <libyuv.h>
#include <turbojpeg.h>
#include "libUVCCamera.h"

#define PIXEL_RGB565        2
#define PIXEL_UYVY            2
#define PIXEL_YUYV            2
#define PIXEL_NV21            1.5
#define PIXEL_RGB            3
#define PIXEL_BGR            3
#define PIXEL_RGBX            4

int uvc_mjpeg2rgbx_tj(uvc_frame_t *in, uvc_frame_t *out);

int uvc_mjpeg2rgbx_new(uvc_frame_t *in, uvc_frame_t *out);


int uvc_rgbx_to_yuyv(uvc_frame_t *in, uvc_frame_t *out);

int uvc_rgbx_to_nv12(uvc_frame_t *in, uvc_frame_t *out);

int uvc_rgbx_to_nv21(uvc_frame_t *in, uvc_frame_t *out);

int uvc_rgbx_to_rgb(uvc_frame_t *in, uvc_frame_t *out);

int uvc_rgbx_to_rgb565(uvc_frame_t *in, uvc_frame_t *out);

int uvc_rgbx_to_bgr(uvc_frame_t *in, uvc_frame_t *out);

#endif //UVC_CAMERA_CONVERTHELPER_H
