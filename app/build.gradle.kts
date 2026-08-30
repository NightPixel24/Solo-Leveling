plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nightpixel.sololeveling"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nightpixel.sololeveling"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.5.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        // Room migration tests (Phase 17) load each version's committed schema JSON as an asset
        // via MigrationTestHelper - point androidTest at the same directory room.schemaLocation
        // (below) already writes to, rather than duplicating the files.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Local reminders (spec Section 7) - WorkManager over AlarmManager's exact alarms since
    // none of these need to-the-second precision, and it's the spec's own suggested option.
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Google Sign-In (Credential Manager) + Calendar API scope authorization (spec Section 4.1).
    // Calendar events themselves are fetched via plain REST calls (see data/calendar/) rather
    // than the heavyweight google-api-client - keeps this in line with the "no backend, keep it
    // simple" approach the rest of the app follows.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Food photo thumbnails (spec Section 4.6) - Coil over manual Bitmap decoding so large
    // camera-resolution JPEGs get sampled/cached automatically instead of loaded full-size.
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // Room migration tests (Phase 17) - replays every real Migration against the actual
    // committed schema JSON for that version, catching mistakes assembleDebug can't (Room only
    // type-checks the Migration's SQL against its own version's schema when a test asks it to).
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
