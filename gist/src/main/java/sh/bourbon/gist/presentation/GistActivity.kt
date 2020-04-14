package sh.bourbon.gist.presentation

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import kotlinx.android.synthetic.main.activity_gist.*
import sh.bourbon.gist.R


class GistActivity : AppCompatActivity() {
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GistActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gist)

        val animation = AnimatorInflater.loadAnimator(this, R.animator.animate_in);
        animation.startDelay = 1000 // Delay animation to avoid TextureView jitter
        animation.setTarget(engineView)
        animation.start()
    }

    override fun finish() {
        val animation = AnimatorInflater.loadAnimator(this, R.animator.animate_out)
        animation.setTarget(engineView)
        animation.start()
        animation.doOnEnd {
            super.finish()
        }
    }
}
