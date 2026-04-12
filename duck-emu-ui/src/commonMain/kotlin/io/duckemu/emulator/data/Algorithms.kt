package io.duckemu.emulator.data

fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }

    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j

    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1]
            } else {
                1 + minOf(
                    dp[i - 1][j],
                    dp[i][j - 1],
                    dp[i - 1][j - 1]
                )
            }
        }
    }

    return dp[a.length][b.length]
}

fun searchMatch(query: String, gameName: String): Boolean {
    if (query.isBlank()) return true

    val q = query.trim().lowercase()
    val name = gameName.lowercase()

    if (name.contains(q)) return true

    val acronym = name.split(" ").map { it.first() }.joinToString("")
    if (acronym.contains(q)) return true

    val tolerance = if (q.length <= 4) 1 else 2
    val distance = levenshtein(q, name.take(q.length + tolerance))
    if (distance <= tolerance) return true

    return name.split(" ").any { word ->
        levenshtein(q, word) <= tolerance
    }
}