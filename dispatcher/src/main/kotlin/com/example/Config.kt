package com.example

data class ConnectionStrings(val mongodb: String)
data class Config(val connectionStrings: ConnectionStrings)