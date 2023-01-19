package build.gist

interface GistEnvironmentEndpoints {
    fun getGistQueueApiUrl(): String
    fun getGistApiUrl(): String
    fun getGistRendererUrl(): String
}

enum class GistEnvironment: GistEnvironmentEndpoints {
    DEV {
        override fun getGistQueueApiUrl() = "https://gist-queue-consumer-api.cloud.dev.gist.build"
        override fun getGistApiUrl() = "https://api.dev.gist.build"
        override fun getGistRendererUrl() = "https://renderer.gist.build/1.0"
    },

    LOCAL {
        override fun getGistQueueApiUrl() = "http://queue.api.local.gist.build:86"
        override fun getGistApiUrl() = "http://api.local.gist.build:83"
        override fun getGistRendererUrl() = "https://renderer.gist.build/1.0"
    },

    PROD {
        override fun getGistQueueApiUrl() = "https://gist-queue-consumer-api.cloud.gist.build"
        override fun getGistApiUrl() = "https://api.gist.build"
        override fun getGistRendererUrl() = "https://renderer.gist.build/1.0"
    };
}