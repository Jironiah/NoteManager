// ApiResponse.kt
package com.example.jinotas.api.notesnocodb

data class ApiResponse(
    val list: List<ApiNote>,
    val pageInfo: PageInfo,
    val stats: Stats
)

data class PageInfo(
    val totalRows: Int,
    val page: Int,
    val pageSize: Int,
    val isFirstPage: Boolean,
    val isLastPage: Boolean
)

data class Stats(
    val dbQueryTime: String
)