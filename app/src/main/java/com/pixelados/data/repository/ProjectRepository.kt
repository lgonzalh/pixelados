package com.pixelados.data.repository

import android.content.Context
import com.pixelados.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Repositorio para persistir proyectos en almacenamiento interno de la app.
 * Usa JSON + Serialization. Un archivo por proyecto.
 */
class ProjectRepository(private val context: Context) {

    private val projectsDir: File = context.filesDir.resolve("projects").apply { mkdirs() }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * Obtiene todos los proyectos ordenados por fecha (más reciente primero).
     */
    suspend fun getAllProjects(): List<Project> = withContext(Dispatchers.IO) {
        projectsDir.listFiles()?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val input = FileInputStream(file)
                    val content = input.bufferedReader().use { it.readText() }
                    input.close()
                    json.decodeFromString<Project>(content)
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    /**
     * Guarda un proyecto (crea o actualiza).
     */
    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "${project.id}.json")
        val output = FileOutputStream(file)
        output.write(json.encodeToString(project).toByteArray())
        output.close()
    }

    /** Recupera un proyecto por id (null si no existe). */
    suspend fun getProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$projectId.json")
        if (!file.exists()) return@withContext null
        try {
            val input = FileInputStream(file)
            val content = input.bufferedReader().use { it.readText() }
            input.close()
            json.decodeFromString<Project>(content)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Limpieza puntual del bug de autoguardado duplicado: versiones anteriores
     * creaban un archivo NUEVO en cada autoguardado de una misma sesión.
     * Esos duplicados comparten (nombre, createdAt, tamaño), así que se agrupa
     * por esa clave y se conserva solo el más reciente de cada grupo.
     * @return cantidad de duplicados eliminados.
     */
    suspend fun cleanupDuplicateProjects(): Int = withContext(Dispatchers.IO) {
        val projects = getAllProjects()
        val groups = projects.groupBy {
            Triple(it.name, it.createdAt, "${it.canvas.width}x${it.canvas.height}")
        }
        var removed = 0
        groups.forEach { (_, list) ->
            if (list.size > 1) {
                val keep = list.maxByOrNull { it.updatedAt } ?: return@forEach
                list.filter { it.id != keep.id }.forEach { dup ->
                    if (File(projectsDir, "${dup.id}.json").delete()) removed++
                }
            }
        }
        removed
    }

    /**
     * Elimina un proyecto por ID.
     */
    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$projectId.json")
        file.delete()
    }

    /**
     * Renombra un proyecto.
     */
    suspend fun renameProject(projectId: String, newName: String) = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$projectId.json")
        if (file.exists()) {
            val input = FileInputStream(file)
                val content = input.bufferedReader().use { it.readText() }
            input.close()
            val project = json.decodeFromString<Project>(content)
            val updated = project.copy(canvas = project.canvas.copy(name = newName))
            val output = FileOutputStream(file)
            output.write(json.encodeToString(updated).toByteArray())
            output.close()
        }
    }
}
