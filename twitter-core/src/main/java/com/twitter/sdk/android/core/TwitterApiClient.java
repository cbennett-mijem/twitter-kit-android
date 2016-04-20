/*
 * Copyright (C) 2015 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.twitter.sdk.android.core;

import com.twitter.sdk.android.core.internal.TwitterApi;
import com.twitter.sdk.android.core.models.BindingValues;
import com.twitter.sdk.android.core.models.BindingValuesAdapter;
import com.twitter.sdk.android.core.models.SafeListAdapter;
import com.twitter.sdk.android.core.models.SafeMapAdapter;
import com.twitter.sdk.android.core.services.AccountService;
import com.twitter.sdk.android.core.services.CollectionService;
import com.twitter.sdk.android.core.services.ConfigurationService;
import com.twitter.sdk.android.core.services.FavoriteService;
import com.twitter.sdk.android.core.services.ListService;
import com.twitter.sdk.android.core.services.MediaService;
import com.twitter.sdk.android.core.services.SearchService;
import com.twitter.sdk.android.core.services.StatusesService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A class to allow authenticated access to Twitter API endpoints.
 * Can be extended to provided additional endpoints by extending and providing Retrofit API
 * interfaces to {@link com.twitter.sdk.android.core.TwitterApiClient#getService(Class)}
 */
public class TwitterApiClient {
    final ConcurrentHashMap<Class, Object> services;
    final Retrofit apiAdapter;

    TwitterApiClient(TwitterAuthConfig authConfig,
                     Session session,
                     TwitterApi twitterApi,
                     SSLSocketFactory sslSocketFactory) {

        if (session == null) {
            throw new IllegalArgumentException("Session must not be null.");
        }

        this.services = new ConcurrentHashMap<>();

        final OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory)
                .addInterceptor(new AuthInterceptor(session, authConfig))
                .build();

        final Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new SafeListAdapter())
                .registerTypeAdapterFactory(new SafeMapAdapter())
                .registerTypeAdapter(BindingValues.class, new BindingValuesAdapter())
                .create();

        apiAdapter = new Retrofit.Builder()
                .client(client)
                .baseUrl(twitterApi.getBaseHostUrl())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    /**
     * Must be instantiated after {@link com.twitter.sdk.android.core.TwitterCore} has been
     * initialized via {@link io.fabric.sdk.android.Fabric#with(android.content.Context, io.fabric.sdk.android.Kit[])}.
     *
     * @param session Session to be used to create the API calls.
     *
     * @throws java.lang.IllegalArgumentException if TwitterSession argument is null
     */
    public TwitterApiClient(Session session) {
        this(TwitterCore.getInstance().getAuthConfig(), session, new TwitterApi(),
                TwitterCore.getInstance().getSSLSocketFactory());
    }

    /**
     * @return {@link com.twitter.sdk.android.core.services.AccountService} to access TwitterApi
     */
    public AccountService getAccountService() {
        return getService(AccountService.class);
    }

    /**
     * @return {@link com.twitter.sdk.android.core.services.FavoriteService} to access TwitterApi
     */
    public FavoriteService getFavoriteService() {
        return getService(FavoriteService.class);
    }

    /**
     * @return {@link com.twitter.sdk.android.core.services.StatusesService} to access TwitterApi
     */
    public StatusesService getStatusesService() {
        return getService(StatusesService.class);
    }

    /**
     * @return {@link com.twitter.sdk.android.core.services.SearchService} to access TwitterApi
     */
    public SearchService getSearchService() {
        return getService(SearchService.class);
    }

    /**
     * @return {@link com.twitter.sdk.android.core.services.ListService} to access TwitterApi
     */
    public ListService getListService() {
        return getService(ListService.class);
    }

    /**
     * Use CollectionTimeline directly, CollectionService is expected to change.
     * @return {@link CollectionService} to access TwitterApi
     */
    public CollectionService getCollectionService() {
        return getService(CollectionService.class);
    }

    /**
     * @return {@link com.twitter.sdk.android.core.services.ConfigurationService} to access TwitterApi
     */
    public ConfigurationService getConfigurationService() {
        return getService(ConfigurationService.class);
    }

    /**
     * @return {@link com.twitter.sdk.android.core.services.MediaService} to access Twitter API
     * upload endpoints.
     */
    public MediaService getMediaService() {
        return getService(MediaService.class);
    }

    /**
     * Converts Retrofit style interface into instance for API access
     *
     * @param cls Retrofit style interface
     * @return instance of cls
     */
    @SuppressWarnings("unchecked")
    protected <T> T getService(Class<T> cls) {
        if (!services.contains(cls)) {
            services.putIfAbsent(cls, apiAdapter.create(cls));
        }
        return (T) services.get(cls);
    }
}
