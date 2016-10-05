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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grarak.cafntracker.utils.Log;
import com.grarak.cafntracker.utils.Utils;

import javax.net.ssl.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by willi on 01.09.16.
 */
public class Tracker {

    private static final String TAG = Tracker.class.getSimpleName();

    private final ArrayList<String> mIds = new ArrayList<>();

    private Tracker(int port, String api, ArrayList<Repo> repos) throws IOException {

        File idsFile = new File("ids.json");
        if (idsFile.exists()) {
            String content = Utils.readFile(idsFile);
            JsonArray array = new JsonParser().parse(content).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                mIds.add(array.get(i).getAsString().trim());
            }
        }

        for (Repo repo : repos) {
            try {
                repo.script_content = Utils.resourceFileReader(
                        getClass().getClassLoader().getResourceAsStream("parsers/" + repo.content_script));
            } catch (IOException ignored) {
                Log.e(TAG, "Failed to read " + repo.content_script);
                return;
            }
        }

        new Thread(() -> {
            while (true) {
                for (Repo repo : repos) {
                    File file = new File("repos/" + repo.name);
                    if (!file.exists()) {
                        Log.i(TAG, "Cloning " + repo.name);
                        try {
                            Utils.executeProcess("git clone " + repo.link + " " + file.getAbsolutePath());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "Failed cloning " + repo.name);
                        }
                    }
                    try {
                        String tags = Utils.executeProcess("git -C " + file.getAbsolutePath() + " tag");
                        repo.tags = Arrays.asList(tags.split("\\r?\\n"));
                        Log.i(TAG, repo.tags.size() + " tags found for " + repo.name);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed getting tags from " + repo.name);
                    }
                    try {
                        Log.i(TAG, "Sleeping");
                        Thread.sleep(10000);

                        Utils.executeProcess("git -C " + file.getAbsolutePath() + " fetch origin");
                        String tags = Utils.executeProcess("git -C " + file.getAbsolutePath() + " tag");

                        List<String> curTags = Arrays.asList(tags.split("\\r?\\n"));
                        List<String> newTags = curTags.stream().filter(tag ->
                                !repo.tags.contains(tag) && !tag.startsWith("android-")).collect(Collectors.toList());

                        for (String newTag : newTags) {
                            File parserScript = new File("parse_script.sh");
                            Utils.writeFile(repo.script_content, parserScript, false);
                            Utils.executeProcess("chmod a+x " + parserScript.getAbsolutePath());
                            String content = Utils.executeProcess(parserScript.getAbsolutePath()
                                    + " " + file.getAbsolutePath() + " " + newTag);

                            for (String id : mIds) {
                                try {
                                    Request.post(id, api, repo.name, content, newTag,
                                            String.format(repo.tag_link, newTag));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.i(TAG, "Failed to send post request to " + id);
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "Failed to update tags for " + repo.name);
                    }
                }
            }
        }).start();

        Log.i(TAG, "Start listening on port " + port);
        SSLServerSocketFactory ssf = loadKeyStore().getServerSocketFactory();
        SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(port);

        while (true) {
            try {
                SSLSocket socket = (SSLSocket) s.accept();
                String address = socket.getRemoteSocketAddress().toString();
                Log.i(TAG, "Connected to " + address);
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                byte[] buffer = new byte[8192];
                dataInputStream.read(buffer);
                String id = new String(buffer).trim();
                Log.i(TAG, id);

                JsonArray repoList = new JsonArray();
                for (Repo repo : repos) {
                    JsonObject repoObject = new JsonObject();
                    repoObject.addProperty("name", repo.name);
                    repoObject.addProperty("content_name", repo.content_name);
                    repoList.add(repoObject);
                }
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.write(repoList.toString().getBytes());

                if (!mIds.contains(id)) {
                    mIds.add(id);

                    JsonArray array = new JsonArray();
                    mIds.forEach(array::add);
                    Utils.writeFile(array.toString(), idsFile, false);
                }
                dataInputStream.close();
                socket.close();
            } catch (IOException ignored) {
                Log.e(TAG, "Failed to connect to client");
            }
        }
    }

    private SSLContext loadKeyStore() throws IOException {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(getClass().getClassLoader().getResourceAsStream("keystore"), "somepass".toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "somepass".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = tmf.getTrustManagers();
            sslContext.init(kmf.getKeyManagers(), trustManagers, null);
            return sslContext;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Couldn't load keystore");
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            showUsage();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) {
            showUsage();
            return;
        }

        String apiKey = args[1];

        String repos;
        try {
            repos = Utils.resourceFileReader(Tracker.class.getClassLoader().getResourceAsStream("repos.json"));
        } catch (IOException ignored) {
            Log.e(TAG, "Couldn't read repos.json form resources");
            return;
        }

        JsonArray repoArray = new JsonParser().parse(repos).getAsJsonArray();
        ArrayList<Repo> repoSet = new ArrayList<>();
        for (int i = 0; i < repoArray.size(); i++) {
            JsonObject repoObject = repoArray.get(i).getAsJsonObject();
            repoSet.add(new Repo(repoObject.get("name").getAsString(), repoObject.get("link").getAsString(),
                    repoObject.get("tag_link").getAsString(), repoObject.get("content_name").getAsString(),
                    repoObject.get("content_parser").getAsString()));
        }

        try {
            new Tracker(port, apiKey, repoSet);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to start server at port " + port);
        }
    }

    private static void showUsage() {
        System.out.println("Usage java -jar tracker.jar [PORT] [GCM_API_KEY]");
    }

}
