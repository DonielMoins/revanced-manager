package app.revanced.manager.backend.api

import android.util.Log
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject

object GitHubAPI {
    private const val tag = "GitHubAPI"
    private const val baseUrl = "https://api.github.com/repos"

    object Releases {
        suspend fun latestRelease(repo: String): Release {
            Log.d(tag, "Fetching latest releases for repo ($repo)")
            val res: List<Release> = client.get("$baseUrl/$repo/releases") {
                parameter("per_page", 1)
            }.body()
            return res.first()
        }

        @Serializable
        class Release(
            @SerialName("tag_name") val tagName: String,
            @SerialName("published_at") val publishedAt: String,
            @SerialName("prerelease") val isPrerelease: Boolean,
            val assets: List<ReleaseAsset>,
            val body: String
        )

        @Serializable
        class ReleaseAsset(
            @SerialName("browser_download_url") val downloadUrl: String,
            val name: String
        )
    }

    object Commits {
        suspend fun latestCommit(repo: String, ref: String): Commit {
            Log.d(tag, "Fetching latest commit for repo ($repo) with ref ($ref)")
            val res: Commit = client.get("$baseUrl/$repo/commits/$ref") {
                parameter("per_page", 1)
            }.body()
            return res
        }

        @Serializable
        class Commit(
            @SerialName("sha") val shaHash: String,
            @SerialName("commit") val commitObj: CommitObject
        ) {
            @Serializable
            class CommitObject(
                val message: String,
                val author: Author,
                val committer: Author
            ) {
                @Serializable
                class Author(
                    val name: String,
                    val date: String
                )
            }
        }
    }

    //  Announcements are fetched from repo's 'announcements.json' file.
    //  Version is passed for backwards compatibility, or possible migration of announcements to a separate repo;
    //  to know which announcements to fetch.
    //  TODO: create separate version channels, Ex.: Stable; Beta; Canary.
    object Announcements {
        suspend fun latestAnnouncement(repo: String, version: String): Announcement {
            when (version.lowercase()) {
                "canary", "beta", "stable" -> {

                    val annObj: AnnouncementObject? =
                        client.get("$baseUrl/$repo/contents/pages/announcements.json").body()
                    if (annObj !is AnnouncementObject) throw Exception("Could not fetch announcements.json file from repo!")
                    val contentsJson: JSONObject = JSONObject(
                        client.get("$baseUrl/$repo/contents/pages/announcements.json")
                            .body() as String
                    )
                    return Announcement(
                        text = contentsJson.get(version.lowercase()).toString(),
                        date = "NOT IMPLEMENTED",
                        version = version
                    )

                }
                else -> {
                    Log.d(
                        tag,
                        "Unknown build version/channel ($version). Announcements will be unavailable."
                    )
                    throw Exception("Unknown build version/channel ($version). Announcements will be unavailable.")
                }

            }

        }


        @Serializable
        class AnnouncementObject(
            @SerialName("sha") val shaHash: String,
            @SerialName("size") val size: String,
            @SerialName("content") val encodedContent: String,
            @SerialName("encoding") val encoding: String,
            @SerialName("download_url") val downloadURL: String,
        )

        class Announcement(
            val text: String,
            val date: String?,
            val version: String?
        ) {

        }
    }
}