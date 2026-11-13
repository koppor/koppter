package io.github.koppor.koppter.plan

import java.net.URI

/**
 * Reads the iCalendar file at the address, which is one that a calendar service publishes for a calendar.
 * Blocks until the file is read; `webcal:` is read as `https:`.
 */
// [impl->dsn~busy-times~1]
fun fetchCalendar(address: String): String {
    val uri = URI(address.trim().replaceFirst(Regex("^webcal:", RegexOption.IGNORE_CASE), "https:"))
    // Not a file of this computer or another scheme that the platform would open.
    require(uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true)) { "not a web address" }
    val connection = uri.toURL().openConnection()
    connection.connectTimeout = 15_000
    connection.readTimeout = 30_000
    return connection.getInputStream().use { it.readBytes().decodeToString() }
}
