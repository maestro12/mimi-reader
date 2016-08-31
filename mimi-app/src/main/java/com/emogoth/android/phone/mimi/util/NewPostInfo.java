/*
 * Copyright (c) 2016. Eli Connelly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.emogoth.android.phone.mimi.util;


public class NewPostInfo {

    private enum Status {
        NONE(0), ERROR(1), SUCCESS(2), SENDING(3);
        private int value;

        Status(final int value) {
            this.value = value;
        }
    }

    private Status postStatus = Status.NONE;
    private String postMessage;

    public Status getStatus() {
        return postStatus;
    }

    public void setStatus(Status postStatus) {
        this.postStatus = postStatus;
    }

    public String getErrorMessage() {
        return postMessage;
    }

    public void setErrorMessage(String postMessage) {
        this.postMessage = postMessage;
    }
}
