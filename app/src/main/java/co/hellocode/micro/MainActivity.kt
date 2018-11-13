package co.hellocode.micro

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import co.hellocode.micro.newpost.NewPostActivity
import co.hellocode.micro.tabs.TabAdapter
import co.hellocode.micro.utils.NEW_POST_REQUEST_CODE
import co.hellocode.micro.utils.PREFS_FILENAME
import co.hellocode.micro.utils.TOKEN
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initToolbar()

        // Make sure user has a token before proceeding
        val prefs = this.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val token: String? = prefs?.getString(TOKEN, null)
        if (token == null) {
            requestUserToken()
        } else {
            completeSetUp()
        }
    }

    private fun completeSetUp() {
        val adapter = TabAdapter(supportFragmentManager)
        view_pager.adapter = adapter
        view_pager.offscreenPageLimit = 4

        fab.setOnClickListener {
            val intent = Intent(this, NewPostActivity::class.java)
            startActivityForResult(intent, NEW_POST_REQUEST_CODE)
        }

        tab_layout.setupWithViewPager(view_pager)
        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })
    }

    private fun requestUserToken() {
        val prefs = this.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val input = EditText(this)
        val builder = AlertDialog.Builder(this)
        builder.setView(input)
                .setTitle("Set your app token")
                .setNegativeButton("Cancel") { dialogInterface, _ ->
                    dialogInterface.cancel()
                }
                .setPositiveButton("Save") { _, _ ->
                    // Put token in sharedPrefs so we can use it to make network calls later
                    prefs.edit().putString(TOKEN, input.text.toString().toLowerCase()).apply()
                    completeSetUp()
                    Toast.makeText(this, "Token set, thanks.", Toast.LENGTH_SHORT).show()
                }
                .create()
                .show()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Pico"
    }
}
