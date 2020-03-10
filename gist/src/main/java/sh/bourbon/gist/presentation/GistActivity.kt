package sh.bourbon.gist.presentation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_gist.*
import sh.bourbon.engine.EngineConfiguration
import sh.bourbon.engine.RouteBehavior
import sh.bourbon.gist.R
import java.lang.Exception
import java.lang.IllegalArgumentException

// TODO: Mark messages as seen when done
class GistActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ORGANIZATION_ID = "EXTRA_ORGANIZATION_ID"
        private const val EXTRA_PROJECT_ID = "EXTRA_PROJECT_ID"
        private const val EXTRA_ENGINE_ENDPOINT = "EXTRA_PROJECT_ID"
        private const val EXTRA_IDENTITY_ENDPOINT = "EXTRA_PROJECT_ID"
        private const val EXTRA_MESSAGE_ID = "EXTRA_MESSAGE_ID"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gist)

        val organizationId =
            intent.getStringExtra(EXTRA_ORGANIZATION_ID) ?: throw createArgException()
        val projectId = intent.getStringExtra(EXTRA_PROJECT_ID) ?: throw createArgException()
        val engineEndpoint = intent.getStringExtra(EXTRA_ENGINE_ENDPOINT)
            ?: throw createArgException()
        val identityEndpoint = intent.getStringExtra(EXTRA_IDENTITY_ENDPOINT)
            ?: throw createArgException()

        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
            ?: throw createArgException()

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

        engineView.updateRoute(messageId, RouteBehavior.RETAIN)
    }

    private fun createArgException(): Exception {
        return IllegalArgumentException("GistActivity must be created using GistActivity.newIntent")
    }
}
