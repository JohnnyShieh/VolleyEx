package com.android.volley.cache;
/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.android.volley.Cache;
import com.android.volley.VolleyLog;
import com.android.volley.disklrucache.DiskLruCache;
import com.android.volley.utils.MD5Utils;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Johnny Shieh
 * @date: 2015-11-27
 *
 * Cache implementation that caches files directly onto the hard disk in the specified
 * directory. And this is base on DiskLruCache.
 */
public class DiskLruBasedCache implements Cache {

    /** The root directory to use for the cache. */
    private final File mRootDirectory;

    /** The maximum size of the cache in bytes. */
    private final int mMaxCacheSizeInBytes;

    /** The App version, if this changes all the cache will clear. */
    private final int mAppVersion;

    /** Default maximum disk usage in bytes. */
    private static final int DEFAULT_DISK_USAGE_BYTES = 10 * 1024 * 1024;

    /** Default number cache corresponding to one cache key. */
    private static final int DEFAULT_KEY_VALUE_COUNT = 1;

    /** Default index of the DiskLruCache.Editor.newInputStream method. */
    private static final int DEFAULT_DISK_VALUE_INDEX = 0;

    /** Magic number for current version of cache file format. */
    private static final int CACHE_MAGIC = 0x20150306;

    /** The Disk Cache store data on specified directory. */
    private DiskLruCache mDiskLruCache;

    /**
     * Constructs an instance of the DiskBasedCache at the specified directory.
     * @param rootDirectory The root directory of the cache.
     * @param maxCacheSizeInBytes The maximum size of the cache in bytes.
     */
    public DiskLruBasedCache(File rootDirectory, int maxCacheSizeInBytes, int appVersion) {
        mRootDirectory = rootDirectory;
        mMaxCacheSizeInBytes = maxCacheSizeInBytes;
        mAppVersion = appVersion;
    }

    /**
     * Constructs an instance of the DiskBasedCache at the specified directory using
     * the default maximum cache size of 10MB.
     * @param rootDirectory The root directory of the cache.
     */
    public DiskLruBasedCache(File rootDirectory, int appVersion) {
        this(rootDirectory, DEFAULT_DISK_USAGE_BYTES, appVersion);
    }

    /**
     * Initializes the DiskLruBasedCache for the specified directory.
     * Creates the root directory if necessary.
     */
    @Override
    public synchronized void initialize() {
        if (!mRootDirectory.exists()) {
            if (!mRootDirectory.mkdirs()) {
                VolleyLog.e("Unable to create cache dir %s", mRootDirectory.getAbsolutePath());
                return;
            }
        }

        try {
            mDiskLruCache = DiskLruCache.open(mRootDirectory, mAppVersion, DEFAULT_KEY_VALUE_COUNT, mMaxCacheSizeInBytes);
        }catch (IOException e) {
            VolleyLog.e(e, "Unable to create DiskLruCache.");
        }
    }

    /**
     * Invalidates an entry in the cache.
     * @param key Cache key
     * @param fullExpire True to fully expire the entry, false to soft expire
     */
    @Override
    public synchronized void invalidate(String key, boolean fullExpire) {
        checkNotClosed();
        Entry entry = get(key);
        if (entry != null) {
            entry.softTtl = 0;
            if (fullExpire) {
                entry.ttl = 0;
            }
            put(key, entry);
        }
    }

    /**
     * Returns the cache entry with the specified key if it exists, null otherwise.
     */
    @Override
    public synchronized Entry get(String key) {
        checkNotClosed();
        String hashkey = MD5Utils.getMD5(key.getBytes());

        InputStream is = null;
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(hashkey);
            if(null != snapshot) {
                is = snapshot.getInputStream(DEFAULT_DISK_VALUE_INDEX);
                CacheHeader header = CacheHeader.readHeader(is);
                byte[] data = streamToBytes(is, (int) header.size);
                return header.toCacheEntry(data);
            }
        } catch (IOException e) {
            VolleyLog.d("Failed to get entry for key %s", key);
            remove(key);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(null != is) {
                    is.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Puts the entry with the specified key into the cache.
     */
    @Override
    public synchronized void put(String key, Entry entry) {
        checkNotClosed();
        String hashkey = MD5Utils.getMD5(key.getBytes());

        OutputStream os = null;
        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskLruCache.edit(hashkey);
            if(null != editor) {
                os = editor.newOutputStream(DEFAULT_DISK_VALUE_INDEX);
                CacheHeader header = new CacheHeader(hashkey, entry);
                boolean success = header.writeHeader(os);
                if (!success) {
                    os.close();
                    VolleyLog.d("Failed to write header for key %s", key);
                    throw new IOException();
                }
                os.write(entry.data);
                editor.commit();
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(null != editor) {
                    editor.abortUnlessCommitted();
                }
                if(null != os) {
                    os.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    /**
     * Removes the specified key from the cache if it exists.
     */
    @Override
    public synchronized void remove(String key) {
        checkNotClosed();
        String hashkey = MD5Utils.getMD5(key.getBytes());
        boolean deleted = false;
        try {
            deleted = mDiskLruCache.remove(hashkey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!deleted) {
            VolleyLog.d("Could not delete cache entry for key=%s", key);
        }
    }

    /**
     * Clears the cache. Deletes all cached files from disk.
     */
    @Override
    public synchronized void clear() {
        checkNotClosed();
        try {
            mDiskLruCache.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initialize();
        VolleyLog.d("Cache cleared.");
    }

    /** Check whether cache is closed or not. */
    private void checkNotClosed() {
        if(null == mDiskLruCache || mDiskLruCache.isClosed()) {
            throw new IllegalStateException("disk lru cache is null or closed");
        }
    }

    /**
     * Returns the number of bytes currently being used to store the values in
     * this cache. This may be greater than the max size if a background
     * deletion is pending.
     */
    @Override
    public synchronized long size() {
        checkNotClosed();
        return mDiskLruCache.size();
    }

    /**
     * flush DiskLruCache. Note that this includes
     * disk access so this should not be executed on the main/UI thread.
     */
    @Override
    public synchronized void flush() {
        checkNotClosed();
        try {
            mDiskLruCache.flush();
            VolleyLog.d("Cache flushed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close DiskLruCache. Note that this includes
     * disk access so this should not be executed on the main/UI thread.
     */
    @Override
    public synchronized void close() {
        if(null == mDiskLruCache || mDiskLruCache.isClosed()) {
            return;
        }
        try {
            mDiskLruCache.close();
            mDiskLruCache = null;
            VolleyLog.d("Cache flushed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the contents of an InputStream into a byte[].
     * */
    private static byte[] streamToBytes(InputStream in, int length) throws IOException {
        byte[] bytes = new byte[length];
        int count;
        int pos = 0;
        while (pos < length && ((count = in.read(bytes, pos, length - pos)) != -1)) {
            pos += count;
        }
        if (pos != length) {
            throw new IOException("Expected " + length + " bytes, read " + pos + " bytes");
        }
        return bytes;
    }

    /**
     * Handles holding onto the cache headers for an entry.
     */
    // Visible for testing.
    static class CacheHeader {
        /** The size of the data identified by this CacheHeader. */
        public long size;

        /** The key that identifies the cache entry. */
        public String key;

        /** ETag for cache coherence. */
        public String etag;

        /** Date of this response as reported by the server. */
        public long serverDate;

        /** The last modified date for the requested object. */
        public long lastModified;

        /** TTL for this record. */
        public long ttl;

        /** Soft TTL for this record. */
        public long softTtl;

        /** Headers from the response resulting in this cache entry. */
        public Map<String, String> responseHeaders;

        private CacheHeader() { }

        /**
         * Instantiates a new CacheHeader object
         * @param key The key that identifies the cache entry
         * @param entry The cache entry.
         */
        public CacheHeader(String key, Entry entry) {
            this.key = key;
            this.size = entry.data.length;
            this.etag = entry.etag;
            this.serverDate = entry.serverDate;
            this.lastModified = entry.lastModified;
            this.ttl = entry.ttl;
            this.softTtl = entry.softTtl;
            this.responseHeaders = entry.responseHeaders;
        }

        /**
         * Reads the header off of an InputStream and returns a CacheHeader object.
         * @param is The InputStream to read from.
         * @throws IOException
         */
        public static CacheHeader readHeader(InputStream is) throws IOException {
            CacheHeader entry = new CacheHeader();
            int magic = readInt(is);
            if (magic != CACHE_MAGIC) {
                // don't bother deleting, it'll get pruned eventually
                throw new IOException();
            }
            entry.size = readLong(is);
            entry.key = readString(is);
            entry.etag = readString(is);
            if (entry.etag.equals("")) {
                entry.etag = null;
            }
            entry.serverDate = readLong(is);
            entry.lastModified = readLong(is);
            entry.ttl = readLong(is);
            entry.softTtl = readLong(is);
            entry.responseHeaders = readStringStringMap(is);

            return entry;
        }

        /**
         * Creates a cache entry for the specified data.
         */
        public Entry toCacheEntry(byte[] data) {
            Entry e = new Entry();
            e.data = data;
            e.etag = etag;
            e.serverDate = serverDate;
            e.lastModified = lastModified;
            e.ttl = ttl;
            e.softTtl = softTtl;
            e.responseHeaders = responseHeaders;
            return e;
        }


        /**
         * Writes the contents of this CacheHeader to the specified OutputStream.
         */
        public boolean writeHeader(OutputStream os) {
            try {
                writeInt(os, CACHE_MAGIC);
                writeLong(os, size);
                writeString(os, key);
                writeString(os, etag == null ? "" : etag);
                writeLong(os, serverDate);
                writeLong(os, lastModified);
                writeLong(os, ttl);
                writeLong(os, softTtl);
                writeStringStringMap(responseHeaders, os);
                os.flush();
                return true;
            } catch (IOException e) {
                VolleyLog.d("%s", e.toString());
                return false;
            }
        }

    }

    /**
     * Simple wrapper around {@link InputStream#read()} that throws EOFException
     * instead of returning -1.
     */
    private static int read(InputStream is) throws IOException {
        int b = is.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    static void writeInt(OutputStream os, int n) throws IOException {
        os.write((n >> 0) & 0xff);
        os.write((n >> 8) & 0xff);
        os.write((n >> 16) & 0xff);
        os.write((n >> 24) & 0xff);
    }

    static int readInt(InputStream is) throws IOException {
        int n = 0;
        n |= (read(is) << 0);
        n |= (read(is) << 8);
        n |= (read(is) << 16);
        n |= (read(is) << 24);
        return n;
    }

    static void writeLong(OutputStream os, long n) throws IOException {
        os.write((byte)(n >>> 0));
        os.write((byte)(n >>> 8));
        os.write((byte)(n >>> 16));
        os.write((byte)(n >>> 24));
        os.write((byte)(n >>> 32));
        os.write((byte)(n >>> 40));
        os.write((byte)(n >>> 48));
        os.write((byte)(n >>> 56));
    }

    static long readLong(InputStream is) throws IOException {
        long n = 0;
        n |= ((read(is) & 0xFFL) << 0);
        n |= ((read(is) & 0xFFL) << 8);
        n |= ((read(is) & 0xFFL) << 16);
        n |= ((read(is) & 0xFFL) << 24);
        n |= ((read(is) & 0xFFL) << 32);
        n |= ((read(is) & 0xFFL) << 40);
        n |= ((read(is) & 0xFFL) << 48);
        n |= ((read(is) & 0xFFL) << 56);
        return n;
    }

    static void writeString(OutputStream os, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        writeLong(os, b.length);
        os.write(b, 0, b.length);
    }

    static String readString(InputStream is) throws IOException {
        int n = (int) readLong(is);
        byte[] b = streamToBytes(is, n);
        return new String(b, "UTF-8");
    }

    static void writeStringStringMap(Map<String, String> map, OutputStream os) throws IOException {
        if (map != null) {
            writeInt(os, map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writeString(os, entry.getKey());
                writeString(os, entry.getValue());
            }
        } else {
            writeInt(os, 0);
        }
    }

    static Map<String, String> readStringStringMap(InputStream is) throws IOException {
        int size = readInt(is);
        Map<String, String> result = (size == 0)
            ? Collections.<String, String>emptyMap()
            : new HashMap<String, String>(size);
        for (int i = 0; i < size; i++) {
            String key = readString(is).intern();
            String value = readString(is).intern();
            result.put(key, value);
        }
        return result;
    }
}
