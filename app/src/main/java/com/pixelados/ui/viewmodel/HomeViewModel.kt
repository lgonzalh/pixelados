package com.pixelados.ui.viewmodel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelados.data.model.Project
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ProjectRepository,
    private val savedColorsRepo: SavedColorsRepository
) : ViewModel() {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects = _projects.asStateFlow()

    /** false hasta que termina la primera lectura del disco (evita el "no hay lienzos" falso). */
    private val _loaded = MutableStateFlow(false)
    val loaded = _loaded.asStateFlow()

    // Para PhotoPicker
    private var pickVisualMediaLauncher: ((PickVisualMediaRequest) -> Unit)? = null

    init {
        // Migración puntual: el autoguardado antiguo creaba un archivo nuevo en
        // cada guardado de una misma sesión. Conserva solo el más reciente.
        viewModelScope.launch {
            val removed = repository.cleanupDuplicateProjects()
            if (removed > 0) println("[Pixelados] Duplicados eliminados al arrancar: $removed")
        }
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _projects.value = repository.getAllProjects()
            _loaded.value = true
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
            loadProjects()
        }
    }

    fun renameProject(project: Project, newName: String) {
        viewModelScope.launch {
            repository.renameProject(project.id, newName)
            loadProjects()
        }
    }

    /** Configura el launcher del PhotoPicker (debe llamarse desde Activity/Composable) */
    fun setPickVisualMediaLauncher(launcher: (PickVisualMediaRequest) -> Unit) {
        pickVisualMediaLauncher = launcher
    }

    /** Lanza el PhotoPicker para seleccionar una imagen */
    fun pickPhoto(activity: Activity) {
        val request = PickVisualMediaRequest(
            mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
        )
        pickVisualMediaLauncher?.invoke(request)
    }

    class Factory(
        private val repository: ProjectRepository,
        private val savedColorsRepo: SavedColorsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, savedColorsRepo) as T
        }
    }
}
