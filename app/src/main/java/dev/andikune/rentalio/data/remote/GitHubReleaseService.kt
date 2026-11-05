package dev.andikune.rentalio.data.remote

import dev.andikune.rentalio.data.model.GitHubRelease
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface GitHubReleaseService {
    @GET("repos/andikune-ux/rental-io/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease

    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        fun create(): GitHubReleaseService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(GitHubReleaseService::class.java)
        }
    }
}
