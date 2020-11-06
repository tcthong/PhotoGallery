package utc.thong.photogallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val isContainerFragmentEmpty = savedInstanceState == null
        if (isContainerFragmentEmpty) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, PhotoGalleryFragment.newInstance())
                .commit()
        }
    }
}