package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate

/**
 * Makes a task of a text that another application shares, such as the link of an article.
 *
 * The [template] is a line in todo.txt syntax: `{}` stands for the shared text, which is put in front without it.
 * A due or threshold date may be an interval from the day of sharing: `{} @research>reading due:2w dur:1h`.
 */
// [impl->dsn~share-actions~1]
data class ShareAction(val title: String, val template: String) {
    fun task(shared: String, today: LocalDate): Task {
        val text = shared.trim().replace(WHITESPACE, " ")
        // The body is set directly: shared text that starts like a priority or a date is not read as one.
        val task = Task(body = if (PLACEHOLDER in template) template.replace(PLACEHOLDER, text) else "$text $template".trim())
        return listOf(Task.DUE, Task.THRESHOLD).fold(task) { result, key ->
            val date = result.tag(key)?.let { Query.date(it, today) } ?: return@fold result
            result.withTag(key, date.toString())
        }
    }

    companion object {
        private const val PLACEHOLDER = "{}"
        private val WHITESPACE = Regex("\\s+")

        /** Reads the actions: one per line as `title = template`; lines without a title are skipped. */
        fun parse(text: String): List<ShareAction> = text.lines().mapNotNull { line ->
            val title = line.substringBefore('=', "").trim()
            if (title.isEmpty()) null else ShareAction(title, line.substringAfter('=').trim())
        }
    }
}
