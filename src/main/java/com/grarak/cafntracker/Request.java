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
package com.grarak.cafntracker;

import com.google.gson.JsonObject;
import com.grarak.cafntracker.utils.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by willi on 04.09.16.
 */
class Request {

    private static final String TAG = Request.class.getSimpleName();

    static void post(String to, String api, String name, String title, String message, String link) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("operation", "create");
        json.addProperty("to", to);

        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("title", title);
        data.addProperty("message", message);
        data.addProperty("link", link);

        json.add("data", data);

        HttpURLConnection conn = (HttpURLConnection) new URL("https://fcm.googleapis.com/fcm/send").openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setFixedLengthStreamingMode(json.toString().getBytes().length);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "key=" + api);

        OutputStream out = conn.getOutputStream();
        out.write(json.toString().getBytes());
        out.flush();
        out.close();

        InputStream inputStream = conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream();
        String response = getString(inputStream);
        inputStream.close();

        Log.i(TAG, "Response: " + response);

        conn.disconnect();
    }

    private static String getString(InputStream stream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append('\n');
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }

}
