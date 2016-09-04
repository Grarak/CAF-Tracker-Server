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

import java.util.List;

/**
 * Created by willi on 03.09.16.
 */
class Repo {

    String name;
    String link;
    String tag_link;
    String content_name;
    String content_script;

    String script_content;
    List<String> tags;

    Repo(String name, String link, String tag_link, String content_name, String content_script) {
        this.name = name;
        this.link = link;
        this.tag_link = tag_link;
        this.content_name = content_name;
        this.content_script = content_script;
    }

}
