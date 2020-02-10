package com.nexters.doctor24.todoc.data.detailed.response

import com.google.gson.annotations.SerializedName

data class Today(
    @SerializedName("dayType")
    val dayType: String,
    @SerializedName("startTime")
    val startTime: StartTime,
    @SerializedName("endTime")
    val endTime: EndTime
)