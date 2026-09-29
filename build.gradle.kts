// Top-level build file where you can add configuration options common to all sub-projects/modules.
//
// Two plugins were removed from this block because nothing in the app consumed what
// they produced, and both cost configuration time on every single build:
//
//   - KSP. Every annotation processor that would have used it is commented out in
//     app/build.gradle.kts, so it resolved the plugin, joined the task graph and
//     produced nothing. Re-add `alias(libs.plugins.google.devtools.ksp)` at the
//     same moment you uncomment a `ksp(...)` dependency.
//   - The Maps Platform secrets plugin. It generates `SECRET_*` BuildConfig fields;
//     this app reads no secrets at all (it talks to public playlists and a public
//     update manifest), so the field it generated was dead. It also dragged a
//     Google Maps Platform plugin into a build with no Maps and no Firebase.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.services) apply false
}
