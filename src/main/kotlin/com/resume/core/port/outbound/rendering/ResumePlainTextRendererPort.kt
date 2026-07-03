package com.resume.core.port.outbound.rendering

import com.resume.core.domain.model.Resume

/**
 * Port for rendering a resume into a plain-text layout.
 *
 * Pure and deterministic: walks [Resume]'s structured data directly without going through
 * HTML or any external binary. Used for the `txt` export format.
 */
fun interface ResumePlainTextRendererPort {
    fun render(resume: Resume): String
}
