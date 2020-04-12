package sh.bourbon.gist.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_gist.*
import sh.bourbon.engine.BourbonEngineListener
import sh.bourbon.engine.EngineConfiguration
import sh.bourbon.engine.RouteBehaviour
import sh.bourbon.gist.R


// TODO: Mark messages as seen when done
class GistActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ORGANIZATION_ID = "EXTRA_ORGANIZATION_ID"
        private const val EXTRA_PROJECT_ID = "EXTRA_PROJECT_ID"
        private const val EXTRA_ENGINE_ENDPOINT = "EXTRA_ENGINE_ENDPOINT"
        private const val EXTRA_IDENTITY_ENDPOINT = "EXTRA_IDENTITY_ENDPOINT"
        private const val EXTRA_MESSAGE_ID = "EXTRA_MESSAGE_ID"

        private const val ACTION_CLOSE = "gist://close"

        var isShown = false

        fun newIntent(
            context: Context,
            organizationId: String,
            projectId: String,
            engineEndpoint: String,
            identityEndpoint: String,
            messageId: String
        ): Intent {
            return Intent(context, GistActivity::class.java).apply {
                putExtra(EXTRA_ORGANIZATION_ID, organizationId)
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_ENGINE_ENDPOINT, engineEndpoint)
                putExtra(EXTRA_IDENTITY_ENDPOINT, identityEndpoint)
                putExtra(EXTRA_MESSAGE_ID, messageId)
            }
        }
    }

    private val organizationId by lazy {
        intent.getStringExtra(EXTRA_ORGANIZATION_ID) ?: throw createArgException()
    }
    private val projectId by lazy {
        intent.getStringExtra(EXTRA_PROJECT_ID) ?: throw createArgException()
    }
    private val engineEndpoint by lazy {
        intent.getStringExtra(EXTRA_ENGINE_ENDPOINT)
            ?: throw createArgException()
    }
    private val identityEndpoint by lazy {
        intent.getStringExtra(EXTRA_IDENTITY_ENDPOINT)
            ?: throw createArgException()
    }
    private val messageId by lazy {
        intent.getStringExtra(EXTRA_MESSAGE_ID)
            ?: throw createArgException()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gist)

        engineView.setup(
            EngineConfiguration(
                organizationId = organizationId,
                projectId = projectId,
                engineEndpoint = engineEndpoint,
                authenticationEndpoint = identityEndpoint,
                engineVersion = 1.0,
                configurationVersion = 1.0
            )
        )

        engineView.setListener(object : BourbonEngineListener {
            var isInitialLoad = true

            override fun onBootstrapped() {
                engineView.updateRoute(messageId, RouteBehaviour.RETAIN)
            }

            override fun onRouteChanged(newRoute: String) {
            }

            override fun onRouteError(route: String) {
                GistSdk.handleEngineRouteError(route)
            }

            override fun onRouteLoaded(route: String) {
                GistSdk.handleEngineRouteLoaded(route)

                if (isInitialLoad) {
                    isInitialLoad = false

                    // Slide up engine view
                    animateEngineEnter()

                    // Notify Gist that the message has been viewed
                    GistSdk.logView(messageId)
                }
            }

            override fun onTap(action: String) {
                when (action) {
                    ACTION_CLOSE -> {
                        GistSdk.handleEngineRouteClosed(messageId)
                        finish()
                    }
                    else -> GistSdk.handleEngineAction(action)
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        isShown = true
    }

    override fun onStop() {
        isShown = false
        super.onStop()
    }

    private fun animateEngineEnter() {
        val slideUp = AnimationUtils.loadAnimation(this@GistActivity, R.anim.anim_in)
        slideUp.startOffset = 1_000 // Offset animation to avoid TextureView jitter

        engineView.visibility = View.VISIBLE
        engineView.startAnimation(slideUp)
    }

    private fun createArgException(): Exception {
        return IllegalArgumentException("GistActivity must be created using GistActivity.newIntent")
    }
}
