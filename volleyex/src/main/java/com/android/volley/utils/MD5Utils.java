package com.android.volley.utils;
/*
 * Copyright (C) 2015 Johnny Shieh
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

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author: Johnny Shieh
 * @date: 2015-06-12
 * @description: The utils class used to generate md5 value
 */
public class MD5Utils {

    // the default hex digits
    private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static ThreadLocal<MessageDigest> MD5 = new ThreadLocal<MessageDigest>(){
        @Override
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("MessageDigest not support MD5 algorithm");
            }
        }
    };

    public static byte[] MD5Encode(byte[] bytes) {
        if(null == bytes) {
            return null;
        }
        try {
            MessageDigest md = MD5.get();
            md.update(bytes);
            return md.digest();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * convert 128 md5 byte array to hex digits
     * @return
     */
    private static String byteArrayToHex(byte[] bytes){
        char[] hexs = new char[bytes.length * 2];
        for(int i = 0; i < bytes.length; i ++) {
            char c0 = hexDigits[bytes[i] & 0xf0 >> 4];
            char c1 = hexDigits[bytes[i] & 0x0f];
            hexs[i * 2] = c0;
            hexs[i * 2 + 1] = c1;
        }
        return new String(hexs);
    }

    public static String getMD5(byte[] bytes) {
        if(null == bytes) {
            return null;
        }
        return byteArrayToHex(MD5Encode(bytes));
    }

    public static String getMD5(String str, String encoding) throws UnsupportedEncodingException {
        if(TextUtils.isEmpty(str)) {
            return null;
        }
        if(null == encoding) {
            return getMD5(str.getBytes());
        }else {
            return getMD5(str.getBytes(encoding));
        }
    }
}
