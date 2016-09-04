/*
 * Copyright (C) 2016 Willi Ye
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
package com.grarak.cafntracker.utils;

/**
 * Created by willi on 01.09.16.
 */
public class Log {

    public static void i(String tag, String message) {
        System.out.println("I/" + tag + ": " + message);
    }

    public static void e(String tag, String message) {
        System.out.println("E/" + tag + ": " + message);
    }

}
