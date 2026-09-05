package com.satvik.mibandnavigator

import java.text.Normalizer

enum class NavDirection {
    LEFT, RIGHT, STRAIGHT, SLIGHT_LEFT, SLIGHT_RIGHT, SHARP_LEFT, SHARP_RIGHT, UTURN, ROUNDABOUT,
    KEEP_LEFT, KEEP_RIGHT, MERGE, EXIT, EXIT_LEFT, EXIT_RIGHT, FERRY,
    DESTINATION, UNKNOWN
}

data class NavData(
    val distance: String,
    val roadName: String,
    val direction: NavDirection,
    val eta: String = "",
    val totalDistance: String = "",
    val roundaboutExit: Int? = null,
    val exitNumber: String = "",
    val arrivalTime: String = "",
    val maneuverText: String = ""
)

class NavigationParser {

    fun parseMapsData(
        title: String,
        text: String,
        subText: String,
        textLines: Array<String>?,
        titleBig: String = "",
        bigText: String = "",
        summaryText: String = "",
        infoText: String = "",
        tickerText: String = ""
    ): NavData {
        val lines = buildList {
            add(title)
            add(text)
            add(subText)
            textLines?.forEach(::add)
            add(titleBig)
            add(bigText)
            add(summaryText)
            add(infoText)
            add(tickerText)
        }.map(::cleanText).filter(String::isNotEmpty)

        val searchableText = lines.joinToString(" ")
        val direction = parseDirection(searchableText)
        val summaryLines = buildList {
            add(subText)
            textLines?.forEach(::add)
            add(summaryText)
            add(infoText)
        }.map(::cleanText).filter(String::isNotEmpty)
        val maneuverText = lines.firstOrNull(::looksLikeInstruction).orEmpty()

        return NavData(
            distance = extractDistance(title) ?: extractDistance(text) ?: extractDistance(titleBig) ?: extractDistance(bigText) ?: cleanText(title),
            roadName = extractRoadName(lines),
            direction = direction,
            eta = extractEta(summaryLines),
            totalDistance = extractTotalDistance(summaryLines),
            roundaboutExit = if (direction == NavDirection.ROUNDABOUT) {
                extractRoundaboutExit(searchableText)
            } else {
                null
            },
            exitNumber = extractExitNumber(searchableText),
            arrivalTime = extractArrivalTime(summaryLines),
            maneuverText = maneuverText
        )
    }

    private fun parseDirection(value: String): NavDirection {
        val text = normalizeForMatch(value)

        return when {
            containsAny(text, "you have arrived", "arrive at", "destination", "tujuan telah tiba", "anda telah tiba", "sampai di tujuan") -> NavDirection.DESTINATION
            isRoundaboutText(text) -> NavDirection.ROUNDABOUT
            containsAny(text, "ferry", "feri") -> NavDirection.FERRY
            containsAny(text, "u-turn", "u turn", "uturn", "make a u", "putar balik") -> NavDirection.UTURN
            containsAny(text, "left ramp", "ramp left", "jalan penghubung kiri") -> NavDirection.LEFT
            containsAny(text, "right ramp", "ramp right", "jalan penghubung kanan") -> NavDirection.RIGHT
            containsAny(text, "exit left", "take the left exit", "take exit left", "left exit", "keluar kiri", "keluaran kiri") -> NavDirection.EXIT_LEFT
            containsAny(text, "exit right", "take the right exit", "take exit right", "right exit", "keluar kanan", "keluaran kanan") -> NavDirection.EXIT_RIGHT
            containsAny(text, "take exit", "take the exit", "use exit", "take the ramp", "use the ramp", "keluar melalui", "ambil keluar", "jalan penghubung") -> NavDirection.EXIT
            containsAny(text, "merge", "join the", "bergabung") -> NavDirection.MERGE
            containsAny(text, "keep left", "stay left", "bear left", "fork left", "ambil kiri", "ambil jalan kiri", "tetap kiri") -> NavDirection.KEEP_LEFT
            containsAny(text, "keep right", "stay right", "bear right", "fork right", "ambil kanan", "ambil jalan kanan", "tetap kanan") -> NavDirection.KEEP_RIGHT
            containsAny(text, "slight left", "slightly left", "veer left", "serong kiri", "sedikit ke kiri", "belok sedikit kiri") -> NavDirection.SLIGHT_LEFT
            containsAny(text, "slight right", "slightly right", "veer right", "serong kanan", "sedikit ke kanan", "belok sedikit kanan") -> NavDirection.SLIGHT_RIGHT
            containsAny(text, "sharp left", "sharply left", "belok tajam ke kiri", "belok tajam kiri") -> NavDirection.SHARP_LEFT
            containsAny(text, "sharp right", "sharply right", "belok tajam ke kanan", "belok tajam kanan") -> NavDirection.SHARP_RIGHT
            containsAny(text, "on ramp") -> NavDirection.MERGE
            containsAny(text, "off ramp") -> NavDirection.EXIT
            containsAny(text, "turn left", "left", "belok kiri", "ke kiri") -> NavDirection.LEFT
            containsAny(text, "turn right", "right", "belok kanan", "ke kanan") -> NavDirection.RIGHT
            containsAny(text, "straight", "continue", "head ", "go on", "enter ", "depart", "name change", "lurus", "lanjut", "terus", "masuk", "perubahan nama jalan") -> NavDirection.STRAIGHT
            else -> NavDirection.UNKNOWN
        }
    }

    private fun extractRoadName(candidates: List<String>): String {
        candidates.firstNotNullOfOrNull(::roadNameAfterCue)?.let(::cleanRoadName)?.let { return it }

        return candidates.firstOrNull { candidate ->
            !looksLikeInstruction(candidate) && !looksLikeTripSummary(candidate)
        }?.let(::cleanRoadName).orEmpty()
    }

    private fun roadNameAfterCue(value: String): String? = roadCueRegex.find(value)?.groupValues?.getOrNull(1)

    private fun cleanRoadName(value: String): String {
        return cleanText(
            value
                .substringBefore("•")
                .substringBefore("·")
                .replace(Regex("(?i)^(?:stay\\s+)?(?:on|onto|ke)\\s+"), "")
        ).trim(',', '.', ';', ':')
    }

    private fun extractDistance(value: String): String? = distanceRegex.find(value)?.value

    private fun extractEta(lines: List<String>): String =
        lines.asSequence().mapNotNull { durationRegex.find(it)?.value }.firstOrNull().orEmpty()

    private fun extractTotalDistance(lines: List<String>): String =
        lines.flatMap { distanceRegex.findAll(it).map(MatchResult::value).toList() }.lastOrNull().orEmpty()

    private fun extractArrivalTime(lines: List<String>): String =
        lines.flatMap { arrivalTimeRegex.findAll(it).map(MatchResult::value).toList() }.lastOrNull().orEmpty()

    private fun extractRoundaboutExit(value: String): Int? {
        numberedRoundaboutExitRegexes.forEach { regex ->
            regex.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { exit ->
                if (exit > 0) return exit
            }
        }

        ordinalExitRegex.find(value)?.groupValues
            ?.drop(1)
            ?.firstOrNull(String::isNotEmpty)
            ?.let(::ordinalNumber)
            ?.let { exit -> if (exit > 0) return exit }

        return null
    }

    private fun extractExitNumber(value: String): String =
        exitNumberRegex.find(value)?.groupValues
            ?.drop(1)
            ?.firstOrNull(String::isNotEmpty)
            .orEmpty()
            .uppercase()

    private fun isRoundaboutText(text: String): Boolean {
        return containsAny(text, "roundabout", "traffic circle", "rotary", "bundaran", "rond-point", "rotonda") ||
            ordinalExitRegex.containsMatchIn(text)
    }

    private fun looksLikeInstruction(value: String): Boolean {
        val text = normalizeForMatch(value)
        return containsAny(
            text,
            "turn", "take", "continue", "keep", "bear", "veer", "fork", "merge", "head ", "go on",
            "u-turn", "left", "right", "straight", "exit", "roundabout", "ferry",
            "belok", "ambil", "lanjut", "tetap", "keluar", "bundaran", "putar balik", "serong", "lurus", "feri"
        )
    }

    private fun looksLikeTripSummary(value: String): Boolean {
        return distanceRegex.containsMatchIn(value) || durationRegex.containsMatchIn(value) || arrivalTimeRegex.containsMatchIn(value)
    }

    private fun cleanText(value: String): String = value.replace(Regex("\\s+"), " ").trim()

    private fun normalizeForMatch(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('–', '-')
            .replace('—', '-')
            .replace('-', ' ')
            .lowercase()
    }

    private fun containsAny(text: String, vararg terms: String): Boolean = terms.any(text::contains)

    private fun ordinalNumber(value: String): Int? = when (normalizeForMatch(value)) {
        "first", "pertama" -> 1
        "second", "kedua" -> 2
        "third", "ketiga" -> 3
        "fourth", "keempat" -> 4
        "fifth", "kelima" -> 5
        "sixth", "keenam" -> 6
        "seventh", "ketujuh" -> 7
        "eighth", "kedelapan" -> 8
        "ninth", "kesembilan" -> 9
        "tenth", "kesepuluh" -> 10
        else -> null
    }

    private companion object {
        val distanceRegex = Regex(
            """\b\d+(?:[.,]\d+)?\s?(?:km|kilometers?|kilometres?|mi|miles?|ft|feet|yd|yards?|m|meters?|metres?)\b""",
            RegexOption.IGNORE_CASE
        )
        val durationRegex = Regex(
            """\b(?:\d+\s*(?:h|hr|hrs|hour|hours|jam)(?:\s*\d+\s*(?:m|min|mins|minute|minutes|menit|mnt))?|\d+\s*(?:m|min|mins|minute|minutes|menit|mnt))\b""",
            RegexOption.IGNORE_CASE
        )
        val arrivalTimeRegex = Regex("""\b\d{1,2}[:.]\d{2}\b""")
        val roadCueRegex = Regex(
            """\b(?:onto|on|towards?|via|to|ke|menuju|melalui)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
        val numberedRoundaboutExitRegexes = listOf(
            Regex("""\b(\d{1,2})(?:st|nd|rd|th)?\s+(?:exit|keluar(?:an)?)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(?:exit|keluar(?:an)?)(?:\s+(?:no\.?|nomor|ke))?\s*-?(\d{1,2})\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(?:pintu\s+)?keluar(?:an)?\s+ke[- ]?(\d{1,2})\b""", RegexOption.IGNORE_CASE)
        )
        val ordinalExitRegex = Regex(
            """\b(?:the\s+)?(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|pertama|kedua|ketiga|keempat|kelima|keenam|ketujuh|kedelapan|kesembilan|kesepuluh)\s+(?:exit|keluar(?:an)?)\b|\b(?:exit|keluar(?:an)?)\s+(?:ke[- ]?)?(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|pertama|kedua|ketiga|keempat|kelima|keenam|ketujuh|kedelapan|kesembilan|kesepuluh)\b""",
            RegexOption.IGNORE_CASE
        )
        val exitNumberRegex = Regex(
            """\b(?:take\s+|use\s+)?exit\s*(?:no\.?\s*)?([0-9]+[a-z]?)\b|\bkeluar(?:an)?(?:\s+(?:no\.?|nomor))?\s*([0-9]+[a-z]?)\b""",
            RegexOption.IGNORE_CASE
        )
    }
}
