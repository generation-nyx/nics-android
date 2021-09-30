package edu.mit.ll.nics.android.utils.glide;

import android.app.Application;
import android.content.Context;
import android.graphics.drawable.PictureDrawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.Excludes;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;

import javax.inject.Inject;

import dagger.Component;
import dagger.Module;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import edu.mit.ll.nics.android.di.AuthModule;
import edu.mit.ll.nics.android.di.Qualifiers.AuthHttpClient;
import okhttp3.OkHttpClient;

/**
 * Registers OkHttp related classes via Glide's annotation processor.
 *
 * <p>For Applications that depend on this library and include an {@link AppGlideModule} and Glide's
 * annotation processor, this class will be automatically included.
 */
@Excludes(com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule.class)
@GlideModule
public final class NICSGlideModule extends AppGlideModule {
    @Inject @AuthHttpClient OkHttpClient client;

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {

        // get AuthHttpClient instance injected
        Context appContext = context.getApplicationContext();
        NICSGlideModuleEntryPoint hiltEntryPoint =
                EntryPointAccessors.fromApplication(appContext, NICSGlideModuleEntryPoint.class);
        OkHttpClient authHttpClient = hiltEntryPoint.authHttpClient();

        registry
                .replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(authHttpClient))
                .register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
                .append(InputStream.class, SVG.class, new SvgDecoder());
    }

    @EntryPoint
    @InstallIn(SingletonComponent.class)
    interface NICSGlideModuleEntryPoint {
        @AuthHttpClient
        OkHttpClient authHttpClient();
    }
}
