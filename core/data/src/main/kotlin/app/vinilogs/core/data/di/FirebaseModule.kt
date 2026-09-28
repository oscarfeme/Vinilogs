package app.vinilogs.core.data.di

import app.vinilogs.core.data.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Firebase SDK entry points [core:data]'s repository implementations depend on.
 *
 * Local-dev emulator wiring: when `BuildConfig.USE_FIREBASE_EMULATOR` is set (opt-in via
 * `firebase.useEmulator=true` in `local.properties`, see `core/data/build.gradle.kts` and
 * `firebase/README.md`) *and* the build is debuggable, every SDK is pointed at the local
 * Emulator Suite instead of whatever project `app/google-services.json` names. `useEmulator`
 * must be called before the singleton is used for anything else, which is guaranteed here
 * since Hilt only ever hands out the instance these `@Provides` methods return. The debuggable
 * check is defence in depth on top of the build-time flag -- a release build can never reach a
 * developer's local emulator even if the flag were left set by mistake.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    private val useEmulator: Boolean
        get() = BuildConfig.USE_FIREBASE_EMULATOR && BuildConfig.DEBUG

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance().apply {
            if (useEmulator) {
                useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, EMULATOR_PORT_AUTH)
            }
        }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance().apply {
            if (useEmulator) {
                useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, EMULATOR_PORT_FIRESTORE)
            }
        }

    // Ports match firebase/firebase.json's emulator config.
    private const val EMULATOR_PORT_AUTH = 9099
    private const val EMULATOR_PORT_FIRESTORE = 8080
}
