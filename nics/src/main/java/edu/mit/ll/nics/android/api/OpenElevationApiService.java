package edu.mit.ll.nics.android.api;

import edu.mit.ll.nics.android.data.messages.OpenElevationMessage;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface OpenElevationApiService {

    @Headers({"Accept: application/json"})
    @GET("lookup")
    Call<OpenElevationMessage> getElevation(@Query(value = "locations", encoded = true) String location);
}
