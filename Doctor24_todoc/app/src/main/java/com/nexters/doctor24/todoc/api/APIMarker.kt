package com.nexters.doctor24.todoc.api

import com.nexters.doctor24.todoc.data.marker.response.ResMapLocation
import com.nexters.doctor24.todoc.data.marker.response.ResMapMarker
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface  APIMarker {
    /** 이제 안씀 getBounds 방식으로 바꿈 **/
    @Deprecated("instead getBounds")
    @GET("medicals/{type}/latitudes/{latitude}/longitudes/{longitude}/facilities")
    suspend fun getMarkers(
        @Path("type") type: String,
        @Path("latitude") latitude: String,
        @Path("longitude") longitude: String
    ): List<ResMapLocation>

    @GET("medicals/{type}/xlatitudes/{xlatitude}/xlongitudes/{xlongitude}/zlatitudes/{zlatitude}/zlongitudes/{zlongitude}/facilities")
    suspend fun getBounds(
        @Path("type") type: String,
        @Path("xlatitude") xlat: String,
        @Path("xlongitude") xlong: String,
        @Path("zlatitude") zlat: String,
        @Path("zlongitude") zlong: String
    ): List<ResMapLocation>
}