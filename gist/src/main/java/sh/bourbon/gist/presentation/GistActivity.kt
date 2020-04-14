package sh.bourbon.gist.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
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
        animateEngineEnter()
    }

    private fun animateEngineEnter() {
        val slideUp = AnimationUtils.loadAnimation(this@GistActivity, R.anim.anim_in)
        slideUp.startOffset = 1_000 // Offset animation to avoid TextureView jitter

        engineView.visibility = View.VISIBLE
        engineView.startAnimation(slideUp)
    }
}
