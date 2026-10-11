package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

interface LocationSearchRepository {
    suspend fun search(query: String): Result<List<Location>>
}
