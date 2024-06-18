/*
 * Copyright (c) 2008-2021, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.nics.android.api;

import edu.mit.ll.nics.android.data.Presence;
import edu.mit.ll.nics.android.data.messages.ChatMessage;
import edu.mit.ll.nics.android.data.messages.DeleteChatMessage;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatApiService {

    @Headers({"Accept: application/json"})
    @GET("chatmsgs/{collabroomId}?sortOrder=desc&dateColumn=created")
    Call<ChatMessage> getChats(@Path(value = "collabroomId", encoded = true) long collabroomId,
                               @Query(value = "fromDate", encoded = true) long fromDate);

    @Headers({"Accept: application/json", "Content-Type: application/json"})
    @POST("chatmsgs/{collabroomId}")
    Call<ChatMessage> postChat(@Path(value = "collabroomId", encoded = true) long collabroomId,
                               @Body RequestBody chat);

    @Headers({"Accept: application/json", "Content-Type: application/json"})
    @POST("collabroom/{incidentId}/{collabroomId}")
    Call<ResponseBody> postChatPresence(@Path(value = "incidentId", encoded = true) long incidentId,
                                        @Path(value = "collabroomId", encoded = true) long collabroomId,
                                        @Body Presence presence);
    @Headers({"Accept: application/json", "Content-Type: application/json"})
    @DELETE("chatmsgs/{collabroomId}/chat/{chatMsgId}/userorg/{requestingUserOrgId}/incident/{incidentId}/username/{chatUsername}")
    Call<DeleteChatMessage> deleteChat(@Path(value = "collabroomId", encoded = true) long collabroomId,
                                       @Path(value = "chatMsgId", encoded = true) long chatMsgId,
                                       @Path(value = "requestingUserOrgId", encoded = true) long userOrgId,
                                       @Path(value = "incidentId", encoded = true) long incidentId,
                                       @Path(value = "chatUsername", encoded = true) String username);
}