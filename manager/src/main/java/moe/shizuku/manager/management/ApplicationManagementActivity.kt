package moe.shizuku.manager.management

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.authorization.AuthorizationManager
import moe.shizuku.manager.databinding.AppsActivityBinding
import moe.shizuku.manager.utils.DeviceAuthentication
import rikka.lifecycle.Status
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.shizuku.Shizuku
import java.util.*

class ApplicationManagementActivity : AppBarActivity() {

    private val viewModel by appsViewModel()
    private val adapter = AppsAdapter()
    private lateinit var authentication: DeviceAuthentication
    private lateinit var binding: AppsActivityBinding
    private var authenticationJob: Job? = null
    private var authenticated = false
    private var authenticating = false

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        if (!isFinishing) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Shizuku.pingBinder()) {
            finish()
            return
        }

        binding = AppsActivityBinding.inflate(layoutInflater)
        binding.root.visibility = View.INVISIBLE
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authentication = DeviceAuthentication(
            this,
            onSuccess = {
                authenticating = false
                authenticated = true
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    showContent()
                }
            },
            onError = {
                authenticating = false
                finish()
            },
        )

        viewModel.packages.observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    adapter.updateData(it.data)
                }
                Status.ERROR -> {
                    finish()
                    val tr = it.error
                    Toast.makeText(this, Objects.toString(tr, "unknown"), Toast.LENGTH_SHORT).show()
                    tr.printStackTrace()
                }
                Status.LOADING -> {

                }
            }
        }
        val recyclerView = binding.list
        recyclerView.adapter = adapter
        recyclerView.fixEdgeEffect()
        recyclerView.addEdgeSpacing(top = 8f, bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                viewModel.load(true)
            }
        })

        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    override fun onDestroy() {
        Shizuku.removeBinderDeadListener(binderDeadListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updateAccess()
    }

    override fun onStop() {
        authenticated = false
        authenticationJob?.cancel()
        if (::binding.isInitialized) {
            binding.root.visibility = View.INVISIBLE
        }
        super.onStop()
    }

    private fun updateAccess() {
        authenticationJob?.cancel()
        authenticationJob = lifecycleScope.launch {
            val authenticationRequired = withContext(Dispatchers.IO) {
                runCatching { AuthorizationManager.getRequireAuthentication() }.getOrNull()
            }
            when {
                authenticationRequired == null -> finish()
                !authenticationRequired || authenticated -> showContent()
                !authenticating -> {
                    authenticating = true
                    authentication.authenticate(getString(R.string.authentication_unlock_title))
                }
            }
        }
    }

    private fun showContent() {
        binding.root.visibility = View.VISIBLE
        if (viewModel.packages.value == null) {
            viewModel.load()
        }
    }
}
