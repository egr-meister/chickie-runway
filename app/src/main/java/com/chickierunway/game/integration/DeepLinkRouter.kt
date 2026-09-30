package com.chickierunway.game.integration

/** Single entry point for links from pushes; queues them until the UI subscribes. */
object DeepLinkRouter {

    private val lock = Any()
    private var handler: ((String) -> Unit)? = null
    // A push tap can cold-start the app before any composable has subscribed.
    private val pending = ArrayDeque<String>()

    fun setHandler(newHandler: (String) -> Unit) {
        val drained: List<String>
        synchronized(lock) {
            handler = newHandler
            drained = pending.toList()
            pending.clear()
        }
        // Outside the lock: the handler touches Compose state and may re-enter.
        drained.forEach(newHandler)
    }

    fun clearHandler() {
        synchronized(lock) { handler = null }
    }

    fun handle(url: String?) {
        if (url.isNullOrBlank()) return
        val current: ((String) -> Unit)?
        synchronized(lock) {
            current = handler
            // Queued only when nobody listens, so each link is delivered exactly once.
            if (current == null) pending.addLast(url)
        }
        current?.invoke(url)
    }
}
