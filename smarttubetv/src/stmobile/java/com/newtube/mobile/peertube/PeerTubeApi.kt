package com.newtube.mobile.peertube

import com.google.gson.JsonElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.Locale

data class PeerTubeVideoPage(val data: List<PeerTubeVideo> = emptyList(), val total: Int = 0)
data class PeerTubeInstancePage(val data: List<PeerTubeInstance> = emptyList(), val total: Int = 0)
data class PeerTubeInstance(val host: String = "", val name: String? = null,
                            val totalVideos: Long? = null, val country: String? = null)
data class PeerSeekPage(val videos: List<PeerSeekVideo> = emptyList(), val total: Int = 0)
data class PeerTubeFile(val fileUrl: String? = null, val resolution: PeerTubeResolution? = null)
data class PeerTubeResolution(val id: Int = 0, val label: String? = null)
data class PeerTubePlaylist(val playlistUrl: String? = null, val files: List<PeerTubeFile>? = null)

data class PeerTubeVideo(
    val uuid: String = "", val name: String = "", val description: String? = null,
    val publishedAt: String? = null, val thumbnailPath: String? = null,
    val thumbnailUrl: String? = null, val previewPath: String? = null,
    val views: Long = 0, val duration: Long = 0, val host: String? = null,
    val url: String? = null, val files: List<PeerTubeFile>? = null,
    val streamingPlaylists: List<PeerTubePlaylist>? = null,
    val directStreamUrl: String? = null, val nsfw: Boolean = false
) {
    fun sourceServer(fallback: String = ""): String {
        val parsed = runCatching { URI(url.orEmpty()) }.getOrNull()
        if (parsed != null && parsed.scheme in setOf("https", "http") && !parsed.host.isNullOrBlank()) {
            val port = parsed.port.takeIf { it > 0 && it != 443 }?.let { ":$it" }.orEmpty()
            return normalizePeerTubeServer(parsed.host + port)
        }
        if (!host.isNullOrBlank()) return normalizePeerTubeServer(host)
        return fallback.takeIf(String::isNotBlank)?.let(::normalizePeerTubeServer).orEmpty()
    }

    fun apiId(): String = uuid.takeIf(String::isNotBlank)
        ?: runCatching { URI(url.orEmpty()).path.substringAfterLast('/') }.getOrDefault("")

    fun playableUrl(maxHeight: Int = 0): String? {
        fun secure(value: String?): String? {
            val raw = value?.trim()?.takeIf(String::isNotBlank) ?: return null
            val uri = runCatching { URI(raw) }.getOrNull() ?: return null
            if (uri.userInfo != null || uri.host.isNullOrBlank() || uri.scheme !in setOf("https", "http")) return null
            return if (uri.scheme == "https") raw else raw.replaceFirst("http://", "https://")
        }
        val allFiles = files.orEmpty() + streamingPlaylists.orEmpty().flatMap { it.files.orEmpty() }
        val candidates = if (maxHeight > 0) {
            allFiles.filter { (it.resolution?.id ?: 0) <= maxHeight }
                .ifEmpty { allFiles.sortedBy { it.resolution?.id ?: 0 }.take(1) }
        } else allFiles
        return streamingPlaylists.orEmpty().firstNotNullOfOrNull { secure(it.playlistUrl) }
            ?: candidates.maxByOrNull { it.resolution?.id ?: 0 }?.fileUrl?.let(::secure)
            ?: secure(directStreamUrl)
    }

    fun thumbnailCandidates(server: String): List<String> {
        val base = runCatching { normalizePeerTubeServer(server) }.getOrDefault("")
        return listOfNotNull(thumbnailUrl, thumbnailPath, previewPath).mapNotNull { value ->
            val raw = value.trim()
            if (raw.isEmpty()) null
            else if (raw.startsWith("https://")) raw
            else if (raw.startsWith("/")) base + raw
            else null
        }.distinct()
    }
}

data class PeerSeekVideo(
    val id: String? = null, val uuid: String? = null, val title: String? = null,
    val name: String? = null, val thumbnailUrl: String? = null, val videoUrl: String? = null,
    val views: Long? = null, val instance: JsonElement? = null,
    val nsfw: Boolean = false
) {
    fun asPeerTubeVideo(): PeerTubeVideo? {
        val identity = uuid?.takeIf(String::isNotBlank) ?: id?.takeIf(String::isNotBlank) ?: return null
        fun field(element: JsonElement?, key: String): String? = runCatching {
            if (element == null || element.isJsonNull) null
            else if (element.isJsonObject) element.asJsonObject.get(key)?.takeUnless { it.isJsonNull }?.asString
            else element.asString
        }.getOrNull()
        val host = (field(instance, "host") ?: field(instance, "domain"))?.removePrefix("https://")?.trimEnd('/')
        val title = title?.takeIf(String::isNotBlank) ?: name?.takeIf(String::isNotBlank) ?: return null
        return PeerTubeVideo(uuid = identity, name = title, thumbnailUrl = thumbnailUrl,
            url = videoUrl, views = views ?: 0L, host = host, nsfw = nsfw)
    }
}

interface PeerTubeApi {
    @GET("api/v1/videos") suspend fun videos(
        @Query("sort") sort: String = "-publishedAt", @Query("count") count: Int = 30,
        @Query("isLocal") isLocal: Boolean? = null, @Query("start") start: Int = 0,
        @Query("nsfw") nsfw: String = "false"
    ): PeerTubeVideoPage

    @GET("api/v1/search/videos") suspend fun search(
        @Query("search") query: String, @Query("start") start: Int = 0,
        @Query("count") count: Int = 30, @Query("sort") sort: String = "-match",
        @Query("nsfw") nsfw: String = "false"
    ): PeerTubeVideoPage

    @GET("api/v1/videos/{id}") suspend fun video(@Path("id") id: String): PeerTubeVideo
}

interface SepiaApi {
    @GET("api/v1/search/videos") suspend fun search(
        @Query("search") query: String, @Query("start") start: Int = 0,
        @Query("count") count: Int = 30, @Query("sort") sort: String = "-match",
        @Query("nsfw") nsfw: String = "false"
    ): PeerTubeVideoPage
}

interface PeerSeekApi {
    @GET("v1/search") suspend fun search(
        @Query("q") query: String, @Query("sort") sort: String = "relevance",
        @Query("page") page: Int = 1, @Query("size") size: Int = 30
    ): PeerSeekPage
}

interface PeerTubeDirectoryApi {
    @GET("api/v1/instances") suspend fun instances(
        @Query("start") start: Int = 0, @Query("count") count: Int = 50,
        @Query("search") search: String? = null
    ): PeerTubeInstancePage
}

object PeerTubeClients {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).build()
    private val servers = linkedMapOf<String, PeerTubeApi>()
    private val sepia: SepiaApi by lazy { retrofit("https://sepiasearch.org/").create(SepiaApi::class.java) }
    private val peerSeek: PeerSeekApi by lazy { retrofit("https://peerseek.video/").create(PeerSeekApi::class.java) }
    private val directory: PeerTubeDirectoryApi by lazy {
        retrofit("https://instances.joinpeertube.org/").create(PeerTubeDirectoryApi::class.java)
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder().baseUrl(baseUrl)
        .client(http).addConverterFactory(GsonConverterFactory.create()).build()

    @Synchronized fun server(value: String): PeerTubeApi {
        val host = normalizePeerTubeServer(value)
        return servers.getOrPut(host) { retrofit("$host/").create(PeerTubeApi::class.java) }
    }

    suspend fun search(query: String, configuredServers: List<String>): List<PeerTubeVideo> {
        val clean = query.trim()
        require(clean.isNotEmpty()) { "Enter a search phrase" }
        val sources = listOf<suspend () -> List<PeerTubeVideo>>(
            { sepia.search(clean).data.mapNotNull(::validatedVideo) },
            { peerSeek.search(clean).videos.mapNotNull(PeerSeekVideo::asPeerTubeVideo).mapNotNull(::validatedVideo) }
        ) + configuredServers.take(5).map { host -> suspend {
            server(host).search(clean).data.mapNotNull(::validatedVideo)
        } }
        val semaphore = Semaphore(3)
        val results = coroutineScope {
            sources.map { source -> async { semaphore.withPermit {
                try { Result.success(source()) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { Result.failure(error) }
            } } }.awaitAll()
        }
        val successful = results.filter { it.isSuccess }.flatMap { it.getOrDefault(emptyList()) }
        if (successful.isEmpty() && results.all { it.isFailure }) {
            throw results.first().exceptionOrNull() ?: IllegalStateException("No search service is available")
        }
        return deduplicate(successful, configuredServers.firstOrNull().orEmpty())
    }

    suspend fun home(server: String): List<PeerTubeVideo> =
        this.server(server).videos(isLocal = true).data.mapNotNull(::validatedVideo)

    suspend fun video(server: String, id: String): PeerTubeVideo =
        validatedVideo(this.server(server).video(id))
            ?: throw IllegalArgumentException("The PeerTube server returned an invalid video URL")

    suspend fun discoverInstances(search: String = ""): List<PeerTubeInstance> =
        directory.instances(count = 50, search = search.trim().takeIf(String::isNotBlank)).data
            .filter { it.host.isNotBlank() }

    @JvmStatic fun searchBlocking(query: String, servers: List<String>): List<PeerTubeVideo> =
        runBlocking(Dispatchers.IO) { search(query, servers) }

    @JvmStatic fun homeBlocking(server: String): List<PeerTubeVideo> =
        runBlocking(Dispatchers.IO) { home(server) }

    @JvmStatic fun videoBlocking(server: String, id: String): PeerTubeVideo =
        runBlocking(Dispatchers.IO) { video(server, id) }

    @JvmStatic fun discoverInstancesBlocking(search: String): List<PeerTubeInstance> =
        runBlocking(Dispatchers.IO) { discoverInstances(search) }
}

/** Enforces the federation result URL and video-id shape before we make another request or play it. */
private fun validatedVideo(video: PeerTubeVideo): PeerTubeVideo? {
    if (video.nsfw) return null
    return try {
        val page = URI(video.url ?: return null)
        if (!page.scheme.equals("https", true) || page.host.isNullOrBlank() || page.userInfo != null) return null
        val match = Regex("^/(?:w|videos/watch)/([A-Za-z0-9]{22}|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12})/?$")
            .matchEntire(page.path ?: return null) ?: return null
        val pathId = match.groupValues[1]
        val id = video.uuid.ifBlank { pathId }
        if (!id.equals(pathId, true)) return null
        val port = page.port.takeIf { it > 0 && it != 443 }?.let { ":$it" }.orEmpty()
        val origin = "https://${page.host.lowercase(Locale.ROOT)}$port"
        video.copy(uuid = id, host = origin, url = "$origin${page.rawPath}")
    } catch (_: Exception) {
        null
    }
}

private fun deduplicate(videos: List<PeerTubeVideo>, fallback: String): List<PeerTubeVideo> {
    val seen = hashSetOf<String>()
    return videos.filter { video ->
        val server = runCatching { video.sourceServer(fallback) }.getOrDefault(fallback).trimEnd('/').lowercase()
        val id = video.uuid.ifBlank { video.url.orEmpty() }.lowercase()
        id.isNotBlank() && seen.add("$server|$id")
    }
}

fun normalizePeerTubeServer(input: String): String {
    val raw = input.trim().let { if (it.contains("://")) it else "https://$it" }.trimEnd('/')
    val uri = URI(raw)
    require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.userInfo == null) {
        "Enter a public HTTPS PeerTube address"
    }
    return "https://${uri.host.lowercase()}${if (uri.port > 0 && uri.port != 443) ":${uri.port}" else ""}"
}
