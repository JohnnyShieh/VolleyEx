/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley.request;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.image.ImageScheme;
import com.android.volley.toolbox.ContentLengthInputStream;
import com.android.volley.utils.IOUtils;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.ImageView.ScaleType;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A canned request for getting an image at a given URL and calling
 * back with a decoded Bitmap.
 */
public class ImageRequest extends Request<Bitmap> {
    /** Socket timeout in milliseconds for image requests */
    private static final int IMAGE_TIMEOUT_MS = 1000;

    /** Default number of retries for image requests */
    private static final int IMAGE_MAX_RETRIES = 2;

    /** Default backoff multiplier for image requests */
    private static final float IMAGE_BACKOFF_MULT = 2f;

    protected static final int BUFFER_SIZE = 32 * 1024; // 32 Kb

    protected static final String CONTENT_CONTACTS_URI_PREFIX = "content://com.android.contacts/";

    private static final String ERROR_UNSUPPORTED_SCHEME = "UIL doesn't support scheme(protocol) by default [%s]. "
        + "You should implement this support yourself (BaseImageDownloader.getStreamFromOtherSource(...))";

    private final Response.Listener<Bitmap> mListener;
    private final Config mDecodeConfig;
    private final int mMaxWidth;
    private final int mMaxHeight;
    private ScaleType mScaleType;

    /** Decoding lock so that we don't decode more than one image at a time (to avoid OOM's) */
    private static final Object sDecodeLock = new Object();

    /**
     * Creates a new image request, decoding to a maximum specified width and
     * height. If both width and height are zero, the image will be decoded to
     * its natural size. If one of the two is nonzero, that dimension will be
     * clamped and the other one will be set to preserve the image's aspect
     * ratio. If both width and height are nonzero, the image will be decoded to
     * be fit in the rectangle of dimensions width x height while keeping its
     * aspect ratio.
     *
     * @param url URL of the image
     * @param listener Listener to receive the decoded bitmap
     * @param maxWidth Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight Maximum height to decode this bitmap to, or zero for
     *            none
     * @param scaleType The ImageViews ScaleType used to calculate the needed image size.
     * @param decodeConfig Format to decode the bitmap to
     * @param errorListener Error listener, or null to ignore errors
     */
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight,
            ScaleType scaleType, Config decodeConfig, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener); 
        setRetryPolicy(
                new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
        mListener = listener;
        mDecodeConfig = decodeConfig;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mScaleType = scaleType;
    }

    /**
     * For API compatibility with the pre-ScaleType variant of the constructor. Equivalent to
     * the normal constructor with {@code ScaleType.CENTER_INSIDE}.
     */
    @Deprecated
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight,
            Config decodeConfig, Response.ErrorListener errorListener) {
        this(url, listener, maxWidth, maxHeight,
                ScaleType.CENTER_INSIDE, decodeConfig, errorListener);
    }
    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary Maximum size of the primary dimension (i.e. width for
     *        max width), or zero to maintain aspect ratio with secondary
     *        dimension
     * @param maxSecondary Maximum size of the secondary dimension, or zero to
     *        maintain aspect ratio with primary dimension
     * @param actualPrimary Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     * @param scaleType The ScaleType used to calculate the needed image size.
     */
    private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
            int actualSecondary, ScaleType scaleType) {

        // If no dominant value at all, just return the actual.
        if ((maxPrimary == 0) && (maxSecondary == 0)) {
            return actualPrimary;
        }

        // If ScaleType.FIT_XY fill the whole rectangle, ignore ratio.
        if (scaleType == ScaleType.FIT_XY) {
            if (maxPrimary == 0) {
                return actualPrimary;
            }
            return maxPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;

        // If ScaleType.CENTER_CROP fill the whole rectangle, preserve aspect ratio.
        if (scaleType == ScaleType.CENTER_CROP) {
            if ((resized * ratio) < maxSecondary) {
                resized = (int) (maxSecondary / ratio);
            }
            return resized;
        }

        if ((resized * ratio) > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

    /*@Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        // Serialize all decode on a global lock to reduce concurrent heap usage.
        synchronized (sDecodeLock) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }*/

    /*
    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     */
    /*private Response<Bitmap> doParse(NetworkResponse response) {
        byte[] data = response.data;
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        if (mMaxWidth == 0 && mMaxHeight == 0) {
            decodeOptions.inPreferredConfig = mDecodeConfig;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        } else {
            // If we have to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight,
                actualWidth, actualHeight);
            int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth,
                actualHeight, actualWidth);

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;
            // TODO(ficus): Do we need this or is it okay since API 8 doesn't support it?
            // decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
            decodeOptions.inSampleSize =
                findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
            Bitmap tempBitmap =
                BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

            // If necessary, scale down to the maximal acceptable size.
            if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth ||
                tempBitmap.getHeight() > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap,
                    desiredWidth, desiredHeight, true);
                tempBitmap.recycle();
            } else {
                bitmap = tempBitmap;
            }
        }

        if (bitmap == null) {
            return Response.error(new ParseError(response));
        } else {
            return Response.success(bitmap, HttpHeaderParser.parseCacheHeaders(response));
        }
    }*/

    // modified by Johnny Shieh : JohnnyShieh17@gamil.com
    @Override
    protected Bitmap parseResponseData(NetworkResponse response) throws Exception {
        // Serialize all decode on a global lock to reduce concurrent heap usage.
        synchronized (sDecodeLock) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                throw e;
            }
        }
    }

    protected InputStream getImageStream(NetworkResponse response) throws IOException {
        InputStream inputStream;
        String imageUri = getUrl();
        switch (ImageScheme.ofUri(imageUri)) {
            case HTTP:
            case HTTPS:
                inputStream = getStreamFromResponse(response);
                break;
            case FILE:
                inputStream = getStreamFromFile(imageUri);
                break;
            case CONTENT:
                inputStream = getStreamFromContent(imageUri);
                break;
            case ASSETS:
                inputStream = getStreamFromAssets(imageUri);
                break;
            case DRAWABLE:
                inputStream = getStreamFromDrawable(imageUri);
                break;
            case UNKNOWN:
            default:
                inputStream = getStreamFromOtherSource(imageUri);
        }
        return inputStream;
    }

    /**
     * Parse {@link InputStream} of image to the decoded bitmap.
     */
    private Bitmap doParse(NetworkResponse response) throws IOException {
        InputStream imageStream = getImageStream(response);
        if(null == imageStream) {
            VolleyLog.e("No stream for image, url=%s", getUrl());
            return null;
        }
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        try {
            if (mMaxWidth == 0 && mMaxHeight == 0) {
                decodeOptions.inPreferredConfig = mDecodeConfig;
                bitmap = BitmapFactory.decodeStream(imageStream, null, decodeOptions);
            } else {
                // If we have to resize this image, first get the natural bounds.
                decodeOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(imageStream, null, decodeOptions);
                int actualWidth = decodeOptions.outWidth;
                int actualHeight = decodeOptions.outHeight;

                // Then compute the dimensions we would ideally like to decode to.
                int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight,
                        actualWidth, actualHeight, mScaleType);
                int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth,
                        actualHeight, actualWidth, mScaleType);

                // reset the image stream.
                if (imageStream.markSupported()) {
                    try {
                        imageStream.reset();
                    } catch (IOException ignored) {}
                }else {
                    IOUtils.closeSilently(imageStream);
                    imageStream = getImageStream(response);
                }

                // Decode to the nearest power of two scaling factor.
                decodeOptions.inJustDecodeBounds = false;
                // TODO(ficus): Do we need this or is it okay since API 8 doesn't support it?
                // decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
                decodeOptions.inSampleSize =
                    findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
                Bitmap tempBitmap = BitmapFactory.decodeStream(imageStream, null, decodeOptions);

                // If necessary, scale down to the maximal acceptable size.
                if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth ||
                        tempBitmap.getHeight() > desiredHeight)) {
                    bitmap = Bitmap.createScaledBitmap(tempBitmap,
                            desiredWidth, desiredHeight, true);
                    tempBitmap.recycle();
                } else {
                    bitmap = tempBitmap;
                }
            }
        }finally {
            IOUtils.closeSilently(imageStream);
        }

        return bitmap;
    }

    /**
     * Retrieves {@link InputStream} of image from parseNetworkResponse.
     *
     * @param response The parseNetworkResponse.
     * @return {@link InputStream} of image
     */
    protected InputStream getStreamFromResponse(NetworkResponse response) {
        if(null == response.data || response.data.length == 0) {
            return null;
        }
        return new ByteArrayInputStream(response.data);
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is located on the local file system or SD card).
     *
     * @param imageUri Image URI
     * @return {@link InputStream} of image
     * @throws IOException if some I/O error occurs reading from file system
     */
    protected InputStream getStreamFromFile(String imageUri) throws IOException {
        String filePath = ImageScheme.FILE.crop(imageUri);
        if (isVideoFileUri(imageUri)) {
            return getVideoThumbnailStream(filePath);
        } else {
            BufferedInputStream imageStream = new BufferedInputStream(new FileInputStream(filePath), BUFFER_SIZE);
            return new ContentLengthInputStream(imageStream, (int) new File(filePath).length());
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private InputStream getVideoThumbnailStream(String filePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            Bitmap bitmap = ThumbnailUtils
                .createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
            if (bitmap != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                return new ByteArrayInputStream(bos.toByteArray());
            }
        }
        return null;
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is accessed using {@link ContentResolver}).
     *
     * @param imageUri Image URI
     * @return {@link InputStream} of image
     * @throws FileNotFoundException if the provided URI could not be opened
     */
    protected InputStream getStreamFromContent(String imageUri) throws FileNotFoundException {
        ContentResolver res = getContext().getContentResolver();

        Uri uri = Uri.parse(imageUri);
        if (isVideoContentUri(uri)) { // video thumbnail
            Long origId = Long.valueOf(uri.getLastPathSegment());
            Bitmap bitmap = MediaStore.Video.Thumbnails
                .getThumbnail(res, origId, MediaStore.Images.Thumbnails.MINI_KIND, null);
            if (bitmap != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                return new ByteArrayInputStream(bos.toByteArray());
            }
        } else if (imageUri.startsWith(CONTENT_CONTACTS_URI_PREFIX)) { // contacts photo
            return getContactPhotoStream(uri);
        }

        return res.openInputStream(uri);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected InputStream getContactPhotoStream(Uri uri) {
        ContentResolver res = getContext().getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return ContactsContract.Contacts.openContactPhotoInputStream(res, uri, true);
        } else {
            return ContactsContract.Contacts.openContactPhotoInputStream(res, uri);
        }
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is located in assets of application).
     *
     * @param imageUri Image URI
     * @return {@link InputStream} of image
     * @throws IOException if some I/O error occurs file reading
     */
    protected InputStream getStreamFromAssets(String imageUri) throws IOException {
        String filePath = ImageScheme.ASSETS.crop(imageUri);
        return getContext().getAssets().open(filePath);
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is located in drawable resources of application).
     *
     * @param imageUri Image URI
     * @return {@link InputStream} of image
     */
    protected InputStream getStreamFromDrawable(String imageUri) {
        String drawableIdString = ImageScheme.DRAWABLE.crop(imageUri);
        int drawableId = Integer.parseInt(drawableIdString);
        return getContext().getResources().openRawResource(drawableId);
    }

    /**
     * Retrieves {@link InputStream} of image by URI from other source with unsupported scheme. Should be overriden by
     * successors to implement image downloading from special sources.<br />
     * This method is called only if image URI has unsupported scheme. Throws {@link UnsupportedOperationException} by
     * default.
     *
     * @param imageUri Image URI
     * @return {@link InputStream} of image
     * @throws IOException                   if some I/O error occurs
     * @throws UnsupportedOperationException if image URI has unsupported scheme(protocol)
     */
    protected InputStream getStreamFromOtherSource(String imageUri) throws IOException {
        throw new UnsupportedOperationException(String.format(ERROR_UNSUPPORTED_SCHEME, imageUri));
    }

    private boolean isVideoContentUri(Uri uri) {
        String mimeType = getContext().getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("video/");
    }

    private boolean isVideoFileUri(String uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null && mimeType.startsWith("video/");
    }
    // modified end

    @Override
    protected void deliverResponse(Bitmap response) {
        mListener.onResponse(getOriginUrl(), response);
    }

    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth Actual width of the bitmap
     * @param actualHeight Actual height of the bitmap
     * @param desiredWidth Desired width of the bitmap
     * @param desiredHeight Desired height of the bitmap
     */
    // Visible for testing.
    static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }

        return (int) n;
    }
}
