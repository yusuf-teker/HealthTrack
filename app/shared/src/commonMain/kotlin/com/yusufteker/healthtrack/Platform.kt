package com.yusufteker.healthtrack

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform