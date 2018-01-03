package com.toranj.beatbaz.dataManager;

import android.content.Context;
import android.os.Debug;
import android.os.Environment;

import com.toranj.beatbaz.app.App;
import com.toranj.beatbaz.app.injection.ApplicationContext;
import com.toranj.beatbaz.dataManager.utils.NetworkUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

import static com.toranj.beatbaz.BuildConfig.DEBUG;
import static okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;

/**
 * Created by itparsa on 12/27/17.
 */

@Singleton
public class AppRequestCache {
    private static final String CACHE_CONTROL = "Cache-Control";

    private Context mContext;

    @Inject
    public AppRequestCache(@ApplicationContext Context context) {
        mContext = context;
    }

    public OkHttpClient createClient(int cacheSize) {
        File httpCacheDirectory = new File(mContext.getCacheDir(), "http-cache");// Here to facilitate the file directly on the SD Kagan catalog HttpCache in ， Generally put in context.getCacheDir() in
        Cache cache = new Cache(httpCacheDirectory, cacheSize);

        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)// Set connection timeout
                .readTimeout(10, TimeUnit.SECONDS)// Read timeout
                .writeTimeout(10, TimeUnit.SECONDS)// Write timeout
                .addNetworkInterceptor(provideCacheInterceptor())// Add a custom cache interceptor （ Explain later ）， Note that it needs to be used here. .addNetworkInterceptor
                .addInterceptor(provideHttpLoggingInterceptor())
                .addInterceptor(provideOfflineCacheInterceptor(mContext))
                .cache(cache)// Add cache
                .build();
    }

    private HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(message -> Timber.d(message));

        httpLoggingInterceptor.setLevel(DEBUG ? HEADERS : NONE);

        return httpLoggingInterceptor;
    }

    private Interceptor provideOfflineCacheInterceptor (Context context) {
        return chain -> {
            Request request = chain.request();

            if (!NetworkUtil.isNetworkAvailable(context)) {
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxStale( 7, TimeUnit.DAYS )
                        .build();

                request = request.newBuilder()
                        .cacheControl( cacheControl )
                        .build();
            }
            return chain.proceed( request );
        };
    }

    private Interceptor provideCacheInterceptor () {
        return chain -> {
            Response response = chain.proceed( chain.request() );
            // re-write response header to force use of cache
            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge( 2, TimeUnit.MINUTES )
                    .build();

            return response.newBuilder()
                    .header( CACHE_CONTROL, cacheControl.toString() )
                    .build();
        };
    }

}
