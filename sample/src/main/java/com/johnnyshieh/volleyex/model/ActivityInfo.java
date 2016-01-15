package com.johnnyshieh.volleyex.model;
/*
 * Copyright (C) 2016 Johnny Shieh Open Source Project
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

/**
 * The model save the name and the class of activity.
 *
 * @author Johnny Shieh
 * @version 1.0
 */
public class ActivityInfo {

    public String displayName;

    public Class<?> classInfo;

    public ActivityInfo(String displayName, Class<?> classInfo) {
        this.displayName = displayName;
        this.classInfo = classInfo;
    }
}
