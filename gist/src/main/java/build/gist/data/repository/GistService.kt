package build.gist.data.repository

import retrofit2.http.GET
import build.gist.data.model.Configuration

interface GistService {
    @GET("/api/v1/configuration")
    suspend fun fetchConfiguration(): Configuration
}