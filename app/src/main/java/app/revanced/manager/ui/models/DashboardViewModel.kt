package app.revanced.manager.ui.models

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.Global
import app.revanced.manager.backend.api.GitHubAPI
import app.revanced.manager.ui.Resource
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel : ViewModel() {
    private val tag = "DashboardViewModel"

    @Suppress("SpellCheckingInspection") // I'll save you the headache.
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

    private var _latestPatcherCommit: GitHubAPI.Commits.Commit? by mutableStateOf(null)
    val patcherCommitDate: String
        get() = _latestPatcherCommit?.commitDate ?: "unknown"

    private var _latestManagerCommit: GitHubAPI.Commits.Commit? by mutableStateOf(null)
    val managerCommitDate: String
        get() = _latestManagerCommit?.commitDate ?: "unknown"

    var latestAnnouncement = mutableStateOf<Resource<GitHubAPI.Announcements.Announcement>>(
        Resource.Loading
    )

    init {
        viewModelScope.launch {
            fetchLastCommit()

            //  Although latestAnnouncement is currently in GitHubAPI, it should be moved later to a more permanent location along with the Announcement source.
            getAnnouncement().collect { response ->
                latestAnnouncement.value =
                    response as Resource<GitHubAPI.Announcements.Announcement>

            }


        }

    }

    private suspend fun getAnnouncement() = flow {
        try {
            emit(Resource.Loading)
            emit(
                Resource.Success(
                    GitHubAPI.Announcements.latestAnnouncement(
                        Global.ghManager,
                        "canary"
                    )
                )
            )
        } catch (e: Exception) {
            emit(Resource.Failure(e))
        }
    }

    private suspend fun fetchLastCommit() {
        try {
            _latestPatcherCommit = GitHubAPI.Commits.latestCommit(Global.ghPatcher, "HEAD")
        } catch (e: Exception) {
            Log.e(tag, "failed to fetch latest patcher commit", e)
        }
        try {
            _latestManagerCommit = GitHubAPI.Commits.latestCommit(Global.ghManager, "HEAD")
        } catch (e: Exception) {
            Log.e(tag, "failed to fetch latest manager commit", e)
        }
    }

    private val GitHubAPI.Commits.Commit.commitDate: String
        get() = DateUtils.getRelativeTimeSpanString(
            formatter.parse(commitObj.committer.date)!!.time,
            Calendar.getInstance().timeInMillis,
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
}