package com.briot.balmerlawrie.implementor

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation.findNavController
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.local.PrefRepository
import com.google.android.material.snackbar.Snackbar
import es.dmoral.toasty.Toasty
import io.github.pierry.progress.Progress
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

// Not object class. AndroidManifest.xml error happen.
class MainApplication : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: MainApplication? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }

        fun hasNetwork(context: Context): Boolean {
            var isConnected = false // Initial Value
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            if (activeNetwork != null && activeNetwork.isConnected)
                isConnected = true
            return isConnected
        }
    }

    override fun onCreate() {
        super.onCreate()
        // initialize for any

        // Use ApplicationContext.
        // example: SharedPreferences etc...
        val context: Context = MainApplication.applicationContext()
    }
}

class ResponseHeaderAuthTokenInterceptor : Interceptor {

    fun hasNetwork(context: Context): Boolean {
        var isConnected = false // Initial Value
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        if (activeNetwork != null && activeNetwork.isConnected)
            isConnected = true
        return isConnected
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val originalResponse = chain.proceed(request)
        val cacheControl = originalResponse.header("Cache-Control")

        val localheaders = originalResponse.headers("token")

        val jwtTokenExists = localheaders.isNotEmpty()

        if (jwtTokenExists) {
            val jwtToken = localheaders.get(0)
            PrefRepository.singleInstance.setKeyValue("token", jwtToken ?: "")
        }

        var cacheHeaderValue = if (!hasNetwork(MainApplication.applicationContext())!!){
            "public, only-if-cached, max-stale=" + PrefConstants().MAX_STALE
        } else {
            "public, max-age=" + PrefConstants().MAX_AGE
        }

        originalResponse.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheHeaderValue)
                .build()

        return originalResponse
    }

}

class RequestHeaderAuthTokenInterceptor : Interceptor {

    fun hasNetwork(context: Context): Boolean {
        var isConnected = false // Initial Value
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        if (activeNetwork != null && activeNetwork.isConnected)
            isConnected = true
        return isConnected
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()

        var request = chain.request()

        if (hasNetwork(MainApplication.applicationContext())!!) {
            builder.removeHeader("Pragma")
            builder.header("Cache-Control", "public, max-age=" + PrefConstants().MAX_AGE).build()
        } else {
            builder.removeHeader("Pragma")
            builder.header("Cache-Control", "public, only-if-cached, max-stale=" + PrefConstants().MAX_STALE).build()
        }

        val tokenStr = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().USER_TOKEN, "")
        if (tokenStr.length > 1) {
            val token: String = "JWT " + tokenStr
            builder.addHeader("Authorization", token)
        }
        builder.addHeader("Content-Type", "application/json")

        return chain.proceed(builder.build())
    }

}

class RetrofitHelper {

    companion object {
        val BASE_URL = BuildConfig.HOSTNAME;

        private fun getOkHttpClient(): OkHttpClient {

            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

            val cacheSize = (5 * 1024 * 1024).toLong()
            //            val  cacheFile = File(MainApplication.applicationContext(), "AppCacheFile")
            var cacheFile = File( MainApplication.applicationContext().cacheDir.path + "/AppCacheFile")
            val cache = Cache(cacheFile, cacheSize)

            val okHttpClient: OkHttpClient.Builder = OkHttpClient().newBuilder()
                    .connectTimeout((30).toLong(), TimeUnit.SECONDS)
                    .cache(cache)
                    .readTimeout((90).toLong(), TimeUnit.SECONDS)
                    .writeTimeout((60).toLong(), TimeUnit.SECONDS)


            okHttpClient.interceptors().add(httpLoggingInterceptor)
            okHttpClient.interceptors().add(RequestHeaderAuthTokenInterceptor())
            okHttpClient.addNetworkInterceptor(ResponseHeaderAuthTokenInterceptor())

            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor();
                // set your desired log level
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                okHttpClient.interceptors().add(logging);
            }


            return okHttpClient.build()
        }

        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(getOkHttpClient())
                .build()
    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
    }

    override fun onSupportNavigateUp()
            = findNavController(findViewById(R.id.nav_host_fragment)).navigateUp()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.activity_main_drawer, menu);
        return true;
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        var savedToken: String = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().USER_TOKEN, "")
        if (savedToken.isEmpty()) {
            return false
        }

        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.nav_logout -> {
                logout()
                true
            }
            R.id.nav_user_profile -> {
                showUserProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUserProfile() {
        var view:View = findViewById(R.id.nav_host_fragment)
        if (view != null) {
            try {
                findNavController(view).navigate(R.id.action_homeFragment_to_userProfileFragment)
            } catch (e: Exception) {

            }
        }
    }

    private fun logout() {
        var savedToken: String = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().USER_TOKEN, "")
        if (savedToken.isEmpty()) {
            return
        }

        invalidateOptionsMenu()
        PrefRepository.singleInstance.setKeyValue(PrefConstants().USER_TOKEN, "")
        PrefRepository.singleInstance.setKeyValue(PrefConstants().USER_NAME, "")
        PrefRepository.singleInstance.setKeyValue(PrefConstants().USER_ID, "")
        PrefRepository.singleInstance.setKeyValue(PrefConstants().EMPLOYEE_STATUS, "")
        PrefRepository.singleInstance.setKeyValue(PrefConstants().EMPLOYEE_NAME, "")
        PrefRepository.singleInstance.setKeyValue(PrefConstants().EMPLOYEE_EMAIL, "")
        PrefRepository.singleInstance.setKeyValue(PrefConstants().EMPLOYEE_PHONE, "")
        PrefRepository.singleInstance.setKeyValue(PrefConstants().ROLE_ID, "")
        PrefRepository.singleInstance.setKeyValue(PrefConstants().ROLE_NAME, "")

        this.applicationContext.let { PrefRepository.singleInstance.serializePrefs(it) }

        val userToken: String = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().USER_TOKEN, "")
        val navController = findNavController(findViewById(R.id.nav_host_fragment))
        if (userToken.isEmpty()) {
            navController.popBackStack(R.id.mainFragment, false)
        }

    }
}

class UiHelper {
    companion object {
        val SNACKBAR_COLOR = PrefConstants().messageBackgroundColor

        fun showAlert(activity: AppCompatActivity, message: String, cancellable: Boolean = false) {
            AlertDialog.Builder(activity, R.style.MyDialogTheme).create().apply {
            setTitle("Alert")
                setMessage(message)
                setCancelable(cancellable)
                setButton(AlertDialog.BUTTON_NEUTRAL, "OK", { dialog, _ -> dialog.dismiss() })
                show()
            }
        }

        fun showSnackbarMessage(activity: AppCompatActivity, message: String, duration: Int = Snackbar.LENGTH_INDEFINITE) {
            val coordinatorLayout = activity.findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.container)

            val snackbar = Snackbar.make(coordinatorLayout!!, message, duration)
            if (duration == Snackbar.LENGTH_INDEFINITE) {
                snackbar.setAction("OK", View.OnClickListener {

                })
                val actionButton = snackbar.view.findViewById<Button>(com.google.android.material.R.id.snackbar_action)
//            actionButton.typeface = ResourcesCompat.getFont(activity, R.font.lato_bold)
                actionButton.textSize = 18f
            }

            snackbar.setActionTextColor(Color.WHITE)
            val snackbarTextView = snackbar.view
                    .findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            snackbarTextView.setTextColor(Color.WHITE)
//            snackbarTextView.typeface = ResourcesCompat.getFont(activity, R.font.lato)
            snackbarTextView.textSize = 16f

            snackbar.view.setBackgroundColor(SNACKBAR_COLOR)
            snackbar.show()
        }

        fun showNoInternetSnackbarMessage(activity: AppCompatActivity) {
            showSnackbarMessage(activity, activity.getString(R.string.no_internet_connection_message), Snackbar.LENGTH_INDEFINITE)
        }

        fun showSomethingWentWrongSnackbarMessage(activity: AppCompatActivity) {
            showSnackbarMessage(activity, activity.getString(R.string.oops_something_went_wrong))
        }

        fun showTryAgainLaterSnackbarMessage(activity: AppCompatActivity) {
            showSnackbarMessage(activity, activity.getString(R.string.try_again_later), Snackbar.LENGTH_LONG)
        }

        fun showProgressIndicator(context: Context, message: String): Progress {
            val progress = Progress(context)

            progress.setBackgroundColor(Color.parseColor("#EEEEEE"))
                    .setMessage(message)
                    .setMessageColor(Color.parseColor("#444444"))
                    .setProgressColor(Color.parseColor("#444444"))
                    .show()

            return progress
        }

        fun hideProgress(progress: Progress?) {
            progress?.dismiss()
        }

        fun isNetworkError(error: Throwable) =
                error is SocketException || error is SocketTimeoutException || error is UnknownHostException

        fun showAlert(activity: AppCompatActivity, message: String) {
            AlertDialog.Builder(activity, R.style.MyDialogTheme).create().apply {
            setTitle("Alert")
                setMessage(message)
                setButton(AlertDialog.BUTTON_NEUTRAL, "OK", { dialog, _ -> dialog.dismiss() })
                show()
            }
        }

        fun showSuccessToast(activity: AppCompatActivity, message: String) {
            var toast = Toasty.success(activity, message, 3000)
//            var toast = Toast.makeText(activity, message, Toast.LENGTH_LONG)
            toast.show()
        }

        fun showErrorToast(activity: AppCompatActivity, message: String) {
            var toast = Toasty.error(activity, message, 3000)
//            var toast = Toast.makeText(activity, message, Toast.LENGTH_LONG)
            toast.show()
        }
        fun showWarningToast(activity: AppCompatActivity, message: String) {
            var toast = Toasty.warning(activity, message, 3000)
//            var toast = Toast.makeText(activity, message, Toast.LENGTH_LONG)
            toast.show()
        }

    }


}
