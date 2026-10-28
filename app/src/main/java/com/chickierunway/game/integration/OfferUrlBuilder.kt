package com.chickierunway.game.integration

/**
 * Assembles the offer URL from Adjust attribution:
 *
 *   «АСО»:   {base}?{all attribution}&campaign=..&sub1=..&subN=..&sub12=..&push_token=..&adid=..#{fragment}
 *   «Прилы»: {base}/{sub1}?{the same query, sub1 kept in query too}
 *
 * Built by string concatenation on purpose: android.net.Uri.Builder re-encodes values that are
 * already encoded, so a campaign carrying `%26` would reach the tracker as `%2526`.
 */
object OfferUrlBuilder {

    private const val CAMPAIGN_KEY = "campaign"
    private const val SUB1_KEY = "sub1"
    private const val ADID_KEY = "adid"

    fun build(
        baseUrl: String,
        attribution: Map<String, String>,
        adid: String?,
        extraParams: List<Pair<String, String>> = emptyList(),
        sub1InPath: Boolean = false,
    ): String {
        val base = baseUrl.trim()
        if (base.isEmpty()) return ""

        // Split without decoding — anything already encoded must pass through as-is.
        val hashIndex = base.indexOf('#')
        val fragment = if (hashIndex >= 0) base.substring(hashIndex) else ""
        val withoutFragment = if (hashIndex >= 0) base.substring(0, hashIndex) else base

        val questionIndex = withoutFragment.indexOf('?')
        val path = if (questionIndex >= 0) withoutFragment.substring(0, questionIndex) else withoutFragment
        val baseQuery = if (questionIndex >= 0) withoutFragment.substring(questionIndex + 1) else ""

        val params = LinkedHashMap<String, String>()
        parseQuery(baseQuery, params)

        // Every attribution field, in arrival order. Filtering to a known subset here is the most
        // common way this integration silently loses attribution when the network adds a key.
        params.putAll(attribution)

        val campaign = attribution[CAMPAIGN_KEY].orEmpty()
        val subs = CampaignParser.parse(campaign)

        // The tracker reads the raw campaign as well as its exploded parts, so it always stays.
        if (campaign.isNotEmpty()) params[CAMPAIGN_KEY] = campaign

        // Parsed subs win over any sub1..subN the tracker sent itself. Re-putting an existing key
        // replaces it in place, so order stays deterministic and no key is emitted twice.
        params.putAll(subs)

        // Extras (device signals, gps_adid, push token) override same-named keys: a campaign of 12+ segments
        // has its own sub12, and without this the URL would carry sub12 twice. Added
        // unconditionally — an empty value and a missing key mean different things on the offer side.
        extraParams.forEach { (key, value) -> params[key] = value }

        // adid last, matching the agreed parameter order on the offer side. A blank id would read
        // as "attributed to nothing" rather than "not known yet".
        if (!adid.isNullOrBlank()) params[ADID_KEY] = adid.trim()

        val query = CampaignParser.toQuery(params)
        val sub1 = subs[SUB1_KEY].orEmpty()
        // «Прилы» also carries sub1 as a path segment after the alias; «АСО» leaves the path alone.
        val finalPath = if (sub1InPath) appendPathSegment(path, sub1) else path

        return buildString {
            append(finalPath)
            if (query.isNotEmpty()) {
                append('?')
                append(query)
            }
            append(fragment)
        }
    }

    /**
     * Appends sub1 as the last path segment, after whatever path the base URL already has.
     * Empty sub1 leaves the path alone; a path already ending in sub1 (build() called again on
     * its own output) is not extended a second time.
     */
    private fun appendPathSegment(path: String, sub1: String): String {
        if (sub1.isEmpty()) return path
        val segment = PercentEncoder.encodePathSegment(sub1)
        // "." / ".." would be collapsed by the server into a different path entirely.
        if (segment == "." || segment == "..") return path

        // "https://host" | "/existing/path/" — the segment goes after the existing path.
        val schemeEnd = path.indexOf("://")
        val originEnd = if (schemeEnd < 0) {
            0
        } else {
            path.indexOf('/', schemeEnd + 3).let { if (it < 0) path.length else it }
        }
        val origin = path.substring(0, originEnd)
        val existingPath = path.substring(originEnd)

        val trimmed = existingPath.trimEnd('/')
        if (trimmed.isNotEmpty() && trimmed.substringAfterLast('/') == segment) return path

        val prefix = if (existingPath.endsWith('/')) existingPath else "$existingPath/"
        return origin + prefix + segment
    }

    private fun parseQuery(query: String, into: LinkedHashMap<String, String>) {
        if (query.isEmpty()) return
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            if (eq < 0) into[pair] = "" else into[pair.substring(0, eq)] = pair.substring(eq + 1)
        }
    }
}
