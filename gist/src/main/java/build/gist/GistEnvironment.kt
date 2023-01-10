package build.gist

interface GistEnvironmentEndpoints {
    fun getGistQueueApiUrl(): String
    fun getGistApiUrl(): String
    fun getGistRendererUrl(): String
}

enum class GistEnvironment: GistEnvironmentEndpoints {
    DEV {
        override fun getGistQueueApiUrl() = "https://queue.api.dev.gist.build"
        override fun getGistApiUrl() = "https://api.dev.gist.build"
        override fun getGistRendererUrl() = "https://code.gist.build/renderer/0.0.20"
    },

    LOCAL {
        override fun getGistQueueApiUrl() = "http://queue.api.local.gist.build:86"
        override fun getGistApiUrl() = "http://api.local.gist.build:83"
        override fun getGistRendererUrl() = "https://renderer.gist.build/1.0"
    },

    PROD {
        override fun getGistQueueApiUrl() = "https://queue.api.gist.build"
        override fun getGistApiUrl() = "https://api.gist.build"
        override fun getGistRendererUrl() = "https://renderer.gist.build/1.0"
    };
}