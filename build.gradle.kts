// Root build file. Plugins are declared here for the whole build but applied
// in the modules that need them - hence `apply false`.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}
