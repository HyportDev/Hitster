package com.example.hitster.res

fun String.toText() = Text.Value(this)

fun Int.toText() = Text.Resource(this)