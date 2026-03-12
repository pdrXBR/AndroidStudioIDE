package com.androidide.domain.usecase

import com.androidide.data.repository.FileRepository
import java.io.File
import javax.inject.Inject

class OpenFileUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(path: String): Result<String> = fileRepository.readFile(path)

    suspend fun saveFile(path: String, content: String): Result<Unit> =
        fileRepository.writeFile(path, content)
}
