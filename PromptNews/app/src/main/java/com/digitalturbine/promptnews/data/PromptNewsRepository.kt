package com.digitalturbine.promptnews.data

import com.digitalturbine.promptnews.domain.model.Prompt
import com.digitalturbine.promptnews.domain.model.PromptResultBundle

interface PromptNewsRepository {
    suspend fun fetchPromptResults(
        prompt: Prompt,
        locale: String? = null,
        geo: String? = null
    ): PromptResultBundle
}
